package com.github.rumsfield.konquest.hook;

import com.github.rumsfield.konquest.model.KonOfflinePlayer;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.entities.*;
import github.scarsz.discordsrv.dependencies.jda.api.exceptions.InsufficientPermissionException;
import github.scarsz.discordsrv.dependencies.jda.api.managers.RoleManager;
import github.scarsz.discordsrv.dependencies.jda.api.requests.restaction.RoleAction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.listener.DiscordSRVListener;
import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * API References
 * <a href="https://docs.jda.wiki/net/dv8tion/jda/api/requests/RestAction.html">...</a>
 */
public class DiscordSrvHook implements PluginHook {

	public enum DiscordMessage {
		KINGDOM_CREATE			("discord.kingdom-create"),
		KINGDOM_DISBAND			("discord.kingdom-disband"),
		KINGDOM_CONQUER			("discord.kingdom-conquer"),
		KINGDOM_CAPITAL_SWAP	("discord.kingdom-capital-swap"),
		TOWN_SETTLE				("discord.town-settle"),
		TOWN_RAID_ALERT			("discord.town-raid-alert"),
		TOWN_DESTROY			("discord.town-destroy"),
		TOWN_CAPTURE			("discord.town-capture"),
		CAMP_DESTROY			("discord.camp-destroy"),
		RUIN_CAPTURE			("discord.ruin-capture"),
		DIPLOMACY_PEACE			("discord.diplomacy-peace"),
		DIPLOMACY_TRADE			("discord.diplomacy-trade"),
		DIPLOMACY_ALLIED		("discord.diplomacy-allied"),
		DIPLOMACY_WAR			("discord.diplomacy-war");

		private final String path;
		DiscordMessage(String path) {
			this.path = path;
		}

		public String getPath() {
			return path;
		}
	}

	private final int barbarianColor = 0xa3a10a;
	private final String roleSuffix = "(auto)";
	private final String noPermissionMessage = "Failed to modify kingdom role to Discord. " +
			"The bot lacks sufficient permissions to manage roles. " +
			"Change the bot's permissions, or disable the auto_roles option in Konquest core.yml.";

	private String roleEmoji;
	private boolean roleIsMentionable;
	private ArrayList<Permission> rolePermissions;

	private final Konquest konquest;
	private boolean isEnabled;
	private final DiscordSRVListener discordSrvListener;
	
	public DiscordSrvHook(Konquest konquest) {
		this.konquest = konquest;
		this.isEnabled = false;
		this.discordSrvListener = new DiscordSRVListener(konquest);
	}

	@Override
	public String getPluginName() {
		return "DiscordSRV";
	}

	@Override
	public int reload() {
		isEnabled = false;
		// Attempt to integrate DiscordSRV
		Plugin discordSrv = Bukkit.getPluginManager().getPlugin("DiscordSRV");
		if(discordSrv == null){
			return 1;
		}
		if(!discordSrv.isEnabled()){
			return 2;
		}
		if(!konquest.getCore().getBoolean(CorePath.INTEGRATION_DISCORDSRV.getPath(),false)) {
			return 3;
		}
		try {
			DiscordSRV.api.subscribe(discordSrvListener);
			isEnabled = true;
			reloadSettings();
			return 0;
		} catch (Exception e) {
			ChatUtil.printConsoleError("Failed to integrate DiscordSRV, see exception message:");
			e.printStackTrace();
			return -1;
		}
	}

	public void reloadSettings() {
		if (!isEnabled) return;
		roleEmoji = konquest.getConfigManager().getConfig("discord").getString("discord.roles.icon-emoji","");
		roleIsMentionable = konquest.getConfigManager().getConfig("discord").getBoolean("discord.roles.is-mentionable",true);
		List<String> rolePermissionNames = konquest.getConfigManager().getConfig("discord").getStringList("discord.roles.permissions");
		rolePermissions = new ArrayList<>();
		for (String permName : rolePermissionNames) {
			try {
				rolePermissions.add(Permission.valueOf(permName));
			} catch (IllegalArgumentException | NullPointerException me) {
				ChatUtil.printConsoleError("Invalid Discord role permission specified in discord.yml: "+permName);
			}
		}
	}
	
	@Override
	public void shutdown() {
		if(isEnabled) {
			try {
				DiscordSRV.api.unsubscribe(discordSrvListener);
			} catch (Exception ignored) {}
		}
	}

