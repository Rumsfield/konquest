package com.github.rumsfield.konquest.listener;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.manager.IntegrationManager;
import com.github.rumsfield.konquest.manager.PlayerManager;
import com.github.rumsfield.konquest.manager.TerritoryManager;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.maxgamer.quickshop.api.event.ShopPreCreateEvent;
import org.maxgamer.quickshop.api.event.ShopPurchaseEvent;

public class QuickShopListener implements Listener{

	private final Konquest konquest;
	private final PlayerManager playerManager;
	private final TerritoryManager territoryManager;
	private final IntegrationManager integrationManager;
	
	public QuickShopListener(KonquestPlugin plugin) {
		this.konquest = plugin.getKonquestInstance();
		this.playerManager = konquest.getPlayerManager();
		this.territoryManager = konquest.getTerritoryManager();
		this.integrationManager = konquest.getIntegrationManager();
	}
	
	//TODO Implement message broker that prevents spam (double events) OR fix QuickShop from firing this event twice
	/**
	 * Allow players to create shops only in friendly towns or their camp
	 */
	@EventHandler()
    public void onShopPreCreate(ShopPreCreateEvent event) {
		if(!integrationManager.getQuickShop().isEnabled()) return;
		if(konquest.isWorldIgnored(event.getLocation())) {
			return;
		}
		// Bypass all checks for admins in bypass mode
		KonPlayer player = playerManager.getPlayer(event.getPlayer());
		if(player == null || player.isAdminBypassActive()) return;
		if(territoryManager.isChunkClaimed(event.getLocation())) {
			KonTerritory territory = territoryManager.getChunkTerritory(event.getLocation());
			if(territory instanceof KonTown) {
				if(!player.getKingdom().equals(territory.getKingdom())) {
					ChatUtil.sendError(event.getPlayer(), MessagePath.QUICKSHOP_ERROR_ENEMY_USE.getMessage());
					event.setCancelled(true);
					return;
				}
				if(((KonTown) territory).isLocInsideMonumentProtectionArea(event.getLocation())) {
					ChatUtil.sendError(event.getPlayer(), MessagePath.QUICKSHOP_ERROR_MONUMENT.getMessage());
					event.setCancelled(true);
					return;
				}
			}
			if(territory instanceof KonCamp && !((KonCamp)territory).isPlayerOwner(event.getPlayer())) {
				ChatUtil.sendError(event.getPlayer(), MessagePath.QUICKSHOP_ERROR_CAMP.getMessage());
				event.setCancelled(true);
				return;
			}
			if(territory instanceof KonCapital) {
				ChatUtil.sendError(event.getPlayer(), MessagePath.QUICKSHOP_ERROR_FAIL.getMessage());
				event.setCancelled(true);
			}
		} else {
			ChatUtil.sendError(event.getPlayer(), MessagePath.QUICKSHOP_ERROR_WILD.getMessage());
			event.setCancelled(true);
		}

	}
	
	/**
	 * Protect shops from being used by enemies
	 */
	@EventHandler()
    public void onShopPurchase(ShopPurchaseEvent event) {
		if(konquest.isWorldIgnored(event.getShop().getLocation())) return;
		if(!integrationManager.getQuickShop().isEnabled()) return;

		// Bypass all checks for admins in bypass mode
		Player purchaser = Bukkit.getPlayer(event.getPurchaser());
		KonPlayer player = playerManager.getPlayer(purchaser);
		if(player != null && !player.isAdminBypassActive()) {
			Location shopLoc = event.getShop().getLocation();
			if(territoryManager.isChunkClaimed(shopLoc)) {
				KonTerritory territory = territoryManager.getChunkTerritory(shopLoc);
				if(!player.getKingdom().equals(territory.getKingdom())) {
					ChatUtil.sendError(purchaser, MessagePath.QUICKSHOP_ERROR_ENEMY_USE.getMessage());
					event.setCancelled(true);
				}
			}
		}
		

	}
	
}
