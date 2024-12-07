package com.github.rumsfield.konquest.model;

import java.util.HashMap;



public class KonStats {

	private final HashMap<KonStatsType,Integer> statMap;
	
	public KonStats() {
		this.statMap = new HashMap<>();
	}
	
	public void setStat(KonStatsType stat, int value) {
		statMap.put(stat, value);
	}
	
	public int getStat(KonStatsType stat) {
		int result = 0;
		if(statMap.containsKey(stat)) {
			result = statMap.get(stat);
		}
		return result;
	}
	
	public void increaseStat(KonStatsType stat, int incr) {
		int value = 0;
		if (statMap.containsKey(stat)) {
			value = statMap.get(stat);
		}
		// Add to value, with limit of 0
		value = Math.max(value + incr, 0);
		statMap.put(stat, value);
	}
	
	public void clearStats() {
		statMap.clear();
	}
	
}
