package konquest.event;

import konquest.Konquest;
import konquest.model.KonPlayer;
import konquest.model.KonTerritory;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;

public class KonquestMonumentDamageEvent extends Event{
	
	private static final HandlerList handlers = new HandlerList();
	private boolean cancelled;
	
	private Konquest konquest;
	private KonPlayer player;
	private KonTerritory territory;
	private BlockBreakEvent blockEvent;
	
	public KonquestMonumentDamageEvent(Konquest konquest, KonPlayer player, KonTerritory territory, BlockBreakEvent blockEvent) {
		this.konquest = konquest;
		this.blockEvent = blockEvent;
		this.territory = territory;
		this.player = player;
		this.cancelled = false;
	}
	
	public Konquest getKonquest() {
		return konquest;
	}
	
	public BlockBreakEvent getBlockEvent() {
		return blockEvent;
	}
	
	public KonTerritory getTerritory() {
		return territory;
	}
	
	public KonPlayer getPlayer() {
		return player;
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
        blockEvent.setCancelled(true);
    }

}