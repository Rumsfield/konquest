package com.github.rumsfield.konquest.listener;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.api.event.camp.KonquestCampDestroyEvent;
import com.github.rumsfield.konquest.api.event.camp.KonquestCampDestroyPostEvent;
import com.github.rumsfield.konquest.api.event.player.KonquestPlayerCampEvent;
import com.github.rumsfield.konquest.api.event.player.KonquestPlayerConquerEvent;
import com.github.rumsfield.konquest.api.event.ruin.KonquestRuinAttackEvent;
import com.github.rumsfield.konquest.api.event.ruin.KonquestRuinCaptureEvent;
import com.github.rumsfield.konquest.api.event.ruin.KonquestRuinCapturePostEvent;
import com.github.rumsfield.konquest.api.event.town.*;
import com.github.rumsfield.konquest.api.model.KonquestRelationshipType;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.manager.CampManager;
import com.github.rumsfield.konquest.manager.KingdomManager;
import com.github.rumsfield.konquest.manager.PlayerManager;
import com.github.rumsfield.konquest.manager.TerritoryManager;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Bed;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BlockListener implements Listener {
	private final Konquest konquest;
	private final KingdomManager kingdomManager;
	private final TerritoryManager territoryManager;
	private final CampManager campManager;
	private final PlayerManager playerManager;
	
	public BlockListener(KonquestPlugin plugin) {
		this.konquest = plugin.getKonquestInstance();
		this.playerManager = konquest.getPlayerManager();
		this.kingdomManager = konquest.getKingdomManager();
		this.territoryManager = konquest.getTerritoryManager();
		this.campManager = konquest.getCampManager();
	}
	
	private void notifyAdminBypass(Player player) {
		// Notify admins about using bypass
		if(player.hasPermission("konquest.admin.bypass")) {
			ChatUtil.sendNotice(player,MessagePath.PROTECTION_NOTICE_IGNORE.getMessage());
		}
	}
	
	/**
	 * Fires when a block breaks.
	 * Check for breaks inside Monuments by enemies. Peaceful members cannot break other territories.
	 */
	@EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
		if(event.isCancelled()) return;
		
		if(konquest.isWorldIgnored(event.getBlock().getWorld())) return;
		
		if(!konquest.getPlayerManager().isOnlinePlayer(event.getPlayer())) {
			ChatUtil.printDebug("Failed to handle onBlockBreak for non-existent player");
			return;
		}
		KonPlayer player = konquest.getPlayerManager().getPlayer(event.getPlayer());
		if(player == null) {
			ChatUtil.printDebug("Failed to handle onBlockBreak for null player");
			return;
		}
		
		// Monitor blocks in claimed territory
		if(territoryManager.isChunkClaimed(event.getBlock().getLocation())) {
			// Bypass event restrictions for player in Admin Bypass Mode
			if(!player.isAdminBypassActive() && !player.getBukkitPlayer().getGameMode().equals(GameMode.SPECTATOR)) {
				Location breakLoc = event.getBlock().getLocation();
				KonTerritory territory = territoryManager.getChunkTerritory(breakLoc);

				// Pre-check
				// Always allow monument critical blocks to be broken
				boolean isAlwaysAllowed = false;
				if(territory instanceof KonTown) {
					KonTown town = (KonTown) territory;
					if(town.getMonument().isLocInside(breakLoc) && event.getBlock().getType().equals(konquest.getKingdomManager().getTownCriticalBlock())) {
						isAlwaysAllowed = true;
					}
				} else if(territory instanceof KonRuin) {
					KonRuin ruin = (KonRuin) territory;
					if(ruin.isCriticalLocation(breakLoc)) {
						isAlwaysAllowed = true;
					}
				}

				// Property Flag Holders
				if(territory instanceof KonPropertyFlagHolder) {
					KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
					if(flagHolder.hasPropertyValue(KonPropertyFlag.BUILD)) {
						if(!flagHolder.getPropertyValue(KonPropertyFlag.BUILD) && !isAlwaysAllowed) {
							notifyAdminBypass(event.getPlayer());
							// Block it
							ChatUtil.sendKonBlockedFlagTitle(player);
							event.setCancelled(true);
							return;
						}
					}
				}
				
				// Territory-specific handling
				if(territory instanceof KonTown) {
					// Checks for capital and town
					boolean isCapital = territory.getTerritoryType().equals(KonquestTerritoryType.CAPITAL);
					boolean isTown = territory.getTerritoryType().equals(KonquestTerritoryType.TOWN);

					KonTown town = (KonTown) territory;
					// Check player's relationship to this town
					KonquestRelationshipType playerRole = kingdomManager.getRelationRole(player.getKingdom(), town.getKingdom());
					
					// Stop all block edits in center chunk by non-enemies
					if(!playerRole.equals(KonquestRelationshipType.ENEMY) && town.isLocInsideMonumentProtectionArea(breakLoc)) {
						ChatUtil.sendKonBlockedProtectionTitle(player);
						event.setCancelled(true);
						return;
					}
					
					// Checks for friendly players
					if(playerRole.equals(KonquestRelationshipType.FRIENDLY)) {
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
							ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_TOWN_NOTICE_NO_LORD.getMessage(town.getName(),town.getTravelName()));
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
							if(town.isPlotOnly() && !town.isPlayerKnight(player.getOfflineBukkitPlayer()) && !town.hasPlot(breakLoc)) {
								// Stop when non-elite player edits non-plot land
								ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_ONLY_PLOT.getMessage());
								event.setCancelled(true);
								return;
							}
						}
					} else {
						// Checks for players in other kingdoms
						boolean isPlayerAlly = playerRole.equals(KonquestRelationshipType.ALLY);
						boolean isAlliedBuildingEnable = konquest.getCore().getBoolean(CorePath.KINGDOMS_ALLY_BUILD.getPath(),false);
						// Protections for when allied building is disabled, or the player is not an ally
						if(!isAlliedBuildingEnable || !isPlayerAlly) {
							// If territory is peaceful, prevent all block edits by others
							if (territory.getKingdom().isPeaceful()) {
								ChatUtil.sendNotice(event.getPlayer(), MessagePath.PROTECTION_NOTICE_PEACEFUL_TOWN.getMessage(town.getName()));
								event.setCancelled(true);
								return;
							}
							// If other player is peaceful, prevent all block edits
							if (player.getKingdom().isPeaceful()) {
								ChatUtil.sendNotice(event.getPlayer(), MessagePath.PROTECTION_NOTICE_PEACEFUL_PLAYER.getMessage());
								event.setCancelled(true);
								return;
							}
							// If no players are online from this Kingdom, prevent block edits
							if (town.getKingdom().isOfflineProtected()) {
								ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_ONLINE.getMessage(town.getKingdom().getName(), town.getName()));
								event.setCancelled(true);
								return;
							}
							// If town is upgraded to require a minimum online resident amount, prevent block edits
							if(town.isTownWatchProtected()) {
								int upgradeLevelWatch = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.WATCH);
								ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_UPGRADE.getMessage(town.getName(), KonUpgrade.WATCH.getName(), upgradeLevelWatch));
								event.setCancelled(true);
								return;
							}
						}
						// If block is a container, protect (optionally)
						boolean isProtectChest = konquest.getCore().getBoolean(CorePath.KINGDOMS_PROTECT_CONTAINERS_BREAK.getPath(),true);
						if(isProtectChest && event.getBlock().getState() instanceof BlockInventoryHolder) {
							ChatUtil.sendKonBlockedProtectionTitle(player);
							event.setCancelled(true);
							return;
						}
						// Check for enemy player
						if (playerRole.equals(KonquestRelationshipType.ENEMY)) {
							// The player is an enemy and may edit blocks
							// Check for barbarian allowed to attack
							boolean isBarbarianAttackEnabled = konquest.getCore().getBoolean(CorePath.BARBARIANS_ALLOW_ATTACK_KINGDOMS.getPath(), true);
							if (player.isBarbarian() && !isBarbarianAttackEnabled) {
								ChatUtil.sendKonBlockedProtectionTitle(player);
								event.setCancelled(true);
								return;
							}
							// Check for capital capture conditions
							if(isCapital && territory.getKingdom().isCapitalImmune()) {
								// Capital is immune and cannot be attacked
								int numTowns = territory.getKingdom().getNumTowns();
								ChatUtil.sendKonBlockedProtectionTitle(player);
								ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_CAPITAL_IMMUNE.getMessage(numTowns, territory.getKingdom().getName()));
								event.setCancelled(true);
								return;
							}
							// Verify town can be captured
							if(town.isCaptureDisabled()) {
								ChatUtil.sendKonBlockedProtectionTitle(player);
								ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_CAPTURE.getMessage(town.getCaptureCooldownString()));
								event.setCancelled(true);
								return;
							}
							// If not enough players are online in the attacker's kingdom, prevent block edits
							boolean isNoProtectedAttack = konquest.getCore().getBoolean(CorePath.KINGDOMS_NO_PROTECTED_ATTACKING.getPath(),false);
							if (isNoProtectedAttack && player.getKingdom().isOfflineProtected()) {
								ChatUtil.sendKonBlockedProtectionTitle(player);
								ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_PROTECTED_ATTACK.getMessage(town.getName()));
								event.setCancelled(true);
								return;
							}
							/* This town can be attacked... */
							
							// Event pre-checks
							boolean isMonument = town.getMonument().isLocInside(breakLoc);
							boolean isCritical = isMonument && event.getBlock().getType().equals(konquest.getKingdomManager().getTownCriticalBlock());
							// Fire event for general attack
							KonquestTownAttackEvent invokeEvent = new KonquestTownAttackEvent(konquest, town, player, event.getBlock(), isMonument, isCritical);
							Konquest.callKonquestEvent(invokeEvent);
							if(invokeEvent.isCancelled()) {
								event.setCancelled(true);
								return;
							}
							// Update MonumentBar state
							town.setAttacked(true,player);
							town.updateBarTitle();
							town.applyGlow(event.getPlayer());
							// Attempt to start a raid alert
							town.sendRaidAlert(player);
							// If town is shielded, prevent all enemy block edits
							if(town.isShielded()) {
								ChatUtil.sendKonBlockedShieldTitle(player);
								event.setCancelled(true);
								return;
							}
							// If town is armored, damage the armor while preventing block breaks
							if(town.isArmored()) {
								// Ignore instant-break blocks
								Material blockMat = event.getBlock().getState().getType();
								//ChatUtil.printDebug("Armor block "+blockMat.toString()+" broke of hardness "+blockMat.getHardness()+", max durability "+blockMat.getMaxDurability()+", solid "+blockMat.isSolid());
								if(blockMat.getHardness() > 0.0 && blockMat.isSolid() && konquest.getKingdomManager().isArmorValid(blockMat)) {
									town.damageArmor(1);
									Konquest.playTownArmorSound(event.getPlayer());
								} else {
									ChatUtil.sendKonBlockedShieldTitle(player);
								}
								event.setCancelled(true);
								return;
							}
							// If block is inside a monument, handle it
							if(town.isLocInsideMonumentProtectionArea(breakLoc)) {
								if(isMonument) {
									// Prevent monument attack when template is blanking or invalid
									if(!town.getKingdom().getMonumentTemplate().isValid()) {
										ChatUtil.sendKonBlockedProtectionTitle(player);
										event.setCancelled(true);
										return;
									}
									// Prevent Barbarians from damaging monuments optionally
									boolean isBarbAttackAllowed = konquest.getCore().getBoolean(CorePath.TOWNS_BARBARIANS_DESTROY.getPath(),true);
									if(player.isBarbarian() && !isBarbAttackAllowed) {
										ChatUtil.sendKonBlockedProtectionTitle(player);
										event.setCancelled(true);
										return;
									}
									// Prevent capture for property flag
									if(!town.getPropertyValue(KonPropertyFlag.CAPTURE)) {
										ChatUtil.sendKonBlockedFlagTitle(player);
										event.setCancelled(true);
										return;
									}
									// Cancel item drops on the broken blocks
									event.setDropItems(false);
									// Handle town capture/destroy
									if(isCritical) {
										onTownCriticalHit(town, player, isCapital);
									}
								} else {
									// Prevent block breaks in the rest of the chunk
									ChatUtil.sendKonBlockedProtectionTitle(player);
									event.setCancelled(true);
									return;
								}
							}
							// Update directives
							konquest.getDirectiveManager().updateDirectiveProgress(player, KonDirective.ATTACK_TOWN);
						} else if(playerRole.equals(KonquestRelationshipType.ALLY)) {
							// Player is in an allied kingdom
							// Can break blocks if town option Allied Building is true.
							if(isAlliedBuildingEnable) {
								// Allied building is enabled in config
								if(!town.isAlliedBuildingAllowed()) {
									// This town has Allied Building set to false, do not allow edits
									ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_NO_ALLIED_BUILD.getMessage());
									ChatUtil.sendKonBlockedProtectionTitle(player);
									event.setCancelled(true);
								}
							} else {
								// Allied building is disabled in config, prevent all block edits.
								ChatUtil.sendKonBlockedProtectionTitle(player);
								event.setCancelled(true);
							}
						} else {
							// Otherwise, prevent all block edits.
							ChatUtil.sendKonBlockedProtectionTitle(player);
							event.setCancelled(true);
						}
					}
				} else if(territory instanceof KonCamp) {
					KonCamp camp = (KonCamp)territory;
					boolean isMemberAllowedContainers = konquest.getCore().getBoolean(CorePath.CAMPS_CLAN_ALLOW_CONTAINERS.getPath(), false);
					boolean isMemberAllowedEdit = konquest.getCore().getBoolean(CorePath.CAMPS_CLAN_ALLOW_EDIT_OFFLINE.getPath(), false);
					boolean isMember = false;
					if(konquest.getCampManager().isCampGrouped(camp)) {
						isMember = konquest.getCampManager().getCampGroup(camp).isPlayerMember(player.getBukkitPlayer());
					}
					// If the camp owner is not online, prevent block breaking optionally
					if(camp.isProtected() && !(isMember && isMemberAllowedEdit)) {
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_CAMP.getMessage(camp.getName()));
						event.setCancelled(true);
						return;
					}
					// If block is a container, protect (optionally) from players other than the owner
					boolean isProtectChest = konquest.getCore().getBoolean(CorePath.CAMPS_PROTECT_CONTAINERS.getPath(),true);
					if(isProtectChest && !(isMember && isMemberAllowedContainers) && !camp.isPlayerOwner(event.getPlayer()) && event.getBlock().getState() instanceof BlockInventoryHolder) {
						ChatUtil.sendKonBlockedProtectionTitle(player);
						event.setCancelled(true);
						return;
					}
					// Prevent group members from breaking beds they don't own
					if(isMember && !camp.isPlayerOwner(event.getPlayer()) && event.getBlock().getBlockData() instanceof Bed) {
						ChatUtil.sendKonBlockedProtectionTitle(player);
						event.setCancelled(true);
						return;
					}
					// Check for barbarian allowed to attack
					boolean isBarbarianAttackEnabled = konquest.getCore().getBoolean(CorePath.BARBARIANS_ALLOW_ATTACK_CAMPS.getPath(), true);
					if (player.isBarbarian() && !camp.isPlayerOwner(event.getPlayer()) && !isBarbarianAttackEnabled) {
						ChatUtil.sendKonBlockedProtectionTitle(player);
						event.setCancelled(true);
						return;
					}
					/* This camp can be attacked... */
					// Make the enemy glow
					if(!camp.isPlayerOwner(event.getPlayer()) && !isMember) {
						camp.applyGlow(event.getPlayer());
					}
					// Remove the camp if a bed is broken within it
					if(event.getBlock().getBlockData() instanceof Bed) {
						// Fire event for pre-destroy
						KonquestCampDestroyEvent invokeEvent = new KonquestCampDestroyEvent(konquest, camp, player, event.getBlock().getLocation());
						Konquest.callKonquestEvent(invokeEvent);
						// Check for cancelled
						if(invokeEvent.isCancelled()) {
							event.setCancelled(true);
							return;
						}
						KonOfflinePlayer owner = konquest.getPlayerManager().getOfflinePlayer(camp.getOwner());
						campManager.removeCamp(camp);
						ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_CAMP_DESTROY.getMessage(camp.getName()));
						KonPlayer onlineOwner = playerManager.getPlayerFromName(camp.getOwner().getName());
						if(onlineOwner != null) {
							ChatUtil.sendError(onlineOwner.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_CAMP_DESTROY_OWNER.getMessage());
						} else {
							ChatUtil.printDebug("Failed attempt to send camp destruction message to offline owner "+camp.getOwner().getName());
						}
						// Fire event for post-destroy
						Konquest.callKonquestEvent(new KonquestCampDestroyPostEvent(konquest, owner, player, event.getBlock().getLocation()));
					}
				} else if(territory instanceof KonRuin) {
					KonRuin ruin = (KonRuin)territory;
					// Check for expired cooldown
					if(ruin.isCaptureDisabled()) {
						ChatUtil.sendKonBlockedProtectionTitle(player);
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_CAPTURE.getMessage(ruin.getCaptureCooldownString()));
						event.setCancelled(true);
						return;
					}
					// Check for broken critical block within this Ruin
					if(ruin.isCriticalLocation(breakLoc)) {
						// Fire event for every attack
						KonquestRuinAttackEvent invokeEvent = new KonquestRuinAttackEvent(konquest, ruin, player, event.getBlock());
						Konquest.callKonquestEvent(invokeEvent);
						if(invokeEvent.isCancelled()) {
							event.setCancelled(true);
							return;
						}
						// Execute custom commands from config
						konquest.executeCustomCommand(CustomCommandPath.RUIN_CRITICAL,player.getBukkitPlayer());
						// Fire event for pre-capture
						List<KonPlayer> rewardPlayers = konquest.getRuinManager().getRuinPlayers(ruin,player.getKingdom());
						if(ruin.getRemainingCriticalHits() == 1) {
							KonquestRuinCaptureEvent invokeEventCapture = new KonquestRuinCaptureEvent(konquest, ruin, player, rewardPlayers);
							Konquest.callKonquestEvent(invokeEventCapture);
							if(invokeEventCapture.isCancelled()) {
								event.setCancelled(true);
								return;
							}
						}
						// Prevent critical block drop
						event.setDropItems(false);
						// Check ruin critical conditions
						boolean hitStatus = ruin.onCriticalHit(breakLoc);
						// Notify Player
						ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_CRITICAL.getMessage(ruin.getRemainingCriticalHits()));
						event.getBlock().getWorld().playSound(breakLoc, Sound.BLOCK_FIRE_EXTINGUISH, 1.0F, 0.6F);
						if(hitStatus) {
							// Ruin has been captures
							konquest.getRuinManager().rewardPlayers(ruin,player.getKingdom());
							// Fire event for post-capture
							Konquest.callKonquestEvent(new KonquestRuinCapturePostEvent(konquest, ruin, player, rewardPlayers));
							// Execute custom commands from config
							konquest.executeCustomCommand(CustomCommandPath.RUIN_CAPTURE,player.getBukkitPlayer());
						} else {
							// All golems target player
							ruin.targetAllGolemsToPlayer(event.getPlayer());
						}
					} else {
						notifyAdminBypass(event.getPlayer());
						// Prevent all other block breaks
						ChatUtil.sendKonBlockedProtectionTitle(player);
						event.setCancelled(true);
					}
				} else if(territory instanceof KonSanctuary) {
					KonSanctuary sanctuary = (KonSanctuary)territory;
					// Always prevent monument template edits
					KonMonumentTemplate template = sanctuary.getTemplate(breakLoc);
					if(template != null) {
						notifyAdminBypass(event.getPlayer());
						// Block it
						ChatUtil.sendKonBlockedProtectionTitle(player);
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
			boolean isWildBuild = konquest.getCore().getBoolean(CorePath.KINGDOMS_WILD_BUILD.getPath(), true);
			boolean isWorldValid = konquest.isWorldValid(event.getBlock().getLocation());
			if(!player.isAdminBypassActive() && !isWildBuild && isWorldValid) {
				// No building is allowed in the wild
				notifyAdminBypass(event.getPlayer());
				ChatUtil.sendKonBlockedProtectionTitle(player);
				event.setCancelled(true);
			}
		}
    }
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onDiamondMine(BlockBreakEvent event) {
		if(!event.isCancelled()) {
			if(konquest.isWorldIgnored(event.getBlock().getWorld())) {
				return;
			}
			Material blockType = event.getBlock().getType();
			boolean isDiamondBreak = blockType.equals(Material.DIAMOND_ORE);
			boolean isDeepslateDiamondBreak = false;
			try {
				isDeepslateDiamondBreak = blockType.equals(Material.DEEPSLATE_DIAMOND_ORE);
			} catch(Exception | NoSuchFieldError ignored) {}

			if(isDiamondBreak || isDeepslateDiamondBreak) {
				boolean isDrop = event.isDropItems();
				ChatUtil.printDebug("Diamond ore block break dropping items: "+isDrop);
				ItemStack handItem = event.getPlayer().getInventory().getItemInMainHand();
				if(isDrop && !handItem.containsEnchantment(Enchantment.SILK_TOUCH)) {
					if(!konquest.getPlayerManager().isOnlinePlayer(event.getPlayer())) {
						ChatUtil.printDebug("Failed to handle onBlockBreakLow for non-existent player");
						return;
					}
					KonPlayer player = playerManager.getPlayer(event.getPlayer());
					konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.DIAMONDS,1);
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onCropHarvest(BlockBreakEvent event) {
		if(event.isCancelled()) return;
		if(konquest.isWorldIgnored(event.getBlock().getWorld())) {
			return;
		}
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
					if(!konquest.getPlayerManager().isOnlinePlayer(event.getPlayer())) {
						ChatUtil.printDebug("Failed to handle onCropHarvest for non-existent player");
						return;
					}
					KonPlayer player = konquest.getPlayerManager().getPlayer(event.getPlayer());
					konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.HARVEST,1);
				}
			}
		}
	}

	/**
	 * Fires when blocks are placed.
	 * Prevent placing blocks inside protected territories.
	 * Check for barbarian bed placement.
	 * Player#setBedSpawnLocation is deprecated, but keep using it
	 * for backwards compatibility to 1.16.
	 */
	@SuppressWarnings( "deprecation" )
	@EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
		if(event.isCancelled()) {
			return;
		}
		
		if(konquest.isWorldIgnored(event.getBlock().getWorld())) {
			return;
		}
		
		// Track last block placed per player
		konquest.lastPlaced.put(event.getPlayer(),event.getBlock().getLocation());
		KonPlayer player = konquest.getPlayerManager().getPlayer(event.getPlayer());
		if (player == null) {
			ChatUtil.printDebug("Failed to handle onBlockPlace for non-existent player");
			return;
		}
		Location placeLoc = event.getBlock().getLocation();
		// Monitor blocks in claimed territory
		if(territoryManager.isChunkClaimed(placeLoc)) {
			// Bypass event restrictions for player in Admin Bypass Mode
			if(player.isAdminBypassActive()) {
				// When player is in admin bypass and places a block
				checkMonumentTemplateBlanking(event);
				return;
			}

			KonTerritory territory = territoryManager.getChunkTerritory(placeLoc);
			assert territory != null;

			// Property Flag Holders
			if(territory instanceof KonPropertyFlagHolder) {
				KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
				if(flagHolder.hasPropertyValue(KonPropertyFlag.BUILD)) {
					if(!flagHolder.getPropertyValue(KonPropertyFlag.BUILD)) {
						notifyAdminBypass(event.getPlayer());
						// Block it
						ChatUtil.sendKonBlockedFlagTitle(player);
						event.setCancelled(true);
						return;
					}
				}
			}

			// Territory-specific checks
			switch(territory.getTerritoryType()) {

				/*
				 * Capital & Town checks
				 */
				case CAPITAL:
				case TOWN:
					assert territory instanceof KonTown;
					KonTown town = (KonTown) territory;
					boolean isCapital = territory.getTerritoryType().equals(KonquestTerritoryType.CAPITAL);
					// Check player's relationship to this town
					KonquestRelationshipType playerRole = kingdomManager.getRelationRole(player.getKingdom(), town.getKingdom());

					// Stop all block edits in center chunk
					if(town.isLocInsideMonumentProtectionArea(placeLoc)) {
						ChatUtil.sendKonBlockedProtectionTitle(player);
						event.setCancelled(true);
						return;
					}

					// Checks for friendly players
					if(playerRole.equals(KonquestRelationshipType.FRIENDLY)) {
						// Notify player when there is no lord
						if(town.canClaimLordship(player)) {
							ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_TOWN_NOTICE_NO_LORD.getMessage(town.getName(),town.getTravelName()));
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
							if(town.isPlotOnly() && !town.isPlayerKnight(player.getOfflineBukkitPlayer()) && !town.hasPlot(placeLoc)) {
								// Stop when non-elite player edits non-plot land
								ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_ONLY_PLOT.getMessage());
								event.setCancelled(true);
								return;
							}
						}
						// Update directives
						konquest.getDirectiveManager().updateDirectiveProgress(player, KonDirective.BUILD_TOWN);
					} else {
						// Checks for players in other kingdoms
						boolean isPlayerAlly = playerRole.equals(KonquestRelationshipType.ALLY);
						boolean isAlliedBuildingEnable = konquest.getCore().getBoolean(CorePath.KINGDOMS_ALLY_BUILD.getPath(),false);
						// Protections for when allied building is disabled, or the player is not an ally
						if(!isAlliedBuildingEnable || !isPlayerAlly) {
							// If territory is peaceful, prevent all block damage by enemies
							if (territory.getKingdom().isPeaceful()) {
								ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_PEACEFUL_TOWN.getMessage(town.getName()));
								event.setCancelled(true);
								return;
							}
							// If enemy player is peaceful, prevent all block placement
							if (player.getKingdom().isPeaceful()) {
								ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_PEACEFUL_PLAYER.getMessage());
								event.setCancelled(true);
								return;
							}
							// If no players are online from this Kingdom, prevent block damage
							if (town.getKingdom().isOfflineProtected()) {
								ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_ONLINE.getMessage(town.getKingdom().getName(), town.getName()));
								event.setCancelled(true);
								return;
							}
							// If town is upgraded to require a minimum online resident amount, prevent block damage
							if(town.isTownWatchProtected()) {
								int upgradeLevelWatch = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.WATCH);
								ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_UPGRADE.getMessage(town.getName(), KonUpgrade.WATCH.getName(), upgradeLevelWatch));
								event.setCancelled(true);
								return;
							}
						}
						// Prevent inventory blocks from being placed
						boolean isProtectChest = konquest.getCore().getBoolean(CorePath.KINGDOMS_PROTECT_CONTAINERS_BREAK.getPath(),true);
						if(isProtectChest && event.getBlock().getState() instanceof BlockInventoryHolder) {
							ChatUtil.sendKonBlockedProtectionTitle(player);
							event.setCancelled(true);
							return;
						}
						// Check for enemy player
						if (playerRole.equals(KonquestRelationshipType.ENEMY)) {
							// The player is an enemy and may edit blocks
							// Check for capital capture conditions
							if(isCapital && territory.getKingdom().isCapitalImmune()) {
								// Capital is immune and cannot be attacked
								int numTowns = territory.getKingdom().getNumTowns();
								ChatUtil.sendKonBlockedProtectionTitle(player);
								ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_CAPITAL_IMMUNE.getMessage(numTowns, territory.getKingdom().getName()));
								event.setCancelled(true);
								return;
							}
							// Verify town can be captured
							if(town.isCaptureDisabled()) {
								ChatUtil.sendKonBlockedProtectionTitle(player);
								ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_CAPTURE.getMessage(town.getCaptureCooldownString()));
								event.setCancelled(true);
								return;
							}
							// If not enough players are online in the attacker's kingdom, prevent block edits
							boolean isNoProtectedAttack = konquest.getCore().getBoolean(CorePath.KINGDOMS_NO_PROTECTED_ATTACKING.getPath(),false);
							if (isNoProtectedAttack && player.getKingdom().isOfflineProtected()) {
								ChatUtil.sendKonBlockedProtectionTitle(player);
								ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_PROTECTED_ATTACK.getMessage(town.getName()));
								event.setCancelled(true);
								return;
							}
							/* This town can be attacked... */

							// If town is shielded, prevent all enemy block edits
							if(town.isShielded()) {
								ChatUtil.sendKonBlockedShieldTitle(player);
								event.setCancelled(true);
								return;
							}
							// If town is armored, prevent block places
							if(town.isArmored()) {
								Konquest.playTownArmorSound(event.getPlayer());
								event.setCancelled(true);
								return;
							}
						} else if(playerRole.equals(KonquestRelationshipType.ALLY)) {
							// Player is in an allied kingdom
							// Can place blocks if town option Allied Building is true.
							if(isAlliedBuildingEnable) {
								// Allied building is enabled in config
								if(!town.isAlliedBuildingAllowed()) {
									// This town has Allied Building set to false, do not allow edits
									ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_NO_ALLIED_BUILD.getMessage());
									ChatUtil.sendKonBlockedProtectionTitle(player);
									event.setCancelled(true);
								}
							} else {
								// Allied building is disabled in config, prevent all block edits.
								ChatUtil.sendKonBlockedProtectionTitle(player);
								event.setCancelled(true);
							}
						} else {
							// If the player is not an enemy, prevent block edits.
							ChatUtil.sendKonBlockedProtectionTitle(player);
							event.setCancelled(true);
							return;
						}
					}
					break;

				/*
				 * Camp checks
				 */
				case CAMP:
					assert territory instanceof KonCamp;
					KonCamp camp = (KonCamp)territory;
					boolean isMemberAllowedEdit = konquest.getCore().getBoolean(CorePath.CAMPS_CLAN_ALLOW_EDIT_OFFLINE.getPath(), false);
					boolean isMember = false;
					if(konquest.getCampManager().isCampGrouped(camp)) {
						isMember = konquest.getCampManager().getCampGroup(camp).isPlayerMember(player.getBukkitPlayer());
					}
					// Prevent additional beds from being placed by anyone
					if(event.getBlock().getBlockData() instanceof Bed) {
						if(camp.isPlayerOwner(player.getBukkitPlayer())){
							// The owner is placing a new bed
							camp.setBedLocation(event.getBlock().getLocation());
							player.getBukkitPlayer().setBedSpawnLocation(event.getBlock().getLocation(), true);
							ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_CAMP_UPDATE.getMessage());
						} else {
							// This player is not the owner, prevent bed placements
							ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_CAMP_BED.getMessage());
							event.setCancelled(true);
							return;
						}
					}
					// If the camp owner is not online, prevent block placement optionally for clan members
					if(camp.isProtected() && !(isMember && isMemberAllowedEdit)) {
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_CAMP.getMessage(camp.getName()));
						event.setCancelled(true);
						return;
					}
					break;

				/*
				 * Ruin checks
				 */
				case RUIN:
					// Prevent all placement within ruins
					notifyAdminBypass(event.getPlayer());
					ChatUtil.sendKonBlockedProtectionTitle(player);
					event.setCancelled(true);
					return;

				/*
				 * Sanctuary checks
				 */
				case SANCTUARY:
					assert territory instanceof KonSanctuary;
					KonSanctuary sanctuary = (KonSanctuary)territory;
					// Always prevent monument template edits
					KonMonumentTemplate template = sanctuary.getTemplate(placeLoc);
					if(template != null) {
						notifyAdminBypass(event.getPlayer());
						// Block it
						ChatUtil.sendKonBlockedProtectionTitle(player);
						event.setCancelled(true);
						return;
					}
					break;

				default:
					// Unknown territory type, do nothing.
					break;
			}

		} else {
			// When placing blocks in the wilderness...
			if(player.isAdminBypassActive()) return;

			// Prevent barbarians who already have a camp from placing a bed
			// Attempt to create a camp for barbarians who place a bed
			// Check if the player is a barbarian placing a bed
			if(player.isBarbarian() && event.getBlock().getBlockData() instanceof Bed && player.getBukkitPlayer().hasPermission("konquest.create.camp")) {
				// Fire event
				KonquestPlayerCampEvent invokePreEvent = new KonquestPlayerCampEvent(konquest, player, event.getBlock().getLocation());
				Konquest.callKonquestEvent(invokePreEvent);
				// Check for cancelled
				if(invokePreEvent.isCancelled()) {
					event.setCancelled(true);
					return;
				}
				boolean status = campManager.addCampForPlayer(event.getBlock().getLocation(), player);
				if(!status) {
					event.setCancelled(true);
				}
			} else {
				// Wild placement by non-barbarian
				boolean isWildBuild = konquest.getCore().getBoolean(CorePath.KINGDOMS_WILD_BUILD.getPath(), true);
				boolean isWorldValid = konquest.isWorldValid(event.getBlock().getLocation());
				if(!isWildBuild && isWorldValid) {
					// No building is allowed in the wild in valid worlds
					notifyAdminBypass(event.getPlayer());
					ChatUtil.sendKonBlockedProtectionTitle(player);
					event.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onSeedPlant(BlockPlaceEvent event) {
		if(!event.isCancelled()) {
			if(konquest.isWorldIgnored(event.getBlock().getWorld())) {
				return;
			}
			if(!konquest.getPlayerManager().isOnlinePlayer(event.getPlayer())) {
				ChatUtil.printDebug("Failed to handle onSeedPlant for non-existent player");
				return;
			}
			Material placedMat = event.getBlockPlaced().getType();
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
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onFarmTill(BlockPlaceEvent event) {
		if(!event.isCancelled()) {
			if(konquest.isWorldIgnored(event.getBlock().getWorld())) {
				return;
			}
			Material placedMat = event.getBlockPlaced().getType();
			if(placedMat.equals(Material.FARMLAND)) {
				if(!konquest.getPlayerManager().isOnlinePlayer(event.getPlayer())) {
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
	 */
	@EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
		// Protect blocks inside of territory
		if(konquest.isWorldIgnored(event.getBlock().getWorld())) return;
		for(Block block : event.blockList()) {
			if(territoryManager.isChunkClaimed(block.getLocation())) {
				KonTerritory territory = territoryManager.getChunkTerritory(block.getLocation());
				Material blockMat = block.getType();
				
				// Protect Sanctuaries
				if(territory.getTerritoryType().equals(KonquestTerritoryType.SANCTUARY)) {
					ChatUtil.printDebug("protecting Sanctuary from block explosion");
					event.setCancelled(true);
					return;
				}
				// Protect Peaceful Territory
				if(territory.getKingdom().isPeaceful()) {
					ChatUtil.printDebug("protecting peaceful kingdom from block explosion");
					event.setCancelled(true);
					return;
				}
				// Town & Capital protections
				if(territory instanceof KonTown) {
					KonTown town = (KonTown)territory;
					// Protect Town Monuments
					if(town.isLocInsideMonumentProtectionArea(block.getLocation())) {
						ChatUtil.printDebug("protecting Town Monument from block explosion");
						event.setCancelled(true);
						return;
					}
					// Protect towns when all kingdom members are offline
					if(town.getKingdom().isOfflineProtected()) {
						ChatUtil.printDebug("protecting offline Town from block explosion");
						event.setCancelled(true);
						return;
					}
					// If town is upgraded to require a minimum online resident amount, prevent block damage
					if(town.isTownWatchProtected()) {
						ChatUtil.printDebug("protecting upgraded Town from block explosion");
						event.setCancelled(true);
						return;
					}
					// Check for capital capture conditions
					if(territory.getTerritoryType().equals(KonquestTerritoryType.CAPITAL) && territory.getKingdom().isCapitalImmune()) {
						// Capital is immune and cannot be attacked
						ChatUtil.printDebug("protecting immune capital from block explosion");
						event.setCancelled(true);
						return;
					}
					// Verify town can be captured
					if(town.isCaptureDisabled()) {
						ChatUtil.printDebug("protecting town in capture cooldown from block explosion");
						event.setCancelled(true);
						return;
					}
					// If town is shielded, prevent all enemy block edits
					if(town.isShielded()) {
						event.setCancelled(true);
						return;
					}
					// If town is armored, damage the armor while preventing block breaks
					if(town.isArmored() && blockMat.getHardness() > 0.0 && blockMat.isSolid() && konquest.getKingdomManager().isArmorValid(blockMat)) {
						int damage = konquest.getCore().getInt(CorePath.TOWNS_ARMOR_TNT_DAMAGE.getPath(),1);
						town.damageArmor(damage);
						Konquest.playTownArmorSound(event.getBlock().getLocation());
						event.setCancelled(true);
						return;
					}
				}
				// Camp protections
				if(territory.getTerritoryType().equals(KonquestTerritoryType.CAMP)) {
					assert territory instanceof KonCamp;
					KonCamp camp = (KonCamp)territory;
					// Protect offline owner camps
					if(camp.isProtected()) {
						ChatUtil.printDebug("EVENT: protecting offline camp from block explosion");
						event.setCancelled(true);
						return;
					}
				}
				
				// Protect chests
				boolean isProtectChest = konquest.getCore().getBoolean(CorePath.KINGDOMS_PROTECT_CONTAINERS_EXPLODE.getPath(),true);
				if(isProtectChest && event.getBlock().getState() instanceof BlockInventoryHolder) {
					ChatUtil.printDebug("EVENT: protecting chest inside territory from block explosion");
					event.setCancelled(true);
					return;
				}
				// Protect beds
				if(event.getBlock().getBlockData() instanceof Bed) {
					ChatUtil.printDebug("EVENT: protecting bed inside territory from block explosion");
					event.setCancelled(true);
					return;
				}
				// Protect Ruins
				if(territory.getTerritoryType().equals(KonquestTerritoryType.RUIN)) {
					ChatUtil.printDebug("protecting Ruin from block explosion");
					event.setCancelled(true);
					return;
				}
			}
		}
	}
	
	/*
	 * Always prevent moving monument blocks.
	 * When all piston parts + blocks are inside ruins, allow, else block.
	 * Send blocked message to non-residents in towns.
	 * Allow admin bypass
	 * 
	 */
	@EventHandler(priority = EventPriority.HIGH)
    public void onPistonExtend(BlockPistonExtendEvent event) {
		// TODO: Add blocked messages to players somehow
		if(konquest.isWorldIgnored(event.getBlock().getWorld())) return;
		Location pistonBaseLoc = event.getBlock().getLocation();
		List<Location> checkLocs = new ArrayList<>();
		// Gather list of locations to check
		for(Block pushBlock : event.getBlocks()) {
			Location blockLoc = pushBlock.getLocation();
			if(!checkLocs.contains(blockLoc)) {
				checkLocs.add(blockLoc);
			}
			Location pushTo = new Location(pushBlock.getWorld(),pushBlock.getX()+event.getDirection().getModX(),pushBlock.getY()+event.getDirection().getModY(),pushBlock.getZ()+event.getDirection().getModZ());
			if(!checkLocs.contains(pushTo)) {
				checkLocs.add(pushTo);
			}
		}
		boolean isPistonProtect = konquest.getCore().getBoolean(CorePath.KINGDOMS_PROTECT_PISTONS_USE.getPath(),true);
		// Review the list of affected blocks
		for(Location extendLoc : checkLocs) {
			// Check for territory edits
			if(territoryManager.isChunkClaimed(extendLoc)) {
				KonTerritory territory = territoryManager.getChunkTerritory(extendLoc);
				// Territory checks
				if(territory instanceof KonSanctuary) {
					KonSanctuary sanctuary = (KonSanctuary) territory;
					// Check if this block is within a monument template
					KonMonumentTemplate template = sanctuary.getTemplate(extendLoc);
					if(template != null) {
						event.setCancelled(true);
						return;
					}
					// If piston is outside of territory
					if(!sanctuary.isLocInside(pistonBaseLoc)) {
						event.setCancelled(true);
						return;
					}
				} else if(territory instanceof KonTown) {
					KonTown town = (KonTown) territory;
					// Check if this block is within a monument
					if(town.isLocInsideMonumentProtectionArea(extendLoc)) {
						event.setCancelled(true);
						return;
					}
					// Check if block is inside an offline kingdom
					if(isPistonProtect && town.getKingdom().isOfflineProtected()) {
						event.setCancelled(true);
						return;
					}
					// If town is shielded or armored, and piston is outside of territory
					if((town.isShielded() || town.isArmored()) && !town.isLocInside(pistonBaseLoc)) {
						event.setCancelled(true);
						return;
					}
				} else if(territory instanceof KonRuin) {
					KonRuin ruin = (KonRuin) territory;
					// If block is critical
					if(ruin.isCriticalLocation(extendLoc)) {
						event.setCancelled(true);
						return;
					}
					boolean isBaseInside = ruin.isLocInside(pistonBaseLoc);
					boolean isBlockInside = ruin.isLocInside(extendLoc);
					// If piston is outside of territory
					if(!isBaseInside) {
						event.setCancelled(true);
						return;
					}
					// If piston is inside but block is outside
					if(!isBlockInside) {
						event.setCancelled(true);
						return;
					}
				} else if(territory instanceof KonCamp) {
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
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onPistonRetract(BlockPistonRetractEvent event) {
		if(konquest.isWorldIgnored(event.getBlock().getWorld())) return;
		Location pistonBaseLoc = event.getBlock().getLocation();
		List<Location> checkLocs = new ArrayList<>();
		// Gather list of locations to check
		for(Block pullBlock : event.getBlocks()) {
			Location blockLoc = pullBlock.getLocation();
			if(!checkLocs.contains(blockLoc)) {
				checkLocs.add(blockLoc);
			}
			Location pullTo = new Location(pullBlock.getWorld(),pullBlock.getX()+event.getDirection().getModX(),pullBlock.getY()+event.getDirection().getModY(),pullBlock.getZ()+event.getDirection().getModZ());
			if(!checkLocs.contains(pullTo)) {
				checkLocs.add(pullTo);
			}
		}
		boolean isPistonProtect = konquest.getCore().getBoolean(CorePath.KINGDOMS_PROTECT_PISTONS_USE.getPath(),true);
		// Review the list of affected blocks
		for(Location retractLoc : checkLocs) {
			// Check for territory edits
			if(territoryManager.isChunkClaimed(retractLoc)) {
				KonTerritory territory = territoryManager.getChunkTerritory(retractLoc);
				// Territory checks
				if(territory instanceof KonSanctuary) {
					KonSanctuary sanctuary = (KonSanctuary) territory;
					// Check if this block is within a monument template
					KonMonumentTemplate template = sanctuary.getTemplate(retractLoc);
					if(template != null) {
						event.setCancelled(true);
						return;
					}
					// If piston is outside of territory
					if(!sanctuary.isLocInside(pistonBaseLoc)) {
						event.setCancelled(true);
						return;
					}
				} else if(territory instanceof KonTown) {
					KonTown town = (KonTown) territory;
					// Check if this block is within a monument
					if(town.isLocInsideMonumentProtectionArea(retractLoc)) {
						event.setCancelled(true);
						return;
					}
					// Check if block is inside an offline kingdom
					if(isPistonProtect && town.getKingdom().isOfflineProtected()) {
						event.setCancelled(true);
						return;
					}
					// If town is shielded or armored, and piston is outside of territory
					if((town.isShielded() || town.isArmored()) && !town.isLocInside(pistonBaseLoc)) {
						event.setCancelled(true);
						return;
					}
				} else if(territory instanceof KonRuin) {
					KonRuin ruin = (KonRuin) territory;
					// If block is critical
					if(ruin.isCriticalLocation(retractLoc)) {
						event.setCancelled(true);
						return;
					}
					boolean isBaseInside = ruin.isLocInside(pistonBaseLoc);
					boolean isBlockInside = ruin.isLocInside(retractLoc);
					// If piston is outside of territory
					if(!isBaseInside) {
						event.setCancelled(true);
						return;
					}
					// If piston is inside but block is outside
					if(!isBlockInside) {
						event.setCancelled(true);
						return;
					}
				} else if(territory instanceof KonCamp) {
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
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onFluidFlow(BlockFromToEvent event) {
		if(konquest.isWorldIgnored(event.getBlock().getWorld())) {
			return;
		}
		// Prevent all flow into monument
		if(isBlockInsideMonument(event.getToBlock())) {
			event.setCancelled(true);
			return;
		}
		// Prevent all flow from wild into territory
		Location locTo = event.getToBlock().getLocation();
		Location locFrom = event.getBlock().getLocation();
		if(!locTo.equals(locFrom)) {
    		boolean isTerritoryTo = territoryManager.isChunkClaimed(locTo);
    		boolean isTerritoryFrom = territoryManager.isChunkClaimed(locFrom);
    		
    		if(isTerritoryTo) {
    			if(isTerritoryFrom) {
    				// Between enemy territories
        			KonTerritory territoryTo = territoryManager.getChunkTerritory(locTo);
        			KonTerritory territoryFrom = territoryManager.getChunkTerritory(locFrom);
        			if(!territoryTo.getKingdom().equals(territoryFrom.getKingdom())) {
        				event.setCancelled(true);
					}
    			} else {
    				// Wild to any territory
        			event.setCancelled(true);
				}
    		}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onBlockForm(BlockFormEvent event) {
		if(konquest.isWorldIgnored(event.getBlock().getWorld())) {
			return;
		}
		if(isBlockInsideMonument(event.getBlock())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onBlockGrow(BlockGrowEvent event) {
		if(konquest.isWorldIgnored(event.getBlock().getWorld())) {
			return;
		}
		if(isBlockInsideMonument(event.getBlock())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onBlockSpread(BlockSpreadEvent event) {
		if(konquest.isWorldIgnored(event.getBlock().getWorld())) {
			return;
		}
		// Prevent all spread inside Monument
		if(isBlockInsideMonument(event.getBlock())) {
			event.setCancelled(true);
			return;
		}
		// Territory checks
		if(territoryManager.isChunkClaimed(event.getBlock().getLocation())) {
			KonTerritory territory = territoryManager.getChunkTerritory(event.getBlock().getLocation());
			if(territory instanceof KonTown) {
				KonTown town = (KonTown) territory;
				// Prevent fire spread inside upgraded Towns
				int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.DAMAGE);
				if(event.getSource().getType().equals(Material.FIRE) && upgradeLevel >= 1) {
					ChatUtil.printDebug("EVENT: Stopped fire spread in upgraded town, DAMAGE");
					event.getSource().setType(Material.AIR);
					event.setCancelled(true);
					return;
				}
				// If town is upgraded to require a minimum online resident amount, prevent block damage
				if(event.getSource().getType().equals(Material.FIRE) && town.isTownWatchProtected()) {
					ChatUtil.printDebug("EVENT: Stopped fire spread in upgraded town, WATCH");
					event.getSource().setType(Material.AIR);
					event.setCancelled(true);
					return;
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
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onBlockIgnite(BlockIgniteEvent event) {
		if(konquest.isWorldIgnored(event.getBlock().getWorld())) return;
		if(!isBlockInsideMonument(event.getBlock())) return;

		event.setCancelled(true);

	}
	
	/*
	 * Check if the given block is inside any town/capital monument or monument template
	 */
	private boolean isBlockInsideMonument(Block block) {
		boolean result = false;
		if(territoryManager.isChunkClaimed(block.getLocation())) {
			KonTerritory territory = territoryManager.getChunkTerritory(block.getLocation());
			if(territory instanceof KonSanctuary && ((KonSanctuary) territory).isLocInsideTemplate(block.getLocation())) {
				result = true;
			}
			if(territory instanceof KonTown && ((KonTown) territory).isLocInsideMonumentProtectionArea(block.getLocation())) {
				result = true;
			}
		}
		return result;
	}
	
	private void checkMonumentTemplateBlanking(BlockEvent event) {
		// Monitor monument templates for blanking by edits from admin bypass players
		Location blockLoc = event.getBlock().getLocation();
		if(territoryManager.isChunkClaimed(blockLoc)) {
			KonTerritory territory = territoryManager.getChunkTerritory(blockLoc);
			if(territory instanceof KonSanctuary) {
				KonSanctuary sanctuary = (KonSanctuary)territory;
				KonMonumentTemplate template = sanctuary.getTemplate(blockLoc);
				if(template != null) {
					// Start monument blanking timer to prevent monument pastes
					sanctuary.startTemplateBlanking(template.getName());
				}
			}
		}
	}
	
	private void onTownCriticalHit(KonTown town, KonPlayer player, boolean isCapital) {
		// Critical block has been destroyed
		if(isCapital) {
			ChatUtil.printDebug("Critical strike on Monument in Capital "+town.getName());
		} else {
			ChatUtil.printDebug("Critical strike on Monument in Town "+town.getName());
		}
		// Evaluate pre-events on final critical hits
		int maxCriticalhits = konquest.getKingdomManager().getMaxCriticalHits();
		if(town.getMonument().getCriticalHits()+1 >= maxCriticalhits) {
			if(isCapital) {
				// Fire event pre-conquer
				KonquestPlayerConquerEvent invokeEvent = new KonquestPlayerConquerEvent(konquest, player, town.getKingdom());
				Konquest.callKonquestEvent(invokeEvent);
				if (invokeEvent.isCancelled()) {
					return;
				}
			}
			if(player.isBarbarian()) {
				// Fire event pre-destroy
				KonquestTownDestroyEvent invokeEvent = new KonquestTownDestroyEvent(konquest, town, player, isCapital);
				Konquest.callKonquestEvent(invokeEvent);
				if (invokeEvent.isCancelled()) {
					return;
				}
			} else {
				// Fire event pre-capture
				KonquestTownCaptureEvent invokeEvent = new KonquestTownCaptureEvent(konquest, town, player, player.getKingdom(), isCapital);
				Konquest.callKonquestEvent(invokeEvent);
				if(invokeEvent.isCancelled()) {
					return;
				}
			}
		}
		// Update critical hits
		town.getMonument().addCriticalHit();
		// Update bar progress
		town.updateBarTitle();
		// Check for capital swap cancellation
		if (town.getKingdom().isCapitalSwapInProgress(town)) {
			// This town or capital is being swapped, cancel it
			town.getKingdom().cancelCapitalSwapWarmup();
		}
		// Evaluate town capture conditions
		if(town.getMonument().getCriticalHits() >= maxCriticalhits) {
			// The Town is at critical max, conquer or destroy
			if(player.isBarbarian()) {
				// Destroy the town when the enemy is a barbarian
				String townName = town.getName();
				String kingdomName = town.getKingdom().getName();
				ArrayList<KonPlayer> monumentPlayers = new ArrayList<>();
				ArrayList<KonPlayer> townLocPlayers = new ArrayList<>();
				for(KonPlayer onlinePlayer : playerManager.getPlayersOnline()) {
					if(town.isLocInsideCenterChunk(onlinePlayer.getBukkitPlayer().getLocation())) {
						monumentPlayers.add(onlinePlayer);
					}
					if(town.isLocInside(onlinePlayer.getBukkitPlayer().getLocation())) {
						townLocPlayers.add(onlinePlayer);
					}
				}
				int x = town.getCenterLoc().getBlockX();
				int y = town.getCenterLoc().getBlockY();
				int z = town.getCenterLoc().getBlockZ();
				Location townCenterLoc = town.getCenterLoc();
				boolean result = false;
				if(isCapital) {
					// Destroy the kingdom
					result = kingdomManager.removeKingdom(town.getKingdom().getName());
					if(!result) {
						ChatUtil.printDebug("Problem destroying Kingdom "+town.getKingdom().getName()+" for a barbarian raider "+player.getBukkitPlayer().getName());
					}
				} else {
					// Destroy the town
					result = kingdomManager.removeTown(town.getName(), town.getKingdom().getName());
					if(!result) {
						ChatUtil.printDebug("Problem destroying Town "+town.getName()+" in Kingdom "+town.getKingdom().getName()+" for a barbarian raider "+player.getBukkitPlayer().getName());
					}
				}
				if(result) {
					// Town is removed, no longer exists
					if(isCapital) {
						ChatUtil.sendBroadcast(MessagePath.PROTECTION_NOTICE_KINGDOM_RAZE.getMessage(kingdomName));
					} else {
						ChatUtil.sendBroadcast(MessagePath.PROTECTION_NOTICE_RAZE.getMessage(townName));
					}
					ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_DESTROY.getMessage(townName));
					for(KonPlayer monPlayer : monumentPlayers) {
						monPlayer.getBukkitPlayer().teleport(konquest.getSafeRandomCenteredLocation(townCenterLoc, 2));
						monPlayer.getBukkitPlayer().playEffect(monPlayer.getBukkitPlayer().getLocation(), Effect.ANVIL_LAND, null);
					}
					for(KonPlayer locPlayer : townLocPlayers) {
						// Clear mob targets for all players within the old town
						locPlayer.clearAllMobAttackers();
						// Update particle border renders for nearby players
						territoryManager.updatePlayerBorderParticles(locPlayer);
					}
					// Update directive progress
					konquest.getDirectiveManager().updateDirectiveProgress(player, KonDirective.CAPTURE_TOWN);
					// Broadcast to Dynmap
					konquest.getMapHandler().postBroadcast(MessagePath.PROTECTION_NOTICE_RAZE.getMessage(townName)+" ("+x+","+y+","+z+")");
					// Fire event post-destroy
					Konquest.callKonquestEvent(new KonquestTownDestroyPostEvent(konquest, townName, kingdomName, player, isCapital));
				}

			} else {
				// Conquer the town for the enemy player's kingdom
				KonKingdom oldKingdom = town.getKingdom();
				String oldKingdomName = town.getKingdom().getName();
				String newKingdomName = player.getKingdom().getName();
				KonTown capturedTown;
				if(isCapital) {
					// Capture the capital
					capturedTown = (KonTown)kingdomManager.captureCapitalForPlayer(town.getKingdom().getName(), player);
				} else {
					// Capture the town
					capturedTown = (KonTown)kingdomManager.captureTownForPlayer(town.getName(), town.getKingdom().getName(), player);
				}
				
				if(capturedTown != null) {
					town = null;
					if(isCapital) {
						ChatUtil.sendBroadcast(MessagePath.PROTECTION_NOTICE_KINGDOM_CONQUER.getMessage(oldKingdomName,newKingdomName));
					} else {
						ChatUtil.sendBroadcast(MessagePath.PROTECTION_NOTICE_CONQUER.getMessage(capturedTown.getName()));
					}
					ChatUtil.printDebug("Monument conversion in Town "+capturedTown.getName());
					ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_CAPTURE.getMessage(capturedTown.getName(),player.getKingdom().getName()));
					// Start Capture disable timer for target town
					capturedTown.setIsCaptureDisabled(true); // includes cooldown timer
					// Execute custom commands from config
					konquest.executeCustomCommand(CustomCommandPath.TOWN_MONUMENT_CAPTURE,player.getBukkitPlayer());
					// Update directive progress
					konquest.getDirectiveManager().updateDirectiveProgress(player, KonDirective.CAPTURE_TOWN);
					// Update stat
					konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.CAPTURES,1);
					if(isCapital) {
						konquest.getAccomplishmentManager().modifyPlayerStat(player, KonStatsType.CONQUESTS, 1);
					}
					// Broadcast to Dynmap
					int x = capturedTown.getCenterLoc().getBlockX();
					int y = capturedTown.getCenterLoc().getBlockY();
					int z = capturedTown.getCenterLoc().getBlockZ();
					konquest.getMapHandler().postBroadcast(MessagePath.PROTECTION_NOTICE_CONQUER.getMessage(capturedTown.getName())+" ("+x+","+y+","+z+")");
					// Fire event post-capture
					Konquest.callKonquestEvent(new KonquestTownCapturePostEvent(konquest, capturedTown, player, oldKingdom, isCapital));
				} else {
					ChatUtil.printDebug("Problem converting Town "+town.getName()+" from Kingdom "+town.getKingdom().getName()+" to "+player.getKingdom().getName());
					// If, for example, a player in the Barbarians default kingdom captured the monument
					town.refreshMonument();
					// Stop the town monument timer
					ChatUtil.printDebug("Stopping monument timer with taskID "+town.getMonumentTimer().getTaskID());
					town.getMonumentTimer().stopTimer();
					// Reset the town MonumentBar
					town.setAttacked(false,player);
					town.setBarProgress(1.0);
					town.updateBarTitle();
				}
			}
		} else {
			// Town has not yet been captured
			int remainingHits = maxCriticalhits - town.getMonument().getCriticalHits();
			int defendReward = konquest.getCore().getInt(CorePath.FAVOR_REWARDS_DEFEND_RAID.getPath());
			ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_CRITICAL.getMessage(remainingHits));
			konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.CRITICALS,1);
			String kingdomName = town.getKingdom().getName();
			// Execute custom commands from config
			konquest.executeCustomCommand(CustomCommandPath.TOWN_MONUMENT_CRITICAL,player.getBukkitPlayer());
			// Alert all players of enemy Kingdom when the first critical block is broken
			if(town.getMonument().getCriticalHits() == 1) {
				for(KonPlayer kingdomPlayer : playerManager.getPlayersInKingdom(kingdomName)) {
					ChatUtil.sendKonPriorityTitle(kingdomPlayer, ChatColor.DARK_RED+MessagePath.PROTECTION_NOTICE_RAID_ALERT.getMessage(), ChatColor.DARK_RED+""+town.getName(), 60, 1, 10);
					ChatUtil.sendNotice(kingdomPlayer.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_RAID_CAPTURE_1.getMessage(town.getName(),town.getTravelName(),defendReward),ChatColor.DARK_RED);
				}
				// Discord integration
				konquest.getIntegrationManager().getDiscordSrv().alertDiscordChannel(kingdomName, MessagePath.PROTECTION_NOTICE_RAID_DISCORD_CHANNEL.getMessage(town.getName()));
				for(OfflinePlayer allPlayer : playerManager.getAllBukkitPlayersInKingdom(kingdomName)) {
					konquest.getIntegrationManager().getDiscordSrv().alertDiscordMember(allPlayer, MessagePath.PROTECTION_NOTICE_RAID_DISCORD_DIRECT.getMessage(town.getName(),kingdomName));
				}
			}
			// Alert all players of enemy Kingdom when half of critical blocks are broken
			if(town.getMonument().getCriticalHits() == maxCriticalhits/2) {
				for(KonPlayer kingdomPlayer : playerManager.getPlayersInKingdom(kingdomName)) {
					ChatUtil.sendKonPriorityTitle(kingdomPlayer, ChatColor.DARK_RED+MessagePath.PROTECTION_NOTICE_RAID_ALERT.getMessage(), ChatColor.DARK_RED+""+town.getName(), 60, 1, 10);
					ChatUtil.sendNotice(kingdomPlayer.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_RAID_CAPTURE_2.getMessage(town.getName(),town.getTravelName(),defendReward),ChatColor.DARK_RED);
				}
			}
			// Alert all players of enemy Kingdom when all but 1 critical blocks are broken
			if(town.getMonument().getCriticalHits() == maxCriticalhits-1) {
				for(KonPlayer kingdomPlayer : playerManager.getPlayersInKingdom(kingdomName)) {
					ChatUtil.sendKonPriorityTitle(kingdomPlayer, ChatColor.DARK_RED+MessagePath.PROTECTION_NOTICE_RAID_ALERT.getMessage(), ChatColor.DARK_RED+""+town.getName(), 60, 1, 10);
					ChatUtil.sendNotice(kingdomPlayer.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_RAID_CAPTURE_3.getMessage(town.getName(),town.getTravelName(),defendReward),ChatColor.DARK_RED);
				}
			}
		}
	}
	
}
