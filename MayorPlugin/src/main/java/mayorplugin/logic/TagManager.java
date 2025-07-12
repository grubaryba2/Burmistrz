package mayorplugin.logic;

import mayorplugin.MayorPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class TagManager {
    private final MayorPlugin plugin;
    private final String TEAM_NAME = "mayor_team";
    private Team mayorTeam;

    public TagManager(MayorPlugin plugin) {
        this.plugin = plugin;
        setupTeam();
    }

    private void setupTeam() {
        if (!plugin.getDataManager().getConfig().getBoolean("tag.enabled", true)) {
            return;
        }
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        mayorTeam = scoreboard.getTeam(TEAM_NAME);
        if (mayorTeam == null) {
            mayorTeam = scoreboard.registerNewTeam(TEAM_NAME);
        }
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getDataManager().getConfig().getString("tag.prefix", "&6[Burmistrz] &r"));
        mayorTeam.setPrefix(prefix);
    }

    public void applyTag(Player player) {
        if (mayorTeam == null || !plugin.getDataManager().getConfig().getBoolean("tag.enabled", true)) {
            return;
        }
        if (!mayorTeam.hasEntry(player.getName())) {
            mayorTeam.addEntry(player.getName());
        }
    }

    public void removeTag(Player player) {
        if (mayorTeam == null || !plugin.getDataManager().getConfig().getBoolean("tag.enabled", true)) {
            return;
        }
        if (mayorTeam.hasEntry(player.getName())) {
            mayorTeam.removeEntry(player.getName());
        }
    }

    public void reload() {
        Team oldTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(TEAM_NAME);
        if(oldTeam != null) {
            for (String entry : oldTeam.getEntries()) {
                oldTeam.removeEntry(entry);
            }
            oldTeam.unregister();
        }
        setupTeam();
    }
}