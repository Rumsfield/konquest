package com.github.rumsfield.konquest.hook;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.github.rumsfield.konquest.utility.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class ProtocolLibHook implements PluginHook {

    private boolean isEnabled;
    private ProtocolManager plib = null;

    public ProtocolLibHook() {
        this.isEnabled = false;
    }

    @Override
    public int reload() {
        isEnabled = false;
        Plugin protocolLib = Bukkit.getPluginManager().getPlugin("ProtocolLib");
        if(protocolLib == null) {
            return 1;
        }
        if(!protocolLib.isEnabled()) {
            return 2;
        }
        try {
            plib = ProtocolLibrary.getProtocolManager();
            isEnabled = true;
            return 0;
        } catch(Exception | NoClassDefFoundError e) {
            ChatUtil.printConsoleError("Failed to load ProtocolLib, is it the latest version?");
            e.printStackTrace();
            return -1;
        }
    }

    public ProtocolManager getLib() {
        return plib;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public String getPluginName() {
        return "ProtocolLib";
    }
}
