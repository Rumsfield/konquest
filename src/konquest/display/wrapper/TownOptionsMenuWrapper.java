package konquest.display.wrapper;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.display.MenuIcon;
import konquest.display.OptionIcon;
import konquest.manager.DisplayManager;
import konquest.display.OptionIcon.optionAction;
import konquest.model.KonPlayer;
import konquest.model.KonTown;
import konquest.utility.MessagePath;

public class TownOptionsMenuWrapper extends MenuWrapper {

	private KonTown town;
	
	public TownOptionsMenuWrapper(Konquest konquest, KonTown town) {
		super(konquest);
		this.town = town;
	}

	@Override
	public void constructMenu() {
		OptionIcon option;
		ArrayList<String> loreList;
		String currentValue;
		ChatColor titleColor = DisplayManager.titleColor;
		ChatColor loreColor = DisplayManager.loreColor;
		ChatColor valueColor = DisplayManager.valueColor;
		ChatColor hintColor = DisplayManager.hintColor;
		
		// Page 0
		String pageLabel = titleColor+town.getName()+" "+MessagePath.LABEL_OPTIONS.getMessage();
		getMenu().addPage(0, 1, pageLabel);
		
		// Open Info Icon
		currentValue = DisplayManager.boolean2Lang(town.isOpen())+" "+DisplayManager.boolean2Symbol(town.isOpen());
		loreList = new ArrayList<String>();
    	loreList.addAll(Konquest.stringPaginate(MessagePath.MENU_OPTIONS_OPEN.getMessage()));
    	loreList.add(loreColor+MessagePath.MENU_OPTIONS_CURRENT.getMessage(valueColor+currentValue));
    	loreList.add(hintColor+MessagePath.MENU_OPTIONS_HINT.getMessage());
		option = new OptionIcon(optionAction.TOWN_OPEN, loreColor+MessagePath.LABEL_OPEN.getMessage(), loreList, Material.DARK_OAK_DOOR, 3);
		getMenu().getPage(0).addIcon(option);
		
		// Open Info Icon
		currentValue = DisplayManager.boolean2Lang(town.isPlotOnly())+" "+DisplayManager.boolean2Symbol(town.isPlotOnly());
		loreList = new ArrayList<String>();
    	loreList.addAll(Konquest.stringPaginate(MessagePath.MENU_OPTIONS_PLOT.getMessage()));
    	loreList.add(loreColor+MessagePath.MENU_OPTIONS_CURRENT.getMessage(valueColor+currentValue));
    	loreList.add(hintColor+MessagePath.MENU_OPTIONS_HINT.getMessage());
		option = new OptionIcon(optionAction.TOWN_PLOT_ONLY, loreColor+MessagePath.LABEL_PLOT.getMessage(), loreList, Material.DIAMOND_SHOVEL, 4);
		getMenu().getPage(0).addIcon(option);
		
		// Redstone Info Icon
		currentValue = DisplayManager.boolean2Lang(town.isEnemyRedstoneAllowed())+" "+DisplayManager.boolean2Symbol(town.isEnemyRedstoneAllowed());
		loreList = new ArrayList<String>();
    	loreList.addAll(Konquest.stringPaginate(MessagePath.MENU_OPTIONS_REDSTONE.getMessage()));
    	loreList.add(loreColor+MessagePath.MENU_OPTIONS_CURRENT.getMessage(valueColor+currentValue));
    	loreList.add(hintColor+MessagePath.MENU_OPTIONS_HINT.getMessage());
		option = new OptionIcon(optionAction.TOWN_REDSTONE, loreColor+MessagePath.LABEL_ENEMY_REDSTONE.getMessage(), loreList, Material.LEVER, 5);
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
