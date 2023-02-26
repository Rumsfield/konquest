package com.github.rumsfield.konquest.display;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestUpgrade;
import com.github.rumsfield.konquest.display.icon.*;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.manager.KingdomManager;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Villager;

import java.util.*;

public class TownManagementMenu extends StateMenu implements ViewableMenu {

    enum MenuState implements StateMenu.State {
        ROOT,
        A_REQUESTS,
        //A_PLOTS, <- there is no explicit plots state in this menu; it opens a new menu
        A_SHIELD,
        A_ARMOR,
        B_PROMOTE,
        B_DEMOTE,
        B_TRANSFER,
        B_UPGRADES,
        B_OPTIONS,
        B_SPECIALIZATION
    }

    enum AccessType implements StateMenu.Access {
        DEFAULT,
        KNIGHT,
        LORD
    }

    /* Icon slot indexes */
    private final int ROOT_SLOT_REQUESTS 		= 1;
    private final int ROOT_SLOT_PLOTS 			= 3;
    private final int ROOT_SLOT_SHIELD 			= 5;
    private final int ROOT_SLOT_ARMOR 			= 7;
    private final int ROOT_SLOT_PROMOTE 		= 10;
    private final int ROOT_SLOT_DEMOTE 	        = 11;
    private final int ROOT_SLOT_TRANSFER 		= 12;
    private final int ROOT_SLOT_UPGRADES 		= 14;
    private final int ROOT_SLOT_OPTIONS 		= 15;
    private final int ROOT_SLOT_SPECIALIZATION	= 16;

    private final String loreColor = DisplayManager.loreFormat;
    private final String valueColor = DisplayManager.loreFormat;
    private final String hintColor = DisplayManager.loreFormat;

    private final KingdomManager manager;
    private final KonPlayer player;
    private final KonTown town;
    private final boolean isAdmin;

    public TownManagementMenu(Konquest konquest, KonPlayer player, KonTown town, boolean isAdmin) {
        super(konquest,MenuState.ROOT,AccessType.DEFAULT);
        this.manager = konquest.getKingdomManager();
        this.player = player;
        this.town = town;
        this.isAdmin = isAdmin;

        initializeMenu();
        renderDefaultViews();
    }

    private void initializeMenu() {
        if(town != null) {
            UUID id = player.getBukkitPlayer().getUniqueId();
            if(isAdmin) {
                menuAccess = AccessType.LORD;
            } else {
                if(town.isLord(id)) {
                    menuAccess = AccessType.LORD;
                } else if(town.isPlayerKnight(player.getBukkitPlayer())) {
                    menuAccess = AccessType.KNIGHT;
                } else {
                    menuAccess = AccessType.DEFAULT;
                }
            }
        }
    }

    private void renderDefaultViews() {
        DisplayMenu renderView;

        /* Root View */
        renderView = createRootView();
        views.put(MenuState.ROOT, renderView);
        refreshNavigationButtons(MenuState.ROOT);

    }

