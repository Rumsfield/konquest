package com.github.rumsfield.konquest.display;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.display.icon.PlayerIcon;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.utility.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A wrapper for a single Inventory view that contains icons.
 * This class is used in higher level menus to represent individual pages.
 */
public class DisplayView {

	private final Inventory inventory;
	private final HashMap<Integer,MenuIcon> iconMap;
	private final Runnable updateTask;
	private BukkitTask executionTask;
	private int refreshTickRate;

	
	public DisplayView(int rows, String label) {
		if(rows > 5) {
			ChatUtil.printConsoleError("Failed to create menu display with "+rows+" rows: "+label);
		}
		int invRows = Math.min(rows,5)+1; // add row for navigation
		this.inventory = Bukkit.createInventory(null, invRows*9, DisplayManager.titleFormat+label);
		this.iconMap = new HashMap<>();
		this.refreshTickRate = 0;
		this.executionTask = null;

		this.updateTask = () -> {
			// Set non-player heads first
			ArrayList<MenuIcon> heads = new ArrayList<>();
			for(MenuIcon icon : iconMap.values()) {
				if(icon instanceof PlayerIcon) {
					heads.add(icon);
				} else if (icon.getIndex() < inventory.getSize()) {
					inventory.setItem(icon.getIndex(), icon.getItem());
				}
			}
			// Set player heads last
			for(MenuIcon icon : heads) {
				if (icon.getIndex() < inventory.getSize()) {
					inventory.setItem(icon.getIndex(), icon.getItem());
				}
			}
			//ChatUtil.printDebug("Updated "+iconMap.size()+" icons in view "+label);
		};

	}

	public void setRefreshTickRate(int ticks) {
		refreshTickRate = ticks;
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
		if (refreshTickRate > 0) {
			executionTask = Bukkit.getScheduler().runTaskTimer(Konquest.getInstance().getPlugin(), updateTask, 0L, refreshTickRate);
		} else {
			executionTask = Bukkit.getScheduler().runTaskAsynchronously(Konquest.getInstance().getPlugin(), updateTask);
		}
	}

	public void clearUpdates() {
		if (executionTask != null) {
			executionTask.cancel();
		}
	}

}
