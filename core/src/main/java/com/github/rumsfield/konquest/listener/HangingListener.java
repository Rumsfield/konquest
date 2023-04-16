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
import org.bukkit.event.Listener;
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
		Location placeLoc = event.getEntity().getLocation();
		if(territoryManager.isChunkClaimed(placeLoc)) {
			KonTerritory territory = territoryManager.getChunkTerritory(placeLoc);
			// Check for break inside of town
			if(territory instanceof KonTown) {
				KonTown town = (KonTown) territory;
				// Check for break inside of town's monument
				if(town.getMonument().isLocInside(placeLoc)) {
					ChatUtil.printDebug("EVENT: Hanging placed inside of monument");
					KonPlayer player = konquest.getPlayerManager().getPlayer(event.getPlayer());
					ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedProtectionColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
					event.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler()
    public void onHangingBreak(HangingBreakByEntityEvent event) {
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
}
