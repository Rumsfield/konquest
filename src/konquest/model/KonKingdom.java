package konquest.model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;

import konquest.Konquest;
import konquest.api.model.KonquestKingdom;
import konquest.utility.ChatUtil;
import konquest.utility.CorePath;
import konquest.utility.RequestKeeper;
import konquest.utility.Timeable;
import konquest.utility.Timer;

public class KonKingdom implements Timeable, KonquestKingdom, KonPropertyFlagHolder {

	public enum Relationship {
		ENEMY,
		SANCTIONED,
		PEACE,
		ALLIED;
	}
	
	public static final Relationship defaultRelation = Relationship.PEACE;
	
	private String name;
	private Konquest konquest;
	private KonCapital capital;
	private KonMonumentTemplate monumentTemplate;
	private HashMap<String, KonTown> townMap;
	private boolean isSmallest;
	private boolean isPeaceful;
	private boolean isOfflineProtected;
	private Timer protectedWarmupTimer;
	private boolean isAdminOperated;
	private boolean isOpen;
	private RequestKeeper joinRequestKeeper;
	private UUID master;
	private Map<UUID,Boolean> members; // True = officer, False = regular
	private Map<KonKingdom,Relationship> relationships;
	private Map<KonPropertyFlag,Boolean> properties;
	
	public KonKingdom(Location loc, String name, Konquest konquest) {
		this.name = name;
		this.konquest = konquest;
		this.capital = new KonCapital(loc, this, konquest);
		this.townMap = new HashMap<String, KonTown>();
		this.monumentTemplate = null; // new kingdoms start with null template
		this.isSmallest = false;
		this.isPeaceful = false;
		this.isOfflineProtected = true;
		this.protectedWarmupTimer = new Timer(this);
		this.isAdminOperated = false;
		this.isOpen = false;
		this.joinRequestKeeper = new RequestKeeper();
		this.master = null;
		this.members = new HashMap<UUID,Boolean>();
		this.relationships = new HashMap<KonKingdom,Relationship>();
		this.properties = new HashMap<KonPropertyFlag,Boolean>();
		initProperties();
	}
	
	// Constructor meant for Barbarians, created on startup
	public KonKingdom(String name, Konquest konquest) {
		this.name = name;
		this.konquest = konquest;
		this.capital = new KonCapital(new Location(konquest.getPlugin().getServer().getWorld("world"),0,65,0), "Barbarian", this, konquest);
		this.townMap = new HashMap<String, KonTown>();
	}
	
	private void initProperties() {
		properties.clear();
		properties.put(KonPropertyFlag.NEUTRAL, 	konquest.getConfigManager().getConfig("properties").getBoolean("properties.kingdoms.neutral"));
		properties.put(KonPropertyFlag.GOLEMS, 		konquest.getConfigManager().getConfig("properties").getBoolean("properties.kingdoms.golems"));
	}
	
	public int initCapital() {
		int status = capital.initClaim();
		if(status != 0) {
			ChatUtil.printDebug("Problem initializing capital of "+name);
		}
		return status;
	}
	
	public void setIsOpen(boolean val) {
		isOpen = val;
	}
	
	public boolean isOpen() {
		return isOpen ? true : false;
	}
	
	public void setIsAdminOperated(boolean val) {
		isAdminOperated = val;
	}
	
	public boolean isAdminOperated() {
		return isAdminOperated ? true : false;
	}

	@Override
	public boolean setPropertyValue(KonPropertyFlag property, boolean value) {
		boolean result = false;
		if(properties.containsKey(property)) {
			properties.put(property, value);
			result = true;
		}
		return result;
	}

	@Override
	public boolean getPropertyValue(KonPropertyFlag property) {
		boolean result = false;
		if(properties.containsKey(property)) {
			result = properties.get(property);
		}
		return result;
	}
	
	@Override
	public boolean hasPropertyValue(KonPropertyFlag property) {
		return properties.containsKey(property);
	}

