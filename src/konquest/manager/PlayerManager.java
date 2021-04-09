package konquest.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.model.KonKingdom;
import konquest.model.KonMonument;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;

public class PlayerManager {

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
	/*
	public KonPlayer addPlayer(Player bukkitPlayer) {
		FileConfiguration playerConfig = konquest.getConfigManager().getConfig("onlinePlayers");
		String playerId = bukkitPlayer.getUniqueId().toString();
		KonPlayer player;
    	if(playerConfig.contains(playerId)) {
    		player = createKonPlayer(bukkitPlayer, konquest.getKingdomManager().getKingdom(playerConfig.getString(playerId+".kingdom")), playerConfig.getBoolean(playerId+".isBarbarian"));
    		ChatUtil.printDebug("Added existing player: "+bukkitPlayer.getName());
    	} else {
    		player = createKonPlayer(bukkitPlayer,konquest.getKingdomManager().getBarbarians(),true);
    		ChatUtil.printDebug("Added new player: "+bukkitPlayer.getName());
    	}
    	return player;
	}*/
	
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
	
	public KonOfflinePlayer getOfflinePlayer(OfflinePlayer offlineBukkitPlayer) {
		return allPlayers.get(offlineBukkitPlayer);
	}
	
	public boolean isPlayerExist(OfflinePlayer offlineBukkitPlayer) {
		return allPlayers.containsKey(offlineBukkitPlayer);
	}
	
