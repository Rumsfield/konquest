package konquest.api;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scoreboard.Scoreboard;

import konquest.api.manager.KonquestCampManager;
import konquest.api.manager.KonquestGuildManager;
import konquest.api.manager.KonquestKingdomManager;
import konquest.api.manager.KonquestPlayerManager;
import konquest.api.manager.KonquestPlotManager;
import konquest.api.manager.KonquestRuinManager;
import konquest.api.manager.KonquestShieldManager;
import konquest.api.manager.KonquestUpgradeManager;
import konquest.api.model.KonquestGuild;
import konquest.api.model.KonquestOfflinePlayer;
import konquest.api.model.KonquestTerritory;

/**
 * The Konquest API. This is the primary means of accessing Konquest objects and methods.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestAPI {

	/**
	 * Get the friendly primary color, from core.yml.
	 * 
	 * @return The friendly primary color
	 */
	public ChatColor getFriendlyPrimaryColor();
	
	/**
	 * Get the enemy primary color, from core.yml.
	 * 
	 * @return The enemy primary color
	 */
	public ChatColor getEnemyPrimaryColor();
	
	/**
	 * Get the primary Konquest scoreboard with teams.
	 * 
	 * @return The primary scoreboard
	 */
	public Scoreboard getScoreboard();
	
	/**
	 * Checks for name conflicts and constraints against all Konquest names.
	 * 
	 * @param name The name of an object (town, ruin, etc)
	 * @return Status code
	 * 			0 - Success, no issue found
	 * 			1 - Error, name is not strictly alpha-numeric
	 * 			2 - Error, name has more than 20 characters
	 * 			3 - Error, name is an existing player
	 * 			4 - Error, name is a kingdom
	 * 			5 - Error, name is a town
	 * 			6 - Error, name is a ruin
	 * 			7 - Error, name is a guild
	 */
	public int validateNameConstraints(String name);
	
	/**
	 * Get the player manager, for all things player related.
	 * 
	 * @return The player manager
	 */
	public KonquestPlayerManager getPlayerManager();
	
	/**
	 * Get the kingdom manager, for all things kingdom/town related.
	 * 
	 * @return The kingdom manager
	 */
	public KonquestKingdomManager getKingdomManager();
	
	/**
	 * Get the camp manager, for all things camp related.
	 * 
	 * @return The camp manager
	 */
	public KonquestCampManager getCampManager();
	
	/**
	 * Get the upgrade manager, for all things town upgrade related.
	 * 
	 * @return The upgrade manager
	 */
	public KonquestUpgradeManager getUpgradeManager();
	
	/**
	 * Get the shield manager, for all things town shield and armor related.
	 * 
	 * @return The shield manager
	 */
	public KonquestShieldManager getShieldManager();
	
	/**
	 * Get the ruin manager, for all things ruin related.
	 * 
	 * @return The ruin manager
	 */
	public KonquestRuinManager getRuinManager();
	
	/**
	 * Get the plot manager, for all things town plot related.
	 * 
	 * @return The ruin manager
	 */
	public KonquestPlotManager getPlotManager();
	
	/**
	 * Get the guild manager, for all things guild related.
	 * 
	 * @return The guild manager
	 */
	public KonquestGuildManager getGuildManager();
	
	/**
	 * Check if a location is in a valid world.
	 * Invalid worlds prevent some Konquest commands.
	 * 
	 * @param loc A location
	 * @return True if the location is in a valid world, else false
	 */
	public boolean isWorldValid(Location loc);
	
	/**
	 * Check if a world is valid.
	 * Invalid worlds prevent some Konquest commands.
	 * 
	 * @param world A world
	 * @return True if the world is valid, else false
	 */
	public boolean isWorldValid(World world);
	
	/**
	 * Check if a location is in an ignored world.
	 * Ignored worlds prevent most Konquest commands and features.
	 * 
	 * @param loc A location
	 * @return True if the location is in an ignored world, else false
	 */
	public boolean isWorldIgnored(Location loc);
	
	/**
	 * Check if a world is ignored.
	 * Ignored worlds prevent most Konquest commands and features.
	 * 
	 * @param world A world
	 * @return True if the world is ignored, else false
	 */
	public boolean isWorldIgnored(World world);
	
	/**
	 * Get a random location in the wild, constrained by radius and offset in Konquest's configuration.
	 * 
	 * @param world The world to generate the random location
	 * @return A random location in the wild
	 */
	public Location getRandomWildLocation(World world);
	
	/**
	 * Get a random safe location in a surrounding chunk to the given center location.
	 * A safe location is one that should not kill the player.
	 * The resulting location will not be in the same chunk as the center location.
	 * 
	 * @param center The location to center the new location around
	 * @param radius The distance in blocks from the center to search for a random location
	 * @return The safe random location
	 */
	public Location getSafeRandomCenteredLocation(Location center, int radius);
	
	/**
	 * Get the primary display color based on relationships. This color is set in the Konquest configuration.
	 * There is a color for each relationship: friendly, enemy, armistice, barbarian
	 * 
	 * @param displayPlayer The observing player who should see the color
	 * @param contextPlayer The target player who's relationship to the observer determines the color
	 * @return The primary display color
	 */
	public ChatColor getDisplayPrimaryColor(KonquestOfflinePlayer displayPlayer, KonquestOfflinePlayer contextPlayer);
	
	/**
	 * Get the primary display color based on relationships. This color is set in the Konquest configuration.
	 * There is a color for each relationship: friendly, enemy, armistice
	 * 
	 * @param displayGuild The observing guild who should see the color
	 * @param contextGuild The target guild who's relationship to the observer determines the color
	 * @return The primary display color
	 */
	public ChatColor getDisplayPrimaryColor(KonquestGuild displayGuild, KonquestGuild contextGuild);
	
	/**
	 * Get the primary display color based on relationships. This color is set in the Konquest configuration.
	 * There is a color for each relationship: friendly, enemy, armistice
	 * 
	 * @param displayGuild The observing player who should see the color
	 * @param contextGuild The target town who's relationship to the observer determines the color
	 * @return The primary display color
	 */
	public ChatColor getDisplayPrimaryColor(KonquestOfflinePlayer displayPlayer, KonquestTerritory contextTerritory);
}
