package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.model.KonPropertyFlag;
import com.github.rumsfield.konquest.model.KonPropertyFlagHolder;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FlagAdminCommand extends CommandBase {
	
	public FlagAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	private final String flagLine = "| %-10s %-10s %s";
	
	/*
	 * The following classes are known property holders
	 * 	- KonKingdom (kingdom name)
	 *  - KonCapital ("capital" + kingdom name)
	 * 	- KonTown (town name)
	 * 	- KonSanctuary (sanctuary name)
	 *  - KonRuin (ruin name)
	 */

    public void execute() {
    	// k admin flag [capital] <name> [<flag>] [<value>]
    	if (getArgs().length != 3 && getArgs().length != 4 && getArgs().length != 5 && getArgs().length != 6) {
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
		} else {
			// The arg ordering is tricky.
			// First try to match the arg with a known holder.
			// Then check if it is "capital", then shift all args down 1.
    		boolean isCapital = false;
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
    		} else if(getKonquest().getRuinManager().isRuin(holderName)) {
				// Found ruin
				holder = getKonquest().getRuinManager().getRuin(holderName);
			} else if(holderName.equals("capital")) {
				if(getArgs().length > 3) {
					String capitalName = getArgs()[3];
					if(getKonquest().getKingdomManager().isKingdom(capitalName)) {
						// Found kingdom capital
						holder = getKonquest().getKingdomManager().getKingdom(capitalName).getCapital();
						isCapital = true;
					}
				}
			}
    		if(holder == null) {
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(holderName));
    			return;
    		}

			// Number of arguments to list available flags of the given holder
			int num_args_list = 3;
			// Number of arguments to display a specific flag
			int num_args_disp = 4;
			// Number of arguments to set a given flag
			int num_args_set = 5;
			if(isCapital) {
				num_args_list = 4;
				num_args_disp = 5;
				num_args_set = 6;
			}
    		
    		if (getArgs().length == num_args_list) {
    			// k admin flag <name>
        		// List available flags for the given property holder
        		Map<KonPropertyFlag,Boolean> holderFlags = holder.getAllProperties();
        		
        		// List all holder properties and values
        		//TODO: Replace with message path
        		 ChatUtil.sendNotice((Player) getSender(), "All Properties");
        		String header = String.format(flagLine, "Flag", "Value", "Description");
                ChatUtil.sendMessage((Player) getSender(), ChatColor.GOLD+header);
                for(KonPropertyFlag flag : holderFlags.keySet()) {
                	String line = String.format(flagLine, flag.getName(), holderFlags.get(flag), flag.getDescription());
                	ChatUtil.sendMessage((Player) getSender(), line);
                }

            } else if (getArgs().length > num_args_list) {
            	
            	String flagName = getArgs()[num_args_list];
            	if(!KonPropertyFlag.contains(flagName)) {
            		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(holderName));
        			return;
            	}
            	KonPropertyFlag flagArg = KonPropertyFlag.getFlag(flagName);
            	if(!holder.hasPropertyValue(flagArg)) {
            		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(holderName));
        			return;
            	}
            	
            	if (getArgs().length == num_args_disp) {
            		// k admin flag <name> <flag>
            		// Display current value and description of given flag
            		boolean flagValue = holder.getPropertyValue(flagArg);
            		String header = String.format(flagLine, "Flag", "Value", "Description");
            		String line = String.format(flagLine, flagArg.getName(), flagValue, flagArg.getDescription());
            		ChatUtil.sendNotice((Player) getSender(), "Single Property");
            		ChatUtil.sendMessage((Player) getSender(), ChatColor.GOLD+header);
            		ChatUtil.sendMessage((Player) getSender(), line);
            		
            	} else if (getArgs().length == num_args_set) {
            		// k admin flag <name> <flag> <value>
            		// Set new value for flag
            		String flagValue = getArgs()[num_args_set-1];
            		boolean bValue = Boolean.parseBoolean(flagValue);
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
    	// k admin flag [capital] <name> [<flag>] [<value>]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 3) {
			// Suggest all known property holders
			tabList.addAll(getKonquest().getKingdomManager().getKingdomNames());
			tabList.addAll(getKonquest().getKingdomManager().getTownNames());
			tabList.addAll(getKonquest().getSanctuaryManager().getSanctuaryNames());
			tabList.addAll(getKonquest().getRuinManager().getRuinNames());
			tabList.add("capital");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length >= 4) {
			// Conditional suggestion indexes
			boolean isCapital = false;
			String holderName = getArgs()[2];
			if(holderName.equals("capital")) {
				isCapital = true;
			}
			// Number of arguments to suggest kingdoms
			int num_args_kingdom = 3;
			// Number of arguments to suggest properties
			int num_args_properties = 4;
			// Number of arguments to suggest properties
			int num_args_values = 5;
			if(isCapital) {
				num_args_kingdom = 4;
				num_args_properties = 5;
				num_args_values = 6;
			}

			if(getArgs().length == num_args_kingdom) {
				// Suggest kingdoms
				tabList.addAll(getKonquest().getKingdomManager().getKingdomNames());
				// Trim down completion options based on current input
				StringUtil.copyPartialMatches(getArgs()[num_args_kingdom-1], tabList, matchedTabList);
				Collections.sort(matchedTabList);
			} else if(getArgs().length == num_args_properties) {
				// Suggest properties
				tabList.addAll(KonPropertyFlag.getFlagStrings());
				// Trim down completion options based on current input
				StringUtil.copyPartialMatches(getArgs()[num_args_properties-1], tabList, matchedTabList);
				Collections.sort(matchedTabList);
			} else if(getArgs().length == num_args_values) {
				// Suggest values
				tabList.add(String.valueOf(true));
				tabList.add(String.valueOf(false));
				// Trim down completion options based on current input
				StringUtil.copyPartialMatches(getArgs()[num_args_values-1], tabList, matchedTabList);
				Collections.sort(matchedTabList);
			}

		}
		return matchedTabList;
	}
}
