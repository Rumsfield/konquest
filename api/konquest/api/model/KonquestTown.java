package konquest.api.model;

import java.util.UUID;

/**
 * A town is a collection of land and has a monument.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestTown extends KonquestTerritory {

	/**
	 * Gets the UUID of the town lord.
	 * Returns null if there is no town lord.
	 * 
	 * @return The UUID of the town lord, or null
	 */
	public UUID getLord();
}
