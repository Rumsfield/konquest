package com.github.rumsfield.konquest.command.admin;

import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
//import com.github.rumsfield.konquest.command.admin.AdminCommandType;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;

public class HelpAdminCommand extends CommandBase {
	
	public HelpAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
        String message = "";
        //ChatUtil.sendNotice((Player) getSender(), "Admin Help: Command, Arguments, Description");
        ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_HELP_NOTICE_MESSAGE.getMessage());
        for(AdminCommandType cmd : AdminCommandType.values()) {
        	
        	String cmdArgsFormatted = cmd.arguments();
        	cmdArgsFormatted = cmdArgsFormatted.replaceAll("<", ChatColor.GRAY+"<"+ChatColor.AQUA);
        	cmdArgsFormatted = cmdArgsFormatted.replaceAll(">", ChatColor.GRAY+">"+ChatColor.AQUA);
        	cmdArgsFormatted = cmdArgsFormatted.replaceAll("\\|", ChatColor.GRAY+"|"+ChatColor.AQUA);
        	cmdArgsFormatted = cmdArgsFormatted.replaceAll("\\]", ChatColor.GRAY+"]"+ChatColor.AQUA);
        	cmdArgsFormatted = cmdArgsFormatted.replaceAll("\\[", ChatColor.GRAY+"["+ChatColor.AQUA);
        	
        	message = ChatColor.GOLD+"/k admin "+cmd.toString().toLowerCase()+" "+ChatColor.AQUA+cmdArgsFormatted+": "+ChatColor.WHITE+cmd.description();
            ChatUtil.sendMessage((Player) getSender(), message);
        }
    }
    
    @Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