	@Override
	public boolean isEnabled() {
		return isEnabled;
	}
	
	public String getLinkMessage(Player player) {
		if (!isEnabled) return "";
		String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
		if (discordId == null) {
			return ChatColor.RED + MessagePath.DISCORD_SRV_NO_LINK.getMessage();
		}
		User user = DiscordUtil.getJda().getUserById(discordId);
		if (user == null) {
			return ChatColor.YELLOW + MessagePath.DISCORD_SRV_NO_USER.getMessage();
		}
		return ChatColor.GREEN + MessagePath.DISCORD_SRV_LINKED_USER.getMessage(user.getAsTag());
	}

	private Member getPlayerMember(OfflinePlayer player) {
		Guild mainGuild = DiscordSRV.getPlugin().getMainGuild();
		if (mainGuild == null) {
			// Invalid guild
			ChatUtil.printDebug("Discord main guild does not exist.");
			return null;
		}
		// Get the player member
		String discordId = "";
		try {
			discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
		} catch (IllegalStateException ignored) {}
		if (discordId == null || discordId.isEmpty()) {
			// No linked account found
			ChatUtil.printDebug("Discord user link for player "+player.getName()+" does not exist.");
			return null;
		}
		Member playerMember = null;
		try {
			playerMember = mainGuild.getMemberById(discordId);
		} catch (NumberFormatException ignored) {}
		if (playerMember == null) {
			// No member found
			ChatUtil.printDebug("Discord guild membership for player "+player.getName()+" does not exist.");
			return null;
		}
		ChatUtil.printDebug("Got Discord member "+playerMember.getEffectiveName()+" from guild "+mainGuild.getName()+" for player "+player.getName());
		return playerMember;
	}

	/*
	 * Role update logic...
	 * - When kingdoms are changed (created, disbanded, conquered, renamed), try to update roles and all affected players.
	 * - Try to create and delete roles during kingdom changes, if enabled.
	 * - When players change their kingdom membership (join/create a kingdom, leave/disband a kingdom, exile to barbarian),
	 * 		remove their old role and add a new one for their new kingdom.
	 */

	private String formatRoleName(String baseName) {
        return baseName + " " + roleSuffix;
	}