	@Override
	public Map<KonPropertyFlag, Boolean> getAllProperties() {
		return new HashMap<KonPropertyFlag, Boolean>(properties);
	}
	
	/*
	 * =================================================
	 * Membership Rank Methods
	 * =================================================
	 */
	
	public void forceMaster(UUID id) {
		master = id;
		members.put(id,true); // Ensure member officer flag is true
	}
	
	public boolean setMaster(UUID id) {
		// Master must be an existing member
		boolean result = false;
		if(!isAdminOperated && members.containsKey(id)) {
			master = id;
			members.put(id,true); // Ensure member officer flag is true
			result = true;
		}
		return result;
	}
	
	public boolean isMaster(UUID id) {
		boolean status = false;
		if(master != null) {
			status = id.equals(master);
		}
		return status;
	}
	
	/**
	 * Set a member's officer status
	 * @param id - Member UUID
	 * @param val - True to make officer, false to make regular
	 * @return True when member is updated, false if id is not a member or is master
	 */
	public boolean setOfficer(UUID id, boolean val) {
		// Target ID must be a member to modify officer flag
		boolean status = false;
		if(members.containsKey(id) && !master.equals(id)) {
			members.put(id,val);
			status = true;
		}
		return status;
	}
	
	// Returns true when player is Master or Officer
	public boolean isOfficer(UUID id) {
		boolean status = false;
		if(members.containsKey(id)) {
			status = members.get(id);
		}
		return status;
	}
	
	/**
	 * 
	 * @param id - Player UUID to add as a member
	 * @param isOfficer - officer flag, True = officer, False = regular
	 * @return True if successfully added, false if already a member
	 */
	public boolean addMember(UUID id, boolean isOfficer) {
		boolean status = false;
		if(!members.containsKey(id)) {
			members.put(id,isOfficer);
			status = true;
		}
		return status;
	}
	
	/**
	 * 
	 * @param id - Player UUID to remove from members
	 * @return True if id was successfully removed, false if id was not a member or was master
	 */
	public boolean removeMember(UUID id) {
		// Master cannot be removed as a member
		boolean status = false;
		if(members.containsKey(id) && !master.equals(id)) {
			members.remove(id);
			status = true;
		}
		return status;
	}
	
	public boolean isMember(UUID id) {
		return members.containsKey(id);
	}
	
	public boolean isMasterValid() {
		return master != null;
	}
	
	public UUID getMaster() {
		return master;
	}
	
	public OfflinePlayer getPlayerMaster() {
		if(master != null) {
			return Bukkit.getOfflinePlayer(master);
		}
		return null;
	}
	
	public ArrayList<OfflinePlayer> getPlayerOfficers() {
		ArrayList<OfflinePlayer> officerList = new ArrayList<OfflinePlayer>();
		for(UUID id : members.keySet()) {
			if(members.get(id)) {
				officerList.add(Bukkit.getOfflinePlayer(id));
			}
		}
		return officerList;
	}
	
	public ArrayList<OfflinePlayer> getPlayerOfficersOnly() {
		ArrayList<OfflinePlayer> officerList = new ArrayList<OfflinePlayer>();
		for(UUID id : members.keySet()) {
			if(members.get(id) && !master.equals(id)) {
				officerList.add(Bukkit.getOfflinePlayer(id));
			}
		}
		return officerList;
	}
	
	public ArrayList<OfflinePlayer> getPlayerMembers() {
		ArrayList<OfflinePlayer> memberList = new ArrayList<OfflinePlayer>();
		for(UUID id : members.keySet()) {
			memberList.add(Bukkit.getOfflinePlayer(id));
		}
		return memberList;
	}
	
	public ArrayList<OfflinePlayer> getPlayerMembersOnly() {
		ArrayList<OfflinePlayer> memberList = new ArrayList<OfflinePlayer>();
		for(UUID id : members.keySet()) {
			if(!members.get(id)) {
				memberList.add(Bukkit.getOfflinePlayer(id));
			}
		}
		return memberList;
	}
	
