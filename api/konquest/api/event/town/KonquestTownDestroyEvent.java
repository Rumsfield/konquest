package konquest.api.event.town;

import org.bukkit.event.Cancellable;

import konquest.api.KonquestAPI;
import konquest.api.model.KonquestPlayer;
import konquest.api.model.KonquestTown;

/**
 * Called before a barbarian player destroys a town by breaking its final critical block.
 * <p>
 * Barbarians can only destroy towns when allowed by the Konquest configuration.
 * Canceling this event will prevent the town from being destroyed, but the final critical block will still be broken.
 * </p>
 * 
 * <p>
 * After this event is cancelled, the town will remain un-destroyed, but all critical blocks will be broken.
 * The monument will regenerate as normal after enemy players break the most recent monument block.
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
