package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.manager.DisplayManager;
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

	/*
	 * The following classes are known property holders
	 * 	- KonKingdom
	 *  - KonCapital
	 * 	- KonTown
	 * 	- KonSanctuary
	 *  - KonRuin
	 */

    public void execute() {
    	// k admin flag kingdom|capital|town|sanctuary|ruin <name> [<flag>] [<value>]
		Player bukkitPlayer = (Player) getSender();
    	if (getArgs().length != 4 && getArgs().length != 5 && getArgs().length != 6) {
			sendInvalidArgMessage(bukkitPlayer, AdminCommandType.FLAG);
		} else {

			String holderType = getArgs()[2];
			String holderName = getArgs()[3];
			KonPropertyFlagHolder holder = null;

    		// Check for valid holder name
			switch(holderType) {
				case "kingdom":
					holder = getKonquest().getKingdomManager().getKingdom(holderName);
					break;
				case "capital":
					if(getKonquest().getKingdomManager().isKingdom(holderName)) {
						// Found kingdom capital
						holder = getKonquest().getKingdomManager().getKingdom(holderName).getCapital();
					}
					break;
				case "town":
					holder = getKonquest().getKingdomManager().getTown(holderName);
					break;
				case "sanctuary":
					holder = getKonquest().getSanctuaryManager().getSanctuary(holderName);
					break;
				case "ruin":
					holder = getKonquest().getRuinManager().getRuin(holderName);
					break;
				default:
					break;
			}
    		if(holder == null) {
    			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(holderName));
    			return;
    		}
    		
    		if (getArgs().length == 4) {
    			// k admin flag kingdom|capital|town|sanctuary|ruin <name>
        		// List available flags for the given property holder
        		Map<KonPropertyFlag,Boolean> holderFlags = holder.getAllProperties();
        		
        		// List all holder properties and values
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_FLAG_NOTICE_ALL_PROPERTIES.getMessage());
                for(KonPropertyFlag flag : holderFlags.keySet()) {
					String flagLine = "| "+DisplayManager.loreFormat+flag.getName()+": "+DisplayManager.valueFormat+holderFlags.get(flag);
					String descLine = "\\-- "+flag.getDescription();
                	ChatUtil.sendMessage((Player) getSender(), flagLine);
					ChatUtil.sendMessage((Player) getSender(), descLine);
                }

            } else if (getArgs().length > 4) {
            	
            	String flagName = getArgs()[4];
            	if(!KonPropertyFlag.contains(flagName)) {
            		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(flagName));
        			return;
            	}
            	KonPropertyFlag flagArg = KonPropertyFlag.getFlag(flagName);
            	if(!holder.hasPropertyValue(flagArg)) {
            		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(flagName));
        			return;
            	}
            	
            	if (getArgs().length == 5) {
            		// k admin flag kingdom|capital|town|sanctuary|ruin <name> <flag>
            		// Display current value and description of given flag
					ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_FLAG_NOTICE_SINGLE_PROPERTY.getMessage());
            		boolean flagValue = holder.getPropertyValue(flagArg);
					String flagLine = "| "+DisplayManager.loreFormat+flagArg.getName()+": "+DisplayManager.valueFormat+flagValue;
					String descLine = "\\-- "+flagArg.getDescription();
					ChatUtil.sendMessage(bukkitPlayer, flagLine);
					ChatUtil.sendMessage(bukkitPlayer, descLine);
            		
            	} else if (getArgs().length == 6) {
            		// k admin flag kingdom|capital|town|sanctuary|ruin <name> <flag> <value>
            		// Set new value for flag
            		String flagValue = getArgs()[5];
            		boolean bValue = Boolean.parseBoolean(flagValue);
            		boolean status = holder.setPropertyValue(flagArg, bValue);
            		if(status) {
            			// Successfully assigned value
            			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_FLAG_NOTICE_SET.getMessage(flagArg.getName(), holderName, bValue));
            		} else {
            			// Failed to assign value
            			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_FAILED.getMessage());
            		}
            	}
            }	
    	}
    }
    
    @Override
	public List<String> tabComplete() {
    	// k admin flag kingdom|capital|town|sanctuary|ruin <name> [<flag>] [<value>]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();

		if(getArgs().length == 3) {
			// Suggest all types
			tabList.add("kingdom");
			tabList.add("capital");
			tabList.add("town");
			tabList.add("sanctuary");
			tabList.add("ruin");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length > 3) {
			String holderType = getArgs()[2];

			if(getArgs().length == 4) {
				// Suggest all known property holders
				switch(holderType) {
					case "kingdom":
					case "capital":
						tabList.addAll(getKonquest().getKingdomManager().getKingdomNames());
						break;
					case "town":
						tabList.addAll(getKonquest().getKingdomManager().getTownNames());
						break;
					case "sanctuary":
						tabList.addAll(getKonquest().getSanctuaryManager().getSanctuaryNames());
						break;
					case "ruin":
						tabList.addAll(getKonquest().getRuinManager().getRuinNames());
						break;
					default:
						break;
				}
				// Trim down completion options based on current input
				StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
				Collections.sort(matchedTabList);
			} else if(getArgs().length == 5) {
				// Suggest properties
				String holderName = getArgs()[3];
				KonPropertyFlagHolder holder = null;
				switch(holderType) {
					case "kingdom":
						holder = getKonquest().getKingdomManager().getKingdom(holderName);
						break;
					case "capital":
						if(getKonquest().getKingdomManager().isKingdom(holderName)) {
							// Found kingdom capital
							holder = getKonquest().getKingdomManager().getKingdom(holderName).getCapital();
						}
						break;
					case "town":
						holder = getKonquest().getKingdomManager().getTown(holderName);
						break;
					case "sanctuary":
						holder = getKonquest().getSanctuaryManager().getSanctuary(holderName);
						break;
					case "ruin":
						holder = getKonquest().getRuinManager().getRuin(holderName);
						break;
					default:
						break;
				}
				if(holder != null) {
					for(KonPropertyFlag flag : holder.getAllProperties().keySet()) {
						tabList.add(flag.toString());
					}
				}
				// Trim down completion options based on current input
				StringUtil.copyPartialMatches(getArgs()[4], tabList, matchedTabList);
				Collections.sort(matchedTabList);
			} else if(getArgs().length == 6) {
				// Suggest values
				tabList.add(String.valueOf(true));
				tabList.add(String.valueOf(false));
				// Trim down completion options based on current input
				StringUtil.copyPartialMatches(getArgs()[5], tabList, matchedTabList);
				Collections.sort(matchedTabList);
			}


		}


		return matchedTabList;
	}
}
