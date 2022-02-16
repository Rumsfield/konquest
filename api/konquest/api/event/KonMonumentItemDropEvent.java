package konquest.api.event;

import konquest.Konquest;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public class KonMonumentItemDropEvent extends KonEvent implements Cancellable {
	
	private Event itemEvent;
	private boolean isCancelled;
	
	public KonMonumentItemDropEvent(Konquest konquest, Event event) {
		super(konquest);
		this.itemEvent = event;
		this.isCancelled = false;
	}
	
	public Event getItemEvent() {
		return itemEvent;
	}
	
	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean val) {
		isCancelled = val;
	}

}
