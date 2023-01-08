package konquest.command.admin;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonPropertyFlag;
import konquest.model.KonPropertyFlagHolder;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class FlagAdminCommand extends CommandBase {
	
	public FlagAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	private final String flagLine = "| %-10s %-10s %s";
	
	/*
	 * The following classes are known property holders
	 * 	- KonKingdom
	 * 	- KonTown
	 * 	- KonSanctuary
	 */

    public void execute() {
    	// k admin flag <name> [<flag>] [<value>]
    	if (getArgs().length != 3 && getArgs().length != 4 && getArgs().length != 5) {
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
    	} else {
    		
    		// Check for valid holder name
    		String holderName = getArgs()[2];
    		KonPropertyFlagHolder holder = null;
    		if(getKonquest().getKingdomManager().isKingdom(holderName)) {
    			// Found kingdom
    			holder = getKonquest().getKingdomManager().getKingdom(holderName);
    		} else if(getKonquest().getKingdomManager().isTown(holderName)) {
    			// Found town
    			holder = getKonquest().getKingdomManager().getTown(holderName);
    		} else if(getKonquest().getSanctuaryManager().isSanctuary(holderName)) {
    			// Found sanctuary
    			holder = getKonquest().getSanctuaryManager().getSanctuary(holderName);
    		}
    		if(holder == null) {
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(holderName));
    			return;
    		}
    		
    		if (getArgs().length == 3) {
    			// k admin flag <name>
        		// List available flags for the given property holder
        		Map<KonPropertyFlag,Boolean> holderFlags = holder.getAllProperties();
        		
        		/*
        		// Determine max length of property names
        		int maxLength = 0;
        		for(KonPropertyFlag flag : holderFlags.keySet()) {
        			if(flag.getName().length() > maxLength) {
        				maxLength = flag.getName().length();
        			}
        		}
        		int space1 = maxLength + 2;
        		*/
        		
        		// List all holder properties and values
        		//TODO: Replace with message path
        		 ChatUtil.sendNotice((Player) getSender(), "All Properties");
        		String header = String.format(flagLine, "Flag", "Value", "Description");
                ChatUtil.sendMessage((Player) getSender(), ChatColor.GOLD+header);
                for(KonPropertyFlag flag : holderFlags.keySet()) {
                	String line = String.format(flagLine, flag.getName(), holderFlags.get(flag), flag.getDescription());
                	ChatUtil.sendMessage((Player) getSender(), line);
                }

            } else if (getArgs().length > 3) {
            	
            	String flagName = getArgs()[3];
            	if(!KonPropertyFlag.contains(flagName)) {
            		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(holderName));
        			return;
            	}
            	KonPropertyFlag flagArg = KonPropertyFlag.getFlag(flagName);
            	if(!holder.hasPropertyValue(flagArg)) {
            		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(holderName));
        			return;
            	}
            	
            	if (getArgs().length == 4) {
            		// k admin flag <name> <flag>
            		// Display current value and description of given flag
            		boolean flagValue = holder.getPropertyValue(flagArg);
            		String header = String.format(flagLine, "Flag", "Value", "Description");
            		String line = String.format(flagLine, flagArg.getName(), flagValue, flagArg.getDescription());
            		ChatUtil.sendNotice((Player) getSender(), "Single Property");
            		ChatUtil.sendMessage((Player) getSender(), ChatColor.GOLD+header);
            		ChatUtil.sendMessage((Player) getSender(), line);
            		
            	} else if (getArgs().length == 5) {
            		// k admin flag <name> <flag> <value>
            		// Set new value for flag
            		String flagValue = getArgs()[4];
            		boolean bValue = Boolean.valueOf(flagValue);
            		boolean status = holder.setPropertyValue(flagArg, bValue);
            		if(status) {
            			// Successfully assigned value
            			ChatUtil.sendNotice((Player) getSender(), "Set property "+flagArg.getName()+" of "+holderName+" to "+bValue);
            		} else {
            			// Failed to assign value
            			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_FAILED.getMessage());
            		}
            	}
            }	
    	}
    }
    
    @Override
	public List<String> tabComplete() {
    	// k admin flag <name> [<flag>] [<value>]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 3) {
			// Suggest all known property holders
			tabList.addAll(getKonquest().getKingdomManager().getKingdomNames());
			tabList.addAll(getKonquest().getKingdomManager().getTownNames());
			tabList.addAll(getKonquest().getSanctuaryManager().getSanctuaryNames());
			
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length >= 4) {
			
			// Determine whether holder name is valid
			String holderName = getArgs()[2];
    		KonPropertyFlagHolder holder = null;
    		if(getKonquest().getKingdomManager().isKingdom(holderName)) {
    			// Found kingdom
    			holder = getKonquest().getKingdomManager().getKingdom(holderName);
    		} else if(getKonquest().getKingdomManager().isTown(holderName)) {
    			// Found town
    			holder = getKonquest().getKingdomManager().getTown(holderName);
    		} else if(getKonquest().getSanctuaryManager().isSanctuary(holderName)) {
    			// Found sanctuary
    			holder = getKonquest().getSanctuaryManager().getSanctuary(holderName);
    		}
    		
    		if(holder != null) {
    			// Holder is valid
    			if(getArgs().length == 4) {
    				// Suggest properties
    				for(KonPropertyFlag flag : holder.getAllProperties().keySet()) {
    					tabList.add(flag.toString());
    				}
    				// Trim down completion options based on current input
    				StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
    				Collections.sort(matchedTabList);
    				
    			} else if(getArgs().length == 5) {
    				String flagName = getArgs()[3];
    				if(holder.hasPropertyValue(KonPropertyFlag.getFlag(flagName))) {
    					// Suggest values
    					tabList.add(String.valueOf(true));
    					tabList.add(String.valueOf(false));
    					// Trim down completion options based on current input
    					StringUtil.copyPartialMatches(getArgs()[4], tabList, matchedTabList);
    					Collections.sort(matchedTabList);
    				}
    			}
    		}
		}
		return matchedTabList;
	}
}
