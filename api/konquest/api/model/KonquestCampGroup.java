package konquest.api.model;

import java.util.Collection;

import org.bukkit.OfflinePlayer;
/**
 * A collection of adjacent camps.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestCampGroup {

	/**
	 * Checks whether this camp group contains the given camp instance.
	 * 
	 * @param camp The camp to check
	 * @return True when this camp group contains the camp, else false
	 */
	public boolean containsCamp(KonquestCamp camp);
	
	/**
	 * Gets all camps in this camp group.
	 * 
	 * @return All camps
	 */
	public Collection<? extends KonquestCamp> getCamps();
	
	/**
	 * Checks whether the given player has a camp that is a part of this camp group.
	 * The player must be a barbarian with a set up camp.
	 * Their camp must be placed adjacent to other camps within this camp group.
	 * Camp group members can optionally edit blocks and access chests in each other's camps.
	 * 
	 * @param player The player to check
	 * @return True when this player is a member of this camp group, else false
	 */
	public boolean isPlayerMember(OfflinePlayer player);
}
