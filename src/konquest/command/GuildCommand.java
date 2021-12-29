package konquest.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import konquest.Konquest;
import konquest.model.KonGuild;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class GuildCommand extends CommandBase {
	
	public GuildCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

	@Override
	public void execute() {
		// [menu|create|add|kick|rename] [name]
		if (getArgs().length != 1 && getArgs().length != 2 && getArgs().length != 3) {
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
    		
    		if(player.isBarbarian()) {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
                return;
        	}
    		
    		// Check for player's guild (can be null)
    		KonGuild guild = getKonquest().getGuildManager().getPlayerGuild(bukkitPlayer);
    		
        	if(getArgs().length == 1) {
        		// No sub-command, open guild menu
        		getKonquest().getDisplayManager().displayGuildMenu(player, guild);
        	} else if(getArgs().length >= 2) {
        		// Has sub-commands
        		String subCmd = getArgs()[1];

        		switch(subCmd.toLowerCase()) {
	            	case "menu":
	            		// Open the guild menu
	            		getKonquest().getDisplayManager().displayGuildMenu(player, guild);
	            		break;
	            	case "create":
	            		// Create a new guild
	            		if(getArgs().length == 3) {
	            			String guildName = getArgs()[2];
	            			
	            			if(!StringUtils.isAlphanumeric(guildName)) {
	                    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_FORMAT_NAME.getMessage());
	                            return;
	                    	}
	                    	if(getKonquest().getPlayerManager().isPlayerNameExist(guildName)) {
	                    		ChatUtil.sendError((Player) getSender(), "Failed to create, that name is already taken");
	                            return;
	                    	}
	                    	int status = getKonquest().getGuildManager().createGuild(guildName, player);
	                    	if(status == 0) {
	                    		// Successful guild creation
	                    		ChatUtil.sendNotice((Player) getSender(), "Successfully created new guild, "+guildName);
	                    		// Open guild menu for newly created guild
	                    		KonGuild newGuild = getKonquest().getGuildManager().getPlayerGuild(bukkitPlayer);
	                    		getKonquest().getDisplayManager().displayGuildMenu(player, newGuild);
	                    	} else {
	                    		switch(status) {
		                    		case 1:
		                    			ChatUtil.sendError((Player) getSender(), "Failed to create, bad name");
		                    			break;
		                    		case 2:
		                    			ChatUtil.sendError((Player) getSender(), "Failed to create, leave your current guild first!");
		                    			break;
		                    		case 3:
		                    			ChatUtil.sendError((Player) getSender(), "Failed to create, not enough favor");
		                    			break;
	                    			default:
	                    				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
	                    				break;
	                    		}
	                    	}
	            		} else {
	            			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	            		}
	            		break;
	            	case "add":
	            		// Invite a new guild member (officers only)
	            		if(guild == null) {
	            			ChatUtil.sendError((Player) getSender(), "Guild not found!");
	            			return;
	            		}
	            		if(!guild.isOfficer(bukkitPlayer.getUniqueId())) {
	            			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
	    	        		return;
	            		}
	            		if(getArgs().length == 3) {
	            			String playerName = getArgs()[2];
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
	            		// Remove a guild member (officers only)
	            		if(guild == null) {
	            			ChatUtil.sendError((Player) getSender(), "Guild not found!");
	            			return;
	            		}
	            		if(!guild.isOfficer(bukkitPlayer.getUniqueId())) {
	            			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
	    	        		return;
	            		}
	            		if(getArgs().length == 3) {
	            			String playerName = getArgs()[2];
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
	            		// Rename your guild (master only)
	            		if(guild == null) {
	            			ChatUtil.sendError((Player) getSender(), "Guild not found!");
	            			return;
	            		}
	            		if(!guild.isMaster(bukkitPlayer.getUniqueId())) {
	            			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
	    	        		return;
	            		}
	            		if(getArgs().length == 3) {
	            			String guildName = getArgs()[2];
	            			if(!StringUtils.isAlphanumeric(guildName)) {
	                    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_FORMAT_NAME.getMessage());
	                            return;
	                    	}
	                    	if(getKonquest().getPlayerManager().isPlayerNameExist(guildName)) {
	                    		ChatUtil.sendError((Player) getSender(), "Failed to rename, that name is already taken");
	                            return;
	                    	}
	                    	boolean status = getKonquest().getGuildManager().renameGuild(guild, guildName, player);
	                    	if(status) {
	            				ChatUtil.sendNotice((Player) getSender(), "Successfully renamed guild to: "+guildName);
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
		// k guild [menu|create|add|kick|rename] [name]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		Player bukkitPlayer = (Player) getSender();
		KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
		
		if(getArgs().length == 2) {
			// suggest sub-commands
			tabList.add("menu");
			tabList.add("create");
			tabList.add("add");
			tabList.add("kick");
			tabList.add("rename");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 3) {
			// Check for player's guild (can be null)
			KonGuild guild = getKonquest().getGuildManager().getPlayerGuild(bukkitPlayer);
			// suggest appropriate arguments
			String subCommand = getArgs()[1];
			String name;
			if(subCommand.equalsIgnoreCase("create") || subCommand.equalsIgnoreCase("rename")) {
				tabList.add("***");
			} else if(subCommand.equalsIgnoreCase("add")) {
				List<String> playerList = new ArrayList<>();
				for(KonOfflinePlayer offlinePlayer : getKonquest().getPlayerManager().getAllPlayersInKingdom(player.getKingdom())) {
					name = offlinePlayer.getOfflineBukkitPlayer().getName();
					if(name != null && guild != null && !guild.isMember(offlinePlayer.getOfflineBukkitPlayer().getUniqueId())) {
						playerList.add(name);
					}
				}
				tabList.addAll(playerList);
			} else if(subCommand.equalsIgnoreCase("kick")) {
				List<String> playerList = new ArrayList<>();
				for(KonOfflinePlayer offlinePlayer : getKonquest().getPlayerManager().getAllPlayersInKingdom(player.getKingdom())) {
					name = offlinePlayer.getOfflineBukkitPlayer().getName();
					if(name != null && guild != null && guild.isMember(offlinePlayer.getOfflineBukkitPlayer().getUniqueId())) {
						playerList.add(name);
					}
				}
				tabList.addAll(playerList);
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
