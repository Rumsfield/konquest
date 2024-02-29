package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class ChatCommand extends CommandBase {

	public ChatCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k chat
		Player bukkitPlayer = (Player) getSender();
    	if (getArgs().length != 1) {
			sendInvalidArgMessage(bukkitPlayer,CommandType.CHAT);
			return;
		}
		if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
			ChatUtil.printDebug("Failed to find non-existent player");
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return;
		}
		KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
		assert player != null;
		boolean isChatFormatEnabled = getKonquest().getCore().getBoolean(CorePath.CHAT_ENABLE_FORMAT.getPath(),true);
		if(!isChatFormatEnabled) {
			// Chat formatting is disabled, so kingdom chat is unavailable
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
			player.setIsGlobalChat(true);
		}
		if(!player.isBarbarian()) {
			if(player.isGlobalChat()) {
				//ChatUtil.sendNotice(bukkitPlayer, "Chat mode: Kingdom");
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_CHAT_NOTICE_ENABLE.getMessage());
				player.setIsGlobalChat(false);
			} else {
				//ChatUtil.sendNotice(bukkitPlayer, "Chat mode: Global");
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_CHAT_NOTICE_DISABLE.getMessage());
				player.setIsGlobalChat(true);
			}
		} else {
			//ChatUtil.sendError(bukkitPlayer, "Cannot use Kingdom chat as Barbarian");
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
		}

	}

	@Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
