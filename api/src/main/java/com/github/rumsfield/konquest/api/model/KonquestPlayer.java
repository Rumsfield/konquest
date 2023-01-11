package com.github.rumsfield.konquest.api.model;

import org.bukkit.entity.Player;

/**
 * Represents an online player in Konquest.
 * This interface wraps around Bukkit's Player interface.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestPlayer extends KonquestOfflinePlayer {

	/**
	 * Get the Player instance represented by this object.
	 * 
	 * @return The player
	 */
    Player getBukkitPlayer();
	
	/**
	 * Get whether the player is in Admin Bypass mode.
	 * 
	 * @return True when in admin bypass mode, else false
	 */
    boolean isAdminBypassActive();
	
	/**
	 * Get whether the player is using global chat or kingdom chat.
	 * 
	 * @return True when using global chat, else false
	 */
    boolean isGlobalChat();
	
	/**
	 * Get whether the player is combat tagged.
	 * 
	 * @return True when the player is combat tagged, else false
	 */
    boolean isCombatTagged();
	
	/**
	 * Get whether the player is using the fly command within friendly territory.
	 * 
	 * @return True when the player is flying, else false
	 */
    boolean isFlyEnabled();
	
}
