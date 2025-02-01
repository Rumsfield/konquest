package com.github.rumsfield.konquest.utility;

import com.github.rumsfield.konquest.api.model.KonquestDiplomacyType;
import com.github.rumsfield.konquest.api.model.KonquestRelationshipType;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;

/**
 * Provides static methods for looking up MessagePath labels from API enums
 */
public class Labeler {

    public static String lookup(KonquestDiplomacyType type) {
        switch(type) {
            case WAR:
                return MessagePath.DIPLOMACY_WAR.getMessage();
            case PEACE:
                return MessagePath.DIPLOMACY_PEACE.getMessage();
            case TRADE:
                return MessagePath.DIPLOMACY_TRADE.getMessage();
            case ALLIANCE:
                return MessagePath.DIPLOMACY_ALLIANCE.getMessage();
            default:
                break;
        }
        return "";
    }

    public static String lookup(KonquestTerritoryType type) {
        switch (type) {
            case WILD:
                return MessagePath.TERRITORY_WILD.getMessage();
            case CAPITAL:
                return MessagePath.TERRITORY_CAPITAL.getMessage();
            case TOWN:
                return MessagePath.TERRITORY_TOWN.getMessage();
            case CAMP:
                return MessagePath.TERRITORY_CAMP.getMessage();
            case RUIN:
                return MessagePath.TERRITORY_RUIN.getMessage();
            case SANCTUARY:
                return MessagePath.TERRITORY_SANCTUARY.getMessage();
            default:
                break;
        }
        return "";
    }

    public static String lookup(KonquestRelationshipType type) {
        switch(type) {
            case BARBARIAN:
                return MessagePath.RELATIONSHIP_BARBARIAN.getMessage();
            case ENEMY:
                return MessagePath.RELATIONSHIP_ENEMY.getMessage();
            case FRIENDLY:
                return MessagePath.RELATIONSHIP_FRIENDLY.getMessage();
            case ALLY:
                return MessagePath.RELATIONSHIP_ALLY.getMessage();
            case TRADE:
                return MessagePath.RELATIONSHIP_TRADER.getMessage();
            case PEACEFUL:
                return MessagePath.RELATIONSHIP_PEACEFUL.getMessage();
            default:
                break;
        }
        return "";
    }

}
