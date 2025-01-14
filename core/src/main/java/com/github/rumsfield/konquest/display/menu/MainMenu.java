package com.github.rumsfield.konquest.display.menu;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.CommandType;
import com.github.rumsfield.konquest.command.admin.AdminCommandType;
import com.github.rumsfield.konquest.display.DisplayView;
import com.github.rumsfield.konquest.display.StateMenu;
import com.github.rumsfield.konquest.display.icon.CommandIcon;
import com.github.rumsfield.konquest.display.icon.InfoIcon;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.display.icon.RandomIcon;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonStatsType;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MainMenu extends StateMenu {

    enum MenuState implements State {
        ROOT,
        HELP,
        KINGDOM,
        INFO,
        TOWN,
        QUEST,
        STATS,
        PREFIX,
        SCORE,
        TRAVEL,
        DASHBOARD,
        DASH_MAP_AUTO,
        DASH_CHAT,
        DASH_BORDER,
        DASH_FLY,
        DASH_BYPASS,
        DASH_MAP,
        DASH_FAVOR,
        DASH_SPY,
        SECRET,
        SECRET_RTD
    }

    private final KonPlayer player;

    private final int ROOT_SLOT_SECRET = 34;

    public MainMenu(Konquest konquest, KonPlayer player) {
        super(konquest, HelpMenu.MenuState.ROOT, null);
        this.player = player;

        /* Initialize menu view */
        setCurrentView(MenuState.ROOT);
    }

    public DisplayView goToDashboard() {
        // Change to dashboard view
        return refreshNewView(MenuState.DASHBOARD);
    }

    /**
     * Creates the root menu view for this menu.
     * This is a single page view.
     */
    private DisplayView createRootView() {
        DisplayView result;
        MenuIcon icon;
        CommandType iconCommand;
        boolean isClickable;

        /* Icon slot indexes */
        // Row 0:  0  1  2  3  4  5  6  7  8
        int ROOT_SLOT_HELP 	    = 1;
        int ROOT_SLOT_DASH 	    = 4;
        int ROOT_SLOT_KINGDOM 	= 7;
        // Row 1:  9 10 11 12 13 14 15 16 17
        int ROOT_SLOT_INFO 		= 10;
        int ROOT_SLOT_TOWN		= 16;
        // Row 2: 18 19 20 21 22 23 24 25 26
        int ROOT_SLOT_QUEST 	= 19;
        int ROOT_SLOT_STATS 	= 21;
        int ROOT_SLOT_PREFIX    = 22;
        int ROOT_SLOT_SCORE 	= 23;
        int ROOT_SLOT_TRAVEL 	= 25;

        result = new DisplayView(3, MessagePath.MENU_MAIN_TITLE.getMessage());

        /* Help Menu */
        icon = new InfoIcon(MessagePath.MENU_MAIN_HELP.getMessage(), CommandType.HELP.iconMaterial(), ROOT_SLOT_HELP, true);
        icon.addDescription(MessagePath.MENU_MAIN_DESCRIPTION_HELP.getMessage());
        icon.addHint(MessagePath.MENU_MAIN_HINT.getMessage());
        icon.setState(MenuState.HELP);
        result.addIcon(icon);

        /* Dashboard */
        icon = new InfoIcon(MessagePath.MENU_MAIN_DASHBOARD.getMessage(), Material.CLOCK, ROOT_SLOT_DASH, true);
        icon.addDescription(MessagePath.MENU_MAIN_DESCRIPTION_DASHBOARD.getMessage());
        icon.addHint(MessagePath.MENU_MAIN_HINT.getMessage());
        icon.setState(MenuState.DASHBOARD);
        result.addIcon(icon);

        /* Kingdom Menu */
        iconCommand = CommandType.KINGDOM;
        isClickable = hasPermission(iconCommand);
        icon = new InfoIcon(MessagePath.MENU_MAIN_KINGDOM.getMessage(), iconCommand.iconMaterial(), ROOT_SLOT_KINGDOM, isClickable);
        icon.addDescription(MessagePath.MENU_MAIN_DESCRIPTION_KINGDOM.getMessage());
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        icon.setState(MenuState.KINGDOM);
        result.addIcon(icon);

        /* Info Menu */
        iconCommand = CommandType.INFO;
        isClickable = hasPermission(iconCommand);
        icon = new InfoIcon(MessagePath.MENU_MAIN_INFO.getMessage(), iconCommand.iconMaterial(), ROOT_SLOT_INFO, isClickable);
        icon.addDescription(MessagePath.MENU_MAIN_DESCRIPTION_INFO.getMessage());
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        icon.setState(MenuState.INFO);
        result.addIcon(icon);

        /* Town Menu (Unavailable to barbarians) */
        iconCommand = CommandType.TOWN;
        isClickable = !player.isBarbarian() && hasPermission(iconCommand);
        icon = new InfoIcon(MessagePath.MENU_MAIN_TOWN.getMessage(), iconCommand.iconMaterial(), ROOT_SLOT_TOWN, isClickable);
        icon.addDescription(MessagePath.MENU_MAIN_DESCRIPTION_TOWN.getMessage());
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        } else {
            if (player.isBarbarian()) {
                icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
            } else {
                icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
            }
        }
        icon.setState(MenuState.TOWN);
        result.addIcon(icon);

        /* Quest Book */
        iconCommand = CommandType.QUEST;
        isClickable = getKonquest().getDirectiveManager().isEnabled() && hasPermission(iconCommand);
        icon = new InfoIcon(MessagePath.MENU_MAIN_QUEST.getMessage(), iconCommand.iconMaterial(), ROOT_SLOT_QUEST, isClickable);
        icon.addDescription(MessagePath.MENU_MAIN_DESCRIPTION_QUEST.getMessage());
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        } else {
            if (getKonquest().getDirectiveManager().isEnabled()) {
                icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
            } else {
                icon.addAlert(MessagePath.LABEL_DISABLED.getMessage());
            }
        }
        icon.setState(MenuState.QUEST);
        result.addIcon(icon);

        /* Stats Book */
        iconCommand = CommandType.STATS;
        isClickable = hasPermission(iconCommand);
        icon = new InfoIcon(MessagePath.MENU_MAIN_STATS.getMessage(), iconCommand.iconMaterial(), ROOT_SLOT_STATS, isClickable);
        icon.addDescription(MessagePath.MENU_MAIN_DESCRIPTION_STATS.getMessage());
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        icon.setState(MenuState.STATS);
        result.addIcon(icon);

        /* Prefix Menu */
        iconCommand = CommandType.PREFIX;
        isClickable = getKonquest().getAccomplishmentManager().isEnabled() && hasPermission(iconCommand);
        icon = new InfoIcon(MessagePath.MENU_MAIN_PREFIX.getMessage(), iconCommand.iconMaterial(), ROOT_SLOT_PREFIX, isClickable);
        icon.addDescription(MessagePath.MENU_MAIN_DESCRIPTION_PREFIX.getMessage());
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        } else {
            if (getKonquest().getAccomplishmentManager().isEnabled()) {
                icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
            } else {
                icon.addAlert(MessagePath.LABEL_DISABLED.getMessage());
            }
        }
        icon.setState(MenuState.PREFIX);
        result.addIcon(icon);

        /* Score Menu */
        iconCommand = CommandType.SCORE;
        isClickable = hasPermission(iconCommand);
        icon = new InfoIcon(MessagePath.MENU_MAIN_SCORE.getMessage(), iconCommand.iconMaterial(), ROOT_SLOT_SCORE, isClickable);
        icon.addDescription(MessagePath.MENU_MAIN_DESCRIPTION_SCORE.getMessage());
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        icon.setState(MenuState.SCORE);
        result.addIcon(icon);

        /* Travel Menu */
        iconCommand = CommandType.TRAVEL;
        isClickable = hasPermission(iconCommand);
        icon = new InfoIcon(MessagePath.MENU_MAIN_TRAVEL.getMessage(), iconCommand.iconMaterial(), ROOT_SLOT_TRAVEL, isClickable);
        icon.addDescription(MessagePath.MENU_MAIN_DESCRIPTION_TRAVEL.getMessage());
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_NO_PERMISSION.getMessage());
        }
        icon.setState(MenuState.TRAVEL);
        result.addIcon(icon);

        /* Navigation */
        addNavEmpty(result);
        addNavClose(result);

        /* Secret */
        icon = new InfoIcon(" ", Material.GRAY_STAINED_GLASS_PANE, ROOT_SLOT_SECRET, true);
        icon.setState(MenuState.SECRET);
        result.addIcon(icon);

        return result;
    }

    /**
     * Creates the secret view.
     * This is a single page view.
     */
    private DisplayView createSecretView() {
        DisplayView result;
        MenuIcon icon;

        result = new DisplayView(1, "Secret Menu");

        /* Roll The Dice */
        icon = new RandomIcon("Roll The Dice", 4, true);
        icon.addDescription(ChatColor.MAGIC+"Gain a mysterious ability.");
        icon.setState(MenuState.SECRET_RTD);
        result.addIcon(icon);

        // Periodically refresh icons
        result.setRefreshTickRate(5);

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
    private DisplayView createDashboardView() {
        DisplayView result;
        MenuIcon icon;
        CommandType iconCommand;
        AdminCommandType iconAdminCommand;
        Material iconMaterial;
        boolean isClickable;
        boolean isPermission;
        boolean currentValue;

        /* Icon slot indexes */
        // Row 0:  0  1  2  3  4  5  6  7  8
        int DASH_SLOT_MAP_AUTO 	= 2;
        int DASH_SLOT_CHAT 	    = 3;
        int DASH_SLOT_BORDER 	= 4;
        int DASH_SLOT_FLY 	    = 5;
        int DASH_SLOT_BYPASS 	= 6;
        // Row 1:  9 10 11 12 13 14 15 16 17
        int DASH_SLOT_MAP 	    = 12;
        int DASH_SLOT_FAVOR 	= 13;
        int DASH_SLOT_SPY 	    = 14;

        result = new DisplayView(2, MessagePath.MENU_MAIN_TITLE_DASHBOARD.getMessage());

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
        icon.setState(MenuState.DASH_MAP_AUTO);
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
        icon.setState(MenuState.DASH_CHAT);
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
        icon.setState(MenuState.DASH_BORDER);
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
        icon.setState(MenuState.DASH_FLY);
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
        icon.setState(MenuState.DASH_BYPASS);
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
        icon.setState(MenuState.DASH_MAP);
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
        icon.setState(MenuState.DASH_FAVOR);
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
        icon.setState(MenuState.DASH_SPY);
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
    public ArrayList<DisplayView> createView(State context) {
        ArrayList<DisplayView> result = new ArrayList<>();
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
    public DisplayView updateState(int slot, boolean clickType) {
        DisplayView result = null;
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
            DisplayView view = getCurrentView();
            if (view == null) return null;
            MenuIcon clickedIcon = view.getIcon(slot);
            MenuState nextState = (MenuState)clickedIcon.getState();
            if (nextState == null) return null;
            switch ((MenuState)getCurrentState()) {
                case ROOT:
                    switch (nextState) {
                        case HELP:
                            // Open Help Menu (close this menu)
                            getKonquest().getDisplayManager().displayHelpMenu(player);
                            break;
                        case DASHBOARD:
                            // Open Dashboard view
                            result = setCurrentView(MenuState.DASHBOARD);
                            break;
                        case KINGDOM:
                            // Open Kingdom Menu (close this menu)
                            getKonquest().getDisplayManager().displayKingdomMenu(player);
                            break;
                        case INFO:
                            // Open Info Menu (close this menu)
                            getKonquest().getDisplayManager().displayInfoMenu(player);
                            break;
                        case TOWN:
                            // Open Town Menu (close this menu)
                            getKonquest().getDisplayManager().displayTownMenu(player);
                            break;
                        case QUEST:
                            // Open Quest Book (close this menu)
                            // Schedule delayed task to open book
                            Bukkit.getScheduler().scheduleSyncDelayedTask(getKonquest().getPlugin(), () -> getKonquest().getDirectiveManager().displayBook(player),5);
                            break;
                        case STATS:
                            // Open Stats Book (close this menu)
                            // Schedule delayed task to open book
                            Bukkit.getScheduler().scheduleSyncDelayedTask(getKonquest().getPlugin(), () -> getKonquest().getAccomplishmentManager().displayStats(player),5);
                            break;
                        case PREFIX:
                            // Open Prefix Menu (close this menu)
                            getKonquest().getDisplayManager().displayPrefixMenu(player);
                            break;
                        case SCORE:
                            // Open Score Menu (close this menu)
                            getKonquest().getDisplayManager().displayScoreMenu(player);
                            break;
                        case TRAVEL:
                            // Open Travel Menu (close this menu)
                            getKonquest().getDisplayManager().displayTravelMenu(player);
                            break;
                    }
                    break;
                case DASHBOARD:
                    boolean doRefresh = true;
                    switch (nextState) {
                        case DASH_MAP_AUTO:
                            // Toggle map auto
                            player.setIsMapAuto(!player.isMapAuto());
                            playStatusSound(player.getBukkitPlayer(),true);
                            break;
                        case DASH_CHAT:
                            // Toggle kingdom chat
                            player.setIsGlobalChat(!player.isGlobalChat());
                            playStatusSound(player.getBukkitPlayer(),true);
                            break;
                        case DASH_BORDER:
                            // Toggle border display
                            player.setIsBorderDisplay(!player.isBorderDisplay());
                            // Update borders
                            getKonquest().getTerritoryManager().updatePlayerBorderParticles(player);
                            playStatusSound(player.getBukkitPlayer(),true);
                            break;
                        case DASH_FLY:
                            // Toggle flying
                            boolean status = getKonquest().getPlayerManager().togglePlayerFly(player);
                            playStatusSound(player.getBukkitPlayer(),status);
                            break;
                        case DASH_BYPASS:
                            // Toggle admin bypass
                            player.setIsAdminBypassActive(!player.isAdminBypassActive());
                            playStatusSound(player.getBukkitPlayer(),true);
                            break;
                        case DASH_MAP:
                        case DASH_FAVOR:
                        case DASH_SPY:
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
                    if (nextState.equals(MenuState.SECRET_RTD)) {
                        rollTheDice();
                    }
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

    // Do something random to the player
    private void rollTheDice() {
        try {
            Location playerLoc = player.getBukkitPlayer().getLocation();
            boolean isHit = false;
            int index = ThreadLocalRandom.current().nextInt(0, 100);
            switch (index) {
                case 0:
                    // Lightning strike
                    isHit = true;
                    Bukkit.getScheduler().scheduleSyncDelayedTask(getKonquest().getPlugin(), () -> {
                        player.getBukkitPlayer().getWorld().strikeLightning(playerLoc);
                    },40);
                    break;
                case 10:
                    // Poison potion effect
                    isHit = true;
                    player.getBukkitPlayer().addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 60, 1));
                    break;
                case 20:
                    // Hunger potion effect
                    isHit = true;
                    player.getBukkitPlayer().addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 20 * 120, 1));
                    break;
                case 30:
                    // Weakness potion effect
                    isHit = true;
                    player.getBukkitPlayer().addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 120, 1));
                    break;
                case 40:
                    // Health Boost potion effect
                    isHit = true;
                    player.getBukkitPlayer().addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 20 * 600, 2));
                    break;
                case 50:
                    // Give grass item
                    isHit = true;
                    giveItems(new ItemStack(Material.GRASS_BLOCK,3));
                    break;
                case 60:
                    // Give poisonous potato item
                    isHit = true;
                    giveItems(new ItemStack(Material.POISONOUS_POTATO,5));
                    break;
                case 70:
                    // Give leather boots item
                    isHit = true;
                    giveItems(new ItemStack(Material.LEATHER_BOOTS,1));
                    break;
                case 80:
                    // Give stick item
                    isHit = true;
                    giveItems(new ItemStack(Material.STICK,7));
                    break;
                case 90:
                    // Increase a random stat
                    isHit = true;
                    int statIndex = ThreadLocalRandom.current().nextInt(0, KonStatsType.values().length);
                    KonStatsType randomStat = KonStatsType.values()[statIndex];
                    int addValue = 1;
                    getKonquest().getAccomplishmentManager().modifyPlayerStat(player,randomStat,addValue);
                    int newValue = player.getPlayerStats().getStat(randomStat);
                    ChatUtil.sendNotice(player, MessagePath.COMMAND_ADMIN_STAT_NOTICE_ADD.getMessage(addValue,randomStat.toString(),player.getBukkitPlayer().getName(),newValue));
                    break;
                default:
                    break;
            }
            playStatusSound(player.getBukkitPlayer(), isHit);
        } catch (Exception | Error issue) {
            ChatUtil.printConsoleError("Failed to apply random effect to player "+player.getBukkitPlayer().getName());
            issue.printStackTrace();
        }
    }

    private void giveItems(ItemStack items) {
        HashMap<Integer,ItemStack> leftovers = player.getBukkitPlayer().getInventory().addItem(items);
        for (ItemStack item : leftovers.values()) {
            player.getBukkitPlayer().getWorld().dropItem(player.getBukkitPlayer().getLocation(),item);
        }
    }
}
