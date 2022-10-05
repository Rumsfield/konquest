package konquest.listener;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.manager.TerritoryManager;
import konquest.model.KonPlayer;
import konquest.model.KonTerritory;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;

public class HangingListener implements Listener {

	private KonquestPlugin konquestPlugin;
	private Konquest konquest;
	private TerritoryManager territoryManager;
	
	public HangingListener(KonquestPlugin plugin) {
		this.konquestPlugin = plugin;
		this.konquest = konquestPlugin.getKonquestInstance();
		this.territoryManager = konquest.getTerritoryManager();
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
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
					ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
					event.setCancelled(true);
					return;
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
		//ChatUtil.printDebug("EVENT: Hanging Entity Broke");
		Location brakeLoc = event.getEntity().getLocation();
		if(territoryManager.isChunkClaimed(brakeLoc)) {
			KonTerritory territory = territoryManager.getChunkTerritory(brakeLoc);
			// Check for break inside of town
			if(territory instanceof KonTown) {
				KonTown town = (KonTown) territory;
				// Check for break inside of town's monument
				if(town.getMonument().isLocInside(brakeLoc)) {
					ChatUtil.printDebug("EVENT: Hanging broke inside of monument");
					if(event.getRemover() instanceof Player) {
						KonPlayer player = konquest.getPlayerManager().getPlayer((Player)event.getRemover());
						ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
					}
					event.setCancelled(true);
					return;
				}
			}
		}
    }
}
