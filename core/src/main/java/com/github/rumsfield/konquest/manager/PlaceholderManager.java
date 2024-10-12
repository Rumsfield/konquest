package com.github.rumsfield.konquest.manager;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.api.manager.KonquestPlaceholderManager;
import com.github.rumsfield.konquest.api.model.KonquestRelationshipType;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.Labeler;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.entity.Player;

import java.util.*;

//TODO: Add enums for each placeholder, implement list of players per-enum with request cooldown times.

// A placeholder cannot be requested until after the cooldown time, configurable duration.
// Cooldown time applies to every placeholder.
// Previous placeholder result is cached and returned for requests made before cooldown time ends.
public class PlaceholderManager implements KonquestPlaceholderManager {

	private enum KingdomValue {
		PLAYERS,
		ONLINE,
		TOWNS,
		LAND,
		FAVOR,
		SCORE
	}
	
	private static class Ranked {
		String name;
		int value;
		Ranked(String name, int value) {
			this.name = name;
			this.value = value;
		}
	}
	
	private final Konquest konquest;
	private final PlayerManager playerManager;
	private final KingdomManager kingdomManager;
	private final RuinManager ruinManager;
	private final TerritoryManager territoryManager;
	private int cooldownSeconds;
	private final Comparator<Ranked> rankedComparator;
	private long topScoreCooldownTime;
	private long topTownCooldownTime;
	private long topLandCooldownTime;
	private ArrayList<Ranked> topScoreList;
	private ArrayList<Ranked> topTownList;
	private ArrayList<Ranked> topLandList;
	private HashMap<String,HashMap<KingdomValue,Integer>> kingdomCache; // Store by kingdom name, then value by type
	private HashMap<String,HashMap<KingdomValue,Long>> kingdomCooldownTimes; // Store by kingdom name, then cooldown by type
	
	public PlaceholderManager(Konquest konquest) {
		this.konquest = konquest;
		this.playerManager = konquest.getPlayerManager();
		this.kingdomManager = konquest.getKingdomManager();
		this.ruinManager = konquest.getRuinManager();
		this.territoryManager = konquest.getTerritoryManager();
		this.cooldownSeconds = 0;
		this.topScoreCooldownTime = 0L;
		this.topTownCooldownTime = 0L;
		this.topLandCooldownTime = 0L;
		this.topScoreList = new ArrayList<>();
		this.topTownList = new ArrayList<>();
		this.topLandList = new ArrayList<>();
		this.kingdomCache = new HashMap<>();
		this.kingdomCooldownTimes = new HashMap<>();
		this.rankedComparator = (rankOne, rankTwo) -> {
			int result = 0;
			if(rankOne.value < rankTwo.value) {
				result = 1;
			} else if(rankOne.value > rankTwo.value) {
				result = -1;
			}
			return result;
		};
	}
	
	public void initialize() {
		topScoreCooldownTime = 0L;
		topTownCooldownTime = 0L;
		topLandCooldownTime = 0L;
		topScoreList = new ArrayList<>();
		topTownList = new ArrayList<>();
		topLandList = new ArrayList<>();
		kingdomCache = new HashMap<>();
		kingdomCooldownTimes = new HashMap<>();
		cooldownSeconds = konquest.getCore().getInt(CorePath.PLACEHOLDER_REQUEST_LIMIT.getPath(),0);
		ChatUtil.printDebug("Placeholder Manager is ready with cool-down seconds: "+cooldownSeconds);
	}
	
	private String boolean2Lang(boolean val) {
 		String result = MessagePath.LABEL_FALSE.getMessage();
 		if(val) {
 			result = MessagePath.LABEL_TRUE.getMessage();
 		}
 		return result;
 	}
	
	/*
	 * Placeholder Requesters
	 */
	
	public String getKingdom(Player player) {
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
		return offlinePlayer == null ? "" : offlinePlayer.getKingdom().getName();
	}
	
