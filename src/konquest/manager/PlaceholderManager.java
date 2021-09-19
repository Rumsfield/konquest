package konquest.manager;

import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.model.KonTerritoryType;
import konquest.model.KonTown;
import konquest.utility.MessagePath;

public class PlaceholderManager {

	private PlayerManager playerManager;
	private KingdomManager kingdomManager;
	
	public PlaceholderManager(Konquest konquest) {
		playerManager = konquest.getPlayerManager();
		kingdomManager = konquest.getKingdomManager();
	}
	
	private String boolean2Lang(boolean val) {
 		String result = MessagePath.LABEL_FALSE.getMessage();
 		if(val) {
 			result = MessagePath.LABEL_TRUE.getMessage();
 		}
 		return result;
 	}
	
	public String getKingdom(Player player) {
		String result = "";
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
    	result = offlinePlayer == null ? "" : offlinePlayer.getKingdom().getName();
    	return result;
	}
	
	public String getExile(Player player) {
		String result = "";
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
    	result = offlinePlayer == null ? "" : offlinePlayer.getExileKingdom().getName();
    	return result;
	}
	
	public String getBarbarian(Player player) {
		String result = "";
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
    	result = offlinePlayer == null ? "" : boolean2Lang(offlinePlayer.isBarbarian());
    	return result;
	}
	
	public String getTownsLord(Player player) {
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
    	String list = "";
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
    	return list;
	}
	
	public String getTownsKnight(Player player) {
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
		String list = "";
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
    	return list;
	}
	
	public String getTownsResident(Player player) {
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
		String list = "";
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
    	return list;
	}
	
	public String getTownsAll(Player player) {
		KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
		String list = "";
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
    	return list;
	}
	
	public String getTerritory(Player player) {
		String result = "";
    	KonPlayer onlinePlayer = playerManager.getPlayer(player);
    	if(onlinePlayer != null && player.isOnline()) {
        	if(kingdomManager.isChunkClaimed(player.getLocation())) {
        		result = kingdomManager.getChunkTerritory(player.getLocation()).getTerritoryType().getLabel();
        	} else {
        		result = KonTerritoryType.WILD.getLabel();
        	}
    	}
    	return result;
	}
	
	public String getLand(Player player) {
		String result = "";
    	KonPlayer onlinePlayer = playerManager.getPlayer(player);
    	if(onlinePlayer != null && player.isOnline()) {
        	if(kingdomManager.isChunkClaimed(player.getLocation())) {
        		result = kingdomManager.getChunkTerritory(player.getLocation()).getName();
        	} else {
        		result = KonTerritoryType.WILD.getLabel();
        	}
    	}
    	return result;
	}
	
	public String getClaimed(Player player) {
		String result = "";
    	KonPlayer onlinePlayer = playerManager.getPlayer(player);
    	if(onlinePlayer != null && player.isOnline()) {
    		result = boolean2Lang(kingdomManager.isChunkClaimed(player.getLocation()));
    	}
    	return result;
	}
	
	public String getScore(Player player) {
		String result = "";
    	KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
    	result = offlinePlayer == null ? "" : String.valueOf(kingdomManager.getPlayerScore(offlinePlayer));
    	return result;
	}
	
	public String getPrefix(Player player) {
		String result = "not yet implemented";
    	//TODO: implement this
    	return result;
	}
	
	public String getLordships(Player player) {
		String result = "";
    	KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
    	int count = 0;
    	if(offlinePlayer != null) {
    		for(KonTown town : offlinePlayer.getKingdom().getTowns()) {
    			if(town.isPlayerLord(offlinePlayer.getOfflineBukkitPlayer())) {
    				count++;
    			}
    		}
    	}
    	result = String.valueOf(count);
    	return result;
	}
	
	public String getResidencies(Player player) {
		String result = "";
    	KonOfflinePlayer offlinePlayer = playerManager.getOfflinePlayer(player);
    	int count = 0;
    	if(offlinePlayer != null) {
    		for(KonTown town : offlinePlayer.getKingdom().getTowns()) {
    			if(town.isPlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
    				count++;
    			}
    		}
    	}
    	result = String.valueOf(count);
    	return result;
	}
	
	public String getChat(Player player) {
		String result = "";
		KonPlayer onlinePlayer = playerManager.getPlayer(player);
    	result = onlinePlayer == null ? "" : boolean2Lang(onlinePlayer.isGlobalChat());
    	return result;
	}
	
	public String getCombat(Player player) {
		String result = "";
		KonPlayer onlinePlayer = playerManager.getPlayer(player);
    	result = onlinePlayer == null ? "" : boolean2Lang(onlinePlayer.isCombatTagged());
    	return result;
	}
	
}
