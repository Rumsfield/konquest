package com.github.rumsfield.konquest.api.event.town;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.model.KonquestKingdom;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;
import com.github.rumsfield.konquest.api.model.KonquestTown;
import org.bukkit.event.HandlerList;

import javax.annotation.Nonnull;

/**
 * Called after an enemy player captures a town for their own kingdom.
 * <p>
 * Players capture towns for their kingdoms when they destroy the final critical block in the town monument.
 * When a town is captured, it transfers ownership to the attacking player's kingdom.
 * If the town is a capital, then the old kingdom will be removed, and the capital will be converted into a town for the new kingdom.
 * This event will not be invoked when {@link KonquestTownCaptureEvent KonquestTownCaptureEvent} is cancelled.
 * </p>
 *
 * @author Rumsfield
 *
 */
public class KonquestTownCapturePostEvent extends KonquestTownEvent {

    private static final HandlerList handlers = new HandlerList();
    private final KonquestPlayer player;
    private final KonquestKingdom oldKingdom;
    private final boolean isCapital;

    /**
     * Default constructor
     * @param konquest The API instance
     * @param town The town
     * @param player The player
     * @param oldKingdom The town's old kingdom
     * @param isCapital Whether the town was a capital
     */
    public KonquestTownCapturePostEvent(KonquestAPI konquest, KonquestTown town, KonquestPlayer player, KonquestKingdom oldKingdom, boolean isCapital) {
        super(konquest, town);
        this.player = player;
        this.oldKingdom = oldKingdom;
        this.isCapital = isCapital;
    }

    /**
     * Gets the player that captured the town, and is the new town lord.
     *
     * @return The player
     */
    public KonquestPlayer getPlayer() {
        return player;
    }

    /**
     * Gets the old kingdom that controlled the town.
     *
     * @return The old kingdom
     */
    public KonquestKingdom getOldKingdom() {
        return oldKingdom;
    }

    /**
     * Checks whether the town was a capital of the old kingdom.
     *
     * @return Whether the town was a capital
     */
    public boolean isCapital() {
        return isCapital;
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
