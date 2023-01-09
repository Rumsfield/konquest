package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class ListCommand extends CommandBase {

	enum ListType {
		KINGDOM,
		TOWN;
	}
	
	private final int MAX_LINES = 8;
	
	public ListCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	// Display a paged list of names
    public void execute() {
    	// k list [kingdom|town] [<page>]
    	if (getArgs().length > 3) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	
        	// Determine list mode
        	ListType mode = ListType.KINGDOM;
        	if (getArgs().length >= 2) {
        		String listMode = getArgs()[1];
        		if(listMode.equalsIgnoreCase("kingdom")) {
        			mode = ListType.KINGDOM;
        		} else if(listMode.equalsIgnoreCase("town")) {
        			mode = ListType.TOWN;
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
    	// k list [kingdom|town] [<page>]
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 2) {
			tabList.add("kingdom");
			tabList.add("town");
			
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
	
    /*
	public void executeOld() {
		// k list kingdoms|towns
    	if (getArgs().length != 2) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	if(!getKonquest().getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        	String cmdMode = getArgs()[1];
        	if(cmdMode.equalsIgnoreCase("kingdoms")) {
        		String kingdomList = "";
            	for(String kingdomName : getKonquest().getKingdomManager().getKingdomNames()) {
            		kingdomList = kingdomList + " " + ChatColor.AQUA+kingdomName +ChatColor.GRAY+",";
            	}
            	if(kingdomList.length() > 2) {
            		kingdomList = kingdomList.substring(0,kingdomList.length()-2);
    			}
            	//kingdomList = kingdomList.substring(0, kingdomList.length()-2);
            	ChatUtil.sendNotice(bukkitPlayer, MessagePath.LABEL_KINGDOMS.getMessage()+":"+kingdomList);
        	} else if(cmdMode.equalsIgnoreCase("towns")) {
        		if(!player.isBarbarian()) {
        			String townList = "";
                	for(KonTown town : player.getKingdom().getTowns()) {
                		String townName = town.getName();
                		int pop = town.getNumResidents();
                		townList = townList + " "+ChatColor.AQUA+townName+ChatColor.YELLOW+"("+pop+")"+ChatColor.GRAY+",";
                	}
                	if(townList.length() > 2) {
                		townList = townList.substring(0,townList.length()-2);
        			}
                	//townList = townList.substring(0, townList.length()-2);
                	ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_LIST_NOTICE_TOWNS.getMessage()+":"+townList);
        		} else {
        			//ChatUtil.sendNotice(bukkitPlayer, "Barbarians do not have any Towns");
        			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
        		}
        	} else {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
                 return;
        	}
        }
	}
	
	public List<String> tabCompleteOld() {
		// k list kingdoms|towns
		List<String> tabList = new ArrayList<>();
		final List<String> matchedTabList = new ArrayList<>();
		
		if(getArgs().length == 2) {
			tabList.add("kingdoms");
			tabList.add("towns");
			
			// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[1], tabList, matchedTabList);
			Collections.sort(matchedTabList);
		}
		return matchedTabList;
	}
	*/
}
