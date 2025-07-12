package pl.twojanazwa.dzialki.handlers;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.twojanazwa.dzialki.SystemDzialekPlugin;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RequestManager {

    private final SystemDzialekPlugin plugin;
    private final File requestsFile;
    private final FileConfiguration requestsConfig;

    public RequestManager(SystemDzialekPlugin plugin) {
        this.plugin = plugin;
        this.requestsFile = new File(plugin.getDataFolder(), "requests.yml");
        if (!requestsFile.exists()) {
            try {
                requestsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.requestsConfig = YamlConfiguration.loadConfiguration(requestsFile);
    }

    public void createRequest(Player player, Location center, int size) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String path = "requests." + player.getUniqueId() + "." + timeStamp;

        requestsConfig.set(path + ".ownerName", player.getName());
        requestsConfig.set(path + ".world", center.getWorld().getName());
        requestsConfig.set(path + ".x", center.getBlockX());
        requestsConfig.set(path + ".y", center.getBlockY());
        requestsConfig.set(path + ".z", center.getBlockZ());
        requestsConfig.set(path + ".size", size);

        try {
            requestsConfig.save(requestsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}