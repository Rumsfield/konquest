package konquest.display;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.display.PlayerIcon.PlayerIconAction;
import konquest.model.KonPlot;
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
	/*
	 * State Flow
	 * ROOT -> ROOT_CREATE -> EDIT_LAND_ADD -> EDIT_PLAYER_ADD -> (Finish)
	 * ROOT -> ROOT_DELETE -> (Finish)
	 * ROOT -> ROOT_EDIT -> EDIT -> EDIT_LAND_ADD -> (Finish)
	 * ROOT -> ROOT_EDIT -> EDIT -> EDIT_LAND_REMOVE -> (Finish)
	 * ROOT -> ROOT_EDIT -> EDIT -> EDIT_PLAYER_ADD -> (Finish)
	 * ROOT -> ROOT_EDIT -> EDIT -> EDIT_PLAYER_REMOVE -> (Finish)
	 * 
	 * Plot edits are done to temporary plot object and committed to town upon finish.
	 */
	
	private KonTown town;
	private Point center;
	private Point origin;
	private Player bukkitPlayer;
	private Location playerLoc;
	private HashMap<PlotState,DisplayMenu> views;
	private ArrayList<DisplayMenu> playerPages;
	private int currentPlayerPage;
	private PlotState currentPlotState;
	private KonPlot tempPlot = null;
	
	public PlotMenu(KonTown town, Player bukkitPlayer) {
		this.town = town;
		this.bukkitPlayer = bukkitPlayer;
		this.playerLoc = bukkitPlayer.getLocation();;
		this.center = Konquest.toPoint(playerLoc);
		this.origin = center;
		this.views = new HashMap<PlotState,DisplayMenu>();
		this.playerPages = new ArrayList<DisplayMenu>();
		this.currentPlayerPage = 0;
		this.currentPlotState = PlotState.ROOT;
		initializeMenu();
		renderMenuViews();
	}
	
	private void initializeMenu() {
		// Create empty display views by default
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
				// Iterate over all land tiles
				drawPoint = new Point(origin.x + c, origin.y + r);
				if(town.getChunkList().containsKey(drawPoint)) {
					loreList = new ArrayList<String>();
			    	boolean isClickable = false;
					Material landMat = Material.GREEN_STAINED_GLASS_PANE;
					String landTitle = ChatColor.GREEN+"Town Land";
					// Render plots
					if(town.hasPlot(drawPoint, town.getWorld()) || (tempPlot != null && tempPlot.hasPoint(drawPoint))) {
						// This tile is part of a plot. Change display attributes...
						KonPlot drawPlot = town.getPlot(drawPoint, town.getWorld());
						landMat = Material.MAGENTA_STAINED_GLASS_PANE;
						landTitle = ChatColor.LIGHT_PURPLE+"Plot";
						if(tempPlot.hasPoint(drawPoint)) {
							landMat = Material.GLASS_PANE;
							landTitle = ChatColor.WHITE+"Editing Plot";
							drawPlot = tempPlot;
						}
						// Display plot player list in lore
						if(drawPlot != null) {
							List<OfflinePlayer> users = drawPlot.getUsers();
							for(int n = 0; n < 4; n++) {
								if(n < 3 && n < users.size()) {
									loreList.add(users.get(n).getName());
								} else if(n == 3 && n == users.size()-1) {
									loreList.add(users.get(n).getName());
								} else if(n == 3 && n < users.size()) {
									int remaining = users.size() - 3;
									loreList.add("and "+remaining+" more...");
								}
							}
						} else {
							loreList.add(ChatColor.DARK_RED+"Invalid Plot");
						}
						if(context.equals(PlotState.ROOT_CREATE) ||
								context.equals(PlotState.ROOT_DELETE) || 
								context.equals(PlotState.ROOT_EDIT) ||
								context.equals(PlotState.EDIT_LAND_REMOVE)) {
							isClickable = true;
							loreList.add(ChatColor.GOLD+"Click");
						}
					} else {
						if(context.equals(PlotState.EDIT_LAND_ADD)) {
							isClickable = true;
							loreList.add(ChatColor.GOLD+"Click");
						}
					}
					if(Konquest.toPoint(playerLoc).equals(drawPoint)) {
						landMat = Material.PLAYER_HEAD;
						loreList.add("You Are Here");
					}
					if(Konquest.toPoint(town.getCenterLoc()).equals(drawPoint)) {
						landMat = Material.OBSIDIAN;
						loreList.add("Town Monument");
					}
					icon = new InfoIcon(landTitle,loreList,landMat,index,isClickable);
					result.addIcon(icon);
				}
				index++;
			}
		}
		return result;
	}
	
	private DisplayMenu createPlayerView(KonPlot plot, PlotState context) {
		if(plot == null) {
			return null;
		}
		DisplayMenu result = null;
		final int MAX_ICONS_PER_PAGE = 45;
		playerPages.clear();
		currentPlayerPage = 0;
		String loreStr = "";
		boolean isClickable = false;
		// Determine list of players for this paged view
		List<OfflinePlayer> players = new ArrayList<OfflinePlayer>();
		if(context.equals(PlotState.EDIT_PLAYER_ADD)) {
			players.addAll(town.getPlayerResidents());
			players.removeAll(plot.getUsers());
			loreStr = "Click to add player";
			isClickable = true;
		} else if(context.equals(PlotState.EDIT_PLAYER_REMOVE)) {
			players.addAll(plot.getUsers());
			loreStr = "Click to remove player";
			isClickable = true;
		} else {
			return null;
		}
		// Create page(s)
		String pageLabel = "";
		List<String> loreList;
		int pageTotal = (int)Math.ceil(((double)players.size())/MAX_ICONS_PER_PAGE);
		if(pageTotal == 0) {
			pageTotal = 1;
		}
		int pageNum = 0;
		ListIterator<OfflinePlayer> playerIter = players.listIterator();
		for(int i = 0; i < pageTotal; i++) {
			int numPageRows = (int)Math.ceil(((double)((players.size() - i*MAX_ICONS_PER_PAGE) % MAX_ICONS_PER_PAGE))/9);
			if(numPageRows == 0) {
				numPageRows = 1;
			}
			pageLabel = getTitle(context)+" "+(i+1)+"/"+pageTotal;
			playerPages.add(pageNum, new DisplayMenu(numPageRows+1, pageLabel));
			int slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && playerIter.hasNext()) {
				/* Player Icon (n) */
				OfflinePlayer currentPlayer = playerIter.next();
				loreList = new ArrayList<String>();
				loreList.add(loreStr);
		    	PlayerIcon player = new PlayerIcon(currentPlayer.getName(),loreList,currentPlayer,slotIndex,isClickable,PlayerIconAction.DISPLAY_INFO);
				playerPages.get(pageNum).addIcon(player);
				slotIndex++;
			}
			pageNum++;
		}
		result = playerPages.get(currentPlayerPage);
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
			ChatUtil.printDebug("Plot Menu navigation update: State "+currentPlotState.toString()+", Index "+index);
			switch(currentPlotState) {
				case ROOT:
					// Scroll arrows [0,1,2,3], close [4], create [5], delete [6], edit [7]
					if(index >= 0 && index <= 3) {
						result = scrollView(index);
					} else if(index == 4) {
						// Close
						result = null;
					} else if(index == 5) {
						// Create
						result = goToState(PlotState.ROOT_CREATE);
					} else if(index == 6) {
						// Delete
						result = goToState(PlotState.ROOT_DELETE);
					} else if(index == 7) {
						// Edit
						result = goToState(PlotState.ROOT_EDIT);
					}
					break;
				case ROOT_CREATE:
					// Scroll arrows [0,1,2,3], close [4], return [5]
					if(index >= 0 && index <= 3) {
						result = scrollView(index);
					} else if(index == 4) {
						// Close
						result = null;
					} else if(index == 5) {
						// Return
						tempPlot = null;
						result = goToState(PlotState.ROOT);
					}
					break;
				case ROOT_DELETE:
					// Scroll arrows [0,1,2,3], close [4], return [5]
					if(index >= 0 && index <= 3) {
						result = scrollView(index);
					} else if(index == 4) {
						// Close
						result = null;
					} else if(index == 5) {
						// Return
						tempPlot = null;
						result = goToState(PlotState.ROOT);
					}
					break;
				case ROOT_EDIT:
					// Scroll arrows [0,1,2,3], close [4], return [5]
					if(index >= 0 && index <= 3) {
						result = scrollView(index);
					} else if(index == 4) {
						// Close
						result = null;
					} else if(index == 5) {
						// Return
						tempPlot = null;
						result = goToState(PlotState.ROOT);
					}
					break;
				case EDIT:
					// Close [4], return [5]
					if(index == 4) {
						// Close
						result = null;
					} else if(index == 5) {
						// Return
						tempPlot = null;
						result = goToState(PlotState.ROOT_EDIT);
					}
					break;
				case EDIT_LAND_ADD:
					// Scroll arrows [0,1,2,3], close [4], return [5], finish [6]
					if(index >= 0 && index <= 3) {
						result = scrollView(index);
					} else if(index == 4) {
						// Close
						result = null;
					} else if(index == 5) {
						// Return
						result = goToState(PlotState.EDIT);
					} else if(index == 6) {
						// Finish
						result = null;
						town.setPlot(tempPlot);
						Konquest.playSuccessSound(bukkitPlayer);
					}
					break;
				case EDIT_LAND_REMOVE:
					// Scroll arrows [0,1,2,3], close [4], return [5], finish [6]
					if(index >= 0 && index <= 3) {
						result = scrollView(index);
					} else if(index == 4) {
						// Close
						result = null;
					} else if(index == 5) {
						// Return
						result = goToState(PlotState.EDIT);
					} else if(index == 6) {
						// Finish
						result = null;
						town.setPlot(tempPlot);
						Konquest.playSuccessSound(bukkitPlayer);
					}
					break;
				case EDIT_PLAYER_ADD:
					// (back [0]) close [4], return [5], finish [6], (next [8])
					if(index == 0) {
						result = goPageBack();
					} else if(index == 4) {
						// Close
						result = null;
					} else if(index == 5) {
						// Return
						result = goToState(PlotState.EDIT);
					} else if(index == 6) {
						// Finish
						result = null;
						town.setPlot(tempPlot);
						Konquest.playSuccessSound(bukkitPlayer);
					} else if(index == 8) {
						result = goPageNext();
					}
					break;
				case EDIT_PLAYER_REMOVE:
					// (back [0]) close [4], return [5], finish [6], (next [8])
					if(index == 0) {
						result = goPageBack();
					} else if(index == 4) {
						// Close
						result = null;
					} else if(index == 5) {
						// Return
						result = goToState(PlotState.EDIT);
					} else if(index == 6) {
						// Finish
						result = null;
						town.setPlot(tempPlot);
						Konquest.playSuccessSound(bukkitPlayer);
					} else if(index == 8) {
						result = goPageNext();
					}
					break;
				default:
					break;
			}
		} else if(slot < navMinIndex) {
			// Click in non-navigation slot
			Point clickPoint = slotToPoint(slot);
			MenuIcon clickedIcon = views.get(currentPlotState).getIcon(slot);
			ChatUtil.printDebug("Plot Menu view update: State "+currentPlotState.toString()+", Slot "+slot+", Point "+clickPoint.x+","+clickPoint.y);
			switch(currentPlotState) {
				case ROOT_CREATE:
					// Check for non-plot town land click, create new plot
					if(!town.hasPlot(clickPoint, town.getWorld())) {
						// Make a new plot with this point
						tempPlot = new KonPlot(clickPoint);
						result = goToState(PlotState.EDIT_LAND_ADD);
					}
					break;
				case ROOT_DELETE:
					// Check for plot town land click, delete existing plot
					if(town.hasPlot(clickPoint, town.getWorld())) {
						// Choose plot to remove
						tempPlot = town.getPlot(clickPoint,town.getWorld());
						result = null;
						town.removePlot(tempPlot);
						Konquest.playSuccessSound(bukkitPlayer);
					}
					break;
				case ROOT_EDIT:
					// Check for plot town land click, edit existing plot
					if(town.hasPlot(clickPoint, town.getWorld())) {
						// Choose plot to edit
						tempPlot = town.getPlot(clickPoint,town.getWorld());
						result = goToState(PlotState.EDIT);
					}
					break;
				case EDIT:
					// Check for edit action clicks
					// Add Land [1], Remove Land [3], Add Players [5], Remove Players [7]
					if(slot == 1) {
						// Add Land
						result = goToState(PlotState.EDIT_LAND_ADD);
					} else if(slot == 3) {
						// Remove Land
						result = goToState(PlotState.EDIT_LAND_REMOVE);
					} else if(slot == 5) {
						// Add Players
						result = goToState(PlotState.EDIT_PLAYER_ADD);
					} else if(slot == 7) {
						// Remove Players
						result = goToState(PlotState.EDIT_PLAYER_REMOVE);
					}
					break;
				case EDIT_LAND_ADD:
					// Check for non-plot town land click, add to chosen plot
					if(!town.hasPlot(clickPoint, town.getWorld()) && !tempPlot.hasPoint(clickPoint)) {
						// Add this point to the temp plot
						tempPlot.addPoint(clickPoint);
						result = goToState(PlotState.EDIT_LAND_ADD);
					}
					break;
				case EDIT_LAND_REMOVE:
					// Check for plot town land click, remove from chosen plot
					if(tempPlot.hasPoint(clickPoint)) {
						// Remove this point from the temp plot
						tempPlot.removePoint(clickPoint);
						result = goToState(PlotState.EDIT_LAND_REMOVE);
					}
					break;
				case EDIT_PLAYER_ADD:
					// Check for player icon click, add to chosen plot
					if(clickedIcon != null && clickedIcon instanceof PlayerIcon) {
						PlayerIcon icon = (PlayerIcon)clickedIcon;
						tempPlot.addUser(icon.getOfflinePlayer());
						result = goToState(PlotState.EDIT_PLAYER_ADD);
					}
					break;
				case EDIT_PLAYER_REMOVE:
					// Check for player icon click, remove from chosen plot
					if(clickedIcon != null && clickedIcon instanceof PlayerIcon) {
						PlayerIcon icon = (PlayerIcon)clickedIcon;
						tempPlot.removeUser(icon.getOfflinePlayer());
						result = goToState(PlotState.EDIT_PLAYER_REMOVE);
					}
					break;
				default:
					break;
			}
		}
		refreshNavigationButtons(currentPlotState);
		return result;
	}
	
	private DisplayMenu scrollView(int index) {
		DisplayMenu result = null;
		if(index == 0) {
			// Scroll left
			origin = new Point(origin.x - 1, origin.y + 0);
			result = createLandView(origin,currentPlotState);
			views.put(currentPlotState, result);
		} else if(index == 1) {
			// Scroll right
			origin = new Point(origin.x + 1, origin.y + 0);
			result = createLandView(origin,currentPlotState);
			views.put(currentPlotState, result);
		}  else if(index == 2) {
			// Scroll up
			origin = new Point(origin.x + 0, origin.y - 1);
			result = createLandView(origin,currentPlotState);
			views.put(currentPlotState, result);
		} else if(index == 3) {
			// Scroll down
			origin = new Point(origin.x + 0, origin.y + 1);
			result = createLandView(origin,currentPlotState);
			views.put(currentPlotState, result);
		}
		return result;
	}
	
	private DisplayMenu goToState(PlotState context) {
		DisplayMenu result = null;
		currentPlotState = context;
		switch(context) {
			case ROOT:
				result = createLandView(origin,currentPlotState);
				views.put(currentPlotState, result);
				break;
			case ROOT_CREATE:
				result = createLandView(origin,currentPlotState);
				views.put(currentPlotState, result);
				break;
			case ROOT_DELETE:
				result = createLandView(origin,currentPlotState);
				views.put(currentPlotState, result);
				break;
			case ROOT_EDIT:
				result = createLandView(origin,currentPlotState);
				views.put(currentPlotState, result);
				break;
			case EDIT:
				result = views.get(currentPlotState);
				break;
			case EDIT_LAND_ADD:
				result = createLandView(origin,currentPlotState);
				views.put(currentPlotState, result);
				break;
			case EDIT_LAND_REMOVE:
				result = createLandView(origin,currentPlotState);
				views.put(currentPlotState, result);
				break;
			case EDIT_PLAYER_ADD:
				result = createPlayerView(tempPlot,currentPlotState);
				views.put(currentPlotState, result);
				break;
			case EDIT_PLAYER_REMOVE:
				result = createPlayerView(tempPlot,currentPlotState);
				views.put(currentPlotState, result);
				break;
			default:
				break;
		}
		return result;
	}
	
	private DisplayMenu goPageBack() {
		DisplayMenu result = null;
		int newIndex = currentPlayerPage-1;
		if(newIndex >= 0) {
			currentPlayerPage = newIndex;
		}
		result = playerPages.get(currentPlayerPage);
		views.put(currentPlotState, result);
		return result;
	}
	
	private DisplayMenu goPageNext() {
		DisplayMenu result = null;
		int newIndex = currentPlayerPage+1;
		if(newIndex < playerPages.size()) {
			currentPlayerPage = newIndex;
		}
		result = playerPages.get(currentPlayerPage);
		views.put(currentPlotState, result);
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
				if(currentPlayerPage > 0) {
					// Place a back button
					view.addIcon(navIconBack(navStart+0));
				} else {
					view.addIcon(navIconEmpty(navStart+0));
				}
				if(currentPlayerPage < playerPages.size()-1) {
					// Place a next button
					view.addIcon(navIconNext(navStart+8));
				} else {
					view.addIcon(navIconEmpty(navStart+8));
				}
				view.addIcon(navIconEmpty(navStart+1));
				view.addIcon(navIconEmpty(navStart+2));
				view.addIcon(navIconEmpty(navStart+3));
				view.addIcon(navIconClose(navStart+4));
				view.addIcon(navIconReturn(navStart+5));
				view.addIcon(navIconFinish(navStart+6));
				view.addIcon(navIconEmpty(navStart+7));
				break;
			case EDIT_PLAYER_REMOVE:
				// (back [0]) close [4], return [5], finish [6], (next [8])
				if(currentPlayerPage > 0) {
					// Place a back button
					view.addIcon(navIconBack(navStart+0));
				} else {
					view.addIcon(navIconEmpty(navStart+0));
				}
				if(currentPlayerPage < playerPages.size()-1) {
					// Place a next button
					view.addIcon(navIconNext(navStart+8));
				} else {
					view.addIcon(navIconEmpty(navStart+8));
				}
				view.addIcon(navIconEmpty(navStart+1));
				view.addIcon(navIconEmpty(navStart+2));
				view.addIcon(navIconEmpty(navStart+3));
				view.addIcon(navIconClose(navStart+4));
				view.addIcon(navIconReturn(navStart+5));
				view.addIcon(navIconFinish(navStart+6));
				view.addIcon(navIconEmpty(navStart+7));
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
	
	private Point slotToPoint(int slot) {
		/*
		 * Slots:	
		 * 	0		1		2		3		4		5		6		7		8
		 * 	9		10		11		12		13		14		15		16		17
		 * 	18		19		20		21		22		23		24		25		26
		 * 	27		28		29		30		31		32		33		34		35
		 * 	36		37		38		39		40		41		42		43		44
		 * 
		 * Points (local diff)
		 * 	-4,-2	-3,-2	-2,-2	-1,-2	0,-2	1,-2	2,-2	3,-2	4,-2
		 * 	-4,-1	-3,-1	-2,-1	-1,-1	0,-1	1,-1	2,-1	3,-1	4,-1
		 * 	-4,0	-3,0	-2,0	-1,0	0,0		1,0		2,0		3,0		4,0
		 * 	-4,1	-3,1	-2,1	-1,1	0,1		1,1		2,1		3,1		4,1	
		 * 	-4,2	-3,2	-2,2	-1,2	0,2		1,2		2,2		3,2		4,2	
		 */
		int xDiff = (slot % 9) - 4;
		int yDiff = Math.floorDiv(slot, 9) - 2;
		Point result = new Point(origin.x + xDiff, origin.y + yDiff);
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
