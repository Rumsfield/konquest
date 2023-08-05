package com.github.rumsfield.konquest.listener;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;

public class WorldListener  implements Listener {

	private final Konquest konquest;
	
	public WorldListener(KonquestPlugin plugin) {
		this.konquest = plugin.getKonquestInstance();
	}
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onPortalCreate(PortalCreateEvent event) {
		ChatUtil.printDebug("EVENT: Portal is being created in "+event.getWorld().getName());
		if(event.isCancelled()) return;
		if(!konquest.isWorldValid(event.getWorld())) return;
		// Check every block associated with the portal
		for(BlockState current_blockState : event.getBlocks()) {
			// Prevent portals from being created inside of Capitals and Monuments in the primary world
			if(konquest.getTerritoryManager().isChunkClaimed(current_blockState.getLocation())) {
				KonTerritory territory = konquest.getTerritoryManager().getChunkTerritory(current_blockState.getLocation());
				// Property Flag Holders
				if(territory instanceof KonPropertyFlagHolder) {
					KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
					if(flagHolder.hasPropertyValue(KonPropertyFlag.PORTALS)) {
						if(!flagHolder.getPropertyValue(KonPropertyFlag.PORTALS)) {
							ChatUtil.printDebug("EVENT: Portal creation stopped inside of "+territory.getName());
							event.setCancelled(true);
							return;
						}
					}
				}
				// Check for portal inside monument/template
				if(isLocInsideMonument(current_blockState.getLocation())) {
					ChatUtil.printDebug("EVENT: Portal creation stopped inside of monument "+territory.getName());
					event.setCancelled(true);
					return;
				}
			}
		}

	}
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
		if(konquest.getTerritoryManager().isChunkClaimed(Konquest.toPoint(event.getChunk()), event.getWorld())) {
			KonTerritory territory = konquest.getTerritoryManager().getChunkTerritory(Konquest.toPoint(event.getChunk()), event.getWorld());
			if(territory instanceof KonTown) {
				KonTown town = (KonTown) territory;
				if(town.isChunkCenter(event.getChunk())) {
					ChatUtil.printDebug("EVENT: Loaded monument chunk of town "+town.getName());
					if(!town.getKingdom().isMonumentTemplateValid()) {
						ChatUtil.printDebug("Could not paste monument in town "+town.getName()+" while monument template is invalid");
					} else if(town.isAttacked()) {
						ChatUtil.printDebug("Could not paste monument in town "+town.getName()+" while under attack");
					} else {
						// Paste current monument template
						boolean status = town.reloadMonument();
						if(!status) {
							ChatUtil.printDebug("Failed to paste invalid monument template!");
						}
					}
				}
			}
		}
		konquest.applyQueuedTeleports(event.getChunk());
	}
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onStructureGrow(StructureGrowEvent event) {
		if(konquest.isWorldIgnored(event.getWorld())) return;
		// Prevent growth if any blocks are within monument
		for(BlockState bState : event.getBlocks()) {
			if(isLocInsideMonument(bState.getLocation())) {
				ChatUtil.printDebug("EVENT: Prevented structure growth within monument");
				event.setCancelled(true);
				return;
			}
		}
	}
	
	/*
	 * Check if the given block is inside any town/capital monument or monument template
	 */
	private boolean isLocInsideMonument(Location loc) {
		if(!konquest.getTerritoryManager().isChunkClaimed(loc)) return false;
		KonTerritory territory = konquest.getTerritoryManager().getChunkTerritory(loc);
		if(territory instanceof KonSanctuary && ((KonSanctuary) territory).isLocInsideTemplate(loc)) {
			return true;
		}
		return territory instanceof KonTown && ((KonTown) territory).isLocInsideMonumentProtectionArea(loc);
	}

}
