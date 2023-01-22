package com.github.rumsfield.konquest.display.wrapper;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandType;
import com.github.rumsfield.konquest.display.icon.CommandIcon;
import com.github.rumsfield.konquest.display.icon.InfoIcon;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class HelpMenuWrapper extends MenuWrapper {

	public HelpMenuWrapper(Konquest konquest) {
		super(konquest);
	}
	
	@Override
	public void constructMenu() {

		int itemIndex = 0;
		int cost;
		int cost_incr;

		double cost_spy = getKonquest().getCore().getDouble(CorePath.FAVOR_COST_SPY.getPath(),0.0);
		double cost_settle = getKonquest().getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SETTLE.getPath(),0.0);
		double cost_settle_incr = getKonquest().getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SETTLE_INCREMENT.getPath(),0.0);
		double cost_claim = getKonquest().getCore().getDouble(CorePath.FAVOR_COST_CLAIM.getPath(),0.0);
		double cost_travel = getKonquest().getCore().getDouble(CorePath.FAVOR_COST_TRAVEL.getPath(),0.0);
    	String communityLink = getKonquest().getCore().getString(CorePath.COMMUNITY_LINK.getPath(),"");

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
			CommandIcon icon = new CommandIcon(cmd, cost, cost_incr, itemIndex);
			getMenu().getPage(0).addIcon(icon);
			itemIndex++;
		}
		// Add info icons
		List<String> loreList = Collections.singletonList(MessagePath.MENU_HELP_COMMUNITY.getMessage());
		InfoIcon info = new InfoIcon(MessagePath.MENU_HELP_COMMUNITY.getMessage(), loreList, Material.MINECART, itemIndex, true);
		info.setInfo(ChatColor.GOLD+MessagePath.MENU_HELP_HINT.getMessage()+": "+ChatColor.DARK_PURPLE+ChatColor.UNDERLINE+communityLink);
		getMenu().getPage(0).addIcon(info);
		
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
			
			String cmdArgsFormatted = cmd.arguments()
					.replaceAll("<", ChatColor.GRAY+"<"+ChatColor.AQUA)
					.replaceAll(">", ChatColor.GRAY+">"+ChatColor.AQUA)
					.replaceAll("\\|", ChatColor.GRAY+"|"+ChatColor.AQUA)
					.replaceAll("]", ChatColor.GRAY+"]"+ChatColor.AQUA)
					.replaceAll("\\[", ChatColor.GRAY+"["+ChatColor.AQUA);
        	
			ChatUtil.sendNotice(bukkitPlayer, ChatColor.GOLD+"/k "+cmd.toString().toLowerCase()+" "+ChatColor.AQUA+cmdArgsFormatted);
		} else if(clickedIcon instanceof InfoIcon) {
			// Info Icons close the GUI and print their info in chat
			InfoIcon icon = (InfoIcon)clickedIcon;
			ChatUtil.sendNotice(bukkitPlayer, icon.getInfo());
		}
	}

}
