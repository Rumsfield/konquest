package com.github.rumsfield.konquest.display.menu;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.api.model.KonquestUpgrade;
import com.github.rumsfield.konquest.command.CommandType;
import com.github.rumsfield.konquest.display.DisplayMenu;
import com.github.rumsfield.konquest.display.StateMenu;
import com.github.rumsfield.konquest.display.icon.*;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.manager.KingdomManager;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.*;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Villager;

import java.util.*;

public class TownMenu extends StateMenu {

    enum MenuState implements State {
        // Base menu states
        ROOT,
        JOIN,
        LEAVE,
        MANAGE,
        INVITES,
        LIST,
        // Management menu states
        MANAGEMENT_ROOT,
        A_REQUESTS,
        A_PLOTS,
        A_INFO,
        A_SHIELD,
        A_ARMOR,
        B_PROMOTE,
        B_DEMOTE,
        B_TRANSFER,
        B_DESTROY,
        B_UPGRADES,
        B_OPTIONS,
        B_SPECIALIZATION,
        CONFIRM_YES,
        CONFIRM_NO
    }

    enum AccessType implements Access {
        DEFAULT,
        KNIGHT,
        LORD,
        ADMIN
    }

    private String contextColor;
    private String titleColor;
    private String nameColor;
    private final String propertyColor;
    private final String alertColor;
    private final String loreColor;
    private final String valueColor;
    private final String hintColor;

    private final KingdomManager manager;
    private final KonPlayer player; // The player using the menu
    private final KonKingdom kingdom; // The kingdom of the player
    private final boolean isAdmin;
    private KonTown town; // The town being managed

    public TownMenu(Konquest konquest, KonPlayer player, boolean isAdmin) {
        super(konquest, MenuState.ROOT, null);
        this.player = player;
        this.isAdmin = isAdmin;
        this.manager = konquest.getKingdomManager();
        this.kingdom = player.getKingdom();
        this.town = null;

        /* Formats */
        contextColor = Konquest.friendColor2;
        titleColor = DisplayManager.titleFormat;
        nameColor = DisplayManager.nameFormat;
        propertyColor = DisplayManager.propertyFormat;
        alertColor = DisplayManager.alertFormat;
        loreColor = DisplayManager.loreFormat;
        valueColor = DisplayManager.valueFormat;
        hintColor = DisplayManager.hintFormat;
        if (isAdmin) {
            contextColor = DisplayManager.adminFormat;
            titleColor = DisplayManager.adminFormat;
            nameColor = DisplayManager.adminFormat;
        }

        /* Initialize menu access */
        updateAccess();

        /* Initialize menu view */
        setCurrentView(MenuState.ROOT);

    }

    /**
     * Change the menu to the management view for a given town.
     * The town must be in the viewing player's kingdom when not in admin mode.
     * @param town The town to manage
     * @return The display view of the menu
     */
    public DisplayMenu goToManagementRoot(KonTown town) {
        if (town == null) return null;
        this.town = town;
        // Update menu access
        updateAccess();
        if (isAccess(AccessType.DEFAULT)) {
            playStatusSound(player.getBukkitPlayer(),false);
            return null;
        }
        // Change to management root view
        return refreshNewView(MenuState.MANAGEMENT_ROOT);
    }

