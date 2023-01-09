package com.github.rumsfield.konquest.api.event.camp;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.event.KonquestEvent;
import com.github.rumsfield.konquest.api.model.KonquestCamp;

/**
 * The base camp event.
 * 
 * @author Rumsfield
 *
 */
public class KonquestCampEvent extends KonquestEvent {

	private KonquestCamp camp;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param camp The camp
	 */
	public KonquestCampEvent(KonquestAPI konquest, KonquestCamp camp) {
		super(konquest);
		this.camp = camp;
	}
	
	/**
	 * Gets the town associated with the event.
	 * 
	 * @return The town
	 */
	public KonquestCamp getCamp() {
		return camp;
	}

}