	public String getExile(Player player) {
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
		return offlinePlayer == null ? "" : offlinePlayer.getExileKingdom().getName();
	}
	
	public String getBarbarian(Player player) {
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
    	return offlinePlayer == null ? "" : boolean2Lang(offlinePlayer.isBarbarian());
	}
	
	public String getTownsLord(Player player) {
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
    	int numTowns = 0;
    	if(offlinePlayer != null) {
    		for(KonTown town : offlinePlayer.getKingdom().getTowns()) {
    			if(town.isPlayerLord(offlinePlayer.getOfflineBukkitPlayer())) {
					numTowns++;
    			}
    		}
    	}
    	return ""+numTowns;
	}
	
	public String getTownsKnight(Player player) {
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
		int numTowns = 0;
    	if(offlinePlayer != null) {
    		for(KonTown town : offlinePlayer.getKingdom().getTowns()) {
    			if(town.isPlayerKnight(offlinePlayer.getOfflineBukkitPlayer()) &&
    					!town.isPlayerLord(offlinePlayer.getOfflineBukkitPlayer())) {
					numTowns++;
    			}
    		}
    	}
    	return ""+numTowns;
	}
	
	public String getTownsResident(Player player) {
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
		int numTowns = 0;
    	if(offlinePlayer != null) {
    		for(KonTown town : offlinePlayer.getKingdom().getTowns()) {
    			if(town.isPlayerResident(offlinePlayer.getOfflineBukkitPlayer()) &&
    					!town.isPlayerKnight(offlinePlayer.getOfflineBukkitPlayer())) {
					numTowns++;
    			}
    		}
    	}
    	return ""+numTowns;
	}
	
	public String getTownsAll(Player player) {
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
		int numTowns = 0;
    	if(offlinePlayer != null) {
    		for(KonTown town : offlinePlayer.getKingdom().getTowns()) {
    			if(town.isPlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
					numTowns++;
    			}
    		}
    	}
    	return ""+numTowns;
	}
	
	public String getTerritory(Player player) {
		String result = "";
    	KonPlayer onlinePlayer = playerManager.getPlayer(player);
    	if(onlinePlayer != null && player.isOnline()) {
        	if(territoryManager.isChunkClaimed(player.getLocation())) {
        		result = Labeler.lookup(territoryManager.getChunkTerritory(player.getLocation()).getTerritoryType());
        	} else {
        		result = Labeler.lookup(KonquestTerritoryType.WILD);
        	}
    	}
    	return result;
	}
	
	public String getLand(Player player) {
		String result = "";
    	KonPlayer onlinePlayer = playerManager.getPlayer(player);
    	if(onlinePlayer != null && player.isOnline()) {
        	if(territoryManager.isChunkClaimed(player.getLocation())) {
        		result = territoryManager.getChunkTerritory(player.getLocation()).getName();
        	} else {
        		result = Labeler.lookup(KonquestTerritoryType.WILD);
        	}
    	}
    	return result;
	}
	
	public String getClaimed(Player player) {
		String result = "";
    	KonPlayer onlinePlayer = playerManager.getPlayer(player);
    	if(onlinePlayer != null && player.isOnline()) {
    		result = boolean2Lang(territoryManager.isChunkClaimed(player.getLocation()));
    	}
    	return result;
	}
	
	public String getScore(Player player) {
    	KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
		return offlinePlayer == null ? "" : String.valueOf(kingdomManager.getPlayerScore(offlinePlayer));
	}
	
	public String getPrefix(Player player) {
		String result = "";
    	KonPlayer onlinePlayer = playerManager.getPlayer(player);
    	if(onlinePlayer != null) {
    		KonPrefix playerPrefix = onlinePlayer.getPlayerPrefix();
			result = playerPrefix.getMainPrefixName();
    	}
    	return result;
	}

