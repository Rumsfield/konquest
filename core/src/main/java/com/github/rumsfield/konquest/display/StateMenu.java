package com.github.rumsfield.konquest.display;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.display.icon.InfoIcon;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.model.KonPrefixType;
import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.*;

public abstract class StateMenu {

    // Interfaces intended for enums of subclasses
    public interface State {}
    public interface Access {}

    private final Konquest konquest;
    private State currentState;
    private Access menuAccess;
    private final HashMap<State,ArrayList<DisplayMenu>> viewPages; // Map of paged views
    private int currentPage;

    protected final Comparator<KonTown> townComparator;
    protected final Comparator<KonKingdom> kingdomComparator;
    protected final Comparator<KonPrefixType> prefixComparator;
    protected final int MAX_ICONS_PER_PAGE = 45;
    protected final int MAX_ROW_SIZE = 9;
    protected final int INDEX_HOME = 3;
    protected final int INDEX_CLOSE = 4;
    protected final int INDEX_RETURN = 5;
    protected final int INDEX_BACK = 0;
    protected final int INDEX_NEXT = 8;

    public StateMenu(Konquest konquest, State initialState, Access initialAccess) {
        this.viewPages = new HashMap<>();
        this.currentPage = 0;
        this.currentState = initialState;
        this.menuAccess = initialAccess;
        this.konquest = konquest;

        this.townComparator = (townOne, townTwo) -> {
            // sort by land, then population
            int result = 0;
            int g1Land = townOne.getChunkList().size();
            int g2Land = townTwo.getChunkList().size();
            if(g1Land < g2Land) {
                result = 1;
            } else if(g1Land > g2Land) {
                result = -1;
            } else {
                int g1Pop = townOne.getNumResidents();
                int g2Pop = townTwo.getNumResidents();
                if(g1Pop < g2Pop) {
                    result = 1;
                } else if(g1Pop > g2Pop) {
                    result = -1;
                }
            }
            return result;
        };

        this.kingdomComparator = (kingdomOne, kingdomTwo) -> {
            // sort by towns, then population
            int result = 0;
            int g1Land = kingdomOne.getNumLand();
            int g2Land = kingdomTwo.getNumLand();
            if(g1Land < g2Land) {
                result = 1;
            } else if(g1Land > g2Land) {
                result = -1;
            } else {
                int g1Pop = kingdomOne.getNumMembers();
                int g2Pop = kingdomTwo.getNumMembers();
                if(g1Pop < g2Pop) {
                    result = 1;
                } else if(g1Pop > g2Pop) {
                    result = -1;
                }
            }
            return result;
        };

        this.prefixComparator = (prefixOne, prefixTwo) -> {
            // sort by level
            int result = 0;
            if(prefixOne.level() < prefixTwo.level()) {
                result = -1;
            } else if(prefixOne.level() > prefixTwo.level()) {
                result = 1;
            }
            return result;
        };
    }

    public Konquest getKonquest() {
        return konquest;
    }

    public State getCurrentState() {
        return currentState;
    }

    public Access getAccess() {
        return menuAccess;
    }

    public void setAccess(Access access) {
        this.menuAccess = access;
    }

    public boolean isAccess(Access checkAccess) {
        return menuAccess.equals(checkAccess);
    }

    public boolean isState(State checkState) {
        return currentState.equals(checkState);
    }

    /*
     * Abstract Methods
     */

    /**
     * Create a list of views for a given menu state.
     * This creates new views, specific to each menu.
     * @param context The menu state for the corresponding view
     * @return The list of menu views to be displayed to the player
     */
    public abstract ArrayList<DisplayMenu> createView(State context);

    /**
     * Change the menu's state based on the clicked inventory slot and type of click (right or left mouse).
     * Assume a clickable icon was clicked and visible to the player.
     * Returning a null value will close the menu.
     * @param slot The inventory slot of the current view that was clicked
     * @param clickType The type of click, true for left-click, false for right click
     * @return The new view state of the menu, or null to close the menu
     */
    public abstract DisplayMenu updateState(int slot, boolean clickType);

    /*
     * Common Methods
     */
    public DisplayMenu setCurrentView(State context) {
        return setCurrentView(context, false);
    }

    public DisplayMenu refreshCurrentView() {
        return setCurrentView(currentState, true);
    }

