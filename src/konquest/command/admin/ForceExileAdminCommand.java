package konquest.command.admin;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.command.CommandBase;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class ForceExileAdminCommand extends CommandBase {
	
	public ForceExileAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	// k admin forceexile player1 [full]
    	if (getArgs().length != 3 && getArgs().length != 4) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	String playerName = getArgs()[2];
        	
        	boolean doRemoveFavor = getKonquest().getConfigManager().getConfig("core").getBoolean("core.exile.remove_favor", true);
        	boolean doFullExile = false;
        	if(getArgs().length == 4 && getArgs()[3].equalsIgnoreCase("full")) {
        		doFullExile = true;
        		
        	}
        	
        	// Allow for exile of either online or offline player
        	// First, attempt to find online player
        	KonPlayer player = getKonquest().getPlayerManager().getPlayerFromName(playerName);
        	if(player == null) {
        		// No online player was found, attempt to find an offline player
        		KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayerFromName(playerName);
        		if(offlinePlayer == null) {
            		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
            		return;
            	} else {
            		// Found offline player, exile them
            		if(getKonquest().getKingdomManager().exileOfflinePlayer(offlinePlayer,doFullExile)) {
            			if(doRemoveFavor) {
                			double balance = KonquestPlugin.getBalance(offlinePlayer.getOfflineBukkitPlayer());
            	            KonquestPlugin.withdrawPlayer(offlinePlayer.getOfflineBukkitPlayer(), balance, true);
            			}
            			ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_FORCEEXILE_NOTICE_PLAYER.getMessage(playerName));
            			if(doFullExile) {
                			ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_FORCEEXILE_NOTICE_FULL.getMessage(playerName));
                		}
            		} else {
            			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_FORCEEXILE_ERROR_FAIL.getMessage());
                		return;
            		}
            	}
        	} else {
        		// Found online player, exile them
        		if(getKonquest().getKingdomManager().exilePlayer(player,true,true,doFullExile)) {
        			if(doRemoveFavor) {
            			double balance = KonquestPlugin.getBalance(player.getBukkitPlayer());
        	            KonquestPlugin.withdrawPlayer(player.getBukkitPlayer(), balance, true);
        			}
            		ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_EXILE_NOTICE_CONFIRMED.getMessage());
            		ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_FORCEEXILE_NOTICE_PLAYER.getMessage(playerName));
            		if(doFullExile) {
            			ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_FORCEEXILE_NOTICE_FULL.getMessage(playerName));
            		}
            	} else {
            		ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_FORCEEXILE_ERROR_FAIL.getMessage());
            		return;
            	}
        	}
        }
    }
    
    @Override
	public List<String> tabComplete() {
    	// k admin forceexile player1 [full]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 3) {
			List<String> playerList = new ArrayList<>();
			for(OfflinePlayer offlinePlayer : getKonquest().getPlayerManager().getAllOfflinePlayers()) {
				playerList.add(offlinePlayer.getName());
			}
			tabList.addAll(playerList);
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			tabList.add("full");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
