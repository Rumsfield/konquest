package konquest.manager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import konquest.Konquest;
import konquest.command.CommandType;
import konquest.display.CommandIcon;
import konquest.display.DisplayMenu;
import konquest.display.InfoIcon;
import konquest.display.MenuIcon;
import konquest.display.PagedMenu;
import konquest.display.PlayerScoreIcon;
import konquest.display.UpgradeIcon;
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
						} else if(clickedIcon instanceof PlayerScoreIcon) {
							// Player Head Icons open a new score menu for the associated player
							PlayerScoreIcon icon = (PlayerScoreIcon)clickedIcon;
							KonOfflinePlayer offlinePlayer = konquest.getPlayerManager().getOfflinePlayer(icon.getOfflinePlayer());
							if(offlinePlayer != null) {
								menuCache.remove(inv);
								displayScoreMenu(clickPlayer, offlinePlayer);
								//ChatUtil.printDebug("Opened new score menu");
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
	 * Score Paged Menu
	 * ===============================================
	 */
	
	/**
	 * Displays a new score menu on the first page
	 * @param displayPlayer	The player to display the menu to
	 * @param scorePlayer	The player to use for scoring and stats
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
				PlayerScoreIcon leader = new PlayerScoreIcon(ChatColor.GOLD+"#"+rank+" "+kingdomColor+leaderboard.getName(n),Arrays.asList(loreColor+MessagePath.MENU_SCORE_PLAYER_SCORE.getMessage()+": "+ChatColor.DARK_PURPLE+leaderboard.getScore(n),ChatColor.GOLD+MessagePath.MENU_SCORE_HINT.getMessage()),leaderboard.getOfflinePlayer(n),n);
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
	
}
