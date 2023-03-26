package com.github.rumsfield.konquest.model;

public class KonArmor {

	private final String id;
	private final int blocks;
	private final int cost, costPerResident, costPerLand;
	
	public KonArmor(String id, int blocks, int cost, int costPerResident, int costPerLand) {
		this.id = id;
		this.blocks = Math.max(blocks,0);
		this.cost = Math.max(cost,0);
		this.costPerResident = Math.max(costPerResident,0);
		this.costPerLand = Math.max(costPerLand,0);
	}
	
	public String getId() {
		return id;
	}
	
	public int getBlocks() {
		return blocks;
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
