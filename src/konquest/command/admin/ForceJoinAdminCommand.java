package konquest.command.admin;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class ForceJoinAdminCommand extends CommandBase {
	
	public ForceJoinAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	//TODO: Refactor this
    	ChatUtil.sendError((Player) getSender(), "Not implemented");
		return;
		/*
    	// k admin forcejoin player1 kingdom1
    	if (getArgs().length != 4) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	int status = -1;
        	String playerName = getArgs()[2];
        	String kingdomName = getArgs()[3];
        	KonPlayer player = getKonquest().getPlayerManager().getPlayerFromName(playerName);

        	if(player == null) {
        		// No online player was found, attempt to find an offline player
        		KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayerFromName(playerName);
        		if(offlinePlayer == null) {
            		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
            		return;
            	} else {
            		// Found offline player, force them to join a kingdom
            		status = getKonquest().getKingdomManager().assignOfflinePlayerKingdom(offlinePlayer, kingdomName, true);
            	}
        	} else {
        		// Found online player, force them to join a kingdom
        		status = getKonquest().getKingdomManager().assignPlayerKingdom(player, kingdomName, true);
        	}
        	
        	switch(status) {
    		case 0:
    			ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_FORCEJOIN_NOTICE_SUCCESS.getMessage(playerName,kingdomName));
    			break;
    		case 1:
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
    			break;
    		case 2:
    			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_JOIN_ERROR_KINGDOM_LIMIT.getMessage(kingdomName));
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
        }
        */
    }
    
    @Override
	public List<String> tabComplete() {
		// k admin forcejoin player1 kingdom1
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 3) {
			List<String> playerList = new ArrayList<>();
			for(OfflinePlayer offlinePlayer : getKonquest().getPlayerManager().getAllOfflinePlayers()) {
				playerList.add(offlinePlayer.getName());
			}
			tabList.addAll(playerList);
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			List<String> kingdomList = getKonquest().getKingdomManager().getKingdomNames();
			tabList.addAll(kingdomList);
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}

}
