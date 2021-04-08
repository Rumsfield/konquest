package konquest.model;

public enum KonTerritoryType {
	WILD,
    CAPITAL,
    TOWN,
    CAMP,
    RUIN,
    NEUTRAL,
    OTHER;

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
}
