package com.github.rumsfield.konquest.display.menu;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.display.DisplayMenu;
import com.github.rumsfield.konquest.display.StateMenu;
import com.github.rumsfield.konquest.display.icon.*;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class ScoreMenu extends StateMenu {

    enum MenuState implements State {
        ROOT,
        PLAYER_SELF,
        PLAYER_ALL,
        PLAYER_SCORE,
        KINGDOM_SELF,
        KINGDOM_ALL,
        KINGDOM_SCORE
    }

    private final KonPlayer player; // The player viewing the menu, and used for self scores
    private KonOfflinePlayer scorePlayer; // The player to use for score details
    private KonKingdom scoreKingdom; // The kingdom to use for score details
    private MenuState returnState;

    public ScoreMenu(Konquest konquest, KonPlayer player) {
        super(konquest, MenuState.ROOT, null);
        this.player = player;
        this.scorePlayer = null;
        this.scoreKingdom = null;
        this.returnState = MenuState.ROOT;

        /* Initialize menu view */
        setCurrentView(MenuState.ROOT);

    }

    public DisplayMenu goToPlayerScore(KonOfflinePlayer scorePlayer) {
        this.scorePlayer = scorePlayer;
        // Change to player score view
        return refreshNewView(MenuState.PLAYER_SCORE);
    }

    public DisplayMenu goToKingdomScore(KonKingdom scoreKingdom) {
        this.scoreKingdom = scoreKingdom;
        // Change to kingdom score view
        return refreshNewView(MenuState.KINGDOM_SCORE);
    }

    /**
     * Creates the root menu view for this menu.
     * This is a single page view.
     */
    private DisplayMenu createRootView() {
        DisplayMenu result;
        MenuIcon icon;

        /* Icon slot indexes */
        // Row 0: 0  1  2  3  4  5  6  7  8
        int ROOT_SLOT_PLAYER = 3;
        int ROOT_SLOT_KINGDOM = 5;
        // Row 1: 9 10 11 12 13 14 15 16 17
        int ROOT_SLOT_ALL_PLAYERS = 12;
        int ROOT_SLOT_ALL_KINGDOMS = 14;

        result = new DisplayMenu(2, getTitle(MenuState.ROOT));

        int playerScore = getKonquest().getKingdomManager().getPlayerScore(player);
        int kingdomScore = getKonquest().getKingdomManager().getKingdomScore(player.getKingdom());
        String playerName = player.getBukkitPlayer().getName();
        String kingdomName = player.getKingdom().getName();

        /* Player Score Icon */
        boolean isPlayerClickable = isPlayerScored(player);
        icon = new InfoIcon(MessagePath.MENU_SCORE_PLAYER_SCORE.getMessage(), Material.PLAYER_HEAD, ROOT_SLOT_PLAYER, isPlayerClickable);
        icon.addDescription(playerName);
        icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), playerScore);
        if (isPlayerClickable) {
            icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
            if (player.isBarbarian()) {
                icon.addDescription(MessagePath.COMMAND_SCORE_ERROR_BARBARIAN.getMessage(playerName));
            } else {
                icon.addDescription(MessagePath.COMMAND_SCORE_ERROR_PEACEFUL.getMessage(kingdomName));
            }
        }
        icon.setState(MenuState.PLAYER_SELF);
        result.addIcon(icon);

        /* Kingdom Score Icon */
        boolean isKingdomClickable = isKingdomScored(player.getKingdom());
        icon = new InfoIcon(MessagePath.MENU_SCORE_KINGDOM_SCORE.getMessage(), Material.GOLDEN_HELMET, ROOT_SLOT_KINGDOM, isKingdomClickable);
        icon.addDescription(kingdomName);
        icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), kingdomScore);
        if (isKingdomClickable) {
            icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
        } else {
            icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
            if (player.isBarbarian()) {
                icon.addDescription(MessagePath.COMMAND_SCORE_ERROR_BARBARIAN.getMessage(playerName));
            } else {
                icon.addDescription(MessagePath.COMMAND_SCORE_ERROR_PEACEFUL.getMessage(kingdomName));
            }
        }
        icon.setState(MenuState.KINGDOM_SELF);
        result.addIcon(icon);

        /* All Players Icon */
        icon = new InfoIcon(MessagePath.LABEL_PLAYERS.getMessage(), Material.EMERALD, ROOT_SLOT_ALL_PLAYERS, true);
        icon.addDescription(MessagePath.MENU_SCORE_DESCRIPTION_PLAYERS.getMessage());
        icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
        icon.setState(MenuState.PLAYER_ALL);
        result.addIcon(icon);

        /* All Kingdoms Icon */
        icon = new InfoIcon(MessagePath.LABEL_KINGDOMS.getMessage(), Material.DIAMOND, ROOT_SLOT_ALL_KINGDOMS, true);
        icon.addDescription(MessagePath.MENU_SCORE_DESCRIPTION_KINGDOMS.getMessage());
        icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
        icon.setState(MenuState.KINGDOM_ALL);
        result.addIcon(icon);

        /* Navigation */
        addNavEmpty(result);
        addNavHome(result);
        addNavClose(result);

        return result;
    }

    /**
     * Creates the all-player view.
     * This can be a multiple paged view.
     */
    private List<DisplayMenu> createPlayerView() {
        ArrayList<MenuIcon> icons = new ArrayList<>();
        List<KonOfflinePlayer> players = new ArrayList<>(getKonquest().getPlayerManager().getAllKonquestOfflinePlayers());
        // Sort by score
        players.sort(playerScoreComparator);

        /* Player Icons */
        int rank = 0;
        MenuIcon icon;
        String currentPlayerName;
        String currentKingdomName;
        int currentScore;
        String contextColor;
        boolean isPlayerClickable;
        for (KonOfflinePlayer currentPlayer : players) {
            currentPlayerName = currentPlayer.getOfflineBukkitPlayer().getName();
            currentKingdomName = currentPlayer.getKingdom().getName();
            currentScore = getKonquest().getKingdomManager().getPlayerScore(currentPlayer);
            contextColor = getKonquest().getDisplaySecondaryColor(player,currentPlayer);
            isPlayerClickable = isPlayerScored(currentPlayer);
            icon = new PlayerIcon(currentPlayer.getOfflineBukkitPlayer(),contextColor,0,isPlayerClickable);
            icon.addProperty("#"+rank);
            icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), currentScore);
            icon.addNameValue(MessagePath.LABEL_KINGDOM.getMessage(), currentKingdomName);
            if (isPlayerClickable) {
                icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
            } else {
                icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
                if (currentPlayer.isBarbarian()) {
                    icon.addDescription(MessagePath.COMMAND_SCORE_ERROR_BARBARIAN.getMessage(currentPlayerName));
                } else {
                    icon.addDescription(MessagePath.COMMAND_SCORE_ERROR_PEACEFUL.getMessage(currentKingdomName));
                }
            }
            icon.setState(MenuState.PLAYER_SCORE);
            icons.add(icon);
            rank++;
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(MenuState.PLAYER_ALL)));
    }

    /**
     * Creates the all-kingdom view.
     * This can be a multiple paged view.
     */
    private List<DisplayMenu> createKingdomView() {
        ArrayList<MenuIcon> icons = new ArrayList<>();
        List<KonKingdom> kingdoms = new ArrayList<>(getKonquest().getKingdomManager().getKingdoms());
        // Sort by score
        kingdoms.sort(kingdomScoreComparator);

        /* Kingdom Icons */
        int rank = 0;
        MenuIcon icon;
        String currentKingdomName;
        int currentScore;
        String contextColor;
        boolean isKingdomClickable;
        boolean isViewer;
        for (KonKingdom currentKingdom : kingdoms) {
            currentKingdomName = currentKingdom.getName();
            currentScore = getKonquest().getKingdomManager().getKingdomScore(currentKingdom);
            contextColor = getKonquest().getDisplaySecondaryColor(player.getKingdom(),currentKingdom);
            isKingdomClickable = isKingdomScored(currentKingdom);
            isViewer = currentKingdom.equals(player.getKingdom());
            icon = new KingdomIcon(currentKingdom,contextColor,0,isKingdomClickable,isViewer);
            icon.addProperty("#"+rank);
            icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), currentScore);
            if (isKingdomClickable) {
                icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
            } else {
                icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
                if (currentKingdom.isCreated()) {
                    icon.addDescription(MessagePath.COMMAND_SCORE_ERROR_PEACEFUL.getMessage(currentKingdomName));
                }
            }
            icon.setState(MenuState.KINGDOM_SCORE);
            icons.add(icon);
            rank++;
        }

        /* Make Pages (includes navigation) */
        return new ArrayList<>(makePages(icons, getTitle(MenuState.KINGDOM_ALL)));
    }

    /**
     * Creates the player score view for this menu.
     * This is a single page view.
     */
    private DisplayMenu createPlayerScoreView() {
        if (scorePlayer == null) return null;
        DisplayMenu result;
        MenuIcon icon;

        /* Icon slot indexes */
        // Row 0: 0  1  2  3  4  5  6  7  8
        int SLOT_TOTAL = 0;
        int SLOT_TOWN_LORD = 2;
        int SLOT_TOWN_KNIGHT = 3;
        int SLOT_TOWN_RESIDENT = 4;
        int SLOT_LAND_LORD = 5;
        int SLOT_LAND_KNIGHT = 6;
        int SLOT_LAND_RESIDENT = 7;

        result = new DisplayMenu(1, getTitle(MenuState.PLAYER_SCORE));

        KonPlayerScoreAttributes playerScoreAttributes = getKonquest().getKingdomManager().getPlayerScoreAttributes(scorePlayer);

        int playerScore = playerScoreAttributes.getScore();
        String playerName = scorePlayer.getOfflineBukkitPlayer().getName();

        /* Player Score Icon */
        icon = new InfoIcon(MessagePath.MENU_SCORE_PLAYER_SCORE.getMessage(), Material.PLAYER_HEAD, SLOT_TOTAL, false);
        icon.addDescription(playerName);
        icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), playerScore);
        result.addIcon(icon);

        /* Score Attribute Icons */
        int attValue;
        int attScore;
        // Town Lords
        attValue = playerScoreAttributes.getAttributeValue(KonPlayerScoreAttributes.ScoreAttribute.TOWN_LORDS);
        attScore = playerScoreAttributes.getAttributeScore(KonPlayerScoreAttributes.ScoreAttribute.TOWN_LORDS);
        icon = new InfoIcon(MessagePath.MENU_SCORE_TOWN_1.getMessage(), Material.GOLD_INGOT, SLOT_TOWN_LORD, false);
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), attValue);
        icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), attScore);
        result.addIcon(icon);
        // Town Knights
        attValue = playerScoreAttributes.getAttributeValue(KonPlayerScoreAttributes.ScoreAttribute.TOWN_KNIGHTS);
        attScore = playerScoreAttributes.getAttributeScore(KonPlayerScoreAttributes.ScoreAttribute.TOWN_KNIGHTS);
        icon = new InfoIcon(MessagePath.MENU_SCORE_TOWN_2.getMessage(), Material.IRON_INGOT, SLOT_TOWN_KNIGHT, false);
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), attValue);
        icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), attScore);
        result.addIcon(icon);
        // Town Residents
        attValue = playerScoreAttributes.getAttributeValue(KonPlayerScoreAttributes.ScoreAttribute.TOWN_RESIDENTS);
        attScore = playerScoreAttributes.getAttributeScore(KonPlayerScoreAttributes.ScoreAttribute.TOWN_RESIDENTS);
        icon = new InfoIcon(MessagePath.MENU_SCORE_TOWN_3.getMessage(), Material.OAK_PLANKS, SLOT_TOWN_RESIDENT, false);
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), attValue);
        icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), attScore);
        result.addIcon(icon);
        // Lord Land
        attValue = playerScoreAttributes.getAttributeValue(KonPlayerScoreAttributes.ScoreAttribute.LAND_LORDS);
        attScore = playerScoreAttributes.getAttributeScore(KonPlayerScoreAttributes.ScoreAttribute.LAND_LORDS);
        icon = new InfoIcon(MessagePath.MENU_SCORE_LAND_1.getMessage(), Material.STONE, SLOT_LAND_LORD, false);
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), attValue);
        icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), attScore);
        result.addIcon(icon);
        // Knight Land
        attValue = playerScoreAttributes.getAttributeValue(KonPlayerScoreAttributes.ScoreAttribute.LAND_KNIGHTS);
        attScore = playerScoreAttributes.getAttributeScore(KonPlayerScoreAttributes.ScoreAttribute.LAND_KNIGHTS);
        icon = new InfoIcon(MessagePath.MENU_SCORE_LAND_2.getMessage(), Material.GRASS_BLOCK, SLOT_LAND_KNIGHT, false);
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), attValue);
        icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), attScore);
        result.addIcon(icon);
        // Resident Land
        attValue = playerScoreAttributes.getAttributeValue(KonPlayerScoreAttributes.ScoreAttribute.LAND_RESIDENTS);
        attScore = playerScoreAttributes.getAttributeScore(KonPlayerScoreAttributes.ScoreAttribute.LAND_RESIDENTS);
        icon = new InfoIcon(MessagePath.MENU_SCORE_LAND_3.getMessage(), Material.DIRT, SLOT_LAND_RESIDENT, false);
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), attValue);
        icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), attScore);
        result.addIcon(icon);

        /* Navigation */
        addNavEmpty(result);
        addNavClose(result);

        return result;
    }

    /**
     * Creates the kingdom score view for this menu.
     * This is a single page view.
     */
    private DisplayMenu createKingdomScoreView() {
        if (scoreKingdom == null) return null;
        DisplayMenu result;
        MenuIcon icon;
        int NUM_LEADERBOARD = 9;

        /* Icon slot indexes */
        // Row 0: 0  1  2  3  4  5  6  7  8
        int SLOT_LEADERBOARD_START = 0;
        // Row 2: 18 19 20 21 22 23 24 25 26
        int SLOT_TOTAL = 18;
        int SLOT_TOWNS = 20;
        int SLOT_LAND = 21;
        int SLOT_FAVOR = 22;
        int SLOT_POPULATION = 23;

        result = new DisplayMenu(3, getTitle(MenuState.KINGDOM_SCORE));

        KonKingdomScoreAttributes kingdomScoreAttributes = getKonquest().getKingdomManager().getKingdomScoreAttributes(scoreKingdom);

        int kingdomScore = kingdomScoreAttributes.getScore();
        String kingdomName = scoreKingdom.getName();

        /* Kingdom Score Icon */
        icon = new InfoIcon(MessagePath.MENU_SCORE_KINGDOM_SCORE.getMessage(), Material.GOLDEN_HELMET, SLOT_TOTAL, false);
        icon.addDescription(kingdomName);
        icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), kingdomScore);
        result.addIcon(icon);

        /* Score Attribute Icons */
        int attValue;
        int attScore;
        // Towns
        attValue = kingdomScoreAttributes.getAttributeValue(KonKingdomScoreAttributes.ScoreAttribute.TOWNS);
        attScore = kingdomScoreAttributes.getAttributeScore(KonKingdomScoreAttributes.ScoreAttribute.TOWNS);
        icon = new InfoIcon(MessagePath.MENU_SCORE_KINGDOM_TOWNS.getMessage(), Material.OBSIDIAN, SLOT_TOWNS, false);
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), attValue);
        icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), attScore);
        result.addIcon(icon);
        // Land
        attValue = kingdomScoreAttributes.getAttributeValue(KonKingdomScoreAttributes.ScoreAttribute.LAND);
        attScore = kingdomScoreAttributes.getAttributeScore(KonKingdomScoreAttributes.ScoreAttribute.LAND);
        icon = new InfoIcon(MessagePath.MENU_SCORE_KINGDOM_LAND.getMessage(), Material.GRASS_BLOCK, SLOT_LAND, false);
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), attValue);
        icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), attScore);
        result.addIcon(icon);
        // Favor
        attValue = kingdomScoreAttributes.getAttributeValue(KonKingdomScoreAttributes.ScoreAttribute.FAVOR);
        attScore = kingdomScoreAttributes.getAttributeScore(KonKingdomScoreAttributes.ScoreAttribute.FAVOR);
        icon = new InfoIcon(MessagePath.MENU_SCORE_KINGDOM_FAVOR.getMessage(), Material.GOLD_INGOT, SLOT_FAVOR, false);
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), attValue);
        icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), attScore);
        result.addIcon(icon);
        // Population
        attValue = kingdomScoreAttributes.getAttributeValue(KonKingdomScoreAttributes.ScoreAttribute.POPULATION);
        attScore = kingdomScoreAttributes.getAttributeScore(KonKingdomScoreAttributes.ScoreAttribute.POPULATION);
        icon = new InfoIcon(MessagePath.MENU_SCORE_KINGDOM_POPULATION.getMessage(), Material.ORANGE_BED, SLOT_POPULATION, false);
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), attValue);
        icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), attScore);
        result.addIcon(icon);

        /* Leaderboard Icons */
        String contextColor = getKonquest().getDisplaySecondaryColor(player.getKingdom(),scoreKingdom);
        KonLeaderboard leaderboard = getKonquest().getKingdomManager().getKingdomLeaderboard(scoreKingdom);
        if (!leaderboard.isEmpty()) {
            int numEntries = NUM_LEADERBOARD;
            if (leaderboard.getSize() < numEntries) {
                numEntries = leaderboard.getSize();
            }
            int rank = 1;
            int index = SLOT_LEADERBOARD_START;
            for (int n = 0; n < numEntries; n++) {
                icon = new PlayerIcon(leaderboard.getOfflinePlayer(n),contextColor,index,true);
                icon.addProperty("#"+rank);
                icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), leaderboard.getScore(n));
                icon.addNameValue(MessagePath.LABEL_KINGDOM.getMessage(), kingdomName);
                icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
                icon.setState(MenuState.PLAYER_SCORE);
                result.addIcon(icon);
                rank++;
                index++;
            }
        }

        /* Navigation */
        addNavEmpty(result);
        addNavClose(result);

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
    public ArrayList<DisplayMenu> createView(State context) {
        ArrayList<DisplayMenu> result = new ArrayList<>();
        switch ((MenuState)context) {
            case ROOT:
                result.add(createRootView());
                break;
            case PLAYER_ALL:
                result.addAll(createPlayerView());
                break;
            case KINGDOM_ALL:
                result.addAll(createKingdomView());
                break;
            case PLAYER_SCORE:
                result.add(createPlayerScoreView());
                break;
            case KINGDOM_SCORE:
                result.add(createKingdomScoreView());
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
                switch (currentState) {
                    case PLAYER_SCORE:
                        this.scorePlayer = null;
                        if (returnState == null) {
                            result = setCurrentView(MenuState.ROOT);
                        } else {
                            result = setCurrentView(returnState);
                        }
                        this.returnState = null;
                        break;
                    case KINGDOM_SCORE:
                        this.scoreKingdom = null;
                        if (returnState == null) {
                            result = setCurrentView(MenuState.ROOT);
                        } else {
                            result = setCurrentView(returnState);
                        }
                        this.returnState = null;
                        break;
                    default:
                        // Return to root
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
            this.returnState = currentState;
            switch (currentState) {
                case ROOT:
                    // Root view, use stored icon state
                    if (nextState == null) return null;
                    switch (nextState) {
                        case PLAYER_ALL:
                        case KINGDOM_ALL:
                            // Go to next state as defined by icon
                            result = setCurrentView(nextState);
                            break;
                        case PLAYER_SELF:
                            this.scorePlayer = player;
                            result = refreshNewView(MenuState.PLAYER_SCORE);
                            break;
                        case KINGDOM_SELF:
                            this.scoreKingdom = player.getKingdom();
                            result = refreshNewView(MenuState.KINGDOM_SCORE);
                            break;
                    }
                    break;
                case PLAYER_ALL:
                case KINGDOM_SCORE:
                    // Choose a player to view detailed score
                    if(clickedIcon instanceof PlayerIcon) {
                        PlayerIcon icon = (PlayerIcon)clickedIcon;
                        this.scorePlayer = getKonquest().getPlayerManager().getOfflinePlayer(icon.getOfflinePlayer());
                        result = refreshNewView(MenuState.PLAYER_SCORE);
                    }
                    break;
                case KINGDOM_ALL:
                    // Choose a kingdom to view detailed score
                    if(clickedIcon instanceof KingdomIcon) {
                        KingdomIcon icon = (KingdomIcon)clickedIcon;
                        this.scoreKingdom = icon.getKingdom();
                        result = refreshNewView(MenuState.KINGDOM_SCORE);
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
                result = MessagePath.MENU_SCORE_TITLE.getMessage();
                break;
            case PLAYER_ALL:
                result = MessagePath.LABEL_PLAYERS.getMessage();
                break;
            case PLAYER_SCORE:
                result = MessagePath.MENU_SCORE_PLAYER_SCORE.getMessage();
                break;
            case KINGDOM_ALL:
                result = MessagePath.LABEL_KINGDOMS.getMessage();
                break;
            case KINGDOM_SCORE:
                result = MessagePath.MENU_SCORE_KINGDOM_SCORE.getMessage();
                break;
            default:
                break;
        }
        return result;
    }

    private boolean isPlayerScored(KonOfflinePlayer player) {
        return !player.isBarbarian() && !player.getKingdom().isPeaceful();
    }

    private boolean isKingdomScored(KonKingdom kingdom) {
        return kingdom.isCreated() && !kingdom.isPeaceful();
    }

}
