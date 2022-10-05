package konquest.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.maxgamer.quickshop.event.ShopPreCreateEvent;
import org.maxgamer.quickshop.event.ShopPurchaseEvent;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.manager.IntegrationManager;
import konquest.manager.PlayerManager;
import konquest.manager.TerritoryManager;
import konquest.model.KonCamp;
import konquest.model.KonCapital;
import konquest.model.KonPlayer;
import konquest.model.KonTerritory;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class QuickShopListener implements Listener{

	private Konquest konquest;
	private PlayerManager playerManager;
	private TerritoryManager territoryManager;
	private IntegrationManager integrationManager;
	
	public QuickShopListener(KonquestPlugin plugin) {
		this.konquest = plugin.getKonquestInstance();
		this.playerManager = konquest.getPlayerManager();
		this.territoryManager = konquest.getTerritoryManager();
		this.integrationManager = konquest.getIntegrationManager();
	}
	
	//TODO Implement message broker that prevents spam (double events) OR fix QuickShop from firing this event twice
	/**
	 * Allow players to create shops only in friendly towns or their camp
	 * @param event
	 */
	@EventHandler(priority = EventPriority.NORMAL)
    public void onShopPreCreate(ShopPreCreateEvent event) {
//		if(konquest.isWorldIgnored(event.getLocation().getWorld())) {
//			return;
//		}
		if(integrationManager.getQuickShop().isEnabled()) {
			//ChatUtil.printDebug("ShopPreCreateEvent: player "+event.getPlayer().getName()+" at "+event.getLocation().toString());
			if(konquest.isWorldIgnored(event.getLocation())) {
				return;
			}
			// Bypass all checks for admins in bypass mode
			KonPlayer player = playerManager.getPlayer(event.getPlayer());
			if(player != null && !player.isAdminBypassActive()) {
				if(territoryManager.isChunkClaimed(event.getLocation())) {
					//ChatUtil.printDebug("...checking claimed chunk");
					KonTerritory territory = territoryManager.getChunkTerritory(event.getLocation());
					if(territory instanceof KonTown) {
						if(!player.getKingdom().equals(territory.getKingdom())) {
							//ChatUtil.printDebug("...chunk is enemy town");
							//ChatUtil.sendError(event.getPlayer(), "Cannot create a shop in enemy towns!");
							ChatUtil.sendError(event.getPlayer(), MessagePath.QUICKSHOP_ERROR_ENEMY_USE.getMessage());
							event.setCancelled(true);
							return;
						}
						if(((KonTown) territory).isLocInsideMonumentProtectionArea(event.getLocation())) {
							//ChatUtil.printDebug("...chunk is monument");
							//ChatUtil.sendError(event.getPlayer(), "Cannot create a shop inside monument!");
							ChatUtil.sendError(event.getPlayer(), MessagePath.QUICKSHOP_ERROR_MONUMENT.getMessage());
							event.setCancelled(true);
							return;
						}
					} 
					if(territory instanceof KonCamp && !((KonCamp)territory).isPlayerOwner(event.getPlayer())) {
						//ChatUtil.printDebug("...player is not camp owner");
						//ChatUtil.sendError(event.getPlayer(), "Can only create shops in your own camp!");
						ChatUtil.sendError(event.getPlayer(), MessagePath.QUICKSHOP_ERROR_CAMP.getMessage());
						event.setCancelled(true);
						return;
					}
					if(territory instanceof KonCapital) {
						//ChatUtil.printDebug("...chunk is capital");
						//ChatUtil.sendError(event.getPlayer(), "Cannot create a shop here!");
						ChatUtil.sendError(event.getPlayer(), MessagePath.QUICKSHOP_ERROR_FAIL.getMessage());
						event.setCancelled(true);
						return;
					}
				} else {
					//ChatUtil.printDebug("...chunk is not claimed, block");
					//ChatUtil.sendError(event.getPlayer(), "Cannot create shops in the wild!");
					ChatUtil.sendError(event.getPlayer(), MessagePath.QUICKSHOP_ERROR_WILD.getMessage());
					event.setCancelled(true);
					return;
				}
			}
			
		}
	}
	
	/**
	 * Protect shops from being used by enemies
	 * @param event
	 */
	@EventHandler(priority = EventPriority.NORMAL)
    public void onShopPurchase(ShopPurchaseEvent event) {
		if(konquest.isWorldIgnored(event.getShop().getLocation())) {
			return;
		}
		if(integrationManager.getQuickShop().isEnabled()) {
			//ChatUtil.printDebug("ShopPurchaseEvent: owner "+Bukkit.getOfflinePlayer(event.getShop().getOwner()).getName()+" at "+event.getShop().getLocation());
		
			// Bypass all checks for admins in bypass mode
			Player purchaser = Bukkit.getPlayer(event.getPurchaser());
			KonPlayer player = playerManager.getPlayer(purchaser);
			if(player != null && !player.isAdminBypassActive()) {
				Location shopLoc = event.getShop().getLocation();
				if(territoryManager.isChunkClaimed(shopLoc)) {
					KonTerritory territory = territoryManager.getChunkTerritory(shopLoc);
					if(!player.getKingdom().equals(territory.getKingdom())) {
						//ChatUtil.sendError(event.getPlayer(), "Cannot use enemy shops!");
						ChatUtil.sendError(purchaser, MessagePath.QUICKSHOP_ERROR_ENEMY_USE.getMessage());
						event.setCancelled(true);
						return;
					}
				}
			}
		
		}
	}
	

	/**
	 * Protects shops when they are located in owner's friendly town from everyone except owner and bypassed admins
	 * @param event
	 */
	/*
	@EventHandler(priority = EventPriority.LOW)
    public void onShopBreak(BlockBreakEvent event) {
		if(integrationManager.isQuickShopEnabled()) {
			if(event.isCancelled()) {
				ChatUtil.printDebug("Failed to listen for QuickShop break, event was cancelled!");
				return;
			} else {
				// Check if the block is a shop chest
				Shop breakShop = QuickShopAPI.getShopAPI().getShopIncludeAttachedWithCaching(event.getBlock().getLocation());
				if(breakShop == null) {
					ChatUtil.printDebug("Failed to get QuickShop shop at broken block location "+event.getBlock().getLocation().toString());
					return;
				} else {
					// Found a broken shop, apply preventions
					KonPlayer player = playerManager.getPlayer(event.getPlayer());
					if(!player.isAdminBypassActive()) {
						
						if(kingdomManager.isChunkClaimed(event.getLocation().getChunk())) {
							
						}
						breakShop.
					}
						
					
				}
			}
		}
	}
	*/
	
}
