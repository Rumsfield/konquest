package com.github.rumsfield.konquest.display.menu;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.display.DisplayMenu;
import com.github.rumsfield.konquest.display.StateMenu;
import com.github.rumsfield.konquest.display.icon.*;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.*;

public class PrefixMenu extends StateMenu {

    enum MenuState implements State {
        ROOT,
        DISABLE,
        PREFIX,
        CUSTOM,
        STATS
    }

    private final KonPlayer player;

    //TODO Add an admin mode for modifying online and offline player prefixes

    // Barbarian players can use this menu, but cannot apply prefixes.
    public PrefixMenu(Konquest konquest, KonPlayer player) {
        super(konquest, MenuState.ROOT, null);
        this.player = player;

        /* Initialize menu view */
        setCurrentView(MenuState.ROOT);

    }

    /**
     * Creates the root menu view for this menu.
     * This is a single page view.
     */
    private DisplayMenu createRootView() {
        if (!getKonquest().getAccomplishmentManager().isEnabled()) return null;
        DisplayMenu result;
        MenuIcon icon;

        /* Icon slot indexes */
        // Row 0: 0  1  2  3  4  5  6  7  8
        int ROOT_SLOT_CURRENT = 3;
        int ROOT_SLOT_DISABLE = 5;
        // Row 1: 9 10 11 12 13 14 15 16 17
        int ROOT_SLOT_PREFIX = 11;
        int ROOT_SLOT_CUSTOM = 13;
        int ROOT_SLOT_STATS = 15;

        result = new DisplayMenu(2, getTitle(MenuState.ROOT));

        /* Current Info Icon */
        String playerPrefix;
        if(player.getPlayerPrefix().isEnabled()) {
            playerPrefix = ChatUtil.parseHex(player.getPlayerPrefix().getMainPrefixName());
        } else {
            playerPrefix = MessagePath.LABEL_DISABLED.getMessage();
        }
        icon = new InfoIcon(MessagePath.MENU_PREFIX_CURRENT_PREFIX.getMessage(), Material.PLAYER_HEAD, ROOT_SLOT_CURRENT, false);
        icon.addDescription(playerPrefix);
        result.addIcon(icon);

        /* Disable Icon */
        boolean isPrefixDisabled = !player.getPlayerPrefix().isEnabled();
        boolean isTitleAlwaysShown = getKonquest().getCore().getBoolean(CorePath.CHAT_ALWAYS_SHOW_TITLE.getPath(),false);
        boolean isDisableClickable = !isPrefixDisabled && !isTitleAlwaysShown;
        icon = new InfoIcon(MessagePath.MENU_PREFIX_DISABLE_PREFIX.getMessage(), Material.MILK_BUCKET, ROOT_SLOT_DISABLE, isDisableClickable);
        if (isDisableClickable) {
            icon.addHint(MessagePath.MENU_HINT_DISABLE.getMessage());
        } else {
            if (isPrefixDisabled) {
                icon.addAlert(MessagePath.COMMAND_PREFIX_ERROR_DISABLE.getMessage());
            } else {
                icon.addAlert(MessagePath.COMMAND_PREFIX_ERROR_ALWAYS_ON.getMessage());
            }
        }
        icon.setState(MenuState.DISABLE);
        result.addIcon(icon);

        /* Prefix Icon (Unavailable for Barbarians) */
        boolean isPrefixClickable = !player.isBarbarian();
        icon = new InfoIcon(MessagePath.MENU_PREFIX_TITLE_PREFIX.getMessage(), Material.NAME_TAG, ROOT_SLOT_PREFIX, isPrefixClickable);
        icon.addDescription(MessagePath.MENU_PREFIX_DESCRIPTION_PREFIX.getMessage());
        if (isPrefixClickable) {
            icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
        }
        icon.setState(MenuState.PREFIX);
        result.addIcon(icon);

        /* Custom Icon (Unavailable for Barbarians) */
        if (getKonquest().getAccomplishmentManager().getNumCustomPrefixes() > 0) {
            boolean isCustomClickable = !player.isBarbarian();
            icon = new InfoIcon(MessagePath.MENU_PREFIX_TITLE_CUSTOM.getMessage(), Material.GOLDEN_CARROT, ROOT_SLOT_CUSTOM, isCustomClickable);
            icon.addDescription(MessagePath.MENU_PREFIX_DESCRIPTION_CUSTOM.getMessage());
            if (isCustomClickable) {
                icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
            } else {
                icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
            }
            icon.setState(MenuState.CUSTOM);
            result.addIcon(icon);
        }

        /* Stats Icon */
        icon = new InfoIcon(MessagePath.MENU_PREFIX_TITLE_STATS.getMessage(), Material.BOOK, ROOT_SLOT_STATS, true);
        icon.addDescription(MessagePath.MENU_PREFIX_DESCRIPTION_STATS.getMessage());
        icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
        icon.setState(MenuState.STATS);
        result.addIcon(icon);

        /* Navigation */
        addNavEmpty(result);
        addNavHome(result);
        addNavClose(result);

        return result;
    }

