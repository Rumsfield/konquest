package konquest.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

//import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.api.model.KonquestTerritoryType;
import konquest.model.KonGuild;
import konquest.model.KonKingdom;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.model.KonPrefix;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

//TODO: Add enums for each placeholder, implement list of players per-enum with request cooldown times.
// A placeholder cannot be requested until after the cooldown time, configurable duration.
// Cooldown time applies to every placeholder.
// Previous placeholder result is cached and returned for requests made before cooldown time ends.
public class PlaceholderManager {

	private enum KingdomValue {
		PLAYERS,
		ONLINE,
		TOWNS,
		LAND,
		FAVOR,
		SCORE;
	}
	
	private class Ranked {
		String name;
		int value;
		Ranked(String name, int value) {
			this.name = name;
			this.value = value;
		}
	}
	
	private Konquest konquest;
	private PlayerManager playerManager;
	private KingdomManager kingdomManager;
	private int cooldownSeconds;
	private Comparator<Ranked> rankedComparator;
	private long topScoreCooldownTime;
	private long topTownCooldownTime;
	private long topLandCooldownTime;
	private ArrayList<Ranked> topScoreList;
	private ArrayList<Ranked> topTownList;
	private ArrayList<Ranked> topLandList;
	private HashMap<KingdomValue,HashMap<String,Integer>> kingdomCache;
	private HashMap<KingdomValue,Long> kingdomCooldownTimes;
	
	public PlaceholderManager(Konquest konquest) {
		this.konquest = konquest;
		this.playerManager = konquest.getPlayerManager();
		this.kingdomManager = konquest.getKingdomManager();
		this.cooldownSeconds = 0;
		this.topScoreCooldownTime = 0L;
		this.topTownCooldownTime = 0L;
		this.topLandCooldownTime = 0L;
		this.topScoreList = new ArrayList<Ranked>();
		this.topTownList = new ArrayList<Ranked>();
		this.topLandList = new ArrayList<Ranked>();
		this.kingdomCache = new HashMap<KingdomValue,HashMap<String,Integer>>();
		this.kingdomCooldownTimes = new HashMap<KingdomValue,Long>();
		for(KingdomValue val : KingdomValue.values()) {
			kingdomCache.put(val, new HashMap<String,Integer>());
			kingdomCooldownTimes.put(val, 0L);
		}
		this.rankedComparator = new Comparator<Ranked>() {
   			@Override
   			public int compare(final Ranked k1, Ranked k2) {
   				int result = 0;
   				if(k1.value < k2.value) {
   					result = 1;
   				} else if(k1.value > k2.value) {
   					result = -1;
   				}
   				return result;
   			}
   		};
	}
	
	public void initialize() {
		topScoreList = new ArrayList<Ranked>();
		topTownList = new ArrayList<Ranked>();
		topLandList = new ArrayList<Ranked>();
		kingdomCache = new HashMap<KingdomValue,HashMap<String,Integer>>();
		kingdomCooldownTimes = new HashMap<KingdomValue,Long>();
		for(KingdomValue val : KingdomValue.values()) {
			kingdomCache.put(val, new HashMap<String,Integer>());
			kingdomCooldownTimes.put(val, 0L);
		}
		cooldownSeconds = konquest.getConfigManager().getConfig("core").getInt("core.placeholder_request_limit",0);
		ChatUtil.printDebug("Placeholder Manager is ready with cooldown seconds: "+cooldownSeconds);
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
		String result = "";
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
    	result = offlinePlayer == null ? "" : offlinePlayer.getKingdom().getName();
    	return result;
	}
	
	public String getExile(Player player) {
		String result = "";
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
    	result = offlinePlayer == null ? "" : offlinePlayer.getExileKingdom().getName();
    	return result;
	}
	
	public String getBarbarian(Player player) {
		String result = "";
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
    	result = offlinePlayer == null ? "" : boolean2Lang(offlinePlayer.isBarbarian());
    	return result;
	}
	
