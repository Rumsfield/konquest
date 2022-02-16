package konquest.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import konquest.Konquest;
import konquest.api.event.KonquestEvent;

public class KonEvent extends Event implements KonquestEvent {

	private static final HandlerList handlers = new HandlerList();
	
	private Konquest konquest;
	
	public KonEvent(Konquest konquest) {
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
