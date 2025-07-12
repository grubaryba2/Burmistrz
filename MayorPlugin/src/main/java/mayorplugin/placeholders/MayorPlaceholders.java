package mayorplugin.placeholders;

import mayorplugin.MayorPlugin;
import mayorplugin.logic.ElectionManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import java.time.Duration;
import java.time.Instant;

public class MayorPlaceholders extends PlaceholderExpansion {

    private final MayorPlugin plugin;

    public MayorPlaceholders(MayorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "mayor";
    }

    @Override
    public @NotNull String getAuthor() {
        return "twojnick (Gemini)";
    }

    @Override
    public @NotNull String getVersion() {
        return "FINAL";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        ElectionManager em = plugin.getElectionManager();
        if (em == null) return "Ładowanie...";

        switch(params) {
            case "current":
                return em.getCurrentMayorName();
            case "term_ends":
            case "election_ends":
                return formatDuration(em.getTermEnd());
            case "election_status":
                return em.isElectionRunning() ? "Trwa głosowanie" : "Wybory zamknięte";
            default:
                return null;
        }
    }

    private String formatDuration(Instant endTime) {
        if (endTime == null) return "Nigdy";
        Duration duration = Duration.between(Instant.now(), endTime);
        if (duration.isNegative() || duration.isZero()) return "Zakończono";

        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("g ");
        if (minutes > 0) sb.append(minutes).append("m ");

        if (sb.length() == 0 && seconds > 0) {
            sb.append(seconds).append("s");
        } else if (sb.length() == 0) {
            return "mniej niż minutę";
        }

        return sb.toString().trim();
    }
}