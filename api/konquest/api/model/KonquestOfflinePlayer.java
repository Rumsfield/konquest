package konquest.api.model;

/**
 * Represents an offline player in Konquest.
 * This interface wraps around Bukkit's OfflinePlayer interface.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestOfflinePlayer {

	/**
	 * Get the offlinePlayer's kingdom object.
	 * 
	 * @return The player's kingdom
	 */
	public KonquestKingdom getKingdom();
	
}
