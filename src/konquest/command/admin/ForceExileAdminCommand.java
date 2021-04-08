package konquest.command.admin;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessageStatic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class ForceExileAdminCommand extends CommandBase {
	
	public ForceExileAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	// k admin forceexile player1 [full]
    	if (getArgs().length != 3 && getArgs().length != 4) {
            ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	World bukkitWorld = bukkitPlayer.getWorld();

        	if(!bukkitWorld.getName().equals(getKonquest().getWorldName())) {
        		ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_WORLD.toString());
                return;
        	}
        	
        	String playerName = getArgs()[2];
        	KonPlayer player = getKonquest().getPlayerManager().getPlayerFromName(playerName);

        	if(player == null) {
        		ChatUtil.sendError((Player) getSender(), "No such player");
        		return;
        	}
        	
        	if(getKonquest().getKingdomManager().exilePlayer(player)) {
        		ChatUtil.sendNotice(player.getBukkitPlayer(), "You have been exiled as a "+ChatColor.DARK_RED+"Barbarian");
        		ChatUtil.sendNotice((Player) getSender(), "Successfully exiled player "+playerName+" to Barbarians");
        	} else {
        		ChatUtil.sendError((Player) getSender(), "Could not exile player, already barbarian or bad random wild location.");
        		return;
        	}
        	
        	if(getArgs().length == 4 && getArgs()[3].equalsIgnoreCase("full")) {
        		player.setExileKingdom(getKonquest().getKingdomManager().getBarbarians());
        		ChatUtil.sendNotice((Player) getSender(), "Successfully reset Exile Kingdom to Barbarians");
        	}
        	
        }
    }
    
    @Override
	public List<String> tabComplete() {
    	// k admin forceexile player1 [full]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 3) {
			List<String> playerList = new ArrayList<>();
			for(KonPlayer player : getKonquest().getPlayerManager().getPlayersOnline()) {
				playerList.add(player.getBukkitPlayer().getName());
			}
			tabList.addAll(playerList);
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			tabList.add("full");
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
