package com.github.rumsfield.konquest.api.event.town;

import org.bukkit.event.Cancellable;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.model.KonquestKingdom;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;
import com.github.rumsfield.konquest.api.model.KonquestTown;
import org.bukkit.event.HandlerList;

import javax.annotation.Nonnull;

/**
 * Called before an enemy player captures a town for their own kingdom.
 * <p>
 * Players capture towns for their kingdoms when they destroy the final critical block in the town monument.
 * When a town is captured, it transfers ownership to the attacking player's kingdom.
 * If the town is a capital, then the old kingdom will be removed, and the capital will be converted into a town for the new kingdom.
 * Canceling this event will prevent the town from being captured by enemy players.
 * The monument will regenerate normally once enemy players stop breaking blocks, and the town will remain in its original kingdom.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestTownCaptureEvent extends KonquestTownEvent implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private boolean isCancelled;
	private final KonquestPlayer player;
	private final KonquestKingdom newKingdom;
	private final boolean isCapital;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param town The town
	 * @param player The player
	 * @param newKingdom The town's new kingdom
	 * @param isCapital Whether the town was a capital
	 */
	public KonquestTownCaptureEvent(KonquestAPI konquest, KonquestTown town, KonquestPlayer player, KonquestKingdom newKingdom, boolean isCapital) {
		super(konquest, town);
		this.isCancelled = false;
		this.player = player;
		this.newKingdom = newKingdom;
		this.isCapital = isCapital;
	}
	
	/**
	 * Gets the player that will capture the town, and will become the town lord.
	 * 
	 * @return The player
	 */
	public KonquestPlayer getPlayer() {
		return player;
	}
	
	/**
	 * Gets the new kingdom that will control the town when it is captured.
	 * 
	 * @return The new kingdom
	 */
	public KonquestKingdom getNewKingdom() {
		return newKingdom;
	}

	/**
	 * Checks whether the town is a capital of its kingdom.
	 *
	 * @return Whether the town is a capital
	 */
	public boolean isCapital() {
		return isCapital;
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
	 * Canceling this event will prevent the town from being captured by enemy players.
	 * The monument will regenerate normally once enemy players stop breaking blocks, and the town will remain in its original kingdom.
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
