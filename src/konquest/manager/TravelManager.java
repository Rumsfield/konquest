package konquest.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.model.KonTerritory;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;
import konquest.utility.Timeable;
import konquest.utility.Timer;
import konquest.utility.TravelPlan;

public class TravelManager implements Timeable {

	public enum TravelDestination {
		CAPITAL,
		CAMP,
		HOME,
		TOWN,
		WILD;
	}
	
	private Konquest konquest;
	private HashMap<Player, TravelPlan> travelers;
	private Timer travelExecutor;
	
	public TravelManager(Konquest konquest) {
		this.konquest = konquest;
		this.travelers = new HashMap<Player, TravelPlan>();
		this.travelExecutor = new Timer(this);
		this.travelExecutor.stopTimer();
		this.travelExecutor.setTime(1);
		this.travelExecutor.startLoopTimer();
		
	}
	
	public void submitTravel(Player bukkitPlayer, TravelDestination destination, KonTerritory territory, Location travelLoc, int warmupSeconds) {
		// determine warmup end time
		if(warmupSeconds > 0) {
			// There is a warmup time, queue the travel
			Date now = new Date();
			long warmupFinishTime = now.getTime() + (warmupSeconds*1000);
			travelers.put(bukkitPlayer, new TravelPlan(destination, territory, travelLoc, warmupFinishTime));
		} else {
			// There is no warmup time, do the travel now
			executeTravel(bukkitPlayer, destination, territory, travelLoc);
		}
	}
	
	// Returns true when the player's travel was successfully canceled
	public boolean cancelTravel(Player bukkitPlayer) {
		boolean result = false;
		if(travelers.containsKey(bukkitPlayer)) {
			travelers.remove(bukkitPlayer);
			result = true;
		}
		return result;
	}
	
	private void executeTravel(Player bukkitPlayer, TravelDestination destination, KonTerritory territory, Location travelLoc) {
		// Do special things depending on the destination type
		switch (destination) {
			case TOWN:
				if(territory != null && territory instanceof KonTown) {
					KonTown town = (KonTown)(territory);
					town.addPlayerTravelCooldown(bukkitPlayer.getUniqueId());
		    		// Give raid defender reward
		    		if(town.isAttacked() && town.addDefender(bukkitPlayer)) {
		    			ChatUtil.printDebug("Raid defense rewarded to player "+bukkitPlayer.getName());
		    			int defendReward = konquest.getConfigManager().getConfig("core").getInt("core.favor.rewards.defend_raid");
			            KonquestPlugin.depositPlayer(bukkitPlayer, defendReward);
		    		}
		    		// Notify town officers of travel
		    		for(OfflinePlayer resident : town.getPlayerResidents()) {
		    			if(resident.isOnline() && (town.isPlayerLord(resident) || town.isPlayerKnight(resident))) {
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
	}


	@Override
	public void onEndTimer(int taskID) {
		if(taskID == 0) {
			ChatUtil.printDebug("Travel Executor Timer ended with null taskID!");
		} else if(taskID == travelExecutor.getTaskID()) {
			// Evaluate all travelers for expired warmup times, and execute travel for them
			ArrayList<Player> expiredTravelers = new ArrayList<Player>();
			for(Player traveler : travelers.keySet()) {
				if(new Date(travelers.get(traveler).getWarmupEndTime()).after(new Date())) {
					// Warmup has expired for this traveler
					executeTravel(traveler, travelers.get(traveler).getDestination(), travelers.get(traveler).getTerritory(), travelers.get(traveler).getLocation());
					expiredTravelers.add(traveler);
				}
			}
			// Remove travelers from the map
			for(Player traveler : expiredTravelers) {
				travelers.remove(traveler);
			}
		}
	}
	
}
