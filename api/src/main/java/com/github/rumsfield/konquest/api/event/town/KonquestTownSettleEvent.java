package com.github.rumsfield.konquest.api.event.town;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.event.player.KonquestPlayerSettleEvent;
import com.github.rumsfield.konquest.api.model.KonquestKingdom;
import com.github.rumsfield.konquest.api.model.KonquestPlayer;
import com.github.rumsfield.konquest.api.model.KonquestTown;

/**
 * Called when a new town is settled by a player.
 * <p>
 * This event cannot be cancelled, as it is called after the town is created.
 * To prevent players settling towns, listen for {@link KonquestPlayerSettleEvent KonquestPlayerSettleEvent} and cancel it.
 * </p>
 * 
 * @author Rumsfield
 *
 */
public class KonquestTownSettleEvent extends KonquestTownEvent {

	private KonquestPlayer player;
	private KonquestKingdom kingdom;
	
	/**
	 * Default constructor
	 * @param konquest The API instance
	 * @param town The town
	 * @param player The player
	 * @param kingdom The kingdom
	 */
	public KonquestTownSettleEvent(KonquestAPI konquest, KonquestTown town, KonquestPlayer player, KonquestKingdom kingdom) {
		super(konquest, town);
		this.player = player;
		this.kingdom = kingdom;
	}
	
	/**
	 * Gets the player that settled the town.
	 * This player is the town lord.
	 * 
	 * @return The player
	 */
	public KonquestPlayer getPlayer() {
		return player;
	}
	
	/**
	 * Gets the kingdom of the new town.
	 * 
	 * @return The kingdom
	 */
	public KonquestKingdom getKingdom() {
		return kingdom;
	}

}
