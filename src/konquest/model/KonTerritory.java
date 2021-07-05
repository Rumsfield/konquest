package konquest.model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

import konquest.Konquest;
import konquest.utility.ChatUtil;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

public abstract class KonTerritory{

	private HashMap<Point,KonTerritory> chunkList;
	private Location centerLoc;
	private Location spawnLoc;
	private KonKingdom kingdom;
	private String name;
	private KonTerritoryType territoryType;
	private Konquest konquest;
	
	
	public KonTerritory(Location loc, String name, KonKingdom kingdom, KonTerritoryType territoryType, Konquest konquest) {
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
		return chunkList.remove(konquest.toPoint(loc)) != null;
	}
	
	public void clearChunks() {
		chunkList.clear();
	}
	
	public boolean isLocInside(Location loc) {
		return loc.getWorld().equals(getWorld()) && chunkList.containsKey(getKonquest().toPoint(loc));
	}
	
	//TODO: Refactor to not use chunks, currently method is unused
	public boolean isLocAdjacent(Location loc) {
		boolean result = false;
		int[] coordLUTX = {0,1,0,-1};
		int[] coordLUTZ = {1,0,-1,0};
		int curX = loc.getChunk().getX();
		int curZ = loc.getChunk().getZ();
		for(int i = 0;i<4;i++) {
			Chunk nextChunk = loc.getWorld().getChunkAt(curX+coordLUTX[i], curZ+coordLUTZ[i]);
			if(nextChunk.getWorld().equals(getWorld()) && chunkList.containsKey(getKonquest().toPoint(nextChunk))) {
				result = true;
				break;
			}
		}
		return result;
	}
	
	//TODO: Refactor to not use chunks, currently method is unused
	public ArrayList<Chunk> getSurroundChunks(Location loc) {
		ArrayList<Chunk> sideChunks = new ArrayList<Chunk>();
		int[] coordLUTX = {0,1,1,1,0,-1,-1,-1};
		int[] coordLUTZ = {1,1,0,-1,-1,-1,0,1};
		int curX = loc.getChunk().getX();
		int curZ = loc.getChunk().getZ();
		for(int i = 0;i<8;i++) {
			sideChunks.add(loc.getWorld().getChunkAt(curX+coordLUTX[i], curZ+coordLUTZ[i]));
		}
		return sideChunks;
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
	
	public KonTerritoryType getTerritoryType() {
		return territoryType;
	}
	
	public HashMap<Point,KonTerritory> getChunkList() {
		return chunkList;
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
