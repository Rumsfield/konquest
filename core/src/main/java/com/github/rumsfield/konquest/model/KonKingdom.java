package com.github.rumsfield.konquest.model;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestDiplomacyType;
import com.github.rumsfield.konquest.api.model.KonquestKingdom;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.api.model.KonquestUpgrade;
import com.github.rumsfield.konquest.utility.Timer;
import com.github.rumsfield.konquest.utility.*;
import org.bukkit.*;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.*;

public class KonKingdom implements Timeable, KonquestKingdom, KonPropertyFlagHolder {
	
	public static final KonquestDiplomacyType defaultRelation = KonquestDiplomacyType.getDefault();
	
	private String name;
	private final Konquest konquest;
	private KonCapital capital;
	private KonTown swapCapital;
	private KonMonumentTemplate monumentTemplate;
	private final HashMap<String, KonTown> townMap;
	private boolean isSmallest;
	private boolean isOfflineProtected;
	private final Timer protectedWarmupTimer;
	private final Timer capitalSwapWarmupTimer;
	private final Timer capitalSwapCountdownTimer;
	private int capitalSwapWarmup;
	private final boolean isCreated;
	private boolean isAdminOperated;
	private boolean isOpen;
	private boolean isLegacy;
	private final RequestKeeper joinRequestKeeper;
	private UUID master;
	private final Map<UUID,Boolean> members; // True = officer, False = regular
	private final Map<KonquestKingdom, KonquestDiplomacyType> activeRelationships; // This kingdom's active diplomatic state with other kingdoms
	private final Map<KonquestKingdom, KonquestDiplomacyType> requestRelationships; // Other kingdom's requested diplomatic state with this kingdom
	private final Map<KonPropertyFlag,Boolean> properties;
	private int webColor;
	
	public KonKingdom(Location loc, String name, Konquest konquest) {
		this.name = name;
		this.konquest = konquest;
		this.capital = new KonCapital(loc, this, konquest);
		this.swapCapital = null;
		this.townMap = new HashMap<>();
		this.monumentTemplate = null; // new kingdoms start with null template
		this.isSmallest = false;
		this.isOfflineProtected = true;
		this.protectedWarmupTimer = new Timer(this);
		this.capitalSwapWarmupTimer = new Timer(this);
		this.capitalSwapCountdownTimer = new Timer(this);
		this.capitalSwapWarmup = 1;
		this.isCreated = true;
		this.isAdminOperated = false;
		this.isOpen = false;
		this.isLegacy = false;
		this.joinRequestKeeper = new RequestKeeper();
		this.master = null;
		this.members = new HashMap<>();
		this.activeRelationships = new HashMap<>();
		this.requestRelationships = new HashMap<>();
		this.properties = new HashMap<>();
		initProperties();
		this.webColor = -1;
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
		this.capitalSwapWarmupTimer = new Timer(this);
		this.capitalSwapCountdownTimer = new Timer(this);
		this.capitalSwapWarmup = 1;
		this.isCreated = false;
		this.isAdminOperated = false;
		this.isOpen = true;
		this.isLegacy = false;
		this.joinRequestKeeper = new RequestKeeper();
		this.master = null;
		this.members = new HashMap<>();
		this.activeRelationships = new HashMap<>();
		this.requestRelationships = new HashMap<>();
		this.properties = new HashMap<>();
		this.webColor = -1;
	}

	public static List<KonPropertyFlag> getProperties() {
		List<KonPropertyFlag> result = new ArrayList<>();
		result.add(KonPropertyFlag.PEACEFUL);
		result.add(KonPropertyFlag.GOLEMS);
		result.add(KonPropertyFlag.JOIN);
		result.add(KonPropertyFlag.LEAVE);
		result.add(KonPropertyFlag.PROMOTE);
		result.add(KonPropertyFlag.DEMOTE);
		result.add(KonPropertyFlag.TRANSFER);
		return result;
	}

