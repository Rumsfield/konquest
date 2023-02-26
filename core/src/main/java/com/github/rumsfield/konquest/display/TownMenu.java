package com.github.rumsfield.konquest.display;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.display.icon.*;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.manager.KingdomManager;
import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.utility.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.*;

public class TownMenu extends StateMenu implements ViewableMenu {
    //TODO: Message paths
    enum MenuState implements State {
        ROOT,
        JOIN,
        LEAVE,
        LIST,
        INVITES,
        REQUESTS
    }

    /* Icon slot indexes */
    private final int ROOT_SLOT_JOIN 			= 0;
    private final int ROOT_SLOT_LEAVE 			= 2;
    private final int ROOT_SLOT_REQUESTS 		= 4;
    private final int ROOT_SLOT_INVITES 		= 6;
    private final int ROOT_SLOT_LIST 			= 8;

    private final String loreColor = DisplayManager.loreFormat;
    private final String valueColor = DisplayManager.valueFormat;
    private final String hintColor = DisplayManager.hintFormat;

    private final KingdomManager manager;
    private final KonPlayer player;
    private final KonKingdom kingdom;
    private Comparator<KonTown> townComparator;

    public TownMenu(Konquest konquest, KonPlayer player) {
        super(konquest, MenuState.ROOT, null);
        this.manager = konquest.getKingdomManager();
        this.player = player;
        this.kingdom = player.getKingdom();
        this.townComparator = null;

        initializeMenu();
        renderDefaultViews();
    }

