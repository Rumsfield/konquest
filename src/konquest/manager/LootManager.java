package konquest.manager;

import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import konquest.Konquest;
import konquest.model.KonPlayer;
import konquest.model.KonTown;
import konquest.model.KonUpgrade;
import konquest.utility.ChatUtil;
import konquest.utility.Timeable;
import konquest.utility.Timer;

public class LootManager implements Timeable{

	private Konquest konquest;
	private HashMap<Location, Long> lootRefreshLog;
	private long refreshTimeSeconds;
	private long markedRefreshTime;
	private int lootCount;
	private Random randomness;
	private Timer lootRefreshTimer;
	
	/*
	 * Monument loot is divided into different categories:
	 * Valuables - Gold Ingots, Iron Ingots, Diamonds, Emeralds
	 * Enchantment Books - Randomly applied enchantments on books
	 * Eggs - Randomly generated eggs
	 * Exotics - end stone, prismarine, purpur, mycelium
	 * Food - cooked meat, etc
	 * Others - Saddle, End Pearl, Blaze Rod, Music Disc
	 */
	public enum LootType {
		VALUABLE (4),
		POTIONS (9),
		FOOD (6),
		HORSE (4),
		EXTRA (4),
		JUNK (2),
		MUSIC (1),
		ENCHANTMENT_BOOK (2);
		private final int p;
		private static int totalP = 32;
		LootType(int p) {
			this.p = p;
		}
		public int probability() {
			return p;
		}
		public static int total() {
			return totalP;
		}
	}
	
	public LootManager(Konquest konquest) {
		this.konquest = konquest;
		this.refreshTimeSeconds = (long)0.0;
		this.markedRefreshTime = (long)0.0;
		this.lootRefreshLog = new HashMap<Location, Long>();
		this.lootCount = 0;
		this.randomness = new Random();
		this.lootRefreshTimer = new Timer(this);
	}
	
	public void initialize() {
		// Parse config for refresh time
		refreshTimeSeconds = konquest.getConfigManager().getConfig("core").getLong("core.monuments.loot_refresh",(long)0.0);
		lootRefreshTimer.stopTimer();
		lootRefreshTimer.setTime((int)refreshTimeSeconds);
		lootRefreshTimer.startLoopTimer();
		markedRefreshTime = new Date().getTime();
		lootCount = konquest.getConfigManager().getConfig("core").getInt("core.monuments.loot_count",1);
		ChatUtil.printDebug("Loot Manager is ready");
	}
	
	/**
	 * Attempts to fill an inventory with loot
	 * @param inventory - the inventory to try and fill
	 * @param count - number of ItemStacks to put into the inventory
	 * @return true: The inventory was successfully filled with loot
	 * 		   false: The inventory was not filled, probably due to unexpired refresh time
	 */
	public boolean updateMonumentLoot(Inventory inventory, int count) {
		//ChatUtil.printDebug("Attempting to update monument loot in "+inventory.toString());
		Date now = new Date();
		Location invLoc = inventory.getLocation();
		//ChatUtil.printDebug("Updating monument loot...");
		if(lootRefreshLog.containsKey(invLoc)) {
			//ChatUtil.printDebug("Found a logged inventory");
			long loggedLastFilledTime = lootRefreshLog.get(invLoc);
			Date lastFilledDate = new Date(loggedLastFilledTime);
			if(lastFilledDate.after(new Date(markedRefreshTime))) {
				// Inventory is not yet done refreshing
				//ChatUtil.printDebug("...Found unexpired loot");
				return false;
			}
		}
		clearUpperInventory(inventory);
		fillLoot(inventory, count);
		//long refreshFinishedTime = now.getTime() + (refreshTimeSeconds*1000);
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
		//ChatUtil.printDebug("...Filling loot");
		int availableSlot = 0;
		// Generate new items
		for(int i=0;i<count;i++) {
			availableSlot = inventory.firstEmpty();
			if(availableSlot == -1) {
				ChatUtil.printDebug("Failed to find empty slot for generated loot in inventory "+inventory.toString());
			} else {
				inventory.setItem(availableSlot,generateRandomLoot());
				//ChatUtil.printDebug("...Setting item in slot "+availableSlot);
			}
		}
	}
	
