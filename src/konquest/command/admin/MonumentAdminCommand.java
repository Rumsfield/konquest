package konquest.command.admin;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonPlayer;
import konquest.model.KonPlayer.RegionType;
import konquest.utility.ChatUtil;
import konquest.utility.MessageStatic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class MonumentAdminCommand extends CommandBase {
	
	public MonumentAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	// k admin monument kingdom1 create|remove
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
        	
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	if(player.isSettingRegion()) {
        		ChatUtil.sendError((Player) getSender(), "Cannot do this while setting regions");
                return;
        	}
        	
        	String kingdomName = getArgs()[2];
        	if(!getKonquest().getKingdomManager().isKingdom(kingdomName)) {
        		ChatUtil.sendError((Player) getSender(), "Bad Kingdom name");
                return;
        	}
        	
        	String cmdMode = getArgs()[3];
        	if(cmdMode.equalsIgnoreCase("create")) {
        		player.settingRegion(RegionType.MONUMENT);
        		player.setRegionKingdomName(kingdomName);
            	ChatUtil.sendNotice((Player) getSender(), "Now creating new Monument Template. Click on Air to cancel.");
            	ChatUtil.sendNotice((Player) getSender(), "Click on the first corner block of the region.");
        	} else if(cmdMode.equalsIgnoreCase("remove")) {
        		player.settingRegion(RegionType.NONE);
        		getKonquest().getKingdomManager().getKingdom(kingdomName).removeMonumentTemplate();
        		ChatUtil.sendNotice((Player) getSender(), "Removed Monument Template for kingdom "+kingdomName);
        	} else {
        		ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
        	}
        }
    }
    
    @Override
	public List<String> tabComplete() {
    	// k admin monument kingdom1 create|remove
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 3) {
			List<String> kingdomList = getKonquest().getKingdomManager().getKingdomNames();
			tabList.addAll(kingdomList);
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			tabList.add("create");
			tabList.add("remove");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
