package konquest.command;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.model.KonDirective;
import konquest.model.KonPlayer;
import konquest.model.KonStatsType;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.MessageStatic;
import net.milkbowl.vault.economy.EconomyResponse;

import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitRunnable;

public class SettleCommand extends CommandBase {

	public SettleCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k settle town1
		if (getArgs().length == 1) {
            //ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
    		ChatUtil.sendError((Player) getSender(), "Must specify a town name");
            return;
        } else if (getArgs().length > 2) {
            //ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
    		ChatUtil.sendError((Player) getSender(), "Bad town name, cannot use spaces!");
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	World bukkitWorld = bukkitPlayer.getWorld();

        	if(!bukkitWorld.getName().equals(getKonquest().getWorldName())) {
        		ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_WORLD.toString());
                return;
        	}
        	
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	if(player.isBarbarian()) {
        		ChatUtil.sendError((Player) getSender(), "Barbarians cannot settle, join a Kingdom with \"/k join\"");
                return;
        	}
        	
        	double cost = getKonquest().getConfigManager().getConfig("core").getDouble("core.favor.cost_settle");
        	double incr = getKonquest().getConfigManager().getConfig("core").getDouble("core.favor.cost_settle_increment");
        	int townCount = getKonquest().getKingdomManager().getPlayerLordships(player);
        	double adj_cost = (((double)townCount)*incr) + cost;
        	if(cost > 0) {
	        	if(KonquestPlugin.getEconomy().getBalance(bukkitPlayer) < adj_cost) {
					ChatUtil.sendError((Player) getSender(), "Not enough Favor, need "+adj_cost);
	                return;
				}
        	}
        	String townName = getArgs()[1];
        	if(!StringUtils.isAlphanumeric(townName)) {
        		ChatUtil.sendError((Player) getSender(), "Town name must only contain letters and/or numbers");
                return;
        	}
        	if(getKonquest().getPlayerManager().isPlayerNameExist(townName)) {
        		ChatUtil.sendError((Player) getSender(), "Could not settle: Invalid Town name, already taken or has spaces.");
                return;
        	}
        	int settleStatus = getKonquest().getKingdomManager().addTown(bukkitPlayer.getLocation(), townName, player.getKingdom().getName());
        	if(settleStatus == 0) { // on successful settle..
        		KonTown town = player.getKingdom().getTown(townName);
        		// Teleport player to safe place around monument, facing monument
        		Location tpLoc = getKonquest().getSafeRandomCenteredLocation(town.getSpawnLoc(), 2);
        		//
        		double x0,x1,z0,z1;
        		x0 = tpLoc.getX();
        		x1 = town.getCenterLoc().getX();
        		z0 = tpLoc.getZ();
        		z1 = town.getCenterLoc().getZ();
        		float yaw = (float)(180-(Math.atan2((x0-x1),(z0-z1))*180/Math.PI));
        		ChatUtil.printDebug("Settle teleport used x0,z0;x1,z1: "+x0+","+z0+";"+x1+","+z1+" and calculated yaw degrees: "+yaw);
        		tpLoc.setYaw(yaw);
        		if(bukkitPlayer.isInsideVehicle()) {
        			ChatUtil.printDebug("Settling player is in a vehicle, type "+bukkitPlayer.getVehicle().getType().toString());
        			Entity vehicle = bukkitPlayer.getVehicle();
        			List<Entity> passengers = vehicle.getPassengers();
        			bukkitPlayer.leaveVehicle();
        			bukkitPlayer.teleport(tpLoc,TeleportCause.PLUGIN);
        			//vehicle.setVelocity(tpLoc.toVector().subtract(vehicle.getLocation().toVector()).normalize());
        			//vehicle.teleport(tpLoc,TeleportCause.PLUGIN);
        			//vehicle.teleport(bukkitPlayer,TeleportCause.PLUGIN);
        			//vehicle.addPassenger(bukkitPlayer);
        			
        			new BukkitRunnable() {
        				public void run() {
        					vehicle.teleport(tpLoc,TeleportCause.PLUGIN);
        					//vehicle.addPassenger(bukkitPlayer);
        					for (Entity e : passengers) {
        						//if (!(e instanceof Player)) {
        							vehicle.addPassenger(e);
        						//}
        					}
        				}
        			}.runTaskLater(getKonquest().getPlugin(), 10L);
        			
        		} else {
        			bukkitPlayer.teleport(tpLoc,TeleportCause.PLUGIN);
        		}
        		ChatUtil.sendNotice((Player) getSender(), "Successfully settled new Town: "+townName);
        		ChatUtil.sendBroadcast(ChatColor.LIGHT_PURPLE+bukkitPlayer.getName()+" has settled the Town of "+ChatColor.AQUA+townName+ChatColor.LIGHT_PURPLE+" for Kingdom "+ChatColor.AQUA+player.getKingdom().getName());
        		// Set player as Lord
        		town.setPlayerLord(player.getOfflineBukkitPlayer());
        		// Add players to town bar
        		town.updateBarPlayers();
        		// Update directive progress
        		getKonquest().getDirectiveManager().updateDirectiveProgress(player, KonDirective.SETTLE_TOWN);
        		// Update stats
        		getKonquest().getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.SETTLED,1);
        		getKonquest().getKingdomManager().updatePlayerMembershipStats(player);
        	} else {
        		switch(settleStatus) {
        		case 1:
        			ChatUtil.sendError((Player) getSender(), "Could not settle: Overlaps with a nearby territory.");
        			break;
        		case 2:
        			ChatUtil.sendError((Player) getSender(), "Could not settle: Couldn't find good Monument placement.");
        			break;
        		case 3:
        			ChatUtil.sendError((Player) getSender(), "Could not settle: Invalid Town name, already taken or has spaces.");
        			break;
        		case 4:
        			ChatUtil.sendError((Player) getSender(), "Could not settle: Invalid Monument Template. Use \"/k admin monument <kingdom> create\"");
        			break;
        		case 5:
        			int distance = getKonquest().getKingdomManager().getDistanceToClosestTerritory(bukkitPlayer.getLocation().getChunk());
        			int min_distance_capital = getKonquest().getConfigManager().getConfig("core").getInt("core.towns.min_distance_capital");
        			int min_distance_town = getKonquest().getConfigManager().getConfig("core").getInt("core.towns.min_distance_town");
        			int min_distance = 0;
        			if(min_distance_capital < min_distance_town) {
        				min_distance = min_distance_capital;
        			} else {
        				min_distance = min_distance_town;
        			}
        			ChatUtil.sendError((Player) getSender(), "Could not settle: Too close to another territory.");
        			ChatUtil.sendError((Player) getSender(), "Currently "+distance+" chunks away, must be at least "+min_distance+".");
        			break;
        		case 11:
        			ChatUtil.sendError((Player) getSender(), "Could not settle: The 16x16 area is not flat enough. Press F3+G to view chunk boundaries. Cut down trees and flatten the ground!");
        			break;
        		case 12:
        			ChatUtil.sendError((Player) getSender(), "Could not settle: Elevation is too high. Try settling somewhere lower.");
        			break;
        		case 13:
        			ChatUtil.sendError((Player) getSender(), "Could not settle: Too much air or water below. Try settling somewhere else, or remove all air/water below you.");
        			break;
        		case 14:
        			ChatUtil.sendError((Player) getSender(), "Could not settle: Problem claiming intial chunks.");
        			break;
        		default:
        			ChatUtil.sendError((Player) getSender(), "Could not settle: Unknown cause. Contact an Admin!");
        			break;
        		}
        		ChatUtil.sendMessage((Player) getSender(), "Use \"/k map\" for helpful info.", ChatColor.RED);
        	}
        	
			if(cost > 0 && settleStatus == 0) {
	        	EconomyResponse r = KonquestPlugin.getEconomy().withdrawPlayer(bukkitPlayer, adj_cost);
	            if(r.transactionSuccess()) {
	            	String balanceF = String.format("%.2f",r.balance);
	            	String amountF = String.format("%.2f",r.amount);
	            	ChatUtil.sendNotice((Player) getSender(), "Favor reduced by "+amountF+", total: "+balanceF);
	            	getKonquest().getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)cost);
	            } else {
	            	ChatUtil.sendError((Player) getSender(), String.format("An error occured: %s", r.errorMessage));
	            }
			}
        }
	}
	
	@Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
