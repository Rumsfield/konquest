package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.command.CommandType;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class BypassAdminCommand extends CommandBase {
	
	public BypassAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
        // k admin bypass
		Player bukkitPlayer = (Player) getSender();
    	if (getArgs().length != 2) {
			sendInvalidArgMessage(bukkitPlayer, AdminCommandType.BYPASS);
		} else {
        	if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	if(player.isAdminBypassActive()) {
        		player.setIsAdminBypassActive(false);
        		//ChatUtil.sendNotice((Player) getSender(), "Disabled admin bypass.");
        		ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_DISABLE_AUTO.getMessage());
        		ChatUtil.resetTitle(bukkitPlayer);
        	} else {
        		player.setIsAdminBypassActive(true);
        		//ChatUtil.sendNotice((Player) getSender(), "Enabled admin bypass, use this command again to disable.");
        		ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_ENABLE_AUTO.getMessage());
        		ChatUtil.sendConstantTitle(bukkitPlayer, "", ChatColor.GOLD+MessagePath.LABEL_BYPASS.getMessage());
        	}
        }
    }
    
    @Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
