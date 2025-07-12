package pl.twojanazwa.reputacja.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;
import pl.twojanazwa.reputacja.SystemReputacjiPlugin;
import pl.twojanazwa.reputacja.handlers.AnimationManager;
import pl.twojanazwa.reputacja.handlers.ArrestManager;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InterakcjeListener implements Listener {

    private final SystemReputacjiPlugin plugin;
    private final ArrestManager arrestManager;
    private final String kajdankiName;
    private final String paralizatorName;

    public InterakcjeListener(SystemReputacjiPlugin plugin) {
        this.plugin = plugin;
        this.arrestManager = plugin.getArrestManager();
        this.kajdankiName = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("kajdanki.nazwa-przedmiotu"));
        this.paralizatorName = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("paralizator.nazwa-przedmiotu"));
    }

    private String t(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private boolean isPluginItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        String strippedKajdankiName = ChatColor.stripColor(this.kajdankiName);
        String strippedParalizatorName = ChatColor.stripColor(this.paralizatorName);
        return (item.getType() == Material.IRON_BARS && displayName.startsWith(strippedKajdankiName)) ||
                (item.getType() == Material.BLAZE_ROD && displayName.startsWith(strippedParalizatorName));
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isPluginItem(event.getItemInHand())) event.setCancelled(true);
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        for (ItemStack item : inventory.getMatrix()) {
            if (isPluginItem(item)) {
                inventory.setResult(null);
                break;
            }
        }
    }

    @EventHandler
    public void onEntityDamageByFang(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager.getType() == EntityType.EVOKER_FANGS && damager.hasMetadata(AnimationManager.FANG_METADATA_KEY)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractAtEntityEvent event) {
        Player policjant = event.getPlayer();
        ItemStack itemInHand = policjant.getInventory().getItemInMainHand();

        if (!isPluginItem(itemInHand) || itemInHand.getType() != Material.IRON_BARS) return;
        event.setCancelled(true);
        if (!(event.getRightClicked() instanceof Player)) return;

        if (plugin.getCooldownsKajdanki().containsKey(policjant.getUniqueId())) {
            return;
        }

        Player cel = (Player) event.getRightClicked();

        if (policjant.getLocation().distanceSquared(cel.getLocation()) > 3 * 3) {
            return;
        }

        long cooldown = plugin.getConfig().getLong("kajdanki.cooldown") * 1000;
        plugin.getCooldownsKajdanki().put(policjant.getUniqueId(), System.currentTimeMillis() + cooldown);

        arrestManager.processArrest(cel, policjant);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (!isPluginItem(itemInHand) || itemInHand.getType() != Material.BLAZE_ROD) return;
        event.setCancelled(true);

        if (plugin.getCooldownsParalizator().containsKey(player.getUniqueId())) return;

        FileConfiguration config = plugin.getConfig();
        ItemMeta meta = itemInHand.getItemMeta();
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return;

        int uses = -1;
        int maxUses = -1;
        int lineIndex = -1;
        for (int i = 0; i < lore.size(); i++) {
            Matcher matcher = Pattern.compile("Użycia: (\\d+)/(\\d+)").matcher(ChatColor.stripColor(lore.get(i)));
            if (matcher.find()) {
                uses = Integer.parseInt(matcher.group(1));
                maxUses = Integer.parseInt(matcher.group(2));
                lineIndex = i;
                break;
            }
        }

        if (uses == -1) return;
        if (uses <= 0) {
            player.sendMessage(t(config.getString("wiadomosci.paralizator-rozladowany")));
            return;
        }

        double range = config.getDouble("paralizator.odleglosc");
        RayTraceResult result = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), range,
                (entity) -> entity instanceof LivingEntity && !entity.equals(player));

        if (result == null || !(result.getHitEntity() instanceof LivingEntity)) return;

        LivingEntity target = (LivingEntity) result.getHitEntity();

        long cooldown = config.getLong("paralizator.cooldown") * 1000;
        plugin.getCooldownsParalizator().put(player.getUniqueId(), System.currentTimeMillis() + cooldown);

        uses--;
        lore.set(lineIndex, ChatColor.GRAY + "Użycia: " + uses + "/" + maxUses);
        meta.setLore(lore);
        itemInHand.setItemMeta(meta);

        int stunDuration = config.getInt("paralizator.czas-ogluszenia");
        arrestManager.applyStun(target, stunDuration);

        if (target instanceof Player) {
            target.sendMessage(t(config.getString("wiadomosci.zostales-sparalizowany")));
        }

        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.5f);
        target.getWorld().spawnParticle(Particle.WITCH, target.getLocation().add(0, 1, 0), 30);
        String targetName = target instanceof Player ? target.getName() : "Byt";
        player.sendMessage(t(config.getString("wiadomosci.trafiono-paralizatorem").replace("{CEL}", targetName)));
    }
}