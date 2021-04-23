package konquest.model;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Rabbit;

public class KonTownRabbit {

	private Location spawnLoc;
	private Rabbit rabbit;
	
	public KonTownRabbit(Location spawnLoc) {
		this.spawnLoc = spawnLoc;
	}
	
	public void spawn() {
		if(rabbit == null || (rabbit != null && rabbit.isDead())) {
			Location modLoc = new Location(spawnLoc.getWorld(),spawnLoc.getX()+0.5,spawnLoc.getY()+1.0,spawnLoc.getZ()+0.5);
			rabbit = (Rabbit)spawnLoc.getWorld().spawnEntity(modLoc, EntityType.RABBIT);
			rabbit.setRabbitType(Rabbit.Type.THE_KILLER_BUNNY);
			
			// Play spawn noise
			spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0F, 1.8F);
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
	
}
