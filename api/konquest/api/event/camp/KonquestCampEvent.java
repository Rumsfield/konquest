package konquest.api.event.camp;

import konquest.api.KonquestAPI;
import konquest.api.event.KonquestEvent;
import konquest.api.model.KonquestCamp;

/**
 * The base camp event.
 * <p>
 * This event is not called by Konquest. Do not listen for it.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestCampEvent extends KonquestEvent {

	private KonquestCamp camp;
	
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
