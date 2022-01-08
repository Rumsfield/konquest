package konquest.display.wrapper;

import org.bukkit.inventory.Inventory;

import konquest.Konquest;
import konquest.display.DisplayMenu;
import konquest.display.MenuIcon;
import konquest.display.PagedMenu;
import konquest.model.KonPlayer;

public abstract class MenuWrapper {
	
	private PagedMenu menu;
	private Konquest konquest;
	
	public MenuWrapper(Konquest konquest) {
		this.konquest = konquest;
		menu = new PagedMenu();
	}
	
	public Konquest getKonquest() {
		return konquest;
	}
	
	public PagedMenu getMenu() {
		return menu;
	}
	
	// This can return null!
	public Inventory getCurrentInventory() {
		DisplayMenu currentPage = menu.getCurrentPage();
		if(currentPage != null) {
			return currentPage.getInventory();
		} else {
			return null;
		}
	}
	
	public abstract void constructMenu();
	
	// Return true to close the menu, false to do nothing
	public abstract boolean onIconClick(KonPlayer clickPlayer, MenuIcon clickedIcon);
	
}
