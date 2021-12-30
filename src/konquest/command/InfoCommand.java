package konquest.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import konquest.Konquest;
//import konquest.KonquestPlugin;
import konquest.model.KonKingdom;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.model.KonTown;
//import konquest.model.KonUpgrade;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

//import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class InfoCommand extends CommandBase {

	public InfoCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k info <kingdomName>|<townName>|<playerName>
		//ChatUtil.printDebug("Entering info command execution...");
    	if (getArgs().length != 1 && getArgs().length != 2) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	int displayState = 0; // 0 = kingdom, 1 = town, 2 = player
        	// Init info as own player's
        	if(!getKonquest().getPlayerManager().isPlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	KonPlayer sender = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	KonOfflinePlayer player = sender;
        	KonKingdom kingdom = player.getKingdom();
        	KonTown town = null;
        	//String color = ""+ChatColor.GREEN;
        	if(getArgs().length == 1) {
        		// Display your own Kingdom info
        		displayState = 0;
        	} else {
        		String name = getArgs()[1];
        		// preliminary town search
        		boolean foundTown = false;
        		for(KonKingdom k : getKonquest().getKingdomManager().getKingdoms()) {
    				if(k.hasTown(name)) {
    					foundTown = true;
    					town = k.getTown(name);
    				}
    			}
        		//Check for Kingdom
        		if(getKonquest().getKingdomManager().isKingdom(name)) {
        			//ChatUtil.printDebug("Parsed kingdom");
        			displayState = 0;
        			kingdom = getKonquest().getKingdomManager().getKingdom(name);
        		} else if(name.equalsIgnoreCase("barbarians")) {
        			//ChatUtil.printDebug("Parsed Barbarian kingdom");
        			displayState = 0;
        			kingdom = getKonquest().getKingdomManager().getBarbarians();
        		} else if(foundTown) {
        			//ChatUtil.printDebug("Parsed Town in sender's kingdom");
        			//Check for Town
        			displayState = 1;
        		} else {
        			// Check for Player
        			//ChatUtil.printDebug("Parsing player");
        			KonOfflinePlayer otherPlayer = getKonquest().getPlayerManager().getAllPlayerFromName(name);
        			//ChatUtil.printDebug("Fetched KonOfflinePlayer");
        			if(otherPlayer != null) {
        				//ChatUtil.printDebug("Player is not null");
        				displayState = 2;
        				player = otherPlayer;
        			} else {
            			//ChatUtil.sendError((Player) getSender(), "Failed to find unknown name, check spelling: "+name);
            			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(name));
            	        return;
        			}
        		}
        	}

        	switch(displayState) {
        	case 0: // Display kingdom info
        		getKonquest().getDisplayManager().displayKingdomInfoMenu(sender, kingdom);
        		break;
        		
        	case 1: // Display town info
        		if(town != null) {
        			getKonquest().getDisplayManager().displayTownInfoMenu(sender, town);
        		} else {
        			ChatUtil.printDebug("Failed to display null town info of town "+getArgs()[1]);
        			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
        		}
        		break;
        		
        	case 2: // Display player info
        		getKonquest().getDisplayManager().displayPlayerInfoMenu(sender, player);
        		break;
        		
        	default:
        		ChatUtil.printDebug("Failed to display info, unknown state");
        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
        		break;
        	}
        }
	}
	
	@Override
	public List<String> tabComplete() {
		// k info <kingdomName>|<townName>|<playerName>
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		if(getArgs().length == 2) {
			List<String> kingdomList = getKonquest().getKingdomManager().getKingdomNames();
			List<String> townList = new ArrayList<String>();
			for(KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
				townList.addAll(kingdom.getTownNames());
			}
			List<String> playerList = new ArrayList<>();
			//for(KonPlayer player : getKonquest().getPlayerManager().getPlayersOnline()) {
			for(OfflinePlayer bukkitOfflinePlayer : getKonquest().getPlayerManager().getAllOfflinePlayers()) {
				playerList.add(bukkitOfflinePlayer.getName());
			}
			
			tabList.addAll(kingdomList);
			tabList.addAll(townList);
			tabList.addAll(playerList);
			
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
