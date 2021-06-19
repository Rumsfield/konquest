package konquest.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.command.CommandType;
import konquest.display.CommandIcon;
import konquest.display.DisplayMenu;
import konquest.display.InfoIcon;
import konquest.display.KingdomIcon;
import konquest.display.MenuIcon;
import konquest.display.PagedMenu;
import konquest.display.PlayerIcon;
import konquest.display.PlayerIcon.IconAction;
import konquest.display.TownIcon;
import konquest.display.UpgradeIcon;
import konquest.model.KonKingdom;
import konquest.model.KonKingdomScoreAttributes;
import konquest.model.KonKingdomScoreAttributes.KonKingdomScoreAttribute;
import konquest.model.KonLeaderboard;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.model.KonPlayerScoreAttributes;
import konquest.model.KonStats;
import konquest.model.KonStatsType;
import konquest.model.KonPlayerScoreAttributes.KonPlayerScoreAttribute;
import konquest.model.KonTown;
import konquest.model.KonUpgrade;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class DisplayManager {

	private Konquest konquest;
	private HashMap<Inventory, KonTown> townCache;
	private HashMap<Inventory, PagedMenu> menuCache;
	
	public DisplayManager(Konquest konquest) {
		this.konquest = konquest;
		this.townCache = new HashMap<Inventory, KonTown>();
		this.menuCache = new HashMap<Inventory, PagedMenu>();
	}
	
	public void initialize() {
		ChatUtil.printDebug("Display Manager is ready");
	}
	
	/*
	 * Common menu methods
	 */
	
	public boolean isDisplayMenu(Inventory inv) {
		boolean result = false;
		if(inv != null) {
			if(menuCache.containsKey(inv)) {
				result = true;
			}
		}
		return result;
	}
	
	public void onDisplayMenuClick(KonPlayer clickPlayer, Inventory inv, int slot) {
		// Switch pages and handle navigation button clicks
		// Open new score menus for leaderboard player clicks
		if(inv != null && menuCache.containsKey(inv)) {
			Player bukkitPlayer = clickPlayer.getBukkitPlayer();
			PagedMenu clickMenu = menuCache.get(inv);
			DisplayMenu currentPage = clickMenu.getPage(inv);
			if(currentPage != null) {
				int nextIndex = currentPage.getInventory().getSize()-1;
				int closeIndex = currentPage.getInventory().getSize()-5;
				int backIndex = currentPage.getInventory().getSize()-9;
				if(slot == nextIndex) {
					clickMenu.nextPageIndex();
					clickMenu.refreshCurrentPage();
					menuCache.remove(inv);
					if(townCache.containsKey(inv)) {
						KonTown town = townCache.remove(inv);
						townCache.put(clickMenu.getCurrentPage().getInventory(), town);
					}
					Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
			            @Override
			            public void run() {
			            	//bukkitPlayer.closeInventory();
			            	bukkitPlayer.openInventory(clickMenu.getCurrentPage().getInventory());
			            	menuCache.put(clickMenu.getCurrentPage().getInventory(), clickMenu);
			            }
			        },1);
					//ChatUtil.printDebug("Clicked page next button");
				} else if(slot == closeIndex) {
					menuCache.remove(inv);
					if(townCache.containsKey(inv)) {
						townCache.remove(inv);
					}
					Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
			            @Override
			            public void run() {
			            	bukkitPlayer.closeInventory();
			            }
			        },1);
					//ChatUtil.printDebug("Clicked page close button");
				} else if(slot == backIndex) {
					clickMenu.previousPageIndex();
					clickMenu.refreshCurrentPage();
					menuCache.remove(inv);
					if(townCache.containsKey(inv)) {
						KonTown town = townCache.remove(inv);
						townCache.put(clickMenu.getCurrentPage().getInventory(), town);
					}
					Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
			            @Override
			            public void run() {
			            	//bukkitPlayer.closeInventory();
			            	bukkitPlayer.openInventory(clickMenu.getCurrentPage().getInventory());
			            	menuCache.put(clickMenu.getCurrentPage().getInventory(), clickMenu);
			            }
			        },1);
					//ChatUtil.printDebug("Clicked page previous button");
				} else {
					// Clicked non-navigation slot
					MenuIcon clickedIcon = currentPage.getIcon(slot);
					if(clickedIcon != null && clickedIcon.isClickable()) {
						// The clicked slot is a clickable icon
						if(clickedIcon instanceof CommandIcon) {
							// Command Icons close the GUI and print a command in chat
							CommandIcon icon = (CommandIcon)clickedIcon;
							CommandType cmd = icon.getCommand();
							ChatUtil.sendNotice(bukkitPlayer, ChatColor.GOLD+"/k "+cmd.toString().toLowerCase()+" "+ChatColor.AQUA+cmd.arguments());
							Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
					            @Override
					            public void run() {
					            	bukkitPlayer.closeInventory();
					            }
					        });
						} else if(clickedIcon instanceof InfoIcon) {
							// Info Icons close the GUI and print their info in chat
							InfoIcon icon = (InfoIcon)clickedIcon;
							ChatUtil.sendNotice(bukkitPlayer, icon.getInfo());
							Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
					            @Override
					            public void run() {
					            	bukkitPlayer.closeInventory();
					            }
					        });
						} else if(clickedIcon instanceof PlayerIcon) {
							// Player Head Icons open a new score menu for the associated player
							PlayerIcon icon = (PlayerIcon)clickedIcon;
							KonOfflinePlayer offlinePlayer = konquest.getPlayerManager().getOfflinePlayer(icon.getOfflinePlayer());
							if(offlinePlayer != null) {
								menuCache.remove(inv);
								switch(icon.getAction()) {
									case DISPLAY_SCORE:
										displayScoreMenu(clickPlayer, offlinePlayer);
										break;
									case DISPLAY_INFO:
										displayPlayerInfoMenu(clickPlayer,offlinePlayer);
										break;
									default:
										break;
								}
							} else {
								ChatUtil.printDebug("Failed to find valid leaderboard offline player");
							}
						} else if(clickedIcon instanceof UpgradeIcon) {
							// Upgrade Icons close the GUI and attempt to apply an upgrade
							if(townCache.containsKey(inv)) {
								UpgradeIcon icon = (UpgradeIcon)clickedIcon;
								boolean status = konquest.getUpgradeManager().addTownUpgrade(townCache.get(inv), icon.getUpgrade(), icon.getLevel(), bukkitPlayer);
								if(status) {
									Bukkit.getWorld(konquest.getWorldName()).playSound(bukkitPlayer.getLocation(), Sound.BLOCK_ANVIL_USE, (float)1.0, (float)1.0);
								}
								Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
						            @Override
						            public void run() {
						            	bukkitPlayer.closeInventory();
						            }
						        });
							} else {
								ChatUtil.printDebug("Failed to find inventory menu in town cache");
							}
						} else if(clickedIcon instanceof KingdomIcon) {
							// Kingdom Icons open a new kingdom info menu for the associated player
							KingdomIcon icon = (KingdomIcon)clickedIcon;
							menuCache.remove(inv);
							displayKingdomInfoMenu(clickPlayer,icon.getKingdom());
						} else if(clickedIcon instanceof TownIcon) {
							// Town Icons open a new town info menu for the associated player
							TownIcon icon = (TownIcon)clickedIcon;
							menuCache.remove(inv);
							displayTownInfoMenu(clickPlayer,icon.getTown());
						}
					}
				}
			}
		}	
	}
	
	public void onDisplayMenuClose(Inventory inv) {
		if(menuCache.containsKey(inv)) {
			menuCache.remove(inv);
		}
		if(townCache.containsKey(inv)) {
			townCache.remove(inv);
		}
	}
	
	/*
	 * ===============================================
	 * Help Menu
	 * ===============================================
	 */
	public void displayHelpMenu(Player bukkitPlayer) {
		ChatUtil.printDebug("Displaying new help menu to "+bukkitPlayer.getName()+", current menu size is "+menuCache.size());
		int i = 0;
		int cost = 0;
		int cost_incr = 0;
		double cost_spy = konquest.getConfigManager().getConfig("core").getDouble("core.favor.cost_spy",0.0);
    	double cost_settle = konquest.getConfigManager().getConfig("core").getDouble("core.favor.cost_settle",0.0);
    	double cost_settle_incr = konquest.getConfigManager().getConfig("core").getDouble("core.favor.cost_settle_increment",0.0);
    	//double cost_rename = konquest.getConfigManager().getConfig("core").getDouble("core.favor.cost_rename",0.0);
    	double cost_claim = konquest.getConfigManager().getConfig("core").getDouble("core.favor.cost_claim",0.0);
    	double cost_travel = konquest.getConfigManager().getConfig("core").getDouble("core.favor.cost_travel",0.0);
    	String communityLink = konquest.getConfigManager().getConfig("core").getString("core.community_link","");
    	// Create new menu
    	PagedMenu newMenu = new PagedMenu();
		// Page 0
    	newMenu.addPage(0, (int)Math.ceil((CommandType.values().length+1)/9), ChatColor.BLACK+"Konquest Help");
    	// Add command icons
		for(CommandType cmd : CommandType.values()) {
			switch (cmd) {
				case SPY:
					cost = (int)cost_spy;
					cost_incr = 0;
					break;
				case SETTLE:
					cost = (int)cost_settle;
					cost_incr = (int)cost_settle_incr;
					break;
				case CLAIM:
					cost = (int)cost_claim;
					cost_incr = 0;
					break;
				case TRAVEL:
					cost = (int)cost_travel;
					cost_incr = 0;
					break;
				default:
					cost = 0;
					cost_incr = 0;
					break;
			} 
			CommandIcon icon = new CommandIcon(cmd, cost, cost_incr, i);
			newMenu.getPage(0).addIcon(icon);
			i++;
		}
		// Add info icons
		List<String> loreList = Arrays.asList(MessagePath.MENU_HELP_COMMUNITY.getMessage());
		InfoIcon info = new InfoIcon(MessagePath.MENU_HELP_COMMUNITY.getMessage(), loreList, Material.MINECART, i, true);
		if(communityLink == null) {
			communityLink = "";
		}
		info.setInfo(ChatColor.GOLD+MessagePath.MENU_HELP_HINT.getMessage()+": "+ChatColor.DARK_PURPLE+ChatColor.UNDERLINE+communityLink);
		newMenu.getPage(0).addIcon(info);
		i++;
		newMenu.refreshNavigationButtons();
		newMenu.setPageIndex(0);
		menuCache.put(newMenu.getCurrentPage().getInventory(), newMenu);
		// Display menu
		Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
            @Override
            public void run() {
            	bukkitPlayer.closeInventory();
            	bukkitPlayer.openInventory(newMenu.getCurrentPage().getInventory());
            }
        });
	}
	
	/*
	 * ===============================================
	 * Town Upgrade Menu
	 * ===============================================
	 */
	public void displayTownUpgradeMenu(Player bukkitPlayer, KonTown town) {
		/*
		// Flush any existing menus for this town
		if(townMenuCache.containsValue(town)) {
			for(Inventory inv : townMenuCache.keySet()) {
				if(town.equals(townMenuCache.get(inv))) {
					townMenuCache.remove(inv);
					townUpgradeMenus.remove(inv);
					break;
				}
			}
		}
		*/
		ChatUtil.printDebug("Displaying new upgrade menu to "+bukkitPlayer.getName()+", current menu size is "+menuCache.size()+", town size is "+townCache.size());
		// Create fresh menu
		//DisplayMenu townUpgradeMenu = new DisplayMenu(1,ChatColor.BLACK+MessagePath.MENU_UPGRADE_TITLE.getMessage());
		PagedMenu newMenu = new PagedMenu();
		// Page 0
		String pageLabel = ChatColor.BLACK+MessagePath.MENU_UPGRADE_TITLE.getMessage();
		newMenu.addPage(0, 1, pageLabel);
		HashMap<KonUpgrade,Integer> availableUpgrades = konquest.getUpgradeManager().getAvailableUpgrades(town);
		int index = 0;
		for(KonUpgrade upgrade : KonUpgrade.values()) {
			if(availableUpgrades.containsKey(upgrade)) {
				int cost = konquest.getUpgradeManager().getUpgradeCost(upgrade, availableUpgrades.get(upgrade));
				int pop = konquest.getUpgradeManager().getUpgradePopulation(upgrade, availableUpgrades.get(upgrade));
				UpgradeIcon icon = new UpgradeIcon(upgrade, availableUpgrades.get(upgrade), index, cost, pop);
				newMenu.getPage(0).addIcon(icon);
				index++;
			}
		}
		newMenu.refreshNavigationButtons();
		newMenu.setPageIndex(0);
		menuCache.put(newMenu.getCurrentPage().getInventory(), newMenu);
		//townUpgradeMenus.put(townUpgradeMenu.getInventory(), townUpgradeMenu);
		townCache.put(newMenu.getCurrentPage().getInventory(), town);
		//ChatUtil.printDebug("townUpgradeMenus is now size "+townUpgradeMenus.size()+", townMenuCache is "+townMenuCache.size());
		// Schedule delayed task to display inventory to player
		Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
            @Override
            public void run() {
            	bukkitPlayer.closeInventory();
            	bukkitPlayer.openInventory(newMenu.getCurrentPage().getInventory());
            }
        });
	}
	
	/*
	 * ===============================================
	 * Score Menu
	 * ===============================================
	 */
 	public void displayScoreMenu(KonPlayer displayPlayer, KonOfflinePlayer scorePlayer) {
 		ChatUtil.printDebug("Displaying new score menu to "+displayPlayer.getBukkitPlayer().getName()+" of player "+scorePlayer.getOfflineBukkitPlayer().getName()+", current menu size is "+menuCache.size());
 		KonPlayerScoreAttributes playerScoreAttributes = konquest.getKingdomManager().getPlayerScoreAttributes(scorePlayer);
		KonKingdomScoreAttributes kingdomScoreAttributes = konquest.getKingdomManager().getKingdomScoreAttributes(scorePlayer.getKingdom());
		int playerScore = playerScoreAttributes.getScore();
		int kingdomScore = kingdomScoreAttributes.getScore();
		Player bukkitPlayer = displayPlayer.getBukkitPlayer();
		String pageLabel = "";
		int i = 0;
		InfoIcon info;
		// Create fresh paged menu
		PagedMenu newMenu = new PagedMenu();
		String kingdomColor = ""+ChatColor.RED;
		if(displayPlayer.getKingdom().equals(scorePlayer.getKingdom())) {
			kingdomColor = ""+ChatColor.GREEN;
		}
		String loreColor = ""+ChatColor.WHITE;
		// Page 0
		pageLabel = ChatColor.BLACK+scorePlayer.getKingdom().getName()+" "+MessagePath.LABEL_SCORE.getMessage()+": "+kingdomScore;
		newMenu.addPage(0, 1, pageLabel);
		info = new InfoIcon(kingdomColor+scorePlayer.getKingdom().getName()+" "+MessagePath.MENU_SCORE_KINGDOM_SCORE.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+kingdomScore), Material.GOLDEN_HELMET, 0, false);
		newMenu.getPage(0).addIcon(info);
		info = new InfoIcon(kingdomColor+MessagePath.MENU_SCORE_KINGDOM_TOWNS.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+ChatColor.AQUA+kingdomScoreAttributes.getAttributeValue(KonKingdomScoreAttribute.TOWNS), loreColor+MessagePath.LABEL_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+kingdomScoreAttributes.getAttributeScore(KonKingdomScoreAttribute.TOWNS)), Material.OBSIDIAN, 1, false);
		newMenu.getPage(0).addIcon(info);
		info = new InfoIcon(kingdomColor+MessagePath.MENU_SCORE_KINGDOM_LAND.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+ChatColor.AQUA+kingdomScoreAttributes.getAttributeValue(KonKingdomScoreAttribute.LAND), loreColor+MessagePath.LABEL_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+kingdomScoreAttributes.getAttributeScore(KonKingdomScoreAttribute.LAND)), Material.GRASS_BLOCK, 2, false);
		newMenu.getPage(0).addIcon(info);
		info = new InfoIcon(kingdomColor+MessagePath.MENU_SCORE_KINGDOM_FAVOR.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+ChatColor.AQUA+kingdomScoreAttributes.getAttributeValue(KonKingdomScoreAttribute.FAVOR), loreColor+MessagePath.LABEL_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+kingdomScoreAttributes.getAttributeScore(KonKingdomScoreAttribute.FAVOR)), Material.GOLD_BLOCK, 3, false);
		newMenu.getPage(0).addIcon(info);
		info = new InfoIcon(kingdomColor+MessagePath.MENU_SCORE_KINGDOM_POPULATION.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+ChatColor.AQUA+kingdomScoreAttributes.getAttributeValue(KonKingdomScoreAttribute.POPULATION), loreColor+MessagePath.LABEL_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+kingdomScoreAttributes.getAttributeScore(KonKingdomScoreAttribute.POPULATION)), Material.PLAYER_HEAD, 4, false);
		newMenu.getPage(0).addIcon(info);
		// Page 1
		pageLabel = ChatColor.BLACK+scorePlayer.getOfflineBukkitPlayer().getName()+" "+MessagePath.LABEL_SCORE.getMessage()+": "+playerScore;
		newMenu.addPage(1, 1, pageLabel);
		info = new InfoIcon(kingdomColor+scorePlayer.getOfflineBukkitPlayer().getName()+" "+MessagePath.MENU_SCORE_PLAYER_SCORE.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+playerScore), Material.DIAMOND_HELMET, 0, false);
		newMenu.getPage(1).addIcon(info);
		info = new InfoIcon(kingdomColor+MessagePath.MENU_SCORE_TOWN_1.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+ChatColor.AQUA+playerScoreAttributes.getAttributeValue(KonPlayerScoreAttribute.TOWN_LORDS), loreColor+MessagePath.LABEL_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+playerScoreAttributes.getAttributeScore(KonPlayerScoreAttribute.TOWN_LORDS)), Material.PURPLE_CONCRETE, 1, false);
		newMenu.getPage(1).addIcon(info);
		info = new InfoIcon(kingdomColor+MessagePath.MENU_SCORE_LAND_1.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+ChatColor.AQUA+playerScoreAttributes.getAttributeValue(KonPlayerScoreAttribute.LAND_LORDS), loreColor+MessagePath.LABEL_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+playerScoreAttributes.getAttributeScore(KonPlayerScoreAttribute.LAND_LORDS)), Material.PURPLE_CARPET, 2, false);
		newMenu.getPage(1).addIcon(info);
		info = new InfoIcon(kingdomColor+MessagePath.MENU_SCORE_TOWN_2.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+ChatColor.AQUA+playerScoreAttributes.getAttributeValue(KonPlayerScoreAttribute.TOWN_KNIGHTS), loreColor+MessagePath.LABEL_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+playerScoreAttributes.getAttributeScore(KonPlayerScoreAttribute.TOWN_KNIGHTS)), Material.BLUE_CONCRETE, 3, false);
		newMenu.getPage(1).addIcon(info);
		info = new InfoIcon(kingdomColor+MessagePath.MENU_SCORE_LAND_2.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+ChatColor.AQUA+playerScoreAttributes.getAttributeValue(KonPlayerScoreAttribute.LAND_KNIGHTS), loreColor+MessagePath.LABEL_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+playerScoreAttributes.getAttributeScore(KonPlayerScoreAttribute.LAND_KNIGHTS)), Material.BLUE_CARPET, 4, false);
		newMenu.getPage(1).addIcon(info);
		info = new InfoIcon(kingdomColor+MessagePath.MENU_SCORE_TOWN_3.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+ChatColor.AQUA+playerScoreAttributes.getAttributeValue(KonPlayerScoreAttribute.TOWN_RESIDENTS), loreColor+MessagePath.LABEL_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+playerScoreAttributes.getAttributeScore(KonPlayerScoreAttribute.TOWN_RESIDENTS)), Material.WHITE_CONCRETE, 5, false);
		newMenu.getPage(1).addIcon(info);
		info = new InfoIcon(kingdomColor+MessagePath.MENU_SCORE_LAND_3.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+ChatColor.AQUA+playerScoreAttributes.getAttributeValue(KonPlayerScoreAttribute.LAND_RESIDENTS), loreColor+MessagePath.LABEL_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+playerScoreAttributes.getAttributeScore(KonPlayerScoreAttribute.LAND_RESIDENTS)), Material.WHITE_CARPET, 6, false);
		newMenu.getPage(1).addIcon(info);
		// Page 2
		pageLabel = ChatColor.BLACK+scorePlayer.getOfflineBukkitPlayer().getName()+" "+MessagePath.LABEL_STATS.getMessage();
		newMenu.addPage(2, 3, pageLabel);
		KonPlayer player = konquest.getPlayerManager().getPlayerFromName(scorePlayer.getOfflineBukkitPlayer().getName());
    	boolean isPlayerOnline = false;
    	KonStats stats;
    	if(player == null) {
    		// Use offline player, pull stats from DB
    		stats = konquest.getDatabaseThread().getDatabase().pullPlayerStats(scorePlayer.getOfflineBukkitPlayer());
    	} else {
    		// Use online player's active stats
    		stats = player.getPlayerStats();
    		isPlayerOnline = true;
    	}
    	i = 0;
    	int statValue = 0;
    	for(KonStatsType stat : KonStatsType.values()) {
    		statValue = stats.getStat(stat);
    		info = new InfoIcon(ChatColor.GOLD+stat.displayName(),Arrays.asList(loreColor+stat.description(),ChatColor.AQUA+""+statValue),stat.getMaterial(),i,false);
    		newMenu.getPage(2).addIcon(info);
    		i++;
    	}
    	if(!isPlayerOnline) {
    		stats = null;
    	}
		// Page 3
		pageLabel = ChatColor.BLACK+scorePlayer.getKingdom().getName()+" "+MessagePath.LABEL_LEADERBOARD.getMessage();
		newMenu.addPage(3, 1, pageLabel);
		KonLeaderboard leaderboard = konquest.getKingdomManager().getKingdomLeaderboard(scorePlayer.getKingdom());
		if(!leaderboard.isEmpty()) {
			int numEntries = 9;
			if(leaderboard.getSize() < numEntries) {
				numEntries = leaderboard.getSize();
			}
			for(int n = 0;n<numEntries;n++) {
				int rank = n + 1;
				PlayerIcon leader = new PlayerIcon(ChatColor.GOLD+"#"+rank+" "+kingdomColor+leaderboard.getName(n),Arrays.asList(loreColor+MessagePath.MENU_SCORE_PLAYER_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+leaderboard.getScore(n),ChatColor.GOLD+MessagePath.MENU_SCORE_HINT.getMessage()),leaderboard.getOfflinePlayer(n),n,true,IconAction.DISPLAY_SCORE);
				newMenu.getPage(3).addIcon(leader);
			}
		}
		newMenu.refreshNavigationButtons();
		newMenu.setPageIndex(0);
		menuCache.put(newMenu.getCurrentPage().getInventory(), newMenu);
		// Schedule delayed task to display inventory to player
		Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
            @Override
            public void run() {
            	bukkitPlayer.closeInventory();
            	bukkitPlayer.openInventory(newMenu.getCurrentPage().getInventory());
            }
        },1);
	}
	
 	/*
	 * ===============================================
	 * Info Menus
	 * ===============================================
	 */
 	// Player Info
 	public void displayPlayerInfoMenu(KonPlayer displayPlayer, KonOfflinePlayer infoPlayer) {
 		
 		boolean isFriendly = displayPlayer.getKingdom().equals(infoPlayer.getKingdom());
 		ChatColor kingdomColor = Konquest.getContextColor(displayPlayer, infoPlayer);
 		
		String loreColor = ""+ChatColor.WHITE;
		String valueColor = ""+ChatColor.AQUA;
		String pageLabel = "";
    	
 		// Create fresh paged menu
 		PagedMenu newMenu = new PagedMenu();
 		List<String> loreList;

 		// Page 0
		pageLabel = ChatColor.BLACK+MessagePath.LABEL_PLAYER.getMessage()+" "+infoPlayer.getOfflineBukkitPlayer().getName();
		newMenu.addPage(0, 1, pageLabel);
		/* Kingdom Icon (3) */
		int numKingdomPlayers = konquest.getPlayerManager().getPlayersInKingdom(infoPlayer.getKingdom()).size();
    	int numAllKingdomPlayers = konquest.getPlayerManager().getAllPlayersInKingdom(infoPlayer.getKingdom()).size();
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_PLAYERS.getMessage()+": "+valueColor+numKingdomPlayers+"/"+numAllKingdomPlayers);
    	if(infoPlayer.getKingdom().isOfflineProtected()) {
    		loreList.add(ChatColor.LIGHT_PURPLE+""+ChatColor.ITALIC+MessagePath.LABEL_PROTECTED.getMessage());
    	}
    	loreList.add(ChatColor.GOLD+MessagePath.MENU_SCORE_HINT.getMessage());
    	KingdomIcon kingdom = new KingdomIcon(infoPlayer.getKingdom(),kingdomColor,Material.GOLDEN_HELMET,loreList,3);
		newMenu.getPage(0).addIcon(kingdom);
		/* Player Score Icon (4) */
		int score = konquest.getKingdomManager().getPlayerScore(infoPlayer);
		loreList = new ArrayList<String>();
		loreList.add(loreColor+MessagePath.MENU_SCORE_PLAYER_SCORE.getMessage()+": "+valueColor+score);
		loreList.add(ChatColor.GOLD+MessagePath.MENU_SCORE_HINT.getMessage());
		PlayerIcon playerScore = new PlayerIcon(kingdomColor+infoPlayer.getOfflineBukkitPlayer().getName(),loreList,infoPlayer.getOfflineBukkitPlayer(),4,true,IconAction.DISPLAY_SCORE);
		newMenu.getPage(0).addIcon(playerScore);
		/* Favor Info Icon (5) */
		String balanceF = String.format("%.2f",KonquestPlugin.getEconomy().getBalance(infoPlayer.getOfflineBukkitPlayer()));
		InfoIcon info = new InfoIcon(kingdomColor+MessagePath.LABEL_FAVOR.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_FAVOR.getMessage()+": "+valueColor+balanceF), Material.GOLD_INGOT, 5, false);
		newMenu.getPage(0).addIcon(info);
		
		// Page 1+
		List<KonTown> playerTowns = sortedTowns(infoPlayer);
		final int MAX_ICONS_PER_PAGE = 45;
		int pageTotal = (int)Math.ceil(((double)playerTowns.size())/MAX_ICONS_PER_PAGE);
		if(pageTotal == 0) {
			pageTotal = 1;
		}
		int pageNum = 1;
		ListIterator<KonTown> townIter = playerTowns.listIterator();
		for(int i = 0; i < pageTotal; i++) {
			int numPageRows = (int)Math.ceil(((double)((playerTowns.size() - i*MAX_ICONS_PER_PAGE) % MAX_ICONS_PER_PAGE))/9);
			pageLabel = ChatColor.BLACK+infoPlayer.getOfflineBukkitPlayer().getName()+" "+MessagePath.LABEL_RESIDENCIES.getMessage()+" "+(i+1)+"/"+pageTotal;
			newMenu.addPage(pageNum, numPageRows, pageLabel);
			int slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && townIter.hasNext()) {
				/* Town Icon (n) */
				KonTown currentTown = townIter.next();
				loreList = new ArrayList<String>();
				Material currentMat = Material.DIRT;
				if(currentTown.isPlayerLord(infoPlayer.getOfflineBukkitPlayer())) {
					currentMat = Material.PURPLE_CONCRETE;
					loreList.add(ChatColor.DARK_PURPLE+MessagePath.LABEL_LORD.getMessage());
				} else if(currentTown.isPlayerElite(infoPlayer.getOfflineBukkitPlayer())) {
					currentMat = Material.BLUE_CONCRETE;
					loreList.add(ChatColor.DARK_BLUE+MessagePath.LABEL_KNIGHT.getMessage());
				} else {
					currentMat = Material.WHITE_CONCRETE;
					loreList.add(loreColor+MessagePath.LABEL_RESIDENT.getMessage());
				}
		    	loreList.add(loreColor+MessagePath.LABEL_POPULATION.getMessage()+": "+valueColor+currentTown.getNumResidents());
		    	loreList.add(loreColor+MessagePath.LABEL_LAND.getMessage()+": "+valueColor+currentTown.getChunkList().size());
		    	loreList.add(ChatColor.GOLD+MessagePath.MENU_SCORE_HINT.getMessage());
		    	TownIcon town = new TownIcon(currentTown,isFriendly,currentMat,loreList,slotIndex);
				newMenu.getPage(pageNum).addIcon(town);
				slotIndex++;
			}
			pageNum++;
		}
		
		newMenu.refreshNavigationButtons();
		newMenu.setPageIndex(0);
		menuCache.put(newMenu.getCurrentPage().getInventory(), newMenu);
		// Schedule delayed task to display inventory to player
		Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
            @Override
            public void run() {
            	displayPlayer.getBukkitPlayer().closeInventory();
            	displayPlayer.getBukkitPlayer().openInventory(newMenu.getCurrentPage().getInventory());
            }
        },1);
 	}
 	
 	// Kingdom Info
  	public void displayKingdomInfoMenu(KonPlayer displayPlayer, KonKingdom infoKingdom) {
  		
  		boolean isFriendly = displayPlayer.getKingdom().equals(infoKingdom);
 		ChatColor kingdomColor = ChatColor.RED;
 		if(isFriendly) {
 			kingdomColor = ChatColor.GREEN;
 		}
 		
		String loreColor = ""+ChatColor.WHITE;
		String valueColor = ""+ChatColor.AQUA;
		String pageLabel = "";
    	
 		// Create fresh paged menu
 		PagedMenu newMenu = new PagedMenu();
 		List<String> loreList;
 		InfoIcon info;

 		// Page 0
		pageLabel = ChatColor.BLACK+MessagePath.COMMAND_INFO_NOTICE_KINGDOM_HEADER.getMessage(infoKingdom.getName());
		newMenu.addPage(0, 1, pageLabel);
		/* Member Info Icon (2) */
		int numKingdomPlayers = konquest.getPlayerManager().getPlayersInKingdom(infoKingdom).size();
    	int numAllKingdomPlayers = konquest.getPlayerManager().getAllPlayersInKingdom(infoKingdom).size();
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_ONLINE_PLAYERS.getMessage()+": "+valueColor+numKingdomPlayers);
    	loreList.add(loreColor+MessagePath.LABEL_TOTAL_PLAYERS.getMessage()+": "+valueColor+numAllKingdomPlayers);
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_PLAYERS.getMessage(), loreList, Material.PLAYER_HEAD, 2, false);
    	newMenu.getPage(0).addIcon(info);
    	/* Properties Info Icon (3) */
    	String isPeaceful = boolean2Symbol(infoKingdom.isPeaceful());
    	String isSmallest = boolean2Symbol(infoKingdom.isSmallest());
    	String isProtected = boolean2Symbol(infoKingdom.isOfflineProtected());
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_PEACEFUL.getMessage()+": "+isPeaceful);
    	loreList.add(loreColor+MessagePath.LABEL_SMALLEST.getMessage()+": "+isSmallest);
    	loreList.add(loreColor+MessagePath.LABEL_PROTECTED.getMessage()+": "+isProtected);
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_PROPERTIES.getMessage(), loreList, Material.PAPER, 3, false);
    	newMenu.getPage(0).addIcon(info);
    	/* Favor Info Icon (4) */
    	ArrayList<KonOfflinePlayer> allPlayersInKingdom = konquest.getPlayerManager().getAllPlayersInKingdom(infoKingdom);
    	int numKingdomFavor = 0;
    	for(KonOfflinePlayer kingdomPlayer : allPlayersInKingdom) {
    		numKingdomFavor += (int) KonquestPlugin.getEconomy().getBalance(kingdomPlayer.getOfflineBukkitPlayer());
    	}
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+numKingdomFavor);
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_FAVOR.getMessage(), loreList, Material.GOLD_BLOCK, 4, false);
    	newMenu.getPage(0).addIcon(info);
    	/* Towns Info Icon (5) */
    	int numKingdomTowns = infoKingdom.getTowns().size();
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+numKingdomTowns);
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_TOWNS.getMessage(), loreList, Material.OBSIDIAN, 5, false);
    	newMenu.getPage(0).addIcon(info);
    	/* Towns Info Icon (6) */
    	int numKingdomLand = 0;
    	for(KonTown town : infoKingdom.getTowns()) {
    		numKingdomLand += town.getChunkList().size();
    	}
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+numKingdomLand);
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_LAND.getMessage(), loreList, Material.GRASS_BLOCK, 6, false);
    	newMenu.getPage(0).addIcon(info);
    	
    	// Page 1+
		List<KonTown> kingdomTowns = sortedTowns(infoKingdom);
		final int MAX_ICONS_PER_PAGE = 45;
		int pageTotal = (int)Math.ceil(((double)kingdomTowns.size())/MAX_ICONS_PER_PAGE);
		if(pageTotal == 0) {
			pageTotal = 1;
		}
		int pageNum = 1;
		ListIterator<KonTown> townIter = kingdomTowns.listIterator();
		for(int i = 0; i < pageTotal; i++) {
			int numPageRows = (int)Math.ceil(((double)((kingdomTowns.size() - i*MAX_ICONS_PER_PAGE) % MAX_ICONS_PER_PAGE))/9);
			pageLabel = ChatColor.BLACK+infoKingdom.getName()+" "+MessagePath.LABEL_TOWNS.getMessage()+" "+(i+1)+"/"+pageTotal;
			newMenu.addPage(pageNum, numPageRows, pageLabel);
			int slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && townIter.hasNext()) {
				/* Town Icon (n) */
				KonTown currentTown = townIter.next();
				loreList = new ArrayList<String>();
				Material currentMat = Material.BEDROCK;
				if(currentTown.isAttacked()) {
					currentMat = Material.RED_WOOL;
					loreList.add(loreColor+ChatColor.ITALIC+MessagePath.PROTECTION_NOTICE_ATTACKED.getMessage());
				} else if(currentTown.isOpen()) {
					currentMat = Material.LIGHT_GRAY_WOOL;
					loreList.add(loreColor+ChatColor.ITALIC+MessagePath.LABEL_OPEN.getMessage());
				}
		    	loreList.add(loreColor+MessagePath.LABEL_POPULATION.getMessage()+": "+valueColor+currentTown.getNumResidents());
		    	loreList.add(loreColor+MessagePath.LABEL_LAND.getMessage()+": "+valueColor+currentTown.getChunkList().size());
		    	loreList.add(ChatColor.GOLD+MessagePath.MENU_SCORE_HINT.getMessage());
		    	TownIcon town = new TownIcon(currentTown,isFriendly,currentMat,loreList,slotIndex);
				newMenu.getPage(pageNum).addIcon(town);
				slotIndex++;
			}
			pageNum++;
		}
		
		newMenu.refreshNavigationButtons();
		newMenu.setPageIndex(0);
		menuCache.put(newMenu.getCurrentPage().getInventory(), newMenu);
		// Schedule delayed task to display inventory to player
		Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
            @Override
            public void run() {
            	displayPlayer.getBukkitPlayer().closeInventory();
            	displayPlayer.getBukkitPlayer().openInventory(newMenu.getCurrentPage().getInventory());
            }
        },1);
  	}
 	
  	// Kingdom Info
   	public void displayTownInfoMenu(KonPlayer displayPlayer, KonTown infoTown) {
   		
   		boolean isFriendly = displayPlayer.getKingdom().equals(infoTown.getKingdom());
 		ChatColor kingdomColor = ChatColor.RED;
 		if(isFriendly) {
 			kingdomColor = ChatColor.GREEN;
 		}
 		
		String loreColor = ""+ChatColor.WHITE;
		String valueColor = ""+ChatColor.AQUA;
		String pageLabel = "";
		
		List<OfflinePlayer> townKnights = new ArrayList<OfflinePlayer>();
		List<OfflinePlayer> townResidents = new ArrayList<OfflinePlayer>();
		for(OfflinePlayer resident : infoTown.getPlayerResidents()) {
			if(!infoTown.isPlayerLord(resident)) {
				if(infoTown.isPlayerElite(resident)) {
					townKnights.add(resident);
				} else {
					townResidents.add(resident);
				}
			}
		}
    	
 		// Create fresh paged menu
 		PagedMenu newMenu = new PagedMenu();
 		List<String> loreList;
 		InfoIcon info;
 		final int MAX_ICONS_PER_PAGE = 45;
		int pageTotal = 1;

 		// Page 0
		pageLabel = ChatColor.BLACK+MessagePath.COMMAND_INFO_NOTICE_TOWN_HEADER.getMessage(infoTown.getName());
		newMenu.addPage(0, 1, pageLabel);
		/* Kingdom Info Icon (0) */
		int numKingdomPlayers = konquest.getPlayerManager().getPlayersInKingdom(infoTown.getKingdom()).size();
    	int numAllKingdomPlayers = konquest.getPlayerManager().getAllPlayersInKingdom(infoTown.getKingdom()).size();
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_PLAYERS.getMessage()+": "+valueColor+numKingdomPlayers+"/"+numAllKingdomPlayers);
    	if(infoTown.getKingdom().isOfflineProtected()) {
    		loreList.add(ChatColor.LIGHT_PURPLE+""+ChatColor.ITALIC+MessagePath.LABEL_PROTECTED.getMessage());
    	}
    	loreList.add(ChatColor.GOLD+MessagePath.MENU_SCORE_HINT.getMessage());
    	KingdomIcon kingdom = new KingdomIcon(infoTown.getKingdom(),kingdomColor,Material.GOLDEN_HELMET,loreList,0);
		newMenu.getPage(0).addIcon(kingdom);
		/* Lord Player Info Icon (1) */
		loreList = new ArrayList<String>();
		if(infoTown.isLordValid()) {
			OfflinePlayer lordPlayer = infoTown.getPlayerLord();
			loreList.add(ChatColor.DARK_PURPLE+MessagePath.LABEL_LORD.getMessage());
			PlayerIcon playerInfo = new PlayerIcon(kingdomColor+lordPlayer.getName(),loreList,lordPlayer,1,true,IconAction.DISPLAY_INFO);
			newMenu.getPage(0).addIcon(playerInfo);
		} else {
			for(String line : Konquest.stringPaginate(MessagePath.COMMAND_TOWN_NOTICE_NO_LORD.getMessage(infoTown.getName(), infoTown.getName(), displayPlayer.getBukkitPlayer().getName()))) {
				loreList.add(ChatColor.RED+line);
			}
			info = new InfoIcon(ChatColor.DARK_PURPLE+MessagePath.LABEL_LORD.getMessage(),loreList,Material.BARRIER,1,false);
			newMenu.getPage(0).addIcon(info);
		}
		/* Invites Info Icon (2) */
		loreList = new ArrayList<String>();
		for(OfflinePlayer invitee : infoTown.getJoinInvites()) {
			loreList.add(loreColor+invitee.getName());
		}
		info = new InfoIcon(kingdomColor+MessagePath.LABEL_INVITES.getMessage(),loreList,Material.DIAMOND,2,false);
		newMenu.getPage(0).addIcon(info);
		/* Requests Info Icon (3) */
		loreList = new ArrayList<String>();
		for(OfflinePlayer requestee : infoTown.getJoinRequests()) {
			loreList.add(loreColor+requestee.getName());
		}
		info = new InfoIcon(kingdomColor+MessagePath.LABEL_REQUESTS.getMessage(),loreList,Material.EMERALD,3,false);
		newMenu.getPage(0).addIcon(info);
		/* Properties Info Icon (5) */
    	String isOpen = boolean2Symbol(infoTown.isOpen());
    	String isProtected = boolean2Symbol(infoTown.isCaptureDisabled());
    	String isAttacked = boolean2Symbol(infoTown.isAttacked());
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_OPEN.getMessage()+": "+isOpen);
    	loreList.add(loreColor+MessagePath.PROTECTION_NOTICE_ATTACKED.getMessage()+": "+isAttacked);
    	loreList.add(loreColor+MessagePath.LABEL_PROTECTED.getMessage()+": "+isProtected);
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_PROPERTIES.getMessage(), loreList, Material.PAPER, 5, false);
    	newMenu.getPage(0).addIcon(info);
    	/* Health Info Icon (6) */
    	int maxCriticalhits = konquest.getConfigManager().getConfig("core").getInt("core.monuments.destroy_amount");
		int townHealth = maxCriticalhits - infoTown.getMonument().getCriticalHits();
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+townHealth+"/"+maxCriticalhits);
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_HEALTH.getMessage(), loreList, Material.GOLDEN_APPLE, 6, false);
    	newMenu.getPage(0).addIcon(info);
    	/* Land Info Icon (7) */
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+infoTown.getChunkList().size());
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_LAND.getMessage(), loreList, Material.GRASS_BLOCK, 7, false);
    	newMenu.getPage(0).addIcon(info);
    	/* Population Info Icon (8) */
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+infoTown.getNumResidents());
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_POPULATION.getMessage(), loreList, Material.WHITE_BED, 8, false);
    	newMenu.getPage(0).addIcon(info);
    	
    	// Page 1
		pageLabel = ChatColor.BLACK+infoTown.getName()+" "+MessagePath.LABEL_UPGRADES.getMessage();
		newMenu.addPage(1, 1, pageLabel);
		int index = 0;
		for(KonUpgrade upgrade : KonUpgrade.values()) {
			int currentLevel = infoTown.getRawUpgradeLevel(upgrade);
			if(currentLevel > 0) {
				String formattedUpgrade = ChatColor.LIGHT_PURPLE+upgrade.getDescription()+" "+currentLevel;
				int level = currentLevel;
				if(infoTown.isUpgradeDisabled(upgrade)) {
					int reducedLevel = infoTown.getUpgradeLevel(upgrade);
					level = reducedLevel;
					if(reducedLevel > 0) {
						formattedUpgrade = ChatColor.LIGHT_PURPLE+upgrade.getDescription()+" "+ChatColor.GRAY+reducedLevel;
					} else {
						formattedUpgrade = ChatColor.GRAY+""+ChatColor.STRIKETHROUGH+upgrade.getDescription()+" "+reducedLevel;
					}
				}
				loreList = new ArrayList<String>();
				for(String line : Konquest.stringPaginate(upgrade.getLevelDescription(level))) {
					loreList.add(ChatColor.RED+line);
				}
				// Create info icon with upgrade info
				info = new InfoIcon(formattedUpgrade, loreList, upgrade.getIcon(), index, false);
				newMenu.getPage(1).addIcon(info);
				index++;
			}
		}

		// Page 2+
		int pageNum = 2;
		pageTotal = (int)Math.ceil(((double)townKnights.size())/MAX_ICONS_PER_PAGE);
		if(pageTotal == 0) {
			pageTotal = 1;
		}
		ListIterator<OfflinePlayer> knightIter = townKnights.listIterator();
		for(int i = 0; i < pageTotal; i++) {
			int numPageRows = (int)Math.ceil(((double)((townKnights.size() - i*MAX_ICONS_PER_PAGE) % MAX_ICONS_PER_PAGE))/9);
			pageLabel = ChatColor.BLACK+infoTown.getName()+" "+MessagePath.LABEL_KNIGHTS.getMessage()+" "+(i+1)+"/"+pageTotal;
			newMenu.addPage(pageNum, numPageRows, pageLabel);
			int slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && knightIter.hasNext()) {
				/* Player Icon (n) */
				OfflinePlayer currentKnight = knightIter.next();
				loreList = new ArrayList<String>();
				loreList.add(ChatColor.DARK_BLUE+MessagePath.LABEL_KNIGHT.getMessage());
		    	loreList.add(ChatColor.GOLD+MessagePath.MENU_SCORE_HINT.getMessage());
		    	PlayerIcon player = new PlayerIcon(kingdomColor+currentKnight.getName(),loreList,currentKnight,slotIndex,true,IconAction.DISPLAY_INFO);
				newMenu.getPage(pageNum).addIcon(player);
				slotIndex++;
			}
			pageNum++;
		}
		
		// Page 3+
		pageTotal = (int)Math.ceil(((double)townResidents.size())/MAX_ICONS_PER_PAGE);
		if(pageTotal == 0) {
			pageTotal = 1;
		}
		ListIterator<OfflinePlayer> residentIter = townResidents.listIterator();
		for(int i = 0; i < pageTotal; i++) {
			int numPageRows = (int)Math.ceil(((double)((townResidents.size() - i*MAX_ICONS_PER_PAGE) % MAX_ICONS_PER_PAGE))/9);
			pageLabel = ChatColor.BLACK+infoTown.getName()+" "+MessagePath.LABEL_RESIDENTS.getMessage()+" "+(i+1)+"/"+pageTotal;
			newMenu.addPage(pageNum, numPageRows, pageLabel);
			int slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && residentIter.hasNext()) {
				/* Player Icon (n) */
				OfflinePlayer currentResident = residentIter.next();
				loreList = new ArrayList<String>();
				loreList.add(ChatColor.WHITE+MessagePath.LABEL_RESIDENT.getMessage());
		    	loreList.add(ChatColor.GOLD+MessagePath.MENU_SCORE_HINT.getMessage());
		    	PlayerIcon player = new PlayerIcon(kingdomColor+currentResident.getName(),loreList,currentResident,slotIndex,true,IconAction.DISPLAY_INFO);
				newMenu.getPage(pageNum).addIcon(player);
				slotIndex++;
			}
			pageNum++;
		}
		
		newMenu.refreshNavigationButtons();
		newMenu.setPageIndex(0);
		menuCache.put(newMenu.getCurrentPage().getInventory(), newMenu);
		// Schedule delayed task to display inventory to player
		Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
            @Override
            public void run() {
            	displayPlayer.getBukkitPlayer().closeInventory();
            	displayPlayer.getBukkitPlayer().openInventory(newMenu.getCurrentPage().getInventory());
            }
        },1);
   	}
   	
   	/*
	 * Helper methods
	 */
  	
 	// Sort player town list by Lord, Knight, Resident, and then by population
 	private List<KonTown> sortedTowns(KonOfflinePlayer player) {
 		List<KonTown> sortedTowns = new ArrayList<KonTown>();
 		// Determine town group lists
 		List<KonTown> lordTowns = new ArrayList<KonTown>();
 		List<KonTown> knightTowns = new ArrayList<KonTown>();
 		List<KonTown> residentTowns = new ArrayList<KonTown>();
 		for(KonTown town : konquest.getKingdomManager().getPlayerResidenceTowns(player)) {
 			if(town.isPlayerLord(player.getOfflineBukkitPlayer())) {
 				lordTowns.add(town);
 			} else if(town.isPlayerElite(player.getOfflineBukkitPlayer())) {
 				knightTowns.add(town);
 			} else {
 				residentTowns.add(town);
 			}
 		}
 		// Sort each town list by population
 		Comparator<KonTown> townComparator = new Comparator<KonTown>() {
 			@Override
 			public int compare(final KonTown k1, KonTown k2) {
 				int result = 0;
 				if(k1.getNumResidents() < k2.getNumResidents()) {
 					result = 1;
 				} else if(k1.getNumResidents() > k2.getNumResidents()) {
 					result = -1;
 				}
 				return result;
 			}
 		};
 		Collections.sort(lordTowns, townComparator);
 		Collections.sort(knightTowns, townComparator);
 		Collections.sort(residentTowns, townComparator);
 		// Add sorted towns to result list
 		sortedTowns.addAll(lordTowns);
 		sortedTowns.addAll(knightTowns);
 		sortedTowns.addAll(residentTowns);
 		
 		return sortedTowns;
 	}
 	
 	// Sort kingdom town list by population
  	private List<KonTown> sortedTowns(KonKingdom kingdom) {
  		List<KonTown> sortedTowns = kingdom.getTowns();

  		// Sort each town list by population
  		Comparator<KonTown> townComparator = new Comparator<KonTown>() {
  			@Override
  			public int compare(final KonTown k1, KonTown k2) {
  				int result = 0;
  				if(k1.getNumResidents() < k2.getNumResidents()) {
  					result = 1;
  				} else if(k1.getNumResidents() > k2.getNumResidents()) {
  					result = -1;
  				}
  				return result;
  			}
  		};
  		Collections.sort(sortedTowns, townComparator);
  		
  		return sortedTowns;
  	}
 	
 	private String boolean2Symbol(boolean val) {
 		String result = ChatColor.DARK_RED+""+ChatColor.BOLD+"\u274C";
    	if(val) {
    		result = ChatColor.DARK_GREEN+""+ChatColor.BOLD+"\u2713";
    	}
    	return result;
 	}
}
