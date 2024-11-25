package com.github.rumsfield.konquest.api.event.town;

import org.bukkit.event.Cancellable;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;
import com.github.rumsfield.konquest.api.model.KonquestTown;

/**
 * Called before a barbarian player destroys a town by breaking its final critical block.
 * <p>
 * Barbarians can only destroy towns when allowed by the Konquest configuration.
 * Canceling this event will prevent the town from being destroyed.
 * The monument will regenerate normally once enemy players stop breaking blocks.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestTownDestroyEvent extends KonquestTownEvent implements Cancellable {

	private boolean isCancelled;
	
	private final KonquestPlayer player;
	private final boolean isCapital;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param town The town
	 * @param player The player
	 * @param isCapital Is the town a capital
	 */
	public KonquestTownDestroyEvent(KonquestAPI konquest, KonquestTown town, KonquestPlayer player, boolean isCapital) {
		super(konquest, town);
		this.isCancelled = false;
		this.player = player;
		this.isCapital = isCapital;
	}

	/**
	 * Gets the player that is destroying the town.
	 * 
	 * @return The player
	 */
	public KonquestPlayer getPlayer() {
		return player;
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
	 * Canceling this event will prevent the town from being destroyed.
	 * The monument will regenerate normally once enemy players stop breaking blocks.
	 *
	 * @param val True to cancel this event, else false
	 */
	@Override
	public void setCancelled(boolean val) {
		isCancelled = val;
	}

}
