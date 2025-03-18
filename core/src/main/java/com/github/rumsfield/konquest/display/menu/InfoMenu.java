package com.github.rumsfield.konquest.display.menu;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.api.model.KonquestDiplomacyType;
import com.github.rumsfield.konquest.api.model.KonquestRelationshipType;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.command.CommandType;
import com.github.rumsfield.konquest.display.DisplayView;
import com.github.rumsfield.konquest.display.StateMenu;
import com.github.rumsfield.konquest.display.icon.*;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;

import java.util.*;

public class InfoMenu extends StateMenu {

    enum MenuState implements State {
        ROOT,
        PLAYER_LIST,
        PLAYER_INFO,
        PLAYER_INFO_TOWNS,
        PLAYER_INFO_STATS,
        KINGDOM_LIST,
        KINGDOM_INFO,
        KINGDOM_INFO_ENEMIES,
        KINGDOM_INFO_ALLIES,
        KINGDOM_INFO_TRADERS,
        KINGDOM_INFO_OFFICERS,
        KINGDOM_INFO_MEMBERS,
        KINGDOM_INFO_TOWNS,
        CAPITAL_LIST,
        TOWN_LIST,
        TOWN_INFO,
        TOWN_INFO_OPTIONS,
        TOWN_INFO_UPGRADES,
        TOWN_INFO_KNIGHTS,
        TOWN_INFO_RESIDENTS,
        SANCTUARY_LIST,
        SANCTUARY_INFO,
        SANCTUARY_INFO_MONUMENTS,
        RUIN_LIST,
        RUIN_INFO,
        CAMP_LIST,
        CAMP_INFO,
        MONUMENT_LIST,
        MONUMENT_INFO,
        MONUMENT_INFO_KINGDOMS,
        DISPLAY_PLAYER_SCORE,
        DISPLAY_KINGDOM_SCORE
    }

    private final Material flagMaterial = Material.ORANGE_BANNER;
    private final Material propMaterial = Material.PAPER;

    private final HashMap<MenuState,MenuState> returnStack;
    private final KonPlayer player;
    private KonOfflinePlayer infoPlayer;
    private KonKingdom infoKingdom;
    private KonTown infoTown;
    private KonCamp infoCamp;
    private KonRuin infoRuin;
    private KonSanctuary infoSanctuary;
    private KonMonumentTemplate infoTemplate;

    public InfoMenu(Konquest konquest, KonPlayer player) {
        super(konquest, MenuState.ROOT, null);
        this.player = player;
        this.infoPlayer = null;
        this.infoKingdom = null;
        this.infoTown = null;
        this.infoCamp = null;
        this.infoRuin = null;
        this.infoSanctuary = null;
        this.infoTemplate = null;
        this.returnStack = new HashMap<>();

        /* Initialize menu view */
        setCurrentView(MenuState.ROOT);

    }

    public void goToPlayerInfo(KonOfflinePlayer player) {
        this.infoPlayer = player;
        refreshNewView(MenuState.PLAYER_INFO);
    }

    public void goToKingdomInfo(KonKingdom kingdom) {
        this.infoKingdom = kingdom;
        refreshNewView(MenuState.KINGDOM_INFO);
    }

    public void goToTownInfo(KonTown town) {
        this.infoTown = town;
        refreshNewView(MenuState.TOWN_INFO);
    }

    public void goToCampInfo(KonCamp camp) {
        this.infoCamp = camp;
        refreshNewView(MenuState.CAMP_INFO);
    }

    public void goToRuinInfo(KonRuin ruin) {
        this.infoRuin = ruin;
        refreshNewView(MenuState.RUIN_INFO);
    }

    public void goToSanctuaryInfo(KonSanctuary sanctuary) {
        this.infoSanctuary = sanctuary;
        refreshNewView(MenuState.SANCTUARY_INFO);
    }

    public void goToTemplateInfo(KonMonumentTemplate template) {
        this.infoTemplate = template;
        refreshNewView(MenuState.MONUMENT_INFO);
    }

    public void goToTemplateListInfo() {
        refreshNewView(MenuState.MONUMENT_LIST);
    }

    /* ============================================ *
     *                   Root View                  *
     * ============================================ */

