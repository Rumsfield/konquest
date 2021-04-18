package konquest.model;

import org.bukkit.Material;

public enum KonStatsType {

	ENCHANTMENTS		(Material.ENCHANTING_TABLE, KonPrefixCategory.CLERGY, 		2, 		"Enchantments applied to items"),
	POTIONS				(Material.BREWING_STAND, 	KonPrefixCategory.CLERGY, 		1, 		"Potions used or thrown"),
	MOBS				(Material.CREEPER_HEAD,		KonPrefixCategory.CLERGY, 		1, 		"Hostile mobs killed"),
	SETTLED				(Material.OAK_DOOR,			KonPrefixCategory.NOBILITY, 	5, 		"Towns settled"),
	CLAIMED				(Material.GRASS_BLOCK,		KonPrefixCategory.NOBILITY, 	1, 		"Land claimed"),
	LORDS				(Material.PURPLE_CONCRETE,	KonPrefixCategory.NOBILITY, 	1, 		"Town lordships"),
	KNIGHTS				(Material.BLUE_CONCRETE,	KonPrefixCategory.NOBILITY, 	0.5, 	"Town knighthoods"),
	RESIDENTS			(Material.WHITE_CONCRETE,	KonPrefixCategory.NOBILITY, 	0.1, 	"Town residencies"),
	INGOTS				(Material.IRON_INGOT,		KonPrefixCategory.TRADESMAN, 	0.5, 	"Ingots smelted"),
	DIAMONDS			(Material.DIAMOND_ORE,		KonPrefixCategory.TRADESMAN, 	1, 		"Diamond ores mined"),
	CRAFTED				(Material.IRON_PICKAXE,	 	KonPrefixCategory.TRADESMAN, 	0.1, 	"Tools, weapons and armor crafting skill"),
	FAVOR				(Material.GOLD_NUGGET,		KonPrefixCategory.TRADESMAN, 	0.01, 	"Favor spent"),
	KILLS				(Material.DIAMOND_SWORD,	KonPrefixCategory.MILITARY, 	1, 		"Players killed"),
	DAMAGE				(Material.CACTUS,			KonPrefixCategory.MILITARY, 	0.01, 	"Player damage given"),
	GOLEMS				(Material.IRON_BLOCK,		KonPrefixCategory.MILITARY, 	0.5, 	"Enemy Iron Golems killed"),
	CAPTURES			(Material.ENDER_EYE,		KonPrefixCategory.MILITARY, 	10, 	"Enemy towns captured"),
	CRITICALS			(Material.OBSIDIAN,			KonPrefixCategory.MILITARY, 	1, 		"Enemy town critical hits"),
	SEEDS				(Material.WHEAT_SEEDS,		KonPrefixCategory.FARMING, 		0.1, 	"Seeds planted"),
	BREED				(Material.CARROT,			KonPrefixCategory.FARMING, 		0.5, 	"Mobs romanced"),
	TILL				(Material.IRON_HOE,			KonPrefixCategory.FARMING, 		0.1, 	"Farm land fertalized"),
	HARVEST				(Material.WHEAT,			KonPrefixCategory.FARMING, 		0.25,   "Crops harvested"),
	FOOD				(Material.BEEF,				KonPrefixCategory.COOKING, 		1, 		"Food items made"),
	MUSIC				(Material.JUKEBOX,			KonPrefixCategory.JOKING, 		1, 		"Music discs played"),
	EGG					(Material.EGG,				KonPrefixCategory.JOKING, 		0.25, 	"Players egged"),
	FISH				(Material.SALMON,			KonPrefixCategory.FISHING, 		1, 		"Fishing skill"),
	SPECIAL				(Material.NETHER_STAR,		KonPrefixCategory.ROYALTY, 		1, 		"Special points");

	private final Material material;
	private final KonPrefixCategory category;
	private final double weight;
	private final String description;
	
	KonStatsType(Material material, KonPrefixCategory category, double weight, String description) {
		this.material = material;
		this.category = category;
		this.weight = weight;
		this.description = description;
	}
	
	public Material getMaterial() {
		return material;
	}
	
	public KonPrefixCategory getCategory() {
		return category;
	}
	
	public double weight() {
		return weight;
	}
	
	public String description(){
		return description;
	}
	
	/**
	 * Gets a KonStatsType enum given a string command.
	 * @param name - The string name of the KonStatsType
	 * @return KonStatsType - Corresponding enum
	 */
	public static KonStatsType getStat(String name) {
		KonStatsType result = null;
		for(KonStatsType stat : KonStatsType.values()) {
			if(stat.toString().equalsIgnoreCase(name)) {
				result = stat;
			}
		}
		return result;
	}
	
}
