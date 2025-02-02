package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.HelperUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Chunk;
import org.bukkit.Effect;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CaptureAdminCommand extends CommandBase {

	public CaptureAdminCommand() {
		// Define name and sender support
		super("capture",false, true);
		// Define arguments
		// <town> <kingdom>
		addArgument(
				newArg("town",false,false)
						.sub( newArg("kingdom",false,false) )
		);
    }

	@Override
	public void execute(Konquest konquest, CommandSender sender, List<String> args) {
		// Check for correct arguments
		if (args.size() != 2) {
			sendInvalidArgMessage(sender);
		}
		String townName = args.get(0);
		String kingdomName = args.get(1);

		// Check whether the town name is a town or the capital.
		// If it's a town, capture the town for the kingdom.
		// If it's a capital, remove the old kingdom and all other towns, exiles all members.

		// Check for existing kingdom
		if(!konquest.getKingdomManager().isKingdom(kingdomName)) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
			return;
		}
		KonKingdom kingdom = konquest.getKingdomManager().getKingdom(kingdomName);
		assert kingdom != null;
		// Check for existing town in kingdom
		if(!kingdom.isCreated() || kingdom.hasTown(townName) || kingdom.hasCapital(townName)) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return;
		}
		// Find town
		KonTown town = null;
		boolean isCapital = false;
		for(KonKingdom testKingdom : konquest.getKingdomManager().getKingdoms()) {
			if(testKingdom.hasTown(townName)) {
				town = testKingdom.getTown(townName);
			} else if(testKingdom.hasCapital(townName)) {
				town = testKingdom.getCapital();
				isCapital = true;
			}
		}
		// Check for found town or capital name
		if(town == null) {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(townName));
			return;
		}
		// Capture the town/capital
		String townKingdomName = town.getKingdom().getName();
		KonTown capturedTown = null;
		if(isCapital) {
			capturedTown = konquest.getKingdomManager().captureCapital(townKingdomName, kingdom);
		} else {
			capturedTown = konquest.getKingdomManager().captureTown(townName, town.getKingdom().getName(), kingdom);
		}
		// Check for successful capture
		if(capturedTown != null) {
			String newKingdomName = kingdom.getName();
			if(isCapital) {
				ChatUtil.sendBroadcast(MessagePath.PROTECTION_NOTICE_KINGDOM_CONQUER.getMessage(townKingdomName,newKingdomName));
			} else {
				ChatUtil.sendBroadcast(MessagePath.PROTECTION_NOTICE_CONQUER.getMessage(townName));
			}
			ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
			// Broadcast to Dynmap
			int x = capturedTown.getCenterLoc().getBlockX();
			int y = capturedTown.getCenterLoc().getBlockY();
			int z = capturedTown.getCenterLoc().getBlockZ();
			konquest.getMapHandler().postBroadcast(MessagePath.PROTECTION_NOTICE_CONQUER.getMessage(capturedTown.getName())+" ("+x+","+y+","+z+")");
		} else {
			ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_FAILED.getMessage());
		}
	}

	@Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		if (args.size() == 1) {
			// Suggest town names
			for(KonKingdom kingdom : konquest.getKingdomManager().getKingdoms()) {
				tabList.addAll(kingdom.getTownNames());
				tabList.add(kingdom.getName());
			}
		} else if (args.size() == 2) {
			// Suggest enemy kingdom names
			tabList.addAll(konquest.getKingdomManager().getKingdomNames());
		}
		return matchLastArgToList(tabList,args);
	}
}
