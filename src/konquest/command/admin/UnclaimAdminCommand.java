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

public class UnclaimAdminCommand extends CommandBase {
	
	public UnclaimAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	// k admin unclaim
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
        	if(getKonquest().getKingdomManager().isChunkClaimed(playerLoc)) {
        		// unclaim the single chunk containing playerLoc from the adjacent territory.
        		String territoryName = getKonquest().getKingdomManager().getChunkTerritory(playerLoc).getName();
        		if(getKonquest().getKingdomManager().unclaimChunk(playerLoc)) {
        			//ChatUtil.sendNotice((Player) getSender(), "Successfully removed chunk from territory: "+territoryName);
        			ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_UNCLAIM_NOTICE_SUCCESS.getMessage(territoryName));
        		} else {
        			//ChatUtil.sendError((Player) getSender(), "There was a problem unclaiming this chunk.");
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_UNCLAIM_ERROR_FAIL.getMessage());
        		}
        	} else {
        		//ChatUtil.sendNotice((Player) getSender(), "Chunk is already unclaimed.");
        		ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_ADMIN_UNCLAIM_ERROR_FAIL.getMessage());
        	}
        }
    }
    
    @Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
