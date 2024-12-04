package com.github.rumsfield.konquest.api.event.player;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;
import org.bukkit.event.HandlerList;

import javax.annotation.Nonnull;

/**
 * Called before a barbarian player places a bed in the wild to create their camp.
 * <p>
 * Canceling this event will prevent the camp creation and bed block placement.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestPlayerCampEvent extends KonquestPlayerEvent implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private boolean isCancelled;
	private final Location location;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param player The player
	 * @param location The location
	 */
	public KonquestPlayerCampEvent(KonquestAPI konquest, KonquestPlayer player, Location location) {
		super(konquest, player);
		this.isCancelled = false;
		this.location = location;
	}
	
	/**
	 * Gets the location of the new camp.
	 * This is the where the bed is being placed.
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
	 * Canceling this event will prevent the camp creation and bed block placement.
	 *
	 * @param val True to cancel this event, else false
	 */
	@Override
	public void setCancelled(boolean val) {
		isCancelled = val;
	}

	/**
	 * Get the handler list
	 *
	 * @return handlers
	 */
	public static HandlerList getHandlerList() {
		return handlers;
	}

	/**
	 * Get the handler list
	 *
	 * @return handlers
	 */
	@Override
	@Nonnull
	public HandlerList getHandlers() {
		return handlers;
	}
}
