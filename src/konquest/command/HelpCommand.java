package konquest.command;

import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class HelpCommand extends CommandBase{

	public HelpCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
        String message = "";
        //ChatUtil.sendNotice((Player) getSender(), "Help: Command, Arguments, Description, Alias");
        ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_HELP_NOTICE_MESSAGE.getMessage());
        for(CommandType cmd : CommandType.values()) {
        	String aliasmsg = "";
        	if(cmd.alias().equals("")==false) {
        		aliasmsg = (" Alias: "+cmd.alias());
        	}
        	
        	String cmdArgsFormatted = cmd.arguments();
        	cmdArgsFormatted = cmdArgsFormatted.replaceAll("<", ChatColor.GRAY+"<"+ChatColor.AQUA);
        	cmdArgsFormatted = cmdArgsFormatted.replaceAll(">", ChatColor.GRAY+">"+ChatColor.AQUA);
        	cmdArgsFormatted = cmdArgsFormatted.replaceAll("\\|", ChatColor.GRAY+"|"+ChatColor.AQUA);
        	cmdArgsFormatted = cmdArgsFormatted.replaceAll("\\]", ChatColor.GRAY+"]"+ChatColor.AQUA);
        	cmdArgsFormatted = cmdArgsFormatted.replaceAll("\\[", ChatColor.GRAY+"["+ChatColor.AQUA);
        	
        	message = ChatColor.GOLD+"/k "+cmd.toString().toLowerCase()+" "+ChatColor.AQUA+cmdArgsFormatted+ChatColor.WHITE+": "+cmd.description()+ChatColor.LIGHT_PURPLE+aliasmsg;
        	ChatUtil.sendMessage((Player) getSender(), message);
        }
    }
    
    @Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
