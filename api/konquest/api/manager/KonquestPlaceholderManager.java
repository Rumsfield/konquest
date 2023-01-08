package konquest.api.manager;

import org.bukkit.entity.Player;

/**
 * A manager for the Konquest placeholder expansion.
 * <p>
 * Konquest services PlaceholderAPI requests with these methods.
 * When given an invalid input argument, the result will be an empty string "".
 * Some requester methods are cached, and will only update their return value for the given input argument based on the Konquest configuration's minimum update time.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public interface KonquestPlaceholderManager {

	/**
	 * Gets the player's current kingdom name.
	 * 
	 * @param player The player
	 * @return The kingdom name, or an empty string when player is invalid
	 */
	public String getKingdom(Player player);
	
	/**
	 * Gets the player's current exile kingdom name.
	 * This is the previously joined kingdom, or barbarians when the player is new.
	 * 
	 * @param player The player
	 * @return The exile kingdom name, or an empty string when player is invalid
	 */
	public String getExile(Player player);
	
	/**
	 * Gets the player's current barbarian status.
	 * 
	 * @param player The player
	 * @return "True" if the player is barbarian, else "False", or an empty string when player is invalid
	 */
	public String getBarbarian(Player player);
	
	/**
	 * Gets a comma-separated list of the towns in which the player is a lord.
	 * 
	 * @param player The player
	 * @return The list of town names, or an empty string when player is invalid
	 */
	public String getTownsLord(Player player);
	
	/**
	 * Gets a comma-separated list of the towns in which the player is a knight.
	 * 
	 * @param player The player
	 * @return The list of town names, or an empty string when player is invalid
	 */
	public String getTownsKnight(Player player);
	
	/**
	 * Gets a comma-separated list of the towns in which the player is a resident.
	 * 
	 * @param player The player
	 * @return The list of town names, or an empty string when player is invalid
	 */
	public String getTownsResident(Player player);
	
	/**
	 * Gets a comma-separated list of all towns in which the player is resident, knight or lord.
	 * 
	 * @param player The player
	 * @return The list of town names, or an empty string when player is invalid
	 */
	public String getTownsAll(Player player);
	
	/**
	 * Gets the type of territory that the player is currently in.
	 * This is the language file definition mapped to the {@link konquest.api.model.KonquestTerritoryType KonquestTerritoryType} enum.
	 * 
	 * @param player The player
	 * @return The territory type, or an empty string when player is invalid
	 */
	public String getTerritory(Player player);
	
	/**
	 * Gets the name of the territory that the player is currently in.
	 * 
	 * @param player The player
	 * @return The territory name, or an empty string when player is invalid
	 */
	public String getLand(Player player);
	
	/**
	 * Gets the status of the chunk where the player is.
	 * 
	 * @param player The player
	 * @return "True" if the chunk is claimed, else "False", or an empty string when player is invalid
	 */
	public String getClaimed(Player player);
	
	/**
	 * Gets the current score of the player.
	 * 
	 * @param player The player
	 * @return The score value, or an empty string when player is invalid
	 */
	public String getScore(Player player);
	
	/**
	 * Gets the current prefix of the player.
	 * 
	 * @param player The player
	 * @return The prefix title, or an empty string when player is invalid
	 */
	public String getPrefix(Player player);
	
	/**
	 * Gets the number of towns in which the player is a lord.
	 * 
	 * @param player The player
	 * @return The number of towns, or an empty string when player is invalid
	 */
	public String getLordships(Player player);
	
	/**
	 * Gets the number of towns in which the player is a resident, knight or lord.
	 * 
	 * @param player The player
	 * @return The number of towns, or an empty string when player is invalid
	 */
	public String getResidencies(Player player);
	
	/**
	 * Gets the status of the player's chat mode.
	 * 
	 * @param player The player
	 * @return "True" if the player is in global chat, else "False", or an empty string when player is invalid
	 */
	public String getChat(Player player);
	
	/**
	 * Gets the status of the player's combat tag.
	 * 
	 * @param player The player
	 * @return "True" if the player is combat tagged, else "False", or an empty string when player is invalid
	 */
	public String getCombat(Player player);
	
	/**
	 * Gets the combat tag string from the Konquest configuration.
	 * 
	 * @param player The player
	 * @return The combat tag if the player is combat tagged, else an empty string, or an empty string when player is invalid
	 */
	public String getCombatTag(Player player);
	
	/**
	 * Gets the relationship type between the two players.
	 * This is the language file definition of the following types:
	 * <p>
	 *   Friendly<br>
	 *   Enemy<br>
	 *   Barbarian<br>
	 *   Armistice<br>
	 * </p>
	 * 
	 * @param playerOne The first player who is making the request
	 * @param playerTwo The second player who is the context of the request
	 * @return The relationship between the two players, or an empty string when either player is invalid
	 */
	public String getRelation(Player playerOne, Player playerTwo);
	
	/**
	 * Gets the relationship color between the two players.
	 * These colors are defined in the Konquest configuration.
	 * 
	 * @param playerOne The first player who is making the request
	 * @param playerTwo The second player who is the context of the request
	 * @return The relationship color between the two players, or an empty string when either player is invalid
	 */
	public String getRelationColor(Player playerOne, Player playerTwo);
	
	/**
	 * Gets the kingdom name and score value, separated by a space, in the given rank of the leaderboard.
	 * This method is cached.
	 * 
	 * @param rank The kingdom rank, between 1 and the total number of kingdoms.
	 * @return The kingdom score value, or "---" when the rank is invalid.
	 */
	public String getTopScore(int rank);
	
	/**
	 * Gets the kingdom name and number of towns, separated by a space, in the given rank of the leaderboard.
	 * This method is cached.
	 * 
	 * @param rank The kingdom rank, between 1 and the total number of kingdoms.
	 * @return The number of towns, or "---" when the rank is invalid.
	 */
	public String getTopTown(int rank);
	
	/**
	 * Gets the kingdom name and total land, separated by a space, in the given rank of the leaderboard.
	 * This method is cached.
	 * 
	 * @param rank The kingdom rank, between 1 and the total number of kingdoms.
	 * @return The total number of land chunks, or "---" when the rank is invalid.
	 */
	public String getTopLand(int rank);
	
	/**
	 * Gets the total number of players in the given kingdom name.
	 * This method is cached.
	 * 
	 * @param name The kingdom name
	 * @return The number of players, or an empty string when the kingdom name is invalid
	 */
	public String getKingdomPlayers(String name);
	
	/**
	 * Gets the current number of online players in the given kingdom name.
	 * This method is cached.
	 * 
	 * @param name The kingdom name
	 * @return The number of online players, or an empty string when the kingdom name is invalid
	 */
	public String getKingdomOnline(String name);
	
	/**
	 * Gets the total number of towns in the given kingdom name.
	 * This method is cached.
	 * 
	 * @param name The kingdom name
	 * @return The number of towns, or an empty string when the kingdom name is invalid
	 */
	public String getKingdomTowns(String name);
	
	/**
	 * Gets the total number of land chunks in the given kingdom name.
	 * This method is cached.
	 * 
	 * @param name The kingdom name
	 * @return The number of land chunks, or an empty string when the kingdom name is invalid
	 */
	public String getKingdomLand(String name);
	
	/**
	 * Gets the total amount of favor owned by players in the given kingdom name.
	 * This method is cached.
	 * 
	 * @param name The kingdom name
	 * @return The amount of favor, or an empty string when the kingdom name is invalid
	 */
	public String getKingdomFavor(String name);
	
	/**
	 * Gets the kingdom score of the given kingdom name.
	 * This method is cached.
	 * 
	 * @param name The kingdom name
	 * @return The kingdom score, or an empty string when the kingdom name is invalid
	 */
	public String getKingdomScore(String name);

}
