package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
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
    	if (getArgs().length != 2) {
            ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS_ADMIN.getMessage());
		} else {
        	Player bukkitPlayer = (Player) getSender();
        	
        	if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	if(player.isAdminBypassActive()) {
        		player.setIsAdminBypassActive(false);
        		//ChatUtil.sendNotice((Player) getSender(), "Disabled admin bypass.");
        		ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_DISABLE_AUTO.getMessage());
        		ChatUtil.resetTitle((Player) getSender());
        	} else {
        		player.setIsAdminBypassActive(true);
        		//ChatUtil.sendNotice((Player) getSender(), "Enabled admin bypass, use this command again to disable.");
        		ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_ENABLE_AUTO.getMessage());
        		ChatUtil.sendConstantTitle((Player) getSender(), "", ChatColor.GOLD+MessagePath.LABEL_BYPASS.getMessage());
        	}
        }
    }
    
    @Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
