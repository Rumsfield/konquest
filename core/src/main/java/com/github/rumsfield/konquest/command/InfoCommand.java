package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InfoCommand extends CommandBase {
	
	public InfoCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k info [player|kingdom|capital|town|ruin|sanctuary <name>]
		Player bukkitPlayer = (Player) getSender();
		if (getArgs().length != 1 && getArgs().length != 3) {
			sendInvalidArgMessage(bukkitPlayer,CommandType.INFO);
		} else {
        	// Init info as own player's kingdom
        	if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
			KonPlayer sender = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
			assert sender != null;

			if(getArgs().length == 1) {
				// Display the player's kingdom info
				getKonquest().getDisplayManager().displayKingdomInfoMenu(sender, sender.getKingdom());
				return;
			}

			// Check for info type, must provide type and name
			if(getArgs().length == 3) {
				String infoType = getArgs()[1];
				String infoName = getArgs()[2];

				// Force type to lowercase for matching
				infoType = infoType.toLowerCase();
				switch(infoType) {
					case "player":
						KonOfflinePlayer otherPlayer = getKonquest().getPlayerManager().getOfflinePlayerFromName(infoName);
						if(otherPlayer != null) {
							getKonquest().getDisplayManager().displayPlayerInfoMenu(sender, otherPlayer);
							return;
						}
						break;
					case "kingdom":
						if(getKonquest().getKingdomManager().isKingdom(infoName)) {
							KonKingdom kingdom = getKonquest().getKingdomManager().getKingdom(infoName);
							getKonquest().getDisplayManager().displayKingdomInfoMenu(sender, kingdom);
							return;
						} else if(infoName.equalsIgnoreCase(MessagePath.LABEL_BARBARIANS.getMessage())) {
							KonKingdom kingdom = getKonquest().getKingdomManager().getBarbarians();
							getKonquest().getDisplayManager().displayKingdomInfoMenu(sender, kingdom);
							return;
						}
						break;
					case "capital":
						for(KonKingdom k : getKonquest().getKingdomManager().getKingdoms()) {
							if(k.hasCapital(infoName)) {
								getKonquest().getDisplayManager().displayTownInfoMenu(sender, k.getCapital());
								return;
							}
						}
						break;
					case "town":
						for(KonKingdom k : getKonquest().getKingdomManager().getKingdoms()) {
							if(k.hasTown(infoName)) {
								getKonquest().getDisplayManager().displayTownInfoMenu(sender, k.getTown(infoName));
								return;
							}
						}
						break;
					case "ruin":
						if(getKonquest().getRuinManager().isRuin(infoName)) {
							KonRuin ruin = getKonquest().getRuinManager().getRuin(infoName);
							getKonquest().getDisplayManager().displayRuinInfoMenu(sender, ruin);
							return;
						}
						break;
					case "sanctuary":
						if(getKonquest().getSanctuaryManager().isSanctuary(infoName)) {
							KonSanctuary sanctuary = getKonquest().getSanctuaryManager().getSanctuary(infoName);
							getKonquest().getDisplayManager().displaySanctuaryInfoMenu(sender, sanctuary);
							return;
						}
						break;
					default:
						sendInvalidArgMessage(bukkitPlayer,CommandType.INFO);
						return;
				}
				// None of the types could find a valid name
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(infoName));
				return;
			}
			// Something messed up?
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
        }
	}
	
	@Override
	public List<String> tabComplete() {
		// k info [player|kingdom|capital|town|ruin|sanctuary <name>]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		if(getArgs().length == 2) {
			tabList.add("player");
			tabList.add("kingdom");
			tabList.add("capital");
			tabList.add("town");
			tabList.add("ruin");
			tabList.add("sanctuary");

			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 3) {
			String type = getArgs()[1].toLowerCase();
			switch(type) {
				case "player":
					for(OfflinePlayer bukkitOfflinePlayer : getKonquest().getPlayerManager().getAllOfflinePlayers()) {
						tabList.add(bukkitOfflinePlayer.getName());
					}
					break;
				case "kingdom":
					tabList.addAll(getKonquest().getKingdomManager().getKingdomNames());
					tabList.add(MessagePath.LABEL_BARBARIANS.getMessage());
					break;
				case "capital":
					tabList.addAll(getKonquest().getKingdomManager().getKingdomNames());
					break;
				case "town":
					for(KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
						tabList.addAll(kingdom.getTownNames());
					}
					break;
				case "ruin":
					tabList.addAll(getKonquest().getRuinManager().getRuinNames());
					break;
				case "sanctuary":
					tabList.addAll(getKonquest().getSanctuaryManager().getSanctuaryNames());
					break;
				default:
					break;
			}

			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}

		return matchedTabList;
	}
}
