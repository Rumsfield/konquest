package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.api.event.player.KonquestPlayerSettleEvent;
import com.github.rumsfield.konquest.api.event.town.KonquestTownSettleEvent;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.HelperUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SettleCommand extends CommandBase {

	public SettleCommand() {
		// Define name and sender support
		super("settle",true, false);
		// name
		addArgument(
				newArg("name",false,false)
		);
    }
	
	public void execute(Konquest konquest, CommandSender sender, List<String> args) {
		// Sender must be player
		KonPlayer player = konquest.getPlayerManager().getPlayer(sender);
		if (player == null) {
			sendInvalidSenderMessage(sender);
			return;
		}
		// Parse arguments
		if (args.isEmpty()) {
            ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_MISSING_NAME.getMessage());
		} else if (args.size() > 1) {
           ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_SPACE_NAME.getMessage());
		} else {
			// Single argument
        	if(player.isBarbarian()) {
        		ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
                return;
        	}
			Player bukkitPlayer = player.getBukkitPlayer();
			// Check for permission
			if(!bukkitPlayer.hasPermission("konquest.create.town")) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage()+" konquest.create.town");
				return;
			}
			// Check for other plugin flags
			if(konquest.getIntegrationManager().getWorldGuard().isEnabled()) {
				// Check new territory claims
				Location settleLoc = bukkitPlayer.getLocation();
				int radius = konquest.getCore().getInt(CorePath.TOWNS_INIT_RADIUS.getPath());
				World locWorld = settleLoc.getWorld();
				for(Point point : HelperUtil.getAreaPoints(settleLoc, radius)) {
					if(!konquest.getIntegrationManager().getWorldGuard().isChunkClaimAllowed(locWorld,point,bukkitPlayer)) {
						// A region is denying this action
						ChatUtil.sendError(bukkitPlayer, MessagePath.REGION_ERROR_CLAIM_DENY.getMessage());
						return;
					}
				}
			}
			// Check max town limit
			KonKingdom settleKingdom = player.getKingdom();
			World settleWorld = bukkitPlayer.getLocation().getWorld();
			if(settleWorld != null) {
				boolean isPerWorld = konquest.getCore().getBoolean(CorePath.KINGDOMS_MAX_TOWN_LIMIT_PER_WORLD.getPath(),false);
				int maxTownLimit = konquest.getCore().getInt(CorePath.KINGDOMS_MAX_TOWN_LIMIT.getPath(),0);
				maxTownLimit = Math.max(maxTownLimit,0); // clamp to 0 minimum
				if(maxTownLimit != 0) {
					int numTownsInWorld = 0;
					if(isPerWorld) {
						// Find towns within the given world
						for(KonTown town : settleKingdom.getCapitalTowns()) {
							if(town.getWorld().equals(settleWorld)) {
								numTownsInWorld++;
							}
						}
					} else {
						// Find all towns
						numTownsInWorld = settleKingdom.getCapitalTowns().size();
					}
					if(numTownsInWorld >= maxTownLimit) {
						// Limit reached
						if(isPerWorld) {
							ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_LIMIT_WORLD.getMessage(numTownsInWorld,maxTownLimit));
						} else {
							ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_FAIL_LIMIT_ALL.getMessage(numTownsInWorld,maxTownLimit));
						}
						return;
					}
				}
			}
			// Check officer only
			boolean isOfficerOnly = konquest.getCore().getBoolean(CorePath.TOWNS_SETTLE_OFFICER_ONLY.getPath(),false);
			if(isOfficerOnly && !settleKingdom.isOfficer(bukkitPlayer.getUniqueId())) {
				// Player is not an officer and must be in order to settle
				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SETTLE_ERROR_OFFICER_ONLY.getMessage());
				return;
			}

			double cost = konquest.getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SETTLE.getPath());
        	double incr = konquest.getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SETTLE_INCREMENT.getPath());
        	int townCount = konquest.getKingdomManager().getPlayerLordships(player);
        	double adj_cost = (((double)townCount)*incr) + cost;
        	if(cost > 0) {
	        	if(KonquestPlugin.getBalance(bukkitPlayer) < adj_cost) {
					ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(adj_cost));
	                return;
				}
        	}
        	String townName = args.get(0);
        	
        	if(konquest.validateName(townName,bukkitPlayer) != 0) {
        		// sends player message within the method
        		return;
        	}
        	// Fire pre event
        	KonquestPlayerSettleEvent invokeEvent = new KonquestPlayerSettleEvent(konquest, player, player.getKingdom(), bukkitPlayer.getLocation(), townName);
			Konquest.callKonquestEvent(invokeEvent);
			if(invokeEvent.isCancelled()) {
				return;
			}
        	// Add town
        	int settleStatus = konquest.getKingdomManager().createTown(bukkitPlayer.getLocation(), townName, player.getKingdom().getName());
        	if(settleStatus == 0) { // on successful settle..
        		KonTown town = player.getKingdom().getTown(townName);
        		// Teleport player to safe place around monument, facing monument
        		konquest.getKingdomManager().teleportAwayFromCenter(town);
				// Send messages
        		ChatUtil.sendNotice(sender, MessagePath.COMMAND_SETTLE_NOTICE_SUCCESS.getMessage(townName));
        		ChatUtil.sendBroadcast(MessagePath.COMMAND_SETTLE_BROADCAST_SETTLE.getMessage(bukkitPlayer.getName(),townName,player.getKingdom().getName()));
        		// Optionally apply starter shield
        		int starterShieldDuration = konquest.getCore().getInt(CorePath.TOWNS_SHIELD_NEW_TOWNS.getPath(),0);
        		if(starterShieldDuration > 0) {
        			konquest.getShieldManager().shieldSet(town,starterShieldDuration);
        		}
        		// Play a success sound
				Konquest.playTownSettleSound(bukkitPlayer.getLocation());
				// Set player as Lord
        		town.setPlayerLord(player.getOfflineBukkitPlayer());
        		// Update directive progress
        		konquest.getDirectiveManager().updateDirectiveProgress(player, KonDirective.SETTLE_TOWN);
        		// Update stats
        		konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.SETTLED,1);
        		konquest.getKingdomManager().updatePlayerMembershipStats(player);
        		// Update labels
        		konquest.getMapHandler().drawLabel(town);
        		konquest.getMapHandler().drawLabel(town.getKingdom().getCapital());
        		
        		// Fire post event
        		KonquestTownSettleEvent invokePostEvent = new KonquestTownSettleEvent(konquest, town, player, town.getKingdom());
    			Konquest.callKonquestEvent(invokePostEvent);
        	} else {
        		int distance;
        		switch(settleStatus) {
        		case 1:
        			ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_OVERLAP.getMessage());
					ChatUtil.sendNotice(sender, MessagePath.COMMAND_SETTLE_NOTICE_MAP_HINT.getMessage());
        			break;
        		case 2:
        			ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_PLACEMENT.getMessage());
					ChatUtil.sendNotice(sender, MessagePath.COMMAND_SETTLE_NOTICE_MAP_HINT.getMessage());
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
        			distance = konquest.getTerritoryManager().getDistanceToClosestTerritory(bukkitPlayer.getLocation());
        			int min_distance_sanc = konquest.getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_SANCTUARY.getPath());
        			int min_distance_town = konquest.getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_TOWN.getPath());
        			int min_distance = Math.min(min_distance_sanc, min_distance_town);
					ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_PROXIMITY.getMessage(distance,min_distance));
					ChatUtil.sendNotice(sender, MessagePath.COMMAND_SETTLE_NOTICE_MAP_HINT.getMessage());
        			break;
        		case 7:
        			distance = konquest.getTerritoryManager().getDistanceToClosestTerritory(bukkitPlayer.getLocation());
        			int max_distance_all = konquest.getCore().getInt(CorePath.TOWNS_MAX_DISTANCE_ALL.getPath());
        			ChatUtil.sendError(sender, MessagePath.COMMAND_SETTLE_ERROR_FAIL_MAX.getMessage(distance,max_distance_all));
					ChatUtil.sendNotice(sender, MessagePath.COMMAND_SETTLE_NOTICE_MAP_HINT.getMessage());
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
        			break;
        		}
        	}
        	
			if(cost > 0 && settleStatus == 0) {
	            if(KonquestPlugin.withdrawPlayer(bukkitPlayer, adj_cost)) {
	            	konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)cost);
	            }
			}
        }
	}
	
	@Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		// Give suggestions
		if(args.size() == 1) {
			tabList.add("***");
		}
		return matchLastArgToList(tabList,args);
	}
}
