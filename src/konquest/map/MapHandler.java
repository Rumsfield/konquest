package konquest.map;

import java.util.Date;

import org.bukkit.Bukkit;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;

import konquest.Konquest;
import konquest.model.KonCamp;
import konquest.model.KonCapital;
import konquest.model.KonKingdom;
import konquest.model.KonRuin;
import konquest.model.KonTerritory;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class MapHandler {
	
	private Konquest konquest;
	private boolean isEnabled;
	
	private static DynmapAPI dapi = null;
	
	private final int ruinColor = 0x242424;
	private final int campColor = 0xa3a10a;
	private final int capitalColor = 0xb942f5;
	private final int townColor = 0xed921a;
	private final int lineColor = 0x000000; //0xFF2828;
	
	public MapHandler(Konquest konquest) {
		this.konquest = konquest;
		this.isEnabled = false;
	}
	
	public void initialize() {
		if (Bukkit.getPluginManager().getPlugin("dynmap") != null) {
			dapi = (DynmapAPI) Bukkit.getServer().getPluginManager().getPlugin("dynmap");
			isEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.integration.dynmap",false);
			if(isEnabled) {
				ChatUtil.printConsoleAlert("Successfully registered Dynmap.");
			} else {
				ChatUtil.printConsoleAlert("Disabled Dynmap integration from core config settings.");
			}
		}
	}
	
	public boolean isEnabled() {
		return isEnabled;
	}
	
	/*
	 * Dynmap Area ID formats:
	 * Ruins
	 * 		MarkerSet Group:  konquest.marker.ruin
	 * 		AreaMarker Areas: konquest.area.ruin.<name>
	 * Camps
	 * 		MarkerSet Group:  konquest.marker.camp
	 * 		AreaMarker Areas: konquest.area.camp.<name>
	 * Kingdoms
	 * 		MarkerSet Group:  konquest.marker.<kingdom>
	 * 		AreaMarker Areas: konquest.area.<kingdom>.capital
	 * 			              konquest.area.<kingdom>.<town>
	 */
	
	//TODO: Make land area (AreaMarkers) descriptions HTML markup
	
	//TODO: Make this class into a listener, make events for territory updates, deletes, etc
	
	//TODO: Figure out way to have created and removed areas update on web page without refresh
	
	/**
	 * Draws a territory with Dynmap, when created or updated
	 * @param territory
	 */
	public void drawDynmapUpdateTerritory(KonTerritory territory) {
		if (!isEnabled) {
			return;
		}
		
		if (!isTerritoryValid(territory)) {
			ChatUtil.printDebug("Could not draw territory "+territory.getName()+" with invalid type, "+territory.getTerritoryType().toString());
			return;
		}

		String groupId = getGroupId(territory);
		String groupLabel = getGroupLabel(territory);
		String areaId = getAreaId(territory);
		String areaLabel = getAreaLabel(territory);
		int areaColor = getAreaColor(territory);
		String iconId = getIconId(territory);
		String iconLabel = getIconLabel(territory);
		MarkerIcon icon = getIconMarker(territory);
		
		MarkerSet territoryGroup = dapi.getMarkerAPI().getMarkerSet(groupId);
		if (territoryGroup == null) {
			territoryGroup = dapi.getMarkerAPI().createMarkerSet(groupId, groupLabel, dapi.getMarkerAPI().getMarkerIcons(), false);
		}
		AreaMarker territoryArea = territoryGroup.findAreaMarker(areaId);
		AreaTerritory drawArea = new AreaTerritory(territory);
		if (territoryArea == null) {
			// Area does not exist, create new
			territoryArea = territoryGroup.createAreaMarker(areaId, areaLabel, true, drawArea.getWorldName(), drawArea.getXCorners(), drawArea.getZCorners(), false);
			if (territoryArea != null) {
				territoryArea.setFillStyle(0.5, areaColor);
				territoryArea.setLineStyle(1, 1, lineColor);
			}
			ChatUtil.printDebug("Drawing new Dynmap area of territory "+territory.getName());
		} else {
			// Area already exists, update corners and label
			territoryArea.setCornerLocations(drawArea.getXCorners(), drawArea.getZCorners());
			territoryArea.setLabel(areaLabel,true);
			ChatUtil.printDebug("Updating Dynmap area corners of territory "+territory.getName());
		}
		Marker territoryIcon = territoryGroup.findMarker(iconId);
		if (territoryIcon == null) {
			// Icon does not exist, create ne
			territoryIcon = territoryGroup.createMarker(iconId, iconLabel, true, drawArea.getWorldName(), drawArea.getCenterX(), drawArea.getCenterY(), drawArea.getCenterZ(), icon, false);
		} else {
			// Icon already exists, update label
			territoryIcon.setLabel(iconLabel);
		}
		
	}
	
	/**
	 * Deletes a territory with Dynmap, when destroyed or removed
	 * @param territory
	 */
	public void drawDynmapRemoveTerritory(KonTerritory territory) {
		if (!isEnabled) {
			return;
		}
		
		if (!isTerritoryValid(territory)) {
			ChatUtil.printDebug("Could not delete territory "+territory.getName()+" with invalid type, "+territory.getTerritoryType().toString());
			return;
		}
		
		String groupId = getGroupId(territory);
		String areaId = getAreaId(territory);
		
		MarkerSet territoryGroup = dapi.getMarkerAPI().getMarkerSet(groupId);
		if (territoryGroup != null) {
			AreaMarker territoryArea = territoryGroup.findAreaMarker(areaId);
			if (territoryArea != null) {
				// Delete area from group
				territoryArea.deleteMarker();
				ChatUtil.printDebug("Removing Dynmap area of territory "+territory.getName());
			}
			if (territoryGroup.getAreaMarkers().isEmpty()) {
				// Delete group if no more areas
				territoryGroup.deleteMarkerSet();
				ChatUtil.printDebug("Removing Dynmap group of territory "+territory.getName());
			}
		}
		
	}
	
	public void postDynmapBroadcast(String message) {
		if (!isEnabled) {
			return;
		}
		dapi.sendBroadcastToWeb("Konquest", message);
	}
	
	public void drawDynmapAllTerritories() {
		if (!isEnabled) {
			return;
		}
		Date start = new Date();
		
		// Ruins
		for (KonRuin ruin : konquest.getRuinManager().getRuins()) {
			drawDynmapUpdateTerritory(ruin);
		}
		
		// Camps
		for (KonCamp camp : konquest.getKingdomManager().getCamps()) {
			drawDynmapUpdateTerritory(camp);
		}
		
		// Kingdoms
		for (KonKingdom kingdom : konquest.getKingdomManager().getKingdoms()) {
			drawDynmapUpdateTerritory(kingdom.getCapital());
			// Towns
			for (KonTown town : kingdom.getTowns()) {
				drawDynmapUpdateTerritory(town);
			}
		}
		
		Date end = new Date();
		int time = (int)(end.getTime() - start.getTime());
		ChatUtil.printDebug("Drawing all territory in Dynmap took "+time+" ms");
	}
	
	public void drawDynmapLabel(KonTerritory territory) {
		if (!isEnabled) {
			return;
		}
		if (!isTerritoryValid(territory)) {
			ChatUtil.printDebug("Could not update label for territory "+territory.getName()+" with invalid type, "+territory.getTerritoryType().toString());
			return;
		}
		String groupId = getGroupId(territory);
		String areaId = getAreaId(territory);
		String areaLabel = getAreaLabel(territory);
		MarkerSet territoryGroup = dapi.getMarkerAPI().getMarkerSet(groupId);
		if (territoryGroup != null) {
			AreaMarker territoryArea = territoryGroup.findAreaMarker(areaId);
			if (territoryArea != null) {
				// Area already exists, update label
				territoryArea.setLabel(areaLabel,true);
			}
		}
	}
	
	private String getGroupId(KonTerritory territory) {
		String result = "konquest";
		switch (territory.getTerritoryType()) {
			case RUIN:
				result = result+".marker.ruin";
				break;
			case CAMP:
				result = result+".marker.camp";
				break;
			case CAPITAL:
				result = result+".marker."+territory.getKingdom().getName().toLowerCase();
				break;
			case TOWN:
				result = result+".marker."+territory.getKingdom().getName().toLowerCase();
				break;
			default:
				break;
		}
		return result;
	}
	
	private String getGroupLabel(KonTerritory territory) {
		String result = "Konquest";
		switch (territory.getTerritoryType()) {
			case RUIN:
				result = "Konquest Ruins";
				break;
			case CAMP:
				result = "Konquest Barbarian Camps";
				break;
			case CAPITAL:
				result = "Konquest Kingdom "+territory.getKingdom().getName();
				break;
			case TOWN:
				result = "Konquest Kingdom "+territory.getKingdom().getName();
				break;
			default:
				break;
		}
		return result;
	}
	
	private String getAreaId(KonTerritory territory) {
		String result = "konquest";
		switch (territory.getTerritoryType()) {
			case RUIN:
				result = result+".area.ruin."+territory.getName().toLowerCase();
				break;
			case CAMP:
				result = result+".area.camp."+territory.getName().toLowerCase();
				break;
			case CAPITAL:
				result = result+".area."+territory.getKingdom().getName().toLowerCase()+".capital";
				break;
			case TOWN:
				result = result+".area."+territory.getKingdom().getName().toLowerCase()+"."+territory.getName().toLowerCase();
				break;
			default:
				break;
		}
		return result;
	}
	
	private String getAreaLabel(KonTerritory territory) {
		String result = "Konquest";
		switch (territory.getTerritoryType()) {
			case RUIN:
				KonRuin ruin = (KonRuin)territory;
				int numCriticals = ruin.getMaxCriticalHits();
				int numSpawns = ruin.getSpawnLocations().size();
				result = "<p>"+
						"<b>"+MessagePath.LABEL_RUIN.getMessage() + ruin.getName() + "</b><br>" +
						MessagePath.LABEL_CRITICAL_HITS.getMessage() + ": " + numCriticals + "<br>" +
						MessagePath.LABEL_GOLEM_SPAWNS.getMessage() + ": " + numSpawns + "<br>" +
						"</p>";
				break;
			case CAMP:
				KonCamp camp = (KonCamp)territory;
				result = "<p>"+
						"<b>"+camp.getName() + "</b><br>" +
						MessagePath.LABEL_BARBARIANS.getMessage() + "<br>" +
						"</p>";
				break;
			case CAPITAL:
				KonCapital capital = (KonCapital)territory;
				int numKingdomTowns = territory.getKingdom().getTowns().size();
				int numKingdomLand = 0;
		    	for(KonTown town : territory.getKingdom().getTowns()) {
		    		numKingdomLand += town.getChunkList().size();
		    	}
				int numAllKingdomPlayers = konquest.getPlayerManager().getAllPlayersInKingdom(territory.getKingdom()).size();
				result = "<p>"+
						"<b>"+capital.getName() + "</b><br>" +
						MessagePath.LABEL_KINGDOM.getMessage() + ": " + capital.getKingdom().getName() + "<br>" +
						MessagePath.LABEL_TOWNS.getMessage() + ": " + numKingdomTowns + "<br>" +
						MessagePath.LABEL_LAND.getMessage() + ": " + numKingdomLand + "<br>" +
						MessagePath.LABEL_PLAYERS.getMessage() + ": " + numAllKingdomPlayers + "<br>" +
						"</p>";
				break;
			case TOWN:
				KonTown town = (KonTown)territory;
				String lordName = "-";
				if(town.getPlayerLord() != null) {
					lordName = town.getPlayerLord().getName();
				}
				result = "<p>"+
						"<b>"+town.getName() + "</b><br>" +
						MessagePath.LABEL_KINGDOM.getMessage() + ": " + town.getKingdom().getName() + "<br>" +
						MessagePath.LABEL_LORD.getMessage() + ": " + lordName + "<br>" +
						MessagePath.LABEL_LAND.getMessage() + ": " + town.getChunkList().size() + "<br>" +
						MessagePath.LABEL_POPULATION.getMessage() + ": " + town.getNumResidents() + "<br>" +
						"</p>";
				break;
			default:
				break;
		}
		return result;
	}
	
	private int getAreaColor(KonTerritory territory) {
		int result = 0xFFFFFF;
		switch (territory.getTerritoryType()) {
			case RUIN:
				result = ruinColor;
				break;
			case CAMP:
				result = campColor;
				break;
			case CAPITAL:
				result = capitalColor;
				break;
			case TOWN:
				result = townColor;
				break;
			default:
				break;
		}
		return result;
	}
	
	private boolean isTerritoryValid(KonTerritory territory) {
		boolean result = false;
		switch (territory.getTerritoryType()) {
			case RUIN:
				result = true;
				break;
			case CAMP:
				result = true;
				break;
			case CAPITAL:
				result = true;
				break;
			case TOWN:
				result = true;
				break;
			default:
				break;
		}
		return result;
	}
	
	private String getIconId(KonTerritory territory) {
		String result = "konquest";
		switch (territory.getTerritoryType()) {
			case RUIN:
				result = result+".icon.ruin."+territory.getName().toLowerCase();
				break;
			case CAMP:
				result = result+".icon.camp."+territory.getName().toLowerCase();
				break;
			case CAPITAL:
				result = result+".icon."+territory.getKingdom().getName().toLowerCase()+".capital";
				break;
			case TOWN:
				result = result+".icon."+territory.getKingdom().getName().toLowerCase()+"."+territory.getName().toLowerCase();
				break;
			default:
				break;
		}
		return result;
	}
	
	private String getIconLabel(KonTerritory territory) {
		String result = "Konquest";
		switch (territory.getTerritoryType()) {
			case RUIN:
				result = MessagePath.LABEL_RUIN.getMessage()+" "+territory.getName();
				break;
			case CAMP:
				result = MessagePath.LABEL_BARBARIAN.getMessage()+" "+territory.getName();
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
	
	private MarkerIcon getIconMarker(KonTerritory territory) {
		MarkerIcon result = null;
		switch (territory.getTerritoryType()) {
			case RUIN:
				result = dapi.getMarkerAPI().getMarkerIcon("tower");
				break;
			case CAMP:
				result = dapi.getMarkerAPI().getMarkerIcon("pirateflag");
				break;
			case CAPITAL:
				result = dapi.getMarkerAPI().getMarkerIcon("star");
				break;
			case TOWN:
				result = dapi.getMarkerAPI().getMarkerIcon("orangeflag");
				break;
			default:
				break;
		}
		return result;
	}
}
