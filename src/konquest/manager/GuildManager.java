package konquest.manager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

import konquest.Konquest;
import konquest.model.KonGuild;
import konquest.model.KonKingdom;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;

public class GuildManager {

	private Konquest konquest;
	private boolean isEnabled;
	/*
	private int payIntervalSeconds;
	private double payPerChunk;
	private double payPerResident;
	private double payLimit;
	private double specialChangeCost;
	*/
	
	private HashSet<KonGuild> guilds;
	
	//TODO: Implement player and town guild cache, for fast lookup
	
	public GuildManager(Konquest konquest) {
		this.konquest = konquest;
		this.isEnabled = false;
		/*
		this.payIntervalSeconds = 1800;
		this.payPerChunk = 1.0;
		this.payPerResident = 0.5;
		this.payLimit = 100;
		this.specialChangeCost = 200;
		*/
		this.guilds = new HashSet<KonGuild>();
	}
	
	public void initialize() {
		isEnabled 			= konquest.getConfigManager().getConfig("core").getBoolean("core.guilds.enable",false);
		/*
		payIntervalSeconds 	= konquest.getConfigManager().getConfig("core").getInt("core.guilds.pay_interval_seconds",0);
		payPerChunk 		= konquest.getConfigManager().getConfig("core").getDouble("core.guilds.pay_per_chunk",0);
		payPerResident 		= konquest.getConfigManager().getConfig("core").getDouble("core.guilds.pay_per_resident",0);
		payLimit 			= konquest.getConfigManager().getConfig("core").getDouble("core.guilds.pay_limit",0);
		specialChangeCost 	= konquest.getConfigManager().getConfig("core").getDouble("core.guilds.special_change_cost",0);
		*/
		loadGuilds();
		ChatUtil.printDebug("Guild Manager is ready, enabled: "+isEnabled);
	}
	
	public boolean isEnabled() {
		return isEnabled;
	}
	
	/**
	 * Creates a new guild
	 * @param name - Guild name
	 * @param master - Player who is the guild master
	 * @return 0	- Success
	 * 	       1	- Bad name
	 * 		   2	- Master already is a guild member
	 */
	public int createGuild(String name, KonPlayer master) {
		if(!name.contains(" ") && !konquest.getKingdomManager().isKingdom(name) && !konquest.getKingdomManager().isTown(name)) {
			KonGuild currentGuild = getPlayerGuild(master.getOfflineBukkitPlayer());
			if(currentGuild == null) {
				KonGuild newGuild = new KonGuild(name, master.getBukkitPlayer().getUniqueId(), master.getKingdom());
				guilds.add(newGuild);
			} else {
				return 2;
			}
		} else {
			return 1;
		}
		return 0;
	}
	
	public boolean renameGuild(KonGuild guild, String name) {
		boolean result = false;
		if(!name.contains(" ") && !konquest.getKingdomManager().isKingdom(name) && !konquest.getKingdomManager().isTown(name)) {
			guild.setName(name);
			result = true;
		}
		return result;
	}
	
	public void removeGuild(KonGuild guild) {
		guilds.remove(guild);
	}
	
	public void toggleGuildOpen(KonGuild guild) {
		if(guild != null) {
			if(guild.isOpen()) {
				guild.setIsOpen(false);
			} else {
				guild.setIsOpen(true);
			}
		}
	}
	
	public void toggleGuildStatus(KonGuild guild, KonGuild otherGuild) {
		if(guild != null && otherGuild != null && !guild.equals(otherGuild)) {
			if(guild.getKingdom().equals(otherGuild.getKingdom())) {
				if(guild.isSanction(otherGuild)) {
					guild.removeSanction(otherGuild);
				} else {
					guild.addSanction(otherGuild);
				}
			} else {
				if(guild.isArmistice(otherGuild)) {
					guild.removeArmistice(otherGuild);
				} else {
					guild.addArmistice(otherGuild);
				}
			}
		}
	}
	
