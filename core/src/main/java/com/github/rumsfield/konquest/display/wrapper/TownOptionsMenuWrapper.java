package com.github.rumsfield.konquest.display.wrapper;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.display.icon.OptionIcon;
import com.github.rumsfield.konquest.display.icon.OptionIcon.optionAction;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class TownOptionsMenuWrapper extends MenuWrapper {

	private final KonTown town;
	
	public TownOptionsMenuWrapper(Konquest konquest, KonTown town) {
		super(konquest);
		this.town = town;
	}

	@Override
	public void constructMenu() {
		OptionIcon option;
		ArrayList<String> loreList;
		String currentValue;
		String titleColor = DisplayManager.titleFormat;
		String loreColor = DisplayManager.loreFormat;
		String valueColor = DisplayManager.valueFormat;
		String hintColor = DisplayManager.hintFormat;
		
		// Page 0
		String pageLabel = titleColor+town.getName()+" "+MessagePath.LABEL_OPTIONS.getMessage();
		getMenu().addPage(0, 1, pageLabel);
		
		// Open Info Icon
		currentValue = DisplayManager.boolean2Lang(town.isOpen())+" "+DisplayManager.boolean2Symbol(town.isOpen());
		loreList = new ArrayList<>(Konquest.stringPaginate(MessagePath.MENU_OPTIONS_OPEN.getMessage()));
    	loreList.add(loreColor+MessagePath.MENU_OPTIONS_CURRENT.getMessage(valueColor+currentValue));
    	loreList.add(hintColor+MessagePath.MENU_OPTIONS_HINT.getMessage());
		option = new OptionIcon(optionAction.TOWN_OPEN, loreColor+MessagePath.LABEL_OPEN.getMessage(), loreList, Material.DARK_OAK_DOOR, 2);
		getMenu().getPage(0).addIcon(option);
		
		// Plot Only Info Icon
		currentValue = DisplayManager.boolean2Lang(town.isPlotOnly())+" "+DisplayManager.boolean2Symbol(town.isPlotOnly());
		loreList = new ArrayList<>(Konquest.stringPaginate(MessagePath.MENU_OPTIONS_PLOT.getMessage()));
    	loreList.add(loreColor+MessagePath.MENU_OPTIONS_CURRENT.getMessage(valueColor+currentValue));
    	loreList.add(hintColor+MessagePath.MENU_OPTIONS_HINT.getMessage());
		option = new OptionIcon(optionAction.TOWN_PLOT_ONLY, loreColor+MessagePath.LABEL_PLOT.getMessage(), loreList, Material.DIAMOND_SHOVEL, 3);
		getMenu().getPage(0).addIcon(option);

		// Friendly Redstone Info Icon
		currentValue = DisplayManager.boolean2Lang(town.isFriendlyRedstoneAllowed())+" "+DisplayManager.boolean2Symbol(town.isFriendlyRedstoneAllowed());
		loreList = new ArrayList<>(Konquest.stringPaginate(MessagePath.MENU_OPTIONS_FRIENDLY_REDSTONE.getMessage()));
		loreList.add(loreColor+MessagePath.MENU_OPTIONS_CURRENT.getMessage(valueColor+currentValue));
		loreList.add(hintColor+MessagePath.MENU_OPTIONS_HINT.getMessage());
		option = new OptionIcon(OptionIcon.optionAction.TOWN_FRIENDLY_REDSTONE, loreColor+MessagePath.LABEL_FRIENDLY_REDSTONE.getMessage(), loreList, Material.LEVER, 4);
		getMenu().getPage(0).addIcon(option);

		// Redstone Info Icon
		currentValue = DisplayManager.boolean2Lang(town.isEnemyRedstoneAllowed())+" "+DisplayManager.boolean2Symbol(town.isEnemyRedstoneAllowed());
		loreList = new ArrayList<>(Konquest.stringPaginate(MessagePath.MENU_OPTIONS_REDSTONE.getMessage()));
    	loreList.add(loreColor+MessagePath.MENU_OPTIONS_CURRENT.getMessage(valueColor+currentValue));
    	loreList.add(hintColor+MessagePath.MENU_OPTIONS_HINT.getMessage());
		option = new OptionIcon(optionAction.TOWN_REDSTONE, loreColor+MessagePath.LABEL_ENEMY_REDSTONE.getMessage(), loreList, Material.REDSTONE, 5);
		getMenu().getPage(0).addIcon(option);
		
		// Golem Offensive Info Icon
		currentValue = DisplayManager.boolean2Lang(town.isGolemOffensive())+" "+DisplayManager.boolean2Symbol(town.isGolemOffensive());
		loreList = new ArrayList<>(Konquest.stringPaginate(MessagePath.MENU_OPTIONS_GOLEM.getMessage()));
    	loreList.add(loreColor+MessagePath.MENU_OPTIONS_CURRENT.getMessage(valueColor+currentValue));
    	loreList.add(hintColor+MessagePath.MENU_OPTIONS_HINT.getMessage());
		option = new OptionIcon(optionAction.TOWN_GOLEM, loreColor+MessagePath.LABEL_GOLEM_OFFENSE.getMessage(), loreList, Material.IRON_SWORD, 6);
		getMenu().getPage(0).addIcon(option);
		
		getMenu().refreshNavigationButtons();
		getMenu().setPageIndex(0);
	}

	@Override
	public void onIconClick(KonPlayer clickPlayer, MenuIcon clickedIcon) {
		Player bukkitPlayer = clickPlayer.getBukkitPlayer();
		if(clickedIcon instanceof OptionIcon) {
			// Option Icons close the GUI and attempt to change a town setting
			OptionIcon icon = (OptionIcon)clickedIcon;
			boolean status = getKonquest().getKingdomManager().changeTownOption(icon.getAction(), town, bukkitPlayer);
			if(status) {
				Konquest.playSuccessSound(bukkitPlayer);
			} else {
				Konquest.playFailSound(bukkitPlayer);
			}
		}
	}

}
