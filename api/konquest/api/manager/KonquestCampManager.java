package konquest.api.manager;

import java.util.ArrayList;

import konquest.api.model.KonquestCamp;
import konquest.api.model.KonquestCampGroup;
import konquest.api.model.KonquestOfflinePlayer;

/**
 * A manager for barbarian camps in Konquest.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestCampManager {

	/**
	 * Checks if the given player has a camp set up, and is a barbarian.
	 * 
	 * @param player The player to check
	 * @return True if the player has a camp, else false
	 */
	public boolean isCampSet(KonquestOfflinePlayer player);
	
	/**
	 * Gets the camp that belongs to a barbarian player. Returns null when the camp does not exist.
	 * 
	 * @param player The camp owner
	 * @return The camp
	 */
	public KonquestCamp getCamp(KonquestOfflinePlayer player);
	
	/**
	 * Gets all camps.
	 * 
	 * @return The list of camps
	 */
	public ArrayList<? extends KonquestCamp> getCamps();
	
	/**
	 * Remove a camp territory and break the bed inside.
	 * 
	 * @param player The camp owner
	 * @return True when the camp is removed successfully, else false
	 */
	public boolean removeCamp(KonquestOfflinePlayer player);
	
	/**
	 * Checks whether camp groups (clans) are enabled in the Konquest configuration.
	 * 
	 * @return True when camp groups are enabled, else false
	 */
	public boolean isCampGroupsEnabled();
	
	/**
	 * Checks whether the given camp is part of a camp group (clan).
	 * A camp group is a collection of adjacent camps.
	 * 
	 * @param camp The camp to check
	 * @return True when the camp is part of a group, else false
	 */
	public boolean isCampGrouped(KonquestCamp camp);
	
	/**
	 * Gets the camp group of the given camp.
	 * Returns null if no group exists.
	 * 
	 * @param camp The camp
	 * @return The camp group, or null if no group exists
	 */
	public KonquestCampGroup getCampGroup(KonquestCamp camp);
}
