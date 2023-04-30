package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.manager.TerritoryManager;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapCommand extends CommandBase {

	private int mapSize = TerritoryManager.DEFAULT_MAP_SIZE;
	
	public MapCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k map [far|f|auto|a]
    	if (getArgs().length != 1 && getArgs().length != 2) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
		} else {
        	Player bukkitPlayer = (Player) getSender();
			if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
			// Display formatted text in chat as chunk map
        	// 10 lines of text by default
        	
        	if(getArgs().length == 2) {
        		if(getArgs()[1].equalsIgnoreCase("far") || getArgs()[1].equalsIgnoreCase("f")) {
        			mapSize = 19;
        		} else if(getArgs()[1].equalsIgnoreCase("auto") || getArgs()[1].equalsIgnoreCase("a")) {
        			if(player.isMapAuto()) {
        				player.setIsMapAuto(false);
        				ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_DISABLE_AUTO.getMessage());
        				return;
        			} else {
        				player.setIsMapAuto(true);
        				ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_ENABLE_AUTO.getMessage());
        			}
        		} else {
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
                    return;
        		}
        	}
        	
        	Bukkit.getScheduler().runTaskAsynchronously(getKonquest().getPlugin(),
					() -> getKonquest().getTerritoryManager().printPlayerMap(player, mapSize));
        	
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