    /**
     * Creates the prefix views.
     * This can be a multiple paged view.
     */
    private List<DisplayMenu> createPrefixView() {
        ArrayList<MenuIcon> prefixIcons = new ArrayList<>();
        MenuIcon icon;

        /* Prefix Icons */
        for(KonPrefixCategory category : KonPrefixCategory.values()) {
            List<KonPrefixType> categoryPrefixes = new ArrayList<>();
            // Determine category level amount
            double level = 0;
            for(KonStatsType statCheck : KonStatsType.values()) {
                if(statCheck.getCategory().equals(category)) {
                    level = level + (player.getPlayerStats().getStat(statCheck) * statCheck.weight());
                }
            }
            // Gather all prefixes in this category
            for(KonPrefixType prefix : KonPrefixType.values()) {
                if(prefix.category().equals(category)) {
                    categoryPrefixes.add(prefix);
                }
            }
            categoryPrefixes.sort(prefixComparator);
            // Create icons for each prefix
            for (KonPrefixType prefix : categoryPrefixes) {
                String categoryLevel = String.format("%.2f",level);
                String levelFormat;
                boolean isClickable;
                if(player.getPlayerPrefix().hasPrefix(prefix)) {
                    isClickable = true;
                    levelFormat = ChatColor.DARK_GREEN+categoryLevel+ChatColor.WHITE+"/"+ChatColor.AQUA+prefix.level();
                } else {
                    isClickable = false;
                    levelFormat = ChatColor.DARK_RED+categoryLevel+ChatColor.WHITE+"/"+ChatColor.AQUA+prefix.level();
                }
                icon = new PrefixIcon(prefix,0,isClickable);
                icon.addProperty(prefix.category().getTitle());
                icon.addDescription(levelFormat);
                if (isClickable) {
                    icon.addHint(MessagePath.MENU_HINT_APPLY.getMessage());
                }
                prefixIcons.add(icon);
            }
            // Create filler icons
            int fillerCount = MAX_ROW_SIZE - Math.floorMod(categoryPrefixes.size(),MAX_ROW_SIZE);
            for (int i = 0; i < fillerCount; i++) {
                prefixIcons.add(new InfoIcon(" ",Material.GLASS_PANE,0,false));
            }
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(prefixIcons, getTitle(MenuState.PREFIX)));
    }

    /**
     * Creates the custom views.
     * This can be a multiple paged view.
     */
    private List<DisplayMenu> createCustomView() {
        ArrayList<MenuIcon> customPrefixIcons = new ArrayList<>();
        MenuIcon icon;

        /* Custom Prefix Icons */
        for (KonCustomPrefix currentCustom : getKonquest().getAccomplishmentManager().getCustomPrefixes()) {
            boolean isClickable = player.getBukkitPlayer().hasPermission("konquest.prefix."+currentCustom.getLabel());
            icon = new PrefixCustomIcon(currentCustom, 0, isClickable);
            if(!player.getPlayerPrefix().isCustomAvailable(currentCustom.getLabel())) {
                String cost = String.format("%.2f",(double)currentCustom.getCost());
                icon.addNameValue(MessagePath.LABEL_COST.getMessage(), cost);
            }
            if(isClickable) {
                icon.addHint(MessagePath.MENU_HINT_APPLY.getMessage());
            } else {
                icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
            }
            customPrefixIcons.add(icon);
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(customPrefixIcons, getTitle(MenuState.CUSTOM)));
    }

