package com.github.rumsfield.konquest.hook;

import com.github.rumsfield.konquest.utility.CorePath;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.util.DiscordUtil;
import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.listener.DiscordSRVListener;
import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;

public class DiscordSrvHook implements PluginHook {

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
			return 0;
		} catch (Exception e) {
			ChatUtil.printConsoleError("Failed to integrate DiscordSRV, see exception message:");
			e.printStackTrace();
			return -1;
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
			return ChatColor.RED + "You are not linked to Discord. Type /discord link";
		}

		User user = DiscordUtil.getJda().getUserById(discordId);
		if (user == null) {
			return ChatColor.YELLOW + "Couldn't find the Discord user you're linked to.";
		}

		return ChatColor.GREEN + "You're linked to " + user.getAsTag() + " in Discord.";

	}
	
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
			String chatFormat =  ChatColor.WHITE+"["+ChatColor.AQUA+"Discord"+ChatColor.WHITE+"] ";
			String chatMessage = guildMessage.getContentDisplay();
			boolean sendMessage = false;
			if(viewerPlayer.getKingdom().equals(kingdom)) {
				chatFormat = chatFormat + Konquest.friendColor1+kingdom.getName()+" "+guildUser.getName();
				messageFormat = ""+ChatColor.GREEN+ChatColor.ITALIC;
				sendMessage = true;
			} else if(viewerPlayer.isAdminBypassActive()) {
				chatFormat = chatFormat + ChatColor.GOLD+kingdom.getName()+" "+guildUser.getName();
				messageFormat = ""+ChatColor.GOLD+ChatColor.ITALIC;
				sendMessage = true;
			}

			if(sendMessage) {
				viewerPlayer.getBukkitPlayer().sendMessage(chatFormat + Konquest.chatDivider + ChatColor.RESET + " " + messageFormat + chatMessage);
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
