package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class KonquestCommand extends CommandBase{

	public KonquestCommand(Konquest konquest, CommandSender sender) {
        super(konquest, sender);
    }

    public void execute() {
		if(!(getSender() instanceof Player)){
			ChatUtil.printConsoleError(MessagePath.GENERIC_ERROR_NO_PLAYER.getMessage());
			return;
		}

		getKonquest().getDisplayManager().displayHelpMenu((Player) getSender());
    }
    
    @Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
