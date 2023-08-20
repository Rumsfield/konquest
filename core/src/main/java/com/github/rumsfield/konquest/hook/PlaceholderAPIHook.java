package com.github.rumsfield.konquest.hook;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class PlaceholderAPIHook implements PluginHook {

    private boolean isEnabled;

    public PlaceholderAPIHook() {
        this.isEnabled = false;
    }

    @Override
    public int reload() {
        isEnabled = false;
        Plugin placeholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if(placeholderAPI == null) {
            return 1;
        }
        if(!placeholderAPI.isEnabled()) {
            return 2;
        }
        isEnabled = true;
        return 0;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public String getPluginName() {
        return "PlaceholderAPI";
    }
}
