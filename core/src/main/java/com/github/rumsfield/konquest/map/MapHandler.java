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
		this.renderers = new HashMap<>();
	}
	
	public void initialize() {
		if (konquest.getIntegrationManager().getDynmap().isEnabled()) {
			renderers.put("Dynmap",new DynmapRender(konquest));
		}
		if (konquest.getIntegrationManager().getBlueMap().isEnabled()) {
			renderers.put("BlueMap",new BlueMapRender(konquest));
		}
		if (konquest.getIntegrationManager().getSquaremap().isEnabled()) {
			renderers.put("squaremap",new SquaremapRender(konquest));
		}
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
		boolean isValid = false;
		switch (territory.getTerritoryType()) {
			case SANCTUARY:
			case RUIN:
			case CAMP:
			case TOWN:
				isValid = true;
				break;
			case CAPITAL:
				// Capitals are only valid for created kingdoms (not barbarians or neutrals)
				if (territory.getKingdom().isCreated()) {
					isValid = true;
				}
				break;
			default:
				break;
		}
		return !isValid;
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
		String bodyBegin = "<body style=\"background-color:#fff0cc;font-family:Georgia;\">";
		String nameHeaderFormat = "<h2 style=\"color:#de791b;\">%s</h2>";
		String typeHeaderFormat = "<h3 style=\"color:#8048b8;\">%s</h3>";
		String propertyLineFormat = "%s: %s <br>";
		StringBuilder labelMaker = new StringBuilder();
		switch (territory.getTerritoryType()) {
			case SANCTUARY:
				KonSanctuary sanctuary = (KonSanctuary)territory;
				int numTemplates = sanctuary.getTemplates().size();
				result = labelMaker.append(bodyBegin)
						.append(String.format(nameHeaderFormat, sanctuary.getName()))
						.append(String.format(typeHeaderFormat, MessagePath.MAP_SANCTUARY.getMessage()))
						.append("<p>")
						.append(String.format(propertyLineFormat, MessagePath.MAP_TEMPLATES.getMessage(), numTemplates))
						.append("</p>")
						.append("</body>")
						.toString();
				break;
			case RUIN:
				KonRuin ruin = (KonRuin)territory;
				int numCriticals = ruin.getMaxCriticalHits();
				int numSpawns = ruin.getSpawnLocations().size();
				result = labelMaker.append(bodyBegin)
						.append(String.format(nameHeaderFormat, ruin.getName()))
						.append(String.format(typeHeaderFormat, MessagePath.MAP_RUIN.getMessage()))
						.append("<p>")
						.append(String.format(propertyLineFormat, MessagePath.MAP_CRITICAL_HITS.getMessage(), numCriticals))
						.append(String.format(propertyLineFormat, MessagePath.MAP_GOLEM_SPAWNS.getMessage(), numSpawns))
						.append("</p>")
						.append("</body>")
						.toString();
				break;
			case CAMP:
				KonCamp camp = (KonCamp)territory;
				result = labelMaker.append(bodyBegin)
						.append(String.format(nameHeaderFormat, camp.getName()))
						.append(String.format(typeHeaderFormat, MessagePath.MAP_BARBARIANS.getMessage()))
						.append("<p>")
						.append(String.format(propertyLineFormat, MessagePath.LABEL_PLAYER.getMessage(), camp.getOwner().getName()))
						.append("</p>")
						.append("</body>")
						.toString();
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
				result = labelMaker.append(bodyBegin)
						.append(String.format(nameHeaderFormat, capital.getName()))
						.append(String.format(typeHeaderFormat, MessagePath.MAP_CAPITAL.getMessage()))
						.append("<p>")
						.append(String.format(propertyLineFormat, MessagePath.MAP_KINGDOM.getMessage(), capital.getKingdom().getName()))
						.append(String.format(propertyLineFormat, MessagePath.MAP_LORD.getMessage(), capitalLordName))
						.append(String.format(propertyLineFormat, MessagePath.MAP_LAND.getMessage(), capital.getNumLand()))
						.append(String.format(propertyLineFormat, MessagePath.MAP_POPULATION.getMessage(), capital.getNumResidents()))
						.append("</p>")
						.append(String.format(typeHeaderFormat, MessagePath.MAP_KINGDOM.getMessage()))
						.append("<p>")
						.append(String.format(propertyLineFormat, MessagePath.MAP_TOWNS.getMessage(), numKingdomTowns))
						.append(String.format(propertyLineFormat, MessagePath.MAP_LAND.getMessage(), numKingdomLand))
						.append(String.format(propertyLineFormat, MessagePath.MAP_PLAYERS.getMessage(), numAllKingdomPlayers))
						.append("</p>")
						.append("</body>")
						.toString();
				break;
			case TOWN:
				KonTown town = (KonTown)territory;
				String townLordName = "-";
				if(town.getPlayerLord() != null) {
					townLordName = town.getPlayerLord().getName();
				}
				result = labelMaker.append(bodyBegin)
						.append(String.format(nameHeaderFormat, town.getName()))
						.append(String.format(typeHeaderFormat, MessagePath.MAP_TOWN.getMessage()))
						.append("<p>")
						.append(String.format(propertyLineFormat, MessagePath.MAP_KINGDOM.getMessage(), town.getKingdom().getName()))
						.append(String.format(propertyLineFormat, MessagePath.MAP_LORD.getMessage(), townLordName))
						.append(String.format(propertyLineFormat, MessagePath.MAP_LAND.getMessage(), town.getNumLand()))
						.append(String.format(propertyLineFormat, MessagePath.MAP_POPULATION.getMessage(), town.getNumResidents()))
						.append("</p>")
						.append("</body>")
						.toString();
				break;
			default:
				break;
		}
		return result;
	}

}
