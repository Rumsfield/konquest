package konquest.listener;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.api.model.KonquestUpgrade;
import konquest.manager.PlayerManager;
import konquest.model.KonCamp;
import konquest.model.KonCapital;
import konquest.model.KonDirective;
import konquest.model.KonPlayer;
import konquest.model.KonStatsType;
import konquest.model.KonTerritory;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
//import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
//import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BlockInventoryHolder;
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
		if(konquest.isWorldIgnored(event.getInventory().getLocation())) {
			return;
		}
		// Monitor blocks in claimed territory
		// Check for merchant trades in claimed territory of guilds
		Location openLoc = event.getInventory().getLocation();
		
		if(openLoc != null && konquest.getKingdomManager().isChunkClaimed(openLoc)) {
			
			if(!konquest.getPlayerManager().isOnlinePlayer((Player)event.getPlayer())) {
				ChatUtil.printDebug("Failed to handle onInventoryOpen for non-existent player");
				return;
			}
			KonPlayer player = playerManager.getPlayer((Player)event.getPlayer());
			// Bypass event restrictions for player in Admin Bypass Mode
			if(!player.isAdminBypassActive()) {
				//ChatUtil.printDebug("inventoryOpen Evaluating territory");
				KonTerritory territory = konquest.getKingdomManager().getChunkTerritory(openLoc);
				// Prevent all inventory openings inside Capitals
				boolean isCapitalUseEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.capital_use",false);
				if(territory instanceof KonCapital && !isCapitalUseEnabled) {
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
					// Protect land and plots
					if(event.getInventory().getHolder() instanceof BlockInventoryHolder ||
							 event.getInventory().getHolder() instanceof DoubleChest ||
							 event.getInventory().getHolder() instanceof Vehicle) {
						// Inventory can hold items, or is a vehicle with storage
						// Protect land and plots
						if(!town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer())) {
							// Stop all edits by non-resident in closed towns
							ChatUtil.sendError((Player)event.getPlayer(), MessagePath.PROTECTION_ERROR_NOT_RESIDENT.getMessage(territory.getName()));
							event.setCancelled(true);
							return;
						}
						if(town.isOpen() || (!town.isOpen() && town.isPlayerResident(player.getOfflineBukkitPlayer()))) {
							// Check for protections in open towns and closed town residents
							if(konquest.getPlotManager().isPlayerPlotProtectContainer(town, openLoc, player.getBukkitPlayer())) {
								// Stop when player edits plot that isn't theirs
								ChatUtil.sendError((Player)event.getPlayer(), MessagePath.PROTECTION_ERROR_NOT_PLOT.getMessage());
								event.setCancelled(true);
								return;
							}
							if(town.isPlotOnly() && !town.isPlayerKnight(player.getOfflineBukkitPlayer()) && !town.hasPlot(openLoc)) {
								// Stop when non-elite player edits non-plot land
								ChatUtil.sendError((Player)event.getPlayer(), MessagePath.PROTECTION_ERROR_ONLY_PLOT.getMessage());
								event.setCancelled(true);
								return;
							}
						}
					}
					// Attempt to put loot into empty chests within the monument
					//if(town.isLocInsideCenterChunk(openLoc) && event.getInventory().getHolder() instanceof Chest) {
					if(town.getMonument().isLocInside(openLoc) && event.getInventory().getType().equals(InventoryType.CHEST)) {
						if(town.isPlayerLord(player.getOfflineBukkitPlayer()) || town.isPlayerKnight(player.getOfflineBukkitPlayer())) {
							//if(isInventoryEmpty(event.getInventory())) {
								// Update loot with default count as defined in core YML
								boolean result = konquest.getLootManager().updateMonumentLoot(event.getInventory(), town);
								ChatUtil.printDebug("Attempted to update loot in town "+territory.getName()+", got "+result);
								if(result) {
									event.getInventory().getLocation().getWorld().playSound(event.getInventory().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, (float)1.0, (float)1.0);
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
					// Attempt to modify merchant trades based on guild specialization and relationships with player
					konquest.getGuildManager().applyTradeDiscounts(player, town, event.getInventory());
				}
				
				// Prevent all inventory openings except for camp owner and clan members when allowed
				boolean isCampContainersProtected = konquest.getConfigManager().getConfig("core").getBoolean("core.camps.protect_containers", true);
				if(territory instanceof KonCamp && isCampContainersProtected) {
					KonCamp camp = (KonCamp) territory;
					boolean isMemberAllowed = konquest.getConfigManager().getConfig("core").getBoolean("core.camps.clan_allow_containers", false);
					boolean isMember = false;
					if(konquest.getCampManager().isCampGrouped(camp)) {
						isMember = konquest.getCampManager().getCampGroup(camp).isPlayerMember(player.getBukkitPlayer());
					}
					if(!(isMember && isMemberAllowed) && !player.getBukkitPlayer().getUniqueId().equals(camp.getOwner().getUniqueId())) {
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
			konquest.getDisplayManager().onDisplayMenuClose(event.getInventory(), event.getPlayer());
		}
	}
	
	@EventHandler(priority = EventPriority.LOW)
    public void onDisplayMenuClick(InventoryClickEvent event) {
		// When a player clicks inside of a display menu inventory
		if(konquest.getDisplayManager().isDisplayMenu(event.getClickedInventory())) {
			int slot = event.getRawSlot();
			Player bukkitPlayer = (Player) event.getWhoClicked();
			KonPlayer player = konquest.getPlayerManager().getPlayer(bukkitPlayer);
			if(player != null && slot < event.getView().getTopInventory().getSize()) {
				event.setResult(Event.Result.DENY);
				/*
				// DEBUG
				String action = event.getAction().name();
				String type = event.getClick().name();
				String result = event.getResult().name();
				ChatUtil.printDebug("Display click type "+type+", action "+action+", result "+result);
				// END DEBUG
				*/
				event.setCancelled(true);
				if(event.getClick().equals(ClickType.LEFT) || event.getClick().equals(ClickType.RIGHT)) {
					boolean clickType = (event.getClick().equals(ClickType.RIGHT)) ? false : true;
					Bukkit.getScheduler().runTask(konquest.getPlugin(), new Runnable() {
			            @Override
			            public void run() {
			            	konquest.getDisplayManager().onDisplayMenuClick(player, event.getClickedInventory(), slot, clickType);
			            }
			        });
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
		if(!event.isCancelled()) {
			// Check for picking up netherite items for stats
			if(event.getAction().equals(InventoryAction.PICKUP_ALL) && event.getClickedInventory() instanceof SmithingInventory &&
					!konquest.isWorldIgnored(event.getInventory().getLocation())) {
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
		if(konquest.isWorldIgnored(event.getInventory().getLocation())) {
			return;
		}
		HumanEntity human = event.getWhoClicked();
		if(!event.isCancelled() && human instanceof Player) {
			if(!konquest.getPlayerManager().isOnlinePlayer((Player)human)) {
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
		if(konquest.isWorldIgnored(event.getBlock().getWorld())) {
			return;
		}
		if(!konquest.getPlayerManager().isOnlinePlayer(event.getPlayer())) {
			ChatUtil.printDebug("Failed to handle onFurnaceExtract for non-existent player");
			return;
		}
		KonPlayer player = playerManager.getPlayer(event.getPlayer());
		Material extractMat = event.getItemType();
		int stackSize = event.getItemAmount();
		if(extractMat.toString().toLowerCase().contains("ingot") ||
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
			if(konquest.isWorldIgnored(event.getInventory().getLocation())) {
				return;
			}
			if(!konquest.getPlayerManager().isOnlinePlayer(event.getEnchanter())) {
				ChatUtil.printDebug("Failed to handle onEnchantItem for non-existent player");
				return;
			}
			KonPlayer player = playerManager.getPlayer(event.getEnchanter());
			konquest.getDirectiveManager().updateDirectiveProgress(player, KonDirective.ENCHANT_ITEM);
			konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.ENCHANTMENTS,1);
			Location enchantLoc = event.getEnchantBlock().getLocation();
			if(enchantLoc != null && konquest.getKingdomManager().isChunkClaimed(enchantLoc)) {
				KonTerritory territory = konquest.getKingdomManager().getChunkTerritory(enchantLoc);
				if(territory instanceof KonTown) {
					KonTown town = (KonTown)territory;
					int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonquestUpgrade.ENCHANT);
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
			if(konquest.isWorldIgnored(event.getInventory().getLocation())) {
				return;
			}
			Location enchantLoc = event.getEnchantBlock().getLocation();
			if(enchantLoc != null && konquest.getKingdomManager().isChunkClaimed(enchantLoc)) {
				KonTerritory territory = konquest.getKingdomManager().getChunkTerritory(enchantLoc);
				if(territory instanceof KonTown) {
					KonTown town = (KonTown)territory;
					int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonquestUpgrade.ENCHANT);
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
