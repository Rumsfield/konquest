package com.github.rumsfield.konquest.api.event.player;

import org.bukkit.event.Cancellable;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.model.KonquestKingdom;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;
import org.bukkit.event.HandlerList;

import javax.annotation.Nonnull;

/**
 * Called before the given player has been assigned to the given kingdom.
 * <p>
 * Canceling this event will prevent the player from changing kingdoms.
 * </p>
 * 
 * @author Rumsfield
 */
public class KonquestPlayerKingdomEvent extends KonquestPlayerEvent implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private final KonquestKingdom newKingdom;
	private final KonquestKingdom oldKingdom;
	private boolean isCancelled;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param player The player
	 * @param newKingdom The new kingdom
	 * @param oldKingdom The old kingdom
	 */
	public KonquestPlayerKingdomEvent(KonquestAPI konquest, KonquestPlayer player, KonquestKingdom newKingdom, KonquestKingdom oldKingdom) {
		super(konquest, player);
		this.newKingdom = newKingdom;
		this.oldKingdom = oldKingdom;
		this.isCancelled = false;
	}
	
	/**
	 * Gets the kingdom that the player is changing to
	 * 
	 * @return The new kingdom
	 */
	public KonquestKingdom getNewKingdom() {
		return newKingdom;
	}
	
	/**
	 * Gets the kingdom that the player is leaving
	 * 
	 * @return The old kingdom
	 */
	public KonquestKingdom getOldKingdom() {
		return oldKingdom;
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
	 * Canceling this event will prevent the player from changing kingdoms.
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
