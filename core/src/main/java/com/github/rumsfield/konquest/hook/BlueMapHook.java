package com.github.rumsfield.konquest.hook;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.utility.CorePath;
import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class BlueMapHook implements PluginHook {

    private final Konquest konquest;
    private boolean isEnabled;
    private boolean isReady;

    public BlueMapHook(Konquest konquest) {
        this.konquest = konquest;
        this.isEnabled = false;
        this.isReady = false;
    }

    @Override
    public String getPluginName() {
        return "BlueMap";
    }

    @Override
    public int reload() {
        isEnabled = false;
        // Attempt to integrate BlueMap
        Plugin bluemap = Bukkit.getPluginManager().getPlugin("BlueMap");
        if(bluemap == null){
            return 1;
        }
        if(!bluemap.isEnabled()){
            return 2;
        }
        if(!konquest.getCore().getBoolean(CorePath.INTEGRATION_BLUEMAP.getPath(),false)) {
            return 3;
        }
        // BlueMap doesn't load its API until post server load.
        isReady = false;
        BlueMapAPI.onEnable(api -> {
            // Consumer that runs every time BlueMap is (re)loaded.
            // This happens post server load, and any time the BlueMap plugin is reloaded.
            // Update the API and re-draw all territories.
            isReady = true;
            konquest.getMapHandler().initialize();
            konquest.getMapHandler().drawAllTerritories("BlueMap");
        });

        isEnabled = true;
        return 0;
    }

    @Override
    public boolean isEnabled() { return isEnabled; }

    public boolean isReady() {
        return isReady;
    }

    @Nullable
    public BlueMapAPI getAPI() {
        return BlueMapAPI.getInstance().orElse(null);
    }

}
