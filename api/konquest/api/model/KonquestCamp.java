package konquest.api.model;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

/**
 * A camp with a bed for a single barbarian player.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestCamp extends KonquestTerritory {

	/**
	 * Checks whether the owner is currently online.
	 * 
	 * @return True when the camp owner is online, else false
	 */
	public boolean isOwnerOnline();
	
	/**
	 * Gets the player who owns this camp.
	 * 
	 * @return The player owner
	 */
	public OfflinePlayer getOwner();
	
	/**
	 * Checks whether the given player is the owner of this camp.
	 * 
	 * @param player The player to check
	 * @return True when the player is the owner, else false
	 */
	public boolean isPlayerOwner(OfflinePlayer player);
	
	/**
	 * Gets the location of the bed in this camp.
	 * 
	 * @return The bed location
	 */
	public Location getBedLocation();
	
	/**
	 * Checks whether this camp is currently protected from attacks.
	 * Camps are protected typically due to the owner being offline.
	 * When a camp is protected, it cannot be edited or accessed.
	 * 
	 * @return True when the camp is protected, else false
	 */
	public boolean isProtected();
}
