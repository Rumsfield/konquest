package com.github.rumsfield.konquest.api.model;

import org.bukkit.Material;

/**
 * The types of kingdom relationships in Konquest.
 * 
 * @author Rumsfield
 *
 */
public enum KonquestRelationship {

	/**
	 * Enemy kingdoms are at war and can attack each other.
	 */
	ENEMY				("relationship.enemy", Material.TNT),
	/**
	 * Sanctioned kingdoms cannot trade and may allow pvp between members.
	 */
	SANCTIONED			("relationship.sanctioned", Material.SOUL_SAND),
	/**
	 * Peaceful kingdoms can trade and may allow pvp between members.
	 */
	PEACE				("relationship.peace", Material.QUARTZ_PILLAR),
	/**
	 * Allied kingdoms can travel to each other's towns and prevent pvp.
	 */
	ALLIED				("relationship.allied", Material.DIAMOND_BLOCK);
	
	private final String label;
	private final Material icon;
	
	KonquestRelationship(String label, Material icon) {
		this.label = label;
		this.icon = icon;
	}

	/**
	 * Gets the display label from the Konquest language files
	 * 
	 * @return The label
	 */
	public String getLabel() {
		return label;
	}
	
	/**
	 * Gets the display icon material used in GUI menus
	 * 
	 * @return The icon material
	 */
	public Material getIcon() {
		return icon;
	}
}
