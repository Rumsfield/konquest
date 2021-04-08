package konquest.command.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonPlayer;
import konquest.model.KonTerritory;
import konquest.utility.ChatUtil;
import konquest.utility.MessageStatic;

import org.bukkit.Chunk;
//import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class ClaimAdminCommand extends CommandBase {
	
	public ClaimAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	// k admin claim [radius|auto] [<r>]
    	if (getArgs().length > 4) {
            ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	//World bukkitWorld = bukkitPlayer.getWorld();
        	/*
        	if(!bukkitWorld.getName().equals(getKonquest().getWorldName())) {
        		ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_WORLD.toString());
                return;
        	}
        	*/
        	if(getArgs().length > 2) {
        		String claimMode = getArgs()[2];
        		switch(claimMode) {
        		case "radius" :
        			if(getArgs().length != 4) {
        				ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
        	            return;
        			}
    				int radius = Integer.parseInt(getArgs()[3]);
    				if(radius < 1 || radius > 10) {
    					ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
    					ChatUtil.sendError((Player) getSender(), "Radius must be greater than 0 and less than or equal to 10.");
    					return;
    				}
    				KonTerritory adjacentTerritory = null;
					for(Chunk adjChunk : getKonquest().getAreaChunks(bukkitPlayer.getLocation(), 2)) {
						if(getKonquest().getKingdomManager().isChunkClaimed(adjChunk)) {
							adjacentTerritory = getKonquest().getKingdomManager().getChunkTerritory(adjChunk);
							break;
						}
					}
					if(adjacentTerritory == null) {
						ChatUtil.sendError((Player) getSender(), "Must claim near an existing territory.");
    					return;
					}		
    				ArrayList<Chunk> chunkList = getKonquest().getAreaChunks(bukkitPlayer.getLocation(), radius);
    				ChatUtil.printDebug("Checking for chunk conflicts with radius "+radius);
    				for(Chunk chunk : chunkList) {
    					if(getKonquest().getKingdomManager().isChunkClaimed(chunk) && !getKonquest().getKingdomManager().getChunkTerritory(chunk).equals(adjacentTerritory)) {
    						ChatUtil.printDebug("Found a chunk conflict");
    						ChatUtil.sendError((Player) getSender(), "Cannot claim over another territory.");
    						return;
    					}
    				}
    				// Claim chunks for adjacent territory
    				if(adjacentTerritory.addChunks(chunkList)) {
    					//getKonquest().getKingdomManager().updateTerritoryCache();
    					for(Chunk chunk : chunkList) {
    						getKonquest().getKingdomManager().addTerritory(chunk,adjacentTerritory);
    					}
    					ChatUtil.sendNotice((Player) getSender(), "Successfully claimed chunks within radius "+radius+" for territory "+adjacentTerritory.getName());
    				} else {
    					ChatUtil.sendError((Player) getSender(), "There was a problem claiming chunks within radius "+radius+" for territory "+adjacentTerritory.getName());
    				}
        			break;
        		case "auto" :
        			KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        			if(!player.isAdminClaimingFollow()) {
        				player.setIsAdminClaimingFollow(true);
        				ChatUtil.sendNotice((Player) getSender(), "Enabled admin auto claim. Use this command again to disable.");
        				getKonquest().getKingdomManager().claimForAdmin(bukkitPlayer, bukkitPlayer.getLocation());
        			} else {
        				player.setIsAdminClaimingFollow(false);
        				ChatUtil.sendNotice((Player) getSender(), "Disabled admin auto claim.");
        			}
        			break;
        		default :
        			ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
                    return;
        		}
        	} else {
        		// Claim the single chunk containing playerLoc for the adjacent territory.
        		getKonquest().getKingdomManager().claimForAdmin(bukkitPlayer, bukkitPlayer.getLocation());
        	}
        }
    }
    
    @Override
	public List<String> tabComplete() {
		// k admin claim [radius|auto] [<r>]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 3) {
			tabList.add("radius");
			tabList.add("auto");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		
		return matchedTabList;
	}
 
}
