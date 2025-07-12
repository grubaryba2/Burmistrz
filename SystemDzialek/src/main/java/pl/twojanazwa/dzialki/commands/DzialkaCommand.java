package pl.twojanazwa.dzialki.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.twojanazwa.dzialki.SystemDzialekPlugin;

import java.util.ArrayList;
import java.util.List;

public class DzialkaCommand implements CommandExecutor {

    private final SystemDzialekPlugin plugin;

    public DzialkaCommand(SystemDzialekPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Tej komendy może użyć tylko gracz.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("dzialka.admin")) {
            player.sendMessage(ChatColor.RED + "Nie masz uprawnień do użycia tej komendy.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("narzedzie")) {
            FileConfiguration config = plugin.getConfig();
            ItemStack tool = new ItemStack(Material.GOLDEN_AXE);
            ItemMeta meta = tool.getItemMeta();

            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("claimTool.name")));
            List<String> lore = new ArrayList<>();
            for (String line : config.getStringList("claimTool.lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(lore);
            tool.setItemMeta(meta);

            player.getInventory().addItem(tool);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.toolReceived")));
            return true;
        }

        player.sendMessage(ChatColor.RED + "Użycie: /dzialka narzedzie");
        return true;
    }
}