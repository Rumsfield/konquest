package com.github.rumsfield.konquest.model;

import java.util.HashMap;

public class KonPlayerScoreAttributes {

	public enum ScoreAttribute {
		TOWN_LORDS 		(20),
		TOWN_KNIGHTS 	(10),
		TOWN_RESIDENTS 	(5),
		LAND_LORDS 		(4),
		LAND_KNIGHTS 	(2),
		LAND_RESIDENTS 	(1);
		
		private final int weight;
		ScoreAttribute(int weight) {
			this.weight = weight;
		}
		
		public int getWeight() {
			return weight;
		}
	}
	
	private final HashMap<ScoreAttribute,Integer> attributeMap;
	
	public KonPlayerScoreAttributes() {
		this.attributeMap = new HashMap<>();
		for(ScoreAttribute attribute : ScoreAttribute.values()) {
			attributeMap.put(attribute, 0);
		}
	}
	
	public void setAttribute(ScoreAttribute attribute, int value) {
		attributeMap.put(attribute, value);
	}
	
	public void addAttribute(ScoreAttribute attribute, int value) {
		if(attributeMap.containsKey(attribute)) {
			int current = attributeMap.get(attribute);
			attributeMap.put(attribute, current+value);
		}
	}
	
	public int getAttributeValue(ScoreAttribute attribute) {
		int result = 0;
		if(attributeMap.containsKey(attribute)) {
			result = attributeMap.get(attribute);
		}
		return result;
	}
	
	public int getAttributeScore(ScoreAttribute attribute) {
		int result = 0;
		if(attributeMap.containsKey(attribute)) {
			result = attributeMap.get(attribute)*attribute.getWeight();
		}
		return result;
	}
	
	public int getScore() {
		int result = 0;
		for(ScoreAttribute attribute : attributeMap.keySet()) {
			result += (attributeMap.get(attribute)*attribute.getWeight());
		}
		return result;
	}
}
