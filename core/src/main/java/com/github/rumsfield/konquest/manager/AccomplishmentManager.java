package com.github.rumsfield.konquest.manager;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.api.event.player.KonquestPlayerPrefixEvent;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class AccomplishmentManager {

	private final Konquest konquest;
	private boolean isEnabled;
	private final HashMap<String,KonCustomPrefix> customPrefixes;
	
	public AccomplishmentManager(Konquest konquest) {
		this.konquest = konquest;
		this.isEnabled = false;
		this.customPrefixes = new HashMap<>();
	}
	
	public void initialize() {
		isEnabled = konquest.getCore().getBoolean(CorePath.ACCOMPLISHMENT_PREFIX.getPath());
		ChatUtil.printDebug("Accomplishment Manager is ready with prefix "+isEnabled);
	}
	
	public boolean isEnabled() {
		return isEnabled;
	}
	
	//TODO: Store category level map in KonStats object instead of re-calculating for every stat update
	public void modifyPlayerStat(KonPlayer player, KonStatsType stat, int amount) {
		// Increase stat by amount and check for prefix updates when not in admin bypass
		if(player.isAdminBypassActive() || player.isBarbarian())return;
		KonStats playerStats = player.getPlayerStats();
		KonPrefix playerPrefix = player.getPlayerPrefix();
		// Increase stat
		playerStats.increaseStat(stat, amount);
		if(!isEnabled) return;
		// Determine stat category level
		double level = 0;
		for(KonStatsType statCheck : KonStatsType.values()) {
			if(statCheck.getCategory().equals(stat.getCategory())) {
				level = level + (playerStats.getStat(statCheck) * statCheck.weight());
			}
		}
		// Apply any missing qualifying prefixes in category
		for(KonPrefixType pre : KonPrefixType.values()) {
			if(!pre.category().equals(stat.getCategory())) {
				// Skip prefixes that do not belong to this stat category
				continue;
			}
			if(pre.level() <= level && !playerPrefix.hasPrefix(pre)) {
				// Add a prefix that meets the current level
				ChatUtil.printDebug("Accomplishment unlock for player "+player.getBukkitPlayer().getName()+" with prefix "+pre.getName());
				playerPrefix.addPrefix(pre);
				ChatUtil.sendKonPriorityTitle(player, ChatColor.DARK_PURPLE+pre.getName(), ChatColor.GOLD+MessagePath.GENERIC_NOTICE_ACCOMPLISHMENT.getMessage(), 60, 5, 10);
				ChatUtil.sendNotice(player.getBukkitPlayer(), ChatColor.WHITE+MessagePath.GENERIC_NOTICE_PREFIX_UNLOCK.getMessage()+": "+ChatColor.DARK_PURPLE+pre.getName());
				player.getBukkitPlayer().getWorld().playSound(player.getBukkitPlayer().getLocation(), Sound.BLOCK_BELL_USE, (float)1.0, (float)1.0);
			} else if(playerPrefix.hasPrefix(pre) && pre.level() > level) {
				// Remove a prefix that is below the current level
				ChatUtil.printDebug("Accomplishment reverted for player "+player.getBukkitPlayer().getName()+" with prefix "+pre.getName());
				playerPrefix.removePrefix(pre);
				if(playerPrefix.getMainPrefix().equals(pre)) {
					// Disable prefix if it was removed
					playerPrefix.setEnable(false);
				}
				ChatUtil.sendNotice(player.getBukkitPlayer(), ChatColor.WHITE+MessagePath.GENERIC_NOTICE_PREFIX_LOST.getMessage()+": "+ChatColor.DARK_RED+pre.getName());
				player.getBukkitPlayer().getWorld().playSound(player.getBukkitPlayer().getLocation(), Sound.BLOCK_BELL_USE, (float)1.0, (float)0.1);
			}
		}

	}
	
	/**
	 * Load prefixes based on stat conditions, only to be used on player join & fetch database info
	 * @param player KonPlayer object
	 */
	public void initPlayerPrefixes(KonPlayer player) {
		if(isEnabled) {
			HashMap<KonPrefixCategory,Double> categoryLevels = new HashMap<>();
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
				if(prefixLevel <= playerLevel) {
					playerPrefix.addPrefix(pre);
				}
			}
		}
	}
	
	public void displayStats(KonPlayer player) {
		KonStats playerStats = player.getPlayerStats();
		ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
		List<String> pages = new ArrayList<>();
		BookMeta meta = (BookMeta)Bukkit.getServer().getItemFactory().getItemMeta(Material.WRITTEN_BOOK);
		// Format book cover
		assert meta != null;
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
			StringBuilder unlockedPrefixNames = new StringBuilder();
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
						unlockedPrefixNames.append(pre.getName()).append(" ");
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
			// Fire event
			KonquestPlayerPrefixEvent invokeEvent = new KonquestPlayerPrefixEvent(konquest, player, "", true);
			Konquest.callKonquestEvent(invokeEvent);
			ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_PREFIX_NOTICE_DISABLE.getMessage());
			result = true;
		} else {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_PREFIX_ERROR_DISABLE.getMessage());
		}
		return result;
	}
	
	public boolean applyPlayerPrefix(KonPlayer player, KonPrefixType prefix) {
		boolean result = false;
		if(player.getPlayerPrefix().selectPrefix(prefix)) {
			player.getPlayerPrefix().setEnable(true);
			// Fire event
			KonquestPlayerPrefixEvent invokeEvent = new KonquestPlayerPrefixEvent(konquest, player, prefix.getName(), false);
			Konquest.callKonquestEvent(invokeEvent);
			ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_PREFIX_NOTICE_NEW.getMessage(prefix.getName()));
			result = true;
		} else {
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
        boolean status;
        String prefixName = "";
        int prefixCost = 0;
        ConfigurationSection prefixEntry;
    	for(String prefixLabel : prefixConfig.getConfigurationSection("prefix").getKeys(false)) {
    		status = true;
    		prefixEntry = prefixConfig.getConfigurationSection("prefix."+prefixLabel);
    		if(prefixEntry != null && StringUtils.isAlphanumeric(prefixLabel.replace("_",""))) {
        		if(prefixEntry.contains("name")) {
        			prefixName = prefixEntry.getString("name","");
        			if(prefixName.isEmpty()) {
        				ChatUtil.printConsoleError("prefix.yml has an invalid name for prefix: "+prefixLabel);
            			status = false;
        			}
        		} else {
        			ChatUtil.printConsoleError("prefix.yml is missing name for prefix: "+prefixLabel);
        			status = false;
        		}
        		if(prefixEntry.contains("cost")) {
        			prefixCost = prefixEntry.getInt("cost",0);
        			prefixCost = Math.max(prefixCost, 0);
        		} else {
        			ChatUtil.printConsoleError("prefix.yml is missing cost for prefix: "+prefixLabel);
        			status = false;
        		}
        		if(status) {
        			customPrefixes.put(prefixLabel.toLowerCase(), new KonCustomPrefix(prefixLabel.toLowerCase(),prefixName,prefixCost));
        			ChatUtil.printDebug("Loaded custom prefix: "+prefixLabel);
        		}
    		} else {
    			ChatUtil.printConsoleError("Failed to load invalid custom prefix, must only contain letters, numbers and underscores: "+prefixLabel);
    		}
    	}
	}
	
	public Set<String> getCustomPrefixLabels() {
		return customPrefixes.keySet();
	}
	
	public List<KonCustomPrefix> getCustomPrefixes() {
		return new ArrayList<>(customPrefixes.values());
	}
	
	/**
	 * Sets a player prefix without checks
	 * @param player - player to set prefix for
	 * @param prefixKey - prefix label
	 * @return true if successful, false if not
	 */
	public boolean setPlayerCustomPrefix(KonPlayer player, String prefixKey) {
		boolean result = false;
		if(customPrefixes.containsKey(prefixKey)) {
			KonCustomPrefix prefix = customPrefixes.get(prefixKey);
			if(player.getPlayerPrefix().setCustomPrefix(prefix)) {
				result = true;
			}
		}
		return result;
	}
	
	/**
	 * Sets a player prefix with checks
	 * @param player - player to set prefix for
	 * @param prefix - prefix label
	 * @return true if successful, false if not
	 */
	public boolean applyPlayerCustomPrefix(KonPlayer player, KonCustomPrefix prefix) {
		boolean checkPassed = false;
		// check for permission, available and cost
		if(!player.getBukkitPlayer().hasPermission("konquest.prefix."+prefix.getLabel())) {
			// no permission
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}
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
		if(!checkPassed) return false;

		player.getPlayerPrefix().addAvailableCustom(prefix.getLabel());
		if(!player.getPlayerPrefix().setCustomPrefix(prefix)) {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return false;
		}
		player.getPlayerPrefix().setEnable(true);
		ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_PREFIX_NOTICE_NEW.getMessage(ChatUtil.parseHex(prefix.getName())));
		return true;
	}

}
