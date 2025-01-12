package com.github.rumsfield.konquest.display.menu;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestRelationshipType;
import com.github.rumsfield.konquest.display.DisplayView;
import com.github.rumsfield.konquest.display.StateMenu;
import com.github.rumsfield.konquest.display.icon.*;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;

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
        this.returnState = null;

        /* Initialize menu view */
        setCurrentView(MenuState.ROOT);

    }

    public void goToPlayerScore(KonOfflinePlayer scorePlayer) {
        if (scorePlayer == null) return;
        this.scorePlayer = scorePlayer;
        // Change to player score view
        refreshNewView(MenuState.PLAYER_SCORE);
    }

    public void goToKingdomScore(KonKingdom scoreKingdom) {
        if (scoreKingdom == null) return;
        this.scoreKingdom = scoreKingdom;
        // Change to kingdom score view
        refreshNewView(MenuState.KINGDOM_SCORE);
    }

    /**
     * Creates the root menu view for this menu.
     * This is a single page view.
     */
    private DisplayView createRootView() {
        DisplayView result;
        MenuIcon icon;

        /* Icon slot indexes */
        int rows = 1;
        // Row 0: 0  1  2  3  4  5  6  7  8
        int ROOT_SLOT_PLAYER = 2;
        int ROOT_SLOT_KINGDOM = 3;
        int ROOT_SLOT_ALL_PLAYERS = 5;
        int ROOT_SLOT_ALL_KINGDOMS = 6;

        result = new DisplayView(rows, getTitle(MenuState.ROOT));

        int playerScore = getKonquest().getKingdomManager().getPlayerScore(player);
        int kingdomScore = getKonquest().getKingdomManager().getKingdomScore(player.getKingdom());
        String playerName = player.getBukkitPlayer().getName();
        String kingdomName = player.getKingdom().getName();

        /* Player Score Icon */
        boolean isPlayerClickable = isPlayerScored(player);
        icon = new PlayerIcon(player.getBukkitPlayer(), Konquest.friendColor2, null, ROOT_SLOT_PLAYER, isPlayerClickable);
        icon.addNameValue(MessagePath.MENU_SCORE_PLAYER_SCORE.getMessage(), playerScore);
        if (isPlayerClickable) {
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
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
        icon = new KingdomIcon(player.getKingdom(), Konquest.friendColor2, KonquestRelationshipType.FRIENDLY, ROOT_SLOT_KINGDOM, isKingdomClickable);
        icon.addNameValue(MessagePath.MENU_SCORE_KINGDOM_SCORE.getMessage(), kingdomScore);
        if (isKingdomClickable) {
            icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
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
        icon = new InfoIcon(MessagePath.LABEL_PLAYERS.getMessage(), Material.PLAYER_HEAD, ROOT_SLOT_ALL_PLAYERS, true);
        icon.addDescription(MessagePath.MENU_SCORE_DESCRIPTION_PLAYERS.getMessage());
        icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
        icon.setState(MenuState.PLAYER_ALL);
        result.addIcon(icon);

        /* All Kingdoms Icon */
        icon = new InfoIcon(MessagePath.LABEL_KINGDOMS.getMessage(), Material.DIAMOND_HELMET, ROOT_SLOT_ALL_KINGDOMS, true);
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
    private List<DisplayView> createPlayerView() {
        ArrayList<MenuIcon> icons = new ArrayList<>();
        List<KonOfflinePlayer> players = new ArrayList<>(getKonquest().getPlayerManager().getAllKonquestOfflinePlayers());
        // Sort by score
        players.sort(playerScoreComparator);

        /* Player Icons */
        int rank = 1;
        MenuIcon icon;
        String currentPlayerName;
        String currentKingdomName;
        int currentScore;
        boolean isPlayerClickable;
        for (KonOfflinePlayer currentPlayer : players) {
            currentPlayerName = currentPlayer.getOfflineBukkitPlayer().getName();
            currentKingdomName = currentPlayer.getKingdom().getName();
            currentScore = getKonquest().getKingdomManager().getPlayerScore(currentPlayer);
            isPlayerClickable = isPlayerScored(currentPlayer);
            icon = new PlayerIcon(currentPlayer.getOfflineBukkitPlayer(),getColor(player,currentPlayer),getRelation(player,currentPlayer),0,isPlayerClickable);
            icon.addProperty(MessagePath.LABEL_LEADERBOARD.getMessage()+" #"+rank);
            icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), currentScore);
            icon.addNameValue(MessagePath.LABEL_KINGDOM.getMessage(), currentKingdomName);
            if (isPlayerClickable) {
                icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
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
    private List<DisplayView> createKingdomView() {
        ArrayList<MenuIcon> icons = new ArrayList<>();
        List<KonKingdom> kingdoms = new ArrayList<>(getKonquest().getKingdomManager().getKingdoms());
        // Sort by score
        kingdoms.sort(kingdomScoreComparator);

        /* Kingdom Icons */
        int rank = 1;
        MenuIcon icon;
        String currentKingdomName;
        int currentScore;
        boolean isKingdomClickable;
        for (KonKingdom currentKingdom : kingdoms) {
            currentKingdomName = currentKingdom.getName();
            currentScore = getKonquest().getKingdomManager().getKingdomScore(currentKingdom);
            isKingdomClickable = isKingdomScored(currentKingdom);
            icon = new KingdomIcon(currentKingdom,getColor(player,currentKingdom),getRelation(player,currentKingdom),0,isKingdomClickable);
            icon.addProperty(MessagePath.LABEL_LEADERBOARD.getMessage()+" #"+rank);
            icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), currentScore);
            if (isKingdomClickable) {
                icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
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
    private DisplayView createPlayerScoreView() {
        if (scorePlayer == null) return null;
        DisplayView result;
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

        result = new DisplayView(1, getTitle(MenuState.PLAYER_SCORE));

        KonPlayerScoreAttributes playerScoreAttributes = getKonquest().getKingdomManager().getPlayerScoreAttributes(scorePlayer);

        int playerScore = playerScoreAttributes.getScore();

        /* Player Score Icon */
        icon = new PlayerIcon(scorePlayer.getOfflineBukkitPlayer(), getColor(player, scorePlayer), getRelation(player, scorePlayer), SLOT_TOTAL, false);
        icon.addNameValue(MessagePath.MENU_SCORE_PLAYER_SCORE.getMessage(), playerScore);
        result.addIcon(icon);

        /* Score Attribute Icons */
        int attValue;
        int attScore;
        // Town Lords
        attValue = playerScoreAttributes.getAttributeValue(KonPlayerScoreAttributes.ScoreAttribute.TOWN_LORDS);
        attScore = playerScoreAttributes.getAttributeScore(KonPlayerScoreAttributes.ScoreAttribute.TOWN_LORDS);
        icon = new InfoIcon(MessagePath.MENU_SCORE_TOWN_1.getMessage(), Material.IRON_HELMET, SLOT_TOWN_LORD, false);
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), attValue);
        icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), attScore);
        result.addIcon(icon);
        // Town Knights
        attValue = playerScoreAttributes.getAttributeValue(KonPlayerScoreAttributes.ScoreAttribute.TOWN_KNIGHTS);
        attScore = playerScoreAttributes.getAttributeScore(KonPlayerScoreAttributes.ScoreAttribute.TOWN_KNIGHTS);
        icon = new InfoIcon(MessagePath.MENU_SCORE_TOWN_2.getMessage(), Material.IRON_HORSE_ARMOR, SLOT_TOWN_KNIGHT, false);
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), attValue);
        icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), attScore);
        result.addIcon(icon);
        // Town Residents
        attValue = playerScoreAttributes.getAttributeValue(KonPlayerScoreAttributes.ScoreAttribute.TOWN_RESIDENTS);
        attScore = playerScoreAttributes.getAttributeScore(KonPlayerScoreAttributes.ScoreAttribute.TOWN_RESIDENTS);
        icon = new InfoIcon(MessagePath.MENU_SCORE_TOWN_3.getMessage(), Material.LEATHER_CHESTPLATE, SLOT_TOWN_RESIDENT, false);
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), attValue);
        icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), attScore);
        result.addIcon(icon);
        // Lord Land
        attValue = playerScoreAttributes.getAttributeValue(KonPlayerScoreAttributes.ScoreAttribute.LAND_LORDS);
        attScore = playerScoreAttributes.getAttributeScore(KonPlayerScoreAttributes.ScoreAttribute.LAND_LORDS);
        icon = new InfoIcon(MessagePath.MENU_SCORE_LAND_1.getMessage(), Material.SMOOTH_STONE, SLOT_LAND_LORD, false);
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), attValue);
        icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), attScore);
        result.addIcon(icon);
        // Knight Land
        attValue = playerScoreAttributes.getAttributeValue(KonPlayerScoreAttributes.ScoreAttribute.LAND_KNIGHTS);
        attScore = playerScoreAttributes.getAttributeScore(KonPlayerScoreAttributes.ScoreAttribute.LAND_KNIGHTS);
        icon = new InfoIcon(MessagePath.MENU_SCORE_LAND_2.getMessage(), Material.OAK_PLANKS, SLOT_LAND_KNIGHT, false);
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), attValue);
        icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), attScore);
        result.addIcon(icon);
        // Resident Land
        attValue = playerScoreAttributes.getAttributeValue(KonPlayerScoreAttributes.ScoreAttribute.LAND_RESIDENTS);
        attScore = playerScoreAttributes.getAttributeScore(KonPlayerScoreAttributes.ScoreAttribute.LAND_RESIDENTS);
        icon = new InfoIcon(MessagePath.MENU_SCORE_LAND_3.getMessage(), Material.GRASS_BLOCK, SLOT_LAND_RESIDENT, false);
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), attValue);
        icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), attScore);
        result.addIcon(icon);

        /* Navigation */
        addNavEmpty(result);
        addNavClose(result);
        addNavReturn(result);

        return result;
    }

    /**
     * Creates the kingdom score view for this menu.
     * This is a single page view.
     */
    private DisplayView createKingdomScoreView() {
        if (scoreKingdom == null) return null;
        DisplayView result;
        MenuIcon icon;
        int NUM_LEADERBOARD = 9;

        /* Icon slot indexes */
        int rows = 2;
        // Row 0: 0  1  2  3  4  5  6  7  8
        int SLOT_LEADERBOARD_START = 0;
        // Row 1: 9 10 11 12 13 14 15 16 17
        int SLOT_TOTAL = 9;
        int SLOT_TOWNS = 11;
        int SLOT_LAND = 12;
        int SLOT_FAVOR = 13;
        int SLOT_POPULATION = 14;

        result = new DisplayView(rows, getTitle(MenuState.KINGDOM_SCORE));

        KonKingdomScoreAttributes kingdomScoreAttributes = getKonquest().getKingdomManager().getKingdomScoreAttributes(scoreKingdom);

        int kingdomScore = kingdomScoreAttributes.getScore();
        String kingdomName = scoreKingdom.getName();

        /* Kingdom Score Icon */
        icon = new KingdomIcon(scoreKingdom, getColor(player,scoreKingdom), getRelation(player,scoreKingdom), SLOT_TOTAL, false);
        icon.addNameValue(MessagePath.MENU_SCORE_KINGDOM_SCORE.getMessage(), kingdomScore);
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
        icon = new InfoIcon(MessagePath.MENU_SCORE_KINGDOM_POPULATION.getMessage(), Material.PLAYER_HEAD, SLOT_POPULATION, false);
        icon.addNameValue(MessagePath.LABEL_TOTAL.getMessage(), attValue);
        icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), attScore);
        result.addIcon(icon);

        /* Leaderboard Icons */
        KonLeaderboard leaderboard = getKonquest().getKingdomManager().getKingdomLeaderboard(scoreKingdom);
        if (leaderboard.isEmpty()) {
            // Error Icon
            icon = new InfoIcon(ChatColor.DARK_RED+MessagePath.LABEL_LEADERBOARD.getMessage(),Material.BARRIER,SLOT_LEADERBOARD_START,false);
            icon.addError(MessagePath.MENU_SCORE_LEADERBOARD_EMPTY.getMessage());
            result.addIcon(icon);
        } else {
            // Player Icons
            int numEntries = NUM_LEADERBOARD;
            if (leaderboard.getSize() < numEntries) {
                numEntries = leaderboard.getSize();
            }
            int rank = 1;
            int index = SLOT_LEADERBOARD_START;
            for (int n = 0; n < numEntries; n++) {
                OfflinePlayer entryPlayer = leaderboard.getOfflinePlayer(n);
                KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayer(entryPlayer);
                if (offlinePlayer == null) {
                    continue;
                }
                icon = new PlayerIcon(entryPlayer,getColor(player,offlinePlayer),getRelation(player,offlinePlayer),index,true);
                icon.addProperty(MessagePath.LABEL_LEADERBOARD.getMessage()+" #"+rank);
                icon.addNameValue(MessagePath.LABEL_SCORE.getMessage(), leaderboard.getScore(n));
                icon.addNameValue(MessagePath.LABEL_KINGDOM.getMessage(), kingdomName);
                icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
                icon.setState(MenuState.PLAYER_SCORE);
                result.addIcon(icon);
                rank++;
                index++;
            }
        }

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
            DisplayView view = getCurrentView();
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
                result = MessagePath.MENU_MAIN_SCORE.getMessage();
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
