package com.github.rumsfield.konquest.display;

import com.github.rumsfield.konquest.display.icon.InfoIcon;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @implNote Pages must have a bottom row dedicated to navigation buttons
 */
public class PagedMenu {

	private final ArrayList<DisplayMenu> pages;
	private int currentPageIndex;
	
	public PagedMenu() {
		this.pages = new ArrayList<>();
		this.currentPageIndex = 0;
	}
	

	public void addPage(int index, int rows, String label) {
		if(index > pages.size()) {
			ChatUtil.printDebug("Failed to add page beyond list index");
			return;
		}
		if(rows > 5) {
			ChatUtil.printDebug("Failed to add page with too many rows");
			return;
		}
		pages.add(index, new DisplayMenu(rows+1, label));
	}
	
	public DisplayMenu getPage(int index) {
		if(index < 0 || index > pages.size()) {
			ChatUtil.printDebug("Failed to get page beyond list index");
			return null;
		}
		return pages.get(index);
	}
	
	public void setPageIndex(int index) {
		if(index < 0 || index > pages.size()) {
			ChatUtil.printDebug("Failed to set page beyond list index");
		} else {
			currentPageIndex = index;
		}
	}
	
	public int nextPageIndex() {
		int newIndex = currentPageIndex+1;
		if(newIndex < pages.size()) {
			currentPageIndex = newIndex;
		}
		return currentPageIndex;
	}
	
	public int previousPageIndex() {
		int newIndex = currentPageIndex-1;
		if(newIndex >= 0) {
			currentPageIndex = newIndex;
		}
		return currentPageIndex;
	}
	
	public int currentPageIndex() {
		return currentPageIndex;
	}
	
	public DisplayMenu getCurrentPage() {
		if(currentPageIndex < 0 || currentPageIndex > pages.size()) {
			ChatUtil.printDebug("Failed to get page beyond list index");
			return null;
		}
		return pages.get(currentPageIndex);
	}
	
	public DisplayMenu getPage(Inventory inv) {
		DisplayMenu result = null;
		for(DisplayMenu menu : pages) {
			if(menu.getInventory().equals(inv)) {
				result = menu;
			}
		}
		return result;
	}
	
	public void refreshCurrentPage() {
		pages.get(currentPageIndex).updateIcons();
	}
	
	public int getNextSlot(int page) {
		int maxSize = pages.get(page).getInventory().getSize();
		return maxSize-1;
	}
	
	public int getCurrentNextSlot() {
		int maxSize = pages.get(currentPageIndex).getInventory().getSize();
		return maxSize-1;
	}
	
	public int getCloseSlot(int page) {
		int maxSize = pages.get(page).getInventory().getSize();
		return maxSize-5;
	}
	
	public int getCurrentCloseSlot() {
		int maxSize = pages.get(currentPageIndex).getInventory().getSize();
		return maxSize-5;
	}
	
	public int getBackSlot(int page) {
		int maxSize = pages.get(page).getInventory().getSize();
		return maxSize-9;
	}
	
	public int getCurrentBackSlot() {
		int maxSize = pages.get(currentPageIndex).getInventory().getSize();
		return maxSize-9;
	}
	
	public void refreshNavigationButtons() {
		// Place a back button on pages > 0
		// Place a next button on pages < max
		// Place a close button on all pages
		for(int i=0;i<pages.size();i++) {
			int nextIndex = getNextSlot(i);
			int closeIndex = getCloseSlot(i);
			int backIndex = getBackSlot(i);
			if(i > 0) {
				// Place a back button
				pages.get(i).addIcon(new InfoIcon(ChatColor.GOLD+MessagePath.LABEL_BACK.getMessage(),Collections.emptyList(),Material.ENDER_PEARL,backIndex,true));
			} else {
				pages.get(i).addIcon(new InfoIcon(" ",Collections.emptyList(),Material.GRAY_STAINED_GLASS_PANE,backIndex,false));
			}
			if(i < pages.size()-1) {
				// Place a next button
				pages.get(i).addIcon(new InfoIcon(ChatColor.GOLD+MessagePath.LABEL_NEXT.getMessage(),Collections.emptyList(),Material.ENDER_PEARL,nextIndex,true));
			} else {
				pages.get(i).addIcon(new InfoIcon(" ",Collections.emptyList(),Material.GRAY_STAINED_GLASS_PANE,nextIndex,false));
			}
			// Place a close button
			pages.get(i).addIcon(new InfoIcon(ChatColor.GOLD+MessagePath.LABEL_CLOSE.getMessage(),Collections.emptyList(),Material.STRUCTURE_VOID,closeIndex,true));
			// Place glass panes
			pages.get(i).addIcon(new InfoIcon(" ",Collections.emptyList(),Material.GRAY_STAINED_GLASS_PANE,backIndex+1,false));
			pages.get(i).addIcon(new InfoIcon(" ",Collections.emptyList(),Material.GRAY_STAINED_GLASS_PANE,backIndex+2,false));
			pages.get(i).addIcon(new InfoIcon(" ",Collections.emptyList(),Material.GRAY_STAINED_GLASS_PANE,backIndex+3,false));
			pages.get(i).addIcon(new InfoIcon(" ",Collections.emptyList(),Material.GRAY_STAINED_GLASS_PANE,nextIndex-3,false));
			pages.get(i).addIcon(new InfoIcon(" ",Collections.emptyList(),Material.GRAY_STAINED_GLASS_PANE,nextIndex-2,false));
			pages.get(i).addIcon(new InfoIcon(" ",Collections.emptyList(),Material.GRAY_STAINED_GLASS_PANE,nextIndex-1,false));
			// Set all items
			pages.get(i).updateIcons();
		}
	}
	
}
