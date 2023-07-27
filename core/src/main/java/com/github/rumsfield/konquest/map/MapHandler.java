package com.github.rumsfield.konquest.map;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;

import java.util.Date;
import java.util.HashMap;

//TODO: Make this class into a listener, make events for territory updates, deletes, etc

//TODO: Figure out way to have created and removed areas update on web page without refresh

public class MapHandler {

	private final Konquest konquest;
	private final HashMap<String,Renderable> renderers;

	static final int sanctuaryColor = 0x646464;
	static final int ruinColor = 0x242424;
	static final int campColor = 0xa3a10a;
	static final int lineDefaultColor = 0x000000;
	static final int lineCapitalColor = 0x8010d0;
	
	public MapHandler(Konquest konquest) {
		this.konquest = konquest;
		renderers = new HashMap<>();
		renderers.put("Dynmap",new DynmapRender(konquest));
		renderers.put("BlueMap",new BlueMapRender(konquest));
	}
	
	public void initialize() {
		for(Renderable ren : renderers.values()) {
			ren.initialize();
		}
	}

	/* Rendering Methods */
	//TODO: Rename these methods
	public void drawUpdateTerritory(KonKingdom kingdom) {
		for(Renderable ren : renderers.values()) {
			ren.drawUpdate(kingdom);
		}
	}

	public void drawUpdateTerritory(KonTerritory territory) {
		for(Renderable ren : renderers.values()) {
			ren.drawUpdate(territory);
		}
	}

	public void drawRemoveTerritory(KonTerritory territory) {
		for(Renderable ren : renderers.values()) {
			ren.drawRemove(territory);
		}
	}
	
	public void drawLabel(KonTerritory territory) {
		for(Renderable ren : renderers.values()) {
			ren.drawLabel(territory);
		}
	}
	
	public void postBroadcast(String message) {
		for(Renderable ren : renderers.values()) {
			ren.postBroadcast(message);
		}
	}
	
	public void drawAllTerritories() {
		Date start = new Date();
		// Sanctuaries
		for (KonSanctuary sanctuary : konquest.getSanctuaryManager().getSanctuaries()) {
			for(Renderable ren : renderers.values()) {
				ren.drawUpdate(sanctuary);
			}
		}
		// Ruins
		for (KonRuin ruin : konquest.getRuinManager().getRuins()) {
			for(Renderable ren : renderers.values()) {
				ren.drawUpdate(ruin);
			}
		}
		// Camps
		for (KonCamp camp : konquest.getCampManager().getCamps()) {
			for(Renderable ren : renderers.values()) {
				ren.drawUpdate(camp);
			}
		}
		// Kingdoms
		for (KonKingdom kingdom : konquest.getKingdomManager().getKingdoms()) {
			for(Renderable ren : renderers.values()) {
				ren.drawUpdate(kingdom);
			}
		}
		Date end = new Date();
		int time = (int)(end.getTime() - start.getTime());
		ChatUtil.printDebug("Rendering all territories in maps took "+time+" ms");
	}

	public void drawAllTerritories(String rendererName) {
		Date start = new Date();
		Renderable renderer = renderers.get(rendererName);
		if(renderer == null) {
			ChatUtil.printDebug("Failed to draw with unknown renderer "+rendererName);
			return;
		}
		// Sanctuaries
		for (KonSanctuary sanctuary : konquest.getSanctuaryManager().getSanctuaries()) {
			renderer.drawUpdate(sanctuary);
		}
		// Ruins
		for (KonRuin ruin : konquest.getRuinManager().getRuins()) {
			renderer.drawUpdate(ruin);
		}
		// Camps
		for (KonCamp camp : konquest.getCampManager().getCamps()) {
			renderer.drawUpdate(camp);
		}
		// Kingdoms
		for (KonKingdom kingdom : konquest.getKingdomManager().getKingdoms()) {
			renderer.drawUpdate(kingdom);
		}
		Date end = new Date();
		int time = (int)(end.getTime() - start.getTime());
		ChatUtil.printDebug("Rendering all territories with "+rendererName+" took "+time+" ms");
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

	static boolean isTerritoryInvalid(KonTerritory territory) {
		boolean result = false;
		switch (territory.getTerritoryType()) {
			case SANCTUARY:
			case RUIN:
			case CAMP:
			case CAPITAL:
			case TOWN:
				result = true;
				break;
			default:
				break;
		}
		return !result;
	}

}
