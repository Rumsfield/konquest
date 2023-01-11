package com.github.rumsfield.konquest.utility;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.github.rumsfield.konquest.manager.TravelManager.TravelDestination;
import com.github.rumsfield.konquest.model.KonTerritory;

public class TravelPlan {
	// Wrapper class for information regarding a player's travel
	
	private final Player traveler;
	private final TravelDestination destination;
	private final KonTerritory territory;
	private final Location location;
	private final long warmupEndTime;
	private final double cost;
	
	public TravelPlan(Player traveler, TravelDestination destination, KonTerritory territory, Location location, long warmupEndTime, double cost) {
		this.traveler = traveler;
		this.destination = destination;
		this.territory = territory;
		this.location = location;
		this.warmupEndTime = warmupEndTime;
		this.cost = cost;
	}
	
	public Player getTraveler() {
		return traveler;
	}
	
	public TravelDestination getDestination() {
		return destination;
	}
	
	public KonTerritory getTerritory() {
		return territory;
	}
	
	public Location getLocation() {
		return location;
	}
	
	public long getWarmupEndTime() {
		return warmupEndTime;
	}
	
	public double getCost() {
		return cost;
	}
}