	/**
	 * Ensure that all kingdoms have roles on Discord, when enabled
	 */
	public void refreshRoles() {
		if (!isEnabled) return;
		boolean isAutoRoleEnabled = konquest.getCore().getBoolean(CorePath.INTEGRATION_DISCORDSRV_OPTIONS_AUTO_ROLES.getPath());
		if (!isAutoRoleEnabled) return;
		Guild mainGuild = DiscordSRV.getPlugin().getMainGuild();
		if (mainGuild == null) {
			ChatUtil.printDebug("Discord main guild does not exist.");
			return;
		}
		int numDiscordRoles = 0;
		int numMemberRoles = 0;
		// Get all kingdom names
        HashMap<String,Integer> allActiveRoles = new HashMap<>(); // names and colors
		for (KonKingdom kingdom : konquest.getKingdomManager().getKingdoms()) {
			allActiveRoles.put(formatRoleName(kingdom.getName()),kingdom.getWebColorFormal());
		}
		allActiveRoles.put(formatRoleName(konquest.getKingdomManager().getBarbarians().getName()),barbarianColor);
		// Check all current Discord roles for actives and pruning
		for (Role currentRole : mainGuild.getRoles()) {
			String roleName = currentRole.getName();
			// Check for role format
			if (!roleName.contains(roleSuffix)) {
				// This role does not have the special Konquest role format, skip it
				continue;
			}
			// Check for kingdom match
			if (allActiveRoles.containsKey(roleName)) {
				// This Discord role matches an active kingdom
				int roleColor = allActiveRoles.get(roleName);
				// Update it
				try {
					currentRole.getManager().
							setColor(roleColor).
							setIcon(roleEmoji).
							setMentionable(roleIsMentionable).
							setPermissions(rolePermissions).
							queue();
					ChatUtil.printDebug("Discord role refresh updated existing role: "+roleName);
					numDiscordRoles++;
				} catch (Exception me) {
					ChatUtil.sendAdminBroadcast(noPermissionMessage);
					ChatUtil.printConsoleError(noPermissionMessage);
					return;
				}
				// Remove this role name from the active set
				allActiveRoles.remove(roleName);
			} else {
				// This Discord role does not match any kingdom, delete it
				try {
					currentRole.delete().reason("Konquest auto role removal").queue();
					ChatUtil.printDebug("Discord role refresh removed stale existing role: "+roleName);
				} catch (Exception me) {
					ChatUtil.sendAdminBroadcast(noPermissionMessage);
					ChatUtil.printConsoleError(noPermissionMessage);
					return;
				}
			}
		}
		// Add all kingdom roles that don't exist
		// Any remaining active roles must be created
		for (String roleName : allActiveRoles.keySet()) {
			int roleColor = allActiveRoles.get(roleName);
			try {
				RoleAction addRole = mainGuild.createRole().
						setName(roleName).
						setColor(roleColor).
						setIcon(roleEmoji).
						setMentionable(roleIsMentionable).
						setPermissions(rolePermissions);
				addRole.reason("Konquest auto role creation");
				addRole.queue();
				ChatUtil.printDebug("Discord role refresh created new role: "+roleName);
				numDiscordRoles++;
			} catch (Exception me) {
				ChatUtil.sendAdminBroadcast(noPermissionMessage);
				ChatUtil.printConsoleError(noPermissionMessage);
				return;
			}
		}
		// Update all member roles
		for (KonOfflinePlayer player : konquest.getPlayerManager().getAllKonquestOfflinePlayers()) {
			Member playerMember = getPlayerMember(player.getOfflineBukkitPlayer());
			if (playerMember == null) {
				// Player is not linked to Discord, skip
				continue;
			}
			// Ensure player is assigned their kingdom role
			String kingdomRoleName = formatRoleName(player.getKingdom().getName());
			boolean hasKingdomRole = false;
			HashSet<Role> rolesToRemove = new HashSet<>();
			for (Role memberRole : playerMember.getRoles()) {
				// Check if member has their kingdom role
				if (memberRole.getName().equalsIgnoreCase(kingdomRoleName)) {
					// This role is for the active kingdom, keep it
					hasKingdomRole = true;
				} else if (memberRole.getName().contains(roleSuffix)) {
					// This role is from another kingdom, remove it
					rolesToRemove.add(memberRole);
				}
			}
			// Add kingdom role
			if (!hasKingdomRole) {
				List<Role> kingdomRoles = mainGuild.getRolesByName(kingdomRoleName,true);
				if (!kingdomRoles.isEmpty()) {
					DiscordUtil.addRoleToMember(playerMember, kingdomRoles.get(0));
					ChatUtil.printDebug("Discord role refresh assigned role to member: "+kingdomRoleName+", "+playerMember.getEffectiveName());
				} else {
					ChatUtil.printDebug("Discord role refresh failed to assign unknown role: "+kingdomRoleName);
				}
			}
			// Remove any extra kingdom roles
			DiscordUtil.removeRolesFromMember(playerMember, rolesToRemove);
			if (!rolesToRemove.isEmpty()) {
				ChatUtil.printDebug("Discord role refresh removed "+rolesToRemove.size()+" stale role(s) from member: "+playerMember.getEffectiveName());
			}
			numMemberRoles++;
		}
		ChatUtil.printConsoleAlert("Discord Auto-Role refresh finished with "+numDiscordRoles+" total kingdom roles, assigned to "+numMemberRoles+" members.");
	}

	/**
	 * Adds a role to the main Discord guild as the name of a kingdom,
	 * if the role does not already exist.
	 * @param kingdomName The kingdom name to use for the new role
	 * @param color The color of the role as a rgb integer
	 */
	public void addKingdomRole(String kingdomName, int color) {
		if (!isEnabled) return;
		boolean isAutoRoleEnabled = konquest.getCore().getBoolean(CorePath.INTEGRATION_DISCORDSRV_OPTIONS_AUTO_ROLES.getPath());
		if (!isAutoRoleEnabled) return;
		Guild mainGuild = DiscordSRV.getPlugin().getMainGuild();
		if (mainGuild == null) {
			ChatUtil.printDebug("Discord main guild does not exist.");
			return;
		}
		if (!mainGuild.getRolesByName(formatRoleName(kingdomName),true).isEmpty()) {
			// This role already exists
			ChatUtil.printDebug("Discord kingdom role already exists, could not add "+kingdomName);
			return;
		}
		// Create the role
		try {
			RoleAction addRole = mainGuild.createRole().
					setName(formatRoleName(kingdomName)).
					setColor(color).
					setIcon(roleEmoji).
					setMentionable(roleIsMentionable).
					setPermissions(rolePermissions);
			addRole.reason("Konquest auto role creation");
			addRole.queue();
		} catch (Exception me) {
			ChatUtil.sendAdminBroadcast(noPermissionMessage);
			ChatUtil.printConsoleError(noPermissionMessage);
		}
	}

