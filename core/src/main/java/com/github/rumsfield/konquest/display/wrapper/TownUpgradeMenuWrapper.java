package com.github.rumsfield.konquest.display.wrapper;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestUpgradable;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.display.icon.UpgradeIcon;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.model.KonquestUpgrade;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class TownUpgradeMenuWrapper extends MenuWrapper {

	private KonTown town;
	
	public TownUpgradeMenuWrapper(Konquest konquest, KonTown town) {
		super(konquest);
		this.town = town;
	}

	@Override
	public void constructMenu() {
		ChatColor titleColor = DisplayManager.titleColor;
		// Page 0
		String pageLabel = titleColor+MessagePath.MENU_UPGRADE_TITLE.getMessage();
		getMenu().addPage(0, 1, pageLabel);
		HashMap<KonquestUpgradable,Integer> availableUpgrades = getKonquest().getUpgradeManager().getAvailableUpgrades(town);
		int index = 0;
		for(KonquestUpgrade upgrade : KonquestUpgrade.values()) {
			if(availableUpgrades.containsKey(upgrade)) {
				int cost = getKonquest().getUpgradeManager().getUpgradeCost(upgrade, availableUpgrades.get(upgrade));
				int pop = getKonquest().getUpgradeManager().getUpgradePopulation(upgrade, availableUpgrades.get(upgrade));
				UpgradeIcon icon = new UpgradeIcon(upgrade, availableUpgrades.get(upgrade), index, cost, pop);
				getMenu().getPage(0).addIcon(icon);
				index++;
			}
		}
		getMenu().refreshNavigationButtons();
		getMenu().setPageIndex(0);
	}

	@Override
	public void onIconClick(KonPlayer clickPlayer, MenuIcon clickedIcon) {
		Player bukkitPlayer = clickPlayer.getBukkitPlayer();
		if(clickedIcon instanceof UpgradeIcon) {
			// Upgrade Icons close the GUI and attempt to apply an upgrade
			UpgradeIcon icon = (UpgradeIcon)clickedIcon;
			boolean status = getKonquest().getUpgradeManager().addTownUpgrade(town, icon.getUpgrade(), icon.getLevel(), bukkitPlayer);
			if(status) {
				Konquest.playSuccessSound(bukkitPlayer);
			} else {
				Konquest.playFailSound(bukkitPlayer);
			}
		}
	}

}
