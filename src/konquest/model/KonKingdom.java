package konquest.model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;

import konquest.Konquest;
import konquest.utility.ChatUtil;
import konquest.utility.Timeable;
import konquest.utility.Timer;

public class KonKingdom implements Timeable{

	private String name;
	private Konquest konquest;
	private KonCapital capital;
	private KonMonumentTemplate monumentTemplate;
	private HashMap<String, KonTown> townMap;
	private boolean isSmallest;
	private boolean isPeaceful;
	private boolean isOfflineProtected;
	private boolean isMonumentBlanking;
	private Timer protectedWarmupTimer;
	private Timer monumentBlankingTimer;
	
	public KonKingdom(Location loc, String name, Konquest konquest) {
		this.name = name;
		this.konquest = konquest;
		this.capital = new KonCapital(loc, this, konquest);
		this.townMap = new HashMap<String, KonTown>();
		this.monumentTemplate = new KonMonumentTemplate(this);
		this.isSmallest = false;
		this.isPeaceful = false;
		this.isOfflineProtected = true;
		this.isMonumentBlanking = false;
		this.protectedWarmupTimer = new Timer(this);
		this.monumentBlankingTimer = new Timer(this);
	}
	
	// Constructor meant for Barbarians, created on startup
	public KonKingdom(String name, Konquest konquest) {
		this.name = name;
		this.konquest = konquest;
		//this.capital = new KonCapital(new Location(konquest.getPlugin().getServer().getWorld(konquest.getWorldName()),0,65,0), this);
		this.capital = new KonCapital(new Location(konquest.getPlugin().getServer().getWorld("world"),0,65,0), "Barbarian", this, konquest);
		
		this.townMap = new HashMap<String, KonTown>();
	}
	
	public void overrideCapital(Location loc) {
		capital.clearChunks();
		capital = new KonCapital(loc, this, konquest);
		capital.initClaim();
	}
	
	public void initCapital() {
		capital.initClaim();
	}
	
	/**
	 * Adds a Town.
	 * @param loc - Location of center
	 * @param name - Name of Town
	 * @return true if Town name does not already exist, else false.
	 */
	public boolean addTown(Location loc, String name) {
		if(!hasTown(name)) {
			townMap.put(name, new KonTown(loc, name, this, konquest));
			//ChatUtil.printDebug("Added town "+name);
			return true;
		} else {
			//ChatUtil.printDebug("Town already exists: "+name);
			return false;
		}
	}
	
	public int initTown(String name) {
		int exitCode = townMap.get(name).initClaim();
		if(exitCode != 0) {
			ChatUtil.printDebug("Problem initializing town "+name);
		}
		return exitCode;
	}
	
	/**
	 * Removes a town and all of its claims.
	 * @param name 0 Name of Town
	 * @return true if Town was successfully removed from the HashMap, else false.
	 */
	public boolean removeTown(String name) {
		KonTown oldTown = townMap.remove(name);
		if(oldTown != null) {
			ChatUtil.printDebug("Removed town "+name);
			oldTown.removeAllBarPlayers();
			boolean pass = oldTown.removeMonumentBlocks();
			if(!pass) {
				ChatUtil.printDebug("Encountered problem removing monument blocks");
			}
		}
		return oldTown != null;
	}
	
	public KonTown removeTownConquer(String name) {
		return townMap.remove(name);
	}
	
	public boolean addTownConquer(String name, KonTown town) {
		if(!townMap.containsKey(name)) {
			townMap.put(name, town);
			return true;
		}
		return false;
	}
	
	public boolean renameTown(String oldName, String newName) {
		if(hasTown(oldName)) {
			KonTown town = getTown(oldName);
			String oldNameActual = town.getName();
			town.setName(newName);
			townMap.remove(oldNameActual);
			townMap.put(newName, town);
			townMap.get(newName).updateBar();
			townMap.get(newName).updateBarPlayers();
			return true;
		}
		return false;
	}
	/* This can throw NPE!
	public boolean renameTown(String oldName, String newName) {
		if(hasTown(oldName)) {
			getTown(oldName).setName(newName);
			KonTown town = townMap.remove(oldName);
			townMap.put(newName, town);
			townMap.get(newName).updateBar();
			townMap.get(newName).updateBarPlayers();
			return true;
		}
		return false;
	}*/
	
