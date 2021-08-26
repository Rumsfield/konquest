package konquest.model;

import konquest.utility.MessagePath;

public enum KonTerritoryType {
	WILD		(MessagePath.TERRITORY_WILD.getMessage()),
    CAPITAL		(MessagePath.TERRITORY_CAPITAL.getMessage()),
    TOWN		(MessagePath.TERRITORY_TOWN.getMessage()),
    CAMP		(MessagePath.TERRITORY_CAMP.getMessage()),
    RUIN		(MessagePath.TERRITORY_RUIN.getMessage()),
    NEUTRAL		(MessagePath.TERRITORY_NEUTRAL.getMessage()),
    OTHER		(MessagePath.TERRITORY_OTHER.getMessage());

	private String label;
	
	KonTerritoryType(String label) {
		this.label = label;
	}
	
	public String getLabel() {
		return label;
	}
	
	/*
    public static KonTerritoryType parseString(String territoryType) {
        switch (territoryType.toLowerCase()) {
            case "wild": return KonTerritoryType.WILD;
            case "capital": return KonTerritoryType.CAPITAL;
            case "town": return KonTerritoryType.TOWN;
            case "camp": return KonTerritoryType.CAMP;
            case "ruin": return KonTerritoryType.RUIN;
            case "neutral": return KonTerritoryType.NEUTRAL;
            case "other": return KonTerritoryType.OTHER;
        }
        return null;
    }
    */
}
