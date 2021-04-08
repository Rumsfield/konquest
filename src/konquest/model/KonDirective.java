package konquest.model;

public enum KonDirective {
	SETTLE_TOWN		("konquest.directive.settle",	1,		"Fresh Start",			"Settle a Town for your Kingdom."),
	CLAIM_LAND		("konquest.directive.claim",	5,		"Path to Nobility",		"Claim land for any Town in your Kingdom."),
	BUILD_TOWN		("konquest.directive.build",	500,	"Masonry",				"Build structures for any Town in your Kingdom."),
	CRAFT_ARMOR		("konquest.directive.armor",	4,		"Knighthood",			"Craft a set of iron armor to protect yourself."),
	CREATE_GOLEM	("konquest.directive.golem",	1,		"Stoic Protector",		"Create an Iron Golem to protect any Town in your Kingdom."),
	ENCHANT_ITEM	("konquest.directive.enchant",	3,		"Divine Tools",			"Apply any enchantment to tools/weapons."),
	ATTACK_TOWN		("konquest.directive.attack",	1,		"Act of Aggression",	"Attack any Town in an enemy Kingdom by breaking blocks."),
	CAPTURE_TOWN	("konquest.directive.capture",	1,		"Military Action",		"Capture any Town in an enemy Kingdom by destroying the critical blocks within the Town Monument."),
	KILL_ENEMY		("konquest.directive.kill",		1,		"Best Enemies",			"Kill a player from an enemy Kingdom.");
	
	private final String permission;
	private final int stages;
	private final String title;
	private final String description;
	KonDirective(String permission, int stages, String title, String description) {
		this.permission = permission;
		this.stages = stages;
		this.title = title;
		this.description = description;
	}
	
	public String permission() {
		return permission;
	}
	
	public int stages() {
		return stages;
	}
	
	public String title() {
		return title;
	}
	
	public String description(){
		return description;
	}
	
	/**
	 * Gets a KonDirective enum given a string command.
	 * @param dir - The string name of the KonDirective
	 * @return KonDirective - Corresponding enum
	 */
	public static KonDirective getDirective(String name) {
		KonDirective result = null;
		for(KonDirective dir : KonDirective.values()) {
			if(dir.toString().equalsIgnoreCase(name)) {
				result = dir;
			}
		}
		return result;
	}
	
	
	
}
