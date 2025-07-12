package pl.twojanazwa.dzialki.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.twojanazwa.dzialki.SystemDzialekPlugin;
import pl.twojanazwa.dzialki.gui.ConfirmationGUI;
import pl.twojanazwa.dzialki.handlers.PlotManager;
import pl.twojanazwa.dzialki.handlers.SelectionManager;

import java.util.List;

public class ToolListener implements Listener {

    private final SystemDzialekPlugin plugin;
    private final SelectionManager selectionManager;
    private final PlotManager plotManager;
    private final String toolName;
    private final String requestName;

    public ToolListener(SystemDzialekPlugin plugin) {
        this.plugin = plugin;
        this.selectionManager = plugin.getSelectionManager();
        this.plotManager = plugin.getPlotManager();
        this.toolName = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("claimTool.name"));
        this.requestName = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("requestPaper.name"));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR || !itemInHand.hasItemMeta()) return;

        String displayName = itemInHand.getItemMeta().getDisplayName();

        if (displayName.equals(toolName)) {
            handleToolInteraction(event, player);
        } else if (displayName.equals(requestName) && itemInHand.getType() == Material.PAPER) {
            handleRequestInteraction(event, player, itemInHand);
        }
    }

    private void handleToolInteraction(PlayerInteractEvent event, Player player) {
        event.setCancelled(true);
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            selectionManager.makeSelection(player, event.getClickedBlock().getLocation());
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.selectionMade")));
        } else if (event.getAction() == Action.RIGHT_CLICK_AIR) {
            if (selectionManager.hasSelection(player)) {
                new ConfirmationGUI(plugin).open(player);
            }
        }
    }

    private void handleRequestInteraction(PlayerInteractEvent event, Player player, ItemStack requestPaper) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        event.setCancelled(true);

        if (!player.hasPermission("dzialka.mayor")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.notMayor")));
            return;
        }

        ItemMeta meta = requestPaper.getItemMeta();
        if (meta == null || !meta.hasLore()) return;

        // POPRAWIONA LOGIKA: po prostu wywołujemy metodę, nie sprawdzamy jej wyniku w 'if'
        List<String> lore = meta.getLore();
        plotManager.createPlotFromRequest(lore);

        String ownerName = ChatColor.stripColor(lore.get(0)).split(": ")[1];
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.plotApproved").replace("{PLAYER}", ownerName)));
        requestPaper.setAmount(requestPaper.getAmount() - 1);
    }
}