    private DisplayMenu createRootView() {
        DisplayMenu result;
        MenuIcon icon;
        List<String> loreList = new ArrayList<>();
        ChatColor officerColor = ChatColor.BLUE;
        ChatColor masterColor = ChatColor.LIGHT_PURPLE;

        int rows = 1;
        if(menuAccess.equals(AccessType.LORD)) {
            rows = 2;
        }

        result = new DisplayMenu(rows+1, getTitle(MenuState.ROOT));

        if(menuAccess.equals(AccessType.DEFAULT)) {
            // Invalid access, error icon
            icon = new InfoIcon("Error", loreList, Material.VOID_AIR, 4, false);
            result.addIcon(icon);

        } else {
            // Render icons for Knights and Lords
            if(menuAccess.equals(AccessType.KNIGHT) || menuAccess.equals(AccessType.LORD)) {
                /* Requests Icon */
                loreList.clear();
                loreList.add(loreColor+"Accept or deny players who want to join your town.");
                int numRequests = town.getJoinRequests().size();
                Material requestMat = Material.GLASS_BOTTLE;
                if(numRequests > 0) {
                    loreList.add(valueColor+""+numRequests);
                    requestMat = Material.HONEY_BOTTLE;
                }
                icon = new InfoIcon(officerColor+"Requests", loreList, requestMat, ROOT_SLOT_REQUESTS, true);
                result.addIcon(icon);

                /* Plots Icon */
                loreList.clear();
                loreList.add(loreColor+"Open the town plot editor.");
                icon = new InfoIcon(officerColor+"Plots", loreList, Material.GRASS_BLOCK, ROOT_SLOT_PLOTS, true);
                result.addIcon(icon);

                /* Shields Icon */
                loreList.clear();
                loreList.add(loreColor+"Apply town shields");
                boolean isShieldsClickable = true;
                if(!konquest.getShieldManager().isShieldsEnabled()) {
                    isShieldsClickable = false;
                    loreList.add(ChatColor.RED+"Disabled");
                }
                icon = new InfoIcon(officerColor+"Shields", loreList, Material.SHIELD, ROOT_SLOT_SHIELD, isShieldsClickable);
                result.addIcon(icon);

                /* Armor Icon */
                loreList.clear();
                loreList.add(loreColor+"Apply town armor");
                boolean isArmorClickable = true;
                if(!konquest.getShieldManager().isArmorsEnabled()) {
                    isArmorClickable = false;
                    loreList.add(ChatColor.RED+"Disabled");
                }
                icon = new InfoIcon(officerColor+"Armor", loreList, Material.DIAMOND_CHESTPLATE, ROOT_SLOT_ARMOR, isArmorClickable);
                result.addIcon(icon);
            }
            // Render icons for lords only
            if(menuAccess.equals(AccessType.LORD)) {
                /* Promote Icon */
                loreList = new ArrayList<>();
                loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_PROMOTE.getMessage());
                icon = new InfoIcon(masterColor+MessagePath.MENU_GUILD_PROMOTE.getMessage(), loreList, Material.DIAMOND_HORSE_ARMOR, ROOT_SLOT_PROMOTE, true);
                result.addIcon(icon);

                /* Demote Icon */
                loreList.clear();
                loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_DEMOTE.getMessage());
                icon = new InfoIcon(masterColor+MessagePath.MENU_GUILD_DEMOTE.getMessage(), loreList, Material.LEATHER_HORSE_ARMOR, ROOT_SLOT_DEMOTE, true);
                result.addIcon(icon);

                /* Transfer Icon */
                loreList.clear();
                loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_TRANSFER.getMessage());
                icon = new InfoIcon(masterColor+MessagePath.MENU_GUILD_TRANSFER.getMessage(), loreList, Material.ELYTRA, ROOT_SLOT_TRANSFER, true);
                result.addIcon(icon);

                /* Upgrades Icon */
                loreList.clear();
                loreList.add(loreColor+"Apply town upgrades.");
                boolean isUpgradesClickable = true;
                if(!konquest.getUpgradeManager().isEnabled()) {
                    isUpgradesClickable = false;
                    loreList.add(ChatColor.RED+"Disabled");
                }
                icon = new InfoIcon(masterColor+"Upgrades", loreList, Material.GOLDEN_APPLE, ROOT_SLOT_UPGRADES, isUpgradesClickable);
                result.addIcon(icon);

                /* Options Icon */
                loreList.clear();
                loreList.add(loreColor+"Edit town options.");
                icon = new InfoIcon(masterColor+"Options", loreList, Material.PAPER, ROOT_SLOT_OPTIONS, true);
                result.addIcon(icon);

