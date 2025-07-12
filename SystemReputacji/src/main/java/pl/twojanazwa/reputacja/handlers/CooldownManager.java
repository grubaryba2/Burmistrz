package pl.twojanazwa.reputacja.handlers;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import pl.twojanazwa.reputacja.SystemReputacjiPlugin;

import java.util.Map;
import java.util.UUID;

public class CooldownManager extends BukkitRunnable {

    private final SystemReputacjiPlugin plugin;
    private final String kajdankiName;
    private final String paralizatorName;
    private final String strippedKajdankiName;
    private final String strippedParalizatorName;

    public CooldownManager(SystemReputacjiPlugin plugin) {
        this.plugin = plugin;
        this.kajdankiName = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("kajdanki.nazwa-przedmiotu"));
        this.paralizatorName = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("paralizator.nazwa-przedmiotu"));
        this.strippedKajdankiName = ChatColor.stripColor(this.kajdankiName);
        this.strippedParalizatorName = ChatColor.stripColor(this.paralizatorName);
    }

    @Override
    public void run() {
        long currentTime = System.currentTimeMillis();

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null || !item.hasItemMeta()) {
                    continue;
                }

                String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());

                if (item.getType() == Material.IRON_BARS && displayName.startsWith(strippedKajdankiName)) {
                    updateItemCooldown(player, item, plugin.getCooldownsKajdanki(), "kajdanki", currentTime);
                }

                if (item.getType() == Material.BLAZE_ROD && displayName.startsWith(strippedParalizatorName)) {
                    updateItemCooldown(player, item, plugin.getCooldownsParalizator(), "paralizator", currentTime);
                }
            }
        }
    }

    private void updateItemCooldown(Player player, ItemStack item, Map<UUID, Long> cooldownMap, String itemType, long currentTime) {
        ItemMeta meta = item.getItemMeta();
        String defaultName, cooldownNameFormat;

        if (itemType.equals("kajdanki")) {
            defaultName = this.kajdankiName;
            cooldownNameFormat = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("kajdanki.nazwa-podczas-cooldownu"));
        } else {
            defaultName = this.paralizatorName;
            cooldownNameFormat = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("paralizator.nazwa-podczas-cooldownu"));
        }

        if (cooldownMap.containsKey(player.getUniqueId())) {
            long timeLeft = (cooldownMap.get(player.getUniqueId()) - currentTime) / 1000;
            if (timeLeft > 0) {
                String newName = cooldownNameFormat.replace("{CZAS}", String.valueOf(timeLeft + 1));
                if (!meta.getDisplayName().equals(newName)) {
                    meta.setDisplayName(newName);
                    item.setItemMeta(meta);
                }
            } else {
                cooldownMap.remove(player.getUniqueId());
                if (!meta.getDisplayName().equals(defaultName)) {
                    meta.setDisplayName(defaultName);
                    item.setItemMeta(meta);
                }
            }
        } else {
            if (!meta.getDisplayName().equals(defaultName)) {
                meta.setDisplayName(defaultName);
                item.setItemMeta(meta);
            }
        }
    }
}