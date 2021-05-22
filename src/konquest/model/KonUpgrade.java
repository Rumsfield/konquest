package konquest.model;

import org.bukkit.Material;

import konquest.utility.MessagePath;

public enum KonUpgrade {

	LOOT 		(3, Material.GOLD_INGOT, 				MessagePath.UPGRADE_LOOT_NAME.getMessage(), 	new String[] {MessagePath.UPGRADE_LOOT_LEVEL_1.getMessage(), MessagePath.UPGRADE_LOOT_LEVEL_2.getMessage(), MessagePath.UPGRADE_LOOT_LEVEL_3.getMessage()}),
	DROPS		(1, Material.LEATHER, 					MessagePath.UPGRADE_DROPS_NAME.getMessage(), 	new String[] {MessagePath.UPGRADE_DROPS_LEVEL_1.getMessage()}),
	FATIGUE		(1, Material.DIAMOND_PICKAXE, 			MessagePath.UPGRADE_FATIGUE_NAME.getMessage(), 	new String[] {MessagePath.UPGRADE_FATIGUE_LEVEL_1.getMessage()}),
	COUNTER		(2, Material.COMPASS, 					MessagePath.UPGRADE_COUNTER_NAME.getMessage(), 	new String[] {MessagePath.UPGRADE_COUNTER_LEVEL_1.getMessage(), MessagePath.UPGRADE_COUNTER_LEVEL_2.getMessage()}),
	HEALTH		(3, Material.ENCHANTED_GOLDEN_APPLE, 	MessagePath.UPGRADE_HEALTH_NAME.getMessage(), 	new String[] {MessagePath.UPGRADE_HEALTH_LEVEL_1.getMessage(), MessagePath.UPGRADE_HEALTH_LEVEL_2.getMessage(), MessagePath.UPGRADE_HEALTH_LEVEL_3.getMessage()}),
	DAMAGE		(2, Material.TNT, 						MessagePath.UPGRADE_DAMAGE_NAME.getMessage(), 	new String[] {MessagePath.UPGRADE_DAMAGE_LEVEL_1.getMessage(), MessagePath.UPGRADE_DAMAGE_LEVEL_2.getMessage()}),
	WATCH 		(3, Material.PLAYER_HEAD, 				MessagePath.UPGRADE_WATCH_NAME.getMessage(), 	new String[] {MessagePath.UPGRADE_WATCH_LEVEL_1.getMessage(), MessagePath.UPGRADE_WATCH_LEVEL_2.getMessage(), MessagePath.UPGRADE_WATCH_LEVEL_3.getMessage()}),
	ENCHANT		(1, Material.ENCHANTED_BOOK, 			MessagePath.UPGRADE_ENCHANT_NAME.getMessage(), 	new String[] {MessagePath.UPGRADE_ENCHANT_LEVEL_1.getMessage()});
	//OLD			(1, Material.NETHER_STAR, ChatColor.MAGIC+"Old One", new String[] {"Summon The Old One"});
	
	private final int levels;
	private final Material icon;
	private final String description;
	private final String[] levelDescriptions;
	KonUpgrade(int levels, Material icon, String description, String[] levelDescriptions) {
		this.levels = levels;
		this.icon = icon;
		this.description = description;
		this.levelDescriptions = levelDescriptions;
	}
	
	public int getMaxLevel() {
		return levels;
	}
	
	public Material getIcon() {
		return icon;
	}
	
	public String getDescription() {
		return description;
	}
	
	/**
	 * Gets level description, starting at 1. Level 0 returns empty string.
	 * @param level
	 * @return
	 */
	public String getLevelDescription(int level) {
		String desc = "";
		if(level > 0 && level <= levelDescriptions.length) {
			desc = levelDescriptions[level-1];
		}
		return desc;
	}
	
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
