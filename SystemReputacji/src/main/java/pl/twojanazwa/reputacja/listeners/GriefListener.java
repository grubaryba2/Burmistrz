package pl.twojanazwa.reputacja.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import pl.twojanazwa.reputacja.SystemReputacjiPlugin;
import pl.twojanazwa.reputacja.handlers.GriefManager;
import pl.twojanazwa.reputacja.handlers.ReputationManager;

public class GriefListener implements Listener {

    private final GriefManager griefManager;
    private final ReputationManager reputationManager;

    public GriefListener(SystemReputacjiPlugin plugin) {
        this.griefManager = plugin.getGriefManager();
        this.reputationManager = plugin.getReputationManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (reputationManager.isBad(player.getUniqueId())) return;

        // Jeśli gracz niszczy blok, który sam postawił (i naprawia grief typu PLACE)
        if (griefManager.resolveGrief(player, event.getBlock(), GriefManager.GriefType.BREAK)) {
            return;
        }
        // W przeciwnym wypadku, to nowy grief typu BREAK
        griefManager.registerGrief(player, event.getBlock(), GriefManager.GriefType.BREAK);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (reputationManager.isBad(player.getUniqueId())) return;

        // Jeśli gracz stawia ten sam blok, który zniszczył (i naprawia grief typu BREAK)
        if (griefManager.resolveGrief(player, event.getBlock(), GriefManager.GriefType.PLACE)) {
            return;
        }
        // W przeciwnym wypadku, to nowy grief typu PLACE
        griefManager.registerGrief(player, event.getBlock(), GriefManager.GriefType.PLACE);
    }
}