package com.github.rumsfield.konquest.display.menu;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.display.DisplayMenu;
import com.github.rumsfield.konquest.display.StateMenu;
import com.github.rumsfield.konquest.display.icon.InfoIcon;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.display.icon.PlayerIcon;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonPlot;
import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.utility.HelperUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.*;
import java.util.List;

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
public class PlotMenu extends StateMenu {

    enum MenuState implements State {
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
        EDIT_PLAYER_REMOVE
    }

    private final int INDEX_SCROLL_LEFT = 0;
    private final int INDEX_SCROLL_UP = 1;
    private final int INDEX_SCROLL_DOWN = 2;
    private final int INDEX_SCROLL_RIGHT = 3;
    private final int INDEX_CREATE = 5;
    private final int INDEX_DELETE = 6;
    private final int INDEX_FINISH = 6;
    private final int INDEX_EDIT = 7;

    private final KonTown town;
    private Point origin;
    private final Player bukkitPlayer;
    private final int maxSize;
    private final Location playerLoc;
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

    public PlotMenu(Konquest konquest, KonTown town, KonPlayer player, int maxSize) {
        super(konquest, MenuState.ROOT, null);
        this.town = town;
        this.bukkitPlayer = player.getBukkitPlayer();
        this.maxSize = maxSize;
        this.playerLoc = bukkitPlayer.getLocation();
        if(town.isLocInside(playerLoc)) {
            this.origin = HelperUtil.toPoint(playerLoc);
        } else {
            this.origin = HelperUtil.toPoint(town.getCenterLoc());
        }

        /* Initialize menu view */
        setCurrentView(MenuState.ROOT);

    }

    /**
     * Creates the edit menu view.
     * This is a single page view.
     */
    private DisplayMenu createEditView() {
        DisplayMenu result = new DisplayMenu(1, getTitle(MenuState.EDIT));
        MenuIcon icon;

        /* Icon slot indexes */
        // Row 0: 0 1 2 3 4 5 6 7 8
        int EDIT_SLOT_ADD_LAND 			= 2;
        int EDIT_SLOT_REMOVE_LAND 		= 3;
        int EDIT_SLOT_ADD_PLAYER 		= 5;
        int EDIT_SLOT_REMOVE_PLAYER 	= 6;
        int EDIT_SLOT_SHOW_PLAYER 	    = 7;

        icon = new InfoIcon(MessagePath.MENU_PLOTS_EDIT_ADD_LAND.getMessage(), Material.GRASS_BLOCK, EDIT_SLOT_ADD_LAND, true);
        icon.setState(MenuState.EDIT_LAND_ADD);
        result.addIcon(icon);

        icon = new InfoIcon(MessagePath.MENU_PLOTS_EDIT_REMOVE_LAND.getMessage(), Material.COARSE_DIRT, EDIT_SLOT_REMOVE_LAND, true);
        icon.setState(MenuState.EDIT_LAND_REMOVE);
        result.addIcon(icon);

        icon = new InfoIcon(MessagePath.MENU_PLOTS_EDIT_ADD_PLAYERS.getMessage(), Material.PLAYER_HEAD, EDIT_SLOT_ADD_PLAYER, true);
        icon.setState(MenuState.EDIT_PLAYER_ADD);
        result.addIcon(icon);

        icon = new InfoIcon(MessagePath.MENU_PLOTS_EDIT_REMOVE_PLAYERS.getMessage(), Material.SKELETON_SKULL, EDIT_SLOT_REMOVE_PLAYER, true);
        icon.setState(MenuState.EDIT_PLAYER_REMOVE);
        result.addIcon(icon);

        icon = new InfoIcon(MessagePath.MENU_PLOTS_EDIT_SHOW_PLAYERS.getMessage(), Material.PAINTING, EDIT_SLOT_SHOW_PLAYER, true);
        icon.setState(MenuState.EDIT_PLAYER_SHOW);
        result.addIcon(icon);

        /* Navigation */
        addNavEmpty(result);
        addNavClose(result);
        addNavReturn(result);

        return result;
    }

