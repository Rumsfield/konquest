package com.github.rumsfield.konquest.listener;

import com.Acrobot.ChestShop.Events.PreShopCreationEvent;
import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.shop.ShopHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChestShopListener implements Listener {

    private final ShopHandler shopHandler;

    public ChestShopListener(KonquestPlugin plugin) {
        this.shopHandler = plugin.getKonquestInstance().getShopHandler();
    }

    /**
     * Allow players to create shops only in friendly towns or their camp
     */
    @EventHandler()
    public void onShopPreCreate(PreShopCreationEvent event) {
        boolean status = shopHandler.onShopCreate(event.getSign().getLocation(), event.getPlayer());
        if(!status) {
            event.setCancelled(true);
        }
    }

    /**
     * Protect shops from being used by enemies
     */
    @EventHandler()
    public void onShopPurchase(PreTransactionEvent event) {
        boolean status = shopHandler.onShopUse(event.getSign().getLocation(), event.getClient());
        if(!status) {
            event.setCancelled(true);
        }
    }
}
