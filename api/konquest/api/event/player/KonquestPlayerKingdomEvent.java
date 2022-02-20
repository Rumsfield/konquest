package konquest.api.event.player;

import org.bukkit.event.Cancellable;

import konquest.api.KonquestAPI;
import konquest.api.model.KonquestKingdom;
import konquest.api.model.KonquestPlayer;

/**
 * Called before the given player has been assigned to the given kingdom (or exiled as barbarian)
 * 
 * @author Rumsfield
 */
public class KonquestPlayerKingdomEvent extends KonquestPlayerEvent implements Cancellable {

	private KonquestKingdom newKingdom;
	private KonquestKingdom exileKingdom;
	private boolean barbarian;
	private boolean isCancelled;
	
	/**
	 * Constructor for a new event
	 * 
	 * @param konquest The KonquestAPI instance
	 * @param newKingdom The kingdom that the player is changing to
	 * @param exileKingdom The kingdom that the player is leaving
	 * @param barbarian Is the player becoming a barbarian?
	 */
	public KonquestPlayerKingdomEvent(KonquestAPI konquest, KonquestPlayer player, KonquestKingdom newKingdom, KonquestKingdom exileKingdom, boolean barbarian) {
		super(konquest, player);
		this.newKingdom = newKingdom;
		this.exileKingdom = exileKingdom;
		this.barbarian = barbarian;
		this.isCancelled = false;
	}
	
	/**
	 * Get the kingdom that the player is changing to
	 * 
	 * @return The new kingdom
	 */
	public KonquestKingdom getKingdom() {
		return newKingdom;
	}
	
	/**
	 * Get the kingdom that the player is leaving
	 * 
	 * @return The old kingdom
	 */
	public KonquestKingdom getExile() {
		return exileKingdom;
	}
	
	/**
	 * Get whether the player is becoming a barbarian or not
	 * 
	 * @return True when the player is becoming a barbarian, else false
	 */
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
