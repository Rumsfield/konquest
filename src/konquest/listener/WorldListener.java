package konquest.listener;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.model.KonCapital;
//import konquest.model.KonMapRenderer;
import konquest.model.KonTerritory;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;

import org.bukkit.Location;
//import org.bukkit.Bukkit;
//import org.bukkit.Chunk;
//import org.bukkit.Location;
//import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
//import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.event.world.PortalCreateEvent;
//import org.bukkit.map.MapRenderer;
//import org.bukkit.map.MapView;
import org.bukkit.event.world.StructureGrowEvent;

public class WorldListener  implements Listener {

	private KonquestPlugin konquestPlugin;
	private Konquest konquest;
	
	public WorldListener(KonquestPlugin plugin) {
		this.konquestPlugin = plugin;
		this.konquest = konquestPlugin.getKonquestInstance();
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onPortalCreate(PortalCreateEvent event) {
		ChatUtil.printDebug("EVENT: Portal is being created in "+event.getWorld().getName());
		
		if(konquest.isWorldValid(event.getWorld())) {
			// Check every block associated with the portal
			for(BlockState current_blockState : event.getBlocks()) {
				// Prevent portals from being created inside of Capitals and Monuments in the primary world
				if(konquest.getTerritoryManager().isChunkClaimed(current_blockState.getLocation())) {
					KonTerritory territory = konquest.getTerritoryManager().getChunkTerritory(current_blockState.getLocation());
					
					if(territory instanceof KonCapital) {
						boolean isPortalAllowed = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.capital_portal",false);
						if(!isPortalAllowed) {
							ChatUtil.printDebug("EVENT: Portal creation stopped inside of capital "+territory.getName());
							event.setCancelled(true);
							return;
						}
					}
					
					if(territory instanceof KonTown) {
						KonTown town = (KonTown) territory;
						if(town.isLocInsideCenterChunk(current_blockState.getLocation())) {
							ChatUtil.printDebug("EVENT: Portal creation stopped inside of town monument "+territory.getName());
							event.setCancelled(true);
							return;
						}
					}
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
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
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onStructureGrow(StructureGrowEvent event) {
		if(konquest.isWorldIgnored(event.getWorld())) {
			return;
		}
		// Prevent growth if any blocks are within monument
		for(BlockState bState : event.getBlocks()) {
			if(isLocInsideMonument(bState.getLocation())) {
				ChatUtil.printDebug("EVENT: Prevented structure growth within monument");
				event.setCancelled(true);
				return;
			}
		}
	}
	
	private boolean isLocInsideMonument(Location loc) {
		boolean result = false;
		if(konquest.getTerritoryManager().isChunkClaimed(loc)) {
			KonTerritory territory = konquest.getTerritoryManager().getChunkTerritory(loc);
			if(territory instanceof KonTown && ((KonTown) territory).isLocInsideMonumentProtectionArea(loc)) {
				result = true;
			}
		}
		return result;
	}
	
	/*
	@EventHandler(priority = EventPriority.NORMAL)
    public void onMapInitialize(MapInitializeEvent event) {
		final MapView view = event.getMap();
		for(MapRenderer ren : view.getRenderers()) {
			if(ren != null) {
				ChatUtil.printDebug("EVENT: Initializing map renderer, contextual "+ren.isContextual());
				ren.initialize(view);
			}
		}	
		World world = Bukkit.getServer().getWorld(konquest.getWorldName());
		Chunk centerChunk = world.getChunkAt(new Location(world,view.getCenterX(), 64, view.getCenterZ()));
		ChatUtil.printDebug("EVENT: Initializing map with center "+view.getCenterX()+","+view.getCenterZ()+", chunk "+centerChunk.toString());
		if(konquest.getKingdomManager().isChunkClaimed(centerChunk)) {
			KonTerritory territory = konquest.getKingdomManager().getChunkTerritory(centerChunk);
			view.addRenderer(new KonMapRenderer(territory.getName()));
			for(MapRenderer ren : view.getRenderers()) {
				if(ren != null) {
					ChatUtil.printDebug("EVENT: Found an existing map renderer, contextual "+ren.isContextual());
					ren.initialize(view);
				}
			}	
		} else {
			ChatUtil.printDebug("EVENT: Center chunk is not claimed!");
		}
	}
	*/
}
