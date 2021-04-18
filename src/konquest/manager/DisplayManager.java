package konquest.manager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import konquest.Konquest;
import konquest.command.CommandType;
import konquest.display.CommandIcon;
import konquest.display.DisplayMenu;
import konquest.display.InfoIcon;
import konquest.display.MenuIcon;
import konquest.display.PagedMenu;
import konquest.display.PlayerHeadIcon;
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

public class DisplayManager {

	private Konquest konquest;
	private DisplayMenu helpMenu;
	private HashMap<Inventory, DisplayMenu> townUpgradeMenus;
	private HashMap<Inventory, KonTown> townMenuCache;
	private HashMap<Inventory, PagedMenu> scoreMenus;
	
	public DisplayManager(Konquest konquest) {
		this.konquest = konquest;
		this.helpMenu = new DisplayMenu(3, ChatColor.BLACK+"Konquest Help");
		this.townUpgradeMenus = new HashMap<Inventory, DisplayMenu>();
		this.townMenuCache = new HashMap<Inventory, KonTown>();
		this.scoreMenus = new HashMap<Inventory, PagedMenu>();
	}
	
	public void initialize() {
		populateHelpMenu();
		ChatUtil.printDebug("Display Manager is ready");
	}
	
	public void displayTownUpgradeMenu(Player bukkitPlayer, KonTown town) {
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
		// Create fresh menu
		DisplayMenu townUpgradeMenu = new DisplayMenu(1,ChatColor.BLACK+"Available Town Upgrades");
		HashMap<KonUpgrade,Integer> availableUpgrades = konquest.getUpgradeManager().getAvailableUpgrades(town);
		int index = 0;
		for(KonUpgrade upgrade : KonUpgrade.values()) {
			if(availableUpgrades.containsKey(upgrade)) {
				int cost = konquest.getUpgradeManager().getUpgradeCost(upgrade, availableUpgrades.get(upgrade));
				int pop = konquest.getUpgradeManager().getUpgradePopulation(upgrade, availableUpgrades.get(upgrade));
				UpgradeIcon icon = new UpgradeIcon(upgrade, availableUpgrades.get(upgrade), index, cost, pop);
				townUpgradeMenu.addIcon(icon);
				index++;
			}
		}
		townUpgradeMenus.put(townUpgradeMenu.getInventory(), townUpgradeMenu);
		townMenuCache.put(townUpgradeMenu.getInventory(), town);
		//ChatUtil.printDebug("townUpgradeMenus is now size "+townUpgradeMenus.size()+", townMenuCache is "+townMenuCache.size());
		// Schedule delayed task to display inventory to player
		Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
            @Override
            public void run() {
            	bukkitPlayer.closeInventory();
            	bukkitPlayer.openInventory(townUpgradeMenu.getInventory());
            }
        });
	}
	
	public boolean isTownUpgradeMenu(Inventory inv) {
		boolean result = false;
		if(townMenuCache.containsKey(inv)) {
			result = true;
		}
		return result;
	}
	
	public boolean onTownUpgradeMenuClick(Player bukkitPlayer, Inventory inv, int slot) {
		boolean result = false;
		if(townMenuCache.containsKey(inv)) {
			MenuIcon clickedIcon = townUpgradeMenus.get(inv).getIcon(slot);
			if(clickedIcon != null && clickedIcon instanceof UpgradeIcon) {
				UpgradeIcon icon = (UpgradeIcon)clickedIcon;
				result = konquest.getUpgradeManager().addTownUpgrade(townMenuCache.get(inv), icon.getUpgrade(), icon.getLevel(), bukkitPlayer);
				Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
		            @Override
		            public void run() {
		            	bukkitPlayer.closeInventory();
		            }
		        });
			}
		}
		return result;
	}
	
	public void displayHelpMenu(Player bukkitPlayer) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
            @Override
            public void run() {
            	bukkitPlayer.closeInventory();
            	bukkitPlayer.openInventory(helpMenu.getInventory());
            }
        });
	}
	
	public void sendHelpContext(Player bukkitPlayer, int slot) {
		String message = "";
		MenuIcon clickedIcon = helpMenu.getIcon(slot);
		boolean isValidSlot = false;
		if(clickedIcon != null) {
			if(clickedIcon instanceof CommandIcon) {
				CommandIcon icon = (CommandIcon)clickedIcon;
				CommandType cmd = icon.getCommand();
				message = ChatColor.GRAY+"Type "+ChatColor.GOLD+"/k "+cmd.toString().toLowerCase()+" "+ChatColor.AQUA+cmd.arguments();
				isValidSlot = true;
			} else if(clickedIcon instanceof InfoIcon) {
				InfoIcon icon = (InfoIcon)clickedIcon;
				String text = icon.getInfo();
				message = text;
				isValidSlot = true;
			}
		}
		if(isValidSlot) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
	            @Override
	            public void run() {
	            	bukkitPlayer.closeInventory();
	            }
	        });
			ChatUtil.sendNotice(bukkitPlayer, message);
		}
	}
	
	public boolean isHelpMenu(Inventory inv) {
		boolean result = false;
		if(inv != null) {
			result = inv.equals(helpMenu.getInventory());
		}
		return result;
	}
	
	private void populateHelpMenu() {
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
			helpMenu.addIcon(icon);
			i++;
		}
		// Add info icons
		List<String> loreList = Arrays.asList("Discord community link");
		InfoIcon info = new InfoIcon("Community", loreList, Material.MINECART, i);
		if(communityLink == null) {
			communityLink = "";
		}
		info.setInfo(ChatColor.GOLD+"Click to join: "+ChatColor.DARK_PURPLE+ChatColor.UNDERLINE+communityLink);
		helpMenu.addIcon(info);
		i++;
	}
	
	public boolean isScoreMenu(Inventory inv) {
		boolean result = false;
		if(scoreMenus.containsKey(inv)) {
			result = true;
		}
		return result;
	}
	
	/**
	 * Displays a new score menu on the first page
	 * @param bukkitPlayer	The player to display the menu to
	 * @param scorePlayer	The player to use for scoring and stats
	 */
	public void displayScoreMenu(Player bukkitPlayer, KonOfflinePlayer scorePlayer) {
		
		KonPlayerScoreAttributes playerScoreAttributes = konquest.getKingdomManager().getPlayerScoreAttributes(scorePlayer);
		KonKingdomScoreAttributes kingdomScoreAttributes = konquest.getKingdomManager().getKingdomScoreAttributes(scorePlayer.getKingdom());
		int playerScore = playerScoreAttributes.getScore();
		int kingdomScore = kingdomScoreAttributes.getScore();
		String pageLabel = "";
		int i = 0;
		InfoIcon info;
		PlayerHeadIcon leader;
		
		// Create fresh paged menu
		PagedMenu newMenu = new PagedMenu();
		// Page 0
		pageLabel = ChatColor.BLACK+scorePlayer.getOfflineBukkitPlayer().getName()+" Score: "+playerScore;
		newMenu.addPage(0, 2, pageLabel);
		info = new InfoIcon(scorePlayer.getKingdom().getName()+" Kingdom Score", Arrays.asList("Total score:"+ChatColor.DARK_PURPLE+kingdomScore), Material.SPONGE, 0);
		newMenu.getPage(0).addIcon(info);
		info = new InfoIcon("Kingdom Towns", Arrays.asList("Total towns: "+ChatColor.AQUA+kingdomScoreAttributes.getAttributeValue(KonKingdomScoreAttribute.TOWNS), "Score contribution: "+ChatColor.DARK_PURPLE+kingdomScoreAttributes.getAttributeScore(KonKingdomScoreAttribute.TOWNS)), Material.DIRT, 1);
		newMenu.getPage(0).addIcon(info);
		info = new InfoIcon("Kingdom Land", Arrays.asList("Total land: "+ChatColor.AQUA+kingdomScoreAttributes.getAttributeValue(KonKingdomScoreAttribute.LAND), "Score contribution: "+ChatColor.DARK_PURPLE+kingdomScoreAttributes.getAttributeScore(KonKingdomScoreAttribute.LAND)), Material.DIRT, 2);
		newMenu.getPage(0).addIcon(info);
		info = new InfoIcon("Kingdom Favor", Arrays.asList("Total favor: "+ChatColor.AQUA+kingdomScoreAttributes.getAttributeValue(KonKingdomScoreAttribute.FAVOR), "Score contribution: "+ChatColor.DARK_PURPLE+kingdomScoreAttributes.getAttributeScore(KonKingdomScoreAttribute.FAVOR)), Material.DIRT, 3);
		newMenu.getPage(0).addIcon(info);
		info = new InfoIcon("Kingdom Population", Arrays.asList("Total population: "+ChatColor.AQUA+kingdomScoreAttributes.getAttributeValue(KonKingdomScoreAttribute.POPULATION), "Score contribution: "+ChatColor.DARK_PURPLE+kingdomScoreAttributes.getAttributeScore(KonKingdomScoreAttribute.POPULATION)), Material.DIRT, 4);
		newMenu.getPage(0).addIcon(info);
		info = new InfoIcon(scorePlayer.getOfflineBukkitPlayer().getName()+" Player Score", Arrays.asList("Total score:"+ChatColor.DARK_PURPLE+playerScore), Material.STONE, 9);
		newMenu.getPage(0).addIcon(info);
		info = new InfoIcon("Player Lordships", Arrays.asList("Towns: "+ChatColor.AQUA+playerScoreAttributes.getAttributeValue(KonPlayerScoreAttribute.TOWN_LORDS), "Score contribution: "+ChatColor.DARK_PURPLE+playerScoreAttributes.getAttributeScore(KonPlayerScoreAttribute.TOWN_LORDS)), Material.DIRT, 10);
		newMenu.getPage(0).addIcon(info);
		info = new InfoIcon("Player Knighthoods", Arrays.asList("Towns: "+ChatColor.AQUA+playerScoreAttributes.getAttributeValue(KonPlayerScoreAttribute.TOWN_KNIGHTS), "Score contribution: "+ChatColor.DARK_PURPLE+playerScoreAttributes.getAttributeScore(KonPlayerScoreAttribute.TOWN_KNIGHTS)), Material.DIRT, 11);
		newMenu.getPage(0).addIcon(info);
		info = new InfoIcon("Player Residencies", Arrays.asList("Towns: "+ChatColor.AQUA+playerScoreAttributes.getAttributeValue(KonPlayerScoreAttribute.TOWN_RESIDENTS), "Score contribution: "+ChatColor.DARK_PURPLE+playerScoreAttributes.getAttributeScore(KonPlayerScoreAttribute.TOWN_RESIDENTS)), Material.DIRT, 12);
		newMenu.getPage(0).addIcon(info);
		info = new InfoIcon("Lord Town Land", Arrays.asList("Land: "+ChatColor.AQUA+playerScoreAttributes.getAttributeValue(KonPlayerScoreAttribute.LAND_LORDS), "Score contribution: "+ChatColor.DARK_PURPLE+playerScoreAttributes.getAttributeScore(KonPlayerScoreAttribute.LAND_LORDS)), Material.DIRT, 13);
		newMenu.getPage(0).addIcon(info);
		info = new InfoIcon("Knight Town Land", Arrays.asList("Land: "+ChatColor.AQUA+playerScoreAttributes.getAttributeValue(KonPlayerScoreAttribute.LAND_KNIGHTS), "Score contribution: "+ChatColor.DARK_PURPLE+playerScoreAttributes.getAttributeScore(KonPlayerScoreAttribute.LAND_KNIGHTS)), Material.DIRT, 14);
		newMenu.getPage(0).addIcon(info);
		info = new InfoIcon("Resident Town Land", Arrays.asList("Land: "+ChatColor.AQUA+playerScoreAttributes.getAttributeValue(KonPlayerScoreAttribute.LAND_RESIDENTS), "Score contribution: "+ChatColor.DARK_PURPLE+playerScoreAttributes.getAttributeScore(KonPlayerScoreAttribute.LAND_RESIDENTS)), Material.DIRT, 15);
		newMenu.getPage(0).addIcon(info);
		// Page 1
		pageLabel = ChatColor.BLACK+scorePlayer.getOfflineBukkitPlayer().getName()+" Stats";
		newMenu.addPage(1, 3, pageLabel);
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
    		info = new InfoIcon(stat.toString(),Arrays.asList(stat.description(),ChatColor.AQUA+""+statValue),Material.GLASS,i);
    		newMenu.getPage(1).addIcon(info);
    		i++;
    	}
    	if(!isPlayerOnline) {
    		stats = null;
    	}
		// Page 2
		pageLabel = ChatColor.BLACK+scorePlayer.getKingdom().getName()+" Leaderboard";
		newMenu.addPage(2, 1, pageLabel);
		KonLeaderboard leaderboard = konquest.getKingdomManager().getKingdomLeaderboard(scorePlayer.getKingdom());
		if(!leaderboard.isEmpty()) {
			int numEntries = 9;
			if(leaderboard.getSize() < numEntries) {
				numEntries = leaderboard.getSize();
			}
			i = 0;
			for(int n = 0;n<numEntries;n++) {
				leader = new PlayerHeadIcon(leaderboard.getName(n),Arrays.asList("Player Score: "+ChatColor.AQUA+leaderboard.getScore(n),"Click to view stats"),leaderboard.getOfflinePlayer(n),i);
				newMenu.getPage(2).addIcon(leader);
				i++;
			}
		}
		newMenu.refreshNavigationButtons();
		scoreMenus.put(newMenu.getPage(0).getInventory(), newMenu);
		// Schedule delayed task to display inventory to player
		Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
            @Override
            public void run() {
            	bukkitPlayer.closeInventory();
            	bukkitPlayer.openInventory(newMenu.getPage(0).getInventory());
            }
        });
		
	}
	
}
