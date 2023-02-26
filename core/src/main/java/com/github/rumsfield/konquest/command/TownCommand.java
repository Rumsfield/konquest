package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.model.KonOfflinePlayer;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonStatsType;
import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import com.github.rumsfield.konquest.utility.Timer;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TownCommand extends CommandBase {

	public TownCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

	@Override
	public void execute() {
		// Open the general town menu (join, leave, list, invites, requests):
		// /k town

		// Open a specific town management menu
		// /k town <town>

		// k town [<town>] [menu|add|kick|lord|rename] [<name>]
		if (getArgs().length < 1 || getArgs().length > 4) {
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
			return;
		}
		Player bukkitPlayer = (Player) getSender();
		if (!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
			ChatUtil.printDebug("Failed to find non-existent player");
			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return;
		}
		KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
		// Verify player is not a barbarian
		assert player != null;
		if (player.isBarbarian()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return;
		}
		// Verify town or capital exists within sender's Kingdom
		// Name can be the name of a town, or the name of the kingdom for the capital
		KonTown town = null;
		if(getArgs().length >= 2) {
			String townName = getArgs()[1];
			boolean isTown = player.getKingdom().hasTown(townName);
			boolean isCapital = player.getKingdom().hasCapital(townName);
			if (isTown) {
				town = player.getKingdom().getTown(townName);
			} else if (isCapital) {
				town = player.getKingdom().getCapital();
			} else {
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_BAD_NAME.getMessage());
				return;
			}
		}

		// Variable arg length conditions
		if(getArgs().length == 1) {
			// No town argument, open general town menu
			getKonquest().getDisplayManager().displayTownMenu(player);
		} else if(getArgs().length == 2) {
			// Given town name only, open town management menu
			if(town == null) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
				return;
			}
			if(!town.isPlayerLord(player.getOfflineBukkitPlayer()) && !town.isPlayerKnight(player.getOfflineBukkitPlayer())) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
				return;
			}
			getKonquest().getDisplayManager().displayTownManagementMenu(player, town, false);
		} else if(getArgs().length >= 3) {
			// Has sub-commands
			if(town == null) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
				return;
			}
			String subCmd = getArgs()[2];
			// Check if town has a lord
			boolean notifyLordTakeover = false;
			if(!town.isLordValid() && !subCmd.equalsIgnoreCase("lord")) {
				if(town.isPlayerKnight(player.getOfflineBukkitPlayer())) {
					notifyLordTakeover = true;
				} else if(town.isPlayerResident(player.getOfflineBukkitPlayer()) && town.getPlayerKnights().isEmpty()) {
					notifyLordTakeover = true;
				} else if(town.getPlayerResidents().isEmpty()){
					notifyLordTakeover = true;
				}
			}
			if(notifyLordTakeover) {
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_NO_LORD.getMessage(town.getName(),town.getName(),bukkitPlayer.getName()));
			}

			// Action based on sub-command
			switch(subCmd.toLowerCase()) {
			case "menu":
				// Verify player is elite or lord of the Town
				if(!town.isPlayerLord(player.getOfflineBukkitPlayer()) && !town.isPlayerKnight(player.getOfflineBukkitPlayer())) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
					return;
				}
				getKonquest().getDisplayManager().displayTownManagementMenu(player, town, false);
				break;

			case "add":
				if(getArgs().length == 4) {
					// Get invite player from name
					String playerName = getArgs()[3];
					KonOfflinePlayer invitee = getKonquest().getPlayerManager().getOfflinePlayerFromName(playerName);
					if(invitee == null) {
						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
						return;
					}
					// Call manager method
					boolean status = getKonquest().getKingdomManager().addTownPlayer(player, invitee, town);
					//TODO: redo these messages
					if(status) {
						ChatUtil.sendNotice(bukkitPlayer,"Invite Success");
					} else {
						ChatUtil.sendError(bukkitPlayer,"Invite Failed");
					}
				} else {
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
					return;
				}
				break;

			case "kick":
				if(getArgs().length == 4) {
					// Get kick player from name
					String playerName = getArgs()[3];
					KonOfflinePlayer kickPlayer = getKonquest().getPlayerManager().getOfflinePlayerFromName(playerName);
					if(kickPlayer == null) {
						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(playerName));
						return;
					}
					// Call manager method
					boolean status = getKonquest().getKingdomManager().kickTownPlayer(player, kickPlayer, town);
					//TODO: redo these messages
					if(status) {
						ChatUtil.sendNotice(bukkitPlayer,"Kick Success");
					} else {
						ChatUtil.sendError(bukkitPlayer,"Kick Failed");
					}
				} else {
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
					return;
				}
				break;

			case "lord":
				if(getArgs().length == 3) {
					// Call manager method
					boolean status = getKonquest().getKingdomManager().lordTownTakeover(player, town);
					//TODO: redo these messages
					if(status) {
						ChatUtil.sendNotice(bukkitPlayer,"Lord Takeover Success");
					} else {
						ChatUtil.sendError(bukkitPlayer,"Lord Takeover Failed");
					}
				} else {
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
					return;
				}
				break;

			case "rename":
				if (getArgs().length == 4) {
					String newTownName = getArgs()[3];
					// Verify player is lord of the Town
					if(!town.isPlayerLord(player.getOfflineBukkitPlayer())) {
						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
						return;
					}
					// Verify enough favor
					double cost = getKonquest().getCore().getDouble(CorePath.FAVOR_TOWNS_COST_RENAME.getPath(),0.0);
					if(cost > 0 && KonquestPlugin.getBalance(bukkitPlayer) < cost) {
						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(cost));
						return;
					}
					// Check new name constraints
					if(getKonquest().validateName(newTownName,bukkitPlayer) != 0) {
						return;
					}
					// Rename the town
					boolean success = getKonquest().getKingdomManager().renameTown(town.getName(), newTownName, town.getKingdom().getName());
					if(success) {
						for(OfflinePlayer resident : town.getPlayerResidents()) {
							if(resident.isOnline()) {
								ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_RENAME.getMessage(bukkitPlayer.getName(),town.getName(),newTownName));
							}
						}
						// Withdraw cost from player
						if(cost > 0 && KonquestPlugin.withdrawPlayer(bukkitPlayer, cost)) {
							getKonquest().getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)cost);
						}
					} else {
						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
					}
				} else {
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
					return;
				}
				break;

			default:
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
			}
		}

	}

	@Override
	public List<String> tabComplete() {
		// k town [<town>] [menu|add|kick|lord|rename] [<name>]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		Player bukkitPlayer = (Player) getSender();
		KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
		if(player == null) {
			return matchedTabList;
		}
		if(getArgs().length == 2) {
			// Suggest all town names in player's kingdom
			tabList.addAll(player.getKingdom().getTownNames());
			// Suggest capital by kingdom name
			tabList.add(player.getKingdom().getName());
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 3) {
			// suggest sub-commands
			tabList.add("add");
			tabList.add("kick");
			tabList.add("lord");
			tabList.add("rename");
			tabList.add("menu");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			// suggest appropriate arguments
			String subCommand = getArgs()[2];
			String name;
			if(subCommand.equalsIgnoreCase("add") || subCommand.equalsIgnoreCase("kick")) {
				List<String> playerList = new ArrayList<>();
				for(KonOfflinePlayer offlinePlayer : getKonquest().getPlayerManager().getAllPlayersInKingdom(player.getKingdom())) {
					name = offlinePlayer.getOfflineBukkitPlayer().getName();
					if(name != null) {
						playerList.add(name);
					}
				}
				tabList.addAll(playerList);
			} else if(subCommand.equalsIgnoreCase("rename")) {
				tabList.add("***");
			}
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}

}
