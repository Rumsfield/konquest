package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.api.model.KonquestUpgrade;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.util.StringUtil;

import java.util.*;

public class TownAdminCommand extends CommandBase {
	
	public TownAdminCommand() {
		// Define name and sender support
		super("town",false, true);
		// Define arguments
		// menu <town>
		addArgument(
				newArg("menu",true,false)
						.sub( newArg("town",false,false) )
		);
		// create <name> <kingdom>
		addArgument(
				newArg("create",true,false)
						.sub( newArg("name",false,false)
								.sub( newArg("kingdom",false,false) ) )
		);
		// remove <town> <kingdom>
		addArgument(
				newArg("remove",true,false)
						.sub( newArg("town",false,false)
								.sub( newArg("kingdom",false,false) ) )
		);
		// rename <town> <name>
		addArgument(
				newArg("rename",true,false)
						.sub( newArg("town",false,false)
								.sub( newArg("name",false,false) ) )
		);
		// plots <town>
		addArgument(
				newArg("plots",true,false)
						.sub( newArg("town",false,false) )
		);
		// specialize <town> [<name>]
		addArgument(
				newArg("specialize",true,false)
						.sub( newArg("town",false,true)
								.sub( newArg("name",false,false) ) )
		);
		// upgrade <town> [<name>] <level>
		addArgument(
				newArg("upgrade",true,false)
						.sub( newArg("town",false,true)
								.sub( newArg("name",false,false)
										.sub( newArg("level",false,false) ) ) )
		);
		// option <town> [<name>] [true|false]
		List<String> optionsArgNames = Arrays.asList("true", "false");
		addArgument(
				newArg("upgrade",true,false)
						.sub( newArg("town",false,true)
								.sub( newArg("name",false,true)
										.sub( newArg(optionsArgNames,true,false) ) ) )
		);
		// shield <town> [clear|set|add] <amount>
		List<String> shieldArmorArgNames = Arrays.asList("clear", "set", "add");
		addArgument(
				newArg("shield",true,false)
						.sub( newArg("town",false,true)
								.sub( newArg(shieldArmorArgNames,true,false)
										.sub( newArg("amount",false,false) ) ) )
		);
		// armor <town> [clear|set|add] <amount>
		addArgument(
				newArg("armor",true,false)
						.sub( newArg("town",false,true)
								.sub( newArg(shieldArmorArgNames,true,false)
										.sub( newArg("amount",false,false) ) ) )
		);
		// resident <town> add|kick|promote|demote|lord <player>
		List<String> residentArgNames = Arrays.asList("add", "kick", "promote", "demote", "lord");
		addArgument(
				newArg("resident",true,false)
						.sub( newArg("town",false,false)
								.sub( newArg(residentArgNames,true,false)
										.sub( newArg("player",false,false) ) ) )
		);
    }

	private String formatOptionLine(KonTownOption option, KonTown town) {
		boolean val = town.getTownOption(option);
		String optionLabel = option.toString();
		String optionValue = DisplayManager.boolean2Lang(val) + " " + DisplayManager.boolean2Symbol(val);
		String optionDescription = option.getDescription();
		return ChatColor.GOLD +
				optionLabel +
				" - " +
				ChatColor.RESET +
				optionValue +
				" " +
				ChatColor.LIGHT_PURPLE +
				optionDescription;
	}

