package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class ListAdminCommand extends CommandBase {
	
	enum ListType {
		KINGDOM,
		TOWN,
		CAMP,
		RUIN,
		SANCTUARY;
	}
	
	private final int MAX_LINES = 8;
	
	public ListAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

	// Display a paged list of names
    public void execute() {
    	// k admin list [kingdom|town|camp|ruin|sanctuary] [<page>]
    	if (getArgs().length > 4) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	
        	// Determine list mode
        	ListType mode = ListType.KINGDOM;
        	if (getArgs().length >= 3) {
        		String listMode = getArgs()[2];
        		if(listMode.equalsIgnoreCase("kingdom")) {
        			mode = ListType.KINGDOM;
        		} else if(listMode.equalsIgnoreCase("town")) {
        			mode = ListType.TOWN;
        		} else if(listMode.equalsIgnoreCase("camp")) {
        			mode = ListType.CAMP;
        		} else if(listMode.equalsIgnoreCase("ruin")) {
        			mode = ListType.RUIN;
        		} else if(listMode.equalsIgnoreCase("sanctuary")) {
        			mode = ListType.SANCTUARY;
        		} else {
        			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
                    return;
        		}
        	}
        	
        	// Populate list lines
        	List<String> lines = new ArrayList<String>();
        	switch(mode) {
	        	case KINGDOM:
	                lines.addAll(getKonquest().getKingdomManager().getKingdomNames());
	        		break;
	        	case TOWN:
	        		lines.addAll(getKonquest().getKingdomManager().getTownNames());
	        		break;
	        	case CAMP:
	        		lines.addAll(getKonquest().getCampManager().getCampNames());
	        		break;
	        	case RUIN:
	        		lines.addAll(getKonquest().getRuinManager().getRuinNames());
	        		break;
	        	case SANCTUARY:
	        		lines.addAll(getKonquest().getSanctuaryManager().getSanctuaryNames());
	        		break;
	        	default :
	        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
	                return;
        	}
        	Collections.sort(lines);
        	ChatUtil.printDebug("List mode "+mode.toString()+" with "+lines.size()+" lines.");
        	
        	if(lines.isEmpty()) {
        		// Nothing to display
        		String header = "Konquest "+mode.toString()+" List, page 0/0";
            	ChatUtil.sendNotice((Player) getSender(),header);
        	} else {
        		// Display paged lines to player
        		int numLines = lines.size(); // should be 1 or more
            	int totalPages = (int)Math.ceil((double)numLines/MAX_LINES); // 1-based
            	totalPages = totalPages < 1 ? 1 : totalPages; // clamp to 1
            	int page = 1; // 1-based
            	if (getArgs().length == 3) {
            		try {
            			page = Integer.parseInt(getArgs()[2]);
        			}
        			catch (NumberFormatException e) {
        				page = 1;
        			}
            	}
            	// Clamp page index
            	page = page < 1 ? 1 : page;
            	page = page > totalPages ? totalPages : page;
            	// Determine line start and end
            	int startIdx = (page-1) * MAX_LINES;
            	int endIdx = startIdx + MAX_LINES ;
            	// Display lines to player
            	//TODO: KR path this
            	String header = "Konquest "+mode.toString()+" List, page "+page+"/"+(totalPages);
            	ChatUtil.sendNotice((Player) getSender(),header);
            	for (int i = startIdx; i < endIdx && i < numLines; i++) {
            		String line = lines.get(i);
            		ChatUtil.sendNotice((Player) getSender(),(i+1)+". "+line);
            	}
        	}
        }
    }
    
    @Override
	public List<String> tabComplete() {
    	// k admin list [kingdom|town|camp|ruin|sanctuary] [<page>]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 3) {
			tabList.add("kingdom");
			tabList.add("town");
			tabList.add("camp");
			tabList.add("ruin");
			tabList.add("sanctuary");
			
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[2], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		} else if(getArgs().length == 4) {
			tabList.add("#");
			
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[3], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
}
