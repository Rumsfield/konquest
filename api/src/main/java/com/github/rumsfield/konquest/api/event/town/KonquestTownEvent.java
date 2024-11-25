package com.github.rumsfield.konquest.api.event.town;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.event.KonquestEvent;
import com.github.rumsfield.konquest.api.model.KonquestTown;

import javax.annotation.Nullable;

/**
 * The base town event.
 * 
 * @author Rumsfield
 *
 */
public class KonquestTownEvent extends KonquestEvent {

	private final KonquestTown town;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param town The town
	 */
	public KonquestTownEvent(KonquestAPI konquest, KonquestTown town) {
		super(konquest);
		this.town = town;
	}
	
	/**
	 * Gets the town associated with the event.
	 * 
	 * @return The town, or null if the town no longer exists
	 */
	public @Nullable KonquestTown getTown() {
		return town;
	}

}
