package com.github.rumsfield.konquest.api.manager;

import com.github.rumsfield.konquest.api.model.KonquestTown;

/**
 * A manager for town shields and armor in Konquest.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestShieldManager {

	/**
	 * Checks whether shields are enabled from the Konquest configuration.
	 * 
	 * @return True when shields are enabled, else false
	 */
    boolean isShieldsEnabled();
	
	/**
	 * Checks whether armors are enabled from the Konquest configuration.
	 * 
	 * @return True when armors are enabled, else false
	 */
    boolean isArmorsEnabled();
	
	/**
	 * Sets a town's shields to expire a number of seconds from now.
	 * When value is less than or equal to 0, the town's shields are deactivated.
	 * When the town already has shields deactivated, and value is less than or equal to zero, this method returns false.
	 * 
	 * @param town The town to modify shields
	 * @param value Time in seconds from now to end shields
	 * @return True when shields were successfully set, else false
	 */
    boolean shieldSet(KonquestTown town, int value);
	
	/**
	 * Adds to a town's shields in seconds, positive or negative.
	 * When the result of the addition is less than or equal to zero, the shields are disabled.
	 * When the town already has shields deactivated, and value is negative, this method returns false.
	 * 
	 * @param town The town to modify shields
	 * @param value Time in seconds to add to shields
	 * @return True when the shields were successfully added, else false
	 */
    boolean shieldAdd(KonquestTown town, int value);
	
	/**
	 * Sets a town's armor amount in blocks.
	 * When value is less than or equal to 0, the town's armor is deactivated.
	 * When the town already has armor deactivated, and value is less than or equal to zero, this method returns false.
	 * 
	 * @param town The town to modify armor
	 * @param value Amount of armor
	 * @return True when armor was successfully set, else false
	 */
    boolean armorSet(KonquestTown town, int value);
	
	/**
	 * Adds to a town's armor in blocks, positive or negative.
	 * When the result of the addition is less than or equal to zero, the armor is disabled.
	 * When the town already has armor deactivated, and value is negative, this method returns false.
	 * 
	 * @param town The town to modify armor
	 * @param value Amount of armor to add
	 * @return True when the armor was successfully added, else false
	 */
    boolean armorAdd(KonquestTown town, int value);
	
	/**
	 * Gets the time in seconds at which the town's shields will expire.
	 * The resulting time is the number of seconds since the Unix epoch.
	 * The Unix epoch is 00:00:00 UTC on 1 January 1970.
	 * 
	 * @param town The town
	 * @return The time in seconds when shields expire
	 */
    int getTownShieldTime(KonquestTown town);
	
	/**
	 * Gets the amount of armor blocks the town currently has.
	 * 
	 * @param town The town
	 * @return The amount of armor blocks
	 */
    int getTownArmorBlocks(KonquestTown town);
	
}
