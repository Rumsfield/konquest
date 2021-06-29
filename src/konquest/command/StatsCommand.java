package konquest.command;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class StatsCommand extends CommandBase {

	public StatsCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k stats
    	if (getArgs().length != 1) {
            //ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
            ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	
        	if(!getKonquest().getPlayerManager().isPlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	getKonquest().getAccomplishmentManager().displayStats(player);
        }
	}

	@Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
