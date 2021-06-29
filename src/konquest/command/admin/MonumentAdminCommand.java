package konquest.command.admin;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonPlayer;
import konquest.model.KonPlayer.RegionType;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class MonumentAdminCommand extends CommandBase {
	
	public MonumentAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	// k admin monument kingdom1 create|remove|show
    	if (getArgs().length != 4) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	World bukkitWorld = bukkitPlayer.getWorld();

        	if(!getKonquest().isWorldValid(bukkitWorld)) {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
                return;
        	}
        	
        	if(!getKonquest().getPlayerManager().isPlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	if(player.isSettingRegion()) {
        		//ChatUtil.sendError((Player) getSender(), "Cannot do this while setting regions");
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_REGION.getMessage());
                return;
        	}
        	
        	String kingdomName = getArgs()[2];
        	if(!getKonquest().getKingdomManager().isKingdom(kingdomName)) {
        		//ChatUtil.sendError((Player) getSender(), "Bad Kingdom name");
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
                return;
        	}
        	kingdomName = getKonquest().getKingdomManager().getKingdom(kingdomName).getName();
        	String cmdMode = getArgs()[3];
        	if(cmdMode.equalsIgnoreCase("create")) {
        		player.settingRegion(RegionType.MONUMENT);
        		player.setRegionKingdomName(kingdomName);
            	//ChatUtil.sendNotice((Player) getSender(), "Now creating new Monument Template. Click on Air to cancel.");
            	//ChatUtil.sendNotice((Player) getSender(), "Click on the first corner block of the region.");
            	ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_CREATE_1.getMessage());
            	ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_CLICK_AIR.getMessage());
        	} else if(cmdMode.equalsIgnoreCase("remove")) {
        		player.settingRegion(RegionType.NONE);
        		getKonquest().getKingdomManager().getKingdom(kingdomName).removeMonumentTemplate();
        		//ChatUtil.sendNotice((Player) getSender(), "Removed Monument Template for kingdom "+kingdomName);
        		ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_REMOVE.getMessage(kingdomName));
        	} else if(cmdMode.equalsIgnoreCase("show")) {
        		boolean isValid = getKonquest().getKingdomManager().getKingdom(kingdomName).getMonumentTemplate().isValid();
        		if(isValid) {
        			player.settingRegion(RegionType.NONE);
        			Location loc0 = getKonquest().getKingdomManager().getKingdom(kingdomName).getMonumentTemplate().getCornerOne();
        			Location loc1 = getKonquest().getKingdomManager().getKingdom(kingdomName).getMonumentTemplate().getCornerTwo();
        			player.startMonumentShow(loc0, loc1);
        			//ChatUtil.sendNotice((Player) getSender(), "Showing existing Monument Template for kingdom "+kingdomName+", outlined in green particles.");
        			ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_SHOW.getMessage(kingdomName));
        		} else {
        			//ChatUtil.sendError((Player) getSender(), "There is no valid Monument Template for kingdom "+kingdomName);
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_MONUMENT_ERROR_NONE.getMessage(kingdomName));
        		}
        	} else {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
        	}
        }
    }
    
    @Override
	public List<String> tabComplete() {
    	// k admin monument kingdom1 create|remove
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 3) {
			List<String> kingdomList = getKonquest().getKingdomManager().getKingdomNames();
			tabList.addAll(kingdomList);
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			tabList.add("create");
			tabList.add("remove");
			tabList.add("show");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