	@Override
	public void execute(Konquest konquest, CommandSender sender, List<String> args) {
		// Sender can be player or console

		// Check if sender is player
		KonPlayer player = konquest.getPlayerManager().getPlayer(sender);
		boolean isSenderPlayer = (player != null);
		// Get sub-command and target kingdom
		if (args.size() < 2) {
			sendInvalidArgMessage(sender);
			return;
		}
		String subCmd = args.get(0);
		String townName = args.get(1);

		// Get town from name
		KonTown town = null;
		if (!subCmd.equalsIgnoreCase("create")) {
			// All sub-commands but create need the town
			town = konquest.getKingdomManager().getTownCapital(townName);
			if (town == null) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(townName));
				return;
			}
		}

		// Execute sub-commands
		switch (subCmd.toLowerCase()) {
			case "menu":
				// Open town management menu
				assert town != null;
				// Sender must be a player
				if (!isSenderPlayer) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_PLAYER.getMessage());
					return;
				}
				// Display menu
				konquest.getDisplayManager().displayTownManagementMenu(player, town, true);
				break;
			case "create":
				// Make a new town
				// Sender must be a player
				if (!isSenderPlayer) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_PLAYER.getMessage());
					return;
				}
				// Get kingdom name
				if (args.size() != 3) {
					sendInvalidArgMessage(sender);
					return;
				}
				String createKingdomName = args.get(2);
				// Check town name
				if (konquest.validateName(townName,sender) != 0) {
					// Messages handled within method
					return;
				}
				// Verify kingdom name
				if (!konquest.getKingdomManager().isKingdom(createKingdomName)) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(createKingdomName));
					return;
				}
				// Check settle location
				Location settleLoc = player.getBukkitPlayer().getLocation();
				if (!konquest.isWorldValid(settleLoc.getWorld())) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
					return;
				}
				// Attempt to create the town
				int exitCode = konquest.getKingdomManager().createTown(settleLoc, townName, createKingdomName);
				if (exitCode == 0) {
					KonTown createdTown = konquest.getKingdomManager().getTown(townName);
					if(createdTown != null) {
						player.getBukkitPlayer().teleport(konquest.getKingdomManager().getKingdom(createKingdomName).getTown(townName).getSpawnLoc());
						// Update labels
						konquest.getMapHandler().drawLabel(createdTown);
						konquest.getMapHandler().drawLabel(createdTown.getKingdom().getCapital());
						ChatUtil.sendNotice(sender, MessagePath.COMMAND_SETTLE_NOTICE_SUCCESS.getMessage(townName));
					} else {
						ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
						return;
					}
				} else {
					int distance;
					switch(exitCode) {
						case 1:
							ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_OVERLAP.getMessage());
							break;
						case 2:
							ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_PLACEMENT.getMessage());
							break;
						case 3:
							ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_NAME.getMessage());
							break;
						case 4:
							ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_TEMPLATE.getMessage());
							break;
						case 5:
							ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
							break;
						case 6:
							distance = konquest.getTerritoryManager().getDistanceToClosestTerritory(settleLoc);
							int min_distance_sanc = konquest.getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_SANCTUARY.getPath());
							int min_distance_town = konquest.getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_TOWN.getPath());
							int min_distance = Math.min(min_distance_sanc, min_distance_town);
							ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_PROXIMITY.getMessage(distance,min_distance));
							break;
						case 7:
							distance = konquest.getTerritoryManager().getDistanceToClosestTerritory(settleLoc);
							int max_distance_all = konquest.getCore().getInt(CorePath.TOWNS_MAX_DISTANCE_ALL.getPath());
							ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_MAX.getMessage(distance,max_distance_all));
							break;
						case 21:
							ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
							break;
						case 22:
							ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_FLAT.getMessage());
							break;
						case 23:
							ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_HEIGHT.getMessage());
							break;
						case 12:
							ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_HEIGHT.getMessage());
							break;
						case 13:
							ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_INIT.getMessage());
							break;
						case 14:
							ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_AIR.getMessage());
							break;
						case 15:
							ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_WATER.getMessage());
							break;
						case 16:
							ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_CONTAINER.getMessage());
							break;
						default:
							ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
							return;
					}
				}
				break;
			case "remove":
				// Remove a town, cannot remove a capital
				assert town != null;
				if (args.size() != 3) {
					sendInvalidArgMessage(sender);
					return;
				}
				String removeKingdomName = args.get(2);
				// Verify kingdom name
				if (!konquest.getKingdomManager().isKingdom(removeKingdomName)) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(removeKingdomName));
					return;
				}
				// Verify town is not a capital
				if (town.getTerritoryType().equals(KonquestTerritoryType.CAPITAL)) {
					// Cannot remove the capital with this command
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_BAD_NAME.getMessage(townName));
					return;
				}
				// Attempt to remove the town
				if (konquest.getKingdomManager().removeTown(townName, removeKingdomName)) {
					ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
				} else {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(townName));
					return;
				}
				break;
			case "rename":
				// Set a new town name
				assert town != null;
				if(args.size() != 3) {
					sendInvalidArgMessage(sender);
					return;
				}
				String newTownName = args.get(2);
				// Rename the town
				if(town.getKingdom().renameTown(townName, newTownName)) {
					ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
				} else {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_FAILED.getMessage());
				}
				break;
			case "specialize":
				// View or set the town trade specialization
				assert town != null;
				// Check for enabled feature
				if (!konquest.getKingdomManager().getIsDiscountEnable()) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
					return;
				}
				if (args.size() == 2) {
					// Display current specialization
					String specialName = town.getSpecialization().toString();
					ChatUtil.sendNotice(sender, MessagePath.COMMAND_TOWN_NOTICE_SPECIALIZE_LIST.getMessage(town.getName(), specialName));
				} else if (args.size() == 3) {
					// Set new specialization
					String professionName = args.get(2);
					Villager.Profession profession;
					try {
						profession = Villager.Profession.valueOf(professionName.toUpperCase());
					} catch (IllegalArgumentException ex) {
						ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(professionName));
						return;
					}
					konquest.getKingdomManager().menuChangeTownSpecialization(town, profession, null, sender, true);
				} else {
					sendInvalidArgMessage(sender);
					return;
				}
				break;
			case "upgrade":
				// View or set the town trade specialization
				assert town != null;
				// Check for enabled feature
				if (!konquest.getUpgradeManager().isEnabled()) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
					return;
				}
				// Get available upgrades
				HashMap<KonquestUpgrade, Integer> availableUpgrades = konquest.getUpgradeManager().getAvailableUpgrades(town);
				if (args.size() == 2) {
					// No upgrade given, display available upgrades
					// Place upgrades in ordinal order into list
					List<KonUpgrade> allUpgrades = new ArrayList<>();
					for (KonUpgrade upgrade : KonUpgrade.values()) {
						if (availableUpgrades.containsKey(upgrade)) {
							allUpgrades.add(upgrade);
						}
					}
					ChatUtil.sendNotice(sender, MessagePath.MENU_UPGRADE_TITLE.getMessage());
					for (KonUpgrade upgrade : allUpgrades) {
						int upgradeLevel = availableUpgrades.get(upgrade);
						String upgradeInfo = ChatColor.GOLD +
								upgrade.toString() +
								" - " +
								ChatColor.RESET +
								upgrade.getDescription() +
								" " +
								upgradeLevel +
								" " +
								ChatColor.LIGHT_PURPLE +
								upgrade.getLevelDescription(upgradeLevel);
						ChatUtil.sendMessage(sender, upgradeInfo);
					}
				} else if (args.size() == 4) {
					// Try to apply given upgrade with given level
					String upgradeName = args.get(2);
					String upgradeLevelStr = args.get(3);
					KonUpgrade upgrade = KonUpgrade.getUpgrade(upgradeName);
					if (upgrade == null) {
						ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(upgradeName));
						return;
					}
					int upgradeLevel;
					try {
						upgradeLevel = Integer.parseInt(upgradeLevelStr);
					} catch(NumberFormatException e) {
						ChatUtil.printDebug("Failed to parse string as int: "+e.getMessage());
						sendInvalidArgMessage(sender);
						return;
					}
					if(upgradeLevel < 0 || upgradeLevel > upgrade.getMaxLevel()) {
						ChatUtil.sendError(sender, MessagePath.COMMAND_TOWN_ERROR_UPGRADE_LEVEL.getMessage(0,upgrade.getMaxLevel()));
						return;
					}
					// Set town upgrade and level
					if (konquest.getUpgradeManager().applyTownUpgrade(town, upgrade, upgradeLevel)) {
						ChatUtil.sendNotice(sender, MessagePath.MENU_UPGRADE_ADD.getMessage(upgrade.getDescription(),upgradeLevel,town.getName()));
					}
				} else {
					sendInvalidArgMessage(sender);
				}
				break;
			case "option":
				// Set town options
				assert town != null;
				if (args.size() == 2) {
					// Display all options
					for (KonTownOption option : KonTownOption.values()) {
						if (konquest.getKingdomManager().isTownOptionFeatureEnabled(option)) {
							String optionInfo = formatOptionLine(option, town);
							ChatUtil.sendMessage(sender, optionInfo);
						}
					}
				} else if (args.size() >= 3) {
					String optionName = args.get(2);
					KonTownOption option = KonTownOption.getOption(optionName);
					if (option == null) {
						ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(optionName));
						return;
					}
					if (!konquest.getKingdomManager().isTownOptionFeatureEnabled(option)) {
						ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
						return;
					}
					if (args.size() == 3) {
						// Display option value
						String optionInfo = formatOptionLine(option, town);
						ChatUtil.sendMessage(sender, optionInfo);
					} else if (args.size() == 4) {
						// Set new value
						String newValStr = args.get(3);
						if (newValStr.equalsIgnoreCase("true")) {
							konquest.getKingdomManager().setTownOption(option, town, sender, true);
						} else if (newValStr.equalsIgnoreCase("false")) {
							konquest.getKingdomManager().setTownOption(option, town, sender, false);
						} else {
							// Unknown value
							ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(newValStr));
							return;
						}
					} else {
						sendInvalidArgMessage(sender);
					}
				} else {
					sendInvalidArgMessage(sender);
				}
				break;
			case "shield":
				// Modify town shields
				assert town != null;
				// Check for enabled feature
				if (!konquest.getShieldManager().isShieldsEnabled()) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
					return;
				}
				// Parse arguments
				if (args.size() == 2) {
					// Display current shield time
					String shieldTime = Konquest.getTimeFormat(town.getRemainingShieldTimeSeconds(),"");
					ChatUtil.sendNotice(sender, MessagePath.COMMAND_TOWN_NOTICE_SHIELD_LIST.getMessage(shieldTime, town.getName()));
				} else if (args.size() == 3) {
					// Parse sub-command
					if (args.get(2).equalsIgnoreCase("clear")) {
						town.deactivateShield();
						ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
					} else {
						sendInvalidArgMessage(sender);
					}
				} else if (args.size() == 4) {
					// Modify shield time
					String shieldValStr = args.get(3);
					int shieldVal;
					try {
						shieldVal = Integer.parseInt(shieldValStr);
					} catch(NumberFormatException e) {
						ChatUtil.printDebug("Failed to parse string as int: "+e.getMessage());
						sendInvalidArgMessage(sender);
						return;
					}
					// Parse remaining sub-command options
					if(args.get(2).equalsIgnoreCase("set")) {
						if(konquest.getShieldManager().shieldSet(town, shieldVal)) {
							ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
						} else {
							// Shields cannot be negative
							sendInvalidArgMessage(sender);
						}
					} else if(args.get(2).equalsIgnoreCase("add")) {
						if(konquest.getShieldManager().shieldAdd(town, shieldVal)) {
							ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
						} else {
							// Shields cannot be negative
							sendInvalidArgMessage(sender);
						}
					} else {
						sendInvalidArgMessage(sender);
					}
				} else {
					sendInvalidArgMessage(sender);
				}
				break;
			case "armor":
				// Modify town armor
				assert town != null;
				// Check for enabled feature
				if (!konquest.getShieldManager().isArmorsEnabled()) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
					return;
				}
				// Parse arguments
				if (args.size() == 2) {
					// Display current armor amount
					String armorBlocks = ""+town.getArmorBlocks();
					ChatUtil.sendNotice(sender, MessagePath.COMMAND_TOWN_NOTICE_ARMOR_LIST.getMessage(armorBlocks, town.getName()));
				} else if (args.size() == 3) {
					// Parse sub-command
					if (args.get(2).equalsIgnoreCase("clear")) {
						town.deactivateArmor();
						ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
					} else {
						sendInvalidArgMessage(sender);
					}
				} else if (args.size() == 4) {
					// Modify armor amount
					String armorValStr = args.get(3);
					int armorVal;
					try {
						armorVal = Integer.parseInt(armorValStr);
					} catch(NumberFormatException e) {
						ChatUtil.printDebug("Failed to parse string as int: "+e.getMessage());
						sendInvalidArgMessage(sender);
						return;
					}
					// Parse remaining sub-command options
					if(args.get(2).equalsIgnoreCase("set")) {
						if(konquest.getShieldManager().armorSet(town, armorVal)) {
							ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
						} else {
							ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_FAILED.getMessage());
						}
					} else if(args.get(2).equalsIgnoreCase("add")) {
						if(konquest.getShieldManager().armorAdd(town, armorVal)) {
							ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
						} else {
							ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_FAILED.getMessage());
						}
					} else {
						sendInvalidArgMessage(sender);
					}
				} else {
					sendInvalidArgMessage(sender);
				}
				break;
			case "resident":
				if (args.size() != 4) {
					sendInvalidArgMessage(sender);
				}
				// Manage town residents
				assert town != null;
				String residentSubCmd = args.get(2);
				String residentPlayerName = args.get(3);
				KonOfflinePlayer residentPlayer = konquest.getPlayerManager().getOfflinePlayerFromName(residentPlayerName);
				if(residentPlayer == null) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(residentPlayerName));
					return;
				}
				UUID residentID = residentPlayer.getOfflineBukkitPlayer().getUniqueId();
				String playerName = residentPlayer.getOfflineBukkitPlayer().getName();
				// Parse sub-commands
				switch(residentSubCmd.toLowerCase()) {
					case "add":
						// Check for matching kingdom
						if(!residentPlayer.getKingdom().equals(town.getKingdom())) {
							ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
							return;
						}
						// Add the player as a resident
						if(town.addPlayerResident(residentPlayer.getOfflineBukkitPlayer(),false)) {
							town.removeJoinRequest(residentID);
							ChatUtil.sendNotice(sender, MessagePath.COMMAND_TOWN_NOTICE_ADD_RESIDENT.getMessage(playerName,townName));
						} else {
							// Player is already a resident
							ChatUtil.sendError(sender, MessagePath.COMMAND_TOWN_ERROR_INVITE_MEMBER.getMessage(playerName));
						}
						break;
					case "kick":
						// Remove the player as a resident
						if(town.removePlayerResident(residentPlayer.getOfflineBukkitPlayer())) {
							town.removeJoinRequest(residentID);
							ChatUtil.sendNotice(sender, MessagePath.COMMAND_TOWN_NOTICE_KICK_RESIDENT.getMessage(playerName,townName));
						} else {
							// Player was not a resident
							ChatUtil.sendError(sender, MessagePath.COMMAND_TOWN_ERROR_KICK_FAIL.getMessage(playerName,townName));
						}
						break;
					case "promote":
						// Check for resident
						if (!town.isPlayerResident(residentPlayer.getOfflineBukkitPlayer())) {
							ChatUtil.sendError(sender, MessagePath.COMMAND_TOWN_ERROR_KNIGHT_RESIDENT.getMessage());
							return;
						}
						// Check for lord
						if(town.isPlayerLord(residentPlayer.getOfflineBukkitPlayer())) {
							ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
							return;
						}
						// Promote the resident to knight
						if(town.setPlayerKnight(residentPlayer.getOfflineBukkitPlayer(), true)) {
							ChatUtil.sendNotice(sender, MessagePath.COMMAND_TOWN_NOTICE_KNIGHT_SET.getMessage(playerName,townName));
						} else {
							ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
						}
						break;
					case "demote":
						// Check for resident
						if (!town.isPlayerResident(residentPlayer.getOfflineBukkitPlayer())) {
							ChatUtil.sendError(sender, MessagePath.COMMAND_TOWN_ERROR_KNIGHT_RESIDENT.getMessage());
							return;
						}
						// Check for lord
						if(town.isPlayerLord(residentPlayer.getOfflineBukkitPlayer())) {
							ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
							return;
						}
						// Demote the knight to resident
						if(town.setPlayerKnight(residentPlayer.getOfflineBukkitPlayer(), false)) {
							ChatUtil.sendNotice(sender, MessagePath.COMMAND_TOWN_NOTICE_KNIGHT_CLEAR.getMessage(playerName,townName));
						} else {
							ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
						}
						break;
					case "lord":
						// Check for matching kingdom
						if(!residentPlayer.getKingdom().equals(town.getKingdom())) {
							ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
							return;
						}
						// Make the player into the new town lord
						town.setPlayerLord(residentPlayer.getOfflineBukkitPlayer());
						town.removeJoinRequest(residentID);
						ChatUtil.sendNotice(sender, MessagePath.COMMAND_TOWN_NOTICE_LORD_SUCCESS.getMessage(townName,playerName));
						break;
					default:
						sendInvalidArgMessage(sender);
						return;
				}
				// Update membership stats for online players
				KonPlayer onlinePlayer = konquest.getPlayerManager().getPlayerFromID(residentID);
				if (onlinePlayer != null) {
					// Player is valid and online
					konquest.getKingdomManager().updatePlayerMembershipStats(onlinePlayer);
				}
				break;
			case "plots":
				// Open the town plot management menu
				assert town != null;
				// Sender must be a player
				if (!isSenderPlayer) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_PLAYER.getMessage());
					return;
				}
				konquest.getDisplayManager().displayTownPlotMenu(player.getBukkitPlayer(), town);
				break;
		}
	}

	@Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		int numArgs = args.size();
		if (numArgs == 1) {
			// suggest sub-commands
			tabList.add("menu");
			tabList.add("create");
			tabList.add("remove");
			tabList.add("rename");
			tabList.add("option");
			tabList.add("resident");
			if (konquest.getKingdomManager().getIsDiscountEnable()) {
				tabList.add("specialize");
			}
			if (konquest.getShieldManager().isArmorsEnabled()) {
				tabList.add("armor");
			}
			if (konquest.getShieldManager().isShieldsEnabled()) {
				tabList.add("shield");
			}
			if (konquest.getUpgradeManager().isEnabled()) {
				tabList.add("upgrade");
			}
			if (konquest.getPlotManager().isEnabled()) {
				tabList.add("plots");
			}
		} else if (numArgs == 2) {
			String subCmd = args.get(0);
			if (subCmd.equalsIgnoreCase("create")) {
				// Suggest new name
				tabList.add("***");
			} else if (subCmd.equalsIgnoreCase("remove")) {
				// Suggest existing town names only
				tabList.addAll(konquest.getKingdomManager().getTownNames());
			} else {
				// Suggest all town and capital names
				tabList.addAll(konquest.getKingdomManager().getTownNames());
				tabList.addAll(konquest.getKingdomManager().getKingdomNames());
			}
		} else if (numArgs == 3) {
			switch (args.get(0).toLowerCase()) {
				case "create":
				case "remove":
					// Existing kingdom names
					tabList.addAll(konquest.getKingdomManager().getKingdomNames());
					break;
				case "rename":
					// New town name
					tabList.add("***");
					break;
				case "specialize":
					if (konquest.getKingdomManager().getIsDiscountEnable()) {
						for (Villager.Profession profession : Villager.Profession.values()) {
							tabList.add(profession.toString());
						}
					}
					break;
				case "upgrade":
					if (konquest.getUpgradeManager().isEnabled()) {
						for (KonUpgrade upgrade : KonUpgrade.values()) {
							tabList.add(upgrade.toString());
						}
					}
					break;
				case "option":
					for (KonTownOption option : KonTownOption.values()) {
						tabList.add(option.toString());
					}
					break;
				case "shield":
					if (konquest.getShieldManager().isShieldsEnabled()) {
						tabList.add("clear");
						tabList.add("set");
						tabList.add("add");
					}
					break;
				case "armor":
					if (konquest.getShieldManager().isArmorsEnabled()) {
						tabList.add("clear");
						tabList.add("set");
						tabList.add("add");
					}
					break;
				case "resident":
					tabList.add("add");
					tabList.add("kick");
					tabList.add("promote");
					tabList.add("demote");
					tabList.add("lord");
					break;
			}
		} else if (numArgs == 4) {
			switch (args.get(0).toLowerCase()) {
				case "shield":
				case "armor":
					if (args.get(2).equalsIgnoreCase("set") || args.get(2).equalsIgnoreCase("add")) {
						tabList.add("#");
					}
					break;
				case "upgrade":
					tabList.add("#");
					break;
				case "option":
					tabList.add("true");
					tabList.add("false");
					break;
				case "resident":
					String townName = args.get(1);
					KonTown town = konquest.getKingdomManager().getTownCapital(townName);
					if (town == null) return Collections.emptyList();
					switch (args.get(2).toLowerCase()) {
						case "add":
							for (KonOfflinePlayer offlinePlayer : konquest.getPlayerManager().getAllKonquestOfflinePlayers()) {
								String name = offlinePlayer.getOfflineBukkitPlayer().getName();
								if (name != null && town.getKingdom().equals(offlinePlayer.getKingdom()) && !town.isPlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
									tabList.add(name);
								}
							}
							break;
						case "kick":
							for (KonOfflinePlayer offlinePlayer : konquest.getPlayerManager().getAllKonquestOfflinePlayers()) {
								String name = offlinePlayer.getOfflineBukkitPlayer().getName();
								if (name != null && town.isPlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
									tabList.add(name);
								}
							}
							break;
						case "promote":
							for (OfflinePlayer offlinePlayer : town.getPlayerResidentsOnly()) {
								String name = offlinePlayer.getName();
								if (name != null) {
									tabList.add(name);
								}
							}
							break;
						case "demote":
							for (OfflinePlayer offlinePlayer : town.getPlayerKnightsOnly()) {
								String name = offlinePlayer.getName();
								if (name != null) {
									tabList.add(name);
								}
							}
							break;
						case "lord":
							for (OfflinePlayer offlinePlayer : town.getKingdom().getPlayerMembers()) {
								String name = offlinePlayer.getName();
								if (name != null && !town.isLord(offlinePlayer.getUniqueId())) {
									tabList.add(name);
								}
							}
							break;
					}
					break;
			}
		}
		return matchLastArgToList(tabList,args);
	}
}