	public String getTownsLord(Player player) {
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
    	String list = "";
    	if(offlinePlayer != null) {
    		for(KonTown town : offlinePlayer.getKingdom().getTowns()) {
    			if(town.isPlayerLord(offlinePlayer.getOfflineBukkitPlayer())) {
    				list = list + town.getName() + ",";
    			}
    		}
    		if(list.length() > 1) {
    			list = list.substring(0,list.length()-1);
			}
    	}
    	return list;
	}
	
	public String getTownsKnight(Player player) {
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
		String list = "";
    	if(offlinePlayer != null) {
    		for(KonTown town : offlinePlayer.getKingdom().getTowns()) {
    			if(town.isPlayerKnight(offlinePlayer.getOfflineBukkitPlayer()) &&
    					!town.isPlayerLord(offlinePlayer.getOfflineBukkitPlayer())) {
    				list = list + town.getName() + ",";
    			}
    		}
    		if(list.length() > 1) {
    			list = list.substring(0,list.length()-1);
			}
    	}
    	return list;
	}
	
	public String getTownsResident(Player player) {
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
		String list = "";
    	if(offlinePlayer != null) {
    		for(KonTown town : offlinePlayer.getKingdom().getTowns()) {
    			if(town.isPlayerResident(offlinePlayer.getOfflineBukkitPlayer()) &&
    					!town.isPlayerKnight(offlinePlayer.getOfflineBukkitPlayer())) {
    				list = list + town.getName() + ",";
    			}
    		}
    		if(list.length() > 1) {
    			list = list.substring(0,list.length()-1);
			}
    	}
    	return list;
	}
	
	public String getTownsAll(Player player) {
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
		String list = "";
    	if(offlinePlayer != null) {
    		for(KonTown town : offlinePlayer.getKingdom().getTowns()) {
    			if(town.isPlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
    				list = list + town.getName() + ",";
    			}
    		}
    		if(list.length() > 1) {
    			list = list.substring(0,list.length()-1);
			}
    	}
    	return list;
	}
	
	public String getTerritory(Player player) {
		String result = "";
    	KonPlayer onlinePlayer = playerManager.getPlayer(player);
    	if(onlinePlayer != null && player.isOnline()) {
        	if(kingdomManager.isChunkClaimed(player.getLocation())) {
        		result = kingdomManager.getChunkTerritory(player.getLocation()).getTerritoryType().getLabel();
        	} else {
        		result = KonquestTerritoryType.WILD.getLabel();
        	}
    	}
    	return result;
	}
	
	public String getLand(Player player) {
		String result = "";
    	KonPlayer onlinePlayer = playerManager.getPlayer(player);
    	if(onlinePlayer != null && player.isOnline()) {
        	if(kingdomManager.isChunkClaimed(player.getLocation())) {
        		result = kingdomManager.getChunkTerritory(player.getLocation()).getName();
        	} else {
        		result = KonquestTerritoryType.WILD.getLabel();
        	}
    	}
    	return result;
	}
	
	public String getClaimed(Player player) {
		String result = "";
    	KonPlayer onlinePlayer = playerManager.getPlayer(player);
    	if(onlinePlayer != null && player.isOnline()) {
    		result = boolean2Lang(kingdomManager.isChunkClaimed(player.getLocation()));
    	}
    	return result;
	}
	
	public String getScore(Player player) {
		String result = "";
    	KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
    	result = offlinePlayer == null ? "" : String.valueOf(kingdomManager.getPlayerScore(offlinePlayer));
    	return result;
	}
	
	public String getPrefix(Player player) {
		String result = "";
    	KonPlayer onlinePlayer = playerManager.getPlayer(player);
    	if(onlinePlayer != null) {
    		KonPrefix playerPrefix = onlinePlayer.getPlayerPrefix();
    		if(playerPrefix.isEnabled()) {
    			result = playerPrefix.getMainPrefixName();
    		} else {
    			result = "";
    		}
    	}
    	return result;
	}
	
