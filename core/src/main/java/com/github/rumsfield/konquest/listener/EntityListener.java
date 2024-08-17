package com.github.rumsfield.konquest.listener;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.api.event.player.KonquestPlayerCombatTagEvent;
import com.github.rumsfield.konquest.api.model.KonquestRelationshipType;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.manager.KingdomManager;
import com.github.rumsfield.konquest.manager.PlayerManager;
import com.github.rumsfield.konquest.manager.TerritoryManager;
import com.github.rumsfield.konquest.utility.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import java.util.UUID;

public class EntityListener implements Listener {

	private final KonquestPlugin konquestPlugin;
	private final Konquest konquest;
	private final KingdomManager kingdomManager;
	private final TerritoryManager territoryManager;
	private final PlayerManager playerManager;
	
	public EntityListener(KonquestPlugin plugin) {
		this.konquestPlugin = plugin;
		this.konquest = konquestPlugin.getKonquestInstance();
		this.playerManager = konquest.getPlayerManager();
		this.kingdomManager = konquest.getKingdomManager();
		this.territoryManager = konquest.getTerritoryManager();
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onItemSpawn(ItemSpawnEvent event) {
		Location dropLoc = event.getEntity().getLocation();
		if(!territoryManager.isChunkClaimed(dropLoc)) return;

		KonTerritory territory = territoryManager.getChunkTerritory(dropLoc);
		// Check for break inside of town
		if(territory instanceof KonTown) {
			KonTown town = (KonTown) territory;
			// Check for break inside of town's monument
			if(town.getMonument().isItemDropsDisabled() && town.getMonument().isLocInside(dropLoc)) {
				event.getEntity().remove();
			}
		}

	}
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onEntityBreed(EntityBreedEvent event) {
		if(event.isCancelled())  return;
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) return;
		if(event.getBreeder() instanceof Player) {
			Player bukkitPlayer = (Player)event.getBreeder();
			if(!konquest.getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
				ChatUtil.printDebug("Failed to handle onEntityBreed for non-existent player");
				return;
			}
			KonPlayer player = konquest.getPlayerManager().getPlayer(bukkitPlayer);
			konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.BREED,1);
		}

	}

	/**
	 * Prevent Endermen from picking up blocks in claimed territory
	 * @param event The block change event
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void onEntityBlockChange(EntityChangeBlockEvent event) {
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) return;
		if(event.isCancelled()) return;
		// Check for territory
		if(event.getEntityType().equals(EntityType.ENDERMAN) && territoryManager.isChunkClaimed(event.getBlock().getLocation())) {
			// Enderman block change happened in claimed territory, protect everything always
			event.setCancelled(true);
			return;
		}
	}

	private boolean isTerritoryExplosionProtected(KonTerritory territory, Entity explosion) {
		boolean isCreeper = explosion instanceof Creeper;

		// Protect Sanctuaries and Ruins always
		if(territory.getTerritoryType().equals(KonquestTerritoryType.SANCTUARY) ||
				territory.getTerritoryType().equals(KonquestTerritoryType.RUIN)) {
			return true;
		}
		// Protect territories with property flags from creepers
		if(isCreeper && territory instanceof KonPropertyFlagHolder) {
			KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
			if(flagHolder.hasPropertyValue(KonPropertyFlag.PVE)) {
				// Block creeper explosions
				if(!flagHolder.getPropertyValue(KonPropertyFlag.PVE)) {
					return true;
				}
			}
		}
		// Conditional camp protections
		if(territory instanceof KonCamp) {
			KonCamp camp = (KonCamp) territory;
			// Protect camps when owner is offline
			if(camp.isProtected()) {
				return true;
			}
		}
		// Conditional town/capital protections
		if(territory instanceof KonTown) {
			KonTown town = (KonTown)territory;

			// Protect against TNT ignited by specific players
			if (explosion instanceof TNTPrimed && explosion.hasMetadata(Konquest.metaTntOwnerId)) {
				UUID ownerId = UUID.fromString(explosion.getMetadata(Konquest.metaTntOwnerId).get(0).asString());
				Player owner = Bukkit.getPlayer(ownerId);
				KonPlayer player = playerManager.getPlayer(owner);
				if (player != null) {
					assert owner != null;
					KonquestRelationshipType playerRole = kingdomManager.getRelationRole(player.getKingdom(), town.getKingdom());
					ChatUtil.printDebug("TNT exploded with owner "+owner.getName()+" relation to "+town.getName()+" as "+playerRole);
					if (playerRole.equals(KonquestRelationshipType.PEACEFUL) ||
							playerRole.equals(KonquestRelationshipType.TRADE) ||
							playerRole.equals(KonquestRelationshipType.ALLY)) {
						ChatUtil.sendKonBlockedProtectionTitle(player);
						return true;
					} else if (!playerRole.equals(KonquestRelationshipType.FRIENDLY)) {
						// For all other non-friendly players
						// If not enough players are online in the attacker's kingdom, prevent block edits
						boolean isNoProtectedAttack = konquest.getCore().getBoolean(CorePath.KINGDOMS_NO_PROTECTED_ATTACKING.getPath(),false);
						if (isNoProtectedAttack && player.getKingdom().isOfflineProtected()) {
							ChatUtil.sendKonBlockedProtectionTitle(player);
							return true;
						}
					}
				}
			}

			// Protect peaceful towns
			if(town.getKingdom().isPeaceful()) {
				return true;
			}
			// Protect towns when all kingdom members are offline
			if(town.getKingdom().isOfflineProtected()) {
				return true;
			}
			// If town is upgraded to require a minimum online resident amount, prevent block damage
			if(town.isTownWatchProtected()) {
				return true;
			}
			// Protect when town has upgrade
			int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.DAMAGE);
			if(upgradeLevel >= 2) {
				return true;
			}
			// Check for capital capture conditions
			if(territory.getTerritoryType().equals(KonquestTerritoryType.CAPITAL) && town.getKingdom().isCapitalImmune()) {
				// Capital is immune and cannot be attacked
				return true;
			}
			// Verify town can be captured
			if(town.isCaptureDisabled()) {
				return true;
			}
			// If town is shielded, prevent all explosions
			if(town.isShielded()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Fires when entities explode.
	 * Protect territory from explosions, and optionally protect chests inside claimed territory.
	 */
	@EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
		// Protect blocks inside of territory
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) return;
		if(event.isCancelled()) return;

		// First, check for whole-territory protections
		Location explosionLoc = event.getLocation();
		boolean isInTerritory = false;
		KonTerritory wholeTerritory = null;
		if(territoryManager.isChunkClaimed(explosionLoc)) {
			wholeTerritory = territoryManager.getChunkTerritory(explosionLoc);
			isInTerritory = true;
		} else {
			for(Block block : event.blockList()) {
				if(territoryManager.isChunkClaimed(block.getLocation())) {
					wholeTerritory = territoryManager.getChunkTerritory(block.getLocation());
					isInTerritory = true;
					break;
				}
			}
		}
		if(isInTerritory && wholeTerritory != null) {
			if(isTerritoryExplosionProtected(wholeTerritory,event.getEntity())) {
				ChatUtil.printDebug("  Protected territory from explosion in "+wholeTerritory.getName());
				event.setCancelled(true);
				return;
			}
		}

		// Second, evaluate individual blocks affected by the explosion for block-based protections
		for(Block block : event.blockList()) {
			if(territoryManager.isChunkClaimed(block.getLocation())) {
				KonTerritory territory = territoryManager.getChunkTerritory(block.getLocation());
				Material blockMat = block.getType();
				// Town protections
				if(territory instanceof KonTown) {
					KonTown town = (KonTown)territory;
					// Protect Town Monuments
					if(town.isLocInsideMonumentProtectionArea(block.getLocation())) {
						ChatUtil.printDebug("protecting Town Monument from entity explosion");
						event.setCancelled(true);
						return;
					}
					// If town is armored, damage the armor while preventing explosions
					if(town.isArmored() && blockMat.getHardness() > 0.0 && blockMat.isSolid() && konquest.getKingdomManager().isArmorValid(blockMat)) {
						int damage = konquest.getCore().getInt(CorePath.TOWNS_ARMOR_TNT_DAMAGE.getPath(),1);
						town.damageArmor(damage);
						Konquest.playTownArmorSound(event.getLocation());
						event.setCancelled(true);
						return;
					}
				}
				// Protect chests
				boolean isProtectChest = konquest.getCore().getBoolean(CorePath.KINGDOMS_PROTECT_CONTAINERS_EXPLODE.getPath(),true);
				if(isProtectChest && block.getState() instanceof BlockInventoryHolder) {
					ChatUtil.printDebug("protecting chest inside Town from entity explosion");
					event.setCancelled(true);
					return;
				}
				// Protect beds
				if(block.getBlockData() instanceof Bed) {
					ChatUtil.printDebug("protecting bed inside territory from entity explosion");
					event.setCancelled(true);
					return;
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onMobSpawn(CreatureSpawnEvent event) {
		if(!territoryManager.isChunkClaimed(event.getLocation())) return;
		// Inside claimed territory...
		KonTerritory territory = territoryManager.getChunkTerritory(event.getLocation());
		//ChatUtil.printDebug("EVENT: Creature spawned in territory "+territory.getTerritoryType().toString()+", cause: "+event.getSpawnReason().toString());
		// Property Flag Holders
		if(territory instanceof KonPropertyFlagHolder) {
			KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
			if(flagHolder.hasPropertyValue(KonPropertyFlag.MOBS)) {
				if(!flagHolder.getPropertyValue(KonPropertyFlag.MOBS)) {
					// Conditions to block spawning...
					if(event.getSpawnReason().equals(SpawnReason.DROWNED) ||
							event.getSpawnReason().equals(SpawnReason.JOCKEY) ||
							event.getSpawnReason().equals(SpawnReason.BUILD_WITHER) ||
							event.getSpawnReason().equals(SpawnReason.LIGHTNING) ||
							event.getSpawnReason().equals(SpawnReason.MOUNT) ||
							event.getSpawnReason().equals(SpawnReason.NATURAL) ||
							event.getSpawnReason().equals(SpawnReason.PATROL) ||
							event.getSpawnReason().equals(SpawnReason.RAID) ||
							event.getSpawnReason().equals(SpawnReason.REINFORCEMENTS) ||
							event.getSpawnReason().equals(SpawnReason.SILVERFISH_BLOCK) ||
							event.getSpawnReason().equals(SpawnReason.SLIME_SPLIT) ||
							event.getSpawnReason().equals(SpawnReason.VILLAGE_INVASION) ||
							event.getEntityType().equals(EntityType.PHANTOM)) {
						event.setCancelled(true);
						return;
					}
				}
			}
		}
		// When a spawn event happens in a Ruin
		if(territory instanceof KonRuin) {
			// Conditions to block spawning...
			EntityType eType = event.getEntityType();
			SpawnReason eReason = event.getSpawnReason();
			boolean stopOnType = !(eType.equals(EntityType.ARMOR_STAND) || eType.equals(EntityType.IRON_GOLEM));
			boolean stopOnReason = !(eReason.equals(SpawnReason.COMMAND) || eReason.equals(SpawnReason.CUSTOM) || eReason.equals(SpawnReason.DEFAULT) || eReason.equals(SpawnReason.SPAWNER));
			if(stopOnType && stopOnReason) {
				event.setCancelled(true);
			}

		}
    }
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onGolemCreate(CreatureSpawnEvent event) {
		// Check to see if player created an Iron Golem
		if(event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.BUILD_IRONGOLEM)) {
			Player closestPlayer = null;
			double closestDistance = Double.MAX_VALUE;
			// Simple linear search
	        for (Player player : event.getLocation().getWorld().getPlayers()) {
	        	ChatUtil.printDebug("Checking for nearest block placement: "+player.getName());
	        	Location lastPlacedBlock = konquest.lastPlaced.get(player);
	            if (lastPlacedBlock != null) {
	            	ChatUtil.printDebug("Checking distance of last placed block");
	            	double distance = event.getLocation().distance(lastPlacedBlock);
	                if (distance < closestDistance && distance < 10) {
	                	closestPlayer = player;
	                    closestDistance = distance;
	                    ChatUtil.printDebug("Found closest block by "+closestPlayer.getName()+", at "+closestDistance);
	                } else {
	                	ChatUtil.printDebug("Distance is too far by "+player.getName()+", at "+distance);
	                }
	            }
	        }
			if(closestPlayer != null) {
				ChatUtil.printDebug("Iron Golem spawned by "+closestPlayer.getName());
				if(!konquest.getPlayerManager().isOnlinePlayer(closestPlayer)) {
					ChatUtil.printDebug("Failed to handle onCreatureSpawn for non-existent player");
					return;
				}
				KonPlayer player = konquest.getPlayerManager().getPlayer(closestPlayer);
				konquest.getDirectiveManager().updateDirectiveProgress(player, KonDirective.CREATE_GOLEM);
			} else {
				ChatUtil.printDebug("Iron Golem spawn could not find nearest player");
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onEntityPotionEffect(EntityPotionEffectEvent event) {
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) return;
		// prevent milk buckets from removing town nerfs in enemy towns
		if(!event.isCancelled() && event.getEntity() instanceof Player) {
			if(!konquest.getPlayerManager().isOnlinePlayer((Player)event.getEntity())) {
				ChatUtil.printDebug("Failed to handle onEntityPotionEffect for non-existent player");
				return;
			}
			Location eventLoc = event.getEntity().getLocation();
			if(event.getCause().equals(EntityPotionEffectEvent.Cause.MILK) && territoryManager.isChunkClaimed(eventLoc)) {
				KonTerritory territory = territoryManager.getChunkTerritory(eventLoc);
				KonPlayer player = konquest.getPlayerManager().getPlayer((Player)event.getEntity());
				KonquestRelationshipType playerRole = kingdomManager.getRelationRole(player.getKingdom(), territory.getKingdom());
				if(playerRole.equals(KonquestRelationshipType.ENEMY) && kingdomManager.isTownNerf(event.getModifiedType())) {
					ChatUtil.printDebug("Cancelling milk bucket removal of town nerfs for player "+player.getBukkitPlayer().getName()+" in territory "+territory.getName());
					event.setCancelled(true);
				}
			}
    	}
	}

	@EventHandler(priority = EventPriority.NORMAL)
    public void onEntityTarget(EntityTargetEvent event) {
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) return;
		Entity target = event.getTarget();
		if(target == null) return;
		Entity eAttacker = event.getEntity();
		Location tLoc = target.getLocation();
		Location eLoc = eAttacker.getLocation();
		// Check for player targets
		if(target instanceof Player) {
			Player bukkitPlayer = (Player)target;
			if(!konquest.getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
				ChatUtil.printDebug("Failed to handle onEntityTarget for non-existent player");
			} else {
				KonPlayer player = playerManager.getPlayer(bukkitPlayer);
				// Check for territory at target's location
				if(territoryManager.isChunkClaimed(tLoc)) {
					KonTerritory targetTerritory = territoryManager.getChunkTerritory(tLoc);
					// Prevent hostile mobs from targeting players inside of territory with PVE disabled
					if(eAttacker instanceof Enemy && eAttacker instanceof Mob && targetTerritory instanceof KonPropertyFlagHolder) {
						Mob monsterAttacker = (Mob)eAttacker;
						KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)targetTerritory;
						if(flagHolder.hasPropertyValue(KonPropertyFlag.PVE)) {
							if(!flagHolder.getPropertyValue(KonPropertyFlag.PVE)) {
								monsterAttacker.setTarget(null);
								event.setCancelled(true);
								return;
							}
						}
					}
				}
				// Check for territory at entity's location
				if(territoryManager.isChunkClaimed(eLoc)) {
					KonTerritory attackerTerritory = territoryManager.getChunkTerritory(eLoc);
					// Prevent Iron Golems from targeting friendly players
					if (eAttacker instanceof IronGolem) {
						IronGolem golemAttacker = (IronGolem) eAttacker;
						if (attackerTerritory.getKingdom().equals(player.getKingdom())) {
							golemAttacker.setPlayerCreated(true);
							golemAttacker.setTarget(null);
							event.setCancelled(true);
							return;
						}
					}
				}
			}
		}
		// Cancel Ruin Golems from targeting non-players
		if(!(target instanceof Player) && target instanceof LivingEntity && eAttacker instanceof IronGolem) {
			IronGolem golemAttacker = (IronGolem)eAttacker;
			for(KonRuin ruin : konquest.getRuinManager().getRuins()) {
				if(ruin.isGolem(golemAttacker)) {
					target.getWorld().spawnParticle(Particle.SOUL,target.getLocation(),10);
					target.remove();
					eLoc.getWorld().playSound(eLoc, Sound.ENTITY_GHAST_DEATH, 1.0F, 0.1F);
					event.setCancelled(true);
					// Scheduled delayed task to change target back to last player if present
					Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(),
							() -> ruin.targetGolemToLast(golemAttacker),1);
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onArrowInteract(EntityInteractEvent event) {
		// prevent arrows from interacting with things like pressure plates
		// Checks
		if(event.isCancelled()) return;
    	if(konquest.isWorldIgnored(event.getEntity().getLocation())) return;
		boolean isProtectedType = event.getEntityType().equals(CompatibilityUtil.getEntityType("item")) ||
				event.getEntityType().equals(EntityType.ARROW) ||
				event.getEntityType().equals(EntityType.SPECTRAL_ARROW);
		if(!isProtectedType) return;
		// Protect claimed territory
		if(territoryManager.isChunkClaimed(event.getBlock().getLocation())) {
			//ChatUtil.printDebug("Entity interacting in claimed territory: "+event.getEntityType());
	        // Protect all territory from arrow & dropped item interaction
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerDamage(EntityDamageEvent event) {
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) return;
		if(!(event.getEntity() instanceof Player)) return;
		Player damagePlayer = (Player)event.getEntity();
		// Try to cancel any travel warmup
		boolean doCancelTravelOnDamage = konquest.getCore().getBoolean(CorePath.TRAVEL_CANCEL_ON_DAMAGE.getPath(), false);
		if(doCancelTravelOnDamage) {
			boolean status = konquest.getTravelManager().cancelTravel(damagePlayer);
			if(status) {
				ChatUtil.sendError(damagePlayer, MessagePath.COMMAND_TRAVEL_ERROR_DAMAGED.getMessage());
			}
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageEvent event) {
		if(konquest.isWorldIgnored(event.getEntity().getWorld()))return;
		Location damageLoc = event.getEntity().getLocation();
		if(!territoryManager.isChunkClaimed(damageLoc)) return;
		KonTerritory territory = territoryManager.getChunkTerritory(damageLoc);
		// Check for damage inside of town
		if(territory instanceof KonTown) {
			KonTown town = (KonTown) territory;
			// Check for damage inside of town's monument
			if(town.getMonument().isDamageDisabled() && town.getMonument().isLocInside(damageLoc)) {
				ChatUtil.printDebug("Damage is currently disabled in "+town.getName()+" monument, cancelling");
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onVehicleDamage(VehicleDamageEvent event) {
		// Player tries to break a vehicle: boat, minecart, etc
		if (event.isCancelled()) return;
		if (konquest.isWorldIgnored(event.getVehicle().getWorld())) return;
		if(!(event.getAttacker() instanceof Player)) return;
		Player bukkitPlayer = (Player)event.getAttacker();
		if (bukkitPlayer == null) return;
		KonPlayer player = konquest.getPlayerManager().getPlayer(bukkitPlayer);
		if(player == null) return;
		Location damageLoc = event.getVehicle().getLocation();
		//ChatUtil.printDebug("EVENT player vehicle damage of " + event.getVehicle().getType());
		if(!player.isAdminBypassActive() && territoryManager.isChunkClaimed(damageLoc)) {
			KonTerritory territory = territoryManager.getChunkTerritory(damageLoc);
			// Property Flag Holders
			if(territory instanceof KonPropertyFlagHolder) {
				KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
				if(flagHolder.hasPropertyValue(KonPropertyFlag.BUILD)) {
					if(!flagHolder.getPropertyValue(KonPropertyFlag.BUILD)) {
						ChatUtil.sendKonBlockedFlagTitle(player);
						event.setCancelled(true);
						return;
					}
				}
			}
			// Specific territory protections
			if(territory instanceof KonTown) {
				KonTown town = (KonTown) territory;
				// Prevent non-friendlies (including enemies) and friendly non-residents
				KonquestRelationshipType playerRole = kingdomManager.getRelationRole(player.getKingdom(), territory.getKingdom());
				if(!playerRole.equals(KonquestRelationshipType.FRIENDLY) || (!town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer()))) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_NOT_RESIDENT.getMessage(territory.getName()));
					event.setCancelled(true);
					return;
				}
			} else if(territory instanceof KonRuin) {
				// Always prevent
				ChatUtil.sendKonBlockedProtectionTitle(player);
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onEntityPlace(EntityPlaceEvent event) {
		// Player tries to place an entity on a block: armor stands, boats, minecarts, end crystals
		if(event.isCancelled()) return;
		if(konquest.isWorldIgnored(event.getBlock().getWorld())) return;
		Player bukkitPlayer = event.getPlayer();
		if(bukkitPlayer == null) return;
		if(!konquest.getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
			ChatUtil.printDebug("Failed to handle onEntityPlace for non-existent player");
			return;
		}
		KonPlayer player = playerManager.getPlayer(bukkitPlayer);
		Location placeLoc = event.getBlock().getLocation();
		//ChatUtil.printDebug("EVENT player entity placement of "+event.getEntityType());
		// Check for claim protections at place location
		if(!player.isAdminBypassActive() && territoryManager.isChunkClaimed(placeLoc)) {
			KonTerritory territory = territoryManager.getChunkTerritory(placeLoc);
			// Property Flag Holders
			if(territory instanceof KonPropertyFlagHolder) {
				KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
				if(flagHolder.hasPropertyValue(KonPropertyFlag.BUILD)) {
					if(!flagHolder.getPropertyValue(KonPropertyFlag.BUILD)) {
						ChatUtil.sendKonBlockedFlagTitle(player);
						event.setCancelled(true);
						return;
					}
				}
			}
			// Specific territory protections
			if(territory instanceof KonTown) {
				// Prevent non-friendlies (including enemies) and friendly non-residents
				KonTown town = (KonTown) territory;
				KonquestRelationshipType playerRole = kingdomManager.getRelationRole(player.getKingdom(), territory.getKingdom());
				if(!playerRole.equals(KonquestRelationshipType.FRIENDLY) || (!town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer()))) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_NOT_RESIDENT.getMessage(territory.getName()));
					event.setCancelled(true);
					return;
				}
			} else if(territory instanceof KonRuin) {
				// Always prevent
				ChatUtil.sendKonBlockedProtectionTitle(player);
				event.setCancelled(true);
				return;
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByPlayer(EntityDamageByEntityEvent event) {
		// Player damages an entity (non-player)
		if(event.isCancelled()) return;
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) return;
		Entity entityVictim = event.getEntity();
		EntityType eType = event.getEntity().getType();
		if(entityVictim instanceof Player) return;// Victim is a player, skip this event.
		Player bukkitPlayer;
		if (event.getDamager() instanceof AbstractArrow) {
			AbstractArrow arrow = (AbstractArrow) event.getDamager();
            if (!(arrow.getShooter() instanceof Player))return;
			bukkitPlayer = (Player) arrow.getShooter();
        } else if (event.getDamager() instanceof Player) {
        	bukkitPlayer = (Player) event.getDamager();
        } else { // if neither player nor arrow shot by player
            return;
        }
		if(!konquest.getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
			ChatUtil.printDebug("Failed to handle onEntityDamageByPlayer for non-existent player");
			return;
		}
        KonPlayer player = playerManager.getPlayer(bukkitPlayer);
    	Location damageLoc = event.getEntity().getLocation();
    	Location attackerLoc = bukkitPlayer.getLocation();
		// Check for claim protections at attacker location
    	if(!player.isAdminBypassActive() && territoryManager.isChunkClaimed(attackerLoc)) {
    		KonTerritory territory = territoryManager.getChunkTerritory(attackerLoc);
    		// Only update golems when player attacks from inside ruin territory
    		if(territory instanceof KonRuin) {
        		KonRuin ruin = (KonRuin) territory;
        		if(eType.equals(EntityType.IRON_GOLEM)) {
        			IronGolem golem = (IronGolem)event.getEntity();
        			// Check for golem death
        			if(golem.getHealth() - event.getFinalDamage() <= 0) {
        				konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.GOLEMS,1);
						// Execute custom commands from config
						if(ruin.isGolem(golem)) {
							konquest.executeCustomCommand(CustomCommandPath.RUIN_GOLEM_KILL,player.getBukkitPlayer());
						}
        			} else {
        				// Golem is still alive
        				ruin.targetGolemToPlayer(bukkitPlayer, golem);
        			}
        		}
        	}
    	}
		// Check for claim protections at damage location
		if(!player.isAdminBypassActive() && territoryManager.isChunkClaimed(damageLoc)) {
        	KonTerritory territory = territoryManager.getChunkTerritory(damageLoc);
        	
        	// Property Flag Holders
			if(territory instanceof KonPropertyFlagHolder) {
				KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
				if(flagHolder.hasPropertyValue(KonPropertyFlag.PVE)) {
					// Block non-enemy PVE
					if(!(flagHolder.getPropertyValue(KonPropertyFlag.PVE) || event.getEntity() instanceof Enemy)) {
						ChatUtil.sendKonBlockedFlagTitle(player);
						event.setCancelled(true);
						return;
					}
				}
			}

        	// Town protections
        	if(territory instanceof KonTown) {
    			KonTown town = (KonTown) territory;
				KonquestRelationshipType playerRole = kingdomManager.getRelationRole(player.getKingdom(), territory.getKingdom());
    			//ChatUtil.printDebug("EVENT player entity damage within town "+town.getName());
    			
    			// Preventions for non-friendlies and non-residents
    			if(!playerRole.equals(KonquestRelationshipType.FRIENDLY) || (!town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer()))) {
    				// Cannot damage item frames or armor stands
    				if(eType.equals(EntityType.ITEM_FRAME) || eType.equals(EntityType.GLOW_ITEM_FRAME) || eType.equals(EntityType.ARMOR_STAND)) {
						ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_NOT_RESIDENT.getMessage(town.getName()));
    					event.setCancelled(true);
						return;
    				}
    			}
    			// Preventions for non-residents only
    			if(playerRole.equals(KonquestRelationshipType.FRIENDLY) && !town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer())) {
    				// Cannot damage farm animals, ever!
    				if(event.getEntity() instanceof Animals || event.getEntity() instanceof Villager) {
    					ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_NOT_RESIDENT.getMessage(town.getName()));
    					event.setCancelled(true);
						return;
    				}
    			}
    			// Preventions for non-friendlies
    			if(!playerRole.equals(KonquestRelationshipType.FRIENDLY)) {
					// Protect peaceful towns
					if(town.getKingdom().isPeaceful()) {
						ChatUtil.sendNotice(bukkitPlayer, MessagePath.PROTECTION_NOTICE_PEACEFUL_TOWN.getMessage(town.getName()));
						event.setCancelled(true);
						return;
					}
    				// Check for farm animal or villager damage
    				if(event.getEntity() instanceof Animals || event.getEntity() instanceof Villager) {
    					// Cannot kill mobs within offline kingdom's town
	    				if(territory.getKingdom().isOfflineProtected()) {
	    					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_ONLINE.getMessage(town.getKingdom().getName(),town.getName()));
	    					event.setCancelled(true);
							return;
	    				}
	    				// If town is upgraded to require a minimum online resident amount, prevent block damage
						if(town.isTownWatchProtected()) {
							int upgradeLevelWatch = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.WATCH);
							ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_UPGRADE.getMessage(town.getName(), KonUpgrade.WATCH.getDescription(), upgradeLevelWatch));
							event.setCancelled(true);
							return;
						}
						// If the player is not enemy with the town, prevent event
						if(!playerRole.equals(KonquestRelationshipType.ENEMY)) {
							ChatUtil.sendKonBlockedProtectionTitle(player);
							event.setCancelled(true);
							return;
						}
						// Check for capital immunity
						if(territory.getTerritoryType().equals(KonquestTerritoryType.CAPITAL) && territory.getKingdom().isCapitalImmune()) {
							// Capital is immune and cannot be attacked
							int numTowns = territory.getKingdom().getNumTowns();
							ChatUtil.sendKonBlockedProtectionTitle(player);
							ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_CAPITAL_IMMUNE.getMessage(numTowns, territory.getKingdom().getName()));
							event.setCancelled(true);
							return;
						}
						// Verify town can be attacked
						if(town.isCaptureDisabled()) {
							ChatUtil.sendKonBlockedProtectionTitle(player);
							ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_CAPTURE.getMessage(town.getCaptureCooldownString()));
							event.setCancelled(true);
							return;
						}
						// If not enough players are online in the attacker's kingdom, prevent animal attacks
						boolean isNoProtectedAttack = konquest.getCore().getBoolean(CorePath.KINGDOMS_NO_PROTECTED_ATTACKING.getPath(),false);
						if (isNoProtectedAttack && player.getKingdom().isOfflineProtected()) {
							ChatUtil.sendKonBlockedProtectionTitle(player);
							ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_PROTECTED_ATTACK.getMessage(town.getName()));
							event.setCancelled(true);
							return;
						}
						// If town is shielded, prevent all enemy entity damage
						if(town.isShielded()) {
							ChatUtil.sendKonBlockedShieldTitle(player);
							event.setCancelled(true);
							return;
						}
						// If town is armored, prevent all enemy entity damage
						if(town.isArmored()) {
							Konquest.playTownArmorSound(player.getBukkitPlayer());
							event.setCancelled(true);
							return;
						}
    				}
    			}
    			// Prevent friendlies from hurting iron golems
    			boolean isFriendlyGolemAttack = konquest.getCore().getBoolean(CorePath.KINGDOMS_ATTACK_FRIENDLY_GOLEMS.getPath());
    			if(!isFriendlyGolemAttack && !playerRole.equals(KonquestRelationshipType.ENEMY) && eType.equals(EntityType.IRON_GOLEM)) {
    				ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_GOLEM.getMessage(town.getName()));
					event.setCancelled(true);
					return;
    			}
    			// Force iron golems to target enemies
    			if(playerRole.equals(KonquestRelationshipType.ENEMY) && eType.equals(EntityType.IRON_GOLEM)) {
    				IronGolem golem = (IronGolem)event.getEntity();
    				// Check if Iron Golem dies from damage
    				if(golem.getHealth() - event.getFinalDamage() > 0) {
    					// Iron Golem lives
	    				LivingEntity currentTarget = golem.getTarget();
	    				if(currentTarget instanceof Player) {
	    					if(!konquest.getPlayerManager().isOnlinePlayer((Player)currentTarget)) {
	    						ChatUtil.printDebug("Failed to handle onEntityDamageByPlayer golem targeting for non-existent player");
	    					} else {
		    					KonPlayer previousTargetPlayer = playerManager.getPlayer((Player)currentTarget);
		    					previousTargetPlayer.removeMobAttacker(golem);
	    					}
	    				}
	    				golem.setTarget(bukkitPlayer);
	    				player.addMobAttacker(golem);
    				} else {
    					// Iron Golem dies
    					konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.GOLEMS,1);
    				}
    			}
    		}
		}
		// Apply statistics for player damagers
		if(entityVictim instanceof Monster) {
			Monster monsterVictim = (Monster)entityVictim;
			if(monsterVictim.getHealth() <= event.getFinalDamage()) {
				konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.MOBS,1);
			}
		}
		
	}
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {
		if(event.isCancelled()) return;
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) return;
		Player victimBukkitPlayer;
        Player attackerBukkitPlayer = null;
        boolean isEggAttack = false;
        if (event.getEntity() instanceof Player) {
        	victimBukkitPlayer = (Player) event.getEntity();
            if (event.getDamager() instanceof AbstractArrow) {
            	AbstractArrow arrow = (AbstractArrow) event.getDamager();
                if (arrow.getShooter() instanceof Player) {
                	attackerBukkitPlayer = (Player) arrow.getShooter();
                } else {
                	return;
                }
            } else if (event.getDamager() instanceof Egg) {
            	Egg egg = (Egg)event.getDamager();
            	if (egg.getShooter() instanceof Player) {
                	attackerBukkitPlayer = (Player) egg.getShooter();
                	isEggAttack = true;
                } else {
                	return;
                }
            } else if (event.getDamager() instanceof Player) {
            	attackerBukkitPlayer = (Player) event.getDamager();
            } else { // if neither player nor arrow
                return;
            }
            
            if(!konquest.getPlayerManager().isOnlinePlayer(victimBukkitPlayer) || !konquest.getPlayerManager().isOnlinePlayer(attackerBukkitPlayer)) {
				ChatUtil.printDebug("Failed to handle onPlayerDamageByPlayer for non-existent player(s)");
				return;
			}
            KonPlayer victimPlayer = playerManager.getPlayer(victimBukkitPlayer);
            KonPlayer attackerPlayer = playerManager.getPlayer(attackerBukkitPlayer);
            if(victimPlayer == null || attackerPlayer == null) {
            	return;
            }

			// Update egg stat
			if(isEggAttack) {
				konquest.getAccomplishmentManager().modifyPlayerStat(attackerPlayer,KonStatsType.EGG,1);
			}

            // Check for property flags
            boolean isWildDamageEnabled = konquest.getCore().getBoolean(CorePath.KINGDOMS_WILD_PVP.getPath(), true);
			boolean isWorldValid = konquest.isWorldValid(victimBukkitPlayer.getLocation());
            boolean isAttackInTerritory = territoryManager.isChunkClaimed(victimBukkitPlayer.getLocation());
			boolean isVictimInsideFriendlyTerritory = false;
			boolean isPropertyPvpProtected = false; // True = no pvp is allowed at all, False = use normal pvp checks
			boolean isPropertyArenaEnabled = false; // True = ignore kingdom relations for pvp, False = use normal relations checks
			if(isAttackInTerritory) {
				KonTerritory territory = territoryManager.getChunkTerritory(victimBukkitPlayer.getLocation());
				assert territory != null;
				// Property Flag Holders
				if(territory instanceof KonPropertyFlagHolder) {
					KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
					if(flagHolder.hasPropertyValue(KonPropertyFlag.PVP)) {
						if(!flagHolder.getPropertyValue(KonPropertyFlag.PVP)) {
							isPropertyPvpProtected = true;
						}
					}
					if(flagHolder.hasPropertyValue(KonPropertyFlag.ARENA)) {
						if(flagHolder.getPropertyValue(KonPropertyFlag.ARENA)) {
							isPropertyArenaEnabled = true;
						}
					}
				}
				// Other territory checks
				isVictimInsideFriendlyTerritory = territory.getKingdom().equals(victimPlayer.getKingdom());
			} else if(!isWildDamageEnabled && isWorldValid) {
				// Always disable PVP in the wild when configured
				event.setCancelled(true);
				return;
			}

			// Check for WorldGuard flags
			boolean isFlagArenaAllowed = konquest.getIntegrationManager().getWorldGuard().isLocationArenaAllowed(victimBukkitPlayer.getLocation(),attackerBukkitPlayer);

			// Check for kingdom relationships
			boolean isAllDamageEnabled = konquest.getCore().getBoolean(CorePath.KINGDOMS_ALLOW_ALL_PVP.getPath(), false);
			boolean isPeaceDamageEnabled = konquest.getCore().getBoolean(CorePath.KINGDOMS_ALLOW_PEACEFUL_PVP.getPath(), false);
			boolean isPlayerEnemy = true;
			KonquestRelationshipType attackerRole = kingdomManager.getRelationRole(attackerPlayer.getKingdom(), victimPlayer.getKingdom());
			if (attackerRole.equals(KonquestRelationshipType.FRIENDLY) ||
					attackerRole.equals(KonquestRelationshipType.ALLY) ||
					attackerRole.equals(KonquestRelationshipType.TRADE) ||
					(attackerRole.equals(KonquestRelationshipType.PEACEFUL) && (!isPeaceDamageEnabled || isVictimInsideFriendlyTerritory))) {
				isPlayerEnemy = false;
			}
//			ChatUtil.printDebug("Player "+attackerBukkitPlayer.getName()+" attacked "+victimBukkitPlayer.getName()+
//					", role="+attackerRole+", enemy="+isPlayerEnemy+", arenaProperty="+isPropertyArenaEnabled+", arenaFlag="+isFlagArenaAllowed);

			// Protection checks when the damage is not inside a territory with ARENA property = true
			if(!isPropertyArenaEnabled && !isFlagArenaAllowed) {
				// Protect victim if they're peaceful
				if (victimPlayer.getKingdom().isPeaceful()) {
					ChatUtil.sendNotice(attackerBukkitPlayer, MessagePath.PROTECTION_NOTICE_PEACEFUL_VICTIM.getMessage());
					event.setCancelled(true);
					return;
				}
				// Protect victim when attacker is peaceful
				if (attackerPlayer.getKingdom().isPeaceful()) {
					ChatUtil.sendNotice(attackerBukkitPlayer, MessagePath.PROTECTION_NOTICE_PEACEFUL_ATTACKER.getMessage());
					event.setCancelled(true);
					return;
				}
				// Protect pvp when property is false
				if (isPropertyPvpProtected) {
					ChatUtil.sendKonBlockedFlagTitle(attackerPlayer);
					event.setCancelled(true);
					return;
				}
				// Prevent optional damage based on relations
				// Kingdoms at peace may allow pvp. Kingdoms in alliance or trade cannot pvp.
				if (!isAllDamageEnabled && !isPlayerEnemy) {
					ChatUtil.sendKonBlockedProtectionTitle(attackerPlayer);
					event.setCancelled(true);
					return;
				}
			}
			/* Finished with protection checks, the victim is damaged...*/

			// The victim may or may not be an enemy. Only do updates for enemy players.
            if (isPlayerEnemy) {
				// Check for death, update directive progress & stat
				if (victimBukkitPlayer.getHealth() - event.getFinalDamage() <= 0) {
					konquest.getDirectiveManager().updateDirectiveProgress(attackerPlayer, KonDirective.KILL_ENEMY);
					konquest.getAccomplishmentManager().modifyPlayerStat(attackerPlayer, KonStatsType.KILLS, 1);
				} else {
					// Player has not died, refresh combat tag timer
					int combatTagCooldownSeconds = konquest.getCore().getInt(CorePath.COMBAT_ENEMY_DAMAGE_COOLDOWN_SECONDS.getPath(), 0);
					boolean combatTagEnabled = konquest.getCore().getBoolean(CorePath.COMBAT_PREVENT_COMMAND_ON_DAMAGE.getPath(), false);
					if (combatTagEnabled && combatTagCooldownSeconds > 0) {
						// Fire event
						KonquestPlayerCombatTagEvent invokePreEvent = new KonquestPlayerCombatTagEvent(konquest, victimPlayer, attackerPlayer, victimBukkitPlayer.getLocation());
						Konquest.callKonquestEvent(invokePreEvent);
						// Check for cancelled
						if (!invokePreEvent.isCancelled()) {
							// Notify player when tag is new
							if (!victimPlayer.isCombatTagged()) {
								ChatUtil.sendKonPriorityTitle(victimPlayer, "", ChatColor.GOLD + MessagePath.PROTECTION_NOTICE_TAGGED.getMessage(), 20, 1, 10);
								ChatUtil.sendNotice(victimBukkitPlayer, MessagePath.PROTECTION_NOTICE_TAG_MESSAGE.getMessage());
							}
							victimPlayer.getCombatTagTimer().stopTimer();
							victimPlayer.getCombatTagTimer().setTime(combatTagCooldownSeconds);
							victimPlayer.getCombatTagTimer().startTimer();
							victimPlayer.setIsCombatTagged(true);
						}
					}
				}
				konquest.getAccomplishmentManager().modifyPlayerStat(attackerPlayer, KonStatsType.DAMAGE, (int) event.getFinalDamage());
			}
        }
    }

	/**
	 * Checks for entities protected by territory when explosion is outside of territory.
	 * For protections from explosions inside of territory, see onEntityExplode.
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void onEntityDamageByExplosion(EntityDamageByEntityEvent event) {
		if (event.isCancelled()) return;
		if (konquest.isWorldIgnored(event.getEntity().getWorld())) return;
		// Damager entity must be explosive (TNT) or Creeper
		if (!(event.getDamager() instanceof Explosive || event.getDamager() instanceof Creeper)) return;
		// Damager must be outside of territory
		if (territoryManager.isChunkClaimed(event.getDamager().getLocation())) return;
		// Try to protect entity if it's inside of territory
		Location entityLoc = event.getEntity().getLocation();
		if(territoryManager.isChunkClaimed(entityLoc)) {
			KonTerritory territory = territoryManager.getChunkTerritory(entityLoc);
			assert territory != null;
			if(isTerritoryExplosionProtected(territory,event.getDamager())) {
				ChatUtil.printDebug("  Protected entity "+event.getEntity().getName()+" from explosion damage in "+territory.getName());
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
		Player bukkitPlayer = event.getEntity();
		KonPlayer player = playerManager.getPlayer(bukkitPlayer);
		if(player != null) {
			player.clearAllMobAttackers();
			player.setIsCombatTagged(false);
			player.getCombatTagTimer().stopTimer();
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
		EntityType eType = event.getEntity().getType();
		if(eType.equals(EntityType.IRON_GOLEM) && event.getEntity() instanceof IronGolem) {
			// Attempt to match this dead golem to a ruin
			IronGolem golem = (IronGolem)event.getEntity();
			for(KonRuin ruin : konquest.getRuinManager().getRuins()) {
				ruin.onGolemDeath(golem);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) return;
		ProjectileSource source = event.getEntity().getShooter();
		if(event.getEntityType().equals(CompatibilityUtil.getEntityType("potion")) && source instanceof Player) {
			Player bukkitPlayer = (Player)source;
			KonPlayer player = playerManager.getPlayer(bukkitPlayer);
			if(player != null) {
				konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.POTIONS,1);
			}
    	}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPotionSplash(PotionSplashEvent event) {
		// Common handler
		onPotionThrown(event);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onLingeringPotionSplash(LingeringPotionSplashEvent event) {
		// Common handler
		onPotionThrown(event);
	}

	private void onPotionThrown(ProjectileHitEvent event) {
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) return;
		ProjectileSource source = event.getEntity().getShooter();
		// Check for splash potion thrown by player
		if(!(event.getEntityType().equals(CompatibilityUtil.getEntityType("potion")) && source instanceof Player)) return;
		Player bukkitPlayer = (Player)source;
		KonPlayer player = playerManager.getPlayer(bukkitPlayer);
		// Check for claimed territory
		if(player == null || player.isAdminBypassActive() || !territoryManager.isChunkClaimed(event.getEntity().getLocation())) return;
		KonTerritory territory = territoryManager.getChunkTerritory(event.getEntity().getLocation());
		// Check for protected territories
		if(territory instanceof KonTown) {
			// Only allow splash potions in the town when source player is friendly or enemy (war)
			KonquestRelationshipType playerRole = kingdomManager.getRelationRole(player.getKingdom(), territory.getKingdom());
			if(!playerRole.equals(KonquestRelationshipType.ENEMY) && !playerRole.equals(KonquestRelationshipType.FRIENDLY)) {
				ChatUtil.sendKonBlockedProtectionTitle(player);
				event.setCancelled(true);
				return;
			}
		} else if(territory instanceof KonSanctuary) {
			// Prevent all splash potions
			ChatUtil.sendKonBlockedProtectionTitle(player);
			event.setCancelled(true);
			return;
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
    public void onAnimalDeathItem(EntityDeathEvent event) {
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) return;
		// Check for ruin golem
		boolean cancelGolemDrops = konquest.getCore().getBoolean(CorePath.RUINS_NO_GOLEM_DROPS.getPath(), true);
		EntityType eType = event.getEntity().getType();
		if(event.getEntity() instanceof IronGolem && eType.equals(EntityType.IRON_GOLEM) && cancelGolemDrops) {
			IronGolem deadGolem = (IronGolem)event.getEntity();
			for(KonRuin ruin : konquest.getRuinManager().getRuins()) {
				if(ruin.isGolem(deadGolem)) {
					// cancel the drop
					ChatUtil.printDebug("Found dead ruin golem, blocking loot drops");
					for(ItemStack item : event.getDrops()) {
						item.setAmount(0);
					}
				}
			}
		}
		// Cause additional items to drop when event is located in town with upgrade
		if(event.getEntity() instanceof Animals && territoryManager.isChunkClaimed(event.getEntity().getLocation())) {
			KonTerritory territory = territoryManager.getChunkTerritory(event.getEntity().getLocation());
			if(territory instanceof KonTown) {
				KonTown town = (KonTown)territory;
				int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.DROPS);
				if(upgradeLevel >= 1) {
					// Add 1 to all drops
					for(ItemStack item : event.getDrops()) {
						int droppedAmount = item.getAmount();
						item.setAmount(droppedAmount+1);
					}
				}
			}
		}
	}

}
