package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TravelAdminCommand  extends CommandBase {
	
	public TravelAdminCommand() {
		// Define name and sender support
		super("travel",true, true);
		// Define arguments
		List<String> argNames = Arrays.asList("town", "kingdom", "camp", "ruin", "sanctuary", "monument");
		// town|kingdom|camp|ruin|sanctuary|monument <name>
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
			sendInvalidSenderMessage(sender);
			return;
		}
		if (args.size() != 2) {
			sendInvalidArgMessage(sender);
			return;
		}
		String destinationName = args.get(1);
		Location destination = null;
		// Parse arguments to get destination location
		switch(args.get(0).toLowerCase()) {
			case "town":
				if (konquest.getKingdomManager().isTown(destinationName)) {
					KonTown town = konquest.getKingdomManager().getTown(destinationName);
					if (town != null) {
						destination = town.getSpawnLoc();
					}
				}
				break;
			case "kingdom":
				if (konquest.getKingdomManager().isKingdom(destinationName)) {
					KonKingdom kingdom = konquest.getKingdomManager().getKingdom(destinationName);
					if (kingdom != null) {
						destination = kingdom.getCapital().getSpawnLoc();
					}
				}
				break;
			case "camp":
				KonOfflinePlayer campPlayer = konquest.getPlayerManager().getOfflinePlayerFromName(destinationName);
				if (campPlayer != null && konquest.getCampManager().isCampSet(campPlayer)) {
					KonCamp camp = konquest.getCampManager().getCamp(campPlayer);
					if (camp != null) {
						destination = camp.getSpawnLoc();
					}
				}
				break;
			case "ruin":
				if (konquest.getRuinManager().isRuin(destinationName)) {
					KonRuin ruin = konquest.getRuinManager().getRuin(destinationName);
					if (ruin != null) {
						destination = ruin.getSpawnLoc();
					}
				}
				break;
			case "sanctuary":
				if (konquest.getSanctuaryManager().isSanctuary(destinationName)) {
					KonSanctuary sanctuary = konquest.getSanctuaryManager().getSanctuary(destinationName);
					if (sanctuary != null) {
						destination = sanctuary.getSpawnLoc();
					}
				}
				break;
			case "monument":
				if (konquest.getSanctuaryManager().isTemplate(destinationName)) {
					KonMonumentTemplate monument = konquest.getSanctuaryManager().getTemplate(destinationName);
					if (monument != null) {
						destination = monument.getSpawnLoc();
					}
				}
				break;
			default:
				sendInvalidArgMessage(sender);
				break;
		}
		// Evaluate destination
		if (destination == null) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(destinationName));
			return;
		}
		// Teleport player to destination
		konquest.telePlayerLocation(player.getBukkitPlayer(), destination);
    }
    
    @Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		if (args.size() == 1) {
			tabList.add("town");
			tabList.add("kingdom");
			tabList.add("camp");
			tabList.add("ruin");
			tabList.add("sanctuary");
			tabList.add("monument");
		} else if (args.size() == 2) {
			switch (args.get(0)) {
				case "town":
					for(KonKingdom kingdom : konquest.getKingdomManager().getKingdoms()) {
						tabList.addAll(kingdom.getTownNames());
					}
					break;
				case "kingdom":
					tabList.addAll(konquest.getKingdomManager().getKingdomNames());
					break;
				case "camp":
					for(KonCamp camp : konquest.getCampManager().getCamps()) {
						tabList.add(camp.getOwner().getName());
					}
					break;
				case "ruin":
					tabList.addAll(konquest.getRuinManager().getRuinNames());
					break;
				case "sanctuary":
					tabList.addAll(konquest.getSanctuaryManager().getSanctuaryNames());
					break;
				case "monument":
					tabList.addAll(konquest.getSanctuaryManager().getAllTemplateNames());
					break;
			}
		}
		return matchLastArgToList(tabList,args);
	}
}
