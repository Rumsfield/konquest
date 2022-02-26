package konquest.api.event.town;

import org.bukkit.event.Cancellable;

import konquest.api.KonquestAPI;
import konquest.api.model.KonquestKingdom;
import konquest.api.model.KonquestPlayer;
import konquest.api.model.KonquestTown;

/**
 * Called before an enemy player captures a town for their own kingdom.
 * <p>
 * Players capture towns for their kingdoms when they destroy the final critical block in the town monument.
 * When a town is captured, it transfers ownership to the attacking player's kingdom.
 * Canceling this event will prevent the town from being captured, but the final critical block will still be broken.
 * </p>
 * 
 * <p>
 * After this event is cancelled, the town will remain un-captured, but all critical blocks will be broken.
 * The monument will regenerate as normal after enemy players break the most recent monument block.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestTownCaptureEvent extends KonquestTownEvent implements Cancellable {

	private boolean isCancelled;
	
	private KonquestPlayer player;
	private KonquestKingdom newKingdom;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param town The town
	 * @param player The player
	 * @param newKingdom The town's new kingdom
	 */
	public KonquestTownCaptureEvent(KonquestAPI konquest, KonquestTown town, KonquestPlayer player, KonquestKingdom newKingdom) {
		super(konquest, town);
		this.isCancelled = false;
		this.player = player;
		this.newKingdom = newKingdom;
	}
	
	/**
	 * Gets the player that captured this town, and is now the town lord.
	 * 
	 * @return The player
	 */
	public KonquestPlayer getPlayer() {
		return player;
	}
	
	/**
	 * Gets the new kingdom that will control the town when it is captured.
	 * 
	 * @return The old kingdom
	 */
	public KonquestKingdom getNewKingdom() {
		return newKingdom;
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
