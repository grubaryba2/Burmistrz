package mayorplugin.data;

import mayorplugin.MayorPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;

public class DataManager {

    private final MayorPlugin plugin;
    private FileConfiguration dataConfig = null;
    private File dataFile = null;

    public DataManager(MayorPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig(); // Zapisuje domyślny config.yml, jeśli nie istnieje
        setupDataFile(); // Ustawia plik data.yml
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        // Po przeładowaniu, warto odświeżyć elementy zależne od konfiguracji
        plugin.getHologramManager().createOrUpdateHologram();
        plugin.getElectionGUI().updateSign();
        plugin.getTagManager().reload();
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public void setupDataFile() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data.yml!");
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public FileConfiguration getDataConfig() {
        if (dataConfig == null) {
            setupDataFile();
        }
        return dataConfig;
    }

    public void saveData() {
        if (dataConfig == null || dataFile == null) {
            return;
        }
        try {
            getDataConfig().save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save data to " + dataFile);
        }
    }

    public void saveLocation(String path, Location loc) {
        // Zapisuje lokalizacje do głównego config.yml, a nie data.yml
        getConfig().set(path, loc);
        plugin.saveConfig();
    }
}