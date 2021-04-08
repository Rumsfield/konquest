package konquest.command.admin;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessageStatic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class ForceJoinAdminCommand extends CommandBase {
	
	public ForceJoinAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
    	// k admin forcejoin player1 kingdom1
    	if (getArgs().length != 4) {
            ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
            return;
        } else {
        	String playerName = getArgs()[2];
        	String kingdomName = getArgs()[3];
        	KonPlayer player = getKonquest().getPlayerManager().getPlayerFromName(playerName);

        	if(player == null) {
        		ChatUtil.sendError((Player) getSender(), "No such player");
        		return;
        	}
        	if(!getKonquest().getKingdomManager().isKingdom(kingdomName)) {
        		ChatUtil.sendError((Player) getSender(), "No such kingdom");
        		return;
        	}
        	getKonquest().getKingdomManager().assignPlayerKingdom(player, kingdomName);
        	ChatUtil.sendNotice((Player) getSender(), "Successfully set player "+playerName+" to kingdom "+kingdomName);
        }
    }
    
    @Override
	public List<String> tabComplete() {
		// k admin forcejoin player1 kingdom1
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
			List<String> kingdomList = getKonquest().getKingdomManager().getKingdomNames();
			tabList.addAll(kingdomList);
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}

}
