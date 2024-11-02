package com.github.rumsfield.konquest.listener;

import com.ghostchu.quickshop.api.event.ShopClickEvent;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.shop.ShopHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.ghostchu.quickshop.api.event.ShopPreCreateEvent;
import com.ghostchu.quickshop.api.event.ShopPurchaseEvent;

import java.util.Optional;

public class QuickShopListener implements Listener {

	private final ShopHandler shopHandler;
	
	public QuickShopListener(KonquestPlugin plugin) {
		this.shopHandler = plugin.getKonquestInstance().getShopHandler();
	}

	/**
	 * Allow players to create shops only in friendly towns or their camp
	 */
	@EventHandler()
    public void onShopPreCreate(ShopPreCreateEvent event) {
		Optional<Player> opPlayer = event.getCreator().getBukkitPlayer();
		if (opPlayer.isPresent()) {
			boolean status = shopHandler.onShopCreate(event.getLocation(), opPlayer.get());
			if(!status) {
				event.setCancelled(true,(String)null);
			}
		}
	}
	
	/**
	 * Protect shops from being used by enemies
	 */
	@EventHandler()
    public void onShopPurchase(ShopPurchaseEvent event) {
		Optional<Player> opPlayer = event.getPurchaser().getBukkitPlayer();
		if (opPlayer.isPresent()) {
			boolean status = shopHandler.onShopUse(event.getShop().getLocation(), opPlayer.get());
			if (!status) {
				event.setCancelled(true,(String)null);
			}
		}
	}

	@EventHandler()
	public void onShopClick(ShopClickEvent event) {
		boolean status = shopHandler.onShopUse(event.getShop().getLocation(), event.getClicker());
		if(!status) {
			event.setCancelled(true,(String)null);
		}
	}
	
}
