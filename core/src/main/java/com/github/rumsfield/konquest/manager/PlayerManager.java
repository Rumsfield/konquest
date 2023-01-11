package com.github.rumsfield.konquest.manager;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.manager.KonquestPlayerManager;
import com.github.rumsfield.konquest.api.model.KonquestKingdom;
import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.model.KonMonument;
import com.github.rumsfield.konquest.model.KonOfflinePlayer;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager implements KonquestPlayerManager {

	private final Konquest konquest;
	private final HashMap<Player, KonPlayer> onlinePlayers;
	private final ConcurrentHashMap<OfflinePlayer, KonOfflinePlayer> allPlayers;
	private final ArrayList<String> blockedCommands;

	public PlayerManager(Konquest konquest) {
		this.konquest = konquest;
		onlinePlayers = new HashMap<>();
		allPlayers = new ConcurrentHashMap<>();
		blockedCommands = new ArrayList<>();
	}
	
	public void initialize() {
		// Gather all block commands from core config
		blockedCommands.clear();
		blockedCommands.addAll(konquest.getConfigManager().getConfig("core").getStringList("core.combat.prevent_command_list"));
		
		//blockedCommands
		
		ChatUtil.printDebug("Player Manager is ready");
	}

	@NotNull
	public ArrayList<String> getBlockedCommands() {
		return blockedCommands;
	}

	@Nullable
	public KonPlayer removePlayer(@NotNull Player bukkitPlayer) {
		KonPlayer player = onlinePlayers.remove(bukkitPlayer);
		ChatUtil.printDebug("Removed online player: "+bukkitPlayer.getName());
		return player;
	}

	@Nullable
	public KonPlayer getPlayer(Player bukkitPlayer) {
		return onlinePlayers.get(bukkitPlayer);
	}
	
	public boolean isOnlinePlayer(Player bukkitPlayer) {
		return onlinePlayers.containsKey(bukkitPlayer);
	}
	
	@Nullable
	public KonOfflinePlayer getOfflinePlayer(OfflinePlayer offlineBukkitPlayer) {
		return allPlayers.get(offlineBukkitPlayer);
	}
	
	public boolean isOfflinePlayer(OfflinePlayer offlineBukkitPlayer) {
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
	 * @param bukkitPlayer - Bukkit player
	 * @return KonPlayer of the bukkit player
	 */
	@NotNull
	public KonPlayer createKonPlayer(Player bukkitPlayer) {
		KonPlayer newPlayer = new KonPlayer(bukkitPlayer, konquest.getKingdomManager().getBarbarians(), true);
        onlinePlayers.put(bukkitPlayer, newPlayer);
        linkOnlinePlayerToCache(newPlayer);
        ChatUtil.printDebug("Created player "+bukkitPlayer.getName());
        return newPlayer;
    }
	
	/**
	 * This method is used for instantiating a KonPlayer using info from the database
	 * @param bukkitPlayer - Bukkit player
	 * @param kingdomName - Name of the kingdom the player is in
	 * @param exileKingdomName - Name of the kingdom the player is exiled from
	 * @param isBarbarian - Is the player a barbarian
	 * @return KonPlayer of the bukkit player
	 */
	@NotNull
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
    		ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_ABSENT.getMessage());
    		ChatUtil.printDebug("Exiled player");
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
	 * @param player - KonPlayer to link to the cache
	 */
	private void linkOnlinePlayerToCache(KonPlayer player) {
		OfflinePlayer bukkitOfflinePlayer = player.getOfflineBukkitPlayer();
		// Search cache for duplicates and remove
		ArrayList<OfflinePlayer> removingList = new ArrayList<>();
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
			// Removing the cast to KonOfflinePlayer should preserve the same object in both allPlayers and onlinePlayers maps.
			// When a KonPlayer is modified from onlinePlayers, it should also reflect in allPlayers, and vice-versa.
			allPlayers.put(bukkitOfflinePlayer, player);
		}
	}

    public KonPlayer getPlayerFromName(String displayName) {
    	for(KonPlayer player : onlinePlayers.values()) {
    		if(player != null) {
				player.getBukkitPlayer().getName();
				if (player.getBukkitPlayer().getName().equalsIgnoreCase(displayName)) {
					return player;
				}
			}
    	}
        return null;
    }
    
    public KonOfflinePlayer getOfflinePlayerFromName(String displayName) {
    	for(KonOfflinePlayer offlinePlayer : allPlayers.values()) {
    		if(offlinePlayer != null && 
    				offlinePlayer.getOfflineBukkitPlayer().getName() != null &&
    				offlinePlayer.getOfflineBukkitPlayer().getName().equalsIgnoreCase(displayName)) {
    			return offlinePlayer;
    		}
    	}
        return null;
    }
    
    public KonPlayer getPlayerFromID(UUID id) {
    	for(KonPlayer player : onlinePlayers.values()) {
    		if(player != null) {
				player.getBukkitPlayer().getName();
				if (player.getBukkitPlayer().getUniqueId().equals(id)) {
					return player;
				}
			}
    	}
        return null;
    }
    
    public KonOfflinePlayer getOfflinePlayerFromID(UUID id) {
    	for(KonOfflinePlayer offlinePlayer : allPlayers.values()) {
    		if(offlinePlayer != null &&
    				offlinePlayer.getOfflineBukkitPlayer().getName() != null &&
    				offlinePlayer.getOfflineBukkitPlayer().getUniqueId().equals(id)) {
    			return offlinePlayer;
    		}
    	}
        return null;
    }
    
    public Collection<OfflinePlayer> getAllOfflinePlayers() {
		return new HashSet<>(allPlayers.keySet());
    }
    
    public Collection<KonOfflinePlayer> getAllKonquestOfflinePlayers() {
		return new HashSet<>(allPlayers.values());
    }
    
    public ArrayList<KonPlayer> getPlayersInKingdom(String kingdomName) {
    	ArrayList<KonPlayer> playerList = new ArrayList<>();
    	for(KonPlayer player : onlinePlayers.values()) {
    		if(player.getKingdom().getName().equalsIgnoreCase(kingdomName)) {
    			playerList.add(player);
    		}
    	}
    	return playerList;
    }
    
    public ArrayList<KonPlayer> getPlayersInKingdom(KonquestKingdom kingdom) {
    	ArrayList<KonPlayer> playerList = new ArrayList<>();
    	for(KonPlayer player : onlinePlayers.values()) {
    		if(player.getKingdom().equals(kingdom)) {
    			playerList.add(player);
    		}
    	}
    	return playerList;
    }
    
    public ArrayList<String> getPlayerNamesInKingdom(String kingdomName) {
    	ArrayList<String> playerNameList = new ArrayList<>();
    	for(KonPlayer player : onlinePlayers.values()) {
    		if(player.getKingdom().getName().equalsIgnoreCase(kingdomName)) {
    			playerNameList.add(player.getBukkitPlayer().getName());
    		}
    	}
    	return playerNameList;
    }
    
    public ArrayList<String> getPlayerNames() {
    	ArrayList<String> playerNameList = new ArrayList<>();
    	for(KonPlayer player : onlinePlayers.values()) {
    		playerNameList.add(player.getBukkitPlayer().getName());
    	}
    	return playerNameList;
    }
    
    public ArrayList<String> getAllPlayerNames() {
    	ArrayList<String> playerNameList = new ArrayList<>();
    	for(KonOfflinePlayer player : allPlayers.values()) {
    		playerNameList.add(player.getOfflineBukkitPlayer().getName());
    	}
    	return playerNameList;
    }
    
    public ArrayList<KonOfflinePlayer> getAllPlayersInKingdom(String kingdomName) {
    	ArrayList<KonOfflinePlayer> playerList = new ArrayList<>();
    	for(KonOfflinePlayer player : allPlayers.values()) {
    		if(player.getKingdom().getName().equalsIgnoreCase(kingdomName)) {
    			playerList.add(player);
    		}
    	}
    	return playerList;
    }
    
    public ArrayList<KonOfflinePlayer> getAllPlayersInKingdom(KonquestKingdom kingdom) {
    	ArrayList<KonOfflinePlayer> playerList = new ArrayList<>();
    	for(KonOfflinePlayer player : allPlayers.values()) {
    		if(player.getKingdom().equals(kingdom)) {
    			playerList.add(player);
    		}
    	}
    	return playerList;
    }
    
    public ArrayList<KonPlayer> getPlayersInMonument(KonMonument monument) {
    	ArrayList<KonPlayer> playerList = new ArrayList<>();
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
		return new HashSet<>(onlinePlayers.values());
    }
    
    public void initAllSavedPlayers() {
    	allPlayers.clear();
    	//ChatUtil.printDebug("Initializing PlayerManager allPlayers cache... ");
    	for(KonOfflinePlayer offlinePlayer : konquest.getDatabaseThread().getDatabase().getAllSavedPlayers()) {
    		//ChatUtil.printDebug("Adding offlinePlayer "+offlinePlayer.getOfflineBukkitPlayer().getName());
    		allPlayers.put(offlinePlayer.getOfflineBukkitPlayer(), offlinePlayer);
    	}
    }

	public Collection<Player> getBukkitPlayersInKingdom(String kingdomName) {
		Collection<Player> playerList = new HashSet<>();
    	for(Player bukkitPlayer : onlinePlayers.keySet()) {
    		KonPlayer player = onlinePlayers.get(bukkitPlayer);
    		if(player.getKingdom().getName().equalsIgnoreCase(kingdomName)) {
    			playerList.add(bukkitPlayer);
    		}
    	}
    	return playerList;
	}

	public Collection<Player> getBukkitPlayersInKingdom(KonquestKingdom kingdom) {
		Collection<Player> playerList = new HashSet<>();
    	for(Player bukkitPlayer : onlinePlayers.keySet()) {
    		KonPlayer player = onlinePlayers.get(bukkitPlayer);
    		if(player.getKingdom().equals(kingdom)) {
    			playerList.add(bukkitPlayer);
    		}
    	}
    	return playerList;
	}

	public Collection<OfflinePlayer> getAllBukkitPlayersInKingdom(String kingdomName) {
		Collection<OfflinePlayer> playerList = new HashSet<>();
    	for(OfflinePlayer bukkitOfflinePlayer : allPlayers.keySet()) {
    		KonOfflinePlayer player = allPlayers.get(bukkitOfflinePlayer);
    		if(player.getKingdom().getName().equalsIgnoreCase(kingdomName)) {
    			playerList.add(bukkitOfflinePlayer);
    		}
    	}
    	return playerList;
	}

	public Collection<OfflinePlayer> getAllBukkitPlayersInKingdom(KonquestKingdom kingdom) {
    	Collection<OfflinePlayer> playerList = new HashSet<>();
    	for(OfflinePlayer bukkitOfflinePlayer : allPlayers.keySet()) {
    		KonOfflinePlayer player = allPlayers.get(bukkitOfflinePlayer);
    		if(player.getKingdom().equals(kingdom)) {
    			playerList.add(bukkitOfflinePlayer);
    		}
    	}
    	return playerList;
	}

	public Collection<Player> getBukkitPlayersOnline() {
		return new HashSet<>(onlinePlayers.keySet());
	}

}