    /**
     * Creates the land view, centered on an origin point.
     * This is a single page view that gets refreshed with every scroll.
     * Contexts: ROOT, ROOT_CREATE, ROOT_DELETE, ROOT_EDIT, CREATE_LAND_ADD, EDIT_LAND_REMOVE, EDIT_LAND_ADD
     */
    private DisplayMenu createLandView(MenuState context) {
        DisplayMenu result = new DisplayMenu(5, getTitle(context));
        List<String> loreList;
        List<String> hintList;
        InfoIcon icon;
        Point drawPoint;
        KonPlot drawPlot;
        int colorSelect = 0;
        HashMap<KonPlot,Material> plotColorMap = new HashMap<>();
        int index = 0;
        for(int r = -2; r <= 2; r++) {
            for(int c = -4; c <= 4; c++) {
                // Iterate over all land tiles
                drawPoint = new Point(origin.x + c, origin.y + r);
                drawPlot = null;
                if(town.getChunkList().containsKey(drawPoint)) {
                    // This tile is claimed by the town
                    loreList = new ArrayList<>();
                    hintList = new ArrayList<>();
                    boolean isClickable = false;
                    Material landMat = Material.GREEN_STAINED_GLASS_PANE;
                    String landTitle = ChatColor.GREEN+MessagePath.MENU_PLOTS_TOWN_LAND.getMessage()+" | "+drawPoint.x+","+drawPoint.y;
                    boolean isPlot = false;
                    // Render all town land, then plots, then remove oldPlot if exists, then render editPlot if exists.
                    // Draw different plots in a sequence of stained-glass pane colors.
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
                    // Add render for editPlot
                    if(editPlot != null && editPlot.hasPoint(drawPoint)) {
                        landMat = Material.GLASS_PANE;
                        landTitle = ChatColor.WHITE+MessagePath.MENU_PLOTS_EDITING_PLOT.getMessage()+" | "+drawPoint.x+","+drawPoint.y;
                        isPlot = true;
                    }
                    // Add context tips to lore
                    if(isPlot) {
                        if(context.equals(MenuState.ROOT_DELETE)) {
                            isClickable = true;
                            hintList.add(MessagePath.MENU_PLOTS_CLICK_DELETE.getMessage());
                        } else if(context.equals(MenuState.ROOT_EDIT)) {
                            isClickable = true;
                            hintList.add(MessagePath.MENU_PLOTS_CLICK_EDIT.getMessage());
                        } else if(context.equals(MenuState.EDIT_LAND_REMOVE)) {
                            isClickable = true;
                            hintList.add(MessagePath.MENU_PLOTS_CLICK_REMOVE_CHUNK.getMessage());
                        }
                    } else {
                        if(context.equals(MenuState.ROOT_CREATE)) {
                            isClickable = true;
                            hintList.add(MessagePath.MENU_PLOTS_CLICK_CREATE.getMessage());
                        } else if(context.equals(MenuState.EDIT_LAND_ADD) || context.equals(MenuState.CREATE_LAND_ADD)) {
                            isClickable = true;
                            hintList.add(MessagePath.MENU_PLOTS_CLICK_ADD_CHUNK.getMessage());
                        }
                    }
                    // Check for monument chunk
                    if(HelperUtil.toPoint(town.getCenterLoc()).equals(drawPoint)) {
                        isClickable = false;
                        loreList.clear();
                        loreList.add(ChatColor.GOLD+town.getName());
                        loreList.add(ChatColor.YELLOW+MessagePath.MENU_PLOTS_TOWN_MONUMENT.getMessage());
                        landMat = Material.OBSIDIAN;
                    }
                    // Add other info to lore
                    if(HelperUtil.toPoint(playerLoc).equals(drawPoint)) {
                        loreList.add(ChatColor.YELLOW+MessagePath.MENU_PLOTS_HERE.getMessage());
                    }
                    // Build icon and add to menu view
                    icon = new InfoIcon(landTitle,landMat,index,isClickable);
                    for (String loreLine : loreList) {
                        icon.addDescription(loreLine);
                    }
                    for (String hintLine : hintList) {
                        icon.addHint(hintLine);
                    }
                    result.addIcon(icon);
                }
                index++;
            }
        }

        /* Navigation with context */
        addNavEmpty(result);
        switch (context) {
            case ROOT:
                addNavScrollLeft(result);
                addNavScrollUp(result);
                addNavScrollDown(result);
                addNavScrollRight(result);
                addNavClose(result);
                addNavCreate(result);
                addNavDelete(result);
                addNavEdit(result);
                break;
            case ROOT_CREATE:
            case ROOT_DELETE:
            case ROOT_EDIT:
                addNavScrollLeft(result);
                addNavScrollUp(result);
                addNavScrollDown(result);
                addNavScrollRight(result);
                addNavClose(result);
                addNavReturn(result);
                break;
            case CREATE_LAND_ADD:
            case EDIT_LAND_REMOVE:
            case EDIT_LAND_ADD:
                addNavScrollLeft(result);
                addNavScrollUp(result);
                addNavScrollDown(result);
                addNavScrollRight(result);
                addNavClose(result);
                addNavReturn(result);
                addNavFinish(result);
                break;
            default:
                break;
        }

        return result;
    }

