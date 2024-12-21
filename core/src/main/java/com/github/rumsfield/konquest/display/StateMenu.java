package com.github.rumsfield.konquest.display;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.display.icon.InfoIcon;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.model.KonKingdom;
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
    private final State rootState;
    private State currentState;
    private Access menuAccess;
    private final HashMap<State,ArrayList<DisplayMenu>> viewPages; // Map of paged views
    private int currentPage;

    protected final Comparator<KonTown> townComparator;
    protected final Comparator<KonKingdom> kingdomComparator;
    protected final int MAX_ICONS_PER_PAGE = 45;
    protected final int MAX_ROW_SIZE = 9;
    protected final int INDEX_CLOSE = 4;
    protected final int INDEX_RETURN = 5;
    protected final int INDEX_BACK = 0;
    protected final int INDEX_NEXT = 8;

    public StateMenu(Konquest konquest, State initialState, Access initialAccess) {
        this.viewPages = new HashMap<>();
        this.currentPage = 0;
        this.rootState = initialState;
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

    /*
     * Abstract Methods
     */

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

    public void initView(DisplayMenu view, State context) {
        initView(Collections.singletonList(view), context);
    }

    public void initView(List<DisplayMenu> view, State context) {
        if (view.isEmpty()) return;
        viewPages.put(context, new ArrayList<>(view));
    }

    public DisplayMenu setCurrentView(State newState) {
        if (!hasView(newState)) return null;
        currentState = newState;
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
        if (getCurrentViewPages() != null) {
            view = getCurrentViewPages().get(currentPage);
        }
        return view;
    }

    public boolean hasView(State checkState) {
        return viewPages.containsKey(checkState);
    }

    public boolean isRoot(State checkState) {
        return rootState.equals(checkState);
    }

    public State getRootState() {
        return rootState;
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

    /*
    public void addNavBackNext(DisplayMenu view) {
        if (view == null) return;
        if(currentPage > 0) {
            // Place a back button [0]
            view.addIcon(navIconBack(getNavStart(view)+INDEX_BACK));
        } else {
            view.addIcon(navIconEmpty(getNavStart(view)+INDEX_BACK));
        }
        if(getCurrentViewPages() != null && currentPage < getCurrentViewPages().size()-1) {
            // Place a next button [8]
            view.addIcon(navIconNext(getNavStart(view)+INDEX_NEXT));
        } else {
            view.addIcon(navIconEmpty(getNavStart(view)+INDEX_NEXT));
        }
    }*/

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
        return getCurrentView();
    }

    protected DisplayMenu goPageNext() {
        currentPage = Math.min(currentPage+1,getCurrentNumPages());
        return getCurrentView();
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
                page.addIcon(iconIter.next());
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

    protected InfoIcon navIconScrollLeft(int index) {
        // Note: Unicode characters do not render correctly in game, must use escape sequence code.
        return new InfoIcon(ChatColor.GOLD+"\u25C0",Collections.emptyList(),Material.STONE_BUTTON,index,true);
    }

    protected InfoIcon navIconScrollRight(int index) {
        // Note: Unicode characters do not render correctly in game, must use escape sequence code.
        return new InfoIcon(ChatColor.GOLD+"\u25B6",Collections.emptyList(),Material.STONE_BUTTON,index,true);
    }

    protected InfoIcon navIconScrollUp(int index) {
        // Note: Unicode characters do not render correctly in game, must use escape sequence code.
        return new InfoIcon(ChatColor.GOLD+"\u25B2",Collections.emptyList(),Material.STONE_BUTTON,index,true);
    }

    protected InfoIcon navIconScrollDown(int index) {
        // Note: Unicode characters do not render correctly in game, must use escape sequence code.
        return new InfoIcon(ChatColor.GOLD+"\u25BC",Collections.emptyList(),Material.STONE_BUTTON,index,true);
    }

    protected InfoIcon navIconCreate(int index) {
        return new InfoIcon(ChatColor.GOLD+MessagePath.MENU_PLOTS_BUTTON_CREATE.getMessage(),Collections.emptyList(),Material.OAK_SAPLING,index,true);
    }

    protected InfoIcon navIconDelete(int index) {
        return new InfoIcon(ChatColor.GOLD+MessagePath.MENU_PLOTS_BUTTON_DELETE.getMessage(),Collections.emptyList(),Material.BARRIER,index,true);
    }

    protected InfoIcon navIconEdit(int index) {
        return new InfoIcon(ChatColor.GOLD+MessagePath.MENU_PLOTS_BUTTON_EDIT.getMessage(),Collections.emptyList(),Material.WRITABLE_BOOK,index,true);
    }

    protected InfoIcon navIconFinish(int index) {
        return new InfoIcon(ChatColor.GOLD+MessagePath.MENU_PLOTS_BUTTON_FINISH.getMessage(),Collections.emptyList(),Material.WRITTEN_BOOK,index,true);
    }

    protected InfoIcon navIconClose(int index) {
        return new InfoIcon(ChatColor.GOLD+ MessagePath.LABEL_CLOSE.getMessage(), Collections.emptyList(), Material.STRUCTURE_VOID,index,true);
    }

    protected InfoIcon navIconBack(int index) {
        return new InfoIcon(ChatColor.GOLD+MessagePath.LABEL_BACK.getMessage(),Collections.emptyList(),Material.ENDER_PEARL,index,true);
    }

    protected InfoIcon navIconNext(int index) {
        return new InfoIcon(ChatColor.GOLD+MessagePath.LABEL_NEXT.getMessage(),Collections.emptyList(),Material.ENDER_PEARL,index,true);
    }

    protected InfoIcon navIconEmpty(int index) {
        return new InfoIcon(" ",Collections.emptyList(),Material.GRAY_STAINED_GLASS_PANE,index,false);
    }

    protected InfoIcon navIconReturn(int index) {
        return new InfoIcon(ChatColor.GOLD+MessagePath.MENU_PLOTS_BUTTON_RETURN.getMessage(),Collections.emptyList(),Material.FIREWORK_ROCKET,index,true);
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
