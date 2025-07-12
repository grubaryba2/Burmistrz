package pl.twojanazwa.reputacja.handlers;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import pl.twojanazwa.reputacja.SystemReputacjiPlugin;

public class AnimationManager {

    private final SystemReputacjiPlugin plugin;
    public static final String FANG_METADATA_KEY = "systemreputacji.fang";

    public AnimationManager(SystemReputacjiPlugin plugin) {
        this.plugin = plugin;
    }

    public void startFangsAnimation(Player target) {
        Entity fangs = target.getWorld().spawnEntity(target.getLocation(), EntityType.EVOKER_FANGS);
        // Nadajemy kłom specjalny, niewidzialny znacznik
        fangs.setMetadata(FANG_METADATA_KEY, new FixedMetadataValue(plugin, true));
        fangs.setSilent(true);
    }
}