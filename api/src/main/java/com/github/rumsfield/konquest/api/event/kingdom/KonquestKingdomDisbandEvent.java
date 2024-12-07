package com.github.rumsfield.konquest.api.event.kingdom;

import com.github.rumsfield.konquest.api.KonquestAPI;
import org.bukkit.event.HandlerList;

import javax.annotation.Nonnull;

/**
 * Called after a kingdom is disbanded and removed.
 * <p>
 * Kingdoms can be disbanded by their kingdom masters,
 * or when all kingdom members have been offline too long and get removed from the kingdom.
 * The kingdom referenced by this event no longer exists, so the getKingdom() method will return null.
 * </p>
 *
 * @author Rumsfield
 *
 */
public class KonquestKingdomDisbandEvent extends KonquestKingdomEvent {

    private static final HandlerList handlers = new HandlerList();
    private final String name;

    /**
     * Default constructor
     * @param konquest The API instance
     * @param name The kingdom of the removed kingdom
     */
    public KonquestKingdomDisbandEvent(KonquestAPI konquest, String name) {
        super(konquest, null);
        this.name = name;
    }

    /**
     * Gets the name of the removed kingdom
     *
     * @return The old kingdom name
     */
    public String getName() {
        return name;
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