	/*
	 * =================================================
	 * Join Request Methods
	 * =================================================
	 */
	
	// Players who have tried joining but need to be added
	public List<OfflinePlayer> getJoinRequests() {
		return joinRequestKeeper.getJoinRequests();
	}
	
	// Players who have been added but need to join
	public List<OfflinePlayer> getJoinInvites() {
		return joinRequestKeeper.getJoinInvites();
	}
	
	public boolean addJoinRequest(UUID id, Boolean type) {
		return joinRequestKeeper.addJoinRequest(id, type);
	}
	
	// Does the player have an existing request to be added?
	public boolean isJoinRequestValid(UUID id) {
		return joinRequestKeeper.isJoinRequestValid(id);
	}
	
	// Does the player have an existing invite to join?
	public boolean isJoinInviteValid(UUID id) {
		return joinRequestKeeper.isJoinInviteValid(id);
	}
	
	public void removeJoinRequest(UUID id) {
		joinRequestKeeper.removeJoinRequest(id);
	}
	
	/*
	 * =================================================
	 * Relationship Methods
	 * =================================================
	 */
	
	public boolean addRelation(KonKingdom kingdom, Relationship relation) {
		boolean result = false;
		if(!relationships.containsKey(kingdom) && !kingdom.equals(this)) {
			relationships.put(kingdom,relation);
			result = true;
		}
		return result;
	}
	
	public boolean removeRelation(KonKingdom kingdom) {
		boolean result = false;
		if(relationships.containsKey(kingdom)) {
			relationships.remove(kingdom);
			result = true;
		}
		return result;
	}
	
	// No relationship means peace
	public boolean hasRelation(KonKingdom kingdom) {
		return relationships.containsKey(kingdom);
	}
	
	public Relationship getRelation(KonKingdom kingdom) {
		Relationship result = defaultRelation;
		if(relationships.containsKey(kingdom)) {
			result = relationships.get(kingdom);
		}
		return result;
	}
	
	public Collection<KonKingdom> getRelationKingdoms() {
		Set<KonKingdom> kingdoms = new HashSet<KonKingdom>();
		kingdoms.addAll(relationships.keySet());
		return kingdoms;
	}
	
	public List<String> getRelationNames() {
		List<String> result = new ArrayList<String>();
		for(KonKingdom kingdom : relationships.keySet()) {
			result.add(kingdom.getName());
		}
		return result;
	}
	
	/*
	 * =================================================
	 * Query Methods
	 * =================================================
	 */
	
	public int getNumMembers() {
		return members.size();
	}
	
	public int getNumMembersOnline() {
		int result = 0;
		for(UUID id : members.keySet()) {
			if(Bukkit.getOfflinePlayer(id).isOnline()) {
				result++;
			}
		}
		return result;
	}
	
	/*
	public int getNumTowns() {
		int result = 0;
		UUID lord = null;
		for(KonTown town : this.getTowns()) {
			lord = town.getLord();
			if(lord != null && members.containsKey(lord)) {
				result++;
			}
		}
		return result;
	}
	
	public int getNumLand() {
		int result = 0;
		UUID lord = null;
		for(KonTown town : this.getTowns()) {
			lord = town.getLord();
			if(lord != null && members.containsKey(lord)) {
				result += town.getChunkList().size();
			}
		}
		return result;
	}
	*/
	
	/*
	 * =================================================
	 * Original Kingdom Methods
	 * =================================================
	 */
	
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
		boolean isBreakDisabledOffline = konquest.getConfigManager().getConfig("core").getBoolean(CorePath.KINGDOMS_NO_ENEMY_EDIT_OFFLINE.getPath());
		return isOfflineProtected && isBreakDisabledOffline;
	}
	
	public void setOfflineProtected(boolean val) {
		isOfflineProtected = val;
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
	
	public void updateMonumentTemplate(KonMonumentTemplate template) {
		setMonumentTemplate(template);
		//TODO: Update all town monuments from new template
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
