package konquest.api.event.player;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;

import konquest.api.KonquestAPI;
import konquest.api.model.KonquestPlayer;

/**
 * Called before a barbarian player places a bed in the wild to create their camp.
 * <p>
 * Cancelling this event will prevent the camp creation and bed block placement.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestPlayerCampEvent extends KonquestPlayerEvent implements Cancellable {

	private boolean isCancelled;
	
	private Location location;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param player The player
	 * @param location The location
	 */
	public KonquestPlayerCampEvent(KonquestAPI konquest, KonquestPlayer player, Location location) {
		super(konquest, player);
		this.isCancelled = false;
		this.location = location;
	}
	
	/**
	 * Gets the location of the new camp.
	 * This is the where the bed is being placed.
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
