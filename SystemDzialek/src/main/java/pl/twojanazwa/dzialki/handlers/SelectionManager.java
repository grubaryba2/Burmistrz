package pl.twojanazwa.dzialki.handlers;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import pl.twojanazwa.dzialki.SystemDzialekPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionManager {

    private final SystemDzialekPlugin plugin;
    private final Map<UUID, Location> playerSelections = new HashMap<>();
    private final Map<UUID, BukkitTask> particleTasks = new HashMap<>();
    private final String toolName;

    public SelectionManager(SystemDzialekPlugin plugin) {
        this.plugin = plugin;
        this.toolName = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("claimTool.name"));
    }

    public void makeSelection(Player player, Location center) {
        clearSelection(player);
        playerSelections.put(player.getUniqueId(), center);
        startParticleTask(player, center);
    }

    public void clearSelection(Player player) {
        playerSelections.remove(player.getUniqueId());
        BukkitTask task = particleTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    public Location getPlayerSelection(UUID playerUuid) {
        return playerSelections.get(playerUuid);
    }

    public boolean hasSelection(Player player) {
        return playerSelections.containsKey(player.getUniqueId());
    }

    private void startParticleTask(Player player, Location center) {
        int halfSize = plugin.getConfig().getInt("plotSize") / 2;

        Location p1 = center.clone().add(-halfSize, -halfSize, -halfSize);
        Location p2 = center.clone().add(halfSize, -halfSize, -halfSize);
        Location p3 = center.clone().add(halfSize, -halfSize, halfSize);
        Location p4 = center.clone().add(-halfSize, -halfSize, halfSize);
        Location p5 = p1.clone().add(0, halfSize * 2, 0);
        Location p6 = p2.clone().add(0, halfSize * 2, 0);
        Location p7 = p3.clone().add(0, halfSize * 2, 0);
        Location p8 = p4.clone().add(0, halfSize * 2, 0);


        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !hasSelection(player) || !player.getWorld().equals(center.getWorld())) {
                    clearSelection(player);
                    this.cancel();
                    return;
                }

                // ZMIANA: Sprawdzamy, czy gracz trzyma narzędzie
                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand.getType() != Material.GOLDEN_AXE || !itemInHand.hasItemMeta() || !itemInHand.getItemMeta().getDisplayName().equals(toolName)) {
                    return; // Jeśli nie trzyma, nic nie rysujemy i czekamy na następne sprawdzenie
                }

                // Rysowanie 12 krawędzi sześcianu
                drawEdge(p1, p2); drawEdge(p2, p3); drawEdge(p3, p4); drawEdge(p4, p1);
                drawEdge(p5, p6); drawEdge(p6, p7); drawEdge(p7, p8); drawEdge(p8, p5);
                drawEdge(p1, p5); drawEdge(p2, p6); drawEdge(p3, p7); drawEdge(p4, p8);
            }

            private void drawEdge(Location start, Location end) {
                double distance = start.distance(end);

                // ZMIANA: Używamy cząsteczek END_ROD dla lepszej widoczności i zwiększamy gęstość
                for (double i = 0; i < distance; i += 0.3) { // mniejsza wartość = gęstsze cząsteczki
                    double x = start.getX() + (i / distance) * (end.getX() - start.getX());
                    double y = start.getY() + (i / distance) * (end.getY() - start.getY());
                    double z = start.getZ() + (i / distance) * (end.getZ() - start.getZ());

                    start.getWorld().spawnParticle(Particle.END_ROD, x, y, z, 1, 0, 0, 0, 0);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L); // Sprawdzaj co pół sekundy (10 ticków)

        particleTasks.put(player.getUniqueId(), task);
    }
}