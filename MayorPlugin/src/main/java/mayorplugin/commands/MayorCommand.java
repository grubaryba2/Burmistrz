package mayorplugin.commands;

import mayorplugin.MayorPlugin;
import mayorplugin.logic.ElectionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.UUID;

public class MayorCommand implements CommandExecutor {

    private final MayorPlugin plugin;

    public MayorCommand(MayorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mayor.admin")) {
            sender.sendMessage(formatMessage("no-permission", false));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "setsign":
                handleSetSign(sender);
                break;
            case "setholo":
                handleSetHolo(sender);
                break;
            case "removeholo":
                handleRemoveHolo(sender);
                break;
            case "reload":
                plugin.getDataManager().reloadConfig();
                sender.sendMessage(formatMessage("config-reloaded"));
                break;
            case "endvote":
                handleEndVote(sender);
                break;
            case "endterm":
                handleEndTerm(sender);
                break;
            case "ban":
                handleBan(sender, args);
                break;
            case "unban":
                handleUnban(sender, args);
                break;
            case "setduration":
                handleSetDuration(sender, args);
                break;
            case "set":
                handleSetMayor(sender, args);
                break;
            case "votes":
                handleVotes(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void handleSetSign(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(formatMessage("must-be-player"));
            return;
        }
        Player player = (Player) sender;
        Block targetBlock = player.getTargetBlock(null, 5);
        if (targetBlock.getState() instanceof Sign) {
            plugin.getDataManager().saveLocation("data.sign-location", targetBlock.getLocation());
            plugin.getElectionGUI().updateSign();
            player.sendMessage(formatMessage("sign-set"));
        } else {
            player.sendMessage(ChatColor.RED + "Musisz patrzeć na tabliczkę!");
        }
    }

    private void handleSetHolo(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(formatMessage("must-be-player"));
            return;
        }
        Player player = (Player) sender;
        plugin.getDataManager().saveLocation("data.hologram-location", player.getLocation());
        plugin.getHologramManager().createOrUpdateHologram();
        player.sendMessage(formatMessage("holo-set"));
    }

    private void handleRemoveHolo(CommandSender sender) {
        plugin.getHologramManager().removeHolograms();
        plugin.getDataManager().getConfig().set("data.hologram-location", null);
        plugin.saveConfig();
        sender.sendMessage(formatMessage("holo-removed"));
    }

    private void handleEndVote(CommandSender sender) {
        ElectionManager em = plugin.getElectionManager();
        if (!em.isElectionRunning()) {
            sender.sendMessage(formatMessage("election-not-running"));
            return;
        }
        em.endElection();
        sender.sendMessage(formatMessage("vote-ended"));
    }

    private void handleEndTerm(CommandSender sender) {
        plugin.getElectionManager().startElection();
        sender.sendMessage(formatMessage("term-ended"));
    }

