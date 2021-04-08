package konquest.command.admin;


public enum AdminFlagType {
	PEACEFUL		("<kingdom> true|false",								"Make a Kingdom peaceful."),
	PLACEHOLDER		("none",												"Not yet implemented.");
	
	private final String flagValues;
	private final String description;
	AdminFlagType(String flagValues, String description) {
		this.flagValues = flagValues;
		this.description = description;
	}
	
	public String flagValues() {
		return flagValues;
	}
	
	public String description(){
		return description;
	}
	
	/**
	 * Gets a AdminFlagType enum given a string command
	 * @param command - The string name of the command
	 * @return AdminFlagType - Corresponding enum
	 */
	public static AdminFlagType getFlag(String flag) {
		AdminFlagType result = PEACEFUL;
		for(AdminFlagType cmd : AdminFlagType.values()) {
			if(cmd.toString().equalsIgnoreCase(flag)) {
				result = cmd;
			}
		}
		return result;
	}
	
	/**
	 * Determines whether a string command is a AdminFlagType
	 * @param flag - The string name of the flag
	 * @return Boolean - True if the string is a flag, false otherwise
	 */
	public static boolean contains(String flag) {
		boolean result = false;
		for(AdminFlagType cmd : AdminFlagType.values()) {
			if(cmd.toString().equalsIgnoreCase(flag)) {
				result = true;
			}
		}
		return result;
	}
}
