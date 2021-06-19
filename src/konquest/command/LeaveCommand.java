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
import konquest.model.KonPlayer;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class LeaveCommand extends CommandBase {

	public LeaveCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k leave townName
		if (getArgs().length != 2) {
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
		}
		
		Player bukkitPlayer = (Player) getSender();
		if(!getKonquest().getPlayerManager().isPlayer(bukkitPlayer)) {
			ChatUtil.printDebug("Failed to find non-existent player");
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return;
		}
		KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
		String leaveName = getArgs()[1];
    	
		if(player.getKingdom().hasTown(leaveName)) {
			// Attempt to leave a Town
			leaveName = player.getKingdom().getTown(leaveName).getName();
			KonTown town = player.getKingdom().getTown(leaveName);
			UUID id = bukkitPlayer.getUniqueId();
			if(town.isPlayerResident(bukkitPlayer)) {
				// Player is resident trying to leave
				// Remove the player as a resident
		    	if(town.removePlayerResident(bukkitPlayer)) {
		    		for(OfflinePlayer resident : town.getPlayerResidents()) {
		    			if(resident.isOnline()) {
		    				//ChatUtil.sendNotice((Player) resident, bukkitPlayer.getName()+" has left the town of "+leaveName);
		    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_LEAVE_NOTICE_TOWN_RESIDENT.getMessage(bukkitPlayer.getName(),leaveName));
		    			}
		    		}
		    		//ChatUtil.sendNotice(bukkitPlayer, "You have left the town of "+leaveName);
		    		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_LEAVE_NOTICE_PLAYER.getMessage(leaveName));
		    		getKonquest().getKingdomManager().updatePlayerMembershipStats(player);
		    	} else {
		    		//ChatUtil.sendError((Player) getSender(), "Failed to remove you from the town of "+leaveName);
		    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_LEAVE_ERROR_NO_RESIDENT.getMessage(leaveName));
		    	}
			} else {
				// Player is not resident, try to decline join invite
				if(town.isJoinInviteValid(id)) {
					// There is an existing invite to join, remove the player from the invites list
					town.removeJoinRequest(id);
					//ChatUtil.sendNotice(bukkitPlayer, "Invite to join "+leaveName+" declined.");
					ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_LEAVE_NOTICE_INVITE_DECLINE.getMessage(leaveName));
				} else {
					//ChatUtil.sendError(bukkitPlayer, "You are not a resident of "+leaveName);
					ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_LEAVE_ERROR_NO_RESIDENT.getMessage(leaveName));
				}
			}
		} else {
    		// Could not find what to leave
    		//ChatUtil.sendError((Player) getSender(), "Failed to leave \""+leaveName+"\", maybe bad spelling or enemy.");
    		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_BAD_NAME.getMessage(leaveName));
    	}
	}
	
	@Override
	public List<String> tabComplete() {
		// k leave townName
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		Player bukkitPlayer = (Player) getSender();
		KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
		if(getArgs().length == 2) {
			List<String> leaveList = new ArrayList<String>();
			if(!player.isBarbarian()) {
				// Only leave options are towns in player's kingdom
				for(KonTown town : player.getKingdom().getTowns()) {
					if(town.isPlayerResident(bukkitPlayer)) {
						leaveList.add(town.getName());
					}
				}
			}
			tabList.addAll(leaveList);
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
	
}
