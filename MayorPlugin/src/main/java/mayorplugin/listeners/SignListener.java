package mayorplugin.listeners;

import mayorplugin.MayorPlugin;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class SignListener implements Listener {

    private final MayorPlugin plugin;

    public SignListener(MayorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        // Sprawdź, czy lokalizacja tabliczki jest w ogóle ustawiona
        if (plugin.getDataManager().getConfig().getString("data.sign-location", "").isEmpty()) {
            return;
        }

        Location signLocation = plugin.getDataManager().getConfig().getLocation("data.sign-location");
        if (signLocation == null || signLocation.getWorld() == null) return;

        Location clickedLocation = clickedBlock.getLocation();

        // Niezawodne porównywanie lokalizacji
        if (clickedLocation.getWorld().equals(signLocation.getWorld()) &&
                clickedLocation.getBlockX() == signLocation.getBlockX() &&
                clickedLocation.getBlockY() == signLocation.getBlockY() &&
                clickedLocation.getBlockZ() == signLocation.getBlockZ()) {

            // Upewnij się, że to na pewno tabliczka
            if (clickedBlock.getState() instanceof Sign) {
                event.setCancelled(true); // Zapobiega otwarciu edytora tabliczki
                plugin.getElectionGUI().openGUI(event.getPlayer());
            }
        }
    }
}