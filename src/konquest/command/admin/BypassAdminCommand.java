package konquest.command.admin;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessageStatic;

import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BypassAdminCommand extends CommandBase {
	
	public BypassAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
        // k admin bypass
    	if (getArgs().length != 2) {
            ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	if(player.isAdminBypassActive()) {
        		player.setIsAdminBypassActive(false);
        		ChatUtil.sendNotice((Player) getSender(), "Disabled admin bypass mode");
        		ChatUtil.resetTitle((Player) getSender());
        	} else {
        		player.setIsAdminBypassActive(true);
        		ChatUtil.sendNotice((Player) getSender(), "Enabled admin bypass mode");
        		ChatUtil.sendConstantTitle((Player) getSender(), "", ChatColor.GOLD+"BYPASS");
        	}
        }
    }
    
    @Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
