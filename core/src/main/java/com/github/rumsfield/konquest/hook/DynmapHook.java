package com.github.rumsfield.konquest.hook;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapAPI;
import org.jetbrains.annotations.Nullable;

public class DynmapHook implements PluginHook {

    private final Konquest konquest;
    private boolean isEnabled;
    private DynmapAPI dAPI;

    public DynmapHook(Konquest konquest) {
        this.konquest = konquest;
        this.isEnabled = false;
        this.dAPI = null;
    }

    @Override
    public String getPluginName() {
        return "Dynmap";
    }

    @Override
    public int reload() {
        // Attempt to integrate Dynmap
        Plugin dynmap = Bukkit.getPluginManager().getPlugin("dynmap");
        if(dynmap == null){
            return 1;
        }
        if(!dynmap.isEnabled()){
            return 2;
        }
        if(!konquest.getCore().getBoolean(CorePath.INTEGRATION_DYNMAP.getPath(),false)) {
            return 3;
        }
        dAPI = (DynmapAPI) Bukkit.getServer().getPluginManager().getPlugin("dynmap");
        if(dAPI == null) {
            ChatUtil.printConsoleError("Failed to register Dynmap. Is it disabled?");
            return -1;
        }
        isEnabled = true;
        return 0;
    }

    @Override
    public boolean isEnabled() { return isEnabled; }

    @Nullable
    public DynmapAPI getDynmapAPI() {
        return dAPI;
    }
}
