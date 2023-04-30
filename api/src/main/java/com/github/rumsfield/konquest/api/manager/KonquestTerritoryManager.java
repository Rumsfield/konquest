package com.github.rumsfield.konquest.api.manager;

import com.github.rumsfield.konquest.api.model.KonquestTerritory;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * A manager for land and general territories in Konquest.
 *
 * @author Rumsfield
 *
 */
public interface KonquestTerritoryManager {

    /**
     * Claims the chunk at the given location for the nearest adjacent territory.
     * Adds the chunk to the chunk map of the territory.
     * The territory is chosen by finding the closest territory center location to the given location.
     *
     * @param loc The location to claim
     * @param force Ignore checks
     * @return  Status code
     * 			<br>0 - success
     * 			<br>1 - error, no adjacent territory
     * 			<br>2 - error, exceeds max distance
     * 			<br>3 - error, already claimed
     * 			<br>4 - error, cancelled by event
     * 		    <br>5 - error, blocked by property flag
     */
    int claimChunk(Location loc, boolean force);

    /**
     * Unclaims the chunk at the given location from its associated territory.
     * Removes the chunk from the chunk map of the territory.
     *
     * @param loc The location to unclaim
     * @param force Ignore checks
     * @return  Status code
     * 			<br>0 - success
     * 			<br>1 - error, no territory at location
     *			<br>2 - error, location in center chunk
     * 			<br>3 - error, internal territory chunk not found
     * 			<br>4 - error, cancelled by event
     * 		    <br>5 - error, blocked by property flag
     */
    int unclaimChunk(Location loc, boolean force);

    /**
     * Checks whether the chunk at the given location is claimed by a territory.
     *
     * @param loc The location to check
     * @return True when the location is inside of claimed territory, else false
     */
    boolean isChunkClaimed(Location loc);

    /**
     * Gets the territory at the given location.
     * If no territory exists, returns null.
     *
     * @param loc The location to request the territory
     * @return The territory at the given location, or null if no territory exists
     */
    KonquestTerritory getChunkTerritory(Location loc);

    /**
     * Finds the distance in chunks (16 blocks) from the given location to the nearest territory.
     * Checks kingdom capitals, towns, and ruins. Does not include camps.
     *
     * @param loc The location to search
     * @return The distance in chunks to the nearest territory, or Integer.MAX_VALUE if not found
     */
    int getDistanceToClosestTerritory(Location loc);

}
