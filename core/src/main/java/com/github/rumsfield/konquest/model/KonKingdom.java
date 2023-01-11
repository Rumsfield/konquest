package com.github.rumsfield.konquest.model;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestKingdom;
import com.github.rumsfield.konquest.api.model.KonquestRelationship;
import com.github.rumsfield.konquest.utility.Timer;
import com.github.rumsfield.konquest.utility.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.List;
import java.util.*;

public class KonKingdom implements Timeable, KonquestKingdom, KonPropertyFlagHolder {
	
	public static final KonquestRelationship defaultRelation = KonquestRelationship.PEACE;
	
	private String name;
	private final Konquest konquest;
	private final KonCapital capital;
	private KonMonumentTemplate monumentTemplate;
	private final HashMap<String, KonTown> townMap;
	private boolean isSmallest;
	private boolean isOfflineProtected;
	private final Timer protectedWarmupTimer;
	private final boolean isCreated;
	private boolean isAdminOperated;
	private boolean isOpen;
	private final RequestKeeper joinRequestKeeper;
	private UUID master;
	private final Map<UUID,Boolean> members; // True = officer, False = regular
	private final Map<KonquestKingdom,KonquestRelationship> activeRelationships; // Active relation state
	private final Map<KonquestKingdom,KonquestRelationship> requestRelationships; // Current relation requests to change state
	private final Map<KonPropertyFlag,Boolean> properties;
	
	public KonKingdom(Location loc, String name, Konquest konquest) {
		this.name = name;
		this.konquest = konquest;
		this.capital = new KonCapital(loc, this, konquest);
		this.townMap = new HashMap<>();
		this.monumentTemplate = null; // new kingdoms start with null template
		this.isSmallest = false;
		this.isOfflineProtected = true;
		this.protectedWarmupTimer = new Timer(this);
		this.isCreated = true;
		this.isAdminOperated = false;
		this.isOpen = false;
		this.joinRequestKeeper = new RequestKeeper();
		this.master = null;
		this.members = new HashMap<>();
		this.activeRelationships = new HashMap<>();
		this.requestRelationships = new HashMap<>();
		this.properties = new HashMap<>();
		initProperties();
	}
	
	// Constructor meant for default kingdoms created on startup (Barbarians, Neutrals)
	public KonKingdom(String name, Konquest konquest) {
		this.name = name;
		this.konquest = konquest;
		this.capital = new KonCapital(new Location(konquest.getPlugin().getServer().getWorld("world"),0,65,0), "konquest_default", this, konquest);
		this.townMap = new HashMap<>();
		this.monumentTemplate = null; // new kingdoms start with null template
		this.isSmallest = false;
		this.isOfflineProtected = false;
		this.protectedWarmupTimer = new Timer(this);
		this.isCreated = false;
		this.isAdminOperated = false;
		this.isOpen = true;
		this.joinRequestKeeper = new RequestKeeper();
		this.master = null;
		this.members = new HashMap<>();
		this.activeRelationships = new HashMap<>();
		this.requestRelationships = new HashMap<>();
		this.properties = new HashMap<>();
	}
	
	private void initProperties() {
		properties.clear();
		properties.put(KonPropertyFlag.NEUTRAL, 	konquest.getConfigManager().getConfig("properties").getBoolean("properties.kingdoms.neutral"));
		properties.put(KonPropertyFlag.GOLEMS, 		konquest.getConfigManager().getConfig("properties").getBoolean("properties.kingdoms.golems"));
	}
	
	public boolean isCreated() {
		return isCreated;
	}
	
	public void setIsOpen(boolean val) {
		isOpen = val;
	}
	
	public boolean isOpen() {
		return isOpen;
	}
	
	public void setIsAdminOperated(boolean val) {
		isAdminOperated = val;
	}
	
	public boolean isAdminOperated() {
		return isAdminOperated;
	}
	
	public boolean isPeaceful() {
		return getPropertyValue(KonPropertyFlag.NEUTRAL);
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
		return new HashMap<>(properties);
	}
	
	/*
	 * =================================================
	 * Membership Rank Methods
	 * =================================================
	 * Only created kingdoms can have members
	 */
	
	public void forceMaster(UUID id) {
		if(!isCreated)return;
		master = id;
		members.put(id,true); // Ensure member officer flag is true

	}
	
