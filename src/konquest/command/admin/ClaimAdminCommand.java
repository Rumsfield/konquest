package konquest.command.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonPlayer;
import konquest.model.KonTerritory;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

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
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	if(!getKonquest().getPlayerManager().isPlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	if(getArgs().length > 2) {
        		String claimMode = getArgs()[2];
        		switch(claimMode) {
        		case "radius" :
        			if(getArgs().length != 4) {
        				ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
        	            return;
        			}
    				int radius = Integer.parseInt(getArgs()[3]);
    				final int min = 1;
        			final int max = 16;
    				if(radius < min || radius > max) {
    					ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
    					//ChatUtil.sendError((Player) getSender(), "Radius must be greater than 0 and less than or equal to 16.");
    					ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_CLAIM_ERROR_RADIUS.getMessage(min,max));
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
						//ChatUtil.sendError((Player) getSender(), "Must claim near an existing territory.");
						ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_CLAIM_ERROR_MISSING.getMessage());
    					return;
					}		
    				ArrayList<Chunk> chunkList = getKonquest().getAreaChunks(bukkitPlayer.getLocation(), radius);
    				ChatUtil.printDebug("Checking for chunk conflicts with radius "+radius);
    				for(Chunk chunk : chunkList) {
    					if(getKonquest().getKingdomManager().isChunkClaimed(chunk) && !getKonquest().getKingdomManager().getChunkTerritory(chunk).equals(adjacentTerritory)) {
    						ChatUtil.printDebug("Found a chunk conflict");
    						//ChatUtil.sendError((Player) getSender(), "Cannot claim over another territory.");
    						ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_CLAIM_ERROR_OVERLAP.getMessage());
    						return;
    					}
    				}
    				// Claim chunks for adjacent territory
    				if(adjacentTerritory.addChunks(chunkList)) {
    					//getKonquest().getKingdomManager().updateTerritoryCache();
    					int numChunks = 0;
    					for(Chunk chunk : chunkList) {
    						getKonquest().getKingdomManager().addTerritory(chunk,adjacentTerritory);
    						numChunks++;
    					}
    					getKonquest().getKingdomManager().updatePlayerBorderParticles(player, bukkitPlayer.getLocation());
    					//ChatUtil.sendNotice((Player) getSender(), "Successfully claimed chunks within radius "+radius+" for territory "+adjacentTerritory.getName());
    					ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_CLAIM_NOTICE_SUCCESS.getMessage(numChunks,adjacentTerritory.getName()));
    				} else {
    					//ChatUtil.sendError((Player) getSender(), "There was a problem claiming chunks within radius "+radius+" for territory "+adjacentTerritory.getName());
    					ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    				}
        			break;
        		case "auto" :
        			if(!player.isAdminClaimingFollow()) {
        				player.setIsAdminClaimingFollow(true);
        				//ChatUtil.sendNotice((Player) getSender(), "Enabled admin auto claim. Use this command again to disable.");
        				ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_ENABLE_AUTO.getMessage());
        				getKonquest().getKingdomManager().claimForAdmin(bukkitPlayer, bukkitPlayer.getLocation());
        			} else {
        				player.setIsAdminClaimingFollow(false);
        				//ChatUtil.sendNotice((Player) getSender(), "Disabled admin auto claim.");
        				ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_DISABLE_AUTO.getMessage());
        			}
        			break;
        		default :
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
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
