package konquest.command.admin;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class MakeTownAdminCommand extends CommandBase {
	
	public MakeTownAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	// k admin maketown town1 kingdom1
    	if (getArgs().length != 4) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	World bukkitWorld = bukkitPlayer.getWorld();

        	if(!bukkitWorld.getName().equals(getKonquest().getWorldName())) {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
                return;
        	}
        	
        	Location playerLoc = bukkitPlayer.getLocation();
        	String townName = getArgs()[2];
        	if(!StringUtils.isAlphanumeric(townName)) {
        		//ChatUtil.sendError((Player) getSender(), "Town name must only contain letters and/or numbers");
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_FORMAT_NAME.getMessage());
                return;
        	}
        	String kingdomName = getArgs()[3];
        	int exitCode = getKonquest().getKingdomManager().addTown(playerLoc, townName, kingdomName);
        	if(exitCode == 0) {
        		bukkitPlayer.teleport(getKonquest().getKingdomManager().getKingdom(kingdomName).getTown(townName).getSpawnLoc());
        		//ChatUtil.sendNotice((Player) getSender(), "Successfully created new Town: "+townName);
        		ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_SETTLE_NOTICE_SUCCESS.getMessage(townName));
        	} else {
        		switch(exitCode) {
        		case 1:
        			//ChatUtil.sendError((Player) getSender(), "Could not settle: Overlaps with a nearby territory.");
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_OVERLAP.getMessage());
        			break;
        		case 2:
        			//ChatUtil.sendError((Player) getSender(), "Could not settle: Couldn't find good Monument placement.");
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_PLACEMENT.getMessage());
        			break;
        		case 3:
        			//ChatUtil.sendError((Player) getSender(), "Could not settle: Invalid Town name, already taken or has spaces.");
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_NAME.getMessage());
        			break;
        		case 4:
        			//ChatUtil.sendError((Player) getSender(), "Could not settle: Invalid Monument Template. Use \"/k admin monument <kingdom> create\"");
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_TEMPLATE.getMessage());
        			break;
        		case 5:
        			int distance = getKonquest().getKingdomManager().getDistanceToClosestTerritory(bukkitPlayer.getLocation().getChunk());
        			int min_distance_capital = getKonquest().getConfigManager().getConfig("core").getInt("core.towns.min_distance_capital");
        			int min_distance_town = getKonquest().getConfigManager().getConfig("core").getInt("core.towns.min_distance_town");
        			int min_distance = 0;
        			if(min_distance_capital < min_distance_town) {
        				min_distance = min_distance_capital;
        			} else {
        				min_distance = min_distance_town;
        			}
        			//ChatUtil.sendError((Player) getSender(), "Could not settle: Too close to another territory.");
        			//ChatUtil.sendError((Player) getSender(), "Currently "+distance+" chunks away, must be at least "+min_distance+".");
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_PROXIMITY.getMessage(distance,min_distance));
        			break;
        		case 11:
        			//ChatUtil.sendError((Player) getSender(), "Could not settle: The 16x16 area is not flat enough. Press F3+G to view chunk boundaries. Cut down trees and flatten the ground!");
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_FLAT.getMessage());
        			break;
        		case 12:
        			//ChatUtil.sendError((Player) getSender(), "Could not settle: Elevation is too high. Try settling somewhere lower.");
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_HEIGHT.getMessage());
        			break;
        		case 13:
        			//ChatUtil.sendError((Player) getSender(), "Could not settle: Too much air or water below. Try settling somewhere else, or remove all air/water below you.");
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_AIR.getMessage());
        			break;
        		case 14:
        			//ChatUtil.sendError((Player) getSender(), "Could not settle: Problem claiming intial chunks.");
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_INIT.getMessage());
        			break;
        		default:
        			//ChatUtil.sendError((Player) getSender(), "Could not settle: Unknown cause. Contact an Admin!");
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
        			break;
        		}
        	}
        }
    }
    
    @Override
	public List<String> tabComplete() {
    	// k admin maketown town1 kingdom1
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 3) {
			tabList.addAll(Collections.emptyList());
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			List<String> kingdomList = getKonquest().getKingdomManager().getKingdomNames();
			tabList.addAll(kingdomList);
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		
		return matchedTabList;
	}
}
