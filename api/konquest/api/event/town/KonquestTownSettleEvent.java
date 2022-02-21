package konquest.api.event.town;

import konquest.api.KonquestAPI;
import konquest.api.model.KonquestKingdom;
import konquest.api.model.KonquestPlayer;
import konquest.api.model.KonquestTown;

/**
 * Called when a new town is settled by a player.
 * <p>
 * This event cannot be cancelled, as it is called after the town is created.
 * To prevent players settling towns, listen for {@link konquest.api.event.player.KonquestPlayerSettleEvent KonquestPlayerSettleEvent} and cancel it.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestTownSettleEvent extends KonquestTownEvent {

	private KonquestPlayer player;
	private KonquestKingdom kingdom;
	
	public KonquestTownSettleEvent(KonquestAPI konquest, KonquestTown town, KonquestPlayer player, KonquestKingdom kingdom) {
		super(konquest, town);
		this.player = player;
		this.kingdom = kingdom;
	}
	
	/**
	 * Gets the player that settled the town.
	 * This player is the town lord.
	 * 
	 * @return The player
	 */
	public KonquestPlayer getPlayer() {
		return player;
	}
	
	/**
	 * Gets the kingdom of the new town.
	 * 
	 * @return The kingdom
	 */
	public KonquestKingdom getKingdom() {
		return kingdom;
	}

}
