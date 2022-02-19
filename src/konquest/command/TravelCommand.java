package konquest.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.model.KonStatsType;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class TravelCommand extends CommandBase {

	public enum TravelDestination {
		CAPITAL,
		CAMP,
		HOME,
		TOWN,
		WILD;
	}
	
	public TravelCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k travel town1|capital|camp|home|wild
    	if (getArgs().length != 2) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	
        	World bukkitWorld = bukkitPlayer.getWorld();
        	if(!getKonquest().isWorldValid(bukkitWorld)) {
        		//ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_WORLD.toString());
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
                return;
        	}
        	
        	if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
			// Verify player is not in enemy territory
			boolean blockEnemyTravel = getKonquest().getConfigManager().getConfig("core").getBoolean("core.kingdoms.no_enemy_travel");
			if (blockEnemyTravel) {
				Location playerLoc = bukkitPlayer.getLocation();
				if(getKonquest().getKingdomManager().isChunkClaimed(playerLoc)) {
					if(!getKonquest().getKingdomManager().getChunkTerritory(playerLoc).getKingdom().equals(player.getKingdom())) {
						//ChatUtil.sendError((Player) getSender(), "Cannot travel within enemy territory!");
						ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_TRAVEL_ERROR_ENEMY_TERRITORY.getMessage());
	                    return;
					}
				}
			}
			// First, determine where the travel location is
        	String travelName = getArgs()[1];
        	Location travelLoc;
        	TravelDestination destination;
        	KonTown town = null;
        	if(travelName.equalsIgnoreCase("capital")) {
        		// Travel to capital
        		if(player.isBarbarian()) {
            		//ChatUtil.sendError((Player) getSender(), "Barbarians can only travel to camp");
            		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
                    return;
            	}
        		boolean isCapitalTravel = getKonquest().getConfigManager().getConfig("core").getBoolean("core.travel.enable.capital",false);
        		if(isCapitalTravel) {
        			travelLoc = player.getKingdom().getCapital().getSpawnLoc();
            		destination = TravelDestination.CAPITAL;
        		} else {
            		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DISABLED.getMessage());
                    return;
        		}
        	} else if(travelName.equalsIgnoreCase("camp")) {
        		// Travel to camp
        		if(!player.isBarbarian()) {
        			//ChatUtil.sendError((Player) getSender(), "Only Barbarians can travel to Camps.");
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
                    return;
        		}
        		if(!getKonquest().getCampManager().isCampSet((KonOfflinePlayer)player)) {
        			//ChatUtil.sendError((Player) getSender(), "You do not have a Camp!");
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_TRAVEL_ERROR_NO_CAMP.getMessage());
                    return;
        		}
        		boolean isCampTravel = getKonquest().getConfigManager().getConfig("core").getBoolean("core.travel.enable.camp",false);
        		if(isCampTravel) {
        			travelLoc = getKonquest().getCampManager().getCamp((KonOfflinePlayer)player).getSpawnLoc();
            		destination = TravelDestination.CAMP;
        		} else {
            		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DISABLED.getMessage());
                    return;
        		}
        	} else if(travelName.equalsIgnoreCase("home")) {
        		// Travel to bed home
        		if(player.isBarbarian()) {
        			//ChatUtil.sendError((Player) getSender(), "Barbarians cannot travel to Home.");
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
                    return;
        		}
        		Location bedLoc = player.getBukkitPlayer().getBedSpawnLocation();
        		if(bedLoc == null) {
        			//ChatUtil.sendError((Player) getSender(), "You do not have a bed home!");
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_TRAVEL_ERROR_NO_HOME.getMessage());
                    return;
        		}
        		boolean isHomeTravel = getKonquest().getConfigManager().getConfig("core").getBoolean("core.travel.enable.home",false);
        		if(isHomeTravel) {
        			travelLoc = bedLoc;
            		destination = TravelDestination.HOME;
        		} else {
            		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DISABLED.getMessage());
                    return;
        		}
        	} else if(travelName.equalsIgnoreCase("wild")) {
        		// Travel to random wild location
        		boolean isWildTravel = getKonquest().getConfigManager().getConfig("core").getBoolean("core.travel.enable.wild",false);
        		if(isWildTravel) {
        			travelLoc = getKonquest().getRandomWildLocation(bukkitWorld);
            		destination = TravelDestination.WILD;
        		} else {
            		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DISABLED.getMessage());
                    return;
        		}
        	} else {
        		// Travel to town
        		if(player.isBarbarian()) {
            		//ChatUtil.sendError((Player) getSender(), "Barbarians can only travel to camp");
            		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
                    return;
            	}
        		if(!player.getKingdom().hasTown(travelName)) {
        			//ChatUtil.sendError((Player) getSender(), "That town does not exist in your Kingdom.");
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_BAD_NAME.getMessage(travelName));
                    return;
        		}
        		town = player.getKingdom().getTown(travelName);
        		if(town.isPlayerTravelDisabled(bukkitPlayer.getUniqueId())) {
        			String cooldown = town.getPlayerTravelCooldownString(bukkitPlayer.getUniqueId());
        			//ChatUtil.sendError((Player) getSender(), "You must wait "+cooldown+" before traveling to "+town.getName());
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_TRAVEL_ERROR_COOLDOWN.getMessage(cooldown,town.getName()));
                    return;
        		}
        		boolean isTownTravel = getKonquest().getConfigManager().getConfig("core").getBoolean("core.travel.enable.towns",false);
        		if(isTownTravel) {
        			travelLoc = town.getSpawnLoc();
            		destination = TravelDestination.TOWN;
        		} else {
            		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DISABLED.getMessage());
                    return;
        		}
        	}
        	// Second, determine whether player can cover cost
        	if(!destination.equals(TravelDestination.CAMP)) {
	        	boolean isTravelAlwaysAllowed = getKonquest().getConfigManager().getConfig("core").getBoolean("core.favor.allow_travel_always",true);
	        	double cost = getKonquest().getConfigManager().getConfig("core").getDouble("core.favor.cost_travel",0.0);
	        	double cost_per_chunk = getKonquest().getConfigManager().getConfig("core").getDouble("core.favor.cost_travel_per_chunk",0.0);
	        	double cost_world = getKonquest().getConfigManager().getConfig("core").getDouble("core.favor.cost_travel_world",0.0);
	        	cost = (cost < 0) ? 0 : cost;
	        	cost_per_chunk = (cost_per_chunk < 0) ? 0 : cost_per_chunk;
	        	double total_cost = 0;
	        	int chunkDistance = Konquest.chunkDistance(travelLoc,bukkitPlayer.getLocation());
	        	if(chunkDistance >= 0) {
	        		// Value is chunk distance within the same world
	        		total_cost = cost + cost_per_chunk*chunkDistance;
	        	} else {
	        		// Value of -1 means travel points are between different worlds
	        		total_cost = cost + cost_world;
	        	}
				if(!isTravelAlwaysAllowed && total_cost > 0) {
					if(KonquestPlugin.getBalance(bukkitPlayer) < total_cost) {
						//ChatUtil.sendError((Player) getSender(), "Not enough Favor, need "+total_cost);
						ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(total_cost));
	                    return;
					}
				}
				if(isTravelAlwaysAllowed && KonquestPlugin.getBalance(bukkitPlayer) < total_cost) {
	        		//ChatUtil.sendNotice((Player) getSender(), "Not enough Favor, this one's on us.");
	        	} else {
	                if(KonquestPlugin.withdrawPlayer(bukkitPlayer, total_cost)) {
	                	getKonquest().getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)total_cost);
	                }
	        	}
        	}
			// Third, do stuff based on travel destination
        	switch (destination) {
        		case TOWN:
        			if(town != null) {
	        			town.addPlayerTravelCooldown(bukkitPlayer.getUniqueId());
	            		// Give raid defender reward
	            		if(town.isAttacked() && town.addDefender(bukkitPlayer)) {
	            			ChatUtil.printDebug("Raid defense rewarded to player "+player.getBukkitPlayer().getName());
	            			int defendReward = getKonquest().getConfigManager().getConfig("core").getInt("core.favor.rewards.defend_raid");
	        	            KonquestPlugin.depositPlayer(player.getBukkitPlayer(), defendReward);
	            		}
	            		//TODO this might get spammy
	            		for(OfflinePlayer resident : town.getPlayerResidents()) {
	    	    			if(resident.isOnline() && (town.isPlayerLord(resident) || town.isPlayerElite(resident))) {
	    	    				//ChatUtil.sendNotice((Player) resident, bukkitPlayer.getName()+" has traveled to "+town.getName());
	    	    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TRAVEL_NOTICE_TOWN_TRAVEL.getMessage(bukkitPlayer.getName(),town.getName()));
	    	    			}
	    	    		}
	            		Location pLoc = bukkitPlayer.getLocation();
	            		Location tLoc = town.getSpawnLoc();
	            		Location dest = new Location(tLoc.getWorld(),tLoc.getX(),tLoc.getY(),tLoc.getZ(),pLoc.getYaw(),pLoc.getPitch());
	            		getKonquest().telePlayerLocation(bukkitPlayer, dest);
        			} else {
        				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
        			}
        			break;
        		case WILD:
        			getKonquest().telePlayerLocation(bukkitPlayer, travelLoc);
        			//ChatUtil.sendNotice((Player) getSender(), "Traveled to a random location in the Wild.");
        			ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_TRAVEL_NOTICE_WILD_TRAVEL.getMessage());
        			break;
        		default:
        			getKonquest().telePlayerLocation(bukkitPlayer, travelLoc);
        			break;
        	}
        }
	}
	
	@Override
	public List<String> tabComplete() {
		// k travel town1|capital|home|wild|camp
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		if(getArgs().length == 2) {
			boolean isCapitalTravel = getKonquest().getConfigManager().getConfig("core").getBoolean("core.travel.enable.capital",false);
			boolean isTownTravel = getKonquest().getConfigManager().getConfig("core").getBoolean("core.travel.enable.towns",false);
			boolean isHomeTravel = getKonquest().getConfigManager().getConfig("core").getBoolean("core.travel.enable.home",false);
			boolean isCampTravel = getKonquest().getConfigManager().getConfig("core").getBoolean("core.travel.enable.camp",false);
			boolean isWildTravel = getKonquest().getConfigManager().getConfig("core").getBoolean("core.travel.enable.wild",false);
			Player bukkitPlayer = (Player) getSender();
			KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
			if(player.isBarbarian()) {
				if(isCampTravel) {tabList.add("camp");}
				if(isWildTravel) {tabList.add("wild");}
	    	} else {
	    		List<String> townList = player.getKingdom().getTownNames();
	    		if(isCapitalTravel) {tabList.add("capital");}
	    		if(isHomeTravel) {tabList.add("home");}
	    		if(isWildTravel) {tabList.add("wild");}
	    		if(isTownTravel) {tabList.addAll(townList);}
	    	}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
