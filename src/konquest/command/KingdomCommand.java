package konquest.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import konquest.Konquest;
import konquest.model.KonKingdom;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.CorePath;
import konquest.utility.MessagePath;

//TODO: KR replace GUILD messages with KINGDOM
public class KingdomCommand extends CommandBase {
	
	public KingdomCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

	@Override
	public void execute() {
		// kingdom [menu|create|add|kick|rename] [name] [template]
		if (getArgs().length != 1 && getArgs().length != 2 && getArgs().length != 3 && getArgs().length != 4) {
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	
        	// Check for player
        	Player bukkitPlayer = (Player) getSender();
        	if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
    		KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
    		
    		// Check for player's kingdom, should be either barbarians or another non-null kingdom
    		KonKingdom kingdom = player.getKingdom();
    		if(kingdom == null) {
    			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
    		
        	if(getArgs().length == 1) {
        		// No sub-command, open kingdom menu
        		getKonquest().getDisplayManager().displayKingdomMenu(player, kingdom, false);
        	} else if(getArgs().length >= 2) {
        		// Has sub-commands
        		String subCmd = getArgs()[1];

        		switch(subCmd.toLowerCase()) {
	            	case "menu":
	            		// Open the kingdom menu
	            		getKonquest().getDisplayManager().displayKingdomMenu(player, kingdom, false);
	            		break;
	            	case "create":
	            		// Create a new kingdom
	            		// Needs kingdom name and template name arguments
	            		if(getArgs().length == 4) {
	            			String kingdomName = getArgs()[2];
	            			String templateName = getArgs()[3];
	            			if(getKonquest().validateName(kingdomName,bukkitPlayer) != 0) {
	            				// Player receives error message within validateName method
	            				return;
	    	            	}

	                    	int status = getKonquest().getKingdomManager().createKingdom(bukkitPlayer.getLocation(), kingdomName, templateName, player, false);
	                    	
	                    	if(status == 0) {
	                    		// Successful kingdom creation
	                    		//TODO: KR messages
	                    		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_GUILD_NOTICE_CREATE.getMessage(kingdomName));
	                    		ChatUtil.sendBroadcast(MessagePath.COMMAND_SETTLE_BROADCAST_SETTLE.getMessage(bukkitPlayer.getName(),kingdomName));
	                    		
	                    		// Open kingdom menu for newly created kingdom
	                    		KonKingdom newKingdom = getKonquest().getKingdomManager().getKingdom(kingdomName);
	                    		getKonquest().getDisplayManager().displayKingdomMenu(player, newKingdom, false);
	                    		
	                    		// Updates
	                    		getKonquest().getKingdomManager().updatePlayerMembershipStats(player);
	                    		//TODO: Add directive
	                    		//getKonquest().getDirectiveManager().updateDirectiveProgress(player, KonDirective.SETTLE_TOWN);
	                    		
	                    	} else {
	                    		switch(status) {
		                    		case 1:
		                    			//ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_TAKEN_NAME.getMessage());
		                    			//TODO: KR message - That name is already taken
		                    			break;
		                    		case 2:
		                    			//TODO: KR message - You must be a barbarian to create a kingdom. Use k exile
		                    			break;
		                    		case 3:
		                    			String cost = String.format("%.2f",getKonquest().getKingdomManager().getCostCreate());
		                    			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(cost));
		                    			break;
		                    		case 4:
		                    			//TODO: KR message - That is not a valid monument template
		                    			break;
		                    		case 5:
		                    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
		                    			break;
		                    		case 6:
		                    			int distance = getKonquest().getTerritoryManager().getDistanceToClosestTerritory(bukkitPlayer.getLocation());
		                    			int min_distance_capital = getKonquest().getConfigManager().getConfig("core").getInt(CorePath.TOWNS_MIN_DISTANCE_CAPITAL.getPath());
		                    			int min_distance_town = getKonquest().getConfigManager().getConfig("core").getInt(CorePath.TOWNS_MIN_DISTANCE_TOWN.getPath());
		                    			int min_distance = 0;
		                    			if(min_distance_capital < min_distance_town) {
		                    				min_distance = min_distance_capital;
		                    			} else {
		                    				min_distance = min_distance_town;
		                    			}
		                    			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_PROXIMITY.getMessage(distance,min_distance));
		                    			//TODO: KR message
		                    			break;
		                    		case 7:
		                    			distance = getKonquest().getTerritoryManager().getDistanceToClosestTerritory(bukkitPlayer.getLocation());
		                    			int max_distance_all = getKonquest().getConfigManager().getConfig("core").getInt(CorePath.TOWNS_MAX_DISTANCE_ALL.getPath());
		                    			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_MAX.getMessage(distance,max_distance_all));
		                    			//TODO: KR message
		                    			break;
		                    		case 8:
		                    			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_PLACEMENT.getMessage());
		                    			//TODO: KR message
		                    			break;
	                    			default:
	                    				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
	                    				break;
	                    		}
	                    	}
	            		} else {
	            			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	            		}
	            		break;
	            	case "add":
	            		// Invite a new kingdom member (officers only)
	            		if(player.isBarbarian()) {
	            			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
	            			return;
	            		}
	            		if(!kingdom.isOfficer(bukkitPlayer.getUniqueId())) {
	            			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
	    	        		return;
	            		}
	            		if(getArgs().length == 3) {
	            			String playerName = getArgs()[2];
	            			KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayerFromName(playerName);
	    			    	if(offlinePlayer == null) {
	    						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
	    		        		return;
	    					}
	            			playerName = offlinePlayer.getOfflineBukkitPlayer().getName();
	            			int status = getKonquest().getKingdomManager().joinKingdomInvite(offlinePlayer.getOfflineBukkitPlayer(), kingdom);
	            			if(status == 0) {
	            				ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_GUILD_NOTICE_INVITE.getMessage(playerName));
	            			} else {
	            				switch(status) {
		            				case 1:
		            					//TODO: KR message - Player is already a member
		            					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
		            					break;
		            				case 2:
		            					//TODO: KR message - Player has already been invited, pending response
		            					ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_GUILD_ERROR_ADD_MEMBER.getMessage(playerName));
		            					break;
	            					default:
	            						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_FAILED.getMessage());
	            						break;
	            				}
	            			}
	            		} else {
	            			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	            		}
	            		break;
	            	case "kick":
	            		// Remove a kingdom member (officers only)
	            		if(player.isBarbarian()) {
	            			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
	            			return;
	            		}
	            		if(!kingdom.isOfficer(bukkitPlayer.getUniqueId())) {
	            			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
	    	        		return;
	            		}
	            		if(getArgs().length == 3) {
	            			String playerName = getArgs()[2];
	            			KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayerFromName(playerName);
	    			    	if(offlinePlayer == null) {
	    						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
	    		        		return;
	    					}
	    			    	OfflinePlayer bukkitOfflinePlayer = offlinePlayer.getOfflineBukkitPlayer();
	            			if(kingdom.isMaster(bukkitOfflinePlayer.getUniqueId())) {
	            				//TODO: Path this
	            				ChatUtil.sendError(bukkitPlayer, "Cannot kick kingdom master");
	            				return;
	            			}
	            			playerName = bukkitOfflinePlayer.getName();
	            			boolean status = getKonquest().getKingdomManager().kickKingdomMember(bukkitOfflinePlayer, kingdom);
	            			if(status) {
	            				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_GUILD_NOTICE_KICK.getMessage(playerName));
	            			} else {
	            				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_FAILED.getMessage());
	            			}
	            		} else {
	            			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	            		}
	            		break;
	            	case "rename":
	            		// Rename your kingdom (master only)
	            		if(player.isBarbarian()) {
	            			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
	            			return;
	            		}
	            		if(!kingdom.isMaster(bukkitPlayer.getUniqueId())) {
	            			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
	    	        		return;
	            		}
	            		if(getArgs().length == 3) {
	            			String newName = getArgs()[2];
	            			String oldName = kingdom.getName();
	            			if(getKonquest().validateName(newName,bukkitPlayer) != 0) {
	            				// Player receives error message within validateName method
	            				return;
	    	            	}
	                    	int status = getKonquest().getKingdomManager().renameKingdom(oldName, newName, player, false);
	                    	switch(status) {
	            				case 0:
	            					ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_GUILD_NOTICE_RENAME.getMessage(newName));
	            					break;
	            				case 1:
	            					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage());
	            					break;
	            				case 2:
	            					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_TAKEN_NAME.getMessage());
	            					break;
	            				case 3:
	            					String cost = String.format("%.2f",getKonquest().getKingdomManager().getCostRename());
	            					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(cost));
	            					break;
	        					default:
	        						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_FAILED.getMessage());
	        						break;
	        				}
	            		} else {
	            			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	            		}
	            		break;
            		default:
            			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            			break;
        		}
        	}
        }
	}

	@Override
	public List<String> tabComplete() {
		// k kingdom [menu|create|add|kick|rename] [name] [template]
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
			// Check for player's kingdom
			KonKingdom kingdom = player.getKingdom();
			// suggest appropriate arguments
			String subCommand = getArgs()[1];
			String name;
			if(subCommand.equalsIgnoreCase("create") || subCommand.equalsIgnoreCase("rename")) {
				tabList.add("***");
			} else if(subCommand.equalsIgnoreCase("add")) {
				List<String> playerList = new ArrayList<>();
				for(KonOfflinePlayer offlinePlayer : getKonquest().getPlayerManager().getAllKonquestOfflinePlayers()) {
					name = offlinePlayer.getOfflineBukkitPlayer().getName();
					if(name != null && kingdom != null && !kingdom.isMember(offlinePlayer.getOfflineBukkitPlayer().getUniqueId())) {
						playerList.add(name);
					}
				}
				tabList.addAll(playerList);
			} else if(subCommand.equalsIgnoreCase("kick")) {
				List<String> playerList = new ArrayList<>();
				for(KonOfflinePlayer offlinePlayer : getKonquest().getPlayerManager().getAllKonquestOfflinePlayers()) {
					name = offlinePlayer.getOfflineBukkitPlayer().getName();
					if(name != null && kingdom != null && kingdom.isMember(offlinePlayer.getOfflineBukkitPlayer().getUniqueId())) {
						playerList.add(name);
					}
				}
				tabList.addAll(playerList);
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			// Suggest template name when creating
			String subCommand = getArgs()[1];
			if(subCommand.equalsIgnoreCase("create")) {
				List<String> templateList = new ArrayList<>();
				templateList.addAll(getKonquest().getSanctuaryManager().getAllValidTemplateNames());
				tabList.addAll(templateList);
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
