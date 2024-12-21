package com.github.rumsfield.konquest.display.menu;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandType;
import com.github.rumsfield.konquest.command.admin.AdminCommandType;
import com.github.rumsfield.konquest.display.DisplayMenu;
import com.github.rumsfield.konquest.display.StateMenu;
import com.github.rumsfield.konquest.display.icon.AdminCommandIcon;
import com.github.rumsfield.konquest.display.icon.CommandIcon;
import com.github.rumsfield.konquest.display.icon.InfoIcon;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.manager.TerritoryManager;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.HelperUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainMenu extends StateMenu {

    enum MenuState implements State {
        ROOT,
        DASHBOARD,
        SECRET
    }

    /* Icon slot indexes */
    // Root View
    // Row 0:  0  1  2  3  4  5  6  7  8
    private final int ROOT_SLOT_HELP 	    = 1;
    private final int ROOT_SLOT_DASH 	    = 4;
    private final int ROOT_SLOT_KINGDOM 	= 7;
    // Row 1:  9 10 11 12 13 14 15 16 17
    private final int ROOT_SLOT_INFO 		= 10;
    private final int ROOT_SLOT_TOWN		= 16;
    // Row 2: 18 19 20 21 22 23 24 25 26
    private final int ROOT_SLOT_QUEST 	    = 19;
    private final int ROOT_SLOT_STATS 	    = 21;
    private final int ROOT_SLOT_PREFIX 		= 22;
    private final int ROOT_SLOT_SCORE 	    = 23;
    private final int ROOT_SLOT_TRAVEL 	    = 25;
    // Row 3: 27 28 29 30 31 32 33 34 35 (Navigation Bar)
    private final int ROOT_SLOT_SECRET 	    = 34;
    // Dashboard View
    // Row 0:  0  1  2  3  4  5  6  7  8
    private final int DASH_SLOT_MAP_AUTO 	= 2;
    private final int DASH_SLOT_CHAT 	    = 3;
    private final int DASH_SLOT_BORDER 	    = 4;
    private final int DASH_SLOT_FLY 	    = 5;
    private final int DASH_SLOT_BYPASS 	    = 6;

    private final String titleColor = DisplayManager.titleFormat;
    private final String loreColor = DisplayManager.loreFormat;
    private final String valueColor = DisplayManager.valueFormat;
    private final String hintColor = DisplayManager.hintFormat;
    private final String alertColor = DisplayManager.alertFormat;
    private final String labelColor = ""+ ChatColor.GOLD;


    private final KonPlayer player;

    public MainMenu(Konquest konquest, KonPlayer player) {
        super(konquest, HelpMenu.MenuState.ROOT, null);
        this.player = player;

        /* Initialize menu views */
        initView(createRootView(), MenuState.ROOT);
        initView(createDashboardView(), MenuState.DASHBOARD);
        initView(createSecretView(), MenuState.SECRET);

        /* Set current menu view */
        setCurrentView(MenuState.ROOT);
    }

    /**
     * Creates the root menu view for this menu.
     * This is a single page view.
     */
    private DisplayMenu createRootView() {
        DisplayMenu result;
        List<String> loreList = new ArrayList<>();
        CommandType iconCommand;
        boolean isClickable;

        result = new DisplayMenu(3, titleColor+MessagePath.MENU_MAIN_TITLE.getMessage());

        /* Help Menu */
        loreList.clear();
        loreList.addAll(HelperUtil.stringPaginate(MessagePath.DESCRIPTION_HELP.getMessage(),loreColor));
        loreList.add(hintColor+MessagePath.MENU_MAIN_HINT.getMessage());
        result.addIcon(new InfoIcon(labelColor+MessagePath.MENU_MAIN_HELP.getMessage(), loreList, CommandType.HELP.iconMaterial(), ROOT_SLOT_HELP, true));

        /* Dashboard */
        loreList.clear();
        loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_MAIN_DESCRIPTION_DASHBOARD.getMessage(),loreColor));
        loreList.add(hintColor+MessagePath.MENU_MAIN_HINT.getMessage());
        result.addIcon(new InfoIcon(labelColor+MessagePath.MENU_MAIN_DASHBOARD.getMessage(), loreList, Material.COMMAND_BLOCK, ROOT_SLOT_DASH, true));

        /* Kingdom Menu */
        iconCommand = CommandType.KINGDOM;
        loreList.clear();
        if (hasPermission(iconCommand)) {
            isClickable = true;
            loreList.addAll(HelperUtil.stringPaginate(iconCommand.description(),loreColor));
            loreList.add(hintColor+MessagePath.MENU_HELP_HINT.getMessage());
        } else {
            isClickable = false;
            loreList.add(alertColor+MessagePath.LABEL_NO_PERMISSION.getMessage());
            loreList.addAll(HelperUtil.stringPaginate(iconCommand.description(),loreColor));
        }
        result.addIcon(new InfoIcon(labelColor+MessagePath.MENU_MAIN_KINGDOM.getMessage(), loreList, iconCommand.iconMaterial(), ROOT_SLOT_KINGDOM, isClickable));

        /* Info Menu */
        iconCommand = CommandType.INFO;
        loreList.clear();
        if (hasPermission(iconCommand)) {
            isClickable = true;
            loreList.addAll(HelperUtil.stringPaginate(iconCommand.description(),loreColor));
            loreList.add(hintColor+MessagePath.MENU_HELP_HINT.getMessage());
        } else {
            isClickable = false;
            loreList.add(alertColor+MessagePath.LABEL_NO_PERMISSION.getMessage());
            loreList.addAll(HelperUtil.stringPaginate(iconCommand.description(),loreColor));
        }
        result.addIcon(new InfoIcon(labelColor+MessagePath.MENU_MAIN_INFO.getMessage(), loreList, iconCommand.iconMaterial(), ROOT_SLOT_INFO, isClickable));

        /* Town Menu */
        iconCommand = CommandType.TOWN;
        loreList.clear();
        if (hasPermission(iconCommand)) {
            isClickable = true;
            loreList.addAll(HelperUtil.stringPaginate(iconCommand.description(),loreColor));
            loreList.add(hintColor+MessagePath.MENU_HELP_HINT.getMessage());
        } else {
            isClickable = false;
            loreList.add(alertColor+MessagePath.LABEL_NO_PERMISSION.getMessage());
            loreList.addAll(HelperUtil.stringPaginate(iconCommand.description(),loreColor));
        }
        result.addIcon(new InfoIcon(labelColor+MessagePath.MENU_MAIN_TOWN.getMessage(), loreList, iconCommand.iconMaterial(), ROOT_SLOT_TOWN, isClickable));

        /* Quest Book */
        iconCommand = CommandType.QUEST;
        loreList.clear();
        if (getKonquest().getDirectiveManager().isEnabled()) {
            if (hasPermission(iconCommand)) {
                isClickable = true;
                loreList.addAll(HelperUtil.stringPaginate(iconCommand.description(),loreColor));
                loreList.add(hintColor+MessagePath.MENU_HELP_HINT.getMessage());
            } else {
                isClickable = false;
                loreList.add(alertColor+MessagePath.LABEL_NO_PERMISSION.getMessage());
                loreList.addAll(HelperUtil.stringPaginate(iconCommand.description(),loreColor));
            }
        } else {
            isClickable = false;
            loreList.add(alertColor+MessagePath.LABEL_DISABLED.getMessage());
            loreList.addAll(HelperUtil.stringPaginate(iconCommand.description(),loreColor));
        }
        result.addIcon(new InfoIcon(labelColor+MessagePath.MENU_MAIN_QUEST.getMessage(), loreList, iconCommand.iconMaterial(), ROOT_SLOT_QUEST, isClickable));

        /* Stats Book */
        iconCommand = CommandType.STATS;
        loreList.clear();
        if (hasPermission(iconCommand)) {
            isClickable = true;
            loreList.addAll(HelperUtil.stringPaginate(iconCommand.description(),loreColor));
            loreList.add(hintColor+MessagePath.MENU_HELP_HINT.getMessage());
        } else {
            isClickable = false;
            loreList.add(alertColor+MessagePath.LABEL_NO_PERMISSION.getMessage());
            loreList.addAll(HelperUtil.stringPaginate(iconCommand.description(),loreColor));
        }
        result.addIcon(new InfoIcon(labelColor+MessagePath.MENU_MAIN_STATS.getMessage(), loreList, iconCommand.iconMaterial(), ROOT_SLOT_STATS, isClickable));

        /* Prefix Menu */
        iconCommand = CommandType.PREFIX;
        loreList.clear();
        if (getKonquest().getAccomplishmentManager().isEnabled()) {
            if (hasPermission(iconCommand)) {
                isClickable = true;
                loreList.addAll(HelperUtil.stringPaginate(iconCommand.description(),loreColor));
                loreList.add(hintColor+MessagePath.MENU_HELP_HINT.getMessage());
            } else {
                isClickable = false;
                loreList.add(alertColor+MessagePath.LABEL_NO_PERMISSION.getMessage());
                loreList.addAll(HelperUtil.stringPaginate(iconCommand.description(),loreColor));
            }
        } else {
            isClickable = false;
            loreList.add(alertColor+MessagePath.LABEL_DISABLED.getMessage());
            loreList.addAll(HelperUtil.stringPaginate(iconCommand.description(),loreColor));
        }
        result.addIcon(new InfoIcon(labelColor+MessagePath.MENU_MAIN_PREFIX.getMessage(), loreList, iconCommand.iconMaterial(), ROOT_SLOT_PREFIX, isClickable));

        /* Score Menu */
        iconCommand = CommandType.SCORE;
        loreList.clear();
        if (hasPermission(iconCommand)) {
            isClickable = true;
            loreList.addAll(HelperUtil.stringPaginate(iconCommand.description(),loreColor));
            loreList.add(hintColor+MessagePath.MENU_HELP_HINT.getMessage());
        } else {
            isClickable = false;
            loreList.add(alertColor+MessagePath.LABEL_NO_PERMISSION.getMessage());
            loreList.addAll(HelperUtil.stringPaginate(iconCommand.description(),loreColor));
        }
        result.addIcon(new InfoIcon(labelColor+MessagePath.MENU_MAIN_SCORE.getMessage(), loreList, iconCommand.iconMaterial(), ROOT_SLOT_SCORE, isClickable));

        /* Travel Menu */
        iconCommand = CommandType.TRAVEL;
        loreList.clear();
        if (hasPermission(iconCommand)) {
            isClickable = true;
            loreList.addAll(HelperUtil.stringPaginate(iconCommand.description(),loreColor));
            loreList.add(hintColor+MessagePath.MENU_HELP_HINT.getMessage());
        } else {
            isClickable = false;
            loreList.add(alertColor+MessagePath.LABEL_NO_PERMISSION.getMessage());
            loreList.addAll(HelperUtil.stringPaginate(iconCommand.description(),loreColor));
        }
        result.addIcon(new InfoIcon(labelColor+MessagePath.MENU_MAIN_TRAVEL.getMessage(), loreList, iconCommand.iconMaterial(), ROOT_SLOT_TRAVEL, isClickable));

        /* Navigation */
        addNavEmpty(result);
        addNavClose(result);

        /* Secret */
        result.addIcon(new InfoIcon(" ", Collections.emptyList(), Material.GRAY_STAINED_GLASS_PANE, ROOT_SLOT_SECRET, true));

        return result;
    }

    /**
     * Creates the secret view.
     * This is a single page view.
     */
    private DisplayMenu createSecretView() {
        DisplayMenu result;
        List<String> loreList = new ArrayList<>();

        result = new DisplayMenu(1, titleColor+"Secret Menu");

        /* TODO */
        loreList.clear();
        loreList.addAll(HelperUtil.stringPaginate("Placeholder icon",loreColor));
        loreList.add(hintColor+"Do not click");
        result.addIcon(new InfoIcon(labelColor+"TODO", loreList, Material.SLIME_BALL, 0, false));

        /* Navigation */
        addNavEmpty(result);
        addNavClose(result);
        addNavReturn(result);

        return result;
    }

    /**
     * Creates the dashboard view.
     * This is a single page view.
     */
    private DisplayMenu createDashboardView() {
        DisplayMenu result;
        List<String> loreList = new ArrayList<>();
        CommandType iconCommand;
        AdminCommandType iconAdminCommand;
        boolean isClickable;

        result = new DisplayMenu(2, titleColor+MessagePath.MENU_MAIN_TITLE_DASHBOARD.getMessage());

        /* Map Auto */
        iconCommand = CommandType.MAP;
        loreList.clear();
        loreList.add(valueColor + getDisplayValue(player.isMapAuto()));
        if (hasPermission(iconCommand)) {
            isClickable = true;
            loreList.add(hintColor+MessagePath.MENU_OPTIONS_HINT.getMessage());
        } else {
            isClickable = false;
            loreList.add(alertColor+MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        result.addIcon(new InfoIcon(labelColor+MessagePath.MENU_MAIN_MAP_AUTO.getMessage(), loreList, iconCommand.iconMaterial(), DASH_SLOT_MAP_AUTO, isClickable));

        /* Kingdom Chat */
        iconCommand = CommandType.CHAT;
        loreList.clear();
        loreList.add(valueColor + getDisplayValue(!player.isGlobalChat()));
        if (hasPermission(iconCommand)) {
            isClickable = true;
            loreList.add(hintColor+MessagePath.MENU_OPTIONS_HINT.getMessage());
        } else {
            isClickable = false;
            loreList.add(alertColor+MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        result.addIcon(new InfoIcon(labelColor+MessagePath.MENU_MAIN_KINGDOM_CHAT.getMessage(), loreList, iconCommand.iconMaterial(), DASH_SLOT_CHAT, isClickable));

        /* Border */
        iconCommand = CommandType.BORDER;
        loreList.clear();
        loreList.add(valueColor + getDisplayValue(player.isBorderDisplay()));
        if (hasPermission(iconCommand)) {
            isClickable = true;
            loreList.add(hintColor+MessagePath.MENU_OPTIONS_HINT.getMessage());
        } else {
            isClickable = false;
            loreList.add(alertColor+MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        result.addIcon(new InfoIcon(labelColor+MessagePath.MENU_MAIN_BORDER.getMessage(), loreList, iconCommand.iconMaterial(), DASH_SLOT_BORDER, isClickable));

        /* Fly */
        iconCommand = CommandType.FLY;
        loreList.clear();
        loreList.add(valueColor + getDisplayValue(player.isFlyEnabled()));
        if (hasPermission(iconCommand)) {
            isClickable = true;
            loreList.add(hintColor+MessagePath.MENU_OPTIONS_HINT.getMessage());
        } else {
            isClickable = false;
            loreList.add(alertColor+MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        result.addIcon(new InfoIcon(labelColor+MessagePath.MENU_MAIN_FLYING.getMessage(), loreList, iconCommand.iconMaterial(), DASH_SLOT_FLY, isClickable));

        /* Bypass */
        iconAdminCommand = AdminCommandType.BYPASS;
        loreList.clear();
        loreList.add(valueColor + getDisplayValue(player.isAdminBypassActive()));
        if (hasPermission(iconAdminCommand)) {
            isClickable = true;
            loreList.add(hintColor+MessagePath.MENU_OPTIONS_HINT.getMessage());
        } else {
            isClickable = false;
            loreList.add(alertColor+MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        result.addIcon(new InfoIcon(labelColor+MessagePath.MENU_MAIN_ADMIN_BYPASS.getMessage(), loreList, iconAdminCommand.iconMaterial(), DASH_SLOT_BYPASS, isClickable));

        // TODO add other commands

        /* Navigation */
        addNavEmpty(result);
        addNavClose(result);
        addNavReturn(result);

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
            } else if (isNavReturn(slot)) {
                // Return to root
                result = setCurrentView(MenuState.ROOT);
            } else if (isNavBack(slot)) {
                result = goPageBack();
            } else if (isNavNext(slot)) {
                result = goPageNext();
            }
        } else if (isCurrentMenuSlot(slot)) {
            // Clicked in menu
            DisplayMenu view = getCurrentView();
            if (view == null) return null;
            switch ((MenuState)getCurrentState()) {
                case ROOT:
                    switch (slot) {
                        case ROOT_SLOT_HELP:
                            // Open Help Menu (close this menu)
                            getKonquest().getDisplayManager().displayHelpMenu(player);
                            break;
                        case ROOT_SLOT_DASH:
                            // Open Dashboard view
                            result = setCurrentView(MenuState.DASHBOARD);
                            break;
                        case ROOT_SLOT_KINGDOM:
                            // Open Kingdom Menu (close this menu)
                            getKonquest().getDisplayManager().displayKingdomMenu(player);
                            break;
                        case ROOT_SLOT_INFO:
                            // Open Info Menu (close this menu)
                            // TODO
                            break;
                        case ROOT_SLOT_TOWN:
                            // Open Town Menu (close this menu)
                            // TODO
                            break;
                        case ROOT_SLOT_QUEST:
                            // Open Quest Book (close this menu)
                            getKonquest().getDirectiveManager().displayBook(player);
                            break;
                        case ROOT_SLOT_STATS:
                            // Open Stats Book (close this menu)
                            getKonquest().getAccomplishmentManager().displayStats(player);
                            break;
                        case ROOT_SLOT_PREFIX:
                            // Open Prefix Menu (close this menu)
                            // TODO
                            break;
                        case ROOT_SLOT_SCORE:
                            // Open Score Menu (close this menu)
                            // TODO
                            break;
                        case ROOT_SLOT_TRAVEL:
                            // Open Travel Menu (close this menu)
                            // TODO
                            break;
                        case ROOT_SLOT_SECRET:
                            // Open Secret view
                            result = setCurrentView(MenuState.SECRET);
                            break;
                    }
                    break;
                case DASHBOARD:
                    switch (slot) {
                        case DASH_SLOT_MAP_AUTO:
                            // Toggle map auto
                            player.setIsMapAuto(!player.isMapAuto());
                            break;
                        case DASH_SLOT_CHAT:
                            // Toggle kingdom chat
                            player.setIsGlobalChat(!player.isGlobalChat());
                            break;
                        case DASH_SLOT_BORDER:
                            // Toggle border display
                            player.setIsBorderDisplay(!player.isBorderDisplay());
                            break;
                        case DASH_SLOT_FLY:
                            // Toggle flying
                            player.setIsFlyEnabled(!player.isFlyEnabled());
                            break;
                        case DASH_SLOT_BYPASS:
                            // Toggle admin bypass
                            player.setIsAdminBypassActive(!player.isAdminBypassActive());
                            break;
                    }
                    // Keep menu open, refresh view
                    result = updateView(createDashboardView(), MenuState.DASHBOARD);
                    break;
                case SECRET:
                    //TODO do something
                    result = view;
                    break;
                default:
                    break;
            }
        }
        return result;
    }

    private boolean hasPermission(CommandType cmd) {
        return player.getBukkitPlayer().hasPermission(cmd.permission());
    }

    private boolean hasPermission(AdminCommandType cmd) {
        return player.getBukkitPlayer().hasPermission(cmd.permission());
    }

    private String getDisplayValue(boolean value) {
        return DisplayManager.boolean2Lang(value) + " " + DisplayManager.boolean2Symbol(value);
    }
}
