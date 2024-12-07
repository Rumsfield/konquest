package com.github.rumsfield.konquest.api.event.town;

import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;
import com.github.rumsfield.konquest.api.model.KonquestTown;
import com.github.rumsfield.konquest.api.event.player.KonquestPlayerConquerEvent;
import org.bukkit.event.HandlerList;

import javax.annotation.Nonnull;

/**
 * Called when an enemy player breaks a block within a town.
 * <p>
 * This event is called before {@link KonquestTownCaptureEvent KonquestTownCaptureEvent},
 * {@link KonquestTownDestroyEvent KonquestTownDestroyEvent}, and {@link KonquestPlayerConquerEvent KonquestPlayerConquerEvent}.
 * This event occurs for all blocks broken inside town land, including inside of the monument and for critical blocks.
 * Canceling this event prevents the block from breaking.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestTownAttackEvent extends KonquestTownEvent implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private boolean isCancelled;
	private final KonquestPlayer attacker;
	private final Block block;
	private final boolean isMonument;
	private final boolean isCritical;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param town The town
	 * @param attacker The attacking player
	 * @param block The block
	 * @param isMonument Is the block inside the monument
	 * @param isCritical Is the block a critical monument block
	 */
	public KonquestTownAttackEvent(KonquestAPI konquest, KonquestTown town, KonquestPlayer attacker, Block block, boolean isMonument, boolean isCritical) {
		super(konquest, town);
		this.isCancelled = false;
		this.attacker = attacker;
		this.block = block;
		this.isMonument = isMonument;
		this.isCritical = isCritical;
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
	 * Checks whether the block was broken anywhere inside the monument.
	 * 
	 * @return True when the block is inside the town monument, else false
	 */
	public boolean isMonument() {
		return isMonument;
	}
	
	/**
	 * Checks whether the block is a critical block in the monument.
	 * 
	 * @return True when the block is a critical block, else false
	 */
	public boolean isCritical() {
		return isCritical;
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
	 * Canceling this event prevents the town from being attacked, and any associated blocks from breaking.
	 *
	 * @param val True to cancel this event, else false
	 */
	@Override
	public void setCancelled(boolean val) {
		isCancelled = val;
	}

	/**
	 * Get the handler list
	 *
	 * @return handlers
	 */
	public static HandlerList getHandlerList() {
		return handlers;
	}

	/**
	 * Get the handler list
	 *
	 * @return handlers
	 */
	@Override
	@Nonnull
	public HandlerList getHandlers() {
		return handlers;
	}

}
