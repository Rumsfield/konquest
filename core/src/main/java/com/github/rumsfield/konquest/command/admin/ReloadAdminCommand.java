package com.github.rumsfield.konquest.command.admin;

import java.util.Collections;
import java.util.List;

import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.utility.ChatUtil;

public class ReloadAdminCommand extends CommandBase {

	public ReloadAdminCommand() {
		// Define name and sender support
		super("reload",false, true);
		// No arguments
    }

	@Override
	public void execute(Konquest konquest, CommandSender sender, List<String> args) {
		if (!args.isEmpty()) {
			sendInvalidArgMessage(sender);
			return;
		}
		// Reload config files
		konquest.reload();
		ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_RELOAD_NOTICE_MESSAGE.getMessage());
    }
    
    @Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		// No arguments to complete
		return Collections.emptyList();
	}
	
	
}
