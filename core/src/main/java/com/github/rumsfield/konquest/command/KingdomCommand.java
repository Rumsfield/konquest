package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.hook.WorldGuardHook;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.ColorRGB;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.awt.*;
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
		if(getSender().hasPermission("konquest.command.admin.monument") && getKonquest().getSanctuaryManager().getNumTemplates() == 0) {
			ChatUtil.sendError((Player) getSender(),MessagePath.COMMAND_ADMIN_KINGDOM_ERROR_NO_TEMPLATES.getMessage());
		}
		// kingdom [menu|create|invite|kick|rename|templates|webcolor] [template] [name]
		Player bukkitPlayer = (Player) getSender();
		if (getArgs().length != 1 && getArgs().length != 2 && getArgs().length != 3 && getArgs().length != 4) {
			sendInvalidArgMessage(bukkitPlayer,CommandType.KINGDOM);
		} else {
        	
        	// Check for player
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
						// Check if players can create kingdoms from config
						boolean isAdminOnly = getKonquest().getCore().getBoolean(CorePath.KINGDOMS_CREATE_ADMIN_ONLY.getPath());
						if(isAdminOnly) {
							ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
							return;
						}
						// Check for permission
						if(!bukkitPlayer.hasPermission("konquest.create.kingdom")) {
							ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage()+" konquest.create.kingdom");
							return;
						}
	            		// Needs kingdom name and template name arguments
						if(getArgs().length == 2) {
							ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_KINGDOM_ERROR_MISSING_TEMPLATE.getMessage());
							return;
						}
						if(getArgs().length == 3) {
							ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_KINGDOM_ERROR_MISSING_NAME.getMessage());
							return;
						}

						String templateName = getArgs()[2];
						String newKingdomName = getArgs()[3];
						if(getKonquest().validateName(newKingdomName,bukkitPlayer) != 0) {
							// Player receives error message within validateName method
							return;
						}

						// Check for other plugin flags
						if(getKonquest().getIntegrationManager().getWorldGuard().isEnabled()) {
							// Check new territory claims
							Location settleLoc = bukkitPlayer.getLocation();
							int radius = getKonquest().getCore().getInt(CorePath.TOWNS_INIT_RADIUS.getPath());
							World locWorld = settleLoc.getWorld();
							for(Point point : getKonquest().getAreaPoints(settleLoc, radius)) {
								if(!getKonquest().getIntegrationManager().getWorldGuard().isChunkClaimAllowed(locWorld,point,bukkitPlayer)) {
									// A region is denying this action
									ChatUtil.sendError(bukkitPlayer, MessagePath.REGION_ERROR_CLAIM_DENY.getMessage());
									return;
								}
							}
						}

						int createStatus = getKonquest().getKingdomManager().createKingdom(bukkitPlayer.getLocation(), newKingdomName, templateName, player, false);

						if(createStatus == 0) {
							// Successful kingdom creation
							KonKingdom createdKingdom = getKonquest().getKingdomManager().getKingdom(newKingdomName);
							ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_CREATE.getMessage(bukkitPlayer.getName(), newKingdomName));
							// Teleport player to safe place around monument, facing monument
							getKonquest().getKingdomManager().teleportAwayFromCenter(createdKingdom.getCapital());
							// Optionally apply starter shield
							int starterShieldDuration = getKonquest().getCore().getInt(CorePath.TOWNS_SHIELD_NEW_TOWNS.getPath(),0);
							if(starterShieldDuration > 0) {
								getKonquest().getShieldManager().shieldSet(createdKingdom.getCapital(),starterShieldDuration);
							}
							// Play a success sound
							Konquest.playTownSettleSound(bukkitPlayer.getLocation());
							// Open kingdom menu for newly created kingdom
							KonKingdom newKingdom = getKonquest().getKingdomManager().getKingdom(newKingdomName);
							getKonquest().getDisplayManager().displayKingdomMenu(player, newKingdom, false);
							// Updates
							getKonquest().getKingdomManager().updatePlayerMembershipStats(player);
							// Update directive progress
							getKonquest().getDirectiveManager().updateDirectiveProgress(player, KonDirective.CREATE_KINGDOM);
							// Update stats
							getKonquest().getAccomplishmentManager().modifyPlayerStat(player, KonStatsType.KINGDOMS, 1);
						} else {
							switch(createStatus) {
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
							ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_SETTLE_NOTICE_MAP_HINT.getMessage());
						}
	            		break;
	            	case "invite":
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
	            			boolean status = getKonquest().getKingdomManager().joinKingdomInvite(player, offlinePlayer, kingdom);
							if(status) {
								ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_KINGDOM_NOTICE_INVITE_SENT.getMessage(playerName));
							}
	            		} else {
							sendInvalidArgMessage(bukkitPlayer,CommandType.KINGDOM);
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
							sendInvalidArgMessage(bukkitPlayer,CommandType.KINGDOM);
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
							sendInvalidArgMessage(bukkitPlayer,CommandType.KINGDOM);
	            		}
	            		break;
					case "templates":
						// Show monument template info menu
						getKonquest().getDisplayManager().displayTemplateInfoMenu(player);
						break;
					case "webcolor":
						if(getKonquest().getCore().getBoolean(CorePath.KINGDOMS_WEB_COLOR_ADMIN_ONLY.getPath())) {
							// Only admins may set kingdom web colors
							ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
							return;
						}
						if(!kingdom.isMaster(bukkitPlayer.getUniqueId())) {
							// The player is not the kingdom master
							ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
							return;
						}
						if(getArgs().length == 2) {
							// Display current color
							int currentWebColor = kingdom.getWebColor();
							String colorStr = "default";
							if(currentWebColor != -1) {
								colorStr = ChatUtil.reverseLookupColorRGB(currentWebColor);
							}
							ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_KINGDOM_NOTICE_WEB_COLOR_SHOW.getMessage(kingdom.getName(),colorStr));
						} else if(getArgs().length == 3) {
							// Set new color
							String colorStr = getArgs()[2];
							if(colorStr.equalsIgnoreCase("default")) {
								kingdom.setWebColor(-1);
							} else {
								int newWebColor = ChatUtil.lookupColorRGB(colorStr);
								if(newWebColor != -1) {
									kingdom.setWebColor(newWebColor);
								} else {
									// The provided color string could not be resolved to a color
									ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_KINGDOM_ERROR_WEB_COLOR_INVALID.getMessage());
									return;
								}
							}
							// Update map
							getKonquest().getMapHandler().drawUpdateTerritory(kingdom.getCapital());
							for (KonTown town : kingdom.getTowns()) {
								getKonquest().getMapHandler().drawUpdateTerritory(town);
							}
							ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_KINGDOM_NOTICE_WEB_COLOR_SET.getMessage(kingdom.getName(),colorStr));
						} else {
							// Incorrect arguments
							sendInvalidArgMessage(bukkitPlayer,CommandType.KINGDOM);
						}
						break;
            		default:
						sendInvalidArgMessage(bukkitPlayer,CommandType.KINGDOM);
            			break;
        		}
        	}
        }
	}

	@Override
	public List<String> tabComplete() {
		// k kingdom [menu|create|invite|kick|rename|templates|webcolor] [template] [name]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		Player bukkitPlayer = (Player) getSender();
		KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
		
		if(getArgs().length == 2) {
			// suggest sub-commands
			tabList.add("menu");
			tabList.add("create");
			tabList.add("invite");
			tabList.add("kick");
			tabList.add("rename");
			tabList.add("templates");
			if(!getKonquest().getCore().getBoolean(CorePath.KINGDOMS_WEB_COLOR_ADMIN_ONLY.getPath())) {
				tabList.add("webcolor");
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 3) {
			// Check for player's kingdom
			KonKingdom kingdom = player.getKingdom();
			// suggest appropriate arguments
			String subCommand = getArgs()[1];
			String name;
			if(subCommand.equalsIgnoreCase("create")) {
				List<String> templateList = new ArrayList<>(getKonquest().getSanctuaryManager().getAllValidTemplateNames());
				tabList.addAll(templateList);
			} else if(subCommand.equalsIgnoreCase("rename")) {
				tabList.add("***");
			} else if(subCommand.equalsIgnoreCase("invite")) {
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
			} else if(subCommand.equalsIgnoreCase("webcolor")) {
				for(ColorRGB color : ColorRGB.values()) {
					tabList.add(color.getName());
				}
				tabList.add("#rrggbb");
				tabList.add("default");
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			String subCommand = getArgs()[1];
			if(subCommand.equalsIgnoreCase("create")) {
				tabList.add("***");
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
