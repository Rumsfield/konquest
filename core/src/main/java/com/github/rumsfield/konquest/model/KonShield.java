package com.github.rumsfield.konquest.model;

public class KonShield {

	private final String id;
	private final int duration;
	private final int cost, costPerResident, costPerLand;
	
	public KonShield(String id, int duration, int cost, int costPerResident, int costPerLand) {
		this.id = id;
		this.duration = Math.max(duration,0);
		this.cost = Math.max(cost,0);
		this.costPerResident = Math.max(costPerResident,0);
		this.costPerLand = Math.max(costPerLand,0);
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

	public int getCostPerResident() {
		return costPerResident;
	}

	public int getCostPerLand() {
		return costPerLand;
	}
}
