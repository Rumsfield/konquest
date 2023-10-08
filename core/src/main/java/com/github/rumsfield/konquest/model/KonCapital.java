package com.github.rumsfield.konquest.model;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestCapital;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.utility.CorePath;

import org.bukkit.Location;


public class KonCapital extends KonTown implements KonquestCapital {

	public KonCapital(Location loc, KonKingdom kingdom, Konquest konquest) {
		super(loc, kingdom.getName(), kingdom, konquest);
		updateName();
	}
	
	public KonCapital(Location loc, String name, KonKingdom kingdom, Konquest konquest) {
		super(loc, name, kingdom, konquest);
	}
	
	public void updateName() {
		setName(getCapitalName());
		updateBarTitle();
	}

	private String getCapitalName() {
		boolean isPrefix = getKonquest().getCore().getBoolean(CorePath.KINGDOMS_CAPITAL_PREFIX_SWAP.getPath(),false);
		String suffix = getKonquest().getCore().getString(CorePath.KINGDOMS_CAPITAL_SUFFIX.getPath(),"");
		String name = getKingdom().getName();
		if(isPrefix) {
			// Apply descriptor before name
			return suffix+" "+name;
		} else {
			// Apply descriptor after name
			return name+" "+suffix;
		}
	}
	
	@Override
	public KonquestTerritoryType getTerritoryType() {
		return KonquestTerritoryType.CAPITAL;
	}

}
