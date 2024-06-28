package com.github.rumsfield.konquest.model;

import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Material;

public enum KonTownOption {

    OPEN                    (false, MessagePath.LABEL_OPEN.getMessage(),                MessagePath.MENU_OPTIONS_OPEN.getMessage(),                 Material.DARK_OAK_DOOR),
    ALLIED_BUILDING         (true,  MessagePath.LABEL_ALLIED_BUILDING.getMessage(),     MessagePath.MENU_OPTIONS_ALLIED_BUILDING.getMessage(),      Material.BRICK_STAIRS),
    PLOTS_ONLY              (false, MessagePath.LABEL_PLOT.getMessage(),                MessagePath.MENU_OPTIONS_PLOT.getMessage(),                 Material.DIAMOND_SHOVEL),
    FRIENDLY_REDSTONE       (true,  MessagePath.LABEL_FRIENDLY_REDSTONE.getMessage(),   MessagePath.MENU_OPTIONS_FRIENDLY_REDSTONE.getMessage(),    Material.LEVER),
    ENEMY_REDSTONE          (false, MessagePath.LABEL_ENEMY_REDSTONE.getMessage(),      MessagePath.MENU_OPTIONS_REDSTONE.getMessage(),             Material.REDSTONE),
    GOLEM_OFFENSE           (false, MessagePath.LABEL_GOLEM_OFFENSE.getMessage(),       MessagePath.MENU_OPTIONS_GOLEM.getMessage(),                Material.IRON_SWORD);

    private final boolean defaultValue;
    private final String description;
    private final String name;
    private final Material displayMaterial;

    KonTownOption(boolean defaultValue, String name, String description, Material displayMaterial) {
        this.defaultValue = defaultValue;
        this.name = name;
        this.description = description;
        this.displayMaterial = displayMaterial;
    }

    public boolean getDefaultValue() {
        return defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Material getDisplayMaterial() {
        return displayMaterial;
    }

    /**
     * Gets an enum given a string name
     * @param name - The string name of the option
     * @return KonTownOption - Corresponding enum
     */
    public static KonTownOption getOption(String name) {
        for(KonTownOption option : KonTownOption.values()) {
            if(option.toString().equalsIgnoreCase(name)) {
                return option;
            }
        }
        // Matching name not found
        return null;
    }
}
