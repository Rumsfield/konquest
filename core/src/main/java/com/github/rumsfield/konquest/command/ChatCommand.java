package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChatCommand extends CommandBase {

	public ChatCommand() {
		// Define name and sender support
		super("chat",true, false);
		// None
		setOptionalArgs(true);
		// [global|kingdom]
		List<String> argNames = Arrays.asList("global", "kingdom");
		addArgument(
				newArg(argNames,true,false)
		);
    }

	@Override
	public void execute(Konquest konquest, CommandSender sender, List<String> args) {
		// Sender must be player
		KonPlayer player = konquest.getPlayerManager().getPlayer(sender);
		if (player == null) {
			sendInvalidSenderMessage(sender);
			return;
		}

		// Check for disabled feature
		boolean isChatFormatEnabled = konquest.getCore().getBoolean(CorePath.CHAT_ENABLE_FORMAT.getPath(),true);
		if (!isChatFormatEnabled) {
			// Chat formatting is disabled, so kingdom chat is unavailable
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
			player.setIsGlobalChat(true);
			return;
		}
		// Check for barbarian player (no kingdom)
		if (player.isBarbarian()) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
			return;
		}

		if (args.isEmpty()) {
			// Toggle between chats
			if(player.isGlobalChat()) {
				// Kingdom Chat
				ChatUtil.sendNotice(sender, MessagePath.COMMAND_CHAT_NOTICE_ENABLE.getMessage());
				player.setIsGlobalChat(false);
			} else {
				// Global Chat
				ChatUtil.sendNotice(sender, MessagePath.COMMAND_CHAT_NOTICE_DISABLE.getMessage());
				player.setIsGlobalChat(true);
			}
		} else {
			// Set specific chat mode
			String chatMode = args.get(0);
			switch (chatMode.toLowerCase()) {
				case "kingdom":
					// Kingdom Chat
					ChatUtil.sendNotice(sender, MessagePath.COMMAND_CHAT_NOTICE_ENABLE.getMessage());
					player.setIsGlobalChat(false);
					break;
				case "global":
					// Global Chat
					ChatUtil.sendNotice(sender, MessagePath.COMMAND_CHAT_NOTICE_DISABLE.getMessage());
					player.setIsGlobalChat(true);
					break;
				default:
					sendInvalidArgMessage(sender);
					return;
			}
		}

	}

	@Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		// Give suggestions
		if(args.size() == 1) {
			tabList.add("global");
			tabList.add("kingdom");
		}
		return matchLastArgToList(tabList,args);
	}
}
