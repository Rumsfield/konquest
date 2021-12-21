package konquest.command;

import java.util.List;

import org.bukkit.command.CommandSender;

import konquest.Konquest;

public class GuildCommand extends CommandBase {

	// <guild> menu|create|add|kick|rename [name]
	
	public GuildCommand(Konquest konquest, CommandSender sender, String[] args) {
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
