package konquest.listener;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.manager.PlayerManager;
import konquest.model.KonCamp;
import konquest.model.KonCapital;
import konquest.model.KonDirective;
import konquest.model.KonPlayer;
import konquest.model.KonStatsType;
import konquest.model.KonTerritory;
import konquest.model.KonTown;
import konquest.model.KonUpgrade;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.BlockInventoryHolder;
//import org.bukkit.inventory.Inventory;
//import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;

public class InventoryListener implements Listener {

	private KonquestPlugin konquestPlugin;
	private Konquest konquest;
	private PlayerManager playerManager;
	
	public InventoryListener(KonquestPlugin plugin) {
		this.konquestPlugin = plugin;
		this.konquest = konquestPlugin.getKonquestInstance();
		this.playerManager = konquest.getPlayerManager();
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryOpen(InventoryOpenEvent event) {
		//ChatUtil.printDebug("EVENT: inventoryOpen");
		// Monitor blocks in claimed territory
		Location openLoc = event.getInventory().getLocation();
		
		if(openLoc != null && konquest.getKingdomManager().isChunkClaimed(openLoc.getChunk())) {
			
			if(!konquest.getPlayerManager().isPlayer((Player)event.getPlayer())) {
				ChatUtil.printDebug("Failed to handle onInventoryOpen for non-existent player");
				return;
			}
			KonPlayer player = playerManager.getPlayer((Player)event.getPlayer());
			// Bypass event restrictions for player in Admin Bypass Mode
			if(!player.isAdminBypassActive()) {
				//ChatUtil.printDebug("inventoryOpen Evaluating territory");
				KonTerritory territory = konquest.getKingdomManager().getChunkTerritory(openLoc.getChunk());
				// Prevent all inventory openings inside Capitals
				if(territory instanceof KonCapital) {
					//ChatUtil.printDebug("Cancelled inventory open event in "+territory.getName());
					ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
					event.setCancelled(true);
					return;
				}
				
				boolean isEnemyInvetoryOpenDenied = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.protect_containers_use");
				
				// Town restrictions for inventories
				if(territory instanceof KonTown) {
					KonTown town = (KonTown) territory;
					// Prevent all inventory openings by enemies
					if(isEnemyInvetoryOpenDenied && !player.getKingdom().equals(town.getKingdom())) {
						//ChatUtil.printDebug("Cancelled inventory open event in "+territory.getName());
						ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
						event.setCancelled(true);
						return;
					}
					// Notify player when there is no lord
					if(town.canClaimLordship(player)) {
						//ChatUtil.sendNotice(player.getBukkitPlayer(), town.getName()+" has no leader! Use \"/k town <name> lord <your name>\" to claim Lordship.");
						ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_TOWN_NOTICE_NO_LORD.getMessage(town.getName(),town.getName(),player.getBukkitPlayer().getName()));
					}
					// Prevent non-residents in closed towns from opening inventories that can hold items
					// However this still allows them to open inventories like enchantment tables, crating bench, etc.
					if(!town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer()) && 
							(event.getInventory().getHolder() instanceof BlockInventoryHolder ||
							 event.getInventory().getHolder() instanceof DoubleChest ||
							 event.getInventory().getHolder() instanceof StorageMinecart)) {
						//ChatUtil.printDebug("Cancelled inventory open event for non-resident in closed town "+territory.getName());
						//ChatUtil.sendNotice(player.getBukkitPlayer(), "You must be a resident to access containers in "+territory.getName(), ChatColor.DARK_RED);
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_NOT_RESIDENT.getMessage(territory.getName()));
						event.setCancelled(true);
						return;
					}
					// Attempt to put loot into empty chests within the monument
					if(town.isLocInsideCenterChunk(openLoc) && event.getInventory().getHolder() instanceof Chest) {
						if(town.isPlayerLord(player.getOfflineBukkitPlayer()) || town.isPlayerElite(player.getOfflineBukkitPlayer())) {
							//if(isInventoryEmpty(event.getInventory())) {
								// Update loot with default count as defined in core YML
								boolean result = konquest.getLootManager().updateMonumentLoot(event.getInventory(), town);
								ChatUtil.printDebug("Attempted to update loot in town "+territory.getName()+", got "+result);
								if(result) {
									Bukkit.getWorld(konquest.getWorldName()).playSound(event.getInventory().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, (float)1.0, (float)1.0);
								} else {
									//ChatUtil.sendNotice(player.getBukkitPlayer(), "Come back later!");
									ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_LOOT_LATER.getMessage());
								}
							//} else {
							//	ChatUtil.sendNotice(player.getBukkitPlayer(), "Empty the chest to get more loot.");
							//}
						} else {
							//ChatUtil.sendNotice(player.getBukkitPlayer(), "You must be a knight or the lord of "+town.getName()+" to open this!");
							ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
							event.setCancelled(true);
							return;
						}
					}
				}
				
