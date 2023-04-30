package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.api.model.KonquestDiplomacyType;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MapView.Scale;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SpyCommand extends CommandBase {

	public SpyCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

	@Override
	public void execute() {
		// k spy
    	if (getArgs().length != 1) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
		} else {
        	Player bukkitPlayer = (Player) getSender();
        	World bukkitWorld = bukkitPlayer.getWorld();
        	// Verify allowed world
        	if(!getKonquest().isWorldValid(bukkitWorld)) {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
                return;
        	}
        	
        	if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	// Verify enough favor
        	double cost = getKonquest().getCore().getDouble(CorePath.FAVOR_COST_SPY.getPath(),0.0);
			if(cost > 0) {
				if(KonquestPlugin.getBalance(bukkitPlayer) < cost) {
					ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(cost));
                    return;
				}
			}
			
			// Verify enough inventory space to place map
			if(bukkitPlayer.getInventory().firstEmpty() == -1) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SPY_ERROR_INVENTORY.getMessage());
                return;
			}
			
			// Find the nearest enemy town
			KonKingdom playerKingdom = player.getKingdom();
			List<KonKingdom> enemyKingdoms = playerKingdom.getActiveRelationKingdoms(KonquestDiplomacyType.WAR);
			KonTerritory closestTerritory = null;
			int minDistance = Integer.MAX_VALUE;
			for(KonKingdom kingdom : enemyKingdoms) {
				for(KonTown town : kingdom.getCapitalTowns()) {
					// Only find enemy towns which do not have the counter-intelligence upgrade level 1+
					int upgradeLevel = getKonquest().getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.COUNTER);
					if(upgradeLevel < 1) {
						int townDist = Konquest.chunkDistance(bukkitPlayer.getLocation(), town.getCenterLoc());
						if(townDist != -1 && townDist < minDistance) {
							minDistance = townDist;
							closestTerritory = town;
						}
					}
				}
			}
			if(closestTerritory == null) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SPY_ERROR_TOWN.getMessage());
                return;
			}
			
			// Generate map item
			ChatUtil.printDebug("Generating map...");
			ItemStack item = new ItemStack(Material.FILLED_MAP, 1);
			MapMeta meta = (MapMeta)item.getItemMeta();
			meta.setColor(Color.RED);
			meta.setLocationName(closestTerritory.getName());
			MapView view = Bukkit.getServer().createMap(bukkitPlayer.getWorld());
			//view.addRenderer(new KonMapRenderer(closestTerritory.getName()));
			view.setCenterX(closestTerritory.getCenterLoc().getBlockX());
			view.setCenterZ(closestTerritory.getCenterLoc().getBlockZ());
			view.setScale(Scale.FARTHEST);
			view.setTrackingPosition(true);
			view.setUnlimitedTracking(true);
			view.setLocked(false);
			for(MapRenderer ren : view.getRenderers()) {
				if(ren != null) {
					ren.initialize(view);
				}
			}
			meta.setMapView(view);
			meta.setLore(Arrays.asList(ChatColor.RESET+""+ChatColor.AQUA+closestTerritory.getName(),ChatColor.RED+"Spy Map",ChatColor.YELLOW+"Centered on an enemy Town"));
			item.setItemMeta(meta);
			// Place map item in player's inventory
			PlayerInventory inv = bukkitPlayer.getInventory();
			inv.setItem(inv.firstEmpty(), inv.getItemInMainHand());
			inv.setItemInMainHand(item);
			
            if(KonquestPlugin.withdrawPlayer(bukkitPlayer, cost)) {
            	getKonquest().getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)cost);
            }
            
			String dist;
			if(minDistance < 32) {
				dist = MessagePath.COMMAND_SPY_NOTICE_NEARBY.getMessage();
			} else if (minDistance < 64) {
				dist = MessagePath.COMMAND_SPY_NOTICE_REGIONAL.getMessage();
			} else if (minDistance < 128) {
				dist = MessagePath.COMMAND_SPY_NOTICE_FARAWAY.getMessage();
			} else {
				dist = MessagePath.COMMAND_SPY_NOTICE_VERY_DISTANT.getMessage();
			}
			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_SPY_NOTICE_SUCCESS.getMessage(dist));
        }
		
	}

	@Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
	
	

}
