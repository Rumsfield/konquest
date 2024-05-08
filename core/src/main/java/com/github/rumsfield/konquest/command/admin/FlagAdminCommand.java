package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;

/**
 * Modifies property flags of various property holder objects.
 * The following classes are known property holders
 * 	- KonKingdom
 *  - KonCapital
 * 	- KonTown
 * 	- KonSanctuary
 *  - KonRuin
 */
public class FlagAdminCommand extends CommandBase {
	
	public FlagAdminCommand() {
		// Define name and sender support
		super("flag",false, true);
		// Define arguments
		List<String> argNames = Arrays.asList("kingdom", "capital", "town", "sanctuary", "ruin");
		List<String> valueNames = Arrays.asList("true", "false");
		// set kingdom|capital|town|sanctuary|ruin <name> <property> true|false
		addArgument(
				newArg("set",true,false)
						.sub( newArg(argNames,true,false)
								.sub( newArg("name",false,false)
										.sub( newArg("property",false,false)
												.sub( newArg(valueNames,true,false) ) ) ) )
		);
		// show kingdom|capital|town|sanctuary|ruin <name> [<property>]
		addArgument(
				newArg("show",true,false)
						.sub( newArg(argNames,true,false)
								.sub( newArg("name",false,true)
										.sub( newArg("property",false,false) ) ) )
		);
		// reset kingdom|capital|town|sanctuary|ruin <name>
		addArgument(
				newArg("reset",true,false)
						.sub( newArg(argNames,true,false)
								.sub( newArg("name",false,false) ) )
		);
		// resetall kingdom|capital|town|sanctuary|ruin
		addArgument(
				newArg("resetall",true,false)
						.sub( newArg(argNames,true,false) )
		);
    }

	private boolean isHolderType(String type) {
		return type.equalsIgnoreCase("kingdom") ||
				type.equalsIgnoreCase("capital") ||
				type.equalsIgnoreCase("town") ||
				type.equalsIgnoreCase("sanctuary") ||
				type.equalsIgnoreCase("ruin");
	}

	private KonPropertyFlagHolder getFlagHolder(Konquest konquest, String type, String name) {
		KonPropertyFlagHolder holder = null;
		switch(type.toLowerCase()) {
			case "kingdom":
				holder = konquest.getKingdomManager().getKingdom(name);
				break;
			case "capital":
				if(konquest.getKingdomManager().isKingdom(name)) {
					// Found kingdom capital
					holder = konquest.getKingdomManager().getKingdom(name).getCapital();
				}
				break;
			case "town":
				holder = konquest.getKingdomManager().getTown(name);
				break;
			case "sanctuary":
				holder = konquest.getSanctuaryManager().getSanctuary(name);
				break;
			case "ruin":
				holder = konquest.getRuinManager().getRuin(name);
				break;
			default:
				break;
		}
		return holder;
	}

	private ArrayList<KonPropertyFlagHolder> getFlagHolders(Konquest konquest, String type) {
		ArrayList<KonPropertyFlagHolder> holders = new ArrayList<>();
		switch(type.toLowerCase()) {
			case "kingdom":
				holders.addAll(konquest.getKingdomManager().getKingdoms());
				break;
			case "capital":
				for(KonKingdom kingdom : konquest.getKingdomManager().getKingdoms()) {
					holders.add(kingdom.getCapital());
				}
				break;
			case "town":
				for(KonKingdom kingdom : konquest.getKingdomManager().getKingdoms()) {
					holders.addAll(kingdom.getTowns());
				}
				break;
			case "sanctuary":
				holders.addAll(konquest.getSanctuaryManager().getSanctuaries());
				break;
			case "ruin":
				holders.addAll(konquest.getRuinManager().getRuins());
				break;
			default:
				break;
		}
		return holders;
	}

