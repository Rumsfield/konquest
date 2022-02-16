package konquest.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import konquest.Konquest;

public class KonquestEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	
	private Konquest konquest;
	
	public KonquestEvent(Konquest konquest) {
		this.konquest = konquest;
	}
	
	public Konquest getKonquest() {
		return konquest;
	}
    
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	public static HandlerList getHandlerList() {
        return handlers;
    }

}