	@Override
	public void initProperties() {
		properties.clear();
		for (KonPropertyFlag flag : getProperties()) {
			properties.put(flag, konquest.getConfigManager().getConfig("properties").getBoolean("properties.kingdoms."+flag.toString().toLowerCase()));
		}
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

	public void setIsLegacy(boolean val) {
		isLegacy = val;
	}

	public boolean isLegacy() {
		return isLegacy;
	}
	
	public void setIsAdminOperated(boolean val) {
		isAdminOperated = val;
	}
	
	public boolean isAdminOperated() {
		return isAdminOperated;
	}
	
	public boolean isPeaceful() {
		return getPropertyValue(KonPropertyFlag.PEACEFUL);
	}

	public boolean isJoinable() {
		return getPropertyValue(KonPropertyFlag.JOIN);
	}

	public boolean isLeaveable() {
		return getPropertyValue(KonPropertyFlag.LEAVE);
	}

	public boolean isPromoteable() {
		return getPropertyValue(KonPropertyFlag.PROMOTE);
	}

	public boolean isDemoteable() {
		return getPropertyValue(KonPropertyFlag.DEMOTE);
	}

	public boolean isTransferable() {
		return getPropertyValue(KonPropertyFlag.TRANSFER);
	}

	public void setWebColor(int val) {
		webColor = val;
	}

	public int getWebColor() {
		return webColor;
	}

	public String getWebColorString() {
		int webColorHash = webColor;
		if(webColor == -1) {
			webColorHash = getName().hashCode() & 0xFFFFFF;
		}
		return ChatUtil.parseHex(String.format("#%06X",webColorHash));
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
		// Master must be an existing member, and kingdom cannot be admin operated
		if(isCreated && !isAdminOperated && members.containsKey(id)) {
			master = id;
			members.put(id,true); // Ensure member officer flag is true
			return true;
		}
		return false;
	}
	
	public boolean isMaster(UUID id) {
		// When kingdom is created and not admin operated, check if master id matches
		if(isCreated && !isAdminOperated && master != null) {
			return id.equals(master);
		}
		return false;
	}
	
	/**
	 * Set a member's officer status
	 * @param id - Member UUID
	 * @param val - True to make officer, false to make regular
	 * @return True when member is updated, false if id is not a member or is master
	 */
	public boolean setOfficer(UUID id, boolean val) {
		// Target ID must be a member to modify officer flag (not a master)
		if(master != null && master.equals(id)) {
			return false;
		}
		if(isCreated && members.containsKey(id)) {
			members.put(id,val);
			return true;
		}
		return false;
	}
	
	// Returns true when player is Master or Officer
	public boolean isOfficer(UUID id) {
		if(isCreated && members.containsKey(id)) {
			return members.get(id);
		}
		return false;
	}
	
	/**
	 * 
	 * @param id - Player UUID to add as a member
	 * @param isOfficer - officer flag, True = officer, False = regular
	 * @return True if successfully added, false if already a member
	 */
	public boolean addMember(UUID id, boolean isOfficer) {
		// Cannot add members if kingdom is not created
		if(!isCreated) {
			ChatUtil.printDebug("Failed to add member to non-created kingdom "+getName());
			return false;
		}
		// Cannot add existing members
		if(members.containsKey(id)) {
			ChatUtil.printDebug("Failed to add existing member to kingdom "+getName());
			return false;
		}
		// Add member
		members.put(id,isOfficer);
		return true;
	}
	
	/**
	 * Removes a player member, as well as their town residencies.
	 * Only works for created kingdoms.
	 * When a kingdom is admin operated, expect master to be null.
	 * 
	 * @param id - Player UUID to remove from members
	 * @return True if id was successfully removed, false if id was not a member or was master
	 */
	public boolean removeMember(UUID id) {
		// Master cannot be removed as a member
		if(master != null && master.equals(id)) {
			ChatUtil.printDebug("Failed to remove master from kingdom "+getName());
			return false;
		}
		// Cannot remove members if kingdom is not created
		if(!isCreated) {
			ChatUtil.printDebug("Failed to remove member from non-created kingdom "+getName());
			return false;
		}
		if(!members.containsKey(id)) {
			ChatUtil.printDebug("Failed to remove non-existent member from kingdom "+getName());
			return false;
		}
		// Remove membership
		members.remove(id);
		// Remove residencies
		removeTownResidencies(id);
		return true;
	}

	public void removeTownResidencies(UUID id) {
		OfflinePlayer townPlayer = Bukkit.getOfflinePlayer(id);
		for(KonTown town : getCapitalTowns()) {
			if(town.removePlayerResident(townPlayer)) {
				konquest.getMapHandler().drawLabel(town);
			}
		}
	}
	
	public boolean isMember(UUID id) {
		return isCreated && members.containsKey(id);
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
	
	public @Nullable OfflinePlayer getPlayerMaster() {
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
			if(!(master != null && master.equals(id)) && members.get(id)) {
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

	public String getPlayerRoleName(KonOfflinePlayer offlinePlayer) {
		return getPlayerRoleName(offlinePlayer.getOfflineBukkitPlayer());
	}
	public String getPlayerRoleName(OfflinePlayer offlinePlayer) {
		String result = "";
		if(isMaster(offlinePlayer.getUniqueId())) {
			result = MessagePath.LABEL_MASTER.getMessage();
		} else if(isOfficer(offlinePlayer.getUniqueId())) {
			result = MessagePath.LABEL_OFFICER.getMessage();
		} else if(isMember(offlinePlayer.getUniqueId())) {
			result = MessagePath.LABEL_MEMBER.getMessage();
		}
		return result;
	}

	public String getPlayerRankName(UUID id) {
		String result = "";
		if(isMaster(id)) {
			result = MessagePath.PLACEHOLDER_RANK_MASTER.getMessage();
		} else if(isOfficer(id)) {
			result = MessagePath.PLACEHOLDER_RANK_OFFICER.getMessage();
		} else if(isMember(id)) {
			result = MessagePath.PLACEHOLDER_RANK_MEMBER.getMessage();
		}
		return result;
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
	// Kingdom manager handles valid relationships, these methods are simple book-keeping
	// If this kingdom is peaceful, never change active relations to other than default (peace)
	
	/* Active Relationship */
	
	public void setActiveRelation(KonquestKingdom kingdom, KonquestDiplomacyType relation) {
		// Only allow created kingdoms to set relations
		if(!isCreated || !kingdom.isCreated()) {
			return;
		}
		// Prevent setting self relation
		if(kingdom.equals(this)) {
			return;
		}
		// Prevent relations when peaceful
		if(isPeaceful()) {
			return;
		}
		// Only add non-default relationships
		if(relation.equals(defaultRelation)) {
			// Remove map entry when default
			activeRelationships.remove(kingdom);
		} else {
			// Set relationship
			activeRelationships.put(kingdom,relation);
		}
	}
	
	public KonquestDiplomacyType getActiveRelation(KonquestKingdom kingdom) {
		return activeRelationships.getOrDefault(kingdom, defaultRelation);
	}
	
	public void removeActiveRelation(KonquestKingdom kingdom) {
		activeRelationships.remove(kingdom);
	}
	
	public void clearActiveRelations() {
		activeRelationships.clear();
	}
	
	public Collection<KonquestKingdom> getActiveRelationKingdoms() {
		return new HashSet<>(activeRelationships.keySet());
	}

	public List<KonKingdom> getActiveRelationKingdoms(KonquestDiplomacyType relation) {
		List<KonKingdom> result = new ArrayList<>();
		for(KonquestKingdom kingdom : activeRelationships.keySet()) {
			if(activeRelationships.get(kingdom).equals(relation) && kingdom instanceof KonKingdom) {
				result.add((KonKingdom)kingdom);
			}
		}
		return result;
	}
	
	/* Request Relationship */
	
	public void setRelationRequest(KonquestKingdom kingdom, KonquestDiplomacyType relation) {
		// Only allow created kingdoms to set relations
		if(!isCreated || !kingdom.isCreated()) {
			return;
		}
		// Prevent setting self relation
		if(kingdom.equals(this)) {
			return;
		}
		// Set request relationship
		requestRelationships.put(kingdom,relation);
	}
	
	public KonquestDiplomacyType getRelationRequest(KonquestKingdom kingdom) {
		return requestRelationships.getOrDefault(kingdom, defaultRelation);
	}
	
	public boolean hasRelationRequest(KonquestKingdom kingdom) {
		return requestRelationships.containsKey(kingdom);
	}
	
	public void removeRelationRequest(KonquestKingdom kingdom) {
		requestRelationships.remove(kingdom);
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
	 * Capital, Town and Miscellaneous Methods
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
			oldTown.purgeResidents();
			oldTown.clearPlots();
			oldTown.clearUpgrades();
			oldTown.clearShieldsArmors();
			oldTown.stopTimers();
			boolean pass = oldTown.removeMonumentBlocks();
			if(!pass) {
				ChatUtil.printDebug("Encountered problem removing monument blocks");
			}
			oldTown = null;
			return true;
		} else {
			return false;
		}
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
			townMap.get(newName).updateBarTitle();
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
		boolean isBreakDisabledOffline = konquest.getCore().getBoolean(CorePath.KINGDOMS_NO_ENEMY_EDIT_OFFLINE.getPath());
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

	public void clearMonumentTemplate() {
		monumentTemplate = null;
	}
	
	public boolean hasMonumentTemplate() {
		return monumentTemplate != null;
	}
	
	public void updateMonumentTemplate(KonMonumentTemplate template) {
		// Update kingdom's template
		setMonumentTemplate(template);
		// Update all towns + capital monument
		for(KonTown town : getCapitalTowns()) {
			town.updateMonumentFromTemplate();
		}
		// Reload towns in loaded chunks
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

	public ArrayList<KonTown> getCapitalTowns() {
		ArrayList<KonTown> result = new ArrayList<>(townMap.values());
		result.add(capital);
		return result;
	}

	public KonTown getTownCapital(String name) {
		KonTown resultTown = null;
		boolean isTown = hasTown(name);
		boolean isCapital = hasCapital(name);
		if (isTown) {
			resultTown = getTown(name);
		} else if (isCapital) {
			resultTown = getCapital();
		}
		return resultTown;
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
		if (taskID == 0) {
			ChatUtil.printDebug("Kingdom Timer ended with null taskID!");
		} else if (taskID == protectedWarmupTimer.getTaskID()) {
			ChatUtil.printDebug("Kingdom protection warmup Timer ended with taskID: "+taskID);
			isOfflineProtected = true;
		} else if (taskID == capitalSwapWarmupTimer.getTaskID()) {
			ChatUtil.printDebug("Kingdom capital swap warmup Timer ended with taskID: "+taskID);
			// When a capital swap warmup timer ends
			capitalSwapCountdownTimer.stopTimer();
			swapCapital.updateCapitalSwapBar(0, true, "");
			capital.updateCapitalSwapBar(0, false, "");
			swapCapitalToTown(swapCapital);
			// Reset links
			swapCapital = null;
			capitalSwapWarmup = 0;
		} else if (taskID == capitalSwapCountdownTimer.getTaskID()) {
			// This ticks every second to show the countdown time
			String remainingTime = HelperUtil.getTimeFormat(capitalSwapWarmupTimer.getTime(),ChatColor.GOLD);
			double progress = 1;
			if (capitalSwapWarmup > 0) {
				progress = (double)capitalSwapWarmupTimer.getTime() / capitalSwapWarmup;
			}
			swapCapital.updateCapitalSwapBar(progress, true, remainingTime);
			capital.updateCapitalSwapBar(progress, false, remainingTime);
		}
	}
	
	public Timer getProtectedWarmupTimer() {
		return protectedWarmupTimer;
	}

	public void startCapitalSwapWarmup(KonTown town, int warmupTime) {
		swapCapital = town;
		capitalSwapWarmup = warmupTime;
		// Start warmup
		capitalSwapWarmupTimer.stopTimer();
		capitalSwapWarmupTimer.setTime(warmupTime);
		capitalSwapWarmupTimer.startTimer();
		// Start countdown
		capitalSwapCountdownTimer.stopTimer();
		capitalSwapCountdownTimer.setTime(0); // 1 second loop
		capitalSwapCountdownTimer.startLoopTimer();
		// Update display bars
		String remainingTime = HelperUtil.getTimeFormat(warmupTime,ChatColor.GOLD);
		swapCapital.updateCapitalSwapBar(1, true, remainingTime);
		capital.updateCapitalSwapBar(1, false, remainingTime);
		// Send broadcast
		ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_CAPITAL_WARMUP.getMessage(getName(),town.getName(),remainingTime));
	}

	public void cancelCapitalSwapWarmup() {
		// Update display bars
		if (swapCapital != null) {
			swapCapital.updateCapitalSwapBar(0, true, "");
			// Send broadcast
			ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_CAPITAL_WARMUP.getMessage(getName(),swapCapital.getName()));
		}
		capital.updateCapitalSwapBar(0, false, "");
		// Stop timers
		capitalSwapWarmupTimer.stopTimer();
		capitalSwapCountdownTimer.stopTimer();
		// Reset links
		swapCapital = null;
		capitalSwapWarmup = 0;
	}

	public boolean isCapitalSwapInProgress(KonTown town) {
		return capitalSwapWarmup > 0 && swapCapital != null && (capital.equals(town) || swapCapital.equals(town));
	}

	public boolean swapCapitalToTown(KonTown town) {
		// Check conditions
		if (town == null || this.capital == null) return false;
		if (!town.getKingdom().equals(this)) return false;
		if (town.getTerritoryType().equals(KonquestTerritoryType.CAPITAL)) return false;
		String townName = town.getName();
		// Make a new Capital from town
		KonCapital newCapital = (KonCapital)town.copy(new KonCapital(town.getCenterLoc(),this,konquest));
		// Make a new Town from the capital, using the original town's name
		KonTown newTown = capital.copy(new KonTown(capital.getCenterLoc(),townName,this,konquest));
		// Replace Capital
		if(capital != null) {
			capital.removeAllBarPlayers();
			capital.stopTimers();
			konquest.getTerritoryManager().removeAllTerritory(capital.getWorld(),capital.getChunkList().keySet());
			konquest.getMapHandler().drawRemoveTerritory(capital);
		}
		capital = newCapital;
		capital.updateBarTitle();
		capital.updateBarPlayers();
		konquest.getKingdomManager().refreshTownNerfs(capital);
		konquest.getKingdomManager().refreshTownHearts(capital);
		konquest.getUpgradeManager().updateTownDisabledUpgrades(capital);
		konquest.getTerritoryManager().addAllTerritory(capital.getWorld(),capital.getChunkList());
		konquest.getMapHandler().drawUpdateTerritory(capital);
		// Replace Town
		KonTown oldTown = townMap.remove(townName);
		if(oldTown != null) {
			oldTown.removeAllBarPlayers();
			oldTown.stopTimers();
			konquest.getTerritoryManager().removeAllTerritory(oldTown.getWorld(),oldTown.getChunkList().keySet());
			konquest.getMapHandler().drawRemoveTerritory(oldTown);
		}
		townMap.put(townName, newTown);
		newTown.updateBarTitle();
		newTown.updateBarPlayers();
		konquest.getKingdomManager().refreshTownNerfs(newTown);
		konquest.getKingdomManager().refreshTownHearts(newTown);
		konquest.getUpgradeManager().updateTownDisabledUpgrades(newTown);
		konquest.getTerritoryManager().addAllTerritory(newTown.getWorld(),newTown.getChunkList());
		konquest.getMapHandler().drawUpdateTerritory(newTown);
		// Send broadcast
		ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_CAPITAL_SWAP.getMessage(getName(),townName));
		return true;
	}

	public void reloadLoadedTownMonuments() {
		Point tPoint;
		List<KonTown> allTownsAndCapital = new ArrayList<>();
		allTownsAndCapital.add(getCapital());
		allTownsAndCapital.addAll(getTowns());
		for(KonTown town : allTownsAndCapital) {
			tPoint = HelperUtil.toPoint(town.getCenterLoc());
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
					Konquest.playTownSettleSound(town.getCenterLoc());
					town.reloadMonument();
				}
			}
		}
	}
}
