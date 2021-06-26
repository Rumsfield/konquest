package konquest.command.admin;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class FlagAdminCommand extends CommandBase {
	
	public FlagAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
        // k admin flag flag1 [arguments]
    	if (getArgs().length == 2) {
            listFlags();
            return;
        } else if (getArgs().length >= 3) {
        	AdminFlagType flagArg = AdminFlagType.getFlag(getArgs()[2]);
            switch (flagArg) {
	            case PEACEFUL:
	            	// k admin flag peaceful <kingdom> <true|false>
	            	if (getArgs().length != 5) {
	            		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	            		return;
	            	} else {
	            		String kingdomName = getArgs()[3];
	            		String flagValue = getArgs()[4];
	            		// Verify Kingdom exists
	            		if(!getKonquest().getKingdomManager().isKingdom(kingdomName)) {
	                		//ChatUtil.sendError((Player) getSender(), "No such kingdom");
	                		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
	                		return;
	                	}
	            		// Assign flag value
	            		boolean bValue = Boolean.valueOf(flagValue);
	            		getKonquest().getKingdomManager().getKingdom(kingdomName).setPeaceful(bValue);
	            		ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
	            	}
	                break;
                default:
                	//ChatUtil.sendError((Player) getSender(), "Flag does not exist");
                	ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(getArgs()[2]));
                	break;
            }
        }
    }
    
    @Override
	public List<String> tabComplete() {
    	// k admin flag flag1 [arguments]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 3) {
			for(AdminFlagType flag : AdminFlagType.values()) {
    			tabList.add(flag.toString());
    		}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length >= 4) {
			AdminFlagType flagArg = AdminFlagType.getFlag(getArgs()[2]);
            switch (flagArg) {
	            case PEACEFUL:
	            	// k admin flag peaceful <kingdom> <true|false>
	            	if(getArgs().length == 4) {
	            		List<String> kingdomList = getKonquest().getKingdomManager().getKingdomNames();
	            		tabList.addAll(kingdomList);
	            		// Trim down completion options based on current input
	        			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
	        			Collections.sort(matchedTabList);
	            	} else if(getArgs().length == 5) {
	            		tabList.add("true");
	            		tabList.add("false");
	            		// Trim down completion options based on current input
	        			StringUtil.copyPartialMatches(getArgs()[4], tabList, matchedTabList);
	        			Collections.sort(matchedTabList);
	            	}
	                break;
                default:
                	tabList = Collections.emptyList();
                	break;
            }
		}
		return matchedTabList;
	}
    
    private void listFlags() {
    	String message = "";
        ChatUtil.sendNotice((Player) getSender(), "Kingdom flags: Flag, Values, Description");
        for(AdminFlagType cmd : AdminFlagType.values()) {
        	message = ChatColor.GOLD+"Flag "+cmd.toString().toLowerCase()+" "+ChatColor.AQUA+cmd.flagValues()+": "+ChatColor.GRAY+cmd.description();
            ChatUtil.sendMessage((Player) getSender(), message);
        }
    }
}
