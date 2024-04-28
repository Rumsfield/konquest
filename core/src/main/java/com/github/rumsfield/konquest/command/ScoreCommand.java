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
		// [all]
		addArgument(
				newArg("all",true,false)
		);
		// <player>
		addArgument(
				newArg("player",false,false)
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
		KonKingdom kingdom = player.getKingdom();
		int kingdomScore;
		int playerScore;
		if(args.isEmpty()) {
			// Display player's own score GUI
			if(player.isBarbarian()) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
			} else if(kingdom.isPeaceful()) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SCORE_ERROR_PEACEFUL.getMessage(kingdom.getName()));
			} else {
				kingdomScore = konquest.getKingdomManager().getKingdomScore(kingdom);
				playerScore = konquest.getKingdomManager().getPlayerScore(player);
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_SCORE_NOTICE_SCORE.getMessage(playerScore,kingdom.getName(),kingdomScore));
				// Display Score GUI
				konquest.getDisplayManager().displayScoreMenu(player, player);
			}
		} else {
			if (args.get(0).equalsIgnoreCase("all")) {
				// Score all Kingdoms
				ChatUtil.sendNotice(bukkitPlayer, ChatColor.GOLD + MessagePath.COMMAND_SCORE_NOTICE_ALL_HEADER.getMessage());
				for (KonKingdom allKingdom : konquest.getKingdomManager().getKingdoms()) {
					if (!kingdom.isPeaceful()) {
						int score = konquest.getKingdomManager().getKingdomScore(allKingdom);
						String color = "" + konquest.getDisplaySecondaryColor(player.getKingdom(), allKingdom);
						ChatUtil.sendMessage(bukkitPlayer, color + allKingdom.getName() + ChatColor.GOLD + ": " + ChatColor.DARK_PURPLE + score);
					}
				}
			} else {
				String playerName = args.get(0);
				// Verify player exists
				KonOfflinePlayer offlinePlayer = konquest.getPlayerManager().getOfflinePlayerFromName(playerName);
				if (offlinePlayer == null) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
					return;
				}
				kingdom = offlinePlayer.getKingdom();
				String offlinePlayerName = offlinePlayer.getOfflineBukkitPlayer().getName();
				if (offlinePlayer.isBarbarian()) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SCORE_ERROR_BARBARIAN.getMessage(offlinePlayerName));
				} else if (kingdom.isPeaceful()) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SCORE_ERROR_PEACEFUL.getMessage(kingdom.getName()));
				} else {
					kingdomScore = konquest.getKingdomManager().getKingdomScore(kingdom);
					playerScore = konquest.getKingdomManager().getPlayerScore(offlinePlayer);
					ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_SCORE_NOTICE_PLAYER.getMessage(offlinePlayerName, playerScore, kingdom.getName(), kingdomScore));
					// Display Score GUI
					konquest.getDisplayManager().displayScoreMenu(player, offlinePlayer);
				}
			}
		}
		
	}
	
	@Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		// Give suggestions
		if(args.size() == 1) {
			tabList.add("all");
			for(OfflinePlayer bukkitOfflinePlayer : konquest.getPlayerManager().getAllOfflinePlayers()) {
				tabList.add(bukkitOfflinePlayer.getName());
			}
		}
		return matchLastArgToList(tabList,args);
	}
}