	/**
	 * Removes a role from the main Discord guild, if it exists.
	 * @param kingdomName The name of the role to remove
	 */
	public void removeKingdomRole(String kingdomName) {
		if (!isEnabled) return;
		boolean isAutoRoleEnabled = konquest.getCore().getBoolean(CorePath.INTEGRATION_DISCORDSRV_OPTIONS_AUTO_ROLES.getPath());
		if (!isAutoRoleEnabled) return;
		Guild mainGuild = DiscordSRV.getPlugin().getMainGuild();
		if (mainGuild == null) {
			ChatUtil.printDebug("Discord main guild does not exist.");
			return;
		}
		for (Role kingdomRole : mainGuild.getRolesByName(formatRoleName(kingdomName),true)) {
			// Remove the role
			try {
				kingdomRole.delete().reason("Konquest auto role removal").queue();
			} catch (Exception me) {
				ChatUtil.sendAdminBroadcast(noPermissionMessage);
				ChatUtil.printConsoleError(noPermissionMessage);
			}
		}
	}

	/**
	 * Modifies an existing kingdom role by name, color, or both.
	 * @param kingdomName The current role name to modify
	 * @param changeToName The new name for the role
	 * @param changeToColor The new color for the role
	 */
	public void changeKingdomRole(String kingdomName, String changeToName, int changeToColor) {
		if (!isEnabled) return;
		boolean isAutoRoleEnabled = konquest.getCore().getBoolean(CorePath.INTEGRATION_DISCORDSRV_OPTIONS_AUTO_ROLES.getPath());
		if (!isAutoRoleEnabled) return;
		Guild mainGuild = DiscordSRV.getPlugin().getMainGuild();
		if (mainGuild == null) {
			ChatUtil.printDebug("Discord main guild does not exist.");
			return;
		}
		for (Role kingdomRole : mainGuild.getRolesByName(formatRoleName(kingdomName),true)) {
			try {
				kingdomRole.getManager().
						setName(formatRoleName(changeToName)).
						setColor(changeToColor).
						setIcon(roleEmoji).
						setMentionable(roleIsMentionable).
						setPermissions(rolePermissions).
						queue();
			} catch (Exception me) {
				ChatUtil.sendAdminBroadcast(noPermissionMessage);
				ChatUtil.printConsoleError(noPermissionMessage);
			}
		}
	}

	/**
	 * Change a player's roles on Discord.
	 * Remove and add roles with this method.
	 * @param player The player who is a member of the Discord guild.
	 * @param removeRoleName The name of a role to remove, skip when empty string
	 * @param addRoleName The name of a role to add, skip when empty string
	 */
	public void modifyPlayerRoles(OfflinePlayer player, String removeRoleName, String addRoleName) {
		Member playerMember = getPlayerMember(player);
		if (playerMember == null) {
			ChatUtil.printDebug("Discord member does not exist for player "+player.getName());
			return;
		}
		Guild mainGuild = DiscordSRV.getPlugin().getMainGuild();
		if (mainGuild == null) {
			ChatUtil.printDebug("Discord main guild does not exist.");
			return;
		}
		// Try to remove roles
		if (!removeRoleName.isEmpty()) {
			HashSet<Role> rolesToRemove = new HashSet<>(mainGuild.getRolesByName(formatRoleName(removeRoleName), true));
			if (!rolesToRemove.isEmpty()) {
				DiscordUtil.removeRolesFromMember(playerMember, rolesToRemove);
			} else {
				ChatUtil.printDebug("Failed to remove role \""+addRoleName+"\" from member \""+playerMember.getEffectiveName()+"\" linked to player \""+player.getName()+"\", role does not exist on Discord.");
			}
		}
		// Try to add roles
		if (!addRoleName.isEmpty()) {
			HashSet<Role> rolesToAdd = new HashSet<>(mainGuild.getRolesByName(formatRoleName(addRoleName), true));
			if (!rolesToAdd.isEmpty()) {
				DiscordUtil.addRolesToMember(playerMember, rolesToAdd);
			} else {
				ChatUtil.printDebug("Failed to add role \""+addRoleName+"\" to member \""+playerMember.getEffectiveName()+"\" linked to player \""+player.getName()+"\", role does not exist on Discord.");
			}
		}
    }

