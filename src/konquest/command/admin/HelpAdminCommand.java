package konquest.command.admin;

import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.command.CommandBase;
//import konquest.command.admin.AdminCommandType;
import konquest.utility.ChatUtil;

public class HelpAdminCommand extends CommandBase {
	
	public HelpAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
        String message = "";
        ChatUtil.sendNotice((Player) getSender(), "Admin Help: Command, Arguments, Description");
        for(AdminCommandType cmd : AdminCommandType.values()) {
        	message = ChatColor.GOLD+"/k admin "+cmd.toString().toLowerCase()+" "+ChatColor.AQUA+cmd.arguments()+": "+ChatColor.GRAY+cmd.description();
            ChatUtil.sendMessage((Player) getSender(), message);
        }
    }
    
    @Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
