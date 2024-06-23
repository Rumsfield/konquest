package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.model.KonOfflinePlayer;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonStats;
import com.github.rumsfield.konquest.model.KonStatsType;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StatAdminCommand extends CommandBase {

	public StatAdminCommand() {
		// Define name and sender support
		super("stat",false, true);
		// Define arguments
		List<String> argNames = Arrays.asList("show", "set", "add", "clear");
		// show|set|add|clear <player> <name> [<value>]
		addArgument(
				newArg(argNames,true,false)
						.sub( newArg("player",false,false)
								.sub( newArg("name",false,true)
										.sub( newArg("value",false,false) ) ) )
		);
	}

	@Override
	public void execute(Konquest konquest, CommandSender sender, List<String> args) {
		if (args.size() < 3) {
			sendInvalidArgMessage(sender);
			return;
		}
		String subCmd = args.get(0);
		String playerName = args.get(1);
		String statName = args.get(2);
		// Verify player exists
		KonOfflinePlayer offlinePlayer = konquest.getPlayerManager().getOfflinePlayerFromName(playerName);
		if(offlinePlayer == null) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
			return;
		}
		String bukkitPlayerName = offlinePlayer.getOfflineBukkitPlayer().getName();
		KonPlayer player = konquest.getPlayerManager().getPlayerFromName(playerName);
		boolean isPlayerOnline = false;
		KonStats stats;
		if(player == null) {
			// Use offline player, pull stats from DB
			stats = konquest.getDatabaseThread().getDatabase().pullPlayerStats(offlinePlayer.getOfflineBukkitPlayer());
		} else {
			// Use online player's active stats
			stats = player.getPlayerStats();
			isPlayerOnline = true;
		}
		// Verify stat exists
		KonStatsType stat = KonStatsType.getStat(statName);
		if(stat == null) {
			ChatUtil.sendError(sender, MessagePath.COMMAND_ADMIN_STAT_ERROR_NAME.getMessage(statName));
			return;
		}
		// Perform sub-commands
		String value = "";
		int valueNum;
		int currentValue;
		switch(subCmd.toLowerCase()) {
		case "show":
			currentValue = stats.getStat(stat);
			ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_STAT_NOTICE_SHOW.getMessage(stat.toString(),bukkitPlayerName,currentValue));
			// Remove reference to offline player's stats
			if(!isPlayerOnline) {
				stats = null;
			}
			break;
		case "set":
			if (args.size() == 4) {
				value = args.get(3);
			}
			try {
				valueNum = Integer.parseInt(value);
			} catch(NumberFormatException e) {
				ChatUtil.sendError(sender, MessagePath.COMMAND_ADMIN_STAT_ERROR_VALUE.getMessage(e.getMessage()));
				return;
			}
			stats.setStat(stat, valueNum);
			ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_STAT_NOTICE_SET.getMessage(stat.toString(),bukkitPlayerName,valueNum));
			if(!isPlayerOnline) {
				konquest.getDatabaseThread().getDatabase().pushPlayerStats(offlinePlayer.getOfflineBukkitPlayer(), stats);
				stats = null;
			} else {
				konquest.getAccomplishmentManager().initPlayerPrefixes(player);
			}
			break;
		case "add":
			if (args.size() == 4) {
				value = args.get(3);
			}
			try {
				valueNum = Integer.parseInt(value);
			} catch(NumberFormatException e) {
				ChatUtil.sendError(sender, MessagePath.COMMAND_ADMIN_STAT_ERROR_VALUE.getMessage(e.getMessage()));
				return;
			}
			currentValue = stats.getStat(stat);
			int newValue = currentValue+valueNum;
			stats.setStat(stat, newValue);
			ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_STAT_NOTICE_ADD.getMessage(valueNum,stat.toString(),bukkitPlayerName,newValue));
			if(!isPlayerOnline) {
				konquest.getDatabaseThread().getDatabase().pushPlayerStats(offlinePlayer.getOfflineBukkitPlayer(), stats);
				stats = null;
			} else {
				konquest.getAccomplishmentManager().initPlayerPrefixes(player);
			}
			break;
		case "clear":
			stats.setStat(stat, 0);
			ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_STAT_NOTICE_CLEAR.getMessage(stat.toString(),bukkitPlayerName));
			if(!isPlayerOnline) {
				konquest.getDatabaseThread().getDatabase().pushPlayerStats(offlinePlayer.getOfflineBukkitPlayer(), stats);
				stats = null;
			} else {
				konquest.getAccomplishmentManager().initPlayerPrefixes(player);
			}
			break;
		default:
			sendInvalidArgMessage(sender);
		}
	}

	@Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		if (args.size() == 1) {
			tabList.add("show");
			tabList.add("set");
			tabList.add("add");
			tabList.add("clear");
		} else if (args.size() == 2) {
			// Suggest player names
			for(KonOfflinePlayer offlinePlayer : konquest.getPlayerManager().getAllKonquestOfflinePlayers()) {
				tabList.add(offlinePlayer.getOfflineBukkitPlayer().getName());
			}
		} else if (args.size() == 3) {
			// suggest stat names
			for(KonStatsType stat : KonStatsType.values()) {
				tabList.add(stat.toString());
			}
		} else if (args.size() == 4) {
			// suggest number
			if(args.get(0).equalsIgnoreCase("set") || args.get(0).equalsIgnoreCase("add")) {
				tabList.add("#");
			}
		}
		return matchLastArgToList(tabList,args);
	}

}
