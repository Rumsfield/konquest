package konquest.command;

import konquest.Konquest;
import konquest.model.KonKingdom;
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

public class ScoreCommand extends CommandBase {

	public ScoreCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k score [<kingdomName>|all]
		// do not score peaceful kingdoms
    	if (getArgs().length != 1 && getArgs().length != 2) {
            ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	KonKingdom kingdom = player.getKingdom();
        	String color = ""+ChatColor.GREEN;
        	int score = 0;
        	if(getArgs().length == 1) {
        		// Score player's own Kingdom
        		if(kingdom.isPeaceful()) {
        			ChatUtil.sendNotice((Player) getSender(), ChatColor.GOLD+"The Kingdom of "+color+kingdom.getName()+ChatColor.GOLD+" is peaceful, and cannot be scored!");
        		} else {
        			score = getKonquest().getKingdomManager().getKingdomScore(kingdom);
            		ChatUtil.sendNotice((Player) getSender(), ChatColor.GOLD+"Kingdom Score of "+color+kingdom.getName()+ChatColor.GOLD+": "+ChatColor.DARK_PURPLE+score);
            		int index = 1;
            		for(String name : getKonquest().getKingdomManager().getKingdomLeaderboard(kingdom)) {
            			ChatUtil.sendMessage((Player) getSender(), ChatColor.GOLD+""+index+". "+color+name);
            			index ++;
            		}
        		}
        	} else if(getArgs()[1].equalsIgnoreCase("all")) {
        		// Score all Kingdoms
        		String header = ChatColor.GOLD+"All Kingdom Scores:";
            	ChatUtil.sendNotice((Player) getSender(), header);
        		for(KonKingdom allKingdom : getKonquest().getKingdomManager().getKingdoms()) {
        			if(!kingdom.isPeaceful()) {
	        			score = getKonquest().getKingdomManager().getKingdomScore(allKingdom);
	        			if(player.getKingdom().equals(allKingdom)) {
	        				color = ""+ChatColor.GREEN;
	        			} else {
	        				color = ""+ChatColor.RED;
	        			}
	        			ChatUtil.sendMessage((Player) getSender(), color+allKingdom.getName()+ChatColor.GOLD+": "+ChatColor.DARK_PURPLE+score);
        			}
        		}
        	} else {
        		String kingdomName = getArgs()[1];
        		if(getKonquest().getKingdomManager().isKingdom(kingdomName)) {
        			kingdom = getKonquest().getKingdomManager().getKingdom(kingdomName);
        			if(kingdom.isPeaceful()) {
        				if(!player.getKingdom().equals(kingdom)) {
	        				color = ""+ChatColor.GRAY;
	        			}
            			ChatUtil.sendNotice((Player) getSender(), ChatColor.GOLD+"The Kingdom of "+color+kingdom.getName()+ChatColor.GOLD+" is peaceful, and cannot be scored!");
            		} else {
	        			score = getKonquest().getKingdomManager().getKingdomScore(kingdom);
	        			if(!player.getKingdom().equals(kingdom)) {
	        				color = ""+ChatColor.RED;
	        			}
	        			ChatUtil.sendNotice((Player) getSender(), ChatColor.GOLD+"Kingdom Score of "+color+kingdom.getName()+ChatColor.GOLD+": "+ChatColor.DARK_PURPLE+score);
	        			int index = 1;
	            		for(String name : getKonquest().getKingdomManager().getKingdomLeaderboard(kingdom)) {
	            			ChatUtil.sendMessage((Player) getSender(), ChatColor.GOLD+""+index+". "+color+name);
	            			index ++;
	            		}
            		}
        		} else {
        			ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
        		}
        	}
        }
	}
	
	@Override
	public List<String> tabComplete() {
		// k score [<kingdomName>|all]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		List<String> kingdomList = getKonquest().getKingdomManager().getKingdomNames();
		
		tabList.addAll(kingdomList);
		tabList.add("all");
		// Trim down completion options based on current input
		StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
		Collections.sort(matchedTabList);
		
		return matchedTabList;
	}
}
