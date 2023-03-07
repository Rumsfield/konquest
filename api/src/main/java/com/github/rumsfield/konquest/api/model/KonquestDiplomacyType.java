package com.github.rumsfield.konquest.api.model;

import org.bukkit.Material;

/**
 * The types of kingdom diplomatic relationships in Konquest.
 *
 * @author Rumsfield
 *
 */
public enum KonquestDiplomacyType {

    /**
     * Enemy kingdoms are at war and can attack each other.
     */
    ENEMY				(Material.TNT),
    /**
     * Sanctioned kingdoms cannot trade and may allow pvp between members.
     */
    SANCTIONED			(Material.SOUL_SAND),
    /**
     * Peaceful kingdoms can trade and may allow pvp between members.
     */
    PEACE				(Material.QUARTZ_PILLAR),
    /**
     * Allied kingdoms can travel to each other's towns and prevent pvp.
     */
    ALLIED				(Material.DIAMOND_BLOCK);

    private final Material icon;

    KonquestDiplomacyType(Material icon) {
        this.icon = icon;
    }

    /**
     * Gets the display icon material used in GUI menus
     *
     * @return The icon material
     */
    public Material getIcon() {
        return icon;
    }

    /**
     * Gets the default diplomatic state
     *
     * @return The default state
     */
    public static KonquestDiplomacyType getDefault() {
        return PEACE;
    }

}
