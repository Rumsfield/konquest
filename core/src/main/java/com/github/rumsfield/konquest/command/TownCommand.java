package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.api.model.KonquestDiplomacyType;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.api.model.KonquestUpgrade;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import com.github.rumsfield.konquest.utility.Timer;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.util.StringUtil;

import java.util.*;

public class TownCommand extends CommandBase {

	public TownCommand() {
		// Define name and sender support
		super("town",true, false);
		// Define arguments
		// Note: <town> non-literal arguments accept names of towns, and name of kingdom for capital, and literal "capital".
		// None
		setOptionalArgs(true);
		// menu
		addArgument(
				newArg("menu",true,false)
		);
		// invites
		addArgument(
				newArg("invites",true,false)
		);
		// requests
		addArgument(
				newArg("requests",true,false)
		);
		// join <town>
		addArgument(
				newArg("join",true,false)
						.sub( newArg("town",false,false) )
		);
		// leave <town>
		addArgument(
				newArg("leave",true,false)
						.sub( newArg("town",false,false) )
		);
		// lord <town>
		addArgument(
				newArg("lord",true,false)
						.sub( newArg("town",false,false) )
		);
		// manage <town> ...
		List<String> requestsArgNames = Arrays.asList("accept", "deny");
		List<String> optionsArgNames = Arrays.asList("true", "false");
		List<String> residentArgNames = Arrays.asList("invite", "kick", "promote", "demote", "lord");
		addArgument(
				newArg("manage",true,false)
						.sub( newArg("town",false,true)
								// manage <town> [menu]
								.sub( newArg("menu",true,false) )
								// manage <town> plots
								.sub( newArg("plots",true,false) )
								// manage <town> rename <name>
								.sub( newArg("rename",true,false)
										.sub( newArg("name",false,false) ) )
								// manage <town> upgrade [<name>]
								.sub( newArg("upgrade",true,true)
										.sub( newArg("name",false,false) ) )
								// manage <town> shield [<name>]
								.sub( newArg("shield",true,true)
										.sub( newArg("name",false,false) ) )
								// manage <town> armor [<name>]
								.sub( newArg("armor",true,true)
										.sub( newArg("name",false,false) ) )
								// manage <town> specialize [<name>]
								.sub( newArg("specialize",true,true)
										.sub( newArg("name",false,false) ) )
								// manage <town> option <name> [true|false]
								.sub( newArg("option",true,true)
										.sub( newArg("name",false,true)
												.sub( newArg(optionsArgNames,true,false) ) ) )
								// manage <town> requests [<player>] accept|deny
								.sub( newArg("requests",true,true)
										.sub( newArg("player",false,false)
												.sub( newArg(requestsArgNames,true,false) ) ) )
								// manage <town> resident invite|kick|promote|demote|lord <player>
								.sub( newArg("resident",true,false)
										.sub( newArg(residentArgNames,true,false)
												.sub( newArg("player",false,false) ) ) ) )
		);
    }

