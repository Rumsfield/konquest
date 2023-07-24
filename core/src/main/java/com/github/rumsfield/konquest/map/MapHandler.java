package com.github.rumsfield.konquest.map;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;

import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

//TODO: Make this class into a listener, make events for territory updates, deletes, etc

//TODO: Figure out way to have created and removed areas update on web page without refresh

public class MapHandler {

	private final Konquest konquest;
	private final DynmapRender dynmapRenderer;

	static final int sanctuaryColor = 0x646464;
	static final int ruinColor = 0x242424;
	static final int campColor = 0xa3a10a;
	static final int lineDefaultColor = 0x000000;
	static final int lineCapitalColor = 0x8010d0;
	
	public MapHandler(Konquest konquest) {
		this.konquest = konquest;
		this.dynmapRenderer = new DynmapRender(konquest);
	}
	
	public void initialize() {
		dynmapRenderer.initialize();
	}

	public static int getWebColor(KonTerritory territory) {
		int result = 0xFFFFFF;
		int webColor = territory.getKingdom().getWebColor();
		if(webColor == -1) {
			int hash = territory.getKingdom().getName().hashCode();
			result = hash & 0xFFFFFF;
		} else {
			result = webColor;
		}
		return result;
	}

	/* Rendering Methods */
	//TODO: Rename these methods
	public void drawDynmapUpdateTerritory(KonKingdom kingdom) {
		dynmapRenderer.drawUpdate(kingdom);
	}

	public void drawDynmapUpdateTerritory(KonTerritory territory) {
		dynmapRenderer.drawUpdate(territory);
	}

	public void drawDynmapRemoveTerritory(KonTerritory territory) {
		dynmapRenderer.drawRemove(territory);
	}
	
	public void drawDynmapLabel(KonTerritory territory) {
		dynmapRenderer.drawLabel(territory);
	}
	
	public void postDynmapBroadcast(String message) {
		dynmapRenderer.postBroadcast(message);
	}
	
	public void drawDynmapAllTerritories() {
		Date start = new Date();
		// Sanctuaries
		for (KonSanctuary sanctuary : konquest.getSanctuaryManager().getSanctuaries()) {
			dynmapRenderer.drawUpdate(sanctuary);
		}
		// Ruins
		for (KonRuin ruin : konquest.getRuinManager().getRuins()) {
			dynmapRenderer.drawUpdate(ruin);
		}
		// Camps
		for (KonCamp camp : konquest.getCampManager().getCamps()) {
			dynmapRenderer.drawUpdate(camp);
		}
		// Kingdoms
		for (KonKingdom kingdom : konquest.getKingdomManager().getKingdoms()) {
			dynmapRenderer.drawUpdate(kingdom);
		}
		Date end = new Date();
		int time = (int)(end.getTime() - start.getTime());
		ChatUtil.printDebug("Rendering all territories in maps took "+time+" ms");
	}

}
