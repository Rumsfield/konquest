package konquest.command;

import konquest.Konquest;
import konquest.model.KonPlayer;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

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
        	String cmdMode = getArgs()[1];
        	if(cmdMode.equalsIgnoreCase("kingdoms")) {
        		String kingdomList = "";
            	for(String kingdomName : getKonquest().getKingdomManager().getKingdomNames()) {
            		kingdomList = kingdomList + " " + ChatColor.AQUA+kingdomName +ChatColor.GRAY+",";
            	}
            	if(kingdomList.length() > 2) {
            		kingdomList = kingdomList.substring(0,kingdomList.length()-2);
    			}
            	//kingdomList = kingdomList.substring(0, kingdomList.length()-2);
            	ChatUtil.sendNotice(bukkitPlayer, MessagePath.LABEL_KINGDOMS.getMessage()+":"+kingdomList);
        	} else if(cmdMode.equalsIgnoreCase("towns")) {
        		if(!player.isBarbarian()) {
        			String townList = "";
                	for(KonTown town : player.getKingdom().getTowns()) {
                		String townName = town.getName();
                		int pop = town.getNumResidents();
                		townList = townList + " "+ChatColor.AQUA+townName+ChatColor.YELLOW+"("+pop+")"+ChatColor.GRAY+",";
                	}
                	if(townList.length() > 2) {
                		townList = townList.substring(0,townList.length()-2);
        			}
                	//townList = townList.substring(0, townList.length()-2);
                	ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_LIST_NOTICE_TOWNS.getMessage()+":"+townList);
        		} else {
        			//ChatUtil.sendNotice(bukkitPlayer, "Barbarians do not have any Towns");
        			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
        		}
        	} else {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
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
