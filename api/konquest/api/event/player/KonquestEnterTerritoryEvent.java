package konquest.api.event.player;

import konquest.api.KonquestAPI;
import konquest.api.event.KonquestEvent;
import konquest.api.model.KonquestPlayer;
import konquest.api.model.KonquestTerritory;

import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Called when a player moves into a territory, by walking or teleporting.
 * This event class is a wrapper for Bukkit's PlayerMoveEvent.
 * 
 * @author Rumsfield
 *
 */
public class KonquestEnterTerritoryEvent extends KonquestEvent implements Cancellable {
	
	private KonquestPlayer player;
	private KonquestTerritory territory;
	private PlayerMoveEvent moveEvent;
	private boolean isCancelled;
	
	/**
	 * Constructor for a new event
	 * 
	 * @param konquest The KonquestAPI instance
	 * @param player The player that entered the territory
	 * @param territory The territory being entered
	 * @param moveEvent The Bukkit PlayerMoveEvent associated with the player
	 */
	public KonquestEnterTerritoryEvent(KonquestAPI konquest, KonquestPlayer player, KonquestTerritory territory, PlayerMoveEvent moveEvent) {
		super(konquest);
		this.player = player;
		this.territory = territory;
		this.moveEvent = moveEvent;
		this.isCancelled = false;
	}
	
	/**
	 * Get the player that entered the territory
	 * 
	 * @return The player
	 */
	public KonquestPlayer getPlayer() {
		return player;
	}
	
	/**
	 * Get the territory that is being entered
	 * 
	 * @return The territory
	 */
	public KonquestTerritory getTerritory() {
		return territory;
	}
	
	/**
	 * Get Bukkit's PlayerMoveEvent associated with the player
	 * 
	 * @return The move event that triggered this Konquest event
	 */
	public PlayerMoveEvent getMoveEvent() {
		return moveEvent;
	}
	
	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean val) {
		isCancelled = val;
		moveEvent.setCancelled(val);
	}

}
