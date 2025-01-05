package com.github.rumsfield.konquest.api.model;

import org.bukkit.Material;

/**
 * An upgrade for a town.
 * Upgrades have a cost in favor, and a population requirement.
 * Upgrades can have multiple levels.
 *
 * @author TropicalShadow
 *
 */
public interface KonquestUpgrade {

    /**
     * Gets the maximum levels for this upgrade.
     *
     * @return The maximum levels
     */
    int getMaxLevel();

    /**
     * Gets the material used for menu icons.
     *
     * @return The material
     */
    Material getIcon();

    /**
     * Gets the name of this upgrade.
     *
     * @return The name
     */
    String getName();

    /**
     * Gets the level description, starting at 1.
     * Level 0 returns empty string.
     * Levels higher than the max level returns an empty string.
     *
     * @param level The level
     * @return The level description
     */
    String getLevelDescription(int level);

}
