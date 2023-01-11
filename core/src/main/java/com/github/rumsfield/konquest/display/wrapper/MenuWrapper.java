package com.github.rumsfield.konquest.display.wrapper;

import org.bukkit.inventory.Inventory;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.display.DisplayMenu;
import com.github.rumsfield.konquest.display.PagedMenu;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.model.KonPlayer;

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
	
	public abstract void onIconClick(KonPlayer clickPlayer, MenuIcon clickedIcon);
	
}
