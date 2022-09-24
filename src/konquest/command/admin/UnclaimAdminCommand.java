package konquest.command.admin;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonPlayer;
import konquest.model.KonPlayer.FollowType;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class UnclaimAdminCommand extends CommandBase {
	
	public UnclaimAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	// k admin unclaim [radius|auto] [<r>]
		if (getArgs().length > 4) {
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
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	
        	if(getArgs().length > 1) {
        		String unclaimMode = getArgs()[2];
        		switch(unclaimMode) {
        		case "radius" :
        			if(getArgs().length != 4) {
        				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
        	            return;
        			}
        			
        			final int min = 1;
        			final int max = 16;
    				int radius = Integer.parseInt(getArgs()[3]);
    				if(radius < min || radius > max) {
    					ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
    					ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_UNCLAIM_ERROR_RADIUS.getMessage(min,max));
    					return;
    				}
    				getKonquest().getKingdomManager().unclaimRadiusForAdmin(bukkitPlayer, bukkitPlayer.getLocation(), radius);
        			break;
        			
        		case "auto" :
        			boolean doAuto = false;
        			// Check if player is already in an auto follow state
        			if(player.isAutoFollowActive()) {
        				// Check if player is already in claim state
        				if(player.getAutoFollow().equals(FollowType.ADMIN_UNCLAIM)) {
        					// Disable the auto state
        					ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_DISABLE_AUTO.getMessage());
        					player.setAutoFollow(FollowType.NONE);
        				} else {
        					// Change state
        					doAuto = true;
        				}
        			} else {
        				// Player is not in any auto mode, enter normal unclaim following state
        				doAuto = true;
        			}
        			if(doAuto) {
        				boolean isUnclaimSuccess = getKonquest().getKingdomManager().unclaimForAdmin(bukkitPlayer, bukkitPlayer.getLocation());
        				if(isUnclaimSuccess) {
        					ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_ENABLE_AUTO.getMessage());
            				player.setAutoFollow(FollowType.ADMIN_UNCLAIM);
        				}
        			}
        			break;
        			
        		default :
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
                    return;
        		}
        	} else {
        		// Unclaim the single chunk containing playerLoc for the current territory.
        		getKonquest().getKingdomManager().unclaimForAdmin(bukkitPlayer, bukkitPlayer.getLocation());
        	}
        }
    }
    
    @Override
	public List<String> tabComplete() {
		// k admin unclaim [radius|auto] [<r>]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 3) {
			tabList.add("radius");
			tabList.add("auto");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			// suggest number
			String subCommand = getArgs()[2];
			if(subCommand.equalsIgnoreCase("radius")) {
				tabList.add("#");
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		
		return matchedTabList;
	}
}
