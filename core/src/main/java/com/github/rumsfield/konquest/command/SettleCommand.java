package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.api.event.player.KonquestPlayerSettleEvent;
import com.github.rumsfield.konquest.api.event.town.KonquestTownSettleEvent;
import com.github.rumsfield.konquest.model.KonDirective;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonStatsType;
import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SettleCommand extends CommandBase {

	public SettleCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k settle town1
		if (getArgs().length == 1) {
            ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_MISSING_NAME.getMessage());
		} else if (getArgs().length > 2) {
           ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_SPACE_NAME.getMessage());
		} else {
        	Player bukkitPlayer = (Player) getSender();
        	
        	if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	if(player.isBarbarian()) {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
                return;
        	}
        	
        	double cost = getKonquest().getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SETTLE.getPath());
        	double incr = getKonquest().getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SETTLE_INCREMENT.getPath());
        	int townCount = getKonquest().getKingdomManager().getPlayerLordships(player);
        	double adj_cost = (((double)townCount)*incr) + cost;
        	if(cost > 0) {
	        	if(KonquestPlugin.getBalance(bukkitPlayer) < adj_cost) {
					ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(adj_cost));
	                return;
				}
        	}
        	String townName = getArgs()[1];
        	
        	if(getKonquest().validateName(townName,bukkitPlayer) != 0) {
        		// sends player message within the method
        		return;
        	}
        	// Fire pre event
        	KonquestPlayerSettleEvent invokeEvent = new KonquestPlayerSettleEvent(getKonquest(), player, player.getKingdom(), bukkitPlayer.getLocation(), townName);
			Konquest.callKonquestEvent(invokeEvent);
			if(invokeEvent.isCancelled()) {
				return;
			}
        	// Add town
        	int settleStatus = getKonquest().getKingdomManager().createTown(bukkitPlayer.getLocation(), townName, player.getKingdom().getName());
        	if(settleStatus == 0) { // on successful settle..
        		KonTown town = player.getKingdom().getTown(townName);
        		// Teleport player to safe place around monument, facing monument
        		getKonquest().getKingdomManager().teleportAwayFromCenter(town);

        		ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_SETTLE_NOTICE_SUCCESS.getMessage(townName));
        		ChatUtil.sendBroadcast(MessagePath.COMMAND_SETTLE_BROADCAST_SETTLE.getMessage(bukkitPlayer.getName(),townName,player.getKingdom().getName()));
        		// Optionally apply starter shield
        		int starterShieldDuration = getKonquest().getCore().getInt(CorePath.TOWNS_SHIELD_NEW_TOWNS.getPath(),0);
        		if(starterShieldDuration > 0) {
        			getKonquest().getShieldManager().shieldSet(town,starterShieldDuration);
        		}
        		// Play a success sound
        		town.getWorld().playSound(town.getCenterLoc(), Sound.BLOCK_ANVIL_USE, (float)0.5, (float)1.2);
        		// Set player as Lord
        		town.setPlayerLord(player.getOfflineBukkitPlayer());
        		// Update directive progress
        		getKonquest().getDirectiveManager().updateDirectiveProgress(player, KonDirective.SETTLE_TOWN);
        		// Update stats
        		getKonquest().getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.SETTLED,1);
        		getKonquest().getKingdomManager().updatePlayerMembershipStats(player);
        		// Update labels
        		getKonquest().getMapHandler().drawDynmapLabel(town);
        		getKonquest().getMapHandler().drawDynmapLabel(town.getKingdom().getCapital());
        		
        		// Fire post event
        		KonquestTownSettleEvent invokePostEvent = new KonquestTownSettleEvent(getKonquest(), town, player, town.getKingdom());
    			Konquest.callKonquestEvent(invokePostEvent);
        	} else {
        		int distance;
        		switch(settleStatus) {
        		case 1:
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_OVERLAP.getMessage());
        			break;
        		case 2:
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_PLACEMENT.getMessage());
        			break;
        		case 3:
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_NAME.getMessage());
        			break;
        		case 4:
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_TEMPLATE.getMessage());
        			break;
        		case 5:
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
        			break;
        		case 6:
        			distance = getKonquest().getTerritoryManager().getDistanceToClosestTerritory(bukkitPlayer.getLocation());
        			int min_distance_sanc = getKonquest().getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_SANCTUARY.getPath());
        			int min_distance_town = getKonquest().getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_TOWN.getPath());
        			int min_distance = Math.min(min_distance_sanc, min_distance_town);
					ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_PROXIMITY.getMessage(distance,min_distance));
        			break;
        		case 7:
        			distance = getKonquest().getTerritoryManager().getDistanceToClosestTerritory(bukkitPlayer.getLocation());
        			int max_distance_all = getKonquest().getCore().getInt(CorePath.TOWNS_MAX_DISTANCE_ALL.getPath());
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_MAX.getMessage(distance,max_distance_all));
        			break;
        		case 21:
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
        			break;
        		case 22:
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_FLAT.getMessage());
        			break;
        		case 23:
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_HEIGHT.getMessage());
        			break;
        		case 12:
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_HEIGHT.getMessage());
        			break;
        		case 13:
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_INIT.getMessage());
        			break;
        		case 14:
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_AIR.getMessage());
        			break;
        		case 15:
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_WATER.getMessage());
        			break;
        		case 16:
        			ChatUtil.sendError((Player) getSender(), MessagePath.COMMAND_SETTLE_ERROR_FAIL_CONTAINER.getMessage());
        			break;
        		default:
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
        			break;
        		}
        		ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_SETTLE_NOTICE_MAP_HINT.getMessage());
        	}
        	
			if(cost > 0 && settleStatus == 0) {
	            if(KonquestPlugin.withdrawPlayer(bukkitPlayer, adj_cost)) {
	            	getKonquest().getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)cost);
	            }
			}
        }
	}
	
	@Override
	public List<String> tabComplete() {
		// k settle ***
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 2) {
			tabList.add("***");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
