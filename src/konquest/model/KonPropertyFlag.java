package konquest.model;

public enum KonPropertyFlag {
//TODO: Replace these strings with MessagePaths
	
	// Properties for territories
	TRAVEL			("Travel", 		"Allow travel to this territory"),
	PVP				("PvP", 		"Allow player damage"),
	BUILD			("Build", 		"Allow block edits"),
	USE				("Use", 		"Allow using blocks"),
	MOBS			("Mobs", 		"Allow mobs to spawn"),
	PORTALS			("Portals", 	"Allow players to use portals"),
	ENTER			("Enter", 		"Allow players to enter"),
	EXIT			("Exit", 		"Allow players to exit"),
	
	// Properties specifically for towns/capitals
	CAPTURE			("Capture", 	"Allow this town to be captured"),
	CLAIM			("Claim", 		"Allow players to claim land"),
	UNCLAIM			("Unclaim", 	"Allow players to unclaim land"),
	UPGRADE			("Upgrade", 	"Allow players to upgrade this town"),
	PLOTS			("Plots", 		"Allow players to set town plots"),
	
	// Properties for kingdoms
	NEUTRAL			("Neutral", 	"Make this kingdom only peaceful"),
	GOLEMS			("Golems", 		"Enable iron golems to attack enemies"),
	
	NONE			("N/A", 		"Nothing");
	
	private final String name;
	private final String descripton;
	
	KonPropertyFlag(String name, String descripton) {
		this.name = name;
		this.descripton = descripton;
	}
	
	public String getName() {
		return name;
	}
	
	public String getDescription() {
		return descripton;
	}
	
	/**
	 * Gets an enum given a string command
	 * @param flag - The string name of the flag
	 * @return KonPropertyFlag - Corresponding enum
	 */
	public static KonPropertyFlag getFlag(String flag) {
		KonPropertyFlag result = NONE;
		for(KonPropertyFlag p : KonPropertyFlag.values()) {
			if(p.toString().equalsIgnoreCase(flag)) {
				result = p;
			}
		}
		return result;
	}
	
	/**
	 * Determines whether a string flag is an enum
	 * @param flag - The string name of the flag
	 * @return Boolean - True if the string is a flag, false otherwise
	 */
	public static boolean contains(String flag) {
		boolean result = false;
		for(KonPropertyFlag p : KonPropertyFlag.values()) {
			if(p.toString().equalsIgnoreCase(flag)) {
				result = true;
			}
		}
		return result;
	}
}