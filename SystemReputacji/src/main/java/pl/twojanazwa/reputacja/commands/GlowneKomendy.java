package pl.twojanazwa.reputacja.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.twojanazwa.reputacja.SystemReputacjiPlugin;
import pl.twojanazwa.reputacja.handlers.ArrestManager;
import pl.twojanazwa.reputacja.handlers.ReputationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GlowneKomendy implements CommandExecutor {

    private final SystemReputacjiPlugin plugin;
    private final ReputationManager reputationManager;
    private final ArrestManager arrestManager;

    public GlowneKomendy(SystemReputacjiPlugin plugin) {
        this.plugin = plugin;
        this.reputationManager = plugin.getReputationManager();
        this.arrestManager = plugin.getArrestManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Użycie: /" + cmdName + " [gracz]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Nie znaleziono gracza o podanym nicku.");
            return true;
        }

        switch (cmdName) {
            case "ustawzly":
                reputationManager.setBad(target);
                sender.sendMessage(ChatColor.GREEN + "Nadano złą reputację graczowi " + target.getName());
                target.sendMessage(ChatColor.RED + "Twoja reputacja została zrujnowana przez administratora.");
                return true;

            case "uniewinnij":
                reputationManager.setGood(target.getUniqueId());
                sender.sendMessage(ChatColor.GREEN + "Uniewinniono gracza " + target.getName());
                target.sendMessage(ChatColor.GREEN + "Twoja reputacja została oczyszczona przez administratora.");
                return true;

            case "wiezienie":
            case "skuj":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Tej komendy może użyć tylko gracz.");
                    return true;
                }
                arrestManager.processAdminArrest(target, (Player) sender);
                return true;

            case "reputacja":
                FileConfiguration configRep = plugin.getConfig();
                if (reputationManager.isBad(target.getUniqueId())) {
                    String msg = configRep.getString("wiadomosci.reputacja-sprawdz-zla").replace("{GRACZ}", target.getName());
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                } else {
                    String msg = configRep.getString("wiadomosci.reputacja-sprawdz-dobra").replace("{GRACZ}", target.getName());
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                }
                return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Tej komendy moze uzyc tylko gracz.");
            return true;
        }

        Player player = (Player) sender;
        FileConfiguration config = plugin.getConfig();

        if (cmdName.equals("ustawwiezienie")) {
            Location loc = player.getLocation();
            config.set("wiezienie.lokalizacja", loc);
            plugin.saveConfig();
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("wiadomosci.ustawiono-wiezienie")));
            return true;
        }

        if (cmdName.equals("dajkajdanki") || cmdName.equals("dajparalizator")) {
            Player itemTarget = getTargetPlayer(player, args);
            if (itemTarget == null) return true;

            if(cmdName.equals("dajkajdanki")) {
                ItemStack kajdanki = new ItemStack(Material.IRON_BARS);
                ItemMeta meta = kajdanki.getItemMeta();
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("kajdanki.nazwa-przedmiotu")));

                List<String> lore = new ArrayList<>();
                for (String line : config.getStringList("kajdanki.lore")) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                lore.add(ChatColor.DARK_GRAY + "ID: " + UUID.randomUUID().toString().substring(0, 8));
                meta.setLore(lore);

                kajdanki.setItemMeta(meta);
                itemTarget.getInventory().addItem(kajdanki);
            } else {
                ItemStack paralizator = new ItemStack(Material.BLAZE_ROD);
                ItemMeta meta = paralizator.getItemMeta();
                int iloscUzyc = config.getInt("paralizator.ilosc-uzyc");
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("paralizator.nazwa-przedmiotu")));

                List<String> lore = new ArrayList<>();
                for (String line : config.getStringList("paralizator.lore")) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                lore.add(ChatColor.GRAY + "Użycia: " + iloscUzyc + "/" + iloscUzyc);
                lore.add(ChatColor.DARK_GRAY + "ID: " + UUID.randomUUID().toString().substring(0, 8));
                meta.setLore(lore);

                paralizator.setItemMeta(meta);
                itemTarget.getInventory().addItem(paralizator);
            }
            return true;
        }
        return false;
    }

    private Player getTargetPlayer(Player sender, String[] args) {
        if (args.length > 0) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Nie znaleziono gracza o podanym nicku.");
                return null;
            }
            return target;
        }
        return sender;
    }
}