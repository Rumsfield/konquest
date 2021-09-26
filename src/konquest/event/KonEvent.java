package konquest.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import konquest.Konquest;

public class KonEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private boolean cancelled;
	
	private Konquest konquest;
	
	public KonEvent(Konquest konquest) {
		this.konquest = konquest;
		this.cancelled = false;
	}
	
	public Konquest getKonquest() {
		return konquest;
	}
	
	public boolean isCancelled() {
        return cancelled;
    }
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }
    
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	public static HandlerList getHandlerList() {
        return handlers;
    }

}
