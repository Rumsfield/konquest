package com.github.rumsfield.konquest.api.event.kingdom;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.model.KonquestDiplomacyType;
import com.github.rumsfield.konquest.api.model.KonquestKingdom;
import org.bukkit.event.HandlerList;

import javax.annotation.Nonnull;

/**
 * Called after a kingdom changes its active diplomatic relationship with another kingdom.
 * <p>
 * Kingdoms have a diplomatic relationship with every other kingdom.
 * Some relationships have to be requested by both kingdoms, depending on the Konquest configuration.
 * Other relationships are instantly applied when either kingdom requests it.
 * This event cannot be canceled, as it is called after the active diplomatic relationship is applied.
 * </p>
 *
 * @author Rumsfield
 *
 */
public class KonquestKingdomDiplomacyEvent extends KonquestKingdomEvent {

    private static final HandlerList handlers = new HandlerList();
    private final KonquestKingdom targetKingdom;
    private final KonquestDiplomacyType diplomacy;

    /**
     * Default constructor
     * @param konquest The API instance
     * @param kingdom The kingdom
     * @param targetKingdom The other kingdom in the diplomatic relationship
     * @param diplomacy The new diplomatic relationship
     */
    public KonquestKingdomDiplomacyEvent(KonquestAPI konquest, KonquestKingdom kingdom, KonquestKingdom targetKingdom, KonquestDiplomacyType diplomacy) {
        super(konquest, kingdom);
        this.targetKingdom = targetKingdom;
        this.diplomacy = diplomacy;
    }

    /**
     * Gets the other kingdom that is the target of the diplomatic relationship.
     *
     * @return The target kingdom
     */
    public KonquestKingdom getTargetKingdom() {
        return targetKingdom;
    }

    /**
     * Gets the diplomatic relationship type between the two kingdoms.
     *
     * @return The diplomacy type
     */
    public KonquestDiplomacyType getDiplomacy() {
        return diplomacy;
    }

    /**
     * Convenience method for whether the diplomatic type is peace.
     *
     * @return True when the diplomacy is peace, else false
     */
    public boolean isPeace() {
        return diplomacy.equals(KonquestDiplomacyType.PEACE);
    }

    /**
     * Convenience method for whether the diplomatic type is war.
     *
     * @return True when the diplomacy is war, else false
     */
    public boolean isWar() {
        return diplomacy.equals(KonquestDiplomacyType.WAR);
    }

    /**
     * Convenience method for whether the diplomatic type is trade.
     *
     * @return True when the diplomacy is trade, else false
     */
    public boolean isTrade() {
        return diplomacy.equals(KonquestDiplomacyType.TRADE);
    }

    /**
     * Convenience method for whether the diplomatic type is alliance.
     *
     * @return True when the diplomacy is alliance, else false
     */
    public boolean isAlliance() {
        return diplomacy.equals(KonquestDiplomacyType.ALLIANCE);
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
