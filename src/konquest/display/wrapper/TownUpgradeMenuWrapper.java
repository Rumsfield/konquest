package konquest.display.wrapper;

import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.display.MenuIcon;
import konquest.display.UpgradeIcon;
import konquest.manager.DisplayManager;
import konquest.model.KonPlayer;
import konquest.model.KonTown;
import konquest.model.KonUpgrade;
import konquest.utility.MessagePath;

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
		HashMap<KonUpgrade,Integer> availableUpgrades = getKonquest().getUpgradeManager().getAvailableUpgrades(town);
		int index = 0;
		for(KonUpgrade upgrade : KonUpgrade.values()) {
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
	public boolean onIconClick(KonPlayer clickPlayer, MenuIcon clickedIcon) {
		boolean result = false;
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
			result = true;
		}
		return result;
	}

}