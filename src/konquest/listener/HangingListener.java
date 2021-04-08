package konquest.listener;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.manager.KingdomManager;
import konquest.model.KonTerritory;
import konquest.model.KonTown;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakEvent;

public class HangingListener implements Listener {

	private KonquestPlugin konquestPlugin;
	private Konquest konquest;
	private KingdomManager kingdomManager;
	
	public HangingListener(KonquestPlugin plugin) {
		this.konquestPlugin = plugin;
		this.konquest = konquestPlugin.getKonquestInstance();
		this.kingdomManager = konquest.getKingdomManager();
	}
	
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onHangingBreak(HangingBreakEvent event) {
		//ChatUtil.printDebug("EVENT: Hanging Entity Broke");
		Location brakeLoc = event.getEntity().getLocation();
		if(kingdomManager.isChunkClaimed(brakeLoc.getChunk())) {
			KonTerritory territory = kingdomManager.getChunkTerritory(brakeLoc.getChunk());
			// Check for break inside of town
			if(territory instanceof KonTown) {
				KonTown town = (KonTown) territory;
				// Check for break inside of town's monument
				if(town.getMonument().isLocInside(brakeLoc)) {
					//ChatUtil.printDebug("EVENT: Hanging Broke inside of monument, do nothing");
					//event.getEntity().remove();
				}
			}
		}
    }
	/*
	@EventHandler(priority = EventPriority.NORMAL)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
		
		EntityType eType = event.getEntity().getType();
		EntityType rType = event.getRemover().getType();
		ChatUtil.printDebug("EVENT: Hanging "+eType.toString()+" broke by entity "+rType.toString());
	}
	*/
}
