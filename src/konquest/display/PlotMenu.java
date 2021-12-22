package konquest.display;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

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
import konquest.utility.MessagePath;

public class PlotMenu implements StateMenu {

	enum PlotState {
		ROOT,
		ROOT_CREATE,
		ROOT_DELETE,
		ROOT_EDIT,
		CREATE_LAND_ADD,
		CREATE_PLAYER_ADD,
		EDIT,
		EDIT_LAND_ADD,
		EDIT_LAND_REMOVE,
		EDIT_PLAYER_SHOW,
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
	private int maxSize;
	private Location playerLoc;
	private HashMap<PlotState,DisplayMenu> views;
	private ArrayList<DisplayMenu> playerPages;
	private int currentPlayerPage;
	private PlotState currentPlotState;
	private KonPlot editPlot = null;
	private KonPlot oldPlot = null;
	private final Material[] plotColors = {
			Material.MAGENTA_STAINED_GLASS_PANE,
			Material.CYAN_STAINED_GLASS_PANE,
			Material.BLUE_STAINED_GLASS_PANE,
			Material.ORANGE_STAINED_GLASS_PANE,
			Material.PINK_STAINED_GLASS_PANE,
			Material.YELLOW_STAINED_GLASS_PANE,
			Material.PURPLE_STAINED_GLASS_PANE,
			Material.LIGHT_BLUE_STAINED_GLASS_PANE
	};
	
	public PlotMenu(KonTown town, Player bukkitPlayer, int maxSize) {
		this.town = town;
		this.bukkitPlayer = bukkitPlayer;
		this.maxSize = maxSize;
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
		/*
		playerPages.add(new DisplayMenu(2, ChatColor.BLACK+town.getName()+" Player"));
		views.put(PlotState.ROOT, new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Plots"));
		views.put(PlotState.ROOT_CREATE, new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Create Plot"));
		views.put(PlotState.ROOT_DELETE, new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Delete Plot"));
		views.put(PlotState.ROOT_EDIT, new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Edit Plot"));
		views.put(PlotState.CREATE_LAND_ADD, new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Add Land"));
		views.put(PlotState.CREATE_PLAYER_ADD, new DisplayMenu(2, ChatColor.BLACK+town.getName()+" Add Player"));
		views.put(PlotState.EDIT, new DisplayMenu(2, ChatColor.BLACK+town.getName()+" Edit"));
		views.put(PlotState.EDIT_LAND_ADD, new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Add Land"));
		views.put(PlotState.EDIT_LAND_REMOVE, new DisplayMenu(6, ChatColor.BLACK+town.getName()+" Remove Land"));
		views.put(PlotState.EDIT_PLAYER_ADD, new DisplayMenu(2, ChatColor.BLACK+town.getName()+" Add Player"));
		views.put(PlotState.EDIT_PLAYER_REMOVE, new DisplayMenu(2, ChatColor.BLACK+town.getName()+" Remove Player"));
		*/
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
		icon = new InfoIcon(ChatColor.GREEN+MessagePath.MENU_PLOTS_EDIT_ADD_LAND.getMessage(), Collections.emptyList(), Material.GRASS_BLOCK, 1, true);
		renderView.addIcon(icon);
		icon = new InfoIcon(ChatColor.RED+MessagePath.MENU_PLOTS_EDIT_REMOVE_LAND.getMessage(), Collections.emptyList(), Material.COARSE_DIRT, 3, true);
		renderView.addIcon(icon);
		icon = new InfoIcon(ChatColor.GREEN+MessagePath.MENU_PLOTS_EDIT_ADD_PLAYERS.getMessage(), Collections.emptyList(), Material.PLAYER_HEAD, 5, true);
		renderView.addIcon(icon);
		icon = new InfoIcon(ChatColor.GOLD+MessagePath.MENU_PLOTS_EDIT_SHOW_PLAYERS.getMessage(), Collections.emptyList(), Material.PAINTING, 6, true);
		renderView.addIcon(icon);
		icon = new InfoIcon(ChatColor.RED+MessagePath.MENU_PLOTS_EDIT_REMOVE_PLAYERS.getMessage(), Collections.emptyList(), Material.SKELETON_SKULL, 7, true);
		renderView.addIcon(icon);
		views.put(PlotState.EDIT, renderView);
		refreshNavigationButtons(PlotState.EDIT);
	}
	
