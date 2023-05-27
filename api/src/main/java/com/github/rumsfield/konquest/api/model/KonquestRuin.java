package com.github.rumsfield.konquest.api.model;

import org.bukkit.Location;

import java.util.Set;

/**
 * A ruin territory with critical blocks and optional ruin golem spawn points.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestRuin extends KonquestTerritory {

	/**
	 * Checks whether this ruin is able to be captured.
	 * Ruins are captured when all critical blocks are destroyed.
	 * Capturing the ruin is disabled after it has been captured for a time duration based on the Konquest configuration.
	 * 
	 * @return True when this ruin cannot be captured, else false
	 */
    boolean isCaptureDisabled();

	/**
	 * Adds a new critical block to this ruin.
	 * The location must be inside of the ruin's claimed chunks.
	 * The block at the location must match the Material type specified in the Konquest configuration for ruin critical blocks.
	 *
	 * @param loc The location within the ruin to make a critical block
	 * @return True when the location was successfully added, else false
	 */
	boolean addCriticalLocation(Location loc);

	/**
	 * Adds a set of critical blocks to this ruin.
	 * This has the same constraints as addCriticalLocation(Location loc).
	 *
	 * @param locs The locations within the ruin to make into critical blocks
	 * @return True when all locations were successfully added, else false (some locations may have been added)
	 */
	boolean addCriticalLocation(Set<Location> locs);

	/**
	 * Remove all critical blocks of this ruin.
	 * This does not remove the blocks from the world, but rather the ruin no longer keeps track of any critical blocks.
	 *
	 */
	void clearCriticalLocations();

	/**
	 * Gets a set of all critical block locations in this ruin.
	 *
	 * @return The set of locations
	 */
	Set<Location> getCriticalLocations();

	/**
	 * Adds a new Ruin Golem spawn point to this ruin.
	 * The given location will be the spawn point for a single Ruin Golem.
	 * When the Ruin Golem dies, it will respawn at its spawn point.
	 * Each spawn point location in the ruin will yield a single Ruin Golem.
	 * The location must be within the ruin's claimed chunks.
	 *
	 * @param loc The location within the ruin to make a spawn point
	 * @return True when the location was successfully added, else false
	 */
	boolean addSpawnLocation(Location loc);

	/**
	 * Adds a set of spawn points to this ruin.
	 * This has the same constraints as addSpawnLocation(Location loc).
	 *
	 * @param locs The locations within the ruin to make into spawn points
	 * @return True when all locations were successfully added, else false (some locations may have been added)
	 */
	boolean addSpawnLocation(Set<Location> locs);

	/**
	 * Remove all spawn points of this ruin.
	 * This also removes any Ruin Golems in the world.
	 *
	 */
	void clearSpawnLocations();
	
	/**
	 * Gets a set of all ruin golem spawn locations in this ruin.
	 * 
	 * @return The set of locations
	 */
    Set<Location> getSpawnLocations();
	
	/**
	 * Gets the remaining number of unbroken critical blocks.
	 * When all critical blocks are broken, the ruin is captured temporarily.
	 * 
	 * @return The amount of unbroken critical blocks.
	 */
    int getRemainingCriticalHits();
	
	/**
	 * Checks if there are any online players inside of this ruin.
	 * 
	 * @return True when there are no players inside of this ruin, else false
	 */
    boolean isEmpty();

}
