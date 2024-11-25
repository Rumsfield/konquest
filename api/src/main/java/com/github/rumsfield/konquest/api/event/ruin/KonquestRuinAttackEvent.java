package com.github.rumsfield.konquest.api.event.ruin;

import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;
import com.github.rumsfield.konquest.api.model.KonquestRuin;

/**
 * Called before a player breaks a critical block within a ruin.
 * <p>
 * This event is called before {@link KonquestRuinCaptureEvent KonquestRuinCaptureEvent}.
 * Players can only break critical blocks inside of ruins.
 * Canceling this event prevents the critical block from braking.
 * </p>
 *
 * @author Rumsfield
 *
 */
public class KonquestRuinAttackEvent extends KonquestRuinEvent implements Cancellable {

	private boolean isCancelled;
	
	private final KonquestPlayer attacker;
	private final Block block;
	
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

	/**
	 * Checks whether this event is canceled.
	 *
	 * @return True when the event is canceled, else false
	 */
	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	/**
	 * Controls whether the event is canceled.
	 * Canceling this event prevents the critical block from braking.
	 *
	 * @param val True to cancel this event, else false
	 */
	@Override
	public void setCancelled(boolean val) {
		isCancelled = val;
	}

}
