package com.github.rumsfield.konquest.display.wrapper;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.display.icon.OptionIcon;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.model.KonTownOption;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
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

		// All town options
		int iconIndex = 1;
		for (KonTownOption townOption : KonTownOption.values()) {
			boolean isOptionEnabled = true;
			// Special option checks
			if (townOption.equals(KonTownOption.ALLIED_BUILDING)) {
				isOptionEnabled = getKonquest().getCore().getBoolean(CorePath.KINGDOMS_ALLY_BUILD.getPath(),false);
			}
			// Try to add icon
			if (isOptionEnabled) {
				boolean val = town.getTownOption(townOption);
				currentValue = DisplayManager.boolean2Lang(val) + " " + DisplayManager.boolean2Symbol(val);
				loreList = new ArrayList<>(Konquest.stringPaginate(townOption.getDescription()));
				loreList.add(loreColor + MessagePath.MENU_OPTIONS_CURRENT.getMessage(valueColor + currentValue));
				loreList.add(hintColor + MessagePath.MENU_OPTIONS_HINT.getMessage());
				option = new OptionIcon(townOption, loreColor + townOption.getName(), loreList, townOption.getDisplayMaterial(), iconIndex);
				getMenu().getPage(0).addIcon(option);
				iconIndex++;
			}
		}
		
		getMenu().refreshNavigationButtons();
		getMenu().setPageIndex(0);
	}

	@Override
	public void onIconClick(KonPlayer clickPlayer, MenuIcon clickedIcon) {
		Player bukkitPlayer = clickPlayer.getBukkitPlayer();
		if(clickedIcon instanceof OptionIcon) {
			// Option Icons close the GUI and attempt to change a town setting
			OptionIcon icon = (OptionIcon)clickedIcon;
			boolean status = getKonquest().getKingdomManager().changeTownOption(icon.getOption(), town, bukkitPlayer);
			if(status) {
				Konquest.playSuccessSound(bukkitPlayer);
			} else {
				Konquest.playFailSound(bukkitPlayer);
			}
		}
	}

}
