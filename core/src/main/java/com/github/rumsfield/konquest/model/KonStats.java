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
	
	public int increaseStat(KonStatsType stat, int incr) {
		int newValue;
		if(statMap.containsKey(stat)) {
			newValue = statMap.get(stat) + incr;
		} else {
			newValue = incr;
		}
		statMap.put(stat, newValue);
		return newValue;
	}
	
	public void clearStats() {
		statMap.clear();
	}
	
}
