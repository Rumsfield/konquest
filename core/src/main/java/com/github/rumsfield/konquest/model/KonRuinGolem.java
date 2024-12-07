package com.github.rumsfield.konquest.model;

import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;

import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;


import com.github.rumsfield.konquest.utility.Timer;


public class KonRuinGolem {

	private final Location spawnLoc;
	private IronGolem golem;
	private final Timer respawnTimer;
	private boolean isRespawnCooldown;
	private LivingEntity lastTarget;
	
	public KonRuinGolem(Location spawnLoc, KonRuin ruin) {
		this.spawnLoc = spawnLoc;
		this.golem = null;
		this.respawnTimer = new Timer(ruin);
		this.isRespawnCooldown = false;
		this.lastTarget = null;
	}

	public void spawn() {
		spawn(false);
	}

	public void spawn(boolean force) {
		if(!force && isRespawnCooldown)return;
		if(golem == null || golem.isDead()) {
			Location modLoc = new Location(spawnLoc.getWorld(),spawnLoc.getX()+0.5,spawnLoc.getY()+1.0,spawnLoc.getZ()+0.5);
			// Load chunk if not already done
			if(!modLoc.getChunk().isLoaded()) {
				modLoc.getChunk().load();
			}
			// Spawn golem entity
			golem = (IronGolem)spawnLoc.getWorld().spawnEntity(modLoc, EntityType.IRON_GOLEM);
			golem.setPlayerCreated(true);
			double baseValue;
			// Modify health
			// Iron Golem default = 20, base = 100
			Attribute maxHealth = CompatibilityUtil.getAttribute("health");
			if (maxHealth != null) {
				AttributeInstance golemHealth = golem.getAttribute(maxHealth);
				if (golemHealth != null) {
					baseValue = golemHealth.getBaseValue();
					golemHealth.setBaseValue(baseValue*4);
					golem.setHealth(baseValue*1.5);
				}
			}
			// Modify movement speed
			// Iron Golem default = 0.7, base = 0.25
			Attribute movementSpeed = CompatibilityUtil.getAttribute("speed");
			if (movementSpeed != null) {
				AttributeInstance golemSpeed = golem.getAttribute(movementSpeed);
				if (golemSpeed != null) {
					baseValue = golemSpeed.getBaseValue();
					golemSpeed.setBaseValue(baseValue*1.5);
				}
			}
			// Play spawn noise
			spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_IRON_GOLEM_REPAIR, 1.0F, 1.2F);
		}
	}
	
	public void respawn() {
		remove();
		spawn(true);
	}
	
	public void remove() {
		if(golem != null) {
			// Load the chunk of the entity to remove it
			if(!golem.getLocation().getChunk().isEntitiesLoaded()) {
				if(golem.getLocation().getChunk().load()) {
					// Chunk loaded successfully
					golem.remove();
					ChatUtil.printDebug("Successfully removed golem in newly loaded chunk");
				} else {
					ChatUtil.printDebug("Failed to load chunk for golem removal");
				}
			} else {
				// Entities are already loaded
				golem.remove();
				ChatUtil.printDebug("Successfully removed golem in previously loaded chunk");
			}
		}
	}
	
	public void kill() {
		if(golem != null) {
			golem.damage(golem.getHealth());
		}
	}
	
	public Location getLocation() {
		if(golem != null && !golem.isDead()) {
			return golem.getLocation();
		} else {
			return spawnLoc;
		}
	}
	
	public void targetTo(LivingEntity target) {
		if(golem != null && !golem.isDead()) {
			golem.setTarget(null);
			golem.setTarget(target);
			lastTarget = target;
			// Play target noise
			spawnLoc.getWorld().playSound(golem.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0F, 0.4F);
		}
	}
	
	public void targetToLast() {
		if(lastTarget == null) return;
		targetTo(lastTarget);
	}
	
	public LivingEntity getLastTarget() {
		return lastTarget;
	}
	
	public void setLastTarget(LivingEntity target) {
		lastTarget = target;
	}
	
	public boolean isTarget(LivingEntity target) {
		boolean result = false;
		if(golem != null && !golem.isDead()) {
			LivingEntity currentTarget = golem.getTarget();
			if(currentTarget != null) {
				result = currentTarget.equals(target);
			}
		}
		return result;
	}
	
	public void dropTargets() {
		if(golem != null && !golem.isDead()) {
			golem.setTarget(null);
			lastTarget = null;
		}
	}
	
	public boolean matches(IronGolem otherGolem) {
		boolean result = false;
		if(golem != null) {
			result = golem.equals(otherGolem);
		}
		return result;
	}
	
	public void setIsRespawnCooldown(boolean val) {
		isRespawnCooldown = val;
	}

	public void startRespawnTimer(int duration) {
		respawnTimer.stopTimer();
		respawnTimer.setTime(duration);
		respawnTimer.startTimer();
	}

	public int getRespawnTimerId() {
		return respawnTimer.getTaskID();
	}
	
}
