package konquest.display;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;

import konquest.Konquest;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;

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
	private Point origin;
	private Location playerLoc;
	private HashMap<PlotState,DisplayMenu> views;
	//private DisplayMenu[] scrollViews;
	private ArrayList<DisplayMenu> playerPages;
	private PlotState currentPlotState;
	
	public PlotMenu(KonTown town, Location playerLoc) {
		this.town = town;
		this.playerLoc = playerLoc;
		this.center = Konquest.toPoint(playerLoc);
		this.origin = center;
		this.views = new HashMap<PlotState,DisplayMenu>();
		//this.scrollViews = new DisplayMenu[4];
		this.playerPages = new ArrayList<DisplayMenu>();
		this.currentPlotState = PlotState.ROOT;
		initializeMenu();
		renderMenuViews();
	}
	
	private void initializeMenu() {
		//scrollViews[0] = new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Plots"); // North
		//scrollViews[1] = new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Plots"); // East
		//scrollViews[2] = new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Plots"); // South
		//scrollViews[3] = new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Plots"); // West
		
		playerPages.add(new DisplayMenu(2, ChatColor.BLACK+town.getName()+" Player"));
		
		views.put(PlotState.ROOT, new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Plots"));
		views.put(PlotState.ROOT_CREATE, new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Create Plot"));
		views.put(PlotState.ROOT_DELETE, new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Delete Plot"));
		views.put(PlotState.ROOT_EDIT, new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Edit Plot"));
		views.put(PlotState.EDIT, new DisplayMenu(2, ChatColor.BLACK+town.getName()+" Edit"));
		views.put(PlotState.EDIT_LAND_ADD, new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Add Land"));
		views.put(PlotState.EDIT_LAND_REMOVE, new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Remove Land"));
		views.put(PlotState.EDIT_PLAYER_ADD, new DisplayMenu(2, ChatColor.BLACK+town.getName()+" Add Player"));
		views.put(PlotState.EDIT_PLAYER_REMOVE, new DisplayMenu(2, ChatColor.BLACK+town.getName()+" Remove Player"));
		
		if(!town.isLocInside(playerLoc)) {
			center = Konquest.toPoint(town.getCenterLoc());
		}
	}
	
	private void renderMenuViews() {
		DisplayMenu renderView;
		
		/* Root View */
		String viewTitle = ChatColor.BLACK+town.getName()+" Plots";
		renderView = createLandView(center,viewTitle,PlotState.ROOT);
		views.put(PlotState.ROOT, renderView);
		origin = center;
		/*
		Point northCenter = new Point(center.x + 0, center.y + 1);
		Point eastCenter = new Point(center.x + 1, center.y + 0);
		Point southCenter = new Point(center.x + 0, center.y - 1);
		Point westCenter = new Point(center.x - 1, center.y + 0);
		scrollViews[0] = createLandView(northCenter,viewTitle,PlotState.ROOT); // North
		scrollViews[1] = createLandView(eastCenter,viewTitle,PlotState.ROOT); // East
		scrollViews[2] = createLandView(southCenter,viewTitle,PlotState.ROOT); // South
		scrollViews[3] = createLandView(westCenter,viewTitle,PlotState.ROOT); // West
		*/
		
	}
	
	private DisplayMenu createLandView(Point origin, String title, PlotState context) {
		DisplayMenu result = new DisplayMenu(6, title);
		List<String> loreList;
		InfoIcon icon;
		Point drawPoint;
		int index = 0;
		for(int r = 2; r >= -2; r--) {
			for(int c = -3; c <= 3; c++) {
				// Iterate over all land tiles
				drawPoint = new Point(origin.x + c, origin.y + r);
				if(town.getChunkList().containsKey(drawPoint)) {
					loreList = new ArrayList<String>();
			    	loreList.add("Test");
					Material landMat = Material.GREEN_STAINED_GLASS_PANE;
					if(Konquest.toPoint(playerLoc).equals(drawPoint)) {
						landMat = Material.PLAYER_HEAD;
						loreList.add("You Are Here");
					}
					if(Konquest.toPoint(town.getCenterLoc()).equals(drawPoint)) {
						landMat = Material.IRON_BARS;
						loreList.add("Town Monument");
					}
					icon = new InfoIcon(ChatColor.GOLD+"Town Land",loreList,landMat,index,false);
					result.addIcon(icon);
				}
				index++;
			}
		}
		return result;
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
			// Clicked in navigation bar, do something based on current state
			switch(currentPlotState) {
			case ROOT:
				// Scroll arrows [0,1,2,3], close [4], create [5], delete [6], edit [7]
				String viewTitle = ChatColor.BLACK+town.getName()+" Plots";
				if(slot == navMinIndex+0) {
					// Scroll left
					origin = new Point(origin.x - 1, origin.y + 0);
					result = createLandView(origin,viewTitle,PlotState.ROOT);
					views.put(PlotState.ROOT, result);
				} else if(slot == navMinIndex+1) {
					// Scroll up
					origin = new Point(origin.x + 0, origin.y + 1);
					result = createLandView(origin,viewTitle,PlotState.ROOT);
					views.put(PlotState.ROOT, result);
				} else if(slot == navMinIndex+2) {
					// Scroll down
					origin = new Point(origin.x + 0, origin.y - 1);
					result = createLandView(origin,viewTitle,PlotState.ROOT);
					views.put(PlotState.ROOT, result);
				} else if(slot == navMinIndex+3) {
					// Scroll right
					origin = new Point(origin.x + 1, origin.y + 0);
					result = createLandView(origin,viewTitle,PlotState.ROOT);
					views.put(PlotState.ROOT, result);
				} else if(slot == navMinIndex+4) {
					// Close
					result = null;
				}
				break;
			case ROOT_CREATE:
				// Scroll arrows [0,1,2,3], close [4], return [5]
				
				break;
			case ROOT_DELETE:
				// Scroll arrows [0,1,2,3], close [4], return [5]
				
				break;
			case ROOT_EDIT:
				// Scroll arrows [0,1,2,3], close [4], return [5]
				
				break;
			case EDIT:
				// Close [4], return [5]
				
				break;
			case EDIT_LAND_ADD:
				// Scroll arrows [0,1,2,3], close [4], return [5], finish [6]
				
				break;
			case EDIT_LAND_REMOVE:
				// Scroll arrows [0,1,2,3], close [4], return [5], finish [6]
				
				break;
			case EDIT_PLAYER_ADD:
				// (back [0]) close [4], return [5], finish [6], (next [8])
				
				break;
			case EDIT_PLAYER_REMOVE:
				// (back [0]) close [4], return [5], finish [6], (next [8])
				
				break;
			default:
				break;
		}
			
		} else {
			// Click in non-navigation slot
			
		}
		
		return result;
	}
	
	/**
	 * Place all navigation button icons on view given context and update icons
	 */
	public void refreshNavigationButtons(PlotState context) {
		
		DisplayMenu view = views.get(context);
		int navStart = view.getInventory().getSize()-9;
		if(navStart < 0) {
			ChatUtil.printDebug("Plot menu nav buttons failed to refresh in context "+context.toString());
			return;
		}
		switch(context) {
			case ROOT:
				// Scroll arrows [0,1,2,3], close [4], create [5], delete [6], edit [7]
				view.addIcon(navIconScrollLeft(navStart+0));
				view.addIcon(navIconScrollUp(navStart+1));
				view.addIcon(navIconScrollDown(navStart+2));
				view.addIcon(navIconScrollRight(navStart+3));
				view.addIcon(navIconClose(navStart+4));
				view.addIcon(navIconCreate(navStart+5));
				view.addIcon(navIconDelete(navStart+6));
				view.addIcon(navIconEdit(navStart+7));
				view.addIcon(navIconEmpty(navStart+8));
				break;
			case ROOT_CREATE:
				// Scroll arrows [0,1,2,3], close [4], return [5]
				view.addIcon(navIconScrollLeft(navStart+0));
				view.addIcon(navIconScrollUp(navStart+1));
				view.addIcon(navIconScrollDown(navStart+2));
				view.addIcon(navIconScrollRight(navStart+3));
				view.addIcon(navIconClose(navStart+4));
				view.addIcon(navIconReturn(navStart+5));
				view.addIcon(navIconEmpty(navStart+6));
				view.addIcon(navIconEmpty(navStart+7));
				view.addIcon(navIconEmpty(navStart+8));
				break;
			case ROOT_DELETE:
				// Scroll arrows [0,1,2,3], close [4], return [5]
				view.addIcon(navIconScrollLeft(navStart+0));
				view.addIcon(navIconScrollUp(navStart+1));
				view.addIcon(navIconScrollDown(navStart+2));
				view.addIcon(navIconScrollRight(navStart+3));
				view.addIcon(navIconClose(navStart+4));
				view.addIcon(navIconReturn(navStart+5));
				view.addIcon(navIconEmpty(navStart+6));
				view.addIcon(navIconEmpty(navStart+7));
				view.addIcon(navIconEmpty(navStart+8));
				break;
			case ROOT_EDIT:
				// Scroll arrows [0,1,2,3], close [4], return [5]
				view.addIcon(navIconScrollLeft(navStart+0));
				view.addIcon(navIconScrollUp(navStart+1));
				view.addIcon(navIconScrollDown(navStart+2));
				view.addIcon(navIconScrollRight(navStart+3));
				view.addIcon(navIconClose(navStart+4));
				view.addIcon(navIconReturn(navStart+5));
				view.addIcon(navIconEmpty(navStart+6));
				view.addIcon(navIconEmpty(navStart+7));
				view.addIcon(navIconEmpty(navStart+8));
				break;
			case EDIT:
				// Close [4], return [5]
				view.addIcon(navIconEmpty(navStart+0));
				view.addIcon(navIconEmpty(navStart+1));
				view.addIcon(navIconEmpty(navStart+2));
				view.addIcon(navIconEmpty(navStart+3));
				view.addIcon(navIconClose(navStart+4));
				view.addIcon(navIconReturn(navStart+5));
				view.addIcon(navIconEmpty(navStart+6));
				view.addIcon(navIconEmpty(navStart+7));
				view.addIcon(navIconEmpty(navStart+8));
				break;
			case EDIT_LAND_ADD:
				// Scroll arrows [0,1,2,3], close [4], return [5], finish [6]
				view.addIcon(navIconScrollLeft(navStart+0));
				view.addIcon(navIconScrollUp(navStart+1));
				view.addIcon(navIconScrollDown(navStart+2));
				view.addIcon(navIconScrollRight(navStart+3));
				view.addIcon(navIconClose(navStart+4));
				view.addIcon(navIconReturn(navStart+5));
				view.addIcon(navIconFinish(navStart+6));
				view.addIcon(navIconEmpty(navStart+7));
				view.addIcon(navIconEmpty(navStart+8));
				break;
			case EDIT_LAND_REMOVE:
				// Scroll arrows [0,1,2,3], close [4], return [5], finish [6]
				view.addIcon(navIconScrollLeft(navStart+0));
				view.addIcon(navIconScrollUp(navStart+1));
				view.addIcon(navIconScrollDown(navStart+2));
				view.addIcon(navIconScrollRight(navStart+3));
				view.addIcon(navIconClose(navStart+4));
				view.addIcon(navIconReturn(navStart+5));
				view.addIcon(navIconFinish(navStart+6));
				view.addIcon(navIconEmpty(navStart+7));
				view.addIcon(navIconEmpty(navStart+8));
				break;
			case EDIT_PLAYER_ADD:
				// (back [0]) close [4], return [5], finish [6], (next [8])
				// TODO
				view.addIcon(navIconBack(navStart+0));
				view.addIcon(navIconEmpty(navStart+1));
				view.addIcon(navIconEmpty(navStart+2));
				view.addIcon(navIconEmpty(navStart+3));
				view.addIcon(navIconClose(navStart+4));
				view.addIcon(navIconEmpty(navStart+5));
				view.addIcon(navIconEmpty(navStart+6));
				view.addIcon(navIconEmpty(navStart+7));
				view.addIcon(navIconNext(navStart+8));
				break;
			case EDIT_PLAYER_REMOVE:
				// (back [0]) close [4], return [5], finish [6], (next [8])
				// TODO
				view.addIcon(navIconBack(navStart+0));
				view.addIcon(navIconEmpty(navStart+1));
				view.addIcon(navIconEmpty(navStart+2));
				view.addIcon(navIconEmpty(navStart+3));
				view.addIcon(navIconClose(navStart+4));
				view.addIcon(navIconEmpty(navStart+5));
				view.addIcon(navIconEmpty(navStart+6));
				view.addIcon(navIconEmpty(navStart+7));
				view.addIcon(navIconNext(navStart+8));
				break;
			default:
				break;
		}
		
		view.updateIcons();
		
	}
	
	private InfoIcon navIconScrollLeft(int index) {
		return new InfoIcon(ChatColor.GOLD+"Scroll Left",Collections.emptyList(),Material.DIRT,index,true);
	}
	
	private InfoIcon navIconScrollRight(int index) {
		return new InfoIcon(ChatColor.GOLD+"Scroll Right",Collections.emptyList(),Material.DIRT,index,true);
	}
	
	private InfoIcon navIconScrollUp(int index) {
		return new InfoIcon(ChatColor.GOLD+"Scroll Up",Collections.emptyList(),Material.DIRT,index,true);
	}
	
	private InfoIcon navIconScrollDown(int index) {
		return new InfoIcon(ChatColor.GOLD+"Scroll Down",Collections.emptyList(),Material.DIRT,index,true);
	}
	
	private InfoIcon navIconCreate(int index) {
		return new InfoIcon(ChatColor.GOLD+"Create",Collections.emptyList(),Material.DIRT,index,true);
	}
	
	private InfoIcon navIconDelete(int index) {
		return new InfoIcon(ChatColor.GOLD+"Delete",Collections.emptyList(),Material.DIRT,index,true);
	}
	
	private InfoIcon navIconEdit(int index) {
		return new InfoIcon(ChatColor.GOLD+"Edit",Collections.emptyList(),Material.DIRT,index,true);
	}
	
	private InfoIcon navIconReturn(int index) {
		return new InfoIcon(ChatColor.GOLD+"Return",Collections.emptyList(),Material.DIRT,index,true);
	}
	
	private InfoIcon navIconFinish(int index) {
		return new InfoIcon(ChatColor.GOLD+"Finish",Collections.emptyList(),Material.DIRT,index,true);
	}
	
	private InfoIcon navIconClose(int index) {
		return new InfoIcon(ChatColor.GOLD+"Close",Collections.emptyList(),Material.STRUCTURE_VOID,index,true);
	}
	
	private InfoIcon navIconBack(int index) {
		return new InfoIcon(ChatColor.GOLD+"Back",Collections.emptyList(),Material.ENDER_PEARL,index,true);
	}
	
	private InfoIcon navIconNext(int index) {
		return new InfoIcon(ChatColor.GOLD+"Next",Collections.emptyList(),Material.ENDER_PEARL,index,true);
	}
	
	private InfoIcon navIconEmpty(int index) {
		return new InfoIcon(" ",Collections.emptyList(),Material.GRAY_STAINED_GLASS_PANE,index,false);
	}
	
}
