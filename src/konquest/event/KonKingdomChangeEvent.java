package konquest.event;

import org.bukkit.event.Cancellable;

import konquest.Konquest;
import konquest.model.KonKingdom;
import konquest.model.KonPlayer;

/**
 * Called before the given player has been assigned to the given kingdom (or exiled as barbarian)
 * @return
 */
public class KonKingdomChangeEvent extends KonEvent implements Cancellable {

	private KonPlayer player;
	private KonKingdom newKingdom;
	private KonKingdom exileKingdom;
	private boolean barbarian;
	private boolean isCancelled;
	
	public KonKingdomChangeEvent(Konquest konquest, KonPlayer player, KonKingdom newKingdom, KonKingdom exileKingdom, boolean barbarian) {
		super(konquest);
		this.player = player;
		this.newKingdom = newKingdom;
		this.exileKingdom = exileKingdom;
		this.barbarian = barbarian;
		this.isCancelled = false;
	}
	
	public KonPlayer getPlayer() {
		return player;
	}
	
	public KonKingdom getKingdom() {
		return newKingdom;
	}
	
	public KonKingdom getExile() {
		return exileKingdom;
	}
	
	public boolean isBarbarian() {
		return barbarian;
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
