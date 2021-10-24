package konquest.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.model.KonCustomPrefix;
import konquest.model.KonPlayer;
import konquest.model.KonPrefix;
import konquest.model.KonPrefixCategory;
import konquest.model.KonPrefixType;
import konquest.model.KonStats;
import konquest.model.KonStatsType;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

/**
 * 
 * @author 
 *
 */
public class AccomplishmentManager {

	private Konquest konquest;
	private boolean isEnabled;
	private HashMap<String,KonCustomPrefix> customPrefixes;
	
	public AccomplishmentManager(Konquest konquest) {
		this.konquest = konquest;
		this.isEnabled = false;
		this.customPrefixes = new HashMap<String,KonCustomPrefix>();
	}
	
	public void initialize() {
		boolean configEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.accomplishment_prefix");
		isEnabled = configEnabled;
		//loadCustomPrefixes(); // moved to database init
		ChatUtil.printDebug("Accomplishment Manager is ready with prefix "+isEnabled);
	}
	
	public boolean isEnabled() {
		return isEnabled;
	}
	
	//TODO: Store category level map in KonStats object instead of re-calculating for every stat update
	public void modifyPlayerStat(KonPlayer player, KonStatsType stat, int amount) {
		// Increase stat by amount and check for prefix updates when not in admin bypass
		if(!player.isAdminBypassActive() && !player.isBarbarian()) {
			KonStats playerStats = player.getPlayerStats();
			KonPrefix playerPrefix = player.getPlayerPrefix();
			// Increase stat
			playerStats.increaseStat(stat, amount);
			if(isEnabled) {
				// Determine stat category level
				double level = 0;
				for(KonStatsType statCheck : KonStatsType.values()) {
					if(statCheck.getCategory().equals(stat.getCategory())) {
						level = level + (playerStats.getStat(statCheck) * statCheck.weight());
					}
				}
				// Apply any missing qualifying prefixes in category
				for(KonPrefixType pre : KonPrefixType.values()) {
					if(pre.category().equals(stat.getCategory()) && pre.level() <= level && !playerPrefix.hasPrefix(pre)) {
						ChatUtil.printDebug("Accomplishment unlock for player "+player.getBukkitPlayer().getName()+" with prefix "+pre.getName());
						playerPrefix.addPrefix(pre);
						//ChatUtil.sendTitle(player.getBukkitPlayer(), ChatColor.DARK_PURPLE+pre.getName(), ChatColor.GOLD+"You've made an Accomplishment!", 60);
						//ChatUtil.sendKonPriorityTitle(player, ChatColor.DARK_PURPLE+pre.getName(), ChatColor.GOLD+"You've made an Accomplishment!", 60, 5, 10);
						//ChatUtil.sendNotice(player.getBukkitPlayer(), ChatColor.WHITE+"Accomplishment prefix unlocked: "+ChatColor.DARK_PURPLE+pre.getName());
						ChatUtil.sendKonPriorityTitle(player, ChatColor.DARK_PURPLE+pre.getName(), ChatColor.GOLD+MessagePath.GENERIC_NOTICE_ACCOMPLISHMENT.getMessage(), 60, 5, 10);
						ChatUtil.sendNotice(player.getBukkitPlayer(), ChatColor.WHITE+MessagePath.GENERIC_NOTICE_PREFIX_UNLOCK.getMessage()+": "+ChatColor.DARK_PURPLE+pre.getName());
						player.getBukkitPlayer().getWorld().playSound(player.getBukkitPlayer().getLocation(), Sound.BLOCK_BELL_USE, (float)1.0, (float)1.0);
					}
				}
			}
		}
	}
	
	/**
	 * Load prefixes based on stat conditions, only to be used on player join & fetch database info
	 * @param player
	 */
	public void initPlayerPrefixes(KonPlayer player) {
		if(isEnabled) {
			HashMap<KonPrefixCategory,Double> categoryLevels = new HashMap<KonPrefixCategory,Double>();
			KonStats playerStats = player.getPlayerStats();
			KonPrefix playerPrefix = player.getPlayerPrefix();
			// Determine player's prefix category levels based on each stat
			for(KonStatsType stat : KonStatsType.values()) {
				double level = 0;
				if(categoryLevels.containsKey(stat.getCategory())) {
					level = categoryLevels.get(stat.getCategory());
				}
				double newLevel = level + (playerStats.getStat(stat) * stat.weight());
				categoryLevels.put(stat.getCategory(), newLevel);
			}
			// Add prefixes to player which meet level requirement
			playerPrefix.clear();
			for(KonPrefixType pre : KonPrefixType.values()) {
				int prefixLevel = pre.level();
				double playerLevel = categoryLevels.get(pre.category());
				//ChatUtil.printDebug("Evaluating prefix "+pre.getName()+" with level "+prefixLevel+" for player "+player.getBukkitPlayer().getName()+" with level "+playerLevel);
				if(prefixLevel <= playerLevel) {
					playerPrefix.addPrefix(pre);
					//ChatUtil.printDebug("    Added prefix!");
				}
			}
		}
	}
	