    private void handleBan(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(formatMessage("usage-ban", false));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(formatMessage("player-not-found").replace("%player%", args[1]));
            return;
        }
        try {
            int duration = Integer.parseInt(args[2]);
            plugin.getElectionManager().banPlayer(target.getUniqueId(), duration);

            if (duration == 0) {
                sender.sendMessage(formatMessage("player-banned-permanent", false)
                        .replace("%player%", target.getName()));
            } else {
                sender.sendMessage(formatMessage("player-banned-temporary")
                        .replace("%player%", target.getName())
                        .replace("%duration%", String.valueOf(duration)));
            }

            if (target.getUniqueId().equals(plugin.getElectionManager().getCurrentMayorUUID())) {
                sender.sendMessage(ChatColor.RED + "Zbanowany gracz był obecnym burmistrzem. Jego kadencja zostaje przerwana.");
                plugin.getElectionManager().startElection();
            }

        } catch (NumberFormatException e) {
            sender.sendMessage(formatMessage("invalid-number"));
        }
    }

    private void handleUnban(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Poprawne użycie: /mayor unban <gracz>");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(formatMessage("player-not-found").replace("%player%", args[1]));
            return;
        }
        plugin.getElectionManager().unbanPlayer(target.getUniqueId());
        sender.sendMessage(formatMessage("player-unbanned").replace("%player%", target.getName()));
    }

    private void handleSetDuration(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(formatMessage("usage-duration", false));
            return;
        }
        String type = args[1].toLowerCase();
        try {
            int value = Integer.parseInt(args[2]);
            if (value <= 0) {
                sender.sendMessage(ChatColor.RED + "Wartość musi być większa od zera.");
                return;
            }
            if (type.equals("term")) {
                plugin.getDataManager().getConfig().set("term-duration-days", value);
                plugin.saveConfig();
                sender.sendMessage(formatMessage("duration-changed")
                        .replace("%type%", "kadencji")
                        .replace("%value%", value + " dni"));
            } else if (type.equals("election")) {
                plugin.getDataManager().getConfig().set("election-duration-hours", value);
                plugin.saveConfig();
                sender.sendMessage(formatMessage("duration-changed")
                        .replace("%type%", "wyborów")
                        .replace("%value%", value + " godzin"));
            } else {
                sender.sendMessage(formatMessage("usage-duration", false));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(formatMessage("invalid-number"));
        }
    }

    private void handleSetMayor(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(formatMessage("usage-setmayor", false));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(formatMessage("player-not-found").replace("%player%", args[1]));
            return;
        }
        plugin.getElectionManager().forceSetMayor(target);
        sender.sendMessage(formatMessage("new-mayor-set").replace("%player%", target.getName()));
    }

    private void handleVotes(CommandSender sender, String[] args) {
        if (!plugin.getElectionManager().isElectionRunning()) {
            sender.sendMessage(formatMessage("election-not-running"));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(formatMessage("usage-votes", false));
            return;
        }

        String action = args[1].toLowerCase();
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);

        UUID targetUUID = target.getUniqueId();
        if (!plugin.getElectionManager().getCandidates().contains(targetUUID)) {
            sender.sendMessage(formatMessage("player-not-candidate"));
            return;
        }

        try {
            int amount = Integer.parseInt(args[3]);
            ElectionManager em = plugin.getElectionManager();

            switch (action) {
                case "add":
                    em.addVotes(targetUUID, amount);
                    break;
                case "set":
                    em.setVotes(targetUUID, amount);
                    break;
                case "remove":
                    em.removeVotes(targetUUID, amount);
                    break;
                default:
                    sender.sendMessage(formatMessage("usage-votes", false));
                    return;
            }
            sender.sendMessage(formatMessage("votes-changed").replace("%player%", target.getName()));

        } catch (NumberFormatException e) {
            sender.sendMessage(formatMessage("invalid-number"));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- Komendy MayorPlugin ---");
        sender.sendMessage(ChatColor.YELLOW + "/mayor setsign");
        sender.sendMessage(ChatColor.YELLOW + "/mayor setholo");
        sender.sendMessage(ChatColor.YELLOW + "/mayor removeholo");
        sender.sendMessage(ChatColor.YELLOW + "/mayor set <gracz>");
        sender.sendMessage(ChatColor.YELLOW + "/mayor endvote");
        sender.sendMessage(ChatColor.YELLOW + "/mayor endterm");
        sender.sendMessage(ChatColor.YELLOW + "/mayor votes <add|set|remove> <gracz> <ilość>");
        sender.sendMessage(ChatColor.YELLOW + "/mayor ban <gracz> <kadencje>");
        sender.sendMessage(ChatColor.YELLOW + "/mayor unban <gracz>");
        sender.sendMessage(ChatColor.YELLOW + "/mayor setduration <term|election> <wartość>");
        sender.sendMessage(ChatColor.YELLOW + "/mayor reload");
    }

    private String formatMessage(String path) {
        return formatMessage(path, true);
    }

    private String formatMessage(String path, boolean usePrefix) {
        String message = plugin.getDataManager().getConfig().getString("messages." + path, "&cMissing message: " + path);
        String prefix = usePrefix ? plugin.getDataManager().getConfig().getString("messages.prefix", "") : "";
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }
}