package konquest.command;

import konquest.Konquest;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessageStatic;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChatCommand extends CommandBase {

	public ChatCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k chat
    	if (getArgs().length != 1) {
            ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	/*
        	World bukkitWorld = bukkitPlayer.getWorld();

        	if(!bukkitWorld.getName().equals(getKonquest().getWorldName())) {
        		ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_WORLD.toString());
                return;
        	}*/
        	
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	if(!player.isBarbarian()) {
	        	if(player.isGlobalChat()) {
	        		ChatUtil.sendNotice(bukkitPlayer, "Chat mode: Kingdom");
	        		player.setIsGlobalChat(false);
	        	} else {
	        		ChatUtil.sendNotice(bukkitPlayer, "Chat mode: Global");
	        		player.setIsGlobalChat(true);
	        	}
        	} else {
        		ChatUtil.sendError(bukkitPlayer, "Cannot use Kingdom chat as Barbarian");
        	}
        }
	}

	@Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