	/**
	 * Online Player requests to join the guild to the officers
	 * @param player - The online player requesting to join the guild
	 * @param guild - The target guild that the player requests to join
	 */
	public void joinGuildRequest(KonPlayer player, KonGuild guild) {
		if(guild != null) {
			UUID id = player.getBukkitPlayer().getUniqueId();
			if(player.getKingdom().equals(guild.getKingdom())) {
				if(!guild.isMember(id)) {
					if(guild.isJoinInviteValid(id)) {
						// There is already a valid invite, add the player to the guild
						guild.addMember(id, false);
						guild.removeJoinRequest(id);
						ChatUtil.sendNotice(player.getBukkitPlayer(), "Successfully joined guild");
					} else if(!guild.isJoinRequestValid(id)){
						// Request to join if not already requested
						guild.addJoinRequest(id, false);
					} else {
						ChatUtil.sendError(player.getBukkitPlayer(), "You have already requested to join");
					}
				}
			} else {
				ChatUtil.sendError(player.getBukkitPlayer(), "Cannot request to join enemy guild");
			}
		}
		
	}
	
	/**
	 * Officer approves or denies join request of (offline) player
	 * @param player - The offline player target of the response
	 * @param guild - The target guild that player wants to join
	 * @param resp - True to approve, false to reject request
	 */
	public boolean respondGuildRequest(OfflinePlayer player, KonGuild guild, boolean resp) {
		boolean result = false;
		if(guild != null) {
			UUID id = player.getUniqueId();
			if(guild.isJoinRequestValid(id)) {
				if(resp) {
					// Approved join request, add player as member
					guild.addMember(id, false);
					result = true;
					if(player.isOnline()) {
						ChatUtil.sendNotice((Player)player, "Guild join request accepted");
					}
				} else {
					// Denied join request
					if(player.isOnline()) {
						ChatUtil.sendNotice((Player)player, "Guild join request denied");
					}
				}
				guild.removeJoinRequest(id);
			}
		}
		return result;
	}
	