                /* Specialization Icon */
                loreList.clear();
                loreList.add(loreColor+"Current: "+valueColor+town.getSpecialization().name());
                loreList.add(loreColor+"Choose a trade specialization.");
                boolean isSpecializationClickable = true;
                if(!konquest.getKingdomManager().getIsDiscountEnable()) {
                    isSpecializationClickable = false;
                    loreList.add(ChatColor.RED+"Disabled");
                }
                icon = new InfoIcon(masterColor+"Specialization", loreList, Material.EMERALD, ROOT_SLOT_SPECIALIZATION, isSpecializationClickable);
                result.addIcon(icon);
            }
        }
        return result;
    }

    private DisplayMenu createPlayerView(MenuState context) {
        // A paged view of players, with lore based on context
        DisplayMenu result;
        pages.clear();
        currentPage = 0;
        String loreHintStr1 = "";
        String loreHintStr2 = "";
        PlayerIcon.PlayerIconAction iconAction = PlayerIcon.PlayerIconAction.DISPLAY_INFO;
        boolean isClickable;
        List<OfflinePlayer> players = new ArrayList<>();

        // Determine list of players given context
        if(context.equals(MenuState.A_REQUESTS)) {
            players.addAll(town.getJoinRequests());
            loreHintStr1 = MessagePath.MENU_GUILD_HINT_REQUEST_1.getMessage();
            loreHintStr2 = MessagePath.MENU_GUILD_HINT_REQUEST_2.getMessage();
            isClickable = true;
        } else if(context.equals(MenuState.B_PROMOTE)) {
            players.addAll(town.getPlayerResidentsOnly());
            loreHintStr1 = MessagePath.MENU_GUILD_HINT_PROMOTE.getMessage();
            isClickable = true;
        } else if(context.equals(MenuState.B_DEMOTE)) {
            players.addAll(town.getPlayerKnightsOnly());
            loreHintStr1 = MessagePath.MENU_GUILD_HINT_DEMOTE.getMessage();
            isClickable = true;
        } else if(context.equals(MenuState.B_TRANSFER)) {
            players.addAll(town.getPlayerKnightsOnly());
            players.addAll(town.getPlayerResidentsOnly());
            loreHintStr1 = MessagePath.MENU_GUILD_HINT_TRANSFER.getMessage();
            isClickable = true;
        } else {
            return null;
        }

        // Create page(s)
        String pageLabel;
        List<String> loreList;
        int pageTotal = getTotalPages(players.size());
        int pageNum = 0;
        ListIterator<OfflinePlayer> listIter = players.listIterator();
        for(int i = 0; i < pageTotal; i++) {
            int pageRows = getNumPageRows(players.size(), i);
            pageLabel = getTitle(context)+" "+(i+1)+"/"+pageTotal;
            pages.add(pageNum, new DisplayMenu(pageRows+1, pageLabel));
            int slotIndex = 0;
            while(slotIndex < MAX_ICONS_PER_PAGE && listIter.hasNext()) {
                /* Player Icon (n) */
                OfflinePlayer currentPlayer = listIter.next();
                loreList = new ArrayList<>();
                String townRole = town.getPlayerRoleName(currentPlayer);
                if(!townRole.equals("")) {
                    loreList.add(townRole);
                }
                if(!loreHintStr1.equals("")) {
                    loreList.add(hintColor+loreHintStr1);
                }
                if(!loreHintStr2.equals("")) {
                    loreList.add(hintColor+loreHintStr2);
                }
                PlayerIcon playerIcon = new PlayerIcon(ChatColor.GREEN+currentPlayer.getName(),loreList,currentPlayer,slotIndex,isClickable,iconAction);
                pages.get(pageNum).addIcon(playerIcon);
                slotIndex++;
            }
            pageNum++;
        }
        result = pages.get(currentPage);
        return result;
    }

    private DisplayMenu createShieldView() {
        DisplayMenu result;
        pages.clear();
        currentPage = 0;
        String pageLabel;

        boolean isShieldsEnabled = konquest.getShieldManager().isShieldsEnabled();
        if(!isShieldsEnabled) {
            return null;
        }

        // Page 0+
        List<KonShield> allShields = konquest.getShieldManager().getShields();
        int pageTotal = getTotalPages(allShields.size());
        int pageNum = 0;
        ListIterator<KonShield> shieldIter = allShields.listIterator();
        for(int i = 0; i < pageTotal; i++) {
            int pageRows = getNumPageRows(allShields.size(), i);
            pageLabel = getTitle(MenuState.A_SHIELD)+" "+(i+1)+"/"+pageTotal;
            pages.add(pageNum, new DisplayMenu(pageRows+1, pageLabel));
            int slotIndex = 0;
            while(slotIndex < MAX_ICONS_PER_PAGE && shieldIter.hasNext()) {
                /* Shield Icon (n) */
                KonShield currentShield = shieldIter.next();
                ShieldIcon shieldIcon = new ShieldIcon(currentShield, true, town.getNumResidents(), slotIndex);
                pages.get(pageNum).addIcon(shieldIcon);
                slotIndex++;
            }
            pageNum++;
        }
        result = pages.get(currentPage);
        return result;
    }

    private DisplayMenu createArmorView() {
        DisplayMenu result;
        pages.clear();
        currentPage = 0;
        String pageLabel;

        boolean isArmorsEnabled = konquest.getShieldManager().isArmorsEnabled();
        if(!isArmorsEnabled) {
            return null;
        }

        // Page 0+
        List<KonArmor> allArmors = konquest.getShieldManager().getArmors();
        int pageTotal = getTotalPages(allArmors.size());
        int pageNum = 0;
        ListIterator<KonArmor> armorIter = allArmors.listIterator();
        for(int i = 0; i < pageTotal; i++) {
            int pageRows = getNumPageRows(allArmors.size(), i);
            pageLabel = getTitle(MenuState.A_ARMOR)+" "+(i+1)+"/"+pageTotal;
            pages.add(pageNum, new DisplayMenu(pageRows+1, pageLabel));
            int slotIndex = 0;
            while(slotIndex < MAX_ICONS_PER_PAGE && armorIter.hasNext()) {
                /* Armor Icon (n) */
                KonArmor currentArmor = armorIter.next();
                ArmorIcon armorIcon = new ArmorIcon(currentArmor, true, town.getNumResidents(), slotIndex);
                pages.get(pageNum).addIcon(armorIcon);
                slotIndex++;
            }
            pageNum++;
        }
        result = pages.get(currentPage);
        return result;
    }

    private DisplayMenu createUpgradeView() {
        DisplayMenu result;
        pages.clear();
        currentPage = 0;
        String pageLabel;

        boolean isUpgradesEnabled = konquest.getUpgradeManager().isEnabled();
        if(!isUpgradesEnabled) {
            return null;
        }

        // Page 0+
        HashMap<KonquestUpgrade, Integer> availableUpgrades = konquest.getUpgradeManager().getAvailableUpgrades(town);
        // Place upgrades in ordinal order into list
        List<KonUpgrade> allUpgrades = new ArrayList<>();
        for(KonUpgrade upgrade : KonUpgrade.values()) {
            if(availableUpgrades.containsKey(upgrade)) {
                allUpgrades.add(upgrade);
            }
        }
        int pageTotal = getTotalPages(allUpgrades.size());
        int pageNum = 0;
        ListIterator<KonUpgrade> upgradeIter = allUpgrades.listIterator();
        for(int i = 0; i < pageTotal; i++) {
            int pageRows = getNumPageRows(allUpgrades.size(), i);
            pageLabel = getTitle(MenuState.B_UPGRADES)+" "+(i+1)+"/"+pageTotal;
            pages.add(pageNum, new DisplayMenu(pageRows+1, pageLabel));
            int slotIndex = 0;
            while(slotIndex < MAX_ICONS_PER_PAGE && upgradeIter.hasNext()) {
                /* Upgrade Icon (n) */
                KonUpgrade currentUpgrade = upgradeIter.next();
                int currentLevel = availableUpgrades.get(currentUpgrade);
                int cost = konquest.getUpgradeManager().getUpgradeCost(currentUpgrade, currentLevel);
                int pop = konquest.getUpgradeManager().getUpgradePopulation(currentUpgrade, currentLevel);
                UpgradeIcon upgradeIcon = new UpgradeIcon(currentUpgrade, currentLevel, slotIndex, cost, pop);
                pages.get(pageNum).addIcon(upgradeIcon);
                slotIndex++;
            }
            pageNum++;
        }
        result = pages.get(currentPage);
        return result;
    }

    private DisplayMenu createOptionsView() {
        DisplayMenu result;
        pages.clear();
        currentPage = 0;
        OptionIcon option;
        ArrayList<String> loreList;
        String currentValue;
        String pageLabel;

        // Page 0
        pageLabel = getTitle(MenuState.B_OPTIONS);
        int pageNum = 0;
        int pageRows = 1;
        pages.add(pageNum, new DisplayMenu(pageRows+1, pageLabel));

        // Open Info Icon
        currentValue = DisplayManager.boolean2Lang(town.isOpen())+" "+DisplayManager.boolean2Symbol(town.isOpen());
        loreList = new ArrayList<>(Konquest.stringPaginate(MessagePath.MENU_OPTIONS_OPEN.getMessage()));
        loreList.add(loreColor+MessagePath.MENU_OPTIONS_CURRENT.getMessage(valueColor+currentValue));
        loreList.add(hintColor+MessagePath.MENU_OPTIONS_HINT.getMessage());
        option = new OptionIcon(OptionIcon.optionAction.TOWN_OPEN, loreColor+MessagePath.LABEL_OPEN.getMessage(), loreList, Material.DARK_OAK_DOOR, 2);
        pages.get(pageNum).addIcon(option);

        // Plot Only Info Icon
        currentValue = DisplayManager.boolean2Lang(town.isPlotOnly())+" "+DisplayManager.boolean2Symbol(town.isPlotOnly());
        loreList = new ArrayList<>(Konquest.stringPaginate(MessagePath.MENU_OPTIONS_PLOT.getMessage()));
        loreList.add(loreColor+MessagePath.MENU_OPTIONS_CURRENT.getMessage(valueColor+currentValue));
        loreList.add(hintColor+MessagePath.MENU_OPTIONS_HINT.getMessage());
        option = new OptionIcon(OptionIcon.optionAction.TOWN_PLOT_ONLY, loreColor+MessagePath.LABEL_PLOT.getMessage(), loreList, Material.DIAMOND_SHOVEL, 3);
        pages.get(pageNum).addIcon(option);

        // Redstone Info Icon
        currentValue = DisplayManager.boolean2Lang(town.isEnemyRedstoneAllowed())+" "+DisplayManager.boolean2Symbol(town.isEnemyRedstoneAllowed());
        loreList = new ArrayList<>(Konquest.stringPaginate(MessagePath.MENU_OPTIONS_REDSTONE.getMessage()));
        loreList.add(loreColor+MessagePath.MENU_OPTIONS_CURRENT.getMessage(valueColor+currentValue));
        loreList.add(hintColor+MessagePath.MENU_OPTIONS_HINT.getMessage());
        option = new OptionIcon(OptionIcon.optionAction.TOWN_REDSTONE, loreColor+MessagePath.LABEL_ENEMY_REDSTONE.getMessage(), loreList, Material.LEVER, 5);
        pages.get(pageNum).addIcon(option);

        // Golem Offensive Info Icon
        currentValue = DisplayManager.boolean2Lang(town.isGolemOffensive())+" "+DisplayManager.boolean2Symbol(town.isGolemOffensive());
        loreList = new ArrayList<>(Konquest.stringPaginate(MessagePath.MENU_OPTIONS_GOLEM.getMessage()));
        loreList.add(loreColor+MessagePath.MENU_OPTIONS_CURRENT.getMessage(valueColor+currentValue));
        loreList.add(hintColor+MessagePath.MENU_OPTIONS_HINT.getMessage());
        option = new OptionIcon(OptionIcon.optionAction.TOWN_GOLEM, loreColor+MessagePath.LABEL_GOLEM_OFFENSE.getMessage(), loreList, Material.IRON_SWORD, 6);
        pages.get(pageNum).addIcon(option);

        result = pages.get(currentPage);
        return result;
    }

    private DisplayMenu createSpecializationView() {
        DisplayMenu result;
        pages.clear();
        currentPage = 0;
        String pageLabel;

        boolean isSpecializationEnabled = konquest.getKingdomManager().getIsDiscountEnable();
        if(!isSpecializationEnabled) {
            return null;
        }

        // Page 0
        pageLabel = getTitle(MenuState.B_SPECIALIZATION);
        int numEntries = Villager.Profession.values().length - 1; // Subtract one to omit current specialization choice
        int pageRows = (int)Math.ceil((double)numEntries / 9);
        int pageNum = 0;
        pages.add(pageNum, new DisplayMenu(pageRows+1, pageLabel));
        int index = 0;
        List<String> loreList = new ArrayList<>();
        if(!isAdmin) {
            double costSpecial = konquest.getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SPECIALIZE.getPath());
            String cost = String.format("%.2f",costSpecial);
            loreList.add(loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+cost);
        }
        loreList.add(hintColor+MessagePath.MENU_GUILD_HINT_SPECIAL.getMessage());
        for(Villager.Profession profession : Villager.Profession.values()) {
            if(town != null && !profession.equals(town.getSpecialization())) {
                ProfessionIcon professionIcon = new ProfessionIcon(ChatColor.GOLD+profession.name(),loreList,profession,index,true);
                pages.get(pageNum).addIcon(professionIcon);
                index++;
            }
        }
        result = pages.get(currentPage);
        return result;
    }

    @Override
    public DisplayMenu getCurrentView() {
        return views.get(currentState);
    }

    @Override
    public DisplayMenu updateState(int slot, boolean clickType) {
        // Assume a clickable icon was clicked
        // Do something based on current state and clicked slot
        DisplayMenu result = null;
        int navMaxIndex = getCurrentView().getInventory().getSize()-1;
        int navMinIndex = getCurrentView().getInventory().getSize()-9;
        if(slot <= navMaxIndex && slot >= navMinIndex) {
            // Clicked in navigation bar
            int index = slot-navMinIndex;
            // (back [0]) close [4], return [5], (next [8])
            if(index == 0) {
                result = goPageBack();
            } else if(index == 5) {
                // Return to previous root
                //result = views.get(MenuState.ROOT);
                result = goToRootView();
                currentState = MenuState.ROOT;
            } else if(index == 8) {
                result = goPageNext();
            }
        } else if(slot < navMinIndex) {
            // Click in non-navigation slot
            MenuIcon clickedIcon = views.get(currentState).getIcon(slot);
            MenuState currentMenuState = (MenuState)currentState;
            switch(currentMenuState) {
                case ROOT:
                    if(slot == ROOT_SLOT_REQUESTS) {
                        // Go to the player requests view
                        currentState = MenuState.A_REQUESTS;
                        result = goToPlayerView(MenuState.A_REQUESTS);

                    } else if(slot == ROOT_SLOT_PLOTS) {
                        // Open the plots menu
                        konquest.getDisplayManager().displayTownPlotMenu(player.getBukkitPlayer(),town);
                        // Return null result to close this menu

                    } else if(slot == ROOT_SLOT_SHIELD) {
                        // Go to the shields view
                        currentState = MenuState.A_SHIELD;
                        result = goToShieldView();

                    } else if(slot == ROOT_SLOT_ARMOR) {
                        // Go to the armor view
                        currentState = MenuState.A_ARMOR;
                        result = goToArmorView();

                    } else if(slot == ROOT_SLOT_PROMOTE) {
                        // Clicked to view members to promote
                        currentState = MenuState.B_PROMOTE;
                        result = goToPlayerView(MenuState.B_PROMOTE);

                    } else if(slot == ROOT_SLOT_DEMOTE) {
                        // Clicked to view officers to demote
                        currentState = MenuState.B_DEMOTE;
                        result = goToPlayerView(MenuState.B_DEMOTE);

                    } else if(slot == ROOT_SLOT_TRANSFER) {
                        // Clicked to view members to transfer master
                        currentState = MenuState.B_TRANSFER;
                        result = goToPlayerView(MenuState.B_TRANSFER);

                    } else if(slot == ROOT_SLOT_UPGRADES) {
                        // Clicked to view town upgrades
                        currentState = MenuState.B_UPGRADES;
                        result = goToUpgradeView();

                    }  else if(slot == ROOT_SLOT_OPTIONS) {
                        // Clicked to view town options
                        currentState = MenuState.B_OPTIONS;
                        result = goToOptionsView();

                    } else if(slot == ROOT_SLOT_SPECIALIZATION) {
                        // Clicked to view trade specializations
                        currentState = MenuState.B_SPECIALIZATION;
                        result = goToSpecializationView();

                    }
                    break;
                case A_REQUESTS:
                    if(clickedIcon instanceof PlayerIcon) {
                        PlayerIcon icon = (PlayerIcon)clickedIcon;
                        OfflinePlayer clickPlayer = icon.getOfflinePlayer();
                        boolean status = manager.menuRespondTownRequest(player, clickPlayer, town, clickType);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = goToPlayerView(MenuState.A_REQUESTS);
                    }
                    break;
                case A_SHIELD:
                    if(clickedIcon instanceof ShieldIcon) {
                        ShieldIcon icon = (ShieldIcon)clickedIcon;
                        boolean status = konquest.getShieldManager().activateTownShield(icon.getShield(), town, player.getBukkitPlayer(), isAdmin);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = goToShieldView();
                    }
                    break;
                case A_ARMOR:
                    if(clickedIcon instanceof ArmorIcon) {
                        ArmorIcon icon = (ArmorIcon)clickedIcon;
                        boolean status = konquest.getShieldManager().activateTownArmor(icon.getArmor(), town, player.getBukkitPlayer(), isAdmin);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = goToArmorView();
                    }
                    break;
                case B_PROMOTE:
                    if(clickedIcon instanceof PlayerIcon) {
                        PlayerIcon icon = (PlayerIcon)clickedIcon;
                        OfflinePlayer clickPlayer = icon.getOfflinePlayer();
                        boolean status = manager.menuPromoteDemoteTownKnight(player,clickPlayer,town,true);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = goToPlayerView(MenuState.B_PROMOTE);
                    }
                    break;
                case B_DEMOTE:
                    if(clickedIcon instanceof PlayerIcon) {
                        PlayerIcon icon = (PlayerIcon)clickedIcon;
                        OfflinePlayer clickPlayer = icon.getOfflinePlayer();
                        boolean status = manager.menuPromoteDemoteTownKnight(player,clickPlayer,town,false);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = goToPlayerView(MenuState.B_DEMOTE);
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
                case B_UPGRADES:
                    if(clickedIcon instanceof UpgradeIcon) {
                        UpgradeIcon icon = (UpgradeIcon)clickedIcon;
                        boolean status = konquest.getUpgradeManager().addTownUpgrade(town, icon.getUpgrade(), icon.getLevel(), player.getBukkitPlayer());
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = goToUpgradeView();
                    }
                    break;
                case B_OPTIONS:
                    if(clickedIcon instanceof OptionIcon) {
                        OptionIcon icon = (OptionIcon)clickedIcon;
                        boolean status = manager.changeTownOption(icon.getAction(), town, player.getBukkitPlayer());
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = goToOptionsView();
                    }
                    break;
                case B_SPECIALIZATION:
                    if(clickedIcon instanceof ProfessionIcon) {
                        ProfessionIcon icon = (ProfessionIcon)clickedIcon;
                        Villager.Profession clickProfession = icon.getProfession();
                        boolean status = manager.menuChangeTownSpecialization(town, clickProfession, player, isAdmin);
                        playStatusSound(player.getBukkitPlayer(),status);
                        result = goToSpecializationView();
                    }
                    break;
                default:
                    break;
            }
        }
        refreshNavigationButtons(currentState);
        return result;
    }

    private String getTitle(MenuState context) {
        String result = "error";
        String color = DisplayManager.titleFormat;
        if(isAdmin) {
            color = ""+ChatColor.GOLD;
        }
        String name = "";
        if(town != null) {
            name = town.getName();
        }
        switch(context) {
            case ROOT:
                result = color+name+" "+"Menu";
                break;
            case A_REQUESTS:
                result = color+name+" "+"Requests";
                break;
            case A_SHIELD:
                result = color+name+" "+"Shields";
                break;
            case A_ARMOR:
                result = color+name+" "+"Armor";
                break;
            case B_PROMOTE:
                result = color+"Promote Knights";
                break;
            case B_DEMOTE:
                result = color+"Demote Knights";
                break;
            case B_TRANSFER:
                result = color+"Transfer Lordship";
                break;
            case B_UPGRADES:
                result = color+name+" "+"Upgrades";
                break;
            case B_OPTIONS:
                result = color+name+" "+"Options";
                break;
            case B_SPECIALIZATION:
                result = color+"Trade Specialization";
                break;
            default:
                break;
        }
        return result;
    }

    private DisplayMenu goToRootView() {
        DisplayMenu result = createRootView();
        views.put(MenuState.ROOT, result);
        return result;
    }

    private DisplayMenu goToPlayerView(MenuState context) {
        DisplayMenu result = createPlayerView(context);
        views.put(context, result);
        return result;
    }

    private DisplayMenu goToShieldView() {
        DisplayMenu result = createShieldView();
        views.put(MenuState.A_SHIELD, result);
        return result;
    }

    private DisplayMenu goToArmorView() {
        DisplayMenu result = createArmorView();
        views.put(MenuState.A_ARMOR, result);
        return result;
    }

    private DisplayMenu goToUpgradeView() {
        DisplayMenu result = createUpgradeView();
        views.put(MenuState.B_UPGRADES, result);
        return result;
    }

    private DisplayMenu goToOptionsView() {
        DisplayMenu result = createOptionsView();
        views.put(MenuState.B_OPTIONS, result);
        return result;
    }

    private DisplayMenu goToSpecializationView() {
        DisplayMenu result = createSpecializationView();
        views.put(MenuState.B_SPECIALIZATION, result);
        return result;
    }

    /**
     * Place all navigation button icons on view given context and update icons
     */
    void refreshNavigationButtons(State context) {
        DisplayMenu view = views.get(context);
        int navStart = view.getInventory().getSize()-9;
        if(navStart < 0) {
            ChatUtil.printDebug("Guild menu nav buttons failed to refresh in context "+context.toString());
            return;
        }
        if(context.equals(MenuState.ROOT)) {
            // Close [4]
            view.addIcon(navIconEmpty(navStart));
            view.addIcon(navIconEmpty(navStart+1));
            view.addIcon(navIconEmpty(navStart+2));
            view.addIcon(navIconEmpty(navStart+3));
            view.addIcon(navIconClose(navStart+4));
            view.addIcon(navIconEmpty(navStart+5));
            view.addIcon(navIconEmpty(navStart+6));
            view.addIcon(navIconEmpty(navStart+7));
            view.addIcon(navIconEmpty(navStart+8));
        } else if(context.equals(MenuState.B_UPGRADES) || context.equals(MenuState.B_OPTIONS) || context.equals(MenuState.B_SPECIALIZATION)) {
            // Close [4], Return [5]
            view.addIcon(navIconEmpty(navStart));
            view.addIcon(navIconEmpty(navStart+1));
            view.addIcon(navIconEmpty(navStart+2));
            view.addIcon(navIconEmpty(navStart+3));
            view.addIcon(navIconClose(navStart+4));
            view.addIcon(navIconReturn(navStart+5));
            view.addIcon(navIconEmpty(navStart+6));
            view.addIcon(navIconEmpty(navStart+7));
            view.addIcon(navIconEmpty(navStart+8));
        } else if(context.equals(MenuState.A_REQUESTS) || context.equals(MenuState.A_SHIELD) || context.equals(MenuState.A_ARMOR) ||
                context.equals(MenuState.B_PROMOTE) || context.equals(MenuState.B_DEMOTE) || context.equals(MenuState.B_TRANSFER)) {
            // (back [0]) close [4], return [5] (next [8])
            if(currentPage > 0) {
                // Place a back button
                view.addIcon(navIconBack(navStart));
            } else {
                view.addIcon(navIconEmpty(navStart));
            }
            if(currentPage < pages.size()-1) {
                // Place a next button
                view.addIcon(navIconNext(navStart+8));
            } else {
                view.addIcon(navIconEmpty(navStart+8));
            }
            view.addIcon(navIconEmpty(navStart+1));
            view.addIcon(navIconEmpty(navStart+2));
            view.addIcon(navIconEmpty(navStart+3));
            view.addIcon(navIconClose(navStart+4));
            view.addIcon(navIconReturn(navStart+5));
            view.addIcon(navIconEmpty(navStart+6));
            view.addIcon(navIconEmpty(navStart+7));
        }
        view.updateIcons();
    }

}
