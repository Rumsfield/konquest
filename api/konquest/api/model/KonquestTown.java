package konquest.api.model;

import java.util.UUID;

/**
 * A town is a collection of land and has a monument.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestTown extends KonquestTerritory {

	public UUID getLord();
}
