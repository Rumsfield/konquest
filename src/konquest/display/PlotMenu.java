package konquest.display;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;

import konquest.model.KonTown;

public class PlotMenu {

	enum PlotState {
		ROOT,
		ROOT_CREATE,
		ROOT_DELETE,
		ROOT_EDIT,
		EDIT,
		EDIT_LAND_ADD,
		EDIT_LAND_REMOVE,
		EDIT_PLAYER_ADD,
		EDIT_PLAYER_REMOVE;
	}
	
	
	private KonTown town;
	private Point center;
	private HashMap<PlotState,DisplayMenu> views;
	private DisplayMenu[] scrollViews;
	private ArrayList<DisplayMenu> playerPages;
	private PlotState currentPlotState;
	
	public PlotMenu(KonTown town, Point center) {
		this.town = town;
		this.center = center;
		this.views = new HashMap<PlotState,DisplayMenu>();
		this.scrollViews = new DisplayMenu[4];
		this.playerPages = new ArrayList<DisplayMenu>();
		this.currentPlotState = PlotState.ROOT;
		initializeMenu();
	}
	
	private void initializeMenu() {
		views.put(PlotState.ROOT, new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Plots"));
		views.put(PlotState.ROOT_CREATE, new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Create Plot"));
		views.put(PlotState.ROOT_DELETE, new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Delete Plot"));
		views.put(PlotState.ROOT_EDIT, new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Edit Plot"));
		views.put(PlotState.EDIT, new DisplayMenu(2, ChatColor.BLACK+town.getName()+" Edit"));
		views.put(PlotState.EDIT_LAND_ADD, new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Add Land"));
		views.put(PlotState.EDIT_LAND_REMOVE, new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Remove Land"));
		views.put(PlotState.EDIT_PLAYER_ADD, new DisplayMenu(2, ChatColor.BLACK+town.getName()+" Add Player"));
		views.put(PlotState.EDIT_PLAYER_REMOVE, new DisplayMenu(2, ChatColor.BLACK+town.getName()+" Remove Player"));
		
		scrollViews[0] = new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Plots"); // North
		scrollViews[1] = new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Plots"); // East
		scrollViews[2] = new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Plots"); // South
		scrollViews[3] = new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Plots"); // West
		
		playerPages.add(new DisplayMenu(2, ChatColor.BLACK+town.getName()+" Player"));
	}
	
	public PlotState getState() {
		return currentPlotState;
	}
	
	public DisplayMenu getCurrentView() {
		return views.get(currentPlotState);
	}
	
	/**
	 * Updates menu state based on clicked slot.
	 * @param slot - Clicked icon index
	 * @return The new DisplayMenu view.
	 */
	public DisplayMenu updateState(int slot) {
		DisplayMenu result = null;
		
		
		int navMaxIndex = getCurrentView().getInventory().getSize()-1;
		int navMinIndex = getCurrentView().getInventory().getSize()-9;
		if(slot <= navMaxIndex && slot >= navMinIndex) {
			// Clicked in navigation bar
			
		} else {
			// Click in non-navigation slot
			
		}
		
		return result;
	}
	
	/**
	 * Place all navigation button icons on all views and update icons
	 */
	public void refreshNavigationButtons() {
		
	}
	
}
