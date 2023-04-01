package com.github.rumsfield.konquest.api.event.town;

import org.bukkit.event.Cancellable;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.model.KonquestKingdom;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;
import com.github.rumsfield.konquest.api.model.KonquestTown;

/**
 * Called before an enemy player captures a town for their own kingdom.
 * <p>
 * Players capture towns for their kingdoms when they destroy the final critical block in the town monument.
 * When a town is captured, it transfers ownership to the attacking player's kingdom.
 * If the town is a capital, then the old kingdom will be removed, and the capital will be converted into a town for the new kingdom.
 * Canceling this event will prevent the town from being captured, but the final critical block will still be broken.
 * </p>
 * 
 * <p>
 * After this event is cancelled, the town will remain un-captured, but all critical blocks will be broken.
 * The monument will regenerate as normal after enemy players break the most recent monument block.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestTownCaptureEvent extends KonquestTownEvent implements Cancellable {

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
	 * Gets the player that captured this town, and is now the town lord.
	 * 
	 * @return The player
	 */
	public KonquestPlayer getPlayer() {
		return player;
	}
	
	/**
	 * Gets the new kingdom that will control the town when it is captured.
	 * 
	 * @return The old kingdom
	 */
	public KonquestKingdom getNewKingdom() {
		return newKingdom;
	}

	/**
	 * Checks whether the town is a capital of the old kingdom.
	 *
	 * @return Whether the town is a capital
	 */
	public boolean isCapital() {
		return isCapital;
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
