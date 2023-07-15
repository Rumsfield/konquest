package com.github.rumsfield.konquest.model;

import com.github.rumsfield.konquest.utility.MessagePath;

import java.util.HashSet;
import java.util.Set;

public enum KonPropertyFlag {
	// Note that the name field must match the enum name so that the admin flag command can match them.
	// Descriptions come from MessagePath so that they can be translated.

	// Properties for territories
	TRAVEL			("Travel", 	MessagePath.PROPERTIES_TRAVEL.getMessage()),
	PVP				("PvP", 		MessagePath.PROPERTIES_PVP.getMessage()),
	PVE				("PvE", 		MessagePath.PROPERTIES_PVE.getMessage()),
	BUILD			("Build", 	MessagePath.PROPERTIES_BUILD.getMessage()),
	USE				("Use", 		MessagePath.PROPERTIES_USE.getMessage()),
	CHEST			("Chest", 	MessagePath.PROPERTIES_CHEST.getMessage()),
	MOBS			("Mobs", 		MessagePath.PROPERTIES_MOBS.getMessage()),
	PORTALS			("Portals", 	MessagePath.PROPERTIES_PORTALS.getMessage()),
	ENTER			("Enter", 	MessagePath.PROPERTIES_ENTER.getMessage()),
	EXIT			("Exit", 		MessagePath.PROPERTIES_EXIT.getMessage()),
	
	// Properties specifically for towns/capitals
	CAPTURE			("Capture", 	MessagePath.PROPERTIES_CAPTURE.getMessage()),
	CLAIM			("Claim", 	MessagePath.PROPERTIES_CLAIM.getMessage()),
	UNCLAIM			("Unclaim", 	MessagePath.PROPERTIES_UNCLAIM.getMessage()),
	UPGRADE			("Upgrade", 	MessagePath.PROPERTIES_UPGRADE.getMessage()),
	PLOTS			("Plots", 	MessagePath.PROPERTIES_PLOTS.getMessage()),
	
	// Properties for kingdoms
	PEACEFUL		("Peaceful", 	MessagePath.PROPERTIES_PEACEFUL.getMessage()),
	GOLEMS			("Golems", 	MessagePath.PROPERTIES_GOLEMS.getMessage()),

	// Properties for membership
	JOIN			("Join", 		MessagePath.PROPERTIES_JOIN.getMessage()),
	LEAVE			("Leave", 	MessagePath.PROPERTIES_LEAVE.getMessage()),
	PROMOTE			("Promote", 	MessagePath.PROPERTIES_PROMOTE.getMessage()),
	DEMOTE			("Demote", 	MessagePath.PROPERTIES_DEMOTE.getMessage()),
	TRANSFER		("Transfer", 	MessagePath.PROPERTIES_TRANSFER.getMessage()),

	NONE			("N/A", 		"Nothing");
	
	private final String name;
	private final String description;
	
	KonPropertyFlag(String name, String description) {
		this.name = name;
		this.description = description;
	}
	
	public String getName() {
		return name;
	}
	
	public String getDescription() {
		return description;
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
