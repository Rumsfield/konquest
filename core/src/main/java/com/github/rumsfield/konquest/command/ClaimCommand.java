package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonPlayer.FollowType;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class ClaimCommand extends CommandBase {

	public ClaimCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k claim [radius|auto] [<r>]
    	if (getArgs().length > 3) {
            ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	World bukkitWorld = bukkitPlayer.getWorld();
        	// Verify that this command is being used in the default world
        	if(!getKonquest().isWorldValid(bukkitWorld)) {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
                return;
        	}
        	// Verify no Barbarians are using this command
        	if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	if(player.isBarbarian()) {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
                return;
        	}

        	if(getArgs().length > 1) {
        		String claimMode = getArgs()[1];
        		switch(claimMode) {
        		case "radius" :
        			if(getArgs().length != 3) {
        				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
        	            return;
        			}
        			final int min = 1;
        			final int max = 5;
    				int radius = Integer.parseInt(getArgs()[2]);
    				if(radius < min || radius > max) {
    					ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
    					//ChatUtil.sendError((Player) getSender(), "Radius must be greater than 0 and less than or equal to 5.");
    					ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_CLAIM_ERROR_RADIUS.getMessage(min,max));
    					return;
    				}
    				getKonquest().getTerritoryManager().claimRadiusForPlayer(bukkitPlayer, bukkitPlayer.getLocation(), radius);
        			break;
        			
        		case "auto" :
        			boolean doAuto = false;
        			// Check if player is already in an auto follow state
        			if(player.isAutoFollowActive()) {
        				// Check if player is already in claim state
        				if(player.getAutoFollow().equals(FollowType.CLAIM)) {
        					// Disable the auto state
        					ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_DISABLE_AUTO.getMessage());
        					player.setAutoFollow(FollowType.NONE);
        				} else {
        					// Change state
        					doAuto = true;
        				}
        			} else {
        				// Player is not in any auto mode, enter normal claim following state
        				doAuto = true;
        			}
        			if(doAuto) {
        				boolean isClaimSuccess = getKonquest().getTerritoryManager().claimForPlayer(bukkitPlayer, bukkitPlayer.getLocation());
        				if(isClaimSuccess) {
        					ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_ENABLE_AUTO.getMessage());
            				player.setAutoFollow(FollowType.CLAIM);
        				}
        			}
        			break;
        			
        		default :
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
                    return;
        		}
        	} else {
        		// Claim the single chunk containing playerLoc for the adjacent territory.
        		getKonquest().getTerritoryManager().claimForPlayer(bukkitPlayer, bukkitPlayer.getLocation());
        	}
        }
	}
	
	@Override
	public List<String> tabComplete() {
		// k claim [radius|auto] [<r>]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 2) {
			tabList.add("radius");
			tabList.add("auto");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 3) {
			// suggest number
			String subCommand = getArgs()[1];
			if(subCommand.equalsIgnoreCase("radius")) {
				tabList.add("#");
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		
		return matchedTabList;
	}
}