    /**
     * Creates a player view based on context.
     * This is a multiple paged view.
     * Contexts: CREATE_PLAYER_ADD, EDIT_PLAYER_ADD, EDIT_PLAYER_REMOVE, EDIT_PLAYER_SHOW
     */
    private List<DisplayMenu> createPlayerView(MenuState context) {
        if (editPlot == null || town == null) return Collections.emptyList();
        List<OfflinePlayer> players = new ArrayList<>();
        ArrayList<MenuIcon> icons = new ArrayList<>();
        MenuIcon icon;

        // Determine list of players given context
        String hintStr;
        switch (context) {
            case CREATE_PLAYER_ADD:
            case EDIT_PLAYER_ADD:
                players.addAll(town.getPlayerResidents());
                players.removeAll(editPlot.getUserOfflinePlayers());
                hintStr = MessagePath.MENU_PLOTS_CLICK_ADD_PLAYER.getMessage();
                break;
            case EDIT_PLAYER_REMOVE:
                players.addAll(editPlot.getUserOfflinePlayers());
                hintStr = MessagePath.MENU_PLOTS_CLICK_REMOVE_PLAYER.getMessage();
                break;
            case EDIT_PLAYER_SHOW:
                players.addAll(editPlot.getUserOfflinePlayers());
                hintStr = MessagePath.MENU_PLOTS_CLICK_MOVE_PLAYER.getMessage();
                break;
            default:
                return null;
        }

        /* Player Icons */
        String contextColor = Konquest.friendColor2;
        for (OfflinePlayer currentPlayer : players) {
            icon = new PlayerIcon(currentPlayer, contextColor, 0,true);
            icon.addHint(hintStr);
            icons.add(icon);
        }

        /* Make Pages (includes navigation) */
        ArrayList<DisplayMenu> result = new ArrayList<>(makePages(icons, getTitle(context)));

        /* Extra Navigation */
        for (DisplayMenu view : result) {
            addNavFinish(view);
        }

        return result;
    }

    /**
     * Create a list of views for a given menu state.
     * This creates new views, specific to each menu.
     *
     * @param context The menu state for the corresponding view
     * @return The list of menu views to be displayed to the player
     */
    @Override
    public ArrayList<DisplayMenu> createView(State context) {
        ArrayList<DisplayMenu> result = new ArrayList<>();
        MenuState currentState = (MenuState)context;
        switch(currentState) {
            case ROOT:
            case ROOT_CREATE:
            case ROOT_DELETE:
            case ROOT_EDIT:
            case CREATE_LAND_ADD:
            case EDIT_LAND_REMOVE:
            case EDIT_LAND_ADD:
                result.add(createLandView(currentState));
                break;
            case CREATE_PLAYER_ADD:
            case EDIT_PLAYER_REMOVE:
            case EDIT_PLAYER_ADD:
            case EDIT_PLAYER_SHOW:
                result.addAll(createPlayerView(currentState));
                break;
            case EDIT:
                result.add(createEditView());
                break;
            default:
                break;
        }
        return result;
    }

