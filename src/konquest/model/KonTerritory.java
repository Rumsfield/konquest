package konquest.model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import konquest.Konquest;
import konquest.api.model.KonquestTerritoryType;
import konquest.api.model.KonquestTerritory;
import konquest.utility.ChatUtil;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

public abstract class KonTerritory implements KonquestTerritory {

	private HashMap<Point,KonTerritory> chunkList;
	private Location centerLoc;
	private Location spawnLoc;
	private KonKingdom kingdom;
	private String name;
	private KonquestTerritoryType territoryType;
	private Konquest konquest;
	
	
	public KonTerritory(Location loc, String name, KonKingdom kingdom, KonquestTerritoryType territoryType, Konquest konquest) {
		this.centerLoc = loc;
		this.spawnLoc = loc;
		this.name = name;
		this.kingdom = kingdom;
		this.territoryType = territoryType;
		this.konquest = konquest;
		chunkList = new HashMap<Point,KonTerritory>();
		//chunkList.put(loc.getChunk(),this);
	}
	/*
	public void addChunk(Chunk chunk) {
		chunkList.put(konquest.toPoint(chunk),this);
		ChatUtil.printDebug("Added chunk in territory "+name);
	}*/
	
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
	 * @param point
	 */
	public void addPoint(Point point) {
		chunkList.put(point,  this);
		//ChatUtil.printDebug("Added point in territory "+name);
	}
	
	/**
	 * Directly adds points to the chunkMap without any checks (override)
	 * @param points
	 */
	public void addPoints(ArrayList<Point> points) {
		for(Point point : points) {
			chunkList.put(point, this);
		}
		//ChatUtil.printDebug("Added points in territory "+name);
	}
	
	public boolean removeChunk(Location loc) {
		return chunkList.remove(Konquest.toPoint(loc)) != null;
	}
	
	public void clearChunks() {
		chunkList.clear();
	}
	
	public boolean isLocInside(Location loc) {
		return loc.getWorld().equals(getWorld()) && chunkList.containsKey(Konquest.toPoint(loc));
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
	
	public KonKingdom getKingdom() {
		return kingdom;
	}
	
	public KonquestTerritoryType getTerritoryType() {
		return territoryType;
	}
	
	public HashMap<Point,KonTerritory> getChunkList() {
		return chunkList;
	}
	
	public HashSet<Point> getChunkPoints() {
		return new HashSet<Point>(chunkList.keySet());
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
	
	//public abstract boolean addChunk(Chunk chunk);
	
	public abstract boolean addChunk(Point point);
	
}
