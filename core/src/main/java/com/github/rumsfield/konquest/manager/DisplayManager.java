package com.github.rumsfield.konquest.manager;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.display.*;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.display.menu.HelpMenu;
import com.github.rumsfield.konquest.display.menu.KingdomMenu;
import com.github.rumsfield.konquest.display.menu.MainMenu;
import com.github.rumsfield.konquest.display.wrapper.*;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class DisplayManager {

	private final Konquest konquest;
	private final HashMap<Inventory, StateMenu> stateMenus;
	
	public static String titleFormat 		= ""+ChatColor.BLACK;
	public static String loreFormat 		= ""+ChatColor.YELLOW;
	public static String valueFormat 		= ""+ChatColor.AQUA;
	public static String hintFormat 		= ""+ChatColor.GOLD+ChatColor.UNDERLINE+"\u21D2"; // ⇒
	public static String propertyFormat 	= ""+ChatColor.LIGHT_PURPLE+ChatColor.ITALIC+"\u25C6"; // ◆
	public static String alertFormat 		= ""+ChatColor.RED+ChatColor.ITALIC+"\u26A0"; // ⚠
	
	public DisplayManager(Konquest konquest) {
		this.konquest = konquest;
		this.stateMenus = new HashMap<>();
	}
	
	public void initialize() {
		ChatUtil.printDebug("Display Manager is ready");
	}
	
	/*
	 * Common menu methods
	 */
	
	public boolean isNotDisplayMenu(@Nullable Inventory inv) {
		if(inv == null) return true;
		return !stateMenus.containsKey(inv);
	}
	
	public void onDisplayMenuClick(KonPlayer clickPlayer, Inventory inv, int slot, boolean clickType) {
		if(inv == null) return;
		Player bukkitPlayer = clickPlayer.getBukkitPlayer();
		try {
			if(stateMenus.containsKey(inv)) {
				// Handle menu navigation and states
				// Every clickable icon in a menu view will update the state and refresh the open inventory
				StateMenu clickMenu = stateMenus.get(inv);
				DisplayMenu currentView = clickMenu.getCurrentView();
				if(currentView == null || !currentView.getInventory().equals(inv)) {
					ChatUtil.printDebug("State menu view is not current!");
					return;
				}
				MenuIcon clickedIcon = currentView.getIcon(slot);
				if(clickedIcon == null || !clickedIcon.isClickable()) {
					return;
				}
				playMenuClickSound(bukkitPlayer);
				// Update menu state
				DisplayMenu updateView = clickMenu.updateState(slot, clickType);
				// Update inventory view
				stateMenus.remove(inv);
				if(updateView != null) {
					// Refresh displayed inventory view
					bukkitPlayer.openInventory(updateView.getInventory());
	            	stateMenus.put(updateView.getInventory(), clickMenu);
				} else {
					// Close inventory view
					bukkitPlayer.closeInventory();
				}
			}
		} catch(Exception | Error e) {
			// Close inventory view
			bukkitPlayer.closeInventory();
        	// Display exception
        	ChatUtil.printConsoleError("Failed to handle menu click, report this as a bug to the plugin author!");
        	e.printStackTrace();
		}
	}
	
	public void onDisplayMenuClose(Inventory inv, HumanEntity owner) {
		stateMenus.remove(inv);
	}

	public void displayMenuToPlayer(KonPlayer viewer, StateMenu menu) {
		DisplayMenu view = menu.getCurrentView();
		if (view == null) return;
		playMenuOpenSound(viewer.getBukkitPlayer());
		stateMenus.put(view.getInventory(), menu);
		ChatUtil.printDebug("Displaying new menu to player " + viewer.getBukkitPlayer().getName() + ", total menus = " + stateMenus.size());
		// Schedule delayed task to display inventory to player
		Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), () -> viewer.getBukkitPlayer().openInventory(view.getInventory()),1);
	}

	/* All menus
	 *
	 * + Help Menu
	 * + Main Menu
	 * + Kingdom Menu
	 * Town Menu
	 * Town Management Menu
	 * Town Plot Menu
	 * Info Menu
	 * Score Menu
	 * Travel Menu
	 * Prefix Menu
	 */

	/*
	 * ===============================================
	 * Help Menu
	 * ===============================================
	 */
	public void displayHelpMenu(KonPlayer displayPlayer) {
		if (displayPlayer == null) return;
		HelpMenu newMenu = new HelpMenu(konquest, displayPlayer);
		displayMenuToPlayer(displayPlayer, newMenu);
	}

	/*
	 * ===============================================
	 * Main Menu
	 * ===============================================
	 */
	public void displayMainMenu(KonPlayer displayPlayer) {
		if (displayPlayer == null) return;
		MainMenu newMenu = new MainMenu(konquest, displayPlayer);
		displayMenuToPlayer(displayPlayer, newMenu);
	}

	/*
	 * ===============================================
	 * Kingdom Menu
	 * ===============================================
	 */
	public void displayKingdomMenu(KonPlayer displayPlayer) {
		displayKingdomMenu(displayPlayer, displayPlayer.getKingdom(), false);
	}

	public void displayKingdomMenu(KonPlayer displayPlayer, KonKingdom kingdom, boolean isAdmin) {
		if (displayPlayer == null) return;
		KingdomMenu newMenu = new KingdomMenu(konquest, displayPlayer, kingdom, isAdmin);
		displayMenuToPlayer(displayPlayer, newMenu);
	}

	/*
	 * ===============================================
	 * Town Menus
	 * ===============================================
	 */
	public void displayTownMenu(KonPlayer displayPlayer) {
		if (displayPlayer == null) return;
		TownMenu newMenu = new TownMenu(konquest, displayPlayer);
		displayMenu(displayPlayer, newMenu);
	}

	public void displayTownManagementMenu(KonPlayer displayPlayer, KonTown town, boolean isAdmin) {
		if (displayPlayer == null) return;
		TownManagementMenu newMenu = new TownManagementMenu(konquest, displayPlayer, town, isAdmin);
		displayMenu(displayPlayer, newMenu);
	}

	public void displayTownPlotMenu(KonPlayer displayPlayer, KonTown town) {
		// Verify plots are enabled
		boolean isPlotsEnabled = konquest.getPlotManager().isEnabled();
		if(!isPlotsEnabled) {
			ChatUtil.sendError(displayPlayer.getBukkitPlayer(), MessagePath.GENERIC_ERROR_DISABLED.getMessage());
		} else {
			// Open the menu
			int maxSize = konquest.getPlotManager().getMaxSize();
			PlotMenu newMenu = new PlotMenu(town, displayPlayer.getBukkitPlayer(), maxSize);
			displayMenu(displayPlayer, newMenu);
		}
	}


	// Old menu wrappers


	/*
	 * ===============================================
	 * Score Menu
	 * ===============================================
	 */
 	public void displayScoreMenu(KonPlayer displayPlayer, KonOfflinePlayer scorePlayer) {
		if (displayPlayer == null) return;
		Player bukkitPlayer = displayPlayer.getBukkitPlayer();
		playMenuOpenSound(bukkitPlayer);
		// Create menu
		ScoreMenuWrapper wrapper = new ScoreMenuWrapper(konquest, scorePlayer, displayPlayer);
		wrapper.constructMenu();
		// Display menu
		showMenuWrapper(bukkitPlayer,wrapper);
	}
	
 	/*
	 * ===============================================
	 * Info Menus
	 * ===============================================
	 */
 	// Player Info
 	public void displayPlayerInfoMenu(KonPlayer displayPlayer, KonOfflinePlayer infoPlayer) {
		if (displayPlayer == null) return;
		Player bukkitPlayer = displayPlayer.getBukkitPlayer();
		playMenuOpenSound(displayPlayer.getBukkitPlayer());
		// Create menu
		PlayerInfoMenuWrapper wrapper = new PlayerInfoMenuWrapper(konquest, infoPlayer, displayPlayer);
		wrapper.constructMenu();
		// Display menu
		showMenuWrapper(bukkitPlayer,wrapper);
 	}
 	
 	// Kingdom Info
  	public void displayKingdomInfoMenu(KonPlayer displayPlayer, KonKingdom infoKingdom) {
		if (displayPlayer == null) return;
		Player bukkitPlayer = displayPlayer.getBukkitPlayer();
		playMenuOpenSound(displayPlayer.getBukkitPlayer());
		// Create menu
		KingdomInfoMenuWrapper wrapper = new KingdomInfoMenuWrapper(konquest, infoKingdom, displayPlayer);
		wrapper.constructMenu();
		// Display menu
		showMenuWrapper(bukkitPlayer,wrapper);
  	}
 	
  	// Town Info
   	public void displayTownInfoMenu(KonPlayer displayPlayer, KonTown infoTown) {
		if (displayPlayer == null) return;
		Player bukkitPlayer = displayPlayer.getBukkitPlayer();
		playMenuOpenSound(displayPlayer.getBukkitPlayer());
		// Create menu
		TownInfoMenuWrapper wrapper = new TownInfoMenuWrapper(konquest, infoTown, displayPlayer);
		wrapper.constructMenu();
		// Display menu
		showMenuWrapper(bukkitPlayer,wrapper);
   	}

	// Monument Template Info
	public void displayTemplateInfoMenu(KonPlayer displayPlayer) {
		if (displayPlayer == null) return;
		Player bukkitPlayer = displayPlayer.getBukkitPlayer();
		playMenuOpenSound(displayPlayer.getBukkitPlayer());
		// Create menu
		MonumentTemplateInfoMenuWrapper wrapper = new MonumentTemplateInfoMenuWrapper(konquest);
		wrapper.constructMenu();
		// Display menu
		showMenuWrapper(bukkitPlayer,wrapper);
	}

	// Sanctuary Info
	public void displaySanctuaryInfoMenu(KonPlayer displayPlayer, KonSanctuary infoSanctuary) {
		if (displayPlayer == null) return;
		Player bukkitPlayer = displayPlayer.getBukkitPlayer();
		playMenuOpenSound(displayPlayer.getBukkitPlayer());
		// Create menu
		SanctuaryInfoMenuWrapper wrapper = new SanctuaryInfoMenuWrapper(konquest, infoSanctuary);
		wrapper.constructMenu();
		// Display menu
		showMenuWrapper(bukkitPlayer, wrapper);
	}

	// Ruin Info
	public void displayRuinInfoMenu(KonPlayer displayPlayer, KonRuin infoRuin) {
		if (displayPlayer == null) return;
		Player bukkitPlayer = displayPlayer.getBukkitPlayer();
		playMenuOpenSound(displayPlayer.getBukkitPlayer());
		// Create menu
		RuinInfoMenuWrapper wrapper = new RuinInfoMenuWrapper(konquest, infoRuin);
		wrapper.constructMenu();
		// Display menu
		showMenuWrapper(bukkitPlayer,wrapper);
	}
   	
   	/*
	 * ===============================================
	 * Prefix Menu
	 * ===============================================
	 */
   	public void displayPrefixMenu(KonPlayer displayPlayer) {
		if (displayPlayer == null) return;
		Player bukkitPlayer = displayPlayer.getBukkitPlayer();
		playMenuOpenSound(displayPlayer.getBukkitPlayer());
		// Create menu
		PrefixMenuWrapper wrapper = new PrefixMenuWrapper(konquest, displayPlayer);
		wrapper.constructMenu();
		// Display menu
		showMenuWrapper(bukkitPlayer,wrapper);
   	}
   	

   	
   	/*
	 * Helper methods
	 */
 	
   	public static String boolean2Symbol(boolean val) {
	   if(val)return ChatColor.DARK_GREEN+""+ChatColor.BOLD+ "\u2713";
	   return ChatColor.DARK_RED+""+ChatColor.BOLD+ "\u274C";
 	}
 	
   	public static String boolean2Lang(boolean val) {
	   if(val) return MessagePath.LABEL_TRUE.getMessage();
	   return MessagePath.LABEL_FALSE.getMessage();
 	}
 	
   	private void playMenuClickSound(Player bukkitPlayer) {
 		bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, (float)1.0, (float)0.8);
 	}
 	
   	private void playMenuOpenSound(Player bukkitPlayer) {
 		bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, (float)1.0, (float)1.4);
 	}
}
