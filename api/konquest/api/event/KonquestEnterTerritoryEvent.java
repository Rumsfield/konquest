package konquest.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerMoveEvent;

import konquest.api.model.KonquestPlayer;
import konquest.api.model.KonquestTerritory;

/**
 * Fired before a player enters any territory.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestEnterTerritoryEvent extends KonquestEvent, Cancellable {

	//TODO: use API interfaces
	
	/**
	 * Get the KonquestPlayer who entered the territory.
	 * 
	 * @return The player
	 */
	public KonquestPlayer getPlayer();
	
	/**
	 * Get the KonquestTerritory that the player is trying to enter.
	 * 
	 * @return The territory
	 */
	public KonquestTerritory getTerritory();
	
	/**
	 * Get Bukkit's PlayerMoveEvent that is associated with the player.
	 * 
	 * @return The move event
	 */
	public PlayerMoveEvent getMoveEvent();
	
	
	public boolean isCancelled();

	
	public void setCancelled(boolean val);
	
}
