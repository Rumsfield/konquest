package com.github.rumsfield.konquest.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import javax.annotation.Nonnull;


import com.github.rumsfield.konquest.api.KonquestAPI;

/**
 * A base event from Konquest, with a reference to the API instance.
 * 
 * @author Rumsfield
 *
 */
public abstract class KonquestEvent extends Event {

	private final KonquestAPI konquest;
	
	/**
	 * Construct an event with a reference to the Konquest API
	 * 
	 * @param konquest The KonquestAPI instance
	 */
	public KonquestEvent(KonquestAPI konquest) {
		this.konquest = konquest;
	}
	
	/**
	 * Get the KonquestAPI instance that sent this event
	 * 
	 * @return The KonquestAPI instance
	 */
	public KonquestAPI getKonquest() {
		return konquest;
	}

	@Nonnull
	public abstract HandlerList getHandlers();

}
