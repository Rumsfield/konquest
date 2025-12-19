package com.github.rumsfield.konquest.api.manager;

import com.github.rumsfield.konquest.api.model.KonquestCamp;
import com.github.rumsfield.konquest.api.model.KonquestOfflinePlayer;

import java.util.ArrayList;

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
    boolean isCampSet(KonquestOfflinePlayer player);
	
	/**
	 * Gets the camp that belongs to a barbarian player. Returns null when the camp does not exist.
	 * 
	 * @param player The camp owner
	 * @return The camp
	 */
    KonquestCamp getCamp(KonquestOfflinePlayer player);
	
	/**
	 * Gets all camps.
	 * 
	 * @return The list of camps
	 */
    ArrayList<? extends KonquestCamp> getCamps();
	
	/**
	 * Remove a camp territory and break the bed inside.
	 * 
	 * @param player The camp owner
	 * @return True when the camp is removed successfully, else false
	 */
    boolean removeCamp(KonquestOfflinePlayer player);

}
