package konquest.api.event;

import konquest.Konquest;
import konquest.model.KonPlayer;
import konquest.model.KonTerritory;

import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerMoveEvent;

public class KonEnterTerritoryEvent extends KonEvent implements Cancellable {
	
	private KonPlayer player;
	private KonTerritory territory;
	private PlayerMoveEvent moveEvent;
	private boolean isCancelled;
	
	public KonEnterTerritoryEvent(Konquest konquest, KonPlayer player, KonTerritory territory, PlayerMoveEvent moveEvent) {
		super(konquest);
		this.player = player;
		this.territory = territory;
		this.moveEvent = moveEvent;
		this.isCancelled = false;
	}
	
	public KonPlayer getPlayer() {
		return player;
	}
	
	public KonTerritory getTerritory() {
		return territory;
	}
	
	public PlayerMoveEvent getMoveEvent() {
		return moveEvent;
	}
	
	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean val) {
		isCancelled = val;
		moveEvent.setCancelled(true);
	}

}
