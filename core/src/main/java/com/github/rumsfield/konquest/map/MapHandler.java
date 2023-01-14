package com.github.rumsfield.konquest.map;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
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

public class MapHandler {
	
	private final Konquest konquest;
	private boolean isEnabled;
	private final HashMap<KonTerritory,AreaTerritory> areaCache;
	
	private static DynmapAPI dapi = null;
	
	private final int ruinColor = 0x242424;
	private final int campColor = 0xa3a10a;
	private final int lineDefaultColor = 0x000000;
	private final int lineCapitalColor = 0x8010d0;
	
	public MapHandler(Konquest konquest) {
		this.konquest = konquest;
		this.isEnabled = false;
		this.areaCache = new HashMap<>();
	}
	
	public void initialize() {
		boolean doRegistration = konquest.getConfigManager().getConfig("core").getBoolean("core.integration.dynmap",false);
		Plugin dynmap = Bukkit.getPluginManager().getPlugin("dynmap");
		if (dynmap != null && dynmap.isEnabled()) {
			if(doRegistration) {
				dapi = (DynmapAPI) Bukkit.getServer().getPluginManager().getPlugin("dynmap");
				if(dapi != null) {
					isEnabled = true;
					ChatUtil.printConsoleAlert("Successfully registered Dynmap.");
				} else {
					ChatUtil.printConsoleError("Failed to register Dynmap. Is it disabled?");
				}
			} else {
				ChatUtil.printConsoleAlert("Disabled Dynmap integration from core config settings.");
			}
				
		} else {
			ChatUtil.printConsoleAlert("Could not integrate Dynmap, missing or disabled.");
		}
	}
	
	public boolean isEnabled() {
		return isEnabled;
	}
	
	/*
	 * Dynmap Area ID formats:
	 * Ruins
	 * 		MarkerSet Group:  		konquest.marker.ruin
	 * 		AreaMarker Points: 		konquest.area.ruin.<name>.point.<n>
	 * 		AreaMarker Contours: 	konquest.area.ruin.<name>.contour.<n>
	 * Camps
	 * 		MarkerSet Group:  		konquest.marker.camp
	 * 		AreaMarker Points: 		konquest.area.camp.<name>.point.<n>
	 * 		AreaMarker Contours: 	konquest.area.camp.<name>.contour.<n>
	 * Kingdoms
	 * 		MarkerSet Group:  		konquest.marker.<kingdom>
	 * 		Capital
	 * 		AreaMarker Points: 		konquest.area.<kingdom>.capital.point.<n>
	 * 		AreaMarker Contours: 	konquest.area.<kingdom>.capital.contour.<n>
	 * 		Towns
	 * 		AreaMarker Points: 		konquest.area.<kingdom>.<town>.point.<n>
	 * 		AreaMarker Contours: 	konquest.area.<kingdom>.<town>.contour.<n>
	 */
	
	//TODO: Make this class into a listener, make events for territory updates, deletes, etc
	
	//TODO: Figure out way to have created and removed areas update on web page without refresh
	
