package konquest.model;

import konquest.Konquest;

import org.bukkit.Chunk;
import org.bukkit.Location;

public class KonCapital extends KonTerritory{
	
	public KonCapital(Location loc, KonKingdom kingdom, Konquest konquest) {
		super(loc, kingdom.getName()+" "+konquest.getConfigManager().getConfig("core").getString("core.kingdoms.capital_suffix"), kingdom, KonTerritoryType.CAPITAL, konquest);
	}
	
	public KonCapital(Location loc, String name, KonKingdom kingdom, Konquest konquest) {
		super(loc, name, kingdom, KonTerritoryType.CAPITAL, konquest);
	}

	
	@Override
	public boolean addChunk(Chunk chunk) {
		addPoint(getKonquest().toPoint(chunk));
		return true;
	}
	
	@Override
	public int initClaim() {
		addChunk(getCenterLoc().getChunk());
		return 0;
	}

}
