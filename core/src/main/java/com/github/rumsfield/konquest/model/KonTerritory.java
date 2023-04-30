package com.github.rumsfield.konquest.model;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestTerritory;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.utility.ChatUtil;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public abstract class KonTerritory implements KonquestTerritory {

	private final HashMap<Point,KonTerritory> chunkList;
	private final Location centerLoc;
	private Location spawnLoc;
	private KonKingdom kingdom;
	private String name;
	private final Konquest konquest;
	
	
	public KonTerritory(Location loc, String name, KonKingdom kingdom, Konquest konquest) {
		this.centerLoc = loc;
		this.spawnLoc = loc;
		this.name = name;
		this.kingdom = kingdom;
		this.konquest = konquest;
		chunkList = new HashMap<>();
	}
	
	public boolean addChunks(ArrayList<Point> points) {
		boolean pass = true;
		for(Point point : points) {
			if(!addChunk(point)) {
				pass = false;
				ChatUtil.printDebug("Failed to add point "+point.toString()+" in territory "+name);
			}
		}
		return pass;
	}
	
	/**
	 * Directly adds a single point to the chunkMap without any checks (override)
	 * @param point the point to add
	 */
	public void addPoint(Point point) {
		chunkList.put(point,  this);
	}
	
	/**
	 * Directly adds points to the chunkMap without any checks (override)
	 * @param points the points to add
	 */
	public void addPoints(ArrayList<Point> points) {
		for(Point point : points) {
			chunkList.put(point, this);
		}
	}
	
	public void addPoints(Set<Point> points) {
		for(Point point : points) {
			chunkList.put(point, this);
		}
	}
	
	public boolean removeChunk(Location loc) {
		return removeChunk(Konquest.toPoint(loc));
	}
	
	public boolean removeChunk(Point point) {
		return chunkList.remove(point) != null;
	}
	
	public void clearChunks() {
		chunkList.clear();
	}
	
	public boolean isLocInside(Location loc) {
		return loc.getWorld().equals(getWorld()) && chunkList.containsKey(Konquest.toPoint(loc));
	}
	
	public boolean isLocInCenter(Location loc) {
		return loc.getWorld().equals(getWorld()) && Konquest.toPoint(loc).equals(Konquest.toPoint(centerLoc));
	}
	
	public boolean hasChunk(Chunk chunk) {
		return chunk.getWorld().equals(getWorld()) && chunkList.containsKey(Konquest.toPoint(chunk));
	}
	
	public boolean isLocAdjacent(Location loc) {
		boolean result = false;
		int[] coordLUTX = {0,1,0,-1};
		int[] coordLUTZ = {1,0,-1,0};
		Point center = Konquest.toPoint(loc);
		for(int i = 0;i<4;i++) {
			if(loc.getWorld().equals(getWorld()) && chunkList.containsKey(new Point(center.x + coordLUTX[i], center.y + coordLUTZ[i]))) {
				result = true;
				break;
			}
		}
		return result;
	}
	
	// Getters
	public Location getCenterLoc() {
		return centerLoc;
	}
	
	public World getWorld() {
		return centerLoc.getWorld();
	}
	
	public Location getSpawnLoc() {
		return spawnLoc;
	}
	
	public String getName() {
		return name;
	}

	public String getTravelName() {
		if(getTerritoryType().equals(KonquestTerritoryType.CAPITAL)) {
			return kingdom.getName();
		} else {
			return name;
		}
	}
	
	public KonKingdom getKingdom() {
		return kingdom;
	}
	
	public HashMap<Point,KonTerritory> getChunkList() {
		return chunkList;
	}
	
	public HashSet<Point> getChunkPoints() {
		return new HashSet<>(chunkList.keySet());
	}
	
	public Konquest getKonquest() {
		return konquest;
	}
	
	// Setters
	public void setSpawn(Location loc) {
		spawnLoc = loc;
	}
	
	public void setKingdom(KonKingdom newKingdom) {
		kingdom = newKingdom;
	}
	
	public void setName(String newName) {
		name = newName;
	}
	
	/**
	 * Method for initializing a new territory, to be implemented by subclasses.
	 * @return  0 - success
	 * 			1 - error, bad monument init
	 * 			2 - error, bad town height
	 * 			3 - error, too much air below town
	 * 			4 - error, bad chunks
	 */
	public abstract int initClaim();
	
	public abstract boolean addChunk(Point point);
	
	// Returns true if the chunk is allowed to be added
	public abstract boolean testChunk(Point point);
	
	public abstract KonquestTerritoryType getTerritoryType();
	
}
