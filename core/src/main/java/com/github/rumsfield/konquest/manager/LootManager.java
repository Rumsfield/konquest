package com.github.rumsfield.konquest.manager;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LootManager implements Timeable{

	private final Konquest konquest;
	private final HashMap<Location, Long> lootRefreshLog;
	private final HashMap<Inventory, Inventory> lootCache;
	private long refreshTimeSeconds;
	private long markedRefreshTime;
	private int monumentLootCount;
	private final Timer lootRefreshTimer;
	private final KonLootTable monumentLootTable;
	private final HashMap<String,KonLootTable> monumentSpecialLootTable;
	private final HashMap<String,KonLootTable> monumentCustomLootTable;
	private final HashMap<Location, Boolean> ruinLootEmptiedLog;
	private int ruinLootCount;
	private final KonLootTable ruinLootTable;
	private final HashMap<String,KonLootTable> ruinCustomLootTable;

	public static String defaultLootTableName = "default";
	
	public LootManager(Konquest konquest) {
		this.konquest = konquest;
		this.refreshTimeSeconds = 0;
		this.markedRefreshTime = 0;
		this.lootRefreshLog = new HashMap<>();
		this.lootCache = new HashMap<>();
		this.monumentLootCount = 0;
		this.lootRefreshTimer = new Timer(this);
		this.monumentLootTable = new KonLootTable();
		this.monumentSpecialLootTable = new HashMap<>();
		this.monumentCustomLootTable = new HashMap<>();
		this.ruinLootEmptiedLog = new HashMap<>();
		this.ruinLootCount = 0;
		this.ruinLootTable = new KonLootTable();
		this.ruinCustomLootTable = new HashMap<>();
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
			ChatUtil.printConsoleError("Failed to load default loot tables, check loot.yml for syntax errors.");
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
				//ChatUtil.printDebug("  Added loot path "+pathName+" item "+itemName+" with amount "+itemAmount+", weight "+itemWeight);
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
				//ChatUtil.printDebug("  Added loot path "+pathName+" potion "+potionType+" with extended "+itemExtended+", upgraded "+itemUpgraded+", weight "+itemWeight);
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
				//ChatUtil.printDebug("  Added loot path "+pathName+" enchant "+bookType.getKey().toString()+" with level "+itemLevel+", weight "+itemWeight);
			}
		}
		return result;
	}
	
	// Loads loot table from file
	private boolean loadAllLoot() {
		monumentLootTable.clearLoot();
		monumentSpecialLootTable.clear();
		monumentCustomLootTable.clear();
		ruinLootTable.clearLoot();
		ruinCustomLootTable.clear();
		FileConfiguration lootConfig = konquest.getConfigManager().getConfig("loot");
		boolean status = true;
		// Default Monument Loot
		if (lootConfig.contains("loot")) {
			monumentLootTable.addLoot(loadItems(lootConfig.getConfigurationSection("loot.items")));
			monumentLootTable.addLoot(loadPotions(lootConfig.getConfigurationSection("loot.potions")));
			monumentLootTable.addLoot(loadEbooks(lootConfig.getConfigurationSection("loot.enchanted_books")));
			if (lootConfig.contains("loot.name")) {
				monumentLootTable.setName(lootConfig.getString("loot.name"));
			} else {
				monumentLootTable.setName(defaultLootTableName);
			}
		} else {
			status = false;
		}
		// Default Ruin Loot
		if (lootConfig.contains("ruins")) {
			ruinLootTable.addLoot(loadItems(lootConfig.getConfigurationSection("ruins.items")));
			ruinLootTable.addLoot(loadPotions(lootConfig.getConfigurationSection("ruins.potions")));
			ruinLootTable.addLoot(loadEbooks(lootConfig.getConfigurationSection("ruins.enchanted_books")));
			if (lootConfig.contains("ruins.name")) {
				ruinLootTable.setName(lootConfig.getString("ruins.name"));
			} else {
				ruinLootTable.setName(defaultLootTableName);
			}
		} else {
			status = false;
		}
		// Special Monument Loot
		ConfigurationSection lootSpecialSection = lootConfig.getConfigurationSection("loot_special");
		if (lootSpecialSection != null) {
			for (String professionName : lootSpecialSection.getKeys(false)) {
				if (CompatibilityUtil.getProfessionFromName(professionName) != null) {
					// Section name matches profession
					KonLootTable professionLoot = new KonLootTable();
					professionLoot.addLoot(loadItems(lootSpecialSection.getConfigurationSection(professionName+".items")));
					professionLoot.addLoot(loadPotions(lootSpecialSection.getConfigurationSection(professionName+".potions")));
					professionLoot.addLoot(loadEbooks(lootSpecialSection.getConfigurationSection(professionName+".enchanted_books")));
					if (lootSpecialSection.contains(professionName+".name")) {
						professionLoot.setName(lootSpecialSection.getString(professionName+".name"));
					} else {
						professionLoot.setName(professionName);
					}
					monumentSpecialLootTable.put(professionName.toLowerCase(),professionLoot);
				} else {
					ChatUtil.printConsoleError("Failed to load special monument loot table "+professionName+", the name must match a Villager Profession.");
				}
			}
		}
		// Custom Monument Loot
		ConfigurationSection lootCustomSection = lootConfig.getConfigurationSection("loot_custom");
		if (lootCustomSection != null) {
			for (String customName : lootCustomSection.getKeys(false)) {
				KonLootTable customLoot = new KonLootTable();
				customLoot.addLoot(loadItems(lootCustomSection.getConfigurationSection(customName+".items")));
				customLoot.addLoot(loadPotions(lootCustomSection.getConfigurationSection(customName+".potions")));
				customLoot.addLoot(loadEbooks(lootCustomSection.getConfigurationSection(customName+".enchanted_books")));
				if (lootCustomSection.contains(customName+".name")) {
					customLoot.setName(lootCustomSection.getString(customName+".name"));
				} else {
					customLoot.setName(customName);
				}
				monumentCustomLootTable.put(customName.toLowerCase(),customLoot);
			}
		}
		// Custom Ruin Loot
		ConfigurationSection ruinCustomSection = lootConfig.getConfigurationSection("ruins_custom");
		if (ruinCustomSection != null) {
			for (String customName : ruinCustomSection.getKeys(false)) {
				KonLootTable customLoot = new KonLootTable();
				customLoot.addLoot(loadItems(ruinCustomSection.getConfigurationSection(customName+".items")));
				customLoot.addLoot(loadPotions(ruinCustomSection.getConfigurationSection(customName+".potions")));
				customLoot.addLoot(loadEbooks(ruinCustomSection.getConfigurationSection(customName+".enchanted_books")));
				if (ruinCustomSection.contains(customName+".name")) {
					customLoot.setName(ruinCustomSection.getString(customName+".name"));
				} else {
					customLoot.setName(customName);
				}
				ruinCustomLootTable.put(customName.toLowerCase(),customLoot);
			}
		}
        return status;
	}

	public void refreshCustomTableAssignments() {
		// Check for valid custom table names in all monument templates and ruins
		for (KonMonumentTemplate template : konquest.getSanctuaryManager().getAllTemplates()) {
			if (!template.isLootTableDefault() && !isMonumentLootTable(template.getLootTableName())) {
				// Loot table specified in this template is not default, but does not exist.
				// Revert to default.
				template.setLootTableDefault();
			}
		}
		for (KonRuin ruin : konquest.getRuinManager().getRuins()) {
			if (!ruin.isLootTableDefault() && !isRuinLootTable(ruin.getLootTableName())) {
				// Loot table specified in this ruin is not default, but does not exist.
				// Revert to default.
				ruin.setLootTableDefault();
			}
		}
	}

	/*
	 * Monument Loot
	 * When profession trade discounts are enabled, use the special tables if a town specialization is set.
	 * Otherwise, use a custom table.
	 * Lastly, use the default.
	 */

	public KonLootTable getMonumentSpecialLootTable(String name) {
		String tableKey = name.toLowerCase();
		if (monumentSpecialLootTable.containsKey(tableKey)) {
			return monumentSpecialLootTable.get(tableKey);
		}
		// No table found
		return null;
	}

	public KonLootTable getMonumentCustomLootTable(String name) {
		String tableKey = name.toLowerCase();
		if (monumentCustomLootTable.containsKey(tableKey)) {
			return monumentCustomLootTable.get(tableKey);
		}
		// No table found
		return null;
	}

	public ArrayList<String> getMonumentLootTableKeys() {
		ArrayList<String> result = new ArrayList<>();
		result.add(defaultLootTableName);
		result.addAll(monumentCustomLootTable.keySet());
		return result;
	}

	public boolean isMonumentLootTable(String name) {
		return name.equalsIgnoreCase(defaultLootTableName) ||
				monumentCustomLootTable.containsKey(name.toLowerCase());
	}

	public boolean isMonumentSpecialLootTable(String name) {
		return monumentSpecialLootTable.containsKey(name.toLowerCase());
	}

	public String getMonumentLootDisplayName(KonMonumentTemplate template) {
		KonLootTable lootTable = null;
		if (!template.isLootTableDefault()) {
			lootTable = getMonumentCustomLootTable(template.getLootTableName());
		}
		if (lootTable == null) {
			lootTable = monumentLootTable;
		}
		return lootTable == null ? "Invalid" : lootTable.getName();
	}

	public String getMonumentLootDisplayName(KonTown town) {
		KonLootTable lootTable = null;
		if (konquest.getKingdomManager().getIsDiscountEnable()) {
			lootTable = getMonumentSpecialLootTable(town.getSpecializationName());
		}
		if (lootTable == null && !town.getKingdom().getMonumentTemplate().isLootTableDefault()) {
			lootTable = getMonumentCustomLootTable(town.getKingdom().getMonumentTemplate().getLootTableName());
		}
		if (lootTable == null) {
			lootTable = monumentLootTable;
		}
		return lootTable == null ? "Invalid" : lootTable.getName();
	}

	public String getMonumentLootTime() {
		String noColor = "";
		int timerCount = Math.max(lootRefreshTimer.getTime(),0);
		return HelperUtil.getTimeFormat(timerCount, noColor);
	}

	/**
	 * Attempts to fill an inventory with loot.
	 * This method creates a new inventory with loot to display to the player.
	 * @return The new inventory filled with loot, else null when loot could not be updated,
	 * 		   probably due to unexpired refresh time.
	 */
	public Inventory updateMonumentLoot(Inventory chestInventory, KonTown town, Player openPlayer) {
		if (chestInventory == null) return null;
		// Get the loot table
		KonLootTable lootTable = null;
		Inventory originalInventory = chestInventory;
		if (chestInventory instanceof DoubleChestInventory) {
			originalInventory = ((DoubleChestInventory)chestInventory).getRightSide();
		}
		Inventory lootInventory = lootCache.get(originalInventory); // can be null
		ChatUtil.printDebug("Attempting to update loot in town "+town.getName());
		// Verify refresh time
		Date now = new Date();
		Location invLoc = originalInventory.getLocation();
		if (invLoc == null || invLoc.getWorld() == null) return null;
		if (lootRefreshLog.containsKey(invLoc)) {
			long loggedLastFilledTime = lootRefreshLog.get(invLoc);
			Date lastFilledDate = new Date(loggedLastFilledTime);
			if(lastFilledDate.after(new Date(markedRefreshTime))) {
				// Inventory is not yet done refreshing
				ChatUtil.printDebug("  Failed, refresh time has not expired");
				ChatUtil.sendNotice(openPlayer, MessagePath.PROTECTION_NOTICE_LOOT_LATER.getMessage());
				return lootInventory;
			}
		}
		// First, check for town specialization
		if (konquest.getKingdomManager().getIsDiscountEnable()) {
			lootTable = getMonumentSpecialLootTable(town.getSpecializationName());
			if (lootTable != null) {
				ChatUtil.printDebug("  Using special monument loot table "+town.getSpecializationName());
			}
		}
		// Second, check for custom kingdom monument template loot
		if (lootTable == null && !town.getKingdom().getMonumentTemplate().isLootTableDefault()) {
			lootTable = getMonumentCustomLootTable(town.getKingdom().getMonumentTemplate().getLootTableName());
			if (lootTable != null) {
				ChatUtil.printDebug("  Using custom monument loot table "+town.getKingdom().getMonumentTemplate().getLootTableName());
			}
		}
		// Finally, use the default
		if (lootTable == null) {
			lootTable = monumentLootTable;
			ChatUtil.printDebug("   Using default monument loot table");
		}
		if (lootTable == null || lootTable.isEmptyLoot()) {
			ChatUtil.printDebug("  Failed, got missing or empty monument loot table");
			return lootInventory;
		}

		/* Passed all checks */

		// Make loot inventory
		lootInventory = konquest.getPlugin().getServer().createInventory(originalInventory.getHolder(), originalInventory.getSize(), lootTable.getName());
		// Get number of loot items
		int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.LOOT);
		int upgradedLootCount = monumentLootCount + upgradeLevel;
		// Update the inventory with loot
		clearUpperInventory(originalInventory);
		// Generate new items
		int finalItemCount = 0;
		int availableSlot;
		for (int i=0; i<upgradedLootCount; i++) {
			availableSlot = lootInventory.firstEmpty();
			if(availableSlot != -1) {
				lootInventory.setItem(availableSlot,lootTable.chooseRandomItem());
				finalItemCount++;
			}
		}
		long lootLastFilledTime = now.getTime();
		lootRefreshLog.put(invLoc,lootLastFilledTime);
		lootCache.put(originalInventory,lootInventory);
		// Play new loot sound
		invLoc.getWorld().playSound(invLoc, Sound.ENTITY_PLAYER_LEVELUP, (float)1.0, (float)1.0);
		// Execute custom commands from config
		konquest.executeCustomCommand(CustomCommandPath.TOWN_MONUMENT_LOOT_OPEN,openPlayer);
		ChatUtil.printDebug("  Success, updated "+finalItemCount+" items");
		return lootInventory;
	}

	/*
	 * Ruin Loot
	 */

	private KonLootTable getRuinCustomLootTable(String name) {
		String tableKey = name.toLowerCase();
		if (ruinCustomLootTable.containsKey(tableKey)) {
			return ruinCustomLootTable.get(tableKey);
		}
		// No table found
		return null;
	}

	public ArrayList<String> getRuinLootTableKeys() {
		ArrayList<String> result = new ArrayList<>();
		result.add(defaultLootTableName);
		result.addAll(ruinCustomLootTable.keySet());
		return result;
	}

	public boolean isRuinLootTable(String name) {
		return name.equalsIgnoreCase(defaultLootTableName) ||
				ruinCustomLootTable.containsKey(name.toLowerCase());
	}

	public String getRuinLootDisplayName(KonRuin ruin) {
		KonLootTable lootTable = null;
		if (!ruin.isLootTableDefault()) {
			lootTable = getRuinCustomLootTable(ruin.getLootTableName());
		}
		if (lootTable == null) {
			lootTable = ruinLootTable;
		}
		return lootTable == null ? "Invalid" : lootTable.getName();
	}

	/**
	 * Attempts to fill an inventory with loot under the following conditions:
	 * - The inventory has not already been emptied
	 * - The ruin is in capture cooldown state
	 * @return The new inventory filled with loot, else null when loot could not be updated,
	 * 		   probably due to already being captured.
	 */
	public Inventory updateRuinLoot(Inventory chestInventory, KonRuin ruin, Player openPlayer) {
		if (chestInventory == null) return null;
		// Get the loot table
		KonLootTable lootTable = null;
		Inventory originalInventory = chestInventory;
		if (chestInventory instanceof DoubleChestInventory) {
			originalInventory = ((DoubleChestInventory)chestInventory).getRightSide();
		}
		Inventory lootInventory = lootCache.get(originalInventory); // can be null
		ChatUtil.printDebug("Attempting to update loot in ruin "+ruin.getName());
		// Verify capture status
		Location invLoc = originalInventory.getLocation();
		if (invLoc == null || invLoc.getWorld() == null) return null;
		int count = ruinLootCount;
		boolean isLootAfterCapture = konquest.getCore().getBoolean(CorePath.RUINS_LOOT_AFTER_CAPTURE.getPath());
		boolean isRuinCaptured = ruin.isCaptureDisabled();
		if(isLootAfterCapture && !isRuinCaptured) {
			// This ruin has not been capture yet
			ChatUtil.printDebug("  Failed, the ruin has not yet been captured");
			ChatUtil.sendNotice(openPlayer, MessagePath.PROTECTION_NOTICE_LOOT_CAPTURE.getMessage());
			return lootInventory;
		}
		if(ruinLootEmptiedLog.containsKey(invLoc) && ruinLootEmptiedLog.get(invLoc)) {
			// Inventory has been emptied already
			ChatUtil.printDebug("  Failed, the inventory has already been updated");
			ChatUtil.sendNotice(openPlayer, MessagePath.PROTECTION_NOTICE_LOOT_CAPTURE.getMessage());
			return lootInventory;
		}
		// First, check for custom ruin loot
		if (!ruin.isLootTableDefault()) {
			lootTable = getRuinCustomLootTable(ruin.getLootTableName());
			if (lootTable != null) {
				ChatUtil.printDebug("  Using custom ruin loot table "+ruin.getLootTableName());
			}
		}
		// Finally, use the default
		if (lootTable == null) {
			lootTable = ruinLootTable;
			ChatUtil.printDebug("  Using default ruin loot table");
		}
		if (lootTable == null || lootTable.isEmptyLoot()) {
			ChatUtil.printDebug("  Failed, got missing or empty ruin loot table");
			return lootInventory;
		}

		/* Passed all checks */

		// Make loot inventory
		lootInventory = konquest.getPlugin().getServer().createInventory(originalInventory.getHolder(), originalInventory.getSize(), lootTable.getName());
		// Update the inventory with loot
		clearUpperInventory(originalInventory);
		// Generate new items
		int finalItemCount = 0;
		int availableSlot;
		for(int i=0;i<count;i++) {
			availableSlot = lootInventory.firstEmpty();
			if(availableSlot != -1) {
				lootInventory.setItem(availableSlot,lootTable.chooseRandomItem());
				finalItemCount++;
			}
		}
		ruinLootEmptiedLog.put(invLoc,true); // This inventory has been emptied
		lootCache.put(originalInventory,lootInventory);
		// Play new loot sound
		invLoc.getWorld().playSound(invLoc, Sound.ENTITY_PLAYER_LEVELUP, (float)1.0, (float)1.0);
		// Execute custom commands from config
		konquest.executeCustomCommand(CustomCommandPath.RUIN_LOOT_OPEN,openPlayer);
		ChatUtil.printDebug("  Success, updated "+finalItemCount+" items");
		return lootInventory;
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
