package konquest.command.admin;

import konquest.utility.MessagePath;

public enum AdminCommandType {
	HELP			("konquest.command.admin.help",				"",																	MessagePath.DESCRIPTION_ADMIN_HELP.getMessage()),
	BYPASS			("konquest.command.admin.bypass",			"",																	MessagePath.DESCRIPTION_ADMIN_BYPASS.getMessage()),
	//MAKEKINGDOM		("konquest.command.admin.makekingdom",		"<kingdom>",														MessagePath.DESCRIPTION_ADMIN_MAKEKINGDOM.getMessage()),
	//MAKETOWN		("konquest.command.admin.maketown",			"<town> <kingdom>",													MessagePath.DESCRIPTION_ADMIN_MAKETOWN.getMessage()),
	CLAIM			("konquest.command.admin.claim",			"[radius|auto|undo] [<radius>]",									MessagePath.DESCRIPTION_ADMIN_CLAIM.getMessage()),
	UNCLAIM			("konquest.command.admin.unclaim",			"[radius|auto] [<radius>]",											MessagePath.DESCRIPTION_ADMIN_UNCLAIM.getMessage()),
	//REMOVEKINGDOM	("konquest.command.admin.removekingdom",	"<kingdom>",														MessagePath.DESCRIPTION_ADMIN_REMOVEKINGDOM.getMessage()),
	//REMOVETOWN		("konquest.command.admin.removetown",		"<kingdom> <town>",													MessagePath.DESCRIPTION_ADMIN_REMOVETOWN.getMessage()),
	//REMOVECAMP		("konquest.command.admin.removecamp",		"<player>",															MessagePath.DESCRIPTION_ADMIN_REMOVECAMP.getMessage()),
	//TODO: KR Update descriptions
	/* New */
	KINGDOM		    ("konquest.command.admin.kingdom",			"menu|create|remove|add|kick|rename <kingdom> [<name>]",			MessagePath.DESCRIPTION_ADMIN_MAKEKINGDOM.getMessage()),
	TOWN		    ("konquest.command.admin.town",			    "create|remove|add|kick|lord|knight|rename|upgrade|shield|armor|plots|options <town> [<name>] [<arg>]",	MessagePath.DESCRIPTION_ADMIN_MAKEKINGDOM.getMessage()),
	CAMP		    ("konquest.command.admin.camp",			    "create|remove <player>",											MessagePath.DESCRIPTION_ADMIN_MAKEKINGDOM.getMessage()),
	CAPTURE		    ("konquest.command.admin.capture",			"<town> <kingdom>",													MessagePath.DESCRIPTION_ADMIN_MAKEKINGDOM.getMessage()),
	/* End */
	
	MONUMENT		("konquest.command.admin.monument",			"create|remove|show|status <name>",									MessagePath.DESCRIPTION_ADMIN_MONUMENT.getMessage()),
	LIST			("konquest.command.admin.list",				"[kingdom|town|camp|ruin|sanctuary] [<page>]",						MessagePath.DESCRIPTION_ADMIN_LIST.getMessage()),
	//FORCECAPTURE	("konquest.command.admin.forcecapture",		"<town> <kingdom>",													MessagePath.DESCRIPTION_ADMIN_FORCECAPTURE.getMessage()),
	//FORCEJOIN 		("konquest.command.admin.forcejoin",		"<player> <kingdom>",												MessagePath.DESCRIPTION_ADMIN_FORCEJOIN.getMessage()),
	//FORCEEXILE		("konquest.command.admin.forceexile",		"<player> [full]", 													MessagePath.DESCRIPTION_ADMIN_FORCEEXILE.getMessage()),
	//FORCETOWN		("konquest.command.admin.forcetown",		"<name> options|add|kick|lord|knight|rename|upgrade|shield|armor|plots [arg1] [arg2]", MessagePath.DESCRIPTION_ADMIN_FORCETOWN.getMessage()),
	//FORCEGUILD		("konquest.command.admin.forceguild",		"<name> [menu|add|kick|rename] [name]", 							MessagePath.DESCRIPTION_ADMIN_FORCEGUILD.getMessage()),
	//RENAME	        ("konquest.command.admin.rename",			"<kingdom> <oldName> <newName>",					    			MessagePath.DESCRIPTION_ADMIN_RENAME.getMessage()),
	RUIN	        ("konquest.command.admin.ruin",				"create|remove|rename|criticals|spawns [<name>] [<name>]",			MessagePath.DESCRIPTION_ADMIN_RUIN.getMessage()),
	SANCTUARY		("konquest.command.admin.sanctuary",		"create|remove|rename <name> [<name>]",								MessagePath.DESCRIPTION_ADMIN_SANCTUARY.getMessage()),
	SAVE			("konquest.command.admin.save",				"", 																MessagePath.DESCRIPTION_ADMIN_SAVE.getMessage()),
	SETTRAVEL		("konquest.command.admin.settravel",		"",																	MessagePath.DESCRIPTION_ADMIN_SETTRAVEL.getMessage()),												
	TRAVEL			("konquest.command.admin.travel",			"<name>",                                   						MessagePath.DESCRIPTION_ADMIN_TRAVEL.getMessage()),
	RELOAD			("konquest.command.admin.reload",			"", 																MessagePath.DESCRIPTION_ADMIN_RELOAD.getMessage()),
	FLAG			("konquest.command.admin.flag",				"<name> [<flag>] [<value>]",                                   		MessagePath.DESCRIPTION_ADMIN_FLAG.getMessage()),
	STAT			("konquest.command.admin.stat",				"<player> <stat> show|set|add|clear [<value>]",						MessagePath.DESCRIPTION_ADMIN_STAT.getMessage());
	
	private final String permission;
	private final String arguments;
	private final String description;
	AdminCommandType(String permission, String arguments, String description) {
		this.permission = permission;
		this.arguments = arguments;
		this.description = description;
	}
	
	public String permission() {
		return permission;
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
