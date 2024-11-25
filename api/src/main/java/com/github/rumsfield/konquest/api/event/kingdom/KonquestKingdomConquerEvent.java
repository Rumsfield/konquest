package com.github.rumsfield.konquest.api.event.kingdom;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.event.player.KonquestPlayerConquerEvent;
import com.github.rumsfield.konquest.api.model.KonquestKingdom;

/**
 * Called after a kingdom is conquered by another kingdom and removed.
 * <p>
 * Kingdoms are conquered when enemy players capture the capital.
 * The kingdom has no more towns, and is removed.
 * The kingdom referenced by this event no longer exists, so the getKingdom() method will return null.
 * This event will not be invoked when {@link KonquestPlayerConquerEvent KonquestPlayerConquerEvent} is cancelled.
 * </p>
 *
 * @author Rumsfield
 *
 */
public class KonquestKingdomConquerEvent extends KonquestKingdomEvent {

    private final String name;
    private final KonquestKingdom conqueror;

    /**
     * Default constructor
     * @param konquest The API instance
     * @param name The name of the removed kingdom
     * @param conqueror The conqueror kingdom
     */
    public KonquestKingdomConquerEvent(KonquestAPI konquest, String name, KonquestKingdom conqueror) {
        super(konquest, null);
        this.name = name;
        this.conqueror = conqueror;
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
     * Gets the kingdom that conquered the removed kingdom
     *
     * @return The conqueror kingdom
     */
    public KonquestKingdom getConqueror() {
        return conqueror;
    }

}
