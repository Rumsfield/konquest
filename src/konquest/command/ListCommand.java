package konquest.command;

import konquest.Konquest;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessageStatic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class ListCommand extends CommandBase {

	public ListCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k list kingdoms|towns
    	if (getArgs().length != 2) {
            ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	String cmdMode = getArgs()[1];
        	if(cmdMode.equalsIgnoreCase("kingdoms")) {
        		String kingdomList = "";
            	for(String kingdomName : getKonquest().getKingdomManager().getKingdomNames()) {
            		kingdomList = kingdomList + " " + ChatColor.AQUA+kingdomName +ChatColor.GRAY+",";
            	}
            	//kingdomList = kingdomList.substring(0, kingdomList.length()-2);
            	ChatUtil.sendNotice(bukkitPlayer, "Kingdoms:"+kingdomList);
        	} else if(cmdMode.equalsIgnoreCase("towns")) {
        		if(!player.isBarbarian()) {
        			String townList = "";
                	for(String townName : player.getKingdom().getTownNames()) {
                		townList = townList + " " + ChatColor.AQUA+townName +ChatColor.GRAY+",";
                	}
                	//townList = townList.substring(0, townList.length()-2);
                	ChatUtil.sendNotice(bukkitPlayer, "Towns in your Kingdom:"+townList);
        		} else {
        			ChatUtil.sendNotice(bukkitPlayer, "Barbarians do not have any Towns");
        		}
        		
        	} else {
        		 ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
                 return;
        	}
        }
	}
	
	@Override
	public List<String> tabComplete() {
		// k list kingdoms|towns
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 2) {
			tabList.add("kingdoms");
			tabList.add("towns");
			
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
