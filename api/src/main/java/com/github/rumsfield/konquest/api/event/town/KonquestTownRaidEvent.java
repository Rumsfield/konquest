package com.github.rumsfield.konquest.api.event.town;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;
import com.github.rumsfield.konquest.api.model.KonquestTown;

/**
 * Called after a town has been attacked, caused by a town raid alert.
 * <p>
 * This event occurs every time a town raid alert is sent to the town's kingdom members, while the town is under attack.
 * Raid alerts have cooldown times specified by the Konquest configuration.
 * A new event is invoked for every raid alert caused by enemy players entering a town, or breaking blocks in the town, while the town is under attack.
 * </p>
 *
 * <p>
 * This event will not be invoked when {@link KonquestTownAttackEvent KonquestTownAttackEvent} is cancelled.
 * </p>
 * @author Rumsfield
 *
 */
public class KonquestTownRaidEvent extends KonquestTownEvent {

    private final KonquestPlayer attacker;

    /**
     * Default constructor
     * @param konquest The API instance
     * @param town The town
     * @param attacker The attacking player that caused the raid alert
     */
    public KonquestTownRaidEvent(KonquestAPI konquest, KonquestTown town, KonquestPlayer attacker) {
        super(konquest, town);
        this.attacker = attacker;
    }

    /**
     * Gets the attacking player that caused the raid alert.
     *
     * @return The attacker
     */
    public KonquestPlayer getAttacker() {
        return attacker;
    }

}
