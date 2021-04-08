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
import konquest.display.UpgradeIcon;
import konquest.model.KonTown;
import konquest.model.KonUpgrade;
import konquest.utility.ChatUtil;

public class DisplayManager {

	private Konquest konquest;
	private DisplayMenu helpMenu;
	//private HashMap<Integer, CommandType> helpCommandMap;
	//private HashMap<Integer, String> helpInfoMap;
	private HashMap<Inventory, DisplayMenu> townUpgradeMenus;
	private HashMap<Inventory, KonTown> townMenuCache;
	
	public DisplayManager(Konquest konquest) {
		this.konquest = konquest;
		this.helpMenu = new DisplayMenu(3, ChatColor.BLACK+"Konquest Help");
		//this.helpCommandMap = new HashMap<Integer, CommandType>();
		//this.helpInfoMap = new HashMap<Integer, String>();
		this.townUpgradeMenus = new HashMap<Inventory, DisplayMenu>();
		this.townMenuCache = new HashMap<Inventory, KonTown>();
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
				bukkitPlayer.closeInventory();
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
			bukkitPlayer.closeInventory();
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
	
	
	
}
