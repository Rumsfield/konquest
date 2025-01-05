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
		// player|kingdom|capital|town|sanctuary|ruin <name>
		List<String> argNames = Arrays.asList("player", "kingdom", "capital", "town", "sanctuary", "ruin");
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
		} else if(args.size() == 2) {
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
						KonKingdom kingdom = konquest.getKingdomManager().getKingdom(infoName);
						konquest.getDisplayManager().displayInfoKingdomMenu(player, kingdom);
						return;
					} else if(infoName.equalsIgnoreCase(MessagePath.LABEL_BARBARIANS.getMessage())) {
						KonKingdom kingdom = konquest.getKingdomManager().getBarbarians();
						konquest.getDisplayManager().displayInfoKingdomMenu(player, kingdom);
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
						KonRuin ruin = konquest.getRuinManager().getRuin(infoName);
						konquest.getDisplayManager().displayInfoRuinMenu(player, ruin);
						return;
					}
					break;
				case "sanctuary":
					if(konquest.getSanctuaryManager().isSanctuary(infoName)) {
						KonSanctuary sanctuary = konquest.getSanctuaryManager().getSanctuary(infoName);
						konquest.getDisplayManager().displayInfoSanctuaryMenu(player, sanctuary);
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
			tabList.add("player");
			tabList.add("kingdom");
			tabList.add("capital");
			tabList.add("town");
			tabList.add("ruin");
			tabList.add("sanctuary");
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
					tabList.add(MessagePath.LABEL_BARBARIANS.getMessage());
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
				default:
					break;
			}
		}
		return matchLastArgToList(tabList,args);
	}
}
