package konquest.command.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonOfflinePlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class RemoveCampAdminCommand extends CommandBase {

	public RemoveCampAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

	public void execute() {
		// k admin removecamp player1
    	if (getArgs().length != 3) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	//Player bukkitPlayer = (Player) getSender();
        	//World bukkitWorld = bukkitPlayer.getWorld();
        	
        	/*
        	if(!getKonquest().isWorldValid(bukkitWorld)) {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
                return;
        	}
        	*/
        	String playerName = getArgs()[2];
        	
        	if(getKonquest().getPlayerManager().isPlayerNameExist(playerName)) {
        		KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getAllPlayerFromName(playerName);
        		if(offlinePlayer != null && offlinePlayer.isBarbarian()) {
        			boolean status = getKonquest().getCampManager().removeCamp(offlinePlayer);
        			if(status) {
        				ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
        			} else {
        				// Failed to remove camp for the given player
        				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_FAILED.getMessage());
        			}
        		} else {
        			// Could not get valid player
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PLAYER.getMessage());
        		}
        	} else {
        		// Could not find a known player by name
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage());
        	}
        }
		
	}

	@Override
	public List<String> tabComplete() {
		// k admin removecamp player1
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		// Suggest player names
		for(KonOfflinePlayer offlinePlayer : getKonquest().getPlayerManager().getAllKonOfflinePlayers()) {
			if(offlinePlayer.isBarbarian()) {
				tabList.add(offlinePlayer.getOfflineBukkitPlayer().getName());
			}
		}
		// Trim down completion options based on current input
		StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
		Collections.sort(matchedTabList);
		return matchedTabList;
	}

}
