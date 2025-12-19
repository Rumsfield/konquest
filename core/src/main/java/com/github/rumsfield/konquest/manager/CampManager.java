package com.github.rumsfield.konquest.manager;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.event.camp.KonquestCampCreateEvent;
import com.github.rumsfield.konquest.api.event.player.KonquestPlayerCampEvent;
import com.github.rumsfield.konquest.api.manager.KonquestCampManager;
import com.github.rumsfield.konquest.api.model.KonquestCamp;
import com.github.rumsfield.konquest.api.model.KonquestOfflinePlayer;
import com.github.rumsfield.konquest.model.KonCamp;
import com.github.rumsfield.konquest.model.KonCampGroup;
import com.github.rumsfield.konquest.model.KonOfflinePlayer;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.HelperUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.data.type.Bed;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.*;

public class CampManager implements KonquestCampManager {

	private final Konquest konquest;
	private final HashMap<String,KonCamp> barbarianCamps; // player uuids to camps
	private boolean isCampDataNull;
	
	public CampManager(Konquest konquest) {
		this.konquest = konquest;
		this.barbarianCamps = new HashMap<>();
		this.isCampDataNull = false;
	}
	
	// intended to be called after database has connected and loaded player tables
	public void initCamps() {
		loadCamps();
		ChatUtil.printDebug("Loaded camps");
	}
	
	public boolean isCampSet(KonquestOfflinePlayer player) {
		String uuid = player.getOfflineBukkitPlayer().getUniqueId().toString();
		return player.isBarbarian() && barbarianCamps.containsKey(uuid);
	}

