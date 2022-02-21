package konquest.api.event.territory;

import org.bukkit.Chunk;
import org.bukkit.event.Cancellable;

import konquest.api.KonquestAPI;
import konquest.api.event.KonquestEvent;
import konquest.api.model.KonquestPlayer;
import konquest.api.model.KonquestTerritory;

/**
 * Represents a generic change of a territory's land chunk.
 * <p>
 * The territory will be modified by either claiming or unclaiming the given chunk.
 * Normal players typically can only claim land for towns, and cannot unclaim any land.
 * Admins can claim and unclaim land for any territory.
 * When this event is cancelled, the territory will be unmodified.
 * </p>
 * @author Rumsfield
 *
 */
public class KonquestTerritoryChunkEvent extends KonquestEvent implements Cancellable {

	private boolean isCancelled;
	
	private KonquestTerritory territory;
	private KonquestPlayer player;
	private Chunk chunk;
	private boolean isClaimed;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param territory The territory
	 * @param player The player
	 * @param chunk The chunk
	 * @param isClaimed Is the chunk claimed or unclaimed
	 */
	public KonquestTerritoryChunkEvent(KonquestAPI konquest, KonquestTerritory territory, KonquestPlayer player, Chunk chunk, boolean isClaimed) {
		super(konquest);
		this.isCancelled = false;
		this.territory = territory;
		this.player = player;
		this.chunk = chunk;
		this.isClaimed = isClaimed;
	}
	
	/**
	 * Get the player that is modifying the territory.
	 * 
	 * @return The player
	 */
	public KonquestPlayer getPlayer() {
		return player;
	}
	
	/**
	 * Get the territory that is being modified.
	 * 
	 * @return The territory
	 */
	public KonquestTerritory getTerritory() {
		return territory;
	}
	
	/**
	 * Get the chunk of land that is being modified.
	 * 
	 * @return The chunk
	 */
	public Chunk getLand() {
		return chunk;
	}
	
	/**
	 * Checks whether the territory is claiming the chunk or not.
	 * 
	 * @return True when the territory is claiming the chunk, else false when it is unclaiming it.
	 */
	public boolean isClaimed() {
		return isClaimed;
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
