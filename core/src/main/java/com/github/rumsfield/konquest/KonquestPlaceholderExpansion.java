package com.github.rumsfield.konquest;

import org.bukkit.entity.Player;

import com.github.rumsfield.konquest.manager.PlaceholderManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import org.jetbrains.annotations.NotNull;

/**
 * This class will be registered through the register-method in the 
 * plugins onEnable-method.
 */
public class KonquestPlaceholderExpansion extends PlaceholderExpansion implements Relational {

	private final KonquestPlugin plugin;
	private final PlaceholderManager placeholderManager;
	
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
     * Because this is an internal class, this check is not needed,
     * and we can simply return {@code true}
     *
     * @return Always true since it's an internal class.
     */
    @Override
    public boolean canRegister(){
        return true;
    }
    
    /**
     * The name of the person who created this expansion should go here. <br>
     * For convenience do we return the author from the plugin.yml
     * 
     * @return The name of the author as a String.
     */
    @Override
    public @NotNull String getAuthor(){
        return plugin.getDescription().getAuthors().toString();
    }

    /**
     * The placeholder identifier should go here. <br>
     * This is what tells PlaceholderAPI to call our onRequest
     * method to obtain a value if a placeholder starts with our 
     * identifier. <br>
     * The identifier has to be lowercase and can't contain _ or %
     *
     * @return The identifier in {@code %<identifier>_<value>%} as String.
     */
    @Override
    public @NotNull String getIdentifier(){
        return "konquest";
    }

    /**
     * This is the version of the expansion. <br>
     * You don't have to use numbers, since it is set as a String.
	 * <br>
     * For convienience do we return the version from the plugin.yml
     *
     * @return The version as a String.
     */
    @Override
    public @NotNull String getVersion(){
        return plugin.getDescription().getVersion();
    }
    
