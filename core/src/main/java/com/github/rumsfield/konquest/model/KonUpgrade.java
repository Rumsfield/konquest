package com.github.rumsfield.konquest.model;

import com.github.rumsfield.konquest.api.model.KonquestUpgrade;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Material;

/**
 * An upgrade for a town.
 * Upgrades have a cost in favor, and a population requirement.
 * Upgrades can have multiple levels.
 * 
 * @author Rumsfield
 *
 */
public enum KonUpgrade implements KonquestUpgrade {

	/**
	 * Monument Loot upgrade
	 */
	LOOT 		(3, Material.GOLD_INGOT, 				MessagePath.UPGRADE_LOOT_NAME.getMessage(), 	new String[] {MessagePath.UPGRADE_LOOT_LEVEL_1.getMessage(), MessagePath.UPGRADE_LOOT_LEVEL_2.getMessage(), MessagePath.UPGRADE_LOOT_LEVEL_3.getMessage()}),	/**
	 * Animal Drops upgrade
	 */
	DROPS		(1, Material.LEATHER, 					MessagePath.UPGRADE_DROPS_NAME.getMessage(), 	new String[] {MessagePath.UPGRADE_DROPS_LEVEL_1.getMessage()}),
	/**
	 * Enemy Fatigue upgrade
	 */
	FATIGUE		(1, Material.DIAMOND_PICKAXE, 			MessagePath.UPGRADE_FATIGUE_NAME.getMessage(), 	new String[] {MessagePath.UPGRADE_FATIGUE_LEVEL_1.getMessage()}),
	/**
	 * Counter-Intelligence upgrade
	 */
	COUNTER		(2, Material.COMPASS, 					MessagePath.UPGRADE_COUNTER_NAME.getMessage(), 	new String[] {MessagePath.UPGRADE_COUNTER_LEVEL_1.getMessage(), MessagePath.UPGRADE_COUNTER_LEVEL_2.getMessage()}),
	/**
	 * Health Buff upgrade
	 */
	HEALTH		(3, Material.ENCHANTED_GOLDEN_APPLE, 	MessagePath.UPGRADE_HEALTH_NAME.getMessage(), 	new String[] {MessagePath.UPGRADE_HEALTH_LEVEL_1.getMessage(), MessagePath.UPGRADE_HEALTH_LEVEL_2.getMessage(), MessagePath.UPGRADE_HEALTH_LEVEL_3.getMessage()}),
	/**
	 * Prevent Damage upgrade
	 */
	DAMAGE		(2, Material.TNT, 						MessagePath.UPGRADE_DAMAGE_NAME.getMessage(), 	new String[] {MessagePath.UPGRADE_DAMAGE_LEVEL_1.getMessage(), MessagePath.UPGRADE_DAMAGE_LEVEL_2.getMessage()}),
	/**
	 * Town Watch upgrade
	 */
	WATCH 		(3, Material.PLAYER_HEAD, 				MessagePath.UPGRADE_WATCH_NAME.getMessage(), 	new String[] {MessagePath.UPGRADE_WATCH_LEVEL_1.getMessage(), MessagePath.UPGRADE_WATCH_LEVEL_2.getMessage(), MessagePath.UPGRADE_WATCH_LEVEL_3.getMessage()}),
	/**
	 * Better Enchantments upgrade
	 */
	ENCHANT		(1, Material.ENCHANTED_BOOK, 			MessagePath.UPGRADE_ENCHANT_NAME.getMessage(), 	new String[] {MessagePath.UPGRADE_ENCHANT_LEVEL_1.getMessage()});

	private final int levels;
	private final Material icon;
	private final String name;
	private final String[] levelDescriptions;
	
	KonUpgrade(int levels, Material icon, String name, String[] levelDescriptions) {
		this.levels = levels;
		this.icon = icon;
		this.name = name;
		this.levelDescriptions = levelDescriptions;
	}
	
	/**
	 * Gets the maximum levels for this upgrade.
	 * 
	 * @return The maximum levels
	 */
	@Override
	public int getMaxLevel() {
		return levels;
	}
	
	/**
	 * Gets the material used for menu icons.
	 * 
	 * @return The material
	 */
	@Override
	public Material getIcon() {
		return icon;
	}
	
	/**
	 * Gets the name of this upgrade.
	 * 
	 * @return The name
	 */
	@Override
	public String getName() {
		return name;
	}
	
	/**
	 * Gets the level description, starting at 1. 
	 * Level 0 returns empty string.
	 * Levels higher than the max level returns an empty string.
	 * 
	 * @param level The level
	 * @return The level description
	 */
	@Override
	public String getLevelDescription(int level) {
		String desc = "";
		if(level > 0 && level <= levelDescriptions.length) {
			desc = levelDescriptions[level-1];
		}
		return desc;
	}
	
	/**
	 * Get the corresponding KonquestUpgrade enum that matches the given name.
	 * Returns null when the name does not match any enum.
	 * 
	 * @param name The name of the enum
	 * @return The KonquestUpgrade enum that matches name
	 */
	public static KonUpgrade getUpgrade(String name) {
		KonUpgrade result = null;
		for(KonUpgrade upgrade : KonUpgrade.values()) {
			if(upgrade.toString().equalsIgnoreCase(name)) {
				result = upgrade;
			}
		}
		return result;
	}
	
}
