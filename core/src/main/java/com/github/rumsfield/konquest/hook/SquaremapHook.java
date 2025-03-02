package com.github.rumsfield.konquest.hook;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;

public class SquaremapHook  implements PluginHook {

    private final Konquest konquest;
    private Squaremap squaremapAPI;
    private boolean isEnabled;

    public SquaremapHook(Konquest konquest) {
        this.konquest = konquest;
        this.squaremapAPI = null;
        this.isEnabled = false;
    }

    @Override
    public String getPluginName() {
        return "squaremap";
    }

    @Override
    public int reload() {

        isEnabled = false;
        // Attempt to integrate squaremap
        Plugin squaremap = Bukkit.getPluginManager().getPlugin("squaremap");
        if(squaremap == null){
            return 1;
        }
        if(!squaremap.isEnabled()){
            return 2;
        }
        if(!konquest.getCore().getBoolean(CorePath.INTEGRATION_SQUAREMAP.getPath(),false)) {
            return 3;
        }
        try {
            squaremapAPI = SquaremapProvider.get();
            isEnabled = true;
        } catch (IllegalStateException ignored) {
            ChatUtil.printConsoleError("Failed to integrate squaremap, plugin not found or disabled");
            return -1;
        }
        return 0;
    }

    @Override
    public boolean isEnabled() { return isEnabled; }

    @Nullable
    public Squaremap getAPI() {
        return squaremapAPI;
    }

}
