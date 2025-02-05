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
	
	public BypassAdminCommand() {
		// Define name and sender support
		super("bypass",true, true);
		// No Arguments
    }

	@Override
    public void execute(Konquest konquest, CommandSender sender, List<String> args) {
		// Sender must be player
		KonPlayer player = konquest.getPlayerManager().getPlayer(sender);
		if (player == null) {
			sendInvalidSenderMessage(sender);
			return;
		}
		if (!args.isEmpty()) {
			sendInvalidArgMessage(sender);
			return;
		}
		if(player.isAdminBypassActive()) {
			player.setIsAdminBypassActive(false);
			ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_DISABLE_AUTO.getMessage());
		} else {
			player.setIsAdminBypassActive(true);
			ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_ENABLE_AUTO.getMessage());
		}
    }
    
    @Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		// No arguments to complete
		return Collections.emptyList();
	}
}
