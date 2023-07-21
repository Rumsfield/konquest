package com.github.rumsfield.konquest.listener;

import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.shop.ShopHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.maxgamer.quickshop.api.event.ShopPreCreateEvent;
import org.maxgamer.quickshop.api.event.ShopPurchaseEvent;

public class QuickShopListener implements Listener{

	private final ShopHandler shopHandler;
	
	public QuickShopListener(KonquestPlugin plugin) {
		this.shopHandler = plugin.getKonquestInstance().getShopHandler();
	}
	
	//TODO Implement message broker that prevents spam (double events) OR fix QuickShop from firing this event twice
	/**
	 * Allow players to create shops only in friendly towns or their camp
	 */
	@EventHandler()
    public void onShopPreCreate(ShopPreCreateEvent event) {
		boolean status = shopHandler.onShopCreate(event.getLocation(), event.getPlayer());
		if(!status) {
			event.setCancelled(true);
		}
	}
	
	/**
	 * Protect shops from being used by enemies
	 */
	@EventHandler()
    public void onShopPurchase(ShopPurchaseEvent event) {
		Player purchaser = Bukkit.getPlayer(event.getPurchaser());
		boolean status = shopHandler.onShopUse(event.getShop().getLocation(), purchaser);
		if(!status) {
			event.setCancelled(true);
		}

	}
	
}
