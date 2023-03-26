package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.utility.MessagePath;

public enum AdminCommandType {
	HELP        ("konquest.command.admin.help",         "",                                                             MessagePath.DESCRIPTION_ADMIN_HELP.getMessage()),
	BYPASS      ("konquest.command.admin.bypass",       "",                                                             MessagePath.DESCRIPTION_ADMIN_BYPASS.getMessage()),
	CLAIM       ("konquest.command.admin.claim",        "[radius|auto|undo] [<radius>]",                                MessagePath.DESCRIPTION_ADMIN_CLAIM.getMessage()),
	UNCLAIM     ("konquest.command.admin.unclaim",      "[radius|auto] [<radius>]",                                     MessagePath.DESCRIPTION_ADMIN_UNCLAIM.getMessage()),
	KINGDOM     ("konquest.command.admin.kingdom",      "menu|create|destroy|add|kick|rename <kingdom> [<name>]",       MessagePath.DESCRIPTION_ADMIN_KINGDOM.getMessage()),
	TOWN        ("konquest.command.admin.town",         "create|destroy|add|kick|lord|knight|rename|upgrade|shield|armor|plots|options|specialize <town> [<name>] [<arg>]",  MessagePath.DESCRIPTION_ADMIN_TOWN.getMessage()),
	CAMP        ("konquest.command.admin.camp",         "create|destroy <player>",                                      MessagePath.DESCRIPTION_ADMIN_CAMP.getMessage()),
	CAPTURE     ("konquest.command.admin.capture",      "<town> <kingdom>",                                             MessagePath.DESCRIPTION_ADMIN_CAPTURE.getMessage()),
	MONUMENT    ("konquest.command.admin.monument",     "create|remove|reset|show|status <name> [<cost>]",              MessagePath.DESCRIPTION_ADMIN_MONUMENT.getMessage()),
	LIST        ("konquest.command.admin.list",         "[kingdom|town|camp|ruin|sanctuary] [<page>]",                  MessagePath.DESCRIPTION_ADMIN_LIST.getMessage()),
	RUIN        ("konquest.command.admin.ruin",         "create|remove|rename|criticals|spawns [<name>] [<name>]",      MessagePath.DESCRIPTION_ADMIN_RUIN.getMessage()),
	SANCTUARY   ("konquest.command.admin.sanctuary",    "create|remove|rename <name> [<name>]",                         MessagePath.DESCRIPTION_ADMIN_SANCTUARY.getMessage()),
	SAVE        ("konquest.command.admin.save",         "",                                                             MessagePath.DESCRIPTION_ADMIN_SAVE.getMessage()),
	SETTRAVEL   ("konquest.command.admin.settravel",    "",                                                             MessagePath.DESCRIPTION_ADMIN_SETTRAVEL.getMessage()),
	TRAVEL      ("konquest.command.admin.travel",       "<name>",                                                       MessagePath.DESCRIPTION_ADMIN_TRAVEL.getMessage()),
	RELOAD      ("konquest.command.admin.reload",       "",                                                             MessagePath.DESCRIPTION_ADMIN_RELOAD.getMessage()),
	FLAG        ("konquest.command.admin.flag",         "[capital] <name> [<flag>] [<value>]",                          MessagePath.DESCRIPTION_ADMIN_FLAG.getMessage()),
	STAT        ("konquest.command.admin.stat",         "<player> <stat> show|set|add|clear [<value>]",                 MessagePath.DESCRIPTION_ADMIN_STAT.getMessage());

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
