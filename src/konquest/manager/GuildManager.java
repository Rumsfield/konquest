package konquest.manager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
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
		ChatUtil.printDebug("Guild Manager is ready, enabled: "+isEnabled);
	}
	
	public boolean isEnabled() {
		return isEnabled;
	}
	
	public int createGuild(String name, KonPlayer master) {
		KonGuild newGuild = new KonGuild(name, master.getBukkitPlayer().getUniqueId(), master.getKingdom());
		guilds.add(newGuild);
		
		return 0;
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
		if(guild != null && otherGuild != null) {
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
	public void joinGuildInvite(KonOfflinePlayer player, KonGuild guild) {
		if(guild != null) {
			UUID id = player.getOfflineBukkitPlayer().getUniqueId();
			if(player.getKingdom().equals(guild.getKingdom())) {
				if(guild.isJoinRequestValid(id)) {
					// There is already a valid request, add the player to the guild
					guild.addMember(id, false);
					guild.removeJoinRequest(id);
					if(player.getOfflineBukkitPlayer().isOnline()) {
						ChatUtil.sendNotice((Player)player.getOfflineBukkitPlayer(), "Guild join request accepted");
					}
				} else if(!guild.isJoinInviteValid(id)) {
					// Invite to join if not already invited
					guild.addJoinRequest(id, true);
				}
			}
		}
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
	
	
	
}
