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

	public FavorCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k favor
		Player bukkitPlayer = (Player) getSender();
    	if (getArgs().length != 1) {
			sendInvalidArgMessage(bukkitPlayer,CommandType.FAVOR);
		} else {
        	if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);

        	double balance = KonquestPlugin.getBalance(bukkitPlayer);
        	double cost_spy = getKonquest().getCore().getDouble(CorePath.FAVOR_COST_SPY.getPath(),0.0);
        	double cost_settle = getKonquest().getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SETTLE.getPath(),0.0);
        	double cost_settle_incr = getKonquest().getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SETTLE_INCREMENT.getPath(),0.0);
        	int townCount = getKonquest().getKingdomManager().getPlayerLordships(player);
        	double cost_settle_adj = (((double)townCount)*cost_settle_incr) + cost_settle;
        	double cost_town_rename = getKonquest().getCore().getDouble(CorePath.FAVOR_TOWNS_COST_RENAME.getPath(),0.0);
        	double cost_claim = getKonquest().getCore().getDouble(CorePath.FAVOR_COST_CLAIM.getPath(),0.0);
        	double cost_travel = getKonquest().getCore().getDouble(CorePath.FAVOR_COST_TRAVEL.getPath(),0.0);
        	String balanceF = String.format("%.2f",balance);
        	ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_FAVOR_NOTICE_MESSAGE.getMessage(balanceF));
        	ChatUtil.sendMessage(bukkitPlayer, "Spy: "+ChatColor.AQUA+cost_spy);
			ChatUtil.sendMessage(bukkitPlayer, "Travel: "+ChatColor.AQUA+cost_travel);
			ChatUtil.sendMessage(bukkitPlayer, "Claim: "+ChatColor.AQUA+cost_claim);
        	ChatUtil.sendMessage(bukkitPlayer, "Town Settle: "+ChatColor.AQUA+cost_settle_adj);
        	ChatUtil.sendMessage(bukkitPlayer, "Town Rename: "+ChatColor.AQUA+cost_town_rename);
        	ChatUtil.sendMessage(bukkitPlayer, "Kingdom Create: "+ChatColor.AQUA+getKonquest().getKingdomManager().getCostCreate());
        	ChatUtil.sendMessage(bukkitPlayer, "Kingdom Rename: "+ChatColor.AQUA+getKonquest().getKingdomManager().getCostRename());
        }
	}
	
	@Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
