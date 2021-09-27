package konquest.event;

import konquest.Konquest;
import konquest.model.KonKingdom;
import konquest.model.KonPlayer;

/**
 * Called before the given player has been assigned to the given kingdom (or exiled as barbarian)
 * @return
 */
public class KonKingdomChangeEvent extends KonEvent {

	private KonPlayer player;
	private KonKingdom newKingdom;
	private KonKingdom exileKingdom;
	private boolean barbarian;
	
	public KonKingdomChangeEvent(Konquest konquest, KonPlayer player, KonKingdom newKingdom, KonKingdom exileKingdom, boolean barbarian) {
		super(konquest);
		this.player = player;
		this.newKingdom = newKingdom;
		this.exileKingdom = exileKingdom;
		this.barbarian = barbarian;
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

}
