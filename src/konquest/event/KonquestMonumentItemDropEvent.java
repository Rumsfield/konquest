package konquest.event;

import konquest.Konquest;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class KonquestMonumentItemDropEvent extends Event{
	
	private static final HandlerList handlers = new HandlerList();
	private boolean cancelled;
	
	private Konquest konquest;
	private Event itemEvent;
	
	public KonquestMonumentItemDropEvent(Konquest konquest, Event event) {
		this.konquest = konquest;
		this.itemEvent = event;
		this.cancelled = false;
	}
	
	public Konquest getKonquest() {
		return konquest;
	}
	
	public Event getItemEvent() {
		return itemEvent;
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
    }

}
