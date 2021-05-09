package konquest.command;

import org.bukkit.Material;

public enum CommandType {
	HELP		(Material.LANTERN, 			"konquest.command.help",		"h",		"",													"Display the help message."),
	JOIN		(Material.GOLDEN_HELMET, 	"konquest.command.join",		"",			"[<kingdom>|<town>]",								"Join a Kingdom or Town."),
	LEAVE		(Material.FEATHER, 			"konquest.command.leave",		"",			"<town>",											"Leave a Town."),
	EXILE		(Material.TNT, 				"konquest.command.exile",		"",			"",													"Exile yourself and become a Barbarian."),
	INFO		(Material.SPRUCE_SIGN, 		"konquest.command.info",		"i",		"[<kingdom>|<town>|<player>]",						"Display info about different things."),
	LIST		(Material.PAPER, 			"konquest.command.list",       	"l",        "kingdoms|towns",                       			"List the Towns in your Kingdom."),
	FAVOR		(Material.GOLD_INGOT, 		"konquest.command.favor",		"f",		"",													"Display your current Favor from the King."),
	MAP			(Material.FILLED_MAP, 		"konquest.command.map",			"m",    	"[far|f|auto|a]",					    			"Display a map of claimed chunks."),
	CHAT		(Material.EMERALD, 			"konquest.command.chat",		"c",		"",													"Switch between global and kingdom-only chat."),
	SPY		    (Material.DIAMOND_SWORD, 	"konquest.command.spy",			"", 		"",													"Reveal the location of an enemy town."),
	SETTLE		(Material.DIAMOND_PICKAXE, 	"konquest.command.settle",		"",			"<name>",											"Settle a new Town."),
	CLAIM		(Material.DIAMOND_SHOVEL, 	"konquest.command.claim",		"",			"[radius|auto] [<radius>]",	                		"Claim a chunk of land."),
	TRAVEL		(Material.COMPASS, 			"konquest.command.travel", 		"t",		"<town>|capital|camp|wild",							"Teleport to a Town, your Camp or to your Capital."),
	TOWN		(Material.NAME_TAG, 		"konquest.command.town",		"", 		"<town> open|close|add|kick|knight|lord|rename|upgrade [player]", 	"Manage Towns you belong to."),
	QUEST		(Material.WRITABLE_BOOK, 	"konquest.command.quest",		"q",		"",													"Display your quest log."),
	STATS		(Material.BOOK, 			"konquest.command.stats",		"s", 		"", 									            "Display your Accomplishment stats."),
	PREFIX		(Material.CARVED_PUMPKIN, 	"konquest.command.prefix",		"p", 		"<name>|off", 										"Set your prefix title."),
	SCORE		(Material.DIAMOND, 			"konquest.command.score",		"", 		"[<player>|all]", 									"Display a Player's current score."),
	ADMIN		(Material.NETHER_STAR, 		"konquest.command.admin",		"",			"",													"For Admins only.");
	
	private final String permission;
	private final String arguments;
	private final String description;
	private final String alias;
	private final Material iconMaterial;
	CommandType(Material iconMaterial, String permission, String alias, String arguments, String description) {
		this.iconMaterial = iconMaterial;
		this.permission = permission;
		this.arguments = arguments;
		this.description = description;
		this.alias = alias;
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
	
	public String alias() {
		return alias;
	}
	
	/**
	 * Gets a CommandType enum given a string command.
	 * If an alias is given, this returns the command enum
	 * @param command - The string name of the command
	 * @return CommandType - Corresponding enum
	 */
	public static CommandType getCommand(String command) {
		CommandType result = HELP;
		for(CommandType cmd : CommandType.values()) {
			if(cmd.toString().equalsIgnoreCase(command) || (!cmd.alias().equals("") && cmd.alias().equalsIgnoreCase(command))) {
				result = cmd;
			}
		}
		return result;
	}
	
	/**
	 * Determines whether a string command is a CommandType
	 * @param command - The string name of the command
	 * @return Boolean - True if the string is a command, false otherwise
	 */
	public static boolean contains(String command) {
		boolean result = false;
		for(CommandType cmd : CommandType.values()) {
			if(cmd.toString().equalsIgnoreCase(command)) {
				result = true;
			}
		}
		return result;
	}
	
}
