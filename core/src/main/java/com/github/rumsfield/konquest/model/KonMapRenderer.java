package com.github.rumsfield.konquest.model;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class KonMapRenderer extends MapRenderer {

	MapCursor territoryMarker;
	MapCursorCollection cursors;
	
	public KonMapRenderer(String territoryName) {
		this.territoryMarker = new MapCursor((byte)0,(byte)0,(byte)0,MapCursor.Type.RED_MARKER,true,territoryName);
		this.cursors = new MapCursorCollection();
		cursors.addCursor(territoryMarker);
	}
	
	@Override
	public void render(MapView view, MapCanvas canvas, Player player) {
		canvas.setCursors(cursors);
	}

}
