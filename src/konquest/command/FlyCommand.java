package konquest.command;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;

import konquest.Konquest;

public class FlyCommand extends CommandBase {

	public FlyCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k fly
	}
	
	@Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
	
}
