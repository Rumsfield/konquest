package konquest.command.admin;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.command.CommandBase;
import konquest.model.KonOfflinePlayer;
import konquest.utility.ChatUtil;
import konquest.utility.CorePath;
import konquest.utility.MessagePath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
        	
        	boolean doRemoveFavor = getKonquest().getConfigManager().getConfig("core").getBoolean(CorePath.EXILE_REMOVE_FAVOR.getPath(), true);
        	boolean doFullExile = false;
        	if(getArgs().length == 4 && getArgs()[3].equalsIgnoreCase("full")) {
        		doFullExile = true;
        	}
        	
        	// Allow for exile of either online or offline player
        	// Try to get ID from name in all players
        	UUID id = null;
        	KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayerFromName(playerName);
        	if(offlinePlayer == null) {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
        		return;
        	} else {
        		id = offlinePlayer.getOfflineBukkitPlayer().getUniqueId();
        	}
        	
        	// Exile the player by ID
        	int status = getKonquest().getKingdomManager().exilePlayerBarbarian(id,true,true,doFullExile);
        	if(status == 0) {
        		if(doRemoveFavor) {
        			double balance = KonquestPlugin.getBalance(offlinePlayer.getOfflineBukkitPlayer());
    	            KonquestPlugin.withdrawPlayer(offlinePlayer.getOfflineBukkitPlayer(), balance, true);
    			}
        		if(offlinePlayer.getOfflineBukkitPlayer().isOnline()) {
        			Player bukkitPlayer = (Player)offlinePlayer.getOfflineBukkitPlayer();
        			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_EXILE_NOTICE_CONFIRMED.getMessage());
        		}
        		ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_FORCEEXILE_NOTICE_PLAYER.getMessage(playerName));
        		if(doFullExile) {
        			ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_FORCEEXILE_NOTICE_FULL.getMessage(playerName));
        		}
			} else if(status == 4) {
				//Do nothing
				ChatUtil.printDebug("Exile cancelled by event");
			} else if(status == 5) {
				ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_FORCEEXILE_ERROR_MASTER.getMessage());
			} else {
				ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_FORCEEXILE_ERROR_FAIL.getMessage());
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
