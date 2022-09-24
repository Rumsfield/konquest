package konquest.command.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonPlayer;
import konquest.model.KonPlayer.FollowType;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class ClaimAdminCommand extends CommandBase {
	
	public ClaimAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	// k admin claim [radius|auto|undo] [<r>]
    	if (getArgs().length > 4) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	Location playerLoc = bukkitPlayer.getLocation();
        	//World playerWorld = playerLoc.getWorld();
        	if(getArgs().length > 2) {
        		String claimMode = getArgs()[2];
        		switch(claimMode) {
        		case "radius" :
        			if(getArgs().length != 4) {
        				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
        	            return;
        			}
    				int radius = Integer.parseInt(getArgs()[3]);
    				final int min = 1;
        			final int max = 16;
    				if(radius < min || radius > max) {
    					ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
    					//ChatUtil.sendError((Player) getSender(), "Radius must be greater than 0 and less than or equal to 16.");
    					ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_CLAIM_ERROR_RADIUS.getMessage(min,max));
    					return;
    				}
    				getKonquest().getKingdomManager().claimRadiusForAdmin(bukkitPlayer, bukkitPlayer.getLocation(), radius);
        			break;
        			
        		case "auto" :
        			boolean doAuto = false;
        			// Check if player is already in an auto follow state
        			if(player.isAutoFollowActive()) {
        				// Check if player is already in claim state
        				if(player.getAutoFollow().equals(FollowType.ADMIN_CLAIM)) {
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
        				getKonquest().getKingdomManager().claimForAdmin(bukkitPlayer, playerLoc);
        				ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_ENABLE_AUTO.getMessage());
        				player.setAutoFollow(FollowType.ADMIN_CLAIM);
        			}
        			break;
        			
        		case "undo" :
        			boolean isUndoSuccess = getKonquest().getKingdomManager().claimUndoForAdmin(player);
        			if(isUndoSuccess) {
        				ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
        			} else {
        				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_FAILED.getMessage());
        			}
        			break;
        			
        		default :
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
                    return;
        		}
        	} else {
        		// Claim the single chunk containing playerLoc for the adjacent territory.
        		getKonquest().getKingdomManager().claimForAdmin(bukkitPlayer, playerLoc);
        	}
        }
    }
    
    @Override
	public List<String> tabComplete() {
		// k admin claim [radius|auto|undo] [<r>]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 3) {
			tabList.add("radius");
			tabList.add("auto");
			tabList.add("undo");
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
