package konquest.command.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonPlayer;
import konquest.model.KonPlayer.RegionType;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class RuinAdminCommand extends CommandBase {

	public RuinAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

	@Override
	public void execute() {
		// k admin ruin create|remove|criticals|spawns <name>
		if (getArgs().length != 4) {
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        }
		Player bukkitPlayer = (Player) getSender();
//		if(getKonquest().isWorldIgnored(bukkitPlayer.getWorld())) {
//			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
//			return;
//		}
		if(!getKonquest().getPlayerManager().isPlayer(bukkitPlayer)) {
			ChatUtil.printDebug("Failed to find non-existent player");
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return;
		}
		KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
		String cmdMode = getArgs()[2];
		String ruinName = getArgs()[3];
		
		if(cmdMode.equalsIgnoreCase("create")) {
			Location playerLoc = bukkitPlayer.getLocation();
        	if(!StringUtils.isAlphanumeric(ruinName)) {
        		//ChatUtil.sendError((Player) getSender(), "Ruin name must only contain letters and/or numbers");
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_FORMAT_NAME.getMessage());
                return;
        	}
        	boolean pass = getKonquest().getRuinManager().addRuin(playerLoc, ruinName);
        	if(!pass) {
        		//ChatUtil.sendError((Player) getSender(), "Failed to create new Ruin: "+ruinName);
        		ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_RUIN_ERROR_CREATE.getMessage(ruinName));
                return;
        	} else {
        		//ChatUtil.sendNotice((Player) getSender(), "Successfully created new Ruin: "+ruinName);
        		ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_RUIN_NOTICE_CREATE.getMessage(ruinName));
        	}
		} else if(cmdMode.equalsIgnoreCase("remove")) {
			// Check for valid ruin
			if(!getKonquest().getRuinManager().isRuin(ruinName)) {
				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(ruinName));
                return;
			}
			boolean pass = getKonquest().getRuinManager().removeRuin(ruinName);
        	if(!pass) {
        		//ChatUtil.sendError((Player) getSender(), "Failed to remove Ruin: "+ruinName);
        		ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_RUIN_ERROR_REMOVE.getMessage(ruinName));
                return;
        	} else {
        		//ChatUtil.sendNotice((Player) getSender(), "Successfully removed Ruin: "+ruinName);
        		ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_RUIN_NOTICE_REMOVE.getMessage(ruinName));
        	}
		} else if(cmdMode.equalsIgnoreCase("criticals")) {
        	if(player.isSettingRegion()) {
        		//ChatUtil.sendError((Player) getSender(), "Cannot do this while setting regions");
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_REGION.getMessage());
                return;
        	}
        	// Check for valid ruin
			if(!getKonquest().getRuinManager().isRuin(ruinName)) {
				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(ruinName));
                return;
			}
			getKonquest().getRuinManager().getRuin(ruinName).clearCriticalLocations();
			player.settingRegion(RegionType.RUIN_CRITICAL);
        	//ChatUtil.sendNotice((Player) getSender(), "Removed all previous critical blocks from "+ruinName+", click on blocks to add.");
        	//ChatUtil.sendNotice((Player) getSender(), "Click on Air to cancel.");
        	ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_RUIN_NOTICE_CRITICALS.getMessage(ruinName));
        	ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_CLICK_AIR.getMessage());
		} else if(cmdMode.equalsIgnoreCase("spawns")) {
        	if(player.isSettingRegion()) {
        		//ChatUtil.sendError((Player) getSender(), "Cannot do this while setting regions");
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_REGION.getMessage());
                return;
        	}
        	// Check for valid ruin
			if(!getKonquest().getRuinManager().isRuin(ruinName)) {
				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(ruinName));
                return;
			}
			getKonquest().getRuinManager().getRuin(ruinName).clearSpawnLocations();
			player.settingRegion(RegionType.RUIN_SPAWN);
        	//ChatUtil.sendNotice((Player) getSender(), "Removed all previous spawn blocks from "+ruinName+", click on blocks to add.");
        	//ChatUtil.sendNotice((Player) getSender(), "Click on Air to cancel.");
        	ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_RUIN_NOTICE_SPAWNS.getMessage(ruinName));
        	ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_CLICK_AIR.getMessage());
		} else {
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
		}
	}

	@Override
	public List<String> tabComplete() {
		// k admin ruin create|remove|criticals|spawns <name>
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		if(getArgs().length == 3) {
			tabList.add("create");
			tabList.add("remove");
			tabList.add("criticals");
			tabList.add("spawns");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			String subCmd = getArgs()[2];
			if(subCmd.equalsIgnoreCase("create")) {
				tabList.add("***");
			} else if(subCmd.equalsIgnoreCase("remove") || subCmd.equalsIgnoreCase("criticals") || subCmd.equalsIgnoreCase("spawns")) {
				tabList.addAll(getKonquest().getRuinManager().getRuinNames());
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
	
}
