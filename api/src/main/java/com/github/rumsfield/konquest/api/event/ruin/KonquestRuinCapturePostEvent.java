package com.github.rumsfield.konquest.api.event.ruin;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;
import com.github.rumsfield.konquest.api.model.KonquestRuin;
import org.bukkit.event.HandlerList;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Called after a player successfully captures a ruin.
 * <p>
 * Players capture ruins by breaking all critical blocks inside.
 * When the final critical block is broken, all players located inside of the ruin receive a reward.
 * This event will not be invoked when {@link KonquestRuinCaptureEvent KonquestRuinCaptureEvent} is cancelled.
 * </p>
 *
 * @author Rumsfield
 *
 */
public class KonquestRuinCapturePostEvent extends KonquestRuinEvent {

    private static final HandlerList handlers = new HandlerList();
    private final KonquestPlayer player;
    private final List<? extends KonquestPlayer> rewardPlayers;

    /**
     * Default constructor
     * @param konquest The API instance
     * @param ruin The ruin
     * @param player The player
     * @param rewardPlayers The players receiving reward
     */
    public KonquestRuinCapturePostEvent(KonquestAPI konquest, KonquestRuin ruin, KonquestPlayer player, List<? extends KonquestPlayer> rewardPlayers) {
        super(konquest, ruin);
        this.player = player;
        this.rewardPlayers = rewardPlayers;
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

    /**
     * Get the handler list
     *
     * @return handlers
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * Get the handler list
     *
     * @return handlers
     */
    @Override
    @Nonnull
    public HandlerList getHandlers() {
        return handlers;
    }
}
