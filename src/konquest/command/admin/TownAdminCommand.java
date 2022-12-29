package konquest.command.admin;

import java.util.List;

import org.bukkit.command.CommandSender;

import konquest.Konquest;
import konquest.command.CommandBase;

public class TownAdminCommand extends CommandBase {

	public TownAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

	@Override
	public void execute() {
		// k admin town menu|create|remove|add|kick|rename|takeover <town> [<name>]
		//TODO KR implement town menus
	}

	@Override
	public List<String> tabComplete() {
		// k admin town menu|create|remove|add|kick|rename|takeover <town> [<name>]
		
		return null;
	}
}
