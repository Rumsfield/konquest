package com.github.rumsfield.konquest.api.event.player;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.event.KonquestEvent;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;

/**
 * The base player event.
 * 
 * @author Rumsfield
 *
 */
public class KonquestPlayerEvent extends KonquestEvent {

	private final KonquestPlayer player;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param player The player
	 */
	public KonquestPlayerEvent(KonquestAPI konquest, KonquestPlayer player) {
		super(konquest);
		this.player = player;
	}
	
	/**
	 * Gets the player associated with the event.
	 * 
	 * @return The player
	 */
	public KonquestPlayer getPlayer() {
		return player;
	}

	/**
	 * Convenience method to get the player's name.
	 *
	 * @return The player's name
	 */
	public String getPlayerName() {
		if (player != null && player.getBukkitPlayer() != null) {
			return player.getBukkitPlayer().getName();
		} else {
			return "";
		}
	}
	
}
