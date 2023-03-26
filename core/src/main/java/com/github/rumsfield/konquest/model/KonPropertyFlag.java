package com.github.rumsfield.konquest.model;

import com.github.rumsfield.konquest.utility.MessagePath;

import java.util.HashSet;
import java.util.Set;

public enum KonPropertyFlag {
	// Note that the name field must match the enum name so that the admin flag command can match them.
	// Descriptions come from MessagePath so that they can be translated.

	// Properties for territories
	TRAVEL			("Travel", 	MessagePath.PROPERTIES_TRAVEL.getMessage()), // Done in TravelCommand
	PVP				("PvP", 		MessagePath.PROPERTIES_PVP.getMessage()), // Done in EntityListener
	PVE				("PvE", 		MessagePath.PROPERTIES_PVE.getMessage()), // Done in EntityListener
	BUILD			("Build", 	MessagePath.PROPERTIES_BUILD.getMessage()), // Done in BlockListener
	USE				("Use", 		MessagePath.PROPERTIES_USE.getMessage()), // Done in InventoryListener, ...
	CHEST			("Chest", 	MessagePath.PROPERTIES_CHEST.getMessage()),
	MOBS			("Mobs", 		MessagePath.PROPERTIES_MOBS.getMessage()), // Done in EntityListener
	PORTALS			("Portals", 	MessagePath.PROPERTIES_PORTALS.getMessage()), // Done in WorldListener
	ENTER			("Enter", 	MessagePath.PROPERTIES_ENTER.getMessage()), // Done in PlayerListener
	EXIT			("Exit", 		MessagePath.PROPERTIES_EXIT.getMessage()), // Done in PlayerListener
	
	// Properties specifically for towns/capitals
	CAPTURE			("Capture", 	MessagePath.PROPERTIES_CAPTURE.getMessage()), // Done in BlockListener
	CLAIM			("Claim", 	MessagePath.PROPERTIES_CLAIM.getMessage()), // Done in TerritoryManager
	UNCLAIM			("Unclaim", 	MessagePath.PROPERTIES_UNCLAIM.getMessage()), // Done in TerritoryManager
	UPGRADE			("Upgrade", 	MessagePath.PROPERTIES_UPGRADE.getMessage()), // Done in TownManagementMenuWrapper
	PLOTS			("Plots", 	MessagePath.PROPERTIES_PLOTS.getMessage()), // Done in TownManagementMenuWrapper
	
	// Properties for kingdoms
	NEUTRAL			("Neutral", 	MessagePath.PROPERTIES_NEUTRAL.getMessage()),
	GOLEMS			("Golems", 	MessagePath.PROPERTIES_GOLEMS.getMessage()), // Done in KonTown
	
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
	
	public static Set<String> getFlagStrings() {
		Set<String> result = new HashSet<>();
		for(KonPropertyFlag p : KonPropertyFlag.values()) {
			result.add(p.toString());
		}
		return result;
	}
}
