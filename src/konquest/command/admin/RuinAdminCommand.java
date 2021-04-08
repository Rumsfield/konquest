package konquest.command.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonPlayer;
import konquest.model.KonPlayer.RegionType;
import konquest.utility.ChatUtil;
import konquest.utility.MessageStatic;

public class RuinAdminCommand extends CommandBase {

	public RuinAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

	@Override
	public void execute() {
		// k admin ruin create|remove|criticals|spawns <name>
		if (getArgs().length != 4) {
            ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
            return;
        }
		Player bukkitPlayer = (Player) getSender();
		String cmdMode = getArgs()[2];
		String ruinName = getArgs()[3];
		
		if(cmdMode.equalsIgnoreCase("create")) {
			Location playerLoc = bukkitPlayer.getLocation();
        	if(!StringUtils.isAlphanumeric(ruinName)) {
        		ChatUtil.sendError((Player) getSender(), "Ruin name must only contain letters and/or numbers");
                return;
        	}
        	boolean pass = getKonquest().getRuinManager().addRuin(playerLoc, ruinName);
        	if(!pass) {
        		ChatUtil.sendError((Player) getSender(), "Failed to create new Ruin: "+ruinName);
                return;
        	} else {
        		ChatUtil.sendNotice((Player) getSender(), "Successfully created new Ruin: "+ruinName);
        	}
		} else if(cmdMode.equalsIgnoreCase("remove")) {
			// Check for valid ruin
			if(!getKonquest().getRuinManager().isRuin(ruinName)) {
				ChatUtil.sendError((Player) getSender(), MessageStatic.BAD_NAME.toString());
                return;
			}
			boolean pass = getKonquest().getRuinManager().removeRuin(ruinName);
        	if(!pass) {
        		ChatUtil.sendError((Player) getSender(), "Failed to remove Ruin: "+ruinName);
                return;
        	} else {
        		ChatUtil.sendNotice((Player) getSender(), "Successfully removed Ruin: "+ruinName);
        	}
		} else if(cmdMode.equalsIgnoreCase("criticals")) {
			KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	if(player.isSettingRegion()) {
        		ChatUtil.sendError((Player) getSender(), "Cannot do this while setting regions");
                return;
        	}
        	// Check for valid ruin
			if(!getKonquest().getRuinManager().isRuin(ruinName)) {
				ChatUtil.sendError((Player) getSender(), MessageStatic.BAD_NAME.toString());
                return;
			}
			getKonquest().getRuinManager().getRuin(ruinName).clearCriticalLocations();
			player.settingRegion(RegionType.RUIN_CRITICAL);
        	ChatUtil.sendNotice((Player) getSender(), "Removed all previous critical blocks from "+ruinName+", click on blocks to add.");
        	ChatUtil.sendNotice((Player) getSender(), "Click on Air to cancel.");
			
		} else if(cmdMode.equalsIgnoreCase("spawns")) {
			KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	if(player.isSettingRegion()) {
        		ChatUtil.sendError((Player) getSender(), "Cannot do this while setting regions");
                return;
        	}
        	// Check for valid ruin
			if(!getKonquest().getRuinManager().isRuin(ruinName)) {
				ChatUtil.sendError((Player) getSender(), MessageStatic.BAD_NAME.toString());
                return;
			}
			getKonquest().getRuinManager().getRuin(ruinName).clearSpawnLocations();
			player.settingRegion(RegionType.RUIN_SPAWN);
        	ChatUtil.sendNotice((Player) getSender(), "Removed all previous spawn blocks from "+ruinName+", click on blocks to add.");
        	ChatUtil.sendNotice((Player) getSender(), "Click on Air to cancel.");
		} else {
			ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
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
			if(subCmd.equalsIgnoreCase("remove") || subCmd.equalsIgnoreCase("criticals") || subCmd.equalsIgnoreCase("spawns")) {
				tabList.addAll(getKonquest().getRuinManager().getRuinNames());
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
	
}
