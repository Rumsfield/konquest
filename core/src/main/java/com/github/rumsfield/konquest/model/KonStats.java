package com.github.rumsfield.konquest.model;

import java.util.HashMap;

//import com.github.rumsfield.konquest.utility.ChatUtil;

public class KonStats {

	private HashMap<KonStatsType,Integer> statMap;
	
	public KonStats() {
		this.statMap = new HashMap<KonStatsType,Integer>();
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
		int newValue = 0;
		if(statMap.containsKey(stat)) {
			newValue = statMap.get(stat) + incr;
		} else {
			newValue = incr;
		}
		statMap.put(stat, newValue);
		//ChatUtil.printDebug("Statistic "+stat.toString()+" increased to "+newValue);
		return newValue;
	}
	
	public void clearStats() {
		statMap.clear();
	}
	
}
