package konquest.command;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;
import konquest.utility.Timer;
//import net.milkbowl.vault.economy.EconomyResponse;

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
            ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	World bukkitWorld = bukkitPlayer.getWorld();

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
        	if(player.isBarbarian()) {
        		//ChatUtil.sendError((Player) getSender(), "You are already a disgrace to your Kingdom.");
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
        		return;
        	} else {
        		
        		// Confirm with the player
        		if(!player.isExileConfirmed()) {
        			boolean isAllowSwitch = getKonquest().getConfigManager().getConfig("core").getBoolean("core.kingdoms.allow_exile_switch",false);
        			// Prompt the player to confirm
        			//ChatUtil.sendNotice((Player) getSender(), "Are you sure you want to become a "+ChatColor.DARK_RED+"Barbarian?");
        			//ChatUtil.sendNotice((Player) getSender(), "You will lose all Favor, stats and titles, and be sent to a random location in the Wild.",ChatColor.DARK_RED);
        			ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_EXILE_NOTICE_PROMPT_1.getMessage(),ChatColor.DARK_RED);
        			ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_EXILE_NOTICE_PROMPT_2.getMessage(),ChatColor.DARK_RED);
        			if(isAllowSwitch) {
        				//ChatUtil.sendNotice((Player) getSender(), "Once exiled, you may join any other Kingdom.",ChatColor.DARK_RED);
        				ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_EXILE_NOTICE_PROMPT_3A.getMessage(),ChatColor.DARK_RED);
        			} else {
        				//ChatUtil.sendNotice((Player) getSender(), "You can only rejoin your current Kingdom, else you must ask an admin to switch Kingdoms.",ChatColor.DARK_RED);
        				ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_EXILE_NOTICE_PROMPT_3B.getMessage(),ChatColor.DARK_RED);
        			}
        			//ChatUtil.sendNotice((Player) getSender(), "Use this command again to confirm.",ChatColor.DARK_RED);
        			ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_EXILE_NOTICE_PROMPT_4.getMessage(),ChatColor.DARK_RED);
        			player.setIsExileConfirmed(true);
        			// Start a timer to cancel the confirmation
        			ChatUtil.printDebug("Starting exile confirmation timer for 60 seconds for player "+bukkitPlayer.getName());
        			Timer exileConfirmTimer = player.getExileConfirmTimer();
        			exileConfirmTimer.stopTimer();
        			exileConfirmTimer.setTime(60);
        			exileConfirmTimer.startTimer();
        			
        		} else {
        			// Exile the player
        			if(getKonquest().getKingdomManager().exilePlayer(player,true,true)) {
            			boolean doRemoveFavor = getKonquest().getConfigManager().getConfig("core").getBoolean("core.exile.remove_favor", true);
            			if(doRemoveFavor) {
	            			double balance = KonquestPlugin.getBalance(bukkitPlayer);
	            			KonquestPlugin.withdrawPlayer(bukkitPlayer, balance);
            			}
                    	//ChatUtil.sendNotice((Player) getSender(), ChatColor.GRAY+"You have been exiled as a "+ChatColor.DARK_RED+"Barbarian"+ChatColor.GRAY+", place a bed to create your Camp.");
                    	ChatUtil.sendNotice((Player) getSender(), MessagePath.COMMAND_EXILE_NOTICE_CONFIRMED.getMessage());
                    	player.setIsExileConfirmed(true);
                    	player.getExileConfirmTimer().stopTimer();
            		} else {
            			//ChatUtil.sendError((Player) getSender(), "Internal error, could not exile. Contact an Admin!");
            			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
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
