package com.github.rumsfield.konquest.model;

public class KonCustomPrefix {

	private final String label;
	private final String name;
	private final int cost;
	
	public KonCustomPrefix(String label, String name, int cost) {
		this.label = label;
		this.name = name;
		this.cost = cost;
	}
	
	public String getLabel() {
		return label;
	}
	
	public String getName() {
		return name;
	}
	
	public int getCost() {
		return cost;
	}
}
