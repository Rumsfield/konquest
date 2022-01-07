package konquest.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Villager;

import konquest.utility.RequestKeeper;

public class KonGuild {

	private boolean isOpen;
	private String name;
	private RequestKeeper joinRequestKeeper;
	private UUID master; // Master should not be null
	private HashMap<UUID,Boolean> members; // True = officer, False = regular
	private Villager.Profession specialization;
	private KonKingdom kingdom;
	private HashSet<KonGuild> friendlySanction;
	private HashSet<KonGuild> enemyArmistice;
	
	public KonGuild(String name, UUID id, KonKingdom kingdom) {
		this.isOpen = false;
		this.name = name;
		this.joinRequestKeeper = new RequestKeeper();
		this.master = id;
		this.members = new HashMap<UUID,Boolean>();
		this.members.put(master, true);
		this.specialization = Villager.Profession.NONE;
		this.kingdom = kingdom;
		this.friendlySanction = new HashSet<KonGuild>();
		this.enemyArmistice = new HashSet<KonGuild>();
	}
	
	/*
	 * =================================================
	 * General Methods
	 * =================================================
	 */
	
	public void setIsOpen(boolean val) {
		isOpen = val;
	}
	
	public boolean isOpen() {
		return isOpen ? true : false;
	}
	
	public void setName(String newName) {
		name = newName;
	}
	
	public String getName() {
		return name;
	}
	
	public void setSpecialization(Villager.Profession newSpec) {
		specialization = newSpec;
	}
	
	public Villager.Profession getSpecialization() {
		return specialization;
	}
	
	public KonKingdom getKingdom() {
		return kingdom;
	}
	
	/*
	 * =================================================
	 * Membership Methods
	 * =================================================
	 */
	
	public boolean setMaster(UUID id) {
		// Master must be an existing member
		boolean result = false;
		if(members.containsKey(id)) {
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
		return master != null ? true : false;
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
	
	public boolean addSanction(KonGuild guild) {
		boolean result = false;
		if(!friendlySanction.contains(guild) && guild.getKingdom().equals(this.getKingdom())) {
			friendlySanction.add(guild);
			result = true;
		}
		return result;
	}
	
	public boolean removeSanction(KonGuild guild) {
		boolean result = false;
		if(friendlySanction.contains(guild) && guild.getKingdom().equals(this.getKingdom())) {
			friendlySanction.remove(guild);
			result = true;
		}
		return result;
	}
	
	public boolean isSanction(KonGuild guild) {
		return friendlySanction.contains(guild);
	}
	
	public List<String> getSanctionNames() {
		List<String> result = new ArrayList<String>();
		for(KonGuild guild : friendlySanction) {
			result.add(guild.getName());
		}
		return result;
	}
	
	public boolean addArmistice(KonGuild guild) {
		boolean result = false;
		if(!enemyArmistice.contains(guild) && !guild.getKingdom().equals(this.getKingdom())) {
			enemyArmistice.add(guild);
		}
		return result;
	}
	
	public boolean removeArmistice(KonGuild guild) {
		boolean result = false;
		if(enemyArmistice.contains(guild) && !guild.getKingdom().equals(this.getKingdom())) {
			enemyArmistice.remove(guild);
			result = true;
		}
		return result;
	}
	
	public boolean isArmistice(KonGuild guild) {
		return enemyArmistice.contains(guild);
	}
	
	public List<String> getArmisticeNames() {
		List<String> result = new ArrayList<String>();
		for(KonGuild guild : enemyArmistice) {
			result.add(guild.getName());
		}
		return result;
	}
	
	public void removeGuildRelationship(KonGuild guild) {
		enemyArmistice.remove(guild);
		friendlySanction.remove(guild);
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
	
	public boolean isTownMember(KonTown town) {
		if(town.getKingdom().equals(this.getKingdom())) {
			UUID lord = town.getLord();
			if(lord != null && members.containsKey(lord)) {
				return true;
			}
		}
		return false;
	}
	
	public int getNumTowns() {
		int result = 0;
		UUID lord = null;
		for(KonTown town : this.getKingdom().getTowns()) {
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
		for(KonTown town : this.getKingdom().getTowns()) {
			lord = town.getLord();
			if(lord != null && members.containsKey(lord)) {
				result += town.getChunkList().size();
			}
		}
		return result;
	}
	
}
