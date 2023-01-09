package com.github.rumsfield.konquest.api.model;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A town is a collection of land and has residents, plots and a monument.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestTown extends KonquestTerritory {
	
	/**
	 * Checks whether the given location is inside of this town's center chunk (the town monument).
	 * 
	 * @param loc The location to check
	 * @return True when the location is inside the center chunk, else false
	 */
	boolean isLocInsideCenterChunk(Location loc);
	
	/**
	 * Gets the monument instance of this town.
	 * 
	 * @return The monument
	 */
	KonquestMonument getMonument();
	
	/**
	 * Checks whether capturing this town is disabled.
	 * When a town is captured by an enemy, capturing is disabled temporarily.
	 * 
	 * @return True when capturing is disabled, else false
	 */
	boolean isCaptureDisabled();
	
	/**
	 * Checks whether this town is under attack.
	 * A town is attacked when an enemy breaks any blocks within town land.
	 * Towns eventually return to normal after the last enemy block break.
	 * 
	 * @return True when the town is attacked, else false
	 */
	boolean isAttacked();
	
	/**
	 * Checks whether the town is open.
	 * Kingdom members can join open towns without needing an invite.
	 * 
	 * @return True when the town is open, else false
	 */
	boolean isOpen();
	
	/**
	 * Checks whether the town option for allowing enemies to use redstone is enabled.
	 * When true, enemies will be able to use switches, buttons, doors, etc.
	 * 
	 * @return True when enemies are allowed to use redstone, else false
	 */
	boolean isEnemyRedstoneAllowed();
	
	/**
	 * Checks whether the town option for allowing residents to only build on plots is enabled.
	 * 
	 * @return True when residents may only build on plots they own, else false
	 */
	boolean isPlotOnly();
	
	/**
	 * Checks whether the given UUID is the town lord.
	 * 
	 * @param id The UUID to check
	 * @return True when the UUID is the town lord, else false
	 */
	boolean isLord(UUID id);
	
	/**
	 * Checks whether the given player is the town lord.
	 * 
	 * @param player The player to check
	 * @return True when the player is the town lord, else false
	 */
	boolean isPlayerLord(OfflinePlayer player);
	
	/**
	 * Checks whether the given player is a town knight.
	 * The town lord is also considered a town knight.
	 * 
	 * @param player The player to check
	 * @return True when the player is a town knight or the town lord, else false
	 */
	boolean isPlayerKnight(OfflinePlayer player);
	
	/**
	 * Checks whether the given player is a town resident.
	 * The town lord and town knights are considered residents.
	 * 
	 * @param player The player to check
	 * @return True when the player is a normal resident, town knight or the town lord, else false
	 */
	boolean isPlayerResident(OfflinePlayer player);
	
	/**
	 * Checks whether the town lord is valid.
	 * Towns may have no town lord, in which case this method will return false.
	 * 
	 * @return True when there is a valid town lord, else false
	 */
	boolean isLordValid();
	
	/**
	 * Gets the town lord player.
	 * Returns null when there is an invalid town lord.
	 * 
	 * @return The town lord player, or null
	 */
	OfflinePlayer getPlayerLord();
	
	/**
	 * Gets the UUID of the town lord.
	 * Returns null if there is no town lord.
	 * 
	 * @return The UUID of the town lord, or null
	 */
	UUID getLord();
	
	/**
	 * Gets the list of all town knights, including the town lord.
	 * 
	 * @return The list of players
	 */
	ArrayList<OfflinePlayer> getPlayerKnights();
	
	/**
	 * Gets the list of all town residents, including town knights and the town lord.
	 * 
	 * @return The list of players
	 */
	ArrayList<OfflinePlayer> getPlayerResidents();
	
	/**
	 * Checks whether this town has the given upgrade.
	 * Any upgrade level above 0 will result in this method returning true.
	 * An upgrade level of 0 means it is disabled, and this will return false.
	 * 
	 * @param upgrade The upgrade to check
	 * @return True if this town has the upgrade enabled, else false
	 */
	boolean hasUpgrade(KonquestUpgradable upgrade);
	
	/**
	 * Checks whether this town has the given upgrade, but it's disabled.
	 * A town that has an upgrade, but then lacks the population requirements, will have that upgrade disabled.
	 * 
	 * @param upgrade The upgrade to check
	 * @return True if the upgrade is disabled, else false
	 */
	boolean isUpgradeDisabled(KonquestUpgradable upgrade);
	
	/**
	 * Checks whether the town is protected by the Town Watch upgrade.
	 * A protected town cannot be attacked by enemies.
	 * 
	 * @return True when the town is protected, else false
	 */
	boolean isTownWatchProtected();
	
	/**
	 * Checks whether the town has active shields.
	 * 
	 * @return True when the town has active shields, else false
	 */
	boolean isShielded();
	
	/**
	 * Checks whether the town has active armor.
	 * 
	 * @return True when the town has active armor, else false
	 */
	boolean isArmored();
	
	/**
	 * Checks whether the town has a plot at the given location.
	 * Town plots can use multiple chunks and can permit only their members to build in them.
	 * 
	 * @param loc The location to check
	 * @return True when there is a plot at the location, else false
	 */
	boolean hasPlot(Location loc);
	
	/**
	 * Gets the town plot at the given location.
	 * Returns null if no plot exists.
	 * 
	 * @param loc The location to get the plot
	 * @return The town plot, or null when none exists
	 */
	KonquestPlot getPlot(Location loc);
	
	/**
	 * Gets all town plots in this town.
	 * 
	 * @return The list of plots
	 */
	List<? extends KonquestPlot> getPlots();

}