    /**
     * Creates the stats views.
     * This can be a multiple paged view.
     */
    private List<DisplayMenu> createStatsView() {
        ArrayList<MenuIcon> statsIcons = new ArrayList<>();
        MenuIcon icon;

        /* Stat Icons */
        for (KonStatsType stat : KonStatsType.values()) {
            int statValue = player.getPlayerStats().getStat(stat);
            icon = new StatIcon(stat, statValue, 0, false);
            statsIcons.add(icon);
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(statsIcons, getTitle(MenuState.STATS)));
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
        switch ((MenuState)context) {
            case ROOT:
                result.add(createRootView());
                break;
            case PREFIX:
                result.addAll(createPrefixView());
                break;
            case CUSTOM:
                result.addAll(createCustomView());
                break;
            case STATS:
                result.addAll(createStatsView());
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
        if (isCurrentNavSlot(slot)) {
            // Clicked in navigation bar
            if (isNavClose(slot)) {
                // Close the menu by returning a null view
                return null;
            } else if (isNavHome(slot)) {
                // Go to main menu
                getKonquest().getDisplayManager().displayMainMenu(player);
            }  else if (isNavReturn(slot)) {
                // Return to root
                result = setCurrentView(MenuState.ROOT);
            } else if (isNavBack(slot)) {
                // Page back
                result = goPageBack();
            } else if (isNavNext(slot)) {
                // Page next
                result = goPageNext();
            }
        } else if (isCurrentMenuSlot(slot)) {
            // Clicked in menu
            DisplayMenu view = getCurrentView();
            if (view == null) return null;
            MenuIcon clickedIcon = view.getIcon(slot);
            MenuState currentState = (MenuState)getCurrentState();
            if (currentState == null) return null;
            MenuState nextState = (MenuState)clickedIcon.getState(); // could be null in some states
            switch (currentState) {
                case ROOT:
                    // Root view, use stored icon state
                    if (nextState == null) return null;
                    switch (nextState) {
                        case PREFIX:
                        case CUSTOM:
                        case STATS:
                            // Go to next state as defined by icon
                            result = setCurrentView(nextState);
                            break;
                        case DISABLE:
                            // Disable the player's prefix title
                            boolean status = getKonquest().getAccomplishmentManager().disablePlayerPrefix(player);
                            playStatusSound(player.getBukkitPlayer(),status);
                            result = refreshCurrentView();
                            break;
                    }
                    break;
                case PREFIX:
                    // Apply a new prefix title
                    if(clickedIcon instanceof PrefixIcon) {
                        PrefixIcon icon = (PrefixIcon)clickedIcon;
                        boolean status = getKonquest().getAccomplishmentManager().applyPlayerPrefix(player,icon.getPrefix());
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = refreshNewView(MenuState.ROOT);
                    }
                    break;
                case CUSTOM:
                    // Apply a new prefix title
                    if(clickedIcon instanceof PrefixCustomIcon) {
                        PrefixCustomIcon icon = (PrefixCustomIcon)clickedIcon;
                        boolean status = getKonquest().getAccomplishmentManager().applyPlayerCustomPrefix(player,icon.getPrefix());
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = refreshNewView(MenuState.ROOT);
                    }
                    break;
                default:
                    break;
            }
        }
        return result;
    }

    private String getTitle(MenuState context) {
        String result = "error";
        switch (context) {
            case ROOT:
                result = MessagePath.MENU_MAIN_PREFIX.getMessage();
                break;
            case PREFIX:
                result = MessagePath.MENU_PREFIX_TITLE_PREFIX.getMessage();
                break;
            case CUSTOM:
                result = MessagePath.MENU_PREFIX_TITLE_CUSTOM.getMessage();
                break;
            case STATS:
                result = MessagePath.MENU_PREFIX_TITLE_STATS.getMessage();
                break;
            default:
                break;
        }
        return result;
    }
}
