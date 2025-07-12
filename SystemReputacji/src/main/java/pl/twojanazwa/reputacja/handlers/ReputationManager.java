package pl.twojanazwa.reputacja.handlers;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.twojanazwa.reputacja.SystemReputacjiPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ReputationManager {

    private final SystemReputacjiPlugin plugin;
    private final Set<UUID> badReputationPlayers;
    private File reputationFile;
    private FileConfiguration reputationConfig;

    public ReputationManager(SystemReputacjiPlugin plugin) {
        this.plugin = plugin;
        this.badReputationPlayers = new HashSet<>();
    }

    public void loadReputation() throws IOException {
        if (reputationFile == null) {
            reputationFile = new File(plugin.getDataFolder(), "reputation.yml");
        }
        if (!reputationFile.exists()) {
            plugin.saveResource("reputation.yml", false);
        }
        reputationConfig = YamlConfiguration.loadConfiguration(reputationFile);
        List<String> badUuids = reputationConfig.getStringList("bad-players");
        for (String uuidStr : badUuids) {
            badReputationPlayers.add(UUID.fromString(uuidStr));
        }
    }

    public void saveReputation() throws IOException {
        if (reputationFile == null || reputationConfig == null) {
            return;
        }
        List<String> badUuids = badReputationPlayers.stream().map(UUID::toString).collect(Collectors.toList());
        reputationConfig.set("bad-players", badUuids);
        reputationConfig.save(reputationFile);
    }

    public boolean isBad(UUID playerUuid) {
        return badReputationPlayers.contains(playerUuid);
    }

    public void setBad(Player player) {
        if (!isBad(player.getUniqueId())) {
            badReputationPlayers.add(player.getUniqueId());
            String msg = plugin.getConfig().getString("wiadomosci.reputacja-zepsuta-atak");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        }
    }

    public void setBadFromGrief(Player player) {
        if (!isBad(player.getUniqueId())) {
            badReputationPlayers.add(player.getUniqueId());
            String msg = plugin.getConfig().getString("wiadomosci.reputacja-zepsuta-grief");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        }
    }

    public void setGood(UUID playerUuid) {
        badReputationPlayers.remove(playerUuid);
    }
}