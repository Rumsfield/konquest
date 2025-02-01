package com.github.rumsfield.konquest.manager;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.display.*;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.display.menu.*;
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

	public static String adminFormat 		= ""+ChatColor.DARK_GRAY;
	public static String titleFormat 		= ""+ChatColor.BLACK;
	public static String nameFormat         = ""+ChatColor.GOLD;
	public static String loreFormat 		= ""+ChatColor.YELLOW;
	public static String valueFormat 		= ""+ChatColor.AQUA;
	public static String hintFormat 		= ""+ChatColor.GRAY+ChatColor.UNDERLINE+"\u21D2"; // ⇒
	public static String propertyFormat 	= ""+ChatColor.GOLD+ChatColor.ITALIC+"\u25C6"; // ◆
	public static String alertFormat 		= ""+ChatColor.RED+ChatColor.ITALIC+"\u26A0"; // ⚠
	public static String errorFormat        = ""+ChatColor.RED;
	
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
				DisplayView currentView = clickMenu.getCurrentView();
				if(currentView == null || !currentView.getInventory().equals(inv)) {
					ChatUtil.printDebug("State menu view is not current!");
					return;
				}
				MenuIcon clickedIcon = currentView.getIcon(slot);
				if(clickedIcon == null || !clickedIcon.isClickable()) {
					return;
				}
				playMenuClickSound(bukkitPlayer);
				// Stop icon updates
				clickMenu.stopIconUpdates();
				// Update menu state
				DisplayView updateView = clickMenu.updateState(slot, clickType);
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
		DisplayView view = menu.getCurrentView();
		if (view == null) return;
		playMenuOpenSound(viewer.getBukkitPlayer());
		stateMenus.put(view.getInventory(), menu);
		// Schedule delayed task to display inventory to player
		Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), () -> viewer.getBukkitPlayer().openInventory(view.getInventory()),1);
	}

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

	public void displayMainMenuDashboard(KonPlayer displayPlayer) {
		if (displayPlayer == null) return;
		MainMenu newMenu = new MainMenu(konquest, displayPlayer);
		newMenu.goToDashboard();
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
		displayTownMenu(displayPlayer, false);
	}

	public void displayTownMenu(KonPlayer displayPlayer, boolean isAdmin) {
		if (displayPlayer == null) return;
		TownMenu newMenu = new TownMenu(konquest, displayPlayer, isAdmin);
		displayMenuToPlayer(displayPlayer, newMenu);
	}

	public void displayTownManagementMenu(KonPlayer displayPlayer, KonTown town, boolean isAdmin) {
		if (displayPlayer == null) return;
		TownMenu newMenu = new TownMenu(konquest, displayPlayer, isAdmin);
		newMenu.goToManagementRoot(town);
		displayMenuToPlayer(displayPlayer, newMenu);
	}

	public void displayTownPlotMenu(KonPlayer displayPlayer, KonTown town) {
		// Verify plots are enabled
		boolean isPlotsEnabled = konquest.getPlotManager().isEnabled();
		if(!isPlotsEnabled) {
			ChatUtil.sendError(displayPlayer.getBukkitPlayer(), MessagePath.GENERIC_ERROR_DISABLED.getMessage());
		} else {
			// Open the menu
			int maxSize = konquest.getPlotManager().getMaxSize();
			PlotMenu newMenu = new PlotMenu(konquest, town, displayPlayer, maxSize);
			displayMenuToPlayer(displayPlayer, newMenu);
		}
	}

	/*
	 * ===============================================
	 * Prefix Menu
	 * ===============================================
	 */
	public void displayPrefixMenu(KonPlayer displayPlayer) {
		if (displayPlayer == null) return;
		PrefixMenu newMenu = new PrefixMenu(konquest, displayPlayer);
		displayMenuToPlayer(displayPlayer, newMenu);
	}

	/*
	 * ===============================================
	 * Score Menu
	 * ===============================================
	 */
	public void displayScoreMenu(KonPlayer displayPlayer) {
		if (displayPlayer == null) return;
		ScoreMenu newMenu = new ScoreMenu(konquest, displayPlayer);
		displayMenuToPlayer(displayPlayer, newMenu);
	}

	public void displayScorePlayerMenu(KonPlayer displayPlayer, KonOfflinePlayer scorePlayer) {
		if (displayPlayer == null) return;
		ScoreMenu newMenu = new ScoreMenu(konquest, displayPlayer);
		newMenu.goToPlayerScore(scorePlayer);
		displayMenuToPlayer(displayPlayer, newMenu);
	}

	public void displayScoreKingdomMenu(KonPlayer displayPlayer, KonKingdom scoreKingdom) {
		if (displayPlayer == null) return;
		ScoreMenu newMenu = new ScoreMenu(konquest, displayPlayer);
		newMenu.goToKingdomScore(scoreKingdom);
		displayMenuToPlayer(displayPlayer, newMenu);
	}

	/*
	 * ===============================================
	 * Info Menu
	 * ===============================================
	 */
	public void displayInfoMenu(KonPlayer displayPlayer) {
		if (displayPlayer == null) return;
		InfoMenu newMenu = new InfoMenu(konquest, displayPlayer);
		displayMenuToPlayer(displayPlayer, newMenu);
	}

 	public void displayInfoPlayerMenu(KonPlayer displayPlayer, KonOfflinePlayer infoPlayer) {
		if (displayPlayer == null) return;
		InfoMenu newMenu = new InfoMenu(konquest, displayPlayer);
		newMenu.goToPlayerInfo(infoPlayer);
		displayMenuToPlayer(displayPlayer, newMenu);
 	}

	public void displayInfoKingdomMenu(KonPlayer displayPlayer, KonKingdom infoKingdom) {
		if (displayPlayer == null) return;
		InfoMenu newMenu = new InfoMenu(konquest, displayPlayer);
		newMenu.goToKingdomInfo(infoKingdom);
		displayMenuToPlayer(displayPlayer, newMenu);
	}

	public void displayInfoTownMenu(KonPlayer displayPlayer, KonTown infoTown) {
		if (displayPlayer == null) return;
		InfoMenu newMenu = new InfoMenu(konquest, displayPlayer);
		newMenu.goToTownInfo(infoTown);
		displayMenuToPlayer(displayPlayer, newMenu);
	}

	public void displayInfoSanctuaryMenu(KonPlayer displayPlayer, KonSanctuary infoSanctuary) {
		if (displayPlayer == null) return;
		InfoMenu newMenu = new InfoMenu(konquest, displayPlayer);
		newMenu.goToSanctuaryInfo(infoSanctuary);
		displayMenuToPlayer(displayPlayer, newMenu);
	}

	public void displayInfoRuinMenu(KonPlayer displayPlayer, KonRuin infoRuin) {
		if (displayPlayer == null) return;
		InfoMenu newMenu = new InfoMenu(konquest, displayPlayer);
		newMenu.goToRuinInfo(infoRuin);
		displayMenuToPlayer(displayPlayer, newMenu);
	}

	public void displayInfoCampMenu(KonPlayer displayPlayer, KonCamp infoCamp) {
		if (displayPlayer == null) return;
		InfoMenu newMenu = new InfoMenu(konquest, displayPlayer);
		newMenu.goToCampInfo(infoCamp);
		displayMenuToPlayer(displayPlayer, newMenu);
	}

	public void displayInfoTemplateMenu(KonPlayer displayPlayer, KonMonumentTemplate infoTemplate) {
		if (displayPlayer == null) return;
		InfoMenu newMenu = new InfoMenu(konquest, displayPlayer);
		newMenu.goToTemplateInfo(infoTemplate);
		displayMenuToPlayer(displayPlayer, newMenu);
	}

	public void displayInfoTemplateListMenu(KonPlayer displayPlayer) {
		if (displayPlayer == null) return;
		InfoMenu newMenu = new InfoMenu(konquest, displayPlayer);
		newMenu.goToTemplateListInfo();
		displayMenuToPlayer(displayPlayer, newMenu);
	}

	/*
	 * ===============================================
	 * Travel Menu
	 * ===============================================
	 */
	public void displayTravelMenu(KonPlayer displayPlayer) {
		displayTravelMenu(displayPlayer, false);
	}

	public void displayTravelMenu(KonPlayer displayPlayer, boolean isAdmin) {
		if (displayPlayer == null) return;
		TravelMenu newMenu = new TravelMenu(konquest, displayPlayer, isAdmin);
		displayMenuToPlayer(displayPlayer, newMenu);
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