	public boolean isCampName(String name) {
		for(KonCamp camp : barbarianCamps.values()) {
			if(camp.getName().equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	public KonCamp getCampByName(String name) {
		for(KonCamp camp : barbarianCamps.values()) {
			if(camp.getName().equalsIgnoreCase(name)) {
				return camp;
			}
		}
		return null;
	}
	
	public KonCamp getCamp(KonquestOfflinePlayer player) {
		String uuid = player.getOfflineBukkitPlayer().getUniqueId().toString();
		return getCamp(uuid);
	}
	
	public KonCamp getCamp(String uuid) {
		return barbarianCamps.get(uuid);
	}
	
	public ArrayList<KonCamp> getCamps() {
		return new ArrayList<>(barbarianCamps.values());
	}
	
	public ArrayList<String> getCampNames() {
		ArrayList<String> campNames = new ArrayList<>();
		for(KonCamp camp : barbarianCamps.values()) {
			campNames.add(camp.getName());
		}
		return campNames;
	}
	
	public void activateCampProtection(@Nullable KonOfflinePlayer offlinePlayer) {
		if(offlinePlayer == null)return;
		KonCamp camp = getCamp(offlinePlayer);
		if(camp == null) return;
		ChatUtil.printDebug("Set camp protection to true for player "+offlinePlayer.getOfflineBukkitPlayer().getName());
		camp.setProtected(true);
	}
	
	public void deactivateCampProtection(KonOfflinePlayer offlinePlayer) {
		if(offlinePlayer == null) return;
		KonCamp camp = getCamp(offlinePlayer);
		if(camp == null) return;
		ChatUtil.printDebug("Set camp protection to false for player "+offlinePlayer.getOfflineBukkitPlayer().getName());
		camp.setProtected(false);
	}
	
	/**
	 * addCamp - primary method for adding a new camp for a barbarian
	 * @param loc - location of the camp
	 * @param player - player adding the camp
	 * @return status 	0 = success
	 * 					1 = camp init claims overlap with existing territory
	 * 					2 = camp already exists for player
	 * 					3 = player is not a barbarian
	 * 					4 = camps are disabled
	 *                  5 = world is invalid
	 */
	public int addCamp(Location loc, KonOfflinePlayer player) {
		boolean enable = konquest.getCore().getBoolean(CorePath.CAMPS_ENABLE.getPath(),true);
		if(!enable) {
			ChatUtil.printDebug("Failed to add camp, feature disabled!");
			return 4;
		}
		if(!konquest.isWorldValid(loc)) {
			ChatUtil.printDebug("Failed to add camp, location is in invalid world");
			return 5;
		}
		String uuid = player.getOfflineBukkitPlayer().getUniqueId().toString();
		if(!player.isBarbarian()) {
			ChatUtil.printDebug("Failed to add camp, player "+player.getOfflineBukkitPlayer().getName()+" "+uuid+" is not a barbarian!");
			return 3;
		}
		if(!barbarianCamps.containsKey(uuid)) {
			// Verify no overlapping init chunks
			int radius = konquest.getCore().getInt(CorePath.CAMPS_INIT_RADIUS.getPath());
			World addWorld = loc.getWorld();
			for(Point point : HelperUtil.getAreaPoints(loc, radius)) {
				if(konquest.getTerritoryManager().isChunkClaimed(point,addWorld)) {
					ChatUtil.printDebug("Found a chunk conflict in camp placement for player "+player.getOfflineBukkitPlayer().getName()+" "+uuid);
					return 1;
				}
			}
			// Attempt to add the camp
			KonCamp newCamp = new KonCamp(loc,player.getOfflineBukkitPlayer(),konquest);
			barbarianCamps.put(uuid,newCamp);
			newCamp.initClaim();
			// Update bar players
			newCamp.updateBarPlayers();
			// update the chunk cache, add points to primary world cache
			konquest.getTerritoryManager().addAllTerritory(loc.getWorld(),newCamp.getChunkList());
			konquest.getMapHandler().drawUpdateTerritory(newCamp);
		} else {
			return 2;
		}
		return 0;
	}

	/*
	 * TODO
	 *  Implement the placement logic
	 *  How to handle placement by players, and admins?
	 *  - Use common function for placing new and stowed camps
	 *  - Have wrappers with checks and messages for players and admins separately
	 *  How does an admin place a camp for someone while their camp is stowed?
	 *  - If the camp is placed, admin cannot place a new camp, must remove original first.
	 *  - If the camp is stowed, admin can place a new camp, effectively places the stowed camp for the player.
	 *  While a camp is stowed, it reserves its original chunk claims but they are not rendered/visible.
	 *  - If the player logs off or disconnects, the stowed camp is restored.
	 *  - If the player is killed or otherwise dies, then the camp is destroyed with them.
	 *  - The player must place the stowed camp for the claims to transfer to a new location.
	 *  - An item is placed in their inventory to represent the stowed camp??
	 *  - A camp can only be in a stowed state while the owner is online, "carrying" the camp with them.
	 */

	// Player must be online performing placement for their own camp
	public void placeCampForPlayer(Location loc, KonPlayer player) {
		// Place a new camp if the player has no camp set
		// Place a stowed camp is the player already has a camp that is stowed
		// New camps have the default init radius, stowed camps have arbitrary claimed chunks
		Player bukkitPlayer = player.getBukkitPlayer();
		String uuid = bukkitPlayer.getUniqueId().toString();
		// Check for barbarian
		if (!player.isBarbarian()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_CAMP_FAIL_BARBARIAN.getMessage());
			return;
		}
		boolean isNewPlacement = false;
		boolean isStowedPlacement = false;
		int radius = konquest.getCore().getInt(CorePath.CAMPS_INIT_RADIUS.getPath());
		ArrayList<Point> placementPoints = new ArrayList<>();
		if (barbarianCamps.containsKey(uuid)) {
			// Camp already exists
			KonCamp playerCamp = getCamp(uuid);
			// Check for stowed camp
			if (!playerCamp.isStowed()) {
				ChatUtil.sendError(bukkitPlayer, "CHANGE Camp must be stowed first");
				return;
			}
			// Get offset placement points
			Point pCenterStowed = HelperUtil.toPoint(playerCamp.getCenterLoc());
			Point pCenterPlace = HelperUtil.toPoint(loc);
			int dx = pCenterPlace.x - pCenterStowed.x;
			int dy = pCenterPlace.y - pCenterStowed.y;
			for (Point pStowed : playerCamp.getChunkPoints()) {
				placementPoints.add(new Point(pStowed.x+dx,pStowed.y+dy));
			}
			isStowedPlacement = true;
		} else {
			// Camp does not exist
			placementPoints.addAll(HelperUtil.getAreaPoints(loc, radius));
			isNewPlacement = true;
		}

		// Check for placement restrictions
		World placeWorld = loc.getWorld();
		for(Point point : placementPoints) {
			// Is there overlapping claimed territory
			if(konquest.getTerritoryManager().isChunkClaimed(point,placeWorld)) {
				ChatUtil.sendError(bukkitPlayer, "CHANGE Camp cannot overlap with other territory");
				return;
			}
			// Is there a WorldGuard region flag denied
			if(konquest.getIntegrationManager().getWorldGuard().isEnabled() &&
					!konquest.getIntegrationManager().getWorldGuard().isChunkClaimAllowed(placeWorld,point,bukkitPlayer)) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.REGION_ERROR_CLAIM_DENY.getMessage());
				return;
			}
		}

		// Fire event
		KonquestPlayerCampEvent invokePreEvent = new KonquestPlayerCampEvent(konquest, player, loc);
		Konquest.callKonquestEvent(invokePreEvent);
		// Check for cancelled
		if(invokePreEvent.isCancelled()) {
			ChatUtil.sendError(bukkitPlayer, "CHANGE Camping denied");
			return;
		}

		// Make the camp
		if (isNewPlacement) {

		}


	}

