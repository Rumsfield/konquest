package com.github.rumsfield.konquest.hook;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BlueMapHook implements PluginHook {

    private final Konquest konquest;
    private boolean isEnabled;
    private Optional<BlueMapAPI> bAPI;

    public BlueMapHook(Konquest konquest) {
        this.konquest = konquest;
        this.isEnabled = false;
        this.bAPI = Optional.empty();
    }

    @Override
    public String getPluginName() {
        return "BlueMap";
    }

    @Override
    public int reload() {
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
        bAPI = BlueMapAPI.getInstance();
        if(bAPI.isEmpty()) {
            ChatUtil.printConsoleError("Failed to register BlueMap. Is it disabled?");
            return -1;
        }
        isEnabled = true;
        return 0;
    }

    @Override
    public boolean isEnabled() { return isEnabled; }

    @Nullable
    public BlueMapAPI getAPI() {
        return bAPI.orElse(null);
    }
}
