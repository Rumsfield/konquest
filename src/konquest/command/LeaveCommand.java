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
import konquest.utility.MessageStatic;

public class LeaveCommand extends CommandBase {

	public LeaveCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k leave townName
		if (getArgs().length != 2) {
			ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
            return;
		}
		
		Player bukkitPlayer = (Player) getSender();
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
		    				ChatUtil.sendNotice((Player) resident, bukkitPlayer.getName()+" has left the town of "+leaveName);
		    			}
		    		}
		    		ChatUtil.sendNotice(bukkitPlayer, "You have left the town of "+leaveName);
		    	} else {
		    		ChatUtil.sendError((Player) getSender(), "Failed to remove you from the town of "+leaveName);
		    	}
			} else {
				// Player is not resident, try to decline join invite
				if(town.isJoinInviteValid(id)) {
					// There is an existing invite to join, remove the player from the invites list
					town.removeJoinRequest(id);
					ChatUtil.sendNotice(bukkitPlayer, "Invite to join "+leaveName+" declined.");
				} else {
					ChatUtil.sendError(bukkitPlayer, "You are not a resident of "+leaveName);
				}
			}
		} else {
    		// Could not find what to leave
    		ChatUtil.sendError((Player) getSender(), "Failed to leave \""+leaveName+"\", maybe bad spelling or enemy.");
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
