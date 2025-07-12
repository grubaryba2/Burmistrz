package mayorplugin;

import mayorplugin.commands.MayorCommand;
import mayorplugin.data.DataManager;
import mayorplugin.gui.ElectionGUI;
import mayorplugin.listeners.BlockProtectionListener;
import mayorplugin.listeners.ChatListener;
import mayorplugin.listeners.PlayerJoinListener;
import mayorplugin.listeners.SignListener;
import mayorplugin.logic.ElectionManager;
import mayorplugin.logic.HologramManager;
import mayorplugin.logic.TagManager;
import mayorplugin.placeholders.MayorPlaceholders;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.logging.Logger;

public final class MayorPlugin extends JavaPlugin {
    private static MayorPlugin instance;
    private DataManager dataManager;
    private ElectionManager electionManager;
    private HologramManager hologramManager;
    private ElectionGUI electionGUI;
    private TagManager tagManager;
    private static Economy econ = null;
    private static final Logger log = Logger.getLogger("Minecraft");

    @Override
    public void onEnable() {
        instance = this;
        if (!setupEconomy()) { log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName())); getServer().getPluginManager().disablePlugin(this); return; }
        this.dataManager = new DataManager(this);
        this.hologramManager = new HologramManager(this);
        this.tagManager = new TagManager(this);
        this.electionManager = new ElectionManager(this);
        this.electionGUI = new ElectionGUI(this);
        getCommand("mayor").setExecutor(new MayorCommand(this));
        getServer().getPluginManager().registerEvents(new SignListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(electionGUI, this);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) { new MayorPlaceholders(this).register(); }
        new BukkitRunnable() { @Override public void run() { hologramManager.createOrUpdateHologram(); electionGUI.updateSign(); } }.runTaskLater(this, 20L);
        getLogger().info("MayorPlugin (FINAL) has been enabled.");
    }
    @Override
    public void onDisable() { if (electionManager != null) electionManager.saveData(); if (hologramManager != null) hologramManager.removeHolograms(); getLogger().info("MayorPlugin has been disabled."); }
    private boolean setupEconomy() { if (getServer().getPluginManager().getPlugin("Vault") == null) return false; RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class); if (rsp == null) return false; econ = rsp.getProvider(); return econ != null; }
    public static MayorPlugin getInstance() { return instance; }
    public DataManager getDataManager() { return dataManager; }
    public ElectionManager getElectionManager() { return electionManager; }
    public HologramManager getHologramManager() { return hologramManager; }
    public ElectionGUI getElectionGUI() { return electionGUI; }
    public TagManager getTagManager() { return tagManager; }
    public static Economy getEconomy() { return econ; }
}