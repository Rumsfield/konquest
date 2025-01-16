package com.github.rumsfield.konquest.display.menu;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.display.DisplayView;
import com.github.rumsfield.konquest.display.StateMenu;
import com.github.rumsfield.konquest.display.icon.*;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.manager.TravelManager;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class TravelMenu extends StateMenu {

    enum MenuState implements State {
        ROOT,
        TOWN_LIST,
        KINGDOM_LIST,
        SANCTUARY_LIST,
        CAMP_LIST,
        RUIN_LIST,
        TEMPLATE_LIST,
        CAPITAL,
        HOME,
        CAMP,
        WILD
    }

    enum AccessType implements Access {
        DEFAULT,
        ADMIN
    }

    private final KonPlayer player;
    private final boolean isAdmin;

    public TravelMenu(Konquest konquest, KonPlayer player, boolean isAdmin) {
        super(konquest, MenuState.ROOT, AccessType.DEFAULT);
        this.player = player;
        this.isAdmin = isAdmin;
        if (isAdmin) {
            setAccess(AccessType.ADMIN);
        } else {
            setAccess(AccessType.DEFAULT);
        }

        /* Initialize menu view */
        setCurrentView(MenuState.ROOT);

    }

    /**
     * Creates the root menu view for this menu.
     * This is a single page view.
     */
    private DisplayView createRootView() {

        // Menu conditions
        // Check for valid world
        if(!isAdmin && !getKonquest().isWorldValid(player.getBukkitPlayer().getWorld())) {
            ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_INVALID_WORLD.getMessage());
            return null;
        }
        // Check for allowed territory travel
        if (!isAdmin && !isTravelInTerritoryAllowed()) {
            ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_TRAVEL_ERROR_ENEMY_TERRITORY.getMessage());
            return null;
        }

        DisplayView result;
        MenuIcon icon;
        boolean isClickable;

        boolean isSanctuaryTravel = getKonquest().getCore().getBoolean(CorePath.TRAVEL_ENABLE_SANCTUARY.getPath(),false);
        boolean isCapitalTravel = getKonquest().getCore().getBoolean(CorePath.TRAVEL_ENABLE_CAPITAL.getPath(),false);
        boolean isCampTravel = getKonquest().getCore().getBoolean(CorePath.TRAVEL_ENABLE_CAMP.getPath(),false);
        boolean isHomeTravel = getKonquest().getCore().getBoolean(CorePath.TRAVEL_ENABLE_HOME.getPath(),false);
        boolean isWildTravel = getKonquest().getCore().getBoolean(CorePath.TRAVEL_ENABLE_WILD.getPath(),false);
        boolean isTownTravel = getKonquest().getCore().getBoolean(CorePath.TRAVEL_ENABLE_TOWNS.getPath(),false);

        /* Icon slot indexes */
        int rows = 1;
        if (isAccess(AccessType.ADMIN)) {
            rows = 2;
        }
        // Row 0: 0  1  2  3  4  5  6  7  8
        int SLOT_KINGDOMS = 0;
        int SLOT_TOWNS = 1;
        int SLOT_SANCTUARIES = 2;
        int SLOT_CAPITAL = 5;
        int SLOT_HOME = 6;
        int SLOT_CAMP = 7;
        int SLOT_WILD = 8;
        // Row 1: 9 10 11 12 13 14 15 16 17
        int SLOT_CAMPS = 9;
        int SLOT_TEMPLATES = 10;
        int SLOT_RUINS = 11;

        result = new DisplayView(rows, getTitle(MenuState.ROOT));

        // Default Icons

        /* Towns Icon */
        isClickable = isAdmin || (isTownTravel && !player.isBarbarian());
        icon = new InfoIcon(MessagePath.LABEL_TOWNS.getMessage(), Material.OBSIDIAN, SLOT_TOWNS, isClickable);
        icon.addProperty(MessagePath.LABEL_TRAVEL.getMessage());
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
        } else if (!isTownTravel) {
            icon.addAlert(MessagePath.LABEL_DISABLED.getMessage());
            icon.addError(MessagePath.GENERIC_ERROR_DISABLED.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
            icon.addError(MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
        }
        icon.setState(MenuState.TOWN_LIST);
        result.addIcon(icon);

        /* Kingdoms Icon */
        isClickable = isAdmin || (isCapitalTravel && !player.isBarbarian());
        icon = new InfoIcon(MessagePath.LABEL_KINGDOMS.getMessage(), Material.DIAMOND_HELMET, SLOT_KINGDOMS, isClickable);
        icon.addProperty(MessagePath.LABEL_TRAVEL.getMessage());
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
        } else if (!isCapitalTravel) {
            icon.addAlert(MessagePath.LABEL_DISABLED.getMessage());
            icon.addError(MessagePath.GENERIC_ERROR_DISABLED.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
            icon.addError(MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
        }
        icon.setState(MenuState.KINGDOM_LIST);
        result.addIcon(icon);

        /* Sanctuaries Icon */
        isClickable = isAdmin || isSanctuaryTravel;
        icon = new InfoIcon(MessagePath.LABEL_SANCTUARIES.getMessage(), Material.SMOOTH_QUARTZ, SLOT_SANCTUARIES, isClickable);
        icon.addProperty(MessagePath.LABEL_TRAVEL.getMessage());
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_DISABLED.getMessage());
            icon.addError(MessagePath.GENERIC_ERROR_DISABLED.getMessage());
        }
        icon.setState(MenuState.SANCTUARY_LIST);
        result.addIcon(icon);

        /* Capital Icon */
        boolean isFlagAllowed = player.getKingdom().getCapital().getPropertyValue(KonPropertyFlag.TRAVEL);
        isClickable = isAdmin || (isCapitalTravel && isFlagAllowed && !player.isBarbarian());
        icon = new InfoIcon(MessagePath.LABEL_CAPITAL.getMessage(), Material.NETHERITE_BLOCK, SLOT_CAPITAL, isClickable);
        icon.addProperty(MessagePath.LABEL_TRAVEL.getMessage());
        icon.addDescription(MessagePath.MENU_TRAVEL_DESCRIPTION_CAPITAL.getMessage());
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_TRAVEL.getMessage());
        } else if (!isCapitalTravel) {
            icon.addAlert(MessagePath.LABEL_DISABLED.getMessage());
            icon.addError(MessagePath.GENERIC_ERROR_DISABLED.getMessage());
        } else if (!isFlagAllowed) {
            icon.addAlert(MessagePath.PROTECTION_ERROR_BLOCKED.getMessage());
            icon.addError(MessagePath.COMMAND_TRAVEL_ERROR_DISABLED.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
            icon.addError(MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
        }
        icon.setState(MenuState.CAPITAL);
        result.addIcon(icon);

        /* Home Icon */
        boolean isHomeAvailable = player.getBukkitPlayer().getBedSpawnLocation() != null;
        isClickable = isAdmin || (isHomeTravel && isHomeAvailable && !player.isBarbarian());
        icon = new InfoIcon(MessagePath.LABEL_HOME.getMessage(), Material.WHITE_BED, SLOT_HOME, isClickable);
        icon.addProperty(MessagePath.LABEL_TRAVEL.getMessage());
        icon.addDescription(MessagePath.MENU_TRAVEL_DESCRIPTION_HOME.getMessage());
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_TRAVEL.getMessage());
        } else if (!isHomeTravel) {
            icon.addAlert(MessagePath.LABEL_DISABLED.getMessage());
            icon.addError(MessagePath.GENERIC_ERROR_DISABLED.getMessage());
        } else if (!isHomeAvailable) {
            icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
            icon.addError(MessagePath.COMMAND_TRAVEL_ERROR_NO_HOME.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
            icon.addError(MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
        }
        icon.setState(MenuState.HOME);
        result.addIcon(icon);

        /* Camp Icon */
        boolean isCampSet = getKonquest().getCampManager().isCampSet(player);
        isClickable = player.isBarbarian() && isCampSet && (isCampTravel || isAdmin);
        icon = new InfoIcon(MessagePath.LABEL_CAMP.getMessage(), Material.ORANGE_BED, SLOT_CAMP, isClickable);
        icon.addProperty(MessagePath.LABEL_TRAVEL.getMessage());
        icon.addDescription(MessagePath.MENU_TRAVEL_DESCRIPTION_CAMP.getMessage());
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_TRAVEL.getMessage());
        } else if (!isHomeTravel) {
            icon.addAlert(MessagePath.LABEL_DISABLED.getMessage());
            icon.addError(MessagePath.GENERIC_ERROR_DISABLED.getMessage());
        } else if (!isCampSet) {
            icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
            icon.addError(MessagePath.COMMAND_TRAVEL_ERROR_NO_CAMP.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
            icon.addError(MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
        }
        icon.setState(MenuState.CAMP);
        result.addIcon(icon);

        /* Wild Icon */
        isClickable = isAdmin || isWildTravel;
        icon = new InfoIcon(MessagePath.GENERIC_NOTICE_WILD.getMessage(), Material.TALL_GRASS, SLOT_WILD, isClickable);
        icon.addProperty(MessagePath.LABEL_TRAVEL.getMessage());
        icon.addDescription(MessagePath.MENU_TRAVEL_DESCRIPTION_WILD.getMessage());
        if (isClickable) {
            icon.addHint(MessagePath.MENU_HINT_TRAVEL.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_DISABLED.getMessage());
            icon.addError(MessagePath.GENERIC_ERROR_DISABLED.getMessage());
        }
        icon.setState(MenuState.WILD);
        result.addIcon(icon);

        // Admin Icons
        if (isAccess(AccessType.ADMIN)) {

            /* Camps Icon */
            icon = new InfoIcon(MessagePath.LABEL_CAMPS.getMessage(), Material.ORANGE_BED, SLOT_CAMPS, true);
            icon.addProperty(MessagePath.LABEL_ADMIN.getMessage());
            icon.addProperty(MessagePath.LABEL_TRAVEL.getMessage());
            icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
            icon.setState(MenuState.CAMP_LIST);
            result.addIcon(icon);

            /* Templates Icon */
            icon = new InfoIcon(MessagePath.LABEL_MONUMENT_TEMPLATES.getMessage(), Material.CRAFTING_TABLE, SLOT_TEMPLATES, true);
            icon.addProperty(MessagePath.LABEL_ADMIN.getMessage());
            icon.addProperty(MessagePath.LABEL_TRAVEL.getMessage());
            icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
            icon.setState(MenuState.TEMPLATE_LIST);
            result.addIcon(icon);

            /* Ruins Icon */
            icon = new InfoIcon(MessagePath.LABEL_RUINS.getMessage(), Material.MOSSY_COBBLESTONE, SLOT_RUINS, true);
            icon.addProperty(MessagePath.LABEL_ADMIN.getMessage());
            icon.addProperty(MessagePath.LABEL_TRAVEL.getMessage());
            icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
            icon.setState(MenuState.RUIN_LIST);
            result.addIcon(icon);

        }

        /* Navigation */
        addNavEmpty(result);
        addNavClose(result);
        if (!isAdmin) {
            addNavHome(result);
        }

        return result;
    }

    /**
     * Creates the town list view.
     * This can be a multiple paged view.
     */
    private List<DisplayView> createTownView() {
        // List towns the player is able to travel to
        ArrayList<MenuIcon> icons = new ArrayList<>();
        List<KonTown> towns = new ArrayList<>();

        if (isAccess(AccessType.ADMIN)) {
            // All towns
            for (KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
                towns.addAll(kingdom.getTowns());
            }
        } else {
            // Friendly and allied towns
            for (KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
                if (getKonquest().getKingdomManager().isPlayerFriendly(player,kingdom) ||
                        getKonquest().getKingdomManager().isPlayerAlly(player,kingdom)) {
                    towns.addAll(kingdom.getTowns());
                }
            }
        }
        // Sort by size
        towns.sort(townNameComparator);

        /* Town Icons */
        MenuIcon icon;
        boolean isClickable;
        boolean isFlagTravelAllowed;
        boolean isCooldownAllowed;
        for (KonTown currentTown : towns) {
            // Check for property flag
            isFlagTravelAllowed = currentTown.getPropertyValue(KonPropertyFlag.TRAVEL);
            // Check town travel cooldown
            isCooldownAllowed = !currentTown.isPlayerTravelDisabled(player.getBukkitPlayer().getUniqueId());
            // Make icon
            isClickable = isAdmin || (isFlagTravelAllowed && isCooldownAllowed);
            icon = new TownIcon(currentTown,getColor(player,currentTown),getRelation(player,currentTown),0,isClickable);
            icon.addNameValue(MessagePath.LABEL_KINGDOM.getMessage(), currentTown.getKingdom().getName());
            if (isClickable) {
                icon.addHint(MessagePath.MENU_HINT_TRAVEL.getMessage());
            } else if (!isFlagTravelAllowed) {
                icon.addAlert(MessagePath.PROTECTION_ERROR_BLOCKED.getMessage());
                icon.addError(MessagePath.COMMAND_TRAVEL_ERROR_DISABLED.getMessage());
            } else {
                String cooldown = currentTown.getPlayerTravelCooldownString(player.getBukkitPlayer().getUniqueId());
                icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
                icon.addError(MessagePath.COMMAND_TRAVEL_ERROR_COOLDOWN.getMessage(cooldown,currentTown.getName()));
            }
            icons.add(icon);
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(MenuState.TOWN_LIST)));
    }

    /**
     * Creates the kingdom list view.
     * This can be a multiple paged view.
     */
    private List<DisplayView> createKingdomView() {
        ArrayList<MenuIcon> icons = new ArrayList<>();
        List<KonKingdom> kingdoms = new ArrayList<>();

        if (isAccess(AccessType.ADMIN)) {
            // All kingdoms
            kingdoms.addAll(getKonquest().getKingdomManager().getKingdoms());
        } else {
            // Friendly and allied kingdoms
            for (KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
                if (getKonquest().getKingdomManager().isPlayerFriendly(player,kingdom) ||
                        getKonquest().getKingdomManager().isPlayerAlly(player,kingdom)) {
                    kingdoms.add(kingdom);
                }
            }
        }
        // Sort by size
        kingdoms.sort(kingdomNameComparator);

        /* Kingdom Icons */
        MenuIcon icon;
        boolean isClickable;
        boolean isFlagTravelAllowed;
        for (KonKingdom currentKingdom : kingdoms) {
            // Check for property flag
            isFlagTravelAllowed = currentKingdom.getCapital().getPropertyValue(KonPropertyFlag.TRAVEL);
            // Make icon
            isClickable = isAdmin || isFlagTravelAllowed;
            icon = new KingdomIcon(currentKingdom,getColor(player,currentKingdom),getRelation(player,currentKingdom),0,isClickable);
            if (isClickable) {
                icon.addHint(MessagePath.MENU_HINT_TRAVEL.getMessage());
            } else {
                icon.addAlert(MessagePath.PROTECTION_ERROR_BLOCKED.getMessage());
                icon.addError(MessagePath.COMMAND_TRAVEL_ERROR_DISABLED.getMessage());
            }
            icons.add(icon);
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(MenuState.KINGDOM_LIST)));
    }

    /**
     * Creates the sanctuary list view.
     * This can be a multiple paged view.
     */
    private List<DisplayView> createSanctuaryView() {
        ArrayList<MenuIcon> icons = new ArrayList<>();
        List<KonSanctuary> sanctuaries = new ArrayList<>(getKonquest().getSanctuaryManager().getSanctuaries());
        // Sort by name
        sanctuaries.sort(sanctuaryComparator);

        /* Sanctuary Icons */
        MenuIcon icon;
        boolean isClickable;
        boolean isFlagTravelAllowed;
        for (KonSanctuary currentSanctuary : sanctuaries) {
            // Check for property flag
            isFlagTravelAllowed = currentSanctuary.getPropertyValue(KonPropertyFlag.TRAVEL);
            // Make icon
            isClickable = isAdmin || isFlagTravelAllowed;
            icon = new SanctuaryIcon(currentSanctuary,0,isClickable);
            if (isClickable) {
                icon.addHint(MessagePath.MENU_HINT_TRAVEL.getMessage());
            } else {
                icon.addAlert(MessagePath.PROTECTION_ERROR_BLOCKED.getMessage());
                icon.addError(MessagePath.COMMAND_TRAVEL_ERROR_DISABLED.getMessage());
            }
            icons.add(icon);
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(MenuState.SANCTUARY_LIST)));
    }

    /**
     * Creates the camp list view.
     * This can be a multiple paged view.
     */
    private List<DisplayView> createCampView() {
        // (Admin) All camps
        ArrayList<MenuIcon> icons = new ArrayList<>();
        List<KonCamp> camps = new ArrayList<>(getKonquest().getCampManager().getCamps());
        // Sort by name
        camps.sort(campComparator);

        /* Camp Icons */
        MenuIcon icon;
        for (KonCamp currentCamp : camps) {
            icon = new CampIcon(currentCamp,0,true);
            icon.addHint(MessagePath.MENU_HINT_TRAVEL.getMessage());
            icons.add(icon);
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(MenuState.CAMP_LIST)));
    }

    /**
     * Creates the ruin list view.
     * This can be a multiple paged view.
     */
    private List<DisplayView> createRuinView() {
        // (Admin) All ruins
        ArrayList<MenuIcon> icons = new ArrayList<>();
        List<KonRuin> ruins = new ArrayList<>(getKonquest().getRuinManager().getRuins());
        // Sort by name
        ruins.sort(ruinComparator);

        /* Ruin Icons */
        MenuIcon icon;
        for (KonRuin currentRuin : ruins) {
            icon = new RuinIcon(currentRuin,0,true);
            icon.addHint(MessagePath.MENU_HINT_TRAVEL.getMessage());
            icons.add(icon);
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(MenuState.RUIN_LIST)));
    }

    /**
     * Creates the template list view.
     * This can be a multiple paged view.
     */
    private List<DisplayView> createTemplateView() {
        // (Admin) All templates
        ArrayList<MenuIcon> icons = new ArrayList<>();
        List<KonMonumentTemplate> templates = new ArrayList<>(getKonquest().getSanctuaryManager().getAllTemplates());
        // Sort by name
        templates.sort(templateComparator);

        /* Template Icons */
        MenuIcon icon;
        for (KonMonumentTemplate currentTemplate : templates) {
            icon = new TemplateIcon(currentTemplate,0,true);
            icon.addHint(MessagePath.MENU_HINT_TRAVEL.getMessage());
            icons.add(icon);
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(MenuState.TEMPLATE_LIST)));
    }

    /**
     * Create a list of views for a given menu state.
     * This creates new views, specific to each menu.
     *
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
            case TOWN_LIST:
                result.addAll(createTownView());
                break;
            case KINGDOM_LIST:
                result.addAll(createKingdomView());
                break;
            case SANCTUARY_LIST:
                result.addAll(createSanctuaryView());
                break;
            case CAMP_LIST:
                result.addAll(createCampView());
                break;
            case RUIN_LIST:
                result.addAll(createRuinView());
                break;
            case TEMPLATE_LIST:
                result.addAll(createTemplateView());
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
            DisplayView view = getCurrentView();
            if (view == null) return null;
            MenuIcon clickedIcon = view.getIcon(slot);
            MenuState currentState = (MenuState)getCurrentState();
            if (currentState == null) return null;
            MenuState nextState = (MenuState)clickedIcon.getState(); // could be null in some states
            // Travel info
            KonTerritory travelTerritory;
            Location travelLocation;
            // State logic
            switch (currentState) {
                case ROOT:
                    // Root view, use stored icon state
                    if (nextState == null) return null;
                    switch (nextState) {
                        case TOWN_LIST:
                        case KINGDOM_LIST:
                        case SANCTUARY_LIST:
                        case CAMP_LIST:
                        case RUIN_LIST:
                        case TEMPLATE_LIST:
                            // Go to next state as defined by icon
                            result = refreshNewView(nextState);
                            break;
                        case CAPITAL:
                            travelTerritory = player.getKingdom().getCapital();
                            travelLocation = travelTerritory.getSpawnLoc();
                            playerTravel(travelLocation, travelTerritory, TravelManager.TravelDestination.CAPITAL);
                            break;
                        case HOME:
                            travelLocation = player.getBukkitPlayer().getBedSpawnLocation();
                            playerTravel(travelLocation, null, TravelManager.TravelDestination.HOME);
                            break;
                        case CAMP:
                            if (getKonquest().getCampManager().isCampSet(player)) {
                                travelTerritory = getKonquest().getCampManager().getCamp(player);
                                travelLocation = travelTerritory.getSpawnLoc();
                                playerTravel(travelLocation, travelTerritory, TravelManager.TravelDestination.CAMP);
                            }
                            break;
                        case WILD:
                            playerTravel(null, null, TravelManager.TravelDestination.WILD);
                            break;
                    }
                    break;
                case TOWN_LIST:
                    // Travel to clicked town
                    if(clickedIcon instanceof TownIcon) {
                        TownIcon icon = (TownIcon)clickedIcon;
                        travelTerritory = icon.getTown();
                        travelLocation = travelTerritory.getSpawnLoc();
                        playerTravel(travelLocation, travelTerritory, TravelManager.TravelDestination.TOWN);
                    }
                    break;
                case KINGDOM_LIST:
                    // Travel to clicked kingdom capital
                    if(clickedIcon instanceof KingdomIcon) {
                        KingdomIcon icon = (KingdomIcon)clickedIcon;
                        travelTerritory = icon.getKingdom().getCapital();
                        travelLocation = travelTerritory.getSpawnLoc();
                        playerTravel(travelLocation, travelTerritory, TravelManager.TravelDestination.CAPITAL);
                    }
                    break;
                case SANCTUARY_LIST:
                    // Travel to clicked sanctuary
                    if(clickedIcon instanceof SanctuaryIcon) {
                        SanctuaryIcon icon = (SanctuaryIcon)clickedIcon;
                        travelTerritory = icon.getSanctuary();
                        travelLocation = travelTerritory.getSpawnLoc();
                        playerTravel(travelLocation, travelTerritory, TravelManager.TravelDestination.SANCTUARY);
                    }
                    break;
                case CAMP_LIST:
                    // (Admin) Travel to clicked camp
                    if(clickedIcon instanceof CampIcon) {
                        CampIcon icon = (CampIcon)clickedIcon;
                        travelLocation = icon.getCamp().getSpawnLoc();
                        playerTravel(travelLocation, null, TravelManager.TravelDestination.OTHER);
                    }
                    break;
                case RUIN_LIST:
                    // (Admin) Travel to clicked ruin
                    if(clickedIcon instanceof RuinIcon) {
                        RuinIcon icon = (RuinIcon)clickedIcon;
                        travelLocation = icon.getRuin().getSpawnLoc();
                        playerTravel(travelLocation, null, TravelManager.TravelDestination.OTHER);
                    }
                    break;
                case TEMPLATE_LIST:
                    // (Admin) Travel to clicked ruin
                    if(clickedIcon instanceof TemplateIcon) {
                        TemplateIcon icon = (TemplateIcon)clickedIcon;
                        travelLocation = icon.getTemplate().getSpawnLoc();
                        playerTravel(travelLocation, null, TravelManager.TravelDestination.OTHER);
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
                result = MessagePath.MENU_MAIN_TRAVEL.getMessage();
                break;
            case TOWN_LIST:
                result = MessagePath.LABEL_TOWNS.getMessage();
                break;
            case KINGDOM_LIST:
                result = MessagePath.LABEL_KINGDOMS.getMessage();
                break;
            case SANCTUARY_LIST:
                result = MessagePath.LABEL_SANCTUARIES.getMessage();
                break;
            case CAMP_LIST:
                result = MessagePath.LABEL_CAMPS.getMessage();
                break;
            case RUIN_LIST:
                result = MessagePath.LABEL_RUINS.getMessage();
                break;
            case TEMPLATE_LIST:
                result = MessagePath.LABEL_MONUMENT_TEMPLATES.getMessage();
                break;
            default:
                break;
        }
        if (isAdmin) {
            result = DisplayManager.adminFormat + MessagePath.LABEL_ADMIN.getMessage() + " - " + result;
        }
        return result;
    }

    private boolean isTravelInTerritoryAllowed() {
        // Verify player is not in enemy territory
        boolean blockEnemyTravel = getKonquest().getCore().getBoolean(CorePath.KINGDOMS_NO_ENEMY_TRAVEL.getPath());
        boolean blockCampTravel = getKonquest().getCore().getBoolean(CorePath.CAMPS_NO_ENEMY_TRAVEL.getPath());
        Location playerLoc = player.getBukkitPlayer().getLocation();
        if(getKonquest().getTerritoryManager().isChunkClaimed(playerLoc)) {
            KonTerritory locTerritory = getKonquest().getTerritoryManager().getChunkTerritory(playerLoc);
            assert locTerritory != null;
            KonKingdom locKingdom = locTerritory.getKingdom();
            boolean isKingdomTravelAllowed = locTerritory.getTerritoryType().equals(KonquestTerritoryType.SANCTUARY) ||
                    locKingdom.equals(player.getKingdom()) ||
                    getKonquest().getKingdomManager().isPlayerPeace(player, locKingdom) ||
                    getKonquest().getKingdomManager().isPlayerTrade(player, locKingdom) ||
                    getKonquest().getKingdomManager().isPlayerAlly(player, locKingdom);
            boolean isCampTravelBlocked = (locTerritory instanceof KonCamp) && !((KonCamp)locTerritory).isPlayerOwner(player.getBukkitPlayer());
            return (!blockEnemyTravel || isKingdomTravelAllowed) && (!blockCampTravel || !isCampTravelBlocked);
        }
        return true;
    }

    private void playerTravel(Location travelLocation, KonTerritory travelTerritory, TravelManager.TravelDestination travelDestination) {
        boolean status;
        if (travelDestination.equals(TravelManager.TravelDestination.WILD)) {
            // For wild travel
            status = true;
            getKonquest().getTravelManager().submitWildTravel(player.getBukkitPlayer());
        } else {
            // For all other travel
            assert travelLocation != null;
            if (isAccess(AccessType.ADMIN)) {
                status = true;
                // Teleport player to destination
                getKonquest().telePlayerLocation(player.getBukkitPlayer(), travelLocation);
            } else {
                // Submit travel plan to the travel manager
                status = getKonquest().getTravelManager().submitTravel(player.getBukkitPlayer(), travelDestination, travelTerritory, travelLocation);
            }
        }
        playStatusSound(player.getBukkitPlayer(),status);
    }

}
