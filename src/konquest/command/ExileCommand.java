package konquest.command;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessageStatic;
import konquest.utility.Timer;
import net.milkbowl.vault.economy.EconomyResponse;

import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ExileCommand extends CommandBase {

	public ExileCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k exile
    	if (getArgs().length != 1) {
            ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
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
        		ChatUtil.sendError((Player) getSender(), "You are already a disgrace to your Kingdom.");
        		return;
        	} else {
        		
        		// Confirm with the player
        		if(!player.isExileConfirmed()) {
        			boolean isAllowSwitch = getKonquest().getConfigManager().getConfig("core").getBoolean("core.kingdoms.allow_exile_switch",false);
        			// Prompt the player to confirm
        			ChatUtil.sendNotice((Player) getSender(), "Are you sure you want to become a "+ChatColor.DARK_RED+"Barbarian?");
        			ChatUtil.sendNotice((Player) getSender(), "You will lose all Favor, stats and titles, and be sent to a random location in the Wild.",ChatColor.DARK_RED);
        			if(isAllowSwitch) {
        				ChatUtil.sendNotice((Player) getSender(), "Once exiled, you may join any other Kingdom.",ChatColor.DARK_RED);
        			} else {
        				ChatUtil.sendNotice((Player) getSender(), "You can only rejoin your current Kingdom, else you must ask an admin to switch Kingdoms.",ChatColor.DARK_RED);
        			}
        			ChatUtil.sendNotice((Player) getSender(), "Use this command again to confirm.",ChatColor.DARK_RED);
        			player.setIsExileConfirmed(true);
        			// Start a timer to cancel the confirmation
        			ChatUtil.printDebug("Starting exile confirmation timer for 60 seconds for player "+bukkitPlayer.getName());
        			Timer exileConfirmTimer = player.getExileConfirmTimer();
        			exileConfirmTimer.stopTimer();
        			exileConfirmTimer.setTime(60);
        			exileConfirmTimer.startTimer();
        			
        		} else {
        			// Exile the player
            		if(getKonquest().getKingdomManager().exilePlayer(player)) {
            			double balance = KonquestPlugin.getEconomy().getBalance(bukkitPlayer);
                    	EconomyResponse r = KonquestPlugin.getEconomy().withdrawPlayer(bukkitPlayer, balance);
        	            if(r.transactionSuccess()) {
        	            	ChatUtil.sendNotice((Player) getSender(), "Favor lost");
        	            } else {
        	            	ChatUtil.sendError((Player) getSender(), String.format("An error occured: %s", r.errorMessage));
        	            }
                    	ChatUtil.sendNotice((Player) getSender(), ChatColor.GRAY+"You have been exiled as a "+ChatColor.DARK_RED+"Barbarian"+ChatColor.GRAY+", place a bed to create your Camp.");
            		} else {
            			ChatUtil.sendError((Player) getSender(), "Internal error, could not exile. Contact an Admin!");
            		}
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