	public boolean setMaster(UUID id) {
		// Master must be an existing member
		boolean result = false;
		if(isCreated && !isAdminOperated && members.containsKey(id)) {
			master = id;
			members.put(id,true); // Ensure member officer flag is true
			result = true;
		}
		return result;
	}
	
	public boolean isMaster(UUID id) {
		boolean status = false;
		if(isCreated && master != null) {
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
		if(isCreated && members.containsKey(id) && !master.equals(id)) {
			members.put(id,val);
			status = true;
		}
		return status;
	}
	
	// Returns true when player is Master or Officer
	public boolean isOfficer(UUID id) {
		boolean status = false;
		if(isCreated && members.containsKey(id)) {
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
		if(isCreated && !members.containsKey(id)) {
			members.put(id,isOfficer);
			status = true;
		}
		return status;
	}
	
	/**
	 * Removes a player member, as well as their town residencies.
	 * Only works for created kingdoms.
	 * 
	 * @param id - Player UUID to remove from members
	 * @return True if id was successfully removed, false if id was not a member or was master
	 */
	public boolean removeMember(UUID id) {
		// Master cannot be removed as a member
		boolean status = false;
		if(isCreated && members.containsKey(id) && !master.equals(id)) {
			// Remove membership
			members.remove(id);
			// Remove residencies
	    	OfflinePlayer townPlayer = Bukkit.getOfflinePlayer(id);
	    	for(KonTown town : getTowns()) {
	    		if(town.removePlayerResident(townPlayer)) {
	    			konquest.getMapHandler().drawDynmapLabel(town);
	    		}
	    	}
			status = true;
		}
		return status;
	}
	
	public boolean isMember(UUID id) {
		return isCreated && members.containsKey(id);
	}
	
	public boolean isMember(Player player) {
		return isMember(player.getUniqueId());
	}
	
	public boolean isMasterValid() {
		return isCreated && master != null;
	}
	
	public UUID getMaster() {
		return master;
	}
	
	public void clearMaster() {
		master = null;
	}
	
	public void clearMembers() {
		members.clear();
	}
	
	public OfflinePlayer getPlayerMaster() {
		if(isCreated && master != null) {
			return Bukkit.getOfflinePlayer(master);
		}
		return null;
	}
	
	public ArrayList<OfflinePlayer> getPlayerOfficers() {
		ArrayList<OfflinePlayer> officerList = new ArrayList<>();
		for(UUID id : members.keySet()) {
			if(members.get(id)) {
				officerList.add(Bukkit.getOfflinePlayer(id));
			}
		}
		return officerList;
	}
	
	public ArrayList<OfflinePlayer> getPlayerOfficersOnly() {
		ArrayList<OfflinePlayer> officerList = new ArrayList<>();
		for(UUID id : members.keySet()) {
			if(members.get(id) && !master.equals(id)) {
				officerList.add(Bukkit.getOfflinePlayer(id));
			}
		}
		return officerList;
	}
	
	public ArrayList<OfflinePlayer> getPlayerMembers() {
		ArrayList<OfflinePlayer> memberList = new ArrayList<>();
		for(UUID id : members.keySet()) {
			memberList.add(Bukkit.getOfflinePlayer(id));
		}
		return memberList;
	}
	
	public ArrayList<OfflinePlayer> getPlayerMembersOnly() {
		ArrayList<OfflinePlayer> memberList = new ArrayList<>();
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
	
	/* Active Relationship */
	
	public boolean setActiveRelation(KonquestKingdom kingdom, KonquestRelationship relation) {
		boolean result = false;
		if(!activeRelationships.containsKey(kingdom) && !kingdom.equals(this)) {
			activeRelationships.put(kingdom,relation);
			result = true;
		}
		return result;
	}
	
	public KonquestRelationship getActiveRelation(KonquestKingdom kingdom) {
		KonquestRelationship result = defaultRelation;
		if(activeRelationships.containsKey(kingdom)) {
			result = activeRelationships.get(kingdom);
		}
		return result;
	}
	
	public boolean removeActiveRelation(KonquestKingdom kingdom) {
		boolean result = false;
		if(activeRelationships.containsKey(kingdom)) {
			activeRelationships.remove(kingdom);
			result = true;
		}
		return result;
	}
	
	public void clearActiveRelations() {
		activeRelationships.clear();
	}
	
	public Collection<KonquestKingdom> getActiveRelationKingdoms() {
		return new HashSet<>(activeRelationships.keySet());
	}
	
	public List<String> getActiveRelationNames() {
		List<String> result = new ArrayList<>();
		for(KonquestKingdom kingdom : activeRelationships.keySet()) {
			result.add(kingdom.getName());
		}
		return result;
	}
	
	/* Request Relationship */
	
	public boolean setRelationRequest(KonquestKingdom kingdom, KonquestRelationship relation) {
		boolean result = false;
		if(!requestRelationships.containsKey(kingdom) && !kingdom.equals(this)) {
			requestRelationships.put(kingdom,relation);
			result = true;
		}
		return result;
	}
	
	public KonquestRelationship getRelationRequest(KonquestKingdom kingdom) {
		KonquestRelationship result = defaultRelation;
		if(requestRelationships.containsKey(kingdom)) {
			result = requestRelationships.get(kingdom);
		}
		return result;
	}
	
	public boolean hasRelationRequest(KonquestKingdom kingdom) {
		return requestRelationships.containsKey(kingdom);
	}
	
	public boolean removeRelationRequest(KonquestKingdom kingdom) {
		boolean result = false;
		if(requestRelationships.containsKey(kingdom)) {
			requestRelationships.remove(kingdom);
			result = true;
		}
		return result;
	}
	
	public void clearRelationRequests() {
		requestRelationships.clear();
	}
	
	public Collection<KonquestKingdom> getRelationRequestKingdoms() {
		return new HashSet<>(requestRelationships.keySet());
	}
	
	public List<String> getRelationRequestNames() {
		List<String> result = new ArrayList<>();
		for(KonquestKingdom kingdom : requestRelationships.keySet()) {
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
	
	public int getNumTowns() {
		return townMap.size();
	}
	
	public int getNumLand() {
		int result = 0;
		for(KonTown town : townMap.values()) {
			result += town.getChunkList().size();
		}
		return result;
	}
	
	public boolean isCapitalImmune() {
		boolean result = true;
		int immunityThreshold = konquest.getCore().getInt(CorePath.KINGDOMS_CAPITAL_IMMUNITY_TOWNS.getPath(),0);
		if(immunityThreshold > 0) {
			// Capital can only be capture when there are fewer towns than threshold
			if(getNumTowns() < immunityThreshold) {
				result = false;
			}
		} else {
			// Capital can always be captured
			result = false;
		}
		return result;
	}
	
	
	/*
	 * =================================================
	 * Original Kingdom Methods
	 * =================================================
	 */
	
	public int initCapital() {
		int status = capital.initClaim();
		if(status != 0) {
			ChatUtil.printDebug("Problem initializing capital of "+name);
		}
		return status;
	}
	
	// Does not completely delete capital, but removes displays and monument
	public boolean removeCapital() {
		boolean result = false;
		if(capital != null) {
			ChatUtil.printDebug("Removed capital of kingdom "+name);
			capital.removeAllBarPlayers();
			result = capital.removeMonumentBlocks();
			if(!result) {
				ChatUtil.printDebug("Encountered problem removing monument blocks of capital");
			}
		}
		return result;
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
			return true;
		} else {
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
	 * @param name - Name of Town
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
	
	public String getMonumentTemplateName() {
		String result = "";
		if(monumentTemplate != null) {
			result = monumentTemplate.getName();
		}
		return result;
	}
	
	public boolean hasMonumentTemplate() {
		return monumentTemplate != null;
	}
	
	public void updateMonumentTemplate(KonMonumentTemplate template) {
		setMonumentTemplate(template);
		reloadLoadedTownMonuments();
	}
	
	public String getName() {
		return name;
	}
	
	// Careful! This can return null
	public KonCapital getCapital() {
		return capital;
	}
	
	public boolean hasCapital() {
		return capital != null;
	}
	
	public boolean hasCapital(String name) {
		// Check that name matches kingdom name
		return name.equalsIgnoreCase(this.name) && capital != null;
	}
	
	// This can return an empty list
	public ArrayList<String> getTownNames() {
		return new ArrayList<>(townMap.keySet());
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
		return new ArrayList<>(townMap.values());
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
