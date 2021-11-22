package konquest.listener;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.event.KonquestMonumentDamageEvent;
import konquest.manager.CampManager;
import konquest.manager.KingdomManager;
import konquest.manager.PlayerManager;
import konquest.model.KonCamp;
import konquest.model.KonCapital;
import konquest.model.KonDirective;
import konquest.model.KonMonumentTemplate;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.model.KonRuin;
import konquest.model.KonStatsType;
import konquest.model.KonTerritory;
import konquest.model.KonTerritoryType;
import konquest.model.KonTown;
import konquest.model.KonUpgrade;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;
import konquest.utility.Timer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Bed;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.ItemStack;

public class BlockListener implements Listener {

	private KonquestPlugin konquestPlugin;
	private Konquest konquest;
	private KingdomManager kingdomManager;
	private CampManager campManager;
	private PlayerManager playerManager;
	
	public BlockListener(KonquestPlugin plugin) {
		this.konquestPlugin = plugin;
		this.konquest = konquestPlugin.getKonquestInstance();
		this.playerManager = konquest.getPlayerManager();
		this.kingdomManager = konquest.getKingdomManager();
		this.campManager = konquest.getCampManager();
	}
	
	/**
	 * Fires when a block breaks.
	 * Check for breaks inside Monuments by enemies. Peaceful members cannot break other territories.
	 * @param event
	 */
	@EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
		//ChatUtil.printDebug("EVENT: blockBreak");
		if(event.isCancelled()) {
			return;
		}
		
		if(!konquest.getPlayerManager().isPlayer(event.getPlayer())) {
			ChatUtil.printDebug("Failed to handle onBlockBreak for non-existent player");
			return;
		}
		KonPlayer player = konquest.getPlayerManager().getPlayer(event.getPlayer());
		
