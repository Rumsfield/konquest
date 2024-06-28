package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonTerritory;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class FlyCommand extends CommandBase {

	public FlyCommand() {
		// Define name and sender support
		super("fly",true, false);
		// No Arguments
    }

	@Override
	public void execute(Konquest konquest, CommandSender sender, List<String> args) {
		// Sender must be player
		KonPlayer player = konquest.getPlayerManager().getPlayer(sender);
		if (player == null) {
			sendInvalidSenderMessage(sender);
			return;
		}
		if (!args.isEmpty()) {
			sendInvalidArgMessage(sender);
			return;
		}
		Player bukkitPlayer = player.getBukkitPlayer();
		if(bukkitPlayer.getGameMode().equals(GameMode.SURVIVAL)) {
			if(player.isFlyEnabled()) {
				player.setIsFlyEnabled(false);
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_DISABLE_AUTO.getMessage());
			} else {
				// Verify player is in friendly territory
				boolean isFriendly = false;
				if(konquest.getTerritoryManager().isChunkClaimed(bukkitPlayer.getLocation())) {
					KonTerritory territory = konquest.getTerritoryManager().getChunkTerritory(bukkitPlayer.getLocation());
					if(territory != null && territory.getKingdom().equals(player.getKingdom())) {
						isFriendly = true;
					}
				}
				if(isFriendly) {
					player.setIsFlyEnabled(true);
					player.setFlyDisableWarmup(false);
					ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_ENABLE_AUTO.getMessage());
				} else {
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
				}
			}
		} else {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
		}
	}
	
	@Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		// No arguments to complete
		return Collections.emptyList();
	}
	
}
