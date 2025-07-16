package com.github.rumsfield.konquest.model;

public enum KonGlobalEventEffect {

    // Effects in the same group cannot be active at the same time.
    // Priority (higher) indicates which effect is used when multiple from the same group are active.

    ALL_PVP                     (10, 0, "title",       "description"),
    DIPLOMACY_WAR               (20, 0, "title",       "description"),
    DIPLOMACY_PEACE             (20, 1, "title",       "description"),
    DISABLE_SHIELDS             (30, 0, "title",       "description"),
    DISABLE_ARMOR               (31, 0, "title",       "description"),
    DISABLE_PROTECTION          (32, 0, "title",       "description"),
    DISABLE_IMMUNITY            (33, 0, "title",       "description"),
    EXP_BOOST_50                (40, 0, "title",       "description"),
    EXP_BOOST_100               (40, 1, "title",       "description"),
    EXP_BOOST_200               (40, 2, "title",       "description"),
    FAVOR_DISCOUNT_25           (50, 0, "title",       "description"),
    FAVOR_DISCOUNT_50           (50, 1, "title",       "description"),
    FAVOR_DISCOUNT_75           (50, 2, "title",       "description"),
    FAVOR_DISCOUNT_100          (50, 3, "title",       "description"),
    FREE_TRAVEl                 (60, 0, "title",       "description"),
    FREE_SHIELDS                (61, 0, "title",       "description"),
    MONUMENT_LOOT_X2            (70, 0, "title",       "description"),
    MONUMENT_LOOT_X5            (70, 1, "title",       "description"),
    MONUMENT_LOOT_X10           (70, 2, "title",       "description"),
    RUIN_LOOT_X2                (80, 0, "title",       "description"),
    RUIN_LOOT_X5                (80, 1, "title",       "description"),
    RUIN_LOOT_X10               (80, 2, "title",       "description");

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


