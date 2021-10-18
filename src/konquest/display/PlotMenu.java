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
	private ArrayList<DisplayMenu> playerPages;
	private PlotState currentPlotState;
	
	public PlotMenu(KonTown town, Location playerLoc) {
		this.town = town;
		this.playerLoc = playerLoc;
		this.center = Konquest.toPoint(playerLoc);
		this.origin = center;
		this.views = new HashMap<PlotState,DisplayMenu>();
		this.playerPages = new ArrayList<DisplayMenu>();
		this.currentPlotState = PlotState.ROOT;
		initializeMenu();
		renderMenuViews();
	}
	
	private void initializeMenu() {
		
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
	
	/*
	 * Render default views
	 */
	private void renderMenuViews() {
		DisplayMenu renderView;
		InfoIcon icon;
		
		/* Root View */
		renderView = createLandView(center,PlotState.ROOT);
		views.put(PlotState.ROOT, renderView);
		origin = center;
		refreshNavigationButtons(PlotState.ROOT);
		
		/* Edit View */
		renderView = new DisplayMenu(2, getTitle(PlotState.EDIT));
		icon = new InfoIcon(ChatColor.GREEN+"Add Land", Collections.emptyList(), Material.GRASS_BLOCK, 1, true);
		renderView.addIcon(icon);
		icon = new InfoIcon(ChatColor.RED+"Remove Land", Collections.emptyList(), Material.STONE, 3, true);
		renderView.addIcon(icon);
		icon = new InfoIcon(ChatColor.GREEN+"Add Players", Collections.emptyList(), Material.PLAYER_HEAD, 5, true);
		renderView.addIcon(icon);
		icon = new InfoIcon(ChatColor.RED+"Remove Players", Collections.emptyList(), Material.CREEPER_HEAD, 7, true);
		renderView.addIcon(icon);
		views.put(PlotState.EDIT, renderView);
		refreshNavigationButtons(PlotState.EDIT);
	}
	
	private DisplayMenu createLandView(Point origin, PlotState context) {
		DisplayMenu result = new DisplayMenu(6, getTitle(context));
		List<String> loreList;
		InfoIcon icon;
		Point drawPoint;
		int index = 0;
		for(int r = -2; r <= 2; r++) {
			for(int c = -4; c <= 4; c++) {
				//ChatUtil.printDebug("Land View index "+index+", row "+r+", column "+c);
				// Iterate over all land tiles
				drawPoint = new Point(origin.x + c, origin.y + r);
				if(town.getChunkList().containsKey(drawPoint)) {
					loreList = new ArrayList<String>();
			    	loreList.add("Test");
					Material landMat = Material.GREEN_STAINED_GLASS_PANE;
					if(Konquest.toPoint(playerLoc).equals(drawPoint)) {
						landMat = Material.PLAYER_HEAD;
						loreList.add("You Are Here");
						//ChatUtil.printDebug("  This is player");
					}
					if(Konquest.toPoint(town.getCenterLoc()).equals(drawPoint)) {
						landMat = Material.IRON_BARS;
						loreList.add("Town Monument");
						//ChatUtil.printDebug("  This is monument");
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
			int index = slot-navMinIndex;
			switch(currentPlotState) {
			case ROOT:
				// Scroll arrows [0,1,2,3], close [4], create [5], delete [6], edit [7]
				if(index >= 0 && index <= 3) {
					result = scrollView(index,PlotState.ROOT);
				} else if(index == 4) {
					// Close
					result = null;
				} else if(index == 5) {
					// Create
					currentPlotState = PlotState.ROOT_CREATE;
					result = createLandView(origin,currentPlotState);
					views.put(currentPlotState, result);
				} else if(index == 6) {
					// Delete
					currentPlotState = PlotState.ROOT_DELETE;
					result = createLandView(origin,currentPlotState);
					views.put(currentPlotState, result);
				} else if(index == 7) {
					// Edit
					currentPlotState = PlotState.ROOT_EDIT;
					result = createLandView(origin,currentPlotState);
					views.put(currentPlotState, result);
				}
				break;
			case ROOT_CREATE:
				// Scroll arrows [0,1,2,3], close [4], return [5]
				if(index >= 0 && index <= 3) {
					result = scrollView(index,PlotState.ROOT_CREATE);
				} else if(index == 4) {
					// Close
					result = null;
				} else if(index == 5) {
					// Return
					currentPlotState = PlotState.ROOT;
					result = createLandView(origin,currentPlotState);
					views.put(currentPlotState, result);
				}
				break;
			case ROOT_DELETE:
				// Scroll arrows [0,1,2,3], close [4], return [5]
				if(index >= 0 && index <= 3) {
					result = scrollView(index,PlotState.ROOT_DELETE);
				} else if(index == 4) {
					// Close
					result = null;
				} else if(index == 5) {
					// Return
					currentPlotState = PlotState.ROOT;
					result = createLandView(origin,currentPlotState);
					views.put(currentPlotState, result);
				}
				break;
			case ROOT_EDIT:
				// Scroll arrows [0,1,2,3], close [4], return [5]
				if(index >= 0 && index <= 3) {
					result = scrollView(index,PlotState.ROOT_EDIT);
				} else if(index == 4) {
					// Close
					result = null;
				} else if(index == 5) {
					// Return
					currentPlotState = PlotState.ROOT;
					result = createLandView(origin,currentPlotState);
					views.put(currentPlotState, result);
				}
				break;
			case EDIT:
				// Close [4], return [5]
				if(index == 4) {
					// Close
					result = null;
				} else if(index == 5) {
					// Return
					currentPlotState = PlotState.ROOT_EDIT;
					result = views.get(currentPlotState);
				}
				break;
			case EDIT_LAND_ADD:
				// Scroll arrows [0,1,2,3], close [4], return [5], finish [6]
				if(index >= 0 && index <= 3) {
					result = scrollView(index,PlotState.EDIT_LAND_ADD);
				} else if(index == 4) {
					// Close
					result = null;
				} else if(index == 5) {
					// Return
					currentPlotState = PlotState.EDIT;
					result = views.get(currentPlotState);
				} else if(index == 6) {
					// Finish
					result = null;
					//TODO
				}
				break;
			case EDIT_LAND_REMOVE:
				// Scroll arrows [0,1,2,3], close [4], return [5], finish [6]
				if(index >= 0 && index <= 3) {
					result = scrollView(index,PlotState.EDIT_LAND_REMOVE);
				} else if(index == 4) {
					// Close
					result = null;
				} else if(index == 5) {
					// Return
					currentPlotState = PlotState.EDIT;
					result = views.get(currentPlotState);
				} else if(index == 6) {
					// Finish
					result = null;
					//TODO
				}
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
			
		} else if(slot < navMinIndex) {
			// Click in non-navigation slot
			
		}
		refreshNavigationButtons(currentPlotState);
		return result;
	}
	
	private DisplayMenu scrollView(int index, PlotState context) {
		DisplayMenu result = null;
		if(index == 0) {
			// Scroll left
			origin = new Point(origin.x - 1, origin.y + 0);
			result = createLandView(origin,context);
			views.put(context, result);
		} else if(index == 1) {
			// Scroll right
			origin = new Point(origin.x + 1, origin.y + 0);
			result = createLandView(origin,context);
			views.put(context, result);
		}  else if(index == 2) {
			// Scroll up
			origin = new Point(origin.x + 0, origin.y - 1);
			result = createLandView(origin,context);
			views.put(context, result);
		} else if(index == 3) {
			// Scroll down
			origin = new Point(origin.x + 0, origin.y + 1);
			result = createLandView(origin,context);
			views.put(context, result);
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
				view.addIcon(navIconScrollRight(navStart+1));
				view.addIcon(navIconScrollUp(navStart+2));
				view.addIcon(navIconScrollDown(navStart+3));
				view.addIcon(navIconClose(navStart+4));
				view.addIcon(navIconCreate(navStart+5));
				view.addIcon(navIconDelete(navStart+6));
				view.addIcon(navIconEdit(navStart+7));
				view.addIcon(navIconEmpty(navStart+8));
				break;
			case ROOT_CREATE:
				// Scroll arrows [0,1,2,3], close [4], return [5]
				view.addIcon(navIconScrollLeft(navStart+0));
				view.addIcon(navIconScrollRight(navStart+1));
				view.addIcon(navIconScrollUp(navStart+2));
				view.addIcon(navIconScrollDown(navStart+3));
				view.addIcon(navIconClose(navStart+4));
				view.addIcon(navIconReturn(navStart+5));
				view.addIcon(navIconEmpty(navStart+6));
				view.addIcon(navIconEmpty(navStart+7));
				view.addIcon(navIconEmpty(navStart+8));
				break;
			case ROOT_DELETE:
				// Scroll arrows [0,1,2,3], close [4], return [5]
				view.addIcon(navIconScrollLeft(navStart+0));
				view.addIcon(navIconScrollRight(navStart+1));
				view.addIcon(navIconScrollUp(navStart+2));
				view.addIcon(navIconScrollDown(navStart+3));
				view.addIcon(navIconClose(navStart+4));
				view.addIcon(navIconReturn(navStart+5));
				view.addIcon(navIconEmpty(navStart+6));
				view.addIcon(navIconEmpty(navStart+7));
				view.addIcon(navIconEmpty(navStart+8));
				break;
			case ROOT_EDIT:
				// Scroll arrows [0,1,2,3], close [4], return [5]
				view.addIcon(navIconScrollLeft(navStart+0));
				view.addIcon(navIconScrollRight(navStart+1));
				view.addIcon(navIconScrollUp(navStart+2));
				view.addIcon(navIconScrollDown(navStart+3));
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
				view.addIcon(navIconScrollRight(navStart+1));
				view.addIcon(navIconScrollUp(navStart+2));
				view.addIcon(navIconScrollDown(navStart+3));
				view.addIcon(navIconClose(navStart+4));
				view.addIcon(navIconReturn(navStart+5));
				view.addIcon(navIconFinish(navStart+6));
				view.addIcon(navIconEmpty(navStart+7));
				view.addIcon(navIconEmpty(navStart+8));
				break;
			case EDIT_LAND_REMOVE:
				// Scroll arrows [0,1,2,3], close [4], return [5], finish [6]
				view.addIcon(navIconScrollLeft(navStart+0));
				view.addIcon(navIconScrollRight(navStart+1));
				view.addIcon(navIconScrollUp(navStart+2));
				view.addIcon(navIconScrollDown(navStart+3));
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
	
	private String getTitle(PlotState context) {
		String result = "";
		switch(context) {
			case ROOT:
				result = ChatColor.BLACK+town.getName()+" Plots";
				break;
			case ROOT_CREATE:
				result = ChatColor.BLACK+town.getName()+" Create Plot";
				break;
			case ROOT_DELETE:
				result = ChatColor.BLACK+town.getName()+" Delete Plot";
				break;
			case ROOT_EDIT:
				result = ChatColor.BLACK+town.getName()+" Edit Plot";
				break;
			case EDIT:
				result = ChatColor.BLACK+town.getName()+" Edit Options";
				break;
			case EDIT_LAND_ADD:
				result = ChatColor.BLACK+town.getName()+" Add Land";
				break;
			case EDIT_LAND_REMOVE:
				result = ChatColor.BLACK+town.getName()+" Remove Land";
				break;
			case EDIT_PLAYER_ADD:
				result = ChatColor.BLACK+town.getName()+" Add Players";
				break;
			case EDIT_PLAYER_REMOVE:
				result = ChatColor.BLACK+town.getName()+" Remove Players";
				break;
			default:
				break;
		}
		return result;
	}
	
	private InfoIcon navIconScrollLeft(int index) {
		return new InfoIcon(ChatColor.GOLD+"\u25C0",Collections.emptyList(),Material.STONE_BUTTON,index,true);
	}
	
	private InfoIcon navIconScrollRight(int index) {
		return new InfoIcon(ChatColor.GOLD+"\u25B6",Collections.emptyList(),Material.STONE_BUTTON,index,true);
	}
	
	private InfoIcon navIconScrollUp(int index) {
		return new InfoIcon(ChatColor.GOLD+"\u25B2",Collections.emptyList(),Material.STONE_BUTTON,index,true);
	}
	
	private InfoIcon navIconScrollDown(int index) {
		return new InfoIcon(ChatColor.GOLD+"\u25BC",Collections.emptyList(),Material.STONE_BUTTON,index,true);
	}
	
	private InfoIcon navIconCreate(int index) {
		return new InfoIcon(ChatColor.GOLD+"Create",Collections.emptyList(),Material.OAK_SAPLING,index,true);
	}
	
	private InfoIcon navIconDelete(int index) {
		return new InfoIcon(ChatColor.GOLD+"Delete",Collections.emptyList(),Material.BARRIER,index,true);
	}
	
	private InfoIcon navIconEdit(int index) {
		return new InfoIcon(ChatColor.GOLD+"Edit",Collections.emptyList(),Material.WRITABLE_BOOK,index,true);
	}
	
	private InfoIcon navIconReturn(int index) {
		return new InfoIcon(ChatColor.GOLD+"Return",Collections.emptyList(),Material.FIREWORK_ROCKET,index,true);
	}
	
	private InfoIcon navIconFinish(int index) {
		return new InfoIcon(ChatColor.GOLD+"Finish",Collections.emptyList(),Material.WRITTEN_BOOK,index,true);
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
