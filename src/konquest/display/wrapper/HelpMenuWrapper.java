package konquest.display.wrapper;

import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.command.CommandType;
import konquest.display.CommandIcon;
import konquest.display.InfoIcon;
import konquest.display.MenuIcon;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class HelpMenuWrapper extends MenuWrapper {

	public HelpMenuWrapper(Konquest konquest) {
		super(konquest);
	}
	
	@Override
	public void constructMenu() {

		int i = 0;
		int cost = 0;
		int cost_incr = 0;
		double cost_spy = getKonquest().getConfigManager().getConfig("core").getDouble("core.favor.cost_spy",0.0);
    	double cost_settle = getKonquest().getConfigManager().getConfig("core").getDouble("core.favor.cost_settle",0.0);
    	double cost_settle_incr = getKonquest().getConfigManager().getConfig("core").getDouble("core.favor.cost_settle_increment",0.0);
    	double cost_claim = getKonquest().getConfigManager().getConfig("core").getDouble("core.favor.cost_claim",0.0);
    	double cost_travel = getKonquest().getConfigManager().getConfig("core").getDouble("core.favor.cost_travel",0.0);
    	String communityLink = getKonquest().getConfigManager().getConfig("core").getString("core.community_link","");

		// Page 0
    	getMenu().addPage(0, (int)Math.ceil(((double)(CommandType.values().length+1))/9), ChatColor.BLACK+MessagePath.MENU_HELP_TITLE.getMessage());
    	// Add command icons
		for(CommandType cmd : CommandType.values()) {
			switch (cmd) {
				case SPY:
					cost = (int)cost_spy;
					cost_incr = 0;
					break;
				case SETTLE:
					cost = (int)cost_settle;
					cost_incr = (int)cost_settle_incr;
					break;
				case CLAIM:
					cost = (int)cost_claim;
					cost_incr = 0;
					break;
				case TRAVEL:
					cost = (int)cost_travel;
					cost_incr = 0;
					break;
				default:
					cost = 0;
					cost_incr = 0;
					break;
			} 
			CommandIcon icon = new CommandIcon(cmd, cost, cost_incr, i);
			getMenu().getPage(0).addIcon(icon);
			i++;
		}
		// Add info icons
		List<String> loreList = Arrays.asList(MessagePath.MENU_HELP_COMMUNITY.getMessage());
		InfoIcon info = new InfoIcon(MessagePath.MENU_HELP_COMMUNITY.getMessage(), loreList, Material.MINECART, i, true);
		if(communityLink == null) {
			communityLink = "";
		}
		info.setInfo(ChatColor.GOLD+MessagePath.MENU_HELP_HINT.getMessage()+": "+ChatColor.DARK_PURPLE+ChatColor.UNDERLINE+communityLink);
		getMenu().getPage(0).addIcon(info);
		i++;
		
		getMenu().refreshNavigationButtons();
		getMenu().setPageIndex(0);
	}

	@Override
	public void onIconClick(KonPlayer clickPlayer, MenuIcon clickedIcon) {
		Player bukkitPlayer = clickPlayer.getBukkitPlayer();
		if(clickedIcon instanceof CommandIcon) {
			// Command Icons close the GUI and print a command in chat
			CommandIcon icon = (CommandIcon)clickedIcon;
			CommandType cmd = icon.getCommand();
			
			String cmdArgsFormatted = cmd.arguments();
        	cmdArgsFormatted = cmdArgsFormatted.replaceAll("<", ChatColor.GRAY+"<"+ChatColor.AQUA);
        	cmdArgsFormatted = cmdArgsFormatted.replaceAll(">", ChatColor.GRAY+">"+ChatColor.AQUA);
        	cmdArgsFormatted = cmdArgsFormatted.replaceAll("\\|", ChatColor.GRAY+"|"+ChatColor.AQUA);
        	cmdArgsFormatted = cmdArgsFormatted.replaceAll("\\]", ChatColor.GRAY+"]"+ChatColor.AQUA);
        	cmdArgsFormatted = cmdArgsFormatted.replaceAll("\\[", ChatColor.GRAY+"["+ChatColor.AQUA);
        	
			ChatUtil.sendNotice(bukkitPlayer, ChatColor.GOLD+"/k "+cmd.toString().toLowerCase()+" "+ChatColor.AQUA+cmdArgsFormatted);
		} else if(clickedIcon instanceof InfoIcon) {
			// Info Icons close the GUI and print their info in chat
			InfoIcon icon = (InfoIcon)clickedIcon;
			ChatUtil.sendNotice(bukkitPlayer, icon.getInfo());
		}
	}

}