	@Override
    public void execute(Konquest konquest, CommandSender sender, List<String> args) {
		if (args.size() < 2) {
			sendInvalidArgMessage(sender);
		}
		String subCmd = args.get(0);
		String holderType = args.get(1);
		String holderName = "";
		String propertyName = "";
		String valueStr = "";
		KonPropertyFlagHolder holder = null;
		KonPropertyFlag propertyFlag = null;
		boolean propertyValue = false;
		// Check for valid type
		boolean isValidType = holderType.equalsIgnoreCase("kingdom") ||
				holderType.equalsIgnoreCase("capital") ||
				holderType.equalsIgnoreCase("town") ||
				holderType.equalsIgnoreCase("sanctuary") ||
				holderType.equalsIgnoreCase("ruin");
		if (!isValidType) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(holderType));
			return;
		}
		// Check for property holder
		if (args.size() == 3) {
			holderName = args.get(2);
			// Get holder
			holder = getFlagHolder(konquest, holderType, holderName);
			if (holder == null) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(holderName));
				return;
			}
		}
		// Check for property flag
		if (args.size() == 4) {
			propertyName = args.get(3);
			// Get Property
			if(!KonPropertyFlag.contains(propertyName)) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(propertyName));
				return;
			}
			propertyFlag = KonPropertyFlag.getFlag(propertyName);
			if(holder != null && !holder.hasPropertyValue(propertyFlag)) {
				ChatUtil.sendError(sender, MessagePath.COMMAND_ADMIN_FLAG_ERROR_INVALID.getMessage(propertyFlag.toString(),holderType.toLowerCase()));
				return;
			}
		}
		// Check for value
		if (args.size() == 5) {
			valueStr = args.get(4);
			// Get value
			if (valueStr.equalsIgnoreCase("true")) {
				propertyValue = true;
			} else if (valueStr.equalsIgnoreCase("false")) {
				propertyValue = false;
			} else {
				sendInvalidArgMessage(sender);
				return;
			}
		}
		// Parse sub-commands
		switch (subCmd.toLowerCase()) {
			case "set":
				// Set a new property flag value
				if (args.size() != 5) {
					sendInvalidArgMessage(sender);
				}
				if (holder == null || propertyFlag == null) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
					return;
				}
				// Set value
				if(holder.setPropertyValue(propertyFlag, propertyValue)) {
					// Successfully assigned value
					ChatUtil.sendNotice(sender, MessagePath.COMMAND_ADMIN_FLAG_NOTICE_SET.getMessage(propertyFlag.getName(), holderName, propertyValue));
					if(propertyFlag.equals(KonPropertyFlag.ARENA) && (holder instanceof KonSanctuary || holder instanceof KonRuin)) {
						// Update title bar
						((KonBarDisplayer) holder).updateBarTitle();
					}
				} else {
					// Failed to assign value
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_FAILED.getMessage());
					return;
				}
				break;
			case "show":
				// Display a property flag value
				if (args.size() != 3 && args.size() != 4) {
					sendInvalidArgMessage(sender);
				}
				if (args.size() == 3) {
					if (holder == null) {
						ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
						return;
					}
					// Display all property values
					String header = MessagePath.COMMAND_ADMIN_FLAG_NOTICE_ALL_PROPERTIES.getMessage() + " - " +
							holderType + " " + holderName;
					ChatUtil.sendNotice(sender, header);
					for(KonPropertyFlag flag : KonPropertyFlag.values()) {
						if(holder.hasPropertyValue(flag)) {
							String flagName = flag.getName();
							String flagValue = holder.getPropertyValue(flag) ? ""+true : ChatColor.RED + ""+false;
							String flagDescription = flag.getDescription();
							String infoLine = DisplayManager.loreFormat + flag + ": " +
									DisplayManager.valueFormat + flagName + " = " + flagValue +
									ChatColor.RESET + " | " + flagDescription;
							ChatUtil.sendMessage(sender, infoLine);
						}
					}
				} else if (args.size() == 4) {
					if (holder == null || propertyFlag == null) {
						ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
						return;
					}
					// Display single property value
					String header = MessagePath.COMMAND_ADMIN_FLAG_NOTICE_SINGLE_PROPERTY.getMessage() + " - " +
							holderType + " " + holderName;
					ChatUtil.sendNotice(sender, header);
					String flagName = propertyFlag.getName();
					String flagValue = holder.getPropertyValue(propertyFlag) ? ""+true : ChatColor.RED + ""+false;
					String flagDescription = propertyFlag.getDescription();
					String infoLine = DisplayManager.loreFormat + propertyFlag + ": " +
							DisplayManager.valueFormat + flagName + " = " + flagValue +
							ChatColor.RESET + " | " + flagDescription;
					ChatUtil.sendMessage(sender, infoLine);
				}
				break;
			case "reset":
				// Reset a holder from properties.yml
				if (args.size() != 3) {
					sendInvalidArgMessage(sender);
				}
				if (holder == null) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
					return;
				}
				holder.initProperties();
				String singleHeader = MessagePath.COMMAND_ADMIN_FLAG_NOTICE_RESET.getMessage() + " - " +
						holderType + "(" + 1 + ")";
				ChatUtil.sendNotice(sender, singleHeader);
				break;
			case "resetall":
				// Reset all holders of the given type from properties.yml
				if (args.size() != 2) {
					sendInvalidArgMessage(sender);
				}
				ArrayList<KonPropertyFlagHolder> holders = getFlagHolders(konquest, holderType);
				for (KonPropertyFlagHolder aHolder : holders) {
					aHolder.initProperties();
				}
				String multiHeader = MessagePath.COMMAND_ADMIN_FLAG_NOTICE_RESET.getMessage() + " - " +
						holderType + "(" + holders.size() + ")";
				ChatUtil.sendNotice(sender, multiHeader);
				break;
			default:
				sendInvalidArgMessage(sender);
				break;
		}
    }
    
    @Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		if (args.size() == 1) {
			// Suggest sub-commands
			tabList.add("show");
			tabList.add("set");
			tabList.add("reset");
			tabList.add("resetall");
		} else if (args.size() == 2) {
			// Suggest all types
			tabList.add("kingdom");
			tabList.add("capital");
			tabList.add("town");
			tabList.add("sanctuary");
			tabList.add("ruin");
		} else if (args.size() == 3) {
			if (args.get(0).equalsIgnoreCase("set") || args.get(0).equalsIgnoreCase("show") || args.get(0).equalsIgnoreCase("reset")) {
				// Suggest all known property holders
				switch(args.get(1).toLowerCase()) {
					case "kingdom":
					case "capital":
						tabList.addAll(konquest.getKingdomManager().getKingdomNames());
						break;
					case "town":
						tabList.addAll(konquest.getKingdomManager().getTownNames());
						break;
					case "sanctuary":
						tabList.addAll(konquest.getSanctuaryManager().getSanctuaryNames());
						break;
					case "ruin":
						tabList.addAll(konquest.getRuinManager().getRuinNames());
						break;
					default:
						break;
				}
			}
		} else if (args.size() == 4) {
			if (args.get(0).equalsIgnoreCase("set") || args.get(0).equalsIgnoreCase("show")) {
				// Suggest properties
				switch (args.get(1).toLowerCase()) {
					case "kingdom":
						for (KonPropertyFlag flag : KonKingdom.getProperties()) {
							tabList.add(flag.toString());
						}
						break;
					case "capital":
					case "town":
						for (KonPropertyFlag flag : KonTown.getProperties()) {
							tabList.add(flag.toString());
						}
						break;
					case "sanctuary":
						for (KonPropertyFlag flag : KonSanctuary.getProperties()) {
							tabList.add(flag.toString());
						}
						break;
					case "ruin":
						for (KonPropertyFlag flag : KonRuin.getProperties()) {
							tabList.add(flag.toString());
						}
						break;
					default:
						break;
				}
			}
		} else if (args.size() == 5) {
			if (args.get(0).equalsIgnoreCase("set")) {
				tabList.add("true");
				tabList.add("false");
			}
		}
		return matchLastArgToList(tabList,args);
	}
}
