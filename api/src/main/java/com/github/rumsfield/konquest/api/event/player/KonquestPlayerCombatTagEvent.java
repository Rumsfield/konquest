package com.github.rumsfield.konquest.api.event.player;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;

/**
 * Called when a player is attacked by another player.
 * <p>
 * Attacking another player causes the victim to become combat tagged.
 * A player that is combat tagged is restricted from using a list of commands from the Konquest configuration.
 * Canceling this event will prevent the victim from becoming combat tagged.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestPlayerCombatTagEvent extends KonquestPlayerEvent implements Cancellable {

	private boolean isCancelled;
	
	private final KonquestPlayer attacker;
	private final Location location;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param player The player
	 * @param attacker The attacking player
	 * @param location The location
	 */
	public KonquestPlayerCombatTagEvent(KonquestAPI konquest, KonquestPlayer player, KonquestPlayer attacker, Location location) {
		super(konquest, player);
		this.isCancelled = false;
		this.attacker = attacker;
		this.location = location;
	}
	
	/**
	 * Gets the attacking player.
	 * 
	 * @return The attacker
	 */
	public KonquestPlayer getAttacker() {
		return attacker;
	}
	
	/**
	 * Gets the location that the player was combat tagged at.
	 * 
	 * @return The location
	 */
	public Location getLocation() {
		return location;
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
