package pl.twojanazwa.reputacja.handlers;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pl.twojanazwa.reputacja.SystemReputacjiPlugin;
import static org.bukkit.ChatColor.translateAlternateColorCodes;

public class ArrestManager {

    private final SystemReputacjiPlugin plugin;
    private final ReputationManager reputationManager;
    private final AnimationManager animationManager;

    public ArrestManager(SystemReputacjiPlugin plugin) {
        this.plugin = plugin;
        this.reputationManager = plugin.getReputationManager();
        this.animationManager = plugin.getAnimationManager();
    }

    public void processArrest(Player cel, Player policjant) {
        FileConfiguration config = plugin.getConfig();
        Economy economy = plugin.getEconomy();

        if (reputationManager.isBad(cel.getUniqueId())) {
            double nagroda = config.getDouble("ekonomia.nagroda-za-zle_osoby");
            economy.depositPlayer(policjant, nagroda);

            String msg = t(config.getString("wiadomosci.aresztowano-zlego").replace("{KWOTA}", economy.format(nagroda)));
            policjant.sendMessage(msg);

            performJailingSequence(cel, policjant);

        } else {
            double grzywna = config.getDouble("ekonomia.grzywna-za-dobre_osoby");
            economy.withdrawPlayer(policjant, grzywna);

            String msgPolicjant = t(config.getString("wiadomosci.aresztowano-dobrego")
                    .replace("{CEL}", cel.getName())
                    .replace("{KWOTA}", economy.format(grzywna)));
            policjant.sendMessage(msgPolicjant);

            String msgCel = t(config.getString("wiadomosci.uniewinniony"));
            cel.sendMessage(msgCel);
        }
    }

    public void processAdminArrest(Player cel, Player admin) {
        if (!reputationManager.isBad(cel.getUniqueId())) {
            admin.sendMessage(t("&cNie możesz wsadzić do więzienia dobrego obywatela!"));
            return;
        }
        performJailingSequence(cel, admin);
        admin.sendMessage(t("&aGracz " + cel.getName() + " został osadzony w więzieniu."));
    }

    private void performJailingSequence(Player cel, Player policjant) {
        FileConfiguration config = plugin.getConfig();

        policjant.playSound(policjant.getLocation(), "custom.wiezienie.aresztowanie", SoundCategory.RECORDS, 1.0f, 1.0f);
        cel.playSound(cel.getLocation(), "custom.wiezienie.aresztowanie", SoundCategory.RECORDS, 1.0f, 1.0f);

        int stunDuration = config.getInt("kajdanki.czas-ogluszenia");
        applyStun(cel, stunDuration);

        animationManager.startFangsAnimation(cel);

        int czasDoTeleportu = config.getInt("kajdanki.czas-do-teleportu");
        if(policjant != cel) {
            String msgSkuto = t(config.getString("wiadomosci.skuto-cel").replace("{CEL}", cel.getName()).replace("{CZAS}", String.valueOf(czasDoTeleportu)));
            policjant.sendMessage(msgSkuto);
        }

        String msgZostalesSkuty = t(config.getString("wiadomosci.zostales-skuty").replace("{CZAS}", String.valueOf(czasDoTeleportu)));
        cel.sendMessage(msgZostalesSkuty);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Location prisonLoc = config.getLocation("wiezienie.lokalizacja");
            if (prisonLoc != null && cel.isValid()) {
                cel.teleport(prisonLoc);
                reputationManager.setGood(cel.getUniqueId());
                if(policjant != cel) {
                    policjant.sendMessage(t(config.getString("wiadomosci.osadzono-w-wiezieniu").replace("{CEL}", cel.getName())));
                }
                cel.sendMessage(t(config.getString("wiadomosci.zostales-osadzony")));
            } else if (prisonLoc == null) {
                policjant.sendMessage(t(config.getString("wiadomosci.brak-wiezienia")));
            }
        }, czasDoTeleportu * 20L);
    }

    // ZMIANA: Metoda jest teraz publiczna
    public void applyStun(LivingEntity target, int durationSeconds) {
        int ticks = durationSeconds * 20;
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, 250, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, ticks, 200, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, ticks, 250, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, ticks, 250, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, ticks, 1, false, false));
    }

    private String t(String text) {
        return translateAlternateColorCodes('&', text);
    }
}