	public String getRank(Player player) {
		String result = "";
		KonPlayer onlinePlayer = playerManager.getPlayer(player);
		if(onlinePlayer != null) {
			KonKingdom kingdom = onlinePlayer.getKingdom();
			result = kingdom.getPlayerRankName(player.getUniqueId());
		}
		return result;
	}
	
	public String getLordships(Player player) {
		String result;
    	KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
    	int count = 0;
    	if(offlinePlayer != null) {
    		for(KonTown town : offlinePlayer.getKingdom().getTowns()) {
    			if(town.isPlayerLord(offlinePlayer.getOfflineBukkitPlayer())) {
    				count++;
    			}
    		}
    	}
    	result = String.valueOf(count);
    	return result;
	}
	
	public String getResidencies(Player player) {
		String result;
    	KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
    	int count = 0;
    	if(offlinePlayer != null) {
    		for(KonTown town : offlinePlayer.getKingdom().getTowns()) {
    			if(town.isPlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
    				count++;
    			}
    		}
    	}
    	result = String.valueOf(count);
    	return result;
	}
	
	public String getChat(Player player) {
		KonPlayer onlinePlayer = playerManager.getPlayer(player);
		return onlinePlayer == null ? "" : boolean2Lang(onlinePlayer.isGlobalChat());
	}
	
	public String getCombat(Player player) {
		KonPlayer onlinePlayer = playerManager.getPlayer(player);
		return onlinePlayer == null ? "" : boolean2Lang(onlinePlayer.isCombatTagged());
	}
	
	public String getCombatTag(Player player) {
		KonPlayer onlinePlayer = playerManager.getPlayer(player);
		return (onlinePlayer != null && onlinePlayer.isCombatTagged()) ? ChatUtil.parseHex(konquest.getCore().getString(CorePath.COMBAT_PLACEHOLDER_TAG.getPath(),"")) : "";
	}

	/*
	 * Placeholder Relational Requesters
	 */
	
	public String getRelation(Player playerOne, Player playerTwo) {
		String result = "";
		KonPlayer onlinePlayerOne = playerManager.getPlayer(playerOne);
		KonPlayer onlinePlayerTwo = playerManager.getPlayer(playerTwo);
		if(onlinePlayerOne != null && onlinePlayerTwo != null) {
			KonKingdom kingdomOne = onlinePlayerOne.getKingdom();
			KonKingdom kingdomTwo = onlinePlayerTwo.getKingdom();
			KonquestRelationshipType role = kingdomManager.getRelationRole(kingdomOne, kingdomTwo);
			switch(role) {
		    	case BARBARIAN:
		    		result = MessagePath.PLACEHOLDER_BARBARIAN.getMessage();
					break;
		    	case ENEMY:
		    		result = MessagePath.PLACEHOLDER_ENEMY.getMessage();
		    		break;
		    	case FRIENDLY:
		    		result = MessagePath.PLACEHOLDER_FRIENDLY.getMessage();
		    		break;
		    	case ALLY:
		    		result = MessagePath.PLACEHOLDER_ALLY.getMessage();
		    		break;
		    	case TRADE:
		    		result = MessagePath.PLACEHOLDER_TRADER.getMessage();
		    		break;
		    	case PEACEFUL:
		    		result = MessagePath.PLACEHOLDER_PEACEFUL.getMessage();
		    		break;
	    		default:
	    			break;
	    	}
		}
		return result;
	}
	
	public String getRelationPrimaryColor(Player playerOne, Player playerTwo) {
		String result = "";
		KonPlayer onlinePlayerOne = playerManager.getPlayer(playerOne);
		KonPlayer onlinePlayerTwo = playerManager.getPlayer(playerTwo);
		if(onlinePlayerOne != null && onlinePlayerTwo != null) {
			result = ""+konquest.getDisplayPrimaryColor(onlinePlayerOne, onlinePlayerTwo);
		}
		return result;
	}

