package com.github.rumsfield.konquest.command;

import org.bukkit.Material;

import com.github.rumsfield.konquest.utility.MessagePath;

public enum CommandType {
	HELP    (Material.LANTERN,          "konquest.command.help",    "h",  new CommandArgumentStructure("[<page>]"),                                                       MessagePath.DESCRIPTION_HELP.getMessage()),
	INFO    (Material.SPRUCE_SIGN,      "konquest.command.info",    "i",  new CommandArgumentStructure("[player|kingdom|capital|town|ruin|sanctuary <name>]"),            MessagePath.DESCRIPTION_INFO.getMessage()),
	LIST    (Material.PAPER,            "konquest.command.list",    "l",  new CommandArgumentStructure("[kingdom|town|ruin|sanctuary] [<page>]"),                         MessagePath.DESCRIPTION_LIST.getMessage()),
	KINGDOM (Material.GOLDEN_SWORD,     "konquest.command.kingdom", "k",  new CommandArgumentStructure("[menu|create|join|exile|invite|kick|rename|templates|webcolor] [<template>|<kingdom>] [<name>]"), MessagePath.DESCRIPTION_KINGDOM.getMessage()),
	TOWN    (Material.OBSIDIAN,         "konquest.command.town",    "t",  new CommandArgumentStructure("[<town>] [menu|join|leave|invite|kick|lord|rename] [<name>]"),    MessagePath.DESCRIPTION_TOWN.getMessage()),
	MAP     (Material.FILLED_MAP,       "konquest.command.map",     "m",  new CommandArgumentStructure("[far|auto]"),                                                     MessagePath.DESCRIPTION_MAP.getMessage()),
	SETTLE  (Material.DIAMOND_PICKAXE,  "konquest.command.settle",  "",   new CommandArgumentStructure("<name>"),                                                         MessagePath.DESCRIPTION_SETTLE.getMessage()),
	CLAIM   (Material.DIAMOND_SHOVEL,   "konquest.command.claim",   "",   new CommandArgumentStructure("[radius|auto] [<radius>]"),                                       MessagePath.DESCRIPTION_CLAIM.getMessage()),
	UNCLAIM (Material.COBWEB,           "konquest.command.unclaim", "",   new CommandArgumentStructure("[radius|auto] [<radius>]"),                                       MessagePath.DESCRIPTION_UNCLAIM.getMessage()),
	TRAVEL  (Material.COMPASS,          "konquest.command.travel",  "v",  new CommandArgumentStructure("<town>|<kingdom>|<sanctuary>|capital|home|wild|camp"),            MessagePath.DESCRIPTION_TRAVEL.getMessage()),
	CHAT    (Material.EMERALD,          "konquest.command.chat",    "c",  new CommandArgumentStructure(""),                                                               MessagePath.DESCRIPTION_CHAT.getMessage()),
	SPY     (Material.IRON_BARS,        "konquest.command.spy",     "",   new CommandArgumentStructure(""),                                                               MessagePath.DESCRIPTION_SPY.getMessage()),
	FAVOR   (Material.GOLD_INGOT,       "konquest.command.favor",   "f",  new CommandArgumentStructure(""),                                                               MessagePath.DESCRIPTION_FAVOR.getMessage()),
	SCORE   (Material.DIAMOND,          "konquest.command.score",   "",   new CommandArgumentStructure("[<player>|all]"),                                                 MessagePath.DESCRIPTION_SCORE.getMessage()),
	QUEST   (Material.WRITABLE_BOOK,    "konquest.command.quest",   "q",  new CommandArgumentStructure(""),                                                               MessagePath.DESCRIPTION_QUEST.getMessage()),
	STATS   (Material.BOOK,             "konquest.command.stats",   "s",  new CommandArgumentStructure(""),                                                               MessagePath.DESCRIPTION_STATS.getMessage()),
	PREFIX  (Material.NAME_TAG,         "konquest.command.prefix",  "p",  new CommandArgumentStructure(""),                                                               MessagePath.DESCRIPTION_PREFIX.getMessage()),
	BORDER  (Material.GREEN_CARPET,     "konquest.command.border",  "b",  new CommandArgumentStructure(""),                                                               MessagePath.DESCRIPTION_BORDER.getMessage()),
	FLY     (Material.ELYTRA,           "konquest.command.fly",     "",   new CommandArgumentStructure(""),                                                               MessagePath.DESCRIPTION_FLY.getMessage()),
	ADMIN   (Material.NETHER_STAR,      "konquest.command.admin",   "",   new CommandArgumentStructure(""),                                                               MessagePath.DESCRIPTION_ADMIN.getMessage());

	private final String permission;
	private final CommandArgumentStructure arguments;
	private final String description;
	private final String alias;
	private final Material iconMaterial;
	CommandType(Material iconMaterial, String permission, String alias, CommandArgumentStructure arguments, String description) {
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
		return arguments.getArgumentString();
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
