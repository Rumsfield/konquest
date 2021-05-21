package konquest.command.admin;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class RemoveKingdomAdminCommand extends CommandBase {
	
	public RemoveKingdomAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	// k admin removekingdom kingdom1
    	if (getArgs().length != 3) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	World bukkitWorld = bukkitPlayer.getWorld();

        	if(!bukkitWorld.getName().equals(getKonquest().getWorldName())) {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
                return;
        	}
        	
        	String kingdomName = getArgs()[2];
        	boolean pass = getKonquest().getKingdomManager().removeKingdom(kingdomName);
        	if(!pass) {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
                return;
        	} else {
        		//ChatUtil.sendNotice((Player) getSender(), "Successfully removed Kingdom: "+kingdomName);
        		ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
        	}
        }
    }
    
    @Override
	public List<String> tabComplete() {
    	// k admin removekingdom kingdom1
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 3) {
			List<String> kingdomList = getKonquest().getKingdomManager().getKingdomNames();
			tabList.addAll(kingdomList);
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