	public String getRelationSecondaryColor(Player playerOne, Player playerTwo) {
		String result = "";
		KonPlayer onlinePlayerOne = playerManager.getPlayer(playerOne);
		KonPlayer onlinePlayerTwo = playerManager.getPlayer(playerTwo);
		if(onlinePlayerOne != null && onlinePlayerTwo != null) {
			result = ""+konquest.getDisplaySecondaryColor(onlinePlayerOne, onlinePlayerTwo);
		}
		return result;
	}

	public String getRelationKingdomWebColor(Player playerTwo) {
		String result = "";
		KonPlayer onlinePlayerTwo = playerManager.getPlayer(playerTwo);
		if(onlinePlayerTwo != null) {
			result = onlinePlayerTwo.getKingdom().getWebColorString();
		}
		return result;
	}
	
	/*
	 * Top rankings
	 * These methods may be called repeatedly for multiple players.
	 * Any time a top method is called, check for past cool-down time.
	 * If cool-down time has passed, update rankings and return requested rank, then set new cool-down time.
	 * Do special formatting on Kingdom of requested player.
	 */
	
	/**
	 * 
	 * @param rank - List index starting at 1
	 * @return top score
	 */
	public String getTopScore(int rank) {
		String result = "---";
		Date now = new Date();
		if(now.after(new Date(topScoreCooldownTime))) {
			// The cool-down time is over
			// Create ranked list of kingdoms
			topScoreList = new ArrayList<>();
			for(KonKingdom kingdom : kingdomManager.getKingdoms()) {
				String kingdomName = kingdom.getName();
				topScoreList.add(new Ranked(kingdomName, kingdomManager.getKingdomScore(kingdom)));
			}
			// Sort the list by value
	   		topScoreList.sort(rankedComparator);
	   		// Update cool-down time
	   		topScoreCooldownTime = now.getTime() + (cooldownSeconds* 1000L);
		}
		// Get requested rank
		if(rank > 0 && rank <= topScoreList.size()) {
   			result = topScoreList.get(rank-1).name + " " + topScoreList.get(rank-1).value;
   		}
		return result;
	}
	
	public String getTopTown(int rank) {
		String result = "---";
		Date now = new Date();
		if(now.after(new Date(topTownCooldownTime))) {
			// The cool-down time is over
			// Create ranked list of kingdoms
			topTownList = new ArrayList<>();
			for(KonKingdom kingdom : kingdomManager.getKingdoms()) {
				String kingdomName = kingdom.getName();
				topTownList.add(new Ranked(kingdomName, kingdom.getTowns().size()));
			}
			// Sort the list by value
	   		topTownList.sort(rankedComparator);
	   		// Update cool-down time
	   		topTownCooldownTime = now.getTime() + (cooldownSeconds* 1000L);
		}
		// Get requested rank
		if(rank > 0 && rank <= topTownList.size()) {
   			result = topTownList.get(rank-1).name + " " + topTownList.get(rank-1).value;
   		}
		return result;
	}
	
	public String getTopLand(int rank) {
		String result = "---";
		Date now = new Date();
		if(now.after(new Date(topLandCooldownTime))) {
			// The cool-down time is over
			// Create ranked list of kingdoms
			topLandList = new ArrayList<>();
			for(KonKingdom kingdom : kingdomManager.getKingdoms()) {
				String kingdomName = kingdom.getName();
				int kingdomLand = 0;
				for(KonTown town : kingdom.getCapitalTowns()) {
					kingdomLand += town.getChunkList().size();
				}
				topLandList.add(new Ranked(kingdomName, kingdomLand));
			}
			// Sort the list by value
	   		topLandList.sort(rankedComparator);
	   		// Update cool-down time
	   		topLandCooldownTime = now.getTime() + (cooldownSeconds* 1000L);
		}
		// Get requested rank
		if(rank > 0 && rank <= topLandList.size()) {
   			result = topLandList.get(rank-1).name + " " + topLandList.get(rank-1).value;
   		}
		return result;
	}
	
