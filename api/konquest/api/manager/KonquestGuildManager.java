package konquest.api.manager;

import java.util.List;

import org.bukkit.OfflinePlayer;

import konquest.api.model.KonquestGuild;
import konquest.api.model.KonquestKingdom;
import konquest.api.model.KonquestOfflinePlayer;
import konquest.api.model.KonquestTown;

/**
 * A manager for guilds in Konquest.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestGuildManager {

	/**
	 * Checks whether guilds are enabled from the Konquest configuration.
	 * 
	 * @return True when guilds are enabled, else false
	 */
	public boolean isEnabled();
	
	/**
	 * Checks whether guild trade discounts are enabled from the Konquest configuration.
	 * 
	 * @return True when guild trade discounts are enabled, else false
	 */
	public boolean isDiscountEnable();
	
	/**
	 * Checks if the given name matches a guild.
	 * 
	 * @param name The guild name
	 * @return True if the name matches a guild instance
	 */
	public boolean isGuild(String name);
	
	/**
	 * Gets the guild instance by the given name.
	 * Returns null when no guild matches the name.
	 * 
	 * @param name The guild name
	 * @return The guild instance, or null when name not found
	 */
	public KonquestGuild getGuild(String name);
	
	/**
	 * Gets a list of all guild names.
	 * This is a convenience method.
	 * 
	 * @return The list of guild names
	 */
	public List<String> getAllGuildNames();
	
	/**
	 * Gets all guilds.
	 * 
	 * @return The list of all guilds
	 */
	public List<? extends KonquestGuild> getAllGuilds();
	
	/**
	 * Gets all guilds which are not in the given kingdom.
	 * 
	 * @param kingdom The kingdom to search for enemy guilds
	 * @return The list of enemy guilds
	 */
	public List<? extends KonquestGuild> getEnemyGuilds(KonquestKingdom kingdom);
	
	/**
	 * Gets all guilds which are in the same kingdom as the given kingdom.
	 * 
	 * @param kingdom The kingdom to search for friendly guilds
	 * @return The list of friendly guilds
	 */
	public List<? extends KonquestGuild> getKingdomGuilds(KonquestKingdom kingdom);
	
	/**
	 * Gets the guild of the given town.
	 * Returns null when the town has no guild.
	 * A town's guild is determined by the guild of the town lord.
	 * 
	 * @param town The town to check
	 * @return The guild instance of the town, or null if the town has no guild
	 */
	public KonquestGuild getTownGuild(KonquestTown town);
	
	/**
	 * Gets the guild of the given player.
	 * Returns null when the player has no guild.
	 * 
	 * @param player The player to check
	 * @return The guild instance of the player, or null if the player has no guild
	 */
	public KonquestGuild getPlayerGuild(OfflinePlayer player);
	
	/**
	 * Checks whether the two guilds are in an armistice.
	 * Guilds in an armistice cannot attack each other.
	 * Only enemy guilds can be in an armistice.
	 * 
	 * @param guild1 The first guild
	 * @param guild2 The second guild, must be an enemy of the first
	 * @return True when both guilds are in an armistice, else false
	 */
	public boolean isArmistice(KonquestGuild guild1, KonquestGuild guild2);
	
	/**
	 * Checks whether the two players' guilds are in an armistice.
	 * Guilds in an armistice cannot attack each other.
	 * Only enemy guilds can be in an armistice.
	 * 
	 * @param player1 The first player
	 * @param player2 The second player, must be an enemy of the first
	 * @return True when both players' guilds are in an armistice, else false
	 */
	public boolean isArmistice(KonquestOfflinePlayer player1, KonquestOfflinePlayer player2);
	
	/**
	 * Checks whether the player's guild is in an armistice with the town.
	 * A town's guild is determined by the guild of the town lord.
	 * Guilds in an armistice cannot attack each other.
	 * Only enemy guilds can be in an armistice.
	 * 
	 * @param player1 The player
	 * @param town2 The town, must be an enemy of the player
	 * @return True when the player's guild and the town's guild are in an armistice, else false
	 */
	public boolean isArmistice(KonquestOfflinePlayer player1, KonquestTown town2);
	
}
