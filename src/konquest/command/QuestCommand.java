package konquest.command;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessageStatic;

public class QuestCommand extends CommandBase {

	public QuestCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k quest
    	if (getArgs().length != 1) {
            ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
            return;
        } else {
        	// Check for global enable
        	if(!getKonquest().getDirectiveManager().isEnabled()) {
        		ChatUtil.sendError((Player) getSender(), "Quests are currently disabled, talk to an admin.");
        		return;
        	}
        	
        	Player bukkitPlayer = (Player) getSender();
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	getKonquest().getDirectiveManager().displayBook(player);
        }
	}

	@Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
