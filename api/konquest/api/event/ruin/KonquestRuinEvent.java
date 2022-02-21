package konquest.api.event.ruin;

import konquest.api.KonquestAPI;
import konquest.api.event.KonquestEvent;
import konquest.api.model.KonquestRuin;

/**
 * The base ruin event.
 * <p>
 * This event is not called by Konquest. Do not listen for it.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestRuinEvent extends KonquestEvent {

	private KonquestRuin ruin;
	
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
