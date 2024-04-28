package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class FavorCommand extends CommandBase {

	public FavorCommand() {
		// Define name and sender support
		super("favor",true, false);
		// No Arguments
    }

	@Override
	public void execute(Konquest konquest, CommandSender sender, List<String> args) {
		// Sender must be player
		KonPlayer player = konquest.getPlayerManager().getPlayer(sender);
		if (player == null) {
			sendInvalidSenderMessage(sender);
			return;
		}

		if (!args.isEmpty()) {
			sendInvalidArgMessage(sender);
			return;
		}
		Player bukkitPlayer = player.getBukkitPlayer();
		double balance = KonquestPlugin.getBalance(bukkitPlayer);
		double cost_spy = konquest.getCore().getDouble(CorePath.FAVOR_COST_SPY.getPath(),0.0);
		double cost_settle = konquest.getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SETTLE.getPath(),0.0);
		double cost_settle_incr = konquest.getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SETTLE_INCREMENT.getPath(),0.0);
		int townCount = konquest.getKingdomManager().getPlayerLordships(player);
		double cost_settle_adj = (((double)townCount)*cost_settle_incr) + cost_settle;
		double cost_town_rename = konquest.getCore().getDouble(CorePath.FAVOR_TOWNS_COST_RENAME.getPath(),0.0);
		double cost_claim = konquest.getCore().getDouble(CorePath.FAVOR_COST_CLAIM.getPath(),0.0);
		double cost_travel = konquest.getCore().getDouble(CorePath.FAVOR_COST_TRAVEL.getPath(),0.0);
		double cost_kingdom_create = konquest.getKingdomManager().getCostCreate();
		double cost_kingdom_rename = konquest.getKingdomManager().getCostRename();
		String balanceF = String.format("%.2f",balance);
		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_FAVOR_NOTICE_MESSAGE.getMessage(balanceF));
		ChatUtil.sendMessage(bukkitPlayer, MessagePath.COMMAND_FAVOR_NOTICE_COST_SPY.getMessage() + 			" - "+ChatColor.AQUA + KonquestPlugin.getCurrencyFormat(cost_spy));
		ChatUtil.sendMessage(bukkitPlayer, MessagePath.COMMAND_FAVOR_NOTICE_COST_TRAVEL.getMessage() +			" - "+ChatColor.AQUA + KonquestPlugin.getCurrencyFormat(cost_travel));
		ChatUtil.sendMessage(bukkitPlayer, MessagePath.COMMAND_FAVOR_NOTICE_COST_CLAIM.getMessage() +			" - "+ChatColor.AQUA + KonquestPlugin.getCurrencyFormat(cost_claim));
		ChatUtil.sendMessage(bukkitPlayer, MessagePath.COMMAND_FAVOR_NOTICE_COST_TOWN_SETTLE.getMessage() +		" - "+ChatColor.AQUA + KonquestPlugin.getCurrencyFormat(cost_settle_adj));
		ChatUtil.sendMessage(bukkitPlayer, MessagePath.COMMAND_FAVOR_NOTICE_COST_TOWN_RENAME.getMessage() +		" - "+ChatColor.AQUA + KonquestPlugin.getCurrencyFormat(cost_town_rename));
		ChatUtil.sendMessage(bukkitPlayer, MessagePath.COMMAND_FAVOR_NOTICE_COST_KINGDOM_CREATE.getMessage() +	" - "+ChatColor.AQUA + KonquestPlugin.getCurrencyFormat(cost_kingdom_create));
		ChatUtil.sendMessage(bukkitPlayer, MessagePath.COMMAND_FAVOR_NOTICE_COST_KINGDOM_RENAME.getMessage() +	" - "+ChatColor.AQUA + KonquestPlugin.getCurrencyFormat(cost_kingdom_rename));
        
	}
	
	@Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		// No arguments to complete
		return Collections.emptyList();
	}
}
