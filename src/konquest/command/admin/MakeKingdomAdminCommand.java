package konquest.command.admin;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;


public class MakeKingdomAdminCommand extends CommandBase {
	
	public MakeKingdomAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
        // k admin makekingdom kingdom1
    	if (getArgs().length != 3) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	World bukkitWorld = bukkitPlayer.getWorld();

        	if(!getKonquest().getPlayerManager().isPlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	
        	if(!getKonquest().isWorldValid(bukkitWorld)) {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
                return;
        	}
        	
        	Location playerLoc = bukkitPlayer.getLocation();
        	String kingdomName = getArgs()[2];
        	if(!StringUtils.isAlphanumeric(kingdomName)) {
        		//ChatUtil.sendError((Player) getSender(), "Kingdom name must only contain letters and/or numbers");
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_FORMAT_NAME.getMessage());
                return;
        	}
        	boolean pass = getKonquest().getKingdomManager().addKingdom(playerLoc, kingdomName);
        	if(!pass) {
        		ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_MAKEKINGDOM_ERROR_NAME.getMessage());
                return;
        	} else {
        		//ChatUtil.sendNotice((Player) getSender(), "Successfully created new Kingdom Capital for "+kingdomName+".");
        		//ChatUtil.sendNotice((Player) getSender(), "Claim additional land with \"/k admin claim\".");
        		//ChatUtil.sendNotice((Player) getSender(), "Set the capital spawn point with \"/k admin settravel\".");
        		//ChatUtil.sendNotice((Player) getSender(), "Next, set up the Kingdom Monument with \"/k admin monument "+kingdomName+" create\".");
        		ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_MAKEKINGDOM_NOTICE_PROMPT_1.getMessage(kingdomName));
        		ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_MAKEKINGDOM_NOTICE_PROMPT_2.getMessage());
        		ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_MAKEKINGDOM_NOTICE_PROMPT_3.getMessage());
        		ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_MAKEKINGDOM_NOTICE_PROMPT_4.getMessage(kingdomName));
        		// Render border particles
        		KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        		getKonquest().getKingdomManager().updatePlayerBorderParticles(player, playerLoc);
        	}
        }
    }
    
    @Override
	public List<String> tabComplete() {
    	// k admin makekingdom ***
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 3) {
			tabList.add("***");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
