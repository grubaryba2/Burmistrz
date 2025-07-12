package mayorplugin.listeners;

import mayorplugin.MayorPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final MayorPlugin plugin;

    public ChatListener(MayorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Sprawdź, czy funkcja tagu jest włączona w konfiguracji
        if (!plugin.getDataManager().getConfig().getBoolean("tag.enabled", true)) {
            return;
        }

        Player player = event.getPlayer();
        // Sprawdź, czy UUID gracza piszącego na czacie jest takie samo jak UUID burmistrza
        if (player.getUniqueId().equals(plugin.getElectionManager().getCurrentMayorUUID())) {
            String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getDataManager().getConfig().getString("tag.prefix", "&6[Burmistrz] &r"));
            String format = event.getFormat();
            event.setFormat(prefix + format);
        }
    }
}