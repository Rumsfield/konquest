package com.github.rumsfield.konquest.model;

import java.util.HashMap;

public class KonKingdomScoreAttributes {
	
	public enum ScoreAttribute {
		TOWNS 		(100),
		LAND 		(10),
		FAVOR 		(1),
		POPULATION 	(50);
		
		private final int weight;
		ScoreAttribute(int weight) {
			this.weight = weight;
		}
		
		public int getWeight() {
			return weight;
		}
	}

	private final HashMap<ScoreAttribute,Integer> attributeMap;
	private final HashMap<ScoreAttribute,Integer> attributeWeights;
	
	public KonKingdomScoreAttributes() {
		this.attributeMap = new HashMap<>();
		this.attributeWeights = new HashMap<>();
		for(ScoreAttribute attribute : ScoreAttribute.values()) {
			attributeMap.put(attribute, 0);
			attributeWeights.put(attribute, attribute.getWeight());
		}
	}
	
	public void setAttributeWeight(ScoreAttribute attribute, int value) {
		attributeWeights.put(attribute, value);
	}
	
	public void setAttribute(ScoreAttribute attribute, int value) {
		attributeMap.put(attribute, value);
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
			result = attributeMap.get(attribute)*attributeWeights.get(attribute);
		}
		return result;
	}
	
	public int getScore() {
		int result = 0;
		for(ScoreAttribute attribute : attributeMap.keySet()) {
			result += (attributeMap.get(attribute)*attributeWeights.get(attribute));
		}
		return result;
	}
}
