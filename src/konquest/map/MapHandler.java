package konquest.map;

import java.awt.Point;
import java.util.Date;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerSet;

import konquest.Konquest;
import konquest.model.KonCamp;
import konquest.model.KonKingdom;
import konquest.model.KonRuin;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;

public class MapHandler {

	private Konquest konquest;
	private boolean isEnabled;
	
	private static DynmapAPI dapi = null;
	
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
		Location center;
		double[] areaCorner1;
		double[] areaCorner2;
		Set<Point> chunks;
		int pid = 0;
		
		final double opacity = 0.5;
		
		// Ruins
		groupId = groupIdBase + "ruin";
		groupLabel = "Konquest Ruins";
		MarkerSet ruinGroup = dapi.getMarkerAPI().createMarkerSet(groupId, groupLabel, dapi.getMarkerAPI().getMarkerIcons(), false);
		if (ruinGroup != null) {
			for (KonRuin ruin : konquest.getRuinManager().getRuins()) {
				areaId = areaIdBase + "ruin." + ruin.getName().toLowerCase();
				areaLabel = "Ruin " + ruin.getName();
				center = ruin.getCenterLoc();
				chunks = ruin.getChunkList().keySet();
				pid = 0;
				for (Point p : chunks) {
					areaCorner1 = new double[] {p.x * 16, p.x * 16 + 16};
					areaCorner2 = new double[] {p.y * 16, p.y * 16 + 16};
					AreaMarker ruinArea = ruinGroup.createAreaMarker(areaId+"."+pid, areaLabel, true, center.getWorld().getName(), areaCorner1, areaCorner2, false);
					if (ruinArea != null) {
						ruinArea.setFillStyle(opacity, 0x242424);
						ruinArea.setLineStyle(0, 1, 0xed1a1a);
					}
					pid++;
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
				areaLabel = "Barbarian Camp " + camp.getName();
				center = camp.getCenterLoc();
				chunks = camp.getChunkList().keySet();
				pid = 0;
				for (Point p : chunks) {
					areaCorner1 = new double[] {p.x * 16, p.x * 16 + 16};
					areaCorner2 = new double[] {p.y * 16, p.y * 16 + 16};
					AreaMarker barbCamp = campGroup.createAreaMarker(areaId+"."+pid, areaLabel, true, center.getWorld().getName(), areaCorner1, areaCorner2, false);
					if (barbCamp != null) {
						barbCamp.setFillStyle(opacity, 0xa3a10a);
						barbCamp.setLineStyle(0, 1, 0xed1a1a);
					}
					pid++;
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
				center = kingdom.getCapital().getCenterLoc();
				chunks = kingdom.getCapital().getChunkList().keySet();
				pid = 0;
				for (Point p : chunks) {
					areaCorner1 = new double[] {p.x * 16, p.x * 16 + 16};
					areaCorner2 = new double[] {p.y * 16, p.y * 16 + 16};
					AreaMarker kingdomCapital = kingdomGroup.createAreaMarker(areaId+"."+pid, areaLabel, true, center.getWorld().getName(), areaCorner1, areaCorner2, false);
					if (kingdomCapital != null) {
						kingdomCapital.setFillStyle(opacity, 0xb942f5);
						kingdomCapital.setLineStyle(0, 1, 0xed1a1a);
					}
					pid++;
				}
				
				// Towns
				for (KonTown town : kingdom.getTowns()) {
					areaId = areaIdBase + kingdom.getName().toLowerCase() + "." + town.getName().toLowerCase();
					areaLabel = kingdom.getName() + " " + town.getName();
					center = town.getCenterLoc();
					chunks = town.getChunkList().keySet();
					pid = 0;
					for (Point p : chunks) {
						areaCorner1 = new double[] {p.x * 16, p.x * 16 + 16};
						areaCorner2 = new double[] {p.y * 16, p.y * 16 + 16};
						AreaMarker kingdomTown = kingdomGroup.createAreaMarker(areaId+"."+pid, areaLabel, true, center.getWorld().getName(), areaCorner1, areaCorner2, false);
						if (kingdomTown != null) {
							kingdomTown.setFillStyle(opacity, 0xed921a);
							kingdomTown.setLineStyle(0, 1, 0xed1a1a);
						}
						pid++;
					}
				}
			}
		}
		
		Date end = new Date();
		int time = (int)(end.getTime() - start.getTime());
		ChatUtil.printDebug("Drawing all territory in Dynmap took "+time+" ms");
		
	}
	
	
}
