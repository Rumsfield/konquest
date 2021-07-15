package konquest.map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerSet;

import konquest.Konquest;
import konquest.model.KonKingdom;
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
		
		String groupIdBase = "konquest.marker.";
		
		String areaIdBase = "konquest.area.";
		
		for (KonKingdom kingdom : konquest.getKingdomManager().getKingdoms()) {
			
			String groupId = groupIdBase + kingdom.getName().toLowerCase();
			String groupLabel = kingdom.getName();
			MarkerSet kingdomGroup = dapi.getMarkerAPI().createMarkerSet(groupId, groupLabel, dapi.getMarkerAPI().getMarkerIcons(), false);
			
			// Capital
			String areaId = areaIdBase + kingdom.getName().toLowerCase() + ".capital";
			String areaLabel = kingdom.getCapital().getName();
			Location center = kingdom.getCapital().getCenterLoc();
			double[] areaCorner1 = new double[] {center.getX()-64, center.getZ()-64};
			double[] areaCorner2 = new double[] {center.getX()+64, center.getZ()+64};
			AreaMarker kingdomCapital = kingdomGroup.createAreaMarker(areaId, areaLabel, true, center.getWorld().getName(), areaCorner1, areaCorner2, false);
			kingdomCapital.setFillStyle(1, 0xb942f5);
			
			// Towns
			for (KonTown town : kingdom.getTowns()) {
				
				areaId = areaIdBase + kingdom.getName().toLowerCase() + "." + town.getName().toLowerCase();
				areaLabel = town.getName();
				center = town.getCenterLoc();
				areaCorner1 = new double[] {center.getX()-64, center.getZ()-64};
				areaCorner2 = new double[] {center.getX()+64, center.getZ()+64};
				AreaMarker kingdomTown = kingdomGroup.createAreaMarker(areaId, areaLabel, true, center.getWorld().getName(), areaCorner1, areaCorner2, false);
				kingdomTown.setFillStyle(1, 0xed921a);
				
			}
		}
		
		
	}
	
	
}
