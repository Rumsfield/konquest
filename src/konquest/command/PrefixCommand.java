package konquest.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import konquest.Konquest;
import konquest.model.KonPlayer;
import konquest.model.KonPrefixType;
import konquest.utility.ChatUtil;
import konquest.utility.MessageStatic;

public class PrefixCommand extends CommandBase {

	public PrefixCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k prefix <name>|off
    	if (getArgs().length != 2) {
            ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
            return;
        } else {
        	if(!getKonquest().getAccomplishmentManager().isEnabled()) {
        		ChatUtil.sendError((Player) getSender(), "Prefixes are currently disabled, talk to an admin.");
        		return;
        	}
        	
        	Player bukkitPlayer = (Player) getSender();
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);

        	if(player.isBarbarian()) {
        		ChatUtil.sendError((Player) getSender(), "Barbarians cannot have a prefix title.");
        		return;
        	}
        	
        	String prefixReq = getArgs()[1];
        	if(prefixReq.equalsIgnoreCase("off")) {
        		// Selected off
        		if(player.getPlayerPrefix().isEnabled()) {
        			player.getPlayerPrefix().setEnable(false);
        			ChatUtil.sendNotice((Player) getSender(), "Turned off your prefix title");
        		} else {
        			ChatUtil.sendNotice((Player) getSender(), "Your prefix title is already off");
        		}
        	} else if(KonPrefixType.isPrefixName(prefixReq)) {
        		// Selected an existing prefix
        		KonPrefixType prefixChosen = KonPrefixType.getPrefixByName(prefixReq);
        		if(player.getPlayerPrefix().selectPrefix(prefixChosen)) {
        			player.getPlayerPrefix().setEnable(true);
        			ChatUtil.sendNotice((Player) getSender(), "Your prefix is now "+prefixChosen.getName());
        		} else {
        			ChatUtil.sendNotice((Player) getSender(), "The prefix "+prefixChosen.getName()+" is not unlocked!");
        		}
        	} else {
        		// Bad selection, not a prefix
        		ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
        	}
        }
	}

	@Override
	public List<String> tabComplete() {
		// k prefix <name>|off
		if(getKonquest().getAccomplishmentManager().isEnabled()) {
			List<String> tabList = new ArrayList<>();
			final List<String> matchedTabList = new ArrayList<>();
			
			if(getArgs().length == 2) {
				Player bukkitPlayer = (Player) getSender();
				KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
				List<String> prefixList = (List<String>) player.getPlayerPrefix().getPrefixNames();
				if(!player.isBarbarian()) {
		    		tabList.add("off");
		    		tabList.addAll(prefixList);
		    	}
				// Trim down completion options based on current input
				StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
				Collections.sort(matchedTabList);
			}
			return matchedTabList;
		} else {
			return Collections.emptyList();
		}
	}
}
