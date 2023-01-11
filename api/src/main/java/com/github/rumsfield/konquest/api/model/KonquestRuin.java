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
	 * Gets a set of all critical block locations in this ruin.
	 * 
	 * @return The set of locations
	 */
    Set<Location> getCriticalLocations();
	
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
