package konquest.command;

import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.utility.ChatUtil;

public class HelpCommand extends CommandBase{

	public HelpCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
        String message = "";
        ChatUtil.sendNotice((Player) getSender(), "Help: Command, Arguments, Description, Alias");
        for(CommandType cmd : CommandType.values()) {
        	String aliasmsg = "";
        	if(cmd.alias().equals("")==false) {
        		aliasmsg = (" Alias: "+cmd.alias());
        	}
        	/*
        	String cmdArgsFormatted = cmd.arguments();
        	cmdArgsFormatted.replaceAll("<", ChatColor.GRAY+"*"+ChatColor.AQUA);
        	cmdArgsFormatted.replaceAll(">", ChatColor.GRAY+"*"+ChatColor.AQUA);
        	cmdArgsFormatted.replaceAll("|", ChatColor.GRAY+"*"+ChatColor.AQUA);
        	cmdArgsFormatted.replaceAll("//]", ChatColor.GRAY+"*"+ChatColor.AQUA);
        	cmdArgsFormatted.replaceAll("//[", ChatColor.GRAY+"*"+ChatColor.AQUA);
        	*/
        	message = ChatColor.GOLD+"/k "+cmd.toString().toLowerCase()+" "+ChatColor.AQUA+cmd.arguments()+ChatColor.GRAY+": "+cmd.description()+ChatColor.LIGHT_PURPLE+aliasmsg;
        	ChatUtil.sendMessage((Player) getSender(), message);
        }
    }
    
    @Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
