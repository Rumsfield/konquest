package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.model.KonOfflinePlayer;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KingdomAdminCommand extends CommandBase {

	public KingdomAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

	@Override
	public void execute() {
		// k admin kingdom menu|create|destroy|add|kick|rename <kingdom> [<name>]
		if (getArgs().length != 4 && getArgs().length != 5) {
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
		} else {
        	
        	// Check for player
        	Player bukkitPlayer = (Player) getSender();
        	if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
    		KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
    		
    		// Get sub-command and target kingdom
    		String subCmd = getArgs()[2];
    		String kingdomName = getArgs()[3];
    		// Check for valid kingdom
    		if(!getKonquest().getKingdomManager().isKingdom(kingdomName)) {
    			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage());
    			return;
    		}
    		KonKingdom kingdom = getKonquest().getKingdomManager().getKingdom(kingdomName);
    		if(kingdom == null || !kingdom.isCreated()) {
    			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
    		
    		// Execute sub-commands
    		switch(subCmd.toLowerCase()) {
            	case "menu":
            		// Open the kingdom menu
            		getKonquest().getDisplayManager().displayKingdomMenu(player, kingdom, true);
            		break;
            		
            	case "create":
            		// Create a new kingdom, admin kingdom
            		// Needs kingdom name and template name arguments
            		if(getArgs().length == 5) {
            			String templateName = getArgs()[4];
            			
            			if(getKonquest().validateName(kingdomName,bukkitPlayer) != 0) {
            				// Player receives error message within validateName method
            				return;
    	            	}

                    	int status = getKonquest().getKingdomManager().createKingdom(bukkitPlayer.getLocation(), kingdomName, templateName, player, true);
                    	
                    	if(status == 0) {
                    		// Successful kingdom creation
                    		//TODO: KR messages
                    		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_GUILD_NOTICE_CREATE.getMessage(kingdomName));
                    		ChatUtil.sendBroadcast(MessagePath.COMMAND_SETTLE_BROADCAST_SETTLE.getMessage(bukkitPlayer.getName(),kingdomName));
                    		
                    		// Open kingdom menu for newly created kingdom
                    		KonKingdom newKingdom = getKonquest().getKingdomManager().getKingdom(kingdomName);
                    		getKonquest().getDisplayManager().displayKingdomMenu(player, newKingdom, true);

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
	                    			int min_distance_sanc = getKonquest().getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_SANCTUARY.getPath());
	                    			int min_distance_town = getKonquest().getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_TOWN.getPath());
	                    			int min_distance = Math.min(min_distance_sanc, min_distance_town);
									ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_PROXIMITY.getMessage(distance,min_distance));
	                    			//TODO: KR message
	                    			break;
	                    		case 7:
	                    			distance = getKonquest().getTerritoryManager().getDistanceToClosestTerritory(bukkitPlayer.getLocation());
	                    			int max_distance_all = getKonquest().getCore().getInt(CorePath.TOWNS_MAX_DISTANCE_ALL.getPath());
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
            	
            	case "destroy":
            		// Remove a kingdom
            		boolean pass = getKonquest().getKingdomManager().removeKingdom(kingdomName);
                	if(!pass) {
                		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
                        return;
                	} else {
                		ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
                	}
            		break;
            		
            	case "add":
            		// Invite a new kingdom member
            		if(getArgs().length == 5) {
            			String playerName = getArgs()[4];
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
            		// Remove a kingdom member
            		if(getArgs().length == 5) {
            			String playerName = getArgs()[4];
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
            		// Rename your kingdom
            		if(getArgs().length == 5) {
            			String newName = getArgs()[4];
            			String oldName = kingdom.getName();
            			if(getKonquest().validateName(newName,bukkitPlayer) != 0) {
            				// Player receives error message within validateName method
            				return;
    	            	}
                    	int status = getKonquest().getKingdomManager().renameKingdom(oldName, newName, player, true);
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

	@Override
	public List<String> tabComplete() {
		// k admin kingdom menu|create|remove|add|kick|rename <kingdom> [<name>]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		if(getArgs().length == 3) {
			// suggest sub-commands
			tabList.add("menu");
			tabList.add("create");
			tabList.add("destroy");
			tabList.add("add");
			tabList.add("kick");
			tabList.add("rename");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			// suggest kingdoms or new name
			String subCommand = getArgs()[2];
			if(subCommand.equalsIgnoreCase("create")) {
				tabList.add("***");
			} else {
				tabList.addAll(getKonquest().getKingdomManager().getKingdomNames());
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 5) {
			// suggest appropriate names
			String subCommand = getArgs()[2];
			String kingdomName = getArgs()[3];
			KonKingdom kingdom = getKonquest().getKingdomManager().getKingdom(kingdomName);
			String name;
			if(subCommand.equalsIgnoreCase("create")) {
				// template name
				tabList.addAll(getKonquest().getSanctuaryManager().getAllValidTemplateNames());
			} else if(subCommand.equalsIgnoreCase("rename")) {
				// new kingdom name
				tabList.add("***");
			} else if(subCommand.equalsIgnoreCase("add")) {
				// non-kingdom member names
				if(kingdom != null && kingdom.isCreated()) {
					for(KonOfflinePlayer offlinePlayer : getKonquest().getPlayerManager().getAllKonquestOfflinePlayers()) {
						name = offlinePlayer.getOfflineBukkitPlayer().getName();
						if(name != null && !kingdom.isMember(offlinePlayer.getOfflineBukkitPlayer().getUniqueId())) {
							tabList.add(name);
						}
					}
				}
			} else if(subCommand.equalsIgnoreCase("kick")) {
				// kingdom member names
				if(kingdom != null && kingdom.isCreated()) {
					for(KonOfflinePlayer offlinePlayer : getKonquest().getPlayerManager().getAllKonquestOfflinePlayers()) {
						name = offlinePlayer.getOfflineBukkitPlayer().getName();
						if(name != null && kingdom.isMember(offlinePlayer.getOfflineBukkitPlayer().getUniqueId())) {
							tabList.add(name);
						}
					}
				}
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[4], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
