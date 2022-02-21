package konquest.api.event.town;

import konquest.api.KonquestAPI;
import konquest.api.event.KonquestEvent;
import konquest.api.model.KonquestTown;

/**
 * The base town event.
 * <p>
 * This event is not called by Konquest. Do not listen for it.
 * </p>
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
