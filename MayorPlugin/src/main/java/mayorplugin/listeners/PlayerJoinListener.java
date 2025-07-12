package mayorplugin.listeners;

import mayorplugin.MayorPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final MayorPlugin plugin;

    public PlayerJoinListener(MayorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Jeśli gracz, który dołącza, jest burmistrzem, nadaj mu tag w tabliście
        if (player.getUniqueId().equals(plugin.getElectionManager().getCurrentMayorUUID())) {
            plugin.getTagManager().applyTag(player);
        }
    }
}