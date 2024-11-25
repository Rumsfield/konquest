package com.github.rumsfield.konquest.api.event.ruin;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;
import com.github.rumsfield.konquest.api.model.KonquestRuin;
import org.bukkit.event.Cancellable;

import java.util.List;

/**
 * Called before a player captures a ruin, but after {@link KonquestRuinAttackEvent KonquestRuinAttackEvent}
 * <p>
 * Players capture ruins by breaking all critical blocks inside.
 * When the final critical block is broken, all players located inside of the ruin receive a reward.
 * Canceling this event will stop the final critical block break and prevent the ruin from being captured.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestRuinCaptureEvent extends KonquestRuinEvent implements Cancellable {

	private boolean isCancelled;
	
	private final KonquestPlayer player;
	private final List<? extends KonquestPlayer> rewardPlayers;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param ruin The ruin
	 * @param player The player
	 * @param rewardPlayers The players receiving reward
	 */
	public KonquestRuinCaptureEvent(KonquestAPI konquest, KonquestRuin ruin, KonquestPlayer player, List<? extends KonquestPlayer> rewardPlayers) {
		super(konquest, ruin);
		this.isCancelled = false;
		this.player = player;
		this.rewardPlayers = rewardPlayers;
	}

	/**
	 * Gets the player that will capture this ruin.
	 * This player broke the final critical block.
	 * 
	 * @return The player
	 */
	public KonquestPlayer getPlayer() {
		return player;
	}
	
	/**
	 * Gets the list of players that will get a reward for capturing the ruin.
	 * 
	 * @return The list of players
	 */
	public List<? extends KonquestPlayer> getRewardPlayers() {
		return rewardPlayers;
	}

	/**
	 * Checks whether this event is canceled.
	 *
	 * @return True when the event is canceled, else false
	 */
	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	/**
	 * Controls whether the event is canceled.
	 * Canceling this event will stop the final critical block break and prevent the ruin from being captured.
	 *
	 * @param val True to cancel this event, else false
	 */
	@Override
	public void setCancelled(boolean val) {
		isCancelled = val;
	}

}