	/**
	 * Draws a territory with Dynmap, when created or updated
	 * @param territory Territory to draw
	 */
	public void drawDynmapUpdateTerritory(KonTerritory territory) {
		if (!isEnabled) return;
		
		if (isTerritoryInvalid(territory)) {
			ChatUtil.printDebug("Could not draw territory "+territory.getName()+" with invalid type, "+territory.getTerritoryType().toString());
			return;
		}
		String groupId = getGroupId(territory);
		String groupLabel = getGroupLabel(territory);
		String areaId = getAreaId(territory);
		String areaLabel = getAreaLabel(territory);
		int areaColor = getAreaColor(territory);
		int lineColor = getLineColor(territory);
		String iconId = getIconId(territory);
		String iconLabel = getIconLabel(territory);
		MarkerIcon icon = getIconMarker(territory);
		// Get territory group
		MarkerSet territoryGroup = dapi.getMarkerAPI().getMarkerSet(groupId);
		if (territoryGroup == null) {
			territoryGroup = dapi.getMarkerAPI().createMarkerSet(groupId, groupLabel, dapi.getMarkerAPI().getMarkerIcons(), false);
		}
		String pointId;
		String contourId;
		AreaMarker areaPoint;
		AreaMarker areaContour;
		// Prune any points and contours
		AreaTerritory drawArea = new AreaTerritory(territory);
		if(areaCache.containsKey(territory)) {
			// Territory is already rendered
			AreaTerritory oldArea = areaCache.get(territory);
			for(int i = (oldArea.getNumContours()-1); i >= drawArea.getNumContours(); i--) {
				contourId = areaId + ".contour." + i;
				areaContour = territoryGroup.findAreaMarker(contourId);
				if (areaContour != null) {
					// Delete area from group
					areaContour.deleteMarker();
				}
			}
			for(int i = (oldArea.getNumPoints()-1); i >= drawArea.getNumPoints(); i--) {
				pointId = areaId + ".point." + i;
				areaPoint = territoryGroup.findAreaMarker(pointId);
				if (areaPoint != null) {
					// Delete area from group
					areaPoint.deleteMarker();
				}
			}
		}
		areaCache.put(territory, drawArea);
		// Update or create all points and contours
		for(int i = 0; i < drawArea.getNumContours(); i++) {
			contourId = areaId + ".contour." + i;
			areaContour = territoryGroup.findAreaMarker(contourId);
			if (areaContour == null) {
				// Area does not exist, create new
				areaContour = territoryGroup.createAreaMarker(contourId, "", false, drawArea.getWorldName(), drawArea.getXContour(i), drawArea.getZContour(i), false);
				if (areaContour != null) {
					areaContour.setFillStyle(0, areaColor);
					areaContour.setLineStyle(1, 1, lineColor);
				}
			} else {
				// Area already exists, update corners
				areaContour.setCornerLocations(drawArea.getXContour(i), drawArea.getZContour(i));
			}
		}
		for(int i = 0; i < drawArea.getNumPoints(); i++) {
			pointId = areaId + ".point." + i;
			areaPoint = territoryGroup.findAreaMarker(pointId);
			if (areaPoint == null) {
				// Area does not exist, create new
				areaPoint = territoryGroup.createAreaMarker(pointId, areaLabel, true, drawArea.getWorldName(), drawArea.getXPoint(i), drawArea.getZPoint(i), false);
				if (areaPoint != null) {
					areaPoint.setFillStyle(0.5, areaColor);
					areaPoint.setLineStyle(0, 0, lineColor);
					areaPoint.setLabel(areaLabel,true);
				}
			} else {
				// Area already exists, update corners and label
				areaPoint.setCornerLocations(drawArea.getXPoint(i), drawArea.getZPoint(i));
				areaPoint.setLabel(areaLabel,true);
			}
		}
		Marker territoryIcon = territoryGroup.findMarker(iconId);
		if (territoryIcon == null) {
			// Icon does not exist, create new
			territoryIcon = territoryGroup.createMarker(iconId, iconLabel, true, drawArea.getWorldName(), drawArea.getCenterX(), drawArea.getCenterY(), drawArea.getCenterZ(), icon, false);
		} else {
			// Icon already exists, update label
			territoryIcon.setLabel(iconLabel);
		}
	}
	
	/**
	 * Deletes a territory with Dynmap, when destroyed or removed
	 * @param territory Territory to delete
	 */
	public void drawDynmapRemoveTerritory(KonTerritory territory) {
		if (!isEnabled) return;
		
		if (isTerritoryInvalid(territory)) {
			ChatUtil.printDebug("Could not delete territory "+territory.getName()+" with invalid type, "+territory.getTerritoryType().toString());
			return;
		}
		ChatUtil.printDebug("Erasing Dynmap area of territory "+territory.getName());
		String groupId = getGroupId(territory);
		String areaId = getAreaId(territory);
		String iconId = getIconId(territory);
		
		MarkerSet territoryGroup = dapi.getMarkerAPI().getMarkerSet(groupId);
		if (territoryGroup != null) {
			if(areaCache.containsKey(territory)) {
				// Territory is already rendered, remove all points and contours
				String pointId;
				String contourId;
				AreaMarker areaPoint;
				AreaMarker areaContour;
				AreaTerritory oldArea = areaCache.get(territory);
				for(int i = 0; i < oldArea.getNumContours(); i++) {
					contourId = areaId + ".contour." + i;
					areaContour = territoryGroup.findAreaMarker(contourId);
					if (areaContour != null) {
						// Delete area from group
						areaContour.deleteMarker();
					}
				}
				for(int i = 0; i < oldArea.getNumPoints(); i++) {
					pointId = areaId + ".point." +i ;
					areaPoint = territoryGroup.findAreaMarker(pointId);
					if (areaPoint != null) {
						// Delete area from group
						areaPoint.deleteMarker();
					}
				}
			} else {
				ChatUtil.printDebug("Failed to erase un-rendered territory "+territory.getName());
			}
			Marker territoryIcon = territoryGroup.findMarker(iconId);
			if (territoryIcon != null) {
				// Delete icon
				territoryIcon.deleteMarker();
				ChatUtil.printDebug("Removing Dynmap icon of territory "+territory.getName());
			}
			if (territoryGroup.getAreaMarkers().isEmpty()) {
				// Delete group if no more areas
				territoryGroup.deleteMarkerSet();
				ChatUtil.printDebug("Removing Dynmap group of territory "+territory.getName());
			}
		}
	}
	
