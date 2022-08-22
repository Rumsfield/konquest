package konquest.api.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import konquest.api.model.KonquestKingdom;
import konquest.api.model.KonquestOfflinePlayer;
import konquest.api.model.KonquestPlayer;

/**
 * A manager for players in Konquest.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestPlayerManager {

	/**
	 * Gets a Konquest player object from the given Bukkit Player.
	 * 
	 * @param bukkitPlayer The player to look up
	 * @return The Konquest player if one exists, else null
	 */
	public KonquestPlayer getPlayer(Player bukkitPlayer);
	
	/**
	 * Checks whether a player is currently online and represented in Konquest.
	 * 
	 * @param bukkitPlayer The player to check
	 * @return True when the player is online and has a KonquestPlayer object, else false
	 */
	public boolean isOnlinePlayer(Player bukkitPlayer);
	
	/**
	 * Gets a Konquest offline player object from the given Bukkit OfflinePlayer.
	 * 
	 * @param offlineBukkitPlayer The offline player to look up
	 * @return The Konquest offline player if one exists, else null
	 */
	public KonquestOfflinePlayer getOfflinePlayer(OfflinePlayer offlineBukkitPlayer);
	
	/**
	 * Checks whether a player exists and is represented in Konquest.
	 * This can be used to check if a player has ever joined the server and is present in the Konquest player database.
	 * 
	 * @param offlineBukkitPlayer The player to check
	 * @return True when the player exists and has a KonquestOfflinePlayer object, else false
	 */
	public boolean isOfflinePlayer(OfflinePlayer offlineBukkitPlayer);
	
	/**
	 * Checks whether a player name exists and is represented in Konquest.
	 * 
	 * @param name The player's name
	 * @return True if there is a player with a matching name in the Konquest player database, else false
	 */
	public boolean isPlayerNameExist(String name);
	
	/**
	 * Gets a Konquest player object from the given name.
	 * 
	 * @param displayName The player name to look up
	 * @return The Konquest player if one exists, else null
	 */
	public KonquestPlayer getPlayerFromName(String displayName);
	
	/**
	 * Gets a Konquest offline player object from the given name.
	 * 
	 * @param displayName The player name to look up
	 * @return The Konquest offline player if one exists, else null
	 */
	public KonquestOfflinePlayer getOfflinePlayerFromName(String displayName);
	
	/**
	 * Gets a Konquest player object from the given UUID.
	 * 
	 * @param id The player UUID to look up
	 * @return The Konquest player if one exists, else null
	 */
	public KonquestPlayer getPlayerFromID(UUID id);
	
	/**
	 * Gets a Konquest offline player object from the given UUID.
	 * 
	 * @param id The player UUID to look up
	 * @return The Konquest offline player if one exists, else null
	 */
	public KonquestOfflinePlayer getOfflinePlayerFromID(UUID id);
	
	/**
	 * Gets all of Bukkit's OfflinePlayers from the Konquest player database.
	 * This is a collection of every player that has joined the server.
	 * 
	 * @return The collection of players
	 */
	public Collection<OfflinePlayer> getAllOfflinePlayers();
	
	/**
	 * Gets all of Konquest's OfflinePlayers from the Konquest player database.
	 * This is a collection of every player that has joined the server.
	 * 
	 * @return The collection of players
	 */
	public Collection<? extends KonquestOfflinePlayer> getAllKonquestOfflinePlayers();
	
	/**
	 * Gets the players currently online that are members of the given kingdom name.
	 * 
	 * @param kingdomName The kingdom name, ignoring case
	 * @return The list of online players in the given kingdom
	 */
	public ArrayList<? extends KonquestPlayer> getPlayersInKingdom(String kingdomName);
	
	/**
	 * Gets the players currently online that are members of the given kingdom object.
	 * 
	 * @param kingdom The kingdom instance
	 * @return The list of online players in the given kingdom
	 */
	public ArrayList<? extends KonquestPlayer> getPlayersInKingdom(KonquestKingdom kingdom);
	
	/**
	 * Gets the Bukkit players currently online that are members of the given kingdom name.
	 * 
	 * @param kingdomName The kingdom name, ignoring case
	 * @return The list of online players in the given kingdom
	 */
	public Collection<Player> getBukkitPlayersInKingdom(String kingdomName);
	
	/**
	 * Gets the Bukkit players currently online that are members of the given kingdom object.
	 * 
	 * @param kingdom The kingdom instance
	 * @return The list of online players in the given kingdom
	 */
	public Collection<Player> getBukkitPlayersInKingdom(KonquestKingdom kingdom);
	
	/**
	 * Gets the names of players currently online that are members of the given kingdom name.
	 * This is a convenience method.
	 * 
	 * @param kingdomName The kingdom name, ignoring case
	 * @return The list of player names in the given kingdom
	 */
	public ArrayList<String> getPlayerNamesInKingdom(String kingdomName);
	
	/**
	 * Gets all of the players that are members of the given kingdom name.
	 * 
	 * @param kingdomName The kingdom name, ignoring case
	 * @return The list of players in the given kingdom
	 */
	public ArrayList<? extends KonquestOfflinePlayer> getAllPlayersInKingdom(String kingdomName);
	
	/**
	 * Gets all of the players that are members of the given kingdom object.
	 * 
	 * @param kingdom The kingdom instance
	 * @return The list of players in the given kingdom
	 */
	public ArrayList<? extends KonquestOfflinePlayer> getAllPlayersInKingdom(KonquestKingdom kingdom);
	
	/**
	 * Gets all of the Bukkit players that are members of the given kingdom name.
	 * 
	 * @param kingdomName The kingdom name, ignoring case
	 * @return The list of players in the given kingdom
	 */
	public Collection<OfflinePlayer> getAllBukkitPlayersInKingdom(String kingdomName);
	
	/**
	 * Gets all of the Bukkit players that are members of the given kingdom object.
	 * 
	 * @param kingdom The kingdom instance
	 * @return The list of players in the given kingdom
	 */
	public Collection<OfflinePlayer> getAllBukkitPlayersInKingdom(KonquestKingdom kingdom);
	
	/**
	 * Gets all of Konquest's players that are currently online.
	 * 
	 * @return The collection of players
	 */
	public Collection<? extends KonquestPlayer> getPlayersOnline();
	
	/**
	 * Gets all Bukkit players that are currently online.
	 * 
	 * @return The collection of players
	 */
	public Collection<Player> getBukkitPlayersOnline();
	
}
