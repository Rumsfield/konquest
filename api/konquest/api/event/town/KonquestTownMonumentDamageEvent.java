package konquest.api.event.town;

import org.bukkit.block.Block;

import konquest.api.KonquestAPI;
import konquest.api.model.KonquestPlayer;
import konquest.api.model.KonquestTown;

/**
 * Called after an enemy player breaks a town monument block.
 * <p>
 * When an enemy breaks a block inside of a town monument, this event is called to handle what happens as a result.
 * Konquest listens for this event to implement default town capture/destroy behavior.
 * This event cannot be cancelled.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestTownMonumentDamageEvent extends KonquestTownEvent {

	private KonquestPlayer attacker;
	private Block block;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param town The town
	 * @param attacker The attacking player
	 * @param block The block
	 */
	public KonquestTownMonumentDamageEvent(KonquestAPI konquest, KonquestTown town, KonquestPlayer attacker, Block block) {
		super(konquest, town);
		this.attacker = attacker;
		this.block = block;
	}
	
	/**
	 * Gets the attacking player.
	 * 
	 * @return The attacker
	 */
	public KonquestPlayer getAttacker() {
		return attacker;
	}
	
	/**
	 * Gets the block broken by the attacker.
	 * 
	 * @return The block
	 */
	public Block getBlock() {
		return block;
	}

}
