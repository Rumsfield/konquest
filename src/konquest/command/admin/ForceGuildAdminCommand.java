package konquest.command.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonGuild;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class ForceGuildAdminCommand extends CommandBase {

	public ForceGuildAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

	@Override
	public void execute() {
		// k admin forceguild <name> [menu|add|kick|rename] [name]
		
		if (getArgs().length != 3 && getArgs().length != 4 && getArgs().length != 5) {
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	
        	if(!getKonquest().getGuildManager().isEnabled()) {
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DISABLED.getMessage());
        		return;
    		}
        	
        	// Check for player
        	Player bukkitPlayer = (Player) getSender();
        	if(!getKonquest().getPlayerManager().isPlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
    		KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);

    		// Get the target guild
    		String guildName = getArgs()[2];
    		KonGuild guild = getKonquest().getGuildManager().getGuild(guildName);
    		if(guild == null) {
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage());
        		return;
    		}
    		
        	if(getArgs().length == 3) {
        		// No sub-command, open guild menu
        		getKonquest().getDisplayManager().displayGuildMenu(player, guild, true);
        	} else if(getArgs().length >= 4) {
        		// Has sub-commands
        		String subCmd = getArgs()[3];

        		switch(subCmd.toLowerCase()) {
	            	case "menu":
	            		// Open the guild menu
	            		getKonquest().getDisplayManager().displayGuildMenu(player, guild, true);
	            		break;
	            	case "add":
	            		// Invite a new guild member
	            		if(getArgs().length == 5) {
	            			String playerName = getArgs()[4];
	            			KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getAllPlayerFromName(playerName);
	    			    	if(offlinePlayer == null) {
	    						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
	    		        		return;
	    					}
	            			if(!guild.getKingdom().equals(offlinePlayer.getKingdom())) {
	            				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
	    		        		return;
	            			}
	            			playerName = offlinePlayer.getOfflineBukkitPlayer().getName();
	            			boolean status = getKonquest().getGuildManager().joinGuildInvite(offlinePlayer, guild);
	            			if(status) {
	            				ChatUtil.sendNotice((Player) getSender(), "Invited player to guild");
	            			} else {
	            				ChatUtil.sendError((Player) getSender(), "Failed to invite to guild");
	            			}
	            		} else {
	            			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	            		}
	            		break;
	            	case "kick":
	            		// Remove a guild member
	            		if(getArgs().length == 5) {
	            			String playerName = getArgs()[4];
	            			KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getAllPlayerFromName(playerName);
	    			    	if(offlinePlayer == null) {
	    						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
	    		        		return;
	    					}
	            			if(!guild.getKingdom().equals(offlinePlayer.getKingdom())) {
	            				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
	    		        		return;
	            			}
	            			playerName = offlinePlayer.getOfflineBukkitPlayer().getName();
	            			boolean status = getKonquest().getGuildManager().kickGuildMember(offlinePlayer.getOfflineBukkitPlayer(), guild);
	            			if(status) {
	            				ChatUtil.sendNotice((Player) getSender(), "Kicked player from guild");
	            			} else {
	            				ChatUtil.sendError((Player) getSender(), "Failed to kick player");
	            			}
	            		} else {
	            			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	            		}
	            		break;
	            	case "rename":
	            		// Rename the guild
	            		if(getArgs().length == 5) {
	            			String guildNewName = getArgs()[4];
	            			if(getKonquest().validateName(guildNewName,bukkitPlayer) != 0) {
	    	            		return;
	    	            	}
	                    	boolean status = getKonquest().getGuildManager().renameGuild(guild, guildNewName, player);
	                    	if(status) {
	            				ChatUtil.sendNotice((Player) getSender(), "Successfully renamed guild to: "+guildNewName);
	            			} else {
	            				ChatUtil.sendError((Player) getSender(), "Failed to rename guild");
	            			}
	            		} else {
	            			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	            		}
	            		break;
            		default:
            			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            			break;
        		}
        	}
        }
		
	}

	@Override
	public List<String> tabComplete() {
		// k admin forceguild <name> [menu|add|kick|rename] [name]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		if(getArgs().length == 3) {
			// suggest guild names
			tabList.addAll(getKonquest().getGuildManager().getAllGuildNames());
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			// suggest sub-commands
			tabList.add("menu");
			tabList.add("add");
			tabList.add("kick");
			tabList.add("rename");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 5) {
			// Check for specified guild
			String guildName = getArgs()[2];
			KonGuild guild = getKonquest().getGuildManager().getGuild(guildName);
			// suggest appropriate arguments
			String subCommand = getArgs()[3];
			String name;
			if(guild != null) {
				if(subCommand.equalsIgnoreCase("rename")) {
					tabList.add("***");
				} else if(subCommand.equalsIgnoreCase("add")) {
					List<String> playerList = new ArrayList<>();
					for(KonOfflinePlayer offlinePlayer : getKonquest().getPlayerManager().getAllPlayersInKingdom(guild.getKingdom())) {
						name = offlinePlayer.getOfflineBukkitPlayer().getName();
						if(name != null && !guild.isMember(offlinePlayer.getOfflineBukkitPlayer().getUniqueId())) {
							playerList.add(name);
						}
					}
					tabList.addAll(playerList);
				} else if(subCommand.equalsIgnoreCase("kick")) {
					List<String> playerList = new ArrayList<>();
					for(KonOfflinePlayer offlinePlayer : getKonquest().getPlayerManager().getAllPlayersInKingdom(guild.getKingdom())) {
						name = offlinePlayer.getOfflineBukkitPlayer().getName();
						if(name != null && guild.isMember(offlinePlayer.getOfflineBukkitPlayer().getUniqueId())) {
							playerList.add(name);
						}
					}
					tabList.addAll(playerList);
				}
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[4], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}

}
