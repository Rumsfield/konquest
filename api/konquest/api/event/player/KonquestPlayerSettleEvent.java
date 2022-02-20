package konquest.api.event.player;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;

import konquest.api.KonquestAPI;
import konquest.api.model.KonquestKingdom;
import konquest.api.model.KonquestPlayer;

/**
 * Called before a player settles a new town using the "/k settle" command.
 * <p>
 * This event is called before the town is created.
 * Cancelling this event will prevent the town from being settled.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestPlayerSettleEvent extends KonquestPlayerEvent implements Cancellable {

	private boolean isCancelled;
	
	private KonquestKingdom kingdom;
	private Location location;
	private String name;
	
	public KonquestPlayerSettleEvent(KonquestAPI konquest, KonquestPlayer player, KonquestKingdom kingdom, Location location, String name) {
		super(konquest, player);
		this.isCancelled = false;
		this.kingdom = kingdom;
		this.location = location;
		this.name = name;
	}
	
	/**
	 * A convenience method to get the kingdom of the player settling the new town.
	 * 
	 * @return The kingdom
	 */
	public KonquestKingdom getKingdom() {
		return kingdom;
	}
	
	/**
	 * Gets the location where the new town will be created.
	 * 
	 * @return The center location of the new town
	 */
	public Location getLocation() {
		return location;
	}
	
	/**
	 * Gets the name of the new town.
	 * 
	 * @return The name
	 */
	public String getName() {
		return name;
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
