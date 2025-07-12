package mayorplugin.logic;

import mayorplugin.MayorPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import java.util.List;

public class HologramManager {

    private final MayorPlugin plugin;
    private final NamespacedKey hologramKey;

    public HologramManager(MayorPlugin plugin) {
        this.plugin = plugin;
        this.hologramKey = new NamespacedKey(plugin, "mayor_hologram_line");
    }

    public void createOrUpdateHologram() {
        Location baseLocation = plugin.getDataManager().getConfig().getLocation("data.hologram-location");
        if (baseLocation == null || baseLocation.getWorld() == null) {
            return;
        }
        removeHolograms();
        List<String> lines = getHologramLines();
        Location currentLineLocation = baseLocation.clone();
        for (int i = lines.size() - 1; i >= 0; i--) {
            spawnHologramLine(currentLineLocation.clone(), lines.get(i));
            currentLineLocation.add(0, 0.25, 0);
        }
    }

    private void spawnHologramLine(Location location, String text) {
        if (location.getWorld() == null) return;
        ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        armorStand.setGravity(false);
        armorStand.setCanPickupItems(false);
        armorStand.setCustomName(parsePlaceholders(text));
        armorStand.setCustomNameVisible(true);
        armorStand.setVisible(false);
        armorStand.setMarker(true);
        armorStand.getPersistentDataContainer().set(hologramKey, PersistentDataType.BYTE, (byte) 1);
    }

    public void removeHolograms() {
        Location baseLocation = plugin.getDataManager().getConfig().getLocation("data.hologram-location");
        if (baseLocation != null && baseLocation.getWorld() != null) {
            for (Entity entity : baseLocation.getWorld().getNearbyEntities(baseLocation, 10, 20, 10)) {
                if (entity instanceof ArmorStand && entity.getPersistentDataContainer().has(hologramKey, PersistentDataType.BYTE)) {
                    entity.remove();
                }
            }
        }
    }

    private String parsePlaceholders(String text) {
        Player onlinePlayer = Bukkit.getOnlinePlayers().stream().findAny().orElse(null);
        String parsedText = ChatColor.translateAlternateColorCodes('&', text);
        if (onlinePlayer != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return PlaceholderAPI.setPlaceholders(onlinePlayer, parsedText);
        }
        return parsedText;
    }

    private List<String> getHologramLines() {
        ElectionManager em = plugin.getElectionManager();
        String configPath = em.isElectionRunning() ? "holograms.election-info" : "holograms.mayor-info";
        return plugin.getDataManager().getConfig().getStringList(configPath);
    }
}