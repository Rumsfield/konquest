package konquest.command;

import org.bukkit.Material;

import konquest.utility.MessagePath;

public enum CommandType {
	HELP    (Material.LANTERN,          "konquest.command.help",    "h",  "",                                                               MessagePath.DESCRIPTION_HELP.getMessage()),
	JOIN    (Material.GOLDEN_HELMET,    "konquest.command.join",    "",   "[<kingdom>|<town>]",                                             MessagePath.DESCRIPTION_JOIN.getMessage()),
	LEAVE   (Material.FEATHER,          "konquest.command.leave",   "",   "<town>",                                                         MessagePath.DESCRIPTION_LEAVE.getMessage()),
	EXILE   (Material.TNT,              "konquest.command.exile",   "",   "",                                                               MessagePath.DESCRIPTION_EXILE.getMessage()),
	INFO    (Material.SPRUCE_SIGN,      "konquest.command.info",    "i",  "[<kingdom>|<town>|<player>]",                                    MessagePath.DESCRIPTION_INFO.getMessage()),
	LIST    (Material.PAPER,            "konquest.command.list",    "l",  "kingdoms|towns",                                                 MessagePath.DESCRIPTION_LIST.getMessage()),
	FAVOR   (Material.GOLD_INGOT,       "konquest.command.favor",   "f",  "",                                                               MessagePath.DESCRIPTION_FAVOR.getMessage()),
	MAP     (Material.FILLED_MAP,       "konquest.command.map",     "m",  "[far|f|auto|a]",                                                 MessagePath.DESCRIPTION_MAP.getMessage()),
	CHAT    (Material.EMERALD,          "konquest.command.chat",    "c",  "",                                                               MessagePath.DESCRIPTION_CHAT.getMessage()),
	SPY     (Material.DIAMOND_SWORD,    "konquest.command.spy",     "",   "",                                                               MessagePath.DESCRIPTION_SPY.getMessage()),
	SETTLE  (Material.DIAMOND_PICKAXE,  "konquest.command.settle",  "",   "<name>",                                                         MessagePath.DESCRIPTION_SETTLE.getMessage()),
	CLAIM   (Material.DIAMOND_SHOVEL,   "konquest.command.claim",   "",   "[radius|auto] [<radius>]",                                       MessagePath.DESCRIPTION_CLAIM.getMessage()),
	TRAVEL  (Material.COMPASS,          "konquest.command.travel",  "t",  "<town>|capital|camp|wild",                                       MessagePath.DESCRIPTION_TRAVEL.getMessage()),
	TOWN    (Material.IRON_DOOR,        "konquest.command.town",    "",   "<town> options|add|kick|knight|lord|rename|upgrade|shield [player]", MessagePath.DESCRIPTION_TOWN.getMessage()),
	QUEST   (Material.WRITABLE_BOOK,    "konquest.command.quest",   "q",  "",                                                               MessagePath.DESCRIPTION_QUEST.getMessage()),
	STATS   (Material.BOOK,             "konquest.command.stats",   "s",  "",                                                               MessagePath.DESCRIPTION_STATS.getMessage()),
	PREFIX  (Material.NAME_TAG,         "konquest.command.prefix",  "p",  "",                                                               MessagePath.DESCRIPTION_PREFIX.getMessage()),
	SCORE   (Material.DIAMOND,          "konquest.command.score",   "",   "[<player>|all]",                                                 MessagePath.DESCRIPTION_SCORE.getMessage()),
	FLY     (Material.ELYTRA,           "konquest.command.fly",     "",   "",                                                               MessagePath.DESCRIPTION_FLY.getMessage()),
	ADMIN   (Material.NETHER_STAR,      "konquest.command.admin",   "",   "",                                                               MessagePath.DESCRIPTION_ADMIN.getMessage());

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
