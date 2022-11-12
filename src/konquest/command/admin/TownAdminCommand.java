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
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<String> tabComplete() {
		// TODO Auto-generated method stub
		return null;
	}
}