    /**
     * Creates the root menu view for this menu.
     * This is a single page view.
     */
    private DisplayMenu createRootView() {
        DisplayMenu result;
        MenuIcon icon;
        CommandType iconCommand;
        boolean isPermission;
        List<String> loreList = new ArrayList<>();
        boolean isClickable = !isAdmin;

        /* Icon slot indexes */
        // Row 0: 0 1 2 3 4 5 6 7 8
        int ROOT_SLOT_JOIN 			= 0;
        int ROOT_SLOT_LEAVE 		= 2;
        int ROOT_SLOT_MANAGE 		= 4;
        int ROOT_SLOT_INVITES 		= 6;
        int ROOT_SLOT_LIST 			= 8;
        // Row 1: 9 10 11 12 13 14 15 16 17
        int ROOT_SLOT_SETTLE		= 12;
        int ROOT_SLOT_CLAIM 		= 13;
        int ROOT_SLOT_UNCLAIM		= 14;

        result = new DisplayMenu(2, getTitle(MenuState.ROOT));

        /* Join Icon (Unavailable for admins) */
        loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_JOIN.getMessage(),loreColor));
        if(isAdmin) {
            loreList.add(alertColor+MessagePath.LABEL_UNAVAILABLE.getMessage());
        } else {
            loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
        }
        icon = new InfoIcon(nameColor+MessagePath.MENU_TOWN_JOIN.getMessage(), loreList, Material.SADDLE, ROOT_SLOT_JOIN, isClickable);
        icon.setState(MenuState.JOIN);
        result.addIcon(icon);

        /* Leave Icon (Unavailable for admins) */
        loreList.clear();
        loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_LEAVE.getMessage(),loreColor));
        if(isAdmin) {
            loreList.add(alertColor+MessagePath.LABEL_UNAVAILABLE.getMessage());
        } else {
            loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
        }
        icon = new InfoIcon(nameColor+MessagePath.MENU_TOWN_LEAVE.getMessage(), loreList, Material.ARROW, ROOT_SLOT_LEAVE, isClickable);
        icon.setState(MenuState.LEAVE);
        result.addIcon(icon);

        /* List Icon */
        loreList.clear();
        loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_LIST.getMessage(),loreColor));
        loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
        icon = new InfoIcon(nameColor+MessagePath.MENU_TOWN_LIST.getMessage(), loreList, Material.PAPER, ROOT_SLOT_LIST, true);
        icon.setState(MenuState.LIST);
        result.addIcon(icon);

        /* Invites Icon (Unavailable for admins) */
        loreList.clear();
        loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_INVITES.getMessage(),loreColor));
        int numInvites = manager.getInviteTowns(player).size();
        Material inviteMat = Material.BOOK;
        if(numInvites > 0) {
            loreList.add(valueColor+numInvites);
            inviteMat = Material.WRITABLE_BOOK;
        }
        if(isAdmin) {
            loreList.add(alertColor+MessagePath.LABEL_UNAVAILABLE.getMessage());
        } else {
            loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
        }
        icon = new InfoIcon(nameColor+MessagePath.MENU_TOWN_INVITES.getMessage(), loreList, inviteMat, ROOT_SLOT_INVITES, isClickable);
        icon.setState(MenuState.INVITES);
        result.addIcon(icon);

        /* Manage Icon */
        loreList.clear();
        loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_MANAGE.getMessage(),loreColor));
        loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
        icon = new InfoIcon(nameColor+MessagePath.MENU_TOWN_MANAGE.getMessage(), loreList, Material.BOOKSHELF, ROOT_SLOT_MANAGE, true);
        icon.setState(MenuState.MANAGE);
        result.addIcon(icon);

        /* Settle Command Icon */
        iconCommand = CommandType.SETTLE;
        loreList.clear();
        double cost_settle = getKonquest().getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SETTLE.getPath(),0.0);
        double cost_settle_incr = getKonquest().getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SETTLE_INCREMENT.getPath(),0.0);
        isPermission = player.getBukkitPlayer().hasPermission(iconCommand.permission());
        if (isPermission) {
            loreList.add(hintColor+MessagePath.MENU_HELP_HINT_COMMAND.getMessage());
        }
        result.addIcon(new CommandIcon(iconCommand, isPermission, (int)cost_settle, (int)cost_settle_incr, loreList, ROOT_SLOT_SETTLE));

        /* Claim Command Icon */
        iconCommand = CommandType.CLAIM;
        loreList.clear();
        double cost_claim = getKonquest().getCore().getDouble(CorePath.FAVOR_COST_CLAIM.getPath(),0.0);
        isPermission = player.getBukkitPlayer().hasPermission(iconCommand.permission());
        if (isPermission) {
            loreList.add(hintColor+MessagePath.MENU_HELP_HINT_COMMAND.getMessage());
        }
        result.addIcon(new CommandIcon(iconCommand, isPermission, (int)cost_claim, 0, loreList, ROOT_SLOT_CLAIM));

        /* Unclaim Command Icon */
        iconCommand = CommandType.UNCLAIM;
        loreList.clear();
        isPermission = player.getBukkitPlayer().hasPermission(iconCommand.permission());
        if (isPermission) {
            loreList.add(hintColor+MessagePath.MENU_HELP_HINT_COMMAND.getMessage());
        }
        result.addIcon(new CommandIcon(iconCommand, isPermission, 0, 0, loreList, ROOT_SLOT_UNCLAIM));

        /* Navigation */
        addNavEmpty(result);
        addNavHome(result);
        addNavClose(result);

        return result;
    }

    /**
     * Creates a town view based on context.
     * This is a multiple paged view.
     * Contexts: JOIN, LEAVE, LIST, INVITES, MANAGE
     */
    private List<DisplayMenu> createTownView(MenuState context) {
        ArrayList<MenuIcon> icons = new ArrayList<>();
        List<KonTown> towns = new ArrayList<>();

        // Determine list of towns given context
        switch (context) {
            case JOIN:
                // List of all valid towns able to join (sends request)
                for(KonTown town : kingdom.getCapitalTowns()) {
                    if(!town.isPlayerResident(player.getOfflineBukkitPlayer())) {
                        towns.add(town);
                    }
                }
                break;
            case LEAVE:
                // List of towns that the player can leave
                for(KonTown town : kingdom.getCapitalTowns()) {
                    if(town.isPlayerResident(player.getOfflineBukkitPlayer())) {
                        towns.add(town);
                    }
                }
                break;
            case LIST:
                // List of all towns
                towns.addAll(kingdom.getCapitalTowns());
                break;
            case INVITES:
                // Towns that have invited the player to join as a resident
                towns.addAll(manager.getInviteTowns(player));
                break;
            case MANAGE:
                if (isAccess(AccessType.ADMIN)) {
                    // All towns in all kingdoms
                    for (KonKingdom kingdom : manager.getKingdoms()) {
                        towns.addAll(kingdom.getCapitalTowns());
                    }
                } else {
                    // Towns that the player can manage
                    towns.addAll(manager.getManageTowns(player));
                }
                break;
            default:
                return Collections.emptyList();
        }
        // Sort list
        towns.sort(townComparator);

        /* Town Icons */
        for (KonTown currentTown : towns) {
            boolean isCurrentTownClickable = true;
            ArrayList<String> alertList = new ArrayList<>();
            ArrayList<String> propertyList = new ArrayList<>();
            ArrayList<String> loreList = new ArrayList<>();
            switch(context) {
                case JOIN:
                    if(currentTown.isJoinable()) {
                        if(currentTown.isOpen() || currentTown.getNumResidents() == 0) {
                            loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_JOIN_NOW.getMessage());
                        } else {
                            loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_JOIN.getMessage());
                        }
                    } else {
                        isCurrentTownClickable = false;
                        alertList.add(alertColor+MessagePath.LABEL_UNAVAILABLE.getMessage());
                    }
                    break;
                case LEAVE:
                    if(currentTown.isLeaveable()) {
                        loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_LEAVE.getMessage());
                    } else {
                        isCurrentTownClickable = false;
                        alertList.add(alertColor+MessagePath.LABEL_UNAVAILABLE.getMessage());
                    }
                    break;
                case LIST:
                    loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_LIST.getMessage());
                    break;
                case INVITES:
                    loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_ACCEPT.getMessage());
                    loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_DECLINE.getMessage());
                    break;
                case MANAGE:
                    // Display number of pending requests
                    int numRequests = currentTown.getJoinRequests().size();
                    if(numRequests > 0) {
                        alertList.add(alertColor+MessagePath.MENU_TOWN_REQUESTS.getMessage()+": "+numRequests);
                    }
                    loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_MANAGE.getMessage());
                    break;
                default:
                    break;
            }
            icons.add(new TownIcon(currentTown,player.getBukkitPlayer(),contextColor,alertList,propertyList,loreList,0,isCurrentTownClickable));
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(context)));
    }

    /**
     * Creates the management root menu view for this menu.
     * This is a single page view.
     */
    private DisplayMenu createManagementRootView() {
        DisplayMenu result;
        MenuIcon icon;
        List<String> loreList = new ArrayList<>();
        int rows = 1;
        if(isAccess(AccessType.LORD)) {
            rows = 2;
        }
        if(isAccess(AccessType.DEFAULT)) {
            // Invalid access
            return null;
        }

        /* Icon slot indexes */
        // Row 0: 0 1 2 3 4 5 6 7 8
        int ROOT_SLOT_REQUESTS 		    = 0;
        int ROOT_SLOT_PLOTS 		    = 2;
        int ROOT_SLOT_INFO 			    = 4;
        int ROOT_SLOT_SHIELD 			= 6;
        int ROOT_SLOT_ARMOR 			= 8;
        // Row 1: 9 10 11 12 13 14 15 16 17
        int ROOT_SLOT_PROMOTE 		    = 10;
        int ROOT_SLOT_DEMOTE 	        = 11;
        int ROOT_SLOT_TRANSFER 		    = 12;
        int ROOT_SLOT_DESTROY 		    = 13;
        int ROOT_SLOT_UPGRADES 		    = 14;
        int ROOT_SLOT_OPTIONS 		    = 15;
        int ROOT_SLOT_SPECIALIZATION	= 16;

        result = new DisplayMenu(rows, getTitle(MenuState.MANAGEMENT_ROOT));

        // Render icons for Knights and Lords
        if(isAccess(AccessType.KNIGHT) || isAccess(AccessType.LORD) || isAccess(AccessType.ADMIN)) {
            /* Requests Icon */
            loreList.clear();
            loreList.add(propertyColor+MessagePath.LABEL_KNIGHT.getMessage());
            loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_REQUESTS.getMessage(),loreColor));
            int numRequests = town.getJoinRequests().size();
            Material requestMat = Material.GLASS_BOTTLE;
            if(numRequests > 0) {
                loreList.add(valueColor+numRequests);
                requestMat = Material.HONEY_BOTTLE;
            }
            loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
            icon = new InfoIcon(contextColor+MessagePath.MENU_TOWN_REQUESTS.getMessage(), loreList, requestMat, ROOT_SLOT_REQUESTS, true);
            icon.setState(MenuState.A_REQUESTS);
            result.addIcon(icon);

            /* Plots Icon */
            if (getKonquest().getPlotManager().isEnabled()) {
                loreList.clear();
                loreList.add(propertyColor + MessagePath.LABEL_KNIGHT.getMessage());
                loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_PLOTS.getMessage(), loreColor));
                boolean isPlotsClickable = true;
                boolean isTownPlotPropertyDisabled = town.hasPropertyValue(KonPropertyFlag.PLOTS) && !town.getPropertyValue(KonPropertyFlag.PLOTS);
                if (isTownPlotPropertyDisabled) {
                    isPlotsClickable = false;
                    loreList.add(alertColor + MessagePath.LABEL_DISABLED.getMessage());
                } else {
                    loreList.add(hintColor + MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
                }
                icon = new InfoIcon(contextColor+MessagePath.MENU_TOWN_PLOTS.getMessage(), loreList, Material.GRASS_BLOCK, ROOT_SLOT_PLOTS, isPlotsClickable);
                icon.setState(MenuState.A_PLOTS);
                result.addIcon(icon);
            }

            /* Info Icon */
            loreList.clear();
            loreList.add(hintColor + MessagePath.MENU_SCORE_HINT.getMessage());
            icon = new TownIcon(town, contextColor, loreList, ROOT_SLOT_INFO, true);
            icon.setState(MenuState.A_INFO);
            result.addIcon(icon);

            /* Shields Icon */
            if (getKonquest().getShieldManager().isShieldsEnabled()) {
                loreList.clear();
                loreList.add(propertyColor + MessagePath.LABEL_KNIGHT.getMessage());
                loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_SHIELDS.getMessage(), loreColor));
                loreList.add(hintColor + MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
                icon = new InfoIcon(contextColor+MessagePath.MENU_TOWN_SHIELDS.getMessage(), loreList, Material.SHIELD, ROOT_SLOT_SHIELD, true);
                icon.setState(MenuState.A_SHIELD);
                result.addIcon(icon);
            }

            /* Armor Icon */
            if (getKonquest().getShieldManager().isArmorsEnabled()) {
                loreList.clear();
                loreList.add(propertyColor + MessagePath.LABEL_KNIGHT.getMessage());
                loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_ARMOR.getMessage(), loreColor));
                loreList.add(hintColor + MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
                icon = new InfoIcon(contextColor+MessagePath.MENU_TOWN_ARMOR.getMessage(), loreList, Material.DIAMOND_CHESTPLATE, ROOT_SLOT_ARMOR, true);
                icon.setState(MenuState.A_ARMOR);
                result.addIcon(icon);
            }
        }
        // Render icons for lords only
        if(isAccess(AccessType.LORD) || isAccess(AccessType.ADMIN)) {
            /* Promote Icon */
            loreList = new ArrayList<>();
            loreList.add(propertyColor+MessagePath.LABEL_LORD.getMessage());
            loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_PROMOTE.getMessage(),loreColor));
            boolean isPromoteClickable = true;
            if(town.isPromoteable() || isAdmin) {
                loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
            } else {
                // This option is unavailable due to property flags
                isPromoteClickable = false;
                loreList.add(alertColor+MessagePath.LABEL_UNAVAILABLE.getMessage());
            }
            icon = new InfoIcon(contextColor+MessagePath.MENU_TOWN_PROMOTE.getMessage(), loreList, Material.DIAMOND_HORSE_ARMOR, ROOT_SLOT_PROMOTE, isPromoteClickable);
            icon.setState(MenuState.B_PROMOTE);
            result.addIcon(icon);

            /* Demote Icon */
            loreList.clear();
            loreList.add(propertyColor+MessagePath.LABEL_LORD.getMessage());
            loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_DEMOTE.getMessage(),loreColor));
            boolean isDemoteClickable = true;
            if(town.isDemoteable() || isAdmin) {
                loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
            } else {
                // This option is unavailable due to property flags
                isDemoteClickable = false;
                loreList.add(alertColor+MessagePath.LABEL_UNAVAILABLE.getMessage());
            }
            icon = new InfoIcon(contextColor+MessagePath.MENU_TOWN_DEMOTE.getMessage(), loreList, Material.LEATHER_HORSE_ARMOR, ROOT_SLOT_DEMOTE, isDemoteClickable);
            icon.setState(MenuState.B_DEMOTE);
            result.addIcon(icon);

            /* Transfer Icon */
            loreList.clear();
            loreList.add(propertyColor+MessagePath.LABEL_LORD.getMessage());
            loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_TRANSFER.getMessage(),loreColor));
            boolean isTransferClickable = true;
            if(town.isTransferable() || isAdmin) {
                loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
            } else {
                // This option is unavailable due to property flags
                isTransferClickable = false;
                loreList.add(alertColor+MessagePath.LABEL_UNAVAILABLE.getMessage());
            }
            icon = new InfoIcon(contextColor+MessagePath.MENU_TOWN_TRANSFER.getMessage(), loreList, Material.ELYTRA, ROOT_SLOT_TRANSFER, isTransferClickable);
            icon.setState(MenuState.B_TRANSFER);
            result.addIcon(icon);

            /* Destroy Icon */
            if (town.getTerritoryType().equals(KonquestTerritoryType.TOWN) && getKonquest().getKingdomManager().getIsTownDestroyLordEnable()) {
                loreList.clear();
                loreList.add(propertyColor + MessagePath.LABEL_LORD.getMessage());
                loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_DESTROY.getMessage(), loreColor));
                loreList.add(hintColor + MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
                icon = new InfoIcon(contextColor+MessagePath.MENU_TOWN_DESTROY.getMessage(), loreList, Material.TNT, ROOT_SLOT_DESTROY, true);
                icon.setState(MenuState.B_DESTROY);
                result.addIcon(icon);
            }

            /* Upgrades Icon */
            if (getKonquest().getUpgradeManager().isEnabled()) {
                loreList.clear();
                loreList.add(propertyColor + MessagePath.LABEL_LORD.getMessage());
                loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_UPGRADES.getMessage(), loreColor));
                boolean isUpgradesClickable = true;
                boolean isTownUpgradePropertyDisabled = town.hasPropertyValue(KonPropertyFlag.UPGRADE) && !town.getPropertyValue(KonPropertyFlag.UPGRADE);
                if (isTownUpgradePropertyDisabled) {
                    isUpgradesClickable = false;
                    loreList.add(alertColor + MessagePath.LABEL_DISABLED.getMessage());
                } else {
                    loreList.add(hintColor + MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
                }
                icon = new InfoIcon(contextColor+MessagePath.MENU_TOWN_UPGRADES.getMessage(), loreList, Material.GOLDEN_APPLE, ROOT_SLOT_UPGRADES, isUpgradesClickable);
                icon.setState(MenuState.B_UPGRADES);
                result.addIcon(icon);
            }

            /* Options Icon */
            loreList.clear();
            loreList.add(propertyColor+MessagePath.LABEL_LORD.getMessage());
            loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_OPTIONS.getMessage(),loreColor));
            loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
            icon = new InfoIcon(contextColor+MessagePath.MENU_TOWN_OPTIONS.getMessage(), loreList, Material.OAK_SIGN, ROOT_SLOT_OPTIONS, true);
            icon.setState(MenuState.B_OPTIONS);
            result.addIcon(icon);

            /* Specialization Icon */
            if (getKonquest().getKingdomManager().getIsDiscountEnable()) {
                loreList.clear();
                loreList.add(propertyColor + MessagePath.LABEL_LORD.getMessage());
                loreList.add(loreColor + MessagePath.MENU_OPTIONS_CURRENT.getMessage(valueColor + town.getSpecializationName()));
                loreList.addAll(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_DESCRIPTION_SPECIAL.getMessage(), loreColor));
                loreList.add(hintColor + MessagePath.MENU_TOWN_HINT_OPEN.getMessage());
                icon = new InfoIcon(contextColor+MessagePath.MENU_TOWN_SPECIAL.getMessage(), loreList, Material.EMERALD, ROOT_SLOT_SPECIALIZATION, true);
                icon.setState(MenuState.B_SPECIALIZATION);
                result.addIcon(icon);
            }
        }

        /* Navigation */
        addNavEmpty(result);
        addNavHome(result);
        addNavClose(result);
        addNavReturn(result);

        return result;
    }

    /**
     * Creates a player view based on context.
     * This is a multiple paged view.
     * Contexts: A_REQUESTS, B_PROMOTE, B_DEMOTE, B_TRANSFER
     */
    private List<DisplayMenu> createPlayerView(MenuState context) {
        if (town == null) return Collections.emptyList();
        String loreHintStr1 = "";
        String loreHintStr2 = "";
        List<OfflinePlayer> players = new ArrayList<>();
        ArrayList<MenuIcon> icons = new ArrayList<>();

        // Determine list of players given context
        switch (context) {
            case A_REQUESTS:
                players.addAll(town.getJoinRequests());
                loreHintStr1 = MessagePath.MENU_TOWN_HINT_ACCEPT.getMessage();
                loreHintStr2 = MessagePath.MENU_TOWN_HINT_DECLINE.getMessage();
                break;
            case B_PROMOTE:
                players.addAll(town.getPlayerResidentsOnly());
                loreHintStr1 = MessagePath.MENU_TOWN_HINT_PROMOTE.getMessage();
                break;
            case B_DEMOTE:
                players.addAll(town.getPlayerKnightsOnly());
                loreHintStr1 = MessagePath.MENU_TOWN_HINT_DEMOTE.getMessage();
                break;
            case B_TRANSFER:
                players.addAll(town.getPlayerKnightsOnly());
                players.addAll(town.getPlayerResidentsOnly());
                loreHintStr1 = MessagePath.MENU_TOWN_HINT_TRANSFER.getMessage();
                break;
            default:
                return Collections.emptyList();
        }

        /* Player Icons */
        for (OfflinePlayer currentPlayer : players) {
            ArrayList<String> loreList = new ArrayList<>();
            String townRole = town.getPlayerRoleName(currentPlayer);
            if(!townRole.isEmpty()) {
                loreList.add(propertyColor+townRole);
            }
            if(!loreHintStr1.isEmpty()) {
                loreList.add(hintColor+loreHintStr1);
            }
            if(!loreHintStr2.isEmpty()) {
                loreList.add(hintColor+loreHintStr2);
            }
            icons.add(new PlayerIcon(contextColor+currentPlayer.getName(),loreList,currentPlayer,0,true,PlayerIcon.PlayerIconAction.DISPLAY_INFO));
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(context)));
    }

    /**
     * Creates the shield menu view.
     * This is a multiple paged view.
     */
    private List<DisplayMenu> createShieldView() {
        if (town == null || !getKonquest().getShieldManager().isShieldsEnabled()) return Collections.emptyList();
        ArrayList<MenuIcon> icons = new ArrayList<>();

        /* Shield Icons */
        for (KonShield currentShield : getKonquest().getShieldManager().getShields()) {
            icons.add(new ShieldIcon(currentShield, town.getNumResidents(), town.getNumLand(), 0));
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(MenuState.A_SHIELD)));
    }

    /**
     * Creates the armor menu view.
     * This is a multiple paged view.
     */
    private List<DisplayMenu> createArmorView() {
        if (town == null || !getKonquest().getShieldManager().isArmorsEnabled()) return Collections.emptyList();
        ArrayList<MenuIcon> icons = new ArrayList<>();

        /* Armor Icons */
        for (KonArmor currentArmor : getKonquest().getShieldManager().getArmors()) {
            icons.add(new ArmorIcon(currentArmor, town.getNumResidents(), town.getNumLand(), 0));
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(MenuState.A_ARMOR)));
    }

    /**
     * Creates the upgrade menu view.
     * This is a multiple paged view.
     */
    private List<DisplayMenu> createUpgradeView() {
        if (town == null || !getKonquest().getUpgradeManager().isEnabled()) return Collections.emptyList();
        ArrayList<MenuIcon> icons = new ArrayList<>();

        // Place upgrades in ordinal order into list
        HashMap<KonquestUpgrade, Integer> availableUpgrades = getKonquest().getUpgradeManager().getAvailableUpgrades(town);
        ArrayList<KonUpgrade> allUpgrades = new ArrayList<>();
        for(KonUpgrade upgrade : KonUpgrade.values()) {
            if(availableUpgrades.containsKey(upgrade)) {
                allUpgrades.add(upgrade);
            }
        }

        /* Upgrade Icons */
        for (KonUpgrade currentUpgrade : allUpgrades) {
            int currentLevel = availableUpgrades.get(currentUpgrade);
            int cost = getKonquest().getUpgradeManager().getUpgradeCost(currentUpgrade, currentLevel);
            int pop = getKonquest().getUpgradeManager().getUpgradePopulation(currentUpgrade, currentLevel);
            icons.add(new UpgradeIcon(currentUpgrade, currentLevel, 0, cost, pop));
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(MenuState.B_UPGRADES)));
    }

    /**
     * Creates the options menu view.
     * This is a multiple paged view.
     */
    private List<DisplayMenu> createOptionsView() {
        if (town == null) return Collections.emptyList();
        ArrayList<MenuIcon> icons = new ArrayList<>();

        // Get enable options
        ArrayList<KonTownOption> allOptions = new ArrayList<>();
        for (KonTownOption townOption : KonTownOption.values()) {
            boolean isOptionEnabled = true;
            // Special option checks
            if (townOption.equals(KonTownOption.ALLIED_BUILDING)) {
                isOptionEnabled = getKonquest().getCore().getBoolean(CorePath.KINGDOMS_ALLY_BUILD.getPath(),false);
            }
            if (isOptionEnabled) {
                allOptions.add(townOption);
            }
        }

        /* Option Icons */
        for (KonTownOption currentOption : allOptions) {
            boolean val = town.getTownOption(currentOption);
            String currentValue = DisplayManager.boolean2Lang(val) + " " + DisplayManager.boolean2Symbol(val);
            ArrayList<String> loreList = new ArrayList<>(HelperUtil.stringPaginate(currentOption.getDescription(), loreColor));
            loreList.add(loreColor + MessagePath.MENU_OPTIONS_CURRENT.getMessage(valueColor + currentValue));
            loreList.add(hintColor + MessagePath.MENU_OPTIONS_HINT.getMessage());
            icons.add(new OptionIcon(currentOption, nameColor + currentOption.getName(), loreList, currentOption.getDisplayMaterial(), 0));
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(MenuState.B_OPTIONS)));
    }

    /**
     * Creates the specialization menu view.
     * This is a multiple paged view.
     */
    private List<DisplayMenu> createSpecializationView() {
        if (town == null || !getKonquest().getKingdomManager().getIsDiscountEnable()) return Collections.emptyList();
        ArrayList<MenuIcon> icons = new ArrayList<>();

        // Common lore for every icon
        List<String> loreList = new ArrayList<>(HelperUtil.stringPaginate(MessagePath.MENU_TOWN_LORE_SPECIAL.getMessage(), loreColor));
        if(!isAdmin) {
            double costSpecial = getKonquest().getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SPECIALIZE.getPath());
            String cost = String.format("%.2f",costSpecial);
            loreList.add(loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+cost);
        }
        loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_SPECIAL.getMessage());

        /* Profession Icons */
        for(Villager.Profession profession : CompatibilityUtil.getProfessions()) {
            if(!CompatibilityUtil.isProfessionEqual(profession,town.getSpecialization())) {
                icons.add(new ProfessionIcon(loreList,profession,0,true));
            }
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(MenuState.B_SPECIALIZATION)));
    }

    /**
     * Creates the destroy menu view.
     * This is a single page view.
     */
    private DisplayMenu createDestroyView() {
        DisplayMenu result;
        MenuIcon icon;
        List<String> loreList = new ArrayList<>();
        result = new DisplayMenu(1, getTitle(MenuState.B_DESTROY));

        /* Icon slot indexes */
        // Row 0: 0 1 2 3 4 5 6 7 8
        int SLOT_YES 	= 3;
        int SLOT_NO 	= 5;

        /* Yes Button */
        loreList.add(hintColor+MessagePath.MENU_TOWN_HINT_DESTROY.getMessage());
        icon = new ConfirmationIcon(true, loreList, Material.GLOWSTONE_DUST, SLOT_YES);
        icon.setState(MenuState.CONFIRM_YES);
        result.addIcon(icon);

        /* No Button */
        loreList.clear();
        loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_EXIT.getMessage());
        icon = new ConfirmationIcon(false, loreList, Material.REDSTONE, SLOT_NO);
        icon.setState(MenuState.CONFIRM_NO);
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
        MenuState currentState = (MenuState)context;
        switch (currentState) {
            case ROOT:
                result.add(createRootView());
                break;
            case JOIN:
            case LEAVE:
            case LIST:
            case INVITES:
            case MANAGE:
                result.addAll(createTownView(currentState));
                break;
            case MANAGEMENT_ROOT:
                result.add(createManagementRootView());
                break;
            case A_REQUESTS:
            case B_PROMOTE:
            case B_DEMOTE:
            case B_TRANSFER:
                result.addAll(createPlayerView(currentState));
                break;
            case A_SHIELD:
                result.addAll(createShieldView());
                break;
            case A_ARMOR:
                result.addAll(createArmorView());
                break;
            case B_UPGRADES:
                result.addAll(createUpgradeView());
                break;
            case B_OPTIONS:
                result.addAll(createOptionsView());
                break;
            case B_SPECIALIZATION:
                result.addAll(createSpecializationView());
                break;
            case B_DESTROY:
                result.add(createDestroyView());
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
     * @param slot The inventory slot of the current view that was clicked
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
            if (isNavClose(slot)) {
                // Close the menu by returning a null view
                return null;
            } else if (isNavHome(slot)) {
                // Go to main menu
                getKonquest().getDisplayManager().displayMainMenu(player);
            } else if (isNavReturn(slot)) {
                // Return to previous
                switch (currentState) {
                    case A_REQUESTS:
                    case A_SHIELD:
                    case A_ARMOR:
                    case B_PROMOTE:
                    case B_DEMOTE:
                    case B_TRANSFER:
                    case B_DESTROY:
                    case B_UPGRADES:
                    case B_OPTIONS:
                    case B_SPECIALIZATION:
                        // Return to management root
                        result = setCurrentView(MenuState.MANAGEMENT_ROOT);
                        break;
                    case MANAGEMENT_ROOT:
                        // Return to manage list
                        this.town = null;
                        result = setCurrentView(MenuState.MANAGE);
                        break;
                    default:
                        // Return to base root
                        result = setCurrentView(MenuState.ROOT);
                        break;
                }
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
            MenuState nextState = (MenuState)clickedIcon.getState(); // could be null in some states
            switch(currentState) {
                /*
                 * Base Menu States
                 */
                case ROOT:
                    // Root view
                    // Check for command icons
                    if (clickedIcon instanceof CommandIcon) {
                        CommandIcon icon = (CommandIcon) clickedIcon;
                        for (String usageLine : icon.getCommand().argumentUsage()) {
                            ChatUtil.sendNotice(player.getBukkitPlayer(), usageLine);
                        }
                        result = view;
                    } else {
                        // Use stored icon state
                        if (nextState == null) return null;
                        switch (nextState) {
                            case JOIN:
                            case LEAVE:
                            case LIST:
                            case INVITES:
                            case MANAGE:
                                // Go to next state as defined by icon
                                result = setCurrentView(nextState);
                                break;
                        }
                    }
                    break;
                case JOIN:
                    // Clicking tries to join a town
                    if(clickedIcon instanceof TownIcon) {
                        TownIcon icon = (TownIcon)clickedIcon;
                        KonTown clickTown = icon.getTown();
                        boolean status = manager.menuJoinTownRequest(player, clickTown);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = refreshCurrentView();
                    }
                    break;
                case LEAVE:
                    // Clicking tries to leave a town
                    if(clickedIcon instanceof TownIcon) {
                        TownIcon icon = (TownIcon)clickedIcon;
                        KonTown clickTown = icon.getTown();
                        boolean status = manager.menuLeaveTown(player, clickTown);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = refreshCurrentView();
                    }
                    break;
                case LIST:
                    // Clicking opens a town info menu
                    if(clickedIcon instanceof TownIcon) {
                        TownIcon icon = (TownIcon)clickedIcon;
                        KonTown clickTown = icon.getTown();
                        getKonquest().getDisplayManager().displayTownInfoMenu(player, clickTown);
                    }
                    break;
                case INVITES:
                    // Clicking accepts or denies a town residence invite
                    if(clickedIcon instanceof TownIcon) {
                        TownIcon icon = (TownIcon)clickedIcon;
                        KonTown clickTown = icon.getTown();
                        boolean status = manager.menuRespondTownInvite(player, clickTown, clickType);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = refreshCurrentView();
                    }
                    break;
                case MANAGE:
                    // Clicking opens a town-specific management menu
                    if(clickedIcon instanceof TownIcon) {
                        TownIcon icon = (TownIcon)clickedIcon;
                        town = icon.getTown();
                        if (town == null) return null;
                        // Update menu access
                        updateAccess();
                        if (isAccess(AccessType.DEFAULT)) {
                            playStatusSound(player.getBukkitPlayer(),false);
                            return null;
                        }
                        result = refreshNewView(MenuState.MANAGEMENT_ROOT);
                    }
                    break;
                /*
                 * Management Menu States
                 */
                case MANAGEMENT_ROOT:
                    // Management Root view, use stored icon state
                    if (nextState == null) return null;
                    switch (nextState) {
                        case A_REQUESTS:
                        case A_SHIELD:
                        case A_ARMOR:
                        case B_PROMOTE:
                        case B_DEMOTE:
                        case B_TRANSFER:
                        case B_DESTROY:
                        case B_UPGRADES:
                        case B_OPTIONS:
                        case B_SPECIALIZATION:
                            // Go to next state as defined by icon
                            result = setCurrentView(nextState);
                            break;
                        case A_PLOTS:
                            // Open the plots menu
                            getKonquest().getDisplayManager().displayTownPlotMenu(player,town);
                            break;
                        case A_INFO:
                            // Open the town info menu
                            getKonquest().getDisplayManager().displayTownInfoMenu(player,town);
                            break;
                    }
                    break;
                case A_REQUESTS:
                    if(clickedIcon instanceof PlayerIcon) {
                        PlayerIcon icon = (PlayerIcon)clickedIcon;
                        OfflinePlayer clickPlayer = icon.getOfflinePlayer();
                        boolean status = manager.menuRespondTownRequest(player, clickPlayer, town, clickType);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = refreshCurrentView();
                    }
                    break;
                case A_SHIELD:
                    if(clickedIcon instanceof ShieldIcon) {
                        ShieldIcon icon = (ShieldIcon)clickedIcon;
                        boolean status = getKonquest().getShieldManager().activateTownShield(icon.getShield(), town, player.getBukkitPlayer(), isAdmin);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = refreshCurrentView();
                    }
                    break;
                case A_ARMOR:
                    if(clickedIcon instanceof ArmorIcon) {
                        ArmorIcon icon = (ArmorIcon)clickedIcon;
                        boolean status = getKonquest().getShieldManager().activateTownArmor(icon.getArmor(), town, player.getBukkitPlayer(), isAdmin);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = refreshCurrentView();
                    }
                    break;
                case B_PROMOTE:
                    if(clickedIcon instanceof PlayerIcon) {
                        PlayerIcon icon = (PlayerIcon)clickedIcon;
                        OfflinePlayer clickPlayer = icon.getOfflinePlayer();
                        boolean status = manager.menuPromoteDemoteTownKnight(player,clickPlayer,town,true);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = refreshCurrentView();
                    }
                    break;
                case B_DEMOTE:
                    if(clickedIcon instanceof PlayerIcon) {
                        PlayerIcon icon = (PlayerIcon)clickedIcon;
                        OfflinePlayer clickPlayer = icon.getOfflinePlayer();
                        boolean status = manager.menuPromoteDemoteTownKnight(player,clickPlayer,town,false);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = refreshCurrentView();
                    }
                    break;
                case B_TRANSFER:
                    if(clickedIcon instanceof PlayerIcon) {
                        PlayerIcon icon = (PlayerIcon)clickedIcon;
                        OfflinePlayer clickPlayer = icon.getOfflinePlayer();
                        boolean status = manager.menuTransferTownLord(player,clickPlayer,town,isAdmin);
                        playStatusSound(player.getBukkitPlayer(),status);
                        // No result, close menu
                    }
                    break;
                case B_DESTROY:
                    if (nextState == null) return null;
                    if (nextState.equals(MenuState.CONFIRM_YES)) {
                        // Destroy the town
                        boolean status = manager.menuDestroyTown(town,player);
                        playStatusSound(player.getBukkitPlayer(),status);
                    }
                    break;
                case B_UPGRADES:
                    if(clickedIcon instanceof UpgradeIcon) {
                        UpgradeIcon icon = (UpgradeIcon)clickedIcon;
                        boolean status = getKonquest().getUpgradeManager().addTownUpgrade(town, icon.getUpgrade(), icon.getLevel(), player.getBukkitPlayer());
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = refreshCurrentView();
                    }
                    break;
                case B_OPTIONS:
                    if(clickedIcon instanceof OptionIcon) {
                        OptionIcon icon = (OptionIcon)clickedIcon;
                        boolean status = manager.changeTownOption(icon.getOption(), town, player.getBukkitPlayer());
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = refreshCurrentView();
                    }
                    break;
                case B_SPECIALIZATION:
                    if(clickedIcon instanceof ProfessionIcon) {
                        ProfessionIcon icon = (ProfessionIcon)clickedIcon;
                        Villager.Profession clickProfession = icon.getProfession();
                        boolean status = manager.menuChangeTownSpecialization(town, clickProfession, player, isAdmin);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = refreshCurrentView();
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
        switch(context) {
            case ROOT:
                result = titleColor+MessagePath.MENU_TOWN_TITLE_ROOT.getMessage();
                break;
            case JOIN:
                result = titleColor+MessagePath.MENU_TOWN_TITLE_JOIN.getMessage();
                break;
            case LEAVE:
                result = titleColor+MessagePath.MENU_TOWN_TITLE_LEAVE.getMessage();
                break;
            case LIST:
                result = titleColor+MessagePath.MENU_TOWN_TITLE_LIST.getMessage();
                break;
            case INVITES:
                result = titleColor+MessagePath.MENU_TOWN_TITLE_INVITES.getMessage();
                break;
            case MANAGE:
                result = titleColor+MessagePath.MENU_TOWN_TITLE_MANAGE_TOWN.getMessage();
                break;
            case MANAGEMENT_ROOT:
                result = titleColor+MessagePath.MENU_TOWN_TITLE_MANAGE.getMessage();
                break;
            case A_REQUESTS:
                result = titleColor+MessagePath.MENU_TOWN_TITLE_REQUESTS.getMessage();
                break;
            case A_SHIELD:
                result = titleColor+MessagePath.MENU_TOWN_SHIELDS.getMessage();
                break;
            case A_ARMOR:
                result = titleColor+MessagePath.MENU_TOWN_ARMOR.getMessage();
                break;
            case B_PROMOTE:
                result = titleColor+MessagePath.MENU_TOWN_PROMOTE.getMessage();
                break;
            case B_DEMOTE:
                result = titleColor+MessagePath.MENU_TOWN_DEMOTE.getMessage();
                break;
            case B_TRANSFER:
                result = titleColor+MessagePath.MENU_TOWN_TRANSFER.getMessage();
                break;
            case B_DESTROY:
                result = titleColor+MessagePath.MENU_TOWN_DESTROY.getMessage();
                break;
            case B_UPGRADES:
                result = titleColor+MessagePath.MENU_TOWN_UPGRADES.getMessage();
                break;
            case B_OPTIONS:
                result = titleColor+MessagePath.MENU_TOWN_OPTIONS.getMessage();
                break;
            case B_SPECIALIZATION:
                result = titleColor+MessagePath.MENU_TOWN_SPECIAL.getMessage();
                break;
            default:
                break;
        }
        return result;
    }

    private void updateAccess() {
        // Update menu access
        UUID id = player.getBukkitPlayer().getUniqueId();
        if(isAdmin) {
            setAccess(AccessType.ADMIN);
        } else if (town != null) {
            if(town.isLord(id)) {
                setAccess(AccessType.LORD);
            } else if(town.isPlayerKnight(player.getBukkitPlayer())) {
                setAccess(AccessType.KNIGHT);
            } else {
                setAccess(AccessType.DEFAULT);
            }
        } else {
            setAccess(AccessType.DEFAULT);
        }
    }

}
