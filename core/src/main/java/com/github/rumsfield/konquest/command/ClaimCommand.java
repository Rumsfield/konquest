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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ClaimCommand extends CommandBase {

	public ClaimCommand() {
		// Define name and sender support
		super("claim",true, false);
		// None
		setOptionalArgs(true);
		// [auto]
		addArgument(
				newArg("auto",true,false)
		);
		// radius <value>
		addArgument(
				newArg("radius",true,false)
						.sub( newArg("value", false, false) )
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

		Player bukkitPlayer = player.getBukkitPlayer();
		World bukkitWorld = bukkitPlayer.getWorld();
		// Verify that this command is being used in the default world
		if (!konquest.isWorldValid(bukkitWorld)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
			return;
		}
		// Verify no Barbarians are using this command
		if (player.isBarbarian()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
			return;
		}

		if (args.isEmpty()) {
			// Claim the single chunk containing playerLoc for the adjacent territory.
			konquest.getTerritoryManager().claimForPlayer(player, bukkitPlayer.getLocation());
		} else {
			// Has mode arguments
			String claimMode = args.get(0);
			if (claimMode.equalsIgnoreCase("radius")){
				boolean isAllowed = konquest.getCore().getBoolean(CorePath.TOWNS_ALLOW_CLAIM_RADIUS.getPath());
				if(!isAllowed) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
					return;
				}
				if(args.size() != 2) {
					sendInvalidArgMessage(bukkitPlayer);
					return;
				}
				final int min = 1;
				final int max = 5;
				int radius = Integer.parseInt(args.get(1));
				if(radius < min || radius > max) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_RADIUS.getMessage(min,max));
					return;
				}
				konquest.getTerritoryManager().claimRadiusForPlayer(player, bukkitPlayer.getLocation(), radius);

			} else if (claimMode.equalsIgnoreCase("auto")){
				boolean isAllowed = konquest.getCore().getBoolean(CorePath.TOWNS_ALLOW_CLAIM_AUTO.getPath());
				if(!isAllowed) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
					return;
				}
				boolean doAuto = false;
				// Check if player is already in an auto follow state
				if (player.isAutoFollowActive()) {
					// Check if player is already in claim state
					if (player.getAutoFollow().equals(FollowType.CLAIM)) {
						// Disable the auto state
						ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_DISABLE_AUTO.getMessage());
						player.setAutoFollow(FollowType.NONE);
					} else {
						// Change state
						doAuto = true;
					}
				} else {
					// Player is not in any auto mode, enter normal claim following state
					doAuto = true;
				}
				if (doAuto) {
					boolean isClaimSuccess = konquest.getTerritoryManager().claimForPlayer(player, bukkitPlayer.getLocation());
					if (isClaimSuccess) {
						ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_ENABLE_AUTO.getMessage());
						player.setAutoFollow(FollowType.CLAIM);
					}
				}
			} else {
				sendInvalidArgMessage(bukkitPlayer);
			}
		}
	}
	
	@Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		// Give suggestions
		if (args.size() == 1) {
			tabList.add("radius");
			tabList.add("auto");
		} else if (args.size() == 2) {
			if(args.get(0).equalsIgnoreCase("radius")){
				tabList.add("#");
			}
		}
		return matchLastArgToList(tabList,args);
	}

}