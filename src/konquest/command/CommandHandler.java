package konquest.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
//import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import konquest.Konquest;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;
import konquest.command.admin.AdminCommand;
import konquest.command.admin.AdminCommandType;

//public class CommandHandler  implements CommandExecutor {
public class CommandHandler  implements TabExecutor {

	private Konquest konquest;
	
	public CommandHandler(Konquest konquest) {
        this.konquest = konquest;
    }
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Bukkit.getLogger().info("You need to be a player to issue commands.");
            return false;
        }
        
        //ChatUtil.printDebug("New Command, player "+sender.getName()+" args "+Arrays.toString(args));
        if (sender.hasPermission("konquest.command")) {
        	if (args.length == 0) {
        		new KonquestCommand(konquest, sender).execute();
        		//ChatUtil.printDebug("Command had no args");
            } else {
            	//Get command type. If args[0] is not a command, defaults to HELP
            	CommandType commandArg = CommandType.getCommand(args[0]);
            	//ChatUtil.printDebug("Command type is "+commandArg.toString());
            	// Handle admin commands differently from normal commands
            	if (commandArg.equals(CommandType.ADMIN)) {
            		// Command is an admin command
            		AdminCommandType adminArg = AdminCommandType.getCommand(args[1]);
            		if (sender.hasPermission(commandArg.permission()) || sender.hasPermission(adminArg.permission())) {
            			// Sender has permission for this admin command
            			new AdminCommand(konquest, sender, args).execute();
            		} else {
            			// Sender does not have permission for this admin command
            			ChatUtil.sendError((Player) sender, MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage()+" "+commandArg.permission());
            			ChatUtil.sendError((Player) sender, MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage()+" "+adminArg.permission());
            		} 
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
	                    case EXILE:
	                        new ExileCommand(konquest, sender, args).execute();
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
	                    case JOIN:
	                        new JoinCommand(konquest, sender, args).execute();
	                        break;
	                    case LEAVE:
	                        new LeaveCommand(konquest, sender, args).execute();
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
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> tabList = new ArrayList<>();

		if (sender.hasPermission("konquest.command")) {
			//ChatUtil.printDebug("Entering onTabComplete, length: "+args.length+", args: "+String.join(",",args));
        	if (args.length == 1) {
        		for(CommandType cmd : CommandType.values()) {
        			if(sender.hasPermission(cmd.permission())) {
        				List<String> baseList = new ArrayList<>();
            			baseList.add(cmd.toString().toLowerCase());
            			// Trim down completion options based on current input
            			StringUtil.copyPartialMatches(args[0], baseList, tabList);
            			Collections.sort(tabList);
        			}
        		}
        		//ChatUtil.printDebug("Tab Complete for 0 args: "+args.length);
        	} else if(args.length >= 1){
            	//Get command type. If args[0] is not a command, defaults to HELP
            	CommandType commandArg = CommandType.getCommand(args[0]);
            	//ChatUtil.printDebug("Arg is "+args[0]+", Command type is "+commandArg.toString());
            	if (sender.hasPermission(commandArg.permission())) {
            		switch (commandArg) {
                    case ADMIN:
                    	tabList.addAll(new AdminCommand(konquest, sender, args).tabComplete());
                        break;
                    case BORDER:
                    	tabList.addAll(new BorderCommand(konquest, sender, args).tabComplete());
                        break;
                    case CHAT:
                    	tabList.addAll(new ChatCommand(konquest, sender, args).tabComplete());
                    	//ChatUtil.printDebug("Tab Complete for ChatCommand: "+args.length);
                        break;
                    case CLAIM:
                    	tabList.addAll(new ClaimCommand(konquest, sender, args).tabComplete());
                    	//ChatUtil.printDebug("Tab Complete for ClaimCommand: "+args.length);
                        break;
                    case EXILE:
                    	tabList.addAll(new ExileCommand(konquest, sender, args).tabComplete());
                    	//ChatUtil.printDebug("Tab Complete for ExileCommand: "+args.length);
                        break;
                    case FAVOR:
                    	tabList.addAll(new FavorCommand(konquest, sender, args).tabComplete());
                    	//ChatUtil.printDebug("Tab Complete for FavorCommand: "+args.length);
                        break;
                    case FLY:
                    	tabList.addAll(new FlyCommand(konquest, sender, args).tabComplete());
                        break;
                    case INFO:
                    	tabList.addAll(new InfoCommand(konquest, sender, args).tabComplete());
                    	//ChatUtil.printDebug("Tab Complete for InfoCommand: "+args.length);
                        break;
                    case JOIN:
                    	tabList.addAll(new JoinCommand(konquest, sender, args).tabComplete());
                    	//ChatUtil.printDebug("Tab Complete for JoinCommand: "+args.length);
                        break;
                    case LEAVE:
                    	tabList.addAll(new LeaveCommand(konquest, sender, args).tabComplete());
                    	//ChatUtil.printDebug("Tab Complete for JoinCommand: "+args.length);
                        break;
                    case LIST:
                    	tabList.addAll(new ListCommand(konquest, sender, args).tabComplete());
                    	//ChatUtil.printDebug("Tab Complete for ListCommand: "+args.length);
                        break;
                    case MAP:
                    	tabList.addAll(new MapCommand(konquest, sender, args).tabComplete());
                    	//ChatUtil.printDebug("Tab Complete for MapCommand: "+args.length);
                        break;
                    case PREFIX:
                    	tabList.addAll(new PrefixCommand(konquest, sender, args).tabComplete());
                    	//ChatUtil.printDebug("Tab Complete for SpyCommand: "+args.length);
                    	break;
                    case QUEST:
                    	tabList.addAll(new QuestCommand(konquest, sender, args).tabComplete());
                    	//ChatUtil.printDebug("Tab Complete for MapCommand: "+args.length);
                        break;
                    case SCORE:
                    	tabList.addAll(new ScoreCommand(konquest, sender, args).tabComplete());
                    	//ChatUtil.printDebug("Tab Complete for ScoreCommand: "+args.length);
                    	break;
                    case SETTLE:
                    	tabList.addAll(new SettleCommand(konquest, sender, args).tabComplete());
                    	//ChatUtil.printDebug("Tab Complete for SettleCommand: "+args.length);
                    	break;
                    case SPY:
                    	tabList.addAll(new SpyCommand(konquest, sender, args).tabComplete());
                    	//ChatUtil.printDebug("Tab Complete for SpyCommand: "+args.length);
                    	break;
                    case STATS:
                    	tabList.addAll(new StatsCommand(konquest, sender, args).tabComplete());
                    	//ChatUtil.printDebug("Tab Complete for SpyCommand: "+args.length);
                    	break;
                    case TOWN:
                    	tabList.addAll(new TownCommand(konquest, sender, args).tabComplete());
                    	//ChatUtil.printDebug("Tab Complete for TownCommand: "+args.length);
                        break;
                    case TRAVEL:
                    	tabList.addAll(new TravelCommand(konquest, sender, args).tabComplete());
                    	//ChatUtil.printDebug("Tab Complete for TravelCommand: "+args.length);
                        break;
                    case HELP:
                    	tabList.addAll(new HelpCommand(konquest, sender, args).tabComplete());
                    	//ChatUtil.printDebug("Tab Complete for HelpCommand: "+args.length);
                        break;
                    default:
                    	//ChatUtil.printDebug("Tab Complete for none: "+args.length);
                    	break;
            		}
            	} /*else {
                    ChatUtil.sendError((Player) sender, "Missing permission "+commandArg.permission());
            	}*/
        	}
        } /*else {
        	ChatUtil.sendError((Player) sender, "Missing permission konquest.command");
        }*/
		//ChatUtil.printDebug("Exiting onTabComplete");
        return tabList;
	}
	
	
}
