package com.github.rumsfield.konquest.manager;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class LootManager implements Timeable{

	private final Konquest konquest;
	private final HashMap<Location, Long> lootRefreshLog;
	private long refreshTimeSeconds;
	private long markedRefreshTime;
	private int monumentLootCount;
	private final Timer lootRefreshTimer;
	private final HashMap<ItemStack,Integer> monumentLootTable;

	private final HashMap<Location, Boolean> ruinLootEmptiedLog;
	private int ruinLootCount;
	private final HashMap<ItemStack,Integer> ruinLootTable;

	
	public LootManager(Konquest konquest) {
		this.konquest = konquest;
		this.refreshTimeSeconds = 0;
		this.markedRefreshTime = 0;
		this.lootRefreshLog = new HashMap<>();
		this.monumentLootCount = 0;
		this.lootRefreshTimer = new Timer(this);
		this.monumentLootTable = new HashMap<>();
		this.ruinLootEmptiedLog = new HashMap<>();
		this.ruinLootCount = 0;
		this.ruinLootTable = new HashMap<>();
	}
	
	public void initialize() {
		// Parse config for refresh time
		refreshTimeSeconds = konquest.getCore().getLong(CorePath.MONUMENTS_LOOT_REFRESH.getPath(),0L);
		lootRefreshTimer.stopTimer();
		if(refreshTimeSeconds > 0) {
			lootRefreshTimer.setTime((int)refreshTimeSeconds);
			lootRefreshTimer.startLoopTimer();
		}
		markedRefreshTime = new Date().getTime();
		monumentLootCount = konquest.getCore().getInt(CorePath.MONUMENTS_LOOT_COUNT.getPath(),0);
		monumentLootCount = Math.max(monumentLootCount,0);
		ruinLootCount = konquest.getCore().getInt(CorePath.RUINS_LOOT_COUNT.getPath(),0);
		ruinLootCount = Math.max(ruinLootCount,0);
		if(loadAllLoot()) {
			ChatUtil.printDebug("Loaded loot table from loot.yml");
		} else {
			ChatUtil.printConsoleError("Failed to load loot table, check for syntax errors.");
		}
		ChatUtil.printDebug("Loot Manager is ready with loot count: "+ monumentLootCount+", "+ruinLootCount);
	}


	private HashMap<ItemStack,Integer> loadItems(ConfigurationSection itemsSection) {
		HashMap<ItemStack,Integer> result = new HashMap<>();
		if(itemsSection == null) return result;
		boolean status;
		Material itemType = null;
		ConfigurationSection lootEntry;
		String pathName = itemsSection.getCurrentPath();
		for(String itemName : itemsSection.getKeys(false)) {
			status = true;
			int itemAmount = 0;
			int itemWeight = 0;
			try {
				itemType = Material.valueOf(itemName);
			} catch(IllegalArgumentException e) {
				ChatUtil.printConsoleError("Invalid loot item \""+itemName+"\" given in loot.yml path "+pathName+", skipping this item.");
				status = false;
			}
			lootEntry = itemsSection.getConfigurationSection(itemName);
			if(lootEntry != null) {
				if(lootEntry.contains("amount")) {
					itemAmount = lootEntry.getInt("amount",1);
					itemAmount = Math.max(itemAmount, 1);
				} else {
					ChatUtil.printConsoleError("loot.yml path "+pathName+" is missing amount for item: "+itemName);
					status = false;
				}
				if(lootEntry.contains("weight")) {
					itemWeight = lootEntry.getInt("weight",0);
					itemWeight = Math.max(itemWeight, 0);
				} else {
					ChatUtil.printConsoleError("loot.yml path "+pathName+" is missing weight for item: "+itemName);
					status = false;
				}
			} else {
				status = false;
				ChatUtil.printConsoleError("loot.yml path "+pathName+" contains invalid item: "+itemName);
			}
			if(status && itemWeight > 0) {
				// Add loot table entry
				result.put(new ItemStack(itemType, itemAmount), itemWeight);
				ChatUtil.printDebug("  Added loot path "+pathName+" item "+itemName+" with amount "+itemAmount+", weight "+itemWeight);
			}
		}
		return result;
	}

	private HashMap<ItemStack,Integer> loadPotions(ConfigurationSection potionsSection) {
		HashMap<ItemStack,Integer> result = new HashMap<>();
		if(potionsSection == null) return result;
		boolean status;
		ConfigurationSection lootEntry;
		PotionType potionType = null;
		String pathName = potionsSection.getCurrentPath();
		for(String potionName : potionsSection.getKeys(false)) {
			status = true;
			boolean itemUpgraded = false;
			boolean itemExtended = false;
			int itemWeight = 0;
			try {
				potionType = PotionType.valueOf(potionName);
			} catch(IllegalArgumentException e) {
				ChatUtil.printConsoleError("Invalid loot potion \""+potionName+"\" given in loot.yml path "+pathName+", skipping this potion.");
				status = false;
			}
			lootEntry = potionsSection.getConfigurationSection(potionName);
			if(lootEntry != null) {
				if(lootEntry.contains("upgraded")) {
					itemUpgraded = lootEntry.getBoolean("upgraded",false);
				} else {
					ChatUtil.printConsoleError("loot.yml path "+pathName+" is missing upgraded for potion: "+potionName);
					status = false;
				}
				if(lootEntry.contains("extended")) {
					itemExtended = lootEntry.getBoolean("extended",false);
				} else {
					ChatUtil.printConsoleError("loot.yml path "+pathName+" is missing extended for potion: "+potionName);
					status = false;
				}
				if(lootEntry.contains("weight")) {
					itemWeight = lootEntry.getInt("weight",0);
					itemWeight = Math.max(itemWeight, 0);
				} else {
					ChatUtil.printConsoleError("loot.yml path "+pathName+" is missing weight for potion: "+potionName);
					status = false;
				}
			} else {
				status = false;
				ChatUtil.printConsoleError("loot.yml path "+pathName+" contains invalid potion: "+potionName);
			}
			if(status && itemWeight > 0) {
				// Add loot table entry
				ItemStack potion = new ItemStack(Material.POTION, 1);
				PotionMeta meta = CompatibilityUtil.setPotionData((PotionMeta) potion.getItemMeta(), potionType, itemExtended, itemUpgraded);
				assert meta != null;
				potion.setItemMeta(meta);
				result.put(potion, itemWeight);
				ChatUtil.printDebug("  Added loot path "+pathName+" potion "+meta.getBasePotionType()+" with extended "+itemExtended+", upgraded "+itemUpgraded+", weight "+itemWeight);
			}
		}
		return result;
	}

	private HashMap<ItemStack,Integer> loadEbooks(ConfigurationSection ebookSection) {
		HashMap<ItemStack, Integer> result = new HashMap<>();
		if (ebookSection == null) return result;
		boolean status;
		ConfigurationSection lootEntry;
		Enchantment bookType;
		String pathName = ebookSection.getCurrentPath();
		for(String enchantName : ebookSection.getKeys(false)) {
			status = true;
			int itemLevel = 0;
			int itemWeight = 0;
			bookType = CompatibilityUtil.getEnchantment(enchantName);
			if(bookType == null) {
				ChatUtil.printConsoleError("Invalid loot enchantment \""+enchantName+"\" given in loot.yml path "+pathName+", skipping this enchantment.");
				status = false;
			}
			lootEntry = ebookSection.getConfigurationSection(enchantName);
			if(lootEntry != null) {
				if(lootEntry.contains("level")) {
					itemLevel = lootEntry.getInt("level",0);
					itemLevel = Math.max(itemLevel, 0);
				} else {
					ChatUtil.printConsoleError("loot.yml path "+pathName+" is missing level for enchantment: "+enchantName);
					status = false;
				}
				if(lootEntry.contains("weight")) {
					itemWeight = lootEntry.getInt("weight",0);
					itemWeight = Math.max(itemWeight, 0);
				} else {
					ChatUtil.printConsoleError("loot.yml path "+pathName+" is missing weight for enchantment: "+enchantName);
					status = false;
				}
			} else {
				status = false;
				ChatUtil.printConsoleError("loot.yml path "+pathName+" contains invalid enchanted book: "+enchantName);
			}
			if(status && itemWeight > 0) {
				// Add loot table entry
				// Limit level
				if(itemLevel < bookType.getStartLevel()) {
					itemLevel = bookType.getStartLevel();
				} else if(itemLevel > bookType.getMaxLevel()) {
					itemLevel = bookType.getMaxLevel();
				}
				ItemStack enchantBook = new ItemStack(Material.ENCHANTED_BOOK, 1);
				EnchantmentStorageMeta enchantMeta = (EnchantmentStorageMeta)enchantBook.getItemMeta();
				enchantMeta.addStoredEnchant(bookType, itemLevel, true);
				enchantBook.setItemMeta(enchantMeta);
				result.put(enchantBook, itemWeight);
				ChatUtil.printDebug("  Added loot path "+pathName+" enchant "+bookType.getKey().toString()+" with level "+itemLevel+", weight "+itemWeight);
			}
		}
		return result;
	}
	
	// Loads loot table from file
	private boolean loadAllLoot() {
		monumentLootTable.clear();
		ruinLootTable.clear();
		FileConfiguration lootConfig = konquest.getConfigManager().getConfig("loot");
        if (lootConfig.get("loot") == null) {
        	ChatUtil.printDebug("There is no loot section in loot.yml");
            return false;
        }
		if (lootConfig.get("ruins") == null) {
			ChatUtil.printDebug("There is no ruins section in loot.yml");
			return false;
		}
        ChatUtil.printDebug("Loading loot...");
        // Load items
		monumentLootTable.putAll(loadItems(lootConfig.getConfigurationSection("loot.items")));
		ruinLootTable.putAll(loadItems(lootConfig.getConfigurationSection("ruins.items")));
        // Load potions
		monumentLootTable.putAll(loadPotions(lootConfig.getConfigurationSection("loot.potions")));
		ruinLootTable.putAll(loadPotions(lootConfig.getConfigurationSection("ruins.potions")));
        // Load enchanted books
		monumentLootTable.putAll(loadEbooks(lootConfig.getConfigurationSection("loot.enchanted_books")));
		ruinLootTable.putAll(loadEbooks(lootConfig.getConfigurationSection("ruins.enchanted_books")));
        return true;
	}

	/*
	 * Monument Loot
	 */

	public String getMonumentLootTime() {
		String noColor = "";
		int timerCount = Math.max(lootRefreshTimer.getTime(),0);
		return Konquest.getTimeFormat(timerCount, noColor);
	}

	/**
	 * Attempts to fill an inventory with loot
	 * @param inventory - the inventory to try and fill
	 * @param count - number of ItemStacks to put into the inventory
	 * @return true: The inventory was successfully filled with loot
	 * 		   false: The inventory was not filled, probably due to unexpired refresh time
	 */
	public boolean updateMonumentLoot(Inventory inventory, int count) {
		Date now = new Date();
		Location invLoc = inventory.getLocation();
		if(lootRefreshLog.containsKey(invLoc)) {
			long loggedLastFilledTime = lootRefreshLog.get(invLoc);
			Date lastFilledDate = new Date(loggedLastFilledTime);
			if(lastFilledDate.after(new Date(markedRefreshTime))) {
				// Inventory is not yet done refreshing
				return false;
			}
		}
		clearUpperInventory(inventory);
		// Generate new items
		int availableSlot;
		for(int i=0;i<count;i++) {
			availableSlot = inventory.firstEmpty();
			if(availableSlot == -1) {
				ChatUtil.printDebug("Failed to find empty slot for generated loot in inventory "+ inventory);
			} else {
				inventory.setItem(availableSlot,chooseRandomItem(monumentLootTable));
			}
		}
		long lootLastFilledTime = now.getTime();
		lootRefreshLog.put(invLoc,lootLastFilledTime);
		return true;
	}
	
	public boolean updateMonumentLoot(Inventory inventory, KonTown town) {
		int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.LOOT);
		int upgradedLootCount = monumentLootCount + upgradeLevel;
		return updateMonumentLoot(inventory,upgradedLootCount);
	}

	/*
	 * Ruin Loot
	 */

	/**
	 * Attempts to fill an inventory with loot under the following conditions:
	 * - The inventory has not already been emptied
	 * - The ruin is in capture cooldown state
	 * @param inventory The inventory to fill with loot
	 * @param ruin The ruin that contains the inventory
	 * @return True when loot was updated, else false
	 */
	public boolean updateRuinLoot(Inventory inventory, KonRuin ruin) {
		Location invLoc = inventory.getLocation();
		int count = ruinLootCount;
		boolean isLootAfterCapture = konquest.getCore().getBoolean(CorePath.RUINS_LOOT_AFTER_CAPTURE.getPath());
		boolean isRuinCaptured = ruin.isCaptureDisabled();
		if(isLootAfterCapture && !isRuinCaptured) {
			// This ruin has not been capture yet
			return false;
		}
		if(ruinLootEmptiedLog.containsKey(invLoc)) {
			if(ruinLootEmptiedLog.get(invLoc)) {
				// Inventory has been emptied already
				return false;
			}
		}
		clearUpperInventory(inventory);
		// Generate new items
		int availableSlot;
		for(int i=0;i<count;i++) {
			availableSlot = inventory.firstEmpty();
			if(availableSlot == -1) {
				ChatUtil.printDebug("Failed to find empty slot for generated loot in inventory "+ inventory);
			} else {
				inventory.setItem(availableSlot,chooseRandomItem(ruinLootTable));
			}
		}
		ruinLootEmptiedLog.put(invLoc,true); // This inventory has been emptied
		return true;
	}

	public void resetRuinLoot(KonRuin ruin) {
		// Set every location in the log within this ruin to false
		for(Map.Entry<Location,Boolean> entry : ruinLootEmptiedLog.entrySet()) {
			if(ruin.isLocInside(entry.getKey())) {
				entry.setValue(false);
			}
		}
	}

	/*
	 * Common
	 */

	private void clearUpperInventory(Inventory inventory) {
		inventory.clear();
	}

	private ItemStack chooseRandomItem(HashMap<ItemStack,Integer> itemOptions) {
		ItemStack item = new ItemStack(Material.DIRT,1);
		ItemMeta defaultMeta = item.getItemMeta();
		defaultMeta.setDisplayName(ChatColor.DARK_RED+"Invalid Loot");
		item.setItemMeta(defaultMeta);
		if(!itemOptions.isEmpty()) {
			// Find total item range
			int total = 0;
			for(int p : itemOptions.values()) {
				total = total + p;
			}
			int typeChoice = ThreadLocalRandom.current().nextInt(total);
			int typeWindow = 0;
			for(ItemStack i : itemOptions.keySet()) {
				if(typeChoice < typeWindow + itemOptions.get(i)) {
					item = i.clone();
					break;
				}
				typeWindow = typeWindow + itemOptions.get(i);
			}
		}
		// Check for stored enchantment with level 0
		ItemMeta meta = item.getItemMeta();
		if(meta instanceof EnchantmentStorageMeta) {
			EnchantmentStorageMeta enchantMeta = (EnchantmentStorageMeta)meta;
			if(enchantMeta.hasStoredEnchants()) {
				Map<Enchantment,Integer> enchants = enchantMeta.getStoredEnchants();
				if(!enchants.isEmpty()) {
					for(Enchantment e : enchants.keySet()) {
						if(enchants.get(e) == 0) {
							// Choose random level
							int newLevel = ThreadLocalRandom.current().nextInt(e.getMaxLevel()+1);
							if(newLevel < e.getStartLevel()) {
								newLevel = e.getStartLevel();
							}
							enchantMeta.removeStoredEnchant(e);
							enchantMeta.addStoredEnchant(e, newLevel, true);
							item.setItemMeta(enchantMeta);
							ChatUtil.printDebug("Enchanted loot item "+ item.getType() +" updated "+e.getKey().getKey()+" from level 0 to "+newLevel);
						}
					}
				}
			}
		}
		return item;
	}

	@Override
	public void onEndTimer(int taskID) {
		if(taskID == 0) {
			ChatUtil.printDebug("Loot Refresh Timer ended with null taskID!");
			return;
		}
		if(taskID == lootRefreshTimer.getTaskID()) {
			markedRefreshTime = new Date().getTime();
			ChatUtil.printDebug("Loot Refresh timer marked new availability time");
			for(KonPlayer player : konquest.getPlayerManager().getPlayersOnline()) {
				KonMonumentTemplate template = player.getKingdom().getMonumentTemplate();
				if(!player.isBarbarian() && template != null && template.hasLoot()) {
					ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.GENERIC_NOTICE_LOOT.getMessage());
				}
			}
		}
	}
}