    public DisplayMenu refreshNewView(State context) {
        return setCurrentView(context, true);
    }

    private DisplayMenu setCurrentView(State context, boolean refresh) {
        if (!hasView(context) || refresh) {
            // Create a new view
            viewPages.put(context, createView(context));
        }
        currentState = context;
        currentPage = 0;
        DisplayMenu view = getCurrentView();
        if (view != null) {
            view.updateIcons();
        }
        return view;
    }

    public int getCurrentNumPages() {
        return getCurrentViewPages() == null ? 0 : getCurrentViewPages().size();
    }

    public @Nullable ArrayList<DisplayMenu> getCurrentViewPages() {
        return viewPages.get(currentState);
    }

    public @Nullable DisplayMenu getCurrentView() {
        // Fetch current display view and update the icons in the inventory
        DisplayMenu view = null;
        if (getCurrentViewPages() != null && !getCurrentViewPages().isEmpty() && currentPage < getCurrentNumPages()) {
            view = getCurrentViewPages().get(currentPage);
        }
        return view;
    }

    public boolean hasView(State checkState) {
        return viewPages.containsKey(checkState);
    }

    /*
     * Navigation Methods
     */

    // Is the slot in the current view's navigation row?
    protected boolean isCurrentNavSlot(int slot) {
        DisplayMenu view = getCurrentView();
        if (view == null) return false;
        int invSize = view.getInventory().getSize();
        int navStop = Math.max(invSize-1,0);
        int navStart = Math.max(invSize-MAX_ROW_SIZE,0);
        return slot <= navStop && slot >= navStart;
    }

    // Is the slot in the current view's menu rows?
    protected boolean isCurrentMenuSlot(int slot) {
        DisplayMenu view = getCurrentView();
        if (view == null) return false;
        int invSize = view.getInventory().getSize();
        int navStart = Math.max(invSize-MAX_ROW_SIZE,0);
        return slot < navStart;
    }

    // Get the nav index offset based on the current slot
    protected int getCurrentNavIndex(int slot) {
        DisplayMenu view = getCurrentView();
        if (view == null) return 0;
        int invSize = view.getInventory().getSize();
        int navStart = Math.max(invSize-MAX_ROW_SIZE,0);
        return Math.min(Math.max(slot-navStart,0),MAX_ROW_SIZE-1);
    }

    protected int getNavStartSlot(DisplayMenu view) {
        if (view == null) return 0;
        int navStart = view.getInventory().getSize()-MAX_ROW_SIZE;
        return Math.max(navStart, 0);
    }

    public void addNavClose(DisplayMenu view) {
        // Close [4]
        if (view == null) return;
        view.addIcon(navIconClose(getNavStartSlot(view)+INDEX_CLOSE));
    }

    public void addNavReturn(DisplayMenu view) {
        // Return [5]
        if (view == null) return;
        view.addIcon(navIconReturn(getNavStartSlot(view)+INDEX_RETURN));
    }

    public void addNavHome(DisplayMenu view) {
        // Home [3]
        if (view == null) return;
        view.addIcon(navIconHome(getNavStartSlot(view)+INDEX_HOME));
    }

    public void addNavBack(DisplayMenu view) {
        // Back [0]
        if (view == null) return;
        view.addIcon(navIconBack(getNavStartSlot(view)+INDEX_BACK));
    }

    public void addNavNext(DisplayMenu view) {
        // Next [8]
        if (view == null) return;
        view.addIcon(navIconNext(getNavStartSlot(view)+INDEX_NEXT));
    }

    public void addNavEmpty(DisplayMenu view) {
        // Fill entire row with empty icons
        if (view == null) return;
        int start = getNavStartSlot(view);
        for (int i = start ; i < start+MAX_ROW_SIZE; i++) {
            view.addIcon(navIconEmpty(i));
        }
    }

    protected boolean isNavIndex(int slot, int index) {
        if (isCurrentNavSlot(slot)) {
            int navIndex = getCurrentNavIndex(slot);
            return navIndex == index;
        }
        return false;
    }

    protected boolean isNavClose(int slot) {
        return isNavIndex(slot,INDEX_CLOSE);
    }

    protected boolean isNavReturn(int slot) {
        return isNavIndex(slot,INDEX_RETURN);
    }

