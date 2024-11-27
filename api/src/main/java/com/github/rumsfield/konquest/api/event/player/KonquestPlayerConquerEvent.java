package com.github.rumsfield.konquest.api.event.player;

import org.bukkit.event.Cancellable;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;
import com.github.rumsfield.konquest.api.event.town.KonquestTownCaptureEvent;
import com.github.rumsfield.konquest.api.event.town.KonquestTownDestroyEvent;
import com.github.rumsfield.konquest.api.model.KonquestKingdom;
import org.bukkit.event.HandlerList;

import javax.annotation.Nonnull;

/**
 * Called before a player conquers a kingdom by capturing its capital.
 * <p>
 * When a player in an enemy kingdom captures a kingdom capital, it becomes a town in their kingdom.
 * When a barbarian player captures a kingdom capital, the capital is destroyed.
 * In both cases, the kingdom is removed, and its members are exiled to become barbarians.
 * This event is called before {@link KonquestTownDestroyEvent KonquestTownDestroyEvent},
 * and {@link KonquestTownCaptureEvent KonquestTownCaptureEvent}
 * Canceling this event will prevent the capital from being captured or destroyed, and the kingdom will remain.
 * The final monument critical block will not be broken.
 * </p>
 *
 * @author Rumsfield
 *
 */
public class KonquestPlayerConquerEvent extends KonquestPlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean isCancelled;
    private final KonquestKingdom kingdom;

    /**
     * Default constructor
     * @param konquest The API instance
     * @param player The player
     * @param kingdom The kingdom
     */
    public KonquestPlayerConquerEvent(KonquestAPI konquest, KonquestPlayer player, KonquestKingdom kingdom) {
        super(konquest, player);
        this.isCancelled = false;
        this.kingdom = kingdom;
    }

    /**
     * Gets the kingdom that will be removed once the capital is captured.
     *
     * @return The kingdom being conquered
     */
    public KonquestKingdom getKingdom() {
        return kingdom;
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
     * Canceling this event will prevent the capital from being captured or destroyed, and the kingdom will remain.
     * The final monument critical block will not be broken.
     *
     * @param val True to cancel this event, else false
     */
    @Override
    public void setCancelled(boolean val) {
        isCancelled = val;
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
