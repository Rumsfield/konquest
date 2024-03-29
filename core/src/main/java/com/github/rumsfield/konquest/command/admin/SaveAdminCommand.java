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
	
	public SaveAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	// k admin save
		Player bukkitPlayer = (Player) getSender();
    	if (getArgs().length != 2) {
			sendInvalidArgMessage(bukkitPlayer, AdminCommandType.SAVE);
        } else {

        	getKonquest().save();

        	ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_SAVE_NOTICE_MESSAGE.getMessage());
        }
    }
    
    @Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