	public void drawDynmapLabel(KonTerritory territory) {
		if (!isEnabled) return;
		if (isTerritoryInvalid(territory)) {
			ChatUtil.printDebug("Could not update label for territory "+territory.getName()+" with invalid type, "+territory.getTerritoryType().toString());
			return;
		}
		String groupId = getGroupId(territory);
		String areaId = getAreaId(territory);
		String areaLabel = getAreaLabel(territory);
		MarkerSet territoryGroup = dapi.getMarkerAPI().getMarkerSet(groupId);
		if (territoryGroup != null) {
			// Update all area point labels
			if(areaCache.containsKey(territory)) {
				// Territory is already rendered, remove all points and contours
				String pointId;
				AreaMarker areaPoint;
				AreaTerritory oldArea = areaCache.get(territory);
				for(int i = 0; i < oldArea.getNumPoints(); i++) {
					pointId = areaId + ".point." +i ;
					areaPoint = territoryGroup.findAreaMarker(pointId);
					if (areaPoint != null) {
						// Area already exists, update label
						areaPoint.setLabel(areaLabel,true);
					}
				}
			} else {
				ChatUtil.printDebug("Failed to label un-rendered territory "+territory.getName());
			}
		}
	}
	
	public void postDynmapBroadcast(String message) {
		if (!isEnabled) return;
		dapi.sendBroadcastToWeb("Konquest", message);
	}
	
	public void drawDynmapAllTerritories() {
		if (!isEnabled) return;
		Date start = new Date();
		
		// Ruins
		for (KonRuin ruin : konquest.getRuinManager().getRuins()) {
			drawDynmapUpdateTerritory(ruin);
		}
		
		// Camps
		for (KonCamp camp : konquest.getCampManager().getCamps()) {
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
				int numKingdomTowns = territory.getKingdom().getTowns().size();
				int numKingdomLand = 0;
		    	for(KonTown town : territory.getKingdom().getTowns()) {
		    		numKingdomLand += town.getChunkList().size();
		    	}
				int numAllKingdomPlayers = konquest.getPlayerManager().getAllPlayersInKingdom(territory.getKingdom()).size();
				result = "<p>"+
						"<b>"+capital.getName() + "</b><br>" +
						MessagePath.MAP_KINGDOM.getMessage() + ": " + capital.getKingdom().getName() + "<br>" +
						MessagePath.MAP_TOWNS.getMessage() + ": " + numKingdomTowns + "<br>" +
						MessagePath.MAP_LAND.getMessage() + ": " + numKingdomLand + "<br>" +
						MessagePath.MAP_PLAYERS.getMessage() + ": " + numAllKingdomPlayers + "<br>" +
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
						MessagePath.MAP_KINGDOM.getMessage() + ": " + town.getKingdom().getName() + "<br>" +
						MessagePath.MAP_LORD.getMessage() + ": " + lordName + "<br>" +
						MessagePath.MAP_LAND.getMessage() + ": " + town.getChunkList().size() + "<br>" +
						MessagePath.MAP_POPULATION.getMessage() + ": " + town.getNumResidents() + "<br>" +
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
			case TOWN:
				result = stringToRGB(territory.getKingdom().getName());
				break;
			default:
				break;
		}
		return result;
	}
	
	private int getLineColor(KonTerritory territory) {
		int result = lineDefaultColor;
		if (Objects.requireNonNull(territory.getTerritoryType()) == KonquestTerritoryType.CAPITAL) {
			result = lineCapitalColor;
		}
		return result;
	}
	
	private boolean isTerritoryInvalid(KonTerritory territory) {
		boolean result = false;
		switch (territory.getTerritoryType()) {
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
	
	private int stringToRGB(String input) {
		int hash = input.hashCode();
		return hash & 0xFFFFFF;
	}
}
