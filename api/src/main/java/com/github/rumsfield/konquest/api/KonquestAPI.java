package com.github.rumsfield.konquest.api;

import com.github.rumsfield.konquest.api.manager.*;
import com.github.rumsfield.konquest.api.model.KonquestKingdom;
import com.github.rumsfield.konquest.api.model.KonquestOfflinePlayer;
import com.github.rumsfield.konquest.api.model.KonquestTerritory;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scoreboard.Scoreboard;

import java.awt.*;

/**
 * The Konquest API. This is the primary means of accessing Konquest objects and methods. Most of the methods are helpers.
 * Use the manager classes to interact with specific portions of Konquest.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestAPI {

	/**
	 * Gets the friendly primary color, from core.yml.
	 * 
	 * @return The friendly primary color
	 */
	ChatColor getFriendlyPrimaryColor();

	/**
	 * Gets the friendly secondary color, from core.yml.
	 *
	 * @return The friendly secondary color
	 */
	@Deprecated
	ChatColor getFriendlySecondaryColor();

	/**
	 * Gets the enemy primary color, from core.yml.
	 * 
	 * @return The enemy primary color
	 */
	ChatColor getEnemyPrimaryColor();
	
	/**
	 * Gets the enemy secondary color, from core.yml.
	 * 
	 * @return The enemy secondary color
	 */
	@Deprecated
	ChatColor getEnemySecondaryColor();
	
	/**
	 * Gets the trade primary color, from core.yml.
	 * 
	 * @return The trade primary color
	 */
	ChatColor getTradePrimaryColor();

	/**
	 * Gets the trade secondary color, from core.yml.
	 *
	 * @return The trade secondary color
	 */
	@Deprecated
	ChatColor getTradeSecondaryColor();

	/**
	 * Gets the peaceful primary color, from core.yml.
	 * 
	 * @return The peaceful primary color
	 */
	ChatColor getPeacefulPrimaryColor();

	/**
	 * Gets the peaceful secondary color, from core.yml.
	 *
	 * @return The peaceful secondary color
	 */
	@Deprecated
	ChatColor getPeacefulSecondaryColor();

	/**
	 * Gets the allied primary color, from core.yml.
	 * 
	 * @return The allied primary color
	 */
	ChatColor getAlliedPrimaryColor();

	/**
	 * Gets the allied secondary color, from core.yml.
	 *
	 * @return The allied secondary color
	 */
	@Deprecated
	ChatColor getAlliedSecondaryColor();

	/**
	 * Gets the barbarian primary color, from core.yml.
	 * 
	 * @return The barbarian primary color
	 */
	ChatColor getBarbarianPrimaryColor();

	/**
	 * Gets the barbarian secondary color, from core.yml.
	 *
	 * @return The barbarian secondary color
	 */
	@Deprecated
	ChatColor getBarbarianSecondaryColor();

	/**
	 * Gets the neutral primary color, from core.yml.
	 * 
	 * @return The neutral primary color
	 */
	ChatColor getNeutralPrimaryColor();

	/**
	 * Gets the neutral secondary color, from core.yml.
	 *
	 * @return The neutral secondary color
	 */
	@Deprecated
	ChatColor getNeutralSecondaryColor();

	/**
	 * Gets the primary Konquest scoreboard with teams.
	 * 
	 * @return The primary scoreboard
	 */
	Scoreboard getScoreboard();
	
	/**
	 * Checks for name conflicts and constraints against all Konquest names.
	 * 
	 * @param name The name of an object (town, ruin, etc)
	 * @return Status code
	 * 			<br>0 - Success, no issue found
	 * 			<br>1 - Error, name is not strictly alphanumeric
	 * 			<br>2 - Error, name has more than 20 characters
	 * 			<br>3 - Error, name is an existing player
	 * 			<br>4 - Error, name is a kingdom
	 * 			<br>5 - Error, name is a town
	 * 			<br>6 - Error, name is a ruin
	 * 			<br>7 - Error, name is a guild [deprecated]
	 * 	  		<br>8 - Error, name is a sanctuary
	 * 	  		<br>9 - Error, name is a template
	 * 	  		<br>10 - Error, name is reserved word
	 */
	int validateNameConstraints(String name);
	
	/**
	 * Gets the player manager, for all things player related.
	 * 
	 * @return The player manager
	 */
	KonquestPlayerManager getPlayerManager();
	
	/**
	 * Gets the kingdom manager, for all things kingdom/town related.
	 * 
	 * @return The kingdom manager
	 */
	KonquestKingdomManager getKingdomManager();

	/**
	 * Gets the territory manager, for all things land/territory related.
	 *
	 * @return The territory manager
	 */
	KonquestTerritoryManager getTerritoryManager();
	
	/**
	 * Gets the camp manager, for all things camp related.
	 * 
	 * @return The camp manager
	 */
	KonquestCampManager getCampManager();
	
	/**
	 * Gets the upgrade manager, for all things town upgrade related.
	 * 
	 * @return The upgrade manager
	 */
	KonquestUpgradeManager getUpgradeManager();
	
	/**
	 * Gets the shield manager, for all things town shield and armor related.
	 * 
	 * @return The shield manager
	 */
	KonquestShieldManager getShieldManager();
	
	/**
	 * Gets the ruin manager, for all things ruin related.
	 * 
	 * @return The ruin manager
	 */
	KonquestRuinManager getRuinManager();
	
	/**
	 * Gets the plot manager, for all things town plot related.
	 * 
	 * @return The ruin manager
	 */
	KonquestPlotManager getPlotManager();
	
	/**
	 * Checks if a location is in a valid world.
	 * Invalid worlds prevent some Konquest commands.
	 * 
	 * @param loc A location
	 * @return True if the location is in a valid world, else false
	 */
	boolean isWorldValid(Location loc);
	
	/**
	 * Checks if a world is valid.
	 * Invalid worlds prevent some Konquest commands.
	 * 
	 * @param world A world
	 * @return True if the world is valid, else false
	 */
	boolean isWorldValid(World world);
	
	/**
	 * Checks if a location is in an ignored world.
	 * Ignored worlds prevent most Konquest commands and features.
	 * 
	 * @param loc A location
	 * @return True if the location is in an ignored world, else false
	 */
	boolean isWorldIgnored(Location loc);
	
	/**
	 * Checks if a world is ignored.
	 * Ignored worlds prevent most Konquest commands and features.
	 * 
	 * @param world A world
	 * @return True if the world is ignored, else false
	 */
	boolean isWorldIgnored(World world);
	
	/**
	 * Gets a random location in the wild, constrained by radius and offset in Konquest's configuration.
	 * 
	 * @param world The world to generate the random location
	 * @return A random location in the wild
	 */
	Location getRandomWildLocation(World world);
	
	/**
	 * Gets a random safe location in a surrounding chunk to the given center location.
	 * A safe location is one that should not kill the player.
	 * The resulting location will not be in the same chunk as the center location.
	 * 
	 * @param center The location to center the new location around
	 * @param radius The distance in blocks from the center to search for a random location
	 * @return The safe random location
	 */
	Location getSafeRandomCenteredLocation(Location center, int radius);
	
	/**
	 * Gets the primary display color based on relationships. This color is set in the Konquest configuration.
	 * There is a color for each {@link com.github.rumsfield.konquest.api.model relationship}.
	 * 
	 * @param displayKingdom The observing kingdom that should see the color
	 * @param contextKingdom The target kingdom whose relationship to the observer determines the color
	 * @return The primary display color
	 */
	String getDisplayPrimaryColor(KonquestKingdom displayKingdom, KonquestKingdom contextKingdom);
	
	/**
	 * Gets the primary display color based on relationships. This color is set in the Konquest configuration.
	 * There is a color for each {@link com.github.rumsfield.konquest.api.model relationship}.
	 * 
	 * @param displayPlayer The observing player who should see the color
	 * @param contextPlayer The target player whose relationship to the observer determines the color
	 * @return The primary display color
	 */
	String getDisplayPrimaryColor(KonquestOfflinePlayer displayPlayer, KonquestOfflinePlayer contextPlayer);
	
	/**
	 * Gets the primary display color based on relationships. This color is set in the Konquest configuration.
	 * There is a color for each {@link com.github.rumsfield.konquest.api.model relationship}.
	 * 
	 * @param displayPlayer The observing player who should see the color
	 * @param contextTerritory The target town whose relationship to the observer determines the color
	 * @return The primary display color
	 */
	String getDisplayPrimaryColor(KonquestOfflinePlayer displayPlayer, KonquestTerritory contextTerritory);
	
	/**
	 * Gets the secondary display color based on relationships. This color is set in the Konquest configuration.
	 * There is a color for each {@link com.github.rumsfield.konquest.api.model relationship}.
	 * 
	 * @param displayKingdom The observing kingdom that should see the color
	 * @param contextKingdom The target kingdom whose relationship to the observer determines the color
	 * @return The secondary display color
	 */
	String getDisplaySecondaryColor(KonquestKingdom displayKingdom, KonquestKingdom contextKingdom);
	
	/**
	 * Gets the secondary display color based on relationships. This color is set in the Konquest configuration.
	 * There is a color for each {@link com.github.rumsfield.konquest.api.model relationship}.
	 * 
	 * @param displayPlayer The observing player who should see the color
	 * @param contextPlayer The target player whose relationship to the observer determines the color
	 * @return The secondary display color
	 */
	String getDisplaySecondaryColor(KonquestOfflinePlayer displayPlayer, KonquestOfflinePlayer contextPlayer);
	
	/**
	 * Gets the secondary display color based on relationships. This color is set in the Konquest configuration.
	 * There is a color for each {@link com.github.rumsfield.konquest.api.model relationship}.
	 * 
	 * @param displayPlayer The observing player who should see the color
	 * @param contextTerritory The target town whose relationship to the observer determines the color
	 * @return The secondary display color
	 */
	String getDisplaySecondaryColor(KonquestOfflinePlayer displayPlayer, KonquestTerritory contextTerritory);
	
	/**
	 * Utility method to convert a location to a point representation of the chunk that contains the location.
	 * The point's "x" field is the chunk's X coordinate, and the point's "y" field is the chunk's Z coordinate.
	 * Note that the point does not preserve the World of the location, so keep track of that separately.
	 * 
	 * @param loc The location to convert into a point
	 * @return A point representing the chunk containing the location
	 */
	static Point toPoint(Location loc) {
		return new Point((int)Math.floor((double)loc.getBlockX()/16),(int)Math.floor((double)loc.getBlockZ()/16));
	}
}
