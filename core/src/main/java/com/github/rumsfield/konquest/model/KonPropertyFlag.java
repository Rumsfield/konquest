package com.github.rumsfield.konquest.model;

import java.util.HashSet;
import java.util.Set;

public enum KonPropertyFlag {
//TODO: Replace these strings with MessagePaths
	
	// Properties for territories
	TRAVEL			("Travel", 		"Allow travel to this territory"), // Done in TravelCommand
	PVP				("PvP", 		"Allow player damage"), // Done in EntityListener
	PVE				("PvE", 		"Allow entity damage"), // Done in EntityListener
	BUILD			("Build", 		"Allow block edits"), // Done in BlockListener
	USE				("Use", 		"Allow using blocks"), // Done in InventoryListener, ...
	CHEST			("Chest", 		"Allow using containers"),
	MOBS			("Mobs", 		"Allow mobs to spawn"), // Done in EntityListener
	PORTALS			("Portals", 	"Allow players to use portals"), // Done in WorldListener
	ENTER			("Enter", 		"Allow players to enter"), // Done in PlayerListener
	EXIT			("Exit", 		"Allow players to exit"), // Done in PlayerListener
	
	// Properties specifically for towns/capitals
	CAPTURE			("Capture", 	"Allow this town to be captured"), // Done in BlockListener
	CLAIM			("Claim", 		"Allow players to claim land"), // Done in TerritoryManager
	UNCLAIM			("Unclaim", 	"Allow players to unclaim land"), // Done in TerritoryManager
	UPGRADE			("Upgrade", 	"Allow players to upgrade this town"), // Done in TownManagementMenuWrapper
	PLOTS			("Plots", 		"Allow players to set town plots"), // Done in TownManagementMenuWrapper
	
	// Properties for kingdoms
	NEUTRAL			("Neutral", 	"Make this kingdom only peaceful"),
	GOLEMS			("Golems", 		"Enable iron golems to attack enemies"), // Done in KonTown
	
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
