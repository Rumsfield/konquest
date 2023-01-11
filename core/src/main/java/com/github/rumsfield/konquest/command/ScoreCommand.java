package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.model.KonOfflinePlayer;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class ScoreCommand extends CommandBase {

	public ScoreCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k score [<player>|all]
		// do not score peaceful kingdoms or barbarians
    	if (getArgs().length != 1 && getArgs().length != 2) {
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
        	KonKingdom kingdom = player.getKingdom();
        	String color = ""+ChatColor.GREEN;
        	int kingdomScore = 0;
        	int playerScore = 0;
        	if(getArgs().length == 1) {
        		// Display player's own score GUI
        		if(player.isBarbarian()) {
        			//ChatUtil.sendError((Player) getSender(), ChatColor.GOLD+"You are a Barbarian, and cannot be scored!");
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
        		} else if(kingdom.isPeaceful()) {
        			//ChatUtil.sendError((Player) getSender(), ChatColor.GOLD+"The Kingdom of "+color+kingdom.getName()+ChatColor.GOLD+" is peaceful, and cannot be scored!");
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SCORE_ERROR_PEACEFUL.getMessage(kingdom.getName()));
        		} else {
        			kingdomScore = getKonquest().getKingdomManager().getKingdomScore(kingdom);
        			playerScore = getKonquest().getKingdomManager().getPlayerScore(player);
        			//ChatUtil.sendNotice((Player) getSender(), ChatColor.GOLD+"Your score: "+ChatColor.AQUA+playerScore+ChatColor.GOLD+", Kingdom of "+color+kingdom.getName()+ChatColor.GOLD+" score: "+ChatColor.DARK_PURPLE+kingdomScore);
        			ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_SCORE_NOTICE_SCORE.getMessage(playerScore,kingdom.getName(),kingdomScore));
        			// Display Score GUI
        			getKonquest().getDisplayManager().displayScoreMenu(player, player);
        		}
        	} else if(getArgs()[1].equalsIgnoreCase("all")) {
        		// Score all Kingdoms
        		//String header = ChatColor.GOLD+"All Kingdom Scores:";
            	ChatUtil.sendNotice((Player) getSender(), ChatColor.GOLD+MessagePath.COMMAND_SCORE_NOTICE_ALL_HEADER.getMessage());
        		for(KonKingdom allKingdom : getKonquest().getKingdomManager().getKingdoms()) {
        			if(!kingdom.isPeaceful()) {
	        			int score = getKonquest().getKingdomManager().getKingdomScore(allKingdom);
	        			if(player.getKingdom().equals(allKingdom)) {
	        				color = ""+ChatColor.GREEN;
	        			} else {
	        				color = ""+ChatColor.RED;
	        			}
	        			ChatUtil.sendMessage((Player) getSender(), color+allKingdom.getName()+ChatColor.GOLD+": "+ChatColor.DARK_PURPLE+score);
        			}
        		}
        	} else {
        		String playerName = getArgs()[1];
        		// Verify player exists
            	KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayerFromName(playerName);
            	if(offlinePlayer == null) {
            		//ChatUtil.sendError((Player) getSender(), "Invalid player name, unknown or bad spelling.");
            		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
                    return;
            	}
            	kingdom = offlinePlayer.getKingdom();
            	if(!player.getKingdom().equals(kingdom)) {
    				color = ""+ChatColor.RED;
    			}
            	String offlinePlayerName = offlinePlayer.getOfflineBukkitPlayer().getName();
            	if(offlinePlayer.isBarbarian()) {
        			//ChatUtil.sendError((Player) getSender(), ChatColor.GOLD+offlinePlayerName+" is a Barbarian, and cannot be scored!");
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SCORE_ERROR_BARBARIAN.getMessage(offlinePlayerName));
        		} else if(kingdom.isPeaceful()) {
        			//ChatUtil.sendError((Player) getSender(), ChatColor.GOLD+"The Kingdom of "+color+kingdom.getName()+ChatColor.GOLD+" is peaceful, and cannot be scored!");
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SCORE_ERROR_PEACEFUL.getMessage(kingdom.getName()));
        		} else {
        			kingdomScore = getKonquest().getKingdomManager().getKingdomScore(kingdom);
        			playerScore = getKonquest().getKingdomManager().getPlayerScore(offlinePlayer);
        			//ChatUtil.sendNotice((Player) getSender(), color+offlinePlayerName+"'s "+ChatColor.GOLD+" score: "+ChatColor.AQUA+playerScore+ChatColor.GOLD+", Kingdom of "+color+kingdom.getName()+ChatColor.GOLD+" score: "+ChatColor.DARK_PURPLE+kingdomScore);
        			ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_SCORE_NOTICE_PLAYER.getMessage(offlinePlayerName,playerScore,kingdom.getName(),kingdomScore));
        			// Display Score GUI
        			getKonquest().getDisplayManager().displayScoreMenu(player, offlinePlayer);
        		}
        	}
        }
	}
	
	@Override
	public List<String> tabComplete() {
		// k score [<player>|all]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		tabList.add("all");
		List<String> playerList = new ArrayList<>();
		for(OfflinePlayer bukkitOfflinePlayer : getKonquest().getPlayerManager().getAllOfflinePlayers()) {
			playerList.add(bukkitOfflinePlayer.getName());
		}
		tabList.addAll(playerList);
		// Trim down completion options based on current input
		StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
		Collections.sort(matchedTabList);
		
		return matchedTabList;
	}
}
