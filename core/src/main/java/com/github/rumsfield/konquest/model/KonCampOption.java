package com.github.rumsfield.konquest.model;

import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Material;

public enum KonCampOption {

    PEACEFUL     (false, MessagePath.LABEL_PEACEFUL.getMessage(),  MessagePath.MENU_OPTIONS_PEACEFUL.getMessage(),  Material.FEATHER),
    TRAVEL       (true,  MessagePath.LABEL_TRAVEL.getMessage(),    MessagePath.MENU_OPTIONS_TRAVEL.getMessage(),    Material.LEATHER_BOOTS),
    BUILD        (true,  MessagePath.LABEL_BUILD.getMessage(),     MessagePath.MENU_OPTIONS_BUILD.getMessage(),     Material.STONE_PICKAXE),
    CHEST        (false, MessagePath.LABEL_CHEST.getMessage(),     MessagePath.MENU_OPTIONS_CHEST.getMessage(),     Material.CHEST),
    MOBS         (false, MessagePath.LABEL_MOBS.getMessage(),      MessagePath.MENU_OPTIONS_MOBS.getMessage(),      Material.CREEPER_HEAD);

    private final boolean defaultValue;
    private final String description;
    private final String name;
    private final Material displayMaterial;

    KonCampOption(boolean defaultValue, String name, String description, Material displayMaterial) {
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
     * @return KonCampOption - Corresponding enum
     */
    public static KonCampOption getOption(String name) {
        for(KonCampOption option : KonCampOption.values()) {
            if(option.toString().equalsIgnoreCase(name)) {
                return option;
            }
        }
        // Matching name not found
        return null;
    }

}
