package com.github.rumsfield.konquest.manager;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonMonumentTemplate;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.model.KonUpgrade;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import com.github.rumsfield.konquest.utility.Timeable;
import com.github.rumsfield.konquest.utility.Timer;
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
import java.util.Random;

public class LootManager implements Timeable{

	private final Konquest konquest;
	private final HashMap<Location, Long> lootRefreshLog;
	private long refreshTimeSeconds;
	private long markedRefreshTime;
	private int lootCount;
	private final Random randomness;
	private final Timer lootRefreshTimer;
	private final HashMap<ItemStack,Integer> lootTable;

	
	public LootManager(Konquest konquest) {
		this.konquest = konquest;
		this.refreshTimeSeconds = 0;
		this.markedRefreshTime = 0;
		this.lootRefreshLog = new HashMap<>();
		this.lootCount = 0;
		this.randomness = new Random();
		this.lootRefreshTimer = new Timer(this);
		this.lootTable = new HashMap<>();
	}
	
	public void initialize() {
		// Parse config for refresh time
		refreshTimeSeconds = konquest.getConfigManager().getConfig("core").getLong("core.monuments.loot_refresh",(long)0.0);
		if(refreshTimeSeconds > 0) {
			lootRefreshTimer.stopTimer();
			lootRefreshTimer.setTime((int)refreshTimeSeconds);
			lootRefreshTimer.startLoopTimer();
		}
		markedRefreshTime = new Date().getTime();
		lootCount = konquest.getConfigManager().getConfig("core").getInt("core.monuments.loot_count",0);
		if(lootCount < 0) {
			lootCount = 0;
		}
		if(loadLoot()) {
			ChatUtil.printConsoleAlert("Loaded loot table from loot.yml");
		} else {
			ChatUtil.printConsoleError("Failed to load loot table, check for syntax errors.");
		}
		ChatUtil.printDebug("Loot Manager is ready with loot count: "+lootCount);
	}
	
