package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
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

	public CaptureAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

	@Override
	public void execute() {
		// k admin capture <town> <kingdom>
		Player bukkitPlayer = (Player) getSender();
		if (getArgs().length != 4) {
			sendInvalidArgMessage(bukkitPlayer, AdminCommandType.CAPTURE);
		} else {
        	String townName = getArgs()[2];
        	String kingdomName = getArgs()[3];

			// Check whether the town name is a town or the capital.
			// If it's a town, capture the town for the kingdom.
			// If it's a capital, remove the old kingdom.

			// Check for existing kingdom
			if(!getKonquest().getKingdomManager().isKingdom(kingdomName)) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(kingdomName));
				return;
			}
			KonKingdom kingdom = getKonquest().getKingdomManager().getKingdom(kingdomName);
			assert kingdom != null;

			// Check for existing town in kingdom
			if(!kingdom.isCreated() || kingdom.hasTown(townName) || kingdom.hasCapital(townName)) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
				return;
			}
			// Find town
			KonTown town = null;
			boolean isCapital = false;
			for(KonKingdom testKingdom : getKonquest().getKingdomManager().getKingdoms()) {
				if(testKingdom.hasTown(townName)) {
					town = testKingdom.getTown(townName);
				} else if(testKingdom.hasCapital(townName)) {
					town = testKingdom.getCapital();
					isCapital = true;
				}
			}
			// Check for found town or capital name
			if(town == null) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(townName));
				return;
			}

			// Capture the town/capital
			String townKingdomName = town.getKingdom().getName();
			KonTown capturedTown = null;
			if(isCapital) {
				capturedTown = getKonquest().getKingdomManager().captureCapital(townKingdomName, kingdom);
			} else {
				capturedTown = getKonquest().getKingdomManager().captureTown(townName, town.getKingdom().getName(), kingdom);
			}

			if(capturedTown != null) {
				String newKingdomName = kingdom.getName();
				if(isCapital) {
					ChatUtil.sendBroadcast(MessagePath.PROTECTION_NOTICE_KINGDOM_CONQUER.getMessage(townKingdomName,newKingdomName));
				} else {
					ChatUtil.sendBroadcast(MessagePath.PROTECTION_NOTICE_CONQUER.getMessage(townName));
				}
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
				// For all online players...
				for(KonPlayer onlinePlayer : getKonquest().getPlayerManager().getPlayersOnline()) {
					// Teleport all players inside center chunk to new spawn location
					if(capturedTown.isLocInsideCenterChunk(onlinePlayer.getBukkitPlayer().getLocation())) {
						onlinePlayer.getBukkitPlayer().teleport(getKonquest().getSafeRandomCenteredLocation(capturedTown.getCenterLoc(), 2));
						onlinePlayer.getBukkitPlayer().playEffect(onlinePlayer.getBukkitPlayer().getLocation(), Effect.ANVIL_LAND, null);
					}
					// Remove mob targets
					if(capturedTown.isLocInside(onlinePlayer.getBukkitPlayer().getLocation())) {
						onlinePlayer.clearAllMobAttackers();
					}
					// Update particle border renders for nearby players
					for(Chunk chunk : getKonquest().getAreaChunks(onlinePlayer.getBukkitPlayer().getLocation(), 2)) {
						if(capturedTown.hasChunk(chunk)) {
							getKonquest().getTerritoryManager().updatePlayerBorderParticles(onlinePlayer);
							break;
						}
					}
				}
				// Broadcast to Dynmap
				int x = capturedTown.getCenterLoc().getBlockX();
				int y = capturedTown.getCenterLoc().getBlockY();
				int z = capturedTown.getCenterLoc().getBlockZ();
				getKonquest().getMapHandler().postDynmapBroadcast(MessagePath.PROTECTION_NOTICE_CONQUER.getMessage(capturedTown.getName())+" ("+x+","+y+","+z+")");
				// Broadcast to Discord
				getKonquest().getIntegrationManager().getDiscordSrv().sendGameToDiscordMessage("global", ":crossed_swords: **"+MessagePath.PROTECTION_NOTICE_CONQUER_DISCORD.getMessage(capturedTown.getName(),capturedTown.getKingdom().getName())+"**");
				capturedTown.getMonumentTimer().stopTimer();
				capturedTown.setAttacked(false,null);
				capturedTown.setBarProgress(1.0);
				capturedTown.updateBarTitle();
			} else {
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_FAILED.getMessage());
			}
        }
	}

	@Override
	public List<String> tabComplete() {
		// k admin capture <town> <kingdom>
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		if(getArgs().length == 3) {
			// Suggest town names
			List<String> townList = new ArrayList<>();
			for(KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
				townList.addAll(kingdom.getTownNames());
				townList.add(kingdom.getName());
			}
			tabList.addAll(townList);
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			// Suggest enemy kingdom names
			List<String> kingdomList = getKonquest().getKingdomManager().getKingdomNames();
			tabList.addAll(kingdomList);
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
