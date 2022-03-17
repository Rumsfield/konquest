package konquest.api.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Villager;

/**
 * A guild is a group of kingdom members.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestGuild {

	/**
	 * Checks whether this guild is open.
	 * Open guilds allow any friendly kingdom member to join.
	 * 
	 * @return True when the guild is open, else false
	 */
	public boolean isOpen();
	
	/**
	 * Gets the name of this guild.
	 * 
	 * @return The guild name
	 */
	public String getName();
	
	/**
	 * Gets the trade specialization of this guild.
	 * 
	 * @return The villager profession trade specialization
	 */
	public Villager.Profession getSpecialization();
	
	/**
	 * Gets the kingdom of this guild.
	 * 
	 * @return The kingdom
	 */
	public KonquestKingdom getKingdom();
	
	/**
	 * Checks whether the given UUID is the guild master.
	 * 
	 * @param id The UUID to check
	 * @return True when the given UUID is the guild master, else false
	 */
	public boolean isMaster(UUID id);
	
	/**
	 * Checks whether the given UUID is a guild officer.
	 * Both the guild master and general officers will return true.
	 * 
	 * @param id The UUID to check
	 * @return True when the given UUID is a guild officer, else false
	 */
	public boolean isOfficer(UUID id);
	
	/**
	 * Checks whether the given UUID is a guild member.
	 * The guild master, all guild officers, and all other guild members will return true.
	 * 
	 * @param id The UUID to check
	 * @return True when the given UUID is a guild member, else false
	 */
	public boolean isMember(UUID id);
	
	/**
	 * Checks if the guild has a valid guild master.
	 * 
	 * @return True when the guild master exists, else false
	 */
	public boolean isMasterValid();
	
	/**
	 * Gets the UUID of the guild master. Can be null.
	 * 
	 * @return The UUID of the guild master
	 */
	public UUID getMaster();
	
	/**
	 * Convenience method for getting the Bukkit OfflinePlayer instance for the guild master.
	 * Can be null.
	 * 
	 * @return The guild master player instance
	 */
	public OfflinePlayer getPlayerMaster();
	
	/**
	 * Gets the list of guild officers, including the guild master.
	 * 
	 * @return The list of players
	 */
	public ArrayList<OfflinePlayer> getPlayerOfficers();
	
	/**
	 * Gets the list of guild officers, excluding the guild master.
	 * 
	 * @return The list of players
	 */
	public ArrayList<OfflinePlayer> getPlayerOfficersOnly();
	
	/**
	 * Gets the list of guild members, including all officers and the guild master.
	 * 
	 * @return The list of players
	 */
	public ArrayList<OfflinePlayer> getPlayerMembers();
	
	/**
	 * Gets the list of guild members, excluding all officers and the guild master.
	 * 
	 * @return The list of players
	 */
	public ArrayList<OfflinePlayer> getPlayerMembersOnly();
	
	/**
	 * Checks whether this guild has sanctioned the given guild.
	 * 
	 * @param guild The other guild
	 * @return True when this guild has sanctioned the other, else false
	 */
	public boolean isSanction(KonquestGuild guild);
	
	/**
	 * Checks whether this guild and the given guild are in an armistice.
	 * 
	 * @param guild The other guild
	 * @return True when both guilds are in an armistice, else false
	 */
	public boolean isArmistice(KonquestGuild guild);
	
	/**
	 * Gets the list of all other guilds in an armistice with this one.
	 * Only guilds from enemy kingdoms can be in an armistice.
	 * 
	 * @return The list of enemy guilds in an armistice
	 */
	public List<String> getArmisticeNames();
	
	/**
	 * Checks whether the given town is a member of this guild.
	 * A town is a member if the town's lord is a guild member.
	 * 
	 * @param town The town to check
	 * @return True when the town is a member, else false
	 */
	public boolean isTownMember(KonquestTown town);
	
}