    /**
     * Change the menu's state based on the clicked inventory slot and type of click (right or left mouse).
     * Assume a clickable icon was clicked and visible to the player.
     * Returning a null value will close the menu.
     *
     * @param slot      The inventory slot of the current view that was clicked
     * @param clickType The type of click, true for left-click, false for right click
     * @return The new view state of the menu, or null to close the menu
     */
    @Override
    public DisplayMenu updateState(int slot, boolean clickType) {
        DisplayMenu result = null;
        MenuState currentState = (MenuState)getCurrentState();
        if (currentState == null) return null;
        if (isCurrentNavSlot(slot)) {
            // Clicked in navigation bar
            switch (currentState) {
                case ROOT:
                    // Scroll arrows [0,1,2,3], close [4], create [5], delete [6], edit [7]
                    if (isNavClose(slot)) {
                        // Close
                        return null;
                    } else if (isNavScroll(slot)) {
                        // Scroll
                        result = scrollView(slot);
                    } else if (isNavCreate(slot)) {
                        // Create
                        result = setCurrentView(MenuState.ROOT_CREATE);
                    } else if (isNavDelete(slot)) {
                        // Delete
                        result = setCurrentView(MenuState.ROOT_DELETE);
                    } else if (isNavEdit(slot)) {
                        // Edit
                        result = setCurrentView(MenuState.ROOT_EDIT);
                    }
                    break;
                case ROOT_CREATE:
                case ROOT_EDIT:
                case ROOT_DELETE:
                    // Scroll arrows [0,1,2,3], close [4], return [5]
                    if (isNavClose(slot)) {
                        // Close
                        return null;
                    } else if (isNavScroll(slot)) {
                        // Scroll
                        result = scrollView(slot);
                    } else if (isNavReturn(slot)) {
                        // Return
                        editPlot = null;
                        oldPlot = null;
                        result = setCurrentView(MenuState.ROOT);
                    }
                    break;
                case CREATE_LAND_ADD:
                    // Scroll arrows [0,1,2,3], close [4], return [5], finish [6]
                    if (isNavClose(slot)) {
                        // Close
                        return null;
                    } else if (isNavScroll(slot)) {
                        // Scroll
                        result = scrollView(slot);
                    } else if (isNavReturn(slot)) {
                        // Return
                        editPlot = null;
                        oldPlot = null;
                        result = setCurrentView(MenuState.ROOT_CREATE);
                    } else if (isNavFinish(slot)) {
                        // Finish
                        result = setCurrentView(MenuState.CREATE_PLAYER_ADD);
                    }
                    break;
                case CREATE_PLAYER_ADD:
                    // (back [0]) close [4], return [5], finish [6], (next [8])
                    if (isNavClose(slot)) {
                        // Close
                        return null;
                    } else if (isNavReturn(slot)) {
                        // Return
                        if(editPlot != null) {
                            editPlot.clearUsers();
                        }
                        result = setCurrentView(MenuState.CREATE_LAND_ADD);
                    } else if (isNavFinish(slot)) {
                        // Finish
                        commitPlot();
                        editPlot = null;
                        oldPlot = null;
                        result = refreshNewView(MenuState.ROOT);
                    } else if (isNavBack(slot)) {
                        // Page back
                        result = goPageBack();
                    } else if (isNavNext(slot)) {
                        // Page next
                        result = goPageNext();
                    }
                    break;
                case EDIT:
                    // Close [4], return [5]
                    if (isNavClose(slot)) {
                        // Close
                        return null;
                    } else if (isNavReturn(slot)) {
                        // Return
                        editPlot = null;
                        oldPlot = null;
                        result = setCurrentView(MenuState.ROOT_EDIT);
                    }
                    break;
                case EDIT_LAND_ADD:
                case EDIT_LAND_REMOVE:
                    // Scroll arrows [0,1,2,3], close [4], return [5], finish [6]
                    if (isNavClose(slot)) {
                        // Close
                        return null;
                    } else if (isNavScroll(slot)) {
                        // Scroll
                        result = scrollView(slot);
                    } else if (isNavReturn(slot)) {
                        // Return
                        result = setCurrentView(MenuState.EDIT);
                    } else if (isNavFinish(slot)) {
                        // Finish
                        commitPlot();
                        editPlot = null;
                        oldPlot = null;
                        result = refreshNewView(MenuState.ROOT);
                    }
                    break;
                case EDIT_PLAYER_SHOW:
                case EDIT_PLAYER_ADD:
                case EDIT_PLAYER_REMOVE:
                    // (back [0]) close [4], return [5], finish [6], (next [8])
                    if (isNavClose(slot)) {
                        // Close
                        return null;
                    } else if (isNavReturn(slot)) {
                        // Return
                        result = setCurrentView(MenuState.EDIT);
                    } else if (isNavFinish(slot)) {
                        // Finish
                        commitPlot();
                        editPlot = null;
                        oldPlot = null;
                        result = refreshNewView(MenuState.ROOT);
                    } else if (isNavBack(slot)) {
                        // Page back
                        result = goPageBack();
                    } else if (isNavNext(slot)) {
                        // Page next
                        result = goPageNext();
                    }
                    break;
                default:
                    break;
            }
        } else if (isCurrentMenuSlot(slot)) {
            // Clicked in menu
            DisplayMenu view = getCurrentView();
            if (view == null) return null;
            Point clickPoint = slotToPoint(slot);
            MenuIcon clickedIcon = view.getIcon(slot);
            MenuState nextState = (MenuState)clickedIcon.getState(); // could be null in some states
            //ChatUtil.printDebug("Plot Menu view update: State "+currentPlotState.toString()+", Slot "+slot+", Point "+clickPoint.x+","+clickPoint.y);
            switch(currentState) {
                case ROOT_CREATE:
                    // Check for non-plot town land click, create new plot
                    if(!town.hasPlot(clickPoint, town.getWorld())) {
                        // Make a new plot with this point
                        oldPlot = null;
                        editPlot = new KonPlot(clickPoint);
                        result = refreshNewView(MenuState.CREATE_LAND_ADD);
                    }
                    break;
                case ROOT_DELETE:
                    // Check for plot town land click, delete existing plot
                    if(town.hasPlot(clickPoint, town.getWorld())) {
                        // Choose plot to remove
                        oldPlot = town.getPlot(clickPoint,town.getWorld());
                        editPlot = null;
                        commitPlot();
                        result = refreshNewView(MenuState.ROOT);
                    }
                    break;
                case ROOT_EDIT:
                    // Check for plot town land click, edit existing plot
                    if(town.hasPlot(clickPoint, town.getWorld())) {
                        // Choose plot to edit
                        oldPlot = town.getPlot(clickPoint,town.getWorld());
                        editPlot = oldPlot.clone();
                        result = setCurrentView(MenuState.EDIT);
                    }
                    break;
                case CREATE_LAND_ADD:
                case EDIT_LAND_ADD:
                    // Check for non-plot town land click, add to chosen plot
                    if(!town.hasPlot(clickPoint, town.getWorld()) && editPlot != null && !editPlot.hasPoint(clickPoint)) {
                        if(maxSize < 1 || editPlot.getPoints().size() < maxSize) {
                            // Add this point to the temp plot
                            editPlot.addPoint(clickPoint);
                        } else {
                            Konquest.playFailSound(bukkitPlayer);
                        }
                        result = refreshCurrentView();
                    }
                    break;
                case CREATE_PLAYER_ADD:
                case EDIT_PLAYER_ADD:
                    // Check for player icon click, add to chosen plot
                    if(clickedIcon instanceof PlayerIcon && editPlot != null) {
                        PlayerIcon icon = (PlayerIcon)clickedIcon;
                        editPlot.addUser(icon.getOfflinePlayer());
                        result = refreshCurrentView();
                    }
                    break;
                case EDIT:
                    // Check for edit action clicks, use stored icon state
                    if (nextState == null) return null;
                    switch (nextState) {
                        case EDIT_LAND_ADD:
                        case EDIT_LAND_REMOVE:
                        case EDIT_PLAYER_ADD:
                        case EDIT_PLAYER_REMOVE:
                        case EDIT_PLAYER_SHOW:
                            result = refreshNewView(nextState);
                            break;
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
                        result = refreshCurrentView();
                    }
                    break;
                case EDIT_PLAYER_SHOW:
                    // Check for player icon click, move player to first index in user list
                    if(clickedIcon instanceof PlayerIcon && editPlot != null) {
                        PlayerIcon icon = (PlayerIcon)clickedIcon;
                        List<UUID> members = editPlot.getUsers();
                        if(members.remove(icon.getOfflinePlayer().getUniqueId())) {
                            editPlot.clearUsers();
                            editPlot.addUser(icon.getOfflinePlayer());
                            editPlot.addUsers(members);
                        }
                        result = refreshCurrentView();
                    }
                    break;
                case EDIT_PLAYER_REMOVE:
                    // Check for player icon click, remove from chosen plot
                    if(clickedIcon instanceof PlayerIcon && editPlot != null) {
                        PlayerIcon icon = (PlayerIcon)clickedIcon;
                        editPlot.removeUser(icon.getOfflinePlayer());
                        result = refreshCurrentView();
                    }
                    break;
                default:
                    break;
            }
        }

        return result;
    }

