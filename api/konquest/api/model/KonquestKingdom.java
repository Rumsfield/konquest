package konquest.api.model;

/**
 * My kingdom for a horse!
 * A kingdom is a collection of towns and a monument template.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestKingdom {

	/**
	 * Get whether the kingdom is peaceful. Peaceful kingdoms cannot be attacked, or attack others.
	 * 
	 * @return True if peaceful, else false
	 */
	public boolean isPeaceful();
	
	/**
	 * Get the name of this kingdom.
	 * 
	 * @return The name
	 */
	public String getName();
	
}