    /**
     * This is the method called when a placeholder with our identifier 
     * is found and needs a value. <br>
     * We specify the value identifier in this method. <br>
     * Since version 2.9.1 can you use OfflinePlayers in your requests.
     *
     * @param  player
     *         A {@link org.bukkit.entity.Player Player}.
     * @param  identifier
     *         A String containing the identifier/value.
     *
     * @return possibly-null String of the requested identifier.
     */
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier){

        if(player == null){
            return "";
        }
        
        // Provide placeholder value info
        String result = null;
        String identifierLower = identifier.toLowerCase();
        switch(identifierLower) {
			/* %konquest_timer_loot% - Time until monument loot refreshes */
			case "timer_loot":
				result = placeholderManager.getTimerLoot(player);
				break;
			/* %konquest_timer_payment% - Time until kingdom payment refreshes */
			case "timer_payment":
				result = placeholderManager.getTimerPayment(player);
				break;
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
	        /* %konquest_num_town_lord% - number of player's lord-only towns */
	        case "num_town_lord":
	        	result = placeholderManager.getTownsLord(player);
	        	break;
	        /* %konquest_num_town_knight% - number of player's knight-only towns */
	        case "num_town_knight":
	        	result = placeholderManager.getTownsKnight(player);
	        	break;
	        /* %konquest_num_town_resident% - number of player's resident-only towns */
	        case "num_town_resident":
	        	result = placeholderManager.getTownsResident(player);
	        	break;
	        /* %konquest_num_town_all% - number of player's total towns */
	        case "num_town_all":
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
	        /* %konquest_prefix% - player's prefix title */
	        case "prefix":
	        	result = placeholderManager.getPrefix(player);
	        	break;
			/* %konquest_rank% - player's kingdom role */
			case "rank":
				result = placeholderManager.getRank(player);
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
	        /* %konquest_combat_tag% - combat tag string from core config */
	        case "combat_tag":
	        	result = placeholderManager.getCombatTag(player);
	        	break;
	        /* %konquest_top_score_1% - kingdom name and score in rank 1 */
	        case "top_score_1":
	        	result = placeholderManager.getTopScore(1);
	        	break;
	        /* %konquest_top_score_2% - kingdom name and score in rank 2 */
	        case "top_score_2":
	        	result = placeholderManager.getTopScore(2);
	        	break;
	        /* %konquest_top_score_3% - kingdom name and score in rank 3 */
	        case "top_score_3":
	        	result = placeholderManager.getTopScore(3);
	        	break;
	        /* %konquest_top_score_4% - kingdom name and score in rank 4 */
	        case "top_score_4":
	        	result = placeholderManager.getTopScore(4);
	        	break;
	        /* %konquest_top_score_5% - kingdom name and score in rank 5 */
	        case "top_score_5":
	        	result = placeholderManager.getTopScore(5);
	        	break;
	        /* %konquest_top_score_6% - kingdom name and score in rank 6 */
	        case "top_score_6":
	        	result = placeholderManager.getTopScore(6);
	        	break;
	        /* %konquest_top_score_7% - kingdom name and score in rank 7 */
	        case "top_score_7":
	        	result = placeholderManager.getTopScore(7);
	        	break;
	        /* %konquest_top_score_8% - kingdom name and score in rank 8 */
	        case "top_score_8":
	        	result = placeholderManager.getTopScore(8);
	        	break;
	        /* %konquest_top_score_9% - kingdom name and score in rank 9 */
	        case "top_score_9":
	        	result = placeholderManager.getTopScore(9);
	        	break;
	        /* %konquest_top_score_10% - kingdom name and score in rank 10 */
	        case "top_score_10":
	        	result = placeholderManager.getTopScore(10);
	        	break;
	        /* %konquest_top_town_1% - kingdom name and towns in rank 1 */
	        case "top_town_1":
	        	result = placeholderManager.getTopTown(1);
	        	break;
	        /* %konquest_top_town_2% - kingdom name and towns in rank 2 */
	        case "top_town_2":
	        	result = placeholderManager.getTopTown(2);
	        	break;
	        /* %konquest_top_town_3% - kingdom name and towns in rank 3 */
	        case "top_town_3":
	        	result = placeholderManager.getTopTown(3);
	        	break;
	        /* %konquest_top_town_4% - kingdom name and towns in rank 4 */
	        case "top_town_4":
	        	result = placeholderManager.getTopTown(4);
	        	break;
	        /* %konquest_top_town_5% - kingdom name and towns in rank 5 */
	        case "top_town_5":
	        	result = placeholderManager.getTopTown(5);
	        	break;
	        /* %konquest_top_town_6% - kingdom name and towns in rank 6 */
	        case "top_town_6":
	        	result = placeholderManager.getTopTown(6);
	        	break;
	        /* %konquest_top_town_7% - kingdom name and towns in rank 7 */
	        case "top_town_7":
	        	result = placeholderManager.getTopTown(7);
	        	break;
	        /* %konquest_top_town_8% - kingdom name and towns in rank 8 */
	        case "top_town_8":
	        	result = placeholderManager.getTopTown(8);
	        	break;
	        /* %konquest_top_town_9% - kingdom name and towns in rank 9 */
	        case "top_town_9":
	        	result = placeholderManager.getTopTown(9);
	        	break;
	        /* %konquest_top_town_10% - kingdom name and towns in rank 10 */
	        case "top_town_10":
	        	result = placeholderManager.getTopTown(10);
	        	break;
	        /* %konquest_top_land_1% - kingdom name and land in rank 1 */
	        case "top_land_1":
	        	result = placeholderManager.getTopLand(1);
	        	break;
	        /* %konquest_top_land_2% - kingdom name and land in rank 2 */
	        case "top_land_2":
	        	result = placeholderManager.getTopLand(2);
	        	break;
	        /* %konquest_top_land_3% - kingdom name and land in rank 3 */
	        case "top_land_3":
	        	result = placeholderManager.getTopLand(3);
	        	break;
	        /* %konquest_top_land_4% - kingdom name and land in rank 4 */
	        case "top_land_4":
	        	result = placeholderManager.getTopLand(4);
	        	break;
	        /* %konquest_top_land_5% - kingdom name and land in rank 5 */
	        case "top_land_5":
	        	result = placeholderManager.getTopLand(5);
	        	break;
	        /* %konquest_top_land_6% - kingdom name and land in rank 6 */
	        case "top_land_6":
	        	result = placeholderManager.getTopLand(6);
	        	break;
	        /* %konquest_top_land_7% - kingdom name and land in rank 7 */
	        case "top_land_7":
	        	result = placeholderManager.getTopLand(7);
	        	break;
	        /* %konquest_top_land_8% - kingdom name and land in rank 8 */
	        case "top_land_8":
	        	result = placeholderManager.getTopLand(8);
	        	break;
	        /* %konquest_top_land_9% - kingdom name and land in rank 9 */
	        case "top_land_9":
	        	result = placeholderManager.getTopLand(9);
	        	break;
	        /* %konquest_top_land_10% - kingdom name and land in rank 10 */
	        case "top_land_10":
	        	result = placeholderManager.getTopLand(10);
	        	break;
			/* %konquest_kingdom_players% - the player's current kingdom's total player count */
			case "kingdom_players":
				result = placeholderManager.getPlayerKingdomPlayers(player);
				break;
			/* %konquest_kingdom_online% - the player's current kingdom's online player count */
			case "kingdom_online":
				result = placeholderManager.getPlayerKingdomOnline(player);
				break;
			/* %konquest_kingdom_towns% - the player's current kingdom's town count */
			case "kingdom_towns":
				result = placeholderManager.getPlayerKingdomTowns(player);
				break;
			/* %konquest_kingdom_land% - the player's current kingdom's claimed land count */
			case "kingdom_land":
				result = placeholderManager.getPlayerKingdomLand(player);
				break;
			/* %konquest_kingdom_favor% - the player's current kingdom's total favor */
			case "kingdom_favor":
				result = placeholderManager.getPlayerKingdomFavor(player);
				break;
			/* %konquest_kingdom_score% - the player's current kingdom's score */
			case "kingdom_score":
				result = placeholderManager.getPlayerKingdomScore(player);
				break;
	        default: 
	        	// Check for kingdom-specific placeholders

	        	/* %konquest_players_<kingdom>% */
				if(identifierLower.matches("^players_.+$")) {
	        		try {
	        			String kingdomName = identifierLower.substring(8);
	        			result = placeholderManager.getKingdomPlayers(kingdomName);
	        		} catch(IndexOutOfBoundsException ignored) {}
	        		
        		/* %konquest_online_<kingdom>% */
	        	} else if(identifierLower.matches("^online_.+$")) {
	        		try {
	        			String kingdomName = identifierLower.substring(7);
	        			result = placeholderManager.getKingdomOnline(kingdomName);
	        		} catch(IndexOutOfBoundsException ignored) {}
	        		
        		/* %konquest_towns_<kingdom>% */
	        	} else if(identifierLower.matches("^towns_.+$")) {
	        		try {
	        			String kingdomName = identifierLower.substring(6);
	        			result = placeholderManager.getKingdomTowns(kingdomName);
	        		} catch(IndexOutOfBoundsException ignored) {}
	        		
        		/* %konquest_land_<kingdom>% */
	        	} else if(identifierLower.matches("^land_.+$")) {
	        		try {
	        			String kingdomName = identifierLower.substring(5);
	        			result = placeholderManager.getKingdomLand(kingdomName);
	        		} catch(IndexOutOfBoundsException ignored) {}
	        		
        		/* %konquest_favor_<kingdom>% */
	        	} else if(identifierLower.matches("^favor_.+$")) {
	        		try {
	        			String kingdomName = identifierLower.substring(6);
	        			result = placeholderManager.getKingdomFavor(kingdomName);
	        		} catch(IndexOutOfBoundsException ignored) {}
	        		
        		/* %konquest_score_<kingdom>% */
	        	} else if(identifierLower.matches("^score_.+$")) {
	        		try {
	        			String kingdomName = identifierLower.substring(6);
	        			result = placeholderManager.getKingdomScore(kingdomName);
	        		} catch(IndexOutOfBoundsException ignored) {}
	        		
	        	}
	        	break;
        }

        return result;
    }

	@Override
	public String onPlaceholderRequest(Player playerOne, Player playerTwo, String identifier) {
		if(playerOne == null || playerTwo == null) {
			return "";
		}
		
		// Provide placeholder value info
        String result = null;
        switch(identifier.toLowerCase()) {
	        /* %rel_konquest_relation% - playerOne's relationship to playerTwo */
        	case "relation":
        		result = placeholderManager.getRelation(playerOne, playerTwo);
	        	break;
	        /* %rel_konquest_relation_color% - playerOne's relationship color to playerTwo */
        	case "relation_color":
        		result = placeholderManager.getRelationPrimaryColor(playerOne, playerTwo);
	        	break;
			/* %rel_konquest_relation2_color% - playerOne's secondary relationship color to playerTwo */
			case "relation2_color":
				result = placeholderManager.getRelationSecondaryColor(playerOne, playerTwo);
				break;
			/* %rel_konquest_kingdom_webcolor% - playerTwo's kingdom web color */
			case "kingdom_webcolor":
				result = placeholderManager.getRelationKingdomWebColor(playerTwo);
				break;
        	default: 
	        	break;
        }
        
		return result;
	}

}
