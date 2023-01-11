package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.model.KonOfflinePlayer;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonStatsType;
import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import com.github.rumsfield.konquest.utility.Timer;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TownCommand extends CommandBase {

	public TownCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

	@Override
	public void execute() {
		// k town <town> [menu|join|leave|add|kick|knight|lord|rename] [<name>]
		if (getArgs().length != 2 && getArgs().length != 3 && getArgs().length != 4) {
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
		} else {
        	Player bukkitPlayer = (Player) getSender();
        	if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
    		KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
    		// Verify player is not a barbarian
			assert player != null;
			if(player.isBarbarian()) {
    			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
        		return;
    		}
    		// Verify town or capital exists within sender's Kingdom
    		// Name can be the name of a town, or the name of the kingdom for the capital
        	String townName = getArgs()[1];
        	boolean isTown = player.getKingdom().hasTown(townName);
        	boolean isCapital = player.getKingdom().hasCapital(townName);
        	KonTown town;
        	if(isTown) {
        		town = player.getKingdom().getTown(townName);
        	} else if(isCapital) {
        		town = player.getKingdom().getCapital();
        	} else {
        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_BAD_NAME.getMessage());
        		return;
        	}
        	
        	if(getArgs().length == 2) {
        		// No sub-command, open town management menu for lord or knights
        		if(town.isPlayerLord(player.getOfflineBukkitPlayer()) || town.isPlayerKnight(player.getOfflineBukkitPlayer())) {
        			getKonquest().getDisplayManager().displayTownManagementMenu(player, town, false);
        		} else {
        			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
				}
        	} else if(getArgs().length >= 3) {
        		// Has sub-commands
        		String subCmd = getArgs()[2];
        		// Check if town has a lord
        		boolean notifyLordTakeover = false;
        		if(!town.isLordValid() && !subCmd.equalsIgnoreCase("lord")) {
            		if(town.isPlayerKnight(player.getOfflineBukkitPlayer())) {
            			notifyLordTakeover = true;
            		} else if(town.isPlayerResident(player.getOfflineBukkitPlayer()) && town.getPlayerKnights().isEmpty()) {
            			notifyLordTakeover = true;
            		} else if(town.getPlayerResidents().isEmpty()){
            			notifyLordTakeover = true;
            		}
            	}
        		if(notifyLordTakeover) {
        			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_NO_LORD.getMessage(townName,townName,bukkitPlayer.getName()));
        		}

	        	// Action based on sub-command
	        	String playerName = "";
	        	String newTownName = "";
	        	switch(subCmd.toLowerCase()) {
	        	case "menu":
	        		// Verify player is elite or lord of the Town
		        	if(!town.isPlayerLord(player.getOfflineBukkitPlayer()) && !town.isPlayerKnight(player.getOfflineBukkitPlayer())) {
		        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
		        		return;
		        	}
		        	getKonquest().getDisplayManager().displayTownManagementMenu(player, town, false);
	        		break;

	        	case "join":
	        		// Attempt to join the given town name
	        		if(!player.isBarbarian()) {
	        			if(town.isPlayerResident(bukkitPlayer)) {
	        				ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_JOIN_ERROR_TOWN_MEMBER.getMessage(townName));
	        				return;
	        			}
	        			UUID myId = bukkitPlayer.getUniqueId();
	        			if(town.isOpen() || town.isJoinInviteValid(myId)) {
	        				// There is an existing invite to join, or the town is open, add the player as a resident
	        				town.removeJoinRequest(myId);
	    	    			// Add the player as a resident
	    			    	if(town.addPlayerResident(bukkitPlayer,false)) {
	    			    		for(OfflinePlayer resident : town.getPlayerResidents()) {
	    			    			if(resident.isOnline()) {
	    			    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_JOIN_NOTICE_TOWN_RESIDENT.getMessage(bukkitPlayer.getName(),townName));
	    			    			}
	    			    		}
	    			    		getKonquest().getKingdomManager().updatePlayerMembershipStats(player);
	    			    	}
	        			} else {
	        				if(town.isJoinRequestValid(myId)) {
	        					// There is already an existing join request
	        					ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_JOIN_ERROR_TOWN_REQUEST.getMessage(townName));
	        				} else {
	        					if(town.getNumResidents() == 0) {
	        						// The town is empty, make the player into the lord
	        						town.setPlayerLord(bukkitPlayer);
	        						town.removeJoinRequest(myId);
	        			    		getKonquest().getKingdomManager().updatePlayerMembershipStats(player);
	        			    		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_JOIN_NOTICE_TOWN_JOIN_LORD.getMessage(townName));
	        					} else {
	        						// Create a new join request
	            					ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_JOIN_NOTICE_TOWN_REQUEST.getMessage(townName));
	            					town.addJoinRequest(myId, false);
	            					town.notifyJoinRequest(myId);
	        					}
	        				}
	        			}
	        		} else {
	        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
	        		}
	        		break;
	        		
	        	case "leave":
	        		// Attempt to leave the given town
	    			UUID myId = bukkitPlayer.getUniqueId();
	    			if(town.isPlayerResident(bukkitPlayer)) {
	    				// Player is resident trying to leave
	    				// Remove the player as a resident
	    		    	if(town.removePlayerResident(bukkitPlayer)) {
	    		    		for(OfflinePlayer resident : town.getPlayerResidents()) {
	    		    			if(resident.isOnline()) {
	    		    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_LEAVE_NOTICE_TOWN_RESIDENT.getMessage(bukkitPlayer.getName(),townName));
	    		    			}
	    		    		}
	    		    		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_LEAVE_NOTICE_PLAYER.getMessage(townName));
	    		    		getKonquest().getKingdomManager().updatePlayerMembershipStats(player);
	    		    	} else {
	    		    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_LEAVE_ERROR_NO_RESIDENT.getMessage(townName));
	    		    	}
	    			} else {
	    				// Player is not resident, try to decline join invite
	    				if(town.isJoinInviteValid(myId)) {
	    					// There is an existing invite to join, remove the player from the invites list
	    					town.removeJoinRequest(myId);
	    					ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_LEAVE_NOTICE_INVITE_DECLINE.getMessage(townName));
	    				} else {
	    					ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_LEAVE_ERROR_NO_RESIDENT.getMessage(townName));
	    				}
	    			}
	        		break;
	        		
				case "add":
					// Verify player is elite or lord of the Town
		        	if(!town.isPlayerLord(player.getOfflineBukkitPlayer()) && !town.isPlayerKnight(player.getOfflineBukkitPlayer())) {
		        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
		        		return;
		        	}
					if (getArgs().length == 4) {
						playerName = getArgs()[3];
					}
				    if(!playerName.equalsIgnoreCase("")) {
				    	KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayerFromName(playerName);
				    	if(offlinePlayer == null) {
							ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
			        		return;
						}
				    	if(!offlinePlayer.getKingdom().equals(town.getKingdom())) {
				    		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
			        		return;
				    	}
				    	playerName = offlinePlayer.getOfflineBukkitPlayer().getName();
				    	//TODO: Add inactive player check?
				    	if(town.isPlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
				    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_ADD_RESIDENT.getMessage(playerName,townName));
				    		return;
				    	}
				    	// Check for join request and add as resident
				    	// Otherwise create join invite
				    	UUID id = offlinePlayer.getOfflineBukkitPlayer().getUniqueId();
				    	KonPlayer onlinePlayer = getKonquest().getPlayerManager().getPlayerFromName(playerName);
				    	if(town.isJoinInviteValid(id)) {
				    		// There is already a join invite for this player
				    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_ADD_INVITE_RESIDENT.getMessage(playerName,townName));
				    		if(onlinePlayer != null) {
				    			ChatUtil.sendNotice(onlinePlayer.getBukkitPlayer(), MessagePath.COMMAND_TOWN_NOTICE_ADD_INVITE_REMINDER.getMessage(townName));
				    		}
				    	} else {
				    		// Check for join request
				    		if(town.isJoinRequestValid(id)) {
				    			// This player has already requested to join, add them
				    			town.removeJoinRequest(id);
				    			// Add the player as a resident
						    	if(town.addPlayerResident(offlinePlayer.getOfflineBukkitPlayer(),false)) {
						    		for(OfflinePlayer resident : town.getPlayerResidents()) {
						    			if(resident.isOnline()) {
						    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_ADD_SUCCESS.getMessage(playerName,townName));
						    			}
						    		}
						    		if(onlinePlayer != null) {
						    			getKonquest().getKingdomManager().updatePlayerMembershipStats(onlinePlayer);
						    		}
						    	}
				    		} else {
				    			// Create a new join invite
				    			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_ADD_INVITE.getMessage(playerName,townName));
				    			town.addJoinRequest(id, true);
				    			if(onlinePlayer != null) {
					    			ChatUtil.sendNotice(onlinePlayer.getBukkitPlayer(), MessagePath.COMMAND_TOWN_NOTICE_ADD_INVITE_PLAYER.getMessage(town.getName(),town.getName(),town.getName()));
					    		}
				    		}
				    	}
				    } else {
				    	ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
		        		return;
				    }
				    break;
				    
				case "kick":
					// Verify player is elite or lord of the Town
		        	if(!town.isPlayerLord(player.getOfflineBukkitPlayer()) && !town.isPlayerKnight(player.getOfflineBukkitPlayer())) {
		        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
		        		return;
		        	}
					if (getArgs().length == 4) {
						playerName = getArgs()[3];
					}
					if(!playerName.equalsIgnoreCase("")) {
						KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayerFromName(playerName);
						if(offlinePlayer == null) {
							ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
			        		return;
						}
						playerName = offlinePlayer.getOfflineBukkitPlayer().getName();
						// Prevent Elites from removing Lord
						if(town.isPlayerKnight(player.getOfflineBukkitPlayer()) && town.isPlayerLord(offlinePlayer.getOfflineBukkitPlayer())) {
							ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			        		return;
						}
						// Check for active invite and remove it,
						// otherwise deny join request
						UUID id = offlinePlayer.getOfflineBukkitPlayer().getUniqueId();
						KonPlayer onlinePlayer = getKonquest().getPlayerManager().getPlayerFromName(playerName);
				    	if(town.isJoinInviteValid(id)) {
				    		// There is a join invite for this player, revoke it
				    		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_KICK_REMOVE.getMessage(playerName,townName));
				    		town.removeJoinRequest(id);
				    	} else {
				    		// Check for join request
				    		if(town.isJoinRequestValid(id)) {
				    			// There is a join request for this player, deny it
				    			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_KICK_REMOVE.getMessage(playerName,townName));
					    		town.removeJoinRequest(id);
				    		} else {
				    			// Attempt to kick current resident
				    			// Remove the player as a resident
						    	if(town.removePlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
						    		for(OfflinePlayer resident : town.getPlayerResidents()) {
						    			if(resident.isOnline()) {
						    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_KICK_RESIDENT.getMessage(playerName,townName));
						    			}
						    		}
						    		if(onlinePlayer != null) {
						    			getKonquest().getKingdomManager().updatePlayerMembershipStats(onlinePlayer);
						    		}
						    	} else {
						    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_KICK_FAIL.getMessage(playerName,townName));
						    	}
				    		}
				    	}
				    } else {
				    	ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
		        		return;
				    }
					break;
				case "lord":
					// Verify conditions for sub-command access
					boolean isSenderOwner = false;
		        	if(town.isLordValid()) {
		        		// Lord exists, only permit access to owner
		        		isSenderOwner = true;
		        		if(!town.isPlayerLord(player.getOfflineBukkitPlayer())) {
			        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			        		return;
			        	}
		        	} else {
		        		// Lord does not exist
		        		if(!town.isOpen() && town.getKingdom().equals(player.getKingdom())) {
		        			// Check player roles in closed towns only, allow any member to claim lordship in open towns
			        		if(!town.getPlayerKnights().isEmpty()) {
			        			// Elite residents exist, permit access to them
			        			if(!town.isPlayerKnight(player.getOfflineBukkitPlayer())) {
			    	        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			    	        		return;
			    	        	}
			        		} else {
			        			// No Elite residents exist
			        			if(!town.getPlayerResidents().isEmpty()) {
			        				// Residents for this town exist, permit access to them
			        				if(!town.isPlayerResident(player.getOfflineBukkitPlayer())) {
				    	        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
				    	        		return;
				    	        	}
			        			} else {
			        				// No residents exist, permit access to any Kingdom member
			        				if(!town.getKingdom().equals(player.getKingdom())) {
				    	        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_ENEMY_TOWN.getMessage());
				    	        		return;
				    	        	}
			        			}
			        		}
		        		}
		        	}
		        	if (getArgs().length == 4) {
						playerName = getArgs()[3];
					}
					if(!playerName.equalsIgnoreCase("")) {
						KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayerFromName(playerName);
						if(offlinePlayer == null) {
							ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
							return;
						}
						if(isSenderOwner) {
							// The current Town owner is giving away Lordship
							if(!offlinePlayer.getKingdom().equals(town.getKingdom())) {
					    		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
				        		return;
					    	}
		        			if(offlinePlayer.getOfflineBukkitPlayer().getUniqueId().equals(player.getBukkitPlayer().getUniqueId())) {
		        				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
				        		return;
		        			}
							// Confirm with the player
			        		if(!player.isGiveLordConfirmed()) {
			        			// Prompt the player to confirm
			        			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_LORD_PROMPT_1.getMessage(townName));
			        			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_LORD_PROMPT_2.getMessage());
			        			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_LORD_PROMPT_3.getMessage());
			        			player.setIsGiveLordConfirmed(true);
			        			// Start a timer to cancel the confirmation
			        			ChatUtil.printDebug("Starting give lord confirmation timer for 60 seconds for player "+bukkitPlayer.getName());
			        			Timer giveLordConfirmTimer = player.getGiveLordConfirmTimer();
			        			giveLordConfirmTimer.stopTimer();
			        			giveLordConfirmTimer.setTime(60);
			        			giveLordConfirmTimer.startTimer();
			        		} else {
			        			// Give lordship
			        			town.setPlayerLord(offlinePlayer.getOfflineBukkitPlayer());
			        			for(OfflinePlayer resident : town.getPlayerResidents()) {
					    			if(resident.isOnline()) {
					    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_LORD_SUCCESS.getMessage(townName,playerName));
					    			}
					    		}
			        			KonPlayer onlinePlayer = getKonquest().getPlayerManager().getPlayerFromName(playerName);
			        			if(onlinePlayer != null) {
					    			getKonquest().getKingdomManager().updatePlayerMembershipStats(onlinePlayer);
					    		}
			        		}
						} else {
							// A non-owner is giving themselves Lord
							if(!offlinePlayer.getOfflineBukkitPlayer().getUniqueId().equals(player.getBukkitPlayer().getUniqueId())) {
		        				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_LORD_SELF.getMessage());
				        		return;
		        			}
		        			town.setPlayerLord(offlinePlayer.getOfflineBukkitPlayer());
		        			town.removeJoinRequest(offlinePlayer.getOfflineBukkitPlayer().getUniqueId());
		        			for(OfflinePlayer resident : town.getPlayerResidents()) {
				    			if(resident.isOnline()) {
				    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_LORD_CLAIM.getMessage(offlinePlayer.getOfflineBukkitPlayer().getName(),townName));
				    			}
				    		}
		        			KonPlayer onlinePlayer = getKonquest().getPlayerManager().getPlayerFromName(playerName);
		        			if(onlinePlayer != null) {
				    			getKonquest().getKingdomManager().updatePlayerMembershipStats(onlinePlayer);
				    		}
						}
				    } else {
				    	ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
		        		return;
				    }
					break;
					
				case "knight":
					// Verify player is lord of the Town
		        	if(!town.isPlayerLord(player.getOfflineBukkitPlayer())) {
		        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
		        		return;
		        	}
		        	if (getArgs().length == 4) {
						playerName = getArgs()[3];
					}
				    if(!playerName.equalsIgnoreCase("")) {
				    	KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayerFromName(playerName);
				    	if(offlinePlayer == null) {
							ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
			        		return;
						}
				    	if(!offlinePlayer.getKingdom().equals(town.getKingdom())) {
				    		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
			        		return;
				    	}
				    	if(offlinePlayer.getOfflineBukkitPlayer().getUniqueId().equals(player.getBukkitPlayer().getUniqueId())) {
	        				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			        		return;
	        			}
				    	// Set resident's elite status
				    	KonPlayer onlinePlayer = getKonquest().getPlayerManager().getPlayerFromName(playerName);
				    	if(town.isPlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
				    		if(town.isPlayerKnight(offlinePlayer.getOfflineBukkitPlayer())) {
				    			// Clear elite
				    			town.setPlayerKnight(offlinePlayer.getOfflineBukkitPlayer(), false);
				    			for(OfflinePlayer resident : town.getPlayerResidents()) {
					    			if(resident.isOnline()) {
					    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_KNIGHT_CLEAR.getMessage(playerName,townName));
					    			}
					    		}
				    		} else {
				    			// Set elite
				    			town.setPlayerKnight(offlinePlayer.getOfflineBukkitPlayer(), true);
				    			for(OfflinePlayer resident : town.getPlayerResidents()) {
					    			if(resident.isOnline()) {
					    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_KNIGHT_SET.getMessage(playerName,townName));
					    			}
					    		}
				    		}
		        			if(onlinePlayer != null) {
				    			getKonquest().getKingdomManager().updatePlayerMembershipStats(onlinePlayer);
				    		}
				    	} else {
				    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_KNIGHT_RESIDENT.getMessage());
				    	}
				    } else {
				    	ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
		        		return;
				    }
				    break;
				    
				case "rename":
					// Verify player is lord of the Town
		        	if(!town.isPlayerLord(player.getOfflineBukkitPlayer())) {
		        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
		        		return;
		        	}
		        	if (getArgs().length == 4) {
						newTownName = getArgs()[3];
					}
		        	if(!newTownName.equalsIgnoreCase("")) {
		        		// Verify enough favor
		            	double cost = getKonquest().getConfigManager().getConfig("core").getDouble("core.favor.cost_rename",0.0);
		    			if(cost > 0) {
		    				if(KonquestPlugin.getBalance(bukkitPlayer) < cost) {
		    					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(cost));
		                        return;
		    				}
		    			}
		    			// Check new name constraints
		    			if(getKonquest().validateName(newTownName,bukkitPlayer) != 0) {
		            		return;
		            	}
		        		// Rename the town
		        		boolean success = getKonquest().getKingdomManager().renameTown(townName, newTownName, town.getKingdom().getName());
		        		if(success) {
		        			for(OfflinePlayer resident : town.getPlayerResidents()) {
				    			if(resident.isOnline()) {
				    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_RENAME.getMessage(bukkitPlayer.getName(),townName,newTownName));
				    			}
				    		}
			        		// Withdraw cost from player
			        		if(cost > 0) {
				                if(KonquestPlugin.withdrawPlayer(bukkitPlayer, cost)) {
				                	getKonquest().getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)cost);
				                }
			        		}
		        		} else {
		        			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
		        		}
		        	} else {
		        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
		        		return;
		        	}
		        	break;
		        	
	        	default:
	        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
				}
        	}
        }
	}

	@Override
	public List<String> tabComplete() {
		// k town <town> [menu|join|leave|add|kick|knight|lord|rename] [<name>]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		Player bukkitPlayer = (Player) getSender();
		KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
		if(getArgs().length == 2) {
			// Suggest all town names in player's kingdom
			for(KonTown town : player.getKingdom().getTowns()) {
				tabList.add(town.getName());
			}
			// Suggest capital by kingdom name
			tabList.add(player.getKingdom().getName());
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 3) {
			// suggest sub-commands
			tabList.add("join");
			tabList.add("leave");
			tabList.add("add");
			tabList.add("kick");
			tabList.add("lord");
			tabList.add("knight");
			tabList.add("rename");
			tabList.add("menu");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			// suggest appropriate arguments
			String subCommand = getArgs()[2];
			String name;
			if(subCommand.equalsIgnoreCase("add") || subCommand.equalsIgnoreCase("kick") || subCommand.equalsIgnoreCase("lord") || subCommand.equalsIgnoreCase("knight")) {
				List<String> playerList = new ArrayList<>();
				for(KonOfflinePlayer offlinePlayer : getKonquest().getPlayerManager().getAllPlayersInKingdom(player.getKingdom())) {
					name = offlinePlayer.getOfflineBukkitPlayer().getName();
					if(name != null) {
						playerList.add(name);
					}
				}
				tabList.addAll(playerList);
			} else if(subCommand.equalsIgnoreCase("rename")) {
				tabList.add("***");
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}

}
