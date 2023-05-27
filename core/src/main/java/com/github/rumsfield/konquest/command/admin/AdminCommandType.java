package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Material;

public enum AdminCommandType {
	HELP        (Material.REDSTONE_TORCH,		"konquest.admin.help",         "[<page>]",                                                     MessagePath.DESCRIPTION_ADMIN_HELP.getMessage()),
	BYPASS      (Material.SPECTRAL_ARROW,		"konquest.admin.bypass",       "",                                                             MessagePath.DESCRIPTION_ADMIN_BYPASS.getMessage()),
	LIST        (Material.PAPER,				"konquest.admin.list",         "[kingdom|town|camp|ruin|sanctuary] [<page>]",                  MessagePath.DESCRIPTION_ADMIN_LIST.getMessage()),
	KINGDOM     (Material.GOLDEN_SWORD,			"konquest.admin.kingdom",      "menu|create|destroy|add|kick|rename|admin <kingdom> [<name>]", MessagePath.DESCRIPTION_ADMIN_KINGDOM.getMessage()),
	TOWN        (Material.OBSIDIAN,				"konquest.admin.town",         "create|destroy|add|kick|lord|knight|rename|upgrade|shield|armor|plots|options|specialize <town> [<name>] [<arg>]",  MessagePath.DESCRIPTION_ADMIN_TOWN.getMessage()),
	CAMP        (Material.ORANGE_BED,			"konquest.admin.camp",         "create|destroy <player>",                                      MessagePath.DESCRIPTION_ADMIN_CAMP.getMessage()),
	CLAIM       (Material.DIAMOND_SHOVEL,		"konquest.admin.claim",        "[radius|auto|undo] [<radius>]",                                MessagePath.DESCRIPTION_ADMIN_CLAIM.getMessage()),
	UNCLAIM     (Material.COBWEB,				"konquest.admin.unclaim",      "[radius|auto] [<radius>]",                                     MessagePath.DESCRIPTION_ADMIN_UNCLAIM.getMessage()),
	CAPTURE     (Material.FISHING_ROD,			"konquest.admin.capture",      "<town> <kingdom>",                                             MessagePath.DESCRIPTION_ADMIN_CAPTURE.getMessage()),
	MONUMENT    (Material.CRAFTING_TABLE,		"konquest.admin.monument",     "create|remove|reset|show|status <name> [<cost>]",              MessagePath.DESCRIPTION_ADMIN_MONUMENT.getMessage()),
	RUIN        (Material.CRACKED_STONE_BRICKS,	"konquest.admin.ruin",         "create|remove|rename|criticals|spawns [<name>] [<name>]",      MessagePath.DESCRIPTION_ADMIN_RUIN.getMessage()),
	SANCTUARY   (Material.BEDROCK,				"konquest.admin.sanctuary",    "create|remove|rename <name> [<name>]",                         MessagePath.DESCRIPTION_ADMIN_SANCTUARY.getMessage()),
	TRAVEL      (Material.COMPASS,				"konquest.admin.travel",       "<name>",                                                       MessagePath.DESCRIPTION_ADMIN_TRAVEL.getMessage()),
	SETTRAVEL   (Material.OAK_SIGN,				"konquest.admin.settravel",    "",                                                             MessagePath.DESCRIPTION_ADMIN_SETTRAVEL.getMessage()),
	FLAG        (Material.ORANGE_BANNER,		"konquest.admin.flag",         "kingdom|capital|town|sanctuary|ruin <name> [<flag>] [<value>]", MessagePath.DESCRIPTION_ADMIN_FLAG.getMessage()),
	STAT        (Material.BOOKSHELF,			"konquest.admin.stat",         "<player> <stat> show|set|add|clear [<value>]",                 MessagePath.DESCRIPTION_ADMIN_STAT.getMessage()),
	SAVE        (Material.TOTEM_OF_UNDYING,		"konquest.admin.save",         "",                                                             MessagePath.DESCRIPTION_ADMIN_SAVE.getMessage()),
	RELOAD      (Material.GLOWSTONE,			"konquest.admin.reload",       "",                                                             MessagePath.DESCRIPTION_ADMIN_RELOAD.getMessage());

	private final String permission;
	private final String arguments;
	private final String description;
	private final Material iconMaterial;
	AdminCommandType(Material iconMaterial, String permission, String arguments, String description) {
		this.iconMaterial = iconMaterial;
		this.permission = permission;
		this.arguments = arguments;
		this.description = description;
	}

	public Material iconMaterial() {
		return iconMaterial;
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
