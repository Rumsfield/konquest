package com.github.rumsfield.konquest.hook;

import com.github.rumsfield.konquest.model.KonOfflinePlayer;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.entities.*;
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
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * API References
 * <a href="https://docs.jda.wiki/net/dv8tion/jda/api/requests/RestAction.html">...</a>
 */
public class DiscordSrvHook implements PluginHook {

	private final String noPermissionMessage = "Failed to modify kingdom roles in Discord. " +
			"The Discord bot lacks sufficient permissions to manage roles. " +
			"Change the bot's permissions, or disable the auto_roles option in Konquest core.yml.";

	private final int barbarianColor = 0xa3a10a;
	private int roleDefaultColor;
	private String roleSuffix;
	private boolean roleIsMentionable;
	private ArrayList<Permission> rolePermissions;

	private final Konquest konquest;
	private boolean isEnabled;
	private final DiscordSRVListener discordSrvListener;
	private boolean isKonquestReady;
	private boolean isDiscordReady;
	
	public DiscordSrvHook(Konquest konquest) {
		this.konquest = konquest;
		this.isEnabled = false;
		this.discordSrvListener = new DiscordSRVListener(konquest);
		this.isKonquestReady = false;
		this.isDiscordReady = false;

		this.roleDefaultColor = barbarianColor;
		this.roleSuffix = "(auto)";
		this.roleIsMentionable = true;
		this.rolePermissions = new ArrayList<>();
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
		String defaultColor = konquest.getCore().getString(CorePath.INTEGRATION_DISCORDSRV_OPTIONS_ROLE_DEFAULT_COLOR.getPath(),"");
		if (defaultColor.isEmpty()) {
			roleDefaultColor = barbarianColor;
		} else {
			roleDefaultColor = ChatUtil.lookupColorRGB(defaultColor);
			if (roleDefaultColor == -1) {
				roleDefaultColor = barbarianColor;
			}
		}
		roleSuffix = konquest.getCore().getString(CorePath.INTEGRATION_DISCORDSRV_OPTIONS_ROLE_SUFFIX.getPath(),"(auto)");
		roleIsMentionable = konquest.getCore().getBoolean(CorePath.INTEGRATION_DISCORDSRV_OPTIONS_ROLE_IS_MENTIONABLE.getPath(),true);
		List<String> rolePermissionNames = konquest.getCore().getStringList(CorePath.INTEGRATION_DISCORDSRV_OPTIONS_ROLE_PERMISSIONS.getPath());
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

	public void setKonquestReady() {
		isKonquestReady = true;
	}

	public void setDiscordReady() {
		isDiscordReady = true;
	}

	private boolean isReady() {
		return isEnabled && isKonquestReady && isDiscordReady;
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
        return roleSuffix.isEmpty() ? baseName : baseName + " " + roleSuffix;
	}

	private boolean isRoleAuto(String roleName) {
		return !roleSuffix.isEmpty() && roleName.contains(roleSuffix);
	}

	/**
	 * Sends a role update command to Discord
	 * @param role The role to modify
	 * @param name The new role name, ignored when empty string
	 * @param color The new role color, ignore when 0
	 * @return True when the update was successful, else false
	 */
	private boolean updateRole(Role role, String name, int color) {
		try {
			RoleManager manager = role.getManager();
			// Update name
			if (!name.isEmpty()) {
				manager = manager.setName(name);
			}
			// Update color
			if (color != 0) {
				manager = manager.setColor(color);
			}
			// Update mentionable
			manager = manager.setMentionable(roleIsMentionable);
			// Update permissions
			manager = manager.setPermissions(rolePermissions);
			// Send update
			manager.reason("Konquest auto role update");
			manager.queue();
			ChatUtil.printDebug("Discord updated existing role: "+role.getName());
		} catch (Exception me) {
			ChatUtil.sendAdminBroadcast(noPermissionMessage);
			ChatUtil.printConsoleError(noPermissionMessage);
			return false;
		}
		return true;
	}

	private boolean createRole(String name, int color, Consumer<Role> callback) {
		Guild mainGuild = DiscordSRV.getPlugin().getMainGuild();
		if (mainGuild == null) {
			ChatUtil.printConsoleError("Discord main guild does not exist.");
			return false;
		}
		try {
			RoleAction addRole = mainGuild.createRole();
			// Set name
			if (!name.isEmpty()) {
				addRole = addRole.setName(name);
			}
			// Set color
			if (color != 0) {
				addRole = addRole.setColor(color);
			}
			// Set mentionable
			addRole = addRole.setMentionable(roleIsMentionable);
			// Set permissions
			addRole = addRole.setPermissions(rolePermissions);
			// Send creation
			addRole.reason("Konquest auto role creation");
			if (callback == null) {
				addRole.queue();
			} else {
				addRole.queue(callback);
			}
			ChatUtil.printDebug("Discord created new role: "+name);
		} catch (Exception me) {
			ChatUtil.sendAdminBroadcast(noPermissionMessage);
			ChatUtil.printConsoleError(noPermissionMessage);
			return false;
		}
		return true;
	}

	private boolean deleteRole(Role role) {
		try {
			role.delete().reason("Konquest auto role removal").queue();
			ChatUtil.printDebug("Discord removed existing role: "+role.getName());
		} catch (Exception me) {
			ChatUtil.sendAdminBroadcast(noPermissionMessage);
			ChatUtil.printConsoleError(noPermissionMessage);
			return false;
		}
		return true;
	}

	public void refreshRoles() {
		if (!isReady()) return;
		boolean isAutoRoleEnabled = konquest.getCore().getBoolean(CorePath.INTEGRATION_DISCORDSRV_OPTIONS_AUTO_ROLES.getPath());
		if (!isAutoRoleEnabled) return;
		ChatUtil.printConsoleAlert("Starting Discord Auto-Role Refresh");
		Guild mainGuild = DiscordSRV.getPlugin().getMainGuild();
		if (mainGuild == null) {
			ChatUtil.printConsoleError("Discord main guild does not exist.");
			return;
		}
		int numRolesCreated = 0;
		int numRolesUpdated = 0;
		int numRolesDeleted = 0;

		// Get all kingdoms
		HashSet<String> activeKingdomRoleNames = new HashSet<>();
		ArrayList<KonKingdom> allKingdoms = new ArrayList<>();
		allKingdoms.add(konquest.getKingdomManager().getBarbarians());
		allKingdoms.addAll(konquest.getKingdomManager().getKingdoms());
		for (KonKingdom kingdom : allKingdoms) {
			activeKingdomRoleNames.add(formatRoleName(kingdom.getName()));
		}

		// Go through all active kingdom roles
		for (KonKingdom kingdom : allKingdoms) {
			String kingdomRoleName = formatRoleName(kingdom.getName()); // may or may not have suffix tag
			int kingdomColor = kingdom.getWebColorFormal();
			if (kingdom.equals(konquest.getKingdomManager().getBarbarians())) {
				kingdomColor = roleDefaultColor;
			}

			// Get kingdom members
			HashSet<Member> kingdomMembers = new HashSet<>();
			for (OfflinePlayer player : konquest.getPlayerManager().getAllBukkitPlayersInKingdom(kingdom)) {
				Member playerMember = getPlayerMember(player);
				if (playerMember == null) {
					// Player is not linked to Discord, skip
					continue;
				}
				kingdomMembers.add(playerMember);
			}

			// Get other Discord roles that are not this kingdom role, which have a suffix tag
			HashSet<Role> rolesToRemove = new HashSet<>();
			for (Role discordRole : mainGuild.getRoles()) {
				String discordRoleName = discordRole.getName();
				if (!discordRoleName.equalsIgnoreCase(kingdomRoleName) && (isRoleAuto(discordRoleName) || activeKingdomRoleNames.contains(discordRoleName))) {
					// Discord role is another active kingdom role, or has a suffix tag
					rolesToRemove.add(discordRole);
				}
			}

			// Check if role exists on discord
			if (mainGuild.getRolesByName(kingdomRoleName,true).isEmpty()) {
				// Role does not exist, create it
				Consumer<Role> callback = (role) -> {
					// Assign all kingdom members the role
					for (Member member : kingdomMembers) {
						DiscordUtil.removeRolesFromMember(member, rolesToRemove);
						DiscordUtil.addRoleToMember(member, role);
						ChatUtil.printDebug("Discord assigned role "+role.getName()+" to member "+member.getEffectiveName());
					}
				};
				// Send creation
				if (createRole(kingdomRoleName, kingdomColor, callback)) {
					numRolesCreated++;
				} else {
					ChatUtil.printConsoleError("Discord Auto-Role refresh failed.");
					return;
				}
			} else {
				// Role exists, update it
				Role kingdomRole = mainGuild.getRolesByName(kingdomRoleName,true).getFirst();
				// Assign all kingdom members the role
				for (Member member : kingdomMembers) {
					DiscordUtil.removeRolesFromMember(member, rolesToRemove);
					DiscordUtil.addRoleToMember(member, kingdomRole);
					ChatUtil.printDebug("Discord assigned role "+kingdomRole.getName()+" to member "+member.getEffectiveName());
				}
				// Send update
				if (updateRole(kingdomRole,"", kingdomColor)) {
					numRolesUpdated++;
				} else {
					ChatUtil.printConsoleError("Discord Auto-Role refresh failed.");
					return;
				}
			}
		}

		// Prune any stale roles from Discord that have a suffix tag
		for (Role discordRole : mainGuild.getRoles()) {
			String roleName = discordRole.getName();
			// Check for role format and active kingdom name
			if (!isRoleAuto(roleName) || activeKingdomRoleNames.contains(roleName)) {
				// This role does not have the special Konquest role format, skip it
				continue;
			}
			if (discordRole.isPublicRole() || discordRole.isManaged()) {
				// This role is public or managed
				continue;
			}
			// This role has a Konquest auto role suffix tag, but is not included in the active kingdom roles.
			// Delete it.
			if (deleteRole(discordRole)) {
				numRolesDeleted++;
			} else {
				ChatUtil.printConsoleError("Discord Auto-Role refresh failed.");
				return;
			}
		}

		ChatUtil.printConsoleAlert("Discord Auto-Role refresh finished with "+numRolesCreated+" kingdom roles created, "+numRolesUpdated+" updated, "+numRolesDeleted+" deleted.");
	}

	/**
	 * Adds a role to the main Discord guild as the name of a kingdom,
	 * if the role does not already exist.
	 * @param kingdomName The kingdom name to use for the new role
	 * @param color The color of the role as a rgb integer
	 */
	public void addKingdomRole(String kingdomName, int color) {
		if (!isReady()) return;
		boolean isAutoRoleEnabled = konquest.getCore().getBoolean(CorePath.INTEGRATION_DISCORDSRV_OPTIONS_AUTO_ROLES.getPath());
		if (!isAutoRoleEnabled) return;
		Guild mainGuild = DiscordSRV.getPlugin().getMainGuild();
		if (mainGuild == null) {
			ChatUtil.printDebug("Discord main guild does not exist.");
			return;
		}
		String roleName = formatRoleName(kingdomName);
		if (!mainGuild.getRolesByName(roleName,true).isEmpty()) {
			// This role already exists
			ChatUtil.printDebug("Discord kingdom role already exists, could not add "+kingdomName);
			return;
		}
		// Create the role
		createRole(roleName, color, (role) -> ChatUtil.printDebug("Discord finished creating role "+role.getName()));
	}

	/**
	 * Removes a role from the main Discord guild, if it exists.
	 * @param kingdomName The name of the role to remove
	 */
	public void removeKingdomRole(String kingdomName) {
		if (!isReady()) return;
		boolean isAutoRoleEnabled = konquest.getCore().getBoolean(CorePath.INTEGRATION_DISCORDSRV_OPTIONS_AUTO_ROLES.getPath());
		if (!isAutoRoleEnabled) return;
		Guild mainGuild = DiscordSRV.getPlugin().getMainGuild();
		if (mainGuild == null) {
			ChatUtil.printDebug("Discord main guild does not exist.");
			return;
		}
		for (Role kingdomRole : mainGuild.getRolesByName(formatRoleName(kingdomName),true)) {
			// Remove the role
			deleteRole(kingdomRole);
		}
	}

	/**
	 * Modifies an existing kingdom role by name, color, or both.
	 * @param kingdomName The current role name to modify
	 * @param changeToName The new name for the role
	 * @param changeToColor The new color for the role
	 */
	public void changeKingdomRole(String kingdomName, String changeToName, int changeToColor) {
		if (!isReady()) return;
		boolean isAutoRoleEnabled = konquest.getCore().getBoolean(CorePath.INTEGRATION_DISCORDSRV_OPTIONS_AUTO_ROLES.getPath());
		if (!isAutoRoleEnabled) return;
		Guild mainGuild = DiscordSRV.getPlugin().getMainGuild();
		if (mainGuild == null) {
			ChatUtil.printDebug("Discord main guild does not exist.");
			return;
		}
		String updateName = "";
		if (changeToName != null && !changeToName.isEmpty() && !changeToName.equals(kingdomName)) {
			updateName = formatRoleName(changeToName);
		}
		int updateColor = 0;
		if (changeToColor != 0 && changeToColor != -1) {
			updateColor = changeToColor;
		}
		for (Role kingdomRole : mainGuild.getRolesByName(formatRoleName(kingdomName),true)) {
			updateRole(kingdomRole, updateName, updateColor);
		}
	}

	/**
	 * Update a player's roles on Discord to match their active kingdom
	 * @param player The player to update roles
	 */
	public void refreshPlayerRoles(KonOfflinePlayer player) {
		if (!isReady()) return;
		boolean isAutoRoleEnabled = konquest.getCore().getBoolean(CorePath.INTEGRATION_DISCORDSRV_OPTIONS_AUTO_ROLES.getPath());
		if (!isAutoRoleEnabled) return;
		Member playerMember = getPlayerMember(player.getOfflineBukkitPlayer());
		if (playerMember == null) {
			return;
		}
		Guild mainGuild = DiscordSRV.getPlugin().getMainGuild();
		if (mainGuild == null) {
			ChatUtil.printDebug("Discord main guild does not exist.");
			return;
		}
		String kingdomRoleName = formatRoleName(player.getKingdom().getName());
		// Get all kingdoms
		HashSet<String> activeKingdomRoleNames = new HashSet<>();
		ArrayList<KonKingdom> allKingdoms = new ArrayList<>();
		allKingdoms.add(konquest.getKingdomManager().getBarbarians());
		allKingdoms.addAll(konquest.getKingdomManager().getKingdoms());
		for (KonKingdom kingdom : allKingdoms) {
			activeKingdomRoleNames.add(formatRoleName(kingdom.getName()));
		}
		// Add the active kingdom role
		// Remove all other kingdom roles, and any other roles with suffix tags
		HashSet<Role> rolesToRemove = new HashSet<>();
		HashSet<Role> rolesToAdd = new HashSet<>();
		// Delay task 5 seconds
		Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), () -> {
			for (Role discordRole : mainGuild.getRoles()) {
				String discordRoleName = discordRole.getName();
				if (discordRoleName.equalsIgnoreCase(kingdomRoleName)) {
					// This discord role is the active kingdom role
					rolesToAdd.add(discordRole);
				} else {
					// This role is not the active role
					if (isRoleAuto(discordRoleName) || activeKingdomRoleNames.contains(discordRoleName)) {
						// Discord role is another active kingdom role, or has a suffix tag
						rolesToRemove.add(discordRole);
					}
				}
			}
			if (rolesToAdd.isEmpty()) {
				ChatUtil.printDebug("Discord is missing active kingdom role "+kingdomRoleName);
			}
			DiscordUtil.modifyRolesOfMember(playerMember,rolesToAdd,rolesToRemove);
			ChatUtil.printDebug("Discord refreshed role \""+kingdomRoleName+"\" for member \""+playerMember.getEffectiveName()+"\" linked to player \""+player.getOfflineBukkitPlayer().getName()+"\".");
		}, 20*5);
	}

	/*
	 * Messaging Methods
	 */

	//TODO remove this? DiscordSRV alerts can replace this function
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

	// For sending chat messages to the DiscordSRV processor
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
