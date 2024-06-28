package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class SaveAdminCommand extends CommandBase {
	
	public SaveAdminCommand() {
		// Define name and sender support
		super("save",false, true);
		// No arguments
    }

	@Override
    public void execute(Konquest konquest, CommandSender sender, List<String> args) {
		if (!args.isEmpty()) {
			sendInvalidArgMessage(sender);
			return;
		}
		// Save config files
		konquest.save();
		ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_SAVE_NOTICE_MESSAGE.getMessage());
    }
    
    @Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		// No arguments to complete
		return Collections.emptyList();
	}
}
