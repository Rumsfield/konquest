package com.github.rumsfield.konquest.listener;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.api.event.player.KonquestPlayerCombatTagEvent;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.model.KonUpgrade;
import com.github.rumsfield.konquest.manager.KingdomManager;
import com.github.rumsfield.konquest.manager.PlayerManager;
import com.github.rumsfield.konquest.manager.TerritoryManager;
import com.github.rumsfield.konquest.manager.KingdomManager.RelationRole;
import com.github.rumsfield.konquest.model.KonDirective;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonPropertyFlag;
import com.github.rumsfield.konquest.model.KonPropertyFlagHolder;
import com.github.rumsfield.konquest.model.KonRuin;
import com.github.rumsfield.konquest.model.KonStatsType;
import com.github.rumsfield.konquest.model.KonTerritory;
import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

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
	
	@EventHandler()
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
	
	@EventHandler(priority = EventPriority.HIGH)
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
	 * Fires when entities explode.
	 * Protect territory from explosions, and optionally protect chests inside claimed territory.
	 */
	@EventHandler()
    public void onEntityExplode(EntityExplodeEvent event) {
		// Protect blocks inside of territory
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) {
			return;
		}
		for(Block block : event.blockList()) {
			if(territoryManager.isChunkClaimed(block.getLocation())) {
				KonTerritory territory = territoryManager.getChunkTerritory(block.getLocation());
				Material blockMat = block.getType();
				
				// Protect Sanctuaries always
				if(territory.getTerritoryType().equals(KonquestTerritoryType.SANCTUARY)) {
					ChatUtil.printDebug("protecting Sanctuary");
					event.setCancelled(true);
					return;
				}
				// Protect Ruins always
				if(territory.getTerritoryType().equals(KonquestTerritoryType.RUIN)) {
					ChatUtil.printDebug("protecting Ruin");
					event.setCancelled(true);
					return;
				}
				// Town protections
				if(territory instanceof KonTown) {
					KonTown town = (KonTown)territory;
					// Protect Town Monuments
					if(town.isLocInsideMonumentProtectionArea(block.getLocation())) {
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
					int upgradeLevelWatch = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.WATCH);
					if(upgradeLevelWatch > 0) {
						if(town.getNumResidentsOnline() < upgradeLevelWatch) {
							ChatUtil.printDebug("protecting upgraded Town WATCH");
							event.setCancelled(true);
							return;
						}
					}
					// Protect when town has upgrade
					int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.DAMAGE);
					if(upgradeLevel >= 2) {
						ChatUtil.printDebug("protecting upgraded Town DAMAGE");
						event.setCancelled(true);
						return;
					}
					// If town is shielded, prevent all explosions
					if(town.isShielded()) {
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
					ChatUtil.printDebug("protecting chest inside Town");
					event.setCancelled(true);
					return;
				}
				// Protect beds
				if(block.getBlockData() instanceof Bed) {
					ChatUtil.printDebug("protecting bed inside territory from explosion");
					event.setCancelled(true);
					return;
				}
			}
		}
	}
	
	@EventHandler()
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
							event.getSpawnReason().equals(SpawnReason.VILLAGE_INVASION)) {
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
	
	@EventHandler(priority = EventPriority.HIGH)
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
	
	@EventHandler()
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
				RelationRole playerRole = kingdomManager.getRelationRole(player.getKingdom(), territory.getKingdom());
				if(playerRole.equals(RelationRole.ENEMY) && kingdomManager.isTownNerf(event.getModifiedType())) {
					ChatUtil.printDebug("Cancelling milk bucket removal of town nerfs for player "+player.getBukkitPlayer().getName()+" in territory "+territory.getName());
					event.setCancelled(true);
				}
			}
    	}
	}

	@EventHandler(priority = EventPriority.LOW)
    public void onEntityTarget(EntityTargetEvent event) {
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) return;
		// Prevent Iron Golems from targeting friendly players
		Entity target = event.getTarget();
		Entity e = event.getEntity();
		EntityType eType = event.getEntity().getType();
		Location eLoc = event.getEntity().getLocation();
		if(eType.equals(EntityType.IRON_GOLEM) && e instanceof IronGolem) {
			IronGolem golemAttacker = (IronGolem)e;
			// Protect friendly players in claimed land
			if(target instanceof Player && territoryManager.isChunkClaimed(eLoc)) {
				Player bukkitPlayer = (Player)target;
				if(!konquest.getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
					ChatUtil.printDebug("Failed to handle onEntityTarget for non-existent player");
				} else {
					KonPlayer player = playerManager.getPlayer(bukkitPlayer);
					KonTerritory territory = territoryManager.getChunkTerritory(eLoc);
					if(territory.getKingdom().equals(player.getKingdom())) {
						ChatUtil.printDebug("Cancelling Iron Golem target of friendly player");
						golemAttacker.setPlayerCreated(true);
						golemAttacker.setTarget(null);
						event.setCancelled(true);
					}
				}
			}
			// Cancel Ruin Golems from targeting non-players
			if(!(target instanceof Player) && target instanceof LivingEntity) {
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
	}
	
	@EventHandler()
    public void onArrowInteract(EntityInteractEvent event) {
    	if(konquest.isWorldIgnored(event.getEntity().getLocation())) return;
    	// prevent arrows from interacting with things
		if (!(event.getEntity() instanceof Arrow)) return;
		if(!event.isCancelled() && territoryManager.isChunkClaimed(event.getBlock().getLocation())) {
	        KonTerritory territory = territoryManager.getChunkTerritory(event.getBlock().getLocation());
	        // Protect territory from arrow interaction
	        // Property Flag Holders
 			if(territory instanceof KonPropertyFlagHolder) {
 				KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
 				if(flagHolder.hasPropertyValue(KonPropertyFlag.USE)) {
 					if(!flagHolder.getPropertyValue(KonPropertyFlag.USE)) {
 						event.setCancelled(true);
					}
 				}
 			}
		}
	}
	
	@EventHandler()
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
	
	@EventHandler()
    public void onEntityDamageByPlayer(EntityDamageByEntityEvent event) {
		// Player damages an entity (non-player)
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) {
			return;
		}
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
					// Block non-monster PVE
					if(!(flagHolder.getPropertyValue(KonPropertyFlag.PVE) || event.getEntity() instanceof Monster)) {
						ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedFlagColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
						event.setCancelled(true);
						return;
					}
				}
			}

        	// Town protections
        	if(territory instanceof KonTown) {
    			KonTown town = (KonTown) territory;
    			RelationRole playerRole = kingdomManager.getRelationRole(player.getKingdom(), territory.getKingdom());
    			//ChatUtil.printDebug("EVENT player entity interaction within town "+town.getName());
    			
    			// Preventions for non-friendlies and non-residents
    			if(!playerRole.equals(RelationRole.FRIENDLY) || (!town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer()))) {
    				// Cannot damage item frames
    				if(eType.equals(EntityType.ITEM_FRAME)) {
    					event.setCancelled(true);
						return;
    				}
    			}
    			// Preventions for non-residents only
    			if(playerRole.equals(RelationRole.FRIENDLY) && !town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer())) {
    				// Cannot damage farm animals, ever!
    				if(event.getEntity() instanceof Animals || event.getEntity() instanceof Villager) {
    					ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_NOT_RESIDENT.getMessage(town.getName()));
    					event.setCancelled(true);
						return;
    				}
    			}
    			// Preventions for non-friendlies
    			if(!playerRole.equals(RelationRole.FRIENDLY)) {
    				// Check for farm animal or villager damage
    				if(event.getEntity() instanceof Animals || event.getEntity() instanceof Villager) {
    					// Cannot kill mobs within offline kingdom's town
	    				if(territory.getKingdom().isOfflineProtected()) {
	    					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_ONLINE.getMessage(town.getKingdom().getName(),town.getName()));
	    					event.setCancelled(true);
							return;
	    				}
	    				// If town is upgraded to require a minimum online resident amount, prevent block damage
						int upgradeLevelWatch = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.WATCH);
						if(upgradeLevelWatch > 0) {
							if(town.getNumResidentsOnline() < upgradeLevelWatch) {
								ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_UPGRADE.getMessage(town.getName(), KonUpgrade.WATCH.getDescription(), upgradeLevelWatch));
								event.setCancelled(true);
								return;
							}
						}
						// If the player is not enemy with the town, prevent event
						if(!playerRole.equals(RelationRole.ENEMY)) {
							ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedProtectionColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
							event.setCancelled(true);
							return;
						}
						// If town is shielded, prevent all enemy entity damage
						if(town.isShielded()) {
							ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedShieldColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
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
    			if(!isFriendlyGolemAttack && !playerRole.equals(RelationRole.ENEMY) && eType.equals(EntityType.IRON_GOLEM)) {
    				ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_GOLEM.getMessage(town.getName()));
					event.setCancelled(true);
					return;
    			}
    			// Force iron golems to target enemies
    			if(playerRole.equals(RelationRole.ENEMY) && eType.equals(EntityType.IRON_GOLEM)) {
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
	
	@EventHandler()
    public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) {
			return;
		}
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
            // Check for protections for attacks within claimed territory
            boolean isWildDamageEnabled = konquest.getCore().getBoolean(CorePath.KINGDOMS_WILD_PVP.getPath(), true);
            boolean isAttackInTerritory = territoryManager.isChunkClaimed(victimBukkitPlayer.getLocation());
			boolean isVictimInsideFriendlyTerritory = false;
            if(isAttackInTerritory) {
            	KonTerritory territory = territoryManager.getChunkTerritory(victimBukkitPlayer.getLocation());
				assert territory != null;
            	// Property Flag Holders
    			if(territory instanceof KonPropertyFlagHolder) {
    				KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
    				if(flagHolder.hasPropertyValue(KonPropertyFlag.PVP)) {
    					if(!flagHolder.getPropertyValue(KonPropertyFlag.PVP)) {
    						ChatUtil.sendKonPriorityTitle(attackerPlayer, "", Konquest.blockedFlagColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
    						event.setCancelled(true);
    						return;
    					}
    				}
    			}
            	// Prevent damage to victim inside peaceful territory if the victim is a member
				isVictimInsideFriendlyTerritory = territory.getKingdom().equals(victimPlayer.getKingdom());
            	if(territory.getKingdom().isPeaceful() && isVictimInsideFriendlyTerritory) {
            		event.setCancelled(true);
                	return;
            	}
            } else if(!isWildDamageEnabled) {
            	event.setCancelled(true);
            	return;
            }
            
            // Update egg stat
            if(isEggAttack) {
            	konquest.getAccomplishmentManager().modifyPlayerStat(attackerPlayer,KonStatsType.EGG,1);
            }
            
            // Prevent optional damage based on relations
			// Kingdoms at peace may allow pvp. Kingdoms in alliance or trade cannot pvp.
            boolean isPeaceDamageEnabled = konquest.getCore().getBoolean(CorePath.KINGDOMS_ALLOW_PEACEFUL_PVP.getPath(), false);
            RelationRole attackerRole = kingdomManager.getRelationRole(attackerPlayer.getKingdom(), victimPlayer.getKingdom());
            if(attackerRole.equals(RelationRole.FRIENDLY) ||
            		attackerRole.equals(RelationRole.ALLY) ||
					attackerRole.equals(RelationRole.TRADE) ||
            		(attackerRole.equals(RelationRole.PEACEFUL) && (!isPeaceDamageEnabled || isVictimInsideFriendlyTerritory))) {
            	ChatUtil.sendKonPriorityTitle(attackerPlayer, "", Konquest.blockedProtectionColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
				event.setCancelled(true);
				return;
            }
            
            // Check for death, update directive progress & stat
            if(victimBukkitPlayer.getHealth() - event.getFinalDamage() <= 0) {
            	konquest.getDirectiveManager().updateDirectiveProgress(attackerPlayer, KonDirective.KILL_ENEMY);
            	konquest.getAccomplishmentManager().modifyPlayerStat(attackerPlayer,KonStatsType.KILLS,1);
            } else {
            	// Player has not died, refresh combat tag timer
            	int combatTagCooldownSeconds = konquest.getCore().getInt(CorePath.COMBAT_ENEMY_DAMAGE_COOLDOWN_SECONDS.getPath(),0);
            	boolean combatTagEnabled = konquest.getCore().getBoolean(CorePath.COMBAT_PREVENT_COMMAND_ON_DAMAGE.getPath(),false);
            	if(combatTagEnabled && combatTagCooldownSeconds > 0) {
            		// Fire event
            		KonquestPlayerCombatTagEvent invokePreEvent = new KonquestPlayerCombatTagEvent(konquest, victimPlayer, attackerPlayer, victimBukkitPlayer.getLocation());
					Konquest.callKonquestEvent(invokePreEvent);
					// Check for cancelled
					if(!invokePreEvent.isCancelled()) {
						// Notify player when tag is new
	            		if(!victimPlayer.isCombatTagged()) {
	            			ChatUtil.sendKonPriorityTitle(victimPlayer, "", ChatColor.GOLD+MessagePath.PROTECTION_NOTICE_TAGGED.getMessage(), 20, 1, 10);
	                		ChatUtil.sendNotice(victimBukkitPlayer, MessagePath.PROTECTION_NOTICE_TAG_MESSAGE.getMessage());
	            		}
	            		victimPlayer.getCombatTagTimer().stopTimer();
	            		victimPlayer.getCombatTagTimer().setTime(combatTagCooldownSeconds);
	            		victimPlayer.getCombatTagTimer().startTimer();
	            		victimPlayer.setIsCombatTagged(true);
					}
            	}
            }
            konquest.getAccomplishmentManager().modifyPlayerStat(attackerPlayer,KonStatsType.DAMAGE,(int)event.getFinalDamage());
        }
    }
	
	@EventHandler()
    public void onPlayerDeath(PlayerDeathEvent event) {
		Player bukkitPlayer = event.getEntity();
		KonPlayer player = playerManager.getPlayer(bukkitPlayer);
		if(player != null) {
			player.clearAllMobAttackers();
			player.setIsCombatTagged(false);
			player.getCombatTagTimer().stopTimer();
		}
	}
	
	@EventHandler()
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
	
	@EventHandler()
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) return;
		ProjectileSource source = event.getEntity().getShooter();
		if(event.getEntityType().equals(EntityType.SPLASH_POTION) && source instanceof Player) {
			Player bukkitPlayer = (Player)source;
			KonPlayer player = playerManager.getPlayer(bukkitPlayer);
			if(player != null) {
				konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.POTIONS,1);
			}
    	}
	}
	
	@EventHandler()
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