	public void displayStats(KonPlayer player) {
		KonStats playerStats = player.getPlayerStats();
		ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
		List<String> pages = new ArrayList<String>();
		BookMeta meta = (BookMeta)Bukkit.getServer().getItemFactory().getItemMeta(Material.WRITTEN_BOOK);
		// Format book cover
		meta.setAuthor("Konquest");
		meta.setGeneration(BookMeta.Generation.ORIGINAL);
		meta.setTitle(MessagePath.MENU_STATS_TITLE.getMessage());
		String titlePage = "";
		titlePage = titlePage+ChatColor.DARK_PURPLE+ChatColor.BOLD+MessagePath.MENU_STATS_TITLE.getMessage();
		titlePage = titlePage+ChatColor.RESET+"\n\n";
		titlePage = titlePage+ChatColor.BLACK+MessagePath.MENU_STATS_INTRO_1.getMessage();
		titlePage = titlePage+ChatColor.RESET+"\n\n";
		titlePage = titlePage+ChatColor.BLACK+MessagePath.MENU_STATS_INTRO_2.getMessage();
		pages.add(titlePage);
		// Format category and stat pages
		for(KonPrefixCategory cat : KonPrefixCategory.values()) {
			// Determine stat category level
			double level = 0;
			for(KonStatsType statCheck : KonStatsType.values()) {
				if(statCheck.getCategory().equals(cat)) {
					level = level + (playerStats.getStat(statCheck) * statCheck.weight());
				}
			}
			// Find next available prefix
			String unlockedPrefixNames = "";
			String nextPrefixName = "";
			double nextLevel = Double.MAX_VALUE;
			for(KonPrefixType pre : KonPrefixType.values()) {
				if(pre.category().equals(cat)) {
					if(pre.level() > level) {
						if(pre.level() < nextLevel) {
							nextPrefixName = pre.getName();
							nextLevel = pre.level();
						}
					} else {
						unlockedPrefixNames = unlockedPrefixNames+pre.getName()+" ";
					}
				}
			}
			String levelProgress = MessagePath.MENU_STATS_MAX.getMessage();
			if(nextLevel != Double.MAX_VALUE) {
				levelProgress = (int)level+"/"+(int)nextLevel;
			}
			String page = "";
			page = page+ChatColor.BLACK+MessagePath.MENU_STATS_CATEGORY.getMessage()+":";
			page = page+ChatColor.RESET+"\n";
			page = page+ChatColor.DARK_PURPLE+cat.getTitle();
			page = page+ChatColor.RESET+"\n";
			page = page+ChatColor.GRAY+levelProgress;
			page = page+ChatColor.RESET+"\n\n";
			page = page+ChatColor.BLACK+MessagePath.MENU_STATS_NEXT.getMessage()+": "+ChatColor.DARK_GREEN+nextPrefixName;
			page = page+ChatColor.RESET+"\n\n";
			page = page+ChatColor.BLACK+MessagePath.MENU_STATS_UNLOCK.getMessage()+": "+ChatColor.GREEN+unlockedPrefixNames;
			pages.add(page);
			// Format individual stats pages for this category
			for(KonStatsType stat : KonStatsType.values()) {
				if(stat.getCategory().equals(cat)) {
					int currentAmount = playerStats.getStat(stat);
					double currentLevel = currentAmount*stat.weight();
					page = "";
					page = page+ChatColor.DARK_PURPLE+cat.getTitle();
					page = page+ChatColor.RESET+"\n";
					page = page+ChatColor.DARK_PURPLE+ChatColor.ITALIC+stat.displayName();
					page = page+ChatColor.RESET+"\n";
					page = page+ChatColor.BLACK+stat.description();
					page = page+ChatColor.RESET+"\n\n";
					page = page+ChatColor.BLACK+MessagePath.MENU_STATS_AMOUNT.getMessage()+": "+ChatColor.GRAY+currentAmount;
					page = page+ChatColor.RESET+"\n";
					page = page+ChatColor.BLACK+MessagePath.MENU_STATS_POINTS.getMessage()+": "+ChatColor.GRAY+(int)currentLevel;
					pages.add(page);
				}
			}
		}
		meta.setPages(pages);
		// Display book
		book.setItemMeta(meta);
		player.getBukkitPlayer().openBook(book);
	}
	