	/**
	 * Officer invites (offline) player to join (with /guild add command)
	 * @param player - The offline player that the officer invites to join their guild
	 * @param guild - The target guild for the join invite
	 */
	public boolean joinGuildInvite(KonOfflinePlayer player, KonGuild guild) {
		boolean result = false;
		if(guild != null) {
			UUID id = player.getOfflineBukkitPlayer().getUniqueId();
			if(player.getKingdom().equals(guild.getKingdom())) {
				if(!guild.isMember(id)) {
					if(guild.isJoinRequestValid(id)) {
						// There is already a valid request, add the player to the guild
						guild.addMember(id, false);
						guild.removeJoinRequest(id);
						if(player.getOfflineBukkitPlayer().isOnline()) {
							ChatUtil.sendNotice((Player)player.getOfflineBukkitPlayer(), "Guild join request accepted");
						}
						result = true;
					} else if(!guild.isJoinInviteValid(id)) {
						// Invite to join if not already invited
						guild.addJoinRequest(id, true);
						result = true;
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Player accepts or declines join invite
	 * @param player - The online player responding to the invite to join
	 * @param guild - The target guild that the player is responding to
	 * @param resp - True to accept, false to decline invite
	 */
	public boolean respondGuildInvite(KonPlayer player, KonGuild guild, boolean resp) {
		boolean result = false;
		if(guild != null) {
			UUID id = player.getBukkitPlayer().getUniqueId();
			if(guild.isJoinInviteValid(id)) {
				if(resp) {
					// Accept join invite, add as member
					guild.addMember(id, false);
					result = true;
					ChatUtil.sendNotice(player.getBukkitPlayer(), "Guild join invite accepted");
				} else {
					// Denied join request
					ChatUtil.sendNotice(player.getBukkitPlayer(), "Guild join invite declined");
				}
				guild.removeJoinRequest(id);
			}
		}
		return result;
	}
	
	public void leaveGuild(KonPlayer player, KonGuild guild) {
		if(guild != null) {
			UUID id = player.getBukkitPlayer().getUniqueId();
			boolean status = guild.removeMember(id);
			if(status) {
				ChatUtil.sendNotice(player.getBukkitPlayer(), "Successfully left the guild");
			} else {
				ChatUtil.sendNotice(player.getBukkitPlayer(), "Failed to leave - you are either the guild master or not a member");
			}
		}
	}
	
	public boolean kickGuildMember(OfflinePlayer player, KonGuild guild) {
		boolean result = false;
		UUID id = player.getUniqueId();
		result = guild.removeMember(id);
		return result;
	}
	
	public void promoteOfficer(OfflinePlayer player, KonGuild guild) {
		UUID id = player.getUniqueId();
		if(!guild.isOfficer(id)) {
			guild.setOfficer(id, true);
		}
	}
	
	public void demoteOfficer(OfflinePlayer player, KonGuild guild) {
		UUID id = player.getUniqueId();
		if(guild.isOfficer(id)) {
			guild.setOfficer(id, false);
		}
	}
	
	public void transferMaster(OfflinePlayer player, KonGuild guild) {
		UUID id = player.getUniqueId();
		if(guild.isMember(id)) {
			guild.setMaster(id);
		}
	}
	
	public void changeSpecialization(Villager.Profession profession, KonGuild guild) {
		guild.setSpecialization(profession);
	}

	/*
	 * ===================================
	 * Query Methods
	 * ===================================
	 */
	
	public List<KonGuild> getAllGuilds() {
		List<KonGuild> result = new ArrayList<KonGuild>();
		for(KonGuild guild : guilds) {
			result.add(guild);
		}
		return result;
	}
	
	public List<KonGuild> getKingdomGuilds(KonKingdom kingdom) {
		List<KonGuild> result = new ArrayList<KonGuild>();
		for(KonGuild guild : guilds) {
			if(kingdom.equals(guild.getKingdom())) {
				result.add(guild);
			}
		}
		return result;
	}
	
	public List<KonGuild> getInviteGuilds(KonPlayer player) {
		List<KonGuild> result = new ArrayList<KonGuild>();
		for(KonGuild guild : guilds) {
			if(player.getKingdom().equals(guild.getKingdom()) && guild.isJoinInviteValid(player.getBukkitPlayer().getUniqueId())) {
				result.add(guild);
			}
		}
		return result;
	}
	
	public KonGuild getTownGuild(KonTown town) {
		KonGuild result = null;
		for(KonGuild guild : guilds) {
			if(guild.isTownMember(town)) {
				result = guild;
				break;
			}
		}
		return result;
	}
	
	public KonGuild getPlayerGuild(OfflinePlayer player) {
		KonGuild result = null;
		for(KonGuild guild : guilds) {
			if(guild.isMember(player.getUniqueId())) {
				result = guild;
				break;
			}
		}
		return result;
	}
	
	public KonGuild getGuild(String name) {
		KonGuild result = null;
		for(KonGuild guild : guilds) {
			if(guild.getName().equals(name)) {
				result = guild;
				break;
			}
		}
		return result;
	}
	
	/*
	 * ===================================
	 * Saving Methods
	 * ===================================
	 */

	private void loadGuilds() {
		if(!isEnabled) {
			ChatUtil.printConsoleAlert("Disabled guilds");
			return;
		}
		FileConfiguration guildsConfig = konquest.getConfigManager().getConfig("guilds");
        if (guildsConfig.get("guilds") == null) {
        	ChatUtil.printDebug("There is no guilds section in guilds.yml");
            return;
        }
        ConfigurationSection guildsSection = guildsConfig.getConfigurationSection("guilds");
        Set<String> guildSet = guildsSection.getKeys(false);
        
        boolean isOpen;
        String specializationName;
        String kingdomName;
        String masterUUIDStr;
        KonKingdom kingdom;
        UUID playerUUID;
        
        // Load initial guild set
        for(String guildName : guildSet) {
        	if(guildsSection.contains(guildName)) {
        		ConfigurationSection guildInstanceSection = guildsSection.getConfigurationSection("guilds."+guildName);
        		// Parse property fields
        		isOpen = guildInstanceSection.getBoolean("open",false);
        		specializationName = guildInstanceSection.getString("specialization","NONE");
        		kingdomName = guildInstanceSection.getString("kingdom");
        		masterUUIDStr = guildInstanceSection.getString("master");
        		kingdom = konquest.getKingdomManager().getKingdom(kingdomName);
        		playerUUID = Konquest.idFromString(masterUUIDStr);
        		// Check valid fields
        		if(!kingdom.equals(konquest.getKingdomManager().getBarbarians()) && playerUUID != null) {
        			// Create guild
        			KonGuild newGuild = new KonGuild(guildName, playerUUID, kingdom);
    				// Set open flag
        			newGuild.setIsOpen(isOpen);
        			// Set profession
        			Villager.Profession profession = Villager.Profession.NONE;
        			try {
        				profession = Villager.Profession.valueOf(specializationName);
        			} catch(Exception e) {
        				ChatUtil.printConsoleAlert("Failed to parse profession "+specializationName+" for guild "+guildName);
        			}
        			newGuild.setSpecialization(profession);
        			// Add members
        			if(guildInstanceSection.contains("members")) {
		            	for(String memberUUIDStr : guildInstanceSection.getConfigurationSection("members").getKeys(false)) {
		            		boolean isOfficer = guildInstanceSection.getBoolean("members."+memberUUIDStr);
		            		playerUUID = Konquest.idFromString(memberUUIDStr);
		            		if(playerUUID != null) {
		            			newGuild.addMember(playerUUID,isOfficer);
		            		}
		            	}
	            	}
        			// Add membership requests
        			if(guildInstanceSection.contains("requests")) {
	            		for(String requestUUIDStr : guildInstanceSection.getConfigurationSection("requests").getKeys(false)) {
	            			boolean type = guildInstanceSection.getBoolean("requests."+requestUUIDStr);
	            			playerUUID = Konquest.idFromString(requestUUIDStr);
		            		if(playerUUID != null) {
		            			newGuild.addJoinRequest(playerUUID, type);
		            		}
	            		}
	            	}
        			guilds.add(newGuild);
        		}
        	}
        }
        // Load subsequent guild relationships
        for(String guildName : guildSet) {
        	if(guildsSection.contains(guildName)) {
        		ConfigurationSection guildInstanceSection = guildsSection.getConfigurationSection("guilds."+guildName);
        		KonGuild currentGuild = getGuild(guildName);
        		if(currentGuild != null) {
	        		// Add sanction and armistice lists
	    			if(guildInstanceSection.contains("sanction")) {
	    				List<String> sanctionList = guildInstanceSection.getStringList("sanction");
	    				for(String sanctionName : sanctionList) {
	    					KonGuild otherGuild = getGuild(sanctionName);
	    					if(otherGuild != null) {
	    						currentGuild.addSanction(otherGuild);
	    					} else {
	    						ChatUtil.printDebug("Failed to add sanction guild by name: "+sanctionName);
	    					}
	    				}
	    			}
	    			if(guildInstanceSection.contains("armistice")) {
	    				List<String> armisticeList = guildInstanceSection.getStringList("armistice");
	    				for(String armisticeName : armisticeList) {
	    					KonGuild otherGuild = getGuild(armisticeName);
	    					if(otherGuild != null) {
	    						currentGuild.addArmistice(otherGuild);
	    					} else {
	    						ChatUtil.printDebug("Failed to add armistice guild by name: "+armisticeName);
	    					}
	    				}
	    			}
        		} else {
        			ChatUtil.printDebug("Failed to find guild by name: "+guildName);
        		}
        	}
        }
        // Finished loading guilds
        ChatUtil.printDebug("Finished loading all guilds");
	}
	
	public void saveGuilds() {
		FileConfiguration guildsConfig = konquest.getConfigManager().getConfig("guilds");
		guildsConfig.set("guilds", null); // reset guilds config
		ConfigurationSection root = guildsConfig.createSection("guilds");
		for(KonGuild guild : guilds) {
			ConfigurationSection guildSection = root.createSection(guild.getName());
			guildSection.set("open", guild.isOpen());
			guildSection.set("specialization", guild.getSpecialization().toString());
			guildSection.set("kingdom", guild.getKingdom().getName());
			ConfigurationSection guildMemberSection = guildSection.createSection("members");
            for(OfflinePlayer member : guild.getPlayerMembers()) {
            	String uuid = member.getUniqueId().toString();
            	if(guild.isMaster(member.getUniqueId())) {
            		guildSection.set("master", uuid);
            	} else if(guild.isOfficer(member.getUniqueId())) {
            		guildMemberSection.set(uuid, true);
            	} else {
            		guildMemberSection.set(uuid, false);
            	}
            }
            ConfigurationSection guildRequestsSection = guildSection.createSection("requests");
            for(OfflinePlayer requestee : guild.getJoinRequests()) {
            	String uuid = requestee.getUniqueId().toString();
            	guildRequestsSection.set(uuid, false);
            }
            for(OfflinePlayer invitee : guild.getJoinInvites()) {
            	String uuid = invitee.getUniqueId().toString();
            	guildRequestsSection.set(uuid, true);
            }
            guildSection.set("sanction", guild.getSanctionNames());
            guildSection.set("armistice", guild.getArmisticeNames());
		}
	}
}
