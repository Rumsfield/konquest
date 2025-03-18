package com.github.rumsfield.konquest.listener;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.api.model.KonquestRelationshipType;
import com.github.rumsfield.konquest.manager.KingdomManager;
import com.github.rumsfield.konquest.manager.TerritoryManager;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;

public class HangingListener implements Listener {

	private final Konquest konquest;
	private final KingdomManager kingdomManager;
	private final TerritoryManager territoryManager;
	
	public HangingListener(KonquestPlugin plugin) {
		this.konquest = plugin.getKonquestInstance();
		this.kingdomManager = konquest.getKingdomManager();
		this.territoryManager = konquest.getTerritoryManager();
	}
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onHangingPlace(HangingPlaceEvent event) {
		if(event.isCancelled()) return;
		if(konquest.isWorldIgnored(event.getBlock().getWorld())) return;
		Player bukkitPlayer = event.getPlayer();
		if(bukkitPlayer == null) return;
		KonPlayer player = konquest.getPlayerManager().getPlayer(event.getPlayer());
		if(player == null) return;
		Location placeLoc = event.getEntity().getLocation();
		//ChatUtil.printDebug("EVENT player hanging placement of "+event.getEntity().getType());
		if(!player.isAdminBypassActive() && territoryManager.isChunkClaimed(placeLoc)) {
			KonTerritory territory = territoryManager.getChunkTerritory(placeLoc);
			// Property Flag Holders
			if(territory instanceof KonPropertyFlagHolder) {
				KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
				if(flagHolder.hasPropertyValue(KonPropertyFlag.BUILD)) {
					if(!flagHolder.getPropertyValue(KonPropertyFlag.BUILD)) {
						ChatUtil.sendKonBlockedProtectionTitle(player);
						event.setCancelled(true);
						return;
					}
				}
			}
			// Specific territory protections
			if(territory instanceof KonTown) {
				KonTown town = (KonTown) territory;
				// Check for placement inside of town's monument
				if(town.getMonument().isLocInside(placeLoc)) {
					//ChatUtil.printDebug("EVENT: Hanging placed inside of monument");
					ChatUtil.sendKonBlockedProtectionTitle(player);
					event.setCancelled(true);
					return;
				}
				// Prevent non-friendlies (including enemies) and friendly non-residents
				KonquestRelationshipType playerRole = kingdomManager.getRelationRole(player.getKingdom(), territory.getKingdom());
				if(!playerRole.equals(KonquestRelationshipType.FRIENDLY) || (!town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer()))) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_NOT_RESIDENT.getMessage(territory.getName()));
					event.setCancelled(true);
					return;
				}
			} else if(territory instanceof KonRuin) {
				// Always prevent
				ChatUtil.sendKonBlockedProtectionTitle(player);
				event.setCancelled(true);
				return;
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onHangingBreakPlayer(HangingBreakByEntityEvent event) {
		// Handle hanging breaks by players
		if(event.isCancelled()) return;
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) return;
		if(!(event.getRemover() instanceof Player)) return;
		Player bukkitPlayer = (Player)event.getRemover();
		KonPlayer player = konquest.getPlayerManager().getPlayer(bukkitPlayer);
		if(player == null) return;
		Location brakeLoc = event.getEntity().getLocation();
		//ChatUtil.printDebug("EVENT player hanging break of "+event.getEntity().getType());
		if(!player.isAdminBypassActive() && territoryManager.isChunkClaimed(brakeLoc)) {
			KonTerritory territory = territoryManager.getChunkTerritory(brakeLoc);
			// Property Flag Holders
			if(territory instanceof KonPropertyFlagHolder) {
				KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
				if(flagHolder.hasPropertyValue(KonPropertyFlag.BUILD)) {
					if(!flagHolder.getPropertyValue(KonPropertyFlag.BUILD)) {
						ChatUtil.sendKonBlockedFlagTitle(player);
						event.setCancelled(true);
						return;
					}
				}
			}
			// Specific territory protections
			if(territory instanceof KonTown) {
				KonTown town = (KonTown) territory;
				// Check for break inside of town's monument
				if(town.getMonument().isLocInside(brakeLoc)) {
					ChatUtil.printDebug("EVENT: Hanging broke inside of monument");
					ChatUtil.sendKonBlockedProtectionTitle(player);
					event.setCancelled(true);
					return;
				}
				// Prevent non-friendlies (including enemies) and friendly non-residents
				KonquestRelationshipType playerRole = kingdomManager.getRelationRole(player.getKingdom(), territory.getKingdom());
				if(!playerRole.equals(KonquestRelationshipType.FRIENDLY) || (!town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer()))) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_NOT_RESIDENT.getMessage(territory.getName()));
					event.setCancelled(true);
					return;
				}
			} else if(territory instanceof KonRuin) {
				// Always prevent
				ChatUtil.sendKonBlockedProtectionTitle(player);
				event.setCancelled(true);
				return;
			}
		}
    }

	@EventHandler(priority = EventPriority.MONITOR)
	public void onHangingBreakMonument(HangingBreakByEntityEvent event) {
		// Handle hanging breaks during monument changes
		Location brakeLoc = event.getEntity().getLocation();
		if(!territoryManager.isChunkClaimed(brakeLoc)) return;
		KonTerritory territory = territoryManager.getChunkTerritory(brakeLoc);
		// Check for break inside of town monument
		if(territory instanceof KonTown) {
			KonTown town = (KonTown) territory;
			if(town.getMonument().isItemDropsDisabled() && town.getMonument().isLocInside(brakeLoc)) {
				event.getEntity().remove();
			}
		}

	}
}
