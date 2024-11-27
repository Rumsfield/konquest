package com.github.rumsfield.konquest.api.event.player;

import org.bukkit.event.Cancellable;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.event.KonquestEvent;
import com.github.rumsfield.konquest.api.model.KonquestKingdom;
import com.github.rumsfield.konquest.api.model.KonquestOfflinePlayer;
import org.bukkit.event.HandlerList;

import javax.annotation.Nonnull;

/**
 * Called before the given player has been exiled from their current kingdom and made into a barbarian.
 * <p>
 * Canceling this event will prevent the player from being exiled.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestPlayerExileEvent extends KonquestEvent implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private boolean isCancelled;
	private final KonquestOfflinePlayer offlinePlayer;
	private final KonquestKingdom oldKingdom;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param offlinePlayer The player
	 * @param oldKingdom The kingdom
	 */
	public KonquestPlayerExileEvent(KonquestAPI konquest, KonquestOfflinePlayer offlinePlayer, KonquestKingdom oldKingdom) {
		super(konquest);
		this.isCancelled = false;
		this.offlinePlayer = offlinePlayer;
		this.oldKingdom = oldKingdom;
	}
	
	/**
	 * Gets the player that is being exiled.
	 * This player can be online or offline.
	 * 
	 * @return The player
	 */
	public KonquestOfflinePlayer getPlayer() {
		return offlinePlayer;
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
	 * Canceling this event will prevent the player from being exiled.
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
