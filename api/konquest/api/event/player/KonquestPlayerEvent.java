package konquest.api.event.player;

import konquest.api.KonquestAPI;
import konquest.api.event.KonquestEvent;
import konquest.api.model.KonquestPlayer;

/**
 * The base player event.
 * <p>
 * This event is not called by Konquest. Do not listen for it.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestPlayerEvent extends KonquestEvent {

	private KonquestPlayer player;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param player The player
	 */
	public KonquestPlayerEvent(KonquestAPI konquest, KonquestPlayer player) {
		super(konquest);
		this.player = player;
	}
	
	/**
	 * Gets the player associated with the event.
	 * 
	 * @return The player
	 */
	public KonquestPlayer getPlayer() {
		return player;
	}
	
}
