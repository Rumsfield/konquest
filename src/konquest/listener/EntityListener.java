package konquest.listener;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.api.event.player.KonquestPlayerCombatTagEvent;
import konquest.api.model.KonquestTerritoryType;
import konquest.api.model.KonquestUpgrade;
import konquest.manager.KingdomManager;
import konquest.manager.PlayerManager;
import konquest.model.KonCapital;
import konquest.model.KonDirective;
import konquest.model.KonPlayer;
import konquest.model.KonRuin;
import konquest.model.KonStatsType;
import konquest.model.KonTerritory;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

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

	private KonquestPlugin konquestPlugin;
	private Konquest konquest;
	private KingdomManager kingdomManager;
	private PlayerManager playerManager;
	
	public EntityListener(KonquestPlugin plugin) {
		this.konquestPlugin = plugin;
		this.konquest = konquestPlugin.getKonquestInstance();
		this.playerManager = konquest.getPlayerManager();
		this.kingdomManager = konquest.getKingdomManager();
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onItemSpawn(ItemSpawnEvent event) {
		//ChatUtil.printDebug("EVENT: Item spawned");
		Location dropLoc = event.getEntity().getLocation();
		if(kingdomManager.isChunkClaimed(dropLoc)) {
			KonTerritory territory = kingdomManager.getChunkTerritory(dropLoc);
			// Check for break inside of town
			if(territory instanceof KonTown) {
				KonTown town = (KonTown) territory;
				// Check for break inside of town's monument
				if(town.getMonument().isItemDropsDisabled() && town.getMonument().isLocInside(dropLoc)) {
					//ChatUtil.printDebug("Item drops are currently disabled in "+town.getName()+" monument, removing");
					event.getEntity().remove();
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onEntityBreed(EntityBreedEvent event) {
		if(!event.isCancelled()) {
			if(konquest.isWorldIgnored(event.getEntity().getWorld())) {
				return;
			}
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
	}
	
	/*
	@EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDropItem(EntityDropItemEvent event) {
		ChatUtil.printDebug("EVENT: Entity dropped item "+event.getItemDrop().getName()+" by entity "+event.getEntityType().toString()+" "+event.getEntity().getName());
	}*/
	
	/**
	 * Fires when entities explode.
	 * Protect capitals from explosions, and optionally protect chests inside claimed territory.
	 * @param event
	 */
	@EventHandler(priority = EventPriority.NORMAL)
    public void onEntityExplode(EntityExplodeEvent event) {
		// Protect blocks inside of territory
		//ChatUtil.printDebug("EVENT: entityExplode");
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) {
			return;
		}
		//boolean isBreakDisabledOffline = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.no_enemy_edit_offline");
		for(Block block : event.blockList()) {
			if(kingdomManager.isChunkClaimed(block.getLocation())) {
				//ChatUtil.printDebug("EVENT: effected block is inside claimed territory");
				KonTerritory territory = kingdomManager.getChunkTerritory(block.getLocation());
				Material blockMat = block.getType();
				
				// Protect Capitals always
				if(territory.getTerritoryType().equals(KonquestTerritoryType.CAPITAL)) {
					ChatUtil.printDebug("protecting Capital");
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
				if(territory.getTerritoryType().equals(KonquestTerritoryType.TOWN)) {
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
					int upgradeLevelWatch = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonquestUpgrade.WATCH);
					if(upgradeLevelWatch > 0) {
						int minimumOnlineResidents = upgradeLevelWatch; // 1, 2, 3
						if(town.getNumResidentsOnline() < minimumOnlineResidents) {
							ChatUtil.printDebug("protecting upgraded Town WATCH");
							event.setCancelled(true);
							return;
						}
					}
					// Protect when town has upgrade
					int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonquestUpgrade.DAMAGE);
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
						int damage = konquest.getConfigManager().getConfig("core").getInt("core.towns.armor_tnt_damage",1);
						town.damageArmor(damage);
						Konquest.playTownArmorSound(event.getLocation());
						event.setCancelled(true);
						return;
					}
				}
				// Protect chests
				boolean isProtectChest = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.protect_containers_break");
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
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
		// Inside of claimed territory...
		if(konquest.getKingdomManager().isChunkClaimed(event.getLocation())) {
			KonTerritory territory = konquest.getKingdomManager().getChunkTerritory(event.getLocation());
			//ChatUtil.printDebug("EVENT: Creature spawned in territory "+territory.getTerritoryType().toString()+", cause: "+event.getSpawnReason().toString());
			// When a spawn event happens in Capital or ruin
			if(territory instanceof KonCapital || territory instanceof KonRuin) {
				// Conditions to block spawning...
				EntityType eType = event.getEntityType();
				SpawnReason eReason = event.getSpawnReason();
				boolean stopOnType = !(eType.equals(EntityType.ARMOR_STAND) || eType.equals(EntityType.IRON_GOLEM));
				boolean stopOnReason = !(eReason.equals(SpawnReason.COMMAND) || eReason.equals(SpawnReason.CUSTOM) || eReason.equals(SpawnReason.DEFAULT) || eReason.equals(SpawnReason.SPAWNER));
				if(stopOnType && stopOnReason) {
					boolean isAllMobSpawnAllowed = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.capital_mobs",false);
					if((territory.getTerritoryType().equals(KonquestTerritoryType.CAPITAL) && !isAllMobSpawnAllowed) || territory.getTerritoryType().equals(KonquestTerritoryType.RUIN))
					event.setCancelled(true);
				}
			}
			// When spawn event happens in town
			if(territory instanceof KonTown) {
				//ChatUtil.printDebug("EVENT: Creature spawned in a Town, cause: "+event.getSpawnReason().toString());
				// Conditions to block spawning...
				if(event.getSpawnReason().equals(SpawnReason.DROWNED) || 
						event.getSpawnReason().equals(SpawnReason.JOCKEY) ||
						event.getSpawnReason().equals(SpawnReason.LIGHTNING) ||
						event.getSpawnReason().equals(SpawnReason.MOUNT) ||
						event.getSpawnReason().equals(SpawnReason.NATURAL) ||
						event.getSpawnReason().equals(SpawnReason.PATROL) ||
						event.getSpawnReason().equals(SpawnReason.RAID) ||
						event.getSpawnReason().equals(SpawnReason.VILLAGE_INVASION)) {
					boolean isAllMobSpawnAllowed = konquest.getConfigManager().getConfig("core").getBoolean("core.towns.town_mobs",false);
					if(!isAllMobSpawnAllowed) {
						event.setCancelled(true);
					}
				}
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
		}
    }
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onEntityPotionEffect(EntityPotionEffectEvent event) {
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) {
			return;
		}
		// prevent milk buckets from removing town nerfs in enemy towns
		if(!event.isCancelled() && event.getEntity() instanceof Player) {
			if(!konquest.getPlayerManager().isOnlinePlayer((Player)event.getEntity())) {
				ChatUtil.printDebug("Failed to handle onEntityPotionEffect for non-existent player");
				return;
			}
			Location eventLoc = event.getEntity().getLocation();
			if(event.getCause().equals(EntityPotionEffectEvent.Cause.MILK) && kingdomManager.isChunkClaimed(eventLoc)) {
				KonTerritory territory = kingdomManager.getChunkTerritory(eventLoc);
				KonPlayer player = konquest.getPlayerManager().getPlayer((Player)event.getEntity());
				if(!player.getKingdom().equals(territory.getKingdom()) && kingdomManager.isTownNerf(event.getModifiedType())) {
					ChatUtil.printDebug("Cancelling milk bucket removal of town nerfs for player "+player.getBukkitPlayer().getName()+" in territory "+territory.getName());
					event.setCancelled(true);
				}
			}
    	}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onEntityInteract(EntityInteractEvent event) {
		// prevent items from interacting with pressure plates
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) {
			return;
		}
		//String entity = event.getEntity().getType().toString();
		//String block = event.getBlock().getType().toString();
		//ChatUtil.printDebug("Entity "+entity+" interacted with block "+block);
		if(event.getEntityType().equals(EntityType.DROPPED_ITEM)) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.LOW)
    public void onEntityTarget(EntityTargetEvent event) {
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) {
			return;
		}
		// Prevent Iron Golems from targeting friendly players
		Entity target = event.getTarget();
		Entity e = event.getEntity();
		EntityType eType = event.getEntity().getType();
		Location eLoc = event.getEntity().getLocation();
		if(eType.equals(EntityType.IRON_GOLEM) && e instanceof IronGolem) {
			IronGolem golemAttacker = (IronGolem)e;
			// Protect friendly players in claimed land
			if(target instanceof Player && kingdomManager.isChunkClaimed(eLoc)) {
				Player bukkitPlayer = (Player)target;
				if(!konquest.getPlayerManager().isOnlinePlayer((Player)bukkitPlayer)) {
					ChatUtil.printDebug("Failed to handle onEntityTarget for non-existent player");
				} else {
					KonPlayer player = playerManager.getPlayer(bukkitPlayer);
					KonTerritory territory = kingdomManager.getChunkTerritory(eLoc);
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
						//ChatUtil.printDebug("Cancelling Ruin Golem target of non-player "+target.getType().toString()+", "+event.getReason().toString());
						//((LivingEntity)target).damage(((LivingEntity)target).getHealth());
						target.getWorld().spawnParticle(Particle.SOUL,target.getLocation(),10);
						target.remove();
						eLoc.getWorld().playSound(eLoc, Sound.ENTITY_GHAST_DEATH, 1.0F, 0.1F);
						//event.setTarget(ruin.getGolemLastTarget(golemAttacker));
						event.setCancelled(true);
						// Scheduled delayed task to change target back to last player if present
						Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
	        	            @Override
	        	            public void run() {
	        	            	ruin.targetGolemToLast(golemAttacker);
	        	            }
	        	        },1);
					}
				}
			}
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageEvent event) {
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) {
			return;
		}
		Location damageLoc = event.getEntity().getLocation();
		if(kingdomManager.isChunkClaimed(damageLoc)) {
			KonTerritory territory = kingdomManager.getChunkTerritory(damageLoc);
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
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamageByPlayer(EntityDamageByEntityEvent event) {
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) {
			return;
		}
		Entity entityVictim = event.getEntity();
		EntityType eType = event.getEntity().getType();
		//EntityType dType = event.getDamager().getType();
		//ChatUtil.printDebug("EVENT: Entity "+eType.toString()+" damaged by entity "+dType.toString());
		Player bukkitPlayer = null;
		if (event.getDamager() instanceof AbstractArrow) {
			AbstractArrow arrow = (AbstractArrow) event.getDamager();
            //ChatUtil.printDebug("...Attacker was an Arrow");
            if (arrow.getShooter() instanceof Player) {
            	bukkitPlayer = (Player) arrow.getShooter();
            	//ChatUtil.printDebug("...Arrow shooter was a Player");
            } else {
            	return;
            }
        } else if (event.getDamager() instanceof Player) {
        	bukkitPlayer = (Player) event.getDamager();
        	//ChatUtil.printDebug("...Attacker was a Player");
        } else { // if neither player nor arrow
            return;
        }
		if(!konquest.getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
			ChatUtil.printDebug("Failed to handle onEntityDamageByPlayer for non-existent player");
			return;
		}
        KonPlayer player = playerManager.getPlayer(bukkitPlayer);
		
		//if(event.getDamager() instanceof Player) {
			//Player bukkitPlayer = (Player)event.getDamager();
			//KonPlayer player = playerManager.getPlayer(bukkitPlayer);
			
        	Location damageLoc = event.getEntity().getLocation();
        	Location attackerLoc = bukkitPlayer.getLocation();
			//boolean isBreakDisabledOffline = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.no_enemy_edit_offline");
        	// Check for claim protections at attacker location
        	if(!player.isAdminBypassActive() && kingdomManager.isChunkClaimed(attackerLoc)) {
        		KonTerritory territory = kingdomManager.getChunkTerritory(attackerLoc);
        		// Only update golems when player attacks from inside ruin territory
        		if(territory instanceof KonRuin) {
	        		KonRuin ruin = (KonRuin) territory;
	        		if(eType.equals(EntityType.IRON_GOLEM)) {
	        			IronGolem golem = (IronGolem)event.getEntity();
	        			// Check for golem death
	        			if(golem.getHealth() - event.getFinalDamage() <= 0) {
	        				konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.GOLEMS,1);
	        				//ruin.onGolemDeath(golem); // moved to onEntityDeathEvent
	        			} else {
	        				// Golem is still alive
	        				ruin.targetGolemToPlayer(bukkitPlayer, golem);
	        			}
	        		}
	        	}
        	}
			// Check for claim protections at damage location
			if(!player.isAdminBypassActive() && kingdomManager.isChunkClaimed(damageLoc)) {
	        	KonTerritory territory = kingdomManager.getChunkTerritory(damageLoc);
	        	
	        	if(territory instanceof KonCapital) {
	        		boolean isCapitalUseEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.capital_use",false);
	        		// Block all non-monster entity damage in capitals optionally
	        		if(!(isCapitalUseEnabled || event.getEntity() instanceof Monster)) {
		        		ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
		        		event.setCancelled(true);
						return;
	        		}
	        	}
	        	
	        	if(territory instanceof KonTown) {
	    			KonTown town = (KonTown) territory;
	    			//ChatUtil.printDebug("EVENT player entity interaction within town "+town.getName());
	    			
	    			// Preventions for enemies and non-residents
	    			if(!player.getKingdom().equals(town.getKingdom()) || (!town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer()))) {
	    				// Cannot damage item frames
	    				//ChatUtil.printDebug("Player identified as enemy or non-resident, clicked entity "+eType.toString());
	    				if(eType.equals(EntityType.ITEM_FRAME)) {
	    					event.setCancelled(true);
							return;
	    				}
	    			}
	    			// Preventions for non-residents only
	    			if(player.getKingdom().equals(town.getKingdom()) && !town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer())) {
	    				// Cannot damage farm animals, ever!
	    				if(event.getEntity() instanceof Animals || event.getEntity() instanceof Villager) {
	    					//ChatUtil.sendNotice(bukkitPlayer, "Must be a resident of "+town.getName()+" to do this!", ChatColor.DARK_RED);
	    					ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_NOT_RESIDENT.getMessage(town.getName()));
	    					event.setCancelled(true);
							return;
	    				}
	    			}
	    			// Preventions for enemies only
	    			if(!player.getKingdom().equals(town.getKingdom())) {
	    				
	    				if(event.getEntity() instanceof Animals || event.getEntity() instanceof Villager) {
	    					// Cannot kill mobs within offline kingdom's town
		    				if(territory.getKingdom().isOfflineProtected()) {
		    					//ChatUtil.sendNotice(bukkitPlayer, "There are not enough "+town.getKingdom().getName()+" players online, cannot attack the Town "+town.getName(), ChatColor.DARK_RED);
		    					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_ONLINE.getMessage(town.getKingdom().getName(),town.getName()));
		    					event.setCancelled(true);
								return;
		    				}
		    				// If town is upgraded to require a minimum online resident amount, prevent block damage
							int upgradeLevelWatch = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonquestUpgrade.WATCH);
							if(upgradeLevelWatch > 0) {
								int minimumOnlineResidents = upgradeLevelWatch; // 1, 2, 3
								if(town.getNumResidentsOnline() < minimumOnlineResidents) {
									//ChatUtil.sendNotice(bukkitPlayer, town.getName()+" is upgraded with "+KonUpgrade.WATCH.getDescription()+" and cannot be attacked without "+minimumOnlineResidents+" residents online", ChatColor.DARK_RED);
									ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_UPGRADE.getMessage(town.getName(),KonquestUpgrade.WATCH.getDescription(),minimumOnlineResidents));
									event.setCancelled(true);
									return;
								}
							}
							// If the town and enemy guilds share an armistice, prevent event
							if(konquest.getGuildManager().isArmistice(player, town)) {
								ChatUtil.sendKonPriorityTitle(player, "", ChatColor.LIGHT_PURPLE+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
								event.setCancelled(true);
								return;
							}
							// If town is shielded, prevent all enemy entity damage
							if(town.isShielded()) {
								ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_AQUA+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
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
	    			boolean isFriendlyGolemAttack = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.attack_friendly_golems");
	    			if(!isFriendlyGolemAttack && player.getKingdom().equals(town.getKingdom()) && eType.equals(EntityType.IRON_GOLEM)) {
	    				//ChatUtil.sendNotice(bukkitPlayer, "You cannot hurt friendly Iron Golems in Town "+town.getName(), ChatColor.DARK_RED);
	    				ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_GOLEM.getMessage(town.getName()));
    					event.setCancelled(true);
						return;
	    			}
	    			// Force iron golems to target enemies
	    			if(!player.getKingdom().equals(town.getKingdom()) && eType.equals(EntityType.IRON_GOLEM)) {
	    				IronGolem golem = (IronGolem)event.getEntity();
	    				// Check if Iron Golem dies from damage
	    				if(golem.getHealth() - event.getFinalDamage() > 0) {
	    					// Iron Golem lives
		    				LivingEntity currentTarget = golem.getTarget();
		    				//ChatUtil.printDebug("Golem: Evaluating new targets in territory "+territory.getName());
		    				if(currentTarget != null && currentTarget instanceof Player) {
		    					if(!konquest.getPlayerManager().isOnlinePlayer((Player)currentTarget)) {
		    						ChatUtil.printDebug("Failed to handle onEntityDamageByPlayer golem targeting for non-existent player");
		    					} else {
			    					KonPlayer previousTargetPlayer = playerManager.getPlayer((Player)currentTarget);
			    					previousTargetPlayer.removeMobAttacker(golem);
		    					}
		    					//ChatUtil.printDebug("Golem: Removed mob attacker from player "+previousTargetPlayer.getBukkitPlayer().getName());
		    				} else {
		    					//ChatUtil.printDebug("Golem: Bad current target");
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
		//}
		
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) {
			return;
		}
		Player victimBukkitPlayer;
        Player attackerBukkitPlayer = null;
        boolean isEggAttack = false;
        if (event.getEntity() instanceof Player) {
        	victimBukkitPlayer = (Player) event.getEntity();
        	//ChatUtil.printDebug("Player was attacked by an entity");
            if (event.getDamager() instanceof AbstractArrow) {
            	AbstractArrow arrow = (AbstractArrow) event.getDamager();
                //ChatUtil.printDebug("...Attacker was an Arrow");
                if (arrow.getShooter() instanceof Player) {
                	attackerBukkitPlayer = (Player) arrow.getShooter();
                	//ChatUtil.printDebug("...Arrow shooter was a Player");
                } else {
                	return;
                }
            } else if (event.getDamager() instanceof Egg) {
            	//ChatUtil.printDebug("...Attacker was an Egg");
            	Egg egg = (Egg)event.getDamager();
            	if (egg.getShooter() instanceof Player) {
                	attackerBukkitPlayer = (Player) egg.getShooter();
                	isEggAttack = true;
                	//ChatUtil.printDebug("...Egg thrower was a Player");
                } else {
                	return;
                }
            } else if (event.getDamager() instanceof Player) {
            	attackerBukkitPlayer = (Player) event.getDamager();
            	//ChatUtil.printDebug("...Attacker was a Player");
            } else { // if neither player nor arrow
                return;
            }
            
            if(!konquest.getPlayerManager().isOnlinePlayer(victimBukkitPlayer) || !konquest.getPlayerManager().isOnlinePlayer(attackerBukkitPlayer)) {
				ChatUtil.printDebug("Failed to handle onPlayerDamageByPlayer for non-existent player(s)");
				return;
			}
            KonPlayer victimPlayer = playerManager.getPlayer(victimBukkitPlayer);
            KonPlayer attackerPlayer = playerManager.getPlayer(attackerBukkitPlayer);
            
            // Check for protections for attacks within claimed territory
            boolean isCapitalDamageEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.capital_pvp", false);
            boolean isWildDamageEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.wild_pvp", true);
            boolean isAttackInTerritory = konquest.getKingdomManager().isChunkClaimed(victimBukkitPlayer.getLocation());
            if(isAttackInTerritory) {
            	KonTerritory territory = konquest.getKingdomManager().getChunkTerritory(victimBukkitPlayer.getLocation());
            	// Optionally prevent damage in Capitals
            	if(!isCapitalDamageEnabled && territory instanceof KonCapital) {
            		event.setCancelled(true);
                	return;
            	}
            	// Prevent damage of victim inside of peaceful territory if the victim is a member
            	if(territory.getKingdom().isPeaceful() && territory.getKingdom().equals(victimPlayer.getKingdom())) {
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
            
            // Prevent damage between Kingdom members who are not barbarians
            if(victimPlayer != null && attackerPlayer != null && victimPlayer.getKingdom().equals(attackerPlayer.getKingdom()) && !victimPlayer.isBarbarian()) {
            	event.setCancelled(true);
            	return;
            }
            
            // Prevent damage between guild members with shared armistice
			if(konquest.getGuildManager().isArmistice(attackerPlayer, victimPlayer)) {
				ChatUtil.sendKonPriorityTitle(attackerPlayer, "", ChatColor.LIGHT_PURPLE+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
				event.setCancelled(true);
				return;
			}
            
            // Check for death, update directive progress & stat
            //ChatUtil.printDebug("Player Damage: "+attackerBukkitPlayer.getName()+" attacked "+victimBukkitPlayer.getName()+", current health: "+victimBukkitPlayer.getHealth()+", damage dealt: "+event.getFinalDamage());
            if(victimBukkitPlayer.getHealth() - event.getFinalDamage() <= 0) {
            	konquest.getDirectiveManager().updateDirectiveProgress(attackerPlayer, KonDirective.KILL_ENEMY);
            	konquest.getAccomplishmentManager().modifyPlayerStat(attackerPlayer,KonStatsType.KILLS,1);
            } else {
            	// Player has not died, refresh combat tag timer
            	int combatTagCooldownSeconds = konquest.getConfigManager().getConfig("core").getInt("core.combat.enemy_damage_cooldown_seconds",0);
            	boolean combatTagEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.combat.prevent_command_on_damage",false);
            	if(combatTagEnabled && combatTagCooldownSeconds > 0) {
            		// Fire event
            		KonquestPlayerCombatTagEvent invokePreEvent = new KonquestPlayerCombatTagEvent(konquest, victimPlayer, attackerPlayer, victimBukkitPlayer.getLocation());
					Konquest.callKonquestEvent(invokePreEvent);
					// Check for cancelled
					if(!invokePreEvent.isCancelled()) {
						// Notify player when tag is new
	            		if(!victimPlayer.isCombatTagged()) {
	            			ChatUtil.sendKonPriorityTitle(victimPlayer, "", ChatColor.GOLD+MessagePath.PROTECTION_NOTICE_TAGGED.getMessage(), 20, 1, 10);
	                		//ChatUtil.sendNotice(victimBukkitPlayer, "You have been tagged in combat");
	                		ChatUtil.sendNotice(victimBukkitPlayer, MessagePath.PROTECTION_NOTICE_TAG_MESSAGE.getMessage());
	            		}
	            		victimPlayer.getCombatTagTimer().stopTimer();
	            		victimPlayer.getCombatTagTimer().setTime(combatTagCooldownSeconds);
	            		victimPlayer.getCombatTagTimer().startTimer();
	            		victimPlayer.setIsCombatTagged(true);
	            		//ChatUtil.printDebug("Refreshed combat tag timer for player "+victimBukkitPlayer.getName());
					}
            	}
            }
            konquest.getAccomplishmentManager().modifyPlayerStat(attackerPlayer,KonStatsType.DAMAGE,(int)event.getFinalDamage());
        }
    }
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
		Player bukkitPlayer = event.getEntity();
		KonPlayer player = playerManager.getPlayer(bukkitPlayer);
		if(player != null) {
			player.clearAllMobAttackers();
			player.setIsCombatTagged(false);
			player.getCombatTagTimer().stopTimer();
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
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
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) {
			return;
		}
		//ChatUtil.printDebug("EVENT: Player interacted with item "+event.getItem().getType().toString());
		ProjectileSource source = event.getEntity().getShooter();
		if(event.getEntityType().equals(EntityType.SPLASH_POTION) && source instanceof Player) {
			Player bukkitPlayer = (Player)source;
			KonPlayer player = playerManager.getPlayer(bukkitPlayer);
			if(player != null) {
				konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.POTIONS,1);
			}
    	}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
    public void onAnimalDeathItem(EntityDeathEvent event) {
		if(konquest.isWorldIgnored(event.getEntity().getWorld())) {
			return;
		}
		// Check for ruin golem
		boolean cancelGolemDrops = konquest.getConfigManager().getConfig("core").getBoolean("core.ruins.no_golem_drops", true);
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
		if(event.getEntity() instanceof Animals && kingdomManager.isChunkClaimed(event.getEntity().getLocation())) {
			KonTerritory territory = kingdomManager.getChunkTerritory(event.getEntity().getLocation());
			if(territory instanceof KonTown) {
				KonTown town = (KonTown)territory;
				int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonquestUpgrade.DROPS);
				if(upgradeLevel >= 1) {
					// Add 1 to all drops
					for(ItemStack item : event.getDrops()) {
						int droppedAmount = item.getAmount();
						//ChatUtil.printDebug("EVENT: Dropping additional item "+item.getType().toString()+", was "+droppedAmount+", added 1.");
						item.setAmount(droppedAmount+1);
					}
				}
			}
		}
	}
	
	/*
	@EventHandler(priority = EventPriority.NORMAL)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
		if(event.getEntity() instanceof Player && kingdomManager.isChunkClaimed(event.getEntity().getLocation().getChunk())) {
			KonTerritory territory = kingdomManager.getChunkTerritory(event.getEntity().getLocation().getChunk());
			Player bukkitPlayer = (Player)event.getEntity();
			KonPlayer player = playerManager.getPlayer(bukkitPlayer);
			if(player.getKingdom().equals(territory.getKingdom()) && territory instanceof KonTown) {
				KonTown town = (KonTown)territory;
				int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.HUNGER);
				if(upgradeLevel >= 1) {
					//ChatUtil.printDebug("EVENT: Cancelling hunger event for friendly player in town "+town.getName());
					bukkitPlayer.setFoodLevel(20);
					event.setCancelled(true);
					return;
				}
			}
		}
	}
	*/
}
