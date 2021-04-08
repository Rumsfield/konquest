package konquest.command.admin;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.utility.ChatUtil;

public class ReloadAdminCommand extends CommandBase {

	public ReloadAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		getKonquest().reload();
        ChatUtil.sendNotice((Player) getSender(), "Reloaded Konquest configuration files.");
    }
    
    @Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
	
	
}