				// Prevent all inventory openings except for camp owner
				if(territory instanceof KonCamp && isEnemyInvetoryOpenDenied) {
					KonCamp camp = (KonCamp) territory;
					if(!player.getBukkitPlayer().getUniqueId().equals(camp.getOwner().getUniqueId())) {
						//ChatUtil.printDebug("Cancelled inventory open event in "+territory.getName());
						ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
						event.setCancelled(true);
						return;
					}
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {
		// When a player closes a display menu inventory
		if(konquest.getDisplayManager().isDisplayMenu(event.getInventory())) {
			konquest.getDisplayManager().onDisplayMenuClose(event.getInventory());
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onDisplayMenuClick(InventoryClickEvent event) {
		int slot = event.getRawSlot();
		Player bukkitPlayer = (Player) event.getWhoClicked();
		KonPlayer player = konquest.getPlayerManager().getPlayer(bukkitPlayer);
		// When a player clicks inside of a display menu inventory
		if(player != null && konquest.getDisplayManager().isDisplayMenu(event.getClickedInventory())) {
			if(slot < event.getView().getTopInventory().getSize()) {
				event.setCancelled(true);
				konquest.getDisplayManager().onDisplayMenuClick(player, event.getClickedInventory(), slot);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
		if(!event.isCancelled()) {
			// Check for picking up netherite items for stats
			if(event.getAction().equals(InventoryAction.PICKUP_ALL) && event.getClickedInventory() instanceof SmithingInventory) {
				Material itemType = event.getCurrentItem().getType();
				int slot = event.getRawSlot();
				//ChatUtil.printDebug("Inventory pickup event in smithing table slot "+slot+" with item "+itemType.toString());
				HumanEntity human = event.getWhoClicked();
				KonPlayer player = playerManager.getPlayer((Player)human);
				if(player != null && (itemType.equals(Material.NETHERITE_AXE) ||
						itemType.equals(Material.NETHERITE_HOE) ||
						itemType.equals(Material.NETHERITE_SHOVEL) ||
						itemType.equals(Material.NETHERITE_PICKAXE) ||
						itemType.equals(Material.NETHERITE_SWORD) ||
						itemType.equals(Material.NETHERITE_HELMET) ||
						itemType.equals(Material.NETHERITE_CHESTPLATE) ||
						itemType.equals(Material.NETHERITE_LEGGINGS) ||
						itemType.equals(Material.NETHERITE_BOOTS))) {
					if(slot == 2) {
						// Player crafted a netherite item
						konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.CRAFTED,5);
					}
				}
			}
			/*
			// Prevent putting stuff into monument loot chests
			Location openLoc = event.getInventory().getLocation();
			if(openLoc != null && konquest.getKingdomManager().isChunkClaimed(openLoc.getChunk())) {
				KonPlayer player = playerManager.getPlayer((Player)event.getWhoClicked());
				// Bypass event restrictions for player in Admin Bypass Mode
				if(!player.isAdminBypassActive()) {
					KonTerritory territory = konquest.getKingdomManager().getChunkTerritory(openLoc.getChunk());
					if(territory instanceof KonTown) {
						KonTown town = (KonTown) territory;
						if(town.isLocInsideCenterChunk(openLoc) && event.getInventory().getHolder() instanceof Chest) {
							int slot = event.getRawSlot();
							InventoryAction action = event.getAction();
							ChatUtil.printDebug("Inventory click event in monument loot chest slot "+slot+" with action "+action.toString());
							if(event.getAction().equals(InventoryAction.PLACE_ALL) ||
									event.getAction().equals(InventoryAction.PLACE_ONE) ||
									event.getAction().equals(InventoryAction.PLACE_SOME) ||
									event.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY) ||
									event.getAction().equals(InventoryAction.SWAP_WITH_CURSOR)) {
								if(slot < event.getView().getTopInventory().getSize()) {
									event.setCancelled(true);
									return;
								}
							}
						}
					}
				}
			}*/
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onCraftItem(CraftItemEvent event) {
		HumanEntity human = event.getWhoClicked();
		if(!event.isCancelled() && human instanceof Player) {
			if(!konquest.getPlayerManager().isPlayer((Player)human)) {
				ChatUtil.printDebug("Failed to handle onCraftItem for non-existent player");
				return;
			}
			Material recipeMaterial = event.getRecipe().getResult().getType();
			KonPlayer player = konquest.getPlayerManager().getPlayer((Player)human);
			if(recipeMaterial.equals(Material.IRON_HELMET) ||
					recipeMaterial.equals(Material.IRON_CHESTPLATE) ||
					recipeMaterial.equals(Material.IRON_LEGGINGS) ||
					recipeMaterial.equals(Material.IRON_BOOTS)) {
				// Player crafted a component of iron armor
				konquest.getDirectiveManager().updateDirectiveProgress(player, KonDirective.CRAFT_ARMOR);
				konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.CRAFTED,2);
			} else if(recipeMaterial.equals(Material.DIAMOND_HELMET) ||
					recipeMaterial.equals(Material.DIAMOND_CHESTPLATE) ||
					recipeMaterial.equals(Material.DIAMOND_LEGGINGS) ||
					recipeMaterial.equals(Material.DIAMOND_BOOTS)) {
				// Player crafted a component of diamond armor
				konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.CRAFTED,3);
			} else if(recipeMaterial.equals(Material.GOLDEN_HELMET) ||
					recipeMaterial.equals(Material.GOLDEN_CHESTPLATE) ||
					recipeMaterial.equals(Material.GOLDEN_LEGGINGS) ||
					recipeMaterial.equals(Material.GOLDEN_BOOTS)) {
				// Player crafted a component of gold armor
				konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.CRAFTED,1);
			} else if(recipeMaterial.equals(Material.LEATHER_HELMET) ||
					recipeMaterial.equals(Material.LEATHER_CHESTPLATE) ||
					recipeMaterial.equals(Material.LEATHER_LEGGINGS) ||
					recipeMaterial.equals(Material.LEATHER_BOOTS)) {
				// Player crafted a component of leather armor
				konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.CRAFTED,1);
			} else if(recipeMaterial.equals(Material.IRON_AXE) ||
					recipeMaterial.equals(Material.IRON_HOE) ||
					recipeMaterial.equals(Material.IRON_SHOVEL) ||
					recipeMaterial.equals(Material.IRON_PICKAXE) ||
					recipeMaterial.equals(Material.IRON_SWORD)) {
				// Player crafted an iron item
				konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.CRAFTED,1);
			} else if(recipeMaterial.equals(Material.DIAMOND_AXE) ||
					recipeMaterial.equals(Material.DIAMOND_HOE) ||
					recipeMaterial.equals(Material.DIAMOND_SHOVEL) ||
					recipeMaterial.equals(Material.DIAMOND_PICKAXE) ||
					recipeMaterial.equals(Material.DIAMOND_SWORD)) {
				// Player crafted a diamond item
				konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.CRAFTED,2);
			}
			// Check for edible item
			if(recipeMaterial.isEdible()) {
				konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FOOD,event.getCurrentItem().getAmount());
			}
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
		if(!konquest.getPlayerManager().isPlayer(event.getPlayer())) {
			ChatUtil.printDebug("Failed to handle onFurnaceExtract for non-existent player");
			return;
		}
		KonPlayer player = playerManager.getPlayer(event.getPlayer());
		Material extractMat = event.getItemType();
		int stackSize = event.getItemAmount();
		if(extractMat.equals(Material.IRON_INGOT) ||
				extractMat.equals(Material.GOLD_INGOT) ||
				extractMat.equals(Material.NETHERITE_SCRAP) ||
				extractMat.equals(Material.BRICK)) {
			konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.INGOTS,stackSize);
		} else if(extractMat.isEdible()) {
			konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FOOD,stackSize);
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onEnchantItem(EnchantItemEvent event) {
		if(!event.isCancelled()) {
			if(!konquest.getPlayerManager().isPlayer(event.getEnchanter())) {
				ChatUtil.printDebug("Failed to handle onEnchantItem for non-existent player");
				return;
			}
			KonPlayer player = playerManager.getPlayer(event.getEnchanter());
			konquest.getDirectiveManager().updateDirectiveProgress(player, KonDirective.ENCHANT_ITEM);
			konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.ENCHANTMENTS,1);
			Location enchantLoc = event.getEnchantBlock().getLocation();
			if(enchantLoc != null && konquest.getKingdomManager().isChunkClaimed(enchantLoc.getChunk())) {
				KonTerritory territory = konquest.getKingdomManager().getChunkTerritory(enchantLoc.getChunk());
				if(territory instanceof KonTown) {
					KonTown town = (KonTown)territory;
					int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.ENCHANT);
					if(upgradeLevel >= 1) {
						// Add 1 (if possible) to all applied enchantment levels
						Map<Enchantment,Integer> enchantsToAdd = event.getEnchantsToAdd();
						for(Enchantment enchant : enchantsToAdd.keySet()) {
							int enchantLevel = enchantsToAdd.get(enchant);
							if(enchantLevel < enchant.getMaxLevel()) {
								enchantsToAdd.replace(enchant, enchantLevel+1);
							}
						}
					}
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
		if(!event.isCancelled()) {
			Location enchantLoc = event.getEnchantBlock().getLocation();
			if(enchantLoc != null && konquest.getKingdomManager().isChunkClaimed(enchantLoc.getChunk())) {
				KonTerritory territory = konquest.getKingdomManager().getChunkTerritory(enchantLoc.getChunk());
				if(territory instanceof KonTown) {
					KonTown town = (KonTown)territory;
					int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.ENCHANT);
					if(upgradeLevel >= 1) {
						// Add 1 (if possible) to all enchantment offers
						EnchantmentOffer[] offers = event.getOffers();
						for(int i=0;i<offers.length;i++) {
							if(offers[i] != null) {
								int enchantLevel = offers[i].getEnchantmentLevel();
								int enchantMaxLevel = offers[i].getEnchantment().getMaxLevel();
								if(enchantLevel < enchantMaxLevel) {
									offers[i].setEnchantmentLevel(enchantLevel+1);
									//ChatUtil.printDebug("Added 1 level to enchantment offer "+offers[i].getEnchantment().getKey().getNamespace()+" in upgraded town");
								}
							}
						}
					}
				}
			}
		}
	}
	
	/*
	private boolean isInventoryEmpty(Inventory inv) {
		//ChatUtil.printDebug("Checking for empty inventory "+inv.toString());
		boolean result = true;
		for(ItemStack item : inv.getStorageContents()) {
			if(item != null) {
				//ChatUtil.printDebug("Found non-null inventory item! It's not empty.");
				result = false;
				break;
			}
		}
		return result;
	}
	*/
}
