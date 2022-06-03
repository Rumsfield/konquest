package konquest.manager;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.data.type.Bed;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.api.event.camp.KonquestCampCreateEvent;
import konquest.api.manager.KonquestCampManager;
import konquest.api.model.KonquestCamp;
import konquest.api.model.KonquestOfflinePlayer;
import konquest.model.KonCamp;
import konquest.model.KonCampGroup;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class CampManager implements KonquestCampManager {

	private Konquest konquest;
	private KingdomManager kingdomManager;
	private HashMap<String,KonCamp> barbarianCamps; // player uuids to camps
	//private HashSet<KonCampGroup> barbarianGroups; // all camp groups
	private HashMap<KonCamp,KonCampGroup> groupMap; // camps to groups
	private boolean isClanEnabled;
	
	public CampManager(Konquest konquest) {
		this.konquest = konquest;
		this.kingdomManager = konquest.getKingdomManager();
		this.barbarianCamps = new HashMap<String, KonCamp>();
		//this.barbarianGroups = new HashSet<KonCampGroup>();
		this.groupMap = new HashMap<KonCamp,KonCampGroup>();
		this.isClanEnabled = false;
	}
	
	// intended to be called after database has connected and loaded player tables
	public void initCamps() {
		isClanEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.camps.clan_enable",false);
		loadCamps();
		refreshGroups();
		ChatUtil.printDebug("Loaded camps and groups");
	}
	
	public boolean isCampSet(KonquestOfflinePlayer player) {
		String uuid = player.getOfflineBukkitPlayer().getUniqueId().toString();
		return player.isBarbarian() && barbarianCamps.containsKey(uuid);
	}
	
	public KonCamp getCamp(KonquestOfflinePlayer player) {
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
	 * 					4 = camps are disabled
	 *                  5 = world is invalid
	 */
	public int addCamp(Location loc, KonOfflinePlayer player) {
		boolean enable = konquest.getConfigManager().getConfig("core").getBoolean("core.camps.enable",true);
		if(!enable) {
			ChatUtil.printDebug("Failed to add camp, feature disabled!");
			return 4;
		}
		if(!konquest.isWorldValid(loc)) {
			ChatUtil.printDebug("Failed to add camp, location is in invalid world");
			return 5;
		}
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
			// Refresh groups
			refreshGroups();
			if(groupMap.containsKey(newCamp)) {
				Konquest.playCampGroupSound(loc);
				// Notify group
				for(KonCamp groupCamp : groupMap.get(newCamp).getCamps()) {
					if(groupCamp.isOwnerOnline() && groupCamp.getOwner() instanceof Player) {
						Player bukkitPlayer = (Player)groupCamp.getOwner();
						ChatUtil.sendNotice(bukkitPlayer, MessagePath.PROTECTION_NOTICE_CAMP_CLAN_ADD.getMessage(newCamp.getName()));
					}
				}
			}
			konquest.getMapHandler().drawDynmapUpdateTerritory(newCamp);
		} else {
			return 2;
		}
		return 0;
	}
	
	public int addCampForPlayer(Location loc, KonPlayer player) {
		int result = addCamp(loc, player);
		if(result == 0) {
			KonCamp newCamp = getCamp(player);
			if(newCamp != null && player != null) {
				// Fire event
				KonquestCampCreateEvent invokeEvent = new KonquestCampCreateEvent(konquest, newCamp, player);
				Konquest.callKonquestEvent(invokeEvent);
			}
		}
		return result;
	}
	
	public boolean removeCamp(KonquestOfflinePlayer player) {
		String uuid = player.getOfflineBukkitPlayer().getUniqueId().toString();
		return removeCamp(uuid);
	}
	
	public boolean removeCamp(String uuid) {
		if(barbarianCamps.containsKey(uuid)) {
			ArrayList<Point> campPoints = new ArrayList<Point>();
			campPoints.addAll(barbarianCamps.get(uuid).getChunkList().keySet());
			konquest.getIntegrationManager().getQuickShop().deleteShopsInPoints(campPoints,barbarianCamps.get(uuid).getWorld());
			KonCamp removedCamp = barbarianCamps.remove(uuid);
			removedCamp.removeAllBarPlayers();
			// Ensure bed is broken
			if(removedCamp.getBedLocation().getBlock().getBlockData() instanceof Bed) {
				removedCamp.getBedLocation().getBlock().breakNaturally();
			}
			//update the chunk cache, remove all points from primary world
			kingdomManager.removeAllTerritory(removedCamp.getWorld(),removedCamp.getChunkList().keySet());
			// Refresh groups
			Collection<KonCamp> groupSet = new ArrayList<KonCamp>();
			if(groupMap.containsKey(removedCamp)) {
				groupSet = groupMap.get(removedCamp).getCamps();
			}
			refreshGroups();
			// Notify group
			for(KonCamp groupCamp : groupSet) {
				if(groupCamp.isOwnerOnline() && groupCamp.getOwner() instanceof Player) {
					Player bukkitPlayer = (Player)groupCamp.getOwner();
					ChatUtil.sendNotice(bukkitPlayer, MessagePath.PROTECTION_NOTICE_CAMP_CLAN_REMOVE.getMessage(removedCamp.getName()));
				}
			}
			konquest.getMapHandler().drawDynmapRemoveTerritory(removedCamp);
			removedCamp = null;
		} else {
			ChatUtil.printDebug("Failed to remove camp for missing UUID "+uuid);
			return false;
		}
		ChatUtil.printDebug("Successfully removed camp for UUID "+uuid);
		return true;
	}
	
	public boolean isCampGrouped(KonquestCamp camp) {
		return groupMap.containsKey(camp);
	}
	
	public KonCampGroup getCampGroup(KonquestCamp camp) {
		return groupMap.get(camp);
	}
	
	public boolean isCampGroupsEnabled() {
		return isClanEnabled;
	}
	
	/**
	 * Attempts to remove a camp from a group
	 * @param camp - a removed camp
	 * @return true when camp has been removed from a group
	 */
	/*
	private boolean removeUpdateGroup(KonCamp camp) {
		boolean result = false;
		// check for existing group
		if(!groupMap.containsKey(camp)) {
			result = true;
		} else {
			// Camp belongs to a group
			KonCampGroup mainGroup = groupMap.get(camp);
			// Search surroundings for all adjacent camps
			HashSet<KonCamp> adjCamps = new HashSet<KonCamp>();
			Location center = camp.getCenterLoc();
			int radius = konquest.getConfigManager().getConfig("core").getInt("core.camps.init_radius");
			for(Point point : konquest.getBorderPoints(center, radius+1)) {
				if(kingdomManager.isChunkClaimed(point,center.getWorld()) && kingdomManager.getChunkTerritory(point,center.getWorld()) instanceof KonCamp) {
					KonCamp adjCamp = (KonCamp)kingdomManager.getChunkTerritory(point,center.getWorld());
					adjCamps.add(adjCamp);
				}
			}
			// Remove camp from main group
			mainGroup.removeCamp(camp);
			groupMap.remove(camp);
			// Update groups of remaining adjacent camps
			
			
		}
		return result;
	}
	*/
	/**
	 * Attempts to add camp to a group
	 * @param camp - a new camp
	 * @return true when camp has been added to a group
	 */
	/*
	private boolean addUpdateGroup(KonCamp camp) {
		boolean result = false;
		// check for existing group
		if(groupMap.containsKey(camp)) {
			result = true;
		} else {
			// Camp has no group
			// Search surroundings for all adjacent camps
			HashSet<KonCamp> adjCamps = new HashSet<KonCamp>();
			Location center = camp.getCenterLoc();
			int radius = konquest.getConfigManager().getConfig("core").getInt("core.camps.init_radius");
			for(Point point : konquest.getBorderPoints(center, radius+1)) {
				if(kingdomManager.isChunkClaimed(point,center.getWorld()) && kingdomManager.getChunkTerritory(point,center.getWorld()) instanceof KonCamp) {
					KonCamp adjCamp = (KonCamp)kingdomManager.getChunkTerritory(point,center.getWorld());
					adjCamps.add(adjCamp);
				}
			}
			// Find any existing groups
			HashSet<KonCampGroup> adjGroups = new HashSet<KonCampGroup>();
			for(KonCamp adjCamp : adjCamps) {
				if(groupMap.containsKey(adjCamp)) {
					adjGroups.add(groupMap.get(adjCamp));
				}
			}
			// Attempt to form groups
			if(adjGroups.isEmpty()) {
				// There are no adjacent groups, try to make one
				if(!adjCamps.isEmpty()) {
					// Make a new group with the adjacent camp(s)
					KonCampGroup campGroup = new KonCampGroup();
					campGroup.addCamp(camp);
					groupMap.put(camp, campGroup);
					for(KonCamp adjCamp : adjCamps) {
						campGroup.addCamp(adjCamp);
						groupMap.put(adjCamp, campGroup);
					}
					barbarianGroups.add(campGroup);
					result = true;
				}
			} else {
				// There are other adjacent group(s)
				Iterator<KonCampGroup> groupIter = adjGroups.iterator();
				if(groupIter.hasNext()) {
					KonCampGroup mainGroup = groupIter.next(); // choose arbitrary group as main
					// Add camp and all adjacent camps to main group
					mainGroup.addCamp(camp);
					groupMap.put(camp, mainGroup);
					for(KonCamp adjCamp : adjCamps) {
						mainGroup.addCamp(adjCamp);
						groupMap.put(adjCamp, mainGroup);
					}
					// Merge other group(s) into main and prune
					while(groupIter.hasNext()) {
						KonCampGroup mergeGroup = groupIter.next();
						mainGroup.mergeGroup(mergeGroup);
						mergeGroup.clearCamps();
						barbarianGroups.remove(mergeGroup);
					}
					result = true;
				}
			}
		}
		return result;
	}
	*/
	private void refreshGroups() {
		groupMap.clear();
		if(!isClanEnabled) {
			return;
		}
		int totalGroups = 0;
		int radius = konquest.getConfigManager().getConfig("core").getInt("core.camps.init_radius");
		Location center = null;
		// Evaluate each camp for adjacent camps, creating groups as necessary
		for(KonCamp currCamp : barbarianCamps.values()) {
			// Verify camp is not already in a group
			if(!groupMap.containsKey(currCamp)) {
				center = currCamp.getCenterLoc();
				// Search surroundings for all adjacent camps
				HashSet<KonCamp> adjCamps = new HashSet<KonCamp>();
				for(Point point : konquest.getBorderPoints(center, radius+1)) {
					if(kingdomManager.isChunkClaimed(point,center.getWorld()) && kingdomManager.getChunkTerritory(point,center.getWorld()) instanceof KonCamp) {
						KonCamp adjCamp = (KonCamp)kingdomManager.getChunkTerritory(point,center.getWorld());
						adjCamps.add(adjCamp);
					}
				}
				// Find any existing groups
				HashSet<KonCampGroup> adjGroups = new HashSet<KonCampGroup>();
				for(KonCamp adjCamp : adjCamps) {
					if(groupMap.containsKey(adjCamp)) {
						adjGroups.add(groupMap.get(adjCamp));
					}
				}
				// Attempt to form groups
				if(adjGroups.isEmpty()) {
					// There are no adjacent groups, try to make one
					if(!adjCamps.isEmpty()) {
						// Make a new group with the adjacent camp(s)
						KonCampGroup campGroup = new KonCampGroup();
						campGroup.addCamp(currCamp);
						groupMap.put(currCamp, campGroup);
						for(KonCamp adjCamp : adjCamps) {
							campGroup.addCamp(adjCamp);
							groupMap.put(adjCamp, campGroup);
						}
						//barbarianGroups.add(campGroup);
						totalGroups++;
					}
				} else {
					// There are other adjacent group(s)
					Iterator<KonCampGroup> groupIter = adjGroups.iterator();
					if(groupIter.hasNext()) {
						KonCampGroup mainGroup = groupIter.next(); // choose arbitrary group as main
						// Add camp and all adjacent camps to main group
						mainGroup.addCamp(currCamp);
						groupMap.put(currCamp, mainGroup);
						for(KonCamp adjCamp : adjCamps) {
							mainGroup.addCamp(adjCamp);
							groupMap.put(adjCamp, mainGroup);
						}
						// Merge other group(s) into main and prune
						while(groupIter.hasNext()) {
							KonCampGroup mergeGroup = groupIter.next();
							mainGroup.mergeGroup(mergeGroup);
							mergeGroup.clearCamps();
							//barbarianGroups.remove(mergeGroup);
							totalGroups--;
						}
					}
				}
			}
			/*
			// Search surrounding chunks
			KonCampGroup campGroup = null;
			int radius = konquest.getConfigManager().getConfig("core").getInt("core.camps.init_radius");
			for(Point point : konquest.getBorderPoints(currCenter, radius+1)) {
				if(kingdomManager.isChunkClaimed(point,currCenter.getWorld()) && kingdomManager.getChunkTerritory(point,currCenter.getWorld()) instanceof KonCamp) {
					KonCamp adjCamp = (KonCamp)kingdomManager.getChunkTerritory(point,currCenter.getWorld());
					// Found adjacent camp, decide to make new group or add
					if(groupMap.containsKey(currCamp)) {
						// current camp is already in a group
						campGroup = groupMap.get(currCamp);
						campGroup.addCamp(adjCamp);
						groupMap.put(adjCamp, campGroup);
					} else if(groupMap.containsKey(adjCamp)) {
						// adjacent camp is already in a group
						campGroup = groupMap.get(adjCamp);
						campGroup.addCamp(currCamp);
						groupMap.put(currCamp, campGroup);
					} else {
						// neither camps are in a group, create new one
						campGroup = new KonCampGroup();
						campGroup.addCamp(adjCamp);
						groupMap.put(adjCamp, campGroup);
						campGroup.addCamp(currCamp);
						groupMap.put(currCamp, campGroup);
						totalGroups++;
					}
				}
			}
			*/
		}
		ChatUtil.printDebug("Refreshed "+totalGroups+" camp groups");
	}
	
	private void loadCamps() {
		boolean enable = konquest.getConfigManager().getConfig("core").getBoolean("core.camps.enable",true);
		if(!enable) {
			ChatUtil.printConsoleAlert("Disabled barbarian camps");
			return;
		}
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
        		if(konquest.getPlayerManager().isOfflinePlayer(offlineBukkitPlayer)) {
        			KonOfflinePlayer offlinePlayer = konquest.getPlayerManager().getOfflinePlayer(offlineBukkitPlayer);
        			// Add stored camp
            		ConfigurationSection playerCampSection = campsSection.getConfigurationSection(uuid);
            		worldName = playerCampSection.getString("world",defaultWorldName);
            		sectionList = playerCampSection.getDoubleList("center");
            		x = sectionList.get(0);
            		y = sectionList.get(1);
            		z = sectionList.get(2);
            		World world = Bukkit.getWorld(worldName);
                	if(world != null) {
                		// Create the camp
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
                		String message = "Failed to load camp for player "+offlineBukkitPlayer.getName()+" in an unloaded world, "+worldName+". Check plugin load order.";
            			ChatUtil.printConsoleError(message);
            			konquest.opStatusMessages.add(message);
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
			campSection.set("center", new int[] {(int) camp.getCenterLoc().getBlockX(),
					 							 (int) camp.getCenterLoc().getBlockY(),
					 							 (int) camp.getCenterLoc().getBlockZ()});
	        campSection.set("chunks", konquest.formatPointsToString(camp.getChunkList().keySet()));
		}
	}
}
