package com.github.rumsfield.konquest.api.event.player;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;

/**
 * Called after a player changes their accomplishment prefix.
 * 
 * @author Rumsfield
 *
 */
public class KonquestPlayerPrefixEvent extends KonquestPlayerEvent {

	private final String prefix;
	private final boolean isDisabled;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param player The player
	 * @param prefix The prefix
	 * @param isDisabled Is the prefix disabled
	 */
	public KonquestPlayerPrefixEvent(KonquestAPI konquest, KonquestPlayer player, String prefix, boolean isDisabled) {
		super(konquest, player);
		this.prefix = prefix;
		this.isDisabled = isDisabled;
	}
	
	/**
	 * Gets the current prefix of the player, as shown in chat.
	 * When isDisabled returns true, no prefix will show in chat, and the prefix will be an empty string.
	 * 
	 * @return The prefix, or an empty string when disabled
	 */
	public String getPrefix() {
		return prefix;
	}
	
	/**
	 * Checks whether the player disabled their prefix.
	 * 
	 * @return True when the prefix was disabled, else false
	 */
	public boolean isDisabled() {
		return isDisabled;
	}

}
