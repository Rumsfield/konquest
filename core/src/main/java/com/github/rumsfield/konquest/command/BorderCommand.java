package com.github.rumsfield.konquest.command;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;

public class BorderCommand extends CommandBase {

	public BorderCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k border
    	if (getArgs().length != 1) {
            ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	if(player.isBorderDisplay()) {
        		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_BORDER_NOTICE_DISABLE.getMessage());
        		player.setIsBorderDisplay(false);
        		getKonquest().getTerritoryManager().stopPlayerBorderParticles(player);
        	} else {
        		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_BORDER_NOTICE_ENABLE.getMessage());
        		player.setIsBorderDisplay(true);
        		getKonquest().getTerritoryManager().updatePlayerBorderParticles(player);
        	}
        }
	}

	@Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
