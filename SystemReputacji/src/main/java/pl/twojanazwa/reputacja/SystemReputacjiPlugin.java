package pl.twojanazwa.reputacja;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import pl.twojanazwa.reputacja.commands.GlowneKomendy;
import pl.twojanazwa.reputacja.handlers.*;
import pl.twojanazwa.reputacja.listeners.GriefListener;
import pl.twojanazwa.reputacja.listeners.InterakcjeListener;
import pl.twojanazwa.reputacja.listeners.ReputationListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SystemReputacjiPlugin extends JavaPlugin {

    private static Economy econ = null;
    private ReputationManager reputationManager;
    private GriefManager griefManager;
    private AnimationManager animationManager;
    private CooldownManager cooldownManager;
    private ArrestManager arrestManager;

    private final Map<UUID, Long> cooldownsKajdanki = new HashMap<>();
    private final Map<UUID, Long> cooldownsParalizator = new HashMap<>();

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe("Nie znaleziono pluginu Vault lub pluginu do ekonomii! Wyłączanie pluginu...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        this.reputationManager = new ReputationManager(this);
        this.griefManager = new GriefManager(this);
        this.animationManager = new AnimationManager(this);
        this.cooldownManager = new CooldownManager(this);
        this.arrestManager = new ArrestManager(this);

        this.cooldownManager.runTaskTimer(this, 0L, 20L);

        try {
            reputationManager.loadReputation();
        } catch (IOException e) {
            getLogger().severe("Nie udało się wczytać pliku reputacji!");
            e.printStackTrace();
        }

        getCommand("ustawwiezienie").setExecutor(new GlowneKomendy(this));
        getCommand("dajkajdanki").setExecutor(new GlowneKomendy(this));
        getCommand("dajparalizator").setExecutor(new GlowneKomendy(this));
        getCommand("reputacja").setExecutor(new GlowneKomendy(this));
        getCommand("wiezienie").setExecutor(new GlowneKomendy(this));
        getCommand("skuj").setExecutor(new GlowneKomendy(this));

        getServer().getPluginManager().registerEvents(new InterakcjeListener(this), this);
        getServer().getPluginManager().registerEvents(new ReputationListener(this), this);
        getServer().getPluginManager().registerEvents(new GriefListener(this), this);

        getLogger().info("Plugin SystemReputacji został załadowany!");
    }

    @Override
    public void onDisable() {
        try {
            reputationManager.saveReputation();
        } catch (IOException e) {
            getLogger().severe("Nie udało się zapisać pliku reputacji!");
            e.printStackTrace();
        }
        getLogger().info("Plugin SystemReputacji został wyłączony.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public Economy getEconomy() { return econ; }
    public ReputationManager getReputationManager() { return reputationManager; }
    public GriefManager getGriefManager() { return griefManager; }
    public AnimationManager getAnimationManager() { return animationManager; }
    public ArrestManager getArrestManager() { return arrestManager; }
    public Map<UUID, Long> getCooldownsKajdanki() { return cooldownsKajdanki; }
    public Map<UUID, Long> getCooldownsParalizator() { return cooldownsParalizator; }
}