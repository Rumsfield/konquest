package konquest.command.admin;


public enum AdminCommandType {
	HELP			("",													"Display the admin help message."),
	BYPASS			("",													"Toggle Admin bypass mode."),
	MAKEKINGDOM		("<kingdom>",											"Create a new Kingdom."),
	MAKETOWN		("<town> <kingdom>",									"Create a new Town for a Kingdom."),
	CLAIM			("[radius|auto] [<radius>]",							"Claim land chunks."),
	UNCLAIM			("",													"Return a chunk to the Wild."),
	REMOVEKINGDOM	("<kingdom>",											"Removes a Kingdom and all claimed Capital and Town chunks."),
	REMOVETOWN		("<kingdom> <town>",									"Removes a Town from a Kingdom and all claimed Town chunks."),
	MONUMENT		("<kingdom> create|remove",								"Defines the Monument for a Kingdom."),
	LIST			("kingdoms|towns|all",						   		 	"Display all Kingdoms or Towns of a Kingdom."),
	FORCEJOIN 		("<player> <kingdom>",									"Forces a player to join a Kingdom and teleports them to the Capital."),
	FORCEEXILE		("<player> [full]", 									"Forces a player to become a Barbarian and teleports them to the Wild."),
	FORCETOWN		("<name> open|close|add|kick|lord|elite|rename|upgrade [arg1] [arg2]", "Forces a Town to accept the provided management sub-commands."),
	RENAME	        ("<kingdom> <oldName> <newName>",					    "Renames a Kingdom or Town to newName."),
	RUIN	        ("create|remove|criticals|spawns",					    "Manage a Ruin territory."),
	SAVE			("", 													"Saves Kingdoms and Players data files."),
	SETTRAVEL		("",													"Sets the travel point for the territory you're in."),												
	TRAVEL			("<kingdom|town>",                                   	"Teleport to the specified kingdom or town."),
	RELOAD			("", 													"Reloads configuration files."),
	FLAG			("<flag> <value>",                                   	"Set kingdom flags. Use the command with no arguments to list available flags."),
	STAT			("<player> <stat> show|set|add|clear [<value>]",		"Manage player stats.");
	
	private final String arguments;
	private final String description;
	AdminCommandType(String arguments, String description) {
		this.arguments = arguments;
		this.description = description;
	}
	
	public String arguments() {
		return arguments;
	}
	
	public String description(){
		return description;
	}
	
	/**
	 * Gets a AdminCommandType enum given a string command
	 * @param command - The string name of the command
	 * @return AdminCommandType - Corresponding enum
	 */
	public static AdminCommandType getCommand(String command) {
		AdminCommandType result = HELP;
		for(AdminCommandType cmd : AdminCommandType.values()) {
			if(cmd.toString().equalsIgnoreCase(command)) {
				result = cmd;
			}
		}
		return result;
	}
	
	/**
	 * Determines whether a string command is a AdminCommandType
	 * @param command - The string name of the command
	 * @return Boolean - True if the string is a command, false otherwise
	 */
	public static boolean contains(String command) {
		boolean result = false;
		for(AdminCommandType cmd : AdminCommandType.values()) {
			if(cmd.toString().equalsIgnoreCase(command)) {
				result = true;
			}
		}
		return result;
	}
}
