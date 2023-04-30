package com.github.rumsfield.konquest.command.admin;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandBase;
import com.github.rumsfield.konquest.command.KonquestCommand;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminCommand extends CommandBase {

    public AdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
        if (getArgs().length == 1) {
        	if (getSender().hasPermission(AdminCommandType.HELP.permission())) {
        		new HelpAdminCommand(getKonquest(), getSender(), getArgs()).execute();
        	} else {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage()+" "+AdminCommandType.HELP.permission());
        	}
        } else if (getArgs().length >= 2) {
        	AdminCommandType commandArg = AdminCommandType.getCommand(getArgs()[1]);
        	if (getSender().hasPermission(commandArg.permission())) {
	            switch (commandArg) {
		            case BYPASS:
		                new BypassAdminCommand(getKonquest(), getSender(), getArgs()).execute();
		                break;
		            case CAMP:
		                new CampAdminCommand(getKonquest(), getSender(), getArgs()).execute();
		                break;
		            case CAPTURE:
		                new CaptureAdminCommand(getKonquest(), getSender(), getArgs()).execute();
		                break;
	            	case CLAIM:
	                    new ClaimAdminCommand(getKonquest(), getSender(), getArgs()).execute();
	                    break;
	            	case FLAG:
	                    new FlagAdminCommand(getKonquest(), getSender(), getArgs()).execute();
	                    break;
	                case HELP:
	                    new HelpAdminCommand(getKonquest(), getSender(), getArgs()).execute();
	                    break;
	                case KINGDOM:
		                new KingdomAdminCommand(getKonquest(), getSender(), getArgs()).execute();
		                break;
	                case LIST:
	                    new ListAdminCommand(getKonquest(), getSender(), getArgs()).execute();
	                    break;
	                case MONUMENT:
	                    new MonumentAdminCommand(getKonquest(), getSender(), getArgs()).execute();
	                    break;
	                case RUIN:
	                    new RuinAdminCommand(getKonquest(), getSender(), getArgs()).execute();
	                    break;
	                case SANCTUARY:
	                    new SanctuaryAdminCommand(getKonquest(), getSender(), getArgs()).execute();
	                    break;
	                case SAVE:
	                    new SaveAdminCommand(getKonquest(), getSender(), getArgs()).execute();
	                    break;
	                case SETTRAVEL:
	                    new SetTravelAdminCommand(getKonquest(), getSender(), getArgs()).execute();
	                    break;
	                case STAT:
	                    new StatAdminCommand(getKonquest(), getSender(), getArgs()).execute();
	                    break;
	                case TOWN:
		                new TownAdminCommand(getKonquest(), getSender(), getArgs()).execute();
		                break;
	                case TRAVEL:
	                    new TravelAdminCommand(getKonquest(), getSender(), getArgs()).execute();
	                    break;
	                case UNCLAIM:
	                    new UnclaimAdminCommand(getKonquest(), getSender(), getArgs()).execute();
	                    break;
	                case RELOAD:
	                    new ReloadAdminCommand(getKonquest(), getSender(), getArgs()).execute();
	                    break;
	                default:
	                	new KonquestCommand(getKonquest(), getSender()).execute();
	                	//ChatUtil.sendError((Player) getSender(), "Command does not exist");
	                	break;
	            }
        	} else {
        		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage()+" "+commandArg.permission());
        	}
        }
    }
    
    public List<String> tabComplete() {
    	List<String> tabList = new ArrayList<>();
        if (getArgs().length == 2) {
        	List<String> baseList = new ArrayList<>();
        	for(AdminCommandType cmd : AdminCommandType.values()) {
        		if(getSender().hasPermission(cmd.permission())) {
        			baseList.add(cmd.toString().toLowerCase());
        		}
    		}
        	// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[1], baseList, tabList);
			Collections.sort(tabList);
        } else if (getArgs().length >= 3) {
        	AdminCommandType commandArg = AdminCommandType.getCommand(getArgs()[1]);
        	if (getSender().hasPermission(commandArg.permission())) {
	            switch (commandArg) {
		            case BYPASS:
		                tabList.addAll(new BypassAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
		                break;
		            case CAMP:
		                tabList.addAll(new CampAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
		                break;
		            case CAPTURE:
		                tabList.addAll(new CaptureAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
		                break;
	            	case CLAIM:
	            		tabList.addAll(new ClaimAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                    break;
	            	case FLAG:
	            		tabList.addAll(new FlagAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                    break;
	                case HELP:
	                	tabList.addAll(new HelpAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                    break;
	                case KINGDOM:
		                tabList.addAll(new KingdomAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
		                break;
	                case LIST:
	                	tabList.addAll(new ListAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                    break;
	                case MONUMENT:
	                	tabList.addAll(new MonumentAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                    break;
	                case RUIN:
	                	tabList.addAll(new RuinAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                    break;
	                case SANCTUARY:
	                	tabList.addAll(new SanctuaryAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                    break;
	                case SAVE:
	                	tabList.addAll(new SaveAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                    break;
	                case SETTRAVEL:
	                	tabList.addAll(new SetTravelAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                    break;
	                case STAT:
	                	tabList.addAll(new StatAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                    break;
	                case TOWN:
		                tabList.addAll(new TownAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
		                break;
	                case TRAVEL:
	                	tabList.addAll(new TravelAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                    break;
	                case UNCLAIM:
	                	tabList.addAll(new UnclaimAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                    break;
	                case RELOAD:
	                	tabList.addAll(new ReloadAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                    break;
	                default:
	                	tabList.addAll(Collections.emptyList());
	                	break;
	            }
        	}
        }
        return tabList;
    }
}
