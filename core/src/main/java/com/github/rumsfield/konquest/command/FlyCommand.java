package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonTerritory;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class FlyCommand extends CommandBase {

	public FlyCommand() {
		// Define name and sender support
		super("fly",true, false);
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
		// Toggle player flying
		konquest.getPlayerManager().togglePlayerFly(player);
	}
	
	@Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		// No arguments to complete
		return Collections.emptyList();
	}
	
}
