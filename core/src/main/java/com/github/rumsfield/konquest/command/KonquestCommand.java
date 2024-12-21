package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class KonquestCommand extends CommandBase{

	public KonquestCommand() {
		super("",false, false);
		// No arguments
    }

	@Override
    public void execute(Konquest konquest, CommandSender sender, List<String> args) {
		// Display help GUI menu to players.
		// Display logo and tip for console.
		if (sender instanceof Player) {
			KonPlayer displayPlayer = konquest.getPlayerManager().getPlayer((Player) sender);
			if (displayPlayer != null) {
				konquest.getDisplayManager().displayHelpMenu(displayPlayer);
			}
		} else if (sender instanceof ConsoleCommandSender) {
			KonquestPlugin.printLogo();
			ChatUtil.sendNotice(sender,"Suggested console commands:");
			ChatUtil.sendNotice(sender,"  k version");
			ChatUtil.sendNotice(sender,"  k reload");
			ChatUtil.sendNotice(sender,"  k save");
			ChatUtil.sendNotice(sender,"  k help");
		}
    }
    
    @Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		// No arguments to complete
		return Collections.emptyList();
	}
}
