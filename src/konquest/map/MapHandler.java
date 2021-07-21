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
	
	public void drawDynmapArea() {
		
		String setId = "konquest.marker.example";
		String label = "Example Area";
		MarkerSet markerset = dapi.getMarkerAPI().createMarkerSet(setId, label, dapi.getMarkerAPI().getMarkerIcons(), false);
        
		String areaId = "konquest.area.example";
        AreaMarker am = markerset.createAreaMarker(areaId, label, true, "world", new double[] {-50, -9}, new double[] {-720, -679}, false);

        am.setLabel("test");
        am.setDescription("example test");
        am.setFillStyle(1, 0x42f4f1);
		
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
	
	public void drawDynmapCreateTerritory(KonTerritory territory) {
		
	}
	
	public void drawDynmapRemoveTerritory(KonTerritory territory) {
		
	}
	
	public void drawDynmapClaimTerritory(KonTerritory territory) {
		
	}
	
	public void drawDynmapCaptureTerritory(KonTerritory territory, KonKingdom oldKingdom) {
		
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
					ruinArea.setLineStyle(1, 1, 0x000000);
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
					barbCamp.setLineStyle(1, 1, 0x000000);
				}
			}
		}
		
		// Kingdoms
		for (KonKingdom kingdom : konquest.getKingdomManager().getKingdoms()) {
			
			groupId = groupIdBase + kingdom.getName().toLowerCase();
			groupLabel = "Konquest "+kingdom.getName();
			MarkerSet kingdomGroup = dapi.getMarkerAPI().createMarkerSet(groupId, groupLabel, dapi.getMarkerAPI().getMarkerIcons(), false);
			
			if (kingdomGroup != null) {
				// Capital
				areaId = areaIdBase + kingdom.getName().toLowerCase() + ".capital";
				areaLabel = kingdom.getCapital().getName();
				at = new AreaTerritory(kingdom.getCapital());
				AreaMarker kingdomCapital = kingdomGroup.createAreaMarker(areaId, areaLabel, true, at.getWorldName(), at.getXCorners(), at.getZCorners(), false);
				if (kingdomCapital != null) {
					kingdomCapital.setFillStyle(opacity, capitalColor);
					kingdomCapital.setLineStyle(1, 1, 0x000000);
				}
				// Towns
				for (KonTown town : kingdom.getTowns()) {
					areaId = areaIdBase + kingdom.getName().toLowerCase() + "." + town.getName().toLowerCase();
					areaLabel = kingdom.getName() + " " + town.getName();
					at = new AreaTerritory(town);
					AreaMarker kingdomTown = kingdomGroup.createAreaMarker(areaId, areaLabel, true, at.getWorldName(), at.getXCorners(), at.getZCorners(), false);
					if (kingdomTown != null) {
						kingdomTown.setFillStyle(opacity, townColor);
						kingdomTown.setLineStyle(1, 1, 0x000000);
					}
				}
			}
		}
		
		Date end = new Date();
		int time = (int)(end.getTime() - start.getTime());
		ChatUtil.printDebug("Drawing all territory in Dynmap took "+time+" ms");
		
	}
	
	
}
