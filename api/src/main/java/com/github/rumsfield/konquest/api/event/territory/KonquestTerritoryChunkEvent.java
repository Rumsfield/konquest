package com.github.rumsfield.konquest.api.event.territory;

import java.awt.Point;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.Cancellable;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.event.KonquestEvent;
import com.github.rumsfield.konquest.api.model.KonquestTerritory;

/**
 * Represents a generic change of a territory's land chunk.
 * <p>
 * The territory will be modified by either claiming or unclaiming the given chunk.
 * Normal players typically can only claim land for towns, and cannot unclaim any land.
 * Admins can claim and unclaim land for any territory.
 * When this event is cancelled, the territory will be unmodified.
 * </p>
 * @author Rumsfield
 *
 */
public class KonquestTerritoryChunkEvent extends KonquestEvent implements Cancellable {

	private boolean isCancelled;
	
	private KonquestTerritory territory;
	private Location location;
	private Set<Point> points;
	private boolean isClaimed;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param territory The territory
	 * @param location The location
	 * @param points The point(s) of land
	 * @param isClaimed Are the point(s) claimed or unclaimed
	 */
	public KonquestTerritoryChunkEvent(KonquestAPI konquest, KonquestTerritory territory, Location location, Set<Point> points, boolean isClaimed) {
		super(konquest);
		this.isCancelled = false;
		this.territory = territory;
		this.location = location;
		this.points = points;
		this.isClaimed = isClaimed;
	}
	
	/**
	 * Gets the territory that is being modified.
	 * 
	 * @return The territory
	 */
	public KonquestTerritory getTerritory() {
		return territory;
	}
	
	/**
	 * Gets the location in the chunk of land being modified.
	 * 
	 * @return The location
	 */
	public Location getLocation() {
		return location;
	}
	
	/**
	 * Gets the world of the chunk(s) of land being modified.
	 * This is a convenience method, getting the world from the location.
	 * 
	 * @return The world
	 */
	public World getWorld() {
		return location.getWorld();
	}
	
	/**
	 * Gets the set of points representing land chunks which were modified.
	 * Each point represents a chunk, in the same world as the location.
	 * 
	 * @return The points of land
	 */
	public Set<Point> getPoints() {
		return points;
	}
	
	/**
	 * Checks whether the territory is claiming the chunk or not.
	 * 
	 * @return True when the territory is claiming the chunk, else false when it is unclaiming it.
	 */
	public boolean isClaimed() {
		return isClaimed;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean val) {
		isCancelled = val;
	}
}
