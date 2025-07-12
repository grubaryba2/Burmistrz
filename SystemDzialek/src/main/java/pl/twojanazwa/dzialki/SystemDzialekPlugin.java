package pl.twojanazwa.dzialki;

import org.bukkit.plugin.java.JavaPlugin;
import pl.twojanazwa.dzialki.commands.DzialkaCommand;
import pl.twojanazwa.dzialki.gui.GUIListener;
import pl.twojanazwa.dzialki.handlers.PlotManager;
import pl.twojanazwa.dzialki.handlers.RequestManager;
import pl.twojanazwa.dzialki.handlers.SelectionManager;
import pl.twojanazwa.dzialki.listeners.RegionListener; // <-- Dodaj import
import pl.twojanazwa.dzialki.listeners.ToolListener;

public final class SystemDzialekPlugin extends JavaPlugin {

    private SelectionManager selectionManager;
    private RequestManager requestManager;
    private PlotManager plotManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.selectionManager = new SelectionManager(this);
        this.requestManager = new RequestManager(this);
        this.plotManager = new PlotManager(this);

        getCommand("dzialka").setExecutor(new DzialkaCommand(this));
        getServer().getPluginManager().registerEvents(new ToolListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        // ZMIANA: Rejestrujemy nowy listener
        getServer().getPluginManager().registerEvents(new RegionListener(this), this);

        getLogger().info("Plugin SystemDzialek został załadowany!");
    }

    public SelectionManager getSelectionManager() { return selectionManager; }
    public RequestManager getRequestManager() { return requestManager; }
    public PlotManager getPlotManager() { return plotManager; }
}