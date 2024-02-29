package com.github.rumsfield.konquest.utility;

public enum CustomCommandPath {

    RUIN_CRITICAL              ("commands.ruin_critical"),
    RUIN_CAPTURE               ("commands.ruin_capture"),
    RUIN_LOOT_OPEN             ("commands.ruin_loot_open"),
    RUIN_GOLEM_KILL            ("commands.ruin_golem_kill"),
    TOWN_MONUMENT_CRITICAL     ("commands.town_monument_critical"),
    TOWN_MONUMENT_CAPTURE      ("commands.town_monument_capture"),
    TOWN_MONUMENT_LOOT_OPEN    ("commands.town_monument_loot_open");

    private final String path;
    CustomCommandPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
