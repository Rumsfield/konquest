package konquest.model;

import konquest.utility.MessagePath;

public enum KonPrefixType {

	// Royalty - special cases
	KING        (KonPrefixCategory.ROYALTY, 20, MessagePath.PREFIX_TITLE_KING.getMessage()),
	QUEEN       (KonPrefixCategory.ROYALTY, 20, MessagePath.PREFIX_TITLE_QUEEN.getMessage()),
	PRINCE      (KonPrefixCategory.ROYALTY, 10, MessagePath.PREFIX_TITLE_PRINCE.getMessage()),
	PRINCESS    (KonPrefixCategory.ROYALTY, 10, MessagePath.PREFIX_TITLE_PRINCESS.getMessage()),
	NOBLE       (KonPrefixCategory.ROYALTY, 5,  MessagePath.PREFIX_TITLE_NOBLE.getMessage()),
	// Clergy - enchanting and potions accomplishments
	CARDINAL    (KonPrefixCategory.CLERGY, 1000,  MessagePath.PREFIX_TITLE_CARDINAL.getMessage()),
	BISHOP      (KonPrefixCategory.CLERGY, 750,   MessagePath.PREFIX_TITLE_BISHOP.getMessage()),
	PRIEST      (KonPrefixCategory.CLERGY, 500,   MessagePath.PREFIX_TITLE_PRIEST.getMessage()),
	CLERIC      (KonPrefixCategory.CLERGY, 300,   MessagePath.PREFIX_TITLE_CLERIC.getMessage()),
	CHAPLAIN    (KonPrefixCategory.CLERGY, 200,   MessagePath.PREFIX_TITLE_CHAPLAIN.getMessage()),
	SCRIBE      (KonPrefixCategory.CLERGY, 100,   MessagePath.PREFIX_TITLE_SCRIBE.getMessage()),
	// Nobility - Settlement and claiming accomplishments
	VICEROY     (KonPrefixCategory.NOBILITY, 300, MessagePath.PREFIX_TITLE_VICEROY.getMessage()),
	DUKE        (KonPrefixCategory.NOBILITY, 150, MessagePath.PREFIX_TITLE_DUKE.getMessage()),
	COUNT       (KonPrefixCategory.NOBILITY, 100, MessagePath.PREFIX_TITLE_COUNT.getMessage()),
	EARL        (KonPrefixCategory.NOBILITY, 75,  MessagePath.PREFIX_TITLE_EARL.getMessage()),
	BARON       (KonPrefixCategory.NOBILITY, 50,  MessagePath.PREFIX_TITLE_BARON.getMessage()),
	LORD        (KonPrefixCategory.NOBILITY, 30,  MessagePath.PREFIX_TITLE_LORD.getMessage()),
	VASSAL      (KonPrefixCategory.NOBILITY, 15,  MessagePath.PREFIX_TITLE_VASSAL.getMessage()),
	PEASANT     (KonPrefixCategory.NOBILITY, 0,   MessagePath.PREFIX_TITLE_PEASANT.getMessage()),
	// Tradesmen - Favor and precious metals accomplishments
	MASTER      (KonPrefixCategory.TRADESMAN, 5000, MessagePath.PREFIX_TITLE_MASTER.getMessage()),
	ARTISAN     (KonPrefixCategory.TRADESMAN, 2000, MessagePath.PREFIX_TITLE_ARTISAN.getMessage()),
	JOURNEYMAN  (KonPrefixCategory.TRADESMAN, 1000, MessagePath.PREFIX_TITLE_JOURNEYMAN.getMessage()),
	CRAFTSMAN   (KonPrefixCategory.TRADESMAN, 500,  MessagePath.PREFIX_TITLE_CRAFTSMAN.getMessage()),
	MERCHANT    (KonPrefixCategory.TRADESMAN, 250,  MessagePath.PREFIX_TITLE_MERCHANT.getMessage()),
	PEDDLER     (KonPrefixCategory.TRADESMAN, 100,  MessagePath.PREFIX_TITLE_PEDDLER.getMessage()),
	// Military - pvp accomplishments
	GENERAL     (KonPrefixCategory.MILITARY, 800, MessagePath.PREFIX_TITLE_GENERAL.getMessage()),
	MAJOR       (KonPrefixCategory.MILITARY, 400, MessagePath.PREFIX_TITLE_MAJOR.getMessage()),
	CAPTAIN     (KonPrefixCategory.MILITARY, 200, MessagePath.PREFIX_TITLE_CAPTAIN.getMessage()),
	SERGEANT    (KonPrefixCategory.MILITARY, 100, MessagePath.PREFIX_TITLE_SERGEANT.getMessage()),
	KNIGHT      (KonPrefixCategory.MILITARY, 50,  MessagePath.PREFIX_TITLE_KNIGHT.getMessage()),
	SQUIRE      (KonPrefixCategory.MILITARY, 10,  MessagePath.PREFIX_TITLE_SQUIRE.getMessage()),
	// Farming - farming accomplishments
	COWBOY      (KonPrefixCategory.FARMING, 1500, MessagePath.PREFIX_TITLE_COWBOY.getMessage()),
	BREEDER     (KonPrefixCategory.FARMING, 1000, MessagePath.PREFIX_TITLE_BREEDER.getMessage()),
	RANCHER     (KonPrefixCategory.FARMING, 600,  MessagePath.PREFIX_TITLE_RANCHER.getMessage()),
	FARMER      (KonPrefixCategory.FARMING, 300,  MessagePath.PREFIX_TITLE_FARMER.getMessage()),
	PICKER      (KonPrefixCategory.FARMING, 150,  MessagePath.PREFIX_TITLE_PICKER.getMessage()),
	SPROUT      (KonPrefixCategory.FARMING, 50,   MessagePath.PREFIX_TITLE_SPROUT.getMessage()),
	// Misc - miscellaneous accomplishments
	JESTER      (KonPrefixCategory.JOKING, 100,   MessagePath.PREFIX_TITLE_JESTER.getMessage()),
	FOOL        (KonPrefixCategory.JOKING, 69,    MessagePath.PREFIX_TITLE_FOOL.getMessage()),
	DANCER      (KonPrefixCategory.JOKING, 50,    MessagePath.PREFIX_TITLE_DANCER.getMessage()),
	BARD        (KonPrefixCategory.JOKING, 20,    MessagePath.PREFIX_TITLE_BARD.getMessage()),
	FISHERMAN   (KonPrefixCategory.FISHING, 200,  MessagePath.PREFIX_TITLE_FISHERMAN.getMessage()),
	CASTER      (KonPrefixCategory.FISHING, 100,  MessagePath.PREFIX_TITLE_CASTER.getMessage()),
	CHUM        (KonPrefixCategory.FISHING, 20,   MessagePath.PREFIX_TITLE_CHUM.getMessage()),
	CHEF        (KonPrefixCategory.COOKING, 200,  MessagePath.PREFIX_TITLE_CHEF.getMessage()),
	COOK        (KonPrefixCategory.COOKING, 100,  MessagePath.PREFIX_TITLE_COOK.getMessage()),
	BAKER       (KonPrefixCategory.COOKING, 20,   MessagePath.PREFIX_TITLE_BAKER.getMessage());
	
	
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
