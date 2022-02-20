package konquest.listener;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.api.event.KonquestEnterTerritoryEvent;
import konquest.api.event.KonquestKingdomChangeEvent;
import konquest.api.event.KonquestMonumentDamageEvent;
import konquest.manager.KingdomManager;
import konquest.manager.PlayerManager;
import konquest.model.KonCamp;
import konquest.model.KonCapital;
import konquest.model.KonDirective;
import konquest.model.KonPlayer;
import konquest.model.KonRuin;
import konquest.model.KonStatsType;
import konquest.model.KonTerritory;
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
		
		// Qualify event fields
		KonPlayer player = null;
		if(event.getPlayer() instanceof KonPlayer) {
			player = (KonPlayer)event.getPlayer();
		} else {
			return;
		}
		KonTerritory territory = null;
		if(event.getTerritory() instanceof KonTerritory) {
			territory = (KonTerritory)event.getTerritory();
		} else {
			return;
		}
		
		// When territory is a capital
		if(territory instanceof KonCapital) {
			// Optionally prevent players from entering
			boolean isEnemyAllowedDenied = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.no_enemy_enter");
			boolean isAdminBypassMode = player.isAdminBypassActive();
			if(!isAdminBypassMode && isEnemyAllowedDenied && !player.getKingdom().equals(territory.getKingdom())) {
				// When Player is in a vehicle, reverse the velocity and eject
				if(player.getBukkitPlayer().isInsideVehicle()) {
					Vehicle vehicle = (Vehicle) player.getBukkitPlayer().getVehicle();
					vehicle.setVelocity(vehicle.getVelocity().multiply(-4));
					vehicle.eject();
				}
				// Cancel the movement
				//ChatUtil.sendNotice(event.getPlayer().getBukkitPlayer(), "Cannot enter enemy Kingdom Capitals"+adminText, ChatColor.DARK_RED);
				ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_CAPITAL_ENTER.getMessage());
				if(player.getBukkitPlayer().hasPermission("konquest.command.admin")) {
					ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_IGNORE.getMessage());
				}
				event.setCancelled(true);
				return;
			}
		}
		
		// When territory is a town
		if(territory instanceof KonTown) {
			KonTown town = (KonTown)territory;

			// For an enemy player...
			if(!player.isAdminBypassActive() &&
					!player.getKingdom().equals(territory.getKingdom()) &&
					!player.getKingdom().isPeaceful() ) {
				
				// If the town and enemy guilds share an armistice
				if(!konquest.getGuildManager().isArmistice(player, town)) {
					// Attempt to start a raid alert
					town.sendRaidAlert();
					// Apply town nerfs
					kingdomManager.applyTownNerf(player, town);
				}
				
			} else {
				// Players entering friendly towns...
				kingdomManager.clearTownNerf(player);
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
			kingdomManager.clearTownNerf(player);
			//kingdomManager.clearTownHearts(event.getPlayer());
		}
		
		// When territory is a camp
		if(territory instanceof KonCamp) {
			KonCamp camp = (KonCamp)territory;
			// Attempt to start a raid alert
			if(!camp.isRaidAlertDisabled() && !player.isAdminBypassActive() && 
					!player.getKingdom().isPeaceful()) {
				// Verify online player
				if(camp.isOwnerOnline() && camp.getOwner() instanceof Player) {
					Player bukkitPlayer = (Player)camp.getOwner();
					boolean isMember = false;
					if(konquest.getCampManager().isCampGrouped(camp)) {
						isMember = konquest.getCampManager().getCampGroup(camp).isPlayerMember(player.getBukkitPlayer());
					}
					// Alert the camp owner if player is not a group member and online
					if(playerManager.isOnlinePlayer(bukkitPlayer) && !isMember && !player.getBukkitPlayer().getUniqueId().equals(camp.getOwner().getUniqueId())) {
						KonPlayer ownerPlayer = playerManager.getPlayer(bukkitPlayer);
						//ChatUtil.sendNotice((Player)camp.getOwner(), "Enemy spotted in "+event.getTerritory().getName()+", use \"/k travel camp\" to defend!", ChatColor.DARK_RED);
						ChatUtil.sendNotice(bukkitPlayer, MessagePath.PROTECTION_NOTICE_RAID.getMessage(territory.getName(),"camp"),ChatColor.DARK_RED);
						ChatUtil.sendKonPriorityTitle(ownerPlayer, ChatColor.DARK_RED+MessagePath.PROTECTION_NOTICE_RAID_ALERT.getMessage(), ChatColor.DARK_RED+""+territory.getName(), 60, 1, 10);
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
		if(territory instanceof KonRuin) {
			KonRuin ruin = (KonRuin)territory;
			// Spawn all ruin golems
			ruin.spawnAllGolems();
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onKonquestMonumentDamage(KonquestMonumentDamageEvent event) {
		//ChatUtil.printDebug("EVENT: Monument damaged in town: "+event.getTerritory().getName()+", kingdom: "+event.getTerritory().getKingdom().getName());
		
		// Qualify event fields
		KonPlayer player = null;
		if(event.getPlayer() instanceof KonPlayer) {
			player = (KonPlayer)event.getPlayer();
		} else {
			return;
		}
		KonTerritory territory = null;
		if(event.getTerritory() instanceof KonTerritory) {
			territory = (KonTerritory)event.getTerritory();
		} else {
			return;
		}
				
		if(!(territory instanceof KonTown)) {
			ChatUtil.printDebug("Error in onKonquestMonumentDamage Event Handler, event passed territory that is not a Town");
			return;
		}
		KonTown town = (KonTown) territory;
		
		// Evaluate for critical strikes
		if(event.getBlockEvent().getBlock().getType().equals(konquest.getKingdomManager().getTownCriticalBlock())) {
			// Critical block has been destroyed
			ChatUtil.printDebug("Critical strike on Monument in Town "+territory.getName());
			town.getMonument().addCriticalHit();
			//int maxCriticalhits = konquest.getConfigManager().getConfig("core").getInt("core.monuments.destroy_amount");
			int maxCriticalhits = konquest.getKingdomManager().getMaxCriticalHits();
			
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
					for(KonPlayer onlinePlayer : playerManager.getPlayersOnline()) {
						if(town.isLocInsideCenterChunk(onlinePlayer.getBukkitPlayer().getLocation())) {
							monumentPlayers.add(onlinePlayer);
						}
						if(town.isLocInside(onlinePlayer.getBukkitPlayer().getLocation())) {
							townLocPlayers.add(onlinePlayer);
						}
					}
					int x = territory.getCenterLoc().getBlockX();
					int y = territory.getCenterLoc().getBlockY();
					int z = territory.getCenterLoc().getBlockZ();
					Timer townMonumentTimer = town.getMonumentTimer();
					if(kingdomManager.removeTown(town.getName(), town.getKingdom().getName())) {
						// Town is removed, no longer exists
						//ChatUtil.sendNotice(event.getPlayer().getBukkitPlayer(), "You have destroyed "+townName+" for the glory of the Barbarian horde!");
						ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_DESTROY.getMessage(townName));
						for(KonPlayer monPlayer : monumentPlayers) {
							monPlayer.getBukkitPlayer().teleport(konquest.getSafeRandomCenteredLocation(town.getCenterLoc(), 2));
							//ChatUtil.printDebug("Effect data is: "+Effect.ANVIL_LAND.getData().getName()+", "+Effect.ANVIL_LAND.getData().toString());
							monPlayer.getBukkitPlayer().playEffect(monPlayer.getBukkitPlayer().getLocation(), Effect.ANVIL_LAND, null);
						}
						for(KonPlayer locPlayer : townLocPlayers) {
							// Clear mob targets for all players within the old town
							locPlayer.clearAllMobAttackers();
							// Update particle border renders for nearby players
							kingdomManager.updatePlayerBorderParticles(locPlayer);
						}
						// Update directive progress
						konquest.getDirectiveManager().updateDirectiveProgress(player, KonDirective.CAPTURE_TOWN);
						// Broadcast to Dynmap
						konquest.getMapHandler().postDynmapBroadcast(MessagePath.PROTECTION_NOTICE_RAZE.getMessage(townName)+" ("+x+","+y+","+z+")");
					} else {
						ChatUtil.printDebug("Problem destroying Town "+territory.getName()+" in Kingdom "+territory.getKingdom().getName()+" for a barbarian raider "+player.getBukkitPlayer().getName());
					}
					// Stop the town monument timer
					ChatUtil.printDebug("Stopping monument timer with taskID "+townMonumentTimer.getTaskID());
					townMonumentTimer.stopTimer();
				} else {
					// Conquer the town for the enemy player's kingdom
					if(kingdomManager.captureTownForPlayer(town.getName(), town.getKingdom().getName(), player)) {
						// Alert all players of original Kingdom
						for(KonPlayer kingdomPlayer : playerManager.getPlayersInKingdom(territory.getKingdom().getName())) {
							//ChatUtil.sendNotice(player.getBukkitPlayer(), "The Town "+event.getTerritory().getName()+" has been conquered!", ChatColor.DARK_RED);
							ChatUtil.sendNotice(kingdomPlayer.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_CONQUER.getMessage(territory.getName()), ChatColor.DARK_RED);
						}
						ChatUtil.printDebug("Monument conversion in Town "+territory.getName());
						//ChatUtil.sendNotice(event.getPlayer().getBukkitPlayer(), "You have conquered "+town.getName()+" for the conquest of "+event.getPlayer().getKingdom().getName()+"!");
						ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_CAPTURE.getMessage(town.getName(),player.getKingdom().getName()));
						// Start Capture disable timer for target town
						int townCaptureTimeSeconds = konquest.getConfigManager().getConfig("core").getInt("core.towns.capture_cooldown");
						Timer captureTimer = town.getCaptureTimer();
						town.setIsCaptureDisabled(true);
						captureTimer.stopTimer();
						captureTimer.setTime(townCaptureTimeSeconds);
						captureTimer.startTimer();
						ChatUtil.printDebug("Starting capture timer for "+townCaptureTimeSeconds+" seconds with taskID "+captureTimer.getTaskID());
						// For all online players...
						for(KonPlayer onlinePlayer : playerManager.getPlayersOnline()) {
							// Teleport all players inside center chunk to new spawn location
							if(town.isLocInsideCenterChunk(onlinePlayer.getBukkitPlayer().getLocation())) {
								onlinePlayer.getBukkitPlayer().teleport(konquest.getSafeRandomCenteredLocation(town.getCenterLoc(), 2));
								onlinePlayer.getBukkitPlayer().playEffect(onlinePlayer.getBukkitPlayer().getLocation(), Effect.ANVIL_LAND, null);
							}
							// Remove mob targets
							if(town.isLocInside(onlinePlayer.getBukkitPlayer().getLocation())) {
								onlinePlayer.clearAllMobAttackers();
							}
							// Update particle border renders for nearby players
							for(Chunk chunk : konquest.getAreaChunks(onlinePlayer.getBukkitPlayer().getLocation(), 2)) {
								if(town.hasChunk(chunk)) {
									kingdomManager.updatePlayerBorderParticles(onlinePlayer);
									break;
								}
							}
							
						}
						// Update directive progress
						konquest.getDirectiveManager().updateDirectiveProgress(player, KonDirective.CAPTURE_TOWN);
						// Update stat
						konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.CAPTURES,1);
						// Broadcast to Dynmap
						int x = territory.getCenterLoc().getBlockX();
						int y = territory.getCenterLoc().getBlockY();
						int z = territory.getCenterLoc().getBlockZ();
						konquest.getMapHandler().postDynmapBroadcast(MessagePath.PROTECTION_NOTICE_CONQUER.getMessage(territory.getName())+" ("+x+","+y+","+z+")");
					} else {
						ChatUtil.printDebug("Problem converting Town "+territory.getName()+" from Kingdom "+territory.getKingdom().getName()+" to "+player.getKingdom().getName());
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
				ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_CRITICAL.getMessage(remainingHits));
				konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.CRITICALS,1);
				
				// Alert all players of enemy Kingdom when the first critical block is broken
				if(town.getMonument().getCriticalHits() == 1) {
					for(KonPlayer kingdomPlayer : playerManager.getPlayersInKingdom(territory.getKingdom().getName())) {
						ChatUtil.sendKonPriorityTitle(kingdomPlayer, ChatColor.DARK_RED+MessagePath.PROTECTION_NOTICE_RAID_ALERT.getMessage(), ChatColor.DARK_RED+""+territory.getName(), 60, 1, 10);
						ChatUtil.sendNotice(kingdomPlayer.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_RAID_CAPTURE_1.getMessage(territory.getName(),territory.getName(),defendReward),ChatColor.DARK_RED);
					}
				}
				
				// Alert all players of enemy Kingdom when half of critical blocks are broken
				if(town.getMonument().getCriticalHits() == maxCriticalhits/2) {
					for(KonPlayer kingdomPlayer : playerManager.getPlayersInKingdom(territory.getKingdom().getName())) {
						ChatUtil.sendKonPriorityTitle(kingdomPlayer, ChatColor.DARK_RED+MessagePath.PROTECTION_NOTICE_RAID_ALERT.getMessage(), ChatColor.DARK_RED+""+territory.getName(), 60, 1, 10);
						ChatUtil.sendNotice(kingdomPlayer.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_RAID_CAPTURE_2.getMessage(territory.getName(),territory.getName(),defendReward),ChatColor.DARK_RED);
					}
				}
				
				// Alert all players of enemy Kingdom when all but 1 critical blocks are broken
				if(town.getMonument().getCriticalHits() == maxCriticalhits-1) {
					for(KonPlayer kingdomPlayer : playerManager.getPlayersInKingdom(territory.getKingdom().getName())) {
						ChatUtil.sendKonPriorityTitle(kingdomPlayer, ChatColor.DARK_RED+MessagePath.PROTECTION_NOTICE_RAID_ALERT.getMessage(), ChatColor.DARK_RED+""+territory.getName(), 60, 1, 10);
						ChatUtil.sendNotice(kingdomPlayer.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_RAID_CAPTURE_3.getMessage(territory.getName(),territory.getName(),defendReward),ChatColor.DARK_RED);
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
    public void onKingdomChange(KonquestKingdomChangeEvent event) {
		//TODO: something
	}
	
}
