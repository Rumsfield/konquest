package com.github.rumsfield.konquest.api.event.player;

import org.bukkit.event.Cancellable;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.model.KonquestKingdom;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;

/**
 * Called before the given player has been assigned to the given kingdom.
 * <p>
 * Canceling this event will prevent the player from changing kingdoms.
 * </p>
 * 
 * @author Rumsfield
 */
public class KonquestPlayerKingdomEvent extends KonquestPlayerEvent implements Cancellable {

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

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean val) {
		isCancelled = val;
	}

}
