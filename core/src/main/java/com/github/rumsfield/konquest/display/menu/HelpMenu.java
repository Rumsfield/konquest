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
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.HelperUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class HelpMenu extends StateMenu {

    enum MenuState implements State {
        ROOT,
        START,
        COMMUNITY,
        MAIN,
        COMMANDS,
        COMMANDS_ADMIN,
        QUEST
    }

    private final String titleColor = DisplayManager.titleFormat;
    private final String loreColor = DisplayManager.loreFormat;
    private final String hintColor = DisplayManager.hintFormat;
    private final String labelColor = ""+ChatColor.GOLD;

    private final KonPlayer player;

    public HelpMenu(Konquest konquest, KonPlayer player) {
        super(konquest, MenuState.ROOT, null);
        this.player = player;

        /* Initialize menu views */
        initView(createRootView(), MenuState.ROOT);
        initView(createStartView(), MenuState.START);
        initView(createCommandView(MenuState.COMMANDS), MenuState.COMMANDS);
        initView(createCommandView(MenuState.COMMANDS_ADMIN), MenuState.COMMANDS_ADMIN);

        /* Set current menu view */
        setCurrentView(MenuState.ROOT);
    }

    /**
     * Creates the root menu view for this menu.
     * This is a single page view.
     */
    private DisplayMenu createRootView() {
        DisplayMenu result;
        MenuIcon icon;
        List<String> loreList = new ArrayList<>();

        /* Icon slot indexes */
        // Row 0: 0  1  2  3  4  5  6  7  8
        int ROOT_SLOT_START 			= 0;
        int ROOT_SLOT_COMMUNITY		    = 2;
        int ROOT_SLOT_MAIN 			    = 4;
        int ROOT_SLOT_COMMANDS 		    = 6;
        int ROOT_SLOT_COMMANDS_ADMIN 	= 8;

        result = new DisplayMenu(1, titleColor+MessagePath.MENU_HELP_TITLE.getMessage());

        /* Getting Started */
        loreList.clear();
        loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_HELP_DESCRIPTION_START.getMessage(),loreColor));
        loreList.add(hintColor+MessagePath.MENU_HELP_HINT.getMessage());
        icon = new InfoIcon(labelColor+MessagePath.MENU_HELP_START.getMessage(), loreList, Material.PAPER, ROOT_SLOT_START, true);
        icon.setState(MenuState.START);
        result.addIcon(icon);

        /* Community Link */
        String communityLink = getKonquest().getCore().getString(CorePath.COMMUNITY_LINK.getPath(),"");
        loreList.clear();
        loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_HELP_DESCRIPTION_COMMUNITY.getMessage(),loreColor));
        loreList.add(hintColor+MessagePath.MENU_HELP_HINT.getMessage());
        InfoIcon communityIcon = new InfoIcon(labelColor+MessagePath.MENU_HELP_COMMUNITY.getMessage(), loreList, Material.MINECART, ROOT_SLOT_COMMUNITY, true);
        communityIcon.setInfo(""+ChatColor.LIGHT_PURPLE+ChatColor.UNDERLINE+communityLink);
        communityIcon.setState(MenuState.COMMUNITY);
        result.addIcon(communityIcon);

        /* Main Menu */
        loreList.clear();
        loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_HELP_DESCRIPTION_MAIN.getMessage(),loreColor));
        loreList.add(hintColor+MessagePath.MENU_HELP_HINT.getMessage());
        icon = new InfoIcon(labelColor+MessagePath.MENU_HELP_MAIN.getMessage(), loreList, Material.LADDER, ROOT_SLOT_MAIN, true);
        icon.setState(MenuState.MAIN);
        result.addIcon(icon);

        /* Commands */
        loreList.clear();
        loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_HELP_DESCRIPTION_COMMANDS.getMessage(),loreColor));
        loreList.add(hintColor+MessagePath.MENU_HELP_HINT.getMessage());
        icon = new InfoIcon(labelColor+MessagePath.MENU_HELP_COMMANDS.getMessage(), loreList, Material.COMPASS, ROOT_SLOT_COMMANDS, true);
        icon.setState(MenuState.COMMANDS);
        result.addIcon(icon);

        /* Admin Commands */
        loreList.clear();
        loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_HELP_DESCRIPTION_ADMIN.getMessage(),loreColor));
        loreList.add(hintColor+MessagePath.MENU_HELP_HINT.getMessage());
        icon = new InfoIcon(labelColor+MessagePath.MENU_HELP_ADMIN.getMessage(), loreList, Material.NETHER_STAR, ROOT_SLOT_COMMANDS_ADMIN, true);
        icon.setState(MenuState.COMMANDS_ADMIN);
        result.addIcon(icon);

        /* Navigation */
        addNavEmpty(result);
        addNavClose(result);

        return result;
    }

    /**
     * Creates the getting started view.
     * This is a single page view.
     */
    private DisplayMenu createStartView() {
        DisplayMenu result;
        MenuIcon icon;
        List<String> loreList = new ArrayList<>();
        int numRows = 1;
        boolean isQuestEnabled = false;

        /* Icon slot indexes */
        // Row 1: 9 10 11 12 13 14 15 16 17
        int START_SLOT_QUEST 	        = 13;

        // Check for enabled quests
        if (getKonquest().getDirectiveManager().isEnabled()) {
            numRows = 2;
            isQuestEnabled = true;
        }

        result = new DisplayMenu(numRows, titleColor+MessagePath.MENU_HELP_START.getMessage());

        /* Tips */
        String[] tips = {
                MessagePath.MENU_HELP_TIP_1.getMessage(),
                MessagePath.MENU_HELP_TIP_2.getMessage(),
                MessagePath.MENU_HELP_TIP_3.getMessage(),
                MessagePath.MENU_HELP_TIP_4.getMessage(),
                MessagePath.MENU_HELP_TIP_5.getMessage(),
                MessagePath.MENU_HELP_TIP_6.getMessage(),
                MessagePath.MENU_HELP_TIP_7.getMessage(),
                MessagePath.MENU_HELP_TIP_8.getMessage(),
                MessagePath.MENU_HELP_TIP_9.getMessage()
        };
        Material[] iconMaterials = {
                Material.ORANGE_BANNER,
                Material.YELLOW_BANNER,
                Material.LIME_BANNER,
                Material.GREEN_BANNER,
                Material.CYAN_BANNER,
                Material.LIGHT_BLUE_BANNER,
                Material.BLUE_BANNER,
                Material.PURPLE_BANNER,
                Material.MAGENTA_BANNER
        };
        for(int i = 0; i < 9; i++) {
            String tip = tips[i];
            Material iconMaterial = iconMaterials[i];
            loreList = new ArrayList<>(HelperUtil.stringPaginate(tip, ChatColor.LIGHT_PURPLE));
            result.addIcon(new InfoIcon(MessagePath.LABEL_INFORMATION.getMessage(), loreList, iconMaterial, i, false));
        }

        /* Quest Book */
        if (isQuestEnabled) {
            loreList.clear();
            loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_HELP_DESCRIPTION_QUEST.getMessage(), loreColor));
            loreList.add(hintColor + MessagePath.MENU_HELP_HINT.getMessage());
            icon = new InfoIcon(labelColor + MessagePath.MENU_HELP_QUEST.getMessage(), loreList, Material.WRITABLE_BOOK, START_SLOT_QUEST, true);
            icon.setState(MenuState.QUEST);
            result.addIcon(icon);
        }

        /* Navigation */
        addNavEmpty(result);
        addNavClose(result);
        addNavReturn(result);

        return result;
    }

    /**
     * Creates the command or admin views, given the state context.
     * This can be a multiple paged view.
     */
    private ArrayList<DisplayMenu> createCommandView(MenuState context) {
        // Context-specific info
        ArrayList<MenuIcon> commandIcons = new ArrayList<>();
        String title = "";
        int index;
        /* Commands (by context) */
        switch (context) {
            case COMMANDS:
                title = MessagePath.MENU_HELP_TITLE_COMMANDS.getMessage();
                double cost_spy = getKonquest().getCore().getDouble(CorePath.FAVOR_COST_SPY.getPath(),0.0);
                double cost_settle = getKonquest().getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SETTLE.getPath(),0.0);
                double cost_settle_incr = getKonquest().getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SETTLE_INCREMENT.getPath(),0.0);
                double cost_claim = getKonquest().getCore().getDouble(CorePath.FAVOR_COST_CLAIM.getPath(),0.0);
                double cost_travel = getKonquest().getCore().getDouble(CorePath.FAVOR_COST_TRAVEL.getPath(),0.0);
                int cost;
                int cost_incr;
                index = 0;
                for(CommandType cmd : CommandType.values()) {
                    switch (cmd) {
                        case SPY:
                            cost = (int) cost_spy;
                            cost_incr = 0;
                            break;
                        case SETTLE:
                            cost = (int) cost_settle;
                            cost_incr = (int) cost_settle_incr;
                            break;
                        case CLAIM:
                            cost = (int) cost_claim;
                            cost_incr = 0;
                            break;
                        case TRAVEL:
                            cost = (int) cost_travel;
                            cost_incr = 0;
                            break;
                        default:
                            cost = 0;
                            cost_incr = 0;
                            break;
                    }
                    boolean isPermission = player.getBukkitPlayer().hasPermission(cmd.permission());
                    commandIcons.add(new CommandIcon(cmd, isPermission, cost, cost_incr, index));
                    index++;
                }
                break;
            case COMMANDS_ADMIN:
                title = MessagePath.MENU_HELP_TITLE_ADMIN.getMessage();
                index = 0;
                for(AdminCommandType adminCmd : AdminCommandType.values()) {
                    boolean isPermission = player.getBukkitPlayer().hasPermission(adminCmd.permission());
                    commandIcons.add(new AdminCommandIcon(adminCmd, isPermission, index));
                    index++;
                }
                break;
        }
        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(commandIcons, title));
    }

    /**
     * Change the menu's state based on the clicked inventory slot and type of click (right or left mouse).
     * Assume a clickable icon was clicked and visible to the player.
     * Returning a null value will close the menu.
     * @param slot The inventory slot of the current view that was clicked
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
            MenuIcon clickedIcon = view.getIcon(slot);
            MenuState currentState = (MenuState)getCurrentState();
            if (currentState == null) return null;
            MenuState nextState = (MenuState)clickedIcon.getState(); // could be null in some states
            switch (currentState) {
                case ROOT:
                    // Root view, use stored icon state
                    if (nextState == null) return null;
                    switch (nextState) {
                        case START:
                        case COMMANDS:
                        case COMMANDS_ADMIN:
                            // Go to next state as defined by icon
                            result = setCurrentView(nextState);
                            break;
                        case COMMUNITY:
                            // Display community link and close menu
                            if (clickedIcon instanceof InfoIcon) {
                                ChatUtil.sendNotice(player.getBukkitPlayer(), ((InfoIcon)clickedIcon).getInfo());
                            }
                            break;
                        case MAIN:
                            // Go to Main Menu and close this menu
                            getKonquest().getDisplayManager().displayMainMenu(player);
                            break;
                    }
                    break;
                case START:
                    if (nextState != null && nextState.equals(MenuState.QUEST)) {
                        // Open Quest Book and close menu
                        getKonquest().getDirectiveManager().displayBook(player);
                    }
                    break;
                case COMMANDS:
                    // Display command usage in chat, but keep menu open to current view
                    if (clickedIcon instanceof CommandIcon) {
                        CommandIcon icon = (CommandIcon) clickedIcon;
                        for (String usageLine : icon.getCommand().argumentUsage()) {
                            ChatUtil.sendNotice(player.getBukkitPlayer(), usageLine);
                        }
                    }
                    result = view;
                    break;
                case COMMANDS_ADMIN:
                    // Display command usage in chat, but keep menu open to current view
                    if (clickedIcon instanceof AdminCommandIcon) {
                        AdminCommandIcon icon = (AdminCommandIcon) clickedIcon;
                        for (String usageLine : icon.getCommand().argumentUsage()) {
                            ChatUtil.sendNotice(player.getBukkitPlayer(), usageLine);
                        }
                    }
                    result = view;
                    break;
                default:
                    break;
            }
        }
        return result;
    }
}
