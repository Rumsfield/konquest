package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.admin.AdminCommand;
import com.github.rumsfield.konquest.command.admin.AdminCommandType;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandHandler  implements TabExecutor {

	private final Konquest konquest;
	
	public CommandHandler(Konquest konquest) {
        this.konquest = konquest;
    }
	
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            Bukkit.getLogger().info("You need to be a player to issue commands.");
            return false;
        }

        if (sender.hasPermission("konquest.command")) {
        	if (args.length == 0) {
        		new KonquestCommand(konquest, sender).execute();
            } else {
            	//Get command type. If args[0] is not a command, defaults to HELP
            	CommandType commandArg = CommandType.getCommand(args[0]);
            	
            	// Handle admin commands differently from normal commands
            	if (commandArg.equals(CommandType.ADMIN)) {
            		// Command is an admin command
            		new AdminCommand(konquest, sender, args).execute();
            	} else {
            		// Command is a normal command
	            	if (sender.hasPermission(commandArg.permission())) {
	            		// Sender has permission for this command
	            		switch (commandArg) {
	                    case BORDER:
	                        new BorderCommand(konquest, sender, args).execute();
	                        break;
	                    case CHAT:
	                        new ChatCommand(konquest, sender, args).execute();
	                        break;
	                    case CLAIM:
	                        new ClaimCommand(konquest, sender, args).execute();
	                        break;
	                    case FAVOR:
	                        new FavorCommand(konquest, sender, args).execute();
	                        break;
	                    case FLY:
	                        new FlyCommand(konquest, sender, args).execute();
	                        break;
	                    case INFO:
	                        new InfoCommand(konquest, sender, args).execute();
	                        break;
	                    case KINGDOM:
	                        new KingdomCommand(konquest, sender, args).execute();
	                        break;
	                    case LIST:
	                        new ListCommand(konquest, sender, args).execute();
	                        break;
	                    case MAP:
	                        new MapCommand(konquest, sender, args).execute();
	                        break;
	                    case PREFIX:
	                        new PrefixCommand(konquest, sender, args).execute();
	                        break;
	                    case QUEST:
	                        new QuestCommand(konquest, sender, args).execute();
	                        break;
	                    case SCORE:
	                    	new ScoreCommand(konquest, sender, args).execute();
	                    	break;
	                    case SETTLE:
	                    	new SettleCommand(konquest, sender, args).execute();
	                    	break;
	                    case SPY:
	                    	new SpyCommand(konquest, sender, args).execute();
	                    	break;
	                    case STATS:
	                    	new StatsCommand(konquest, sender, args).execute();
	                    	break;
	                    case TOWN:
	                        new TownCommand(konquest, sender, args).execute();
	                        break;
	                    case TRAVEL:
	                        new TravelCommand(konquest, sender, args).execute();
	                        break;
	                    case UNCLAIM:
	                        new UnclaimCommand(konquest, sender, args).execute();
	                        break;
	                    case HELP:
	                        new HelpCommand(konquest, sender, args).execute();
	                        break;
	                    default:
	                    	return false;
	            		}
	            	} else {
	            		// Sender does not have permission for this command
	                    ChatUtil.sendError((Player) sender, MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage()+" "+commandArg.permission());
	            	}
            	}
        	}
        } else {
        	ChatUtil.sendError((Player) sender, MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage()+" konquest.command");
        }
        return true;
    }

	@Override
	public List<String> onTabComplete(CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
		List<String> tabList = new ArrayList<>();

		if (sender.hasPermission("konquest.command")) {
			//ChatUtil.printDebug("Entering onTabComplete, length: "+args.length+", args: "+String.join(",",args));
        	if (args.length == 1) {
        		List<String> baseList = new ArrayList<>();
        		for(CommandType cmd : CommandType.values()) {
        			String suggestion = cmd.toString().toLowerCase();
        			if(cmd.equals(CommandType.ADMIN)) {
        				// Suggest admin command if any sub-command permissions are valid
        				for(AdminCommandType subcmd : AdminCommandType.values()) {
        					if(sender.hasPermission(subcmd.permission()) && !baseList.contains(suggestion)) {
        						baseList.add(suggestion);
        					}
        				}
        			} else if(sender.hasPermission(cmd.permission())) {
        				// Suggest command if permission is valid
        				baseList.add(suggestion);
        			}
        		}
    			// Trim down completion options based on current input
    			StringUtil.copyPartialMatches(args[0], baseList, tabList);
    			Collections.sort(tabList);
        		//ChatUtil.printDebug("Tab Complete for 0 args: "+args.length);
        	} else if(args.length >= 1){
            	//Get command type. If args[0] is not a command, defaults to HELP
            	CommandType commandArg = CommandType.getCommand(args[0]);
            	//ChatUtil.printDebug("Arg is "+args[0]+", Command type is "+commandArg.toString());
            	// Handle admin commands differently from normal commands
            	if (commandArg.equals(CommandType.ADMIN)) {
            		// Command is an admin command
            		tabList.addAll(new AdminCommand(konquest, sender, args).tabComplete());
            	} else {
            		// Command is a normal command
	            	if (sender.hasPermission(commandArg.permission())) {
	            		switch (commandArg) {
	                    case BORDER:
	                    	tabList.addAll(new BorderCommand(konquest, sender, args).tabComplete());
	                        break;
	                    case CHAT:
	                    	tabList.addAll(new ChatCommand(konquest, sender, args).tabComplete());
	                    	break;
	                    case CLAIM:
	                    	tabList.addAll(new ClaimCommand(konquest, sender, args).tabComplete());
	                    	break;
	                    case FAVOR:
	                    	tabList.addAll(new FavorCommand(konquest, sender, args).tabComplete());
	                    	break;
	                    case FLY:
	                    	tabList.addAll(new FlyCommand(konquest, sender, args).tabComplete());
	                        break;
	                    case INFO:
	                    	tabList.addAll(new InfoCommand(konquest, sender, args).tabComplete());
	                    	break;
	                    case KINGDOM:
	                    	tabList.addAll(new KingdomCommand(konquest, sender, args).tabComplete());
	                        break;
	                    case LIST:
	                    	tabList.addAll(new ListCommand(konquest, sender, args).tabComplete());
	                    	break;
	                    case MAP:
	                    	tabList.addAll(new MapCommand(konquest, sender, args).tabComplete());
	                    	break;
	                    case PREFIX:
	                    	tabList.addAll(new PrefixCommand(konquest, sender, args).tabComplete());
	                    	break;
	                    case QUEST:
	                    	tabList.addAll(new QuestCommand(konquest, sender, args).tabComplete());
	                    	break;
	                    case SCORE:
	                    	tabList.addAll(new ScoreCommand(konquest, sender, args).tabComplete());
	                    	break;
	                    case SETTLE:
	                    	tabList.addAll(new SettleCommand(konquest, sender, args).tabComplete());
	                    	break;
	                    case SPY:
	                    	tabList.addAll(new SpyCommand(konquest, sender, args).tabComplete());
	                    	break;
	                    case STATS:
	                    	tabList.addAll(new StatsCommand(konquest, sender, args).tabComplete());
	                    	break;
	                    case TOWN:
	                    	tabList.addAll(new TownCommand(konquest, sender, args).tabComplete());
	                        break;
	                    case TRAVEL:
	                    	tabList.addAll(new TravelCommand(konquest, sender, args).tabComplete());
	                        break;
	                    case UNCLAIM:
	                    	tabList.addAll(new UnclaimCommand(konquest, sender, args).tabComplete());
	                        break;
	                    case HELP:
	                    	tabList.addAll(new HelpCommand(konquest, sender, args).tabComplete());
	                        break;
	                    default:
	                    	break;
	            		}
	            	}
            	}
        	}
        }
        return tabList;
	}
	
	
}
