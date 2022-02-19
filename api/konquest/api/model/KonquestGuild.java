package konquest.api.model;


/**
 * A guild is a group of kingdom members.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestGuild {

	public KonquestKingdom getKingdom();
	
	public boolean isArmistice(KonquestGuild guild);
}
