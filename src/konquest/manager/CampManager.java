package konquest.manager;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import konquest.Konquest;
import konquest.model.KonCamp;
import konquest.model.KonOfflinePlayer;
import konquest.utility.ChatUtil;

public class CampManager {

	private Konquest konquest;
	private KingdomManager kingdomManager;
	private HashMap<String,KonCamp> barbarianCamps;
	
	public CampManager(Konquest konquest) {
		this.konquest = konquest;
		this.kingdomManager = konquest.getKingdomManager();
		this.barbarianCamps = new HashMap<String, KonCamp>();
	}
	
	public void initCamps() {
		loadCamps();
		//updateTerritoryCache(); // function moved into loadCamps()
		ChatUtil.printDebug("Loaded camps");
	}
	
	public boolean isCampSet(KonOfflinePlayer player) {
		String uuid = player.getOfflineBukkitPlayer().getUniqueId().toString();
		return player.isBarbarian() && barbarianCamps.containsKey(uuid);
	}
	
	public KonCamp getCamp(KonOfflinePlayer player) {
		String uuid = player.getOfflineBukkitPlayer().getUniqueId().toString();
		return barbarianCamps.get(uuid);
	}
	
	public ArrayList<KonCamp> getCamps() {
		ArrayList<KonCamp> camps = new ArrayList<KonCamp>(barbarianCamps.values());
		return camps;
	}
	
	/**
	 * addCamp - primary method for adding a camp for a barbarian
	 * @param loc
	 * @param player
	 * @return status 	0 = success
	 * 					1 = camp init claims overlap with existing territory
	 * 					2 = camp already exists for player
	 * 					3 = player is not a barbarian
	 */
	public int addCamp(Location loc, KonOfflinePlayer player) {
		String uuid = player.getOfflineBukkitPlayer().getUniqueId().toString();
		//ChatUtil.printDebug("Attempting to add new Camp for player "+player.getOfflineBukkitPlayer().getName()+" "+uuid);
		if(!player.isBarbarian()) {
			ChatUtil.printDebug("Failed to add camp, player "+player.getOfflineBukkitPlayer().getName()+" "+uuid+" is not a barbarian!");
			return 3;
		}
		if(!barbarianCamps.containsKey(uuid)) {
			// Verify no overlapping init chunks
			int radius = konquest.getConfigManager().getConfig("core").getInt("core.camps.init_radius");
			//ChatUtil.printDebug("Checking for chunk conflicts with radius "+radius);
			World addWorld = loc.getWorld();
			for(Point point : konquest.getAreaPoints(loc, radius)) {
				if(kingdomManager.isChunkClaimed(point,addWorld)) {
					ChatUtil.printDebug("Found a chunk conflict in camp placement for player "+player.getOfflineBukkitPlayer().getName()+" "+uuid);
					return 1;
				}
			}
			// Attempt to add the camp
			KonCamp newCamp = new KonCamp(loc,player.getOfflineBukkitPlayer(),kingdomManager.getBarbarians(),konquest);
			barbarianCamps.put(uuid,newCamp);
			newCamp.initClaim();
			// Update bar players
			newCamp.updateBarPlayers();
			//update the chunk cache, add points to primary world cache
			kingdomManager.addAllTerritory(loc.getWorld(),newCamp.getChunkList());
			konquest.getMapHandler().drawDynmapUpdateTerritory(newCamp);
		} else {
			return 2;
		}
		return 0;
	}
	
	public boolean removeCamp(KonOfflinePlayer player) {
		String uuid = player.getOfflineBukkitPlayer().getUniqueId().toString();
		return removeCamp(uuid);
	}
	
