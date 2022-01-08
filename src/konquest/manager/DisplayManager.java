package konquest.manager;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import konquest.Konquest;
import konquest.display.DisplayMenu;
import konquest.display.GuildMenu;
import konquest.display.MenuIcon;
import konquest.display.PagedMenu;
import konquest.display.PlotMenu;
import konquest.display.StateMenu;
import konquest.display.wrapper.HelpMenuWrapper;
import konquest.display.wrapper.KingdomInfoMenuWrapper;
import konquest.display.wrapper.MenuWrapper;
import konquest.display.wrapper.PlayerInfoMenuWrapper;
import konquest.display.wrapper.PrefixMenuWrapper;
import konquest.display.wrapper.ScoreMenuWrapper;
import konquest.display.wrapper.TownInfoMenuWrapper;
import konquest.display.wrapper.TownOptionsMenuWrapper;
import konquest.display.wrapper.TownShieldMenuWrapper;
import konquest.display.wrapper.TownUpgradeMenuWrapper;
import konquest.model.KonGuild;
import konquest.model.KonKingdom;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class DisplayManager {

	private Konquest konquest;
	private HashMap<Inventory, MenuWrapper> pagedMenus;
	private HashMap<Inventory, StateMenu> stateMenus;
	
	public static ChatColor titleColor = ChatColor.BLACK;
	public static ChatColor loreColor = ChatColor.YELLOW;
	public static ChatColor valueColor = ChatColor.AQUA;
	public static ChatColor hintColor = ChatColor.GOLD;
	
	public DisplayManager(Konquest konquest) {
		this.konquest = konquest;
		this.pagedMenus = new HashMap<Inventory, MenuWrapper>();
		this.stateMenus = new HashMap<Inventory, StateMenu>();
	}
	
	public void initialize() {
		ChatUtil.printDebug("Display Manager is ready");
	}
	
	/*
	 * Common menu methods
	 */
	
	public boolean isDisplayMenu(Inventory inv) {
		boolean result = false;
		if(inv != null) {
			if(pagedMenus.containsKey(inv)) {
				result = true;
			} else if(stateMenus.containsKey(inv)) {
				result = true;
			}
		}
		return result;
	}
	
	public void onDisplayMenuClick(KonPlayer clickPlayer, Inventory inv, int slot, boolean clickType) {
		if(inv == null) {
			return;
		}
		Player bukkitPlayer = clickPlayer.getBukkitPlayer();
		// Switch pages and handle navigation button clicks
		if(pagedMenus.containsKey(inv)) {
			MenuWrapper wrapper = pagedMenus.get(inv);
			if(wrapper == null) {
				return;
			}
			PagedMenu clickMenu = wrapper.getMenu();
			if(clickMenu == null) {
				return;
			}
			DisplayMenu currentPage = clickMenu.getCurrentPage();
			if(currentPage == null) {
				return;
			}
			MenuIcon clickedIcon = currentPage.getIcon(slot);
			if(clickedIcon == null || !clickedIcon.isClickable()) {
				return;
			}
			playMenuClickSound(bukkitPlayer);
			int nextIndex = clickMenu.getCurrentNextSlot();
			int closeIndex = clickMenu.getCurrentCloseSlot();
			int backIndex = clickMenu.getCurrentBackSlot();
			pagedMenus.remove(inv);
			
			// Handle click context
			if(slot == nextIndex) {
				clickMenu.nextPageIndex();
				clickMenu.refreshCurrentPage();
				Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
		            @Override
		            public void run() {
		            	bukkitPlayer.openInventory(wrapper.getCurrentInventory());
		            	pagedMenus.put(wrapper.getCurrentInventory(), wrapper);
		            }
		        },1);
			} else if(slot == closeIndex) {
				Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
		            @Override
		            public void run() {
		            	bukkitPlayer.closeInventory();
		            }
		        },1);
			} else if(slot == backIndex) {
				clickMenu.previousPageIndex();
				clickMenu.refreshCurrentPage();
				Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
		            @Override
		            public void run() {
		            	bukkitPlayer.openInventory(clickMenu.getCurrentPage().getInventory());
		            	pagedMenus.put(wrapper.getCurrentInventory(), wrapper);
		            }
		        },1);
			} else {
				// Clicked non-navigation slot
				boolean status = wrapper.onIconClick(clickPlayer, clickedIcon);
				if(status) {
					Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
			            @Override
			            public void run() {
			            	bukkitPlayer.closeInventory();
			            }
			        });
				}
			}
		} else if(stateMenus.containsKey(inv)) {
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
			// Update plot menu state
			DisplayMenu updateView = clickMenu.updateState(slot, clickType);
			// Update inventory view
			stateMenus.remove(inv);
			if(updateView != null) {
				// Refresh displayed inventory view
				Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
		            @Override
		            public void run() {
		            	bukkitPlayer.openInventory(updateView.getInventory());
		            	stateMenus.put(updateView.getInventory(), clickMenu);
		            }
		        },1);
			} else {
				// Close inventory view
				Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
		            @Override
		            public void run() {
		            	bukkitPlayer.closeInventory();
		            }
		        },1);
			}
		}
	}
	
	public void onDisplayMenuClose(Inventory inv) {
		if(pagedMenus.containsKey(inv)) {
			pagedMenus.remove(inv);
		}
		if(stateMenus.containsKey(inv)) {
			stateMenus.remove(inv);
		}
	}
	
	private void showDisplayMenu(Player bukkitPlayer, Inventory inv) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
            @Override
            public void run() {
            	bukkitPlayer.openInventory(inv);
            }
        });
	}
	
	/*
	 * ===============================================
	 * Help Menu
	 * ===============================================
	 */
	public void displayHelpMenu(Player bukkitPlayer) {
		//ChatUtil.printDebug("Displaying new help menu to "+bukkitPlayer.getName()+", current menu size is "+menuCache.size());
		playMenuOpenSound(bukkitPlayer);
		// Create menu
		HelpMenuWrapper wrapper = new HelpMenuWrapper(konquest);
		wrapper.constructMenu();
		pagedMenus.put(wrapper.getCurrentInventory(), wrapper);
		// Display menu
		showDisplayMenu(bukkitPlayer,wrapper.getCurrentInventory());
	}
	
	/*
	 * ===============================================
	 * Town Menus
	 * ===============================================
	 */
	public void displayTownUpgradeMenu(Player bukkitPlayer, KonTown town) {
		playMenuOpenSound(bukkitPlayer);
		// Create menu
		TownUpgradeMenuWrapper wrapper = new TownUpgradeMenuWrapper(konquest, town);
		wrapper.constructMenu();
		pagedMenus.put(wrapper.getCurrentInventory(), wrapper);
		// Display menu
		showDisplayMenu(bukkitPlayer,wrapper.getCurrentInventory());
	}
	
	public void displayTownShieldMenu(Player bukkitPlayer, KonTown town) {
		playMenuOpenSound(bukkitPlayer);
		// Create menu
		TownShieldMenuWrapper wrapper = new TownShieldMenuWrapper(konquest, town);
		wrapper.constructMenu();
		pagedMenus.put(wrapper.getCurrentInventory(), wrapper);
		// Display menu
		showDisplayMenu(bukkitPlayer,wrapper.getCurrentInventory());
	}

	public void displayTownOptionsMenu(Player bukkitPlayer, KonTown town) {
		playMenuOpenSound(bukkitPlayer);
		// Create menu
		TownOptionsMenuWrapper wrapper = new TownOptionsMenuWrapper(konquest, town);
		wrapper.constructMenu();
		pagedMenus.put(wrapper.getCurrentInventory(), wrapper);
		// Display menu
		showDisplayMenu(bukkitPlayer,wrapper.getCurrentInventory());
	}
	
	/*
	 * ===============================================
	 * Score Menu
	 * ===============================================
	 */
 	public void displayScoreMenu(KonPlayer displayPlayer, KonOfflinePlayer scorePlayer) {
 		Player bukkitPlayer = displayPlayer.getBukkitPlayer();
 		playMenuOpenSound(bukkitPlayer);
 		// Create menu
		ScoreMenuWrapper wrapper = new ScoreMenuWrapper(konquest, scorePlayer, displayPlayer);
		wrapper.constructMenu();
		pagedMenus.put(wrapper.getCurrentInventory(), wrapper);
		// Display menu
		showDisplayMenu(bukkitPlayer,wrapper.getCurrentInventory());
	}
	
 	/*
	 * ===============================================
	 * Info Menus
	 * ===============================================
	 */
 	// Player Info
 	public void displayPlayerInfoMenu(KonPlayer displayPlayer, KonOfflinePlayer infoPlayer) {
 		Player bukkitPlayer = displayPlayer.getBukkitPlayer();
 		playMenuOpenSound(displayPlayer.getBukkitPlayer());
 		// Create menu
		PlayerInfoMenuWrapper wrapper = new PlayerInfoMenuWrapper(konquest, infoPlayer, displayPlayer);
		wrapper.constructMenu();
		pagedMenus.put(wrapper.getCurrentInventory(), wrapper);
		// Display menu
		showDisplayMenu(bukkitPlayer,wrapper.getCurrentInventory());
 	}
 	
 	// Kingdom Info
  	public void displayKingdomInfoMenu(KonPlayer displayPlayer, KonKingdom infoKingdom) {
  		Player bukkitPlayer = displayPlayer.getBukkitPlayer();
 		playMenuOpenSound(displayPlayer.getBukkitPlayer());
 		// Create menu
		KingdomInfoMenuWrapper wrapper = new KingdomInfoMenuWrapper(konquest, infoKingdom, displayPlayer);
		wrapper.constructMenu();
		pagedMenus.put(wrapper.getCurrentInventory(), wrapper);
		// Display menu
		showDisplayMenu(bukkitPlayer,wrapper.getCurrentInventory());
  	}
 	
  	// Town Info
   	public void displayTownInfoMenu(KonPlayer displayPlayer, KonTown infoTown) {
   		Player bukkitPlayer = displayPlayer.getBukkitPlayer();
   		playMenuOpenSound(displayPlayer.getBukkitPlayer());
   		// Create menu
		TownInfoMenuWrapper wrapper = new TownInfoMenuWrapper(konquest, infoTown, displayPlayer);
		wrapper.constructMenu();
		pagedMenus.put(wrapper.getCurrentInventory(), wrapper);
		// Display menu
		showDisplayMenu(bukkitPlayer,wrapper.getCurrentInventory());
   	}
   	
   	/*
	 * ===============================================
	 * Prefix Menu
	 * ===============================================
	 */
   	public void displayPrefixMenu(KonPlayer displayPlayer) {
   		Player bukkitPlayer = displayPlayer.getBukkitPlayer();
   		playMenuOpenSound(displayPlayer.getBukkitPlayer());
 		// Create menu
		PrefixMenuWrapper wrapper = new PrefixMenuWrapper(konquest, displayPlayer);
		wrapper.constructMenu();
		pagedMenus.put(wrapper.getCurrentInventory(), wrapper);
		// Display menu
		showDisplayMenu(bukkitPlayer,wrapper.getCurrentInventory());
   	}
   	
   	/*
	 * ===============================================
	 * Plot Menu
	 * ===============================================
	 */
   	public void displayPlotMenu(Player bukkitPlayer, KonTown town) {
   		//ChatUtil.printDebug("Displaying new plots menu to "+bukkitPlayer.getName()+", current menu size is "+plotMenus.size());
		playMenuOpenSound(bukkitPlayer);
		int maxSize = konquest.getPlotManager().getMaxSize();
		PlotMenu newMenu = new PlotMenu(town, bukkitPlayer, maxSize);
		stateMenus.put(newMenu.getCurrentView().getInventory(), newMenu);
		// Schedule delayed task to display inventory to player
		Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
            @Override
            public void run() {
            	bukkitPlayer.openInventory(newMenu.getCurrentView().getInventory());
            }
        },1);
	}
   	
   	/*
	 * ===============================================
	 * Guild Menu
	 * ===============================================
	 */
   	public void displayGuildMenu(KonPlayer displayPlayer, KonGuild guild, boolean isAdmin) {
   		//ChatUtil.printDebug("Displaying new guild menu to "+bukkitPlayer.getName()+", current menu size is "+plotMenus.size());
		playMenuOpenSound(displayPlayer.getBukkitPlayer());
		GuildMenu newMenu = new GuildMenu(konquest.getGuildManager(), displayPlayer, guild, isAdmin);
		stateMenus.put(newMenu.getCurrentView().getInventory(), newMenu);
		// Schedule delayed task to display inventory to player
		Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
            @Override
            public void run() {
            	displayPlayer.getBukkitPlayer().openInventory(newMenu.getCurrentView().getInventory());
            }
        },1);
	}
   	
   	/*
	 * Helper methods
	 */
 	
   	public static String boolean2Symbol(boolean val) {
 		String result = ChatColor.DARK_RED+""+ChatColor.BOLD+"\u274C";
    	if(val) {
    		result = ChatColor.DARK_GREEN+""+ChatColor.BOLD+"\u2713";
    	}
    	return result;
 	}
 	
   	public static String boolean2Lang(boolean val) {
 		String result = MessagePath.LABEL_FALSE.getMessage();
 		if(val) {
 			result = MessagePath.LABEL_TRUE.getMessage();
 		}
 		return result;
 	}
 	
   	private void playMenuClickSound(Player bukkitPlayer) {
 		bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, (float)1.0, (float)0.8);
 	}
 	
   	private void playMenuOpenSound(Player bukkitPlayer) {
 		bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, (float)1.0, (float)1.4);
 	}
}
