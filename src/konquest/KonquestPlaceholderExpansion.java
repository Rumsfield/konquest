package konquest;

import org.bukkit.entity.Player;

import konquest.manager.KingdomManager;
import konquest.manager.PlayerManager;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.model.KonTerritoryType;
import konquest.model.KonTown;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

/**
 * This class will be registered through the register-method in the 
 * plugins onEnable-method.
 */
public class KonquestPlaceholderExpansion extends PlaceholderExpansion {

	private KonquestPlugin plugin;
	
	private PlayerManager playerManager;
	private KingdomManager kingdomManager;
	
	public KonquestPlaceholderExpansion(KonquestPlugin plugin) {
		this.plugin = plugin;
		playerManager = plugin.getKonquestInstance().getPlayerManager();
		kingdomManager = plugin.getKonquestInstance().getKingdomManager();
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
        
        KonOfflinePlayer offlinePlayer;
        KonPlayer onlinePlayer;
        String list;
        int count;
        
        // Provide placeholder value info
        String result = null;
        switch(identifier.toLowerCase()) {
	        /* %konquest_kingdom% - player's kingdom name */
        	case "kingdom":
        		offlinePlayer = playerManager.getOfflinePlayer(player);
	        	result = offlinePlayer == null ? "" : offlinePlayer.getKingdom().getName();
	        	break;
	        /* %konquest_exile% - player's exile kingdom name */
        	case "exile":
        		offlinePlayer = playerManager.getOfflinePlayer(player);
	        	result = offlinePlayer == null ? "" : offlinePlayer.getExileKingdom().getName();
	        	break;
	        /* %konquest_barbarian% - true if player is barbarian, else false */
	        case "barbarian":
	        	offlinePlayer = playerManager.getOfflinePlayer(player);
	        	result = offlinePlayer == null ? "" : String.valueOf(offlinePlayer.isBarbarian());
	        	break;
	        /* %konquest_towns_lord% - comma-separated list of player's lord only towns */
	        case "towns_lord":
	        	offlinePlayer = playerManager.getOfflinePlayer(player);
	        	list = "";
	        	if(offlinePlayer != null) {
		    		for(KonTown town : offlinePlayer.getKingdom().getTowns()) {
		    			if(town.isPlayerLord(offlinePlayer.getOfflineBukkitPlayer())) {
		    				list = list + town.getName() + ",";
		    			}
		    		}
		    		if(list.length() > 1) {
		    			list = list.substring(0,list.length()-1);
	    			}
	        	}
	    		result = list;
	        	break;
	        /* %konquest_towns_knight% - comma-separated list of player's knight only towns */
	        case "towns_knight":
	        	offlinePlayer = playerManager.getOfflinePlayer(player);
	        	list = "";
	        	if(offlinePlayer != null) {
		    		for(KonTown town : offlinePlayer.getKingdom().getTowns()) {
		    			if(town.isPlayerElite(offlinePlayer.getOfflineBukkitPlayer()) &&
		    					!town.isPlayerLord(offlinePlayer.getOfflineBukkitPlayer())) {
		    				list = list + town.getName() + ",";
		    			}
		    		}
		    		if(list.length() > 1) {
		    			list = list.substring(0,list.length()-1);
	    			}
	        	}
	    		result = list;
	        	break;
	        /* %konquest_towns_resident% - comma-separated list of player's resident only towns */
	        case "towns_resident":
	        	offlinePlayer = playerManager.getOfflinePlayer(player);
	        	list = "";
	        	if(offlinePlayer != null) {
		    		for(KonTown town : offlinePlayer.getKingdom().getTowns()) {
		    			if(town.isPlayerResident(offlinePlayer.getOfflineBukkitPlayer()) &&
		    					!town.isPlayerElite(offlinePlayer.getOfflineBukkitPlayer())) {
		    				list = list + town.getName() + ",";
		    			}
		    		}
		    		if(list.length() > 1) {
		    			list = list.substring(0,list.length()-1);
	    			}
	        	}
	    		result = list;
	        	break;
	        /* %konquest_towns_all% - comma-separated list of player's all towns */
	        case "towns_all":
	        	offlinePlayer = playerManager.getOfflinePlayer(player);
	        	list = "";
	        	if(offlinePlayer != null) {
		    		for(KonTown town : offlinePlayer.getKingdom().getTowns()) {
		    			if(town.isPlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
		    				list = list + town.getName() + ",";
		    			}
		    		}
		    		if(list.length() > 1) {
		    			list = list.substring(0,list.length()-1);
	    			}
	        	}
	    		result = list;
	        	break;
	        /* %konquest_territory% - player's current location territory type */
	        case "territory":
	        	result = "";
	        	onlinePlayer = playerManager.getPlayer(player);
	        	if(onlinePlayer != null && player.isOnline()) {
		        	if(kingdomManager.isChunkClaimed(player.getLocation())) {
		        		result = kingdomManager.getChunkTerritory(player.getLocation()).getTerritoryType().name();
		        	} else {
		        		result = KonTerritoryType.WILD.name();
		        	}
	        	}
	        	break;
	        /* %konquest_land% - player's current location territory name */
	        case "land":
	        	result = "";
	        	onlinePlayer = playerManager.getPlayer(player);
	        	if(onlinePlayer != null && player.isOnline()) {
		        	if(kingdomManager.isChunkClaimed(player.getLocation())) {
		        		result = kingdomManager.getChunkTerritory(player.getLocation()).getName();
		        	} else {
		        		result = KonTerritoryType.WILD.name();
		        	}
	        	}
	        	break;
	        /* %konquest_claimed% - true if the player's current location is claimed, else false */
	        case "claimed":
	        	result = "";
	        	onlinePlayer = playerManager.getPlayer(player);
	        	if(onlinePlayer != null && player.isOnline()) {
	        		result = String.valueOf(kingdomManager.isChunkClaimed(player.getLocation()));
	        	}
	        	break;
	        /* %konquest_score% - player's score value */
	        case "score":
	        	offlinePlayer = playerManager.getOfflinePlayer(player);
	        	result = offlinePlayer == null ? "" : String.valueOf(kingdomManager.getPlayerScore(offlinePlayer));
	        	break;
	        /* %konquest_prefix% - player's prefix title */
	        case "prefix":
	        	result = "not yet implemented";
	        	break;
	        /* %konquest_lordships% - number of player's lordships */
	        case "lordships":
	        	offlinePlayer = playerManager.getOfflinePlayer(player);
	        	count = 0;
	        	if(offlinePlayer != null) {
		    		for(KonTown town : offlinePlayer.getKingdom().getTowns()) {
		    			if(town.isPlayerLord(offlinePlayer.getOfflineBukkitPlayer())) {
		    				count++;
		    			}
		    		}
	        	}
	        	result = String.valueOf(count);
	        	break;
	        /* %konquest_residencies% - number of player's total residencies, including lordships */
	        case "residencies":
	        	offlinePlayer = playerManager.getOfflinePlayer(player);
	        	count = 0;
	        	if(offlinePlayer != null) {
		    		for(KonTown town : offlinePlayer.getKingdom().getTowns()) {
		    			if(town.isPlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
		    				count++;
		    			}
		    		}
	        	}
	        	result = String.valueOf(count);
	        	break;
	        /* %konquest_chat% - true if player is using global chat, else false */
	        case "chat":
	        	onlinePlayer = playerManager.getPlayer(player);
	        	result = onlinePlayer == null ? "" : String.valueOf(onlinePlayer.isGlobalChat());
	        	break;
	        /* %konquest_combat% - true if player is combat tagged, else false */
	        case "combat":
	        	onlinePlayer = playerManager.getPlayer(player);
	        	result = onlinePlayer == null ? "" : String.valueOf(onlinePlayer.isCombatTagged());
	        	break;
	        default: 
	        	break;
        }

        return result;
    }

}
