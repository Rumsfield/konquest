package com.github.rumsfield.konquest.api.event.camp;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.model.KonquestCamp;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;

/**
 * Called before a player breaks a camp's bed to destroy it.
 * <p>
 * This event is called when any player attempts to break the camp's bed, even the camp owner.
 * Canceling this event will prevent the bed from breaking, and the camp will not be destroyed.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestCampDestroyEvent extends KonquestCampEvent implements Cancellable {

	private boolean isCancelled;
	
	private final KonquestPlayer player;
	private final Location location;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param camp The camp
	 * @param player The player
	 * @param location The location
	 */
	public KonquestCampDestroyEvent(KonquestAPI konquest, KonquestCamp camp, KonquestPlayer player, Location location) {
		super(konquest, camp);
		this.isCancelled = false;
		this.player = player;
		this.location = location;
	}
	
	/**
	 * Gets the player that is destroying the camp's bed.
	 * 
	 * @return The player
	 */
	public KonquestPlayer getPlayer() {
		return player;
	}
	
	/**
	 * Gets the location of camp's bed.
	 * 
	 * @return The location
	 */
	public Location getLocation() {
		return location;
	}

	/**
	 * Checks whether this event is canceled.
	 *
	 * @return True when the event is canceled, else false
	 */
	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	/**
	 * Controls whether the event is canceled.
	 * Canceling this event will prevent the bed from breaking, and the camp will not be destroyed.
	 *
	 * @param val True to cancel this event, else false
	 */
	@Override
	public void setCancelled(boolean val) {
		isCancelled = val;
	}

}
