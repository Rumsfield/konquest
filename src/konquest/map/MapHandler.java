package konquest.map;

import org.bukkit.Bukkit;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerSet;

import konquest.Konquest;
import konquest.utility.ChatUtil;

public class MapHandler {

	//private Konquest konquest;
	private boolean isEnabled;
	
	private static DynmapAPI dapi = null;
	
	public MapHandler(Konquest konquest) {
		//this.konquest = konquest;
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
}
