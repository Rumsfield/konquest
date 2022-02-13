package konquest.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.api.manager.KonquestPlayerManager;
import konquest.model.KonKingdom;
import konquest.model.KonMonument;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class PlayerManager implements KonquestPlayerManager {

	private Konquest konquest;
	private HashMap<Player, KonPlayer> onlinePlayers;
	private ConcurrentHashMap<OfflinePlayer, KonOfflinePlayer> allPlayers;
	private ArrayList<String> blockedCommands;
	//private HashMap<String, Integer> kingdomOnlinePlayerCount;

	public PlayerManager(Konquest konquest) {
		this.konquest = konquest;
		onlinePlayers = new HashMap<Player, KonPlayer>();
		allPlayers = new ConcurrentHashMap<OfflinePlayer, KonOfflinePlayer>();
		blockedCommands = new ArrayList<String>();
		//kingdomOnlinePlayerCount = new HashMap<String, Integer>();
	}
	
	public void initialize() {
		//loadPlayers();
		//updateAllSavedPlayers();
		//initAllSavedPlayers();
		// Gather all block commands from core config
		blockedCommands.clear();
		blockedCommands.addAll(konquest.getConfigManager().getConfig("core").getStringList("core.combat.prevent_command_list"));
		
		//blockedCommands
		
		ChatUtil.printDebug("Player Manager is ready");
	}
	
	public ArrayList<String> getBlockedCommands() {
		return blockedCommands;
	}
	
	public KonPlayer removePlayer(Player bukkitPlayer) {
		KonPlayer player = onlinePlayers.remove(bukkitPlayer);
		ChatUtil.printDebug("Removed online player: "+bukkitPlayer.getName());
		return player;
	}
	
	public KonPlayer getPlayer(Player bukkitPlayer) {
		return onlinePlayers.get(bukkitPlayer);
	}
	
	public boolean isPlayer(Player bukkitPlayer) {
		return onlinePlayers.containsKey(bukkitPlayer);
	}
	
	
	public KonOfflinePlayer getOfflinePlayer(OfflinePlayer offlineBukkitPlayer) {
		return allPlayers.get(offlineBukkitPlayer);
	}
	
	public boolean isPlayerExist(OfflinePlayer offlineBukkitPlayer) {
		return allPlayers.containsKey(offlineBukkitPlayer);
	}
	
	public boolean isPlayerNameExist(String name) {
		boolean result = false;
		for(OfflinePlayer offlineBukkitPlayer : allPlayers.keySet()) {
			if(offlineBukkitPlayer.getName() != null && name.equalsIgnoreCase(offlineBukkitPlayer.getName())) {
				result = true;
				break;
			}
		}
		return result;
	}
	
	/**
	 * This method is used for creating a new KonPlayer that has never joined before
	 * @param bukkitPlayer
	 * @return
	 */
	public KonPlayer createKonPlayer(Player bukkitPlayer) {
		KonPlayer newPlayer = new KonPlayer(bukkitPlayer, konquest.getKingdomManager().getBarbarians(), true);
        onlinePlayers.put(bukkitPlayer, newPlayer);
        linkOnlinePlayerToCache(newPlayer);
        /*if(!isBarbarian && bukkitPlayer.getBedSpawnLocation() == null) {
        	bukkitPlayer.setBedSpawnLocation(kingdom.getCapital().getSpawnLoc(), true);
        	ChatUtil.printDebug("Set bed spawn location for player: "+bukkitPlayer.getName());
        }*/
        //player.getBukkitPlayer().setBedSpawnLocation(kingdom.getCapital().getSpawnLoc());
        ChatUtil.printDebug("Created player "+bukkitPlayer.getName());
        return newPlayer;
    }
	
	/**
	 * This method is used for instantiating a KonPlayer using info from the database
	 * @param bukkitPlayer
	 * @param kingdomName
	 * @param exileKingdomName
	 * @param isBarbarian
	 * @return
	 */
	public KonPlayer importKonPlayer(Player bukkitPlayer, String kingdomName, String exileKingdomName, boolean isBarbarian) {
		KonPlayer importedPlayer;

    	// Create KonPlayer instance
    	if(isBarbarian) {
    		// Player data has barbarian flag set true, create barbarian player
    		importedPlayer = new KonPlayer(bukkitPlayer, konquest.getKingdomManager().getBarbarians(), true);
    	} else {
    		// Player's barbarian flag is false, should belong to a kingdom
    		KonKingdom playerKingdom = konquest.getKingdomManager().getKingdom(kingdomName);
    		if(playerKingdom.equals(konquest.getKingdomManager().getBarbarians())) {
    			// The player's kingdom is barbarians but barbarian flag is false
    			// The player's kingdom was probably removed or changed names while offline
    			// Override the barbarian flag to force the player into barbarians
    			importedPlayer = new KonPlayer(bukkitPlayer, konquest.getKingdomManager().getBarbarians(), true);
    			ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_FORCE_BARBARIAN.getMessage());
    			ChatUtil.printDebug("Forced non-barbarian player with no kingdom to become a barbarian");
    		} else {
    			// The player has a valid kingdom and barbarian flag is false
    			importedPlayer = new KonPlayer(bukkitPlayer, playerKingdom, false);
    		}
    	}
    	
    	// Check if player has exceeded offline timeout
    	//long timeoutSeconds = konquest.getConfigManager().getConfig("core").getLong("core.kingdoms.offline_timeout_seconds");
    	long timeoutSeconds = konquest.getOfflineTimeoutSeconds();
    	long lastSeenTimeMilliseconds = bukkitPlayer.getLastPlayed();
    	boolean isOfflineTimeout = false;
    	// ignore this check if timeout is 0 or if there is no lastSeen timestamp
    	if(timeoutSeconds != 0 && lastSeenTimeMilliseconds != 0) {
    		Date now = new Date();
    		if(now.after(new Date(lastSeenTimeMilliseconds + (timeoutSeconds*1000)))) {
    			ChatUtil.printDebug("Player has exceeded offline timeout");
    			isOfflineTimeout = true;
    		}
    	}
    	
    	// Inform player of purged residencies
    	if(isOfflineTimeout) {
    		//ChatUtil.sendNotice(bukkitPlayer, ChatColor.RED+"You have been absent for too long and have been kicked from all towns.");
    		ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_ABSENT.getMessage());
    		ChatUtil.printDebug("Exiled player");
    		/*
    		// Exile the player and set their exile kingdom to barbarian so they can choose a new Kingdom.
    		if(konquest.getKingdomManager().exilePlayer(importedPlayer)) {
    			importedPlayer.setExileKingdom(konquest.getKingdomManager().getBarbarians());
    			ChatUtil.sendNotice(importedPlayer.getBukkitPlayer(), "You have been absent for too long and are now exiled as a "+ChatColor.DARK_RED+"Barbarian");
	    		ChatUtil.printDebug("Exiled player");
	    	} else {
	    		ChatUtil.printDebug("Could not properly exile player");
	    	}
	    	*/
    	}
    	
    	// Update player's exile kingdom
    	if(konquest.getKingdomManager().isKingdom(exileKingdomName)) {
    		// Apply saved info
    		importedPlayer.setExileKingdom(konquest.getKingdomManager().getKingdom(exileKingdomName));
    	} else {
    		// By default, make exile kingdom barbarians
    		importedPlayer.setExileKingdom(konquest.getKingdomManager().getBarbarians());
    	}
    	
    	if(onlinePlayers.containsKey(bukkitPlayer)) {
    		ChatUtil.printDebug("Skipped importing existing player "+bukkitPlayer.getName());
    	} else {
    		onlinePlayers.put(bukkitPlayer, importedPlayer);
        	linkOnlinePlayerToCache(importedPlayer);
        	ChatUtil.printDebug("Imported player "+bukkitPlayer.getName());
    	}
    	
    	return importedPlayer;
	}
	
	/**
	 * This method puts an online player into the allPlayers cache and removes any existing duplicates by UUID.
	 * This ensures that online players with changed fields will be visible to other methods utilizing the cache for player data.
	 * @param player
	 */
	private void linkOnlinePlayerToCache(KonPlayer player) {
		OfflinePlayer bukkitOfflinePlayer = player.getOfflineBukkitPlayer();
		// Search cache for duplicates and remove
		ArrayList<OfflinePlayer> removingList = new ArrayList<OfflinePlayer>();
		for(OfflinePlayer offline : allPlayers.keySet()) {
			if(offline.getUniqueId().equals(bukkitOfflinePlayer.getUniqueId())) {
				removingList.add(offline);
			}
		}
		for(OfflinePlayer offline : removingList) {
			allPlayers.remove(offline);
		}
		// Put online player into cache
		if(bukkitOfflinePlayer.getName() != null) {
			allPlayers.put(bukkitOfflinePlayer, (KonOfflinePlayer)player);
		}
	}

    public KonPlayer getPlayerFromName(String displayName) {
    	//ChatUtil.printDebug("Finding player by name: "+displayName);
    	for(KonPlayer player : onlinePlayers.values()) {
    		//ChatUtil.printDebug("Checking player name "+player.getBukkitPlayer().getName());
    		if(player != null &&
    				player.getBukkitPlayer().getName() != null &&
    				player.getBukkitPlayer().getName().equalsIgnoreCase(displayName)) {
    			return player;
    		}
    	}
        return null;
    }
    
    public KonOfflinePlayer getAllPlayerFromName(String displayName) {
    	//ChatUtil.printDebug("Finding all player by name: "+displayName);
    	for(KonOfflinePlayer offlinePlayer : allPlayers.values()) {
    		if(offlinePlayer != null && 
    				offlinePlayer.getOfflineBukkitPlayer().getName() != null &&
    				offlinePlayer.getOfflineBukkitPlayer().getName().equalsIgnoreCase(displayName)) {
    			return offlinePlayer;
    		}
    	}
        return null;
    }
    
    public Collection<OfflinePlayer> getAllOfflinePlayers() {
    	return allPlayers.keySet();
    }
    
    public Collection<KonOfflinePlayer> getAllKonOfflinePlayers() {
    	return allPlayers.values();
    }
    
    public ArrayList<KonPlayer> getPlayersInKingdom(String kingdomName) {
    	ArrayList<KonPlayer> playerList = new ArrayList<KonPlayer>();
    	for(KonPlayer player : onlinePlayers.values()) {
    		if(player.getKingdom().getName().equalsIgnoreCase(kingdomName)) {
    			playerList.add(player);
    		}
    	}
    	return playerList;
    }
    
    public ArrayList<KonPlayer> getPlayersInKingdom(KonKingdom kingdom) {
    	ArrayList<KonPlayer> playerList = new ArrayList<KonPlayer>();
    	for(KonPlayer player : onlinePlayers.values()) {
    		if(player.getKingdom().equals(kingdom)) {
    			playerList.add(player);
    		}
    	}
    	return playerList;
    }
    
    public ArrayList<String> getPlayerNamesInKingdom(String kingdomName) {
    	ArrayList<String> playerNameList = new ArrayList<String>();
    	for(KonPlayer player : onlinePlayers.values()) {
    		if(player.getKingdom().getName().equalsIgnoreCase(kingdomName)) {
    			playerNameList.add(player.getBukkitPlayer().getName());
    		}
    	}
    	return playerNameList;
    }
    
    public ArrayList<KonOfflinePlayer> getAllPlayersInKingdom(String kingdomName) {
    	//ChatUtil.printDebug("Gathering all players in Kingdom "+kingdomName);
    	ArrayList<KonOfflinePlayer> playerList = new ArrayList<KonOfflinePlayer>();
    	for(KonOfflinePlayer player : allPlayers.values()) {
    		if(player.getKingdom().getName().equalsIgnoreCase(kingdomName)) {
    			playerList.add(player);
    		}
    	}
    	return playerList;
    }
    
    public ArrayList<KonOfflinePlayer> getAllPlayersInKingdom(KonKingdom kingdom) {
    	//ChatUtil.printDebug("Gathering all players in Kingdom "+kingdomName);
    	ArrayList<KonOfflinePlayer> playerList = new ArrayList<KonOfflinePlayer>();
    	for(KonOfflinePlayer player : allPlayers.values()) {
    		if(player.getKingdom().equals(kingdom)) {
    			playerList.add(player);
    		}
    	}
    	return playerList;
    }
    
    public ArrayList<KonPlayer> getPlayersInMonument(KonMonument monument) {
    	ArrayList<KonPlayer> playerList = new ArrayList<KonPlayer>();
    	Chunk monumentChunk = monument.getTravelPoint().getChunk();
    	Chunk playerChunk;
    	int playerY;
    	for(KonPlayer player : onlinePlayers.values()) {
    		playerChunk = player.getBukkitPlayer().getLocation().getChunk();
    		playerY = player.getBukkitPlayer().getLocation().getBlockY();
    		boolean isInChunk = playerChunk.getX() == monumentChunk.getX() && playerChunk.getZ() == monumentChunk.getZ();
    		if(isInChunk && playerY >= monument.getBaseY() && playerY <= monument.getTopY()) {
    			playerList.add(player);
    		}
    	}
    	return playerList;
    }
    
    public Collection<KonPlayer> getPlayersOnline() {
    	return onlinePlayers.values();
    }
    
    public void initAllSavedPlayers() {
    	allPlayers.clear();
    	//ChatUtil.printDebug("Initializing PlayerManager allPlayers cache... ");
    	for(KonOfflinePlayer offlinePlayer : konquest.getDatabaseThread().getDatabase().getAllSavedPlayers()) {
    		//ChatUtil.printDebug("Adding offlinePlayer "+offlinePlayer.getOfflineBukkitPlayer().getName());
    		allPlayers.put(offlinePlayer.getOfflineBukkitPlayer(), offlinePlayer);
    	}
    }

}