    /**
     * Creates the root menu view for this menu.
     * This is a single page view.
     */
    private DisplayView createRootView() {
        DisplayView result;
        MenuIcon icon;

        /* Icon slot indexes */
        // Row 0: 0  1  2  3  4  5  6  7  8
        int ROOT_SLOT_PLAYERS = 1;
        int ROOT_SLOT_KINGDOMS = 3;
        int ROOT_SLOT_CAMPS = 5;
        int ROOT_SLOT_RUINS = 7;
        // Row 1: 9 10 11 12 13 14 15 16 17
        int ROOT_SLOT_CAPITALS = 9;
        int ROOT_SLOT_TOWNS = 11;
        int ROOT_SLOT_SANCTUARIES = 15;
        int ROOT_SLOT_MONUMENTS = 17;

        result = new DisplayView(2, getTitle(MenuState.ROOT));

        /* Players Icon */
        icon = new InfoIcon(MessagePath.LABEL_PLAYERS.getMessage(), Material.PLAYER_HEAD, ROOT_SLOT_PLAYERS, true);
        icon.addProperty(MessagePath.LABEL_INFORMATION.getMessage());
        int numPlayers = getKonquest().getPlayerManager().getAllKonquestOfflinePlayers().size();
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), numPlayers);
        icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        icon.setState(MenuState.PLAYER_LIST);
        result.addIcon(icon);

        /* Kingdoms Icon */
        icon = new InfoIcon(MessagePath.LABEL_KINGDOMS.getMessage(), Material.DIAMOND_HELMET, ROOT_SLOT_KINGDOMS, true);
        icon.addProperty(MessagePath.LABEL_INFORMATION.getMessage());
        int numKingdoms = getKonquest().getKingdomManager().getKingdoms().size();
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), numKingdoms);
        icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        icon.setState(MenuState.KINGDOM_LIST);
        result.addIcon(icon);

        /* Camps Icon */
        icon = new InfoIcon(MessagePath.LABEL_CAMPS.getMessage(), Material.ORANGE_BED, ROOT_SLOT_CAMPS, true);
        icon.addProperty(MessagePath.LABEL_INFORMATION.getMessage());
        int numCamps = getKonquest().getCampManager().getCamps().size();
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), numCamps);
        icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        icon.setState(MenuState.CAMP_LIST);
        result.addIcon(icon);

        /* Ruins Icon */
        icon = new InfoIcon(MessagePath.LABEL_RUINS.getMessage(), Material.MOSSY_COBBLESTONE, ROOT_SLOT_RUINS, true);
        icon.addProperty(MessagePath.LABEL_INFORMATION.getMessage());
        int numRuins = getKonquest().getRuinManager().getRuins().size();
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), numRuins);
        icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        icon.setState(MenuState.RUIN_LIST);
        result.addIcon(icon);

        /* Capitals Icon */
        icon = new InfoIcon(MessagePath.LABEL_CAPITALS.getMessage(), Material.NETHERITE_BLOCK, ROOT_SLOT_CAPITALS, true);
        icon.addProperty(MessagePath.LABEL_INFORMATION.getMessage());
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), numKingdoms);
        icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        icon.setState(MenuState.CAPITAL_LIST);
        result.addIcon(icon);

        /* Towns Icon */
        icon = new InfoIcon(MessagePath.LABEL_TOWNS.getMessage(), Material.OBSIDIAN, ROOT_SLOT_TOWNS, true);
        icon.addProperty(MessagePath.LABEL_INFORMATION.getMessage());
        int numTowns = 0;
        for (KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
            numTowns += kingdom.getNumTowns();
        }
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), numTowns);
        icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        icon.setState(MenuState.TOWN_LIST);
        result.addIcon(icon);

        /* Sanctuaries Icon */
        icon = new InfoIcon(MessagePath.LABEL_SANCTUARIES.getMessage(), Material.SMOOTH_QUARTZ, ROOT_SLOT_SANCTUARIES, true);
        icon.addProperty(MessagePath.LABEL_INFORMATION.getMessage());
        int numSanctuaries = getKonquest().getSanctuaryManager().getSanctuaries().size();
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), numSanctuaries);
        icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        icon.setState(MenuState.SANCTUARY_LIST);
        result.addIcon(icon);

        /* Monument Templates Icon */
        icon = new InfoIcon(MessagePath.LABEL_MONUMENT_TEMPLATES.getMessage(), Material.CRAFTING_TABLE, ROOT_SLOT_MONUMENTS, true);
        icon.addProperty(MessagePath.LABEL_INFORMATION.getMessage());
        int numTemplates = getKonquest().getSanctuaryManager().getNumTemplates();
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), numTemplates);
        icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        icon.setState(MenuState.MONUMENT_LIST);
        result.addIcon(icon);

        /* Navigation */
        addNavEmpty(result);
        addNavHome(result);
        addNavClose(result);

        return result;
    }

    /* ============================================ *
     *                  List Views                  *
     * ============================================ */

    /**
     * Creates the player list view.
     * This can be a multiple paged view.
     * Context states: PLAYER_LIST, KINGDOM_INFO_OFFICERS, KINGDOM_INFO_MEMBERS, TOWN_INFO_KNIGHTS, TOWN_INFO_RESIDENTS
     */
    private List<DisplayView> createPlayerView(MenuState context) {
        ArrayList<MenuIcon> icons = new ArrayList<>();
        List<KonOfflinePlayer> players = new ArrayList<>();

        // Get list of players
        switch (context) {
            case PLAYER_LIST:
                players.addAll(getKonquest().getPlayerManager().getAllKonquestOfflinePlayers());
                break;
            case KINGDOM_INFO_OFFICERS:
                if (infoKingdom != null) {
                    for (OfflinePlayer officer : infoKingdom.getPlayerOfficersOnly()) {
                        players.add(getKonquest().getPlayerManager().getOfflinePlayer(officer));
                    }
                }
                break;
            case KINGDOM_INFO_MEMBERS:
                if (infoKingdom != null) {
                    for (OfflinePlayer member : infoKingdom.getPlayerMembersOnly()) {
                        players.add(getKonquest().getPlayerManager().getOfflinePlayer(member));
                    }
                }
                break;
            case TOWN_INFO_KNIGHTS:
                if (infoTown != null) {
                    for (OfflinePlayer knight : infoTown.getPlayerKnightsOnly()) {
                        players.add(getKonquest().getPlayerManager().getOfflinePlayer(knight));
                    }
                }
                break;
            case TOWN_INFO_RESIDENTS:
                if (infoTown != null) {
                    for (OfflinePlayer resident : infoTown.getPlayerResidentsOnly()) {
                        players.add(getKonquest().getPlayerManager().getOfflinePlayer(resident));
                    }
                }
                break;
            default:
                break;
        }

        // Sort by alphabetical names
        players.sort(playerComparator);

        /* Player Icons */
        MenuIcon icon;
        for (KonOfflinePlayer currentPlayer : players) {
            icon = new PlayerIcon(currentPlayer.getOfflineBukkitPlayer(),getColor(player,currentPlayer),getRelation(player,currentPlayer),0,true);
            // Context lore
            switch (context) {
                case PLAYER_LIST:
                    if (!currentPlayer.isBarbarian()) {
                        icon.addNameValue(MessagePath.LABEL_KINGDOM.getMessage(), currentPlayer.getKingdom().getName());
                    }
                    break;
                case KINGDOM_INFO_OFFICERS:
                    icon.addProperty(MessagePath.RELATIONSHIP_RANK_OFFICER.getMessage());
                    break;
                case KINGDOM_INFO_MEMBERS:
                    icon.addProperty(MessagePath.RELATIONSHIP_RANK_MEMBER.getMessage());
                    break;
                case TOWN_INFO_KNIGHTS:
                    icon.addProperty(MessagePath.RELATIONSHIP_ROLE_KNIGHT.getMessage());
                    break;
                case TOWN_INFO_RESIDENTS:
                    icon.addProperty(MessagePath.RELATIONSHIP_ROLE_RESIDENT.getMessage());
                    break;
                default:
                    break;
            }
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
            icon.setState(MenuState.PLAYER_INFO);
            icons.add(icon);
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(context)));
    }

    /**
     * Creates the kingdom list view.
     * This can be a multiple paged view.
     * Context states: KINGDOM_LIST, MONUMENT_INFO_KINGDOMS, KINGDOM_INFO_ENEMIES, KINGDOM_INFO_ALLIES, KINGDOM_INFO_TRADERS
     */
    private List<DisplayView> createKingdomView(MenuState context) {
        ArrayList<MenuIcon> icons = new ArrayList<>();
        List<KonKingdom> kingdoms = new ArrayList<>();

        // Get list of kingdoms
        switch (context) {
            case KINGDOM_LIST:
                kingdoms.addAll(getKonquest().getKingdomManager().getKingdoms());
                break;
            case MONUMENT_INFO_KINGDOMS:
                if (infoTemplate != null) {
                    for(KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
                        KonMonumentTemplate kingdomTemplate = kingdom.getMonumentTemplate();
                        if(kingdomTemplate != null && kingdomTemplate.equals(infoTemplate)) {
                            kingdoms.add(kingdom);
                        }
                    }
                }
                break;
            case KINGDOM_INFO_ENEMIES:
                if (infoKingdom != null) {
                    kingdoms.addAll(infoKingdom.getActiveRelationKingdoms(KonquestDiplomacyType.WAR));
                }
                break;
            case KINGDOM_INFO_ALLIES:
                if (infoKingdom != null) {
                    kingdoms.addAll(infoKingdom.getActiveRelationKingdoms(KonquestDiplomacyType.ALLIANCE));
                }
                break;
            case KINGDOM_INFO_TRADERS:
                if (infoKingdom != null) {
                    kingdoms.addAll(infoKingdom.getActiveRelationKingdoms(KonquestDiplomacyType.TRADE));
                }
                break;
            default:
                break;
        }

        // Sort by size
        kingdoms.sort(kingdomComparator);

        /* Kingdom Icons */
        MenuIcon icon;
        for (KonKingdom currentKingdom : kingdoms) {
            icon = new KingdomIcon(currentKingdom,getColor(player,currentKingdom),getRelation(player,currentKingdom),0,true);
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
            icon.setState(MenuState.KINGDOM_INFO);
            icons.add(icon);
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(context)));
    }

    /**
     * Creates the town list view.
     * This can be a multiple paged view.
     * Context states: TOWN_LIST, CAPITAL_LIST, PLAYER_INFO_TOWNS, KINGDOM_INFO_TOWNS
     */
    private List<DisplayView> createTownView(MenuState context) {
        ArrayList<MenuIcon> icons = new ArrayList<>();
        List<KonTown> towns = new ArrayList<>();

        // Get list of towns (or capitals)
        switch (context) {
            case TOWN_LIST:
                for (KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
                    towns.addAll(kingdom.getTowns());
                }
                break;
            case CAPITAL_LIST:
                for (KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
                    towns.add(kingdom.getCapital());
                }
                break;
            case PLAYER_INFO_TOWNS:
                if (infoPlayer != null) {
                    towns.addAll(getKonquest().getKingdomManager().getPlayerResidenceTowns(infoPlayer));
                }
                break;
            case KINGDOM_INFO_TOWNS:
                if (infoKingdom != null) {
                    towns.addAll(infoKingdom.getTowns());
                }
                break;
            default:
                break;
        }

        // Sort by size
        towns.sort(townComparator);

        /* Town Icons */
        MenuIcon icon;
        for (KonTown currentTown : towns) {
            icon = new TownIcon(currentTown,getColor(player,currentTown),getRelation(player,currentTown),0,true);
            if (context.equals(MenuState.PLAYER_INFO_TOWNS) && infoPlayer != null) {
                String townRole = currentTown.getPlayerRoleName(infoPlayer);
                if(!townRole.isEmpty()) {
                    icon.addNameValue(MessagePath.LABEL_TOWN_ROLE.getMessage(), townRole);
                }
            }
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
            icon.setState(MenuState.TOWN_INFO);
            icons.add(icon);
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(context)));
    }

    /**
     * Creates the camp list view.
     * This can be a multiple paged view.
     */
    private List<DisplayView> createCampView() {
        ArrayList<MenuIcon> icons = new ArrayList<>();
        List<KonCamp> camps = new ArrayList<>(getKonquest().getCampManager().getCamps());
        // Sort by name
        camps.sort(campComparator);

        /* Camp Icons */
        MenuIcon icon;
        for (KonCamp currentCamp : camps) {
            icon = new CampIcon(currentCamp,0,true);
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
            icon.setState(MenuState.CAMP_INFO);
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
        ArrayList<MenuIcon> icons = new ArrayList<>();
        List<KonRuin> ruins = new ArrayList<>(getKonquest().getRuinManager().getRuins());
        // Sort by name
        ruins.sort(ruinComparator);

        /* Ruin Icons */
        MenuIcon icon;
        for (KonRuin currentRuin : ruins) {
            icon = new RuinIcon(currentRuin,0,true);
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
            icon.setState(MenuState.RUIN_INFO);
            icons.add(icon);
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(MenuState.RUIN_LIST)));
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
        for (KonSanctuary currentSanctuary : sanctuaries) {
            icon = new SanctuaryIcon(currentSanctuary,0,true);
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
            icon.setState(MenuState.SANCTUARY_INFO);
            icons.add(icon);
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(MenuState.SANCTUARY_LIST)));
    }

    /**
     * Creates the monument template list view.
     * This can be a multiple paged view.
     * Context states: MONUMENT_LIST, SANCTUARY_INFO_MONUMENTS
     */
    private List<DisplayView> createMonumentView(MenuState context) {
        ArrayList<MenuIcon> icons = new ArrayList<>();
        List<KonMonumentTemplate> templates = new ArrayList<>();

        // Get list of monuments
        switch (context) {
            case MONUMENT_LIST:
                templates.addAll(getKonquest().getSanctuaryManager().getAllTemplates());
                break;
            case SANCTUARY_INFO_MONUMENTS:
                if (infoSanctuary != null) {
                    templates.addAll(infoSanctuary.getTemplates());
                }
                break;
            default:
                break;
        }

        // Sort by name
        templates.sort(templateComparator);

        /* Template Icons */
        MenuIcon icon;
        for (KonMonumentTemplate currentTemplate : templates) {
            icon = new TemplateIcon(currentTemplate,0,true);
            // Context Lore
            if (context.equals(MenuState.MONUMENT_LIST)) {
                icon.addNameValue(MessagePath.TERRITORY_SANCTUARY.getMessage(), getKonquest().getSanctuaryManager().getSanctuaryNameOfTemplate(currentTemplate.getName()));
            }
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
            icon.setState(MenuState.MONUMENT_INFO);
            icons.add(icon);
        }

        /* Error Icon */
        if (templates.isEmpty()) {
            icon = new InfoIcon(ChatColor.DARK_RED+MessagePath.LABEL_MONUMENT_TEMPLATE.getMessage(),Material.BARRIER,0,false);
            icon.addError(MessagePath.COMMAND_ADMIN_KINGDOM_ERROR_NO_TEMPLATES.getMessage());
            icons.add(icon);
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(context)));
    }

    /**
     * Creates the stats views.
     * This can be a multiple paged view.
     */
    private List<DisplayView> createStatsView() {
        if (infoPlayer == null) return Collections.emptyList();
        ArrayList<MenuIcon> icons = new ArrayList<>();
        MenuIcon icon;

        // Get stats from online player or database
        KonPlayer onlineInfoPlayer = getKonquest().getPlayerManager().getPlayerFromID(infoPlayer.getOfflineBukkitPlayer().getUniqueId());
        KonStats stats;
        if(onlineInfoPlayer == null) {
            // Use offline player, pull stats from DB
            stats = getKonquest().getDatabaseThread().getDatabase().pullPlayerStats(infoPlayer.getOfflineBukkitPlayer());
        } else {
            // Use online player's active stats
            stats = onlineInfoPlayer.getPlayerStats();
        }

        /* Stat Icons */
        for (KonStatsType stat : KonStatsType.values()) {
            int statValue = stats.getStat(stat);
            icon = new StatIcon(stat, statValue, 0, false);
            icons.add(icon);
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(MenuState.PLAYER_INFO_STATS)));
    }

    /**
     * Creates the options views.
     * This can be a multiple paged view.
     */
    private List<DisplayView> createOptionsView() {
        if (infoTown == null) return Collections.emptyList();
        ArrayList<MenuIcon> icons = new ArrayList<>();
        MenuIcon icon;

        /* Option Icons */
        for (KonTownOption option : KonTownOption.values()) {
            boolean optionValue = infoTown.getTownOption(option);
            String currentValue = DisplayManager.boolean2Lang(optionValue) + " " + DisplayManager.boolean2Symbol(optionValue);
            icon = new OptionIcon(option, 0, false);
            icon.addNameValue(MessagePath.LABEL_CURRENT.getMessage(), currentValue);
            icons.add(icon);
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(MenuState.TOWN_INFO_OPTIONS)));
    }

    /**
     * Creates the upgrades views.
     * This can be a multiple paged view.
     */
    private List<DisplayView> createUpgradesView() {
        if (infoTown == null || !getKonquest().getUpgradeManager().isEnabled()) return Collections.emptyList();
        ArrayList<MenuIcon> icons = new ArrayList<>();
        MenuIcon icon;

        /* Upgrade Icons */
        String upgradeName;
        int upgradeLevel;
        boolean isDisabled;
        for (KonUpgrade upgrade : KonUpgrade.values()) {
            int currentLevel = infoTown.getRawUpgradeLevel(upgrade);
            if (currentLevel <= 0) continue;
            if (infoTown.isUpgradeDisabled(upgrade)) {
                int reducedLevel = infoTown.getUpgradeLevel(upgrade);
                upgradeLevel = reducedLevel;
                if (reducedLevel > 0) {
                    upgradeName = upgrade.getName() + " " + ChatColor.GRAY + reducedLevel;
                } else {
                    upgradeName = ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + upgrade.getName() + " " + reducedLevel;
                }
                isDisabled = true;
            } else {
                upgradeLevel = currentLevel;
                upgradeName = upgrade.getName() + " " + currentLevel;
                isDisabled = false;
            }
            icon = new InfoIcon(upgradeName, upgrade.getIcon(), 0, false);
            icon.addDescription(upgrade.getLevelDescription(upgradeLevel));
            if (isDisabled) {
                icon.addAlert(MessagePath.LABEL_DISABLED.getMessage());
                icon.addError(MessagePath.UPGRADE_DISABLED.getMessage());
            }
            icons.add(icon);
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(MenuState.TOWN_INFO_UPGRADES)));
    }

    /* ============================================ *
     *                  Info Views                  *
     * ============================================ */

    /**
     * Creates the player info view.
     * This is a single page view.
     */
    private DisplayView createPlayerInfoView() {
        if (infoPlayer == null) return null;
        DisplayView result;
        MenuIcon icon;

        /* Icon slot indexes */
        int rows = 1;
        // Row 0: 0  1  2  3  4  5  6  7  8
        int SLOT_PLAYER = 1;
        int SLOT_KINGDOM_CAMP = 3;
        int SLOT_SCORE = 4;
        int SLOT_TOWNS = 6;
        int SLOT_STATS = 7;
        // Row 1: 9 10 11 12 13 14 15 16 17

        String contextColor = getColor(player,infoPlayer);
        KonquestRelationshipType relation = getRelation(player,infoPlayer);
        String title = MessagePath.LABEL_PLAYER.getMessage()+" "+infoPlayer.getOfflineBukkitPlayer().getName();
        result = new DisplayView(rows, title);

        /* Player Icon */
        icon = new PlayerIcon(infoPlayer.getOfflineBukkitPlayer(), contextColor, relation, SLOT_PLAYER, false);
        if (!infoPlayer.isBarbarian()) {
            icon.addNameValue(MessagePath.LABEL_KINGDOM_RANK.getMessage(), infoPlayer.getKingdom().getPlayerRankName(infoPlayer));
        }
        result.addIcon(icon);

        if (infoPlayer.isBarbarian()) {
            /* Camp Icon */
            if (getKonquest().getCampManager().isCampSet(infoPlayer)) {
                icon = new CampIcon(getKonquest().getCampManager().getCamp(infoPlayer), SLOT_KINGDOM_CAMP, true);
                icon.addDescription(MessagePath.MENU_INFO_CAMP_PLACED.getMessage());
                icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
                icon.setState(MenuState.CAMP_INFO);
            } else {
                icon = new InfoIcon(MessagePath.TERRITORY_CAMP.getMessage(), Material.BARRIER, SLOT_KINGDOM_CAMP, false);
                icon.addProperty(MessagePath.LABEL_BARBARIANS.getMessage());
                icon.addDescription(MessagePath.MENU_INFO_CAMP_MISSING.getMessage());
            }
        } else {
            /* Kingdom Icon */
            icon = new KingdomIcon(infoPlayer.getKingdom(), contextColor, relation, SLOT_KINGDOM_CAMP, true);
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
            icon.setState(MenuState.KINGDOM_INFO);
        }
        result.addIcon(icon);

        /* Score Icon (Unavailable to Barbarians and peaceful kingdom member) */
        boolean isScoreClickable = !infoPlayer.isBarbarian() && !infoPlayer.getKingdom().isPeaceful();
        icon = new InfoIcon(MessagePath.MENU_SCORE_PLAYER_SCORE.getMessage(), CommandType.SCORE.iconMaterial(), SLOT_SCORE, isScoreClickable);
        if (isScoreClickable) {
            int playerScore = getKonquest().getKingdomManager().getPlayerScore(infoPlayer);
            icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), playerScore);
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
            icon.setState(MenuState.DISPLAY_PLAYER_SCORE);
        } else {
            icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
            if (infoPlayer.isBarbarian()) {
                icon.addError(MessagePath.COMMAND_SCORE_ERROR_BARBARIAN.getMessage(infoPlayer.getOfflineBukkitPlayer().getName()));
            } else {
                icon.addError(MessagePath.COMMAND_SCORE_ERROR_PEACEFUL.getMessage(infoPlayer.getKingdom().getName()));
            }
        }
        result.addIcon(icon);

        /* Town List Icon (Unavailable to Barbarians) */
        boolean isTownClickable = !infoPlayer.isBarbarian();
        icon = new InfoIcon(MessagePath.LABEL_TOWNS.getMessage(), CommandType.TOWN.iconMaterial(), SLOT_TOWNS, isTownClickable);
        if (isTownClickable) {
            int numTowns = getKonquest().getKingdomManager().getPlayerResidencies(infoPlayer);
            icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), numTowns);
            icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
            icon.setState(MenuState.PLAYER_INFO_TOWNS);
        } else {
            icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
        }
        result.addIcon(icon);

        /* Stats List Icon */
        icon = new InfoIcon(MessagePath.MENU_PREFIX_TITLE_STATS.getMessage(), CommandType.STATS.iconMaterial(), SLOT_STATS, true);
        icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
        icon.setState(MenuState.PLAYER_INFO_STATS);
        result.addIcon(icon);

        /* Navigation */
        addNavEmpty(result);
        addNavClose(result);
        addNavReturn(result);

        return result;
    }

    /**
     * Creates the kingdom info view.
     * This is a single page view.
     * The info kingdom could be barbarians or neutrals (not created)
     */
    private DisplayView createKingdomInfoView() {
        if (infoKingdom == null) return null;
        DisplayView result;
        MenuIcon icon;

        /* Icon slot indexes */
        int rows = 3;
        // Row 0: 0  1  2  3  4  5  6  7  8
        int SLOT_KINGDOM = 0;
        int SLOT_DIPLOMACY = 1;
        int SLOT_FLAGS = 2;
        int SLOT_PROPS = 3;
        int SLOT_CAPITAL = 5;
        int SLOT_MASTER = 6;
        int SLOT_MONUMENT = 7;
        int SLOT_SCORE = 8;
        // Row 1: 9 10 11 12 13 14 15 16 17
        int SLOT_OFFICERS = 12;
        int SLOT_MEMBERS = 13;
        int SLOT_TOWNS = 14;
        // Row 2: 18 19 20 21 22 23 24 25 26
        int SLOT_ENEMIES = 21;
        int SLOT_ALLIES = 22;
        int SLOT_TRADERS = 23;

        boolean isClickable = infoKingdom.isCreated();
        String contextColor = getColor(player,infoKingdom);
        KonquestRelationshipType relation = getRelation(player,infoKingdom);
        String title;
        if (infoKingdom.isCreated()) {
            title = MessagePath.LABEL_KINGDOM.getMessage()+" "+infoKingdom.getName();
        } else {
            title = infoKingdom.getName();
        }
        result = new DisplayView(rows, title);

        /* Kingdom Icon */
        int numKingdomPlayers = getKonquest().getPlayerManager().getPlayersInKingdom(infoKingdom).size();
        int numKingdomOfficers = infoKingdom.getPlayerOfficersOnly().size();
        int numKingdomFavor = 0;
        for(KonOfflinePlayer kingdomPlayer : getKonquest().getPlayerManager().getAllPlayersInKingdom(infoKingdom)) {
            numKingdomFavor += (int) KonquestPlugin.getBalance(kingdomPlayer.getOfflineBukkitPlayer());
        }
        int currentWebColor = infoKingdom.getWebColor();
        String colorName = "default";
        if(currentWebColor != -1) {
            colorName = ChatUtil.reverseLookupColorRGB(currentWebColor);
        }
        String webColorFormat = infoKingdom.getWebColorString()+colorName;
        icon = new KingdomIcon(infoKingdom, contextColor, relation, SLOT_KINGDOM, false);
        icon.addNameValue(MessagePath.LABEL_ONLINE_PLAYERS.getMessage(), numKingdomPlayers);
        if (infoKingdom.isCreated()) {
            icon.addNameValue(MessagePath.LABEL_OFFICERS.getMessage(), numKingdomOfficers);
            icon.addNameValue(MessagePath.LABEL_FAVOR.getMessage(), numKingdomFavor);
            icon.addNameValue(MessagePath.LABEL_MAP_COLOR.getMessage(), webColorFormat);
        }
        result.addIcon(icon);

        /* Diplomacy Icon */
        KonquestDiplomacyType currentDiplomacy = getKonquest().getKingdomManager().getDiplomacy(player.getKingdom(),infoKingdom);
        icon = new DiplomacyIcon(currentDiplomacy, SLOT_DIPLOMACY, false);
        icon.addProperty(MessagePath.LABEL_DIPLOMACY.getMessage());
        if(player.getKingdom().hasRelationRequest(infoKingdom) || infoKingdom.hasRelationRequest(player.getKingdom())) {
            icon.addAlert(MessagePath.MENU_KINGDOM_REQUESTS.getMessage());
        }
        if(player.getKingdom().hasRelationRequest(infoKingdom)) {
            // They have sent a valid diplomacy change request to us
            String ourRequestStatus = Labeler.lookup(player.getKingdom().getRelationRequest(infoKingdom));
            icon.addNameValue(MessagePath.MENU_KINGDOM_THEY_REQUESTED.getMessage(), ourRequestStatus);
        }
        if(infoKingdom.hasRelationRequest(player.getKingdom())) {
            // We have sent a valid diplomacy change request to them
            String theirRequestStatus = Labeler.lookup(infoKingdom.getRelationRequest(player.getKingdom()));
            icon.addNameValue(MessagePath.MENU_KINGDOM_WE_REQUESTED.getMessage(), theirRequestStatus);
        }
        switch(currentDiplomacy) {
            case PEACE:
                icon.addDescription(MessagePath.MENU_KINGDOM_DIPLOMACY_PEACE_INFO.getMessage());
                break;
            case TRADE:
                icon.addDescription(MessagePath.MENU_KINGDOM_DIPLOMACY_TRADE_INFO.getMessage());
                break;
            case WAR:
                icon.addDescription(MessagePath.MENU_KINGDOM_DIPLOMACY_WAR_INFO.getMessage());
                break;
            case ALLIANCE:
                icon.addDescription(MessagePath.MENU_KINGDOM_DIPLOMACY_ALLIANCE_INFO.getMessage());
                break;
            default:
                break;
        }
        result.addIcon(icon);

        /* Flags Icon */
        icon = new InfoIcon(MessagePath.LABEL_FLAGS.getMessage(), flagMaterial, SLOT_FLAGS, false);
        for(KonPropertyFlag flag : KonPropertyFlag.values()) {
            if(infoKingdom.hasPropertyValue(flag)) {
                String flagDisplaySymbol = DisplayManager.boolean2Symbol(infoKingdom.getPropertyValue(flag));
                icon.addNameValue(flag.getName(), flagDisplaySymbol);
            }
        }
        result.addIcon(icon);

        /* Properties Icon */
        icon = new InfoIcon(MessagePath.LABEL_PROPERTIES.getMessage(), propMaterial, SLOT_PROPS, false);
        icon.addNameValue(MessagePath.LABEL_ADMIN_KINGDOM.getMessage(), DisplayManager.boolean2Symbol(infoKingdom.isAdminOperated()));
        icon.addNameValue(MessagePath.LABEL_PROTECTED.getMessage(), DisplayManager.boolean2Symbol(infoKingdom.isOfflineProtected()));
        icon.addNameValue(MessagePath.LABEL_OPEN.getMessage(), DisplayManager.boolean2Symbol(infoKingdom.isOpen()));
        icon.addNameValue(MessagePath.LABEL_SMALLEST.getMessage(), DisplayManager.boolean2Symbol(infoKingdom.isSmallest()));
        icon.addNameValue(MessagePath.LABEL_PEACEFUL.getMessage(), DisplayManager.boolean2Symbol(infoKingdom.isPeaceful()));
        result.addIcon(icon);

        /* Capital Icon (Unavailable to Barbarians) */
        if (isClickable) {
            icon = new TownIcon(infoKingdom.getCapital(), contextColor, relation, SLOT_CAPITAL, true);
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
            icon.setState(MenuState.TOWN_INFO);
        } else {
            icon = new InfoIcon(MessagePath.TERRITORY_CAPITAL.getMessage(), Material.NETHERITE_BLOCK, SLOT_CAPITAL, false);
            icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
        }
        result.addIcon(icon);

        /* Master Icon (Unavailable to Barbarians and Admin Kingdoms) */
        if (infoKingdom.isCreated() && !infoKingdom.isAdminOperated()) {
            if (infoKingdom.isMasterValid()) {
                icon = new PlayerIcon(infoKingdom.getPlayerMaster(), contextColor, relation, SLOT_MASTER, true);
                icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
                icon.setState(MenuState.PLAYER_INFO);
            } else {
                icon = new InfoIcon(MessagePath.RELATIONSHIP_RANK_MASTER.getMessage(), Material.BARRIER, SLOT_MASTER, false);
                icon.addAlert(MessagePath.LABEL_INVALID.getMessage());
            }
        } else {
            icon = new InfoIcon(MessagePath.RELATIONSHIP_RANK_MASTER.getMessage(), Material.IRON_HELMET, SLOT_MASTER, false);
            icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
        }
        result.addIcon(icon);

        /* Monument Icon (Unavailable to Barbarians) */
        if (isClickable) {
            if (infoKingdom.hasMonumentTemplate()) {
                icon = new TemplateIcon(infoKingdom.getMonumentTemplate(), SLOT_MONUMENT, true);
                icon.addNameValue(MessagePath.LABEL_LOOT_TYPE.getMessage(), getKonquest().getLootManager().getMonumentLootDisplayName(infoKingdom.getMonumentTemplate()));
                icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
                icon.setState(MenuState.MONUMENT_INFO);
            } else {
                icon = new InfoIcon(MessagePath.LABEL_MONUMENT_TEMPLATE.getMessage(), Material.BARRIER, SLOT_MONUMENT, false);
                icon.addAlert(MessagePath.LABEL_INVALID.getMessage());
            }
        } else {
            icon = new InfoIcon(MessagePath.LABEL_MONUMENT_TEMPLATE.getMessage(), Material.CRAFTING_TABLE, SLOT_MONUMENT, false);
            icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
        }
        result.addIcon(icon);

        /* Score Icon (Unavailable to Barbarians and peaceful kingdoms) */
        boolean isScoreClickable = infoKingdom.isCreated() && !infoKingdom.isPeaceful();
        icon = new InfoIcon(MessagePath.MENU_SCORE_KINGDOM_SCORE.getMessage(), CommandType.SCORE.iconMaterial(), SLOT_SCORE, isScoreClickable);
        if (isScoreClickable) {
            int kingdomScore = getKonquest().getKingdomManager().getKingdomScore(infoKingdom);
            icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), kingdomScore);
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
            icon.setState(MenuState.DISPLAY_KINGDOM_SCORE);
        } else {
            icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
            if (infoKingdom.isCreated()) {
                icon.addDescription(MessagePath.COMMAND_SCORE_ERROR_PEACEFUL.getMessage(infoKingdom.getName()));
            } else {
                icon.addDescription(MessagePath.COMMAND_SCORE_ERROR_KINGDOM.getMessage());
            }
        }
        result.addIcon(icon);

        /* Officers Icon */
        int numOfficers = infoKingdom.getPlayerOfficersOnly().size();
        icon = new InfoIcon(MessagePath.LABEL_OFFICERS.getMessage(), Material.IRON_HORSE_ARMOR, SLOT_OFFICERS, isClickable);
        if (isClickable) {
            icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), numOfficers);
            icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
            icon.setState(MenuState.KINGDOM_INFO_OFFICERS);
        } else {
            icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
        }
        result.addIcon(icon);

        /* Members Icon */
        int numMembers = infoKingdom.getPlayerMembersOnly().size();
        icon = new InfoIcon(MessagePath.LABEL_MEMBERS.getMessage(), Material.LEATHER_CHESTPLATE, SLOT_MEMBERS, true);
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), numMembers);
        icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
        icon.setState(MenuState.KINGDOM_INFO_MEMBERS);
        result.addIcon(icon);

        /* Towns Icon (Unavailable to Barbarians) */
        int numTowns = infoKingdom.getNumTowns();
        icon = new InfoIcon(MessagePath.LABEL_TOWNS.getMessage(), CommandType.TOWN.iconMaterial(), SLOT_TOWNS, isClickable);
        if (isClickable) {
            icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), numTowns);
            icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
            icon.setState(MenuState.KINGDOM_INFO_TOWNS);
        } else {
            icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
        }
        result.addIcon(icon);

        /* Enemies Icon (Unavailable to Barbarians) */
        int numEnemies = infoKingdom.getActiveRelationKingdoms(KonquestDiplomacyType.WAR).size();
        icon = new InfoIcon(MessagePath.DIPLOMACY_WAR.getMessage(), Material.GUNPOWDER, SLOT_ENEMIES, isClickable);
        if (isClickable) {
            icon.addNameValue(MessagePath.LABEL_KINGDOMS.getMessage(), numEnemies);
            icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
            icon.setState(MenuState.KINGDOM_INFO_ENEMIES);
        } else {
            icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
        }
        result.addIcon(icon);

        /* Allies Icon (Unavailable to Barbarians) */
        int numAllies = infoKingdom.getActiveRelationKingdoms(KonquestDiplomacyType.ALLIANCE).size();
        icon = new InfoIcon(MessagePath.DIPLOMACY_ALLIANCE.getMessage(), Material.DIAMOND, SLOT_ALLIES, isClickable);
        if (isClickable) {
            icon.addNameValue(MessagePath.LABEL_KINGDOMS.getMessage(), numAllies);
            icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
            icon.setState(MenuState.KINGDOM_INFO_ALLIES);
        } else {
            icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
        }
        result.addIcon(icon);

        /* Traders Icon (Unavailable to Barbarians) */
        int numTraders = infoKingdom.getActiveRelationKingdoms(KonquestDiplomacyType.TRADE).size();
        icon = new InfoIcon(MessagePath.DIPLOMACY_TRADE.getMessage(), Material.EMERALD, SLOT_TRADERS, isClickable);
        if (isClickable) {
            icon.addNameValue(MessagePath.LABEL_KINGDOMS.getMessage(), numTraders);
            icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
            icon.setState(MenuState.KINGDOM_INFO_TRADERS);
        } else {
            icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
        }
        result.addIcon(icon);

        /* Navigation */
        addNavEmpty(result);
        addNavClose(result);
        addNavReturn(result);

        return result;
    }

    /**
     * Creates the town info view.
     * This is a single page view.
     * The info town could be a capital or town.
     */
    private DisplayView createTownInfoView() {
        if (infoTown == null) return null;
        DisplayView result;
        MenuIcon icon;

        /* Icon slot indexes */
        int rows = 2;
        // Row 0: 0  1  2  3  4  5  6  7  8
        int SLOT_TOWN = 1;
        int SLOT_FLAGS = 2;
        int SLOT_PROPS = 3;
        int SLOT_SPECIAL = 4;
        int SLOT_KINGDOM = 6;
        int SLOT_LORD = 7;
        // Row 1: 9 10 11 12 13 14 15 16 17
        int SLOT_KNIGHTS = 12;
        int SLOT_RESIDENTS = 13;
        int SLOT_OPTIONS = 14;
        int SLOT_UPGRADES = 15;

        boolean isCapital = infoTown.getTerritoryType().equals(KonquestTerritoryType.CAPITAL);
        String contextColor = getColor(player,infoTown);
        KonquestRelationshipType relation = getRelation(player,infoTown);
        String title;
        if (isCapital) {
            title = infoTown.getName();
        } else {
            title = MessagePath.TERRITORY_TOWN.getMessage()+" "+infoTown.getName();
        }
        result = new DisplayView(rows, title);

        /* Town Icon */
        String lootTableName = getKonquest().getLootManager().getMonumentLootDisplayName(infoTown);
        int numTownPlayers = infoTown.getNumResidentsOnline();
        int maxCriticalHits = getKonquest().getCore().getInt(CorePath.MONUMENTS_DESTROY_AMOUNT.getPath());
        int remainingCriticalHits = maxCriticalHits - infoTown.getMonument().getCriticalHits();
        String townHealth = remainingCriticalHits+"/"+maxCriticalHits;
        String shieldTime = HelperUtil.getTimeFormat(infoTown.getRemainingShieldTimeSeconds(),"");
        int armorBlocks = infoTown.getArmorBlocks();
        icon = new TownIcon(infoTown, contextColor, relation, SLOT_TOWN, false);
        icon.addNameValue(MessagePath.LABEL_LOOT_TYPE.getMessage(), lootTableName);
        icon.addNameValue(MessagePath.LABEL_ONLINE_PLAYERS.getMessage(), numTownPlayers);
        icon.addNameValue(MessagePath.LABEL_HEALTH.getMessage(), townHealth);
        icon.addNameValue(MessagePath.LABEL_SHIELD.getMessage(), shieldTime);
        icon.addNameValue(MessagePath.LABEL_ARMOR.getMessage(), armorBlocks);
        result.addIcon(icon);

        /* Flags Icon */
        icon = new InfoIcon(MessagePath.LABEL_FLAGS.getMessage(), flagMaterial, SLOT_FLAGS, false);
        for(KonPropertyFlag flag : KonPropertyFlag.values()) {
            if(infoTown.hasPropertyValue(flag)) {
                String flagDisplaySymbol = DisplayManager.boolean2Symbol(infoTown.getPropertyValue(flag));
                icon.addNameValue(flag.getName(), flagDisplaySymbol);
            }
        }
        result.addIcon(icon);

        /* Properties Icon */
        icon = new InfoIcon(MessagePath.LABEL_PROPERTIES.getMessage(), propMaterial, SLOT_PROPS, false);
        if (isCapital) {
            icon.addNameValue(MessagePath.LABEL_IMMUNITY.getMessage(), DisplayManager.boolean2Symbol(infoTown.getKingdom().isCapitalImmune()));
        }
        icon.addNameValue(MessagePath.RELATIONSHIP_ROLE_LORD.getMessage(), DisplayManager.boolean2Symbol(infoTown.isLordValid()));
        icon.addNameValue(MessagePath.PROTECTION_NOTICE_ATTACKED.getMessage(), DisplayManager.boolean2Symbol(infoTown.isAttacked()));
        icon.addNameValue(MessagePath.LABEL_PROTECTED.getMessage(), DisplayManager.boolean2Symbol((infoTown.isCaptureDisabled() || infoTown.getKingdom().isOfflineProtected() || infoTown.isTownWatchProtected())));
        icon.addNameValue(MessagePath.LABEL_SHIELD.getMessage(), DisplayManager.boolean2Symbol(infoTown.isShielded()));
        icon.addNameValue(MessagePath.LABEL_ARMOR.getMessage(), DisplayManager.boolean2Symbol(infoTown.isArmored()));
        icon.addNameValue(MessagePath.LABEL_PEACEFUL.getMessage(), DisplayManager.boolean2Symbol(infoTown.getKingdom().isPeaceful()));
        result.addIcon(icon);

        /* Specialization Icon */
        if (getKonquest().getKingdomManager().getIsDiscountEnable()) {
            icon = new ProfessionIcon(infoTown.getSpecialization(), SLOT_SPECIAL, false);
            icon.addProperty(MessagePath.LABEL_SPECIALIZATION.getMessage());
            icon.addDescription(MessagePath.MENU_TOWN_INFO_SPECIAL.getMessage());
            result.addIcon(icon);
        }

        /* Kingdom Icon */
        icon = new KingdomIcon(infoTown.getKingdom(), contextColor, relation, SLOT_KINGDOM, true);
        icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        icon.setState(MenuState.KINGDOM_INFO);
        result.addIcon(icon);

        /* Lord Icon */
        if(infoTown.isLordValid()) {
            icon = new PlayerIcon(infoTown.getPlayerLord(), contextColor, relation, SLOT_LORD, true);
            icon.addProperty(MessagePath.RELATIONSHIP_ROLE_LORD.getMessage());
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
            icon.setState(MenuState.PLAYER_INFO);
        } else {
            icon = new InfoIcon(MessagePath.RELATIONSHIP_ROLE_LORD.getMessage(), Material.IRON_HELMET, SLOT_LORD, false);
            icon.addAlert(MessagePath.LABEL_NO_LORD.getMessage());
            if (infoTown.canClaimLordship(player)) {
                icon.addError(MessagePath.COMMAND_TOWN_NOTICE_NO_LORD.getMessage(infoTown.getName(), infoTown.getTravelName()));
            }
        }
        result.addIcon(icon);

        /* Knights Icon */
        int numTownKnights = infoTown.getPlayerKnightsOnly().size();
        icon = new InfoIcon(MessagePath.LABEL_KNIGHTS.getMessage(), Material.IRON_HORSE_ARMOR, SLOT_KNIGHTS, true);
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), numTownKnights);
        icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
        icon.setState(MenuState.TOWN_INFO_KNIGHTS);
        result.addIcon(icon);

        /* Residents Icon */
        int numTownResidents = infoTown.getPlayerResidentsOnly().size();
        icon = new InfoIcon(MessagePath.LABEL_RESIDENTS.getMessage(), Material.LEATHER_CHESTPLATE, SLOT_RESIDENTS, true);
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), numTownResidents);
        icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
        icon.setState(MenuState.TOWN_INFO_RESIDENTS);
        result.addIcon(icon);

        /* Options Icon */
        int numOptions = KonTownOption.values().length;
        icon = new InfoIcon(MessagePath.LABEL_OPTIONS.getMessage(), Material.OAK_SIGN, SLOT_OPTIONS, true);
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), numOptions);
        icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
        icon.setState(MenuState.TOWN_INFO_OPTIONS);
        result.addIcon(icon);

        /* Upgrades Icon */
        int numUpgrades = infoTown.getUpgrades().size();
        icon = new InfoIcon(MessagePath.LABEL_UPGRADES.getMessage(), Material.GOLDEN_APPLE, SLOT_UPGRADES, true);
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), numUpgrades);
        icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
        icon.setState(MenuState.TOWN_INFO_UPGRADES);
        result.addIcon(icon);

        /* Navigation */
        addNavEmpty(result);
        addNavClose(result);
        addNavReturn(result);

        return result;
    }

    /**
     * Creates the sanctuary info view.
     * This is a single page view.
     */
    private DisplayView createSanctuaryInfoView() {
        if (infoSanctuary == null) return null;
        DisplayView result;
        MenuIcon icon;

        /* Icon slot indexes */
        int rows = 1;
        // Row 0: 0  1  2  3  4  5  6  7  8
        int SLOT_SANCTUARY = 3;
        int SLOT_FLAGS = 4;
        int SLOT_MONUMENTS = 6;

        int numTemplates = infoSanctuary.getTemplates().size();
        String title = MessagePath.TERRITORY_SANCTUARY.getMessage()+" "+infoSanctuary.getName();
        result = new DisplayView(rows, title);

        /* Sanctuary Icon */
        icon = new SanctuaryIcon(infoSanctuary, SLOT_SANCTUARY, false);
        icon.addNameValue(MessagePath.LABEL_MONUMENT_TEMPLATES.getMessage(), numTemplates);
        icon.addNameValue(MessagePath.LABEL_LAND.getMessage(), infoSanctuary.getChunkList().size());
        result.addIcon(icon);

        /* Flags Icon */
        icon = new InfoIcon(MessagePath.LABEL_FLAGS.getMessage(), flagMaterial, SLOT_FLAGS, false);
        for(KonPropertyFlag flag : KonPropertyFlag.values()) {
            if(infoSanctuary.hasPropertyValue(flag)) {
                String flagDisplaySymbol = DisplayManager.boolean2Symbol(infoSanctuary.getPropertyValue(flag));
                icon.addNameValue(flag.getName(), flagDisplaySymbol);
            }
        }
        result.addIcon(icon);

        /* Monuments Icon */
        icon = new InfoIcon(MessagePath.LABEL_MONUMENT_TEMPLATES.getMessage(), Material.CRAFTING_TABLE, SLOT_MONUMENTS, true);
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), numTemplates);
        icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
        icon.setState(MenuState.SANCTUARY_INFO_MONUMENTS);
        result.addIcon(icon);

        /* Navigation */
        addNavEmpty(result);
        addNavClose(result);
        addNavReturn(result);

        return result;
    }

    /**
     * Creates the ruin info view.
     * This is a single page view.
     */
    private DisplayView createRuinInfoView() {
        if (infoRuin == null) return null;
        DisplayView result;
        MenuIcon icon;

        /* Icon slot indexes */
        int rows = 1;
        // Row 0: 0  1  2  3  4  5  6  7  8
        int SLOT_RUIN = 3;
        int SLOT_CAPTURE = 4;
        int SLOT_FLAGS = 5;

        String title = MessagePath.TERRITORY_RUIN.getMessage()+" "+infoRuin.getName();
        result = new DisplayView(rows, title);

        /* Ruin Icon */
        icon = new RuinIcon(infoRuin, SLOT_RUIN, false);
        icon.addNameValue(MessagePath.LABEL_CRITICAL_HITS.getMessage(), infoRuin.getMaxCriticalHits());
        icon.addNameValue(MessagePath.LABEL_GOLEM_SPAWNS.getMessage(), infoRuin.getSpawnLocations().size());
        icon.addNameValue(MessagePath.LABEL_LOOT_TYPE.getMessage(), getKonquest().getLootManager().getRuinLootDisplayName(infoRuin));
        result.addIcon(icon);

        /* Capture Icon */
        Material captureMaterial = infoRuin.isCaptureDisabled() ? Material.POPPY : Material.IRON_BLOCK;
        icon = new InfoIcon(MessagePath.MENU_INFO_RUIN_STATUS.getMessage(), captureMaterial, SLOT_CAPTURE, false);
        if(infoRuin.isCaptureDisabled()) {
            // Currently on capture cooldown
            icon.addDescription(MessagePath.PROTECTION_ERROR_CAPTURE.getMessage(infoRuin.getCaptureCooldownString()));
        } else {
            // Can be captured
            icon.addDescription(MessagePath.MENU_INFO_RUIN_CAPTURE.getMessage());
        }
        result.addIcon(icon);

        /* Flags Icon */
        icon = new InfoIcon(MessagePath.LABEL_FLAGS.getMessage(), flagMaterial, SLOT_FLAGS, false);
        for(KonPropertyFlag flag : KonPropertyFlag.values()) {
            if(infoRuin.hasPropertyValue(flag)) {
                String flagDisplaySymbol = DisplayManager.boolean2Symbol(infoRuin.getPropertyValue(flag));
                icon.addNameValue(flag.getName(), flagDisplaySymbol);
            }
        }
        result.addIcon(icon);

        /* Navigation */
        addNavEmpty(result);
        addNavClose(result);
        addNavReturn(result);

        return result;
    }

    /**
     * Creates the camp info view.
     * This is a single page view.
     */
    private DisplayView createCampInfoView() {
        if (infoCamp == null) return null;
        DisplayView result;
        MenuIcon icon;

        /* Icon slot indexes */
        int rows = 1;
        // Row 0: 0  1  2  3  4  5  6  7  8
        int SLOT_CAMP = 3;
        int SLOT_CLAN = 4;
        int SLOT_OWNER = 5;

        String title = MessagePath.TERRITORY_CAMP.getMessage()+" "+infoCamp.getOwner().getName();
        result = new DisplayView(rows, title);

        /* Camp Icon */
        icon = new CampIcon(infoCamp, SLOT_CAMP, false);
        result.addIcon(icon);

        /* Clan Icon */
        icon = new InfoIcon(MessagePath.LABEL_CAMP_CLAN.getMessage(), Material.CHAIN, SLOT_CLAN, false);
        if (getKonquest().getCampManager().isCampGroupsEnabled()) {
            if (getKonquest().getCampManager().isCampGrouped(infoCamp)) {
                KonCampGroup clan = getKonquest().getCampManager().getCampGroup(infoCamp);
                int numClanCamps = clan.getCamps().size();
                icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), numClanCamps);
            } else {
                icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
            }
        } else {
            icon.addAlert(MessagePath.LABEL_DISABLED.getMessage());
        }
        result.addIcon(icon);

        /* Owner Icon */
        icon = new PlayerIcon(infoCamp.getOwner(), getColor(player,infoCamp.getKingdom()), getRelation(player,infoCamp.getKingdom()), SLOT_OWNER, true);
        icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        icon.setState(MenuState.PLAYER_INFO);
        result.addIcon(icon);

        /* Navigation */
        addNavEmpty(result);
        addNavClose(result);
        addNavReturn(result);

        return result;
    }

    /**
     * Creates the monument template info view.
     * This is a single page view.
     */
    private DisplayView createMonumentInfoView() {
        if (infoTemplate == null) return null;
        DisplayView result;
        MenuIcon icon;

        /* Icon slot indexes */
        int rows = 1;
        // Row 0: 0  1  2  3  4  5  6  7  8
        int SLOT_TEMPLATE = 2;
        int SLOT_SANCTUARY = 4;
        int SLOT_KINGDOMS = 6;

        String title = MessagePath.LABEL_MONUMENT_TEMPLATE.getMessage()+" "+infoTemplate.getName();
        result = new DisplayView(rows, title);

        /* Monument Template Icon */
        double totalCost = getKonquest().getKingdomManager().getCostTemplate() + infoTemplate.getCost();
        icon = new TemplateIcon(infoTemplate, SLOT_TEMPLATE, false);
        icon.addNameValue(MessagePath.LABEL_COST.getMessage(), KonquestPlugin.getCurrencyFormat(totalCost));
        icon.addNameValue(MessagePath.LABEL_LOOT_TYPE.getMessage(), getKonquest().getLootManager().getMonumentLootDisplayName(infoTemplate));
        result.addIcon(icon);

        /* Sanctuary Icon */
        KonSanctuary templateSanctuary = getKonquest().getSanctuaryManager().getSanctuaryOfTemplate(infoTemplate);
        if (templateSanctuary != null) {
            icon = new SanctuaryIcon(templateSanctuary, SLOT_SANCTUARY, true);
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
            icon.setState(MenuState.SANCTUARY_INFO);
            result.addIcon(icon);
        }

        /* Kingdoms Icon */
        int kingdomCount = 0;
        for(KonKingdom kingdom : getKonquest().getKingdomManager().getKingdoms()) {
            if(kingdom.getMonumentTemplate() != null && kingdom.getMonumentTemplate().equals(infoTemplate)) {
                kingdomCount++;
            }
        }
        icon = new InfoIcon(MessagePath.LABEL_KINGDOMS.getMessage(), Material.DIAMOND_HELMET, SLOT_KINGDOMS, true);
        icon.addDescription(MessagePath.MENU_INFO_TEMPLATE_KINGDOMS.getMessage());
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), kingdomCount);
        icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
        icon.setState(MenuState.MONUMENT_INFO_KINGDOMS);
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
     *
     * @param context The menu state for the corresponding view
     * @return The list of menu views to be displayed to the player
     */
    @Override
    public ArrayList<DisplayView> createView(State context) {
        ArrayList<DisplayView> result = new ArrayList<>();
        MenuState currentState = (MenuState)context;
        switch (currentState) {
            case ROOT:
                result.add(createRootView());
                break;
            case PLAYER_LIST:
            case KINGDOM_INFO_OFFICERS:
            case KINGDOM_INFO_MEMBERS:
            case TOWN_INFO_KNIGHTS:
            case TOWN_INFO_RESIDENTS:
                result.addAll(createPlayerView(currentState));
                break;
            case KINGDOM_LIST:
            case KINGDOM_INFO_ENEMIES:
            case KINGDOM_INFO_ALLIES:
            case KINGDOM_INFO_TRADERS:
            case MONUMENT_INFO_KINGDOMS:
                result.addAll(createKingdomView(currentState));
                break;
            case CAPITAL_LIST:
            case TOWN_LIST:
            case PLAYER_INFO_TOWNS:
            case KINGDOM_INFO_TOWNS:
                result.addAll(createTownView(currentState));
                break;
            case CAMP_LIST:
                result.addAll(createCampView());
                break;
            case RUIN_LIST:
                result.addAll(createRuinView());
                break;
            case SANCTUARY_LIST:
                result.addAll(createSanctuaryView());
                break;
            case MONUMENT_LIST:
            case SANCTUARY_INFO_MONUMENTS:
                result.addAll(createMonumentView(currentState));
                break;
            case PLAYER_INFO_STATS:
                result.addAll(createStatsView());
                break;
            case PLAYER_INFO:
                result.add(createPlayerInfoView());
                break;
            case KINGDOM_INFO:
                result.add(createKingdomInfoView());
                break;
            case TOWN_INFO:
                result.add(createTownInfoView());
                break;
            case TOWN_INFO_OPTIONS:
                result.addAll(createOptionsView());
                break;
            case TOWN_INFO_UPGRADES:
                result.addAll(createUpgradesView());
                break;
            case SANCTUARY_INFO:
                result.add(createSanctuaryInfoView());
                break;
            case RUIN_INFO:
                result.add(createRuinInfoView());
                break;
            case CAMP_INFO:
                result.add(createCampInfoView());
                break;
            case MONUMENT_INFO:
                result.add(createMonumentInfoView());
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
            }  else if (isNavReturn(slot)) {
                // Return to previous view
                MenuState returnState = popReturn(currentState);
                if (returnState == null) {
                    result = setCurrentView(MenuState.ROOT);
                } else {
                    result = setCurrentView(returnState);
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
            DisplayView view = getCurrentView();
            if (view == null) return null;
            MenuIcon clickedIcon = view.getIcon(slot);
            MenuState nextState = (MenuState)clickedIcon.getState(); // could be null in some states
            if (nextState == null) return null;
            switch (currentState) {
                case ROOT:
                    clearReturn();
                    switch (nextState) {
                        case PLAYER_LIST:
                        case KINGDOM_LIST:
                        case CAPITAL_LIST:
                        case TOWN_LIST:
                        case CAMP_LIST:
                        case RUIN_LIST:
                        case SANCTUARY_LIST:
                        case MONUMENT_LIST:
                            // Go to next state as defined by icon
                            result = setCurrentView(nextState);
                            break;
                    }
                    break;
                case PLAYER_LIST:
                case KINGDOM_INFO_OFFICERS:
                case KINGDOM_INFO_MEMBERS:
                case TOWN_INFO_KNIGHTS:
                case TOWN_INFO_RESIDENTS:
                case CAMP_INFO:
                    if(nextState.equals(MenuState.PLAYER_INFO) && clickedIcon instanceof PlayerIcon) {
                        infoPlayer = getKonquest().getPlayerManager().getOfflinePlayer(((PlayerIcon)clickedIcon).getOfflinePlayer());
                        result = refreshNewView(nextState);
                    }
                    break;
                case KINGDOM_LIST:
                case KINGDOM_INFO_ENEMIES:
                case KINGDOM_INFO_ALLIES:
                case KINGDOM_INFO_TRADERS:
                case MONUMENT_INFO_KINGDOMS:
                    if(nextState.equals(MenuState.KINGDOM_INFO) && clickedIcon instanceof KingdomIcon) {
                        infoKingdom = ((KingdomIcon)clickedIcon).getKingdom();
                        result = refreshNewView(nextState);
                    }
                    break;
                case CAPITAL_LIST:
                case TOWN_LIST:
                case PLAYER_INFO_TOWNS:
                case KINGDOM_INFO_TOWNS:
                    if(nextState.equals(MenuState.TOWN_INFO) && clickedIcon instanceof TownIcon) {
                        infoTown = ((TownIcon)clickedIcon).getTown();
                        result = refreshNewView(nextState);
                    }
                    break;
                case CAMP_LIST:
                    if(nextState.equals(MenuState.CAMP_INFO) && clickedIcon instanceof CampIcon) {
                        infoCamp = ((CampIcon)clickedIcon).getCamp();
                        result = refreshNewView(nextState);
                    }
                    break;
                case RUIN_LIST:
                    if(nextState.equals(MenuState.RUIN_INFO) && clickedIcon instanceof RuinIcon) {
                        infoRuin = ((RuinIcon)clickedIcon).getRuin();
                        result = refreshNewView(nextState);
                    }
                    break;
                case SANCTUARY_LIST:
                    if(nextState.equals(MenuState.SANCTUARY_INFO) && clickedIcon instanceof SanctuaryIcon) {
                        infoSanctuary = ((SanctuaryIcon)clickedIcon).getSanctuary();
                        result = refreshNewView(nextState);
                    }
                    break;
                case MONUMENT_LIST:
                case SANCTUARY_INFO_MONUMENTS:
                    if(nextState.equals(MenuState.MONUMENT_INFO) && clickedIcon instanceof TemplateIcon) {
                        infoTemplate = ((TemplateIcon)clickedIcon).getTemplate();
                        result = refreshNewView(nextState);
                    }
                    break;
                case PLAYER_INFO:
                    switch (nextState) {
                        case CAMP_INFO:
                            if(clickedIcon instanceof CampIcon) {
                                infoCamp = ((CampIcon)clickedIcon).getCamp();
                                result = refreshNewView(nextState);
                            }
                            break;
                        case KINGDOM_INFO:
                            if(clickedIcon instanceof KingdomIcon) {
                                infoKingdom = ((KingdomIcon)clickedIcon).getKingdom();
                                result = refreshNewView(nextState);
                            }
                            break;
                        case DISPLAY_PLAYER_SCORE:
                            getKonquest().getDisplayManager().displayScorePlayerMenu(player, infoPlayer);
                            break;
                        case PLAYER_INFO_TOWNS:
                        case PLAYER_INFO_STATS:
                            result = refreshNewView(nextState);
                            break;
                    }
                    break;
                case KINGDOM_INFO:
                    switch (nextState) {
                        case TOWN_INFO:
                            if(clickedIcon instanceof TownIcon) {
                                infoTown = ((TownIcon)clickedIcon).getTown();
                                result = refreshNewView(nextState);
                            }
                            break;
                        case PLAYER_INFO:
                            if(clickedIcon instanceof PlayerIcon) {
                                infoPlayer = getKonquest().getPlayerManager().getOfflinePlayer(((PlayerIcon)clickedIcon).getOfflinePlayer());
                                result = refreshNewView(nextState);
                            }
                            break;
                        case MONUMENT_INFO:
                            if(clickedIcon instanceof TemplateIcon) {
                                infoTemplate = ((TemplateIcon)clickedIcon).getTemplate();
                                result = refreshNewView(nextState);
                            }
                            break;
                        case DISPLAY_KINGDOM_SCORE:
                            getKonquest().getDisplayManager().displayScoreKingdomMenu(player, infoKingdom);
                            break;
                        case KINGDOM_INFO_OFFICERS:
                        case KINGDOM_INFO_MEMBERS:
                        case KINGDOM_INFO_TOWNS:
                        case KINGDOM_INFO_ENEMIES:
                        case KINGDOM_INFO_ALLIES:
                        case KINGDOM_INFO_TRADERS:
                            result = refreshNewView(nextState);
                            break;
                    }
                    break;
                case TOWN_INFO:
                    switch (nextState) {
                        case KINGDOM_INFO:
                            if(clickedIcon instanceof KingdomIcon) {
                                infoKingdom = ((KingdomIcon)clickedIcon).getKingdom();
                                result = refreshNewView(nextState);
                            }
                            break;
                        case PLAYER_INFO:
                            if(clickedIcon instanceof PlayerIcon) {
                                infoPlayer = getKonquest().getPlayerManager().getOfflinePlayer(((PlayerIcon)clickedIcon).getOfflinePlayer());
                                result = refreshNewView(nextState);
                            }
                            break;
                        case TOWN_INFO_KNIGHTS:
                        case TOWN_INFO_RESIDENTS:
                        case TOWN_INFO_OPTIONS:
                        case TOWN_INFO_UPGRADES:
                            result = refreshNewView(nextState);
                            break;
                    }
                    break;
                case SANCTUARY_INFO:
                    if (nextState.equals(MenuState.SANCTUARY_INFO_MONUMENTS)) {
                        result = refreshNewView(nextState);
                    }
                    break;
                case MONUMENT_INFO:
                    switch (nextState) {
                        case SANCTUARY_INFO:
                            if(clickedIcon instanceof SanctuaryIcon) {
                                infoSanctuary = ((SanctuaryIcon)clickedIcon).getSanctuary();
                                result = refreshNewView(nextState);
                            }
                            break;
                        case MONUMENT_INFO_KINGDOMS:
                            result = refreshNewView(nextState);
                            break;
                    }
                    break;
                default:
                    break;
            }
            pushReturn(currentState, nextState);
        }
        return result;
    }

    private String getTitle(MenuState context) {
        String result = "error";
        switch (context) {
            case ROOT:
                result = MessagePath.MENU_MAIN_INFO.getMessage();
                break;
            case PLAYER_LIST:
                result = MessagePath.LABEL_PLAYERS.getMessage();
                break;
            case KINGDOM_LIST:
            case MONUMENT_INFO_KINGDOMS:
                result = MessagePath.LABEL_KINGDOMS.getMessage();
                break;
            case CAPITAL_LIST:
                result = MessagePath.LABEL_CAPITALS.getMessage();
                break;
            case PLAYER_INFO_TOWNS:
            case KINGDOM_INFO_TOWNS:
            case TOWN_LIST:
                result = MessagePath.LABEL_TOWNS.getMessage();
                break;
            case CAMP_LIST:
                result = MessagePath.LABEL_CAMPS.getMessage();
                break;
            case RUIN_LIST:
                result = MessagePath.LABEL_RUINS.getMessage();
                break;
            case SANCTUARY_LIST:
                result = MessagePath.LABEL_SANCTUARIES.getMessage();
                break;
            case SANCTUARY_INFO_MONUMENTS:
            case MONUMENT_LIST:
                result = MessagePath.LABEL_MONUMENT_TEMPLATES.getMessage();
                break;
            case PLAYER_INFO_STATS:
                result = MessagePath.MENU_PREFIX_TITLE_STATS.getMessage();
                break;
            case KINGDOM_INFO_OFFICERS:
                result = MessagePath.LABEL_OFFICERS.getMessage();
                break;
            case KINGDOM_INFO_MEMBERS:
                result = MessagePath.LABEL_MEMBERS.getMessage();
                break;
            case KINGDOM_INFO_ENEMIES:
                result = MessagePath.DIPLOMACY_WAR.getMessage();
                break;
            case KINGDOM_INFO_ALLIES:
                result = MessagePath.DIPLOMACY_ALLIANCE.getMessage();
                break;
            case KINGDOM_INFO_TRADERS:
                result = MessagePath.DIPLOMACY_TRADE.getMessage();
                break;
            case TOWN_INFO_KNIGHTS:
                result = MessagePath.LABEL_KNIGHTS.getMessage();
                break;
            case TOWN_INFO_RESIDENTS:
                result = MessagePath.LABEL_RESIDENTS.getMessage();
                break;
            case TOWN_INFO_OPTIONS:
                result = MessagePath.LABEL_OPTIONS.getMessage();
                break;
            case TOWN_INFO_UPGRADES:
                result = MessagePath.LABEL_UPGRADES.getMessage();
                break;
            default:
                break;
        }
        return result;
    }

    private void pushReturn(MenuState currentState, MenuState nextState) {
        if (!returnStack.containsKey(nextState)) {
            returnStack.put(nextState,currentState);
        }
    }

    private MenuState popReturn(MenuState currentState) {
        return returnStack.get(currentState);
    }

    private void clearReturn() {
        returnStack.clear();
    }

}
