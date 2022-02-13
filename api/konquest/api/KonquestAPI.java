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
	
	public KonquestPlayerManager getPlayerManager();
	
	
}
