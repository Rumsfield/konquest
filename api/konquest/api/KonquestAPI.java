package konquest.api;

import org.bukkit.ChatColor;

import konquest.api.manager.KonquestPlayerManager;

public interface KonquestAPI {

	/**
	 * Get the friendly primary color, from core.yml.
	 * 
	 * @return The friendly primary color
	 */
	public ChatColor getFriendlyPrimaryColor();
	
	/**
	 * Get the enemy primary color, from core.yml.
	 * 
	 * @return The enemy primary color
	 */
	public ChatColor getEnemyPrimaryColor();
	
	public KonquestPlayerManager getPlayerManager();
	
	
}
