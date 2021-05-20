package konquest.command;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class KonquestCommand extends CommandBase{

	public KonquestCommand(Konquest konquest, CommandSender sender) {
        super(konquest, sender);
    }

    public void execute() {
        /*
    	getSender().sendMessage(new String[] {
                "Konquest",
                "By Rumsfield",
                "Use /k help for more information"
        });
        */
    	if(getSender() instanceof Player) {
    		Player bukkitPlayer = (Player) getSender();
    		getKonquest().getDisplayManager().displayHelpMenu(bukkitPlayer);
    	} else {
    		//ChatUtil.printConsole("You must be a player do use this command!");
    		ChatUtil.printConsoleError(MessagePath.GENERIC_ERROR_NO_PLAYER.getMessage());
    	}
    }
    
    @Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