	public boolean removeCamp(String uuid) {
		if(barbarianCamps.containsKey(uuid)) {
			ArrayList<Point> campPoints = new ArrayList<Point>();
			campPoints.addAll(barbarianCamps.get(uuid).getChunkList().keySet());
			konquest.getIntegrationManager().deleteShopsInPoints(campPoints,barbarianCamps.get(uuid).getWorld());
			KonCamp removedCamp = barbarianCamps.remove(uuid);
			removedCamp.removeAllBarPlayers();
			//update the chunk cache, remove all points from primary world
			kingdomManager.removeAllTerritory(removedCamp.getWorld(),removedCamp.getChunkList().keySet());
			konquest.getMapHandler().drawDynmapRemoveTerritory(removedCamp);
			removedCamp = null;
		} else {
			ChatUtil.printDebug("Failed to remove camp for missing UUID "+uuid);
			return false;
		}
		ChatUtil.printDebug("Successfully removed camp for UUID "+uuid);
		return true;
	}
	
	private void loadCamps() {
    	FileConfiguration campsConfig = konquest.getConfigManager().getConfig("camps");
        if (campsConfig.get("camps") == null) {
        	ChatUtil.printDebug("There is no camps section in camps.yml");
            return;
        }
        ConfigurationSection campsSection = campsConfig.getConfigurationSection("camps");
        Set<String> playerSet = campsSection.getKeys(false);
        double x,y,z;
        List<Double> sectionList;
        String worldName;
        String defaultWorldName = konquest.getConfigManager().getConfig("core").getString("core.world_name","world");
        int totalCamps = 0;
        for(String uuid : playerSet) {
        	if(campsSection.contains(uuid)) {
        		OfflinePlayer offlineBukkitPlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        		//ChatUtil.printDebug("Adding camp for player "+offlineBukkitPlayer.getName());
        		if(konquest.getPlayerManager().isPlayerExist(offlineBukkitPlayer)) {
        			KonOfflinePlayer offlinePlayer = konquest.getPlayerManager().getOfflinePlayer(offlineBukkitPlayer);
        			// Add stored camp
            		ConfigurationSection playerCampSection = campsSection.getConfigurationSection(uuid);
            		worldName = playerCampSection.getString("world",defaultWorldName);
            		sectionList = playerCampSection.getDoubleList("center");
            		x = sectionList.get(0);
            		y = sectionList.get(1);
            		z = sectionList.get(2);
            		World world = Bukkit.getWorld(worldName);
                	Location camp_center = new Location(world,x,y,z);
            		int status = addCamp(camp_center, offlinePlayer);
            		if(status == 0) {
	            		getCamp(offlinePlayer).addPoints(konquest.formatStringToPoints(playerCampSection.getString("chunks")));
	            		kingdomManager.addAllTerritory(world,getCamp(offlinePlayer).getChunkList());
	            		totalCamps++;
            		} else {
            			ChatUtil.printDebug("Failed to add camp for player "+offlineBukkitPlayer.getName()+", error code: "+status);
            		}
        		} else {
        			if(offlineBukkitPlayer != null) {
        				ChatUtil.printDebug("Failed to find player "+offlineBukkitPlayer.getName()+" when adding their camp");
        			} else {
        				ChatUtil.printDebug("Failed to find null player when adding their camp");
        			}
        			
        		}
        	}
        }
        ChatUtil.printDebug("Updated all camps from camps.yml, total "+totalCamps);
    }
	
	public void saveCamps() {
		FileConfiguration campsConfig = konquest.getConfigManager().getConfig("camps");
		campsConfig.set("camps", null); // reset camps config
		ConfigurationSection root = campsConfig.createSection("camps");
		for(String uuid : barbarianCamps.keySet()) {
			KonCamp camp = barbarianCamps.get(uuid);
			ConfigurationSection campSection = root.createSection(uuid);
			campSection.set("world", camp.getWorld().getName());
			campSection.set("center", new int[] {(int) camp.getCenterLoc().getX(),
					 							 (int) camp.getCenterLoc().getY(),
					 							 (int) camp.getCenterLoc().getZ()});
	        campSection.set("chunks", konquest.formatPointsToString(camp.getChunkList().keySet()));
		}
	}
}