	private ItemStack generateRandomLoot() {
		// Randomly choose LootType
		int typeChoice = randomness.nextInt(LootType.total());
		int typeWindow = 0;
		LootType type = LootType.values()[0];
		for(LootType t : LootType.values()) {
			if(typeChoice < typeWindow + t.probability()) {
				type = t;
				break;
			}
			typeWindow = typeWindow + t.probability();
		}
		//ChatUtil.printDebug("Generating Loot Type "+type.toString()+" from choice "+typeChoice);
		// Randomly create item stack relevant to the type
		ItemStack item = null;
		HashMap<ItemStack,Integer> itemOptions = new HashMap<ItemStack,Integer>();
		switch(type) {
			case VALUABLE:
				itemOptions.put(new ItemStack(Material.GOLD_INGOT, 	18), 	1);
				itemOptions.put(new ItemStack(Material.IRON_INGOT, 	36), 	1);
				itemOptions.put(new ItemStack(Material.DIAMOND, 	4), 	1);
				itemOptions.put(new ItemStack(Material.EMERALD, 	18), 	1);
				itemOptions.put(new ItemStack(Material.COAL, 		64), 	1);
				itemOptions.put(new ItemStack(Material.GUNPOWDER, 	20), 	1);
				item = chooseRandomItem(itemOptions);
				break;
			case POTIONS:
				itemOptions.put(new ItemStack(Material.BLAZE_ROD, 		  8), 6);
				itemOptions.put(new ItemStack(Material.NETHER_WART, 	  8), 6);
				itemOptions.put(new ItemStack(Material.GLOWSTONE_DUST, 	 12), 6);
				itemOptions.put(new ItemStack(Material.EXPERIENCE_BOTTLE, 5), 4);
				ItemStack potion;
				PotionMeta meta;
				potion = new ItemStack(Material.POTION, 1);
				meta = (PotionMeta) potion.getItemMeta();
				meta.setBasePotionData(new PotionData(PotionType.SPEED, false, false));
				potion.setItemMeta(meta);
				itemOptions.put(potion, 3);
				potion = new ItemStack(Material.POTION, 1);
				meta = (PotionMeta) potion.getItemMeta();
				meta.setBasePotionData(new PotionData(PotionType.REGEN, false, false));
				potion.setItemMeta(meta);
				itemOptions.put(potion, 3);
				potion = new ItemStack(Material.POTION, 1);
				meta = (PotionMeta) potion.getItemMeta();
				meta.setBasePotionData(new PotionData(PotionType.INSTANT_HEAL, false, false));
				potion.setItemMeta(meta);
				itemOptions.put(potion, 3);
				potion = new ItemStack(Material.POTION, 1);
				meta = (PotionMeta) potion.getItemMeta();
				meta.setBasePotionData(new PotionData(PotionType.JUMP, false, false));
				potion.setItemMeta(meta);
				itemOptions.put(potion, 3);
				potion = new ItemStack(Material.POTION, 1);
				meta = (PotionMeta) potion.getItemMeta();
				meta.setBasePotionData(new PotionData(PotionType.SLOW_FALLING, false, false));
				potion.setItemMeta(meta);
				itemOptions.put(potion, 3);
				potion = new ItemStack(Material.POTION, 1);
				meta = (PotionMeta) potion.getItemMeta();
				meta.setBasePotionData(new PotionData(PotionType.FIRE_RESISTANCE, false, false));
				potion.setItemMeta(meta);
				itemOptions.put(potion, 3);
				item = chooseRandomItem(itemOptions);
				break;
			case FOOD:
				itemOptions.put(new ItemStack(Material.COOKED_BEEF, 	8), 	1);
				itemOptions.put(new ItemStack(Material.COOKED_PORKCHOP, 8), 	1);
				itemOptions.put(new ItemStack(Material.COOKED_CHICKEN, 	8), 	1);
				itemOptions.put(new ItemStack(Material.BREAD, 			16), 	1);
				item = chooseRandomItem(itemOptions);
				break;
			case HORSE:
				itemOptions.put(new ItemStack(Material.SADDLE, 						1), 4);
				itemOptions.put(new ItemStack(Material.IRON_HORSE_ARMOR, 			1), 5);
				itemOptions.put(new ItemStack(Material.GOLDEN_HORSE_ARMOR, 			1), 4);
				itemOptions.put(new ItemStack(Material.DIAMOND_HORSE_ARMOR, 		1), 2);
				itemOptions.put(new ItemStack(Material.HORSE_SPAWN_EGG, 			1), 4);
				itemOptions.put(new ItemStack(Material.ZOMBIE_HORSE_SPAWN_EGG, 		1), 1);
				itemOptions.put(new ItemStack(Material.SKELETON_HORSE_SPAWN_EGG, 	1), 1);
				item = chooseRandomItem(itemOptions);
				break;
			case EXTRA:
				itemOptions.put(new ItemStack(Material.ENDER_PEARL, 		4), 2);
				itemOptions.put(new ItemStack(Material.MELON, 				4), 2);
				itemOptions.put(new ItemStack(Material.PUMPKIN, 			4), 2);
				itemOptions.put(new ItemStack(Material.SHEEP_SPAWN_EGG, 	2), 2);
				itemOptions.put(new ItemStack(Material.COW_SPAWN_EGG, 		2), 3);
				itemOptions.put(new ItemStack(Material.BOOK, 				6), 4);
				itemOptions.put(new ItemStack(Material.NAME_TAG, 			1), 1);
				item = chooseRandomItem(itemOptions);
				break;
			case JUNK:
				itemOptions.put(new ItemStack(Material.COBWEB, 				4), 2);
				itemOptions.put(new ItemStack(Material.EGG, 				4), 3);
				itemOptions.put(new ItemStack(Material.FIREWORK_ROCKET, 	4), 2);
				itemOptions.put(new ItemStack(Material.CARROT_ON_A_STICK, 	1), 1);
				item = chooseRandomItem(itemOptions);
				break;
			case MUSIC:
				itemOptions.put(new ItemStack(Material.MUSIC_DISC_11, 		1), 1);
				itemOptions.put(new ItemStack(Material.MUSIC_DISC_13, 		1), 1);
				itemOptions.put(new ItemStack(Material.MUSIC_DISC_BLOCKS, 	1), 1);
				itemOptions.put(new ItemStack(Material.MUSIC_DISC_CAT, 		1), 1);
				itemOptions.put(new ItemStack(Material.MUSIC_DISC_CHIRP, 	1), 1);
				itemOptions.put(new ItemStack(Material.MUSIC_DISC_FAR, 		1), 1);
				itemOptions.put(new ItemStack(Material.MUSIC_DISC_MALL, 	1), 1);
				itemOptions.put(new ItemStack(Material.MUSIC_DISC_MELLOHI, 	1), 1);
				itemOptions.put(new ItemStack(Material.MUSIC_DISC_STAL, 	1), 1);
				itemOptions.put(new ItemStack(Material.MUSIC_DISC_STRAD, 	1), 1);
				itemOptions.put(new ItemStack(Material.MUSIC_DISC_WAIT, 	1), 1);
				itemOptions.put(new ItemStack(Material.MUSIC_DISC_WARD, 	1), 1);
				item = chooseRandomItem(itemOptions);
				break;
			case ENCHANTMENT_BOOK:
				int itemIndex = randomness.nextInt(Enchantment.values().length);
				Enchantment enchant = Enchantment.values()[itemIndex];
				Material itemMaterial = Material.ENCHANTED_BOOK;
				int itemCount = 1;
				int enchantLevel = randomness.nextInt(enchant.getMaxLevel()+1);
				if(enchantLevel < enchant.getStartLevel()) {
					enchantLevel = enchant.getStartLevel();
				}
				item = new ItemStack(itemMaterial, itemCount);
				EnchantmentStorageMeta enchantMeta = (EnchantmentStorageMeta)item.getItemMeta();
				//ChatUtil.printDebug("Adding enchantment "+enchant.toString()+", level "+enchantLevel);
				enchantMeta.addStoredEnchant(enchant, enchantLevel, true);
				item.setItemMeta(enchantMeta);
				break;
			/*
			case EGG:
				itemOptions.put(Material.BLAZE_SPAWN_EGG, 1);
				itemOptions.put(Material.CAT_SPAWN_EGG, 1);
				itemOptions.put(Material.CAVE_SPIDER_SPAWN_EGG, 1);
				itemOptions.put(Material.CREEPER_SPAWN_EGG, 1);
				itemOptions.put(Material.DROWNED_SPAWN_EGG, 1);
				itemOptions.put(Material.ENDERMAN_SPAWN_EGG, 1);
				itemOptions.put(Material.MOOSHROOM_SPAWN_EGG, 1);
				itemOptions.put(Material.PARROT_SPAWN_EGG, 1);
				itemOptions.put(Material.SKELETON_SPAWN_EGG, 1);
				itemOptions.put(Material.SLIME_SPAWN_EGG, 1);
				itemOptions.put(Material.SPIDER_SPAWN_EGG, 1);
				itemOptions.put(Material.VILLAGER_SPAWN_EGG, 1);
				itemOptions.put(Material.ZOMBIE_SPAWN_EGG, 1);
				itemOptions.put(Material.ZOMBIE_PIGMAN_SPAWN_EGG, 1);
				materialList = new ArrayList<Material>(itemOptions.keySet());
				itemIndex = randomness.nextInt(materialList.size());
				itemMaterial = materialList.get(itemIndex);
				itemCount = randomness.nextInt(itemOptions.get(itemMaterial))+1;
				item = new ItemStack(itemMaterial, itemCount);
				break;
			*/
			default:
				break;
		}
		//ChatUtil.printDebug("Generated item is "+item.getType().toString());
		return item;
	}
	
