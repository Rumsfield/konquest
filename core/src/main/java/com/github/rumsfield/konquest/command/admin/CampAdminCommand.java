package com.github.rumsfield.konquest.command.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.model.KonOfflinePlayer;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;

public class CampAdminCommand extends CommandBase {

	public CampAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

	@Override
	public void execute() {
		// k admin camp create|remove <player>
		//TODO: KR add message paths
		if (getArgs().length != 4) {
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        }
		Player bukkitPlayer = (Player) getSender();
		if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
			ChatUtil.printDebug("Failed to find non-existent player");
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return;
		}
		String cmdMode = getArgs()[2];
		String playerName = getArgs()[3];
		
		// Qualify arguments
		KonOfflinePlayer targetPlayer = getKonquest().getPlayerManager().getOfflinePlayerFromName(playerName);
		if(targetPlayer == null) {
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage());
			return;
		}
		if(!targetPlayer.isBarbarian()) {
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PLAYER.getMessage());
			return;
		}
		
		// Execute sub-commands
		if(cmdMode.equalsIgnoreCase("create")) {
			// Create a new camp for the target player
			if(getKonquest().isWorldIgnored(bukkitPlayer.getWorld())) {
				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
				return;
			}
			if(getKonquest().getCampManager().isCampSet(targetPlayer)) {
				ChatUtil.sendError((Player) getSender(), "Player already has a camp, remove it first before creating a new one.");
                return;
			}
			Location playerLoc = bukkitPlayer.getLocation();
			int status = getKonquest().getCampManager().addCamp(playerLoc, targetPlayer);
        	if(status == 0) {
        		ChatUtil.sendNotice((Player) getSender(), "Successfully created a camp for player "+targetPlayer.getOfflineBukkitPlayer().getName());
        	} else {
        		ChatUtil.sendError((Player) getSender(), "Failed to create a camp :(");
                return;
        	}
        	
		} else if(cmdMode.equalsIgnoreCase("remove")) {
			if(!getKonquest().getCampManager().isCampSet(targetPlayer)) {
				ChatUtil.sendError((Player) getSender(), "Player does not have a camp.");
                return;
			}
			boolean status = getKonquest().getCampManager().removeCamp(targetPlayer);
			if(status) {
				ChatUtil.sendNotice((Player) getSender(), "Successfully removed the camp for player "+targetPlayer.getOfflineBukkitPlayer().getName());
				KonPlayer onlineOwner = getKonquest().getPlayerManager().getPlayerFromName(playerName);
				if(onlineOwner != null) {
					ChatUtil.sendError(onlineOwner.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_CAMP_DESTROY_OWNER.getMessage());
				}
			} else {
				ChatUtil.sendError((Player) getSender(), "Failed to remove the camp :(");
                return;
			}
			
		} else {
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
		}

	}

	@Override
	public List<String> tabComplete() {
		// k admin camp create|remove <player>
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		if(getArgs().length == 3) {
			tabList.add("create");
			tabList.add("remove");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			// suggest player names
			tabList.addAll(getKonquest().getPlayerManager().getAllPlayerNames());
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
