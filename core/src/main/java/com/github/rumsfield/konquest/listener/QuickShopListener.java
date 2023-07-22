package com.github.rumsfield.konquest.listener;

import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.shop.ShopHandler;
import com.github.rumsfield.konquest.utility.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.maxgamer.quickshop.api.event.PlayerShopClickEvent;
import org.maxgamer.quickshop.api.event.ShopPreCreateEvent;
import org.maxgamer.quickshop.api.event.ShopPurchaseEvent;

public class QuickShopListener implements Listener{

	private final ShopHandler shopHandler;
	
	public QuickShopListener(KonquestPlugin plugin) {
		this.shopHandler = plugin.getKonquestInstance().getShopHandler();
	}

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

	@EventHandler()
	public void onShopClick(PlayerShopClickEvent event) {
		boolean status = shopHandler.onShopUse(event.getShop().getLocation(), event.getPlayer());
		if(!status) {
			event.setCancelled(true);
		}
	}
	
}
