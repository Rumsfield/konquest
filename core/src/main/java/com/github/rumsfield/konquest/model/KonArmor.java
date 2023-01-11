package com.github.rumsfield.konquest.model;

public class KonArmor {

	private final String id;
	private final int blocks;
	private final int cost;
	
	public KonArmor(String id, int blocks, int cost) {
		this.id = id;
		this.blocks = blocks;
		this.cost = cost;
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
}