	public String getLordships(Player player) {
		String result = "";
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
		String result = "";
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
		String result = "";
		KonPlayer onlinePlayer = playerManager.getPlayer(player);
    	result = onlinePlayer == null ? "" : boolean2Lang(onlinePlayer.isGlobalChat());
    	return result;
	}
	
	public String getCombat(Player player) {
		String result = "";
		KonPlayer onlinePlayer = playerManager.getPlayer(player);
    	result = onlinePlayer == null ? "" : boolean2Lang(onlinePlayer.isCombatTagged());
    	return result;
	}
	
	public String getCombatTag(Player player) {
		String result = "";
		KonPlayer onlinePlayer = playerManager.getPlayer(player);
    	result = (onlinePlayer != null && onlinePlayer.isCombatTagged()) ? ChatUtil.parseHex(konquest.getConfigManager().getConfig("core").getString("core.combat.placeholder_tag","")) : "";
    	return result;
	}
	
	public String getGuild(Player player) {
		String result = "";
		KonGuild guild = konquest.getGuildManager().getPlayerGuild(player);
    	result = guild == null ? "" : guild.getName();
    	return result;
	}
	
	/*
	 * Placeholder Relational Requesters
	 */
	
	public String getRelation(Player playerOne, Player playerTwo) {
		String result = "";
		KonPlayer onlinePlayerOne = playerManager.getPlayer(playerOne);
		KonPlayer onlinePlayerTwo = playerManager.getPlayer(playerTwo);
		if(onlinePlayerOne != null && onlinePlayerTwo != null) {
			boolean isArmistice = konquest.getGuildManager().isArmistice(onlinePlayerOne, onlinePlayerTwo);
	    	if(onlinePlayerTwo.isBarbarian()) {
	    		result = MessagePath.PLACEHOLDER_BARBARIAN.getMessage();
			} else {
				if(onlinePlayerOne.getKingdom().equals(onlinePlayerTwo.getKingdom())) {
					result = MessagePath.PLACEHOLDER_FRIENDLY.getMessage();
	    		} else {
	    			if(isArmistice) {
	    				result = MessagePath.PLACEHOLDER_ARMISTICE.getMessage();
	    			} else {
	    				result = MessagePath.PLACEHOLDER_ENEMY.getMessage();
	    			}
	    		}
			}
		}
		return result;
	}
	
	public String getRelationColor(Player playerOne, Player playerTwo) {
		String result = "";
		KonPlayer onlinePlayerOne = playerManager.getPlayer(playerOne);
		KonPlayer onlinePlayerTwo = playerManager.getPlayer(playerTwo);
		if(onlinePlayerOne != null && onlinePlayerTwo != null) {
			result = ""+konquest.getDisplayPrimaryColor(onlinePlayerOne, onlinePlayerTwo);
		}
		return result;
	}
	
	/*
	 * Top rankings
	 * These methods may be called repeatedly for multiple players.
	 * Any time a top method is called, check for past cooldown time.
	 * If cooldown time has passed, update rankings and return requested rank, then set new cooldown time.
	 * Do special formatting on Kingdom of requested player.
	 */
	
	/**
	 * 
	 * @param player
	 * @param rank - List index starting at 1
	 * @return
	 */
	public String getTopScore(Player player, int rank) {
		String result = "---";
		Date now = new Date();
		if(now.after(new Date(topScoreCooldownTime))) {
			ChatUtil.printDebug("Fetching new placeholder top score");
			// The cooldown time is over
			// Create ranked list of kingdoms
			//KonPlayer onlinePlayer = playerManager.getPlayer(player);
			topScoreList = new ArrayList<Ranked>();
			for(KonKingdom kingdom : kingdomManager.getKingdoms()) {
				String kingdomName = kingdom.getName();
				//if(onlinePlayer != null && onlinePlayer.getKingdom().equals(kingdom)) {
				//	kingdomName = ChatColor.GREEN+kingdomName;
				//}
				topScoreList.add(new Ranked(kingdomName, kingdomManager.getKingdomScore(kingdom)));
			}
			// Sort the list by value
	   		Collections.sort(topScoreList, rankedComparator);
	   		// Update cooldown time
	   		topScoreCooldownTime = now.getTime() + (cooldownSeconds*1000);
		} else {
			ChatUtil.printDebug("Fetching cached placeholder top score");
		}
		// Get requested rank
		if(rank > 0 && rank <= topScoreList.size()) {
   			result = topScoreList.get(rank-1).name + " " + topScoreList.get(rank-1).value;
   		}
		return result;
	}
	
