package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonKingdom;
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
    	// k admin flag kingdom|capital|town|sanctuary|ruin <name>|all [<flag>|reset] [<value>]
		Player bukkitPlayer = (Player) getSender();
    	if (getArgs().length != 4 && getArgs().length != 5 && getArgs().length != 6) {
			sendInvalidArgMessage(bukkitPlayer, AdminCommandType.FLAG);
		} else {

			String holderType = getArgs()[2];
			holderType = holderType.toLowerCase();
			String holderName = getArgs()[3];
			ArrayList<KonPropertyFlagHolder> holders = new ArrayList<>();
			boolean isAll = false;

    		// Check for valid holder name
			if (holderName.equalsIgnoreCase("all")) {
				isAll = true;
				// Operation on all holders of specified type
				switch(holderType) {
					case "kingdom":
						holders.addAll(getKonquest().getKingdomManager().getKingdoms());
						break;
					case "capital":
						for(KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
							holders.add(kingdom.getCapital());
						}
						break;
					case "town":
						for(KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
							holders.addAll(kingdom.getTowns());
						}
						break;
					case "sanctuary":
						holders.addAll(getKonquest().getSanctuaryManager().getSanctuaries());
						break;
					case "ruin":
						holders.addAll(getKonquest().getRuinManager().getRuins());
						break;
					default:
						break;
				}
			} else {
				// Operation on single holder of given type
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
				if(holder == null) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(holderName));
					return;
				}
				holders.add(holder);
			}
			// At this point, holders may be empty if "all" was used, else it should have a single element.
			KonPropertyFlagHolder infoHolder = null;
			if(!holders.isEmpty()) {
				infoHolder = holders.get(0);
			}
			int numHolders = holders.size();

    		if (getArgs().length == 4) {
    			// k admin flag kingdom|capital|town|sanctuary|ruin <name>|all
				if (isAll) {
					// When multiple holders, show amount of holders and common flags
					String header = MessagePath.COMMAND_ADMIN_FLAG_NOTICE_ALL_PROPERTIES.getMessage() + " - " +
							holderType + "(" + numHolders + ")";
					ChatUtil.sendNotice(bukkitPlayer, header);
					if(infoHolder != null) {
						Map<KonPropertyFlag,Boolean> holderFlags = infoHolder.getAllProperties();
						for(KonPropertyFlag flag : holderFlags.keySet()) {
							String flagName = flag.getName();
							String flagDescription = flag.getDescription();
							String infoLine = DisplayManager.loreFormat + flag + ": " + flagName +
									ChatColor.RESET + " | " + flagDescription;
							ChatUtil.sendMessage((Player) getSender(), infoLine);
						}
					}
				} else {
					// When single holder, list all flags with current values
					String header = MessagePath.COMMAND_ADMIN_FLAG_NOTICE_ALL_PROPERTIES.getMessage() + " - " +
							holderType + " " + holderName;
					ChatUtil.sendNotice(bukkitPlayer, header);
					if(infoHolder != null) {
						Map<KonPropertyFlag,Boolean> holderFlags = infoHolder.getAllProperties();
						for(KonPropertyFlag flag : holderFlags.keySet()) {
							String flagName = flag.getName();
							String flagValue = String.valueOf(holderFlags.get(flag));
							String flagDescription = flag.getDescription();
							String infoLine = DisplayManager.loreFormat + flag + ": " + flagName +
									DisplayManager.valueFormat + " = " + flagValue +
									ChatColor.RESET + " | " + flagDescription;
							ChatUtil.sendMessage((Player) getSender(), infoLine);
						}
					}
				}

            } else if (getArgs().length > 4) {
            	// Attempt to display a valid flag, or reset all flags, for the given holder(s)
            	String flagNameArg = getArgs()[4];
				KonPropertyFlag flag = KonPropertyFlag.getFlag(flagNameArg);
				if(flagNameArg.equalsIgnoreCase("reset")) {
					// When reset, revert all flags to the value stored in properties.yml
					for (KonPropertyFlagHolder resetHolder : holders) {
						resetHolder.initProperties();
					}
					String header = MessagePath.COMMAND_ADMIN_FLAG_NOTICE_RESET.getMessage() + " - " +
							holderType + "(" + numHolders + ")";
					ChatUtil.sendNotice(bukkitPlayer, header);
					return;
				} else {
					if(!KonPropertyFlag.contains(flagNameArg) || infoHolder == null) {
						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(flagNameArg));
						return;
					}
					if(!infoHolder.hasPropertyValue(flag)) {
						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(flagNameArg));
						return;
					}
				}
            	
            	if (getArgs().length == 5) {
            		// k admin flag kingdom|capital|town|sanctuary|ruin <name>|all <flag>
					// Display single flag value(s)
					if (isAll) {
						// When multiple holders, display the flag and the number of true/false
						int numTrue = 0;
						int numFalse = 0;
						for (KonPropertyFlagHolder countHolder : holders) {
							if(countHolder.getPropertyValue(flag)) {
								numTrue++;
							} else {
								numFalse++;
							}
						}
						String header = MessagePath.COMMAND_ADMIN_FLAG_NOTICE_SINGLE_PROPERTY.getMessage() + " - " +
								holderType + "(" + numHolders + ")";
						ChatUtil.sendNotice(bukkitPlayer, header);
						String flagName = flag.getName();
						String flagValues = true + "(" + numTrue + "), " + false + "(" + numFalse + ")";
						String flagDescription = flag.getDescription();
						String infoLine = DisplayManager.loreFormat + flag + ": " + flagName +
								DisplayManager.valueFormat + " = " + flagValues +
								ChatColor.RESET + " | " + flagDescription;
						ChatUtil.sendMessage((Player) getSender(), infoLine);
					} else {
						// When single holder, display the value of the flag
						String header = MessagePath.COMMAND_ADMIN_FLAG_NOTICE_SINGLE_PROPERTY.getMessage() + " - " +
								holderType + " " + holderName;
						ChatUtil.sendNotice(bukkitPlayer, header);
						String flagName = flag.getName();
						String flagValue = String.valueOf(infoHolder.getPropertyValue(flag));
						String flagDescription = flag.getDescription();
						String infoLine = DisplayManager.loreFormat + flag + ": " + flagName +
								DisplayManager.valueFormat + " = " + flagValue +
								ChatColor.RESET + " | " + flagDescription;
						ChatUtil.sendMessage((Player) getSender(), infoLine);
					}
            	} else if (getArgs().length == 6) {
            		// k admin flag kingdom|capital|town|sanctuary|ruin <name>|all <flag> <value>
            		// Set new value for flag
            		String flagValueArg = getArgs()[5];
            		boolean bValue = Boolean.parseBoolean(flagValueArg);
					for (KonPropertyFlagHolder setHolder : holders) {
						if(setHolder.setPropertyValue(flag, bValue)) {
							// Successfully assigned value
							ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_FLAG_NOTICE_SET.getMessage(flag.getName(), holderName, bValue));
						} else {
							// Failed to assign value
							ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_FAILED.getMessage());
							return;
						}
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
				tabList.add("all");
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
				tabList.add("reset");
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
