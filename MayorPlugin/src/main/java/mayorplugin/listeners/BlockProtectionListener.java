package mayorplugin.listeners;

import mayorplugin.MayorPlugin;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockProtectionListener implements Listener {

    private final MayorPlugin plugin;

    public BlockProtectionListener(MayorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Sprawdź, czy lokalizacja tabliczki jest w ogóle ustawiona
        if (plugin.getDataManager().getConfig().getString("data.sign-location", "").isEmpty()) {
            return;
        }

        Location signLocation = plugin.getDataManager().getConfig().getLocation("data.sign-location");
        if (signLocation == null || signLocation.getWorld() == null) return;

        Location brokenBlockLocation = event.getBlock().getLocation();

        // Porównaj świat i koordynaty bloku
        if (brokenBlockLocation.getWorld().equals(signLocation.getWorld()) &&
                brokenBlockLocation.getBlockX() == signLocation.getBlockX() &&
                brokenBlockLocation.getBlockY() == signLocation.getBlockY() &&
                brokenBlockLocation.getBlockZ() == signLocation.getBlockZ()) {

            // Pozwól na zniszczenie tylko w trybie Creative
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                event.setCancelled(true);
                String message = plugin.getDataManager().getConfig().getString("messages.sign-protected", "&cNie możesz zniszczyć tabliczki wyborczej!");
                event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            } else {
                // Jeśli zniszczono w Creative, usuń lokalizację z configu, aby nie była już chroniona
                plugin.getDataManager().getConfig().set("data.sign-location", "");
                plugin.saveConfig();
                event.getPlayer().sendMessage(ChatColor.YELLOW + "Zniszczono i zresetowano lokalizację tabliczki wyborczej.");
            }
        }
    }
}