package konquest.map;

import java.util.Date;

import org.bukkit.Bukkit;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerSet;

import konquest.Konquest;
import konquest.model.KonCamp;
import konquest.model.KonKingdom;
import konquest.model.KonRuin;
import konquest.model.KonTerritory;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;

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
			isEnabled = true;
			ChatUtil.printConsoleAlert("Successfully registered Dynmap.");
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
		
		MarkerSet territoryGroup = dapi.getMarkerAPI().getMarkerSet(groupId);
		if (territoryGroup == null) {
			territoryGroup = dapi.getMarkerAPI().createMarkerSet(groupId, groupLabel, dapi.getMarkerAPI().getMarkerIcons(), false);
		}
		AreaMarker territoryArea = territoryGroup.findAreaMarker(areaId);
		AreaTerritory drawArea = new AreaTerritory(territory);
		if (territoryArea == null) {
			// Area does not exist, create new
			AreaMarker newArea = territoryGroup.createAreaMarker(areaId, areaLabel, true, drawArea.getWorldName(), drawArea.getXCorners(), drawArea.getZCorners(), false);
			if (newArea != null) {
				newArea.setFillStyle(0.5, areaColor);
				newArea.setLineStyle(1, 1, lineColor);
			}
		} else {
			// Area already exists, update corners
			territoryArea.setCornerLocations(drawArea.getXCorners(), drawArea.getZCorners());
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
			}
			if (territoryGroup.getAreaMarkers().isEmpty()) {
				// Delete group if no more areas
				territoryGroup.deleteMarkerSet();
			}
		}
		
	}
	
	public void drawDynmapRefreshTerritory(KonTerritory territory) {
		drawDynmapRemoveTerritory(territory);
		drawDynmapUpdateTerritory(territory);
	}
	
	public void drawDynmapAllTerritories() {
		if (!isEnabled) {
			return;
		}
		
		Date start = new Date();
		String groupIdBase = "konquest.marker.";
		
		String areaIdBase = "konquest.area.";
		
		String groupId;
		String groupLabel;
		String areaId;
		String areaLabel;
		AreaTerritory at;
		
		final double opacity = 0.5;
		
		// Ruins
		groupId = groupIdBase + "ruin";
		groupLabel = "Konquest Ruins";
		MarkerSet ruinGroup = dapi.getMarkerAPI().createMarkerSet(groupId, groupLabel, dapi.getMarkerAPI().getMarkerIcons(), false);
		if (ruinGroup != null) {
			for (KonRuin ruin : konquest.getRuinManager().getRuins()) {
				areaId = areaIdBase + "ruin." + ruin.getName().toLowerCase();
				areaLabel = "Ruin " + ruin.getName();
				at = new AreaTerritory(ruin);
				AreaMarker ruinArea = ruinGroup.createAreaMarker(areaId, areaLabel, true, at.getWorldName(), at.getXCorners(), at.getZCorners(), false);
				if (ruinArea != null) {
					ruinArea.setFillStyle(opacity, ruinColor);
					ruinArea.setLineStyle(1, 1, lineColor);
				}
			}
		}
				
		// Camps
		groupId = groupIdBase + "camp";
		groupLabel = "Konquest Barbarian Camps";
		MarkerSet campGroup = dapi.getMarkerAPI().createMarkerSet(groupId, groupLabel, dapi.getMarkerAPI().getMarkerIcons(), false);
		if (campGroup != null) {
			for (KonCamp camp : konquest.getKingdomManager().getCamps()) {
				areaId = areaIdBase + "camp." + camp.getName().toLowerCase();
				areaLabel = "Barbarian " + camp.getName();
				at = new AreaTerritory(camp);
				AreaMarker barbCamp = campGroup.createAreaMarker(areaId, areaLabel, true, at.getWorldName(), at.getXCorners(), at.getZCorners(), false);
				if (barbCamp != null) {
					barbCamp.setFillStyle(opacity, campColor);
					barbCamp.setLineStyle(1, 1, lineColor);
				}
			}
		}
		
		// Kingdoms
		for (KonKingdom kingdom : konquest.getKingdomManager().getKingdoms()) {
			
			groupId = groupIdBase + kingdom.getName().toLowerCase();
			groupLabel = "Konquest Kingdom "+kingdom.getName();
			MarkerSet kingdomGroup = dapi.getMarkerAPI().createMarkerSet(groupId, groupLabel, dapi.getMarkerAPI().getMarkerIcons(), false);
			
			if (kingdomGroup != null) {
				// Capital
				areaId = areaIdBase + kingdom.getName().toLowerCase() + ".capital";
				areaLabel = kingdom.getCapital().getName();
				at = new AreaTerritory(kingdom.getCapital());
				AreaMarker kingdomCapital = kingdomGroup.createAreaMarker(areaId, areaLabel, true, at.getWorldName(), at.getXCorners(), at.getZCorners(), false);
				if (kingdomCapital != null) {
					kingdomCapital.setFillStyle(opacity, capitalColor);
					kingdomCapital.setLineStyle(1, 1, lineColor);
				}
				// Towns
				for (KonTown town : kingdom.getTowns()) {
					areaId = areaIdBase + kingdom.getName().toLowerCase() + "." + town.getName().toLowerCase();
					areaLabel = kingdom.getName() + " " + town.getName();
					at = new AreaTerritory(town);
					AreaMarker kingdomTown = kingdomGroup.createAreaMarker(areaId, areaLabel, true, at.getWorldName(), at.getXCorners(), at.getZCorners(), false);
					if (kingdomTown != null) {
						kingdomTown.setFillStyle(opacity, townColor);
						kingdomTown.setLineStyle(1, 1, lineColor);
					}
				}
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
				result = "Ruin "+territory.getName();
				break;
			case CAMP:
				result = "Barbarian "+territory.getName();
				break;
			case CAPITAL:
				result = territory.getKingdom().getName();
				break;
			case TOWN:
				result = territory.getKingdom().getName()+" "+territory.getName();
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
}