	private KonTown getVerifyTown(KonPlayer player, String townName) {
		// Verify town or capital exists within sender's Kingdom
		// Name can be the name of a town, or the name of the kingdom for the capital
		// Or the literal string "capital".
		KonTown town = null;
		if (!townName.isEmpty()) {
			if (townName.equalsIgnoreCase("capital")) {
				town = player.getKingdom().getCapital();
			} else {
				town = player.getKingdom().getTownCapital(townName);
			}
		}
		if (town == null) {
			ChatUtil.sendError(player, MessagePath.GENERIC_ERROR_BAD_NAME.getMessage(townName));
		}
		return town;
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
		final int MAX_LIST_NAMES = 10;
		// Sender must be player
		KonPlayer player = konquest.getPlayerManager().getPlayer(sender);
		if (player == null) {
			ChatUtil.printDebug("Command executed with null player", true);
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return;
		}
		Player bukkitPlayer = player.getBukkitPlayer();
		UUID playerID = bukkitPlayer.getUniqueId();
		// Verify player is not a barbarian
		if (player.isBarbarian()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
			return;
		}

		// Parse arguments
		if(args.isEmpty()) {
			// No arguments, open general town menu
			konquest.getDisplayManager().displayTownMenu(player);
			return;
		}
		// Has arguments
		String subCmd = args.get(0);

		switch(subCmd.toLowerCase()) {
			case "menu":
				// Open the general menu
				konquest.getDisplayManager().displayTownMenu(player);
				break;
			case "invites":
				// View list of towns that have invited the player to join
				if (args.size() == 1) {
					String nameListStr = formatStringListLimited(konquest.getKingdomManager().getInviteTownNames(player), MAX_LIST_NAMES);
					ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_INVITE_LIST.getMessage());
					ChatUtil.sendMessage(bukkitPlayer, nameListStr);
				} else {
					sendInvalidArgMessage(bukkitPlayer);
				}
				break;
			case "requests":
				// View list of towns that have pending requests to join it
				if (args.size() == 1) {
					String nameListStr = formatStringListLimited(konquest.getKingdomManager().getRequestTownNames(player), MAX_LIST_NAMES);
					ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_REQUEST_TOWN_LIST.getMessage());
					ChatUtil.sendMessage(bukkitPlayer, nameListStr);
				} else {
					sendInvalidArgMessage(bukkitPlayer);
				}
				break;
			case "join":
				if (args.size() == 2) {
					// Call manager method, includes messages
					KonTown town = getVerifyTown(player, args.get(1));
					if (town == null) return;
					konquest.getKingdomManager().menuJoinTownRequest(player, town);
				} else {
					sendInvalidArgMessage(bukkitPlayer);
				}
				break;
			case "leave":
				if (args.size() == 2) {
					// Call manager method, includes messages
					KonTown town = getVerifyTown(player, args.get(1));
					if (town == null) return;
					konquest.getKingdomManager().menuLeaveTown(player, town);
				} else {
					sendInvalidArgMessage(bukkitPlayer);
				}
				break;
			case "lord":
				if (args.size() == 2) {
					// Call manager method, includes messages
					KonTown town = getVerifyTown(player, args.get(1));
					if (town == null) return;
					konquest.getKingdomManager().lordTownTakeover(player, town);
				} else {
					sendInvalidArgMessage(bukkitPlayer);
				}
				break;
			case "manage":
				// Management sub-commands
				if (args.size() >= 2) {
					KonTown town = getVerifyTown(player, args.get(1));
					if (town == null) return;
					// Check if town has a lord
					boolean notifyLordTakeover = false;
					if(!town.isLordValid()) {
						if(town.isPlayerKnight(player.getOfflineBukkitPlayer())) {
							notifyLordTakeover = true;
						} else if(town.isPlayerResident(player.getOfflineBukkitPlayer()) && town.getPlayerKnights().isEmpty()) {
							notifyLordTakeover = true;
						} else if(town.getPlayerResidents().isEmpty()){
							notifyLordTakeover = true;
						}
					}
					if(notifyLordTakeover) {
						ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_NO_LORD.getMessage(town.getName(),town.getTravelName()));
					}
					// Check that player is knight or lord
					if(!town.isPlayerLord(player.getOfflineBukkitPlayer()) && !town.isPlayerKnight(player.getOfflineBukkitPlayer())) {
						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
						return;
					}
					// Sub-Commands
					if (args.size() == 2) {
						// No sub-commands, open menu
						konquest.getDisplayManager().displayTownManagementMenu(player, town, false);
					} else {
						// Has sub-commands
						String manageSubCmd = args.get(2);
						switch(manageSubCmd.toLowerCase()) {
							case "plots":
								// Verify player is lord of the Town
								if(!town.isLord(playerID)) {
									ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
									return;
								}
								if (args.size() == 3) {
									// Open plots menu
									konquest.getDisplayManager().displayTownPlotMenu(bukkitPlayer, town);
								} else {
									sendInvalidArgMessage(bukkitPlayer);
								}
								break;
							case "rename":
								// Verify player is lord of the Town
								if(!town.isLord(playerID)) {
									ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
									return;
								}
								if (args.size() == 4) {
									String newTownName = args.get(3);
									// Verify enough favor
									double cost = konquest.getCore().getDouble(CorePath.FAVOR_TOWNS_COST_RENAME.getPath(),0.0);
									if(cost > 0 && KonquestPlugin.getBalance(bukkitPlayer) < cost) {
										ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(cost));
										return;
									}
									String oldTownName = town.getName();
									// Check for town name
									if(!konquest.getKingdomManager().isTown(oldTownName)) {
										ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_BAD_NAME.getMessage(oldTownName));
										return;
									}
									// Check new name constraints
									if(konquest.validateName(newTownName,bukkitPlayer) != 0) {
										return;
									}
									// Rename the town
									boolean success = konquest.getKingdomManager().renameTown(oldTownName, newTownName, town.getKingdom().getName());
									if(success) {
										for(OfflinePlayer resident : town.getPlayerResidents()) {
											if(resident.isOnline()) {
												ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_RENAME.getMessage(bukkitPlayer.getName(),oldTownName,newTownName));
											}
										}
										// Withdraw cost from player
										if(cost > 0 && KonquestPlugin.withdrawPlayer(bukkitPlayer, cost)) {
											konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)cost);
										}
									} else {
										ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
									}
								} else {
									sendInvalidArgMessage(bukkitPlayer);
								}
								break;
							case "upgrade":
								// Check for enabled feature
								if (!konquest.getUpgradeManager().isEnabled()) {
									ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
									return;
								}
								// Verify player is lord of the Town
								if(!town.isLord(playerID)) {
									ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
									return;
								}
								HashMap<KonquestUpgrade, Integer> availableUpgrades = konquest.getUpgradeManager().getAvailableUpgrades(town);
								if (args.size() == 3) {
									// No upgrade given, display available upgrades
									// Place upgrades in ordinal order into list
									List<KonUpgrade> allUpgrades = new ArrayList<>();
									for (KonUpgrade upgrade : KonUpgrade.values()) {
										if (availableUpgrades.containsKey(upgrade)) {
											allUpgrades.add(upgrade);
										}
									}
									ChatUtil.sendNotice(bukkitPlayer, MessagePath.MENU_UPGRADE_TITLE.getMessage());
									for (KonUpgrade upgrade : allUpgrades) {
										int upgradeLevel = availableUpgrades.get(upgrade);
										String upgradeInfo = ChatColor.GOLD +
												upgrade.getDescription() +
												" " +
												upgradeLevel +
												ChatColor.RESET +
												" - " +
												ChatColor.LIGHT_PURPLE +
												upgrade.getLevelDescription(upgradeLevel);
										ChatUtil.sendMessage(bukkitPlayer, upgradeInfo);
									}
								} else if (args.size() == 4) {
									// Try to apply given upgrade
									String upgradeName = args.get(3);
									KonUpgrade upgrade = KonUpgrade.getUpgrade(upgradeName);
									if (upgrade == null) {
										ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(upgradeName));
										return;
									}
									if (!availableUpgrades.containsKey(upgrade)) {
										ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_UPGRADE_UNAVAILABLE.getMessage(upgradeName, town.getName()));
										return;
									}
									int upgradeLevel = availableUpgrades.get(upgrade);
									konquest.getUpgradeManager().addTownUpgrade(town, upgrade, upgradeLevel, player.getBukkitPlayer());
								} else {
									sendInvalidArgMessage(bukkitPlayer);
								}
								break;
							case "shield":
								// Check for enabled feature
								if (!konquest.getShieldManager().isShieldsEnabled()) {
									ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
									return;
								}
								if (args.size() == 3) {
									// Display current shield time and available shields
									String shieldTime = Konquest.getTimeFormat(town.getRemainingShieldTimeSeconds(),"");
									ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_SHIELD_LIST.getMessage(shieldTime, town.getName()));
									for (KonShield shield : konquest.getShieldManager().getShields()) {
										String shieldName = shield.getId();
										String shieldDuration = Konquest.getTimeFormat(shield.getDurationSeconds(),"");
										int shieldCost = konquest.getShieldManager().getTotalCostShield(shield,town);
										String shieldInfo = ChatColor.GOLD +
												shieldName +
												" - " +
												ChatColor.RESET +
												shieldDuration +
												", " +
												ChatColor.DARK_AQUA +
												MessagePath.LABEL_COST.getMessage() +
												" " +
												shieldCost;
										ChatUtil.sendMessage(bukkitPlayer, shieldInfo);
									}
								} else if (args.size() == 4) {
									// Try to apply shields
									String shieldName = args.get(3);
									KonShield shield = konquest.getShieldManager().getShield(shieldName);
									if (shield == null) {
										ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(shieldName));
										return;
									}
									// Manager method includes checks, messages
									konquest.getShieldManager().activateTownShield(shield, town, player.getBukkitPlayer(), false);
								} else {
									sendInvalidArgMessage(bukkitPlayer);
								}
								break;
							case "armor":
								// Check for enabled feature
								if (!konquest.getShieldManager().isArmorsEnabled()) {
									ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
									return;
								}
								if (args.size() == 3) {
									// Display current armor count and available armors
									String armorBlocks = ""+town.getArmorBlocks();
									ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_ARMOR_LIST.getMessage(armorBlocks, town.getName()));
									for (KonArmor armor : konquest.getShieldManager().getArmors()) {
										String armorName = armor.getId();
										String armorAmount = ""+armor.getBlocks();
										int armorCost = konquest.getShieldManager().getTotalCostArmor(armor,town);
										String armorInfo = ChatColor.GOLD +
												armorName +
												" - " +
												ChatColor.RESET +
												armorAmount +
												", " +
												ChatColor.DARK_AQUA +
												MessagePath.LABEL_COST.getMessage() +
												" " +
												armorCost;
										ChatUtil.sendMessage(bukkitPlayer, armorInfo);
									}
								} else if (args.size() == 4) {
									// Try to apply armor
									String armorName = args.get(3);
									KonArmor armor = konquest.getShieldManager().getArmor(armorName);
									if (armor == null) {
										ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(armorName));
										return;
									}
									// Manager method includes checks, messages
									konquest.getShieldManager().activateTownArmor(armor, town, player.getBukkitPlayer(), false);
								} else {
									sendInvalidArgMessage(bukkitPlayer);
								}
								break;
							case "specialize":
								// Verify player is lord of the Town
								if(!town.isLord(playerID)) {
									ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
									return;
								}
								// Check for enabled feature
								if (!konquest.getKingdomManager().getIsDiscountEnable()) {
									ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
									return;
								}
								if (args.size() == 3) {
									// Display current profession
									String specialName = town.getSpecialization().toString();
									ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_SPECIALIZE_LIST.getMessage(town.getName(), specialName));
								} else if (args.size() == 4) {
									// Try to change profession
									String professionName = args.get(3);
									Villager.Profession profession;
									try {
										profession = Villager.Profession.valueOf(professionName.toUpperCase());
									} catch (IllegalArgumentException ex) {
										ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(professionName));
										return;
									}
									konquest.getKingdomManager().menuChangeTownSpecialization(town, profession, player, false);
								} else {
									sendInvalidArgMessage(bukkitPlayer);
								}
								break;
							case "option":
								// Verify player is lord of the Town
								if(!town.isLord(playerID)) {
									ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
									return;
								}
								if (args.size() == 3) {
									// Display all options
									for (KonTownOption option : KonTownOption.values()) {
										if (konquest.getKingdomManager().isTownOptionFeatureEnabled(option)) {
											String optionInfo = formatOptionLine(option, town);
											ChatUtil.sendMessage(sender, optionInfo);
										}
									}
								} else if (args.size() >= 4) {
									String optionName = args.get(3);
									KonTownOption option = KonTownOption.getOption(optionName);
									if (option == null) {
										ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(optionName));
										return;
									}
									if (!konquest.getKingdomManager().isTownOptionFeatureEnabled(option)) {
										ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
										return;
									}
									if (args.size() == 4) {
										// Display option value
										String optionInfo = formatOptionLine(option, town);
										ChatUtil.sendMessage(bukkitPlayer, optionInfo);
									} else if (args.size() == 5) {
										// Set new value
										String newValStr = args.get(4);
										if (newValStr.equalsIgnoreCase("true")) {
											konquest.getKingdomManager().setTownOption(option, town, bukkitPlayer, true);
										} else if (newValStr.equalsIgnoreCase("false")) {
											konquest.getKingdomManager().setTownOption(option, town, bukkitPlayer, false);
										} else {
											// Unknown value
											ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(newValStr));
											return;
										}
									} else {
										sendInvalidArgMessage(bukkitPlayer);
									}
								} else {
									sendInvalidArgMessage(bukkitPlayer);
								}
								break;
							case "requests":
								if (args.size() == 3) {
									// No player name given, list requests
									ArrayList<String> requestPlayerNames = new ArrayList<>();
									for (OfflinePlayer requester : town.getJoinRequests()) {
										requestPlayerNames.add(requester.getName());
									}
									String nameListStr = formatStringListLimited(requestPlayerNames, MAX_LIST_NAMES);
									ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_REQUEST_PLAYER_LIST.getMessage(town.getName()));
									ChatUtil.sendMessage(bukkitPlayer, nameListStr);
								} else if (args.size() == 5) {
									// Accept or deny player
									String requesterName = args.get(3);
									String requestDecision = args.get(4);
									KonOfflinePlayer requester = konquest.getPlayerManager().getOfflinePlayerFromName(requesterName);
									if(requester == null) {
										ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(requesterName));
										return;
									}
									boolean response;
									if (requestDecision.equalsIgnoreCase("accept")) {
										response = true;
									} else if (requestDecision.equalsIgnoreCase("deny")) {
										response = false;
									} else {
										sendInvalidArgMessage(bukkitPlayer);
										return;
									}
									// Manager method includes messages
									konquest.getKingdomManager().menuRespondTownRequest(player,requester.getOfflineBukkitPlayer(),town,response);
								} else {
									sendInvalidArgMessage(bukkitPlayer);
								}
								break;
							case "resident":
								if (args.size() == 5) {
									// /k town manage (town) resident invite|kick|promote|demote|lord (player)
									String residentSubCmd = args.get(3);
									String residentPlayerName = args.get(4);
									KonOfflinePlayer residentPlayer = konquest.getPlayerManager().getOfflinePlayerFromName(residentPlayerName);
									if(residentPlayer == null) {
										ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(residentPlayerName));
										return;
									}
									// Parse sub-commands
									switch(residentSubCmd.toLowerCase()) {
										case "invite":
											// Invite other player to join town
											// Call manager method, includes checks & messages
											konquest.getKingdomManager().addTownPlayer(player, residentPlayer, town);
											break;
										case "kick":
											// Kick resident player from town
											// Call manager method, includes checks & messages
											konquest.getKingdomManager().kickTownPlayer(player, residentPlayer, town);
											break;
										case "promote":
											// Promote the resident to knight
											// Call manager method, includes checks & messages
											konquest.getKingdomManager().menuPromoteDemoteTownKnight(player,residentPlayer.getOfflineBukkitPlayer(),town,true);
											break;
										case "demote":
											// Demote the knight to resident
											// Call manager method, includes checks & messages
											konquest.getKingdomManager().menuPromoteDemoteTownKnight(player,residentPlayer.getOfflineBukkitPlayer(),town,false);
											break;
										case "lord":
											// Make the player into the new town lord
											// Call manager method, includes checks & messages
											konquest.getKingdomManager().menuTransferTownLord(player,residentPlayer.getOfflineBukkitPlayer(),town,false);
											break;
										default:
											sendInvalidArgMessage(bukkitPlayer);
											break;
									}
								} else {
									sendInvalidArgMessage(bukkitPlayer);
								}
								break;
							default:
								sendInvalidArgMessage(bukkitPlayer);
								break;
						}
					}
				} else {
					sendInvalidArgMessage(bukkitPlayer);
				}
				break;
			default:
				sendInvalidArgMessage(bukkitPlayer);
				break;
		}
	}

	@Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		// Sender must be player to get kingdom
		KonPlayer player = konquest.getPlayerManager().getPlayer(sender);
		if (player == null) return Collections.emptyList();
		// Check for player's kingdom
		KonKingdom kingdom = player.getKingdom();
		if (kingdom == null) return Collections.emptyList();
		List<String> tabList = new ArrayList<>();
		int numArgs = args.size();
		if (numArgs == 1) {
			// suggest sub-commands
			tabList.add("menu");
			tabList.add("invites");
			tabList.add("requests");
			tabList.add("join");
			tabList.add("leave");
			tabList.add("lord");
			tabList.add("manage");
		} else if (numArgs == 2) {
			switch (args.get(0).toLowerCase()) {
				case "join":
					// Suggest town names that the player can join
					for (KonTown town : kingdom.getCapitalTowns()) {
						if (!town.isPlayerResident(player.getBukkitPlayer())) {
							if (town.getTerritoryType().equals(KonquestTerritoryType.CAPITAL)) {
								tabList.add(town.getKingdom().getName());
								tabList.add("capital");
							} else {
								tabList.add(town.getName());
							}
						}
					}
					break;
				case "leave":
					// Suggest town names that the player can leave
					for (KonTown town : kingdom.getCapitalTowns()) {
						if (town.isPlayerResident(player.getBukkitPlayer())) {
							if (town.getTerritoryType().equals(KonquestTerritoryType.CAPITAL)) {
								tabList.add(town.getKingdom().getName());
								tabList.add("capital");
							} else {
								tabList.add(town.getName());
							}
						}
					}
					break;
				case "lord":
					// Suggest all town names with no lord
					for (KonTown town : kingdom.getCapitalTowns()) {
						if (!town.isLordValid()) {
							if (town.getTerritoryType().equals(KonquestTerritoryType.CAPITAL)) {
								tabList.add(town.getKingdom().getName());
								tabList.add("capital");
							} else {
								tabList.add(town.getName());
							}
						}
					}
					break;
				case "manage":
					// Suggest managed towns
					for (KonTown town : kingdom.getCapitalTowns()) {
						if (town.isPlayerKnight(player.getBukkitPlayer())) {
							if (town.getTerritoryType().equals(KonquestTerritoryType.CAPITAL)) {
								tabList.add(town.getKingdom().getName());
								tabList.add("capital");
							} else {
								tabList.add(town.getName());
							}
						}
					}
					break;
			}
		} else if (numArgs == 3) {
			if (args.get(0).equalsIgnoreCase("manage")) {
				// suggest sub-commands
				tabList.add("rename");
				tabList.add("option");
				tabList.add("requests");
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
			}
		} else if (numArgs == 4) {
			switch (args.get(2).toLowerCase()) {
				case "rename":
					tabList.add("***");
					break;
				case "upgrade":
					if (konquest.getUpgradeManager().isEnabled()) {
						for (KonUpgrade upgrade : KonUpgrade.values()) {
							tabList.add(upgrade.toString());
						}
					}
					break;
				case "shield":
					if (konquest.getShieldManager().isShieldsEnabled()) {
						for (KonShield shield : konquest.getShieldManager().getShields()) {
							tabList.add(shield.getId());
						}
					}
					break;
				case "armor":
					if (konquest.getShieldManager().isArmorsEnabled()) {
						for (KonArmor armor : konquest.getShieldManager().getArmors()) {
							tabList.add(armor.getId());
						}
					}
					break;
				case "specialize":
					if (konquest.getKingdomManager().getIsDiscountEnable()) {
						for (Villager.Profession profession : Villager.Profession.values()) {
							tabList.add(profession.toString());
						}
					}
					break;
				case "option":
					for (KonTownOption option : KonTownOption.values()) {
						tabList.add(option.toString());
					}
					break;
				case "requests":
					String townName = args.get(1);
					KonTown town = kingdom.getTownCapital(townName);
					if (town != null) {
						for (OfflinePlayer requester : town.getJoinRequests()) {
							tabList.add(requester.getName());
						}
					}
					break;
				case "resident":
					tabList.add("invite");
					tabList.add("kick");
					tabList.add("promote");
					tabList.add("demote");
					tabList.add("lord");
					break;
			}
		} else if (numArgs == 5) {
			switch (args.get(2).toLowerCase()) {
				case "option":
					tabList.add("true");
					tabList.add("false");
					break;
				case "requests":
					tabList.add("accept");
					tabList.add("deny");
					break;
				case "resident":
					String townName = args.get(1);
					KonTown town = kingdom.getTownCapital(townName);
					if (town == null) return Collections.emptyList();
					switch (args.get(3).toLowerCase()) {
						case "invite":
							for (KonOfflinePlayer offlinePlayer : konquest.getPlayerManager().getAllKonquestOfflinePlayers()) {
								String name = offlinePlayer.getOfflineBukkitPlayer().getName();
								if (name != null && kingdom.equals(offlinePlayer.getKingdom()) && !town.isPlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
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
							for (OfflinePlayer offlinePlayer : town.getPlayerResidents()) {
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