	public boolean isPlayerNameExist(String name) {
		boolean result = false;
		for(OfflinePlayer offlineBukkitPlayer : allPlayers.keySet()) {
			if(name.equalsIgnoreCase(offlineBukkitPlayer.getName())) {
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
    		importedPlayer = new KonPlayer(bukkitPlayer, konquest.getKingdomManager().getBarbarians(), true);
    	} else {
    		importedPlayer = new KonPlayer(bukkitPlayer, konquest.getKingdomManager().getKingdom(kingdomName), false);
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
    		ChatUtil.sendNotice(bukkitPlayer, ChatColor.RED+"You have been absent for too long and have been kicked from all towns.");
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
    	
    	onlinePlayers.put(bukkitPlayer, importedPlayer);
    	linkOnlinePlayerToCache(importedPlayer);
    	ChatUtil.printDebug("Imported player "+bukkitPlayer.getName());
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
		allPlayers.put(bukkitOfflinePlayer, (KonOfflinePlayer)player);
	}

    public KonPlayer getPlayerFromName(String displayName) {
    	//ChatUtil.printDebug("Finding player by name: "+displayName);
    	for(KonPlayer player : onlinePlayers.values()) {
    		//ChatUtil.printDebug("Checking player name "+player.getBukkitPlayer().getName());
    		if(player.getBukkitPlayer().getName().equalsIgnoreCase(displayName)) {
    		//if(player.getBukkitPlayer().getUniqueId().equals(Bukkit.getPlayer(displayName).getUniqueId())) {
    			return player;
    		}
    	}
        return null;
    }
    
    public KonOfflinePlayer getAllPlayerFromName(String displayName) {
    	//ChatUtil.printDebug("Finding all player by name: "+displayName);
    	for(KonOfflinePlayer offlinePlayer : allPlayers.values()) {
    		if(offlinePlayer != null && offlinePlayer.getOfflineBukkitPlayer().getName().equalsIgnoreCase(displayName)) {
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
    
    /*
    public int getNumKingdomPlayersOnline(String kingdomName) {
    	if(kingdomOnlinePlayerCount.containsKey(kingdomName)) {
    		return kingdomOnlinePlayerCount.get(kingdomName);
    	}
    	return -1;
    }
    
    public void updateNumKingdomPlayersOnline() {
    	kingdomOnlinePlayerCount.clear();
    	for(KonKingdom kingdom : konquest.getKingdomManager().getKingdoms()) {
    		kingdomOnlinePlayerCount.put(kingdom.getName(), 0);
    	}
    	for(KonPlayer player : onlinePlayers.values()) {
    		String kingdomName = player.getKingdom().getName();
			int oldNum = kingdomOnlinePlayerCount.get(kingdomName);
			kingdomOnlinePlayerCount.put(kingdomName, oldNum+1);
    	}
    }*/
    
    // Economy-related methods
    
    
    // Loading and Saving methods
    /*
    private void loadPlayers() {
    	FileConfiguration onlinePlayersConfig = konquest.getConfigManager().getConfig("onlinePlayers");
        if (onlinePlayersConfig.get("onlinePlayers") == null) {
        	ChatUtil.printDebug("There is no onlinePlayers section in onlinePlayers.yml");
            return;
        }
        // Load all Players
        for(String uuid : onlinePlayersConfig.getConfigurationSection("onlinePlayers").getKeys(false)) {
        	Player bukkitPlayer = Bukkit.getPlayer(UUID.fromString(uuid));
        	ConfigurationSection playerSection = onlinePlayersConfig.getConfigurationSection("onlinePlayers."+uuid);
        	String kingdomName = playerSection.getString("kingdom");
        	boolean isBarbarian = playerSection.getBoolean("barbarian");
        	createKonPlayer(bukkitPlayer, konquest.getKingdomManager().getKingdom(kingdomName), isBarbarian);
        }
        ChatUtil.printDebug("Loaded Players");
    }*/
    
    /*
    public void updateAllSavedPlayers() {
    	//ChatUtil.printDebug("Updating all players list from players.yml");
    	allPlayers.clear();
    	FileConfiguration playersConfig = konquest.getConfigManager().getConfig("players");
        if (playersConfig.get("players") == null) {
        	ChatUtil.printDebug("There is no players section in players.yml");
            return;
        }
        ConfigurationSection playersSection = playersConfig.getConfigurationSection("players");
        Set<String> playerSet = playersSection.getKeys(false);
        for(String uuid : playerSet) {
        	String kingdomName = playersSection.getString(uuid+".kingdom");
        	//ChatUtil.printDebug("... kingdom is "+kingdomName);
        	//String exileKingdomName = playersConfig.getString("players."+uuid+".exileKingdom");
        	boolean isBarbarian = playersSection.getBoolean(uuid+".barbarian"); 
        	//ChatUtil.printDebug("... isBarbarian is "+isBarbarian);
        	//ChatUtil.printDebug("Loading id "+uuid);
        	OfflinePlayer offlineBukkitPlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        	// Check for null player name
        	if(offlineBukkitPlayer.getName() == null) {
        		ChatUtil.printDebug("offlineBukkitPlayer is null! "+uuid);
        	} else {
        		KonOfflinePlayer offlinePlayer = new KonOfflinePlayer(offlineBukkitPlayer, konquest.getKingdomManager().getKingdom(kingdomName), isBarbarian);
            	allPlayers.put(offlineBukkitPlayer, offlinePlayer);
        	}
        }
        ChatUtil.printDebug("Updated all players list from players.yml, size is "+allPlayers.size());
    }*/
    
    public void initAllSavedPlayers() {
    	allPlayers.clear();
    	//ChatUtil.printDebug("Initializing PlayerManager allPlayers cache... ");
    	for(KonOfflinePlayer offlinePlayer : konquest.getDatabaseThread().getDatabase().getAllSavedPlayers()) {
    		//ChatUtil.printDebug("Adding offlinePlayer "+offlinePlayer.getOfflineBukkitPlayer().getName());
    		allPlayers.put(offlinePlayer.getOfflineBukkitPlayer(), offlinePlayer);
    	}
    }
    
    /**
     * Load a player from the players.yml file if the player has an entry.
     * @param bukkitPlayer
     * @return true if the player was found and loaded, else false.
     */
    /*public boolean loadPlayer(Player bukkitPlayer) {
    	FileConfiguration playersConfig = konquest.getConfigManager().getConfig("players");
        if (playersConfig.get("players") == null) {
        	ChatUtil.printDebug("There is no players section in players.yml");
            return false;
        }
    	ConfigurationSection playerSection = playersConfig.getConfigurationSection("players."+bukkitPlayer.getUniqueId().toString());
    	if(playerSection == null) {
    		return false;
    	}
    	String kingdomName = playerSection.getString("kingdom");
    	String exileKingdomName = playerSection.getString("exileKingdom");
    	boolean isBarbarian = playerSection.getBoolean("barbarian");
    	KonPlayer addedPlayer;
    	// Create KonPlayer instance
    	if(isBarbarian) {
    		addedPlayer = createKonPlayer(bukkitPlayer, konquest.getKingdomManager().getBarbarians(), isBarbarian);
    	} else {
    		addedPlayer = createKonPlayer(bukkitPlayer, konquest.getKingdomManager().getKingdom(kingdomName), isBarbarian);
    	}
    	// Check if player has exceeded offline timeout
    	long timeoutSeconds = konquest.getConfigManager().getConfig("core").getLong("core.kingdoms.offline_timeout_seconds");
    	//long lastSeenTimeMilliseconds = playerSection.getLong("lastSeen");
    	long lastSeenTimeMilliseconds = bukkitPlayer.getLastPlayed();
    	boolean isOfflineTimeout = false;
    	// ignore this check if timeout is 0 or if there is no lastSeen timestamp
    	if(timeoutSeconds != 0 && lastSeenTimeMilliseconds != 0) {
    		ChatUtil.printDebug("Testing player for offline timeout");
    		Date now = new Date();
    		if(now.after(new Date(lastSeenTimeMilliseconds + (timeoutSeconds*1000)))) {
    			ChatUtil.printDebug("Player has exceeded offline timeout");
    			// Exile the player and set their exile kingdom to barbarian so they can choose a new Kingdom.
    			exileKingdomName = konquest.getKingdomManager().getBarbarians().getName();
    			isOfflineTimeout = true;
    		}
    	}
    	// Update player's Exile Kingdom
    	if(konquest.getKingdomManager().isKingdom(exileKingdomName)) {
    		addedPlayer.setExileKingdom(konquest.getKingdomManager().getKingdom(exileKingdomName));
		} else if(konquest.getKingdomManager().getBarbarians().getName().equals(exileKingdomName)) {
			addedPlayer.setExileKingdom(konquest.getKingdomManager().getBarbarians());
		} else {
			ChatUtil.printDebug("There was a problem setting player's Exile Kingdom: "+bukkitPlayer.getName());
		}
    	// Exile player
    	if(isOfflineTimeout) {
	    	if(konquest.getKingdomManager().exilePlayer(addedPlayer)) {
	    		ChatUtil.sendNotice(addedPlayer.getBukkitPlayer(), "You have been absent for too long and are now exiled as a "+ChatColor.DARK_RED+"Barbarian");
	    		ChatUtil.printDebug("Exiled player");
	    	} else {
	    		ChatUtil.printDebug("Could not properly exile player");
	    	}
    	}
    	// Update player's directive progress
    	ConfigurationSection playerDirectiveSection = playersConfig.getConfigurationSection("players."+bukkitPlayer.getUniqueId().toString()+".directives");
    	if(playerDirectiveSection == null) {
    		ChatUtil.printDebug("Player "+bukkitPlayer.getName()+" has no saved directives");
    	} else {
    		String allDirectives = "";
    		for(String directiveName : playerDirectiveSection.getKeys(false)) {
    			int directiveProgress = playerDirectiveSection.getInt(directiveName);
    			addedPlayer.setDirectiveProgress(KonDirective.getDirective(directiveName), directiveProgress);
    			allDirectives = allDirectives+directiveName+":"+directiveProgress+",";
    		}
    		ChatUtil.printDebug("Player "+bukkitPlayer.getName()+" directives = "+allDirectives);
    	}
    	return true;
    }*/
    
    /**
     * Save new player data to players.yml while preserving existing player data.
     */
    /*public void saveAllPlayers() {
    	FileConfiguration playersConfig = konquest.getConfigManager().getConfig("players");
    	ConfigurationSection root;
    	// Check for root section
    	if (playersConfig.get("players") == null) {
        	// When no root section exists, create on
    		ChatUtil.printDebug("Could not find root players section while saving, creating new file.");
    		playersConfig.set("players", null); // reset players config
    		root = playersConfig.createSection("players");
        } else {
        	// When a root section is found, overwrite existing players with up-to-date data
        	root = playersConfig.getConfigurationSection("players");
        }
    	// Iterate over all onlinePlayers in the cache
		for(KonPlayer player : onlinePlayers.values()) {
			savePlayerConfig(player, root);
		}
		// Iterate over all offline Players in the cache
		int totalSavedOfflinePlayers = 0;
		for(KonOfflinePlayer offlinePlayer : allPlayers.values()) {
			if(!offlinePlayer.getOfflineBukkitPlayer().isOnline()) {
				saveOfflinePlayerConfig(offlinePlayer, root);
				totalSavedOfflinePlayers++;
			}
		}
    	ChatUtil.printDebug("Saved "+onlinePlayers.values().size()+" Online Players, "+totalSavedOfflinePlayers+" Offline Players");
    }*/
    
    /*
    public void savePlayers() {
    	FileConfiguration playersConfig = konquest.getConfigManager().getConfig("players");
    	ConfigurationSection root;
    	// Check for root section
    	if (playersConfig.get("players") == null) {
        	// When no root section exists, create on
    		ChatUtil.printDebug("Could not find root players section while saving, creating new file.");
    		playersConfig.set("players", null); // reset players config
    		root = playersConfig.createSection("players");
        } else {
        	// When a root section is found, overwrite existing players with up-to-date data
        	root = playersConfig.getConfigurationSection("players");
        }
    	// Iterate over all onlinePlayers in the cache
		for(KonPlayer player : onlinePlayers.values()) {
			savePlayerConfig(player, root);
		}
    	ChatUtil.printDebug("Saved "+onlinePlayers.values().size()+" Players");
    }*/
    
    /*
    public void savePlayer(KonPlayer player) {
    	FileConfiguration playersConfig = konquest.getConfigManager().getConfig("players");
    	ConfigurationSection root;
    	// Check for root section
    	if (playersConfig.get("players") == null) {
        	// When no root section exists, create on
    		ChatUtil.printDebug("Could not find root players section while saving, creating new file.");
    		playersConfig.set("players", null); // reset players config
    		root = playersConfig.createSection("players");
        } else {
        	// When a root section is found, overwrite existing players with up-to-date data
        	root = playersConfig.getConfigurationSection("players");
        }
    	
    	savePlayerConfig(player, root);
		
    	ChatUtil.printDebug("Saved Player "+player.getBukkitPlayer().getName());
    }*/
    
    /*
    private void savePlayerConfig(KonPlayer player, ConfigurationSection root) {
    	String uuid = player.getBukkitPlayer().getUniqueId().toString();
		ConfigurationSection playerSection;
		if(root.contains(uuid)) {
			// Overwrite existing player data
			playerSection = root.getConfigurationSection(uuid);
		} else {
			// Create new player entry
			playerSection = root.createSection(uuid);
		}
		playerSection.set("kingdom", player.getKingdom().getName());
		playerSection.set("exileKingdom", player.getExileKingdom().getName());
		playerSection.set("barbarian", player.isBarbarian());
		// Save camp info
		if(konquest.getKingdomManager().isCampSet((KonOfflinePlayer)player)) {
			//ChatUtil.printDebug("Saving camp info for online player "+player.getBukkitPlayer().getName());
			KonCamp camp = konquest.getKingdomManager().getCamp(player);
			ConfigurationSection playerCampSection = playerSection.createSection("camp");
			ConfigurationSection playerCampCenterSection = playerCampSection.createSection("center");
			playerCampCenterSection.set("x", (int) camp.getCenterLoc().getX());
			playerCampCenterSection.set("y", (int) camp.getCenterLoc().getY());
			playerCampCenterSection.set("z", (int) camp.getCenterLoc().getZ());
			String chunk_coords_camp = "";
	        for(Point point : camp.getChunkList().keySet()) {
	        	//ChatUtil.printDebug("Saving "+kingdom.getName()+" "+town.getName()+" chunk "+point.getX()+","+point.getY());
	        	chunk_coords_camp = chunk_coords_camp+(int)point.getX()+","+(int)point.getY()+".";
	        }
	        //ChatUtil.printDebug("Saving "+kingdom.getName()+" "+town.getName()+" coord string: "+chunk_coords_town);
	        playerCampSection.set("chunks", chunk_coords_camp);
		} else {
			//ChatUtil.printDebug("Clearing camp info for online player "+player.getBukkitPlayer().getName());
			playerSection.set("camp",null);
		}
		//playerSection.set("lastSeen", new Date().getTime());
		// Save directive progress
		if(player.getDirectives().size() > 0) {
			ChatUtil.printDebug("Saving directive progress of player "+player.getBukkitPlayer().getName());
			ConfigurationSection directiveSection = playerSection.createSection("directives");
			for(KonDirective dir : player.getDirectives()) {
				directiveSection.set(dir.toString().toLowerCase(),player.getDirectiveProgress(dir));
			}
		} else {
			ChatUtil.printDebug("Player "+player.getBukkitPlayer().getName()+" has no directive progress to save");
		}
    }*/
    
    /*
    private void saveOfflinePlayerConfig(KonOfflinePlayer player, ConfigurationSection root) {
    	String uuid = player.getOfflineBukkitPlayer().getUniqueId().toString();
		ConfigurationSection playerSection;
		if(root.contains(uuid)) {
			// Overwrite existing player data
			playerSection = root.getConfigurationSection(uuid);
		} else {
			// Create new player entry
			playerSection = root.createSection(uuid);
		}
		playerSection.set("kingdom", player.getKingdom().getName());
		playerSection.set("exileKingdom", player.getExileKingdom().getName());
		playerSection.set("barbarian", player.isBarbarian());
		// Save camp info
		if(konquest.getKingdomManager().isCampSet(player)) {
			//ChatUtil.printDebug("Saving camp info for offline player "+player.getOfflineBukkitPlayer().getName());
			KonCamp camp = konquest.getKingdomManager().getCamp(player);
			ConfigurationSection playerCampSection = playerSection.createSection("camp");
			ConfigurationSection playerCampCenterSection = playerCampSection.createSection("center");
			playerCampCenterSection.set("x", (int) camp.getCenterLoc().getX());
			playerCampCenterSection.set("y", (int) camp.getCenterLoc().getY());
			playerCampCenterSection.set("z", (int) camp.getCenterLoc().getZ());
			String chunk_coords_camp = "";
	        for(Point point : camp.getChunkList().keySet()) {
	        	//ChatUtil.printDebug("Saving "+kingdom.getName()+" "+town.getName()+" chunk "+point.getX()+","+point.getY());
	        	chunk_coords_camp = chunk_coords_camp+(int)point.getX()+","+(int)point.getY()+".";
	        }
	        //ChatUtil.printDebug("Saving "+kingdom.getName()+" "+town.getName()+" coord string: "+chunk_coords_town);
	        playerCampSection.set("chunks", chunk_coords_camp);
		} else {
			//ChatUtil.printDebug("Clearing camp info for offline player "+player.getOfflineBukkitPlayer().getName());
			playerSection.set("camp",null);
		}
    }*/
	
}
