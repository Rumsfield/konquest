package konquest.command;

import konquest.Konquest;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessageStatic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class ClaimCommand extends CommandBase {

	public ClaimCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k claim [radius|auto] [<r>]
    	if (getArgs().length > 3) {
            ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	World bukkitWorld = bukkitPlayer.getWorld();
        	// Verify that this command is being used in the default world
        	if(!bukkitWorld.getName().equals(getKonquest().getWorldName())) {
        		ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_WORLD.toString());
                return;
        	}
        	// Verify no Barbarians are using this command
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	if(player.isBarbarian()) {
        		ChatUtil.sendError((Player) getSender(), "Barbarians cannot claim.");
                return;
        	}
        	
        	if(getArgs().length > 1) {
        		String claimMode = getArgs()[1];
        		switch(claimMode) {
        		case "radius" :
        			if(getArgs().length != 3) {
        				ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
        	            return;
        			}
    				int radius = Integer.parseInt(getArgs()[2]);
    				if(radius < 1 || radius > 5) {
    					ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
    					ChatUtil.sendError((Player) getSender(), "Radius must be greater than 0 and less than or equal to 5.");
    					return;
    				}
    				getKonquest().getKingdomManager().claimRadiusForPlayer(bukkitPlayer, bukkitPlayer.getLocation(), radius);
        			break;
        			
        		case "auto" :
        			if(player.isAdminClaimingFollow()) {
        				ChatUtil.sendNotice((Player) getSender(), "Cannot enable auto claim when admin auto claim is enabled.");
        				return;
        			}
        			if(!player.isClaimingFollow()) {
        				boolean isClaimSuccess = getKonquest().getKingdomManager().claimForPlayer(bukkitPlayer, bukkitPlayer.getLocation());
        				if(isClaimSuccess) {
        					ChatUtil.sendNotice((Player) getSender(), "Enabled auto claim. Use this command again to disable.");
        					player.setIsClaimingFollow(true);
        				}
        			} else {
        				player.setIsClaimingFollow(false);
        				ChatUtil.sendNotice((Player) getSender(), "Disabled auto claim.");
        			}
        			break;
        			
        		default :
        			ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
                    return;
        		}
        	} else {
        		// Claim the single chunk containing playerLoc for the adjacent territory.
        		getKonquest().getKingdomManager().claimForPlayer(bukkitPlayer, bukkitPlayer.getLocation());
        	}
        }
	}
	
	@Override
	public List<String> tabComplete() {
		// k claim [radius|auto] [<r>]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 2) {
			tabList.add("radius");
			tabList.add("auto");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		
		return matchedTabList;
	}
}