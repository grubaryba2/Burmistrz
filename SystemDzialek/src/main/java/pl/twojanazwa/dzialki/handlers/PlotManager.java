package pl.twojanazwa.dzialki.handlers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.twojanazwa.dzialki.SystemDzialekPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;

public class PlotManager {

    private final SystemDzialekPlugin plugin;
    private final File plotsFile;
    private final FileConfiguration plotsConfig;
    private final Map<UUID, PlotRegion> activePlots = new HashMap<>();

    public PlotManager(SystemDzialekPlugin plugin) {
        this.plugin = plugin;
        this.plotsFile = new File(plugin.getDataFolder(), "plots.yml");
        if (!plotsFile.exists()) {
            try {
                plotsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.plotsConfig = YamlConfiguration.loadConfiguration(plotsFile);
        loadPlots();
    }

    public void createPlotFromRequest(List<String> lore) {
        String ownerName = ChatColor.stripColor(lore.get(0)).split(": ")[1];
        String worldName = ChatColor.stripColor(lore.get(2)).split(": ")[1];
        int x = Integer.parseInt(ChatColor.stripColor(lore.get(3)).split(": ")[1]);
        int y = Integer.parseInt(ChatColor.stripColor(lore.get(4)).split(": ")[1]);
        int z = Integer.parseInt(ChatColor.stripColor(lore.get(5)).split(": ")[1]);
        int size = Integer.parseInt(ChatColor.stripColor(lore.get(6)).split("x")[0]);

        Player owner = Bukkit.getPlayer(ownerName);
        if (owner == null) return;

        UUID plotId = UUID.randomUUID();
        String path = "plots." + plotId;

        plotsConfig.set(path + ".owner-uuid", owner.getUniqueId().toString());
        plotsConfig.set(path + ".owner-name", ownerName);
        plotsConfig.set(path + ".world", worldName);
        plotsConfig.set(path + ".x", x);
        plotsConfig.set(path + ".y", y);
        plotsConfig.set(path + ".z", z);
        plotsConfig.set(path + ".size", size);

        try {
            plotsConfig.save(plotsFile);
            loadPlots(); // Przeładuj działki, aby nowa była od razu aktywna
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPlots() {
        activePlots.clear();
        ConfigurationSection plotsSection = plotsConfig.getConfigurationSection("plots");
        if (plotsSection == null) return;

        for (String key : plotsSection.getKeys(false)) {
            UUID plotId = UUID.fromString(key);
            String path = "plots." + key;
            UUID ownerUuid = UUID.fromString(plotsConfig.getString(path + ".owner-uuid"));
            String ownerName = plotsConfig.getString(path + ".owner-name");
            World world = Bukkit.getWorld(plotsConfig.getString(path + ".world"));
            int x = plotsConfig.getInt(path + ".x");
            int y = plotsConfig.getInt(path + ".y");
            int z = plotsConfig.getInt(path + ".z");
            int size = plotsConfig.getInt(path + ".size");

            if (world != null) {
                Location center = new Location(world, x, y, z);
                activePlots.put(plotId, new PlotRegion(ownerUuid, ownerName, center, size));
            }
        }
    }

    public PlotRegion getPlotAt(Location location) {
        for (PlotRegion plot : activePlots.values()) {
            if (plot.contains(location)) {
                return plot;
            }
        }
        return null;
    }

    // Wewnętrzna klasa do przechowywania danych o działce
    public static class PlotRegion {
        private final UUID ownerUuid;
        private final String ownerName;
        private final Location corner1;
        private final Location corner2;

        public PlotRegion(UUID ownerUuid, String ownerName, Location center, int size) {
            this.ownerUuid = ownerUuid;
            this.ownerName = ownerName;
            int halfSize = size / 2;
            this.corner1 = center.clone().add(-halfSize, -halfSize, -halfSize);
            this.corner2 = center.clone().add(halfSize, halfSize, halfSize);
        }

        public boolean contains(Location loc) {
            if (!loc.getWorld().equals(corner1.getWorld())) {
                return false;
            }
            return loc.getX() >= corner1.getX() && loc.getX() <= corner2.getX() &&
                    loc.getY() >= corner1.getY() && loc.getY() <= corner2.getY() &&
                    loc.getZ() >= corner1.getZ() && loc.getZ() <= corner2.getZ();
        }

        public String getOwnerName() {
            return ownerName;
        }
    }
}