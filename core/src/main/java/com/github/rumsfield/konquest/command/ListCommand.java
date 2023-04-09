package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListCommand extends CommandBase {

	public ListCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	// Display a paged list of names
    public void execute() {
    	// k list [kingdom|town|sanctuary] [<page>]
    	if (getArgs().length > 3) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
		} else {
        	Player bukkitPlayer = (Player) getSender();

        	// Determine list mode
        	ListType mode = ListType.KINGDOM;
        	if (getArgs().length >= 2) {
        		String listMode = getArgs()[1];
        		if(listMode.equalsIgnoreCase("kingdom")) {
					mode = ListType.KINGDOM;
				} else if(listMode.equalsIgnoreCase("town")) {
        			mode = ListType.TOWN;
        		} else if(listMode.equalsIgnoreCase("sanctuary")) {
					mode = ListType.SANCTUARY;
				} else {
        			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
                    return;
        		}
        	}
        	
        	// Populate list lines
        	List<String> lines = new ArrayList<>();
        	switch(mode) {
	        	case KINGDOM:
	                lines.addAll(getKonquest().getKingdomManager().getKingdomNames());
	        		break;
	        	case TOWN:
	        		lines.addAll(getKonquest().getKingdomManager().getTownNames());
	        		break;
				case SANCTUARY:
					lines.addAll(getKonquest().getSanctuaryManager().getSanctuaryNames());
					break;
	        	default :
	        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	                return;
        	}
        	Collections.sort(lines);
        	ChatUtil.printDebug("List mode "+ mode +" with "+lines.size()+" lines.");
        	
        	if(lines.isEmpty()) {
        		// Nothing to display
        		String header = MessagePath.COMMAND_LIST_NOTICE_HEADER.getMessage(mode.getLabel()) + " 0/0";
            	ChatUtil.sendNotice(bukkitPlayer, header);
        	} else {
        		// Display paged lines to player
        		int numLines = lines.size(); // should be 1 or more
				int MAX_LINES = 8;
				int totalPages = (int)Math.ceil(((double)numLines)/MAX_LINES); // 1-based
            	totalPages = Math.max(totalPages, 1);
            	int page = 1; // 1-based
            	if (getArgs().length == 3) {
            		try {
            			page = Integer.parseInt(getArgs()[2]);
        			}
					catch (NumberFormatException ex) {
						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(ex.getMessage()));
						return;
					}
            	}
            	// Clamp page index
				page = Math.min(page,totalPages);
				page = Math.max(page,1);
            	// Determine line start and end
            	int startIdx = (page-1) * MAX_LINES;
            	int endIdx = startIdx + MAX_LINES;
            	// Display lines to player
				String header = MessagePath.COMMAND_LIST_NOTICE_HEADER.getMessage(mode.getLabel()) + " "+page+"/"+totalPages;
            	ChatUtil.sendNotice(bukkitPlayer,header);
				List<String> pageLines = new ArrayList<>();
            	for (int i = startIdx; i < endIdx && i < numLines; i++) {
            		String line = ""+ChatColor.GOLD+(i+1)+". "+ChatColor.AQUA+lines.get(i);
					pageLines.add(line);
            	}
				ChatUtil.sendCommaNotice(bukkitPlayer,pageLines);
        	}
        }
    }
    
    @Override
	public List<String> tabComplete() {
    	// k list [kingdom|town|sanctuary] [<page>]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 2) {
			tabList.add("kingdom");
			tabList.add("town");
			tabList.add("sanctuary");
			
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 3) {
			tabList.add("#");
			
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}

}
