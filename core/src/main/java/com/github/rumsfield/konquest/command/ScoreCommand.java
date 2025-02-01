package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.model.KonOfflinePlayer;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ScoreCommand extends CommandBase {

	public ScoreCommand() {
		// Define name and sender support
		super("score",true, false);
		// None
		setOptionalArgs(true);
		// [menu]
		addArgument(
				newArg("menu",true,false)
		);
		// all
		addArgument(
				newArg("all",true,false)
		);
		// player|kingdom <name>
		List<String> argNames = Arrays.asList("player", "kingdom");
		addArgument(
				newArg(argNames,true,false)
						.sub( newArg("name",false,false) )
		);
    }

	@Override
	public void execute(Konquest konquest, CommandSender sender, List<String> args) {
		// Sender must be player
		KonPlayer player = konquest.getPlayerManager().getPlayer(sender);
		if (player == null) {
			sendInvalidSenderMessage(sender);
			return;
		}
		// do not score peaceful kingdoms or barbarians
		Player bukkitPlayer = player.getBukkitPlayer();
		if(args.isEmpty()) {
			// Display base score menu
			konquest.getDisplayManager().displayScoreMenu(player);
			// Show player's score in chat
			if(!player.isBarbarian() && !player.getKingdom().isPeaceful()) {
				int kingdomScore = konquest.getKingdomManager().getKingdomScore(player.getKingdom());
				int playerScore = konquest.getKingdomManager().getPlayerScore(player);
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_SCORE_NOTICE_SCORE.getMessage(playerScore,player.getKingdom().getName(),kingdomScore));
			}
		} else {
			// Has arguments
			switch(args.get(0).toLowerCase()) {
				case "menu":
					// Display base score menu
					konquest.getDisplayManager().displayScoreMenu(player);
					break;
				case "all":
					// Score all Kingdoms
					ChatUtil.sendNotice(bukkitPlayer, ChatColor.GOLD + MessagePath.COMMAND_SCORE_NOTICE_ALL_HEADER.getMessage());
					for (KonKingdom allKingdom : konquest.getKingdomManager().getKingdoms()) {
						if (!allKingdom.isPeaceful()) {
							int score = konquest.getKingdomManager().getKingdomScore(allKingdom);
							String color = konquest.getDisplaySecondaryColor(player.getKingdom(), allKingdom);
							ChatUtil.sendMessage(bukkitPlayer, color + allKingdom.getName() + ChatColor.GOLD + ": " + ChatColor.DARK_PURPLE + score);
						}
					}
					break;
				case "player":
					if (args.size() != 2) {
						sendInvalidArgMessage(sender);
						return;
					}
					String playerName = args.get(1);
					KonOfflinePlayer scorePlayer = konquest.getPlayerManager().getOfflinePlayerFromName(playerName);
					if(scorePlayer == null) {
						ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
						return;
					}
					if (scorePlayer.isBarbarian()) {
						ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SCORE_ERROR_BARBARIAN.getMessage(scorePlayer.getOfflineBukkitPlayer().getName()));
					} else if (scorePlayer.getKingdom().isPeaceful()) {
						ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SCORE_ERROR_PEACEFUL.getMessage(scorePlayer.getKingdom().getName()));
					} else {
						int kingdomScore = konquest.getKingdomManager().getKingdomScore(scorePlayer.getKingdom());
						int playerScore = konquest.getKingdomManager().getPlayerScore(scorePlayer);
						ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_SCORE_NOTICE_PLAYER.getMessage(scorePlayer.getOfflineBukkitPlayer().getName(), playerScore, scorePlayer.getKingdom().getName(), kingdomScore));
						// Display Score GUI
						konquest.getDisplayManager().displayScorePlayerMenu(player, scorePlayer);
					}
					break;
				case "kingdom":
					if (args.size() != 2) {
						sendInvalidArgMessage(sender);
						return;
					}
					String kingdomName = args.get(1);
					if (!konquest.getKingdomManager().isKingdom(kingdomName)) {
						ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
						return;
					}
					KonKingdom scoreKingdom = konquest.getKingdomManager().getKingdom(kingdomName);
					if (scoreKingdom.isPeaceful()) {
						ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SCORE_ERROR_PEACEFUL.getMessage(scoreKingdom.getName()));
					} else {
						int kingdomScore = konquest.getKingdomManager().getKingdomScore(scoreKingdom);
						ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_SCORE_NOTICE_KINGDOM.getMessage(scoreKingdom.getName(), kingdomScore));
						// Display Score GUI
						konquest.getDisplayManager().displayScoreKingdomMenu(player, scoreKingdom);
					}
					break;
				default:
					sendInvalidArgMessage(sender);
			}

		}
		
	}
	
	@Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		// Give suggestions
		if(args.size() == 1) {
			tabList.add("menu");
			tabList.add("all");
			tabList.add("player");
			tabList.add("kingdom");
		} else if(args.size() == 2) {
			String type = args.get(0).toLowerCase();
			switch(type) {
				case "player":
					for(OfflinePlayer bukkitOfflinePlayer : konquest.getPlayerManager().getAllOfflinePlayers()) {
						tabList.add(bukkitOfflinePlayer.getName());
					}
					break;
				case "kingdom":
					tabList.addAll(konquest.getKingdomManager().getKingdomNames());
					break;
				default:
					break;
			}
		}
		return matchLastArgToList(tabList,args);
	}
}
