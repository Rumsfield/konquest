package com.github.rumsfield.konquest.command;



import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class PrefixCommand extends CommandBase {

	public PrefixCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k prefix
		Player bukkitPlayer = (Player) getSender();
    	if (getArgs().length != 1) {
			sendInvalidArgMessage(bukkitPlayer,CommandType.PREFIX);
		} else {
        	if(!getKonquest().getAccomplishmentManager().isEnabled()) {
        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
        		return;
        	}
        	if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);

        	if(player.isBarbarian()) {
        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
        		return;
        	}
        	
        	getKonquest().getDisplayManager().displayPrefixMenu(player);

		}
	}

	@Override
	public List<String> tabComplete() {
		// k prefix
		// No arguments to complete
		return Collections.emptyList();
	}
}
