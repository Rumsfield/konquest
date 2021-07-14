package konquest.map;

import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerSet;

import konquest.KonquestPlugin;

public class MapHandler {

	
	public MapHandler() {
	}
	
	public void drawDynmapArea() {
		DynmapAPI dapi = KonquestPlugin.getDynmapAPI();
		
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