	/**
	 * createMonumentTemplate - Creates a new monument template, verifies stuff.
	 * @param corner1
	 * @param corner2
	 * @param travelPoint
	 * @return  0 - Success
	 * 			1 - Region is not 16x16 blocks in base
	 * 			2 - Region does not contain enough critical blocks
	 * 			3 - Region does not contain the travel point
	 * 			4 - Region is not within capital territory
	 */
	public int createMonumentTemplate(Location corner1, Location corner2, Location travelPoint) {
		// Check that both corners are within capital territory
		if(!capital.isLocInside(corner1) || !capital.isLocInside(corner2)) {
			ChatUtil.printDebug("Failed to create Monument Template, corners are not inside capital territory");
			return 4;
		}
		// Check 16x16 base dimensions
		int diffX = (int)Math.abs(corner1.getX()-corner2.getX())+1;
		int diffZ = (int)Math.abs(corner1.getZ()-corner2.getZ())+1;
		if(diffX != 16 || diffZ != 16) {
			ChatUtil.printDebug("Failed to create Monument Template, not 16x16: "+diffX+"x"+diffZ);
			return 1;
		}
		// Check for at least as many critical blocks as required critical hits
		// Also check for any chests for loot flag
		int maxCriticalhits = konquest.getConfigManager().getConfig("core").getInt("core.monuments.destroy_amount");
		int bottomBlockX, bottomBlockY, bottomBlockZ = 0;
		int topBlockX, topBlockY, topBlockZ = 0;
		int c1X = corner1.getBlockX();
		int c1Y = corner1.getBlockY();
		int c1Z = corner1.getBlockZ();
		int c2X = corner2.getBlockX();
		int c2Y = corner2.getBlockY();
		int c2Z = corner2.getBlockZ();
		bottomBlockX = (c1X < c2X) ? c1X : c2X;
		bottomBlockY = (c1Y < c2Y) ? c1Y : c2Y;
		bottomBlockZ = (c1Z < c2Z) ? c1Z : c2Z;
		topBlockX = (c1X < c2X) ? c2X : c1X;
		topBlockY = (c1Y < c2Y) ? c2Y : c1Y;
		topBlockZ = (c1Z < c2Z) ? c2Z : c1Z;
		int criticalBlockCount = 0;
		boolean containsChest = false;
		for (int x = bottomBlockX; x <= topBlockX; x++) {
            for (int y = bottomBlockY; y <= topBlockY; y++) {
                for (int z = bottomBlockZ; z <= topBlockZ; z++) {
                	Block monumentBlock = corner1.getWorld().getBlockAt(x, y, z);
                	if(monumentBlock.getType().equals(konquest.getKingdomManager().getTownCriticalBlock())) {
                		criticalBlockCount++;
                	} else if(monumentBlock.getState() instanceof Chest) {
                		containsChest = true;
                	}
                }
            }
        }
		if(criticalBlockCount < maxCriticalhits) {
			ChatUtil.printDebug("Failed to create Monument Template, not enough critical blocks. Found "+criticalBlockCount+", required "+maxCriticalhits);
			return 2;
		}
		// Check that travel point is inside of the corner bounds
		if(travelPoint.getBlockX() < bottomBlockX || travelPoint.getBlockX() > topBlockX ||
				travelPoint.getBlockY() < bottomBlockY || travelPoint.getBlockY() > topBlockY ||
				travelPoint.getBlockZ() < bottomBlockZ || travelPoint.getBlockZ() > topBlockZ) {
			ChatUtil.printDebug("Failed to create Monument Template, travel point is outside of corner bounds");
			return 3;
		}
		// Passed all checks, create monument
		monumentTemplate = new KonMonumentTemplate(this, corner1, corner2, travelPoint);
		// Set loot flag
		monumentTemplate.setLoot(containsChest);
		if(containsChest) {
			ChatUtil.printDebug("Created Monument Template with loot chest(s)");
		} else {
			ChatUtil.printDebug("Created Monument Template without loot");
		}
		return 0;
	}
	