	/*
	 * Messaging Methods
	 */

	public boolean sendGameToDiscordMessage(String channel, String message) {
		if (!isEnabled) return false;
		TextChannel textChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channel);

		// null if the channel isn't specified in the config.yml
		if (textChannel == null) {
			ChatUtil.printDebug("Channel called \""+channel+"\" could not be found in the DiscordSRV configuration");
			return false;
		}

		textChannel.sendMessage(message).queue();
		return true;
	}
	
	public void sendGameChatToDiscord(Player player, String message, String channel, boolean isCancelled) {
		if (!isEnabled) return;
		DiscordSRV.getPlugin().processChatMessage(player, message, channel, isCancelled);
	}
	
	// Send message from discord to kingdom chat
	public void sendDiscordToGameChatKingdomChannel(User guildUser, Message guildMessage, String kingdomChannel) {
		if (!isEnabled) return;

		if(kingdomChannel.equalsIgnoreCase("global") || !konquest.getKingdomManager().isKingdom(kingdomChannel)) return;
		KonKingdom kingdom = konquest.getKingdomManager().getKingdom(kingdomChannel);
		for(KonPlayer viewerPlayer : konquest.getPlayerManager().getPlayersOnline()) {
			String messageFormat = "";
			String chatFormat =  ChatColor.WHITE+"["+ChatColor.AQUA+MessagePath.DISCORD_SRV_DISCORD.getMessage()+ChatColor.WHITE+"] ";
			String chatMessage = guildMessage.getContentDisplay();
			boolean sendMessage = false;
			if(viewerPlayer.getKingdom().equals(kingdom)) {
				chatFormat = chatFormat + Konquest.friendColor1+kingdom.getName()+" "+guildUser.getName();
				messageFormat = ""+ChatColor.RESET+Konquest.friendColor2+ChatColor.ITALIC;
				sendMessage = true;
			} else if(viewerPlayer.isAdminBypassActive()) {
				chatFormat = chatFormat + ChatColor.GOLD+kingdom.getName()+" "+guildUser.getName();
				messageFormat = ""+ChatColor.RESET+ChatColor.GOLD+ChatColor.ITALIC;
				sendMessage = true;
			}
			if(sendMessage) {
				viewerPlayer.getBukkitPlayer().sendMessage(chatFormat + " » " + messageFormat + chatMessage);
			}
		}
	}
	
	// Sends the linked player a direct message
	public void alertDiscordMember(OfflinePlayer player, String message) {
		boolean doAlert = konquest.getCore().getBoolean(CorePath.INTEGRATION_DISCORDSRV_OPTIONS_RAID_ALERT_DIRECT.getPath(),false);
		if(isEnabled && doAlert) {
			String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
			if (discordId != null) {
				User user = DiscordUtil.getJda().getUserById(discordId);
				// will be null if the bot isn't in a Discord server with the user (e.g. they left the main Discord server)
		        if (user != null) {
		            // opens/retrieves the private channel for the user & sends a message to it (if retrieving the private channel was successful)
		            user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage(message).queue());
		        }
			}
		}
	}
	
	// Sends the channel a message to @everyone
	public void alertDiscordChannel(String channel, String message) {
		boolean doAlert = konquest.getCore().getBoolean(CorePath.INTEGRATION_DISCORDSRV_OPTIONS_RAID_ALERT_CHANNEL.getPath(),false);
		if(!isEnabled || !doAlert)return;
		TextChannel textChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channel);

		// null if the channel isn't specified in the config.yml
		if(textChannel == null){
			ChatUtil.printDebug("Channel called \""+channel+"\" could not be found in the DiscordSRV configuration");
			return;
		}
		textChannel.sendMessage("@everyone " + message).queue();
	}

}
