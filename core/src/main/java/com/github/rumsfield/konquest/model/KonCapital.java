package com.github.rumsfield.konquest.model;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestCapital;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.utility.CorePath;

import org.bukkit.Location;


public class KonCapital extends KonTown implements KonquestCapital {

	public KonCapital(Location loc, KonKingdom kingdom, Konquest konquest) {
		super(loc, kingdom.getName()+" "+konquest.getCore().getString(CorePath.KINGDOMS_CAPITAL_SUFFIX.getPath()), kingdom, konquest);
		
	}
	
	public KonCapital(Location loc, String name, KonKingdom kingdom, Konquest konquest) {
		super(loc, name, kingdom, konquest);
	}
	
	public void updateName() {
		setName(getKingdom().getName()+" "+getKonquest().getCore().getString(CorePath.KINGDOMS_CAPITAL_SUFFIX.getPath()));
		updateBarTitle();
	}
	
	@Override
	public KonquestTerritoryType getTerritoryType() {
		return KonquestTerritoryType.CAPITAL;
	}

}
