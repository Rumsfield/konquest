package konquest.api.model;

import java.util.ArrayList;


/**
 * My kingdom for a horse!
 * A kingdom is a capital, a collection of towns, and a monument template.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestKingdom {

	/**
	 * Checks whether this kingdom is currently the smallest, in terms of players.
	 * 
	 * @return True when this kingdom is the smallest, else false
	 */
	public boolean isSmallest();
	
	/**
	 * Checks whether this kingdom is currently protected from attacks.
	 * Kingdoms become protected when there are not enough players online, based on settings in the Konquest configuration.
	 * When a kingdom is protected, enemies cannot attack its towns.
	 * 
	 * @return True when this kingdom is protected, else false
	 */
	public boolean isOfflineProtected();
	
	/**
	 * Get whether the kingdom is peaceful. Peaceful kingdoms cannot be attacked, or attack others.
	 * 
	 * @return True if peaceful, else false
	 */
	public boolean isPeaceful();
	
	/**
	 * Checks whether this kingdom currently has a blanked monument template.
	 * A monument template is blanked when a player in admin bypass mode edits it.
	 * Blanking lasts for 2 minutes after the last block edit.
	 * When a kingdom's monument template is blanked, players cannot settle new towns, enemies cannot attack town monuments, and monuments will not refresh.
	 * 
	 * @return True when the monument template is blanked, else false
	 */
	public boolean isMonumentBlanking();
	
	/**
	 * Get the name of this kingdom.
	 * 
	 * @return The name
	 */
	public String getName();
	
	/**
	 * Gets the capital territory instance for this kingdom.
	 * 
	 * @return The capital territory
	 */
	public KonquestCapital getCapital();
	
	/**
	 * Gets the list of all town names in this kingdom.
	 * 
	 * @return The list of towns
	 */
	public ArrayList<String> getTownNames();
	
	/**
	 * Checks whether this kingdom has a town with the given name.
	 * 
	 * @param name The town name to check
	 * @return True when a town exists with the given name in this kingdom, else false
	 */
	public boolean hasTown(String name);
	
	/**
	 * Gets the town instance by the given name.
	 * 
	 * @param name The town name
	 * @return The town instance
	 */
	public KonquestTown getTown(String name);
	
	/**
	 * Get a list of all towns in this kingdom.
	 * 
	 * @return The list of towns
	 */
	public ArrayList<? extends KonquestTown> getTowns();
	
}
