package konquest.api.event.player;

import org.bukkit.event.Cancellable;

import konquest.api.KonquestAPI;
import konquest.api.event.KonquestEvent;
import konquest.api.model.KonquestKingdom;
import konquest.api.model.KonquestOfflinePlayer;

/**
 * Called before the given player has been exiled from their current kingdom and made into a barbarian.
 * <p>
 * Canceling this event will prevent the player from being exiled.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestPlayerExileEvent extends KonquestEvent implements Cancellable {

	private boolean isCancelled;
	private KonquestOfflinePlayer offlinePlayer;
	private KonquestKingdom oldKingdom;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param offlinePlayer The player
	 * @param oldKingdom The kingdom
	 */
	public KonquestPlayerExileEvent(KonquestAPI konquest, KonquestOfflinePlayer offlinePlayer, KonquestKingdom oldKingdom) {
		super(konquest);
		this.isCancelled = false;
		this.offlinePlayer = offlinePlayer;
		this.oldKingdom = oldKingdom;
	}
	
	/**
	 * Gets the player that is being exiled.
	 * This player can be online or offline.
	 * 
	 * @return The player
	 */
	public KonquestOfflinePlayer getPlayer() {
		return offlinePlayer;
	}
	
	/**
	 * Gets the kingdom that the player is leaving
	 * 
	 * @return The old kingdom
	 */
	public KonquestKingdom getOldKingdom() {
		return oldKingdom;
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