	// Loads loot table from file
	private boolean loadLoot() {
		lootTable.clear();
		
		FileConfiguration lootConfig = konquest.getConfigManager().getConfig("loot");
        if (lootConfig.get("loot") == null) {
        	ChatUtil.printDebug("There is no loot section in loot.yml");
            return false;
        }
        ConfigurationSection lootEntry;
        boolean status;
        ChatUtil.printDebug("Loading loot...");
        
        // Load items
        Material itemType = null;
        ConfigurationSection itemsSection = lootConfig.getConfigurationSection("loot.items");
        if(itemsSection != null) {
        	for(String itemName : itemsSection.getKeys(false)) {
        		status = true;
        		int itemAmount = 0;
            	int itemWeight = 0;
        		try {
        			itemType = Material.valueOf(itemName);
        		} catch(IllegalArgumentException e) {
            		ChatUtil.printConsoleError("Invalid loot item \""+itemName+"\" given in loot.yml, skipping this item.");
            		status = false;
        		}
            	lootEntry = itemsSection.getConfigurationSection(itemName);
            	if(lootEntry != null) {
	            	if(lootEntry.contains("amount")) {
	            		itemAmount = lootEntry.getInt("amount",1);
	            		itemAmount = Math.max(itemAmount, 1);
	        		} else {
	        			ChatUtil.printConsoleError("loot.yml is missing amount for item: "+itemName);
	        			status = false;
	        		}
	            	if(lootEntry.contains("weight")) {
	            		itemWeight = lootEntry.getInt("weight",0);
	            		itemWeight = Math.max(itemWeight, 0);
	        		} else {
	        			ChatUtil.printConsoleError("loot.yml is missing weight for item: "+itemName);
	        			status = false;
	        		}
            	} else {
            		status = false;
            		ChatUtil.printConsoleError("loot.yml contains invalid item: "+itemName);
            	}
            	if(status && itemWeight > 0) {
            		// Add loot table entry
            		lootTable.put(new ItemStack(itemType, itemAmount), itemWeight);
            		ChatUtil.printDebug("  Added loot item "+itemName+" with amount "+itemAmount+", weight "+itemWeight);
            	}
            }
        }
        
        // Load potions
        PotionType potionType = null;
        ConfigurationSection potionsSection = lootConfig.getConfigurationSection("loot.potions");
        if(potionsSection != null) {
        	for(String potionName : potionsSection.getKeys(false)) {
        		status = true;
        		boolean itemUpgraded = false;
        		boolean itemExtended = false;
            	int itemWeight = 0;
        		try {
        			potionType = PotionType.valueOf(potionName);
        		} catch(IllegalArgumentException e) {
            		ChatUtil.printConsoleError("Invalid loot potion \""+potionName+"\" given in loot.yml, skipping this potion.");
            		status = false;
        		}
            	lootEntry = potionsSection.getConfigurationSection(potionName);
            	if(lootEntry != null) {
	            	if(lootEntry.contains("upgraded")) {
	            		itemUpgraded = lootEntry.getBoolean("upgraded",false);
	        		} else {
	        			ChatUtil.printConsoleError("loot.yml is missing upgraded for potion: "+potionName);
	        			status = false;
	        		}
	            	if(lootEntry.contains("extended")) {
	            		itemExtended = lootEntry.getBoolean("extended",false);
	        		} else {
	        			ChatUtil.printConsoleError("loot.yml is missing extended for potion: "+potionName);
	        			status = false;
	        		}
	            	if(lootEntry.contains("weight")) {
	            		itemWeight = lootEntry.getInt("weight",0);
	            		itemWeight = Math.max(itemWeight, 0);
	        		} else {
	        			ChatUtil.printConsoleError("loot.yml is missing weight for potion: "+potionName);
	        			status = false;
	        		}
            	} else {
            		status = false;
            		ChatUtil.printConsoleError("loot.yml contains invalid potion: "+potionName);
            	}
            	if(status && itemWeight > 0) {
            		// Add loot table entry
            		ItemStack potion = new ItemStack(Material.POTION, 1);
    				PotionMeta meta;
    				meta = (PotionMeta) potion.getItemMeta();
    				try {
    					meta.setBasePotionData(new PotionData(potionType, itemExtended, itemUpgraded));
    				} catch(IllegalArgumentException e) {
    					meta.setBasePotionData(new PotionData(potionType, false, false));
    					ChatUtil.printConsoleError("Invalid options extended="+itemExtended+", upgraded="+itemUpgraded+" for potion "+potionName+" in loot.yml");
    				}
    				potion.setItemMeta(meta);
            		lootTable.put(potion, itemWeight);
            		ChatUtil.printDebug("  Added loot potion "+potionName+" with extended "+itemExtended+", upgraded "+itemUpgraded+", weight "+itemWeight);
            	}
            }
        }
        
        // Load enchanted books
        Enchantment bookType;
        ConfigurationSection ebookSection = lootConfig.getConfigurationSection("loot.enchanted_books");
        if(ebookSection != null) {
        	for(String enchantName : ebookSection.getKeys(false)) {
        		status = true;
        		int itemLevel = 0;
            	int itemWeight = 0;
        		bookType = getEnchantment(enchantName);
            	if(bookType == null) {
            		ChatUtil.printConsoleError("Invalid loot enchantment \""+enchantName+"\" given in loot.yml, skipping this enchantment.");
            		status = false;
        		}
            	lootEntry = ebookSection.getConfigurationSection(enchantName);
            	if(lootEntry != null) {
	            	if(lootEntry.contains("level")) {
	            		itemLevel = lootEntry.getInt("level",0);
	            		itemLevel = Math.max(itemLevel, 0);
	        		} else {
	        			ChatUtil.printConsoleError("loot.yml is missing level for enchantment: "+enchantName);
	        			status = false;
	        		}
	            	if(lootEntry.contains("weight")) {
	            		itemWeight = lootEntry.getInt("weight",0);
	            		itemWeight = Math.max(itemWeight, 0);
	        		} else {
	        			ChatUtil.printConsoleError("loot.yml is missing weight for enchantment: "+enchantName);
	        			status = false;
	        		}
            	} else {
            		status = false;
            		ChatUtil.printConsoleError("loot.yml contains invalid enchanted book: "+enchantName);
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
            		lootTable.put(enchantBook, itemWeight);
            		ChatUtil.printDebug("  Added loot enchant "+enchantName+" with level "+itemLevel+", weight "+itemWeight);
            	}
            }
        }
        
        return true;
	}
	
	private Enchantment getEnchantment(String fieldName) {
		Enchantment result = null;
		try {
			Class<?> c = Enchantment.class;
			Field field = c.getDeclaredField(fieldName);
			result = (Enchantment)field.get(null);
		} catch(NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ignored) {}
		return result;
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
		fillLoot(inventory, count);
		long lootLastFilledTime = now.getTime();
		lootRefreshLog.put(invLoc,lootLastFilledTime);
		return true;
	}
	
	public boolean updateMonumentLoot(Inventory inventory) {
		return updateMonumentLoot(inventory,lootCount);
	}
	
	public boolean updateMonumentLoot(Inventory inventory, KonTown town) {
		int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.LOOT);
		int upgradedLootCount = lootCount + upgradeLevel;
		return updateMonumentLoot(inventory,upgradedLootCount);
	}
	
	private void clearUpperInventory(Inventory inventory) {
		inventory.clear();
	}
	
	private void fillLoot(Inventory inventory, int count) {
		int availableSlot;
		// Generate new items
		for(int i=0;i<count;i++) {
			availableSlot = inventory.firstEmpty();
			if(availableSlot == -1) {
				ChatUtil.printDebug("Failed to find empty slot for generated loot in inventory "+ inventory);
			} else {
				inventory.setItem(availableSlot,chooseRandomItem(lootTable));
			}
		}
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
			int typeChoice = randomness.nextInt(total);
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
							int newLevel = randomness.nextInt(e.getMaxLevel()+1);
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
