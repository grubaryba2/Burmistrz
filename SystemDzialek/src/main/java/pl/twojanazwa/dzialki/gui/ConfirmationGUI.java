package pl.twojanazwa.dzialki.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.twojanazwa.dzialki.SystemDzialekPlugin;

import java.util.ArrayList;
import java.util.List;

public class ConfirmationGUI {

    private final SystemDzialekPlugin plugin;

    public ConfirmationGUI(SystemDzialekPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        FileConfiguration config = plugin.getConfig();
        String title = ChatColor.translateAlternateColorCodes('&', config.getString("confirmationMenu.title"));
        Inventory gui = Bukkit.createInventory(null, 9, title);

        // Przycisk "TAK"
        ItemStack yesButton = new ItemStack(Material.GREEN_WOOL);
        ItemMeta yesMeta = yesButton.getItemMeta();
        yesMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("confirmationMenu.yesItem.name")));
        List<String> yesLore = new ArrayList<>();
        for (String line : config.getStringList("confirmationMenu.yesItem.lore")) {
            yesLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        yesMeta.setLore(yesLore);
        yesButton.setItemMeta(yesMeta);

        // Przycisk "NIE"
        ItemStack noButton = new ItemStack(Material.RED_WOOL);
        ItemMeta noMeta = noButton.getItemMeta();
        noMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("confirmationMenu.noItem.name")));
        List<String> noLore = new ArrayList<>();
        for (String line : config.getStringList("confirmationMenu.noItem.lore")) {
            noLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        noMeta.setLore(noLore);
        noButton.setItemMeta(noMeta);

        gui.setItem(2, yesButton); // Slot 3
        gui.setItem(6, noButton);  // Slot 7

        player.openInventory(gui);
    }
}