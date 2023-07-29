package com.github.rumsfield.konquest.map;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;

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

	static boolean isEnableKingdoms = true;
	static boolean isEnableCamps = true;
	static boolean isEnableSanctuaries = true;
	static boolean isEnableRuins = true;

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
		isEnableKingdoms = konquest.getCore().getBoolean(CorePath.INTEGRATION_MAP_OPTIONS_ENABLE_KINGDOMS.getPath());
		isEnableCamps = konquest.getCore().getBoolean(CorePath.INTEGRATION_MAP_OPTIONS_ENABLE_CAMPS.getPath());
		isEnableSanctuaries = konquest.getCore().getBoolean(CorePath.INTEGRATION_MAP_OPTIONS_ENABLE_SANCTUARIES.getPath());
		isEnableRuins = konquest.getCore().getBoolean(CorePath.INTEGRATION_MAP_OPTIONS_ENABLE_RUINS.getPath());
	}

	/* Rendering Methods */

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

	static boolean isTerritoryDisabled(KonTerritory territory) {
		boolean result = false;
		switch (territory.getTerritoryType()) {
			case SANCTUARY:
				result = isEnableSanctuaries;
				break;
			case RUIN:
				result = isEnableRuins;
				break;
			case CAMP:
				result = isEnableCamps;
				break;
			case CAPITAL:
			case TOWN:
				result = isEnableKingdoms;
				break;
			default:
				break;
		}
		return !result;
	}

	static String getGroupLabel(KonTerritory territory) {
		String result = "Konquest";
		switch (territory.getTerritoryType()) {
			case SANCTUARY:
				result = "Konquest Sanctuaries";
				break;
			case RUIN:
				result = "Konquest Ruins";
				break;
			case CAMP:
				result = "Konquest Barbarian Camps";
				break;
			case CAPITAL:
			case TOWN:
				result = "Konquest Kingdoms";
				break;
			default:
				break;
		}
		return result;
	}

	static String getIconLabel(KonTerritory territory) {
		String result = "Konquest";
		switch (territory.getTerritoryType()) {
			case SANCTUARY:
				result = MessagePath.MAP_SANCTUARY.getMessage()+" "+territory.getName();
				break;
			case RUIN:
				result = MessagePath.MAP_RUIN.getMessage()+" "+territory.getName();
				break;
			case CAMP:
				result = MessagePath.MAP_BARBARIAN.getMessage()+" "+territory.getName();
				break;
			case CAPITAL:
				result = territory.getKingdom().getCapital().getName();
				break;
			case TOWN:
				result = territory.getKingdom().getName()+" "+territory.getName();
				break;
			default:
				break;
		}
		return result;
	}

	static String getAreaLabel(KonTerritory territory) {
		String result = "Konquest";
		switch (territory.getTerritoryType()) {
			case SANCTUARY:
				KonSanctuary sanctuary = (KonSanctuary)territory;
				int numTemplates = sanctuary.getTemplates().size();
				result = "<p>"+
						"<b>"+sanctuary.getName() + "</b><br>" +
						MessagePath.MAP_SANCTUARY.getMessage() + "<br>" +
						MessagePath.MAP_TEMPLATES.getMessage() + ": " + numTemplates + "<br>" +
						"</p>";
				break;
			case RUIN:
				KonRuin ruin = (KonRuin)territory;
				int numCriticals = ruin.getMaxCriticalHits();
				int numSpawns = ruin.getSpawnLocations().size();
				result = "<p>"+
						"<b>"+ruin.getName() + "</b><br>" +
						MessagePath.MAP_RUIN.getMessage() + "<br>" +
						MessagePath.MAP_CRITICAL_HITS.getMessage() + ": " + numCriticals + "<br>" +
						MessagePath.MAP_GOLEM_SPAWNS.getMessage() + ": " + numSpawns + "<br>" +
						"</p>";
				break;
			case CAMP:
				KonCamp camp = (KonCamp)territory;
				result = "<p>"+
						"<b>"+camp.getName() + "</b><br>" +
						MessagePath.MAP_BARBARIANS.getMessage() + "<br>" +
						"</p>";
				break;
			case CAPITAL:
				KonCapital capital = (KonCapital)territory;
				String capitalLordName = "-";
				if(capital.getPlayerLord() != null) {
					capitalLordName = capital.getPlayerLord().getName();
				}
				int numAllKingdomPlayers = territory.getKingdom().getNumMembers();
				int numKingdomTowns = territory.getKingdom().getTowns().size();
				int numKingdomLand = 0;
				for(KonTown town : territory.getKingdom().getCapitalTowns()) {
					numKingdomLand += town.getNumLand();
				}
				result = "<p>"+
						"<b>"+capital.getName() + "</b><br>" +
						MessagePath.MAP_LORD.getMessage() + ": " + capitalLordName + "<br>" +
						MessagePath.MAP_LAND.getMessage() + ": " + capital.getNumLand() + "<br>" +
						MessagePath.MAP_POPULATION.getMessage() + ": " + capital.getNumResidents() + "<br>" +
						"</p>"+
						"<p>"+
						"<b>"+capital.getKingdom().getName() + "</b><br>" +
						MessagePath.MAP_TOWNS.getMessage() + ": " + numKingdomTowns + "<br>" +
						MessagePath.MAP_LAND.getMessage() + ": " + numKingdomLand + "<br>" +
						MessagePath.MAP_PLAYERS.getMessage() + ": " + numAllKingdomPlayers + "<br>" +
						"</p>";
				break;
			case TOWN:
				KonTown town = (KonTown)territory;
				String townLordName = "-";
				if(town.getPlayerLord() != null) {
					townLordName = town.getPlayerLord().getName();
				}
				result = "<p>"+
						"<b>"+town.getName() + "</b><br>" +
						MessagePath.MAP_KINGDOM.getMessage() + ": " + town.getKingdom().getName() + "<br>" +
						MessagePath.MAP_LORD.getMessage() + ": " + townLordName + "<br>" +
						MessagePath.MAP_LAND.getMessage() + ": " + town.getNumLand() + "<br>" +
						MessagePath.MAP_POPULATION.getMessage() + ": " + town.getNumResidents() + "<br>" +
						"</p>";
				break;
			default:
				break;
		}
		return result;
	}

}
