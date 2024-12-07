package com.github.rumsfield.konquest.api.event.ruin;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.event.KonquestEvent;
import com.github.rumsfield.konquest.api.model.KonquestRuin;

/**
 * The base ruin event.
 * 
 * @author Rumsfield
 *
 */
public abstract class KonquestRuinEvent extends KonquestEvent {

	private final KonquestRuin ruin;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param ruin The ruin
	 */
	public KonquestRuinEvent(KonquestAPI konquest, KonquestRuin ruin) {
		super(konquest);
		this.ruin = ruin;
	}
	
	/**
	 * Gets the ruin associated with the event.
	 * 
	 * @return The ruin
	 */
	public KonquestRuin getRuin() {
		return ruin;
	}

}
