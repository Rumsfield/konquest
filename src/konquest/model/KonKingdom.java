package konquest.model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Sound;

import konquest.Konquest;
import konquest.api.model.KonquestKingdom;
import konquest.utility.ChatUtil;
import konquest.utility.Timeable;
import konquest.utility.Timer;

public class KonKingdom implements Timeable, KonquestKingdom {

	private String name;
	private Konquest konquest;
	private KonCapital capital;
	private KonMonumentTemplate monumentTemplate;
	private HashMap<String, KonTown> townMap;
	private boolean isSmallest;
	private boolean isPeaceful;
	private boolean isOfflineProtected;
	private boolean isMonumentBlanking; //TODO KR remove this
	private Timer protectedWarmupTimer;
	
	public KonKingdom(Location loc, String name, Konquest konquest) {
		this.name = name;
		this.konquest = konquest;
		this.capital = new KonCapital(loc, this, konquest);
		this.townMap = new HashMap<String, KonTown>();
		this.monumentTemplate = null; // new kingdoms start with null template
		this.isSmallest = false;
		this.isPeaceful = false;
		this.isOfflineProtected = true;
		this.isMonumentBlanking = false;
		this.protectedWarmupTimer = new Timer(this);
	}
	
	// Constructor meant for Barbarians, created on startup
	public KonKingdom(String name, Konquest konquest) {
		this.name = name;
		this.konquest = konquest;
		this.capital = new KonCapital(new Location(konquest.getPlugin().getServer().getWorld("world"),0,65,0), "Barbarian", this, konquest);
		this.townMap = new HashMap<String, KonTown>();
	}
	
	public int initCapital() {
		int status = capital.initClaim();
		return status;
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
	
	public boolean isMonumentTemplateValid() {
		boolean result = false;
		if(monumentTemplate != null) {
			result = monumentTemplate.isValid();
		}
		return result;
	}
	
	public void setMonumentTemplate(KonMonumentTemplate template) {
		monumentTemplate = template;
	}
	
	public KonMonumentTemplate getMonumentTemplate() {
		return monumentTemplate;
	}
	
	public String getName() {
		return name;
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
		}
	}
	
	public Timer getProtectedWarmupTimer() {
		return protectedWarmupTimer;
	}
	
	public void updateAllTownBars() {
		for(KonTown town : townMap.values()) {
			town.updateBarPlayers();
		}
	}
	
	public void reloadLoadedTownMonuments() {
		Point tPoint;
		for(KonTown town : getTowns()) {
			tPoint = Konquest.toPoint(town.getCenterLoc());
			if(town.getWorld().isChunkLoaded(tPoint.x,tPoint.y)) {
				if(town.isAttacked()) {
					ChatUtil.printDebug("Could not paste monument in town "+town.getName()+" while under attack");
				} else {
					// Teleport players out of the chunk
					for(KonPlayer player : konquest.getPlayerManager().getPlayersOnline()) {
						if(town.getMonument().isLocInside(player.getBukkitPlayer().getLocation())) {
							player.getBukkitPlayer().teleport(konquest.getSafeRandomCenteredLocation(town.getCenterLoc(), 2));
							//player.getBukkitPlayer().playSound(player.getBukkitPlayer().getLocation(), Sound.BLOCK_ANVIL_USE, (float)1, (float)1.2);
						}
					}
					// Update monument from template
					town.reloadMonument();
					town.getWorld().playSound(town.getCenterLoc(), Sound.BLOCK_ANVIL_USE, (float)1, (float)0.8);
				}
			}
		}
	}
}
