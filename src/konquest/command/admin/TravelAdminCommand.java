package konquest.command.admin;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonKingdom;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class TravelAdminCommand  extends CommandBase {
	
	public TravelAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	// k admin travel kingdom1|town1|ruin1
    	if (getArgs().length != 3 && getArgs().length != 4) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	
        	String travelTo = getArgs()[2];
        	Location destination;
        	if(getKonquest().getKingdomManager().isKingdom(travelTo)) {
        		destination = getKonquest().getKingdomManager().getKingdom(travelTo).getCapital().getSpawnLoc();
        		getKonquest().telePlayerLocation(bukkitPlayer, destination);
        		return;
        	} else if(getKonquest().getRuinManager().isRuin(travelTo)) {
        		destination = getKonquest().getRuinManager().getRuin(travelTo).getCenterLoc();
        		getKonquest().telePlayerLocation(bukkitPlayer, destination);
        		return;
        	} else {
        		for(KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
        			if(kingdom.hasTown(travelTo)) {
        				getKonquest().telePlayerTerritory(bukkitPlayer, kingdom.getTown(travelTo));
        				return;
        			}
        		}
        	}
        	//ChatUtil.sendError((Player) getSender(), "Travel destination does not exist");
        	ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(travelTo));
        }
    }
    
    @Override
	public List<String> tabComplete() {
    	// k admin travel town1|kingdom1|ruin1
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();

		if(getArgs().length == 3) {
			List<String> kingdomList = getKonquest().getKingdomManager().getKingdomNames();
			tabList.addAll(kingdomList);
			for(String kingdomName : kingdomList) {
				tabList.addAll(getKonquest().getKingdomManager().getKingdom(kingdomName).getTownNames());
			}
			for(String ruinName : getKonquest().getRuinManager().getRuinNames()) {
				tabList.add(ruinName);
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}	
		return matchedTabList;
	}
}
