package com.github.rumsfield.konquest.manager;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonStatsType;
import com.github.rumsfield.konquest.model.KonTerritory;
import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.utility.*;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class TravelManager implements Timeable {

	public enum TravelDestination {
		SANCTUARY,
		CAPITAL,
		CAMP,
		HOME,
		TOWN,
		WILD,
		OTHER
	}
	
	private final Konquest konquest;
	private final HashMap<Player, TravelPlan> travelers;
	private final Timer travelExecutor;
	
	public TravelManager(Konquest konquest) {
		this.konquest = konquest;
		this.travelers = new HashMap<>();
		this.travelExecutor = new Timer(this);
		this.travelExecutor.stopTimer();
		this.travelExecutor.setTime(1);
		this.travelExecutor.startLoopTimer();
		
	}

	public void submitWildTravel(Player bukkitPlayer) {
		if(!konquest.isWorldValid(bukkitPlayer.getWorld())) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
			return;
		}
		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TRAVEL_NOTICE_WILD_SEARCH.getMessage());
		// Wild travel is a little different.
		// It takes a lot of effort to load random chunks and find a valid travel location,
		// too much effort for 1 tick. So the search will take place in a separate thread
		// over multiple ticks. When the task is finished, use a lamba expression to submit the travel.
		RandomWildLocationSearchTask task = new RandomWildLocationSearchTask(konquest,bukkitPlayer.getWorld(),location -> {
			if(location == null) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TRAVEL_ERROR_WILD_TIMEOUT.getMessage());
			} else {
				konquest.getTravelManager().submitTravel(bukkitPlayer, TravelDestination.WILD, null, location);
			}
		});
		// schedule the task to run once every 10 ticks (20 ticks per second)
		task.runTaskTimer(konquest.getPlugin(), 20L, 10L);
	}
	
	public boolean submitTravel(Player bukkitPlayer, TravelDestination destination, KonTerritory territory, Location travelLoc) {
		if(travelLoc == null) return false;

		// Check for other plugin flags
		if(konquest.getIntegrationManager().getWorldGuard().isEnabled()) {
			// Origin location
			if(!konquest.getIntegrationManager().getWorldGuard().isLocationTravelExitAllowed(bukkitPlayer.getLocation(),bukkitPlayer)) {
				// A region is denying this action
				ChatUtil.sendError(bukkitPlayer, MessagePath.REGION_ERROR_TRAVEL_EXIT_DENY.getMessage());
				return false;
			}
			// Destination location
			if(!konquest.getIntegrationManager().getWorldGuard().isLocationTravelEnterAllowed(travelLoc,bukkitPlayer)) {
				// A region is denying this action
				ChatUtil.sendError(bukkitPlayer, MessagePath.REGION_ERROR_TRAVEL_ENTER_DENY.getMessage());
				return false;
			}
		}

		// Determine whether player can cover cost
		boolean isTravelAlwaysAllowed = konquest.getCore().getBoolean(CorePath.FAVOR_ALLOW_TRAVEL_ALWAYS.getPath(),true);
		double cost = konquest.getCore().getDouble(CorePath.FAVOR_COST_TRAVEL.getPath(),0.0);
		double cost_per_chunk = konquest.getCore().getDouble(CorePath.FAVOR_COST_TRAVEL_PER_CHUNK.getPath(),0.0);
		double cost_world = konquest.getCore().getDouble(CorePath.FAVOR_COST_TRAVEL_WORLD.getPath(),0.0);
		double cost_camp = konquest.getCore().getDouble(CorePath.FAVOR_COST_TRAVEL_CAMP.getPath(),0.0);
		cost = (cost < 0) ? 0 : cost;
		cost_per_chunk = (cost_per_chunk < 0) ? 0 : cost_per_chunk;
		cost_camp = (cost_camp < 0) ? 0 : cost_camp;
		double total_cost;
		if(destination.equals(TravelDestination.CAMP)) {
			// Player is traveling to camp, fixed cost
			total_cost = cost_camp;
		} else {
			// Player is traveling to town, capital, sanctuary or wild
			int chunkDistance = HelperUtil.chunkDistance(travelLoc,bukkitPlayer.getLocation());
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
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(total_cost));
				return false;
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
		int warmupSeconds = konquest.getCore().getInt(CorePath.TRAVEL_WARMUP.getPath(),0);
		warmupSeconds = Math.max(warmupSeconds, 0);
		if(warmupSeconds > 0) {
			String warmupTimeStr = ""+warmupSeconds;
			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TRAVEL_NOTICE_WARMUP.getMessage(warmupTimeStr));
		}
		// determine warmup end time
		if(warmupSeconds > 0) {
			// There is a warmup time, queue the travel
			Date now = new Date();
			long warmupFinishTime = now.getTime() + (warmupSeconds* 1000L);
			travelers.put(bukkitPlayer, new TravelPlan(bukkitPlayer, destination, territory, travelLocAng, warmupFinishTime, total_cost));
			ChatUtil.printDebug("Submitted new travel plan for "+bukkitPlayer.getName()+": now "+now.getTime()+" to "+warmupFinishTime+", size "+travelers.size());
		} else {
			// There is no warmup time, do the travel now
			executeTravel(bukkitPlayer, destination, territory, travelLocAng, total_cost);
		}
		return true;
	}
	
	// Returns true when the player's travel was successfully canceled
	public boolean cancelTravel(Player bukkitPlayer) {
		boolean result = false;
		//TODO: Check that the traveler is not currently executing any teleportation
		if(travelers.containsKey(bukkitPlayer)) {
			travelers.remove(bukkitPlayer);
			result = true;
		}
		return result;
	}
	
	private void executeTravel(Player bukkitPlayer, TravelDestination destination, KonTerritory territory, Location travelLoc, double cost) {
		// Do special things depending on the destination type
		String territoryName = territory == null ? "null" : territory.getName();
		ChatUtil.printDebug("Executing travel for "+bukkitPlayer.getName()+" to "+destination.toString()+" "+territoryName);
		konquest.telePlayerLocation(bukkitPlayer, travelLoc);
		switch (destination) {
			case TOWN:
				if(territory instanceof KonTown) {
					KonTown town = (KonTown)(territory);
					town.addPlayerTravelCooldown(bukkitPlayer.getUniqueId());
		    		// Give raid defender reward
		    		if(town.isAttacked() && town.addDefender(bukkitPlayer)) {
		    			ChatUtil.printDebug("Raid defense rewarded to player "+bukkitPlayer.getName());
		    			int defendReward = konquest.getCore().getInt(CorePath.FAVOR_REWARDS_DEFEND_RAID.getPath());
			            KonquestPlugin.depositPlayer(bukkitPlayer, defendReward);
		    		}
		    		// Notify town officers of travel
		    		for(OfflinePlayer resident : town.getPlayerResidents()) {
		    			if(resident.isOnline() && (town.isPlayerLord(resident) || town.isPlayerKnight(resident)) && !resident.getUniqueId().equals(bukkitPlayer.getUniqueId())) {
		    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TRAVEL_NOTICE_TOWN_TRAVEL.getMessage(bukkitPlayer.getName(),town.getName()));
		    			}
		    		}
				}
				break;
			case WILD:
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TRAVEL_NOTICE_WILD_TRAVEL.getMessage());
				break;
			default:
				break;
		}
		// Pay up
		if(KonquestPlugin.withdrawPlayer(bukkitPlayer, cost)) {
			// Successfully withdrew non-zero amount from player, update stats
			KonPlayer player = konquest.getPlayerManager().getPlayer(bukkitPlayer);
			if(player != null) {
				konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)cost);
			}
        }
	}


	@Override
	public void onEndTimer(int taskID) {
		if(taskID == 0) {
			ChatUtil.printDebug("Travel Executor Timer ended with null taskID!");
		} else if(taskID == travelExecutor.getTaskID()) {
			// Evaluate all travelers for expired warmup times, and execute travel for them
			ArrayList<Player> expiredTravelers = new ArrayList<>();
			ArrayList<TravelPlan> expiredPlans = new ArrayList<>();
			for(Player traveler : travelers.keySet()) {
				Date now = new Date();
				if(now.after(new Date(travelers.get(traveler).getWarmupEndTime()))) {
					ChatUtil.printDebug("Found ready traveler "+traveler.getName()+", "+now.getTime()+" is after "+travelers.get(traveler).getWarmupEndTime());
					// Warmup has expired for this traveler
					expiredTravelers.add(traveler);
					expiredPlans.add(travelers.get(traveler));
				}
			}
			// Remove travelers from the map
			for(Player traveler : expiredTravelers) {
				travelers.remove(traveler);
			}
			// Execute travel plans
			for(TravelPlan plan : expiredPlans) {
				executeTravel(plan.getTraveler(), plan.getDestination(), plan.getTerritory(), plan.getLocation(), plan.getCost());
			}
		}
	}
	
}
