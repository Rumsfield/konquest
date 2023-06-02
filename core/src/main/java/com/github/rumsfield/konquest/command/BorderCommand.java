package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.Labeler;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class BorderCommand extends CommandBase {

	public BorderCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k border
		Player bukkitPlayer = (Player) getSender();
    	if (getArgs().length != 1) {
			sendInvalidArgMessage(bukkitPlayer,CommandType.BORDER);
			return;
		}
		if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
			ChatUtil.printDebug("Failed to find non-existent player");
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
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

	@Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
