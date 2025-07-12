package pl.twojanazwa.reputacja.handlers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pl.twojanazwa.reputacja.SystemReputacjiPlugin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator; // POPRAWKA: Dodano brakujący import
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GriefManager {

    private final SystemReputacjiPlugin plugin;
    private final ReputationManager reputationManager;
    private final Map<UUID, List<GriefInfo>> playerGriefs = new HashMap<>();
    private final Map<UUID, BossBar> activeBossBars = new HashMap<>();
    private final long repairTime;
    private final String bossBarTitle;
    private final BarColor bossBarColor;
    private final BarStyle bossBarStyle;

    public GriefManager(SystemReputacjiPlugin plugin) {
        this.plugin = plugin;
        this.reputationManager = plugin.getReputationManager();
        this.repairTime = plugin.getConfig().getLong("reputacja.czas-na-naprawe-griefu", 15) * 1000;

        this.bossBarTitle = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("bossbar.tytul"));
        this.bossBarColor = BarColor.valueOf(plugin.getConfig().getString("bossbar.kolor", "RED").toUpperCase());
        this.bossBarStyle = BarStyle.valueOf(plugin.getConfig().getString("bossbar.styl", "SOLID").toUpperCase());

        startGriefChecker();
    }

    public void registerGrief(Player player, Block block, GriefType type) {
        playerGriefs.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>())
                .add(new GriefInfo(player, block.getLocation(), block.getBlockData(), type));
        showBossBar(player);
    }

    public boolean resolveGrief(Player player, Block block, GriefType resolutionType) {
        List<GriefInfo> griefs = playerGriefs.get(player.getUniqueId());
        if (griefs == null || griefs.isEmpty()) {
            return false;
        }

        Iterator<GriefInfo> iterator = griefs.iterator();
        while (iterator.hasNext()) {
            GriefInfo info = iterator.next();
            if (info.getLocation().equals(block.getLocation())) {
                if (resolutionType == GriefType.BREAK && info.getType() == GriefType.PLACE) {
                    iterator.remove();
                    checkAndRemoveBossBar(player);
                    return true;
                }
                if (resolutionType == GriefType.PLACE && info.getType() == GriefType.BREAK &&
                        block.getBlockData().getMaterial() == info.getOriginalBlockData().getMaterial()) {
                    iterator.remove();
                    checkAndRemoveBossBar(player);
                    return true;
                }
            }
        }
        return false;
    }

    private void showBossBar(Player player) {
        if (!activeBossBars.containsKey(player.getUniqueId())) {
            BossBar bossBar = Bukkit.createBossBar(bossBarTitle, bossBarColor, bossBarStyle);
            bossBar.setProgress(1.0);
            bossBar.addPlayer(player);
            activeBossBars.put(player.getUniqueId(), bossBar);
        }
    }

    private void checkAndRemoveBossBar(Player player) {
        List<GriefInfo> griefs = playerGriefs.get(player.getUniqueId());
        if (griefs == null || griefs.isEmpty()) {
            BossBar bossBar = activeBossBars.remove(player.getUniqueId());
            if (bossBar != null) {
                bossBar.removeAll();
            }
        }
    }

    private void startGriefChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (playerGriefs.isEmpty()) {
                    if(!activeBossBars.isEmpty()){
                        activeBossBars.values().forEach(BossBar::removeAll);
                        activeBossBars.clear();
                    }
                    return;
                }

                long currentTime = System.currentTimeMillis();

                playerGriefs.forEach((uuid, griefs) -> {
                    griefs.removeIf(info -> {
                        if (currentTime - info.getTimestamp() > repairTime) {
                            Player griefer = info.getGriefer();
                            if (griefer != null && griefer.isOnline()) {
                                reputationManager.setBadFromGrief(griefer);
                            }
                            return true;
                        }
                        return false;
                    });
                });

                activeBossBars.entrySet().removeIf(entry -> {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    List<GriefInfo> griefs = playerGriefs.get(entry.getKey());

                    if (player == null || !player.isOnline() || griefs == null || griefs.isEmpty()) {
                        entry.getValue().removeAll();
                        return true;
                    }

                    long oldestGriefTime = griefs.stream().mapToLong(GriefInfo::getTimestamp).min().orElse(currentTime);
                    long timePassed = currentTime - oldestGriefTime;
                    double progress = Math.max(0, 1.0 - ((double) timePassed / repairTime));
                    long secondsLeft = Math.max(0, (repairTime - timePassed) / 1000);

                    entry.getValue().setProgress(progress);
                    entry.getValue().setTitle(bossBarTitle.replace("{CZAS}", String.valueOf(secondsLeft + 1)));

                    return false;
                });
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 20L);
    }

    public enum GriefType { BREAK, PLACE }

    private static class GriefInfo {
        private final Player griefer;
        private final Location location;
        private final BlockData originalBlockData;
        private final GriefType type;
        private final long timestamp;

        public GriefInfo(Player griefer, Location location, BlockData originalBlockData, GriefType type) {
            this.griefer = griefer;
            this.location = location;
            this.originalBlockData = originalBlockData;
            this.type = type;
            this.timestamp = System.currentTimeMillis();
        }
        public Player getGriefer() { return griefer; }
        public Location getLocation() { return location; }
        public BlockData getOriginalBlockData() { return originalBlockData; }
        public GriefType getType() { return type; }
        public long getTimestamp() { return timestamp; }
    }
}