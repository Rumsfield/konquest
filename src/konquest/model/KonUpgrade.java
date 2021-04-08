package konquest.model;

import org.bukkit.Material;
import org.bukkit.ChatColor;

public enum KonUpgrade {

	LOOT 		(3, Material.GOLD_INGOT, "Monument Loot", new String[] {"Adds 1 more loot item per chest", "Adds 2 more loot items per chest", "Adds 3 more loot items per chest"}),
	DROPS		(1, Material.LEATHER, "Animal Drops", new String[] {"Animal mobs drop 1 more item"}),
	FATIGUE		(1, Material.DIAMOND_PICKAXE, "Enemy Fatigue", new String[] {"Enemies get mining fatigue 2"}),
	COUNTER		(2, Material.COMPASS, "Counter-Intelligence", new String[] {"Prevent spy maps from locating this town", "Prevent compasses from pointing to this town"}),
	HEALTH		(3, Material.ENCHANTED_GOLDEN_APPLE, "Health Buff", new String[] {"Adds 1 heart to friendly health bars", "Adds 2 hearts to friendly health bars", "Adds 3 hearts to friendly health bars"}),
	DAMAGE		(2, Material.TNT, "Prevent Damage", new String[] {"Prevents all fire spread", "Prevents all explosions"}),
	WATCH 		(3, Material.PLAYER_HEAD, "Town Watch", new String[] {"Prevents enemy raids unless at least 1 resident is online", "Prevents enemy raids unless at least 2 residents are online", "Prevents enemy raids unless at least 3 residents are online"}),
	ENCHANT		(1, Material.ENCHANTED_BOOK, "Better Enchantments", new String[] {"Raises enchantment offers by 1 level"}),
	OLD			(1, Material.NETHER_STAR, ChatColor.MAGIC+"Old One", new String[] {"Summon The Old One"});
	
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
