package konquest.api.model;

import konquest.utility.MessagePath;

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
	WILD		(MessagePath.TERRITORY_WILD.getMessage()),
	/**
	 * Kingdom capital
	 */
	CAPITAL		(MessagePath.TERRITORY_CAPITAL.getMessage()),
	/**
	 * Town
	 */
	TOWN		(MessagePath.TERRITORY_TOWN.getMessage()),
	/**
	 * Barbarian camp
	 */
	CAMP		(MessagePath.TERRITORY_CAMP.getMessage()),
	/**
	 * Ruin
	 */
	RUIN		(MessagePath.TERRITORY_RUIN.getMessage()),
	/**
	 * Neutral land, not yet implemented
	 */
	NEUTRAL		(MessagePath.TERRITORY_NEUTRAL.getMessage()),
	/**
	 * Unknown territory
	 */
	OTHER		(MessagePath.TERRITORY_OTHER.getMessage());

	private String label;
	
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
