package konquest.command.admin;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.utility.ChatUtil;
import konquest.utility.MessageStatic;

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
            ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	World bukkitWorld = bukkitPlayer.getWorld();

        	if(!bukkitWorld.getName().equals(getKonquest().getWorldName())) {
        		ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_WORLD.toString());
                return;
        	}
        	
        	Location playerLoc = bukkitPlayer.getLocation();
        	// Check if player's location is within a territory
        	if(getKonquest().getKingdomManager().isChunkClaimed(playerLoc.getChunk())) {
        		getKonquest().getKingdomManager().getChunkTerritory(playerLoc.getChunk()).setSpawn(playerLoc);
        		ChatUtil.sendNotice((Player) getSender(), "Successfully set new travel point in "+getKonquest().getKingdomManager().getChunkTerritory(playerLoc.getChunk()).getName());
        	} else {
        		ChatUtil.sendNotice((Player) getSender(), "Failed to set travel point in unknown territory");
        	}
        }
    }
    
    @Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