	/*
	 * Placeholder Kingdom Requesters
	 */

	private boolean isCooldownOver(String nameKey, KingdomValue valueType) {
		// Check for cache entry
		Date now = new Date();
		Long futureTime = now.getTime() + (cooldownSeconds* 1000L);
		boolean isReady = false;
		if (kingdomCooldownTimes.containsKey(nameKey)) {
			// A cache entry exists
			if (now.after(new Date(kingdomCooldownTimes.get(nameKey).get(valueType)))) {
				// The cool-down time is over
				isReady = true;
				kingdomCooldownTimes.get(nameKey).put(valueType, futureTime);
			}
		} else if (kingdomManager.isKingdom(nameKey)) {
			isReady = true;
			// Create a new cache entry for a valid kingdom name
			kingdomCache.put(nameKey,new HashMap<>());
			kingdomCache.get(nameKey).put(valueType, 0);
			kingdomCooldownTimes.put(nameKey,new HashMap<>());
			kingdomCooldownTimes.get(nameKey).put(valueType, futureTime);
		}
		return isReady;
	}

	public String getPlayerKingdomPlayers(Player player) {
		String result = "";
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
		if(offlinePlayer != null) {
			result = getKingdomPlayers(offlinePlayer.getKingdom().getName());
		}
		return result;
	}

	public String getKingdomPlayers(String name) {
		String nameKey = name.toLowerCase();
		KingdomValue type = KingdomValue.PLAYERS;
		String result = "";
		// Try to update the cache
		if (isCooldownOver(nameKey,type) && kingdomCache.containsKey(nameKey)) {
			int value = playerManager.getAllPlayersInKingdom(kingdomManager.getKingdom(name)).size();
			kingdomCache.get(nameKey).put(type, value);
		}
		// Return the result
		if (kingdomCache.containsKey(nameKey)) {
			result = ""+kingdomCache.get(nameKey).get(type);
		}
		return result;
	}

	public String getPlayerKingdomOnline(Player player) {
		String result = "";
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
		if(offlinePlayer != null) {
			result = getKingdomOnline(offlinePlayer.getKingdom().getName());
		}
		return result;
	}

	public String getKingdomOnline(String name) {
		String nameKey = name.toLowerCase();
		KingdomValue type = KingdomValue.ONLINE;
		String result = "";
		// Try to update the cache
		if (isCooldownOver(nameKey,type) && kingdomCache.containsKey(nameKey)) {
			int value = playerManager.getPlayersInKingdom(kingdomManager.getKingdom(name)).size();
			kingdomCache.get(nameKey).put(type, value);
		}
		// Return the result
		if (kingdomCache.containsKey(nameKey)) {
			result = ""+kingdomCache.get(nameKey).get(type);
		}
		return result;
	}

	public String getPlayerKingdomTowns(Player player) {
		String result = "";
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
		if(offlinePlayer != null) {
			result = getKingdomTowns(offlinePlayer.getKingdom().getName());
		}
		return result;
	}

	public String getKingdomTowns(String name) {
		String nameKey = name.toLowerCase();
		KingdomValue type = KingdomValue.TOWNS;
		String result = "";
		// Try to update the cache
		if (isCooldownOver(nameKey,type) && kingdomCache.containsKey(nameKey)) {
			int value = kingdomManager.getKingdom(name).getTowns().size();
			kingdomCache.get(nameKey).put(type, value);
		}
		// Return the result
		if (kingdomCache.containsKey(nameKey)) {
			result = ""+kingdomCache.get(nameKey).get(type);
		}
		return result;
	}

	public String getPlayerKingdomLand(Player player) {
		String result = "";
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
		if(offlinePlayer != null) {
			result = getKingdomLand(offlinePlayer.getKingdom().getName());
		}
		return result;
	}

