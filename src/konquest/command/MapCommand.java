package konquest.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import konquest.Konquest;
import konquest.manager.KingdomManager;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

//import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class MapCommand extends CommandBase {

	public MapCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k map [far|f|auto|a]
    	if (getArgs().length != 1 && getArgs().length != 2) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	//World bukkitWorld = bukkitPlayer.getWorld();
        	/*
        	if(!bukkitWorld.getName().equals(getKonquest().getWorldName())) {
        		ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_WORLD.toString());
                return;
        	}*/
        	if(!getKonquest().getPlayerManager().isPlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
			// Display formatted text in chat as chunk map
        	// 10 lines of text by default
        	//int mapSize = 9; // must be odd so that player's chunk is in the center
        	int mapSize = KingdomManager.DEFAULT_MAP_SIZE;
        	if(getArgs().length == 2) {
        		/*if(!getArgs()[1].equalsIgnoreCase("far") && !getArgs()[1].equalsIgnoreCase("f")) {
        			ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
                    return;
        		}*/
        		if(getArgs()[1].equalsIgnoreCase("far") || getArgs()[1].equalsIgnoreCase("f")) {
        			mapSize = 19;
        		} else if(getArgs()[1].equalsIgnoreCase("auto") || getArgs()[1].equalsIgnoreCase("a")) {
        			if(player.isMapAuto()) {
        				player.setIsMapAuto(false);
        				//ChatUtil.sendNotice((Player) getSender(), "Disabled auto map");
        				ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_DISABLE_AUTO.getMessage());
        				return;
        			} else {
        				player.setIsMapAuto(true);
        				//ChatUtil.sendNotice((Player) getSender(), "Enabled auto map");
        				ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_ENABLE_AUTO.getMessage());
        			}
        		} else {
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
                    return;
        		}
        	}
        	getKonquest().getKingdomManager().printPlayerMap(player, mapSize);
        }
	}
	
	@Override
	public List<String> tabComplete() {
		// k map [far|f]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		if(getArgs().length == 2) {
			tabList.add("far");
			tabList.add("f");
			tabList.add("auto");
			tabList.add("a");
			
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
