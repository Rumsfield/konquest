package konquest.api.event.ruin;

import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;

import konquest.api.KonquestAPI;
import konquest.api.model.KonquestPlayer;
import konquest.api.model.KonquestRuin;

/**
 * Called when a player breaks a critical block within a ruin, but before {@link KonquestRuinCaptureEvent KonquestRuinCaptureEvent}
 * <p>
 * Players can only break critical blocks inside of ruins.
 * Canceling this event prevents the block from braking.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestRuinAttackEvent extends KonquestRuinEvent implements Cancellable {

	private boolean isCancelled;
	
	private KonquestPlayer attacker;
	private Block block;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param ruin The ruin
	 * @param attacker The attacking player
	 * @param block The block
	 */
	public KonquestRuinAttackEvent(KonquestAPI konquest, KonquestRuin ruin, KonquestPlayer attacker, Block block) {
		super(konquest, ruin);
		this.isCancelled = false;
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

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean val) {
		isCancelled = val;
	}

}
