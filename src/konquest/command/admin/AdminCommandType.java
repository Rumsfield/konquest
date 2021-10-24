package konquest.command.admin;

import konquest.utility.MessagePath;

public enum AdminCommandType {
	HELP			("",																	MessagePath.DESCRIPTION_ADMIN_HELP.getMessage()),
	BYPASS			("",																	MessagePath.DESCRIPTION_ADMIN_BYPASS.getMessage()),
	MAKEKINGDOM		("<kingdom>",															MessagePath.DESCRIPTION_ADMIN_MAKEKINGDOM.getMessage()),
	MAKETOWN		("<town> <kingdom>",													MessagePath.DESCRIPTION_ADMIN_MAKETOWN.getMessage()),
	CLAIM			("[radius|auto] [<radius>]",											MessagePath.DESCRIPTION_ADMIN_CLAIM.getMessage()),
	UNCLAIM			("",																	MessagePath.DESCRIPTION_ADMIN_UNCLAIM.getMessage()),
	REMOVEKINGDOM	("<kingdom>",															MessagePath.DESCRIPTION_ADMIN_REMOVEKINGDOM.getMessage()),
	REMOVETOWN		("<kingdom> <town>",													MessagePath.DESCRIPTION_ADMIN_REMOVETOWN.getMessage()),
	REMOVECAMP		("<player>",															MessagePath.DESCRIPTION_ADMIN_REMOVECAMP.getMessage()),
	MONUMENT		("<kingdom> create|remove|show",										MessagePath.DESCRIPTION_ADMIN_MONUMENT.getMessage()),
	LIST			("kingdoms|towns|all",						   		 					MessagePath.DESCRIPTION_ADMIN_LIST.getMessage()),
	FORCECAPTURE	("<town> <kingdom>",													MessagePath.DESCRIPTION_ADMIN_FORCECAPTURE.getMessage()),
	FORCEJOIN 		("<player> <kingdom>",													MessagePath.DESCRIPTION_ADMIN_FORCEJOIN.getMessage()),
	FORCEEXILE		("<player> [full]", 													MessagePath.DESCRIPTION_ADMIN_FORCEEXILE.getMessage()),
	FORCETOWN		("<name> options|add|kick|lord|knight|rename|upgrade|shield|armor|plots [arg1] [arg2]", MessagePath.DESCRIPTION_ADMIN_FORCETOWN.getMessage()),
	RENAME	        ("<kingdom> <oldName> <newName>",					    				MessagePath.DESCRIPTION_ADMIN_RENAME.getMessage()),
	RUIN	        ("create|remove|criticals|spawns [<name>]",								MessagePath.DESCRIPTION_ADMIN_RUIN.getMessage()),
	SAVE			("", 																	MessagePath.DESCRIPTION_ADMIN_SAVE.getMessage()),
	SETTRAVEL		("",																	MessagePath.DESCRIPTION_ADMIN_SETTRAVEL.getMessage()),												
	TRAVEL			("<kingdom|town>",                                   					MessagePath.DESCRIPTION_ADMIN_TRAVEL.getMessage()),
	RELOAD			("", 																	MessagePath.DESCRIPTION_ADMIN_RELOAD.getMessage()),
	FLAG			("<flag> <value>",                                   					MessagePath.DESCRIPTION_ADMIN_FLAG.getMessage()),
	STAT			("<player> <stat> show|set|add|clear [<value>]",						MessagePath.DESCRIPTION_ADMIN_STAT.getMessage());
	
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
