package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonPlayer.FollowType;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UnclaimAdminCommand extends CommandBase {
	
	public UnclaimAdminCommand() {
		// Define name and sender support
		super("unclaim",true, true);
		// Define arguments
		// None
		setOptionalArgs(true);
		// auto
		addArgument(
				newArg("auto",true,false)
		);
		// radius <value>
		addArgument(
				newArg("radius",true,false)
						.sub( newArg("value",false,false) )
		);
    }

	@Override
    public void execute(Konquest konquest, CommandSender sender, List<String> args) {
		// Sender must be player
		KonPlayer player = konquest.getPlayerManager().getPlayer(sender);
		if (player == null) {
			sendInvalidSenderMessage(sender);
			return;
		}
		Location playerLoc = player.getBukkitPlayer().getLocation();
		// Check for valid world
		if(!konquest.isWorldValid(playerLoc.getWorld())) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
			return;
		}
		// If no arguments
		if (args.isEmpty()) {
			// Unclaim the single chunk containing playerLoc for the current territory.
			konquest.getTerritoryManager().unclaimForAdmin(player, playerLoc);
			return;
		}
		// Parse arguments
		switch(args.get(0).toLowerCase()) {
			case "radius" :
				if(args.size() != 2) {
					sendInvalidArgMessage(sender);
					return;
				}
				int radius;
				try {
					radius = Integer.parseInt(args.get(1));
				} catch(NumberFormatException e) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(e.getMessage()));
					return;
				}
				final int min = 1;
				final int max = 16;
				if(radius < min || radius > max) {
					ChatUtil.sendError(sender, MessagePath.COMMAND_UNCLAIM_ERROR_RADIUS.getMessage(min,max));
					return;
				}
				konquest.getTerritoryManager().unclaimRadiusForAdmin(player, playerLoc, radius);
				break;

			case "auto" :
				boolean doAuto = false;
				// Check if player is already in an auto follow state
				if(player.isAutoFollowActive()) {
					// Check if player is already in claim state
					if(player.getAutoFollow().equals(FollowType.ADMIN_UNCLAIM)) {
						// Disable the auto state
						ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_DISABLE_AUTO.getMessage());
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
					boolean isUnclaimSuccess = konquest.getTerritoryManager().unclaimForAdmin(player, playerLoc);
					if(isUnclaimSuccess) {
						ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_ENABLE_AUTO.getMessage());
						player.setAutoFollow(FollowType.ADMIN_UNCLAIM);
					}
				}
				break;

			default :
				sendInvalidArgMessage(sender);
		}
    }
    
    @Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		if (args.size() == 1) {
			tabList.add("radius");
			tabList.add("auto");
		} else if (args.size() == 2) {
			// suggest number
			if(args.get(0).equalsIgnoreCase("radius")) {
				tabList.add("#");
			}
		}
		return matchLastArgToList(tabList,args);
	}
}