    private DisplayMenu scrollView(int slot) {
        int navIndex = getCurrentNavIndex(slot);
        if (navIndex == INDEX_SCROLL_LEFT) {
            // Scroll left
            origin = new Point(origin.x - 1, origin.y);
        } else if (navIndex == INDEX_SCROLL_UP) {
            // Scroll up
            origin = new Point(origin.x, origin.y - 1);
        } else if (navIndex == INDEX_SCROLL_DOWN) {
            // Scroll down
            origin = new Point(origin.x, origin.y + 1);
        } else if (navIndex == INDEX_SCROLL_RIGHT) {
            // Scroll right
            origin = new Point(origin.x + 1, origin.y);
        }
        return refreshCurrentView();
    }

    private String getTitle(MenuState context) {
        String result = "";
        switch(context) {
            case ROOT:
                result = MessagePath.MENU_PLOTS_TITLE_PLOTS.getMessage();
                break;
            case ROOT_CREATE:
                result = MessagePath.MENU_PLOTS_TITLE_CREATE.getMessage();
                break;
            case ROOT_DELETE:
                result = MessagePath.MENU_PLOTS_TITLE_DELETE.getMessage();
                break;
            case ROOT_EDIT:
                result = MessagePath.MENU_PLOTS_TITLE_EDIT.getMessage();
                break;
            case CREATE_LAND_ADD:
            case EDIT_LAND_ADD:
                result = MessagePath.MENU_PLOTS_TITLE_ADD_LAND.getMessage();
                break;
            case CREATE_PLAYER_ADD:
            case EDIT_PLAYER_ADD:
                result = MessagePath.MENU_PLOTS_TITLE_ADD_PLAYERS.getMessage();
                break;
            case EDIT:
                result = MessagePath.MENU_PLOTS_TITLE_EDIT_OPTIONS.getMessage();
                break;
            case EDIT_LAND_REMOVE:
                result = MessagePath.MENU_PLOTS_TITLE_REMOVE_LAND.getMessage();
                break;
            case EDIT_PLAYER_SHOW:
                result = MessagePath.MENU_PLOTS_TITLE_SHOW_PLAYERS.getMessage();
                break;
            case EDIT_PLAYER_REMOVE:
                result = MessagePath.MENU_PLOTS_TITLE_REMOVE_PLAYERS.getMessage();
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
        return new Point(origin.x + xDiff, origin.y + yDiff);
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

    /*
     * Custom Navigation
     */

    private boolean isNavScroll(int slot) {
        if (isCurrentNavSlot(slot)) {
            int navIndex = getCurrentNavIndex(slot);
            return navIndex == INDEX_SCROLL_LEFT ||
                    navIndex == INDEX_SCROLL_UP ||
                    navIndex == INDEX_SCROLL_DOWN ||
                    navIndex == INDEX_SCROLL_RIGHT;
        }
        return false;
    }

    private boolean isNavCreate(int slot) {
        return isNavIndex(slot,INDEX_CREATE);
    }

    private boolean isNavDelete(int slot) {
        return isNavIndex(slot,INDEX_DELETE);
    }

    private boolean isNavEdit(int slot) {
        return isNavIndex(slot,INDEX_EDIT);
    }

    private boolean isNavFinish(int slot) {
        return isNavIndex(slot,INDEX_FINISH);
    }

    private void addNavScrollLeft(DisplayMenu view) {
        if (view == null) return;
        int index = getNavStartSlot(view)+INDEX_SCROLL_LEFT;
        view.addIcon(new InfoIcon(ChatColor.GOLD+"\u25C0",Material.STONE_BUTTON,index,true));
    }

    private void addNavScrollUp(DisplayMenu view) {
        if (view == null) return;
        int index = getNavStartSlot(view)+INDEX_SCROLL_UP;
        view.addIcon(new InfoIcon(ChatColor.GOLD+"\u25B2",Material.STONE_BUTTON,index,true));
    }

    private void addNavScrollDown(DisplayMenu view) {
        if (view == null) return;
        int index = getNavStartSlot(view)+INDEX_SCROLL_DOWN;
        view.addIcon(new InfoIcon(ChatColor.GOLD+"\u25BC",Material.STONE_BUTTON,index,true));
    }

    private void addNavScrollRight(DisplayMenu view) {
        if (view == null) return;
        int index = getNavStartSlot(view)+INDEX_SCROLL_RIGHT;
        view.addIcon(new InfoIcon(ChatColor.GOLD+"\u25B6",Material.STONE_BUTTON,index,true));
    }

    private void addNavCreate(DisplayMenu view) {
        if (view == null) return;
        int index = getNavStartSlot(view)+INDEX_CREATE;
        view.addIcon(new InfoIcon(ChatColor.GOLD+MessagePath.MENU_PLOTS_BUTTON_CREATE.getMessage(),Material.OAK_SAPLING,index,true));
    }

    private void addNavDelete(DisplayMenu view) {
        if (view == null) return;
        int index = getNavStartSlot(view)+INDEX_DELETE;
        view.addIcon(new InfoIcon(ChatColor.GOLD+MessagePath.MENU_PLOTS_BUTTON_DELETE.getMessage(),Material.BARRIER,index,true));
    }

    private void addNavEdit(DisplayMenu view) {
        if (view == null) return;
        int index = getNavStartSlot(view)+INDEX_EDIT;
        view.addIcon(new InfoIcon(ChatColor.GOLD+MessagePath.MENU_PLOTS_BUTTON_EDIT.getMessage(),Material.WRITABLE_BOOK,index,true));
    }

    private void addNavFinish(DisplayMenu view) {
        if (view == null) return;
        int index = getNavStartSlot(view)+INDEX_FINISH;
        view.addIcon(new InfoIcon(ChatColor.GOLD+MessagePath.MENU_PLOTS_BUTTON_FINISH.getMessage(),Material.WRITTEN_BOOK,index,true));
    }


}
