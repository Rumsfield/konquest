package konquest.listener;

import konquest.Konquest;
import konquest.KonquestPlugin;
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
//import konquest.manager.PlayerManager;
import konquest.utility.ChatUtil;
import konquest.utility.Timer;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
//import org.bukkit.Location;
import org.bukkit.Material;
//import org.bukkit.entity.Entity;
//import org.bukkit.entity.EntityType;
//import org.bukkit.entity.IronGolem;
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
				ChatUtil.sendNotice(event.getPlayer().getBukkitPlayer(), "Cannot enter enemy Kingdom Capitals", ChatColor.DARK_RED);
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
				// Attempt to start a raid alert
				if(!town.isRaidAlertDisabled()) {
					// Alert all players of enemy Kingdom
					for(KonPlayer player : playerManager.getPlayersInKingdom(event.getTerritory().getKingdom().getName())) {
						ChatUtil.sendNotice(player.getBukkitPlayer(), ChatColor.DARK_RED+"Enemy spotted in "+event.getTerritory().getName()+", use "+ChatColor.AQUA+"/k travel "+event.getTerritory().getName()+ChatColor.DARK_RED+" to defend!");
						ChatUtil.sendKonPriorityTitle(player, ChatColor.DARK_RED+"Raid Alert!", ChatColor.DARK_RED+""+event.getTerritory().getName(), 60, 1, 10);
					}
					// Start Raid Alert disable timer for target town
					int raidAlertTimeSeconds = konquest.getConfigManager().getConfig("core").getInt("core.towns.raid_alert_cooldown");
					ChatUtil.printDebug("Starting raid alert timer for "+raidAlertTimeSeconds+" seconds");
					Timer raidAlertTimer = town.getRaidAlertTimer();
					town.setIsRaidAlertDisabled(true);
					raidAlertTimer.stopTimer();
					raidAlertTimer.setTime(raidAlertTimeSeconds);
					raidAlertTimer.startTimer();
				}
				
				// Apply town nerfs
				kingdomManager.applyTownNerf(event.getPlayer(), town);
			} else {
				// Players entering friendly towns...
				kingdomManager.clearTownNerf(event.getPlayer());
			}
			
			// For a friendly player...
			if(!event.getPlayer().isAdminBypassActive() && event.getPlayer().getKingdom().equals(event.getTerritory().getKingdom())) {
				// Apply town hearts
				kingdomManager.applyTownHearts(event.getPlayer(), town);
			} else {
				kingdomManager.clearTownHearts(event.getPlayer());
			}
			
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
			kingdomManager.clearTownHearts(event.getPlayer());
		}
		
		// When territory is a camp
		// TODO check for allowable players, because some barbarians may be hostile and others may be friendly
		if(event.getTerritory() instanceof KonCamp) {
			KonCamp camp = (KonCamp)event.getTerritory();
			
			// Attempt to start a raid alert
			if(!camp.isRaidAlertDisabled() && !event.getPlayer().isAdminBypassActive() && 
					!event.getPlayer().getKingdom().isPeaceful()) {
				// Alert the camp owner if online
				if(camp.isOwnerOnline() && !event.getPlayer().getBukkitPlayer().getUniqueId().equals(camp.getOwner().getUniqueId())) {
					KonPlayer ownerPlayer = playerManager.getPlayer((Player)camp.getOwner());
					ChatUtil.sendNotice((Player)camp.getOwner(), "Enemy spotted in "+event.getTerritory().getName()+", use \"/k travel camp\" to defend!", ChatColor.DARK_RED);
					ChatUtil.sendKonTitle(ownerPlayer, ChatColor.DARK_RED+"Raid Alert!", ChatColor.DARK_RED+""+event.getTerritory().getName());
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
		
		// When territory is a ruin
		if(event.getTerritory() instanceof KonRuin) {
			KonRuin ruin = (KonRuin)event.getTerritory();
			// Spawn all ruin golems
			ruin.spawnAllGolems();
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onKonquestMonumentDamage(KonquestMonumentDamageEvent event) {
		ChatUtil.printDebug("EVENT: Monument damaged in town: "+event.getTerritory().getName()+", kingdom: "+event.getTerritory().getKingdom().getName());
		
		if(!(event.getTerritory() instanceof KonTown)) {
			ChatUtil.printDebug("Error in onKonquestMonumentDamage Event Handler, event passed territory that is not a Town");
			return;
		}
		KonTown town = (KonTown) event.getTerritory();
		
		// Verify town can be captured
		if(town.isCaptureDisabled()) {
			ChatUtil.sendNotice(event.getPlayer().getBukkitPlayer(), "This Town cannot be conquered again so soon!");
			event.setCancelled(true);
			return;
		}
		
		// Start Monument regenerate timer for target town
		int monumentRegenTimeSeconds = konquest.getConfigManager().getConfig("core").getInt("core.monuments.damage_regen");
		Timer monumentTimer = town.getMonumentTimer();
		monumentTimer.stopTimer();
		monumentTimer.setTime(monumentRegenTimeSeconds);
		monumentTimer.startTimer();
		ChatUtil.printDebug("Starting monument timer for "+monumentRegenTimeSeconds+" seconds with taskID "+monumentTimer.getTaskID());
		
		// Update MonumentBar state
		town.setAttacked(true);
		town.updateBar();
		//town.updateBarPlayers();
		
		// Re-apply town nerfs to attacker
		//kingdomManager.applyTownNerf(event.getPlayer());
		
		// Evaluate for critical strikes
		String criticalBlockTypeName = konquest.getConfigManager().getConfig("core").getString("core.monuments.critical_block");
		if(event.getBlockEvent().getBlock().getType().equals(Material.valueOf(criticalBlockTypeName))) {
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
					//ArrayList<KonPlayer> monumentPlayers = playerManager.getPlayersInMonument(town.getMonument());
					ArrayList<KonPlayer> monumentPlayers = new ArrayList<KonPlayer>();
					for(KonPlayer player : playerManager.getPlayersOnline()) {
						if(town.isLocInsideCenterChunk(player.getBukkitPlayer().getLocation())) {
							monumentPlayers.add(player);
						}
					}
					Location townSpawnLoc = town.getSpawnLoc();
					String townName = town.getName();
					ArrayList<KonPlayer> townLocPlayers = new ArrayList<KonPlayer>();
					for(KonPlayer player : playerManager.getPlayersOnline()) {
						if(town.isLocInside(player.getBukkitPlayer().getLocation())) {
							townLocPlayers.add(player);
						}
					}
					Timer townMonumentTimer = town.getMonumentTimer();
					if(kingdomManager.removeTown(town.getName(), town.getKingdom().getName())) {
						// Town is removed, no longer exists
						ChatUtil.sendNotice(event.getPlayer().getBukkitPlayer(), "You have destroyed "+townName+" for the glory of the Barbarian horde!");
						int local_x = 0;
						if(townSpawnLoc.getBlockX() > 0) {
							local_x = townSpawnLoc.getBlockX() % 16;
						} else {
							local_x = Math.abs(townSpawnLoc.getBlockX() % -16);
						}
						int local_z = 0;
						if(townSpawnLoc.getBlockZ() > 0) {
							local_z = townSpawnLoc.getBlockZ() % 16;
						} else {
							local_z = Math.abs(townSpawnLoc.getBlockZ() % -16);
						}
						int new_y = Bukkit.getServer().getWorld(konquest.getWorldName()).getChunkAt(townSpawnLoc).getChunkSnapshot(true,false,false).getHighestBlockYAt(local_x, local_z) + 1;
						townSpawnLoc.setY((double)new_y);
						// Teleport all players inside monument to a safe location
						event.getPlayer().getBukkitPlayer().teleport(townSpawnLoc);
						for(KonPlayer player : monumentPlayers) {
							player.getBukkitPlayer().teleport(townSpawnLoc);
						}
						
						for(KonPlayer player : monumentPlayers) {
							player.getBukkitPlayer().teleport(konquest.getSafeRandomCenteredLocation(town.getCenterLoc(), 2));
							//ChatUtil.printDebug("Effect data is: "+Effect.ANVIL_LAND.getData().getName()+", "+Effect.ANVIL_LAND.getData().toString());
							player.getBukkitPlayer().playEffect(player.getBukkitPlayer().getLocation(), Effect.ANVIL_LAND, null);
						}
						// Clear mob targets for all players within the old town
						for(KonPlayer player : townLocPlayers) {
							player.clearAllMobAttackers();
						}
						// Update directive progress
						konquest.getDirectiveManager().updateDirectiveProgress(event.getPlayer(), KonDirective.CAPTURE_TOWN);
					} else {
						ChatUtil.printDebug("Problem destroying Town "+event.getTerritory().getName()+" in Kingdom "+event.getTerritory().getKingdom().getName()+" for a barbarian raider "+event.getPlayer().getBukkitPlayer().getName());
					}
					// Stop the town monument timer
					ChatUtil.printDebug("Stopping monument timer with taskID "+townMonumentTimer.getTaskID());
					townMonumentTimer.stopTimer();
				} else {
					// Conquer the town for the enemy player's kingdom
					if(kingdomManager.conquerTown(town.getName(), town.getKingdom().getName(), event.getPlayer())) {
						// Alert all players of original Kingdom
						for(KonPlayer player : playerManager.getPlayersInKingdom(event.getTerritory().getKingdom().getName())) {
							ChatUtil.sendNotice(player.getBukkitPlayer(), "The Town "+event.getTerritory().getName()+" has been conquered!", ChatColor.DARK_RED);
						}
						ChatUtil.printDebug("Monument conversion in Town "+event.getTerritory().getName());
						ChatUtil.sendNotice(event.getPlayer().getBukkitPlayer(), "You have conquered "+town.getName()+" for the conquest of "+event.getPlayer().getKingdom().getName()+"!");
						// Teleport all players inside center chunk to new spawn location
						/*
						event.getPlayer().getBukkitPlayer().teleport(town.getSpawnLoc());
						for(KonPlayer player : playerManager.getPlayersInMonument(town.getMonument())) {
							player.getBukkitPlayer().teleport(town.getSpawnLoc());
						}
						*/
						for(KonPlayer player : playerManager.getPlayersOnline()) {
							if(town.isLocInsideCenterChunk(player.getBukkitPlayer().getLocation())) {
								player.getBukkitPlayer().teleport(konquest.getSafeRandomCenteredLocation(town.getCenterLoc(), 2));
								player.getBukkitPlayer().playEffect(player.getBukkitPlayer().getLocation(), Effect.ANVIL_LAND, null);
							}
						}
						// Start Capture disable timer for target town
						int townCaptureTimeSeconds = konquest.getConfigManager().getConfig("core").getInt("core.towns.capture_cooldown");
						Timer captureTimer = town.getCaptureTimer();
						town.setIsCaptureDisabled(true);
						captureTimer.stopTimer();
						captureTimer.setTime(townCaptureTimeSeconds);
						captureTimer.startTimer();
						ChatUtil.printDebug("Starting capture timer for "+townCaptureTimeSeconds+" seconds with taskID "+captureTimer.getTaskID());
						// Remove mob targets
						for(KonPlayer player : playerManager.getPlayersOnline()) {
							if(town.isLocInside(player.getBukkitPlayer().getLocation())) {
								player.clearAllMobAttackers();
							}
						}
						// Update directive progress
						konquest.getDirectiveManager().updateDirectiveProgress(event.getPlayer(), KonDirective.CAPTURE_TOWN);
						// Update stat
						konquest.getAccomplishmentManager().modifyPlayerStat(event.getPlayer(),KonStatsType.CAPTURES,1);
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
				ChatUtil.sendNotice(event.getPlayer().getBukkitPlayer(), "Critical Strike! "+remainingHits+" Critical Blocks remain.");
				konquest.getAccomplishmentManager().modifyPlayerStat(event.getPlayer(),KonStatsType.CRITICALS,1);
				
				// Alert all players of enemy Kingdom when the first critical block is broken
				if(town.getMonument().getCriticalHits() == 1) {
					for(KonPlayer player : playerManager.getPlayersInKingdom(event.getTerritory().getKingdom().getName())) {
						ChatUtil.sendKonPriorityTitle(player, ChatColor.DARK_RED+"Raid Alert!", ChatColor.DARK_RED+town.getName()+" is being conquered!", 60, 1, 10 );
						ChatUtil.sendNotice(player.getBukkitPlayer(), ChatColor.DARK_RED+"Use "+ChatColor.AQUA+"/k travel "+event.getTerritory().getName()+ChatColor.DARK_RED+" to defend and receive "+ChatColor.DARK_GREEN+defendReward+" Favor!");
					}
				}
				
				// Alert all players of enemy Kingdom when half of critical blocks are broken
				if(town.getMonument().getCriticalHits() == maxCriticalhits/2) {
					for(KonPlayer player : playerManager.getPlayersInKingdom(event.getTerritory().getKingdom().getName())) {
						ChatUtil.sendKonPriorityTitle(player, ChatColor.DARK_RED+"Raid Alert!", ChatColor.DARK_RED+town.getName()+" is at half strength!", 60, 1, 10 );
						ChatUtil.sendNotice(player.getBukkitPlayer(), ChatColor.DARK_RED+"Use "+ChatColor.AQUA+"/k travel "+event.getTerritory().getName()+ChatColor.DARK_RED+" to defend and receive "+ChatColor.DARK_GREEN+defendReward+" Favor!");
					}
				}
				
				// Alert all players of enemy Kingdom when all but 1 critical blocks are broken
				if(town.getMonument().getCriticalHits() == maxCriticalhits-1) {
					for(KonPlayer player : playerManager.getPlayersInKingdom(event.getTerritory().getKingdom().getName())) {
						ChatUtil.sendKonPriorityTitle(player, ChatColor.DARK_RED+"Raid Alert!", ChatColor.DARK_RED+town.getName()+" is in critical condition!", 60, 1, 10 );
						ChatUtil.sendNotice(player.getBukkitPlayer(), ChatColor.DARK_RED+"Use "+ChatColor.AQUA+"/k travel "+event.getTerritory().getName()+ChatColor.DARK_RED+" to defend and receive "+ChatColor.DARK_GREEN+defendReward+" Favor!");
					}
				}
			}
		}
	}
}
