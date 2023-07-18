package com.github.rumsfield.konquest.hook;

public class ChestShopHook implements PluginHook {
    @Override
    public int reload() {
        return PluginHook.super.reload();
    }

    @Override
    public void shutdown() {
        PluginHook.super.shutdown();
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public String getPluginName() {
        return "ChestShop";
    }
}
