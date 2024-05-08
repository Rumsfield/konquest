package com.github.rumsfield.konquest.model;

import com.github.rumsfield.konquest.Konquest;
import org.bukkit.Location;
import org.bukkit.World;

import java.awt.*;

public class KonMonumentTemplate {

	private Location corner1;
	private Location corner2;
	private Location travelPoint;
	private boolean isValid;
	private boolean isBlanking;
	private boolean hasLoot;
	private String name;
	private double cost;
	private int numCriticals;
	private int numBlocks;
	private int numLootChests;

	private Location shadowCorner1;
	private Location shadowCorner2;
	private Location shadowTravelPoint;
	private double shadowCost;
	
	public KonMonumentTemplate(String name, Location corner1, Location corner2, Location travelPoint, double cost) {
		this.name = name;
		this.corner1 = corner1;
		this.corner2 = corner2;
		this.travelPoint = travelPoint;
		this.cost = cost;
		this.isValid = false;
		this.isBlanking = false;
		this.hasLoot = false;
		this.numCriticals = 0;
		this.numBlocks = 0;
		this.numLootChests = 0;
		this.shadowCorner1 = null;
		this.shadowCorner2 = null;
		this.shadowTravelPoint = null;
		this.shadowCost = 0;
	}
	
	public int getNumCriticals() {
		return numCriticals;
	}
	
	public int getNumBlocks() {
		return numBlocks;
	}
	
	public int getNumLootChests() {
		return numLootChests;
	}
	
	public String getName() {
		return name;
	}

	public double getCost() {
		return cost;
	}
	
	public int getHeight() {
		if(isValid && corner1 != null && corner2 != null) {
			return (int)Math.abs(corner1.getY()-corner2.getY());
		} else {
			return 0;
		}
	}
	
	public boolean isValid() {
		return isValid;
	}
	
	public boolean isBlanking() {
		return isBlanking;
	}
	
	public Location getCornerOne() {
		return corner1;
	}
	
	public Location getCornerTwo() {
		return corner2;
	}
	
	public Location getTravelPoint() {
		return travelPoint;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public void setNumCriticals(int val) {
		numCriticals = val;
	}
	
	public void setNumBlocks(int val) {
		numBlocks = val;
	}
	
	public void setNumLootChests(int val) {
		numLootChests = val;
	}
	
	public void setCornerOne(Location loc) {
		shadowCorner1 = corner1;
		corner1 = loc;
	}
	
	public void setCornerTwo(Location loc) {
		shadowCorner2 = corner2;
		corner2 = loc;
	}
	
	public void setTravelPoint(Location loc) {
		shadowTravelPoint = travelPoint;
		travelPoint = loc;
	}

	public void setCost(double cost) {
		shadowCost = this.cost;
		this.cost = cost;
	}

	public boolean restorePrevious() {
		if(shadowCorner1 != null && shadowCorner2 != null && shadowTravelPoint != null) {
			corner1 = shadowCorner1;
			corner2 = shadowCorner2;
			travelPoint = shadowTravelPoint;
			cost = shadowCost;
			return true;
		}
		return false;
	}
	
	public void setValid(boolean isNewValid) {
		isValid = isNewValid;
	}
	
	public void setBlanking(boolean isNewBlanking) {
		isBlanking = isNewBlanking;
	}
	
	public void setLoot(boolean val) {
		hasLoot = val;
	}
	
	public boolean hasLoot() {
		return hasLoot;
	}
	
	public boolean isLocInside(Location loc) {
		int topBlockX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int topBlockY = Math.max(corner1.getBlockY(), corner2.getBlockY());
        int topBlockZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());
        int bottomBlockX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int bottomBlockY = Math.min(corner1.getBlockY(), corner2.getBlockY());
        int bottomBlockZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
		return loc.getBlockX() <= topBlockX && loc.getBlockY() <= topBlockY && loc.getBlockZ() <= topBlockZ &&
				loc.getBlockX() >= bottomBlockX && loc.getBlockY() >= bottomBlockY && loc.getBlockZ() >= bottomBlockZ;
	}
	
	// Returns true if this template's region is inside the given chunk (point,world)
	public boolean isInsideChunk(Point point, World world) {
		boolean result = false;
		if(corner1.getWorld().equals(world) && corner2.getWorld().equals(world)) {
			// Determine 4 X/Y plane corners
			World allWorld = corner1.getWorld();
			double topX = Math.max(corner1.getX(), corner2.getX());
			double topZ = Math.max(corner1.getZ(), corner2.getZ());
			double botX = Math.min(corner1.getX(), corner2.getX());
			double botZ = Math.min(corner1.getZ(), corner2.getZ());
	        
	        Location c1 = new Location(allWorld, topX, 0.0, topZ);
	        Location c2 = new Location(allWorld, topX, 0.0, botZ);
	        Location c3 = new Location(allWorld, botX, 0.0, topZ);
	        Location c4 = new Location(allWorld, botX, 0.0, botZ);
			
			Point p1 = Konquest.toPoint(c1);
			Point p2 = Konquest.toPoint(c2);
			Point p3 = Konquest.toPoint(c3);
			Point p4 = Konquest.toPoint(c4);
			
			if(point.equals(p1) || point.equals(p2) || point.equals(p3) || point.equals(p4)) {
				result = true;
			}
		}
		return result;
	}
}
