package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class SetTravelAdminCommand extends CommandBase {
	
	public SetTravelAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	// k admin setTravel
		Player bukkitPlayer = (Player) getSender();
    	if (getArgs().length != 2) {
			sendInvalidArgMessage(bukkitPlayer, AdminCommandType.SETTRAVEL);
		} else {
        	World bukkitWorld = bukkitPlayer.getWorld();

        	if(!getKonquest().isWorldValid(bukkitWorld)) {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
                return;
        	}
        	
        	Location playerLoc = bukkitPlayer.getLocation();
        	// Check if player's location is within a territory
        	if(getKonquest().getTerritoryManager().isChunkClaimed(playerLoc)) {
        		getKonquest().getTerritoryManager().getChunkTerritory(playerLoc).setSpawn(playerLoc);
        		ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_SETTRAVEL_NOTICE_SUCCESS.getMessage(getKonquest().getTerritoryManager().getChunkTerritory(playerLoc).getName()));
        	} else {
        		ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_SETTRAVEL_ERROR_FAIL.getMessage());
        	}
        }
    }
    
    @Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
