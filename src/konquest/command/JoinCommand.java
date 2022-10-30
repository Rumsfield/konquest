package konquest.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import konquest.Konquest;
import konquest.model.KonPlayer;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class JoinCommand extends CommandBase {

	public JoinCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k join [kingdomName|townName]
		if (getArgs().length != 1 && getArgs().length != 2) {
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
		}
		
		Player bukkitPlayer = (Player) getSender();
		if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
			ChatUtil.printDebug("Failed to find non-existent player");
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return;
		}
		KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
		String joinName = "";
    	if (getArgs().length == 1) { // No name given, join smallest Kingdom or exile Kingdom
    		if(player.getExileKingdom().equals(getKonquest().getKingdomManager().getBarbarians())) {
    			joinName = getKonquest().getKingdomManager().getSmallestKingdomName();
    		} else {
    			joinName = player.getExileKingdom().getName();
    		}
        } else if (getArgs().length == 2){ // Use the given name
        	joinName = getArgs()[1];
        }
    	
    	if(getKonquest().getKingdomManager().getKingdoms().isEmpty()) {
    		//ChatUtil.sendError((Player) getSender(), "There are no Kingdoms. Create one with \"/k admin makekingdom <name>\"");
    		ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_JOIN_ERROR_NO_KINGDOMS.getMessage());
    		return;
    	}
    	
    	if(getKonquest().getKingdomManager().isKingdom(joinName)) {
    		// Check for cooldown
    		if(getKonquest().getKingdomManager().isPlayerJoinCooldown(bukkitPlayer)) {
    			int remainingCooldown = getKonquest().getKingdomManager().getJoinCooldownRemainingSeconds(bukkitPlayer);
				String cooldownTimeStr = Konquest.getTimeFormat(remainingCooldown, ChatColor.AQUA);
    			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_JOIN_ERROR_KINGDOM_COOLDOWN.getMessage(cooldownTimeStr));
    			return;
    		}
    		
    		// Attempt to join a Kingdom
    		joinName = getKonquest().getKingdomManager().getKingdom(joinName).getName();
    		boolean isAllowSwitch = getKonquest().getConfigManager().getConfig("core").getBoolean("core.kingdoms.allow_exile_switch",false);
    		// Must be a barbarian to join a kingdom
    		if(player.isBarbarian()) {
    			if(getKonquest().getKingdomManager().getKingdom(joinName).equals(player.getExileKingdom()) ||
    					getKonquest().getKingdomManager().getBarbarians().equals(player.getExileKingdom()) ||
    					isAllowSwitch) {
    				// Allow barbarians to join any kingdom when they're new (exile kingdom = barbarians) or if switching is allowed in config
    				// Allow barbarians to join their exiled kingdoms
        			int status = getKonquest().getKingdomManager().assignPlayerKingdom(bukkitPlayer.getUniqueId(), joinName, false);
        			switch(status) {
	        			case 0:
	        				ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_JOIN_NOTICE_KINGDOM_JOIN.getMessage(ChatColor.AQUA+joinName));
	        				ChatUtil.sendBroadcast(ChatColor.LIGHT_PURPLE+MessagePath.COMMAND_JOIN_BROADCAST_PLAYER_JOIN.getMessage(bukkitPlayer.getName(),ChatColor.AQUA+joinName));
	        				// Apply cooldown
	        				getKonquest().getKingdomManager().applyPlayerJoinCooldown(bukkitPlayer);
	        				break;
	        			case 1:
	        				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_BAD_NAME.getMessage(joinName));
	        				break;
	        			case 2:
	        				ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_JOIN_ERROR_KINGDOM_LIMIT.getMessage(joinName));
	        				break;
	        			case 3:
	        				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage());
	        				break;
	        			case 4:
	        				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_FAILED.getMessage());
	        				break;
        				default:
        					ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
        					break;
        			}
        		} else {
        			//ChatUtil.sendError((Player) getSender(), "You cannot join that Kingdom. Ask an admin to move you.");
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_JOIN_ERROR_KINGDOM_DENIED.getMessage());
        		}
        	} else if(getKonquest().getKingdomManager().getKingdom(joinName).equals(player.getKingdom())) {
        		//ChatUtil.sendError((Player) getSender(), "You are already a member.");
        		ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_JOIN_ERROR_KINGDOM_MEMBER.getMessage());
        	} else {
        		if(isAllowSwitch) {
        			//ChatUtil.sendError((Player) getSender(), "Cannot switch Kingdom unless you are exiled. Type \"/k exile\"");
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_JOIN_ERROR_KINGDOM_EXILE.getMessage());
        		} else {
        			//ChatUtil.sendError((Player) getSender(), "Cannot join a rival Kingdom. Ask an admin to move you.");
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_JOIN_ERROR_KINGDOM_DENIED.getMessage());
        		}
        	}
    	} else if(player.getKingdom().hasTown(joinName)) {
    		// Attempt to join a Town
    		joinName = player.getKingdom().getTown(joinName).getName();
    		if(!player.isBarbarian()) {
    			KonTown town = player.getKingdom().getTown(joinName);
    			if(town.isPlayerResident(bukkitPlayer)) {
    				//ChatUtil.sendError(bukkitPlayer, "You are already a resident of "+joinName);
    				ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_JOIN_ERROR_TOWN_MEMBER.getMessage(joinName));
    				return;
    			}
    			UUID id = bukkitPlayer.getUniqueId();
    			if(town.isOpen() || town.isJoinInviteValid(id)) {
    				// There is an existing invite to join, or the town is open, add the player as a resident
    				town.removeJoinRequest(id);
	    			// Add the player as a resident
			    	if(town.addPlayerResident(bukkitPlayer,false)) {
			    		for(OfflinePlayer resident : town.getPlayerResidents()) {
			    			if(resident.isOnline()) {
			    				//ChatUtil.sendNotice((Player) resident, bukkitPlayer.getName()+" has joined as a resident of "+joinName);
			    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_JOIN_NOTICE_TOWN_RESIDENT.getMessage(bukkitPlayer.getName(),joinName));
			    			}
			    		}
			    		getKonquest().getKingdomManager().updatePlayerMembershipStats(player);
			    	}
    			} else {
    				if(town.isJoinRequestValid(id)) {
    					// There is already an existing join request
    					//ChatUtil.sendError(bukkitPlayer, "You have already requested to join "+joinName);
    					ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_JOIN_ERROR_TOWN_REQUEST.getMessage(joinName));
    				} else {
    					if(town.getNumResidents() == 0) {
    						// The town is empty, make the player into the lord
    						town.setPlayerLord((OfflinePlayer)bukkitPlayer);
    						town.removeJoinRequest(id);
    			    		getKonquest().getKingdomManager().updatePlayerMembershipStats(player);
    			    		//ChatUtil.sendNotice(bukkitPlayer, "You have joined as the lord of "+joinName+".");
    			    		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_JOIN_NOTICE_TOWN_JOIN_LORD.getMessage(joinName));
    					} else {
    						// Create a new join request
        					//ChatUtil.sendNotice(bukkitPlayer, "Sending a request to join "+joinName+". Wait for the Lord or Knights to add you.");
        					ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_JOIN_NOTICE_TOWN_REQUEST.getMessage(joinName));
        					town.addJoinRequest(id, false);
        					town.notifyJoinRequest(id);
    					}
    				}
    			}
    		} else {
    			//ChatUtil.sendError((Player) getSender(), "You cannot join a town as a barbarian.");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
    		}
    	} else {
    		// Could not find what to join
    		//ChatUtil.sendError((Player) getSender(), "Failed to join \""+joinName+"\", maybe bad spelling or enemy.");
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_BAD_NAME.getMessage(joinName));
    	}
	}
	
	@Override
	public List<String> tabComplete() {
		// k join [kingdomName|townName]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		Player bukkitPlayer = (Player) getSender();
		KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
		if(getArgs().length == 2) {
			List<String> joinList = new ArrayList<String>();
			if(player.isBarbarian()) {
				/*if(player.getExileKingdom().equals(getKonquest().getKingdomManager().getBarbarians())) {
					// Player is a new barbarian and can join any kingdom
					joinList.addAll(getKonquest().getKingdomManager().getKingdomNames());
				} else {
					// Player is an exiled barbarian and can only join exile kingdom
					joinList.add(player.getExileKingdom().getName());
				}*/
				joinList.addAll(getKonquest().getKingdomManager().getKingdomNames());
			} else {
				// Only join options are towns in player's kingdom
				for(KonTown town : player.getKingdom().getTowns()) {
					if(!town.isPlayerResident(bukkitPlayer)) {
						joinList.add(town.getName());
					}
				}
			}
			tabList.addAll(joinList);
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
	
	
	/*
	public void execute() {
		// k join [kingdomName|townName]
		if (getArgs().length != 1 && getArgs().length != 2) {
			ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
            return;
		}
		
		Player bukkitPlayer = (Player) getSender();
		KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
		String kingdomName = "";
    	if (getArgs().length == 1) { // No Kingdom name given, join smallest Kingdom or exile Kingdom
    		if(player.getExileKingdom().equals(getKonquest().getKingdomManager().getBarbarians())) {
    			kingdomName = getKonquest().getKingdomManager().getSmallestKingdomName();
    		} else {
    			kingdomName = player.getExileKingdom().getName();
    		}
        } else if (getArgs().length == 2){
        	kingdomName = getArgs()[1];
        }
    	
    	if(!getKonquest().getKingdomManager().isKingdom(kingdomName)) {
    		ChatUtil.sendError((Player) getSender(), "That Kingdom does not exist");
            return;
    	}
    	kingdomName = getKonquest().getKingdomManager().getKingdom(kingdomName).getName();

    	if(player.isBarbarian()) {
    		if(getKonquest().getKingdomManager().getKingdom(kingdomName).equals(player.getExileKingdom())) {
    			// Allow barbarians to join their exiled kingdoms
    			getKonquest().getKingdomManager().assignPlayerKingdom(player, kingdomName);
            	ChatUtil.sendNotice((Player) getSender(), "Returned from Exile to Kingdom "+ChatColor.AQUA+kingdomName);
    		} else if(getKonquest().getKingdomManager().getBarbarians().equals(player.getExileKingdom())) {
    			// Allow barbarians to join any kingdom when they're new (exile kingdom = barbarians)
    			int status = getKonquest().getKingdomManager().assignPlayerKingdom(player, kingdomName);
    			if(status == 0) {
    				ChatUtil.sendNotice((Player) getSender(), "Joined Kingdom "+ChatColor.AQUA+kingdomName);
                	ChatUtil.sendBroadcast(ChatColor.LIGHT_PURPLE+bukkitPlayer.getName()+" has joined Kingdom "+ChatColor.AQUA+kingdomName);
    			} else if(status == 2) {
    				ChatUtil.sendNotice((Player) getSender(), ChatColor.RED+"The Kingdom "+ChatColor.AQUA+kingdomName+ChatColor.RED+" has too many players! Use \"/k join\" to join the smallest Kingdom");
    			}
    		} else {
    			ChatUtil.sendError((Player) getSender(), "You may only return to your original Kingdom. Ask an admin to move you.");
    		}
    	} else if(getKonquest().getKingdomManager().getKingdom(kingdomName).equals(player.getKingdom())) {
    		ChatUtil.sendError((Player) getSender(), "You are already a member.");
    	} else {
    		ChatUtil.sendError((Player) getSender(), "Cannot join a rival Kingdom. Ask an admin to move you.");
    	}
        	

	}
	*/
}