    private void initializeMenu() {
        //TODO other init?

        townComparator = (townOne, townTwo) -> {
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
    }

    private void renderDefaultViews() {
        DisplayMenu renderView;

        /* Root View */
        renderView = createRootView();
        views.put(MenuState.ROOT, renderView);
        refreshNavigationButtons(MenuState.ROOT);

    }

    private DisplayMenu createRootView() {
        DisplayMenu result;
        MenuIcon icon;
        List<String> loreList = new ArrayList<>();

        ChatColor kingdomColor = konquest.getFriendlyPrimaryColor();

        final int rows = 1;

        result = new DisplayMenu(rows+1, getTitle(TownMenu.MenuState.ROOT));

        /* Join Icon */
        loreList.add(loreColor+"Join a new town");
        icon = new InfoIcon(kingdomColor+"Join", loreList, Material.SADDLE, ROOT_SLOT_JOIN, true);
        result.addIcon(icon);

        /* Exile Icon */
        loreList.clear();
        loreList.add(loreColor+"Leave an existing town");
        icon = new InfoIcon(kingdomColor+"Leave", loreList, Material.ARROW, ROOT_SLOT_LEAVE, true);
        result.addIcon(icon);

        /* List Icon */
        loreList.clear();
        loreList.add(loreColor+"List all towns in your kingdom");
        icon = new InfoIcon(kingdomColor+"List", loreList, Material.PAPER, ROOT_SLOT_LIST, true);
        result.addIcon(icon);

        /* Invites Icon */
        loreList.clear();
        loreList.add(loreColor+"Show town resident invites");
        int numInvites = manager.getInviteTowns(player).size();
        Material inviteMat = Material.BOOK;
        if(numInvites > 0) {
            loreList.add(valueColor+""+numInvites);
            inviteMat = Material.WRITABLE_BOOK;
        }
        icon = new InfoIcon(kingdomColor+"Invites", loreList, inviteMat, ROOT_SLOT_INVITES, true);
        result.addIcon(icon);

        /* Requests Icon */
        loreList.clear();
        loreList.add(loreColor+"Show town resident requests");
        int numRequests = manager.getRequestTowns(player).size();
        Material requestMat = Material.GLASS_BOTTLE;
        if(numRequests > 0) {
            loreList.add(valueColor+""+numRequests);
            requestMat = Material.HONEY_BOTTLE;
        }
        icon = new InfoIcon(kingdomColor+"Requests", loreList, requestMat, ROOT_SLOT_REQUESTS, true);
        result.addIcon(icon);

        return result;
    }

    private DisplayMenu createTownView(MenuState context) {
        // A paged view of towns, with lore based on context
        DisplayMenu result;
        pages.clear();
        currentPage = 0;
        String loreHintStr1 = "";
        String loreHintStr2 = "";
        boolean isClickable = false;
        List<KonTown> towns = new ArrayList<>();

        // Determine list of towns given context
        if(context.equals(MenuState.JOIN)) {
            // List of all valid towns able to join (sends request)
            for(KonTown town : kingdom.getTowns()) {
                if(!town.isPlayerResident(player.getOfflineBukkitPlayer())) {
                    towns.add(town);
                }
            }
            loreHintStr1 = "Click to request";
            loreHintStr2 = "Click to join";
            isClickable = true;
        } else if(context.equals(MenuState.LEAVE)) {
            // List of towns that the player can leave
            for(KonTown town : kingdom.getTowns()) {
                if(town.isPlayerResident(player.getOfflineBukkitPlayer())) {
                    towns.add(town);
                }
            }
            loreHintStr1 = "Click to leave";
            isClickable = true;
        } else if(context.equals(MenuState.LIST)) {
            // List of all towns
            towns.add(kingdom.getCapital());
            towns.addAll(kingdom.getTowns());
            loreHintStr1 = "Click to view";
            isClickable = true;
        } else if(context.equals(MenuState.INVITES)) {
            // Towns that have invited the player to join as a resident
            towns.addAll(manager.getInviteTowns(player));
            loreHintStr1 = "Left-click to accept";
            loreHintStr2 = "Right-click to decline";
            isClickable = true;
        } else if(context.equals(MenuState.REQUESTS)) {
            // Towns with pending resident join requests
            towns.addAll(manager.getRequestTowns(player));
            loreHintStr1 = "Click to manage";
            isClickable = true;
        } else {
            return null;
        }
        // Sort list
        towns.sort(townComparator);

        // Create page(s)
        String pageLabel;
        List<String> loreList;
        int pageTotal = getTotalPages(towns.size());
        int pageNum = 0;
        ListIterator<KonTown> listIter = towns.listIterator();
        for(int i = 0; i < pageTotal; i++) {
            int pageRows = getNumPageRows(towns.size(), i);
            pageLabel = getTitle(context)+" "+(i+1)+"/"+pageTotal;
            pages.add(pageNum, new DisplayMenu(pageRows+1, pageLabel));
            int slotIndex = 0;
            while(slotIndex < MAX_ICONS_PER_PAGE && listIter.hasNext()) {
                /* Town Icon (n) */
                KonTown currentTown = listIter.next();
                ChatColor contextColor = konquest.getFriendlyPrimaryColor();
                loreList = new ArrayList<>();
                if(!loreHintStr1.equals("")) {
                    loreList.add(hintColor+loreHintStr1);
                }
                if(!loreHintStr2.equals("")) {
                    loreList.add(hintColor+loreHintStr2);
                }
                TownIcon icon = new TownIcon(currentTown,contextColor,loreList,slotIndex,isClickable);
                pages.get(pageNum).addIcon(icon);
                slotIndex++;
            }
            pageNum++;
        }
        result = pages.get(currentPage);
        return result;
    }

    @Override
    public DisplayMenu getCurrentView() {
        return views.get(currentState);
    }

    @Override
    public DisplayMenu updateState(int slot, boolean clickType) {
        // Assume a clickable icon was clicked
        // Do something based on current state and clicked slot
        DisplayMenu result = null;
        int navMaxIndex = getCurrentView().getInventory().getSize()-1;
        int navMinIndex = getCurrentView().getInventory().getSize()-9;
        if(slot <= navMaxIndex && slot >= navMinIndex) {
            // Clicked in navigation bar
            int index = slot-navMinIndex;
            // (back [0]) close [4], return [5], (next [8])
            if(index == 0) {
                result = goPageBack();
            } else if(index == 5) {
                // Return to previous root
                result = goToRootView();
                currentState = MenuState.ROOT;
            } else if(index == 8) {
                result = goPageNext();
            }
        } else if(slot < navMinIndex) {
            // Click in non-navigation slot
            MenuIcon clickedIcon = views.get(currentState).getIcon(slot);
            MenuState currentMenuState = (MenuState)currentState;
            switch(currentMenuState) {
                case ROOT:
                    if(slot == ROOT_SLOT_JOIN) {
                        currentState = MenuState.JOIN;
                        result = goToTownView(MenuState.JOIN);

                    } else if(slot == ROOT_SLOT_LEAVE) {
                        currentState = MenuState.LEAVE;
                        result = goToTownView(MenuState.LEAVE);

                    } else if(slot == ROOT_SLOT_LIST) {
                        currentState = MenuState.LIST;
                        result = goToTownView(MenuState.LIST);

                    } else if(slot == ROOT_SLOT_INVITES) {
                        currentState = MenuState.INVITES;
                        result = goToTownView(MenuState.INVITES);

                    } else if(slot == ROOT_SLOT_REQUESTS) {
                        currentState = MenuState.REQUESTS;
                        result = goToTownView(MenuState.REQUESTS);

                    }
                    break;
                case JOIN:
                    // Clicking tries to join a town
                    if(clickedIcon instanceof TownIcon) {
                        TownIcon icon = (TownIcon)clickedIcon;
                        KonTown clickTown = icon.getTown();
                        boolean status = manager.menuJoinTownRequest(player, clickTown);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = goToTownView(MenuState.JOIN);
                    }
                    break;
                case LEAVE:
                    // Clicking tries to leave a town
                    if(clickedIcon instanceof TownIcon) {
                        TownIcon icon = (TownIcon)clickedIcon;
                        KonTown clickTown = icon.getTown();
                        boolean status = manager.menuLeaveTown(player, clickTown);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = goToTownView(MenuState.LEAVE);
                    }
                    break;
                case LIST:
                    // Clicking opens a town info menu
                    if(clickedIcon instanceof TownIcon) {
                        TownIcon icon = (TownIcon)clickedIcon;
                        KonTown clickTown = icon.getTown();
                        konquest.getDisplayManager().displayTownInfoMenu(player, clickTown);
                    }
                    break;
                case INVITES:
                    // Clicking accepts or denies a town residence invite
                    if(clickedIcon instanceof TownIcon) {
                        TownIcon icon = (TownIcon)clickedIcon;
                        KonTown clickTown = icon.getTown();
                        boolean status = manager.menuRespondTownInvite(player, clickTown, clickType);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = goToTownView(MenuState.INVITES);
                    }
                    break;
                case REQUESTS:
                    // Clicking opens a town-specific management menu
                    if(clickedIcon instanceof TownIcon) {
                        TownIcon icon = (TownIcon)clickedIcon;
                        KonTown clickTown = icon.getTown();
                        konquest.getDisplayManager().displayTownManagementMenu(player,clickTown,false);
                    }
                    break;
                default:
                    break;
            }
        }
        refreshNavigationButtons(currentState);
        return result;
    }

    private String getTitle(MenuState context) {
        String result = "error";
        ChatColor color = ChatColor.BLACK;
        switch(context) {
            case ROOT:
                result = color+"Town Menu";
                break;
            case JOIN:
                result = color+"Join a Town";
                break;
            case LEAVE:
                result = color+"Leave a Town";
                break;
            case LIST:
                result = color+"List Towns";
                break;
            case INVITES:
                result = color+"Resident Invites";
                break;
            case REQUESTS:
                result = color+"Resident Requests";
                break;
            default:
                break;
        }
        return result;
    }

    private DisplayMenu goToTownView(TownMenu.MenuState context) {
        DisplayMenu result = createTownView(context);
        views.put(context, result);
        return result;
    }

    private DisplayMenu goToRootView() {
        DisplayMenu result = createRootView();
        views.put(TownMenu.MenuState.ROOT, result);
        return result;
    }

    /**
     * Place all navigation button icons on view given context and update icons
     */
    void refreshNavigationButtons(State context) {
        DisplayMenu view = views.get(context);
        int navStart = view.getInventory().getSize()-9;
        if(navStart < 0) {
            ChatUtil.printDebug("Town menu nav buttons failed to refresh in context "+context.toString());
            return;
        }
        if(context.equals(MenuState.ROOT)) {
            // Close [4]
            view.addIcon(navIconEmpty(navStart));
            view.addIcon(navIconEmpty(navStart+1));
            view.addIcon(navIconEmpty(navStart+2));
            view.addIcon(navIconEmpty(navStart+3));
            view.addIcon(navIconClose(navStart+4));
            view.addIcon(navIconEmpty(navStart+5));
            view.addIcon(navIconEmpty(navStart+6));
            view.addIcon(navIconEmpty(navStart+7));
            view.addIcon(navIconEmpty(navStart+8));
        } else if(context.equals(MenuState.JOIN) || context.equals(MenuState.LEAVE) || context.equals(MenuState.LIST) ||
                context.equals(MenuState.INVITES) || context.equals(MenuState.REQUESTS)) {
            // (back [0]) close [4], return [5] (next [8])
            if(currentPage > 0) {
                // Place a back button
                view.addIcon(navIconBack(navStart));
            } else {
                view.addIcon(navIconEmpty(navStart));
            }
            if(currentPage < pages.size()-1) {
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
            view.addIcon(navIconEmpty(navStart+6));
            view.addIcon(navIconEmpty(navStart+7));
        }
        view.updateIcons();
    }

}
