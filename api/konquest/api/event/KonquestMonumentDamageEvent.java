package konquest.api.event;

import konquest.api.KonquestAPI;
import konquest.api.model.KonquestPlayer;
import konquest.api.model.KonquestTerritory;

import org.bukkit.event.Cancellable;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Called when a town monument is damaged. Any block break inside of a town monument counts as damage.
 * This class is a wrapper for Bukkit's BlockBreakEvent.
 * 
 * @author Rumsfield
 *
 */
public class KonquestMonumentDamageEvent extends KonquestEvent implements Cancellable {
	
	private KonquestPlayer player;
	private KonquestTerritory territory;
	private BlockBreakEvent blockEvent;
	private boolean isCancelled;
	
	/**
	 * Constructor for a new event
	 * 
	 * @param konquest The KonquestAPI instance
	 * @param player The player that damaged the town monument
	 * @param territory The territory where the monument damage occured
	 * @param blockEvent The Bukkit BlockBreakEvent associated with this monument damage
	 */
	public KonquestMonumentDamageEvent(KonquestAPI konquest, KonquestPlayer player, KonquestTerritory territory, BlockBreakEvent blockEvent) {
		super(konquest);
		this.blockEvent = blockEvent;
		this.territory = territory;
		this.player = player;
		this.isCancelled = false;
	}
	
	/**
	 * Get the player that damaged the town monument
	 * 
	 * @return The player
	 */
	public KonquestPlayer getPlayer() {
		return player;
	}
	
	/**
	 * Get the territory where the monument damage occured
	 * 
	 * @return The territory
	 */
	public KonquestTerritory getTerritory() {
		return territory;
	}
	
	/**
	 * Get Bukkit's BlockBreakEvent associated with the damage
	 * 
	 * @return The break event that triggered this Konquest event
	 */
	public BlockBreakEvent getBlockEvent() {
		return blockEvent;
	}
	
	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean val) {
		isCancelled = val;
		blockEvent.setCancelled(val);
	}

}