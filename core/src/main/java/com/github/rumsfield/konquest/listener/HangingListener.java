package com.github.rumsfield.konquest.listener;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
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
	private final TerritoryManager territoryManager;
	
	public HangingListener(KonquestPlugin plugin) {
		this.konquest = plugin.getKonquestInstance();
		this.territoryManager = konquest.getTerritoryManager();
	}
	
	@EventHandler()
    public void onHangingPlace(HangingPlaceEvent event) {
		KonPlayer player = konquest.getPlayerManager().getPlayer(event.getPlayer());
		if(player == null) return;
		Location placeLoc = event.getEntity().getLocation();
		if(!player.isAdminBypassActive() && territoryManager.isChunkClaimed(placeLoc)) {
			KonTerritory territory = territoryManager.getChunkTerritory(placeLoc);

			// Sanctuary & Ruin protections
			if((territory instanceof KonSanctuary) || (territory instanceof KonRuin)) {
				// Prevent placing all hanging things
				ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedProtectionColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
				event.setCancelled(true);
				return;
			}

			// Check for break inside of town
			if(territory instanceof KonTown) {
				KonTown town = (KonTown) territory;
				// Check for break inside of town's monument
				if(town.getMonument().isLocInside(placeLoc)) {
					ChatUtil.printDebug("EVENT: Hanging placed inside of monument");
					ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedProtectionColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
					event.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.LOW)
    public void onHangingBreakPlayer(HangingBreakByEntityEvent event) {
		// Handle hanging breaks by players
		if(!(event.getRemover() instanceof Player)) return;
		KonPlayer player = konquest.getPlayerManager().getPlayer((Player)event.getRemover());
		if(player == null) return;
		Location brakeLoc = event.getEntity().getLocation();
		if(!player.isAdminBypassActive() && territoryManager.isChunkClaimed(brakeLoc)) {
			KonTerritory territory = territoryManager.getChunkTerritory(brakeLoc);

			// Sanctuary & Ruin protections
			if((territory instanceof KonSanctuary) || (territory instanceof KonRuin)) {
				// Prevent breaking all hanging things
				ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedProtectionColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
				event.setCancelled(true);
				return;
			}

			// Town protections
			if(territory instanceof KonTown) {
				KonTown town = (KonTown) territory;
				// Check for break inside of town's monument
				if(town.getMonument().isLocInside(brakeLoc)) {
					ChatUtil.printDebug("EVENT: Hanging broke inside of town monument");
					ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedProtectionColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
					event.setCancelled(true);
				}
			}
		}
    }

	@EventHandler()
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
