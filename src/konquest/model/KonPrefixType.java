package konquest.model;

public enum KonPrefixType {

	// Royalty - special cases
	KING		(KonPrefixCategory.ROYALTY, 20, "King"),
	QUEEN		(KonPrefixCategory.ROYALTY, 20, "Queen"),
	PRINCE		(KonPrefixCategory.ROYALTY, 10, "Prince"),
	PRINCESS	(KonPrefixCategory.ROYALTY, 10, "Princess"),
	NOBLE		(KonPrefixCategory.ROYALTY, 5, "Noble"),
	// Clergy - enchanting and potions accomplishments
	CARDINAL	(KonPrefixCategory.CLERGY, 1000, "Cardinal"),
	BISHOP		(KonPrefixCategory.CLERGY, 750, "Bishop"),
	PRIEST		(KonPrefixCategory.CLERGY, 500, "Priest"),
	CLERIC		(KonPrefixCategory.CLERGY, 300, "Cleric"),
	CHAPLAIN	(KonPrefixCategory.CLERGY, 200, "Chaplain"),
	SCRIBE		(KonPrefixCategory.CLERGY, 100, "Scribe"),
	// Nobility - Settlement and claiming accomplishments
	VICEROY		(KonPrefixCategory.NOBILITY, 300, "Viceroy"),
	DUKE		(KonPrefixCategory.NOBILITY, 150, "Duke"),
	COUNT		(KonPrefixCategory.NOBILITY, 100, "Count"),
	EARL		(KonPrefixCategory.NOBILITY, 75, "Earl"),
	BARON		(KonPrefixCategory.NOBILITY, 50, "Baron"),
	LORD		(KonPrefixCategory.NOBILITY, 30, "Lord"),
	VASSAL		(KonPrefixCategory.NOBILITY, 15, "Vassal"),
	PEASANT		(KonPrefixCategory.NOBILITY, 0, "Peasant"),
	// Tradesmen - Favor and precious metals accomplishments
	MASTER		(KonPrefixCategory.TRADESMAN, 5000, "Master"),
	ARTISAN		(KonPrefixCategory.TRADESMAN, 2000, "Artisan"),
	JOURNEYMAN	(KonPrefixCategory.TRADESMAN, 1000, "Journeyman"),
	CRAFTSMAN	(KonPrefixCategory.TRADESMAN, 500, "Craftsman"),
	MERCHANT	(KonPrefixCategory.TRADESMAN, 250, "Merchant"),
	PEDDLER		(KonPrefixCategory.TRADESMAN, 100, "Peddler"),
	// Military - pvp accomplishments
	GENERAL		(KonPrefixCategory.MILITARY, 800, "General"),
	MAJOR		(KonPrefixCategory.MILITARY, 400, "Major"),
	CAPTAIN		(KonPrefixCategory.MILITARY, 200, "Captain"),
	SERGEANT	(KonPrefixCategory.MILITARY, 100, "Sergeant"),
	KNIGHT		(KonPrefixCategory.MILITARY, 50, "Sir"),
	SQUIRE		(KonPrefixCategory.MILITARY, 10, "Squire"),
	// Farming - farming accomplishments
	COWBOY		(KonPrefixCategory.FARMING, 1500, "Cowboy"),
	BREEDER		(KonPrefixCategory.FARMING, 1000, "Breeder"),
	RANCHER		(KonPrefixCategory.FARMING, 600, "Rancher"),
	FARMER		(KonPrefixCategory.FARMING, 300, "Farmer"),
	PICKER		(KonPrefixCategory.FARMING, 150, "Picker"),
	SPROUT		(KonPrefixCategory.FARMING, 50, "Sprout"),
	// Misc - miscellaneous accomplishments
	JESTER		(KonPrefixCategory.JOKING, 100, "Jester"),
	FOOL		(KonPrefixCategory.JOKING, 69, "Fool"),
	DANCER		(KonPrefixCategory.JOKING, 50, "Dancer"),
	BARD		(KonPrefixCategory.JOKING, 20, "Bard"),
	FISHERMAN	(KonPrefixCategory.FISHING, 200, "Fisherman"),
	CASTER		(KonPrefixCategory.FISHING, 100, "Caster"),
	CHUM		(KonPrefixCategory.FISHING, 20, "Chum"),
	CHEF		(KonPrefixCategory.COOKING, 200, "Chef"),
	COOK		(KonPrefixCategory.COOKING, 100, "Cook"),
	BAKER		(KonPrefixCategory.COOKING, 20, "Baker");
	
	
	private final KonPrefixCategory category;
	private final int level;
	private final String name;
	
	KonPrefixType(KonPrefixCategory category, int level, String name) {
		this.category = category;
		this.level = level;
		this.name = name;
	}
	
	public KonPrefixCategory category() {
		return category;
	}
	
	public int level() {
		return level;
	}
	
	public String getName() {
		return name;
	}
	
	public static KonPrefixType getDefault() {
		return PEASANT;
	}
	
	public static KonPrefixType getPrefix(String name) {
		KonPrefixType result = getDefault();
		for(KonPrefixType pre : KonPrefixType.values()) {
			if(pre.toString().equalsIgnoreCase(name)) {
				result = pre;
			}
		}
		return result;
	}
	
	public static boolean isPrefixName(String name) {
		boolean result = false;
		for(KonPrefixType pre : KonPrefixType.values()) {
			if(pre.getName().equalsIgnoreCase(name)) {
				result = true;
			}
		}
		return result;
	}
	
	public static KonPrefixType getPrefixByName(String name) {
		KonPrefixType result = getDefault();
		for(KonPrefixType pre : KonPrefixType.values()) {
			if(pre.getName().equalsIgnoreCase(name)) {
				result = pre;
			}
		}
		return result;
	}
}
