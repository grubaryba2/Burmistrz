package mayorplugin.gui;

import mayorplugin.MayorPlugin;
import mayorplugin.logic.ElectionManager;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ElectionGUI implements Listener {

    private final MayorPlugin plugin;
    private final int ITEMS_PER_PAGE = 45;

    public ElectionGUI(MayorPlugin plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player) {
        ElectionManager em = plugin.getElectionManager();
        if (em.isElectionRunning()) {
            openElectionView(player, 0);
        } else {
            openMayorInfoView(player);
        }
    }

    private void openMayorInfoView(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_BLUE + "Informacje o Burmistrzu");
        ElectionManager em = plugin.getElectionManager();

        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", ""));
        }

        ItemStack mayorHead;
        if (em.getCurrentMayorUUID() != null) {
            OfflinePlayer mayor = Bukkit.getOfflinePlayer(em.getCurrentMayorUUID());
            mayorHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) mayorHead.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(mayor);
                meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Burmistrz: " + ChatColor.YELLOW + mayor.getName());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Kadencja kończy się za:");
                lore.add(ChatColor.WHITE + this.formatDuration(em.getTermEnd()));
                meta.setLore(lore);
                mayorHead.setItemMeta(meta);
            }
        } else {
            mayorHead = createGuiItem(Material.BARRIER, ChatColor.RED + "Brak Burmistrza", ChatColor.GRAY + "Wybory rozpoczną się wkrótce.");
        }
        gui.setItem(13, mayorHead);

        double cost = plugin.getDataManager().getConfig().getDouble("candidacy-cost");
        gui.setItem(22, createGuiItem(Material.EMERALD, ChatColor.GREEN + "Kandyduj w następnych wyborach", ChatColor.GRAY + "Koszt: " + cost + "$"));

        player.openInventory(gui);
    }

    private void openElectionView(Player player, int page) {
        ElectionManager em = plugin.getElectionManager();
        List<UUID> candidates = em.getCandidates();
        int maxPages = (int) Math.ceil((double) candidates.size() / ITEMS_PER_PAGE);
        if (maxPages == 0) maxPages = 1;

        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.DARK_BLUE + "Wybory - Strona " + (page + 1) + "/" + maxPages);

        int startIndex = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int candidateIndex = startIndex + i;
            if (candidateIndex < candidates.size()) {
                OfflinePlayer candidate = Bukkit.getOfflinePlayer(candidates.get(candidateIndex));
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(candidate);
                    meta.setDisplayName(ChatColor.GREEN + candidate.getName());
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "Głosy: " + ChatColor.YELLOW + em.getVotes().getOrDefault(candidate.getUniqueId(), 0));
                    lore.add("");
                    lore.add(ChatColor.AQUA + "Kliknij, aby zagłosować!");
                    meta.setLore(lore);
                    head.setItemMeta(meta);
                }
                gui.setItem(i, head);
            }
        }

        for (int i = 45; i < 54; i++) {
            gui.setItem(i, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", ""));
        }

        if (page > 0) {
            gui.setItem(45, createGuiItem(Material.ARROW, ChatColor.YELLOW + "Poprzednia strona", ""));
        }
        if (page < maxPages - 1) {
            gui.setItem(53, createGuiItem(Material.ARROW, ChatColor.YELLOW + "Następna strona", ""));
        }

        double cost = plugin.getDataManager().getConfig().getDouble("candidacy-cost");
        gui.setItem(49, createGuiItem(Material.EMERALD, ChatColor.GREEN + "Kandyduj!", ChatColor.GRAY + "Koszt: " + cost + "$"));

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String rawTitle = event.getView().getTitle();
        String strippedTitle = ChatColor.stripColor(rawTitle);

        boolean isElectionGUI = strippedTitle.startsWith("Wybory - Strona ");
        boolean isInfoGUI = strippedTitle.equals("Informacje o Burmistrzu");

        if (!isElectionGUI && !isInfoGUI) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        if (isElectionGUI) {
            handleElectionGUIClick(player, clickedItem, strippedTitle);
        } else {
            handleInfoGUIClick(player, clickedItem);
        }
    }

    private void handleInfoGUIClick(Player player, ItemStack clickedItem) {
        if (clickedItem.getType() == Material.EMERALD) {
            handleCandidacy(player);
        }
    }

    private void handleElectionGUIClick(Player player, ItemStack clickedItem, String plainTitle) {
        String[] titleParts = plainTitle.split(" ");
        if (titleParts.length < 4) return;
        String pagePart = titleParts[3];
        int currentPage = Integer.parseInt(pagePart.split("/")[0]) - 1;

        if (clickedItem.getType() == Material.PLAYER_HEAD) {
            handleVote(player, clickedItem);
            return;
        }

        switch (clickedItem.getType()) {
            case ARROW:
                if (clickedItem.getItemMeta().getDisplayName().contains("Poprzednia")) {
                    openElectionView(player, currentPage - 1);
                } else if (clickedItem.getItemMeta().getDisplayName().contains("Następna")) {
                    openElectionView(player, currentPage + 1);
                }
                break;
            case EMERALD:
                handleCandidacy(player);
                break;
            default:
                break;
        }
    }

    private void handleVote(Player player, ItemStack clickedItem) {
        ElectionManager em = plugin.getElectionManager();
        if (!em.isElectionRunning()) {
            player.sendMessage(formatMessage("election-not-running"));
            player.closeInventory();
            return;
        }
        if (em.hasVoted(player.getUniqueId())) {
            player.sendMessage(formatMessage("already-voted"));
            return;
        }
        SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
        if (meta != null && meta.getOwningPlayer() != null) {
            OfflinePlayer candidate = meta.getOwningPlayer();
            if (candidate.getUniqueId().equals(player.getUniqueId())) {
                player.sendMessage(formatMessage("cannot-vote-for-self"));
                return;
            }
            em.addVote(candidate.getUniqueId(), player.getUniqueId());
            player.sendMessage(formatMessage("vote-success").replace("%player%", candidate.getName()));
            player.closeInventory();
        }
    }

    private void handleCandidacy(Player player) {
        ElectionManager em = plugin.getElectionManager();

        if (em.isCandidate(player.getUniqueId()) || em.isFutureCandidate(player.getUniqueId())) {
            player.sendMessage(formatMessage("already-candidate"));
            return;
        }

        int banDuration = em.getBanDuration(player.getUniqueId());
        if (banDuration != -1) {
            String durationStr = (banDuration == 0) ? "na zawsze" : String.valueOf(banDuration);
            player.sendMessage(formatMessage("player-is-banned").replace("%duration%", durationStr));
            return;
        }

        Economy econ = MayorPlugin.getEconomy();
        double cost = plugin.getDataManager().getConfig().getDouble("candidacy-cost");

        if (!econ.has(player, cost)) {
            player.sendMessage(formatMessage("not-enough-money").replace("%cost%", String.valueOf(cost)));
            return;
        }

        EconomyResponse r = econ.withdrawPlayer(player, cost);
        if (r.transactionSuccess()) {
            if (em.isElectionRunning()) {
                em.addCandidate(player.getUniqueId());
                player.sendMessage(formatMessage("candidacy-success"));
                openElectionView(player, 0);
            } else {
                em.addFutureCandidate(player.getUniqueId());
                player.sendMessage(formatMessage("candidacy-future-success"));
                player.closeInventory();
            }
        } else {
            player.sendMessage(ChatColor.RED + "Wystąpił nieoczekiwany błąd podczas transakcji: " + r.errorMessage);
        }
    }

    public void updateSign() {
        if (plugin.getDataManager().getConfig().getString("data.sign-location", "").isEmpty()) return;

        try {
            Location loc = plugin.getDataManager().getConfig().getLocation("data.sign-location");
            if (loc == null || loc.getWorld() == null || !(loc.getBlock().getState() instanceof Sign)) return;

            Sign sign = (Sign) loc.getBlock().getState();
            for (int i = 0; i < 4; i++) {
                String line = plugin.getDataManager().getConfig().getString("sign.line" + (i + 1));
                sign.setLine(i, ChatColor.translateAlternateColorCodes('&', line));
            }
            sign.update();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update sign, it might be in an unloaded chunk.");
        }
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatMessage(String path) {
        String message = plugin.getDataManager().getConfig().getString("messages." + path, "&cMissing message: " + path);
        String prefix = plugin.getDataManager().getConfig().getString("messages.prefix", "");
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }

    private String formatDuration(Instant endTime) {
        if (endTime == null) return "Nigdy";
        Duration duration = Duration.between(Instant.now(), endTime);
        if (duration.isNegative() || duration.isZero()) return "Zakończono";

        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("g ");
        if (minutes > 0) sb.append(minutes).append("m ");

        if (sb.length() == 0 && seconds > 0) {
            sb.append(seconds).append("s");
        } else if (sb.length() == 0) {
            return "mniej niż minutę";
        }

        return sb.toString().trim();
    }
}