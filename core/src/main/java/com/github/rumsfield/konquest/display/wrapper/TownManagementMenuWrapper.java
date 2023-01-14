package com.github.rumsfield.konquest.display.wrapper;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.display.icon.InfoIcon;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TownManagementMenuWrapper extends MenuWrapper {

	private final KonTown manageTown;
	private final KonPlayer observer;
	private final Boolean isAdmin;
	
	public TownManagementMenuWrapper(Konquest konquest, KonTown manageTown, KonPlayer observer, boolean isAdmin) {
		super(konquest);
		this.manageTown = manageTown;
		this.observer = observer;
		this.isAdmin = isAdmin;
	}

	@Override
	public void constructMenu() {
		String pageLabel;
 		List<String> loreList;
 		InfoIcon info;

 		ChatColor titleColor = DisplayManager.titleColor;
		ChatColor loreColor = DisplayManager.loreColor;
		ChatColor hintColor = DisplayManager.hintColor;
		
		boolean isLord = false;
		boolean isKnight = false;
		String townName = "null";
		
		if(manageTown != null) {
			isLord = manageTown.isPlayerLord(observer.getOfflineBukkitPlayer());
			isKnight = manageTown.isPlayerKnight(observer.getOfflineBukkitPlayer());
			townName = manageTown.getName();
		}
		
		//TODO: KR paths
		pageLabel = titleColor+townName+" Town Management";
		getMenu().addPage(0, 1, pageLabel);
		
		/* Shields Info Icon (1) */
		if(isAdmin || isLord || isKnight) {
			// Verify either shields or armors are enabled
	    	boolean isShieldsEnabled = getKonquest().getShieldManager().isShieldsEnabled();
	    	boolean isArmorsEnabled = getKonquest().getShieldManager().isArmorsEnabled();
	    	if(isShieldsEnabled || isArmorsEnabled) {
	    		loreList = new ArrayList<>();
		    	loreList.add(loreColor+"Apply town shields and armor.");
		    	loreList.add(hintColor+"Click to manage");
		    	info = new InfoIcon("Shields", loreList, Material.SHIELD, 1, true);
		    	info.setInfo("shields");
		    	getMenu().getPage(0).addIcon(info);
	    	}
		}
    	/* Plots Info Icon (3) */
		if(isAdmin || isLord || isKnight) {
			// Verify plots are enabled
	    	boolean isPlotsEnabled = getKonquest().getPlotManager().isEnabled();
	    	if(isPlotsEnabled) {
	    		loreList = new ArrayList<>();
		    	loreList.add(loreColor+"Edit town plots.");
		    	loreList.add(hintColor+"Click to manage");
		    	info = new InfoIcon("Plots", loreList, Material.GRASS, 3, true);
		    	info.setInfo("plots");
		    	getMenu().getPage(0).addIcon(info);
	    	}
		}
    	/* Upgrades Info Icon (5) */
		if(isAdmin || isLord) {
			loreList = new ArrayList<>();
	    	loreList.add(loreColor+"Apply town upgrades.");
	    	loreList.add(hintColor+"Click to manage");
	    	info = new InfoIcon("Upgrades", loreList, Material.GOLDEN_APPLE, 5, true);
	    	info.setInfo("upgrades");
	    	getMenu().getPage(0).addIcon(info);
		}
    	/* Upgrades Info Icon (7) */
		if(isAdmin || isLord) {
			loreList = new ArrayList<>();
	    	loreList.add(loreColor+"Edit town options.");
	    	loreList.add(hintColor+"Click to manage");
	    	info = new InfoIcon("Options", loreList, Material.WRITABLE_BOOK, 7, true);
	    	info.setInfo("options");
	    	getMenu().getPage(0).addIcon(info);
		}
		
		getMenu().refreshNavigationButtons();
		getMenu().setPageIndex(0);
	}

	@Override
	public void onIconClick(KonPlayer clickPlayer, MenuIcon clickedIcon) {
		Player bukkitPlayer = clickPlayer.getBukkitPlayer();
		if(manageTown == null) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage("Null Town"));
			return;
		}
		if(clickedIcon instanceof InfoIcon) {
			// Info Icons close the GUI and open their respective menus.
			InfoIcon icon = (InfoIcon)clickedIcon;
			String info = icon.getInfo();
			switch (info) {
				case "shields":
					// Open shields menu
					getKonquest().getDisplayManager().displayTownShieldMenu(bukkitPlayer, manageTown);
					break;
				case "plots":
					// Open plot menu
					getKonquest().getDisplayManager().displayPlotMenu(bukkitPlayer, manageTown);
					break;
				case "upgrades":
					// Open upgrade menu
					getKonquest().getDisplayManager().displayTownUpgradeMenu(bukkitPlayer, manageTown);
					break;
				case "options":
					// Open options menu
					getKonquest().getDisplayManager().displayTownOptionsMenu(bukkitPlayer, manageTown);
					break;
				default:
					// Unknown icon info
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage("Unknown Selection"));
					break;
			}
		}
	}
	
	
}
