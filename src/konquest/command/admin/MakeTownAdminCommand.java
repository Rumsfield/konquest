package konquest.command.admin;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.utility.ChatUtil;
import konquest.utility.MessageStatic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class MakeTownAdminCommand extends CommandBase {
	
	public MakeTownAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	// k admin maketown town1 kingdom1
    	if (getArgs().length != 4) {
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
        	String townName = getArgs()[2];
        	if(!StringUtils.isAlphanumeric(townName)) {
        		ChatUtil.sendError((Player) getSender(), "Town name must only contain letters and/or numbers");
                return;
        	}
        	String kingdomName = getArgs()[3];
        	int exitCode = getKonquest().getKingdomManager().addTown(playerLoc, townName, kingdomName);
        	if(exitCode == 0) {
        		bukkitPlayer.teleport(getKonquest().getKingdomManager().getKingdom(kingdomName).getTown(townName).getSpawnLoc());
        		ChatUtil.sendNotice((Player) getSender(), "Successfully created new Town: "+townName);
        	} else {
        		switch(exitCode) {
        		case 1:
        			ChatUtil.sendError((Player) getSender(), "Could not create town: chunk claim conflict");
        			break;
        		case 2:
        			ChatUtil.sendError((Player) getSender(), "Could not create town: bad monument placement");
        			break;
        		case 3:
        			ChatUtil.sendError((Player) getSender(), "Could not create town: bad name");
        			break;
        		case 4:
        			ChatUtil.sendError((Player) getSender(), "Could not create town: invalid monument template");
        			break;
        		case 5:
        			ChatUtil.sendError((Player) getSender(), "Could not create town: too close to other territory");
        			break;
        		case 11:
        			ChatUtil.sendError((Player) getSender(), "Could not create town: ground is not flat enough");
        			break;
        		case 12:
        			ChatUtil.sendError((Player) getSender(), "Could not create town: ground is too high");
        			break;
        		case 13:
        			ChatUtil.sendError((Player) getSender(), "Could not create town: too much air below town");
        			break;
        		case 14:
        			ChatUtil.sendError((Player) getSender(), "Could not create town: initial claimed chunks may be missing");
        			break;
        		default:
        			ChatUtil.sendError((Player) getSender(), "Could not create town: unknown");
        			break;
        		}
        	}
        }
    }
    
    @Override
	public List<String> tabComplete() {
    	// k admin maketown town1 kingdom1
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 3) {
			tabList.addAll(Collections.emptyList());
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			List<String> kingdomList = getKonquest().getKingdomManager().getKingdomNames();
			tabList.addAll(kingdomList);
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		
		return matchedTabList;
	}
}
