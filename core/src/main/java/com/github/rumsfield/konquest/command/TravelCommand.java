package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.manager.KingdomManager;
import com.github.rumsfield.konquest.manager.TravelManager.TravelDestination;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import com.github.rumsfield.konquest.utility.RandomWildLocationSearchTask;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TravelCommand extends CommandBase {

	KingdomManager kManager;
	
	public TravelCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
		this.kManager = getKonquest().getKingdomManager();
    }
	
	public void execute() {
		// k travel <town>|<kingdom>|<sanctuary>|capital|home|wild|camp
		Player bukkitPlayer = (Player) getSender();
    	if (getArgs().length != 2) {
			sendInvalidArgMessage(bukkitPlayer,CommandType.TRAVEL);
		} else {
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
			boolean blockEnemyTravel = getKonquest().getCore().getBoolean(CorePath.KINGDOMS_NO_ENEMY_TRAVEL.getPath());
			if (blockEnemyTravel) {
				Location playerLoc = bukkitPlayer.getLocation();
				if(getKonquest().getTerritoryManager().isChunkClaimed(playerLoc)) {
					KonTerritory locTerritory = getKonquest().getTerritoryManager().getChunkTerritory(playerLoc);
					assert locTerritory != null;
					KonKingdom locKingdom = locTerritory.getKingdom();
					boolean isTravelAllowed = locTerritory.getTerritoryType().equals(KonquestTerritoryType.SANCTUARY) ||
							locKingdom.equals(player.getKingdom()) ||
							kManager.isPlayerPeace(player,locKingdom) ||
							kManager.isPlayerTrade(player,locKingdom) ||
							kManager.isPlayerAlly(player,locKingdom);
					if(!isTravelAllowed) {
						ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_TRAVEL_ERROR_ENEMY_TERRITORY.getMessage());
						return;
					}
				}
			}
			/*
			 * First, determine where the travel location is
			 */
        	String travelName = getArgs()[1];
        	Location travelLoc = null;
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
					ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_TRAVEL_ERROR_DISABLED.getMessage());
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
				ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_TRAVEL_NOTICE_WILD_SEARCH.getMessage());
				destination = TravelDestination.WILD;
				// Wild travel is a little different.
				// It takes a lot of effort to load random chunks and find a valid travel location,
				// too much effort for 1 tick. So the search will take place in a separate thread
				// over multiple ticks. When the task is finished, use a lamba expression to submit the travel.
				RandomWildLocationSearchTask task = new RandomWildLocationSearchTask(getKonquest(),bukkitWorld,location -> {
					if(location == null) {
						ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TRAVEL_ERROR_WILD_TIMEOUT.getMessage());
					} else {
						getKonquest().getTravelManager().submitTravel(bukkitPlayer, destination, null, location);
					}
				});
				// schedule the task to run once every 20 ticks (20 ticks per second)
				task.runTaskTimer(getKonquest().getPlugin(), 0L, 20L);

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
						ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_TRAVEL_ERROR_DISABLED.getMessage());
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
						ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_TRAVEL_ERROR_DISABLED.getMessage());
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
						ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_TRAVEL_ERROR_NO_TOWN.getMessage());
						return;
					}
					// Check for property flag
					if(!travelTown.getPropertyValue(KonPropertyFlag.TRAVEL)) {
						ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedFlagColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
						ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_TRAVEL_ERROR_DISABLED.getMessage());
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

			// Submit the travel to the manager. It will take things from here.
			getKonquest().getTravelManager().submitTravel(bukkitPlayer, destination, travelTerritory, travelLoc);
			
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
