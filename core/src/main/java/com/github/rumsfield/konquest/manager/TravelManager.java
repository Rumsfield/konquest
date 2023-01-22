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
		WILD
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
	
	public void submitTravel(Player bukkitPlayer, TravelDestination destination, KonTerritory territory, Location travelLoc, int warmupSeconds, double cost) {
		// determine warmup end time
		if(warmupSeconds > 0) {
			// There is a warmup time, queue the travel
			Date now = new Date();
			long warmupFinishTime = now.getTime() + (warmupSeconds* 1000L);
			travelers.put(bukkitPlayer, new TravelPlan(bukkitPlayer, destination, territory, travelLoc, warmupFinishTime, cost));
			ChatUtil.printDebug("Submitted new travel plan for "+bukkitPlayer.getName()+": now "+now.getTime()+" to "+warmupFinishTime+", size "+travelers.size());
		} else {
			// There is no warmup time, do the travel now
			executeTravel(bukkitPlayer, destination, territory, travelLoc, cost);
		}
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
		konquest.telePlayerLocation(bukkitPlayer, travelLoc);
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
