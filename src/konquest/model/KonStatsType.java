package konquest.model;

public enum KonStatsType {

	ENCHANTMENTS		(0, KonPrefixCategory.CLERGY, 		2, 		"Enchantments applied to items"),
	POTIONS				(0, KonPrefixCategory.CLERGY, 		1, 		"Potions used or thrown"),
	MOBS				(0,	KonPrefixCategory.CLERGY, 		1, 		"Hostile mobs killed"),
	SETTLED				(0,	KonPrefixCategory.NOBILITY, 	5, 		"Towns settled"),
	CLAIMED				(0,	KonPrefixCategory.NOBILITY, 	1, 		"Land claimed"),
	LORDS				(0,	KonPrefixCategory.NOBILITY, 	1, 		"Town lordships"),
	KNIGHTS				(0,	KonPrefixCategory.NOBILITY, 	0.5, 	"Town knighthoods"),
	RESIDENTS			(0,	KonPrefixCategory.NOBILITY, 	0.1, 	"Town residencies"),
	INGOTS				(0,	KonPrefixCategory.TRADESMAN, 	0.5, 	"Ingots smelted"),
	DIAMONDS			(0,	KonPrefixCategory.TRADESMAN, 	1, 		"Diamond ores mined"),
	CRAFTED				(0,	KonPrefixCategory.TRADESMAN, 	0.1, 	"Tools, weapons and armor crafting skill"),
	FAVOR				(0,	KonPrefixCategory.TRADESMAN, 	0.01, 	"Favor spent"),
	KILLS				(0,	KonPrefixCategory.MILITARY, 	1, 		"Players killed"),
	DAMAGE				(0,	KonPrefixCategory.MILITARY, 	0.01, 	"Player damage given"),
	GOLEMS				(0,	KonPrefixCategory.MILITARY, 	0.5, 	"Enemy Iron Golems killed"),
	CAPTURES			(0,	KonPrefixCategory.MILITARY, 	10, 	"Enemy towns captured"),
	CRITICALS			(0,	KonPrefixCategory.MILITARY, 	1, 		"Enemy town critical hits"),
	SEEDS				(0,	KonPrefixCategory.FARMING, 		0.1, 	"Seeds planted"),
	BREED				(0,	KonPrefixCategory.FARMING, 		0.5, 	"Mobs romanced"),
	TILL				(0,	KonPrefixCategory.FARMING, 		0.1, 	"Farm land fertalized"),
	HARVEST				(0,	KonPrefixCategory.FARMING, 		0.25,   "Crops harvested"),
	FOOD				(0,	KonPrefixCategory.COOKING, 		1, 		"Food items made"),
	MUSIC				(0,	KonPrefixCategory.JOKING, 		1, 		"Music discs played"),
	EGG					(0,	KonPrefixCategory.JOKING, 		0.25, 	"Players egged"),
	FISH				(0,	KonPrefixCategory.FISHING, 		1, 		"Fishing skill"),
	SPECIAL				(0,	KonPrefixCategory.ROYALTY, 		1, 		"Special points");

	private final int defaultValue;
	private final KonPrefixCategory category;
	private final double weight;
	private final String description;
	
	KonStatsType(int defaultValue, KonPrefixCategory category, double weight, String description) {
		this.defaultValue = defaultValue;
		this.category = category;
		this.weight = weight;
		this.description = description;
	}
	
	public int defaultValue() {
		return defaultValue;
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
