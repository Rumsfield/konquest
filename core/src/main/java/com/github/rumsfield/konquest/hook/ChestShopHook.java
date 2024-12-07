package com.github.rumsfield.konquest.hook;

import com.Acrobot.ChestShop.ChestShop;
import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.listener.ChestShopListener;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class ChestShopHook implements PluginHook {

    private final Konquest konquest;
    private ChestShop chestShopAPI;
    private boolean isEnabled;

    public ChestShopHook(Konquest konquest) {
        this.konquest = konquest;
        this.isEnabled = false;
        this.chestShopAPI = null;
    }

    @Override
    public int reload() {
        isEnabled = false;
        // Attempt to integrate ChestShop
        Plugin chestShop = Bukkit.getPluginManager().getPlugin("ChestShop");
        if(chestShop == null) {
            return 1;
        }
        if(!chestShop.isEnabled()) {
            return 2;
        }
        if(!konquest.getCore().getBoolean(CorePath.INTEGRATION_CHESTSHOP.getPath(),false)) {
            return 3;
        }
        try {
            chestShopAPI = (ChestShop) chestShop;
            konquest.getPlugin().getServer().getPluginManager().registerEvents(new ChestShopListener(konquest.getPlugin()), konquest.getPlugin());
            isEnabled = true;
            return 0;
        } catch (NoClassDefFoundError ignore) {
            ChatUtil.printConsoleError("Wrong ChestShop plugin installed, check the Konquest wiki for official dependencies.");
            return 4;
        }
    }

    @Override
    public void shutdown() {
        // Do Nothing
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public String getPluginName() {
        return "ChestShop";
    }

    @Nullable
    public ChestShop getAPI() {
        return chestShopAPI;
    }
}
