package com.github.rumsfield.konquest.display;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.display.icon.PlayerIcon;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.utility.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.HashSet;

/**
 * A wrapper for a single Inventory view that contains icons.
 * This class is used in higher level menus to represent individual pages.
 */
public class DisplayMenu {

	private final Inventory inventory;
	private final HashMap<Integer,MenuIcon> iconMap;
	
	public DisplayMenu (int rows, String label) {
		if(rows > 5) {
			ChatUtil.printConsoleError("Failed to create menu display with "+rows+" rows: "+label);
		}
		int invRows = Math.min(rows,5)+1; // add row for navigation
		inventory = Bukkit.createInventory(null, invRows*9, DisplayManager.titleFormat+label);
		iconMap = new HashMap<>();
	}
	
	public Inventory getInventory() {
        return inventory;
    }
	
	public void addIcon(MenuIcon icon) {
		iconMap.put(icon.getIndex(), icon);
	}
	
	public MenuIcon getIcon(int index) {
		return iconMap.get(index);
	}
	
	public void updateIcons() {
		Bukkit.getScheduler().runTaskAsynchronously(Konquest.getInstance().getPlugin(), () -> {
			// Set non-player heads first
			HashSet<MenuIcon> heads = new HashSet<>();
			for(MenuIcon icon : iconMap.values()) {
				if(icon instanceof PlayerIcon) {
					heads.add(icon);
				} else {
					inventory.setItem(icon.getIndex(), icon.getItem());
				}
			}
			// Set player heads last
			for(MenuIcon icon : heads) {
				inventory.setItem(icon.getIndex(), icon.getItem());
			}
		});
	}

}
