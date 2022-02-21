package konquest.api.event.camp;

import konquest.api.KonquestAPI;
import konquest.api.model.KonquestCamp;
import konquest.api.model.KonquestPlayer;

/**
 * Called when a new camp is created by a barbarian player.
 * <p>
 * This event cannot be cancelled, as it is called after the camp is created.
 * To prevent players creating camps, listen for {@link konquest.api.event.player.KonquestPlayerCampEvent KonquestPlayerCampEvent} and cancel it.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestCampCreateEvent extends KonquestCampEvent {

	private KonquestPlayer player;
	
	public KonquestCampCreateEvent(KonquestAPI konquest, KonquestCamp camp, KonquestPlayer player) {
		super(konquest, camp);
		this.player = player;
	}
	
	/**
	 * Gets the player that created the camp.
	 * 
	 * @return The player
	 */
	public KonquestPlayer getPlayer() {
		return player;
	}

}
