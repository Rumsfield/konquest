package konquest.api.event.town;

import org.bukkit.event.Cancellable;

import konquest.api.KonquestAPI;
import konquest.api.model.KonquestPlayer;
import konquest.api.model.KonquestTown;

/**
 * Called before a barbarian player destroys a town by breaking its final critical block.
 * <p>
 * Barbarians can only destroy towns when allowed by the Konquest configuration.
 * Canceling this event will stop the final critical block break and prevent the town from being destroyed.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestTownDestroyEvent extends KonquestTownEvent implements Cancellable {

	private boolean isCancelled;
	
	private KonquestPlayer player;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param town The town
	 * @param player The player
	 */
	public KonquestTownDestroyEvent(KonquestAPI konquest, KonquestTown town, KonquestPlayer player) {
		super(konquest, town);
		this.isCancelled = false;
	}

	/**
	 * Gets the player that is destroying the town.
	 * 
	 * @return The player
	 */
	public KonquestPlayer getPlayer() {
		return player;
	}
	
	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean val) {
		isCancelled = val;
	}

}
