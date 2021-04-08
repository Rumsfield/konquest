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

public class UnclaimAdminCommand extends CommandBase {
	
	public UnclaimAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	// k admin unclaim
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
        	if(getKonquest().getKingdomManager().isChunkClaimed(playerLoc.getChunk())) {
        		// unclaim the single chunk containing playerLoc from the adjacent territory.
        		String territoryName = getKonquest().getKingdomManager().getChunkTerritory(playerLoc.getChunk()).getName();
        		if(getKonquest().getKingdomManager().unclaimChunk(playerLoc)) {
        			ChatUtil.sendNotice((Player) getSender(), "Successfully removed chunk from territory: "+territoryName);
        		} else {
        			ChatUtil.sendError((Player) getSender(), "There was a problem unclaiming this chunk.");
        		}
        	} else {
        		ChatUtil.sendNotice((Player) getSender(), "Chunk is already unclaimed.");
        	}
        }
    }
    
    @Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