	private ItemStack chooseRandomItem(HashMap<ItemStack,Integer> itemOptions) {
		// Find total item range
		int total = 0;
		for(int p : itemOptions.values()) {
			total = total + p;
		}
		int typeChoice = randomness.nextInt(total);
		int typeWindow = 0;
		ItemStack item = null;
		for(ItemStack i : itemOptions.keySet()) {
			if(typeChoice < typeWindow + itemOptions.get(i)) {
				item = i;
				break;
			}
			typeWindow = typeWindow + itemOptions.get(i);
		}
		//ChatUtil.printDebug("Choosing random item out of "+total+", chose "+typeChoice+", got "+item.getType().toString());
		return item;
	}

	@Override
	public void onEndTimer(int taskID) {
		if(taskID == 0) {
			ChatUtil.printDebug("Loot Refresh Timer ended with null taskID!");
		} else if(taskID == lootRefreshTimer.getTaskID()) {
			markedRefreshTime = new Date().getTime();
			ChatUtil.printDebug("Loot Refresh timer marked new availability time");
			for(KonPlayer player : konquest.getPlayerManager().getPlayersOnline()) {
				if(!player.isBarbarian()) {
					ChatUtil.sendNotice(player.getBukkitPlayer(), "New town monument loot is available.");
				}
			}
		}
	}
}
