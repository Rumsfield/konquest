package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.manager.TravelManager.TravelDestination;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TravelCommand extends CommandBase {
	
	public TravelCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k travel <town>|<kingdom>|<sanctuary>|capital|home|wild|camp
    	if (getArgs().length != 2) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
		} else {
        	Player bukkitPlayer = (Player) getSender();
        	
        	World bukkitWorld = bukkitPlayer.getWorld();
        	if(!getKonquest().isWorldValid(bukkitWorld)) {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
                return;
        	}
        	
        	if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
			if(player == null) {
				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
				return;
			}
			// Verify player is not in enemy territory
			// But always allow travel from neutral kingdom (ruins, sanctuary)
			boolean blockEnemyTravel = getKonquest().getCore().getBoolean(CorePath.KINGDOMS_NO_ENEMY_TRAVEL.getPath());
			if (blockEnemyTravel) {
				Location playerLoc = bukkitPlayer.getLocation();
				if(getKonquest().getTerritoryManager().isChunkClaimed(playerLoc)) {
					KonKingdom locKingdom = getKonquest().getTerritoryManager().getChunkTerritory(playerLoc).getKingdom();
					if(!locKingdom.equals(getKonquest().getKingdomManager().getNeutrals()) && !locKingdom.equals(player.getKingdom())) {
						ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_TRAVEL_ERROR_ENEMY_TERRITORY.getMessage());
	                    return;
					}
				}
			}
			/*
			 * First, determine where the travel location is
			 */
        	String travelName = getArgs()[1];
        	Location travelLoc;
        	TravelDestination destination;
        	KonTerritory travelTerritory = null;
			boolean isSanctuaryTravel = getKonquest().getCore().getBoolean(CorePath.TRAVEL_ENABLE_SANCTUARY.getPath(),false);
			boolean isCapitalTravel = getKonquest().getCore().getBoolean(CorePath.TRAVEL_ENABLE_CAPITAL.getPath(),false);
			boolean isCampTravel = getKonquest().getCore().getBoolean(CorePath.TRAVEL_ENABLE_CAMP.getPath(),false);
			boolean isHomeTravel = getKonquest().getCore().getBoolean(CorePath.TRAVEL_ENABLE_HOME.getPath(),false);
			boolean isWildTravel = getKonquest().getCore().getBoolean(CorePath.TRAVEL_ENABLE_WILD.getPath(),false);
			boolean isTownTravel = getKonquest().getCore().getBoolean(CorePath.TRAVEL_ENABLE_TOWNS.getPath(),false);
			// Given name can be a sanctuary name, town name, the kingdom name, an allied town/kingdom name, or fixed:
			// capital, wild, camp, home
        	if(travelName.equalsIgnoreCase("capital")) {
        		// Travel to player's own capital
        		if(player.isBarbarian()) {
            		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
                    return;
            	}
        		if(!isCapitalTravel) {
            		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DISABLED.getMessage());
                    return;
        		}
				KonCapital travelCapital = player.getKingdom().getCapital();
				if(travelCapital == null) {
					ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
					return;
				}
				// Check for property flag
				if(!travelCapital.getPropertyValue(KonPropertyFlag.TRAVEL)) {
					ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedFlagColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
					return;
				}
				travelLoc = travelCapital.getSpawnLoc();
				destination = TravelDestination.CAPITAL;
				travelTerritory = travelCapital;
        	} else if(travelName.equalsIgnoreCase("camp")) {
        		// Travel to player's own camp
        		if(!player.isBarbarian()) {
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
                    return;
        		}
        		if(!getKonquest().getCampManager().isCampSet(player)) {
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_TRAVEL_ERROR_NO_CAMP.getMessage());
                    return;
        		}
        		if(!isCampTravel) {
            		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DISABLED.getMessage());
                    return;
        		}
				KonCamp travelCamp = getKonquest().getCampManager().getCamp(player);
				travelLoc = travelCamp.getSpawnLoc();
				destination = TravelDestination.CAMP;
				travelTerritory = travelCamp;
        	} else if(travelName.equalsIgnoreCase("home")) {
        		// Travel to player's own bed home
        		if(player.isBarbarian()) {
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
                    return;
        		}
        		Location bedLoc = player.getBukkitPlayer().getBedSpawnLocation();
        		if(bedLoc == null) {
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_TRAVEL_ERROR_NO_HOME.getMessage());
                    return;
        		}
        		if(!isHomeTravel) {
            		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DISABLED.getMessage());
                    return;
        		}
				travelLoc = bedLoc;
				destination = TravelDestination.HOME;
        	} else if(travelName.equalsIgnoreCase("wild")) {
        		// Travel to random wild location
        		if(!isWildTravel) {
            		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DISABLED.getMessage());
                    return;
        		}
				travelLoc = getKonquest().getRandomWildLocation(bukkitWorld);
				destination = TravelDestination.WILD;
        	} else {
        		// Given name is not a fixed option, check for valid names
				if(getKonquest().getSanctuaryManager().isSanctuary(travelName)) {
					// Name is a sanctuary
					if(!isSanctuaryTravel) {
						ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DISABLED.getMessage());
						return;
					}
					KonSanctuary travelSanctuary = getKonquest().getSanctuaryManager().getSanctuary(travelName);
					if(travelSanctuary == null) {
						ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
						return;
					}
					// Check for property flag
					if(!travelSanctuary.getPropertyValue(KonPropertyFlag.TRAVEL)) {
						ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedFlagColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
						return;
					}
					travelLoc = travelSanctuary.getSpawnLoc();
					destination = TravelDestination.SANCTUARY;
					travelTerritory = travelSanctuary;
				} else if(getKonquest().getKingdomManager().isKingdom(travelName)) {
					// Name is a kingdom
					if(player.isBarbarian()) {
						ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
						return;
					}
					if(!isCapitalTravel) {
						ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DISABLED.getMessage());
						return;
					}
					KonCapital travelCapital = getKonquest().getKingdomManager().getCapital(travelName);
					if(travelCapital == null) {
						ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
						return;
					}
					// Check for player's own kingdom or allied kingdom
					if(!player.getKingdom().equals(travelCapital.getKingdom()) && !getKonquest().getKingdomManager().isPlayerAlly(player,travelCapital.getKingdom())) {
						ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
						return;
					}
					// Check for property flag
					if(!travelCapital.getPropertyValue(KonPropertyFlag.TRAVEL)) {
						ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedFlagColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
						return;
					}
					travelLoc = travelCapital.getSpawnLoc();
					destination = TravelDestination.CAPITAL;
					travelTerritory = travelCapital;
				} else if(getKonquest().getKingdomManager().isTown(travelName)) {
					// Name is a town
					if(player.isBarbarian()) {
						ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
						return;
					}
					if(!isTownTravel) {
						ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DISABLED.getMessage());
						return;
					}
					KonTown travelTown = getKonquest().getKingdomManager().getTown(travelName);
					if(travelTown == null) {
						ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
						return;
					}
					// Check for player's own town or allied town
					if(!player.getKingdom().equals(travelTown.getKingdom()) && !getKonquest().getKingdomManager().isPlayerAlly(player,travelTown.getKingdom())) {
						ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
						return;
					}
					// Check for property flag
					if(!travelTown.getPropertyValue(KonPropertyFlag.TRAVEL)) {
						ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedFlagColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
						return;
					}
					// Check town travel cooldown
					if(travelTown.isPlayerTravelDisabled(bukkitPlayer.getUniqueId())) {
						String cooldown = travelTown.getPlayerTravelCooldownString(bukkitPlayer.getUniqueId());
						ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_TRAVEL_ERROR_COOLDOWN.getMessage(cooldown,travelTown.getName()));
						return;
					}
					travelLoc = travelTown.getSpawnLoc();
					destination = TravelDestination.TOWN;
					travelTerritory = travelTown;
				} else {
					ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_BAD_NAME.getMessage(travelName));
					return;
				}
        	}

			/*
			 * Second, determine whether player can cover cost
			 */
    		boolean isTravelAlwaysAllowed = getKonquest().getCore().getBoolean(CorePath.FAVOR_ALLOW_TRAVEL_ALWAYS.getPath(),true);
        	double cost = getKonquest().getCore().getDouble(CorePath.FAVOR_COST_TRAVEL.getPath(),0.0);
        	double cost_per_chunk = getKonquest().getCore().getDouble(CorePath.FAVOR_COST_TRAVEL_PER_CHUNK.getPath(),0.0);
        	double cost_world = getKonquest().getCore().getDouble(CorePath.FAVOR_COST_TRAVEL_WORLD.getPath(),0.0);
        	double cost_camp = getKonquest().getCore().getDouble(CorePath.FAVOR_COST_TRAVEL_CAMP.getPath(),0.0);
        	cost = (cost < 0) ? 0 : cost;
        	cost_per_chunk = (cost_per_chunk < 0) ? 0 : cost_per_chunk;
        	cost_camp = (cost_camp < 0) ? 0 : cost_camp;
        	double total_cost;
        	if(destination.equals(TravelDestination.CAMP)) {
        		// Player is traveling to camp, fixed cost
        		total_cost = cost_camp;
        	} else {
        		// Player is traveling to town, capital, sanctuary or wild
        		int chunkDistance = Konquest.chunkDistance(travelLoc,bukkitPlayer.getLocation());
	        	if(chunkDistance >= 0) {
	        		// Value is chunk distance within the same world
	        		total_cost = cost + cost_per_chunk*chunkDistance;
	        	} else {
	        		// Value of -1 means travel points are between different worlds
	        		total_cost = cost + cost_world;
	        	}
        	}
			if(!isTravelAlwaysAllowed && total_cost > 0) {
				if(KonquestPlugin.getBalance(bukkitPlayer) < total_cost) {
					ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(total_cost));
                    return;
				}
			}
			if(isTravelAlwaysAllowed && KonquestPlugin.getBalance(bukkitPlayer) < total_cost) {
				// Override cost to 0 to allow the player to travel, when they don't have enough money.
				total_cost = 0;
        	}
			
			// Condition destination location. Sanctuary destinations have preserved look angles.
			// Other destinations should use the player's current direction.
			Location travelLocAng = travelLoc;
			if(!destination.equals(TravelDestination.SANCTUARY)) {
				Location pLoc = bukkitPlayer.getLocation();
				travelLocAng = new Location(travelLoc.getWorld(),travelLoc.getX(),travelLoc.getY(),travelLoc.getZ(),pLoc.getYaw(),pLoc.getPitch());
			}

			// Wait for a warmup duration, then do stuff
			// Cancel travel if player moves?
			// 	- Players need a private flag: waiting for travel warmup
			// 	- Player move listener needs to check this flag and optionally cancel it
			// If player uses another travel command during warmup, cancel current warmup and begin a new one for new destination
			int travelWarmup = getKonquest().getCore().getInt(CorePath.TRAVEL_WARMUP.getPath(),0);
			travelWarmup = Math.max(travelWarmup, 0);
			if(travelWarmup > 0) {
				String warmupTimeStr = ""+travelWarmup;
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TRAVEL_NOTICE_WARMUP.getMessage(warmupTimeStr));
			}

			// Submit the travel to the manager. It will take things from here.
			getKonquest().getTravelManager().submitTravel(bukkitPlayer, destination, travelTerritory, travelLocAng, travelWarmup, total_cost);
			
        }
	}
	
	@Override
	public List<String> tabComplete() {
		// k travel <town>|<kingdom>|<sanctuary>|capital|home|wild|camp
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		if(getArgs().length == 2) {
			boolean isSanctuaryTravel = getKonquest().getCore().getBoolean(CorePath.TRAVEL_ENABLE_SANCTUARY.getPath(),false);
			boolean isCapitalTravel = getKonquest().getCore().getBoolean(CorePath.TRAVEL_ENABLE_CAPITAL.getPath(),false);
			boolean isTownTravel = getKonquest().getCore().getBoolean(CorePath.TRAVEL_ENABLE_TOWNS.getPath(),false);
			boolean isHomeTravel = getKonquest().getCore().getBoolean(CorePath.TRAVEL_ENABLE_HOME.getPath(),false);
			boolean isCampTravel = getKonquest().getCore().getBoolean(CorePath.TRAVEL_ENABLE_CAMP.getPath(),false);
			boolean isWildTravel = getKonquest().getCore().getBoolean(CorePath.TRAVEL_ENABLE_WILD.getPath(),false);
			Player bukkitPlayer = (Player) getSender();
			KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
			if(player.isBarbarian()) {
				if(isCampTravel) {tabList.add("camp");}
				if(isWildTravel) {tabList.add("wild");}
				if(isSanctuaryTravel) {tabList.addAll(getKonquest().getSanctuaryManager().getSanctuaryNames());}
	    	} else {
				if(isHomeTravel) {tabList.add("home");}
				if(isWildTravel) {tabList.add("wild");}
				if(isSanctuaryTravel) {tabList.addAll(getKonquest().getSanctuaryManager().getSanctuaryNames());}
				if(isCapitalTravel) {tabList.add("capital");}
				for(KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
					if(player.getKingdom().equals(kingdom) || getKonquest().getKingdomManager().isKingdomAlliance(player.getKingdom(),kingdom)) {
						if(isCapitalTravel) {tabList.add(kingdom.getName());}
						if(isTownTravel) {tabList.addAll(kingdom.getTownNames());}
					}
				}
	    	}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
