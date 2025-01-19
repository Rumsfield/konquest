package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;

public class InfoCommand extends CommandBase {
	
	public InfoCommand() {
		// Define name and sender support
		super("info",true, false);
		// None
		setOptionalArgs(true);
		// [menu]
		addArgument(
				newArg("menu",true,false)
		);
		// player|kingdom|capital|town|sanctuary|ruin|camp|template <name>
		List<String> argNames = Arrays.asList("player", "kingdom", "capital", "town", "sanctuary", "ruin", "camp", "template");
		addArgument(
				newArg(argNames,true,false)
						.sub( newArg("name",false,false) )
		);
    }

	@Override
	public void execute(Konquest konquest, CommandSender sender, List<String> args) {
		// Sender must be player
		KonPlayer player = konquest.getPlayerManager().getPlayer(sender);
		if (player == null) {
			ChatUtil.printDebug("Command executed with null player", true);
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return;
		}
		// Parse arguments
		if (args.isEmpty()) {
			// No arguments
			// Display the base info menu
			konquest.getDisplayManager().displayInfoMenu(player);
		} else if (args.size() == 1) {
			// Single argument
			String infoType = args.get(0);
			if (infoType.equalsIgnoreCase("menu")) {
				// Display base info menu
				konquest.getDisplayManager().displayInfoMenu(player);
			} else {
				sendInvalidArgMessage(sender);
			}
		} else if (args.size() == 2) {
			// Two arguments
			String infoType = args.get(0);
			String infoName = args.get(1);
			// Force type to lowercase for matching
			switch(infoType.toLowerCase()) {
				case "player":
					KonOfflinePlayer otherPlayer = konquest.getPlayerManager().getOfflinePlayerFromName(infoName);
					if(otherPlayer != null) {
						konquest.getDisplayManager().displayInfoPlayerMenu(player, otherPlayer);
						return;
					}
					break;
				case "kingdom":
					if(konquest.getKingdomManager().isKingdom(infoName)) {
						konquest.getDisplayManager().displayInfoKingdomMenu(player, konquest.getKingdomManager().getKingdom(infoName));
						return;
					} else if(infoName.equalsIgnoreCase(konquest.getKingdomManager().getBarbarians().getName())) {
						konquest.getDisplayManager().displayInfoKingdomMenu(player, konquest.getKingdomManager().getBarbarians());
						return;
					}
					break;
				case "capital":
					for(KonKingdom k : konquest.getKingdomManager().getKingdoms()) {
						if(k.hasCapital(infoName)) {
							konquest.getDisplayManager().displayInfoTownMenu(player, k.getCapital());
							return;
						}
					}
					break;
				case "town":
					for(KonKingdom k : konquest.getKingdomManager().getKingdoms()) {
						if(k.hasTown(infoName)) {
							konquest.getDisplayManager().displayInfoTownMenu(player, k.getTown(infoName));
							return;
						}
					}
					break;
				case "ruin":
					if(konquest.getRuinManager().isRuin(infoName)) {
						konquest.getDisplayManager().displayInfoRuinMenu(player, konquest.getRuinManager().getRuin(infoName));
						return;
					}
					break;
				case "sanctuary":
					if(konquest.getSanctuaryManager().isSanctuary(infoName)) {
						konquest.getDisplayManager().displayInfoSanctuaryMenu(player, konquest.getSanctuaryManager().getSanctuary(infoName));
						return;
					}
					break;
				case "camp":
					if(konquest.getCampManager().isCampName(infoName)) {
						konquest.getDisplayManager().displayInfoCampMenu(player, konquest.getCampManager().getCampByName(infoName));
						return;
					}
					break;
				case "template":
					if(konquest.getSanctuaryManager().isTemplate(infoName)) {
						konquest.getDisplayManager().displayInfoTemplateMenu(player, konquest.getSanctuaryManager().getTemplate(infoName));
						return;
					}
					break;
				default:
					sendInvalidArgMessage(sender);
					return;
			}
			// None of the types could find a valid name
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(infoName));
		} else {
			// Wrong number of arguments
			sendInvalidArgMessage(sender);
		}
	}
	
	@Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		// Give suggestions
		if(args.size() == 1) {
			tabList.add("menu");
			tabList.add("player");
			tabList.add("kingdom");
			tabList.add("capital");
			tabList.add("town");
			tabList.add("ruin");
			tabList.add("sanctuary");
			tabList.add("camp");
			tabList.add("template");
		} else if(args.size() == 2) {
			String type = args.get(0).toLowerCase();
			switch(type) {
				case "player":
					for(OfflinePlayer bukkitOfflinePlayer : konquest.getPlayerManager().getAllOfflinePlayers()) {
						tabList.add(bukkitOfflinePlayer.getName());
					}
					break;
				case "kingdom":
					tabList.addAll(konquest.getKingdomManager().getKingdomNames());
					tabList.add(konquest.getKingdomManager().getBarbarians().getName());
					break;
				case "capital":
					tabList.addAll(konquest.getKingdomManager().getKingdomNames());
					break;
				case "town":
					for(KonKingdom kingdom : konquest.getKingdomManager().getKingdoms()) {
						tabList.addAll(kingdom.getTownNames());
					}
					break;
				case "ruin":
					tabList.addAll(konquest.getRuinManager().getRuinNames());
					break;
				case "sanctuary":
					tabList.addAll(konquest.getSanctuaryManager().getSanctuaryNames());
					break;
				case "camp":
					tabList.addAll(konquest.getCampManager().getCampNames());
					break;
				case "template":
					tabList.addAll(konquest.getSanctuaryManager().getAllTemplateNames());
					break;
				default:
					break;
			}
		}
		return matchLastArgToList(tabList,args);
	}
}
