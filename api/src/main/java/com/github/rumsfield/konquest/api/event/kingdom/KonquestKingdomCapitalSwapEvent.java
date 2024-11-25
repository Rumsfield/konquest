package com.github.rumsfield.konquest.api.event.kingdom;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.model.KonquestCapital;
import com.github.rumsfield.konquest.api.model.KonquestKingdom;
import com.github.rumsfield.konquest.api.model.KonquestTown;

/**
 * Called after a kingdom swaps its capital to another town.
 * <p>
 * A capital swap involves changing a town into the kingdom's new capital, and preserves the town's land claims,
 * upgrades, residents, and all other attributes. The original capital takes the name of the town.
 * Kingdom masters may move the capital to another town, if allowed by the Konquest configuration.
 * This event cannot be canceled, as it is called after the capital swap is finished.
 * </p>
 *
 * @author Rumsfield
 *
 */
public class KonquestKingdomCapitalSwapEvent extends KonquestKingdomEvent {

    private final KonquestCapital capital;
    private final KonquestTown town;

    /**
     * Default constructor
     * @param konquest The API instance
     * @param kingdom The kingdom
     * @param capital The new capital
     * @param town The town (old capital)
     */
    public KonquestKingdomCapitalSwapEvent(KonquestAPI konquest, KonquestKingdom kingdom, KonquestCapital capital, KonquestTown town) {
        super(konquest, kingdom);
        this.capital = capital;
        this.town = town;
    }

    /**
     * Gets the capital after the swap.
     * This used to be a town in the kingdom.
     *
     * @return The new capital
     */
    public KonquestCapital getCapital() {
        return capital;
    }

    /**
     * Gets the town after the swap.
     * This used to be the kingdom's capital.
     *
     * @return The new town
     */
    public KonquestTown getTown() {
        return town;
    }

}
