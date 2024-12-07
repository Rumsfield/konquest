package com.github.rumsfield.konquest.api.event.kingdom;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.event.KonquestEvent;
import com.github.rumsfield.konquest.api.model.KonquestKingdom;

import javax.annotation.Nullable;

/**
 * The base kingdom event.
 *
 * @author Rumsfield
 *
 */
public abstract class KonquestKingdomEvent extends KonquestEvent {

    private final KonquestKingdom kingdom;

    /**
     * Default constructor
     * @param konquest The API instance
     * @param kingdom The kingdom
     */
    public KonquestKingdomEvent(KonquestAPI konquest, KonquestKingdom kingdom) {
        super(konquest);
        this.kingdom = kingdom;
    }

    /**
     * Gets the kingdom associated with the event.
     *
     * @return The kingdom, or null if the kingdom no longer exists
     */
    public @Nullable KonquestKingdom getKingdom() {
        return kingdom;
    }

}
