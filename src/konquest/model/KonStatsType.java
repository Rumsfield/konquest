package konquest.model;

import org.bukkit.Material;

import konquest.utility.MessagePath;

public enum KonStatsType {

	ENCHANTMENTS    (Material.ENCHANTING_TABLE, KonPrefixCategory.CLERGY,     2,     MessagePath.STAT_ENCHANTMENTS.getMessage(), MessagePath.STAT_ENCHANTMENTS_INFO.getMessage()),
	POTIONS         (Material.BREWING_STAND,    KonPrefixCategory.CLERGY,     1,     MessagePath.STAT_POTIONS.getMessage(),      MessagePath.STAT_POTIONS_INFO.getMessage()),
	MOBS            (Material.CREEPER_HEAD,     KonPrefixCategory.CLERGY,     1,     MessagePath.STAT_MOBS.getMessage(),         MessagePath.STAT_MOBS_INFO.getMessage()),
	SETTLED         (Material.OAK_DOOR,         KonPrefixCategory.NOBILITY,   5,     MessagePath.STAT_SETTLED.getMessage(),      MessagePath.STAT_SETTLED_INFO.getMessage()),
	CLAIMED         (Material.GRASS_BLOCK,      KonPrefixCategory.NOBILITY,   1,     MessagePath.STAT_CLAIMED.getMessage(),      MessagePath.STAT_CLAIMED_INFO.getMessage()),
	LORDS           (Material.PURPLE_CONCRETE,  KonPrefixCategory.NOBILITY,   1,     MessagePath.STAT_LORDS.getMessage(),        MessagePath.STAT_LORDS_INFO.getMessage()),
	KNIGHTS         (Material.BLUE_CONCRETE,    KonPrefixCategory.NOBILITY,   0.5,   MessagePath.STAT_KNIGHTS.getMessage(),      MessagePath.STAT_KNIGHTS_INFO.getMessage()),
	RESIDENTS       (Material.WHITE_CONCRETE,   KonPrefixCategory.NOBILITY,   0.1,   MessagePath.STAT_RESIDENTS.getMessage(),    MessagePath.STAT_RESIDENTS_INFO.getMessage()),
	INGOTS          (Material.IRON_INGOT,       KonPrefixCategory.TRADESMAN,  0.5,   MessagePath.STAT_INGOTS.getMessage(),       MessagePath.STAT_INGOTS_INFO.getMessage()),
	DIAMONDS        (Material.DIAMOND_ORE,      KonPrefixCategory.TRADESMAN,  1,     MessagePath.STAT_DIAMONDS.getMessage(),     MessagePath.STAT_DIAMONDS_INFO.getMessage()),
	CRAFTED         (Material.IRON_PICKAXE,     KonPrefixCategory.TRADESMAN,  0.1,   MessagePath.STAT_CRAFTED.getMessage(),      MessagePath.STAT_CRAFTED_INFO.getMessage()),
	FAVOR           (Material.GOLD_INGOT,       KonPrefixCategory.TRADESMAN,  0.01,  MessagePath.STAT_FAVOR.getMessage(),        MessagePath.STAT_FAVOR_INFO.getMessage()),
	KILLS           (Material.DIAMOND_SWORD,    KonPrefixCategory.MILITARY,   1,     MessagePath.STAT_KILLS.getMessage(),        MessagePath.STAT_KILLS_INFO.getMessage()),
	DAMAGE          (Material.CACTUS,           KonPrefixCategory.MILITARY,   0.01,  MessagePath.STAT_DAMAGE.getMessage(),       MessagePath.STAT_DAMAGE_INFO.getMessage()),
	GOLEMS          (Material.IRON_BLOCK,       KonPrefixCategory.MILITARY,   0.5,   MessagePath.STAT_GOLEMS.getMessage(),       MessagePath.STAT_GOLEMS_INFO.getMessage()),
	CAPTURES        (Material.ENDER_EYE,        KonPrefixCategory.MILITARY,   10,    MessagePath.STAT_CAPTURES.getMessage(),     MessagePath.STAT_CAPTURES_INFO.getMessage()),
	CRITICALS       (Material.OBSIDIAN,         KonPrefixCategory.MILITARY,   1,     MessagePath.STAT_CRITICALS.getMessage(),    MessagePath.STAT_CRITICALS_INFO.getMessage()),
	SEEDS           (Material.WHEAT_SEEDS,      KonPrefixCategory.FARMING,    0.1,   MessagePath.STAT_SEEDS.getMessage(),        MessagePath.STAT_SEEDS_INFO.getMessage()),
	BREED           (Material.CARROT,           KonPrefixCategory.FARMING,    0.5,   MessagePath.STAT_BREED.getMessage(),        MessagePath.STAT_BREED_INFO.getMessage()),
	TILL            (Material.IRON_HOE,         KonPrefixCategory.FARMING,    0.1,   MessagePath.STAT_TILL.getMessage(),         MessagePath.STAT_TILL_INFO.getMessage()),
	HARVEST         (Material.WHEAT,            KonPrefixCategory.FARMING,    0.25,  MessagePath.STAT_HARVEST.getMessage(),      MessagePath.STAT_HARVEST_INFO.getMessage()),
	FOOD            (Material.BEEF,             KonPrefixCategory.COOKING,    1,     MessagePath.STAT_FOOD.getMessage(),         MessagePath.STAT_FOOD_INFO.getMessage()),
	MUSIC           (Material.JUKEBOX,          KonPrefixCategory.JOKING,     1,     MessagePath.STAT_MUSIC.getMessage(),        MessagePath.STAT_MUSIC_INFO.getMessage()),
	EGG             (Material.EGG,              KonPrefixCategory.JOKING,     0.25,  MessagePath.STAT_EGG.getMessage(),          MessagePath.STAT_EGG_INFO.getMessage()),
	FISH            (Material.SALMON,           KonPrefixCategory.FISHING,    1,     MessagePath.STAT_FISH.getMessage(),         MessagePath.STAT_FISH_INFO.getMessage()),
	SPECIAL         (Material.NETHER_STAR,      KonPrefixCategory.ROYALTY,    1,     MessagePath.STAT_SPECIAL.getMessage(),      MessagePath.STAT_SPECIAL_INFO.getMessage());

	private final Material material;
	private final KonPrefixCategory category;
	private final double weight;
	private final String name;
	private final String description;
	
	KonStatsType(Material material, KonPrefixCategory category, double weight, String name, String description) {
		this.material = material;
		this.category = category;
		this.weight = weight;
		this.name = name;
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
	
	public String displayName() {
		return name;
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
