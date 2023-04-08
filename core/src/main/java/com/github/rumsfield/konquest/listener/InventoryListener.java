package com.github.rumsfield.konquest.listener;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.api.model.KonquestRelationshipType;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.manager.KingdomManager;
import com.github.rumsfield.konquest.manager.PlayerManager;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.DoubleChest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.SmithingInventory;

import java.util.Map;

public class InventoryListener implements Listener {

	private final Konquest konquest;
	private final KingdomManager kingdomManager;
	private final PlayerManager playerManager;
	
	public InventoryListener(KonquestPlugin plugin) {
		this.konquest = plugin.getKonquestInstance();
		this.kingdomManager = konquest.getKingdomManager();
		this.playerManager = konquest.getPlayerManager();
	}
	
	@EventHandler()
    public void onInventoryOpen(InventoryOpenEvent event) {
		if(konquest.isWorldIgnored(event.getInventory().getLocation())) return;
		// Monitor blocks in claimed territory
		// Check for merchant trades in claimed territory of guilds
		Location openLoc = event.getInventory().getLocation();
		
		if(openLoc != null && konquest.getTerritoryManager().isChunkClaimed(openLoc)) {
			
			if(!konquest.getPlayerManager().isOnlinePlayer((Player)event.getPlayer())) {
				ChatUtil.printDebug("Failed to handle onInventoryOpen for non-existent player");
				return;
			}
			KonPlayer player = playerManager.getPlayer((Player)event.getPlayer());
			// Bypass event restrictions for player in Admin Bypass Mode
			if(!player.isAdminBypassActive()) {
				// Determine type of inventory
				boolean isContainer = event.getInventory().getHolder() instanceof BlockInventoryHolder ||
						event.getInventory().getHolder() instanceof DoubleChest ||
						event.getInventory().getHolder() instanceof Vehicle;
				boolean isMerchant = event.getInventory().getType().equals(InventoryType.MERCHANT) &&
						event.getInventory() instanceof MerchantInventory;
				// Get territory
				KonTerritory territory = konquest.getTerritoryManager().getChunkTerritory(openLoc);
				// Property Flag Holders
				if(isContainer && territory instanceof KonPropertyFlagHolder) {
					KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
					if(flagHolder.hasPropertyValue(KonPropertyFlag.CHEST)) {
						if(!flagHolder.getPropertyValue(KonPropertyFlag.CHEST)) {
							ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedFlagColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
							event.setCancelled(true);
							return;
						}
					}
				}
				KonquestRelationshipType territoryRole = kingdomManager.getRelationRole(territory.getKingdom(), player.getKingdom());
				// Town restrictions for inventories
				if(territory instanceof KonTown) {
					KonTown town = (KonTown) territory;
					// Protection checks
					if(isMerchant) {
						// Inventory is a Villager merchant
						// Only friendly, trade and ally players can use merchants
						boolean isMerchantAllowed = territoryRole.equals(KonquestRelationshipType.FRIENDLY) || territoryRole.equals(KonquestRelationshipType.TRADE) || territoryRole.equals(KonquestRelationshipType.ALLY);
						// Prevent opening by enemy and sanction kingdom members
						if(!isMerchantAllowed) {
							ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_MERCHANT.getMessage(town.getKingdom().getName()));
							event.setCancelled(true);
							return;
						}
						// Attempt to modify merchant trades based on town specialization and relationships with player
						town.applyTradeDiscounts(player, event.getInventory());
					} else if(isContainer) {
						// Inventory can hold items, or is a vehicle with storage

						// Prevent inventory openings by non-friendlies
						boolean isEnemyInventoryOpenDenied = konquest.getCore().getBoolean(CorePath.KINGDOMS_PROTECT_CONTAINERS_USE.getPath());
						if(isEnemyInventoryOpenDenied && !territoryRole.equals(KonquestRelationshipType.FRIENDLY)) {
							ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedProtectionColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
							event.setCancelled(true);
							return;
						}
						// Notify player when there is no lord
						if(town.canClaimLordship(player)) {
							ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_TOWN_NOTICE_NO_LORD.getMessage(town.getName(),town.getTravelName()));
						}
						// Prevent non-residents in closed towns from opening inventories that can hold items
						// However this still allows them to open inventories like enchantment tables, crafting bench, etc.
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
					if(town.getMonument().isLocInside(openLoc) && event.getInventory().getType().equals(InventoryType.CHEST)) {
						if(town.isPlayerLord(player.getOfflineBukkitPlayer()) || town.isPlayerKnight(player.getOfflineBukkitPlayer())) {
							// Update loot with default count as defined in core YML
							boolean result = konquest.getLootManager().updateMonumentLoot(event.getInventory(), town);
							ChatUtil.printDebug("Attempted to update loot in town "+territory.getName()+", got "+result);
							if(result) {
								event.getInventory().getLocation().getWorld().playSound(event.getInventory().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, (float)1.0, (float)1.0);
							} else {
								ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_LOOT_LATER.getMessage());
							}
						} else {
							ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
							event.setCancelled(true);
							return;
						}
					}
				}
				
				// Prevent all inventory openings except for camp owner and clan members when allowed
				boolean isCampContainersProtected = konquest.getCore().getBoolean(CorePath.CAMPS_PROTECT_CONTAINERS.getPath(), true);
				if(territory instanceof KonCamp && isCampContainersProtected) {
					KonCamp camp = (KonCamp) territory;
					boolean isMemberAllowed = konquest.getCore().getBoolean(CorePath.CAMPS_CLAN_ALLOW_CONTAINERS.getPath(), false);
					boolean isMember = false;
					if(konquest.getCampManager().isCampGrouped(camp)) {
						isMember = konquest.getCampManager().getCampGroup(camp).isPlayerMember(player.getBukkitPlayer());
					}
					if(!(isMember && isMemberAllowed) && !player.getBukkitPlayer().getUniqueId().equals(camp.getOwner().getUniqueId())) {
						ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedProtectionColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
						event.setCancelled(true);
					}
				}
			}
		}
	}
	
	@EventHandler()
    public void onInventoryClose(InventoryCloseEvent event) {
		// When a player closes a display menu inventory
		if(konquest.getDisplayManager().isNotDisplayMenu(event.getInventory())) return;
		konquest.getDisplayManager().onDisplayMenuClose(event.getInventory(), event.getPlayer());
	}
	
	@EventHandler(priority = EventPriority.LOW)
    public void onDisplayMenuClick(InventoryClickEvent event) {
		// When a player clicks inside a display menu inventory
		if(konquest.getDisplayManager().isNotDisplayMenu(event.getClickedInventory())) return;
		int slot = event.getRawSlot();
		Player bukkitPlayer = (Player) event.getWhoClicked();
		KonPlayer player = konquest.getPlayerManager().getPlayer(bukkitPlayer);
		if(player == null || slot >= event.getView().getTopInventory().getSize()) return;
		event.setResult(Event.Result.DENY);
		event.setCancelled(true);
		if(event.getClick().equals(ClickType.LEFT) || event.getClick().equals(ClickType.RIGHT)) {
			boolean clickType = !event.getClick().equals(ClickType.RIGHT);
			Bukkit.getScheduler().runTask(konquest.getPlugin(),
					() -> konquest.getDisplayManager().onDisplayMenuClick(player, event.getClickedInventory(), slot, clickType));
		}

	}
	
	@EventHandler()
    public void onNetheriteCraft(InventoryClickEvent event) {
		if(!event.isCancelled()) {
			// Check for picking up netherite items for stats
			if(event.getAction().equals(InventoryAction.PICKUP_ALL) && event.getClickedInventory() instanceof SmithingInventory &&
					!konquest.isWorldIgnored(event.getInventory().getLocation())) {
				Material itemType = event.getCurrentItem().getType();
				int slot = event.getRawSlot();
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
		}
	}
	
	@EventHandler()
    public void onCraftItem(CraftItemEvent event) {
		if(konquest.isWorldIgnored(event.getInventory().getLocation())) {
			return;
		}
		HumanEntity human = event.getWhoClicked();
		if(event.isCancelled() || !(human instanceof Player)) return;
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
	
	@EventHandler()
    public void onFurnaceExtract(FurnaceExtractEvent event) {
		if(konquest.isWorldIgnored(event.getBlock().getWorld())) return;
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
	
	@EventHandler()
    public void onEnchantItem(EnchantItemEvent event) {
		if(event.isCancelled())return;
		if(konquest.isWorldIgnored(event.getInventory().getLocation())) return;
		if(!konquest.getPlayerManager().isOnlinePlayer(event.getEnchanter())) {
			ChatUtil.printDebug("Failed to handle onEnchantItem for non-existent player");
			return;
		}
		KonPlayer player = playerManager.getPlayer(event.getEnchanter());
		konquest.getDirectiveManager().updateDirectiveProgress(player, KonDirective.ENCHANT_ITEM);
		konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.ENCHANTMENTS,1);
		Location enchantLoc = event.getEnchantBlock().getLocation();
		if(konquest.getTerritoryManager().isChunkClaimed(enchantLoc)) {
			KonTerritory territory = konquest.getTerritoryManager().getChunkTerritory(enchantLoc);
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
	
	@EventHandler()
    public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
		if(event.isCancelled())return;
		if(konquest.isWorldIgnored(event.getInventory().getLocation())) return;

		Location enchantLoc = event.getEnchantBlock().getLocation();
		if(!konquest.getTerritoryManager().isChunkClaimed(enchantLoc)) return;
		KonTerritory territory = konquest.getTerritoryManager().getChunkTerritory(enchantLoc);
		if(!(territory instanceof KonTown)) return;
		KonTown town = (KonTown)territory;
		int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.ENCHANT);
		if(upgradeLevel < 1)return;

		// Add 1 (if possible) to all enchantment offers
		EnchantmentOffer[] offers = event.getOffers();
		for (EnchantmentOffer offer : offers) {
			if (offer != null) {
				int enchantLevel = offer.getEnchantmentLevel();
				int enchantMaxLevel = offer.getEnchantment().getMaxLevel();
				if (enchantLevel < enchantMaxLevel) {
					offer.setEnchantmentLevel(enchantLevel + 1);
				}
			}
		}

	}

}
