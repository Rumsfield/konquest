package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SanctuaryAdminCommand extends CommandBase {

	public SanctuaryAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

	@Override
	public void execute() {
		// k admin sanctuary create|remove|rename <name> [<name>]
		if (getArgs().length != 4 && getArgs().length != 5) {
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        }
		Player bukkitPlayer = (Player) getSender();
		if(getKonquest().isWorldIgnored(bukkitPlayer.getWorld())) {
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
			return;
		}
		if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
			ChatUtil.printDebug("Failed to find non-existent player");
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return;
		}

		String cmdMode = getArgs()[2];
		String name = getArgs()[3];
		
		if(cmdMode.equalsIgnoreCase("create")) {
			if (getArgs().length != 4) {
				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
				return;
			}
			Location playerLoc = bukkitPlayer.getLocation();
			if(getKonquest().validateName(name,bukkitPlayer) != 0) {
        		return;
        	}
        	boolean pass = getKonquest().getSanctuaryManager().addSanctuary(playerLoc, name);
        	if(!pass) {
        		ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_SANCTUARY_ERROR_CREATE.getMessage(name));
			} else {
        		ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_SANCTUARY_NOTICE_CREATE.getMessage(name));
        		// Render border particles
        		KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        		getKonquest().getTerritoryManager().updatePlayerBorderParticles(player);
        	}
		} else if(cmdMode.equalsIgnoreCase("remove")) {
			if(!getKonquest().getSanctuaryManager().isSanctuary(name)) {
				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(name));
                return;
			}
			boolean pass = getKonquest().getSanctuaryManager().removeSanctuary(name);
        	if(!pass) {
        		ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_SANCTUARY_ERROR_REMOVE.getMessage(name));
			} else {
        		ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_SANCTUARY_NOTICE_REMOVE.getMessage(name));
        	}
		} else if(cmdMode.equalsIgnoreCase("rename")) {
			if (getArgs().length == 5) {
				if(!getKonquest().getSanctuaryManager().isSanctuary(name)) {
					ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(name));
	                return;
				}
				String newName = getArgs()[4];
				if(getKonquest().validateName(newName,bukkitPlayer) != 0) {
	        		return;
	        	}
				boolean pass = getKonquest().getSanctuaryManager().renameSanctuary(name,newName);
				if(!pass) {
	        		ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_SANCTUARY_ERROR_RENAME.getMessage(name,newName));
				} else {
	        		ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_SANCTUARY_NOTICE_RENAME.getMessage(name,newName));
	        	}
			} else {
				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
			}
		} else {
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
		}
	}
	
	@Override
	public List<String> tabComplete() {
		// k admin sanctuary create|remove|rename <name> [<name>]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		if(getArgs().length == 3) {
			tabList.add("create");
			tabList.add("remove");
			tabList.add("rename");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			String subCmd = getArgs()[2];
			if(subCmd.equalsIgnoreCase("create")) {
				tabList.add("***");
			} else if(subCmd.equalsIgnoreCase("remove") || subCmd.equalsIgnoreCase("rename")) {
				tabList.addAll(getKonquest().getSanctuaryManager().getSanctuaryNames());
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 5) {
			String subCmd = getArgs()[2];
			if(subCmd.equalsIgnoreCase("rename")) {
				tabList.add("***");
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[4], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
	
}
