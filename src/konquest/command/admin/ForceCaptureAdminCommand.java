package konquest.command.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonKingdom;
import konquest.model.KonPlayer;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class ForceCaptureAdminCommand extends CommandBase {

	public ForceCaptureAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k admin forcecapture town1 kingdom1
		if (getArgs().length != 4) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	String townName = getArgs()[2];
        	String kingdomName = getArgs()[3];
        	boolean status = false;
        	if(getKonquest().getKingdomManager().isKingdom(kingdomName)) {
        		KonKingdom kingdom = getKonquest().getKingdomManager().getKingdom(kingdomName);
        		if(!kingdom.equals(getKonquest().getKingdomManager().getBarbarians()) &&
        				!kingdom.equals(getKonquest().getKingdomManager().getNeutrals()) && 
        				!kingdom.hasTown(townName)) {
        			// Find town's kingdom
        			KonTown town = null;
        			for(KonKingdom testKingdom : getKonquest().getKingdomManager().getKingdoms()) {
        				if(testKingdom.hasTown(townName)) {
        					town = testKingdom.getTown(townName);
        				}
        			}
        			if(town == null) {
        				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(townName));
        			} else {
        				status = getKonquest().getKingdomManager().captureTown(townName, town.getKingdom().getName(), kingdom);
        				if(status) {
        					ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
        					// Remove mob targets
    						for(KonPlayer player : getKonquest().getPlayerManager().getPlayersOnline()) {
    							if(town.isLocInside(player.getBukkitPlayer().getLocation())) {
    								player.clearAllMobAttackers();
    							}
    						}
        				} else {
        					ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_FAILED.getMessage());
        				}
        			}
        		} else {
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
        		}
        	} else {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
        	}
        }
	}
	
	@Override
	public List<String> tabComplete() {
		// k admin forcecapture town1 kingdom1
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		if(getArgs().length == 3) {
			// Suggest town names
			List<String> townList = new ArrayList<String>();
			for(KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
				townList.addAll(kingdom.getTownNames());
			}
			tabList.addAll(townList);
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			// Suggest enemy kingdom names
			List<String> kingdomList = getKonquest().getKingdomManager().getKingdomNames();
			tabList.addAll(kingdomList);
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
	
}
