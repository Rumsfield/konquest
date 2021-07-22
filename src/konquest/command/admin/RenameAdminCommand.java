package konquest.command.admin;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonKingdom;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class RenameAdminCommand extends CommandBase {
	
	public RenameAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	// k admin rename kingdomName oldName newName
    	if (getArgs().length != 5) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {

        	String kingdomName = getArgs()[2];
        	String oldName = getArgs()[3];
        	String newName = getArgs()[4];
        	
        	if(!getKonquest().getKingdomManager().isKingdom(kingdomName)) {
        		//ChatUtil.sendError((Player) getSender(), "Invalid Kingdom name.");
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
                return;
        	}
        	
        	KonKingdom kingdom = getKonquest().getKingdomManager().getKingdom(kingdomName);
        	if(kingdom.getName().equals(oldName)) {
    			// Change kingdom name
    			getKonquest().getKingdomManager().renameKingdom(oldName, newName);
    			//ChatUtil.sendNotice((Player) getSender(), "Successfully renamed Kingdom "+oldName+" to "+newName);
    			ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_RENAME_NOTICE_KINGDOM.getMessage(oldName,newName));
    			return;
    		} else {
    			// Search for matching town names
    			for(KonTown town : kingdom.getTowns()) {
    				if(town.getName().equals(oldName)) {
    					// Change town name
    					getKonquest().getKingdomManager().renameTown(oldName, newName, kingdom.getName());
    					//ChatUtil.sendNotice((Player) getSender(), "Successfully renamed Town "+oldName+" to "+newName);
    					ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_ADMIN_RENAME_NOTICE_TOWN.getMessage(oldName,newName));
    					return;
    				}
    			}
    		}
        	//ChatUtil.sendError((Player) getSender(), "Could not find Kingdom or Town with name "+oldName);
        	ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(oldName));
        }
    }
    
    @Override
	public List<String> tabComplete() {
    	// k admin rename kingdomName oldName newName
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 3) {
			List<String> kingdomList = getKonquest().getKingdomManager().getKingdomNames();
			tabList.addAll(kingdomList);
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			List<String> kingdomList = getKonquest().getKingdomManager().getKingdomNames();
			String kingdomName = getArgs()[2];
			if(getKonquest().getKingdomManager().isKingdom(kingdomName)) {
				List<String> townList = getKonquest().getKingdomManager().getKingdom(kingdomName).getTownNames();
				tabList.addAll(kingdomList);
				tabList.addAll(townList);
				// Trim down completion options based on current input
				StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
				Collections.sort(matchedTabList);
			} else {
				ChatUtil.printDebug("RenameAdminCommand bad kingdom argument "+kingdomName);
			}
		}
		return matchedTabList;
	}
}
