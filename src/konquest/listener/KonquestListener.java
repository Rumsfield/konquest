package konquest.listener;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.event.KonKingdomChangeEvent;
import konquest.event.KonquestEnterTerritoryEvent;
import konquest.event.KonquestMonumentDamageEvent;
import konquest.manager.KingdomManager;
import konquest.manager.PlayerManager;
import konquest.model.KonCamp;
import konquest.model.KonCapital;
import konquest.model.KonDirective;
import konquest.model.KonPlayer;
import konquest.model.KonRuin;
import konquest.model.KonStatsType;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;
import konquest.utility.Timer;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Effect;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;


public class KonquestListener implements Listener {

	private Konquest konquest;
	private KingdomManager kingdomManager;
	private PlayerManager playerManager;
	
	public KonquestListener(KonquestPlugin plugin) {
		this.konquest = plugin.getKonquestInstance();
		this.playerManager = konquest.getPlayerManager();
		this.kingdomManager = konquest.getKingdomManager();
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onKonquestEnterTerritory(KonquestEnterTerritoryEvent event) {
		//ChatUtil.printDebug("EVENT: Player "+event.getPlayer().getBukkitPlayer().getDisplayName()+" entered new territory");
		
		// When territory is a capital
		if(event.getTerritory() instanceof KonCapital) {
			// Optionally prevent players from entering
			boolean isEnemyAllowedDenied = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.no_enemy_enter");
			boolean isAdminBypassMode = event.getPlayer().isAdminBypassActive();
			if(!isAdminBypassMode && isEnemyAllowedDenied && !event.getPlayer().getKingdom().equals(event.getTerritory().getKingdom())) {
				// When Player is in a vehicle, reverse the velocity and eject
				if(event.getPlayer().getBukkitPlayer().isInsideVehicle()) {
					Vehicle vehicle = (Vehicle) event.getPlayer().getBukkitPlayer().getVehicle();
					vehicle.setVelocity(vehicle.getVelocity().multiply(-4));
					vehicle.eject();
				}
				// Cancel the movement
				//ChatUtil.sendNotice(event.getPlayer().getBukkitPlayer(), "Cannot enter enemy Kingdom Capitals"+adminText, ChatColor.DARK_RED);
				ChatUtil.sendError(event.getPlayer().getBukkitPlayer(), MessagePath.PROTECTION_ERROR_CAPITAL_ENTER.getMessage());
				if(event.getPlayer().getBukkitPlayer().hasPermission("konquest.command.admin")) {
					ChatUtil.sendNotice(event.getPlayer().getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_IGNORE.getMessage());
				}
				event.setCancelled(true);
				return;
			}
		}
		
		// When territory is a town
		if(event.getTerritory() instanceof KonTown) {
			KonTown town = (KonTown)event.getTerritory();

			// For an enemy player...
			if(!event.getPlayer().isAdminBypassActive() &&
					!event.getPlayer().getKingdom().equals(event.getTerritory().getKingdom()) &&
					!event.getPlayer().getKingdom().isPeaceful() ) {
				
				// If the town and enemy guilds share an armistice
				if(!konquest.getGuildManager().isArmistice(event.getPlayer(), town)) {
					// Attempt to start a raid alert
					town.sendRaidAlert();
					// Apply town nerfs
					kingdomManager.applyTownNerf(event.getPlayer(), town);
				}
				
			} else {
				// Players entering friendly towns...
				kingdomManager.clearTownNerf(event.getPlayer());
			}
			/*
			// Attempt to modify town hearts
			if(!event.getPlayer().isAdminBypassActive()) {
				kingdomManager.clearTownHearts(event.getPlayer());
				kingdomManager.applyTownHearts(event.getPlayer(), town);
			}
			*/
			/*
			if(!event.getPlayer().isAdminBypassActive() && event.getPlayer().getKingdom().equals(event.getTerritory().getKingdom())) {
				// Apply town hearts
				kingdomManager.applyTownHearts(event.getPlayer(), town);
			} else {
				kingdomManager.clearTownHearts(event.getPlayer());
			}
			*/
			
			/*
			// Force all nearby (within 4 chunks of town center) Iron Golems to attack the enemy
			//TODO Make golems attack nearest enemy?
			//TODO Add conditions to stop golems from targeting enemies
			boolean isGolemAttackEnemies = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.golem_attack_enemies");
			if(isGolemAttackEnemies && !event.getPlayer().getKingdom().equals(event.getTerritory().getKingdom())) {
				Location centerLoc = event.getTerritory().getCenterLoc();
				for(Entity e : centerLoc.getWorld().getNearbyEntities(centerLoc,64,64,64,(e) -> e.getType() == EntityType.IRON_GOLEM)) {
					IronGolem golem = (IronGolem)e;
					golem.setTarget(event.getPlayer().getBukkitPlayer());
				}
			}*/
		} else {
			// Territory other than Town
			kingdomManager.clearTownNerf(event.getPlayer());
			//kingdomManager.clearTownHearts(event.getPlayer());
		}
		
		// When territory is a camp
		if(event.getTerritory() instanceof KonCamp) {
			KonCamp camp = (KonCamp)event.getTerritory();
			// Attempt to start a raid alert
			if(!camp.isRaidAlertDisabled() && !event.getPlayer().isAdminBypassActive() && 
					!event.getPlayer().getKingdom().isPeaceful()) {
				// Verify online player
				if(camp.isOwnerOnline() && camp.getOwner() instanceof Player) {
					Player bukkitPlayer = (Player)camp.getOwner();
					boolean isMember = false;
					if(konquest.getCampManager().isCampGrouped(camp)) {
						isMember = konquest.getCampManager().getCampGroup(camp).isPlayerMember(event.getPlayer().getBukkitPlayer());
					}
					// Alert the camp owner if player is not a group member and online
					if(playerManager.isPlayer(bukkitPlayer) && !isMember && !event.getPlayer().getBukkitPlayer().getUniqueId().equals(camp.getOwner().getUniqueId())) {
						KonPlayer ownerPlayer = playerManager.getPlayer(bukkitPlayer);
						//ChatUtil.sendNotice((Player)camp.getOwner(), "Enemy spotted in "+event.getTerritory().getName()+", use \"/k travel camp\" to defend!", ChatColor.DARK_RED);
						ChatUtil.sendNotice(bukkitPlayer, MessagePath.PROTECTION_NOTICE_RAID.getMessage(event.getTerritory().getName(),"camp"),ChatColor.DARK_RED);
						ChatUtil.sendKonPriorityTitle(ownerPlayer, ChatColor.DARK_RED+MessagePath.PROTECTION_NOTICE_RAID_ALERT.getMessage(), ChatColor.DARK_RED+""+event.getTerritory().getName(), 60, 1, 10);
						// Start Raid Alert disable timer for target town
						int raidAlertTimeSeconds = konquest.getConfigManager().getConfig("core").getInt("core.towns.raid_alert_cooldown");
						ChatUtil.printDebug("Starting raid alert timer for "+raidAlertTimeSeconds+" seconds");
						Timer raidAlertTimer = camp.getRaidAlertTimer();
						camp.setIsRaidAlertDisabled(true);
						raidAlertTimer.stopTimer();
						raidAlertTimer.setTime(raidAlertTimeSeconds);
						raidAlertTimer.startTimer();
					}
				}
			}
		}
		
		// When territory is a ruin
		if(event.getTerritory() instanceof KonRuin) {
			KonRuin ruin = (KonRuin)event.getTerritory();
			// Spawn all ruin golems
			ruin.spawnAllGolems();
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onKonquestMonumentDamage(KonquestMonumentDamageEvent event) {
		//ChatUtil.printDebug("EVENT: Monument damaged in town: "+event.getTerritory().getName()+", kingdom: "+event.getTerritory().getKingdom().getName());
		
		if(!(event.getTerritory() instanceof KonTown)) {
			ChatUtil.printDebug("Error in onKonquestMonumentDamage Event Handler, event passed territory that is not a Town");
			return;
		}
		KonTown town = (KonTown) event.getTerritory();
		
		// Evaluate for critical strikes
		if(event.getBlockEvent().getBlock().getType().equals(konquest.getKingdomManager().getTownCriticalBlock())) {
			// Critical block has been destroyed
			ChatUtil.printDebug("Critical strike on Monument in Town "+event.getTerritory().getName());
			town.getMonument().addCriticalHit();
			int maxCriticalhits = konquest.getConfigManager().getConfig("core").getInt("core.monuments.destroy_amount");
			
			// Update bar progress
			double progress = (double)(maxCriticalhits - town.getMonument().getCriticalHits()) / (double)maxCriticalhits;
			town.setBarProgress(progress);
			
			// Evaluate town capture conditions
			if(town.getMonument().getCriticalHits() >= maxCriticalhits) {
				// The Town is at critical max, conquer or destroy
				if(event.getPlayer().isBarbarian()) {
					// Destroy the town when the enemy is a barbarian
					String townName = town.getName();
					ArrayList<KonPlayer> monumentPlayers = new ArrayList<KonPlayer>();
					ArrayList<KonPlayer> townLocPlayers = new ArrayList<KonPlayer>();
					for(KonPlayer player : playerManager.getPlayersOnline()) {
						if(town.isLocInsideCenterChunk(player.getBukkitPlayer().getLocation())) {
							monumentPlayers.add(player);
						}
						if(town.isLocInside(player.getBukkitPlayer().getLocation())) {
							townLocPlayers.add(player);
						}
					}
					int x = event.getTerritory().getCenterLoc().getBlockX();
					int y = event.getTerritory().getCenterLoc().getBlockY();
					int z = event.getTerritory().getCenterLoc().getBlockZ();
					Timer townMonumentTimer = town.getMonumentTimer();
					if(kingdomManager.removeTown(town.getName(), town.getKingdom().getName())) {
						// Town is removed, no longer exists
						//ChatUtil.sendNotice(event.getPlayer().getBukkitPlayer(), "You have destroyed "+townName+" for the glory of the Barbarian horde!");
						ChatUtil.sendNotice(event.getPlayer().getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_DESTROY.getMessage(townName));
						for(KonPlayer player : monumentPlayers) {
							player.getBukkitPlayer().teleport(konquest.getSafeRandomCenteredLocation(town.getCenterLoc(), 2));
							//ChatUtil.printDebug("Effect data is: "+Effect.ANVIL_LAND.getData().getName()+", "+Effect.ANVIL_LAND.getData().toString());
							player.getBukkitPlayer().playEffect(player.getBukkitPlayer().getLocation(), Effect.ANVIL_LAND, null);
						}
						for(KonPlayer player : townLocPlayers) {
							// Clear mob targets for all players within the old town
							player.clearAllMobAttackers();
							// Update particle border renders for nearby players
							kingdomManager.updatePlayerBorderParticles(player);
						}
						// Update directive progress
						konquest.getDirectiveManager().updateDirectiveProgress(event.getPlayer(), KonDirective.CAPTURE_TOWN);
						// Broadcast to Dynmap
						konquest.getMapHandler().postDynmapBroadcast(MessagePath.PROTECTION_NOTICE_RAZE.getMessage(townName)+" ("+x+","+y+","+z+")");
					} else {
						ChatUtil.printDebug("Problem destroying Town "+event.getTerritory().getName()+" in Kingdom "+event.getTerritory().getKingdom().getName()+" for a barbarian raider "+event.getPlayer().getBukkitPlayer().getName());
					}
					// Stop the town monument timer
					ChatUtil.printDebug("Stopping monument timer with taskID "+townMonumentTimer.getTaskID());
					townMonumentTimer.stopTimer();
				} else {
					// Conquer the town for the enemy player's kingdom
					if(kingdomManager.captureTownForPlayer(town.getName(), town.getKingdom().getName(), event.getPlayer())) {
						// Alert all players of original Kingdom
						for(KonPlayer player : playerManager.getPlayersInKingdom(event.getTerritory().getKingdom().getName())) {
							//ChatUtil.sendNotice(player.getBukkitPlayer(), "The Town "+event.getTerritory().getName()+" has been conquered!", ChatColor.DARK_RED);
							ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_CONQUER.getMessage(event.getTerritory().getName()), ChatColor.DARK_RED);
						}
						ChatUtil.printDebug("Monument conversion in Town "+event.getTerritory().getName());
						//ChatUtil.sendNotice(event.getPlayer().getBukkitPlayer(), "You have conquered "+town.getName()+" for the conquest of "+event.getPlayer().getKingdom().getName()+"!");
						ChatUtil.sendNotice(event.getPlayer().getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_CAPTURE.getMessage(town.getName(),event.getPlayer().getKingdom().getName()));
						// Start Capture disable timer for target town
						int townCaptureTimeSeconds = konquest.getConfigManager().getConfig("core").getInt("core.towns.capture_cooldown");
						Timer captureTimer = town.getCaptureTimer();
						town.setIsCaptureDisabled(true);
						captureTimer.stopTimer();
						captureTimer.setTime(townCaptureTimeSeconds);
						captureTimer.startTimer();
						ChatUtil.printDebug("Starting capture timer for "+townCaptureTimeSeconds+" seconds with taskID "+captureTimer.getTaskID());
						// For all online players...
						for(KonPlayer player : playerManager.getPlayersOnline()) {
							// Teleport all players inside center chunk to new spawn location
							if(town.isLocInsideCenterChunk(player.getBukkitPlayer().getLocation())) {
								player.getBukkitPlayer().teleport(konquest.getSafeRandomCenteredLocation(town.getCenterLoc(), 2));
								player.getBukkitPlayer().playEffect(player.getBukkitPlayer().getLocation(), Effect.ANVIL_LAND, null);
							}
							// Remove mob targets
							if(town.isLocInside(player.getBukkitPlayer().getLocation())) {
								player.clearAllMobAttackers();
							}
							// Update particle border renders for nearby players
							for(Chunk chunk : konquest.getAreaChunks(player.getBukkitPlayer().getLocation(), 2)) {
								if(town.hasChunk(chunk)) {
									kingdomManager.updatePlayerBorderParticles(player);
									break;
								}
							}
							
						}
						// Update directive progress
						konquest.getDirectiveManager().updateDirectiveProgress(event.getPlayer(), KonDirective.CAPTURE_TOWN);
						// Update stat
						konquest.getAccomplishmentManager().modifyPlayerStat(event.getPlayer(),KonStatsType.CAPTURES,1);
						// Broadcast to Dynmap
						int x = event.getTerritory().getCenterLoc().getBlockX();
						int y = event.getTerritory().getCenterLoc().getBlockY();
						int z = event.getTerritory().getCenterLoc().getBlockZ();
						konquest.getMapHandler().postDynmapBroadcast(MessagePath.PROTECTION_NOTICE_CONQUER.getMessage(event.getTerritory().getName())+" ("+x+","+y+","+z+")");
					} else {
						ChatUtil.printDebug("Problem converting Town "+event.getTerritory().getName()+" from Kingdom "+event.getTerritory().getKingdom().getName()+" to "+event.getPlayer().getKingdom().getName());
						// If, for example, a player in the Barbarians default kingdom captured the monument
						town.refreshMonument();
					}
					// Stop the town monument timer
					ChatUtil.printDebug("Stopping monument timer with taskID "+town.getMonumentTimer().getTaskID());
					town.getMonumentTimer().stopTimer();
					// Reset the town MonumentBar
					//town.resetBar();
					town.setAttacked(false);
					town.setBarProgress(1.0);
					town.updateBar();
				}
			} else {
				// Town has not yet been captured
				int remainingHits = maxCriticalhits - town.getMonument().getCriticalHits();
				int defendReward = konquest.getConfigManager().getConfig("core").getInt("core.favor.rewards.defend_raid");
				//ChatUtil.sendNotice(event.getPlayer().getBukkitPlayer(), "Critical Strike! "+remainingHits+" Critical Blocks remain.");
				ChatUtil.sendNotice(event.getPlayer().getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_CRITICAL.getMessage(remainingHits));
				konquest.getAccomplishmentManager().modifyPlayerStat(event.getPlayer(),KonStatsType.CRITICALS,1);
				
				// Alert all players of enemy Kingdom when the first critical block is broken
				if(town.getMonument().getCriticalHits() == 1) {
					for(KonPlayer player : playerManager.getPlayersInKingdom(event.getTerritory().getKingdom().getName())) {
						//ChatUtil.sendKonPriorityTitle(player, ChatColor.DARK_RED+"Raid Alert!", ChatColor.DARK_RED+town.getName()+" is being conquered!", 60, 1, 10 );
						//ChatUtil.sendNotice(player.getBukkitPlayer(), ChatColor.DARK_RED+"Use "+ChatColor.AQUA+"/k travel "+event.getTerritory().getName()+ChatColor.DARK_RED+" to defend and receive "+ChatColor.DARK_GREEN+defendReward+" Favor!");
						ChatUtil.sendKonPriorityTitle(player, ChatColor.DARK_RED+MessagePath.PROTECTION_NOTICE_RAID_ALERT.getMessage(), ChatColor.DARK_RED+""+event.getTerritory().getName(), 60, 1, 10);
						ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_RAID_CAPTURE_1.getMessage(event.getTerritory().getName(),event.getTerritory().getName(),defendReward),ChatColor.DARK_RED);
					}
				}
				
				// Alert all players of enemy Kingdom when half of critical blocks are broken
				if(town.getMonument().getCriticalHits() == maxCriticalhits/2) {
					for(KonPlayer player : playerManager.getPlayersInKingdom(event.getTerritory().getKingdom().getName())) {
						//ChatUtil.sendKonPriorityTitle(player, ChatColor.DARK_RED+"Raid Alert!", ChatColor.DARK_RED+town.getName()+" is at half strength!", 60, 1, 10 );
						//ChatUtil.sendNotice(player.getBukkitPlayer(), ChatColor.DARK_RED+"Use "+ChatColor.AQUA+"/k travel "+event.getTerritory().getName()+ChatColor.DARK_RED+" to defend and receive "+ChatColor.DARK_GREEN+defendReward+" Favor!");
						ChatUtil.sendKonPriorityTitle(player, ChatColor.DARK_RED+MessagePath.PROTECTION_NOTICE_RAID_ALERT.getMessage(), ChatColor.DARK_RED+""+event.getTerritory().getName(), 60, 1, 10);
						ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_RAID_CAPTURE_2.getMessage(event.getTerritory().getName(),event.getTerritory().getName(),defendReward),ChatColor.DARK_RED);
					}
				}
				
				// Alert all players of enemy Kingdom when all but 1 critical blocks are broken
				if(town.getMonument().getCriticalHits() == maxCriticalhits-1) {
					for(KonPlayer player : playerManager.getPlayersInKingdom(event.getTerritory().getKingdom().getName())) {
						//ChatUtil.sendKonPriorityTitle(player, ChatColor.DARK_RED+"Raid Alert!", ChatColor.DARK_RED+town.getName()+" is in critical condition!", 60, 1, 10 );
						//ChatUtil.sendNotice(player.getBukkitPlayer(), ChatColor.DARK_RED+"Use "+ChatColor.AQUA+"/k travel "+event.getTerritory().getName()+ChatColor.DARK_RED+" to defend and receive "+ChatColor.DARK_GREEN+defendReward+" Favor!");
						ChatUtil.sendKonPriorityTitle(player, ChatColor.DARK_RED+MessagePath.PROTECTION_NOTICE_RAID_ALERT.getMessage(), ChatColor.DARK_RED+""+event.getTerritory().getName(), 60, 1, 10);
						ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_RAID_CAPTURE_3.getMessage(event.getTerritory().getName(),event.getTerritory().getName(),defendReward),ChatColor.DARK_RED);
					}
				}
			}
		}
	}
	
	/*
	 * General Konquest Events
	 */
	
	/**
	 * Handles when a player is assigned to a kingdom or becomes a barbarian
	 * @param event
	 */
	@EventHandler(priority = EventPriority.NORMAL)
    public void onKingdomChange(KonKingdomChangeEvent event) {
		//TODO: something
	}
	
}