	public boolean disablePlayerPrefix(KonPlayer player) {
		boolean result = false;
		if(player.getPlayerPrefix().isEnabled()) {
			player.getPlayerPrefix().setEnable(false);
			//ChatUtil.sendNotice((Player) getSender(), "Turned off your prefix title");
			ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_PREFIX_NOTICE_DISABLE.getMessage());
			result = true;
		} else {
			//ChatUtil.sendNotice((Player) getSender(), "Your prefix title is already off");
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_PREFIX_ERROR_DISABLE.getMessage());
		}
		return result;
	}
	
	public boolean applyPlayerPrefix(KonPlayer player, KonPrefixType prefix) {
		boolean result = false;
		if(player.getPlayerPrefix().selectPrefix(prefix)) {
			player.getPlayerPrefix().setEnable(true);
			//ChatUtil.sendNotice((Player) getSender(), "Your prefix is now "+prefixChosen.getName());
			ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_PREFIX_NOTICE_NEW.getMessage(prefix.getName()));
			result = true;
		} else {
			//ChatUtil.sendNotice((Player) getSender(), "The prefix "+prefixChosen.getName()+" is not unlocked!");
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_PREFIX_ERROR_NEW.getMessage(prefix.getName()));
		}
		return result;
	}
	
	// Load custom prefixes from file
	public void loadCustomPrefixes() {
		customPrefixes.clear();
		// Get all custom prefixes from file
		FileConfiguration prefixConfig = konquest.getConfigManager().getConfig("prefix");
        if (prefixConfig.get("prefix") == null) {
        	ChatUtil.printDebug("There is no prefix section in prefix.yml");
            return;
        }
        boolean status = true;
        String prefixName = "";
        int prefixCost = 0;
        ConfigurationSection prefixEntry = null;
    	for(String prefixLabel : prefixConfig.getConfigurationSection("prefix").getKeys(false)) {
    		status = true;
    		prefixEntry = prefixConfig.getConfigurationSection("prefix."+prefixLabel);
    		if(prefixEntry != null) {
        		if(prefixEntry.contains("name")) {
        			prefixName = prefixEntry.getString("name","");
        			if(prefixName.equals("") || prefixName.isEmpty()) {
        				ChatUtil.printConsoleError("prefix.yml has an invalid name for prefix: "+prefixLabel);
            			status = false;
        			}
        		} else {
        			ChatUtil.printConsoleError("prefix.yml is missing name for prefix: "+prefixLabel);
        			status = false;
        		}
        		if(prefixEntry.contains("cost")) {
        			prefixCost = prefixEntry.getInt("cost",0);
        			prefixCost = prefixCost < 0 ? 0 : prefixCost;
        		} else {
        			ChatUtil.printConsoleError("prefix.yml is missing cost for prefix: "+prefixLabel);
        			status = false;
        		}
        		if(status) {
        			customPrefixes.put(prefixLabel.toLowerCase(), new KonCustomPrefix(prefixLabel.toLowerCase(),prefixName,prefixCost));
        			ChatUtil.printDebug("Loaded custom prefix: "+prefixLabel);
        		}
    		} else {
    			ChatUtil.printDebug("Failed to load null prefix entry: "+prefixLabel);
    		}
    	}
	}
	
	public Set<String> getCustomPrefixLabels() {
		return customPrefixes.keySet();
	}
	
	public List<KonCustomPrefix> getCustomPrefixes() {
		List<KonCustomPrefix> result = new ArrayList<KonCustomPrefix>();
		for(KonCustomPrefix c : customPrefixes.values()) {
			result.add(c);
		}
		return result;
	}
	
	/**
	 * Sets a player prefix without checks
	 * @param player
	 * @param label
	 * @return
	 */
	public boolean setPlayerCustomPrefix(KonPlayer player, String label) {
		boolean result = false;
		if(customPrefixes.containsKey(label)) {
			KonCustomPrefix prefix = customPrefixes.get(label);
			if(player.getPlayerPrefix().setCustomPrefix(prefix)) {
				result = true;
			}
		}
		return result;
	}
	
	/**
	 * Sets a player prefix with checks
	 * @param player
	 * @param label
	 * @return
	 */
	public boolean applyPlayerCustomPrefix(KonPlayer player, KonCustomPrefix prefix) {
		boolean result = false;
		boolean checkPassed = false;
		// check for permission, available and cost
		if(player.getBukkitPlayer().hasPermission("konquest.prefix."+prefix.getLabel())) {
			if(player.getPlayerPrefix().isCustomAvailable(prefix.getLabel())) {
				// Player already owns this prefix
				checkPassed = true;
			} else {
				// Attempt to purchase this prefix
				int cost = prefix.getCost();
				if(cost > 0) {
		        	if(KonquestPlugin.getBalance(player.getBukkitPlayer()) < cost) {
						// player is too poor
		        		ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(cost));
					} else if(KonquestPlugin.withdrawPlayer(player.getBukkitPlayer(), cost)) {
			            checkPassed = true;
					}
	        	} else {
					// it's free!
					checkPassed = true;
				}
			}
		} else {
			// no permission
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
		}
		if(checkPassed) {
			player.getPlayerPrefix().addAvailableCustom(prefix.getLabel());
			if(player.getPlayerPrefix().setCustomPrefix(prefix)) {
				player.getPlayerPrefix().setEnable(true);
				ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_PREFIX_NOTICE_NEW.getMessage(ChatUtil.parseHex(prefix.getName())));
				result = true;
			} else {
				ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			}
		}
		return result;
	}

}
