package pl.twojanazwa.reputacja.listeners;

import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import pl.twojanazwa.reputacja.SystemReputacjiPlugin;
import pl.twojanazwa.reputacja.handlers.ReputationManager;
import java.util.List;

public class ReputationListener implements Listener {

    private final SystemReputacjiPlugin plugin;
    private final ReputationManager reputationManager;
    private final List<String> protectedAnimals;

    public ReputationListener(SystemReputacjiPlugin plugin) {
        this.plugin = plugin;
        this.reputationManager = plugin.getReputationManager();
        this.protectedAnimals = plugin.getConfig().getStringList("reputacja.chronione-zwierzeta");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player attacker = (Player) event.getDamager();
        Entity victim = event.getEntity();

        if (reputationManager.isBad(attacker.getUniqueId())) return;

        if (victim instanceof Player) {
            Player victimPlayer = (Player) victim;
            if (!attacker.equals(victimPlayer)) {
                reputationManager.setBad(attacker);
            }
        }
        else if (victim instanceof Animals || protectedAnimals.contains(victim.getType().name())) {
            reputationManager.setBad(attacker);
        }
    }
}