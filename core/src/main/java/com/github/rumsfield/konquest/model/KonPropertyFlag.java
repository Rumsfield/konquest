package com.github.rumsfield.konquest.model;

import com.github.rumsfield.konquest.utility.MessagePath;

import java.util.HashSet;
import java.util.Set;

public enum KonPropertyFlag {
	// Note that the name field must match the enum name so that the admin flag command can match them.
	// Descriptions come from MessagePath so that they can be translated.

	// Properties for territories
	TRAVEL			(MessagePath.PROPERTIES_TRAVEL_NAME.getMessage(), 	MessagePath.PROPERTIES_TRAVEL.getMessage()),
	PVP				(MessagePath.PROPERTIES_PVP_NAME.getMessage(), 		MessagePath.PROPERTIES_PVP.getMessage()),
	PVE				(MessagePath.PROPERTIES_PVE_NAME.getMessage(), 		MessagePath.PROPERTIES_PVE.getMessage()),
	BUILD			(MessagePath.PROPERTIES_BUILD_NAME.getMessage(), 	MessagePath.PROPERTIES_BUILD.getMessage()),
	USE				(MessagePath.PROPERTIES_USE_NAME.getMessage(), 		MessagePath.PROPERTIES_USE.getMessage()),
	CHEST			(MessagePath.PROPERTIES_CHEST_NAME.getMessage(), 	MessagePath.PROPERTIES_CHEST.getMessage()),
	MOBS			(MessagePath.PROPERTIES_MOBS_NAME.getMessage(), 	MessagePath.PROPERTIES_MOBS.getMessage()),
	PORTALS			(MessagePath.PROPERTIES_PORTALS_NAME.getMessage(), 	MessagePath.PROPERTIES_PORTALS.getMessage()),
	ENTER			(MessagePath.PROPERTIES_ENTER_NAME.getMessage(), 	MessagePath.PROPERTIES_ENTER.getMessage()),
	EXIT			(MessagePath.PROPERTIES_EXIT_NAME.getMessage(), 	MessagePath.PROPERTIES_EXIT.getMessage()),
	SHOP			(MessagePath.PROPERTIES_SHOP_NAME.getMessage(), 	MessagePath.PROPERTIES_SHOP.getMessage()),
	ARENA		    (MessagePath.PROPERTIES_ARENA_NAME.getMessage(), 	MessagePath.PROPERTIES_ARENA.getMessage()),

	// Properties specifically for towns/capitals
	CAPTURE			(MessagePath.PROPERTIES_CAPTURE_NAME.getMessage(), 	MessagePath.PROPERTIES_CAPTURE.getMessage()),
	CLAIM			(MessagePath.PROPERTIES_CLAIM_NAME.getMessage(), 	MessagePath.PROPERTIES_CLAIM.getMessage()),
	UNCLAIM			(MessagePath.PROPERTIES_UNCLAIM_NAME.getMessage(), 	MessagePath.PROPERTIES_UNCLAIM.getMessage()),
	UPGRADE			(MessagePath.PROPERTIES_UPGRADE_NAME.getMessage(), 	MessagePath.PROPERTIES_UPGRADE.getMessage()),
	PLOTS			(MessagePath.PROPERTIES_PLOTS_NAME.getMessage(), 	MessagePath.PROPERTIES_PLOTS.getMessage()),
	
	// Properties for kingdoms
	PEACEFUL		(MessagePath.PROPERTIES_PEACEFUL_NAME.getMessage(), MessagePath.PROPERTIES_PEACEFUL.getMessage()),
	GOLEMS			(MessagePath.PROPERTIES_GOLEMS_NAME.getMessage(), 	MessagePath.PROPERTIES_GOLEMS.getMessage()),

	// Properties for membership
	JOIN			(MessagePath.PROPERTIES_JOIN_NAME.getMessage(), 	MessagePath.PROPERTIES_JOIN.getMessage()),
	LEAVE			(MessagePath.PROPERTIES_LEAVE_NAME.getMessage(), 	MessagePath.PROPERTIES_LEAVE.getMessage()),
	PROMOTE			(MessagePath.PROPERTIES_PROMOTE_NAME.getMessage(), 	MessagePath.PROPERTIES_PROMOTE.getMessage()),
	DEMOTE			(MessagePath.PROPERTIES_DEMOTE_NAME.getMessage(), 	MessagePath.PROPERTIES_DEMOTE.getMessage()),
	TRANSFER		(MessagePath.PROPERTIES_TRANSFER_NAME.getMessage(), MessagePath.PROPERTIES_TRANSFER.getMessage()),

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
