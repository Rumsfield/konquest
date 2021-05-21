package konquest.command.admin;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonKingdom;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class ListAdminCommand extends CommandBase {
	
	public ListAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	// k admin list kingdoms|towns|all|ruins
    	if (getArgs().length != 3) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	
        	String listMode = getArgs()[2];
        	
        	switch(listMode) {
        	case "kingdoms":
        		//ChatUtil.sendNotice((Player) getSender(), "All Kingdoms:");
        		ChatUtil.sendNotice((Player) getSender(), MessagePath.LABEL_KINGDOMS.getMessage());
                for(String kingdomName : getKonquest().getKingdomManager().getKingdomNames()) {
                    ChatUtil.sendNotice((Player) getSender(), kingdomName);
                }
        		break;
        	case "towns":
        		//ChatUtil.sendNotice((Player) getSender(), "All Towns:");
        		ChatUtil.sendNotice((Player) getSender(), MessagePath.LABEL_TOWNS.getMessage());
        		for(KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
                    for(String townName : kingdom.getTownNames()) {
                    	ChatUtil.sendNotice((Player) getSender(), townName);
                    }
                }
        		break;
        	case "all":
        		//ChatUtil.sendNotice((Player) getSender(), "All Kingdoms and Towns:");
        		for(KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
        			ChatUtil.sendNotice((Player) getSender(), MessagePath.LABEL_KINGDOM.getMessage()+" "+kingdom.getName(), ChatColor.GOLD);
                    for(String townName : kingdom.getTownNames()) {
                    	ChatUtil.sendNotice((Player) getSender(), MessagePath.LABEL_TOWN.getMessage()+" "+townName, ChatColor.GRAY);
                    }
                }
        		break;
        	case "ruins":
        		//ChatUtil.sendNotice((Player) getSender(), "All Ruins:");
        		ChatUtil.sendNotice((Player) getSender(), MessagePath.LABEL_RUINS.getMessage());
                for(String ruinName : getKonquest().getRuinManager().getRuinNames()) {
                    ChatUtil.sendNotice((Player) getSender(), ruinName);
                }
        		break;
        	default :
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
                return;
        	}
        }
    }
    
    @Override
	public List<String> tabComplete() {
    	// k admin list kingdoms|towns|all
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 3) {
			tabList.add("kingdoms");
			tabList.add("towns");
			tabList.add("all");
			tabList.add("ruins");
			
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
