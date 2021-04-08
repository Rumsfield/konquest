package konquest.command.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.command.KonquestCommand;
import konquest.utility.ChatUtil;

public class AdminCommand extends CommandBase {

    public AdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
        if (getArgs().length == 1) {
        	ChatUtil.sendNotice((Player) getSender(), "Try \"/k admin help\" for a list of available commands");
        } else if (getArgs().length >= 2) {
        	AdminCommandType commandArg = AdminCommandType.getCommand(getArgs()[1]);
            switch (commandArg) {
	            case BYPASS:
	                new BypassAdminCommand(getKonquest(), getSender(), getArgs()).execute();
	                break;
            	case CLAIM:
                    new ClaimAdminCommand(getKonquest(), getSender(), getArgs()).execute();
                    break;
            	case FLAG:
                    new FlagAdminCommand(getKonquest(), getSender(), getArgs()).execute();
                    break;
                case FORCEEXILE:
                    new ForceExileAdminCommand(getKonquest(), getSender(), getArgs()).execute();
                    break;
                case FORCEJOIN:
                    new ForceJoinAdminCommand(getKonquest(), getSender(), getArgs()).execute();
                    break;
                case FORCETOWN:
                    new ForceTownAdminCommand(getKonquest(), getSender(), getArgs()).execute();
                    break;
                case HELP:
                    new HelpAdminCommand(getKonquest(), getSender(), getArgs()).execute();
                    break;
                case LIST:
                    new ListAdminCommand(getKonquest(), getSender(), getArgs()).execute();
                    break;
                case MAKEKINGDOM:
                    new MakeKingdomAdminCommand(getKonquest(), getSender(), getArgs()).execute();
                    break;
                case MAKETOWN:
                    new MakeTownAdminCommand(getKonquest(), getSender(), getArgs()).execute();
                    break;
                case MONUMENT:
                    new MonumentAdminCommand(getKonquest(), getSender(), getArgs()).execute();
                    break;
                case REMOVEKINGDOM:
                    new RemoveKingdomAdminCommand(getKonquest(), getSender(), getArgs()).execute();
                    break;
                case REMOVETOWN:
                    new RemoveTownAdminCommand(getKonquest(), getSender(), getArgs()).execute();
                    break;
                case RENAME:
                    new RenameAdminCommand(getKonquest(), getSender(), getArgs()).execute();
                    break;
                case RUIN:
                    new RuinAdminCommand(getKonquest(), getSender(), getArgs()).execute();
                    break;
                case SAVE:
                    new SaveAdminCommand(getKonquest(), getSender(), getArgs()).execute();
                    break;
                case SETTRAVEL:
                    new SetTravelAdminCommand(getKonquest(), getSender(), getArgs()).execute();
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
                	ChatUtil.sendError((Player) getSender(), "Command does not exist");
                	break;
            }
        }
    }
    
    public List<String> tabComplete() {
    	List<String> tabList = new ArrayList<>();
    	//ChatUtil.printDebug("Entering Admin Command tabComplete, length: "+getArgs().length+", args: "+String.join(",",getArgs()));
        if (getArgs().length == 2) {
        	List<String> baseList = new ArrayList<>();
        	for(AdminCommandType cmd : AdminCommandType.values()) {
        		baseList.add(cmd.toString().toLowerCase());
    		}
        	// Trim down completion options based on current input
			StringUtil.copyPartialMatches(getArgs()[1], baseList, tabList);
			Collections.sort(tabList);
        } else if (getArgs().length >= 3) {
        	AdminCommandType commandArg = AdminCommandType.getCommand(getArgs()[1]);
        	//ChatUtil.printDebug("Arg is "+getArgs()[1]+", Command type is "+commandArg.toString());
            switch (commandArg) {
	            case BYPASS:
	                tabList.addAll(new BypassAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                //ChatUtil.printDebug("Tab Complete for BypassAdminCommand: "+getArgs().length);
	                break;
            	case CLAIM:
            		tabList.addAll(new ClaimAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                //ChatUtil.printDebug("Tab Complete for ClaimAdminCommand: "+getArgs().length);
                    break;
            	case FLAG:
            		tabList.addAll(new FlagAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                //ChatUtil.printDebug("Tab Complete for FlagAdminCommand: "+getArgs().length);
                    break;
                case FORCEEXILE:
                	tabList.addAll(new ForceExileAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                //ChatUtil.printDebug("Tab Complete for ForceExileAdminCommand: "+getArgs().length);
                    break;
                case FORCEJOIN:
                	tabList.addAll(new ForceJoinAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                //ChatUtil.printDebug("Tab Complete for ForceJoinAdminCommand: "+getArgs().length);
                    break;
                case FORCETOWN:
                	tabList.addAll(new ForceTownAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                //ChatUtil.printDebug("Tab Complete for ForceJoinAdminCommand: "+getArgs().length);
                    break;
                case HELP:
                	tabList.addAll(new HelpAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                //ChatUtil.printDebug("Tab Complete for HelpAdminCommand: "+getArgs().length);
                    break;
                case LIST:
                	tabList.addAll(new ListAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                //ChatUtil.printDebug("Tab Complete for ListAdminCommand: "+getArgs().length);
                    break;
                case MAKEKINGDOM:
                	tabList.addAll(new MakeKingdomAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                //ChatUtil.printDebug("Tab Complete for MakeKingdomAdminCommand: "+getArgs().length);
                    break;
                case MAKETOWN:
                	tabList.addAll(new MakeTownAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                //ChatUtil.printDebug("Tab Complete for MakeTownAdminCommand: "+getArgs().length);
                    break;
                case MONUMENT:
                	tabList.addAll(new MonumentAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                //ChatUtil.printDebug("Tab Complete for MonumentAdminCommand: "+getArgs().length);
                    break;
                case REMOVEKINGDOM:
                	tabList.addAll(new RemoveKingdomAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                //ChatUtil.printDebug("Tab Complete for RemoveKingdomAdminCommand: "+getArgs().length);
                    break;
                case REMOVETOWN:
                	tabList.addAll(new RemoveTownAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                //ChatUtil.printDebug("Tab Complete for RemoveTownAdminCommand: "+getArgs().length);
                    break;
                case RENAME:
                	tabList.addAll(new RenameAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                //ChatUtil.printDebug("Tab Complete for RenameAdminCommand: "+getArgs().length);
                    break;
                case RUIN:
                	tabList.addAll(new RuinAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
                    break;
                case SAVE:
                	tabList.addAll(new SaveAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                //ChatUtil.printDebug("Tab Complete for SaveAdminCommand: "+getArgs().length);
                    break;
                case SETTRAVEL:
                	tabList.addAll(new SetTravelAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                //ChatUtil.printDebug("Tab Complete for SetTravelAdminCommand: "+getArgs().length);
                    break;
                case TRAVEL:
                	tabList.addAll(new TravelAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                //ChatUtil.printDebug("Tab Complete for TravelAdminCommand: "+getArgs().length);
                    break;
                case UNCLAIM:
                	tabList.addAll(new UnclaimAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
	                //ChatUtil.printDebug("Tab Complete for UnclaimAdminCommand: "+getArgs().length);
                    break;
                case RELOAD:
                	tabList.addAll(new ReloadAdminCommand(getKonquest(), getSender(), getArgs()).tabComplete());
                    break;
                default:
                	tabList.addAll(Collections.emptyList());
                	break;
            }
        }
        //ChatUtil.printDebug("Exiting Admin Command tabComplete");
        return tabList;
    }
}
