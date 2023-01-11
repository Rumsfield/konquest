package com.github.rumsfield.konquest.api.event.town;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.event.KonquestEvent;
import com.github.rumsfield.konquest.api.model.KonquestTown;

/**
 * The base town event.
 * 
 * @author Rumsfield
 *
 */
public class KonquestTownEvent extends KonquestEvent {

	private KonquestTown town;
	
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
	 * @return The town
	 */
	public KonquestTown getTown() {
		return town;
	}

}
