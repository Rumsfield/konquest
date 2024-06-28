package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonTerritory;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class SetTravelAdminCommand extends CommandBase {
	
	public SetTravelAdminCommand() {
		// Define name and sender support
		super("settravel",true, true);
		// No arguments
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
		Location playerLoc = player.getBukkitPlayer().getLocation();
    	// Check for valid world
		if(!konquest.isWorldValid(playerLoc.getWorld())) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
			return;
		}
		// Check if player's location is within a territory
		if(konquest.getTerritoryManager().isChunkClaimed(playerLoc)) {
			KonTerritory territory = konquest.getTerritoryManager().getChunkTerritory(playerLoc);
			if (territory == null) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
				return;
			}
			territory.setSpawn(playerLoc);
			ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_SETTRAVEL_NOTICE_SUCCESS.getMessage(territory.getName()));
		} else {
			ChatUtil.sendError(sender, MessagePath.COMMAND_ADMIN_SETTRAVEL_ERROR_FAIL.getMessage());
		}
    }
    
    @Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		// No arguments to complete
		return Collections.emptyList();
	}
}
