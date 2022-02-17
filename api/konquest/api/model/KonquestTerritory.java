package konquest.api.model;

/**
 * The base interface for any territory.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestTerritory {

	/**
	 * Get the territory's kingdom.
	 * 
	 * @return The kingdom object
	 */
	public KonquestKingdom getKingdom();
	
	/**
	 * Get the name of this territory.
	 * 
	 * @return The name
	 */
	public String getName();
	
}
