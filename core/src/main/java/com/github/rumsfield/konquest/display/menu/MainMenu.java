package com.github.rumsfield.konquest.display.menu;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandType;
import com.github.rumsfield.konquest.command.admin.AdminCommandType;
import com.github.rumsfield.konquest.display.DisplayMenu;
import com.github.rumsfield.konquest.display.StateMenu;
import com.github.rumsfield.konquest.display.icon.CommandIcon;
import com.github.rumsfield.konquest.display.icon.InfoIcon;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.ArrayList;
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
    // Row 1:  9 10 11 12 13 14 15 16 17
    private final int DASH_SLOT_MAP 	    = 12;
    private final int DASH_SLOT_FAVOR 	    = 13;
    private final int DASH_SLOT_SPY 	    = 14;

    private final KonPlayer player;

    public MainMenu(Konquest konquest, KonPlayer player) {
        super(konquest, HelpMenu.MenuState.ROOT, null);
        this.player = player;

        /* Initialize menu view */
        setCurrentView(MenuState.ROOT);
    }

    public DisplayMenu goToDashboard() {
        // Change to dashboard view
        return refreshNewView(MenuState.DASHBOARD);
    }

    /**
     * Creates the root menu view for this menu.
     * This is a single page view.
     */
    private DisplayMenu createRootView() {
        DisplayMenu result;
        MenuIcon icon;
        CommandType iconCommand;
        boolean isClickable;

        result = new DisplayMenu(3, MessagePath.MENU_MAIN_TITLE.getMessage());

        /* Help Menu */
        icon = new InfoIcon(MessagePath.MENU_MAIN_HELP.getMessage(), CommandType.HELP.iconMaterial(), ROOT_SLOT_HELP, true);
        icon.addDescription(MessagePath.DESCRIPTION_HELP.getMessage());
        icon.addHint(MessagePath.MENU_MAIN_HINT.getMessage());
        result.addIcon(icon);

        /* Dashboard */
        icon = new InfoIcon(MessagePath.MENU_MAIN_DASHBOARD.getMessage(), Material.CLOCK, ROOT_SLOT_DASH, true);
        icon.addDescription(MessagePath.MENU_MAIN_DESCRIPTION_DASHBOARD.getMessage());
        icon.addHint(MessagePath.MENU_MAIN_HINT.getMessage());
        result.addIcon(icon);

        /* Kingdom Menu */
        iconCommand = CommandType.KINGDOM;
        isClickable = hasPermission(iconCommand);
        icon = new InfoIcon(MessagePath.MENU_MAIN_KINGDOM.getMessage(), iconCommand.iconMaterial(), ROOT_SLOT_KINGDOM, isClickable);
        icon.addDescription(iconCommand.description());
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        result.addIcon(icon);

        /* Info Menu */
        iconCommand = CommandType.INFO;
        isClickable = hasPermission(iconCommand);
        icon = new InfoIcon(MessagePath.MENU_MAIN_INFO.getMessage(), iconCommand.iconMaterial(), ROOT_SLOT_INFO, isClickable);
        icon.addDescription(iconCommand.description());
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        result.addIcon(icon);

        /* Town Menu (Unavailable to barbarians) */
        iconCommand = CommandType.TOWN;
        isClickable = !player.isBarbarian() && hasPermission(iconCommand);
        icon = new InfoIcon(MessagePath.MENU_MAIN_TOWN.getMessage(), iconCommand.iconMaterial(), ROOT_SLOT_TOWN, isClickable);
        icon.addDescription(iconCommand.description());
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        } else {
            if (player.isBarbarian()) {
                icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
            } else {
                icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
            }
        }
        result.addIcon(icon);

        /* Quest Book */
        iconCommand = CommandType.QUEST;
        isClickable = getKonquest().getDirectiveManager().isEnabled() && hasPermission(iconCommand);
        icon = new InfoIcon(MessagePath.MENU_MAIN_QUEST.getMessage(), iconCommand.iconMaterial(), ROOT_SLOT_QUEST, isClickable);
        icon.addDescription(iconCommand.description());
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        } else {
            if (getKonquest().getDirectiveManager().isEnabled()) {
                icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
            } else {
                icon.addAlert(MessagePath.LABEL_DISABLED.getMessage());
            }
        }
        result.addIcon(icon);

        /* Stats Book */
        iconCommand = CommandType.STATS;
        isClickable = hasPermission(iconCommand);
        icon = new InfoIcon(MessagePath.MENU_MAIN_STATS.getMessage(), iconCommand.iconMaterial(), ROOT_SLOT_STATS, isClickable);
        icon.addDescription(iconCommand.description());
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        result.addIcon(icon);

        /* Prefix Menu */
        iconCommand = CommandType.PREFIX;
        isClickable = getKonquest().getAccomplishmentManager().isEnabled() && hasPermission(iconCommand);
        icon = new InfoIcon(MessagePath.MENU_MAIN_PREFIX.getMessage(), iconCommand.iconMaterial(), ROOT_SLOT_PREFIX, isClickable);
        icon.addDescription(iconCommand.description());
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        } else {
            if (getKonquest().getAccomplishmentManager().isEnabled()) {
                icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
            } else {
                icon.addAlert(MessagePath.LABEL_DISABLED.getMessage());
            }
        }
        result.addIcon(icon);

        /* Score Menu */
        iconCommand = CommandType.SCORE;
        isClickable = hasPermission(iconCommand);
        icon = new InfoIcon(MessagePath.MENU_MAIN_SCORE.getMessage(), iconCommand.iconMaterial(), ROOT_SLOT_SCORE, isClickable);
        icon.addDescription(iconCommand.description());
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        result.addIcon(icon);

        /* Travel Menu */
        iconCommand = CommandType.TRAVEL;
        isClickable = hasPermission(iconCommand);
        icon = new InfoIcon(MessagePath.MENU_MAIN_TRAVEL.getMessage(), iconCommand.iconMaterial(), ROOT_SLOT_TRAVEL, isClickable);
        icon.addDescription(iconCommand.description());
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        result.addIcon(icon);

        /* Navigation */
        addNavEmpty(result);
        addNavClose(result);

        /* Secret */
        result.addIcon(new InfoIcon(" ", Material.GRAY_STAINED_GLASS_PANE, ROOT_SLOT_SECRET, true));

        return result;
    }

    /**
     * Creates the secret view.
     * This is a single page view.
     */
    private DisplayMenu createSecretView() {
        DisplayMenu result;
        MenuIcon icon;

        result = new DisplayMenu(1, "Secret Menu");

        /* TODO */
        icon = new InfoIcon("TODO", Material.SLIME_BALL, 0, false);
        icon.addDescription("Placeholder icon");
        icon.addHint("Do not click");
        result.addIcon(icon);

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
        MenuIcon icon;
        CommandType iconCommand;
        AdminCommandType iconAdminCommand;
        Material iconMaterial;
        boolean isClickable;
        boolean isPermission;
        boolean currentValue;

        result = new DisplayMenu(2, MessagePath.MENU_MAIN_TITLE_DASHBOARD.getMessage());

        /* Map Auto */
        iconCommand = CommandType.MAP;
        currentValue = player.isMapAuto();
        iconMaterial = currentValue ? Material.LIGHT_GRAY_WOOL : Material.LIGHT_GRAY_CARPET;
        isClickable = hasPermission(iconCommand);
        icon = new InfoIcon(MessagePath.MENU_MAIN_MAP_AUTO.getMessage(), iconMaterial, DASH_SLOT_MAP_AUTO, isClickable);
        icon.addDescription(getDisplayValue(currentValue));
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_CHANGE.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        result.addIcon(icon);

        /* Kingdom Chat */
        iconCommand = CommandType.CHAT;
        currentValue = !player.isGlobalChat();
        iconMaterial = currentValue ? Material.GREEN_WOOL : Material.GREEN_CARPET;
        isClickable = !player.isBarbarian() && hasPermission(iconCommand);
        icon = new InfoIcon(MessagePath.MENU_MAIN_KINGDOM_CHAT.getMessage(), iconMaterial, DASH_SLOT_CHAT, isClickable);
        icon.addDescription(getDisplayValue(currentValue));
        boolean isChatFormatEnabled = getKonquest().getCore().getBoolean(CorePath.CHAT_ENABLE_FORMAT.getPath(),true);
        if (isChatFormatEnabled) {
            if (!player.isBarbarian()) {
                if (hasPermission(iconCommand)) {
                    icon.addHint(MessagePath.MENU_HINT_CHANGE.getMessage());
                } else {
                    icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
                }
            } else {
                icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
            }
        } else {
            icon.addAlert(MessagePath.LABEL_DISABLED.getMessage());
        }
        result.addIcon(icon);

        /* Border */
        iconCommand = CommandType.BORDER;
        currentValue = player.isBorderDisplay();
        iconMaterial = currentValue ? Material.BROWN_WOOL : Material.BROWN_CARPET;
        isClickable = hasPermission(iconCommand);
        icon = new InfoIcon(MessagePath.MENU_MAIN_BORDER.getMessage(), iconMaterial, DASH_SLOT_BORDER, isClickable);
        icon.addDescription(getDisplayValue(currentValue));
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_CHANGE.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        result.addIcon(icon);

        /* Fly */
        iconCommand = CommandType.FLY;
        currentValue = player.isFlyEnabled();
        iconMaterial = currentValue ? Material.LIGHT_BLUE_WOOL : Material.LIGHT_BLUE_CARPET;
        isClickable = hasPermission(iconCommand);
        icon = new InfoIcon(MessagePath.MENU_MAIN_FLYING.getMessage(), iconMaterial, DASH_SLOT_FLY, isClickable);
        icon.addDescription(getDisplayValue(currentValue));
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_CHANGE.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        result.addIcon(icon);

        /* Bypass */
        iconAdminCommand = AdminCommandType.BYPASS;
        currentValue = player.isAdminBypassActive();
        iconMaterial = currentValue ? Material.ORANGE_WOOL : Material.ORANGE_CARPET;
        isClickable = hasPermission(iconAdminCommand);
        icon = new InfoIcon(MessagePath.MENU_MAIN_ADMIN_BYPASS.getMessage(), iconMaterial, DASH_SLOT_BYPASS, isClickable);
        icon.addDescription(getDisplayValue(currentValue));
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_CHANGE.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        result.addIcon(icon);

        /* Map Command */
        iconCommand = CommandType.MAP;
        isPermission = player.getBukkitPlayer().hasPermission(iconCommand.permission());
        icon = new CommandIcon(iconCommand, isPermission, 0, 0, DASH_SLOT_MAP);
        if (isPermission) {
            icon.addHint(MessagePath.MENU_MAIN_HINT_MAP_1.getMessage());
            icon.addHint(MessagePath.MENU_MAIN_HINT_MAP_2.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        result.addIcon(icon);

        /* Favor Command */
        iconCommand = CommandType.FAVOR;
        isPermission = player.getBukkitPlayer().hasPermission(iconCommand.permission());
        icon = new CommandIcon(iconCommand, isPermission, 0, 0, DASH_SLOT_FAVOR);
        if (isPermission) {
            icon.addHint(MessagePath.MENU_MAIN_HINT_FAVOR.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        result.addIcon(icon);

        /* Spy Command */
        iconCommand = CommandType.SPY;
        double cost_spy = getKonquest().getCore().getDouble(CorePath.FAVOR_COST_SPY.getPath(),0.0);
        isPermission = player.getBukkitPlayer().hasPermission(iconCommand.permission());
        icon = new CommandIcon(iconCommand, isPermission, (int)cost_spy, 0, DASH_SLOT_SPY);
        if (isPermission) {
            icon.addHint(MessagePath.MENU_MAIN_HINT_SPY.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        result.addIcon(icon);

        /* Navigation */
        addNavEmpty(result);
        addNavClose(result);
        addNavReturn(result);

        return result;
    }

    /**
     * Create a list of views for a given menu state.
     * This creates new views, specific to each menu.
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
            case DASHBOARD:
                result.add(createDashboardView());
                break;
            case SECRET:
                result.add(createSecretView());
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
            } else if (isNavReturn(slot)) {
                // Return to root
                result = setCurrentView(MenuState.ROOT);
            } else if (isNavBack(slot)) {
                // Page back
                result = goPageBack();
            } else if (isNavNext(slot)) {
                // Page next
                result = goPageNext();
            } else if (slot == ROOT_SLOT_SECRET) {
                // Open Secret view
                result = setCurrentView(MenuState.SECRET);
            }
        } else if (isCurrentMenuSlot(slot)) {
            // Clicked in menu
            DisplayMenu view = getCurrentView();
            if (view == null) return null;
            MenuIcon clickedIcon = view.getIcon(slot);
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
                            getKonquest().getDisplayManager().displayTownMenu(player);
                            break;
                        case ROOT_SLOT_QUEST:
                            // Open Quest Book (close this menu)
                            // Schedule delayed task to open book
                            Bukkit.getScheduler().scheduleSyncDelayedTask(getKonquest().getPlugin(), () -> getKonquest().getDirectiveManager().displayBook(player),5);
                            break;
                        case ROOT_SLOT_STATS:
                            // Open Stats Book (close this menu)
                            // Schedule delayed task to open book
                            Bukkit.getScheduler().scheduleSyncDelayedTask(getKonquest().getPlugin(), () -> getKonquest().getAccomplishmentManager().displayStats(player),5);
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
                    }
                    break;
                case DASHBOARD:
                    boolean doRefresh = true;
                    switch (slot) {
                        case DASH_SLOT_MAP_AUTO:
                            // Toggle map auto
                            player.setIsMapAuto(!player.isMapAuto());
                            playStatusSound(player.getBukkitPlayer(),true);
                            break;
                        case DASH_SLOT_CHAT:
                            // Toggle kingdom chat
                            player.setIsGlobalChat(!player.isGlobalChat());
                            playStatusSound(player.getBukkitPlayer(),true);
                            break;
                        case DASH_SLOT_BORDER:
                            // Toggle border display
                            player.setIsBorderDisplay(!player.isBorderDisplay());
                            // Update borders
                            getKonquest().getTerritoryManager().updatePlayerBorderParticles(player);
                            playStatusSound(player.getBukkitPlayer(),true);
                            break;
                        case DASH_SLOT_FLY:
                            // Toggle flying
                            boolean status = getKonquest().getPlayerManager().togglePlayerFly(player);
                            playStatusSound(player.getBukkitPlayer(),status);
                            break;
                        case DASH_SLOT_BYPASS:
                            // Toggle admin bypass
                            player.setIsAdminBypassActive(!player.isAdminBypassActive());
                            playStatusSound(player.getBukkitPlayer(),true);
                            break;
                        case DASH_SLOT_MAP:
                        case DASH_SLOT_FAVOR:
                        case DASH_SLOT_SPY:
                            // Execute command
                            if (clickedIcon instanceof CommandIcon) {
                                CommandIcon icon = (CommandIcon)clickedIcon;
                                List<String> args = new ArrayList<>();
                                CommandType cmd = icon.getCommand();
                                if (cmd.equals(CommandType.MAP) && !clickType) {
                                    // Right Click, far map
                                    args.add("far");
                                }
                                cmd.command().execute(getKonquest(), player.getBukkitPlayer(),args);
                                if (cmd.equals(CommandType.SPY)) {
                                    doRefresh = false;
                                }
                            }
                            break;
                    }
                    // Keep menu open, refresh view
                    if (doRefresh) {
                        result = refreshCurrentView();
                    }
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
