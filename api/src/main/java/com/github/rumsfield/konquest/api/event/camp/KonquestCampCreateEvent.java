package com.github.rumsfield.konquest.api.event.camp;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.event.player.KonquestPlayerCampEvent;
import com.github.rumsfield.konquest.api.model.KonquestCamp;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;

/**
 * Called after a new camp is created by a barbarian player.
 * <p>
 * This event cannot be canceled, as it is called after the camp is created.
 * To prevent players creating camps, listen for {@link KonquestPlayerCampEvent KonquestPlayerCampEvent} and cancel it.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestCampCreateEvent extends KonquestCampEvent {

	private final KonquestPlayer player;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param camp The camp
	 * @param player The player
	 */
	public KonquestCampCreateEvent(KonquestAPI konquest, KonquestCamp camp, KonquestPlayer player) {
		super(konquest, camp);
		this.player = player;
	}
	
	/**
	 * Gets the player that created the camp.
	 * 
	 * @return The player
	 */
	public KonquestPlayer getPlayer() {
		return player;
	}

}
