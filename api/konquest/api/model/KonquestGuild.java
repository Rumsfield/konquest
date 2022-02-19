package konquest.api.model;


/**
 * A guild is a group of kingdom members.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestGuild {

	/**
	 * Gets the kingdom of this guild.
	 * 
	 * @return The kingdom
	 */
	public KonquestKingdom getKingdom();
	
	/**
	 * Checks whether this guild and the given guild are in an armistice.
	 * 
	 * @param guild The other guild
	 * @return True when both guilds are in an armistice, else false
	 */
	public boolean isArmistice(KonquestGuild guild);
}
