package konquest.api.event.ruin;

import java.util.List;

import org.bukkit.event.Cancellable;

import konquest.api.KonquestAPI;
import konquest.api.model.KonquestPlayer;
import konquest.api.model.KonquestRuin;

/**
 * Called before a player captures a ruin.
 * <p>
 * Players capture ruins by breaking all critical blocks inside.
 * When the final critical block is broken, all players located inside of the ruin receive a reward.
 * Cancelling this event will stop the final critical block break and prevent the ruin from being captured.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestRuinCaptureEvent extends KonquestRuinEvent implements Cancellable {

	private boolean isCancelled;
	
	private KonquestPlayer player;
	private List<? extends KonquestPlayer> rewardPlayers;
	
	public KonquestRuinCaptureEvent(KonquestAPI konquest, KonquestRuin ruin, KonquestPlayer player, List<? extends KonquestPlayer> rewardPlayers) {
		super(konquest, ruin);
		this.isCancelled = false;
	}

	/**
	 * Gets the player that captured this ruin.
	 * This player broke the final critical block.
	 * 
	 * @return The player
	 */
	public KonquestPlayer getPlayer() {
		return player;
	}
	
	/**
	 * Gets the list of players that get a reward for capturing the ruin.
	 * 
	 * @return The list of players
	 */
	public List<? extends KonquestPlayer> getRewardPlayers() {
		return rewardPlayers;
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
