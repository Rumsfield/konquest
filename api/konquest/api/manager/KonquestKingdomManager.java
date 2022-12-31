package konquest.api.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;

import konquest.api.model.KonquestKingdom;
import konquest.api.model.KonquestOfflinePlayer;
import konquest.api.model.KonquestPlayer;
import konquest.api.model.KonquestTown;

/**
 * A manager for kingdoms and towns in Konquest.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestKingdomManager {

	/**
	 * Add a new kingdom at the given location with the given name.
	 * The kingdom will create a new capital territory in the chunk of the location.
	 * 
	 * @param loc The center location of the new kingdom's capital
	 * @param name The name of the new kingdom
	 * @return True when the new kingdom is successfully created, else false
	 */
	//public boolean addKingdom(Location loc, String name);
	
	/**
	 * Remove a kingdom with the given name.
	 * All kingdom members will be exiled to barbarians.
	 * All towns in the kingdom will be removed, with monuments replaced with air.
	 * The kingdom capital will be removed.
	 * 
	 * @param name The name of the kingdom to remove, case-sensitive
	 * @return True when the kingdom is successfully removed, else false
	 */
	public boolean removeKingdom(String name);
	
	/**
	 * Rename a kingdom.
	 * 
	 * @param oldName The current name of the kingdom, case-sensitive
	 * @param newName The new name for the kingdom
	 * @return True when the new name is valid and successfully applied to the kingdom, else false
	 */
	//public boolean renameKingdom(String oldName, String newName);
	
	/**
	 * Primary method for adding a player member to a kingdom.
	 * Assign a player to a kingdom and teleport them to the capital spawn point.
	 * Optionally checks for join permissions based on Konquest configuration.
	 * Optionally enforces maximum kingdom membership difference based on Konquest configuration.
	 * 
	 * @param id The player to assign, by UUID
	 * @param kingdomName The kingdom name, case-sensitive
	 * @param force Ignore permission and max membership limits when true
	 * @return status
	 * 				<br>0	- success
	 *  			<br>1 	- kingdom name does not exist
	 *  			<br>2 	- the kingdom is full (config option max_player_diff)
	 *  			<br>3 	- missing permission
	 *  			<br>4 	- cancelled
	 *  			<br>5 	- joining is denied, player is already member
	 *  			<br>6	- joining is denied, failed switch criteria
	 *  			<br>7 	- Unknown player ID
	 *             <br>-1 	- internal error
	 */
	public int assignPlayerKingdom(UUID id, String kingdomName, boolean force);
	
	/**
	 * Exiles a player to be a Barbarian.
	 * Sets their exileKingdom value to their current Kingdom.
	 * (Optionally) Teleports to a random Wild location.
	 * (Optionally) Removes all stats and disables prefix.
	 * (Optionally) Resets their exileKingdom to Barbarians, making them look like a new player to Konquest.
	 * Applies exile cooldown timer.
	 * 
	 * @param player The player to exile
	 * @param teleport Teleport the player based on Konquest configuration when true
	 * @param clearStats Remove all player stats and prefix when true
	 * @param isFull Perform a full exile such that the player has no exile kingdom, like they just joined the server
	 * @param force Ignore most checks when true
	 * @return status
	 * 				<br>0	- success
	 * 				<br>1	- Player is already a barbarian
	 * 				<br>2	- Invalid world
	 * 				<br>3	- Failed to find valid teleport location
	 * 				<br>4	- cancelled by event
	 * 				<br>6	- Exile denied, player is a kingdom master
	 * 				<br>8   - Cooldown remaining
	 * 				<br>9   - Unknown player ID
	 *             <br>-1 	- internal error
	 */
	public int exilePlayerBarbarian(UUID id, boolean teleport, boolean clearStats, boolean isFull, boolean force);
	
	/**
	 * Create a new town centered at the given location, with the given name, for the given kingdom name.
	 * The town will copy a new monument from the kingdom monument template into the chunk of the given location.
	 * The town will create an intial territory based on the Konquest configuration.
	 * 
	 * @param loc The center location of the town
	 * @param name The name of the town
	 * @param kingdomName The name of the kingdom that the town belongs to, case-sensitive
	 * @return  Status code
	 * 			<br>0 - success
	 * 			<br>1 - error, initial territory chunks conflict with another territory
	 * 			<br>3 - error, bad name
	 * 			<br>4 - error, invalid monument template
	 * 			<br>5 - error, bad town placement, invalid world
	 * 			<br>6 - error, bad town placement, too close to another territory
	 * 			<br>7 - error, bad town placement, too far from other territories
	 *  	   <br>12 - error, town init fail, bad town height
	 *  	   <br>13 - error, town init fail, too much air
	 *  	   <br>14 - error, town init fail, bad chunks
     * 		   <br>21 - error, town init fail, invalid monument
	 * 		   <br>22 - error, town init fail, bad monument gradient
	 * 		   <br>23 - error, town init fail, monument placed on bedrock
	 */
	public int createTown(Location loc, String name, String kingdomName);
	
	/**
	 * Remove a town.
	 * The territory will be deleted, and the town monument will be replaced with air.
	 * 
	 * @param name The name of the town to remove
	 * @param kingdomName The name of the kingdom that the town belongs to
	 * @return True when the town is successfully removed, else false
	 */
	public boolean removeTown(String name, String kingdomName);
	
	/**
	 * Rename a town.
	 * 
	 * @param oldName The current name of the town to be renamed
	 * @param newName The new name for the town
	 * @param kingdomName The name of the kingdom that the town belongs to
	 * @return True when the town is successfully renamed, else false
	 */
	public boolean renameTown(String oldName, String newName, String kingdomName);
	
	/**
	 * Transfers ownership of a town to the given player's kingdom.
	 * The player that captures the town will become the new town lord.
	 * The player's kingdom will assume ownership of the town.
	 * 
	 * @param name The name of the town to capture
	 * @param oldKingdomName The name of the town's current kingdom
	 * @param conquerPlayer The player to capture the town for
	 * @return The captured town when successful, else null
	 */
	public KonquestTown captureTownForPlayer(String name, String oldKingdomName, KonquestPlayer conquerPlayer);
	
	/**
	 * Claims the chunk at the given location for the nearest adjacent territory.
	 * Adds the chunk to the chunk map of the territory.
	 * The territory is chosen by finding the closest territory center location to the given location.
	 * 
	 * @param loc The location to claim
	 * @return  Status code
	 * 			<br>0 - success
	 * 			<br>1 - error, no adjacent territory
	 * 			<br>2 - error, exceeds max distance
	 * 			<br>3 - error, already claimed
	 * 			<br>4 - error, cancelled by event
	 */
	//public int claimChunk(Location loc);
	
	/**
	 * Unclaims the chunk at the given location from its associated territory.
	 * Removes the chunk from the chunk map of the territory.
	 * 
	 * @param loc The location to unclaim
	 * @return  Status code
	 * 			<br>0 - success
	 * 			<br>1 - error, no territory at location
	 *			<br>2 - error, location in center chunk
	 * 			<br>3 - error, internal territory chunk not found
	 * 			<br>4 - error, cancelled by event
	 */
	//public int unclaimChunk(Location loc);
	
	/**
	 * Checks whether the chunk at the given location is claimed by a territory.
	 * 
	 * @param loc The location to check
	 * @return True when the location is inside of claimed territory, else false
	 */
	//public boolean isChunkClaimed(Location loc);
	
	/**
	 * Gets the territory at the given location.
	 * If no territory exists, returns null.
	 * 
	 * @param loc The location to request the territory
	 * @return The territory at the given location, or null if no territory exists
	 */
	//public KonquestTerritory getChunkTerritory(Location loc);
	
	
	/**
	 * Finds the distance in chunks (16 blocks) from the given location to the nearest territory.
	 * Checks kingdom capitals, towns, and ruins. Does not include camps.
	 *  
	 * @param loc The location to search
	 * @return The distance in chunks to the nearest territory, or Integer.MAX_VALUE if not found
	 */
	//public int getDistanceToClosestTerritory(Location loc);
	
	/**
	 * Gets a list of all kingdom names.
	 * 
	 * @return The list of kingdom names
	 */
	public ArrayList<String> getKingdomNames();
	
	/**
	 * Gets a list of all kingdoms.
	 * 
	 * @return The list of kingdoms
	 */
	public ArrayList<? extends KonquestKingdom> getKingdoms();
	
	/**
	 * Checks whether the given name is a kingdom. Excludes the default barbarians kingdom.
	 * 
	 * @param name The name of the kingdom
	 * @return True when the name matches a kingdom, else false
	 */
	public boolean isKingdom(String name);
	
	/**
	 * Gets a kingdom by name. Returns the barbarian kingdom by default.
	 * 
	 * @param name The name of the kingdom
	 * @return The kingdom, or barbarians when not found
	 */
	public KonquestKingdom getKingdom(String name);
	
	/**
	 * Checks whether the given name is a town.
	 * 
	 * @param name The name of the town
	 * @return True when the name matches a town, else false
	 */
	public boolean isTown(String name);
	
	/**
	 * Gets the barbarians kingdom.
	 * This is a default kingdom created internally by Konquest for barbarian players.
	 * 
	 * @return The barbarians kingdom
	 */
	public KonquestKingdom getBarbarians();
	
	/**
	 * Gets the neutrals kingdom.
	 * This is a default kingdom created internally by Konquest for ruin territories and other territories that do not belong to a kingdom.
	 * 
	 * @return The neutrals kingdom
	 */
	public KonquestKingdom getNeutrals();
	
	/**
	 * Gets the towns which the given player is the lord of.
	 * 
	 * @param player The player
	 * @return List of towns
	 */
	public List<? extends KonquestTown> getPlayerLordshipTowns(KonquestOfflinePlayer player);
	
	/**
	 * Gets the towns which the given player is a knight of.
	 * 
	 * @param player The player
	 * @return List of towns
	 */
	public List<? extends KonquestTown> getPlayerKnightTowns(KonquestOfflinePlayer player);
	
	/**
	 * Gets the towns which the given player is a resident of.
	 * 
	 * @param player The player
	 * @return List of towns
	 */
	public List<? extends KonquestTown> getPlayerResidenceTowns(KonquestOfflinePlayer player);
	
	/**
	 * Gets the monument critical block material.
	 * This is the block type that must be destroyed within town monuments in order to capture the town.
	 * 
	 * @return The critical material
	 */
	public Material getTownCriticalBlock();
	
	/**
	 * Gets the maximum number of critical hits for each town monument.
	 * When this number of critical blocks are destroyed, the town will be captured.
	 * 
	 * @return The number of maximum critical hits
	 */
	public int getMaxCriticalHits();
	
}
