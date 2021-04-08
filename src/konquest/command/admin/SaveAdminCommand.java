package konquest.command.admin;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.utility.ChatUtil;
import konquest.utility.MessageStatic;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SaveAdminCommand extends CommandBase {
	
	public SaveAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	// k admin save
    	if (getArgs().length != 2) {
            ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	
        	getKonquest().getKingdomManager().saveKingdoms();
        	getKonquest().getKingdomManager().saveCamps();
        	getKonquest().getRuinManager().saveRuins();
        	//getKonquest().getPlayerManager().saveAllPlayers();
        	getKonquest().getConfigManager().saveConfigs();
        	
        	ChatUtil.sendNotice(bukkitPlayer, "Saved all kingdoms, camps and ruins.");
        }
    }
    
    @Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
