package konquest.command.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.model.KonStats;
import konquest.model.KonStatsType;
import konquest.utility.ChatUtil;
import konquest.utility.MessageStatic;

public class StatAdminCommand extends CommandBase {

	public StatAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
		super(konquest, sender, args);
	}

	@Override
	public void execute() {
		// k admin stat <player> <stat> show|set|add|clear [<value>]
		if (getArgs().length != 5 && getArgs().length != 6) {
            ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
            return;
        } else {
        	
        	String playerName = getArgs()[2];
        	String statName = getArgs()[3];
        	String subCmd = getArgs()[4];
        	// Verify player exists
        	KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getAllPlayerFromName(playerName);
        	if(offlinePlayer == null) {
        		ChatUtil.sendError((Player) getSender(), "Invalid player name, unknown or bad spelling.");
                return;
        	}
        	String bukkitPlayerName = offlinePlayer.getOfflineBukkitPlayer().getName();
        	KonPlayer player = getKonquest().getPlayerManager().getPlayerFromName(playerName);
        	boolean isPlayerOnline = false;
        	KonStats stats;
        	if(player == null) {
        		// Use offline player, pull stats from DB
        		stats = getKonquest().getDatabaseThread().getDatabase().pullPlayerStats(offlinePlayer.getOfflineBukkitPlayer());
        	} else {
        		// Use online player's active stats
        		stats = player.getPlayerStats();
        		isPlayerOnline = true;
        	}
        	// Verify stat exists
        	KonStatsType stat = KonStatsType.getStat(statName);
        	if(stat == null) {
        		ChatUtil.sendError((Player) getSender(), "Invalid stat, could not match to existing stats.");
                return;
        	}
        	// Perform sub-commands
        	String value = "";
        	int valueNum = 0;
        	int currentValue = 0;
        	switch(subCmd.toLowerCase()) {
        	case "show":
        		currentValue = stats.getStat(stat);
            	ChatUtil.sendNotice((Player) getSender(), "Stat "+stat.toString()+" for player "+bukkitPlayerName+": "+currentValue);
            	// Remove reference to offline player's stats
            	if(!isPlayerOnline) {
            		stats = null;
            	}
        		break;
        	case "set":
        		if (getArgs().length == 6) {
        			value = getArgs()[5];
				}
        		try {
        			valueNum = Integer.parseInt(value);
        		} catch(NumberFormatException e) {
        			ChatUtil.sendError((Player) getSender(), "Invalid stat value: "+e.getMessage());
                    return;
        		}
        		stats.setStat(stat, valueNum);
        		ChatUtil.sendNotice((Player) getSender(), "Set stat "+stat.toString()+" for player "+bukkitPlayerName+": "+valueNum);
        		if(!isPlayerOnline) {
        			getKonquest().getDatabaseThread().getDatabase().pushPlayerStats(offlinePlayer.getOfflineBukkitPlayer(), stats);
        			stats = null;
            	}
        		break;
        	case "add":
        		if (getArgs().length == 6) {
        			value = getArgs()[5];
				}
        		try {
        			valueNum = Integer.parseInt(value);
        		} catch(NumberFormatException e) {
        			ChatUtil.sendError((Player) getSender(), "Invalid stat value: "+e.getMessage());
                    return;
        		}
        		currentValue = stats.getStat(stat);
        		int newValue = currentValue+valueNum;
        		stats.setStat(stat, newValue);
        		ChatUtil.sendNotice((Player) getSender(), "Added "+valueNum+" to stat "+stat.toString()+" for player "+bukkitPlayerName+": "+newValue);
        		if(!isPlayerOnline) {
        			getKonquest().getDatabaseThread().getDatabase().pushPlayerStats(offlinePlayer.getOfflineBukkitPlayer(), stats);
        			stats = null;
            	}
        		break;
        	case "clear":
        		stats.setStat(stat, 0);
        		ChatUtil.sendNotice((Player) getSender(), "Cleared stat "+stat.toString()+" for player "+bukkitPlayerName);
            	// Remove reference to offline player's stats
            	if(!isPlayerOnline) {
            		stats = null;
            	}
        		break;
        	default:
        		ChatUtil.sendError((Player) getSender(), "Invalid sub-command, expected show|set|add|clear");
        		return;
        	}
        }
	}

	@Override
	public List<String> tabComplete() {
		// k admin stat <player> <stat> show|set|add|clear [<value>]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		if(getArgs().length == 3) {
			// Suggest player names
			for(KonOfflinePlayer offlinePlayer : getKonquest().getPlayerManager().getAllKonOfflinePlayers()) {
				tabList.add(offlinePlayer.getOfflineBukkitPlayer().getName());
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			// suggest stat names
			for(KonStatsType stat : KonStatsType.values()) {
				tabList.add(stat.toString());
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 5) {
			// suggest sub-commands
			tabList.add("show");
			tabList.add("set");
			tabList.add("add");
			tabList.add("clear");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[4], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 6) {
			// suggest number
			String subCommand = getArgs()[4];
			if(subCommand.equalsIgnoreCase("set") || subCommand.equalsIgnoreCase("add")) {
				tabList.add("#");
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[5], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}

}
