package konquest.api.event;

import konquest.api.KonquestAPI;

/*
 * Planned event tree
 * 
 * KonquestEvent - Base event class
 * 
 * 
 * 
 */

/**
 * A Konquest event.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestEvent {

	/**
	 * Get the API instance that fired this event.
	 * 
	 * @return The Konquest API
	 */
	public KonquestAPI getKonquest();
	
}