	public boolean isSmallest() {
		return isSmallest;
	}
	
	public void setSmallest(boolean val) {
		isSmallest = val;
	}
	
	public boolean isPeaceful() {
		return isPeaceful;
	}
	
	public void setPeaceful(boolean val) {
		isPeaceful = val;
	}
	
	public boolean isOfflineProtected() {
		boolean isBreakDisabledOffline = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.no_enemy_edit_offline");
		return isOfflineProtected && isBreakDisabledOffline;
	}
	
	public void setOfflineProtected(boolean val) {
		isOfflineProtected = val;
	}
	
	public boolean isMonumentBlanking() {
		return isMonumentBlanking;
	}
	
	/*
	public void setMonumentBlanking(boolean val) {
		isMonumentBlanking = val;
	}
	*/
	
	public void removeMonumentTemplate() {
		monumentTemplate.setValid(false);
	}
	
	public KonMonumentTemplate getMonumentTemplate() {
		return monumentTemplate;
	}
	
	public String getName() {
		return name;
	}
	
	public Konquest getKonquest() {
		return konquest;
	}
	
	// Careful! This can return null
	public KonCapital getCapital() {
		return capital;
	}
	
	// This can return an empty list
	public ArrayList<String> getTownNames() {
		ArrayList<String> names = new ArrayList<String>(townMap.keySet());
		return names;
	}
	
	public KonTown getTown(String name) {
		// Check for exact String key match
		boolean isTown = townMap.containsKey(name);
		if(isTown) {
			return townMap.get(name);
		} else {
			// Check for case-insensitive name
			for(String townName : townMap.keySet()) {
				if(name.equalsIgnoreCase(townName)) {
					return townMap.get(townName);
				}
			}
		}
		// Could not find a town by name
		return null;
	}
	
	public ArrayList<KonTown> getTowns() {
		ArrayList<KonTown> towns = new ArrayList<KonTown>(townMap.values());
		return towns;
	}
	
	public boolean isTownMapEmpty() {
		return townMap.isEmpty();
	}
	
	public boolean hasTown(String name) {
		boolean isTown = false;
		// Check for exact String key match
		isTown = townMap.containsKey(name);
		if(isTown) {
			return true;
		} else {
			// Check for case-insensitive name
			for(String townName : townMap.keySet()) {
				if(townName.equalsIgnoreCase(name)) {
					return true;
				}
			}
		}
		// Could not find a town by name
		return false;
	}
	
	public void setName(String newName) {
		name = newName;
	}

	@Override
	public void onEndTimer(int taskID) {
		if(taskID == 0) {
			ChatUtil.printDebug("Kingdom Timer ended with null taskID!");
		} else if(taskID == protectedWarmupTimer.getTaskID()) {
			ChatUtil.printDebug("Kingdom protection warmup Timer ended with taskID: "+taskID);
			isOfflineProtected = true;
		} else if(taskID == monumentBlankingTimer.getTaskID()) {
			ChatUtil.printDebug("Kingdom monument blanking Timer ended with taskID: "+taskID);
			isMonumentBlanking = false;
			reloadLoadedTownMonuments();
		}
	}
	
	public Timer getProtectedWarmupTimer() {
		return protectedWarmupTimer;
	}
	
	/*
	public Timer getMonumentBlankingTimer() {
		return monumentBlankingTimer;
	}
	*/
	public void startMonumentBlanking() {
		isMonumentBlanking = true;
		monumentBlankingTimer.stopTimer();
		monumentBlankingTimer.setTime(30);
		monumentBlankingTimer.startTimer();
		//ChatUtil.printDebug("Starting 30 second monument blanking timer for kingdom "+getName());
	}
	
	private void reloadLoadedTownMonuments() {
		Point tPoint;
		for(KonTown town : getTowns()) {
			tPoint = konquest.toPoint(town.getCenterLoc());
			if(town.getWorld().isChunkLoaded(tPoint.x,tPoint.y)) {
				town.reloadMonument();
			}
		}
	}
}
