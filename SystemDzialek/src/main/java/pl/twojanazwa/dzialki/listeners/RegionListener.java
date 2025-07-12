package pl.twojanazwa.dzialki.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import pl.twojanazwa.dzialki.SystemDzialekPlugin;
import pl.twojanazwa.dzialki.handlers.PlotManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegionListener implements Listener {

    private final SystemDzialekPlugin plugin;
    private final PlotManager plotManager;
    private final Map<UUID, String> playerLastPlot = new HashMap<>();

    public RegionListener(SystemDzialekPlugin plugin) {
        this.plugin = plugin;
        this.plotManager = plugin.getPlotManager();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        Location to = event.getTo();

        PlotManager.PlotRegion currentPlot = plotManager.getPlotAt(to);
        String lastPlotOwner = playerLastPlot.get(player.getUniqueId());

        if (currentPlot != null) {
            String currentOwner = currentPlot.getOwnerName();
            if (!currentOwner.equals(lastPlotOwner)) {
                String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.plotEntryTitle"));
                String subtitle = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.plotEntrySubtitle").replace("{PLAYER}", currentOwner));
                player.sendTitle(title, subtitle, 10, 40, 10);
                playerLastPlot.put(player.getUniqueId(), currentOwner);
            }
        } else {
            if (lastPlotOwner != null) {
                playerLastPlot.remove(player.getUniqueId());
            }
        }
    }
}