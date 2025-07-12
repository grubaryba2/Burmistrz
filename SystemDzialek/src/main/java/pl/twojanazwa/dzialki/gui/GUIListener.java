package pl.twojanazwa.dzialki.gui;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.twojanazwa.dzialki.SystemDzialekPlugin;
import pl.twojanazwa.dzialki.handlers.RequestManager;
import pl.twojanazwa.dzialki.handlers.SelectionManager;

import java.util.ArrayList;
import java.util.List;

public class GUIListener implements Listener {

    private final SystemDzialekPlugin plugin;
    private final SelectionManager selectionManager;
    private final RequestManager requestManager;

    public GUIListener(SystemDzialekPlugin plugin) {
        this.plugin = plugin;
        this.selectionManager = plugin.getSelectionManager();
        this.requestManager = plugin.getRequestManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("confirmationMenu.title"));
        if (!event.getView().getTitle().equals(title)) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        Location selection = selectionManager.getPlayerSelection(player.getUniqueId());
        if(selection == null) {
            player.closeInventory();
            return;
        }

        if (clickedItem.getType() == Material.GREEN_WOOL) { // Kliknięto "TAK"
            player.closeInventory();

            // Logika tworzenia wniosku
            int size = plugin.getConfig().getInt("plotSize");
            requestManager.createRequest(player, selection, size);
            player.getInventory().setItemInMainHand(null); // Usuń narzędzie
            giveRequestPaper(player, selection, size);

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.requestCreated")));
            selectionManager.clearSelection(player);

        } else if (clickedItem.getType() == Material.RED_WOOL) { // Kliknięto "NIE"
            player.closeInventory();
            selectionManager.clearSelection(player);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.selectionCancelled")));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("confirmationMenu.title"));
        if (event.getView().getTitle().equals(title)) {
            // Jeśli gracz zamknął menu bez wyboru, czyścimy jego selekcję
            selectionManager.clearSelection((Player) event.getPlayer());
        }
    }

    private void giveRequestPaper(Player player, Location center, int size) {
        FileConfiguration config = plugin.getConfig();
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("requestPaper.name")));
        List<String> lore = new ArrayList<>();
        for (String line : config.getStringList("requestPaper.lore")) {
            line = line.replace("{PLAYER}", player.getName())
                    .replace("{WORLD}", center.getWorld().getName())
                    .replace("{X}", String.valueOf(center.getBlockX()))
                    .replace("{Y}", String.valueOf(center.getBlockY()))
                    .replace("{Z}", String.valueOf(center.getBlockZ()))
                    .replace("{SIZE}", String.valueOf(size));
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(lore);
        meta.addEnchant(Enchantment.LURE, 1, false);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        paper.setItemMeta(meta);

        player.getInventory().addItem(paper);
    }
}