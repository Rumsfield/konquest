package konquest.hook;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.util.DiscordUtil;
import konquest.Konquest;
import konquest.listener.DiscordSRVListener;
import konquest.model.KonKingdom;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;

public class DiscordSrvHook implements PluginHook {

	private Konquest konquest;
	private boolean isEnabled;
	private DiscordSRVListener discordSrvListener;
	
	public DiscordSrvHook(Konquest konquest) {
		this.konquest = konquest;
		this.isEnabled = false;
		this.discordSrvListener = new DiscordSRVListener(konquest);
	}
	
	@Override
	public void reload() {
		// Attempt to integrate DiscordSRV
		Plugin discordSrv = Bukkit.getPluginManager().getPlugin("DiscordSRV");
		if (discordSrv != null && discordSrv.isEnabled()) {
			if(konquest.getConfigManager().getConfig("core").getBoolean("core.integration.discordsrv",false)) {
				try {
					DiscordSRV.api.subscribe(discordSrvListener);
					isEnabled = true;
					ChatUtil.printConsoleAlert("Successfully integrated DiscordSRV");
				} catch (Exception e) {
					ChatUtil.printConsoleError("Failed to integrate DiscordSRV, see exception message:");
					e.printStackTrace();
				}
			} else {
				ChatUtil.printConsoleAlert("Disabled DiscordSRV integration from core config settings.");
			}
		} else {
			ChatUtil.printConsoleAlert("Could not integrate DiscordSRV, missing or disabled.");
		}
	}
	
	@Override
	public void shutdown() {
		if(isEnabled && discordSrvListener != null) {
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
		String result = "";
		if (isEnabled) {
			String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
	        if (discordId == null) {
	        	result = ChatColor.RED + "You are not linked to Discord. Type /discord link";
	        } else {
	        	User user = DiscordUtil.getJda().getUserById(discordId);
		        if (user == null) {
		        	result = ChatColor.YELLOW + "Couldn't find the Discord user you're linked to.";
		        } else {
		        	result = ChatColor.GREEN + "You're linked to " + user.getAsTag() + " in Discord.";
		        }
	        }
		}
		return result;
	}
	
	public boolean sendGameToDiscordMessage(String message, String channel) {
		boolean result = false;
		if (isEnabled) {
	        TextChannel textChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("channel");
	
	        // null if the channel isn't specified in the config.yml
	        if (textChannel != null) {
	            textChannel.sendMessage(message).queue();
	            result = true;
	        } else {
	        	ChatUtil.printDebug("Channel called \""+channel+"\" could not be found in the DiscordSRV configuration");
	        }
		}
        return result;
	}
	
	public void sendGameChatToDiscord(Player player, String message, String channel, boolean isCancelled) {
		if (isEnabled) {
			DiscordSRV.getPlugin().processChatMessage(player, message, channel, isCancelled);
		}
	}
	
	// Send message from discord to kingdom chat
	public void sendDiscordToGameChatKingdomChannel(User guildUser, Message guildMessage, String kingdomChannel) {
		if (isEnabled) {
			if(!kingdomChannel.equalsIgnoreCase("global") && konquest.getKingdomManager().isKingdom(kingdomChannel)) {
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
		}
	}

}