	// Return false to cancel placing a bed
	public boolean addCampForPlayer(Location loc, KonPlayer player) {
		Player bukkitPlayer = player.getBukkitPlayer();
		// Check for other plugin flags
		if(konquest.getIntegrationManager().getWorldGuard().isEnabled()) {
			// Check new territory claims
			int radius = konquest.getCore().getInt(CorePath.CAMPS_INIT_RADIUS.getPath());
			World locWorld = loc.getWorld();
			for(Point point : HelperUtil.getAreaPoints(loc, radius)) {
				if(!konquest.getIntegrationManager().getWorldGuard().isChunkClaimAllowed(locWorld,point,bukkitPlayer)) {
					// A region is denying this action
					ChatUtil.sendError(bukkitPlayer, MessagePath.REGION_ERROR_CLAIM_DENY.getMessage());
					return false;
				}
			}
		}
		// Try to add the camp
		int status = addCamp(loc, player);
		if(status == 0) { // on successful camp setup...
			KonCamp newCamp = getCamp(player);
			if(newCamp != null) {
				// Fire event
				KonquestCampCreateEvent invokeEvent = new KonquestCampCreateEvent(konquest, newCamp, player);
				Konquest.callKonquestEvent(invokeEvent);
				// Post-camp setup
				player.getBukkitPlayer().setBedSpawnLocation(loc, true);
				ChatUtil.sendKonTitle(player, "", Konquest.barbarianColor2+newCamp.getName());
			}
			ChatUtil.sendNotice(bukkitPlayer, MessagePath.PROTECTION_NOTICE_CAMP_CREATE.getMessage());
		} else {
			switch(status) {
				case 1:
					ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_CAMP_FAIL_OVERLAP.getMessage());
					return false;
				case 2:
					ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_CAMP_CREATE.getMessage());
					return false;
				case 3:
					ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_CAMP_FAIL_BARBARIAN.getMessage());
					return false;
				case 4:
					// This error message is removed because it could be annoying to see it every time a bed is placed when camps are disabled.
					break;
				case 5:
					// This error message is removed because it could be annoying to see it every time a bed is placed in an invalid world.
					break;
				case 6:
					ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_CAMP_FAIL_OFFLINE.getMessage());
					break;
				default:
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
					break;
			}
		}
		return true;
	}
	
	public boolean removeCamp(KonquestOfflinePlayer player) {
		String uuid = player.getOfflineBukkitPlayer().getUniqueId().toString();
		return removeCamp(uuid);
	}
	
	public boolean removeCamp(KonCamp camp) {
		String uuid = camp.getOwner().getUniqueId().toString();
		return removeCamp(uuid);
	}
	
	public boolean removeCamp(String uuid) {
		if(barbarianCamps.containsKey(uuid)) {
			ArrayList<Point> campPoints = new ArrayList<>(barbarianCamps.get(uuid).getChunkList().keySet());
			konquest.getShopHandler().deleteShopsInPoints(campPoints,barbarianCamps.get(uuid).getWorld());
			KonCamp removedCamp = barbarianCamps.remove(uuid);
			removedCamp.removeAllBarPlayers();
			// Ensure bed is broken
			if(removedCamp.getBedLocation().getBlock().getBlockData() instanceof Bed) {
				removedCamp.getBedLocation().getBlock().breakNaturally();
			}
			// Update the chunk cache, remove all points from primary world
			konquest.getTerritoryManager().removeAllTerritory(removedCamp.getWorld(),removedCamp.getChunkList().keySet());
			// Refresh groups
			Collection<KonCamp> groupSet = new ArrayList<>();
			if(groupMap.containsKey(removedCamp)) {
				groupSet = groupMap.get(removedCamp).getCamps();
			}
			refreshGroups();
			// Notify group
			for(KonCamp groupCamp : groupSet) {
				if(groupCamp.isOwnerOnline()) {
					KonPlayer ownerOnlinePlayer = konquest.getPlayerManager().getPlayerFromID(groupCamp.getOwner().getUniqueId());
					if(ownerOnlinePlayer != null) {
						ChatUtil.sendNotice(ownerOnlinePlayer.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_CAMP_CLAN_REMOVE.getMessage(removedCamp.getName()));
					}
				}
			}
			konquest.getMapHandler().drawRemoveTerritory(removedCamp);
			removedCamp = null;
		} else {
			ChatUtil.printDebug("Failed to remove camp for missing UUID "+uuid);
			return false;
		}
		ChatUtil.printDebug("Successfully removed camp for UUID "+uuid);
		return true;
	}
	

	
	private void loadCamps() {
		boolean enable = konquest.getCore().getBoolean(CorePath.CAMPS_ENABLE.getPath(),true);
		if(!enable) {
			ChatUtil.printConsoleAlert("Disabled barbarian camps");
			return;
		}
		FileConfiguration campsConfig = konquest.getConfigManager().getConfig("camps");
        if (campsConfig.get("camps") == null) {
			ChatUtil.printConsoleError("Failed to load any camps from camps.yml! Check file permissions.");
			isCampDataNull = true;
            return;
        }
        ConfigurationSection campsSection = campsConfig.getConfigurationSection("camps");
        Set<String> playerSet = campsSection.getKeys(false);
        double x,y,z;
        List<Double> sectionList;
        String worldName;
        String defaultWorldName = konquest.getCore().getString(CorePath.WORLD_NAME.getPath(),"world");
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
		            		getCamp(offlinePlayer).addPoints(HelperUtil.formatStringToPoints(playerCampSection.getString("chunks")));
		            		konquest.getTerritoryManager().addAllTerritory(world,getCamp(offlinePlayer).getChunkList());
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
					ChatUtil.printDebug("Failed to find player " + offlineBukkitPlayer.getName() + " when adding their camp");
				}
        	}
        }
        ChatUtil.printDebug("Updated all camps from camps.yml, total "+totalCamps);
    }
	
	public void saveCamps() {
		if(isCampDataNull && barbarianCamps.isEmpty()) {
			// There was probably an issue loading camps, do not save.
			ChatUtil.printConsoleError("Aborted saving camp data because a problem was encountered while loading data from camps.yml");
			return;
		}
		// Create new config entries
		FileConfiguration newSaveConfig = new YamlConfiguration();
		ConfigurationSection root = newSaveConfig.createSection("camps");
		try {
			for (String uuid : barbarianCamps.keySet()) {
				KonCamp camp = barbarianCamps.get(uuid);
				ConfigurationSection campSection = root.createSection(uuid);
				campSection.set("world", camp.getWorld().getName());
				campSection.set("center", new int[]{camp.getCenterLoc().getBlockX(),
						camp.getCenterLoc().getBlockY(),
						camp.getCenterLoc().getBlockZ()});
				campSection.set("chunks", HelperUtil.formatPointsToString(camp.getChunkList().keySet()));
			}
			// Save to file
			FileConfiguration campsConfig = konquest.getConfigManager().getConfig("camps");
			campsConfig.set("camps", newSaveConfig.get("camps")); // apply new save data
			if(!barbarianCamps.isEmpty()) {
				ChatUtil.printConsole("Saved Camps");
			}
		} catch (Exception | Error internalError) {
			ChatUtil.printConsoleError("Failed to save camps, report this as a bug to the plugin author!");
			internalError.printStackTrace();
		}
	}
}
