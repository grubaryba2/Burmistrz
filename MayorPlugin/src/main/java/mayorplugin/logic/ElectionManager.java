package mayorplugin.logic;

import mayorplugin.MayorPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scheduler.BukkitRunnable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class ElectionManager {

    private final MayorPlugin plugin;
    private String currentMayorName;
    private UUID currentMayorUUID;
    private Instant termEnd;
    private boolean electionRunning = false;
    private Map<UUID, Integer> votes = new HashMap<>();
    private List<UUID> candidates = new ArrayList<>();
    private List<UUID> futureCandidates = new ArrayList<>();
    private List<UUID> hasVoted = new ArrayList<>();
    private Map<UUID, Integer> bannedPlayers = new HashMap<>();

    private PermissionAttachment currentMayorAttachment;

    public ElectionManager(MayorPlugin plugin) {
        this.plugin = plugin;
        loadData();

        new BukkitRunnable() {
            @Override
            public void run() {
                Instant now = Instant.now();
                if (termEnd != null && now.isAfter(termEnd)) {
                    if (electionRunning) {
                        endElection();
                    } else {
                        startElection();
                    }
                }
                plugin.getHologramManager().createOrUpdateHologram();
            }
        }.runTaskTimer(plugin, 20L * 10, 20L * 60);
    }

    public void startElection() {
        if (currentMayorUUID != null) {
            Player oldMayorPlayer = Bukkit.getPlayer(currentMayorUUID);
            if (oldMayorPlayer != null && currentMayorAttachment != null) {
                try {
                    oldMayorPlayer.removeAttachment(currentMayorAttachment);
                } catch (Exception ignored) {}
            }
            currentMayorAttachment = null;
        }

        if (currentMayorUUID != null) {
            Player oldMayorPlayer = Bukkit.getPlayer(currentMayorUUID);
            if (oldMayorPlayer != null) {
                plugin.getTagManager().removeTag(oldMayorPlayer);
            }
        }
        this.currentMayorName = "Brak";
        this.currentMayorUUID = null;
        this.electionRunning = true;
        this.candidates.clear();
        this.candidates.addAll(futureCandidates);
        this.futureCandidates.clear();
        this.votes.clear();
        this.hasVoted.clear();

        for(UUID candidate : candidates) {
            votes.put(candidate, 0);
        }

        long electionDurationHours = plugin.getDataManager().getConfig().getLong("election-duration-hours");
        this.termEnd = Instant.now().plus(Duration.ofHours(electionDurationHours));

        saveData();

        String title = format(plugin.getDataManager().getConfig().getString("messages.election-start-title"));
        String subtitle = format(plugin.getDataManager().getConfig().getString("messages.election-start-subtitle"));
        Sound sound = Sound.valueOf(plugin.getDataManager().getConfig().getString("sounds.election-start", "ENTITY_ENDER_DRAGON_GROWL"));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(title, subtitle, 10, 70, 20);
            p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
        }

        plugin.getHologramManager().createOrUpdateHologram();
    }

    public void endElection() {
        this.electionRunning = false;

        Player oldMayorPlayer = (currentMayorUUID != null) ? Bukkit.getPlayer(currentMayorUUID) : null;

        if (currentMayorUUID != null) {
            if (oldMayorPlayer != null) {
                plugin.getTagManager().removeTag(oldMayorPlayer);
            }
        }

        OfflinePlayer winner = null;
        int winnerVotes = 0;

        if (candidates.size() == 1) {
            winner = Bukkit.getOfflinePlayer(candidates.get(0));
            winnerVotes = getVotes().getOrDefault(winner.getUniqueId(), 0);
        } else if (candidates.size() > 1) {
            Map.Entry<UUID, Integer> winnerEntry = votes.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);
            if(winnerEntry != null) {
                winner = Bukkit.getOfflinePlayer(winnerEntry.getKey());
                winnerVotes = winnerEntry.getValue();
            }
        }

        if (winner != null) {
            this.currentMayorName = winner.getName();
            this.currentMayorUUID = winner.getUniqueId();
            announceNewMayor(winner, winnerVotes);
            updateMayorPermissions(oldMayorPlayer, winner.getPlayer());
        } else {
            this.currentMayorName = "Brak";
            this.currentMayorUUID = null;
            updateMayorPermissions(oldMayorPlayer, null);
            Bukkit.broadcastMessage(format(plugin.getDataManager().getConfig().getString("messages.prefix") + plugin.getDataManager().getConfig().getString("messages.no-candidates-broadcast")));
        }

        long termDurationDays = plugin.getDataManager().getConfig().getLong("term-duration-days");
        this.termEnd = Instant.now().plus(Duration.ofDays(termDurationDays));
        decreaseBanDurations();
        saveData();
        plugin.getHologramManager().createOrUpdateHologram();
    }

    public void forceSetMayor(OfflinePlayer newMayor) {
        Player oldMayorPlayer = (currentMayorUUID != null) ? Bukkit.getPlayer(currentMayorUUID) : null;

        if (currentMayorUUID != null) {
            if (oldMayorPlayer != null) {
                plugin.getTagManager().removeTag(oldMayorPlayer);
            }
        }
        this.currentMayorName = newMayor.getName();
        this.currentMayorUUID = newMayor.getUniqueId();
        this.electionRunning = false;
        long termDurationDays = plugin.getDataManager().getConfig().getLong("term-duration-days");
        this.termEnd = Instant.now().plus(Duration.ofDays(termDurationDays));

        announceNewMayor(newMayor, -1);
        updateMayorPermissions(oldMayorPlayer, newMayor.getPlayer());

        saveData();
        plugin.getHologramManager().createOrUpdateHologram();
    }

    private void updateMayorPermissions(Player oldMayor, Player newMayor) {
        if (oldMayor != null && currentMayorAttachment != null) {
            try {
                oldMayor.removeAttachment(currentMayorAttachment);
            } catch (Exception ignored) {}
        }
        currentMayorAttachment = null;

        if (newMayor != null) {
            currentMayorAttachment = newMayor.addAttachment(plugin, "dzialka.mayor", true);
        }
    }

    private void announceNewMayor(OfflinePlayer mayor, int votes) {
        if (votes != -1) {
            String broadcast = plugin.getDataManager().getConfig().getString("messages.new-mayor-broadcast")
                    .replace("%mayor%", mayor.getName())
                    .replace("%votes%", String.valueOf(votes));
            Bukkit.broadcastMessage(format(plugin.getDataManager().getConfig().getString("messages.prefix") + broadcast));
        }

        String title = format(plugin.getDataManager().getConfig().getString("messages.new-mayor-title"));
        String subtitle = format(plugin.getDataManager().getConfig().getString("messages.new-mayor-subtitle").replace("%mayor%", mayor.getName()));
        Sound sound = Sound.valueOf(plugin.getDataManager().getConfig().getString("sounds.new-mayor", "ENTITY_PLAYER_LEVELUP"));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(title, subtitle, 10, 70, 20);
            p.playSound(p.getLocation(), sound, 1.0f, 1.0f);
        }

        Player newMayorPlayer = mayor.getPlayer();
        if (newMayorPlayer != null) {
            plugin.getTagManager().applyTag(newMayorPlayer);
        }
    }

    public void addVotes(UUID candidate, int amount) {
        setVotes(candidate, votes.getOrDefault(candidate, 0) + amount);
    }

    public void setVotes(UUID candidate, int amount) {
        if (candidates.contains(candidate)) {
            votes.put(candidate, Math.max(0, amount));
            saveData();
        }
    }

    public void removeVotes(UUID candidate, int amount) {
        addVotes(candidate, -amount);
    }

    public void addCandidate(UUID playerUUID) {
        if (!candidates.contains(playerUUID)) {
            candidates.add(playerUUID);
            votes.put(playerUUID, 0);
            saveData();
        }
    }

    public void addFutureCandidate(UUID uuid) {
        if (!futureCandidates.contains(uuid) && !isCandidate(uuid)) {
            futureCandidates.add(uuid);
            saveData();
        }
    }

    public boolean isCandidate(UUID uuid) { return candidates.contains(uuid); }

    public boolean isFutureCandidate(UUID uuid) { return futureCandidates.contains(uuid); }

    public void banPlayer(UUID uuid, int duration) {
        bannedPlayers.put(uuid, duration);
        saveData();
    }

    public void unbanPlayer(UUID uuid) {
        bannedPlayers.remove(uuid);
        saveData();
    }

    public int getBanDuration(UUID uuid) {
        return bannedPlayers.getOrDefault(uuid, -1);
    }

    private void decreaseBanDurations() {
        Map<UUID, Integer> updatedBans = new HashMap<>();
        for (Map.Entry<UUID, Integer> entry : bannedPlayers.entrySet()) {
            if (entry.getValue() > 1) {
                updatedBans.put(entry.getKey(), entry.getValue() - 1);
            } else if (entry.getValue() != 0) {
                // Ban na 1 kadencję, więc go usuwamy.
            } else {
                updatedBans.put(entry.getKey(), 0);
            }
        }
        this.bannedPlayers = updatedBans;
    }

    public void addVote(UUID candidateUUID, UUID voterUUID) {
        votes.put(candidateUUID, votes.getOrDefault(candidateUUID, 0) + 1);
        hasVoted.add(voterUUID);
        saveData();
    }

    public String getCurrentMayorName() { return currentMayorName; }

    public UUID getCurrentMayorUUID() { return currentMayorUUID; }

    public Instant getTermEnd() { return termEnd; }

    public boolean isElectionRunning() { return electionRunning; }

    public List<UUID> getCandidates() { return Collections.unmodifiableList(candidates); }

    public boolean hasVoted(UUID playerUUID) { return hasVoted.contains(playerUUID); }

    public Map<UUID, Integer> getVotes() { return Collections.unmodifiableMap(votes); }

    public void loadData() {
        FileConfiguration data = plugin.getDataManager().getDataConfig();
        this.currentMayorName = data.getString("mayor.currentName", "Brak");
        String uuidStr = data.getString("mayor.currentUUID");
        this.currentMayorUUID = (uuidStr != null && !uuidStr.isEmpty()) ? UUID.fromString(uuidStr) : null;

        String termEndStr = data.getString("mayor.termEnd");
        if (termEndStr != null && !termEndStr.isEmpty()) {
            try {
                this.termEnd = Instant.parse(termEndStr);
            } catch (Exception e) {
                this.termEnd = Instant.now().plus(Duration.ofDays(plugin.getDataManager().getConfig().getLong("term-duration-days")));
            }
        } else {
            this.termEnd = Instant.now().plus(Duration.ofDays(plugin.getDataManager().getConfig().getLong("term-duration-days")));
        }
        this.electionRunning = data.getBoolean("election.running", false);
        this.candidates = data.getStringList("election.candidates").stream().map(UUID::fromString).collect(Collectors.toList());
        this.futureCandidates = data.getStringList("election.futureCandidates").stream().map(UUID::fromString).collect(Collectors.toList());
        this.hasVoted = data.getStringList("election.hasVoted").stream().map(UUID::fromString).collect(Collectors.toList());

        this.votes.clear();
        if (data.isConfigurationSection("election.votes")) {
            for (String key : data.getConfigurationSection("election.votes").getKeys(false)) {
                votes.put(UUID.fromString(key), data.getInt("election.votes." + key));
            }
        }

        this.bannedPlayers.clear();
        if (data.isConfigurationSection("banned-players")) {
            for (String key : data.getConfigurationSection("banned-players").getKeys(false)) {
                bannedPlayers.put(UUID.fromString(key), data.getInt("banned-players." + key));
            }
        }
    }

    public void saveData() {
        FileConfiguration data = plugin.getDataManager().getDataConfig();
        data.set("mayor.currentName", currentMayorName);
        data.set("mayor.currentUUID", currentMayorUUID != null ? currentMayorUUID.toString() : null);
        data.set("mayor.termEnd", termEnd != null ? termEnd.toString() : null);
        data.set("election.running", electionRunning);
        data.set("election.candidates", candidates.stream().map(UUID::toString).collect(Collectors.toList()));
        data.set("election.futureCandidates", futureCandidates.stream().map(UUID::toString).collect(Collectors.toList()));
        data.set("election.hasVoted", hasVoted.stream().map(UUID::toString).collect(Collectors.toList()));

        data.set("election.votes", null);
        for (Map.Entry<UUID, Integer> entry : votes.entrySet()) {
            data.set("election.votes." + entry.getKey().toString(), entry.getValue());
        }

        data.set("banned-players", null);
        for (Map.Entry<UUID, Integer> entry : bannedPlayers.entrySet()) {
            data.set("banned-players." + entry.getKey().toString(), entry.getValue());
        }

        plugin.getDataManager().saveData();
    }

    private String format(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}