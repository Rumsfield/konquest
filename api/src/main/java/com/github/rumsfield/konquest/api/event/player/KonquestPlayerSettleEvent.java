package com.github.rumsfield.konquest.api.event.player;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.model.KonquestKingdom;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;

/**
 * Called before a player settles a new town using the "/k settle" command.
 * <p>
 * This event is called before the town is created.
 * Canceling this event will prevent the town from being settled.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestPlayerSettleEvent extends KonquestPlayerEvent implements Cancellable {

	private boolean isCancelled;
	
	private final KonquestKingdom kingdom;
	private final Location location;
	private final String name;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param player The player
	 * @param kingdom The kingdom
	 * @param location The location
	 * @param name The name of the town
	 */
	public KonquestPlayerSettleEvent(KonquestAPI konquest, KonquestPlayer player, KonquestKingdom kingdom, Location location, String name) {
		super(konquest, player);
		this.isCancelled = false;
		this.kingdom = kingdom;
		this.location = location;
		this.name = name;
	}
	
	/**
	 * A convenience method to get the kingdom of the player settling the new town.
	 * 
	 * @return The kingdom
	 */
	public KonquestKingdom getKingdom() {
		return kingdom;
	}
	
	/**
	 * Gets the location where the new town will be created.
	 * 
	 * @return The center location of the new town
	 */
	public Location getLocation() {
		return location;
	}
	
	/**
	 * Gets the name of the new town.
	 * 
	 * @return The name
	 */
	public String getName() {
		return name;
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
	 * Canceling this event will prevent the town from being settled.
	 *
	 * @param val True to cancel this event, else false
	 */
	@Override
	public void setCancelled(boolean val) {
		isCancelled = val;
	}

}