	public String getKingdomLand(String name) {
		String nameKey = name.toLowerCase();
		KingdomValue type = KingdomValue.LAND;
		String result = "";
		// Try to update the cache
		if (isCooldownOver(nameKey,type) && kingdomCache.containsKey(nameKey)) {
			int value = 0;
			for(KonTown town : kingdomManager.getKingdom(name).getCapitalTowns()) {
				value += town.getChunkList().size();
			}
			kingdomCache.get(nameKey).put(type, value);
		}
		// Return the result
		if (kingdomCache.containsKey(nameKey)) {
			result = ""+kingdomCache.get(nameKey).get(type);
		}
		return result;
	}

	public String getPlayerKingdomFavor(Player player) {
		String result = "";
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
		if(offlinePlayer != null) {
			result = getKingdomFavor(offlinePlayer.getKingdom().getName());
		}
		return result;
	}

	public String getKingdomFavor(String name) {
		String nameKey = name.toLowerCase();
		KingdomValue type = KingdomValue.FAVOR;
		String result = "";
		// Try to update the cache
		if (isCooldownOver(nameKey,type) && kingdomCache.containsKey(nameKey)) {
			int value = 0;
			for(KonOfflinePlayer kingdomPlayer : playerManager.getAllPlayersInKingdom(kingdomManager.getKingdom(name))) {
				value += (int) KonquestPlugin.getBalance(kingdomPlayer.getOfflineBukkitPlayer());
			}
			kingdomCache.get(nameKey).put(type, value);
		}
		// Return the result
		if (kingdomCache.containsKey(nameKey)) {
			result = ""+kingdomCache.get(nameKey).get(type);
		}
		return result;
	}

	public String getPlayerKingdomScore(Player player) {
		String result = "";
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
		if(offlinePlayer != null) {
			result = getKingdomScore(offlinePlayer.getKingdom().getName());
		}
		return result;
	}

	public String getKingdomScore(String name) {
		String nameKey = name.toLowerCase();
		KingdomValue type = KingdomValue.SCORE;
		String result = "";
		// Try to update the cache
		if (isCooldownOver(nameKey,type) && kingdomCache.containsKey(nameKey)) {
			int value = kingdomManager.getKingdomScore(kingdomManager.getKingdom(name));
			kingdomCache.get(nameKey).put(type, value);
		}
		// Return the result
		if (kingdomCache.containsKey(nameKey)) {
			result = ""+kingdomCache.get(nameKey).get(type);
		}
		return result;
	}

	/*
	 * Placeholder Ruin Requesters
	 */

	public String getRuinCooldown(String name) {
		String result = "";
		if(ruinManager.isRuin(name)) {
			result = ruinManager.getRuin(name).getCaptureTime();
		}
		return result;
	}

	public String getRuinCapture(String name) {
		String result = "";
		if(ruinManager.isRuin(name)) {
			result = boolean2Lang(!ruinManager.getRuin(name).isCaptureDisabled());
		}
		return result;
	}

	public String getRuinCriticals(String name) {
		String result = "";
		if(ruinManager.isRuin(name)) {
			result = ""+ruinManager.getRuin(name).getCriticalLocations().size();
		}
		return result;
	}

	public String getRuinSpawns(String name) {
		String result = "";
		if(ruinManager.isRuin(name)) {
			result = ""+ruinManager.getRuin(name).getSpawnLocations().size();
		}
		return result;
	}

	/*
	 * Placeholder Timers
	 */

	public String getTimerLoot(Player player) {
		String result = "";
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
		if(offlinePlayer != null) {
			result = konquest.getLootManager().getMonumentLootTime();
		}
		return result;
	}

	public String getTimerPayment(Player player) {
		String result = "";
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
		if(offlinePlayer != null) {
			result = konquest.getKingdomManager().getKingdomPayTime();
		}
		return result;
	}
	
}
