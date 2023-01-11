package com.github.rumsfield.konquest.display.wrapper;

import org.bukkit.inventory.Inventory;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.display.DisplayMenu;
import com.github.rumsfield.konquest.display.PagedMenu;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.model.KonPlayer;
import org.jetbrains.annotations.Nullable;

public abstract class MenuWrapper {
	
	private final PagedMenu menu;
	private final Konquest konquest;
	
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

	@Nullable
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
