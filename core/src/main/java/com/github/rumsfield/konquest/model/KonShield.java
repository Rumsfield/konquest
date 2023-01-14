package com.github.rumsfield.konquest.model;

public class KonShield {

	private final String id;
	private final int duration;
	private final int cost;
	
	public KonShield(String id, int duration, int cost) {
		this.id = id;
		this.duration = duration;
		this.cost = cost;
	}
	
	public String getId() {
		return id;
	}
	
	public int getDurationSeconds() {
		return duration;
	}

	public int getCost() {
		return cost;
	}
}
