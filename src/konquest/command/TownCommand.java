package konquest.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.model.KonStatsType;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;
import konquest.utility.Timer;
import net.milkbowl.vault.economy.EconomyResponse;

public class TownCommand extends CommandBase {

	public TownCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

	@Override
	public void execute() {
		// k town <name> open|close|add|kick|knight|lord|rename|upgrade|shield [arg]
		if (getArgs().length != 3 && getArgs().length != 4) {
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	String townName = getArgs()[1];
        	String subCmd = getArgs()[2];
        	
        	Player bukkitPlayer = (Player) getSender();
        	if(!getKonquest().getPlayerManager().isPlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
    		KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	
        	// Verify town exists within sender's Kingdom
        	if(!player.getKingdom().hasTown(townName)) {
        		//ChatUtil.sendError((Player) getSender(), "No such Town in your Kingdom");
        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_BAD_NAME.getMessage());
        		return;
        	}
        	KonTown town = player.getKingdom().getTown(townName);
        	
        	// Check if town has a lord
        	if(!town.isLordValid() && !subCmd.equalsIgnoreCase("lord")) {
        		if(town.isPlayerElite(player.getOfflineBukkitPlayer())) {
        			//ChatUtil.sendNotice((Player) getSender(), townName+" is lordless! Use \"/k town <name> lord <your name>\" to claim Lordship.");
        			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_NO_LORD.getMessage(townName,townName,bukkitPlayer.getName()));
        		} else if(town.isPlayerResident(player.getOfflineBukkitPlayer()) && town.getPlayerElites().isEmpty()) {
        			//ChatUtil.sendNotice((Player) getSender(), townName+" is leaderless! Use \"/k town <name> lord <your name>\" to claim Lordship.");
        			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_NO_LORD.getMessage(townName,townName,bukkitPlayer.getName()));
        		} else if(town.getPlayerResidents().isEmpty()){
        			//ChatUtil.sendNotice((Player) getSender(), townName+" is abandoned! Use \"/k town <name> lord <your name>\" to claim Lordship.");
        			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_NO_LORD.getMessage(townName,townName,bukkitPlayer.getName()));
        		}
        	}
        	
        	// Action based on sub-command
        	String playerName = "";
        	String newTownName = "";
        	switch(subCmd.toLowerCase()) {
        	case "open":
        		// Verify player is lord of the Town
            	if(!town.isPlayerLord(player.getOfflineBukkitPlayer())) {
            		//ChatUtil.sendError((Player) getSender(), "You must be the Lord of "+townName+" to do this");
            		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
            		return;
            	}
            	if(town.isOpen()) {
            		//ChatUtil.sendNotice((Player) getSender(), townName+" is already open for access by all players");
            		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_OPEN.getMessage(townName));
            	} else {
            		town.setIsOpen(true);
            		for(OfflinePlayer resident : town.getPlayerResidents()) {
		    			if(resident.isOnline()) {
		    				//ChatUtil.sendNotice((Player) resident, "Opened "+townName+" for access by all players");
		    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_OPEN.getMessage(townName));
		    			}
		    		}
            	}
        		break;
        	case "close":
        		// Verify player is lord of the Town
            	if(!town.isPlayerLord(player.getOfflineBukkitPlayer())) {
            		//ChatUtil.sendError((Player) getSender(), "You must be the Lord of "+townName+" to do this");
            		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
            		return;
            	}
            	if(town.isOpen()) {
            		town.setIsOpen(false);
            		for(OfflinePlayer resident : town.getPlayerResidents()) {
		    			if(resident.isOnline()) {
		    				//ChatUtil.sendNotice((Player) resident, "Closed "+townName+", only residents may build and access containers");
		    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_CLOSE.getMessage(townName));
		    			}
		    		}
            	} else {
            		//ChatUtil.sendNotice((Player) getSender(), townName+" is already closed for residents only");
            		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_CLOSE.getMessage(townName));
            	}
        		break;
			case "add":
				// Verify player is elite or lord of the Town
	        	if(!town.isPlayerLord(player.getOfflineBukkitPlayer()) && !town.isPlayerElite(player.getOfflineBukkitPlayer())) {
	        		//ChatUtil.sendError((Player) getSender(), "You must be a Knight or the Lord of "+townName+" to do this");
	        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
	        		return;
	        	}
				if (getArgs().length == 4) {
					playerName = getArgs()[3];
				}
			    if(!playerName.equalsIgnoreCase("")) {
			    	KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getAllPlayerFromName(playerName);
			    	if(offlinePlayer == null) {
						//ChatUtil.sendError((Player) getSender(), "Invalid player name!");
						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
		        		return;
					}
			    	if(!offlinePlayer.getKingdom().equals(town.getKingdom())) {
			    		//ChatUtil.sendError((Player) getSender(), "That player is an enemy!");
			    		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
		        		return;
			    	}
			    	playerName = offlinePlayer.getOfflineBukkitPlayer().getName();
			    	/*
			    	long offlineTimeout = getKonquest().getOfflineTimeoutSeconds();
			    	long lastPlayedTime = offlinePlayer.getOfflineBukkitPlayer().getLastPlayed();
			    	Date now = new Date();
			    	if(offlineTimeout != 0 && now.after(new Date(lastPlayedTime + (offlineTimeout*1000)))) {
			    		ChatUtil.sendError((Player) getSender(), "That player is not active!");
		        		return;
			    	}*/
			    	if(town.isPlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
			    		//ChatUtil.sendError((Player) getSender(), playerName+" is already a resident of "+townName);
			    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_ADD_RESIDENT.getMessage(playerName,townName));
			    		return;
			    	}
			    	// Check for join request and add as resident
			    	// Otherwise create join invite
			    	UUID id = offlinePlayer.getOfflineBukkitPlayer().getUniqueId();
			    	KonPlayer onlinePlayer = getKonquest().getPlayerManager().getPlayerFromName(playerName);
			    	if(town.isJoinInviteValid(id)) {
			    		// There is already a join invite for this player
			    		//ChatUtil.sendError((Player) getSender(), playerName+" has already been invited to join "+townName);
			    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_ADD_INVITE_RESIDENT.getMessage(playerName,townName));
			    		if(onlinePlayer != null) {
			    			//ChatUtil.sendNotice(onlinePlayer.getBukkitPlayer(), "You have been invited to join the town of "+townName);
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
					    				//ChatUtil.sendNotice((Player) resident, "Added "+playerName+" as a resident of "+townName);
					    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_ADD_SUCCESS.getMessage(playerName,townName));
					    			}
					    		}
					    		if(onlinePlayer != null) {
					    			getKonquest().getKingdomManager().updatePlayerMembershipStats(onlinePlayer);
					    		}
					    	}
			    		} else {
			    			// Create a new join invite
			    			//ChatUtil.sendNotice((Player) getSender(), "Sending an invite to "+playerName+" to join "+townName);
			    			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_ADD_INVITE.getMessage(playerName,townName));
			    			town.addJoinRequest(id, true);
			    			if(onlinePlayer != null) {
				    			//ChatUtil.sendNotice(onlinePlayer.getBukkitPlayer(), "You have been invited to join the town of "+townName);
				    			//ChatUtil.sendNotice(onlinePlayer.getBukkitPlayer(), "You're invited to join the town of "+town.getName()+", use \"/k join "+town.getName()+"\"", ChatColor.LIGHT_PURPLE);
				    			//ChatUtil.sendNotice(onlinePlayer.getBukkitPlayer(), "You're invited to join the town of "+town.getName()+", use \"/k join "+town.getName()+"\" to accept or \"/k leave "+town.getName()+"\" to decline", ChatColor.LIGHT_PURPLE);
				    			ChatUtil.sendNotice(onlinePlayer.getBukkitPlayer(), MessagePath.COMMAND_TOWN_NOTICE_ADD_INVITE_PLAYER.getMessage(town.getName(),town.getName(),town.getName()));
				    		}
			    		}
			    	}
			    } else {
			    	//ChatUtil.sendError((Player) getSender(), "Must provide a player name!");
			    	ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	        		return;
			    }
			    break;
			case "kick":
				// Verify player is elite or lord of the Town
	        	if(!town.isPlayerLord(player.getOfflineBukkitPlayer()) && !town.isPlayerElite(player.getOfflineBukkitPlayer())) {
	        		//ChatUtil.sendError((Player) getSender(), "You must be a Knight or the Lord of "+townName+" to do this");
	        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
	        		return;
	        	}
				if (getArgs().length == 4) {
					playerName = getArgs()[3];
				}
				if(!playerName.equalsIgnoreCase("")) {
					KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getAllPlayerFromName(playerName);
					if(offlinePlayer == null) {
						//ChatUtil.sendError((Player) getSender(), "Invalid player name!");
						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
		        		return;
					}
					playerName = offlinePlayer.getOfflineBukkitPlayer().getName();
					// Prevent Elites from removing Lord
					if(town.isPlayerElite(player.getOfflineBukkitPlayer()) && town.isPlayerLord(offlinePlayer.getOfflineBukkitPlayer())) {
						//ChatUtil.sendError((Player) getSender(), "You cannot kick the Lord!");
						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
		        		return;
					}
					// Check for active invite and remove it,
					// otherwise deny join request
					UUID id = offlinePlayer.getOfflineBukkitPlayer().getUniqueId();
					KonPlayer onlinePlayer = getKonquest().getPlayerManager().getPlayerFromName(playerName);
			    	if(town.isJoinInviteValid(id)) {
			    		// There is a join invite for this player, revoke it
			    		//ChatUtil.sendNotice((Player) getSender(), "Revoked join invite of "+playerName+" for "+townName);
			    		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_KICK_REMOVE.getMessage(playerName,townName));
			    		town.removeJoinRequest(id);
			    	} else {
			    		// Check for join request
			    		if(town.isJoinRequestValid(id)) {
			    			// There is a join request for this player, deny it
			    			//ChatUtil.sendNotice((Player) getSender(), "Denied join request of "+playerName+" for "+townName);
			    			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_KICK_REMOVE.getMessage(playerName,townName));
				    		town.removeJoinRequest(id);
			    		} else {
			    			// Attempt to kick current resident
			    			// Remove the player as a resident
					    	if(town.removePlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
					    		for(OfflinePlayer resident : town.getPlayerResidents()) {
					    			if(resident.isOnline()) {
					    				//ChatUtil.sendNotice((Player) resident, "Kicked resident "+playerName+" from "+townName);
					    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_KICK_RESIDENT.getMessage(playerName,townName));
					    			}
					    		}
					    		if(onlinePlayer != null) {
					    			getKonquest().getKingdomManager().updatePlayerMembershipStats(onlinePlayer);
					    		}
					    	} else {
					    		//ChatUtil.sendError((Player) getSender(), playerName+" is not a resident of "+townName);
					    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_KICK_FAIL.getMessage(playerName,townName));
					    	}
			    		}
			    	}
			    } else {
			    	//ChatUtil.sendError((Player) getSender(), "Must provide a player name!");
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
		        		//ChatUtil.sendError((Player) getSender(), "You must be the Lord of "+townName+" to do this");
		        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
		        		return;
		        	}
	        	} else {
	        		// Lord does not exist
	        		if(!town.isOpen() && town.getKingdom().equals(player.getKingdom())) {
	        			// Check player roles in closed towns only, allow any member to claim lordship in open towns
		        		if(!town.getPlayerElites().isEmpty()) {
		        			// Elite residents exist, permit access to them
		        			if(!town.isPlayerElite(player.getOfflineBukkitPlayer())) {
		    	        		//ChatUtil.sendError((Player) getSender(), "You must be a Knight in "+townName+" to do this");
		    	        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
		    	        		return;
		    	        	}
		        		} else {
		        			// No Elite residents exist
		        			if(!town.getPlayerResidents().isEmpty()) {
		        				// Residents for this town exist, permit access to them
		        				if(!town.isPlayerResident(player.getOfflineBukkitPlayer())) {
			    	        		//ChatUtil.sendError((Player) getSender(), "You must be a Resident of "+townName+" to do this");
			    	        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			    	        		return;
			    	        	}
		        			} else {
		        				// No residents exist, permit access to any Kingdom member
		        				if(!town.getKingdom().equals(player.getKingdom())) {
			    	        		//ChatUtil.sendError((Player) getSender(), "You are an enemy of "+townName);
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
					if(isSenderOwner) {
						// The current Town owner is giving away Lordship
						KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getAllPlayerFromName(playerName);
	        			if(offlinePlayer == null) {
							//ChatUtil.sendError((Player) getSender(), "Invalid player name!");
							ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
			        		return;
						}
	        			if(!offlinePlayer.getKingdom().equals(town.getKingdom())) {
				    		//ChatUtil.sendError((Player) getSender(), "That player is an enemy!");
				    		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
			        		return;
				    	}
	        			if(offlinePlayer.getOfflineBukkitPlayer().getUniqueId().equals(player.getBukkitPlayer().getUniqueId())) {
	        				//ChatUtil.sendError((Player) getSender(), "You are already the Lord of "+townName+"!");
	        				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			        		return;
	        			}
						// Confirm with the player
		        		if(!player.isGiveLordConfirmed()) {
		        			// Prompt the player to confirm
		        			//ChatUtil.sendNotice((Player) getSender(), "Are you sure you want to give Lordship of "+townName+"?");
		        			//ChatUtil.sendNotice((Player) getSender(), "You will no longer own this town and be made a Knight resident.",ChatColor.DARK_RED);
		        			//ChatUtil.sendNotice((Player) getSender(), "Use this command again to confirm.",ChatColor.DARK_RED);
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
				    				//ChatUtil.sendNotice((Player) resident, "Gave Lordship of "+townName+" to "+playerName);
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
						KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getAllPlayerFromName(playerName);
	        			if(offlinePlayer == null) {
							//ChatUtil.sendError((Player) getSender(), "Invalid player name!");
							ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
			        		return;
						}
	        			if(!offlinePlayer.getOfflineBukkitPlayer().getUniqueId().equals(player.getBukkitPlayer().getUniqueId())) {
	        				//ChatUtil.sendError((Player) getSender(), "You may only claim Lordship for yourself!");
	        				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_LORD_SELF.getMessage());
			        		return;
	        			}
	        			town.setPlayerLord(offlinePlayer.getOfflineBukkitPlayer());
	        			town.removeJoinRequest(offlinePlayer.getOfflineBukkitPlayer().getUniqueId());
	        			for(OfflinePlayer resident : town.getPlayerResidents()) {
			    			if(resident.isOnline()) {
			    				//ChatUtil.sendNotice((Player) resident, offlinePlayer.getOfflineBukkitPlayer().getName()+" has claimed Lordship of "+townName+"!");
			    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_LORD_CLAIM.getMessage(offlinePlayer.getOfflineBukkitPlayer().getName(),townName));
			    			}
			    		}
	        			KonPlayer onlinePlayer = getKonquest().getPlayerManager().getPlayerFromName(playerName);
	        			if(onlinePlayer != null) {
			    			getKonquest().getKingdomManager().updatePlayerMembershipStats(onlinePlayer);
			    		}
					}
			    } else {
			    	//ChatUtil.sendError((Player) getSender(), "Must provide a player name!");
			    	ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	        		return;
			    }
				break;
			case "knight":
				// Verify player is lord of the Town
	        	if(!town.isPlayerLord(player.getOfflineBukkitPlayer())) {
	        		//ChatUtil.sendError((Player) getSender(), "You must be the Lord of "+townName+" to do this");
	        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
	        		return;
	        	}
	        	if (getArgs().length == 4) {
					playerName = getArgs()[3];
				}
			    if(!playerName.equalsIgnoreCase("")) {
			    	KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getAllPlayerFromName(playerName);
			    	if(offlinePlayer == null) {
						//ChatUtil.sendError((Player) getSender(), "Invalid player name!");
						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
		        		return;
					}
			    	if(!offlinePlayer.getKingdom().equals(town.getKingdom())) {
			    		//ChatUtil.sendError((Player) getSender(), "That player is an enemy!");
			    		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
		        		return;
			    	}
			    	if(offlinePlayer.getOfflineBukkitPlayer().getUniqueId().equals(player.getBukkitPlayer().getUniqueId())) {
        				//ChatUtil.sendError((Player) getSender(), "You cannot change your Knighthood in "+townName+"!");
        				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
		        		return;
        			}
			    	// Set resident's elite status
			    	KonPlayer onlinePlayer = getKonquest().getPlayerManager().getPlayerFromName(playerName);
			    	if(town.isPlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
			    		if(town.isPlayerElite(offlinePlayer.getOfflineBukkitPlayer())) {
			    			// Clear elite
			    			town.setPlayerElite(offlinePlayer.getOfflineBukkitPlayer(), false);
			    			for(OfflinePlayer resident : town.getPlayerResidents()) {
				    			if(resident.isOnline()) {
				    				//ChatUtil.sendNotice((Player) resident, playerName+" is no longer a Knight in "+townName);
				    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_KNIGHT_CLEAR.getMessage(playerName,townName));
				    			}
				    		}
			    			//ChatUtil.sendNotice((Player) getSender(), "Use this command again to set Knight status.");
			    		} else {
			    			// Set elite
			    			town.setPlayerElite(offlinePlayer.getOfflineBukkitPlayer(), true);
			    			for(OfflinePlayer resident : town.getPlayerResidents()) {
				    			if(resident.isOnline()) {
				    				//ChatUtil.sendNotice((Player) resident, playerName+" has been Knighted in "+townName);
				    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_KNIGHT_SET.getMessage(playerName,townName));
				    			}
				    		}
			    			//ChatUtil.sendNotice((Player) getSender(), "Use this command again to remove Knight status.");
			    		}
	        			if(onlinePlayer != null) {
			    			getKonquest().getKingdomManager().updatePlayerMembershipStats(onlinePlayer);
			    		}
			    	} else {
			    		//ChatUtil.sendError((Player) getSender(), "Knights must be added as residents first!");
			    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_KNIGHT_RESIDENT.getMessage());
			    	}
			    } else {
			    	//ChatUtil.sendError((Player) getSender(), "Must provide a player name!");
			    	ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	        		return;
			    }
			    break;
			case "rename":
				// Verify player is lord of the Town
	        	if(!town.isPlayerLord(player.getOfflineBukkitPlayer())) {
	        		//ChatUtil.sendError((Player) getSender(), "You must be the Lord of "+townName+" to do this");
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
	    					//ChatUtil.sendError(bukkitPlayer, "Not enough Favor, need "+cost);
	    					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(cost));
	                        return;
	    				}
	    			}
	        		// Rename the town
	        		//boolean success = player.getKingdom().renameTown(townName, newTownName);
	        		boolean success = getKonquest().getKingdomManager().renameTown(townName, newTownName, town.getKingdom().getName());
	        		if(success) {
	        			for(OfflinePlayer resident : town.getPlayerResidents()) {
			    			if(resident.isOnline()) {
			    				//ChatUtil.sendNotice((Player) resident, "Lord "+bukkitPlayer.getName()+" has renamed the Town of "+townName+" to "+newTownName);
			    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_RENAME.getMessage(bukkitPlayer.getName(),townName,newTownName));
			    			}
			    		}
		        		// Withdraw cost from player
		        		if(cost > 0) {
			        		EconomyResponse r = KonquestPlugin.withdrawPlayer(bukkitPlayer, cost);
			                if(r.transactionSuccess()) {
			                	String balanceF = String.format("%.2f",r.balance);
				            	String amountF = String.format("%.2f",r.amount);
				            	//ChatUtil.sendNotice((Player) getSender(), "Favor reduced by "+amountF+", total: "+balanceF);
				            	ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_REDUCE_FAVOR.getMessage(amountF,balanceF));
			                	getKonquest().getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)cost);
			                } else {
			                	//ChatUtil.sendError((Player) getSender(), String.format("An error occured: %s", r.errorMessage));
			                	ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(r.errorMessage));
			                }
		        		}
	        		}
	        	} else {
	        		//ChatUtil.sendError((Player) getSender(), "Must provide a new Town name!");
	        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	        		return;
	        	}
	        	break;
			case "upgrade":
        		// Verify player is lord of the Town
            	if(!town.isPlayerLord(player.getOfflineBukkitPlayer())) {
            		//ChatUtil.sendError((Player) getSender(), "You must be the Lord of "+townName+" to do this");
            		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
            		return;
            	}
            	getKonquest().getDisplayManager().displayTownUpgradeMenu(bukkitPlayer, town);
        		break;
			case "shield":
            	// Verify player is elite or lord of the Town
	        	if(!town.isPlayerLord(player.getOfflineBukkitPlayer()) && !town.isPlayerElite(player.getOfflineBukkitPlayer())) {
	        		//ChatUtil.sendError((Player) getSender(), "You must be a Knight or the Lord of "+townName+" to do this");
	        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
	        		return;
	        	}
            	// Verify either shields or armors are enabled
            	boolean isShieldsEnabled = getKonquest().getShieldManager().isShieldsEnabled();
            	boolean isArmorsEnabled = getKonquest().getShieldManager().isArmorsEnabled();
            	if(!isShieldsEnabled && !isArmorsEnabled) {
            		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
            		return;
            	}
            	getKonquest().getDisplayManager().displayTownShieldMenu(bukkitPlayer, town);
				break;
        	default:
        		//ChatUtil.sendError((Player) getSender(), "Invalid sub-command, expected open|close|add|kick|knight|lord|rename|upgrade");
        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
        		return;
        	}
        }
	}

	@Override
	public List<String> tabComplete() {
		// k town <name> open|close|add|kick|knight|lord|rename|upgrade|shield [arg]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		Player bukkitPlayer = (Player) getSender();
		KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
		if(getArgs().length == 2) {
			// Suggest town names where the player is elite or if lord is not set
			for(KonTown town : player.getKingdom().getTowns()) {
				if(town.isPlayerElite(bukkitPlayer) || !town.isLordValid()) {
					tabList.add(town.getName());
				}
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 3) {
			// suggest sub-commands
			tabList.add("open");
			tabList.add("close");
			tabList.add("add");
			tabList.add("kick");
			tabList.add("lord");
			tabList.add("knight");
			tabList.add("rename");
			tabList.add("upgrade");
			tabList.add("shield");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			// suggest appropriate arguments
			String subCommand = getArgs()[2];
			if(subCommand.equalsIgnoreCase("add") || subCommand.equalsIgnoreCase("kick") || subCommand.equalsIgnoreCase("lord") || subCommand.equalsIgnoreCase("knight")) {
				List<String> playerList = new ArrayList<>();
				for(KonOfflinePlayer offlinePlayer : getKonquest().getPlayerManager().getAllPlayersInKingdom(player.getKingdom())) {
					playerList.add(offlinePlayer.getOfflineBukkitPlayer().getName());
				}
				tabList.addAll(playerList);
				// Trim down completion options based on current input
				StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
				Collections.sort(matchedTabList);
			}
		}
		return matchedTabList;
	}

}