	public String getTopTown(Player player, int rank) {
		String result = "---";
		Date now = new Date();
		if(now.after(new Date(topTownCooldownTime))) {
			// The cooldown time is over
			// Create ranked list of kingdoms
			//KonPlayer onlinePlayer = playerManager.getPlayer(player);
			topTownList = new ArrayList<Ranked>();
			for(KonKingdom kingdom : kingdomManager.getKingdoms()) {
				String kingdomName = kingdom.getName();
				//if(onlinePlayer != null && onlinePlayer.getKingdom().equals(kingdom)) {
				//	kingdomName = ChatColor.GREEN+kingdomName;
				//}
				topTownList.add(new Ranked(kingdomName, kingdom.getTowns().size()));
			}
			// Sort the list by value
	   		Collections.sort(topTownList, rankedComparator);
	   		// Update cooldown time
	   		topTownCooldownTime = now.getTime() + (cooldownSeconds*1000);
		}
		// Get requested rank
		if(rank > 0 && rank <= topTownList.size()) {
   			result = topTownList.get(rank-1).name + " " + topTownList.get(rank-1).value;
   		}
		return result;
	}
	
	public String getTopLand(Player player, int rank) {
		String result = "---";
		Date now = new Date();
		if(now.after(new Date(topLandCooldownTime))) {
			// The cooldown time is over
			// Create ranked list of kingdoms
			//KonPlayer onlinePlayer = playerManager.getPlayer(player);
			topLandList = new ArrayList<Ranked>();
			for(KonKingdom kingdom : kingdomManager.getKingdoms()) {
				String kingdomName = kingdom.getName();
				int kingdomLand = 0;
				//if(onlinePlayer != null && onlinePlayer.getKingdom().equals(kingdom)) {
				//	kingdomName = ChatColor.GREEN+kingdomName;
				//}
				for(KonTown town : kingdom.getTowns()) {
					kingdomLand += town.getChunkList().size();
				}
				topLandList.add(new Ranked(kingdomName, kingdomLand));
			}
			// Sort the list by value
	   		Collections.sort(topLandList, rankedComparator);
	   		// Update cooldown time
	   		topLandCooldownTime = now.getTime() + (cooldownSeconds*1000);
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
	
	public String getKingdomPlayers(String name) {
		String result = "";
		// Check cooldown time, update cache if expired
		KingdomValue type = KingdomValue.PLAYERS;
		if(kingdomManager.isKingdom(name) && 
				kingdomCooldownTimes.containsKey(type) &&
				kingdomCache.containsKey(type)) {
			Date now = new Date();
			if(now.after(new Date(kingdomCooldownTimes.get(type)))) {
				// Cooldown is expired, update value
				int value = playerManager.getAllPlayersInKingdom(kingdomManager.getKingdom(name)).size();
				kingdomCache.get(type).put(name, value);
				// Update new cooldown time
				kingdomCooldownTimes.put(type, now.getTime() + (cooldownSeconds*1000));
			}
			// Get value
			result = ""+kingdomCache.get(type).get(name);
		}
		return result;
	}
	
	public String getKingdomOnline(String name) {
		String result = "";
		// Check cooldown time, update cache if expired
		KingdomValue type = KingdomValue.ONLINE;
		if(kingdomManager.isKingdom(name) && 
				kingdomCooldownTimes.containsKey(type) &&
				kingdomCache.containsKey(type)) {
			Date now = new Date();
			if(now.after(new Date(kingdomCooldownTimes.get(type)))) {
				// Cooldown is expired, update value
				int value = playerManager.getPlayersInKingdom(kingdomManager.getKingdom(name)).size();
				kingdomCache.get(type).put(name, value);
				// Update new cooldown time
				kingdomCooldownTimes.put(type, now.getTime() + (cooldownSeconds*1000));
			}
			// Get value
			result = ""+kingdomCache.get(type).get(name);
		}
		return result;
	}
	
	public String getKingdomTowns(String name) {
		String result = "";
		// Check cooldown time, update cache if expired
		KingdomValue type = KingdomValue.TOWNS;
		if(kingdomManager.isKingdom(name) && 
				kingdomCooldownTimes.containsKey(type) &&
				kingdomCache.containsKey(type)) {
			Date now = new Date();
			if(now.after(new Date(kingdomCooldownTimes.get(type)))) {
				// Cooldown is expired, update value
				int value = kingdomManager.getKingdom(name).getTowns().size();
				kingdomCache.get(type).put(name, value);
				// Update new cooldown time
				kingdomCooldownTimes.put(type, now.getTime() + (cooldownSeconds*1000));
			}
			// Get value
			result = ""+kingdomCache.get(type).get(name);
		}
		return result;
	}
	
	public String getKingdomLand(String name) {
		String result = "";
		// Check cooldown time, update cache if expired
		KingdomValue type = KingdomValue.LAND;
		if(kingdomManager.isKingdom(name) && 
				kingdomCooldownTimes.containsKey(type) &&
				kingdomCache.containsKey(type)) {
			Date now = new Date();
			if(now.after(new Date(kingdomCooldownTimes.get(type)))) {
				// Cooldown is expired, update value
				int value = 0;
		    	for(KonTown town : kingdomManager.getKingdom(name).getTowns()) {
		    		value += town.getChunkList().size();
		    	}
				kingdomCache.get(type).put(name, value);
				// Update new cooldown time
				kingdomCooldownTimes.put(type, now.getTime() + (cooldownSeconds*1000));
			}
			// Get value
			result = ""+kingdomCache.get(type).get(name);
		}
		return result;
	}
	
	public String getKingdomFavor(String name) {
		String result = "";
		// Check cooldown time, update cache if expired
		KingdomValue type = KingdomValue.FAVOR;
		if(kingdomManager.isKingdom(name) && 
				kingdomCooldownTimes.containsKey(type) &&
				kingdomCache.containsKey(type)) {
			Date now = new Date();
			if(now.after(new Date(kingdomCooldownTimes.get(type)))) {
				// Cooldown is expired, update value
				int value = 0;
		    	for(KonOfflinePlayer kingdomPlayer : playerManager.getAllPlayersInKingdom(kingdomManager.getKingdom(name))) {
		    		value += (int) KonquestPlugin.getBalance(kingdomPlayer.getOfflineBukkitPlayer());
		    	}
				kingdomCache.get(type).put(name, value);
				// Update new cooldown time
				kingdomCooldownTimes.put(type, now.getTime() + (cooldownSeconds*1000));
			}
			// Get value
			result = ""+kingdomCache.get(type).get(name);
		}
		return result;
	}
	
	public String getKingdomScore(String name) {
		String result = "";
		// Check cooldown time, update cache if expired
		KingdomValue type = KingdomValue.SCORE;
		if(kingdomManager.isKingdom(name) && 
				kingdomCooldownTimes.containsKey(type) &&
				kingdomCache.containsKey(type)) {
			Date now = new Date();
			if(now.after(new Date(kingdomCooldownTimes.get(type)))) {
				// Cooldown is expired, update value
				int value = kingdomManager.getKingdomScore(kingdomManager.getKingdom(name));
				kingdomCache.get(type).put(name, value);
				// Update new cooldown time
				kingdomCooldownTimes.put(type, now.getTime() + (cooldownSeconds*1000));
			}
			// Get value
			result = ""+kingdomCache.get(type).get(name);
		}
		return result;
	}
	
}
