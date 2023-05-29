package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonPlayer.FollowType;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnclaimCommand extends CommandBase {

	public UnclaimCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k unclaim [radius|auto] [<r>]
		Player bukkitPlayer = (Player) getSender();
    	boolean isEnabled = getKonquest().getCore().getBoolean(CorePath.TOWNS_ALLOW_UNCLAIM.getPath(),false);
		if (!isEnabled) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
			return;
		}
		if (getArgs().length > 3) {
			sendInvalidArgMessage(bukkitPlayer,CommandType.UNCLAIM);
		} else {
        	World bukkitWorld = bukkitPlayer.getWorld();
        	// Verify that this command is being used in the default world
        	if(!getKonquest().isWorldValid(bukkitWorld)) {
        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
                return;
        	}
        	// Verify no Barbarians are using this command
        	if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	if(player.isBarbarian()) {
        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
                return;
        	}

			if(getArgs().length <= 1) {
				// Unclaim the single chunk containing playerLoc for the current territory.
				getKonquest().getTerritoryManager().unclaimForPlayer(bukkitPlayer, bukkitPlayer.getLocation());
			} else {
				String unclaimMode = getArgs()[1];
				switch(unclaimMode) {
					case "radius" :
						if(getArgs().length != 3) {
							sendInvalidArgMessage(bukkitPlayer,CommandType.UNCLAIM);
							return;
						}

						final int min = 1;
						final int max = 5;
						int radius = Integer.parseInt(getArgs()[2]);
						if(radius < min || radius > max) {
							sendInvalidArgMessage(bukkitPlayer,CommandType.UNCLAIM);
							ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_UNCLAIM_ERROR_RADIUS.getMessage(min,max));
							return;
						}
						getKonquest().getTerritoryManager().unclaimRadiusForPlayer(bukkitPlayer, bukkitPlayer.getLocation(), radius);

						break;

					case "auto" :
						boolean doAuto = false;
						// Check if player is already in an auto follow state
						if(player.isAutoFollowActive()) {
							// Check if player is already in claim state
							if(player.getAutoFollow().equals(FollowType.UNCLAIM)) {
								// Disable the auto state
								ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_DISABLE_AUTO.getMessage());
								player.setAutoFollow(FollowType.NONE);
							} else {
								// Change state
								doAuto = true;
							}
						} else {
							// Player is not in any auto mode, enter normal un-claim following state
							doAuto = true;
						}
						if(doAuto) {
							boolean isUnclaimSuccess = getKonquest().getTerritoryManager().unclaimForPlayer(bukkitPlayer, bukkitPlayer.getLocation());
							if(isUnclaimSuccess) {
								ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_ENABLE_AUTO.getMessage());
								player.setAutoFollow(FollowType.UNCLAIM);
							}
						}
						break;

					default :
						sendInvalidArgMessage(bukkitPlayer,CommandType.UNCLAIM);
				}
			}
        }
	}
	
	@Override
	public List<String> tabComplete() {
		// k unclaim [radius|auto] [<r>]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 2) {
			tabList.add("radius");
			tabList.add("auto");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 3) {
			// suggest number
			String subCommand = getArgs()[1];
			if(subCommand.equalsIgnoreCase("radius")) {
				tabList.add("#");
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		
		return matchedTabList;
	}
}
