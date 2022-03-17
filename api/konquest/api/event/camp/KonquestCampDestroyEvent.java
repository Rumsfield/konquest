package konquest.api.event.camp;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;

import konquest.api.KonquestAPI;
import konquest.api.model.KonquestCamp;
import konquest.api.model.KonquestPlayer;

/**
 * Called before a player breaks a camp's bed to destroy it.
 * <p>
 * This event is called when any player breaks the camp's bed, even the camp owner.
 * Canceling this event will prevent the bed from breaking, and the camp will not be destroyed.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestCampDestroyEvent extends KonquestCampEvent implements Cancellable {

	private boolean isCancelled;
	
	private KonquestPlayer player;
	private Location location;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param camp The camp
	 * @param player The player
	 * @param location The location
	 */
	public KonquestCampDestroyEvent(KonquestAPI konquest, KonquestCamp camp, KonquestPlayer player, Location location) {
		super(konquest, camp);
		this.isCancelled = false;
		this.player = player;
		this.location = location;
	}
	
	/**
	 * Gets the player that is destroying the camp's bed.
	 * 
	 * @return The player
	 */
	public KonquestPlayer getPlayer() {
		return player;
	}
	
	/**
	 * Gets the location of camp's bed.
	 * 
	 * @return The location
	 */
	public Location getLocation() {
		return location;
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
