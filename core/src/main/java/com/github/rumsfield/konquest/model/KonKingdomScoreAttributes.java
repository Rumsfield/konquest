package com.github.rumsfield.konquest.model;

import java.util.HashMap;

public class KonKingdomScoreAttributes {
	
	public enum KonKingdomScoreAttribute {
		TOWNS 		(100),
		LAND 		(10),
		FAVOR 		(1),
		POPULATION 	(50);
		
		private final int weight;
		KonKingdomScoreAttribute(int weight) {
			this.weight = weight;
		}
		
		public int getWeight() {
			return weight;
		}
	}

	private final HashMap<KonKingdomScoreAttribute,Integer> attributeMap;
	private final HashMap<KonKingdomScoreAttribute,Integer> attributeWeights;
	
	public KonKingdomScoreAttributes() {
		this.attributeMap = new HashMap<>();
		this.attributeWeights = new HashMap<>();
		for(KonKingdomScoreAttribute attribute : KonKingdomScoreAttribute.values()) {
			attributeMap.put(attribute, 0);
			attributeWeights.put(attribute, attribute.getWeight());
		}
	}
	
	public void setAttributeWeight(KonKingdomScoreAttribute attribute, int value) {
		attributeWeights.put(attribute, value);
	}
	
	public void setAttribute(KonKingdomScoreAttribute attribute, int value) {
		attributeMap.put(attribute, value);
	}
	
	public int getAttributeValue(KonKingdomScoreAttribute attribute) {
		int result = 0;
		if(attributeMap.containsKey(attribute)) {
			result = attributeMap.get(attribute);
		}
		return result;
	}
	
	public int getAttributeScore(KonKingdomScoreAttribute attribute) {
		int result = 0;
		if(attributeMap.containsKey(attribute)) {
			result = attributeMap.get(attribute)*attributeWeights.get(attribute);
		}
		return result;
	}
	
	public int getScore() {
		int result = 0;
		for(KonKingdomScoreAttribute attribute : attributeMap.keySet()) {
			result += (attributeMap.get(attribute)*attributeWeights.get(attribute));
		}
		return result;
	}
}
