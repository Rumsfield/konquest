package com.github.rumsfield.konquest.model;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Rabbit;

import com.github.rumsfield.konquest.utility.ChatUtil;

public class KonTownRabbit {

	private final Location spawnLoc;
	private Rabbit rabbit;
	
	public KonTownRabbit(Location spawnLoc) {
		this.spawnLoc = spawnLoc;
	}
	
	public void spawn() {
		if(rabbit == null || rabbit.isDead()) {
			Location modLoc = new Location(spawnLoc.getWorld(),spawnLoc.getX()+0.5,spawnLoc.getY()+1.0,spawnLoc.getZ()+0.5);
			rabbit = (Rabbit)spawnLoc.getWorld().spawnEntity(modLoc, EntityType.RABBIT);
			rabbit.setRabbitType(Rabbit.Type.THE_KILLER_BUNNY);
			double defaultValue;
			// Modify health
			defaultValue = rabbit.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue();
			rabbit.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(defaultValue*2);
			rabbit.setHealth(defaultValue*2);
			// Play spawn noise
			spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5F, 2.0F);
			ChatUtil.printDebug("Spawned town rabbit");
		} else {
			ChatUtil.printDebug("Failed to spawn town rabbit");
		}
	}
	
	public void respawn() {
		remove();
		spawn();
	}
	
	public void remove() {
		if(rabbit != null) {
			rabbit.remove();
		}
	}
	
	public void kill() {
		if(rabbit != null) {
			rabbit.damage(rabbit.getHealth());
		}
	}
	
	public Location getLocation() {
		if(rabbit != null && !rabbit.isDead()) {
			return rabbit.getLocation();
		} else {
			return spawnLoc;
		}
	}
	
	public void targetTo(LivingEntity target) {
		if(rabbit != null && !rabbit.isDead()) {
			rabbit.setTarget(null);
			rabbit.setTarget(target);
			// Play target noise
		}
	}
	
}
