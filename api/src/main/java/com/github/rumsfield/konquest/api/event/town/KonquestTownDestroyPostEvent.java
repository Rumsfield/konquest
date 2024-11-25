package com.github.rumsfield.konquest.api.event.town;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;

/**
 * Called after a barbarian player destroys a town by breaking its final critical block,
 * or after a town has been destroyed by its own kingdom members.
 * <p>
 * Barbarians can only destroy towns when allowed by the Konquest configuration.
 * Kingdom masters and town lords can destroy their own towns when allowed by the Konquest configuration.
 * The town referenced by this event no longer exists, so the getTown() method will return null.
 * This event will not be invoked when {@link KonquestTownDestroyEvent KonquestTownDestroyEvent} is cancelled and
 * the player is a barbarian.
 * </p>
 *
 * @author Rumsfield
 *
 */
public class KonquestTownDestroyPostEvent extends KonquestTownEvent {

    private final String townName;
    private final String kingdomName;
    private final KonquestPlayer player;
    private final boolean isCapital;

    /**
     * Default constructor
     * @param konquest The API instance
     * @param townName The town name
     * @param kingdomName The kingdom name
     * @param player The player
     * @param isCapital Is the town a capital
     */
    public KonquestTownDestroyPostEvent(KonquestAPI konquest, String townName, String kingdomName, KonquestPlayer player, boolean isCapital) {
        super(konquest, null);
        this.townName = townName;
        this.kingdomName = kingdomName;
        this.player = player;
        this.isCapital = isCapital;
    }

    /**
     * Gets the name of the destroyed town.
     *
     * @return The destroyed town's name
     */
    public String getTownName() {
        return townName;
    }

    /**
     * Gets the name of the destroyed town's kingdom.
     *
     * @return The destroyed town's kingdom's name
     */
    public String getKingdomName() {
        return kingdomName;
    }

    /**
     * Gets the player that destroyed the town.
     * This player could be a barbarian, or a member of the town's kingdom.
     *
     * @return The player
     */
    public KonquestPlayer getPlayer() {
        return player;
    }

    /**
     * Checks whether the town was a capital of its kingdom.
     *
     * @return Whether the town was a capital
     */
    public boolean isCapital() {
        return isCapital;
    }

    /**
     * Convenience method for checking whether the town was destroyed by a barbarian player.
     *
     * @return The player
     */
    public boolean isBarbarian() {
        return player.isBarbarian();
    }

}
