package konquest.api.model;

import java.awt.Point;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * The base interface for any territory.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestTerritory {

	/**
	 * Checks whether the given location is inside of this territory.
	 * 
	 * @param loc The location to check
	 * @return True when the location is inside, else false
	 */
	public boolean isLocInside(Location loc);
	
	/**
	 * Checks whether the given location is outside of this territory but adjacent.
	 * Adjacent here means bordering the 4 main cardinal directions: north, east south, west.
	 * 
	 * @param loc The location to check
	 * @return True when the location is directly adjacent to this territory, else false
	 */
	public boolean isLocAdjacent(Location loc);
	
	/**
	 * Gets the center location of this territory.
	 * 
	 * @return The center location
	 */
	public Location getCenterLoc();
	
	/**
	 * Gets the world of this territory.
	 * 
	 * @return The territory's world
	 */
	public World getWorld();
	
	/**
	 * Gets the spawn location of this territory.
	 * Players who travel here will be teleported to this location.
	 * 
	 * @return The spawn location
	 */
	public Location getSpawnLoc();
	
	/**
	 * Gets the name of this territory.
	 * 
	 * @return The name
	 */
	public String getName();
	
	/**
	 * Gets the territory's kingdom.
	 * 
	 * @return The kingdom object
	 */
	public KonquestKingdom getKingdom();
	
	/**
	 * Gets the territory type of this territory.
	 * The type is an enum that's a convenience for determining sub-interfaces, like whether this territory is a town or a ruin.
	 * 
	 * @return The territory type
	 */
	public KonquestTerritoryType getTerritoryType();
	
	/**
	 * Gets the set of all land chunks claimed by this territory, as points.
	 * It is more efficient to work with points than Bukkit's Chunk objects.
	 * 
	 * @return The set of claimed points
	 */
	public HashSet<Point> getChunkPoints();
	
}