	private DisplayMenu createLandView(Point origin, PlotState context) {
		DisplayMenu result = new DisplayMenu(6, getTitle(context));
		List<String> loreList;
		InfoIcon icon;
		Point drawPoint;
		KonPlot drawPlot;
		int colorSelect = 0;
		HashMap<KonPlot,Material> plotColorMap = new HashMap<KonPlot,Material>();
		int index = 0;
		for(int r = -2; r <= 2; r++) {
			for(int c = -4; c <= 4; c++) {
				// Iterate over all land tiles
				drawPoint = new Point(origin.x + c, origin.y + r);
				drawPlot = null;
				if(town.getChunkList().containsKey(drawPoint)) {
					// This tile is claimed by the town
					loreList = new ArrayList<String>();
					//loreList.add(ChatColor.GOLD+""+drawPoint.x+","+drawPoint.y);
			    	boolean isClickable = false;
					Material landMat = Material.GREEN_STAINED_GLASS_PANE;
					String landTitle = ChatColor.GREEN+MessagePath.MENU_PLOTS_TOWN_LAND.getMessage()+" | "+drawPoint.x+","+drawPoint.y;
					boolean isPlot = false;
					// Render all town land, then plots, then remove oldPlot if exist, then render editPlot if exist.
					// Draw different plots in a sequence of stained glass pane colors.
					if(town.hasPlot(drawPoint, town.getWorld())) {
						drawPlot = town.getPlot(drawPoint, town.getWorld());
						// Determine plot color
						if(plotColorMap.containsKey(drawPlot)) {
							// Use chosen color
							landMat = plotColorMap.get(drawPlot);
						} else {
							// Choose new color in sequence
							landMat = plotColors[colorSelect];
							plotColorMap.put(drawPlot, plotColors[colorSelect]);
							colorSelect++;
							if(colorSelect >= plotColors.length) {
								colorSelect = 0;
							}
						}
						landTitle = ChatColor.LIGHT_PURPLE+MessagePath.LABEL_PLOT.getMessage()+" | "+drawPoint.x+","+drawPoint.y;
						// Display plot player list in lore
						if(drawPlot != null) {
							List<OfflinePlayer> users = drawPlot.getUserOfflinePlayers();
							for(int n = 0; n < 4; n++) {
								if(n < 3 && n < users.size()) {
									loreList.add(users.get(n).getName());
								} else if(n == 3 && n == users.size()-1) {
									loreList.add(users.get(n).getName());
								} else if(n == 3 && n < users.size()) {
									int remaining = users.size() - 3;
									loreList.add(remaining+"+");
								}
							}
						} else {
							loreList.add(ChatColor.DARK_RED+"Invalid Plot");
						}
						isPlot = true;
					}
					// Remove plot render if tile belongs to oldPlot
					if(oldPlot != null && oldPlot.hasPoint(drawPoint)) {
						landMat = Material.GREEN_STAINED_GLASS_PANE;
						landTitle = ChatColor.GREEN+MessagePath.MENU_PLOTS_TOWN_LAND.getMessage()+" | "+drawPoint.x+","+drawPoint.y;
						loreList.clear();
						isPlot = false;
					}
					// Add render for editPloy
					if(editPlot != null && editPlot.hasPoint(drawPoint)) {
						landMat = Material.GLASS_PANE;
						landTitle = ChatColor.WHITE+MessagePath.MENU_PLOTS_EDITING_PLOT.getMessage()+" | "+drawPoint.x+","+drawPoint.y;
						isPlot = true;
					}
					// Add context tips to lore
					if(isPlot) {
						if(context.equals(PlotState.ROOT_DELETE)) {
							isClickable = true;
							loreList.add(ChatColor.GOLD+MessagePath.MENU_PLOTS_CLICK_DELETE.getMessage());
						} else if(context.equals(PlotState.ROOT_EDIT)) {
							isClickable = true;
							loreList.add(ChatColor.GOLD+MessagePath.MENU_PLOTS_CLICK_EDIT.getMessage());
						} else if(context.equals(PlotState.EDIT_LAND_REMOVE)) {
							isClickable = true;
							loreList.add(ChatColor.GOLD+MessagePath.MENU_PLOTS_CLICK_REMOVE_CHUNK.getMessage());
						}
					} else {
						if(context.equals(PlotState.ROOT_CREATE)) {
							isClickable = true;
							loreList.add(ChatColor.GOLD+MessagePath.MENU_PLOTS_CLICK_CREATE.getMessage());
						} else if(context.equals(PlotState.EDIT_LAND_ADD) || context.equals(PlotState.CREATE_LAND_ADD)) {
							isClickable = true;
							loreList.add(ChatColor.GOLD+MessagePath.MENU_PLOTS_CLICK_ADD_CHUNK.getMessage());
						}
					}
					// Check for monument chunk
					if(Konquest.toPoint(town.getCenterLoc()).equals(drawPoint)) {
						isClickable = false;
						loreList.clear();
						loreList.add(ChatColor.YELLOW+MessagePath.MENU_PLOTS_TOWN_MONUMENT.getMessage());
						landMat = Material.OBSIDIAN;
					}
					// Add other info to lore
					if(Konquest.toPoint(playerLoc).equals(drawPoint)) {
						//landMat = Material.PLAYER_HEAD;
						loreList.add(ChatColor.YELLOW+MessagePath.MENU_PLOTS_HERE.getMessage());
					}
					// Build icon and add to menu view
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
		if(context.equals(PlotState.EDIT_PLAYER_ADD) || context.equals(PlotState.CREATE_PLAYER_ADD)) {
			players.addAll(town.getPlayerResidents());
			players.removeAll(plot.getUserOfflinePlayers());
			loreStr = ChatColor.GOLD+MessagePath.MENU_PLOTS_CLICK_ADD_PLAYER.getMessage();
			isClickable = true;
		} else if(context.equals(PlotState.EDIT_PLAYER_REMOVE)) {
			players.addAll(plot.getUserOfflinePlayers());
			loreStr = ChatColor.GOLD+MessagePath.MENU_PLOTS_CLICK_REMOVE_PLAYER.getMessage();
			isClickable = true;
		} else if(context.equals(PlotState.EDIT_PLAYER_SHOW)) {
			players.addAll(plot.getUserOfflinePlayers());
			loreStr = ChatColor.GOLD+MessagePath.MENU_PLOTS_CLICK_MOVE_PLAYER.getMessage();
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
			int numPageRows = (int)Math.ceil(((double)(players.size() - i*MAX_ICONS_PER_PAGE))/9);
			if(numPageRows < 1) {
				numPageRows = 1;
			} else if(numPageRows > 5) {
				numPageRows = 5;
			}
			pageLabel = getTitle(context)+" "+(i+1)+"/"+pageTotal;
			playerPages.add(pageNum, new DisplayMenu(numPageRows+1, pageLabel));
			int slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && playerIter.hasNext()) {
				/* Player Icon (n) */
				OfflinePlayer currentPlayer = playerIter.next();
				loreList = new ArrayList<String>();
				loreList.add(loreStr);
		    	PlayerIcon player = new PlayerIcon(ChatColor.GREEN+currentPlayer.getName(),loreList,currentPlayer,slotIndex,isClickable,PlayerIconAction.DISPLAY_INFO);
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
			//ChatUtil.printDebug("Plot Menu navigation update: State "+currentPlotState.toString()+", Index "+index);
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
						editPlot = null;
						oldPlot = null;
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
						editPlot = null;
						oldPlot = null;
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
						editPlot = null;
						oldPlot = null;
						result = goToState(PlotState.ROOT);
					}
					break;
				case CREATE_LAND_ADD:
					// Scroll arrows [0,1,2,3], close [4], return [5], finish [6]
					if(index >= 0 && index <= 3) {
						result = scrollView(index);
					} else if(index == 4) {
						// Close
						result = null;
					} else if(index == 5) {
						// Return
						editPlot = null;
						oldPlot = null;
						result = goToState(PlotState.ROOT_CREATE);
					} else if(index == 6) {
						// Finish
						result = goToState(PlotState.CREATE_PLAYER_ADD);
					}
					break;
				case CREATE_PLAYER_ADD:
					// (back [0]) close [4], return [5], finish [6], (next [8])
					if(index == 0) {
						result = goPageBack();
					} else if(index == 4) {
						// Close
						result = null;
					} else if(index == 5) {
						// Return
						if(editPlot != null) {
							editPlot.clearUsers();
						}
						result = goToState(PlotState.CREATE_LAND_ADD);
					} else if(index == 6) {
						// Finish
						result = null;
						commitPlot();
					} else if(index == 8) {
						result = goPageNext();
					}
					break;
				case EDIT:
					// Close [4], return [5]
					if(index == 4) {
						// Close
						result = null;
					} else if(index == 5) {
						// Return
						editPlot = null;
						oldPlot = null;
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
						commitPlot();
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
						commitPlot();
					}
					break;
				case EDIT_PLAYER_SHOW:
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
						commitPlot();
					} else if(index == 8) {
						result = goPageNext();
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
						commitPlot();
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
						commitPlot();
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
			//ChatUtil.printDebug("Plot Menu view update: State "+currentPlotState.toString()+", Slot "+slot+", Point "+clickPoint.x+","+clickPoint.y);
			switch(currentPlotState) {
				case ROOT_CREATE:
					// Check for non-plot town land click, create new plot
					if(!town.hasPlot(clickPoint, town.getWorld())) {
						// Make a new plot with this point
						oldPlot = null;
						editPlot = new KonPlot(clickPoint);
						result = goToState(PlotState.CREATE_LAND_ADD);
					}
					break;
				case ROOT_DELETE:
					// Check for plot town land click, delete existing plot
					if(town.hasPlot(clickPoint, town.getWorld())) {
						// Choose plot to remove
						oldPlot = town.getPlot(clickPoint,town.getWorld());
						editPlot = null;
						result = null;
						commitPlot();
					}
					break;
				case ROOT_EDIT:
					// Check for plot town land click, edit existing plot
					if(town.hasPlot(clickPoint, town.getWorld())) {
						// Choose plot to edit
						oldPlot = town.getPlot(clickPoint,town.getWorld());
						editPlot = oldPlot.clone();
						result = goToState(PlotState.EDIT);
					}
					break;
				case CREATE_LAND_ADD:
					// Check for non-plot town land click, add to chosen plot
					if(!town.hasPlot(clickPoint, town.getWorld()) && editPlot != null && !editPlot.hasPoint(clickPoint)) {
						if(maxSize < 1 || editPlot.getPoints().size() < maxSize) {
							// Add this point to the temp plot
							editPlot.addPoint(clickPoint);
						} else {
							Konquest.playFailSound(bukkitPlayer);
						}
						result = goToState(PlotState.CREATE_LAND_ADD);
					}
					break;
				case CREATE_PLAYER_ADD:
					// Check for player icon click, add to chosen plot
					if(clickedIcon != null && clickedIcon instanceof PlayerIcon && editPlot != null) {
						PlayerIcon icon = (PlayerIcon)clickedIcon;
						editPlot.addUser(icon.getOfflinePlayer());
						result = goToState(PlotState.CREATE_PLAYER_ADD);
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
					} else if(slot == 6) {
						// Add Players
						result = goToState(PlotState.EDIT_PLAYER_SHOW);
					} else if(slot == 7) {
						// Remove Players
						result = goToState(PlotState.EDIT_PLAYER_REMOVE);
					}
					break;
				case EDIT_LAND_ADD:
					// Check for non-plot town land click, add to chosen plot
					if(!town.hasPlot(clickPoint, town.getWorld()) && editPlot != null && !editPlot.hasPoint(clickPoint)) {
						if(maxSize < 1 || editPlot.getPoints().size() < maxSize) {
							// Add this point to the temp plot
							editPlot.addPoint(clickPoint);
						} else {
							Konquest.playFailSound(bukkitPlayer);
						}
						result = goToState(PlotState.EDIT_LAND_ADD);
					}
					break;
				case EDIT_LAND_REMOVE:
					// Check for plot town land click, remove from chosen plot
					if(editPlot != null && editPlot.hasPoint(clickPoint)) {
						if(editPlot.getPoints().size() > 1) {
							// Remove this point from the temp plot
							editPlot.removePoint(clickPoint);
						} else {
							Konquest.playFailSound(bukkitPlayer);
						}
						result = goToState(PlotState.EDIT_LAND_REMOVE);
					}
					break;
				case EDIT_PLAYER_ADD:
					// Check for player icon click, add to chosen plot
					if(clickedIcon != null && clickedIcon instanceof PlayerIcon && editPlot != null) {
						PlayerIcon icon = (PlayerIcon)clickedIcon;
						editPlot.addUser(icon.getOfflinePlayer());
						result = goToState(PlotState.EDIT_PLAYER_ADD);
					}
					break;
				case EDIT_PLAYER_SHOW:
					// Check for player icon click, move player to first index in user list
					if(clickedIcon != null && clickedIcon instanceof PlayerIcon && editPlot != null) {
						PlayerIcon icon = (PlayerIcon)clickedIcon;
						List<UUID> members = editPlot.getUsers();
						if(members.remove(icon.getOfflinePlayer().getUniqueId())) {
							editPlot.clearUsers();
							editPlot.addUser(icon.getOfflinePlayer());
							editPlot.addUsers(members);
						}
						result = goToState(PlotState.EDIT_PLAYER_SHOW);
					}
					break;
				case EDIT_PLAYER_REMOVE:
					// Check for player icon click, remove from chosen plot
					if(clickedIcon != null && clickedIcon instanceof PlayerIcon && editPlot != null) {
						PlayerIcon icon = (PlayerIcon)clickedIcon;
						editPlot.removeUser(icon.getOfflinePlayer());
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
			// Scroll up
			origin = new Point(origin.x + 0, origin.y - 1);
			result = createLandView(origin,currentPlotState);
			views.put(currentPlotState, result);
		} else if(index == 2) {
			// Scroll down
			origin = new Point(origin.x + 0, origin.y + 1);
			result = createLandView(origin,currentPlotState);
			views.put(currentPlotState, result);
		} else if(index == 3) {
			// Scroll right
			origin = new Point(origin.x + 1, origin.y + 0);
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
			case CREATE_LAND_ADD:
				result = createLandView(origin,currentPlotState);
				views.put(currentPlotState, result);
				break;
			case CREATE_PLAYER_ADD:
				result = createPlayerView(editPlot,currentPlotState);
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
			case EDIT_PLAYER_SHOW:
				result = createPlayerView(editPlot,currentPlotState);
				views.put(currentPlotState, result);
				break;
			case EDIT_PLAYER_ADD:
				result = createPlayerView(editPlot,currentPlotState);
				views.put(currentPlotState, result);
				break;
			case EDIT_PLAYER_REMOVE:
				result = createPlayerView(editPlot,currentPlotState);
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
	
	private void commitPlot() {
		// Attempt to remove old plot
		if(oldPlot != null) {
			town.removePlot(oldPlot);
		}
		// Attempt to put new plot
		if(editPlot != null) {
			town.putPlot(editPlot);
		}
		Konquest.playSuccessSound(bukkitPlayer);
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
		if(context.equals(PlotState.ROOT)) {
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
		} else if(context.equals(PlotState.ROOT_CREATE) || context.equals(PlotState.ROOT_DELETE) || context.equals(PlotState.ROOT_EDIT)) {
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
		} else if(context.equals(PlotState.CREATE_LAND_ADD) || context.equals(PlotState.EDIT_LAND_ADD) || context.equals(PlotState.EDIT_LAND_REMOVE)) {
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
		} else if(context.equals(PlotState.CREATE_PLAYER_ADD) || context.equals(PlotState.EDIT_PLAYER_SHOW) || context.equals(PlotState.EDIT_PLAYER_ADD) || context.equals(PlotState.EDIT_PLAYER_REMOVE)) {
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
		} else if(context.equals(PlotState.EDIT)) {
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
		}
		view.updateIcons();
	}
	
	private String getTitle(PlotState context) {
		String result = "";
		switch(context) {
			case ROOT:
				result = ChatColor.BLACK+town.getName()+" "+MessagePath.MENU_PLOTS_TITLE_PLOTS.getMessage();
				break;
			case ROOT_CREATE:
				result = ChatColor.BLACK+town.getName()+" "+MessagePath.MENU_PLOTS_TITLE_CREATE.getMessage();
				break;
			case ROOT_DELETE:
				result = ChatColor.BLACK+town.getName()+" "+MessagePath.MENU_PLOTS_TITLE_DELETE.getMessage();
				break;
			case ROOT_EDIT:
				result = ChatColor.BLACK+town.getName()+" "+MessagePath.MENU_PLOTS_TITLE_EDIT.getMessage();
				break;
			case CREATE_LAND_ADD:
				result = ChatColor.BLACK+town.getName()+" "+MessagePath.MENU_PLOTS_TITLE_ADD_LAND.getMessage();
				break;
			case CREATE_PLAYER_ADD:
				result = ChatColor.BLACK+town.getName()+" "+MessagePath.MENU_PLOTS_TITLE_ADD_PLAYERS.getMessage();
				break;
			case EDIT:
				result = ChatColor.BLACK+town.getName()+" "+MessagePath.MENU_PLOTS_TITLE_EDIT_OPTIONS.getMessage();
				break;
			case EDIT_LAND_ADD:
				result = ChatColor.BLACK+town.getName()+" "+MessagePath.MENU_PLOTS_TITLE_ADD_LAND.getMessage();
				break;
			case EDIT_LAND_REMOVE:
				result = ChatColor.BLACK+town.getName()+" "+MessagePath.MENU_PLOTS_TITLE_REMOVE_LAND.getMessage();
				break;
			case EDIT_PLAYER_SHOW:
				result = ChatColor.BLACK+town.getName()+" "+MessagePath.MENU_PLOTS_TITLE_SHOW_PLAYERS.getMessage();
				break;
			case EDIT_PLAYER_ADD:
				result = ChatColor.BLACK+town.getName()+" "+MessagePath.MENU_PLOTS_TITLE_ADD_PLAYERS.getMessage();
				break;
			case EDIT_PLAYER_REMOVE:
				result = ChatColor.BLACK+town.getName()+" "+MessagePath.MENU_PLOTS_TITLE_REMOVE_PLAYERS.getMessage();
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
		return new InfoIcon(ChatColor.GOLD+MessagePath.MENU_PLOTS_BUTTON_CREATE.getMessage(),Collections.emptyList(),Material.OAK_SAPLING,index,true);
	}
	
	private InfoIcon navIconDelete(int index) {
		return new InfoIcon(ChatColor.GOLD+MessagePath.MENU_PLOTS_BUTTON_DELETE.getMessage(),Collections.emptyList(),Material.BARRIER,index,true);
	}
	
	private InfoIcon navIconEdit(int index) {
		return new InfoIcon(ChatColor.GOLD+MessagePath.MENU_PLOTS_BUTTON_EDIT.getMessage(),Collections.emptyList(),Material.WRITABLE_BOOK,index,true);
	}
	
	private InfoIcon navIconReturn(int index) {
		return new InfoIcon(ChatColor.GOLD+MessagePath.MENU_PLOTS_BUTTON_RETURN.getMessage(),Collections.emptyList(),Material.FIREWORK_ROCKET,index,true);
	}
	
	private InfoIcon navIconFinish(int index) {
		return new InfoIcon(ChatColor.GOLD+MessagePath.MENU_PLOTS_BUTTON_FINISH.getMessage(),Collections.emptyList(),Material.WRITTEN_BOOK,index,true);
	}
	
	private InfoIcon navIconClose(int index) {
		return new InfoIcon(ChatColor.GOLD+MessagePath.LABEL_CLOSE.getMessage(),Collections.emptyList(),Material.STRUCTURE_VOID,index,true);
	}
	
	private InfoIcon navIconBack(int index) {
		return new InfoIcon(ChatColor.GOLD+MessagePath.LABEL_BACK.getMessage(),Collections.emptyList(),Material.ENDER_PEARL,index,true);
	}
	
	private InfoIcon navIconNext(int index) {
		return new InfoIcon(ChatColor.GOLD+MessagePath.LABEL_NEXT.getMessage(),Collections.emptyList(),Material.ENDER_PEARL,index,true);
	}
	
	private InfoIcon navIconEmpty(int index) {
		return new InfoIcon(" ",Collections.emptyList(),Material.GRAY_STAINED_GLASS_PANE,index,false);
	}
	
}
