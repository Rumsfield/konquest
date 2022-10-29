package konquest.api.model;

import org.bukkit.Material;

import konquest.utility.MessagePath;

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
	ENEMY				(MessagePath.RELATIONSHIP_ENEMY.getMessage(), Material.TNT),
	/**
	 * Sanctioned kingdoms cannot trade and may allow pvp between members.
	 */
	SANCTIONED			(MessagePath.RELATIONSHIP_SANCTIONED.getMessage(), Material.SOUL_SAND),
	/**
	 * Peaceful kingdoms can trade and may allow pvp between members.
	 */
	PEACE				(MessagePath.RELATIONSHIP_PEACE.getMessage(), Material.QUARTZ_PILLAR),
	/**
	 * Allied kingdoms can travel to each others towns and prevent pvp.
	 */
	ALLIED				(MessagePath.RELATIONSHIP_ALLIED.getMessage(), Material.DIAMOND_BLOCK);
	
	private String label;
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
