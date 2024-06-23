package com.github.rumsfield.konquest.hook;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.listener.EssentialsXListener;
import com.github.rumsfield.konquest.utility.CorePath;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class EssentialsXHook implements PluginHook {

    private final Konquest konquest;
    private boolean isEnabled;

    public EssentialsXHook(Konquest konquest) {
        this.konquest = konquest;
        this.isEnabled = false;
    }

    @Override
    public String getPluginName() {
        return "EssentialsX";
    }

    @Override
    public int reload() {
        isEnabled = false;
        // Attempt to integrate EssentialsX
        Plugin essentialsX = Bukkit.getPluginManager().getPlugin("Essentials");
        if(essentialsX == null) {
            return 1;
        }
        if(!essentialsX.isEnabled()) {
            return 2;
        }
        if(!konquest.getCore().getBoolean(CorePath.INTEGRATION_ESSENTIALSX.getPath(),false)) {
            return 3;
        }

        isEnabled = true;
        konquest.getPlugin().getServer().getPluginManager().registerEvents(new EssentialsXListener(konquest), konquest.getPlugin());
        return 0;
    }

    @Override
    public void shutdown() {
        // Do Nothing
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }
}
