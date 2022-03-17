package konquest.api.event.territory;

import org.bukkit.event.Cancellable;

import konquest.api.KonquestAPI;
import konquest.api.event.KonquestEvent;
import konquest.api.model.KonquestPlayer;
import konquest.api.model.KonquestTerritory;

/**
 * Called before a player enters or leaves a territory. 
 * <p>
 * This only happens at chunk boundaries.
 * The wild is not a territory, and is represented as a null territory field.
 * When this event is cancelled, the player's movement will also be cancelled and they will be prevented from moving.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestTerritoryMoveEvent extends KonquestEvent implements Cancellable {

	private boolean isCancelled;
	
	private KonquestTerritory territoryTo;
	private KonquestTerritory territoryFrom;
	private KonquestPlayer player;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param territoryTo The territory moving to
	 * @param territoryFrom The territory moving from
	 * @param player The player
	 */
	public KonquestTerritoryMoveEvent(KonquestAPI konquest, KonquestTerritory territoryTo, KonquestTerritory territoryFrom, KonquestPlayer player) {
		super(konquest);
		this.isCancelled = false;
		this.territoryTo = territoryTo;
		this.territoryFrom = territoryFrom;
		this.player = player;
	}
	
	/**
	 * Get the player that moved between territories.
	 * 
	 * @return The player
	 */
	public KonquestPlayer getPlayer() {
		return player;
	}
	
	/**
	 * Get the territory that the player is moving into.
	 * Returns null when moving into the wild.
	 * 
	 * @return The territory, or null when wild
	 */
	public KonquestTerritory getTerritoryTo() {
		return territoryTo;
	}
	
	/**
	 * Get the territory that the player is moving out of.
	 * Returns null when moving out of the wild.
	 * 
	 * @return The territory, or null when wild
	 */
	public KonquestTerritory getTerritoryFrom() {
		return territoryFrom;
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
