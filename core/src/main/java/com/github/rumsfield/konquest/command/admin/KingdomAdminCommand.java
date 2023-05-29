package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
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
import java.util.UUID;

public class KingdomAdminCommand extends CommandBase {

	public KingdomAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

	@Override
	public void execute() {
		Player bukkitPlayer = (Player) getSender();
		// Notify sender of missing templates
		if(getKonquest().getSanctuaryManager().getNumTemplates() == 0) {
			ChatUtil.sendError(bukkitPlayer,MessagePath.COMMAND_ADMIN_KINGDOM_ERROR_NO_TEMPLATES.getMessage());
		}
		// k admin kingdom menu|create|destroy|add|kick|rename <kingdom> [<name>]
		if (getArgs().length != 4 && getArgs().length != 5) {
			sendInvalidArgMessage(bukkitPlayer, AdminCommandType.KINGDOM);
		} else {
        	
        	// Check for player

        	if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
    		KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
    		assert player != null;
    		// Get sub-command and target kingdom
    		String subCmd = getArgs()[2];
    		String kingdomName = getArgs()[3];
			KonKingdom kingdom;
    		
    		// Execute sub-commands
    		switch(subCmd.toLowerCase()) {
            	case "menu":
            		// Open the kingdom menu
					// Check for valid kingdom
					if(!getKonquest().getKingdomManager().isKingdom(kingdomName)) {
						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
						return;
					}
					kingdom = getKonquest().getKingdomManager().getKingdom(kingdomName);
					assert kingdom != null;
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
							KonKingdom createdKingdom = getKonquest().getKingdomManager().getKingdom(kingdomName);
                    		ChatUtil.sendBroadcast(MessagePath.COMMAND_ADMIN_KINGDOM_BROADCAST_CREATE.getMessage(kingdomName));
							// Teleport player to safe place around monument, facing monument
							getKonquest().getKingdomManager().teleportAwayFromCenter(createdKingdom.getCapital());
							// Open kingdom menu for newly created kingdom
                    		KonKingdom newKingdom = getKonquest().getKingdomManager().getKingdom(kingdomName);
                    		getKonquest().getDisplayManager().displayKingdomMenu(player, newKingdom, true);
                    	} else {
							switch(status) {
								case 1:
									// Note that this error should not be reached due to previous checks
									ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_TAKEN_NAME.getMessage());
									break;
								case 4:
									ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_KINGDOM_ERROR_INVALID_TEMPLATE.getMessage(templateName));
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
								case 12:
									ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_HEIGHT.getMessage());
									break;
								case 13:
									ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_INIT.getMessage());
									break;
								case 14:
									ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_AIR.getMessage());
									break;
								case 15:
									ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_WATER.getMessage());
									break;
								case 16:
									ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_CONTAINER.getMessage());
									break;
								case 22:
									ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_FLAT.getMessage());
									break;
								case 23:
									ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_HEIGHT.getMessage());
									break;
								default:
									ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
									break;
							}
                    	}
            		} else {
						sendInvalidArgMessage(bukkitPlayer, AdminCommandType.KINGDOM);
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
            		// Force a player to join a kingdom
            		if(getArgs().length == 5) {
            			String playerName = getArgs()[4];
            			KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayerFromName(playerName);
    			    	if(offlinePlayer == null) {
    						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
    		        		return;
    					}
            			playerName = offlinePlayer.getOfflineBukkitPlayer().getName();
						UUID id = offlinePlayer.getOfflineBukkitPlayer().getUniqueId();
						// Check for valid kingdom
						if(!getKonquest().getKingdomManager().isKingdom(kingdomName)) {
							ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
							return;
						}
						kingdom = getKonquest().getKingdomManager().getKingdom(kingdomName);
						assert kingdom != null;
						// Force player assignment into kingdom
						int status = getKonquest().getKingdomManager().assignPlayerKingdom(id,kingdom.getName(),true);
						switch(status) {
							case 0:
								// Success
								// Broadcast successful join
								ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_JOIN.getMessage(playerName,kingdom.getName()));
								break;
							case 5:
								// Player is already a member
								ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_KINGDOM_ERROR_INVITE_MEMBER.getMessage(playerName));
								break;
							default:
								ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_FAILED.getMessage());
								break;
						}
            		} else {
						sendInvalidArgMessage(bukkitPlayer, AdminCommandType.KINGDOM);
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
            			playerName = bukkitOfflinePlayer.getName();
						UUID id = offlinePlayer.getOfflineBukkitPlayer().getUniqueId();
						int status = getKonquest().getKingdomManager().exilePlayerBarbarian(id,true,true,true,true);
						switch(status) {
							case 0:
								// Success
								ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_KINGDOM_NOTICE_KICK.getMessage(playerName));
								break;
							case 1:
								// Player is already a barbarian
								ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_KINGDOM_ERROR_KICK_BARBARIAN.getMessage());
								break;
							default:
								ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_FAILED.getMessage());
								break;
						}
            		} else {
						sendInvalidArgMessage(bukkitPlayer, AdminCommandType.KINGDOM);
            		}
            		break;
            		
            	case "rename":
            		// Rename the kingdom
            		if(getArgs().length == 5) {
            			String newName = getArgs()[4];
						// Check for valid kingdom
						if(!getKonquest().getKingdomManager().isKingdom(kingdomName)) {
							ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
							return;
						}
						kingdom = getKonquest().getKingdomManager().getKingdom(kingdomName);
						assert kingdom != null;
            			String oldName = kingdom.getName();
            			if(getKonquest().validateName(newName,bukkitPlayer) != 0) {
            				// Player receives error message within validateName method
            				return;
    	            	}
                    	int status = getKonquest().getKingdomManager().renameKingdom(oldName, newName, player, true);
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
        					default:
        						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_FAILED.getMessage());
        						break;
        				}
            		} else {
						sendInvalidArgMessage(bukkitPlayer, AdminCommandType.KINGDOM);
            		}
            		break;

				case "admin":
					// Set whether a kingdom is admin operated
					if(getArgs().length == 5) {
						String newValue = getArgs()[4];
						boolean isNewValue = Boolean.parseBoolean(newValue);

						// Check for valid kingdom
						if(!getKonquest().getKingdomManager().isKingdom(kingdomName)) {
							ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
							return;
						}
						kingdom = getKonquest().getKingdomManager().getKingdom(kingdomName);
						assert kingdom != null;
						boolean isAdminOperated = kingdom.isAdminOperated();

						// Flag switch logic
						if(isNewValue) {
							// Make the kingdom into an admin kingdom, if not already
							if(isAdminOperated) {
								ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_ADMIN_KINGDOM_ERROR_ADMIN_SET.getMessage(kingdom.getName()));
								return;
							}
							// Set admin operated flag
							kingdom.setIsAdminOperated(true);
							// Clear master
							kingdom.clearMaster();
							ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_KINGDOM_NOTICE_ADMIN_SET.getMessage(kingdom.getName()));
						} else {
							// Make the kingdom into a regular kingdom, if not already
							if(!isAdminOperated) {
								ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_ADMIN_KINGDOM_ERROR_ADMIN_CLEAR.getMessage(kingdom.getName()));
								return;
							}
							// Clear admin operated flag
							kingdom.setIsAdminOperated(false);
							ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_KINGDOM_NOTICE_ADMIN_CLEAR.getMessage(kingdom.getName()));
						}
					} else {
						sendInvalidArgMessage(bukkitPlayer, AdminCommandType.KINGDOM);
					}

					break;

				default:
					sendInvalidArgMessage(bukkitPlayer, AdminCommandType.KINGDOM);
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
			tabList.add("admin");
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
			String name;
			if(subCommand.equalsIgnoreCase("create")) {
				// template name
				tabList.addAll(getKonquest().getSanctuaryManager().getAllValidTemplateNames());
			} else if(subCommand.equalsIgnoreCase("rename")) {
				// new kingdom name
				tabList.add("***");
			} else if(subCommand.equalsIgnoreCase("add")) {
				// non-kingdom member names
				KonKingdom kingdom = getKonquest().getKingdomManager().getKingdom(kingdomName);
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
				KonKingdom kingdom = getKonquest().getKingdomManager().getKingdom(kingdomName);
				if(kingdom != null && kingdom.isCreated()) {
					for(KonOfflinePlayer offlinePlayer : getKonquest().getPlayerManager().getAllKonquestOfflinePlayers()) {
						name = offlinePlayer.getOfflineBukkitPlayer().getName();
						if(name != null && kingdom.isMember(offlinePlayer.getOfflineBukkitPlayer().getUniqueId())) {
							tabList.add(name);
						}
					}
				}
			} else if(subCommand.equalsIgnoreCase("admin")) {
				// Suggest values
				tabList.add(String.valueOf(true));
				tabList.add(String.valueOf(false));
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[4], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