		// Monitor blocks in claimed territory
		if(kingdomManager.isChunkClaimed(event.getBlock().getLocation())) {
			// Bypass event restrictions for player in Admin Bypass Mode
			if(!player.isAdminBypassActive()) {
				//ChatUtil.printDebug("Evaluating blockBreak in claimed territory...");
				KonTerritory territory = konquest.getKingdomManager().getChunkTerritory(event.getBlock().getLocation());
				Location breakLoc = event.getBlock().getLocation();
				// Capital considerations...
				if(territory instanceof KonCapital) {
					boolean isCapitalBuild = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.capital_build",false);
					if(isCapitalBuild) {
						// Players can build in capital
						// Always prevent monument template edits
						KonMonumentTemplate template = territory.getKingdom().getMonumentTemplate();
						if(template != null && template.isLocInside(event.getBlock().getLocation())) {
							ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
							if(event.getPlayer().hasPermission("konquest.command.admin")) {
		    					ChatUtil.sendNotice(event.getPlayer(),MessagePath.PROTECTION_NOTICE_IGNORE.getMessage());
		    				}
							event.setCancelled(true);
							return;
						}
						// Restrict enemies
						if(!player.getKingdom().equals(territory.getKingdom())) {
							// Enemies cannot build in capital
							ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
							if(event.getPlayer().hasPermission("konquest.command.admin")) {
		    					ChatUtil.sendNotice(event.getPlayer(),MessagePath.PROTECTION_NOTICE_IGNORE.getMessage());
		    				}
							event.setCancelled(true);
							return;
						}
					} else {
						// No building is allowed in capital
						ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
						if(event.getPlayer().hasPermission("konquest.command.admin")) {
	    					ChatUtil.sendNotice(event.getPlayer(),MessagePath.PROTECTION_NOTICE_IGNORE.getMessage());
	    				}
						event.setCancelled(true);
						return;
					}
				}
				// Town considerations...
				if(territory instanceof KonTown) {
					KonTown town = (KonTown) territory;
					// Prevent all block breaks by this town's Kingdom members
					if(player.getKingdom().equals(town.getKingdom())) {
						// If player is allied...
						// Stop all block breaks in center chunk
						if(town.isLocInsideCenterChunk(breakLoc)) {
							ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
							event.setCancelled(true);
							return;
						}
						/*
						Open:
							all players can edit blocks outside of plots
							only resident plot users can edit blocks in respective plots
							any player can claim lordship
						else:
							Only residents can edit blocks outside of plots and within their own plot
							No Residents:
								Anyone can claim lordship
							else:
								Only residents can claim lordship
						*/
						// Notify player when there is no lord
						if(town.canClaimLordship(player)) {
							//ChatUtil.sendNotice(player.getBukkitPlayer(), town.getName()+" has no leader! Use \"/k town <name> lord <your name>\" to claim Lordship.");
							ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_TOWN_NOTICE_NO_LORD.getMessage(town.getName(),town.getName(),player.getBukkitPlayer().getName()));
						}
						// Protect land and plots
						if(!town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer())) {
							// Stop all edits by non-resident in closed towns
							ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_NOT_RESIDENT.getMessage(territory.getName()));
							event.setCancelled(true);
							return;
						}
						if(town.isOpen() || (!town.isOpen() && town.isPlayerResident(player.getOfflineBukkitPlayer()))) {
							// Check for protections in open towns and closed town residents
							if(konquest.getPlotManager().isPlayerPlotProtectBuild(town, breakLoc, player.getBukkitPlayer())) {
								// Stop when player edits plot that isn't theirs
								ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_NOT_PLOT.getMessage());
								event.setCancelled(true);
								return;
							}
							if(town.isPlotOnly() && !town.isPlayerElite(player.getOfflineBukkitPlayer()) && !town.hasPlot(breakLoc)) {
								// Stop when non-elite player edits non-plot land
								ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_ONLY_PLOT.getMessage());
								event.setCancelled(true);
								return;
							}
						}
					} else {
						// If player is enemy, protect containers and allow block breaks inside monument region
						//ChatUtil.printDebug("blockBreak occured in an enemy Town");
						
						// If territory is peaceful, prevent all block damage by enemies
						if(territory.getKingdom().isPeaceful()) {
							//ChatUtil.sendNotice(player.getBukkitPlayer(), "This Kingdom of "+town.getKingdom().getName()+" is peaceful! You cannot attack the Town "+town.getName(), ChatColor.DARK_RED);
							ChatUtil.sendNotice(event.getPlayer(), MessagePath.PROTECTION_NOTICE_PEACEFUL_TOWN.getMessage(town.getName()));
							event.setCancelled(true);
							return;
						}
						
						// If enemy player is peaceful, prevent all block damage
						if(player.getKingdom().isPeaceful()) {
							//ChatUtil.sendNotice(player.getBukkitPlayer(), "Your Kingdom of "+town.getKingdom().getName()+" is peaceful! You cannot attack the Town "+town.getName(), ChatColor.DARK_RED);
							ChatUtil.sendNotice(event.getPlayer(), MessagePath.PROTECTION_NOTICE_PEACEFUL_PLAYER.getMessage());
							event.setCancelled(true);
							return;
						}
						
						// If no players are online from this Kingdom, prevent block damage
						//boolean isBreakDisabledOffline = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.no_enemy_edit_offline");
						if(town.getKingdom().isOfflineProtected()) {
							//ChatUtil.sendNotice(player.getBukkitPlayer(), town.getKingdom().getName()+" doesn't have enough online players, cannot attack the Town "+town.getName(), ChatColor.DARK_RED);
							ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_ONLINE.getMessage(town.getKingdom().getName(),town.getName()));
							event.setCancelled(true);
							return;
						}
						
						// If town is upgraded to require a minimum online resident amount, prevent block damage
						int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.WATCH);
						if(upgradeLevel > 0) {
							int minimumOnlineResidents = upgradeLevel; // 1, 2, 3
							if(town.getNumResidentsOnline() < minimumOnlineResidents) {
								//ChatUtil.sendNotice(player.getBukkitPlayer(), town.getName()+" is upgraded with "+KonUpgrade.WATCH.getDescription()+" and cannot be attacked without "+minimumOnlineResidents+" residents online", ChatColor.DARK_RED);
								ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_UPGRADE.getMessage(town.getName(),KonUpgrade.WATCH.getDescription(),minimumOnlineResidents));
								event.setCancelled(true);
								return;
							}
						}
						
						// If block is a container, protect (optionally)
						boolean isProtectChest = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.protect_containers_break");
						if(isProtectChest && event.getBlock().getState() instanceof BlockInventoryHolder) {
							ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
							event.setCancelled(true);
							return;
						}
						
						// Verify town can be captured
						if(town.isCaptureDisabled()) {
							//ChatUtil.sendNotice(event.getPlayer().getBukkitPlayer(), "This Town cannot be conquered again so soon!");
							ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_CAPTURE.getMessage(town.getCaptureCooldownString()));
							event.setCancelled(true);
							return;
						}
						// Update MonumentBar state
						town.setAttacked(true);
						town.updateBar();
						town.applyGlow(event.getPlayer());
						// Attempt to start a raid alert
						town.sendRaidAlert();
						
						// If town is shielded, prevent all enemy block edits
						if(town.isShielded()) {
							ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_AQUA+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
							event.setCancelled(true);
							return;
						}
						
						// If town is armored, damage the armor while preventing block breaks
						if(town.isArmored()) {
							// Ignore instant-break blocks
							Material blockMat = event.getBlock().getState().getType();
							int hardness = (int)blockMat.getHardness();
							ChatUtil.printDebug("Armor block broke of hardness "+blockMat.getHardness());
							if(hardness > 0) {
								town.damageArmor(1);
								Konquest.playTownArmorSound(event.getPlayer());
							}
							event.setCancelled(true);
							return;
						}
						
						// If block is inside a monument, throw KonquestMonumentDamageEvent
						if(town.isLocInsideCenterChunk(breakLoc)) {
							if(town.getMonument().isLocInside(breakLoc)) {
								// Prevent Barbarians from damaging monuments optionally
								boolean isBarbAttackAllowed = konquest.getConfigManager().getConfig("core").getBoolean("core.towns.barbarians_destroy",true);
								if(player.isBarbarian() && !isBarbAttackAllowed) {
									//ChatUtil.sendNotice(player.getBukkitPlayer(), "Barbarians cannot damage Town Monuments.", ChatColor.DARK_RED);
									ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
									event.setCancelled(true);
									return;
								}
								// Cancel item drops on the broken blocks
								event.setDropItems(false);
								// Throw Konquest event
								KonquestMonumentDamageEvent invokeEvent = new KonquestMonumentDamageEvent(konquest, player, kingdomManager.getChunkTerritory(breakLoc), event);
			    	            Bukkit.getServer().getPluginManager().callEvent(invokeEvent);
							} else {
								// Prevent block breaks in the rest of the chunk
								//ChatUtil.sendNotice(player.getBukkitPlayer(), "Cannot break blocks outside the Monument structure", ChatColor.DARK_RED);
								ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
								event.setCancelled(true);
								return;
							}
						}
						
						konquest.getDirectiveManager().updateDirectiveProgress(player, KonDirective.ATTACK_TOWN);
					}
				}
				// Camp considerations...
				if(territory.getTerritoryType().equals(KonTerritoryType.CAMP)) {
					KonCamp camp = (KonCamp)territory;
					boolean isMemberAllowedContainers = konquest.getConfigManager().getConfig("core").getBoolean("core.camps.clan_allow_containers", false);
					boolean isMemberAllowedEdit = konquest.getConfigManager().getConfig("core").getBoolean("core.camps.clan_allow_edit_offline", false);
					boolean isMember = false;
					if(konquest.getCampManager().isCampGrouped(camp)) {
						isMember = konquest.getCampManager().getCampGroup(camp).isPlayerMember(player.getBukkitPlayer());
					}
					
					// If the camp owner is not online, prevent block breaking optionally
					if(camp.isProtected() && !(isMember && isMemberAllowedEdit)) {
						//ChatUtil.sendNotice(player.getBukkitPlayer(), "Cannot attack "+camp.getName()+", owner is offline", ChatColor.DARK_RED);
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_CAMP.getMessage(camp.getName()));
						event.setCancelled(true);
						return;
					}
					
					// If block is a container, protect (optionally) from players other than the owner
					boolean isProtectChest = konquest.getConfigManager().getConfig("core").getBoolean("core.camps.protect_containers");
					if(isProtectChest && !(isMember && isMemberAllowedContainers) && !camp.isPlayerOwner(event.getPlayer()) && event.getBlock().getState() instanceof BlockInventoryHolder) {
						ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
						event.setCancelled(true);
						return;
					}
					
					// Prevent group members from breaking beds they don't own
					if(isMember && !camp.isPlayerOwner(event.getPlayer()) && event.getBlock().getBlockData() instanceof Bed) {
						ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
						event.setCancelled(true);
						return;
					}
					
					// Remove the camp if a bed is broken within it
					if(event.getBlock().getBlockData() instanceof Bed) {
						campManager.removeCamp(camp.getOwner().getUniqueId().toString());
						//ChatUtil.sendNotice(player.getBukkitPlayer(), "Destroyed "+camp.getName()+"!");
						ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_CAMP_DESTROY.getMessage(camp.getName()));
						KonPlayer onlineOwner = playerManager.getPlayerFromName(camp.getOwner().getName());
						if(onlineOwner != null) {
							//ChatUtil.sendNotice(onlineOwner.getBukkitPlayer(), "Your Camp has been destroyed!");
							ChatUtil.sendError(onlineOwner.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_CAMP_DESTROY_OWNER.getMessage());
						} else {
							ChatUtil.printDebug("Failed attempt to send camp destruction message to offline owner "+camp.getOwner().getName());
						}
					}
				}
				// Ruin considerations...
				if(territory.getTerritoryType().equals(KonTerritoryType.RUIN)) {
					KonRuin ruin = (KonRuin)territory;
					// Check for expired cooldown
					if(ruin.isCaptureDisabled()) {
						ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
						//ChatUtil.sendError(player.getBukkitPlayer(), "This Ruin cannot be captured for another "+ruin.getCaptureCooldownString());
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_CAPTURE.getMessage(ruin.getCaptureCooldownString()));
						event.setCancelled(true);
						return;
					}
					// Check for broken critical block within this Ruin
					if(ruin.isCriticalLocation(breakLoc)) {
						// Prevent critical block drop
						event.setDropItems(false);
						// Restart capture cooldown timer
						int ruinCaptureTimeSeconds = konquest.getConfigManager().getConfig("core").getInt("core.ruins.capture_cooldown");
						Timer captureTimer = ruin.getCaptureTimer();
						captureTimer.stopTimer();
						captureTimer.setTime(ruinCaptureTimeSeconds);
						captureTimer.startTimer();
						ChatUtil.printDebug("Starting ruin capture timer for "+ruinCaptureTimeSeconds+" seconds with taskID "+captureTimer.getTaskID()+" for Ruin "+ruin.getName());
						// Disable broken critical block
						ruin.setCriticalLocationEnabled(breakLoc, false);
						// Update bar progress
						double progress = (double)(ruin.getRemainingCriticalHits()) / (double)ruin.getMaxCriticalHits();
						ruin.setBarProgress(progress);
						//ChatUtil.sendNotice(player.getBukkitPlayer(), "Critical hit to Ruin "+ruin.getName());
						ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_CRITICAL.getMessage(ruin.getRemainingCriticalHits()));
						event.getBlock().getWorld().playSound(breakLoc, Sound.BLOCK_FIRE_EXTINGUISH, 1.0F, 0.6F);
						// Evaluate critical blocks
						if(ruin.getRemainingCriticalHits() == 0) {
							ruin.setIsCaptureDisabled(true);
							konquest.getRuinManager().rewardPlayers(ruin,player);
							//ChatUtil.sendNotice(player.getBukkitPlayer(), "Captured Ruin "+ruin.getName());
						} else {
							// Force all alive golems to respawn
							ruin.respawnAllDistantGolems();
							// All golems target player
							ruin.targetAllGolemsToPlayer(event.getPlayer());
						}
					} else {
						// Prevent all other block breaks
						ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
						event.setCancelled(true);
						return;
					}
				}
			} else {
				// When player is in admin bypass and breaks a block
				checkMonumentTemplateBlanking(event);
			}
		} else {
			// Break occurred in the wild
			boolean isWildBuild = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.wild_build", true);
			if(!player.isAdminBypassActive() && !isWildBuild) {
				// No building is allowed in the wild
				ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
				if(event.getPlayer().hasPermission("konquest.command.admin")) {
					ChatUtil.sendNotice(event.getPlayer(),MessagePath.PROTECTION_NOTICE_IGNORE.getMessage());
				}
				event.setCancelled(true);
				return;
			}
		}
    }
	@EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreakLow(BlockBreakEvent event) {
		if(!event.isCancelled()) {
			if(event.getBlock().getType().equals(Material.DIAMOND_ORE)) {
				boolean isDrop = event.isDropItems();
				ChatUtil.printDebug("Diamond ore block break dropping items: "+isDrop);
				ItemStack handItem = event.getPlayer().getInventory().getItemInMainHand();
				if(isDrop && handItem != null && !handItem.containsEnchantment(Enchantment.SILK_TOUCH)) {
					if(!konquest.getPlayerManager().isPlayer(event.getPlayer())) {
						ChatUtil.printDebug("Failed to handle onBlockBreakLow for non-existent player");
						return;
					}
					KonPlayer player = playerManager.getPlayer(event.getPlayer());
					konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.DIAMONDS,1);
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onCropHarvest(BlockBreakEvent event) {
		if(!event.isCancelled()) {
			Material brokenMat = event.getBlock().getType();
			//ChatUtil.printDebug("EVENT: Broke material "+brokenMat.toString());
			if(brokenMat.equals(Material.WHEAT) ||
					brokenMat.equals(Material.POTATOES) ||
					brokenMat.equals(Material.CARROTS) ||
					brokenMat.equals(Material.BEETROOTS)) {
				if(event.getBlock().getBlockData() instanceof Ageable) {
					Ageable crop = (Ageable)event.getBlock().getBlockData();
					//ChatUtil.printDebug("Broke crop block with age: "+crop.getAge());
					if(crop.getAge() == crop.getMaximumAge()) {
						if(!konquest.getPlayerManager().isPlayer(event.getPlayer())) {
							ChatUtil.printDebug("Failed to handle onCropHarvest for non-existent player");
							return;
						}
						KonPlayer player = konquest.getPlayerManager().getPlayer(event.getPlayer());
						konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.HARVEST,1);
					}
				}
			}
		}
	}
	
	/**
	 * Fires when blocks are placed.
	 * Prevent placing blocks inside capitals and monuments
	 * Check for barbarian bed placement
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
		
		if(event.isCancelled()) {
			return;
		}
		
		// Track last block placed per player
		konquest.lastPlaced.put(event.getPlayer(),event.getBlock().getLocation());
		if(!konquest.getPlayerManager().isPlayer(event.getPlayer())) {
			ChatUtil.printDebug("Failed to handle onBlockPlace for non-existent player");
			return;
		}
		KonPlayer player = konquest.getPlayerManager().getPlayer(event.getPlayer());
		Location placeLoc = event.getBlock().getLocation();
		// Monitor blocks in claimed territory
		if(kingdomManager.isChunkClaimed(placeLoc)) {
			// Bypass event restrictions for player in Admin Bypass Mode
			if(!player.isAdminBypassActive()) {
				KonTerritory territory = kingdomManager.getChunkTerritory(placeLoc);
				// Capital considerations...
				if(territory instanceof KonCapital) {
					boolean isCapitalBuild = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.capital_build",false);
					if(isCapitalBuild) {
						// Players can build in capital
						// Always prevent monument template edits
						KonMonumentTemplate template = territory.getKingdom().getMonumentTemplate();
						if(template != null && template.isLocInside(placeLoc)) {
							ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
							if(event.getPlayer().hasPermission("konquest.command.admin")) {
		    					ChatUtil.sendNotice(event.getPlayer(),MessagePath.PROTECTION_NOTICE_IGNORE.getMessage());
		    				}
							event.setCancelled(true);
							return;
						}
						// Restrict enemies
						if(!player.getKingdom().equals(territory.getKingdom())) {
							// Enemies cannot place blocks in capital
							ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
							if(event.getPlayer().hasPermission("konquest.command.admin")) {
		    					ChatUtil.sendNotice(event.getPlayer(),MessagePath.PROTECTION_NOTICE_IGNORE.getMessage());
		    				}
							event.setCancelled(true);
							return;
						}
					} else {
						// No building is allowed in capital
						ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
						if(event.getPlayer().hasPermission("konquest.command.admin")) {
	    					ChatUtil.sendNotice(event.getPlayer(),MessagePath.PROTECTION_NOTICE_IGNORE.getMessage());
	    				}
						event.setCancelled(true);
						return;
					}
				}
				// Preventions for Towns
				if(territory.getTerritoryType().equals(KonTerritoryType.TOWN)) {
					KonTown town = (KonTown) territory;
					// Prevent all block placements in center chunk
					if(town.isLocInsideCenterChunk(event.getBlock().getLocation())) {
						ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
						event.setCancelled(true);
						return;
					}
					// For enemy players...
					if(!player.getKingdom().equals(town.getKingdom())) {
						// If territory is peaceful, prevent all block damage by enemies
						if(territory.getKingdom().isPeaceful()) {
							//ChatUtil.sendNotice(player.getBukkitPlayer(), "This Kingdom of "+town.getKingdom().getName()+" is peaceful! You cannot attack the Town "+town.getName(), ChatColor.DARK_RED);
							ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_PEACEFUL_TOWN.getMessage(town.getName()));
							event.setCancelled(true);
							return;
						}
						// If enemy player is peaceful, prevent all block placement
						if(player.getKingdom().isPeaceful()) {
							//ChatUtil.sendNotice(player.getBukkitPlayer(), "Your Kingdom of "+town.getKingdom().getName()+" is peaceful! You cannot attack the Town "+town.getName(), ChatColor.DARK_RED);
							ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_PEACEFUL_PLAYER.getMessage());
							event.setCancelled(true);
							return;
						}
						// If no players are online from this Kingdom, prevent block damage
						//boolean isBreakDisabledOffline = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.no_enemy_edit_offline");
						if(town.getKingdom().isOfflineProtected()) {
							//ChatUtil.sendNotice(player.getBukkitPlayer(), town.getKingdom().getName()+" doesn't have enough online players, cannot attack the Town "+town.getName(), ChatColor.DARK_RED);
							ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_ONLINE.getMessage(town.getKingdom().getName(),town.getName()));
							event.setCancelled(true);
							return;
						}
						// If town is upgraded to require a minimum online resident amount, prevent block damage
						int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.WATCH);
						if(upgradeLevel > 0) {
							int minimumOnlineResidents = upgradeLevel; // 1, 2, 3
							if(town.getNumResidentsOnline() < minimumOnlineResidents) {
								//ChatUtil.sendNotice(player.getBukkitPlayer(), town.getName()+" is upgraded with "+KonUpgrade.WATCH.getDescription()+" and cannot be attacked without "+minimumOnlineResidents+" residents online", ChatColor.DARK_RED);
								ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_UPGRADE.getMessage(town.getName(),KonUpgrade.WATCH.getDescription(),minimumOnlineResidents));
								event.setCancelled(true);
								return;
							}
						}
						// If town is shielded, prevent all enemy block edits
						if(town.isShielded()) {
							ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_AQUA+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
							event.setCancelled(true);
							return;
						}
						// If town is armored, prevent block places
						if(town.isArmored()) {
							Konquest.playTownArmorSound(event.getPlayer());
							event.setCancelled(true);
							return;
						}
						// Prevent inventory blocks from being placed by enemies
						boolean isProtectChest = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.protect_containers_break");
						if(isProtectChest && event.getBlock().getState() instanceof BlockInventoryHolder) {
							ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
							event.setCancelled(true);
							return;
						}
						
					} else {
						// For friendly players...
						// Notify player when there is no lord
						if(town.canClaimLordship(player)) {
							//ChatUtil.sendNotice(player.getBukkitPlayer(), town.getName()+" has no leader! Use \"/k town <name> lord <your name>\" to claim Lordship.");
							ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_TOWN_NOTICE_NO_LORD.getMessage(town.getName(),town.getName(),player.getBukkitPlayer().getName()));
						}
						// Protect land and plots
						if(!town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer())) {
							// Stop all edits by non-resident in closed towns
							ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_NOT_RESIDENT.getMessage(territory.getName()));
							event.setCancelled(true);
							return;
						}
						if(town.isOpen() || (!town.isOpen() && town.isPlayerResident(player.getOfflineBukkitPlayer()))) {
							// Check for protections in open towns and closed town residents
							if(konquest.getPlotManager().isPlayerPlotProtectBuild(town, placeLoc, player.getBukkitPlayer())) {
								// Stop when player edits plot that isn't theirs
								ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_NOT_PLOT.getMessage());
								event.setCancelled(true);
								return;
							}
							if(town.isPlotOnly() && !town.isPlayerElite(player.getOfflineBukkitPlayer()) && !town.hasPlot(placeLoc)) {
								// Stop when non-elite player edits non-plot land
								ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_ONLY_PLOT.getMessage());
								event.setCancelled(true);
								return;
							}
						}
						konquest.getDirectiveManager().updateDirectiveProgress(player, KonDirective.BUILD_TOWN);
					}
				}
				// Preventions for Camps
				if(territory.getTerritoryType().equals(KonTerritoryType.CAMP)) {
					KonCamp camp = (KonCamp)territory;
					boolean isMemberAllowedEdit = konquest.getConfigManager().getConfig("core").getBoolean("core.camps.clan_allow_edit_offline", false);
					boolean isMember = false;
					if(konquest.getCampManager().isCampGrouped(camp)) {
						isMember = konquest.getCampManager().getCampGroup(camp).isPlayerMember(player.getBukkitPlayer());
					}
					// Prevent additional beds from being placed by anyone
					if(event.getBlock().getBlockData() instanceof Bed) {
						if(event.getBlock().getWorld().getBlockAt(camp.getBedLocation()).getBlockData() instanceof Bed) {
							// The camp has a bed block already
							//ChatUtil.sendNotice(player.getBukkitPlayer(), "Cannot place additional beds within this Camp", ChatColor.DARK_RED);
							ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_CAMP_BED.getMessage());
							event.setCancelled(true);
							return;
						} else if(camp.isPlayerOwner(player.getBukkitPlayer())){
							// The camp does not have a bed and the owner is placing a new one
							camp.setBedLocation(event.getBlock().getLocation());
							player.getBukkitPlayer().setBedSpawnLocation(event.getBlock().getLocation(), true);
							//ChatUtil.sendNotice(player.getBukkitPlayer(), "Updated your Camp's bed location");
							ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_CAMP_UPDATE.getMessage());
						} else {
							// The camp does not have a bed and this player is not the owner
							//ChatUtil.sendNotice(player.getBukkitPlayer(), "Cannot place beds in someone else's Camp", ChatColor.DARK_RED);
							ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_CAMP_BED.getMessage());
							event.setCancelled(true);
							return;
						}
					}
					// If the camp owner is not online, prevent block placement optionally for clan members
					if(camp.isProtected() && !(isMember && isMemberAllowedEdit)) {
						//ChatUtil.sendNotice(player.getBukkitPlayer(), "Cannot attack "+camp.getName()+", owner is offline", ChatColor.DARK_RED);
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_CAMP.getMessage(camp.getName()));
						event.setCancelled(true);
						return;
					}
				}
				// Ruin considerations...
				if(territory.getTerritoryType().equals(KonTerritoryType.RUIN)) {
					// Prevent all placement within ruins
					ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
					event.setCancelled(true);
					return;
				}
			} else {
				checkMonumentTemplateBlanking(event);
			}
		} else {
			// When placing blocks in the wilderness...

			// Prevent barbarians who already have a camp from placing a bed
			// Attempt to create a camp for barbarians who place a bed
			if(!player.isAdminBypassActive()) {
				// Check if the player is a barbarian placing a bed
				if(player.isBarbarian() && event.getBlock().getBlockData() instanceof Bed) {
					if(campManager.isCampSet(player)) {
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_CAMP_CREATE.getMessage());
					} else {
						int status = campManager.addCamp(event.getBlock().getLocation(), (KonOfflinePlayer)player);
						if(status == 0) { // on successful camp setup...
							player.getBukkitPlayer().setBedSpawnLocation(event.getBlock().getLocation(), true);
							//ChatUtil.sendNotice(event.getPlayer(), "Successfully set up camp");
							ChatUtil.sendNotice(event.getPlayer(), MessagePath.PROTECTION_NOTICE_CAMP_CREATE.getMessage());
							String territoryName = campManager.getCamp((KonOfflinePlayer)player).getName();
							ChatUtil.sendKonTitle(player, "", ChatColor.YELLOW+territoryName);
						} else {
							switch(status) {
					    	case 1:
					    		//ChatUtil.sendError(event.getPlayer(), "Camping failed: New claims overlap with existing territory.");
					    		ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_CAMP_FAIL_OVERLAP.getMessage());
					    		event.setCancelled(true);
					    		break;
					    	case 2:
					    		//ChatUtil.sendError(event.getPlayer(), "Camping failed: Your camp already exists.");
					    		ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_CAMP_FAIL_EXISTS.getMessage());
					    		event.setCancelled(true);
					    		break;
					    	case 3:
					    		//ChatUtil.sendError(event.getPlayer(), "Camping failed: You are not a Barbarian.");
					    		ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_CAMP_FAIL_BARBARIAN.getMessage());
					    		event.setCancelled(true);
					    		break;
					    	case 4:
					    		//ChatUtil.sendError(event.getPlayer(), MessagePath.GENERIC_ERROR_DISABLED.getMessage());
					    		break;
					    	default:
					    		//ChatUtil.sendError(event.getPlayer(), "Camping failed: Unknown cause. Contact an Admin!");
					    		ChatUtil.sendError(event.getPlayer(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
					    		break;
							}
							return;
						}
					}
				} else {
					// Place occurred in the wild
					boolean isWildBuild = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.wild_build", true);
					if(!isWildBuild) {
						// No building is allowed in the wild
						ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
						if(event.getPlayer().hasPermission("konquest.command.admin")) {
							ChatUtil.sendNotice(event.getPlayer(),MessagePath.PROTECTION_NOTICE_IGNORE.getMessage());
						}
						event.setCancelled(true);
						return;
					}
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
    public void onSeedPlant(BlockPlaceEvent event) {
		if(!event.isCancelled()) {
			if(!konquest.getPlayerManager().isPlayer(event.getPlayer())) {
				ChatUtil.printDebug("Failed to handle onSeedPlant for non-existent player");
				return;
			}
			Material placedMat = event.getBlockPlaced().getType();
			//ChatUtil.printDebug("EVENT: Placed material "+placedMat.toString());
			if(placedMat.equals(Material.WHEAT) ||
					placedMat.equals(Material.POTATOES) ||
					placedMat.equals(Material.CARROTS) ||
					placedMat.equals(Material.PUMPKIN_STEM) ||
					placedMat.equals(Material.MELON_STEM) ||
					placedMat.equals(Material.COCOA_BEANS) ||
					placedMat.equals(Material.BEETROOTS) ||
					placedMat.equals(Material.SUGAR_CANE)) {
				KonPlayer player = konquest.getPlayerManager().getPlayer(event.getPlayer());
				konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.SEEDS,1);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
    public void onFarmTill(BlockPlaceEvent event) {
		if(!event.isCancelled()) {
			Material placedMat = event.getBlockPlaced().getType();
			if(placedMat.equals(Material.FARMLAND)) {
				if(!konquest.getPlayerManager().isPlayer(event.getPlayer())) {
					ChatUtil.printDebug("Failed to handle onFarmTill for non-existent player");
					return;
				}
				KonPlayer player = konquest.getPlayerManager().getPlayer(event.getPlayer());
				konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.TILL,1);
			}
		}
	}
	
	/**
	 * Fires when blocks explode.
	 * Protect capitals from explosions, and optionally protect chests inside claimed territory.
	 * @param event
	 */
	@EventHandler(priority = EventPriority.NORMAL)
    public void onBlockExplode(BlockExplodeEvent event) {
		// Protect blocks inside of territory
		//ChatUtil.printDebug("EVENT: blockExplode");
		for(Block block : event.blockList()) {
			if(kingdomManager.isChunkClaimed(block.getLocation())) {
				ChatUtil.printDebug("effected block is inside claimed territory");
				KonTerritory territory = kingdomManager.getChunkTerritory(block.getLocation());
				// Protect Capitals
				if(territory.getTerritoryType().equals(KonTerritoryType.CAPITAL)) {
					ChatUtil.printDebug("protecting Capital");
					event.setCancelled(true);
					return;
				}
				// Protect Peaceful Territory
				if(territory.getKingdom().isPeaceful()) {
					ChatUtil.printDebug("protecting peaceful kingdom");
					event.setCancelled(true);
					return;
				}
				// Town protections
				if(territory.getTerritoryType().equals(KonTerritoryType.TOWN)) {
					KonTown town = (KonTown)territory;
					// Protect Town Monuments
					if(town.isLocInsideCenterChunk(block.getLocation())) {
						ChatUtil.printDebug("protecting Town Monument");
						event.setCancelled(true);
						return;
					}
					// Protect towns when all kingdom members are offline
					if(playerManager.getPlayersInKingdom(town.getKingdom()).isEmpty()) {
						ChatUtil.printDebug("protecting offline Town");
						event.setCancelled(true);
						return;
					}
					// If town is upgraded to require a minimum online resident amount, prevent block damage
					int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.WATCH);
					if(upgradeLevel > 0) {
						int minimumOnlineResidents = upgradeLevel; // 1, 2, 3
						if(town.getNumResidentsOnline() < minimumOnlineResidents) {
							ChatUtil.printDebug("protecting upgraded Town");
							event.setCancelled(true);
							return;
						}
					}
					// If town is shielded, prevent all enemy block edits
					if(town.isShielded()) {
						event.setCancelled(true);
						return;
					}
					// If town is armored, damage the armor while preventing block breaks
					if(town.isArmored()) {
						int damage = konquest.getConfigManager().getConfig("core").getInt("core.towns.armor_tnt_damage",1);
						town.damageArmor(damage);
						Konquest.playTownArmorSound(event.getBlock().getLocation());
						event.setCancelled(true);
						return;
					}
				}
				// Camp protections
				if(territory.getTerritoryType().equals(KonTerritoryType.CAMP)) {
					KonCamp camp = (KonCamp)territory;
					// Protect offline owner camps
					if(camp.isProtected()) {
						ChatUtil.printDebug("EVENT: protecting offline camp from explosion");
						event.setCancelled(true);
						return;
					}
				}
				
				// Protect chests
				boolean isProtectChest = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.protect_containers_explode");
				if(isProtectChest && event.getBlock().getState() instanceof BlockInventoryHolder) {
					ChatUtil.printDebug("EVENT: protecting chest inside territory from explosion");
					event.setCancelled(true);
					return;
				}
				// Protect beds
				if(event.getBlock().getBlockData() instanceof Bed) {
					ChatUtil.printDebug("EVENT: protecting bed inside territory from explosion");
					event.setCancelled(true);
					return;
				}
				// Protect Ruins
				if(territory.getTerritoryType().equals(KonTerritoryType.RUIN)) {
					ChatUtil.printDebug("protecting Ruin");
					event.setCancelled(true);
					return;
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onPistonExtend(BlockPistonExtendEvent event) {
		//boolean isBreakDisabledOffline = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.no_enemy_edit_offline");
		// review the list of affected blocks, prevent any movement of monument blocks
		for(Block pushBlock : event.getBlocks()) {
			if(kingdomManager.isChunkClaimed(pushBlock.getLocation())) {
				KonTerritory territory = kingdomManager.getChunkTerritory(pushBlock.getLocation());
				if(territory instanceof KonCapital) {
					KonCapital capital = (KonCapital) territory;
					// Check if this block is within a monument template
					KonMonumentTemplate template = capital.getKingdom().getMonumentTemplate();
					if(template != null && template.isLocInside(pushBlock.getLocation())) {
						event.setCancelled(true);
						return;
					}
					// Check if block is inside of an offline kingdom's capital
					if(playerManager.getPlayersInKingdom(capital.getKingdom()).isEmpty()) {
						event.setCancelled(true);
						return;
					}
					// If piston is outside of territory
					if(!capital.isLocInside(event.getBlock().getLocation())) {
						event.setCancelled(true);
						return;
					}
				}
				if(territory instanceof KonTown) {
					KonTown town = (KonTown) territory;
					// Check if this block is within a monument
					if(town.isLocInsideCenterChunk(pushBlock.getLocation())) {
						//ChatUtil.printDebug("EVENT: Monument block pushed by piston, cancelling");
						event.setCancelled(true);
						return;
					}
					// Check if block is inside of an offline kingdom's town
					if(playerManager.getPlayersInKingdom(territory.getKingdom()).isEmpty()) {
						event.setCancelled(true);
						return;
					}
					// If town is shielded or armored, and piston is outside of territory
					if((town.isShielded() || town.isArmored()) && !town.isLocInside(event.getBlock().getLocation())) {
						event.setCancelled(true);
						return;
					}
				}
				if(territory instanceof KonRuin) {
					event.setCancelled(true);
					return;
				}
				if(territory instanceof KonCamp) {
					KonCamp camp = (KonCamp) territory;
					// Check if owner is offline
					if(camp.isProtected()) {
						event.setCancelled(true);
						return;
					}
				}
			}
			// Check if this block will move into a monument or monument template
			Location pushTo = new Location(pushBlock.getWorld(),pushBlock.getX()+event.getDirection().getModX(),pushBlock.getY()+event.getDirection().getModY(),pushBlock.getZ()+event.getDirection().getModZ());
			if(kingdomManager.isChunkClaimed(pushTo)) {
				KonTerritory territory = kingdomManager.getChunkTerritory(pushTo);
				if(territory instanceof KonCapital) {
					KonMonumentTemplate template = territory.getKingdom().getMonumentTemplate();
					if(template != null && template.isLocInside(pushBlock.getLocation())) {
						event.setCancelled(true);
						return;
					}
					// Check if block is inside of an offline kingdom's capital
					if(playerManager.getPlayersInKingdom(territory.getKingdom()).isEmpty()) {
						event.setCancelled(true);
						return;
					}
				}
				if(territory instanceof KonTown) {
					if(((KonTown) territory).isLocInsideCenterChunk(pushTo)) {
						//ChatUtil.printDebug("EVENT: block attempted to move into a monument by piston, cancelling");
						event.setCancelled(true);
						return;
					}
					// Check if block is inside of an offline kingdom's town
					if(playerManager.getPlayersInKingdom(territory.getKingdom()).isEmpty()) {
						event.setCancelled(true);
						return;
					}
				}
				if(territory instanceof KonRuin) {
					event.setCancelled(true);
					return;
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onPistonRetract(BlockPistonRetractEvent event) {
		//boolean isBreakDisabledOffline = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.no_enemy_edit_offline");
		// review the list of affected blocks, prevent any movement of monument blocks
		for(Block pullBlock : event.getBlocks()) {
			if(kingdomManager.isChunkClaimed(pullBlock.getLocation())) {
				KonTerritory territory = kingdomManager.getChunkTerritory(pullBlock.getLocation());
				if(territory instanceof KonCapital) {
					KonCapital capital = (KonCapital) territory;
					// Check if this block is within a monument template
					KonMonumentTemplate template = capital.getKingdom().getMonumentTemplate();
					if(template != null && template.isLocInside(pullBlock.getLocation())) {
						event.setCancelled(true);
						return;
					}
					// Check if block is inside of an offline kingdom's capital
					if(playerManager.getPlayersInKingdom(capital.getKingdom()).isEmpty()) {
						event.setCancelled(true);
						return;
					}
					// If piston is outside of territory
					if(!capital.isLocInside(event.getBlock().getLocation())) {
						event.setCancelled(true);
						return;
					}
				}
				if(territory instanceof KonTown) {
					KonTown town = (KonTown) territory;
					// Check if this block is within a monument
					if(town.isLocInsideCenterChunk(pullBlock.getLocation())) {
						//ChatUtil.printDebug("EVENT: Monument block pulled by piston, cancelling");
						event.setCancelled(true);
						return;
					}
					// Check if block is inside of an offline kingdom's town
					if(playerManager.getPlayersInKingdom(territory.getKingdom()).isEmpty()) {
						event.setCancelled(true);
						return;
					}
					// If town is shielded or armored, and piston is outside of territory
					if((town.isShielded() || town.isArmored()) && !town.isLocInside(event.getBlock().getLocation())) {
						event.setCancelled(true);
						return;
					}
				}
				if(territory instanceof KonRuin) {
					event.setCancelled(true);
					return;
				}
				if(territory instanceof KonCamp) {
					KonCamp camp = (KonCamp) territory;
					// Check if owner is offline
					if(camp.isProtected()) {
						event.setCancelled(true);
						return;
					}
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onFluidFlow(BlockFromToEvent event) {
		// Prevent all flow into monument
		if(isBlockInsideMonument(event.getToBlock())) {
			event.setCancelled(true);
			return;
		}
		// Prevent all flow from wild into territory
		Location locTo = event.getToBlock().getLocation();
		Location locFrom = event.getBlock().getLocation();
		if(!locTo.equals(locFrom)) {
    		boolean isTerritoryTo = kingdomManager.isChunkClaimed(locTo);
    		boolean isTerritoryFrom = kingdomManager.isChunkClaimed(locFrom);
    		
    		if(isTerritoryTo) {
    			if(isTerritoryFrom) {
    				// Between enemy territories
        			KonTerritory territoryTo = kingdomManager.getChunkTerritory(locTo);
        			KonTerritory territoryFrom = kingdomManager.getChunkTerritory(locFrom);
        			if(!territoryTo.getKingdom().equals(territoryFrom.getKingdom())) {
        				event.setCancelled(true);
            			return;
        			}
    			} else {
    				// Wild to any territory
        			event.setCancelled(true);
        			return;
    			}
    		}
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onBlockForm(BlockFormEvent event) {
		if(isBlockInsideMonument(event.getBlock())) {
			event.setCancelled(true);
			return;
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onBlockGrow(BlockGrowEvent event) {
		if(isBlockInsideMonument(event.getBlock())) {
			event.setCancelled(true);
			return;
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onBlockSpread(BlockSpreadEvent event) {
		if(kingdomManager.isChunkClaimed(event.getBlock().getLocation())) {
			KonTerritory territory = kingdomManager.getChunkTerritory(event.getBlock().getLocation());
			if(territory instanceof KonTown) {
				KonTown town = (KonTown) territory;
				// Prevent all spread inside Monument
				if(town.isLocInsideCenterChunk(event.getBlock().getLocation())) {
					event.setCancelled(true);
					return;
				}
				// Prevent fire spread inside upgraded Towns
				int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.DAMAGE);
				if(event.getSource().getType().equals(Material.FIRE) && upgradeLevel >= 1) {
					ChatUtil.printDebug("EVENT: Stopped fire spread in upgraded town, DAMAGE");
					event.getSource().setType(Material.AIR);
					event.setCancelled(true);
					return;
				}
				// If town is upgraded to require a minimum online resident amount, prevent block damage
				int upgradeLevelWatch = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.WATCH);
				if(event.getSource().getType().equals(Material.FIRE) && upgradeLevelWatch > 0) {
					int minimumOnlineResidents = upgradeLevelWatch; // 1, 2, 3
					if(town.getNumResidentsOnline() < minimumOnlineResidents) {
						ChatUtil.printDebug("EVENT: Stopped fire spread in upgraded town, WATCH");
						event.getSource().setType(Material.AIR);
						event.setCancelled(true);
						return;
					}
				}
				// If town is shielded
				if(event.getSource().getType().equals(Material.FIRE) && town.isShielded()) {
					event.getSource().setType(Material.AIR);
					event.setCancelled(true);
					return;
				}
				// If town is armored
				if(event.getSource().getType().equals(Material.FIRE) && town.isArmored()) {
					event.getSource().setType(Material.AIR);
					event.setCancelled(true);
					return;
				}
			}
			if(territory instanceof KonRuin) {
				if(event.getSource().getType().equals(Material.FIRE)) {
					event.getSource().setType(Material.AIR);
				}
				event.setCancelled(true);
				return;
			}
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onBlockIgnite(BlockIgniteEvent event) {
		if(isBlockInsideMonument(event.getBlock())) {
			event.setCancelled(true);
			return;
		}
	}
	
	private boolean isBlockInsideMonument(Block block) {
		boolean result = false;
		if(kingdomManager.isChunkClaimed(block.getLocation())) {
			KonTerritory territory = kingdomManager.getChunkTerritory(block.getLocation());
			if(territory instanceof KonTown && ((KonTown) territory).isLocInsideCenterChunk(block.getLocation())) {
				result = true;
			}
		}
		return result;
	}
	
	private void checkMonumentTemplateBlanking(BlockEvent event) {
		// Monitor monument templates for blanking by edits from admin bypass players
		KonTerritory territory = konquest.getKingdomManager().getChunkTerritory(event.getBlock().getLocation());
		if(territory instanceof KonCapital) {
			KonMonumentTemplate template = territory.getKingdom().getMonumentTemplate();
			if(template != null && template.isLocInside(event.getBlock().getLocation())) {
				// Start monument blanking timer to prevent monument pastes
				territory.getKingdom().startMonumentBlanking();
			}
		}
	}
	
}
