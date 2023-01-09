package com.github.rumsfield.konquest.api.model;

/**
 * The types of territory in Konquest.
 * 
 * @author Rumsfield
 *
 */
public enum KonquestTerritoryType {
	/**
	 * The wild, which is no territory
	 */
	WILD		("territory.wild"),
	/**
	 * Kingdom capital
	 */
	CAPITAL		("territory.capital"),
	/**
	 * Town
	 */
	TOWN		("territory.town"),
	/**
	 * Barbarian camp
	 */
	CAMP		("territory.camp"),
	/**
	 * Ruin
	 */
	RUIN		("territory.ruin"),
	/**
	 * Sanctuary land
	 */
	SANCTUARY	("territory.sanctuary");

	private final String label;
	
	KonquestTerritoryType(String label) {
		this.label = label;
	}
	
	/**
	 * Gets the display label from the Konquest language files
	 * 
	 * @return The label
	 */
	public String getLabel() {
		return label;
	}

}
