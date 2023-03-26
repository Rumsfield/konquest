package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.model.KonOfflinePlayer;
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

public class CampAdminCommand extends CommandBase {

	public CampAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

	@Override
	public void execute() {
		// k admin camp create|destroy <player>
		Player bukkitPlayer = (Player) getSender();
		if (getArgs().length != 4) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        }
		if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
			ChatUtil.printDebug("Failed to find non-existent player");
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return;
		}
		String cmdMode = getArgs()[2];
		String playerName = getArgs()[3];
		
		// Qualify arguments
		KonOfflinePlayer targetPlayer = getKonquest().getPlayerManager().getOfflinePlayerFromName(playerName);
		if(targetPlayer == null) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage());
			return;
		}
		if(!targetPlayer.isBarbarian()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PLAYER.getMessage());
			return;
		}
		String targetName = targetPlayer.getOfflineBukkitPlayer().getName();

		// Execute sub-commands
		if(cmdMode.equalsIgnoreCase("create")) {
			// Create a new camp for the target player
			if(getKonquest().isWorldIgnored(bukkitPlayer.getWorld())) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
				return;
			}
			if(getKonquest().getCampManager().isCampSet(targetPlayer)) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_ADMIN_CAMP_ERROR_CREATE_EXIST.getMessage());
                return;
			}
			Location playerLoc = bukkitPlayer.getLocation();
			int status = getKonquest().getCampManager().addCamp(playerLoc, targetPlayer);
			switch(status) {
				case 0:
					ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_CAMP_NOTICE_CREATE.getMessage(targetName));
					break;
				case 1:
					ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_CAMP_FAIL_OVERLAP.getMessage());
					break;
				case 2:
					ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_CAMP_CREATE.getMessage());
					break;
				case 3:
					ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_CAMP_FAIL_BARBARIAN.getMessage());
					break;
				case 4:
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
					break;
				case 5:
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
					break;
				default:
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
					break;
			}
		} else if(cmdMode.equalsIgnoreCase("destroy")) {
			if(!getKonquest().getCampManager().isCampSet(targetPlayer)) {
				ChatUtil.sendError(bukkitPlayer, "Player does not have a camp.");
                return;
			}
			boolean status = getKonquest().getCampManager().removeCamp(targetPlayer);
			if(status) {
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_CAMP_NOTICE_DESTROY.getMessage(targetName));
				KonPlayer onlineOwner = getKonquest().getPlayerManager().getPlayerFromName(playerName);
				if(onlineOwner != null) {
					ChatUtil.sendError(onlineOwner.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_CAMP_DESTROY_OWNER.getMessage());
				}
			} else {
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_FAILED.getMessage());
			}
		} else {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
		}

	}

	@Override
	public List<String> tabComplete() {
		// k admin camp create|remove <player>
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		if(getArgs().length == 3) {
			tabList.add("create");
			tabList.add("destroy");
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
