package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TravelAdminCommand  extends CommandBase {
	
	public TravelAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	// k admin travel <name>
    	if (getArgs().length != 3 && getArgs().length != 4) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
		} else {
        	Player bukkitPlayer = (Player) getSender();
        	
        	String travelTo = getArgs()[2];
        	Location destination;
        	if(getKonquest().getKingdomManager().isKingdom(travelTo)) {
        		destination = getKonquest().getKingdomManager().getKingdom(travelTo).getCapital().getSpawnLoc();
        		getKonquest().telePlayerLocation(bukkitPlayer, destination);
        		return;
        	} else if(getKonquest().getSanctuaryManager().isSanctuary(travelTo)) {
        		destination = getKonquest().getSanctuaryManager().getSanctuary(travelTo).getSpawnLoc();
        		getKonquest().telePlayerLocation(bukkitPlayer, destination);
        		return;
        	} else if(getKonquest().getRuinManager().isRuin(travelTo)) {
        		destination = getKonquest().getRuinManager().getRuin(travelTo).getSpawnLoc();
        		getKonquest().telePlayerLocation(bukkitPlayer, destination);
        		return;
        	} else if(getKonquest().getCampManager().isCampName(travelTo)) {
        		destination = getKonquest().getCampManager().getCampFromName(travelTo).getSpawnLoc();
        		getKonquest().telePlayerLocation(bukkitPlayer, destination);
        		return;
            } else {
        		for(KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
        			if(kingdom.hasTown(travelTo)) {
        				getKonquest().telePlayerLocation(bukkitPlayer, kingdom.getTown(travelTo).getSpawnLoc());
        				return;
        			}
        		}
        	}
        	ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(travelTo));
        }
    }
    
    @Override
	public List<String> tabComplete() {
    	// k admin travel <name>
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();

		if(getArgs().length == 3) {
			List<String> kingdomList = getKonquest().getKingdomManager().getKingdomNames();
			tabList.addAll(kingdomList);
			for(String kingdomName : kingdomList) {
				tabList.addAll(getKonquest().getKingdomManager().getKingdom(kingdomName).getTownNames());
			}
			tabList.addAll(getKonquest().getRuinManager().getRuinNames());
			tabList.addAll(getKonquest().getCampManager().getCampNames());
			tabList.addAll(getKonquest().getSanctuaryManager().getSanctuaryNames());
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}	
		return matchedTabList;
	}
}
