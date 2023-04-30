package com.github.rumsfield.konquest.model;

import java.awt.Point;
import java.util.Collection;
import java.util.HashMap;

// Wrapper class for a collection of all claimed land chunks (points) in a specific world, mapped to which territory owns them.
public class KonTerritoryCache {

	private final HashMap<Point,KonTerritory> territoryCache;
	
	public KonTerritoryCache() {
		territoryCache = new HashMap<>();
	}
	
	public void put(Point point, KonTerritory territory) {
		territoryCache.put(point,territory);
	}
	
	public void putAll(HashMap<Point,KonTerritory> map) {
		territoryCache.putAll(map);
	}
	
	public boolean has(Point point) {
		return territoryCache.containsKey(point);
	}
	
	public KonTerritory get(Point point) {
		return territoryCache.get(point);
	}
	
	public boolean remove(Point point) {
		return territoryCache.remove(point) != null;
	}
	
	public boolean removeAll(Collection<Point> points) {
		boolean result = true;
		for(Point point : points) {
			if(territoryCache.remove(point) == null) {
				result = false;
			}
		}
		return result;
	}

}
