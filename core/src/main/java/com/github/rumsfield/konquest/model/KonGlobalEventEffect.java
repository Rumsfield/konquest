package com.github.rumsfield.konquest.model;

import com.github.rumsfield.konquest.utility.MessagePath;

public enum KonGlobalEventEffect {

    // Effects in the same group cannot be active at the same time.
    // Priority (higher) indicates which effect is used when multiple from the same group are active.

    ALL_PVP                     (10, 0, MessagePath.EFFECTS_PVP_NAME.getMessage(),           MessagePath.EFFECTS_PVP.getMessage()),
    ALL_WAR                     (20, 0, MessagePath.EFFECTS_WAR_NAME.getMessage(),           MessagePath.EFFECTS_WAR.getMessage()),
    ALL_PEACE                   (20, 1, MessagePath.EFFECTS_PEACE_NAME.getMessage(),         MessagePath.EFFECTS_PEACE.getMessage()),
    FREE_TRAVEL                 (30, 0, MessagePath.EFFECTS_TRAVEL_NAME.getMessage(),        MessagePath.EFFECTS_TRAVEL.getMessage()),
    FREE_SHIELDS                (40, 0, MessagePath.EFFECTS_SHIELDS_NAME.getMessage(),       MessagePath.EFFECTS_SHIELDS.getMessage()),
    NO_SHIELDS                  (40, 1, MessagePath.EFFECTS_NO_SHIELDS_NAME.getMessage(),    MessagePath.EFFECTS_NO_SHIELDS.getMessage()),
    NO_ARMOR                    (41, 0, MessagePath.EFFECTS_NO_ARMOR_NAME.getMessage(),      MessagePath.EFFECTS_NO_ARMOR.getMessage()),
    NO_PROTECTION               (42, 0, MessagePath.EFFECTS_NO_PROTECTION_NAME.getMessage(), MessagePath.EFFECTS_NO_PROTECTION.getMessage()),
    NO_IMMUNITY                 (43, 0, MessagePath.EFFECTS_NO_IMMUNITY_NAME.getMessage(),   MessagePath.EFFECTS_NO_IMMUNITY.getMessage()),
    FAVOR_1                     (50, 0, MessagePath.EFFECTS_FAVOR_1_NAME.getMessage(),       MessagePath.EFFECTS_FAVOR_1.getMessage()),
    FAVOR_2                     (50, 1, MessagePath.EFFECTS_FAVOR_2_NAME.getMessage(),       MessagePath.EFFECTS_FAVOR_2.getMessage()),
    FAVOR_3                     (50, 2, MessagePath.EFFECTS_FAVOR_3_NAME.getMessage(),       MessagePath.EFFECTS_FAVOR_3.getMessage()),
    MONUMENT_LOOT_1             (60, 0, MessagePath.EFFECTS_MONUMENT_1_NAME.getMessage(),    MessagePath.EFFECTS_MONUMENT_1.getMessage()),
    MONUMENT_LOOT_2             (60, 1, MessagePath.EFFECTS_MONUMENT_2_NAME.getMessage(),    MessagePath.EFFECTS_MONUMENT_2.getMessage()),
    MONUMENT_LOOT_3             (60, 2, MessagePath.EFFECTS_MONUMENT_3_NAME.getMessage(),    MessagePath.EFFECTS_MONUMENT_3.getMessage()),
    RUIN_LOOT_1                 (70, 0, MessagePath.EFFECTS_RUIN_1_NAME.getMessage(),        MessagePath.EFFECTS_RUIN_1.getMessage()),
    RUIN_LOOT_2                 (70, 1, MessagePath.EFFECTS_RUIN_2_NAME.getMessage(),        MessagePath.EFFECTS_RUIN_2.getMessage()),
    RUIN_LOOT_3                 (70, 2, MessagePath.EFFECTS_RUIN_3_NAME.getMessage(),        MessagePath.EFFECTS_RUIN_3.getMessage()),
    EXP_BOOST_1                 (80, 0, MessagePath.EFFECTS_EXP_1_NAME.getMessage(),         MessagePath.EFFECTS_EXP_1.getMessage()),
    EXP_BOOST_2                 (80, 1, MessagePath.EFFECTS_EXP_2_NAME.getMessage(),         MessagePath.EFFECTS_EXP_2.getMessage()),
    EXP_BOOST_3                 (80, 2, MessagePath.EFFECTS_EXP_3_NAME.getMessage(),         MessagePath.EFFECTS_EXP_3.getMessage());

    private final int group;
    private final int priority;
    private final String title;
    private final String description;
    KonGlobalEventEffect(int group, int priority, String title, String description) {
        this.group = group;
        this.priority = priority;
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    // True if the other effect is in the same group as this one
    public boolean isGroupConflict(KonGlobalEventEffect otherEffect) {
        return this.group == otherEffect.group;
    }

    // True if the other effect is in the same group and a higher priority than this one
    public boolean isHigherPriority(KonGlobalEventEffect otherEffect) {
        return this.group == otherEffect.group && this.priority < otherEffect.priority;
    }

    /**
     * Gets a KonGlobalEventEffect enum given a string name.
     * @param name - The string name of the KonGlobalEventEffect
     * @return KonGlobalEventEffect - Corresponding enum, else null
     */
    public static KonGlobalEventEffect getEffect(String name) {
        KonGlobalEventEffect result = null;
        for(KonGlobalEventEffect effect : KonGlobalEventEffect.values()) {
            if(effect.toString().equalsIgnoreCase(name)) {
                result = effect;
            }
        }
        return result;
    }

}


