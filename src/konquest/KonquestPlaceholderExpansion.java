package konquest;

import org.bukkit.entity.Player;

import konquest.manager.PlaceholderManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

/**
 * This class will be registered through the register-method in the 
 * plugins onEnable-method.
 */
public class KonquestPlaceholderExpansion extends PlaceholderExpansion {

	private KonquestPlugin plugin;
	private PlaceholderManager placeholderManager;
	
	public KonquestPlaceholderExpansion(KonquestPlugin plugin) {
		this.plugin = plugin;
		placeholderManager = plugin.getKonquestInstance().getPlaceholderManager();
	}
	
	/**
     * Because this is an internal class,
     * you must override this method to let PlaceholderAPI know to not unregister your expansion class when
     * PlaceholderAPI is reloaded
     *
     * @return true to persist through reloads
     */
	@Override
    public boolean persist(){
        return true;
    }
	
	/**
     * Because this is a internal class, this check is not needed
     * and we can simply return {@code true}
     *
     * @return Always true since it's an internal class.
     */
    @Override
    public boolean canRegister(){
        return true;
    }
    
    /**
     * The name of the person who created this expansion should go here.
     * For convienience do we return the author from the plugin.yml
     * 
     * @return The name of the author as a String.
     */
    @Override
    public String getAuthor(){
        return plugin.getDescription().getAuthors().toString();
    }

    /**
     * The placeholder identifier should go here.
     * This is what tells PlaceholderAPI to call our onRequest 
     * method to obtain a value if a placeholder starts with our 
     * identifier.
     * The identifier has to be lowercase and can't contain _ or %
     *
     * @return The identifier in {@code %<identifier>_<value>%} as String.
     */
    @Override
    public String getIdentifier(){
        return "konquest";
    }

    /**
     * This is the version of the expansion.
     * You don't have to use numbers, since it is set as a String.
     *
     * For convienience do we return the version from the plugin.yml
     *
     * @return The version as a String.
     */
    @Override
    public String getVersion(){
        return plugin.getDescription().getVersion();
    }
    