    protected boolean isNavHome(int slot) {
        return isNavIndex(slot,INDEX_HOME);
    }

    protected boolean isNavBack(int slot) {
        return isNavIndex(slot,INDEX_BACK);
    }

    protected boolean isNavNext(int slot) {
        return isNavIndex(slot,INDEX_NEXT);
    }

    /*
     * Page Methods
     */

    protected int getTotalPages(int numElements) {
        int pageTotal = (int)Math.ceil(((double)numElements)/MAX_ICONS_PER_PAGE);
        pageTotal = Math.max(pageTotal,1);
        return pageTotal;
    }

    protected int getNumPageRows(int numElements, int pageIndex) {
        int numPageRows = (int)Math.ceil(((double)(numElements - pageIndex*MAX_ICONS_PER_PAGE))/MAX_ROW_SIZE);
        numPageRows = Math.max(numPageRows,1);
        numPageRows = Math.min(numPageRows,5);
        return numPageRows;
    }

    protected DisplayMenu goPageBack() {
        currentPage = Math.max(currentPage-1,0);
        DisplayMenu view = getCurrentView();
        if (view != null) {
            view.updateIcons();
        }
        return view;
    }

    protected DisplayMenu goPageNext() {
        currentPage = Math.min(currentPage+1,getCurrentNumPages());
        DisplayMenu view = getCurrentView();
        if (view != null) {
            view.updateIcons();
        }
        return view;
    }

    protected ArrayList<DisplayMenu> makePages(List<MenuIcon> icons, String baseTitle) {
        ArrayList<DisplayMenu> result = new ArrayList<>();
        ListIterator<MenuIcon> iconIter = icons.listIterator();
        int numIcons = icons.size();
        int pageTotal = getTotalPages(numIcons);
        String pageLabel;
        for(int i = 0; i < pageTotal; i++) {
            int pageRows = getNumPageRows(numIcons, i);
            if (pageTotal > 1) {
                pageLabel = baseTitle+" "+(i+1)+"/"+pageTotal;
            } else {
                pageLabel = baseTitle;
            }
            // Create page
            DisplayMenu page = new DisplayMenu(pageRows, pageLabel);
            // Fill icons
            int slotIndex = 0;
            while(slotIndex < MAX_ICONS_PER_PAGE && iconIter.hasNext()) {
                MenuIcon icon = iconIter.next();
                icon.setIndex(slotIndex);
                page.addIcon(icon);
                slotIndex++;
            }

            // Add navigation
            addNavEmpty(page);
            addNavClose(page);
            addNavReturn(page);
            if (i > 0) {
                addNavBack(page);
            }
            if (i < pageTotal-1) {
                addNavNext(page);
            }
            result.add(page);
        }
        return result;
    }

    /*
     * Navigation Icons
     */

    protected InfoIcon navIconClose(int index) {
        return new InfoIcon(ChatColor.GOLD+MessagePath.LABEL_CLOSE.getMessage(),Material.STRUCTURE_VOID,index,true);
    }

    protected InfoIcon navIconBack(int index) {
        return new InfoIcon(ChatColor.GOLD+MessagePath.LABEL_BACK.getMessage(),Material.ENDER_PEARL,index,true);
    }

    protected InfoIcon navIconNext(int index) {
        return new InfoIcon(ChatColor.GOLD+MessagePath.LABEL_NEXT.getMessage(),Material.ENDER_PEARL,index,true);
    }

    protected InfoIcon navIconReturn(int index) {
        return new InfoIcon(ChatColor.GOLD+MessagePath.LABEL_RETURN.getMessage(),Material.FIREWORK_ROCKET,index,true);
    }

    protected InfoIcon navIconHome(int index) {
        return new InfoIcon(ChatColor.GOLD+MessagePath.MENU_MAIN_TITLE.getMessage(),Material.BELL,index,true);
    }

    protected InfoIcon navIconEmpty(int index) {
        return new InfoIcon(" ",Material.GRAY_STAINED_GLASS_PANE,index,false);
    }

    /*
     * Menu Sounds
     */

    protected void playStatusSound(Player bukkitPlayer, boolean status) {
        if(status) {
            Konquest.playSuccessSound(bukkitPlayer);
        } else {
            Konquest.playFailSound(bukkitPlayer);
        }
    }
}
