package konquest.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MapView.Scale;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.model.KonKingdom;
//import konquest.model.KonMapRenderer;
import konquest.model.KonPlayer;
import konquest.model.KonStatsType;
import konquest.model.KonTerritory;
import konquest.model.KonTown;
import konquest.model.KonUpgrade;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;
import net.milkbowl.vault.economy.EconomyResponse;

public class SpyCommand extends CommandBase {

	public SpyCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

	@Override
	public void execute() {
		// k spy
    	if (getArgs().length != 1) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	World bukkitWorld = bukkitPlayer.getWorld();
        	// Verify allowed world
        	if(!getKonquest().isWorldValid(bukkitWorld)) {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
                return;
        	}
        	
        	if(!getKonquest().getPlayerManager().isPlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	// Verify enough favor
        	double cost = getKonquest().getConfigManager().getConfig("core").getDouble("core.favor.cost_spy",0.0);
			if(cost > 0) {
				if(KonquestPlugin.getEconomy().getBalance(bukkitPlayer) < cost) {
					//ChatUtil.sendError(bukkitPlayer, "Not enough Favor, need "+cost);
					ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(cost));
                    return;
				}
			}
			
			// Verify enough inventory space to place map
			if(bukkitPlayer.getInventory().firstEmpty() == -1) {
				//ChatUtil.sendError(bukkitPlayer, "Not enough inventory space, make some room");
				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_SPY_ERROR_INVENTORY.getMessage());
                return;
			}
			
			// Find nearest enemy town
			ArrayList<KonKingdom> enemyKingdoms = getKonquest().getKingdomManager().getKingdoms();
			if(!enemyKingdoms.isEmpty()) {
				enemyKingdoms.remove(player.getKingdom());
			}
			KonTerritory closestTerritory = null;
			int minDistance = Integer.MAX_VALUE;
			for(KonKingdom kingdom : enemyKingdoms) {
				for(KonTown town : kingdom.getTowns()) {
					// Only find enemy towns which do not have the counter-intelligence upgrade level 1+
					int upgradeLevel = getKonquest().getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.COUNTER);
					if(upgradeLevel < 1) {
						int townDist = getKonquest().distanceInChunks(bukkitPlayer.getLocation().getChunk(), town.getCenterLoc().getChunk());
						if(townDist < minDistance) {
							minDistance = townDist;
							closestTerritory = town;
						}
					}
				}
			}
			if(closestTerritory == null) {
				//ChatUtil.sendError(bukkitPlayer, "Could not find an enemy town");
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
					//ChatUtil.printDebug("Found an existing map renderer, contextual "+ren.isContextual());
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
			
			EconomyResponse r = KonquestPlugin.getEconomy().withdrawPlayer(bukkitPlayer, cost);
            if(r.transactionSuccess()) {
            	String balanceF = String.format("%.2f",r.balance);
            	String amountF = String.format("%.2f",r.amount);
            	//ChatUtil.sendNotice((Player) getSender(), "Favor reduced by "+amountF+", total: "+balanceF);
            	ChatUtil.sendNotice((Player) getSender(), MessagePath.GENERIC_NOTICE_REDUCE_FAVOR.getMessage(amountF,balanceF));
            	getKonquest().getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)cost);
            } else {
            	//ChatUtil.sendError((Player) getSender(), String.format("An error occured: %s", r.errorMessage));
            	ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(r.errorMessage));
            }
            
			String dist = "";
			if(minDistance < 32) {
				//dist = "nearby";
				dist = MessagePath.COMMAND_SPY_NOTICE_NEARBY.getMessage();
			} else if (minDistance < 64) {
				//dist = "regional";
				dist = MessagePath.COMMAND_SPY_NOTICE_REGIONAL.getMessage();
			} else if (minDistance < 128) {
				//dist = "faraway";
				dist = MessagePath.COMMAND_SPY_NOTICE_FARAWAY.getMessage();
			} else {
				//dist = "very distant";
				dist = MessagePath.COMMAND_SPY_NOTICE_VERY_DISTANT.getMessage();
			}
			ChatUtil.sendNotice(bukkitPlayer, "A spy has recovered this map of a "+dist+" enemy town!");
			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_SPY_NOTICE_SUCCESS.getMessage(dist));
        }
		
	}

	@Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
	
	

}