    /**
     * This is the method called when a placeholder with our identifier 
     * is found and needs a value.
     * We specify the value identifier in this method.
     * Since version 2.9.1 can you use OfflinePlayers in your requests.
     *
     * @param  player
     *         A {@link org.bukkit.Player Player}.
     * @param  identifier
     *         A String containing the identifier/value.
     *
     * @return possibly-null String of the requested identifier.
     */
    @Override
    public String onPlaceholderRequest(Player player, String identifier){

        if(player == null){
            return "";
        }
        
        // Provide placeholder value info
        String result = null;
        switch(identifier.toLowerCase()) {
	        /* %konquest_kingdom% - player's kingdom name */
        	case "kingdom":
        		result = placeholderManager.getKingdom(player);
	        	break;
	        /* %konquest_exile% - player's exile kingdom name */
        	case "exile":
        		result = placeholderManager.getExile(player);
	        	break;
	        /* %konquest_barbarian% - true if player is barbarian, else false */
	        case "barbarian":
	        	result = placeholderManager.getBarbarian(player);
	        	break;
	        /* %konquest_towns_lord% - comma-separated list of player's lord only towns */
	        case "towns_lord":
	        	result = placeholderManager.getTownsLord(player);
	        	break;
	        /* %konquest_towns_knight% - comma-separated list of player's knight only towns */
	        case "towns_knight":
	        	result = placeholderManager.getTownsKnight(player);
	        	break;
	        /* %konquest_towns_resident% - comma-separated list of player's resident only towns */
	        case "towns_resident":
	        	result = placeholderManager.getTownsResident(player);
	        	break;
	        /* %konquest_towns_all% - comma-separated list of player's all towns */
	        case "towns_all":
	        	result = placeholderManager.getTownsAll(player);
	        	break;
	        /* %konquest_territory% - player's current location territory type */
	        case "territory":
	        	result = placeholderManager.getTerritory(player);
	        	break;
	        /* %konquest_land% - player's current location territory name */
	        case "land":
	        	result = placeholderManager.getLand(player);
	        	break;
	        /* %konquest_claimed% - true if the player's current location is claimed, else false */
	        case "claimed":
	        	result = placeholderManager.getClaimed(player);
	        	break;
	        /* %konquest_score% - player's score value */
	        case "score":
	        	result = placeholderManager.getScore(player);
	        	break;
	        /* %konquest_lordships% - number of player's lordships */
	        case "lordships":
	        	result = placeholderManager.getLordships(player);
	        	break;
	        /* %konquest_residencies% - number of player's total residencies, including lordships */
	        case "residencies":
	        	result = placeholderManager.getResidencies(player);
	        	break;
	        /* %konquest_chat% - true if player is using global chat, else false */
	        case "chat":
	        	result = placeholderManager.getChat(player);
	        	break;
	        /* %konquest_combat% - true if player is combat tagged, else false */
	        case "combat":
	        	result = placeholderManager.getCombat(player);
	        	break;
	        /* %konquest_top_score_1% - kingdom name and score in rank 1 */
	        case "top_score_1":
	        	result = placeholderManager.getTopScore(player, 1);
	        	break;
	        /* %konquest_top_score_2% - kingdom name and score in rank 2 */
	        case "top_score_2":
	        	result = placeholderManager.getTopScore(player, 2);
	        	break;
	        /* %konquest_top_score_3% - kingdom name and score in rank 3 */
	        case "top_score_3":
	        	result = placeholderManager.getTopScore(player, 3);
	        	break;
	        /* %konquest_top_score_4% - kingdom name and score in rank 4 */
	        case "top_score_4":
	        	result = placeholderManager.getTopScore(player, 4);
	        	break;
	        /* %konquest_top_score_5% - kingdom name and score in rank 5 */
	        case "top_score_5":
	        	result = placeholderManager.getTopScore(player, 5);
	        	break;
	        /* %konquest_top_score_6% - kingdom name and score in rank 6 */
	        case "top_score_6":
	        	result = placeholderManager.getTopScore(player, 6);
	        	break;
	        /* %konquest_top_score_7% - kingdom name and score in rank 7 */
	        case "top_score_7":
	        	result = placeholderManager.getTopScore(player, 7);
	        	break;
	        /* %konquest_top_score_8% - kingdom name and score in rank 8 */
	        case "top_score_8":
	        	result = placeholderManager.getTopScore(player, 8);
	        	break;
	        /* %konquest_top_score_9% - kingdom name and score in rank 9 */
	        case "top_score_9":
	        	result = placeholderManager.getTopScore(player, 9);
	        	break;
	        /* %konquest_top_score_10% - kingdom name and score in rank 10 */
	        case "top_score_10":
	        	result = placeholderManager.getTopScore(player, 10);
	        	break;
	        /* %konquest_top_town_1% - kingdom name and towns in rank 1 */
	        case "top_town_1":
	        	result = placeholderManager.getTopTown(player, 1);
	        	break;
	        /* %konquest_top_town_2% - kingdom name and towns in rank 2 */
	        case "top_town_2":
	        	result = placeholderManager.getTopTown(player, 2);
	        	break;
	        /* %konquest_top_town_3% - kingdom name and towns in rank 3 */
	        case "top_town_3":
	        	result = placeholderManager.getTopTown(player, 3);
	        	break;
	        /* %konquest_top_town_4% - kingdom name and towns in rank 4 */
	        case "top_town_4":
	        	result = placeholderManager.getTopTown(player, 4);
	        	break;
	        /* %konquest_top_town_5% - kingdom name and towns in rank 5 */
	        case "top_town_5":
	        	result = placeholderManager.getTopTown(player, 5);
	        	break;
	        /* %konquest_top_town_6% - kingdom name and towns in rank 6 */
	        case "top_town_6":
	        	result = placeholderManager.getTopTown(player, 6);
	        	break;
	        /* %konquest_top_town_7% - kingdom name and towns in rank 7 */
	        case "top_town_7":
	        	result = placeholderManager.getTopTown(player, 7);
	        	break;
	        /* %konquest_top_town_8% - kingdom name and towns in rank 8 */
	        case "top_town_8":
	        	result = placeholderManager.getTopTown(player, 8);
	        	break;
	        /* %konquest_top_town_9% - kingdom name and towns in rank 9 */
	        case "top_town_9":
	        	result = placeholderManager.getTopTown(player, 9);
	        	break;
	        /* %konquest_top_town_10% - kingdom name and towns in rank 10 */
	        case "top_town_10":
	        	result = placeholderManager.getTopTown(player, 10);
	        	break;
	        /* %konquest_top_land_1% - kingdom name and land in rank 1 */
	        case "top_land_1":
	        	result = placeholderManager.getTopLand(player, 1);
	        	break;
	        /* %konquest_top_land_2% - kingdom name and land in rank 2 */
	        case "top_land_2":
	        	result = placeholderManager.getTopLand(player, 2);
	        	break;
	        /* %konquest_top_land_3% - kingdom name and land in rank 3 */
	        case "top_land_3":
	        	result = placeholderManager.getTopLand(player, 3);
	        	break;
	        /* %konquest_top_land_4% - kingdom name and land in rank 4 */
	        case "top_land_4":
	        	result = placeholderManager.getTopLand(player, 4);
	        	break;
	        /* %konquest_top_land_5% - kingdom name and land in rank 5 */
	        case "top_land_5":
	        	result = placeholderManager.getTopLand(player, 5);
	        	break;
	        /* %konquest_top_land_6% - kingdom name and land in rank 6 */
	        case "top_land_6":
	        	result = placeholderManager.getTopLand(player, 6);
	        	break;
	        /* %konquest_top_land_7% - kingdom name and land in rank 7 */
	        case "top_land_7":
	        	result = placeholderManager.getTopLand(player, 7);
	        	break;
	        /* %konquest_top_land_8% - kingdom name and land in rank 8 */
	        case "top_land_8":
	        	result = placeholderManager.getTopLand(player, 8);
	        	break;
	        /* %konquest_top_land_9% - kingdom name and land in rank 9 */
	        case "top_land_9":
	        	result = placeholderManager.getTopLand(player, 9);
	        	break;
	        /* %konquest_top_land_10% - kingdom name and land in rank 10 */
	        case "top_land_10":
	        	result = placeholderManager.getTopLand(player, 10);
	        	break;
	        default: 
	        	break;
        }

        return result;
    }

}
