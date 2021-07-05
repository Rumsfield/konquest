package konquest.command.admin;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetTravelAdminCommand extends CommandBase {
	
	public SetTravelAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	// k admin setTravel
    	if (getArgs().length != 2) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	World bukkitWorld = bukkitPlayer.getWorld();

        	if(!getKonquest().isWorldValid(bukkitWorld)) {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
                return;
        	}
        	
        	Location playerLoc = bukkitPlayer.getLocation();
        	// Check if player's location is within a territory
        	if(getKonquest().getKingdomManager().isChunkClaimed(playerLoc)) {
        		getKonquest().getKingdomManager().getChunkTerritory(playerLoc).setSpawn(playerLoc);
        		//ChatUtil.sendNotice((Player) getSender(), "Successfully set new travel point in "+getKonquest().getKingdomManager().getChunkTerritory(playerLoc.getChunk()).getName());
        		ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_SETTRAVEL_NOTICE_SUCCESS.getMessage(getKonquest().getKingdomManager().getChunkTerritory(playerLoc).getName()));
        	} else {
        		//ChatUtil.sendError((Player) getSender(), "Failed to set travel point in unknown territory");
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
