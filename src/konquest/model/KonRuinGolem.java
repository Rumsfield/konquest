package konquest.model;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
//import org.bukkit.craftbukkit.v1_16_R3.entity.CraftGolem;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;

//import konquest.utility.ChatUtil;
import konquest.utility.Timer;
//import net.minecraft.server.v1_16_R3.EntityGolem;

public class KonRuinGolem {

	private Location spawnLoc;
	private IronGolem golem;
	private Timer respawnTimer;
	private boolean isRespawnCooldown;
	private LivingEntity lastTarget;
	private KonRuin ruin;
	
	public KonRuinGolem(Location spawnLoc, KonRuin ruin) {
		this.spawnLoc = spawnLoc;
		this.ruin = ruin;
		this.golem = null;
		this.respawnTimer = new Timer(ruin);
		this.isRespawnCooldown = false;
		this.lastTarget = null;
	}
	
	public void spawn() {
		if(isRespawnCooldown) {
			//ChatUtil.printDebug("Could not spawn KonRuinGolem on respawn cooldown");
		} else {
			if(golem == null || (golem != null && golem.isDead())) {
				Location modLoc = new Location(spawnLoc.getWorld(),spawnLoc.getX()+0.5,spawnLoc.getY()+1.0,spawnLoc.getZ()+0.5);
				golem = (IronGolem)spawnLoc.getWorld().spawnEntity(modLoc, EntityType.IRON_GOLEM);
				golem.setPlayerCreated(true);
				double defaultValue = 0;
				// Modify health
				defaultValue = golem.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue();
				golem.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(defaultValue*4);
				golem.setHealth(defaultValue*1.5);
				// Modify movement speed
				defaultValue = golem.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getDefaultValue();
				golem.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(defaultValue*0.5);
				// Modify loot table optionally
				boolean cancelGolemDrops = ruin.getKonquest().getConfigManager().getConfig("core").getBoolean("core.ruins.no_golem_drops", true);
				if(cancelGolemDrops) {
					golem.setLootTable(null);
				}
				// Modify follow range
				//golem.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(10);
				// Play spawn noise
				spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_IRON_GOLEM_REPAIR, 1.0F, 1.2F);
			} else {
				//ChatUtil.printDebug("Failed to spawn KonRuinGolem");
			}
		}
	}
	
	public void respawn() {
		remove();
		spawn();
	}
	
	public void remove() {
		if(golem != null) {
			golem.remove();
		} else {
			//ChatUtil.printDebug("Failed to remove KonRuinGolem");
		}
	}
	
	public void kill() {
		if(golem != null) {
			golem.damage(golem.getHealth());
		} else {
			//ChatUtil.printDebug("Failed to kill KonRuinGolem");
		}
	}
	
	public Location getLocation() {
		if(golem != null && !golem.isDead()) {
			return golem.getLocation();
		} else {
			return spawnLoc;
		}
	}
	
	/*
	public void navigateTo(Location loc) {
		EntityGolem g = ((CraftGolem)golem).getHandle();
		g.getNavigation().a(loc.getX(),loc.getY(),loc.getZ(),0.8);
	}
	*/
	
	public void targetTo(LivingEntity target) {
		if(golem != null && !golem.isDead()) {
			golem.setTarget(null);
			golem.setTarget(target);
			lastTarget = target;
			// Play target noise
			spawnLoc.getWorld().playSound(golem.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0F, 0.4F);
		} else {
			//ChatUtil.printDebug("Failed to set target of KonRuinGolem");
		}
	}
	
	public void targetToLast() {
		if(lastTarget != null) {
			targetTo(lastTarget);
			//ChatUtil.printDebug("Ruin Golem is targeting last "+lastTarget.getType().toString());
		} else {
			//ChatUtil.printDebug("Failed to target last, got null");
		}
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
			//ChatUtil.printDebug("Dropping target for Ruin Golem");
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
	
	public Timer getRespawnTimer() {
		return respawnTimer;
	}
	
}
