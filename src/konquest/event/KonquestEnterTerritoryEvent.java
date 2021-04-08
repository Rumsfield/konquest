package konquest.event;

import konquest.Konquest;
import konquest.model.KonPlayer;
import konquest.model.KonTerritory;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerMoveEvent;

public class KonquestEnterTerritoryEvent extends Event{
	
	private static final HandlerList handlers = new HandlerList();
	private boolean cancelled;
	
	private Konquest konquest;
	private KonPlayer player;
	private KonTerritory territory;
	private PlayerMoveEvent moveEvent;
	
	public KonquestEnterTerritoryEvent(Konquest konquest, KonPlayer player, KonTerritory territory, PlayerMoveEvent moveEvent) {
		this.konquest = konquest;
		this.player = player;
		this.territory = territory;
		this.moveEvent = moveEvent;
		this.cancelled = false;
	}
	
	public Konquest getKonquest() {
		return konquest;
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
	public HandlerList getHandlers() {
		return handlers;
	}
	public static HandlerList getHandlerList() {
        return handlers;
    }
	public boolean isCancelled() {
        return cancelled;
    }
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
        moveEvent.setCancelled(true);
    }

}
