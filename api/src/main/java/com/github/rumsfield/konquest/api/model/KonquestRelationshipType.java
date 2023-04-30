package com.github.rumsfield.konquest.api.model;

/**
 * The types of player relationships in Konquest.
 *
 * @author Rumsfield
 *
 */
public enum KonquestRelationshipType {

    /**
     * Player/territory is barbarian
     */
    BARBARIAN,

    /**
     * Territory has no kingdom (ruin, sanctuary, etc)
     */
    NEUTRAL,

    /**
     * Player/territory are in the same kingdom
     */
    FRIENDLY,

    /**
     * Player/territory are in kingdoms at war
     */
    ENEMY,

    /**
     * Player/territory are in kingdoms at peace
     */
    PEACEFUL,

    /**
     * Player/territory are in trading kingdoms
     */
    TRADE,

    /**
     * Player/territory are in allied kingdoms
     */
    ALLY
}
