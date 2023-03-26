package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonDirective;
import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.model.KonOfflinePlayer;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KingdomCommand extends CommandBase {
	
	public KingdomCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

	@Override
	public void execute() {
		// Notify sender when admin
		if(getSender().hasPermission("konquest.command.admin") && getKonquest().getSanctuaryManager().getNumTemplates() == 0) {
			ChatUtil.sendError((Player) getSender(),MessagePath.COMMAND_KINGDOM_ERROR_NO_TEMPLATES.getMessage());
		}
		// kingdom [menu|create|add|kick|rename|templates] [name] [template]
		if (getArgs().length != 1 && getArgs().length != 2 && getArgs().length != 3 && getArgs().length != 4) {
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
			if(player == null) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
				return;
			}
    		// Check for player's kingdom, should be either barbarians or another non-null kingdom
    		KonKingdom kingdom = player.getKingdom();
    		if(kingdom == null) {
    			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
			String kingdomName = kingdom.getName();
    		
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
	            			String newKingdomName = getArgs()[2];
	            			String templateName = getArgs()[3];
	            			if(getKonquest().validateName(newKingdomName,bukkitPlayer) != 0) {
	            				// Player receives error message within validateName method
	            				return;
	    	            	}

	                    	int status = getKonquest().getKingdomManager().createKingdom(bukkitPlayer.getLocation(), newKingdomName, templateName, player, false);
	                    	
	                    	if(status == 0) {
	                    		// Successful kingdom creation
								KonKingdom createdKingdom = getKonquest().getKingdomManager().getKingdom(newKingdomName);
	                    		ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_CREATE.getMessage(bukkitPlayer.getName(), newKingdomName));
								// Teleport player to safe place around monument, facing monument
								getKonquest().getKingdomManager().teleportAwayFromCenter(createdKingdom.getCapital());
								// Play a success sound
								Konquest.playTownSettleSound(bukkitPlayer.getLocation());
								// Open kingdom menu for newly created kingdom
	                    		KonKingdom newKingdom = getKonquest().getKingdomManager().getKingdom(newKingdomName);
	                    		getKonquest().getDisplayManager().displayKingdomMenu(player, newKingdom, false);
	                    		// Updates
	                    		getKonquest().getKingdomManager().updatePlayerMembershipStats(player);
								// Update directive progress
								getKonquest().getDirectiveManager().updateDirectiveProgress(player, KonDirective.CREATE_KINGDOM);
	                    	} else {
	                    		switch(status) {
		                    		case 1:
										// Note that this error should not be reached due to previous checks
		                    			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_TAKEN_NAME.getMessage());
		                    			break;
		                    		case 2:
		                    			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_KINGDOM_ERROR_BARBARIAN_CREATE.getMessage());
		                    			break;
		                    		case 3:
										double templateCost = getKonquest().getSanctuaryManager().getTemplate(templateName).getCost();
										double totalCost = getKonquest().getKingdomManager().getCostCreate() + templateCost;
		                    			String cost = String.format("%.2f",totalCost);
		                    			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(cost));
		                    			break;
		                    		case 4:
		                    			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_KINGDOM_ERROR_INVALID_TEMPLATE.getMessage());
		                    			break;
		                    		case 5:
		                    			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
		                    			break;
		                    		case 6:
		                    			int distance = getKonquest().getTerritoryManager().getDistanceToClosestTerritory(bukkitPlayer.getLocation());
		                    			int min_distance_sanc = getKonquest().getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_SANCTUARY.getPath());
		                    			int min_distance_town = getKonquest().getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_TOWN.getPath());
		                    			int min_distance = Math.min(min_distance_sanc, min_distance_town);
										ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_PROXIMITY.getMessage(distance,min_distance));
		                    			break;
		                    		case 7:
		                    			distance = getKonquest().getTerritoryManager().getDistanceToClosestTerritory(bukkitPlayer.getLocation());
		                    			int max_distance_all = getKonquest().getCore().getInt(CorePath.TOWNS_MAX_DISTANCE_ALL.getPath());
		                    			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_MAX.getMessage(distance,max_distance_all));
		                    			break;
		                    		case 8:
		                    			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_PLACEMENT.getMessage());
		                    			break;
	                    			default:
	                    				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
	                    				break;
	                    		}
	                    	}
	            		} else {
							ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_KINGDOM_ERROR_MISSING_TEMPLATE.getMessage());
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
	            			boolean status = getKonquest().getKingdomManager().joinKingdomInvite(player, offlinePlayer.getOfflineBukkitPlayer(), kingdom);
							if(status) {
								ChatUtil.sendNotice((Player)player, MessagePath.COMMAND_KINGDOM_NOTICE_INVITE_SENT.getMessage(playerName));
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
	            				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_KINGDOM_ERROR_KICK_MASTER.getMessage());
	            				return;
	            			}
	            			playerName = bukkitOfflinePlayer.getName();
	            			boolean status = getKonquest().getKingdomManager().kickKingdomMember(player, bukkitOfflinePlayer);
							if(status) {
								ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_KINGDOM_NOTICE_KICK.getMessage(playerName));
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
	            					ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_KINGDOM_NOTICE_RENAME.getMessage(newName));
									ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_RENAME.getMessage(oldName,newName));
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
					case "templates":
						// Show monument template info menu
						getKonquest().getDisplayManager().displayTemplateInfoMenu(player);
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
		// k kingdom [menu|create|add|kick|rename|templates] [name] [template]
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
			tabList.add("templates");
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
				List<String> templateList = new ArrayList<>(getKonquest().getSanctuaryManager().getAllValidTemplateNames());
				tabList.addAll(templateList);
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
