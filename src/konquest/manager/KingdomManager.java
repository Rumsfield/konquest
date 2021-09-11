package konquest.manager;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
//import java.util.concurrent.ScheduledThreadPoolExecutor;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Snow;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.command.TravelCommand.TravelDestination;
import konquest.display.OptionIcon.optionAction;
import konquest.model.KonCamp;
import konquest.model.KonCapital;
import konquest.model.KonDirective;
import konquest.model.KonKingdom;
import konquest.model.KonKingdomScoreAttributes;
import konquest.model.KonKingdomScoreAttributes.KonKingdomScoreAttribute;
import konquest.model.KonLeaderboard;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.model.KonPlayerScoreAttributes;
import konquest.model.KonPlayerScoreAttributes.KonPlayerScoreAttribute;
import konquest.model.KonRuin;
import konquest.model.KonStatsType;
import konquest.model.KonTerritory;
import konquest.model.KonTerritoryCache;
import konquest.model.KonTerritoryType;
import konquest.model.KonTown;
import konquest.model.KonUpgrade;
import konquest.utility.ChatUtil;
import konquest.utility.LoadingPrinter;
import konquest.utility.MessagePath;
import konquest.utility.Timer;

public class KingdomManager {

	private Konquest konquest;
	private HashMap<String, KonKingdom> kingdomMap;
	private KonKingdom barbarians;
	private KonKingdom neutrals;
	private HashMap<World,KonTerritoryCache> territoryWorldCache;
	private HashMap<PotionEffectType,Integer> townNerfs;
	private Material townCriticalBlock;
	
	public static final int DEFAULT_MAP_SIZE = 9; // 10 lines of chat, minus one for header, odd so player's chunk is centered
	
	public KingdomManager(Konquest konquest) {
		this.konquest = konquest;
		kingdomMap = new HashMap<String, KonKingdom>();
		barbarians = null;
		neutrals = null;
		territoryWorldCache = new HashMap<World,KonTerritoryCache>();
		townNerfs = new HashMap<PotionEffectType,Integer>();
		townCriticalBlock = Material.OBSIDIAN;
	}
	
	public void initialize() {
		barbarians = new KonKingdom(MessagePath.LABEL_BARBARIANS.getMessage(),konquest);
		neutrals = new KonKingdom(MessagePath.LABEL_NEUTRALS.getMessage(),konquest);
		loadCriticalBlocks();
		loadKingdoms();
		updateKingdomOfflineProtection();
		makeTownNerfs();
		ChatUtil.printDebug("Kingdom Manager is ready");
	}
	
	public boolean addKingdom(Location loc, String name) {
		if(!name.contains(" ") && !isKingdom(name)) {
			kingdomMap.put(name, new KonKingdom(loc, name, konquest));
			kingdomMap.get(name).initCapital();
			kingdomMap.get(name).getCapital().updateBarPlayers();
			//updateTerritoryCache();
			addAllTerritory(loc.getWorld(),kingdomMap.get(name).getCapital().getChunkList());
			konquest.getMapHandler().drawDynmapUpdateTerritory(kingdomMap.get(name).getCapital());
			//ChatUtil.printDebug("Added new Kingdom "+name);
			return true;
		}
		return false;
	}
	
	public boolean removeKingdom(String name) {
		if(kingdomMap.containsKey(name)) {
			KonKingdom kingdom = kingdomMap.get(name);
			// Exile all members
			for(KonPlayer member : konquest.getPlayerManager().getPlayersInKingdom(kingdom)) {
				exilePlayer(member,false,false);
			}
			// Remove all towns
			for(KonTown town : kingdom.getTowns()) {
				removeTown(town.getName(),kingdom.getName());
			}
			kingdom = null;
			KonKingdom oldKingdom = kingdomMap.remove(name);
			if(oldKingdom != null) {
				// Remove capital
				oldKingdom.getCapital().removeAllBarPlayers();
				removeAllTerritory(oldKingdom.getCapital().getWorld(),oldKingdom.getCapital().getChunkList().keySet());
				konquest.getMapHandler().drawDynmapRemoveTerritory(oldKingdom.getCapital());
				oldKingdom = null;
				ChatUtil.printDebug("Removed Kingdom "+name);
				return true;
			}
		}
		return false;
	}
	
	public boolean renameKingdom(String oldName, String newName) {
		if(kingdomMap.containsKey(oldName)) {
			KonKingdom oldKingdom = getKingdom(oldName);
			for (KonTown town : oldKingdom.getTowns()) {
				konquest.getMapHandler().drawDynmapRemoveTerritory(town);
			}
			konquest.getMapHandler().drawDynmapRemoveTerritory(oldKingdom.getCapital());
			oldKingdom = null;
			getKingdom(oldName).setName(newName);
			KonKingdom kingdom = kingdomMap.remove(oldName);
			kingdomMap.put(newName, kingdom);
			kingdom.getCapital().updateName();
			konquest.getMapHandler().drawDynmapUpdateTerritory(kingdom.getCapital());
			for (KonTown town : kingdom.getTowns()) {
				konquest.getMapHandler().drawDynmapUpdateTerritory(town);
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Assign a player to a Kingdom and teleport them there.
	 * @param player
	 * @param kingdomName
	 * @return int status
	 * 				0 = success
	 *  			1 = kingdom name does not exist
	 *  			2 = the kingdom is full (config option max_player_diff)
	 *  			3 = missing permission
	 */
	public int assignPlayerKingdom(KonPlayer player, String kingdomName, boolean force) {
		//TODO: Some sort of penalty for changing kingdoms, check if barbarian first
		if(isKingdom(kingdomName)) {
			// Check for permission
			boolean isPerKingdomJoin = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.per_kingdom_join_permissions",false);
			if (isPerKingdomJoin && !force) {
				String permission = "konquest.join."+getKingdom(kingdomName).getName().toLowerCase();
				if(!player.getBukkitPlayer().hasPermission(permission)) {
					return 3;
				}
			}
			int config_max_player_diff = konquest.getConfigManager().getConfig("core").getInt("core.kingdoms.max_player_diff");
			int smallestKingdomPlayerCount = 0;
			int targetKingdomPlayerCount = 0;
			if(config_max_player_diff != 0) {
				// Find smallest kingdom, compare player count to this kingdom's
				String smallestKingdomName = getSmallestKingdomName();
				smallestKingdomPlayerCount = konquest.getPlayerManager().getAllPlayersInKingdom(smallestKingdomName).size();
				targetKingdomPlayerCount = konquest.getPlayerManager().getAllPlayersInKingdom(kingdomName).size();
			}
			// Join kingdom is max_player_diff is disabled, or if the desired kingdom is within the max diff
			if(config_max_player_diff == 0 || force ||
					(config_max_player_diff != 0 && targetKingdomPlayerCount < (smallestKingdomPlayerCount+config_max_player_diff))) {
				// Remove player from any enemy towns
				KonKingdom assignedKingdom = getKingdom(kingdomName);
				Collection<KonKingdom> enemyKingdoms = kingdomMap.values();
				enemyKingdoms.remove(assignedKingdom);
				for(KonKingdom enemy : enemyKingdoms) {
					for(KonTown town : enemy.getTowns()) {
			    		if(town.removePlayerResident(player.getOfflineBukkitPlayer())) {
			    			ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_EXILE_NOTICE_TOWN.getMessage(town.getName()));
			    			konquest.getMapHandler().drawDynmapLabel(town);
			    		}
			    	}
				}
				// Remove any barbarian camps
				konquest.getCampManager().removeCamp(player);
				// Set kingdom
				player.setKingdom(getKingdom(kingdomName));
		    	player.setBarbarian(false);
		    	// Teleport to capital
		    	Location spawn = player.getKingdom().getCapital().getSpawnLoc();
		    	konquest.telePlayerLocation(player.getBukkitPlayer(), spawn);
		    	player.getBukkitPlayer().setBedSpawnLocation(spawn, true);
		    	// Updates
		    	getKingdom(kingdomName).getCapital().addBarPlayer(player);
		    	updateSmallestKingdom();
		    	konquest.updateNamePackets(player);
		    	konquest.getDirectiveManager().displayBook(player);
		    	konquest.getMapHandler().drawDynmapLabel(getKingdom(kingdomName).getCapital());
		    	updateKingdomOfflineProtection();
			} else {
				return 2;
			}
		} else {
			return 1;
		}
		return 0;
	}
	
	/**
	 * Exiles a player to the Barbarians and teleports to a random Wild location.
	 * Sets their exileKingdom value to their current Kingdom.
	 * Removes all stats and disables prefix.
	 * @param player
	 * @return true if not already barbarian and the teleport was successful, else false.
	 */
	public boolean exilePlayer(KonPlayer player, boolean teleport, boolean stats) {
    	if(player.isBarbarian()) {
    		return false;
    	}
    	KonKingdom oldKingdom = player.getKingdom();
    	//boolean doWildTeleport = konquest.getConfigManager().getConfig("core").getBoolean("core.exile.random_wild", true);
    	if(teleport) {
    		if(!konquest.isWorldValid(player.getBukkitPlayer().getLocation().getWorld())) {
        		return false;
        	}
    		int radius = konquest.getConfigManager().getConfig("core").getInt("core.travel_wild_random_radius",200);
			Location randomWildLoc = konquest.getRandomWildLocation(radius*2,player.getBukkitPlayer().getLocation().getWorld());
	    	if(randomWildLoc == null) {
	    		return false;
	    	}
	    	player.getBukkitPlayer().teleport(randomWildLoc);
	    	player.getBukkitPlayer().setBedSpawnLocation(randomWildLoc, true);
    	}
    	player.setExileKingdom(oldKingdom);
    	// Remove residency
    	for(KonTown town : player.getKingdom().getTowns()) {
    		if(town.removePlayerResident(player.getOfflineBukkitPlayer())) {
    			//ChatUtil.sendNotice(player.getBukkitPlayer(), "Banished from "+town.getName()+"!");
    			ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_EXILE_NOTICE_TOWN.getMessage(town.getName()));
    			konquest.getMapHandler().drawDynmapLabel(town);
    		}
    	}
    	//boolean doRemoveStats = konquest.getConfigManager().getConfig("core").getBoolean("core.exile.remove_stats", true);
    	if(stats) {
	    	// Clear all stats
	    	player.getPlayerStats().clearStats();
	    	// Disable prefix
	    	player.getPlayerPrefix().setEnable(false);
    	}
    	// Force into global chat mode
    	player.setIsGlobalChat(true);
    	// Make into barbarian
    	player.setKingdom(getBarbarians());
    	player.setBarbarian(true);
    	//konquest.getPlayerManager().saveAllPlayers();
    	//konquest.getPlayerManager().updateAllSavedPlayers();
    	konquest.updateNamePackets(player);
    	konquest.getMapHandler().drawDynmapLabel(oldKingdom.getCapital());
    	updateSmallestKingdom();
    	updateKingdomOfflineProtection();
    	return true;
	}
	
	/**
	 * addTown - Primary method for adding a town
	 * @param loc - location of town center
	 * @param name - name of town
	 * @param kingdomName - name of kingdom that the town belongs to
	 * @return  0 - success
	 * 			1 - error, chunk claim conflict
	 * 			2 - error, bad monument placement
	 * 			3 - error, bad name
	 * 			4 - error, invalid monument template
	 * 			5 - error, bad town placement, invalid world
	 * 			6 - error, bad town placement, too close
	 * 			7 - error, bad town placement, too far
	 * 		   21 - error, town init fail, invalid monument
	 * 		   22 - error, town init fail, bad monument gradient
	 * 		   23 - error, town init fail, monument placed on bedrock
	 *  	   12 - error, town init fail, bad town height
	 *  	   13 - error, town init fail, too much air
	 *  	   14 - error, town init fail, bad chunks
	 */
	public int addTown(Location loc, String name, String kingdomName) {
		ChatUtil.printDebug("Attempting to add new town "+name+" for kingdom "+kingdomName);
		if(!name.contains(" ") && isKingdom(kingdomName)) {
			if(!konquest.isWorldValid(loc.getWorld())) {
				ChatUtil.printDebug("Failed to add town, invalid world: "+loc.getWorld().getName());
				return 5;
			}
			int min_distance_capital = konquest.getConfigManager().getConfig("core").getInt("core.towns.min_distance_capital");
			int min_distance_town = konquest.getConfigManager().getConfig("core").getInt("core.towns.min_distance_town");
			int searchDistance = 0;
			int minDistance = Integer.MAX_VALUE;
			for(KonKingdom kingdom : kingdomMap.values()) {
				// Verify name is not taken by existing towns or kingdoms
				if(name.equalsIgnoreCase(kingdom.getName()) || kingdom.hasTown(name)) {
					ChatUtil.printDebug("Failed to add town, name equals existing town or kingdom: "+name);
					return 3;
				}
				// Verify proximity to other territories
				searchDistance = Konquest.chunkDistance(loc, kingdom.getCapital().getCenterLoc());
				if(searchDistance != -1 && min_distance_capital > 0 && searchDistance < min_distance_capital) {
					ChatUtil.printDebug("Failed to add town, too close to capital "+kingdom.getCapital().getName());
					return 6;
				}
				if(searchDistance != -1 && searchDistance < minDistance) {
					minDistance = searchDistance;
				}
				for(KonTown town : kingdom.getTowns()) {
					searchDistance = Konquest.chunkDistance(loc, town.getCenterLoc());
					if(searchDistance != -1 && min_distance_town > 0 && searchDistance < min_distance_town) {
						ChatUtil.printDebug("Failed to add town, too close to town "+town.getName());
						return 6;
					}
					if(searchDistance != -1 && searchDistance < minDistance) {
						minDistance = searchDistance;
					}
				}
			}
			for(KonRuin ruin : konquest.getRuinManager().getRuins()) {
				if(name.equalsIgnoreCase(ruin.getName())) {
					ChatUtil.printDebug("Failed to add town, name equals existing ruin: "+name);
					return 3;
				}
				searchDistance = Konquest.chunkDistance(loc, ruin.getCenterLoc());
				if(searchDistance != -1 && min_distance_town > 0 && searchDistance < min_distance_town) {
					ChatUtil.printDebug("Failed to add town, too close to ruin "+ruin.getName());
					return 6;
				}
				if(searchDistance != -1 && searchDistance < minDistance) {
					minDistance = searchDistance;
				}
			}
			// Verify max distance
			int maxLimit = konquest.getConfigManager().getConfig("core").getInt("core.towns.max_distance_all",0);
			if(minDistance != 0 && maxLimit > 0 && minDistance > maxLimit) {
				ChatUtil.printDebug("Failed to add town, too far from other towns and capitals");
				return 7;
			}
			for(TravelDestination keyword : TravelDestination.values()) {
				if(name.equalsIgnoreCase(keyword.toString())) {
					ChatUtil.printDebug("Failed to add town, name equals territory keyword: "+name);
					return 3;
				}
			}
			// Verify no overlapping init chunks
			int radius = konquest.getConfigManager().getConfig("core").getInt("core.towns.init_radius");
			if(radius < 1) {
				radius = 1;
			}
			ChatUtil.printDebug("Checking for chunk conflicts with radius "+radius);
			for(Point point : konquest.getAreaPoints(loc, radius)) {
				if(isChunkClaimed(point,loc.getWorld())) {
					ChatUtil.printDebug("Found a chunk conflict");
					return 1;
				}
			}
			// Verify valid monument template
			if(getKingdom(kingdomName).getMonumentTemplate().isValid()) {
				// Modify location to max Y at given X,Z
				Point point = konquest.toPoint(loc);
				int xLocal = loc.getBlockX() - (point.x*16);
				int zLocal = loc.getBlockZ() - (point.y*16);
				Chunk chunk = loc.getChunk();
				int yFloor = chunk.getChunkSnapshot(true, false, false).getHighestBlockYAt(xLocal, zLocal);
				while((chunk.getBlock(xLocal, yFloor, zLocal).isPassable() || !chunk.getBlock(xLocal, yFloor, zLocal).getType().isOccluding()) && yFloor > 0) {
					yFloor--;
				}
				loc.setY(yFloor+1);
				// Attempt to add the town
				if(getKingdom(kingdomName).addTown(loc, name)) {
					// Attempt to initialize the town
					int initStatus = getKingdom(kingdomName).initTown(name);
					if(initStatus == 0) {
						// When a town is added successfully, update the chunk cache
						//updateTerritoryCache();
						addAllTerritory(loc.getWorld(),getKingdom(kingdomName).getTown(name).getChunkList());
						konquest.getMapHandler().drawDynmapUpdateTerritory(getKingdom(kingdomName).getTown(name));
						return 0;
					} else {
						// Remove town if init fails, exit code 10+
						getKingdom(kingdomName).removeTown(name);
						return 10+initStatus;
					}
				} else { return 3; }
			} else { return 4; }
		}
		return 3;
	}
	
	/**
	 * Primary method for removing a town
	 * @param name
	 * @param kingdomName
	 * @return
	 */
	public boolean removeTown(String name, String kingdomName) {
		if(isKingdom(kingdomName) && getKingdom(kingdomName).hasTown(name)) {
			KonKingdom kingdom = getKingdom(kingdomName);
			KonTown town = kingdom.getTown(name);
			ArrayList<Point> townPoints = new ArrayList<Point>();
			townPoints.addAll(town.getChunkList().keySet());
			clearAllTownNerfs(town);
			clearAllTownHearts(town);
			town.removeAllBarPlayers();
			if(kingdom.removeTown(name)) {
				// When a town is removed successfully, update the chunk cache
				//updateTerritoryCache();
				removeAllTerritory(town.getWorld(),townPoints);
				konquest.getMapHandler().drawDynmapRemoveTerritory(town);
				konquest.getMapHandler().drawDynmapLabel(town.getKingdom().getCapital());
				konquest.getIntegrationManager().deleteShopsInPoints(townPoints,town.getWorld());
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * Primary method for renaming a town
	 * @param oldName
	 * @param newName
	 * @param kingdomName
	 * @return
	 */
	public boolean renameTown(String oldName, String newName, String kingdomName) {
		if(isKingdom(kingdomName) && getKingdom(kingdomName).hasTown(oldName)) {
			KonKingdom kingdom = getKingdom(kingdomName);
			konquest.getMapHandler().drawDynmapRemoveTerritory(kingdom.getTown(oldName));
			boolean success = kingdom.renameTown(oldName, newName);
			if (success) {
				KonTown town = kingdom.getTown(newName);
				if (town != null) {
					konquest.getMapHandler().drawDynmapUpdateTerritory(town);
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Primary method for transferring ownership of a town between kingdoms
	 * @param name - Town name
	 * @param oldKingdomName - Kingdom name of conquered Town
	 * @param conquerPlayer - Player who initiated the conquering
	 * @return true if all names exist, else false
	 */
	public boolean captureTownForPlayer(String name, String oldKingdomName, KonPlayer conquerPlayer) {
		if(conquerPlayer == null) {
			return false;
		}
		if(conquerPlayer.isBarbarian()) {
			return false;
		}
		if(captureTown(name, oldKingdomName, conquerPlayer.getKingdom())) {
			conquerPlayer.getKingdom().getTown(name).setPlayerLord(conquerPlayer.getOfflineBukkitPlayer());
			return true;
		}
		return false;
	}
	
	public boolean captureTown(String name, String oldKingdomName, KonKingdom conquerKingdom) {
		if(!conquerKingdom.getMonumentTemplate().isValid()) {
			return false;
		}
		boolean captureUpgrades = konquest.getConfigManager().getConfig("core").getBoolean("core.towns.capture_upgrades",true);
		if(isKingdom(oldKingdomName) && getKingdom(oldKingdomName).hasTown(name)) {
			konquest.getMapHandler().drawDynmapRemoveTerritory(getKingdom(oldKingdomName).getTown(name));
			getKingdom(oldKingdomName).getTown(name).purgeResidents();
			getKingdom(oldKingdomName).getTown(name).clearShieldsArmors();
			getKingdom(oldKingdomName).getTown(name).setKingdom(conquerKingdom);
			conquerKingdom.addTownConquer(name, getKingdom(oldKingdomName).removeTownConquer(name));
			conquerKingdom.getTown(name).refreshMonument();
			refreshTownNerfs(conquerKingdom.getTown(name));
			refreshTownHearts(conquerKingdom.getTown(name));
			conquerKingdom.getTown(name).updateBarPlayers();
			if(captureUpgrades) {
				konquest.getUpgradeManager().updateTownDisabledUpgrades(conquerKingdom.getTown(name));
			} else {
				conquerKingdom.getTown(name).clearUpgrades();
			}
			konquest.getMapHandler().drawDynmapUpdateTerritory(conquerKingdom.getTown(name));
			konquest.getMapHandler().drawDynmapLabel(getKingdom(oldKingdomName).getCapital());
			konquest.getMapHandler().drawDynmapLabel(conquerKingdom.getCapital());
			konquest.getIntegrationManager().deleteShopsInPoints(conquerKingdom.getTown(name).getChunkList().keySet(),conquerKingdom.getTown(name).getWorld());
			return true;
		}
		return false;
	}
	
	/**\
	 * claimChunk - Primary method for claiming chunks for closest adjacent territories
	 * @param loc
	 * @return  0 - success
	 * 			1 - error, no adjacent territory
	 * 			2 - error, exceeds max distance
	 * 			3 - error, already claimed
	 */
	public int claimChunk(Location loc) {
		if(isChunkClaimed(loc)) {
			return 3;
		}
		boolean foundAdjTerr = false;
		KonTerritory closestAdjTerr = null;
		World claimWorld = loc.getWorld();
		for(Point sidePoint : konquest.getSidePoints(loc)) {
			if(isChunkClaimed(sidePoint,claimWorld)) {
				if(!foundAdjTerr) {
					closestAdjTerr = getChunkTerritory(sidePoint,claimWorld);
					foundAdjTerr = true;
				} else {
					//int closestDist = Konquest.distanceInChunks(loc, closestAdjTerr.getCenterLoc());
					int closestDist = Konquest.chunkDistance(loc, closestAdjTerr.getCenterLoc());
					//int currentDist = Konquest.distanceInChunks(loc, getChunkTerritory(sideChunk).getCenterLoc());
					int currentDist = Konquest.chunkDistance(loc, getChunkTerritory(sidePoint,claimWorld).getCenterLoc());
					if(currentDist != -1 && closestDist != -1 && currentDist < closestDist) {
						closestAdjTerr = getChunkTerritory(sidePoint,claimWorld);
					}
				}
			}
		}
		if(foundAdjTerr) {
			Point addPoint = konquest.toPoint(loc);
			if(closestAdjTerr.getWorld().equals(loc.getWorld()) && closestAdjTerr.addChunk(addPoint)) {
				addTerritory(loc.getWorld(),addPoint,closestAdjTerr);
				konquest.getMapHandler().drawDynmapUpdateTerritory(closestAdjTerr);
				konquest.getMapHandler().drawDynmapLabel(closestAdjTerr.getKingdom().getCapital());
				return 0;
			} else {
				return 2;
			}
		}
		return 1;
	}
	
	public void claimForAdmin(Player bukkitPlayer, Location claimLoc) {
    	int claimStatus = claimChunk(claimLoc);
    	switch(claimStatus) {
    	case 0:
    		KonPlayer player = konquest.getPlayerManager().getPlayer(bukkitPlayer);
    		updatePlayerBorderParticles(player, bukkitPlayer.getLocation());
    		//ChatUtil.sendNotice(bukkitPlayer, "Successfully added chunk for territory: "+getChunkTerritory(claimLoc.getChunk()).getName());
    		ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
    		break;
    	case 1:
    		//ChatUtil.sendError(bukkitPlayer, "Failed to claim chunk: no adjacent territory.");
    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_FAIL_ADJACENT.getMessage());
    		break;
    	case 2:
    		//ChatUtil.sendError(bukkitPlayer, "Failed to claim chunk: too far from territory center.");
    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_FAIL_FAR.getMessage());
    		break;
    	case 3:
    		//ChatUtil.sendError(bukkitPlayer, "Failed to claim chunk: already claimed.");
    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_FAIL_CLAIMED.getMessage());
    		break;
    	default:
    		//ChatUtil.sendError(bukkitPlayer, "Failed to claim chunk: unknown.");
    		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(claimStatus));
    		break;
    	}
    }
	
	public boolean claimForPlayer(Player bukkitPlayer, Location claimLoc) {
		KonPlayer player = konquest.getPlayerManager().getPlayer(bukkitPlayer);
		World claimWorld = claimLoc.getWorld();
		// Verify no surrounding enemy or capital territory
    	for(Point point : konquest.getAreaPoints(claimLoc,2)) {
    		if(konquest.getKingdomManager().isChunkClaimed(point,claimWorld)) {
    			if(player != null && !player.getKingdom().equals(konquest.getKingdomManager().getChunkTerritory(point,claimWorld).getKingdom())) {
    				//ChatUtil.sendError(bukkitPlayer, "You are too close to enemy territory!");
    				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_PROXIMITY.getMessage());
    				return false;
    			}
    			if(konquest.getKingdomManager().getChunkTerritory(point,claimWorld).getTerritoryType().equals(KonTerritoryType.CAPITAL)) {
    				//ChatUtil.sendError(bukkitPlayer, "You are too close to the Capital!");
    				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_PROXIMITY.getMessage());
    				return false;
    			}
    		}
    	}
    	// Ensure player can cover the cost
    	double cost = konquest.getConfigManager().getConfig("core").getDouble("core.favor.cost_claim");
    	if(KonquestPlugin.getBalance(bukkitPlayer) < cost) {
			//ChatUtil.sendError(bukkitPlayer, "Not enough Favor, need "+cost);
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(cost));
            return false;
		}
    	// Attempt to claim the current chunk
    	int claimStatus = claimChunk(claimLoc);
    	switch(claimStatus) {
    	case 0:
    		KonTerritory territory = getChunkTerritory(claimLoc);
    		String territoryName = territory.getName();
    		//ChatUtil.sendNotice(bukkitPlayer, "Successfully claimed land for Town: "+territoryName);
    		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_CLAIM_NOTICE_SUCCESS.getMessage("1",territoryName));
    		konquest.getDirectiveManager().updateDirectiveProgress(player, KonDirective.CLAIM_LAND);
    		konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.CLAIMED,1);
    		// Display town info to players in the newly claimed chunk
    		for(KonPlayer occupant : konquest.getPlayerManager().getPlayersOnline()) {
    			if(occupant.getBukkitPlayer().getLocation().getChunk().equals(claimLoc.getChunk())) {
    				if(occupant.getKingdom().equals(territory.getKingdom())) {
    					ChatUtil.sendKonTitle(player, "", ChatColor.GREEN+territoryName);
    				} else {
    					ChatUtil.sendKonTitle(player, "", ChatColor.RED+territoryName);
    				}
    				if(territory instanceof KonTown) {
	    				KonTown town = (KonTown) territory;
	    				town.addBarPlayer(player);
    				}
    				updatePlayerBorderParticles(occupant, occupant.getBukkitPlayer().getLocation());
    			}
    		}
    		break;
    	case 1:
    		//ChatUtil.sendError(bukkitPlayer, "Failed to claim land: No adjacent Town.");
    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_FAIL_ADJACENT.getMessage());
    		break;
    	case 2:
    		//ChatUtil.sendError(bukkitPlayer, "Failed to claim land: Too far from Town center.");
    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_FAIL_FAR.getMessage());
    		break;
    	case 3:
    		//ChatUtil.sendError(bukkitPlayer, "Failed to claim land: Already claimed.");
    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_FAIL_CLAIMED.getMessage());
    		break;
    	default:
    		//ChatUtil.sendError(bukkitPlayer, "Failed to claim land: Unknown. Contact an Admin!");
    		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(claimStatus));
    		break;
    	}
    	// Reduce the player's favor
		if(cost > 0 && claimStatus == 0) {
        	EconomyResponse r = KonquestPlugin.withdrawPlayer(bukkitPlayer, cost);
            if(r.transactionSuccess()) {
            	String balanceF = String.format("%.2f",r.balance);
            	String amountF = String.format("%.2f",r.amount);
            	//ChatUtil.sendNotice(bukkitPlayer, "Favor reduced by "+amountF+", total: "+balanceF);
            	ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_REDUCE_FAVOR.getMessage(amountF,balanceF));
            	konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)cost);
            } else {
            	//ChatUtil.sendError(bukkitPlayer, String.format("An error occured: %s", r.errorMessage));
            	ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(r.errorMessage));
            }
		}
		return claimStatus == 0;
	}
	
	public boolean claimRadiusForPlayer(Player bukkitPlayer, Location claimLoc, int radius) {
		KonPlayer player = konquest.getPlayerManager().getPlayer(bukkitPlayer);
		World claimWorld = claimLoc.getWorld();
		ArrayList<Point> claimChunks = new ArrayList<Point>();
		// Verify no surrounding enemy or capital territory
    	for(Point point : konquest.getAreaPoints(claimLoc,radius+1)) {
    		if(konquest.getKingdomManager().isChunkClaimed(point,claimWorld)) {
    			if(player != null && !player.getKingdom().equals(konquest.getKingdomManager().getChunkTerritory(point,claimWorld).getKingdom())) {
    				//ChatUtil.sendError(bukkitPlayer, "Too close to enemy territory!");
    				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_PROXIMITY.getMessage());
    				return false;
    			}
    			if(konquest.getKingdomManager().getChunkTerritory(point,claimWorld).getTerritoryType().equals(KonTerritoryType.CAPITAL)) {
    				//ChatUtil.sendError(bukkitPlayer, "Too close to the Capital!");
    				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_PROXIMITY.getMessage());
    				return false;
    			}
    		}
    	}
    	// Find all unclaimed chunks around center chunk
    	for(Point point : konquest.getSurroundingPoints(claimLoc,radius)) {
    		if(!konquest.getKingdomManager().isChunkClaimed(point,claimWorld)) {
    			claimChunks.add(point);
    		}
    	}
    	int unclaimedChunkAmount = claimChunks.size();
    	boolean isCenterClaimed = true;
    	if(!konquest.getKingdomManager().isChunkClaimed(claimLoc)) {
    		unclaimedChunkAmount++;
    		isCenterClaimed = false;
    	}
    	// Ensure player can cover the cost
    	double cost = konquest.getConfigManager().getConfig("core").getDouble("core.favor.cost_claim");
    	double totalCost = unclaimedChunkAmount * cost;
    	if(KonquestPlugin.getBalance(bukkitPlayer) < totalCost) {
			//ChatUtil.sendError(bukkitPlayer, "Not enough Favor, need "+totalCost);
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(totalCost));
            return false;
		}
    	// Ensure the center chunk is claimed
    	int numChunksClaimed = 0;
    	if(!isCenterClaimed) {
    		// Claim the center chunk and do some checks to make sure it succeeded
    		int claimStatus = claimChunk(claimLoc);
        	switch(claimStatus) {
        	case 0:
        		numChunksClaimed++;
        		break;
        	case 1:
        		//ChatUtil.sendError(bukkitPlayer, "Failed to claim chunk: no adjacent territory.");
        		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_FAIL_ADJACENT.getMessage());
        		return false;
        	case 2:
        		//ChatUtil.sendError(bukkitPlayer, "Failed to claim chunk: too far from territory center.");
        		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_FAIL_FAR.getMessage());
        		return false;
        	case 3:
        		//ChatUtil.sendError(bukkitPlayer, "Failed to claim chunk: already claimed.");
        		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_FAIL_CLAIMED.getMessage());
        		return false;
        	default:
        		//ChatUtil.sendError(bukkitPlayer, "Failed to claim chunk: unknown error.");
        		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(claimStatus));
        		return false;
        	}
    	}
    	// Use territory of center chunk to claim remaining radius unclaimed chunks
    	KonTerritory territory = getChunkTerritory(claimLoc);
    	boolean allChunksAdded = true;
    	if(territory != null) {
    		for(Point newChunk : claimChunks) {
    			if(territory.addChunk(newChunk)) {
    				addTerritory(claimWorld,newChunk,territory);
    				numChunksClaimed++;
    			} else {
    				allChunksAdded = false;
    			}
    		}
    		if(!allChunksAdded) {
    			//ChatUtil.sendError(bukkitPlayer, "Failed to claim all land, too far from town center. Claimed "+numChunksClaimed+" chunks.");
    			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_RADIUS_PARTIAL.getMessage(numChunksClaimed,territory.getName()));
    		} else {
    			//ChatUtil.sendNotice(bukkitPlayer, "Successfully claimed "+numChunksClaimed+" chunks of land for Town: "+territory.getName());
    			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_CLAIM_NOTICE_SUCCESS.getMessage(numChunksClaimed,territory.getName()));
    		}
    		konquest.getDirectiveManager().updateDirectiveProgress(player, KonDirective.CLAIM_LAND);
    		konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.CLAIMED,numChunksClaimed);
    		konquest.getMapHandler().drawDynmapUpdateTerritory(territory);
    		konquest.getMapHandler().drawDynmapLabel(territory.getKingdom().getCapital());
    		String territoryName = territory.getName();
    		// Display town info to players in the newly claimed chunks
    		for(KonPlayer occupant : konquest.getPlayerManager().getPlayersOnline()) {
    			if(territory.isLocInside(occupant.getBukkitPlayer().getLocation())) {
    				if(occupant.getKingdom().equals(territory.getKingdom())) {
    					ChatUtil.sendKonTitle(player, "", ChatColor.GREEN+territoryName);
    				} else {
    					ChatUtil.sendKonTitle(player, "", ChatColor.RED+territoryName);
    				}
    				if(territory instanceof KonTown) {
	    				KonTown town = (KonTown) territory;
	    				town.addBarPlayer(player);
    				}
    				updatePlayerBorderParticles(occupant, occupant.getBukkitPlayer().getLocation());
    			}
    		}
    	} else {
    		//ChatUtil.sendError(bukkitPlayer, "Failed to find territory.");
    		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_MISSING.getMessage());
    		return false;
    	}
    	// Reduce the player's favor
		if(cost > 0 && numChunksClaimed > 0) {
			double finalCost = numChunksClaimed*cost;
	    	EconomyResponse r = KonquestPlugin.withdrawPlayer(bukkitPlayer, finalCost);
	        if(r.transactionSuccess()) {
	        	String balanceF = String.format("%.2f",r.balance);
	        	String amountF = String.format("%.2f",r.amount);
	        	//ChatUtil.sendNotice(bukkitPlayer, "Favor reduced by "+amountF+", total: "+balanceF);
	        	ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_REDUCE_FAVOR.getMessage(amountF,balanceF));
	        	konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)finalCost);
	        } else {
	        	//ChatUtil.sendError(bukkitPlayer, String.format("An error occured: %s", r.errorMessage));
	        	ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(r.errorMessage));
	        }
		}
		return true;
	}
	
	/**
	 * unclaimChunk - primary method for unclaiming a chunk for a territory
	 * @param loc
	 * @return
	 */
	public boolean unclaimChunk(Location loc) {
		if(isChunkClaimed(loc)) {
			KonTerritory territory = getChunkTerritory(loc);
			Set<KonPlayer> occupants = new HashSet<KonPlayer>();
			for(KonPlayer occupant : konquest.getPlayerManager().getPlayersOnline()) {
				if(territory.isLocInside(occupant.getBukkitPlayer().getLocation())) {
					occupants.add(occupant);
				}
			}
			if(territory.removeChunk(loc)) {
				//updateTerritoryCache();
				removeTerritory(loc);
	    		for(KonPlayer occupant : occupants) {
    				if(territory instanceof KonTown) {
	    				KonTown town = (KonTown) territory;
	    				town.removeBarPlayer(occupant);
    				} else if(territory instanceof KonCapital) {
    					KonCapital capital = (KonCapital) territory;
    					capital.removeBarPlayer(occupant);
    				} else if(territory instanceof KonRuin) {
    					KonRuin ruin = (KonRuin) territory;
    					ruin.removeBarPlayer(occupant);
    				} else if(territory instanceof KonCamp) {
    					KonCamp camp = (KonCamp) territory;
    					camp.removeBarPlayer(occupant);
    				}
    				updatePlayerBorderParticles(occupant, occupant.getBukkitPlayer().getLocation());
	    		}
				konquest.getMapHandler().drawDynmapUpdateTerritory(territory);
				konquest.getMapHandler().drawDynmapLabel(territory.getKingdom().getCapital());
				return true;
			}
		}
		return false;
	}
	
	public void addAllTerritory(World world, HashMap<Point,KonTerritory> pointMap) {
		if(territoryWorldCache.containsKey(world)) {
			territoryWorldCache.get(world).putAll(pointMap);
			//ChatUtil.printDebug("Added points for territory "+territoryName+" into existing world cache: "+world.getName());
		} else {
			KonTerritoryCache cache = new KonTerritoryCache();
			cache.putAll(pointMap);
			territoryWorldCache.put(world, cache);
			//ChatUtil.printDebug("Added points for territory "+territoryName+" into new world cache: "+world.getName());
		}
	}
	
	public void addTerritory(World world, Point point, KonTerritory territory) {
		if(territoryWorldCache.containsKey(world)) {
			territoryWorldCache.get(world).put(point, territory);
			//ChatUtil.printDebug("Added single point for territory "+territory.getName()+" into existing world cache: "+world.getName());
		} else {
			KonTerritoryCache cache = new KonTerritoryCache();
			cache.put(point, territory);
			territoryWorldCache.put(world, cache);
			//ChatUtil.printDebug("Added single point for territory "+territory.getName()+" into new world cache: "+world.getName());
		}
	}
	
	public boolean removeAllTerritory(World world, Collection<Point> points) {
		boolean result = false;
		if(territoryWorldCache.containsKey(world)) {
			if(territoryWorldCache.get(world).removeAll(points)) {
				result = true;
				//ChatUtil.printDebug("Removed all points for territory from world cache: "+world.getName());
			} else {
				ChatUtil.printDebug("Failed to remove all points for territory from world cache: "+world.getName());
			}
		} else {
			ChatUtil.printDebug("Failed to find world cache for bulk point removal: "+world.getName());
		}
		return result;
	}
	
	public boolean removeTerritory(Location loc) {
		boolean result = removeTerritory(loc.getWorld(),konquest.toPoint(loc));
		return result;
	}
	
	public boolean removeTerritory(World world, Point point) {
		boolean result = false;
		if(territoryWorldCache.containsKey(world)) {
			if(territoryWorldCache.get(world).remove(point)) {
				result = true;
				//ChatUtil.printDebug("Removed single point for territory from world cache: "+world.getName());
			} else {
				ChatUtil.printDebug("Failed to remove single point for territory from world cache: "+world.getName());
			}
		} else {
			ChatUtil.printDebug("Failed to find world cache for point removal: "+world.getName());
		}
		return result;
	}
	
	/*
	 * More methods...
	 */
	
	public String getSmallestKingdomName() {
		String smallestName = "";
		for(KonKingdom kingdom : kingdomMap.values()) {
			if(kingdom.isSmallest()) {
				smallestName = kingdom.getName();
			}
		}
		return smallestName;
	}
	
	public void updateSmallestKingdom() {
		KonKingdom smallestKingdom = null;
		int smallestSize = Integer.MAX_VALUE;
		for(KonKingdom kingdom : kingdomMap.values()) {
			kingdom.setSmallest(false);
			int size = konquest.getPlayerManager().getAllPlayersInKingdom(kingdom.getName()).size();
			if(size < smallestSize) {
				smallestKingdom = kingdom;
				smallestSize = size;
			}
		}
		if(smallestKingdom == null) {
			ChatUtil.printDebug("Failed to update smallest kingdom, got null!");
			return;
		}
		smallestKingdom.setSmallest(true);
		ChatUtil.printDebug("Updated smallest kingdom: "+smallestKingdom.getName());
	}
	
	public boolean isChunkClaimed(Location loc) {
		return isChunkClaimed(konquest.toPoint(loc),loc.getWorld());
	}
	
	public boolean isChunkClaimed(Point point, World world) {
		boolean result = false;
		if(territoryWorldCache.containsKey(world)) {
			result = territoryWorldCache.get(world).has(point);
		}
		return result;
	}
	
	// This can return null!
	public KonTerritory getChunkTerritory(Location loc) {
		return getChunkTerritory(konquest.toPoint(loc),loc.getWorld());
	}
	
	// This can return null!
	public KonTerritory getChunkTerritory(Point point, World world) {
		if(territoryWorldCache.containsKey(world)) {
			return territoryWorldCache.get(world).get(point);
		} else {
			return null;
		}
	}
	
	// This can return null!
	public KonTerritory getClosestEnemyTown(Location loc, KonKingdom friendlyKingdom) {
		KonTerritory closestTerritory = null;
		int minDistance = Integer.MAX_VALUE;
		for(KonKingdom kingdom : kingdomMap.values()) {
			if(!kingdom.equals(friendlyKingdom)) {
				for(KonTown town : kingdom.getTowns()) {
					int townDist = Konquest.chunkDistance(loc, town.getCenterLoc());
					if(townDist != -1 && townDist < minDistance) {
						minDistance = townDist;
						closestTerritory = town;
					}
				}
			}
		}
		return closestTerritory;
	}
	
	public int getDistanceToClosestTerritory(Location loc) {
		int minDistance = Integer.MAX_VALUE;
		int searchDist = 0;
		for(KonKingdom kingdom : kingdomMap.values()) {
			searchDist = Konquest.chunkDistance(loc, kingdom.getCapital().getCenterLoc());
			if(searchDist != -1 && searchDist < minDistance) {
				minDistance = searchDist;
			}
			for(KonTown town : kingdom.getTowns()) {
				searchDist = Konquest.chunkDistance(loc, town.getCenterLoc());
				if(searchDist != -1 && searchDist < minDistance) {
					minDistance = searchDist;
				}
			}
		}
		for(KonRuin ruin : konquest.getRuinManager().getRuins()) {
			searchDist = Konquest.chunkDistance(loc, ruin.getCenterLoc());
			if(searchDist != -1 && searchDist < minDistance) {
				minDistance = searchDist;
			}
		}
		return minDistance;
	}
	
	public ArrayList<String> getKingdomNames() {
		ArrayList<String> names = new ArrayList<String>(kingdomMap.keySet());
		return names;
	}
	
	public ArrayList<KonKingdom> getKingdoms() {
		ArrayList<KonKingdom> kingdoms = new ArrayList<KonKingdom>(kingdomMap.values());
		return kingdoms;
	}
	
	public KonKingdom getKingdom(String name) {
		if(kingdomMap.containsKey(name)) {
			return kingdomMap.get(name);
		} else if(name.equalsIgnoreCase("barbarians")){
			return barbarians;
		} else {
			// Check for case-insensitive name
			for(String kingdomName : kingdomMap.keySet()) {
				if(name.equalsIgnoreCase(kingdomName)) {
					return kingdomMap.get(kingdomName);
				}
			}
		}
		// Could not find a kingdom by name, return barbarians
		ChatUtil.printDebug("KingdomManager couldn't get Kingdom! "+name);
		return barbarians;
	}
	
	public boolean isKingdom(String name) {
		boolean isKingdom = false;
		// Check for exact String key match
		isKingdom = kingdomMap.containsKey(name);
		if(isKingdom) {
			return true;
		} else {
			// Check for case-insensitive name
			for(String kingdomName : kingdomMap.keySet()) {
				if(name.equalsIgnoreCase(kingdomName)) {
					return true;
				}
			}
		}
		// Could not find a kingdom by name
		return false;
	}
	
	public KonKingdom getBarbarians() {
		return barbarians;
	}
	
	public KonKingdom getNeutrals() {
		return neutrals;
	}
	
	/**
	 * Updates a town option based on action
	 * @param action
	 * @param town
	 * @param bukkitPlayer
	 * @return true if action was successful
	 */
	public boolean changeTownOption(optionAction action, KonTown town, Player bukkitPlayer) {
		boolean result = false;
		switch(action) {
			case TOWN_OPEN:
				// Verify town lord
				if(town.isPlayerLord(bukkitPlayer)) {
					if(town.isOpen()) {
						// Close the town
	            		town.setIsOpen(false);
	            		for(OfflinePlayer resident : town.getPlayerResidents()) {
			    			if(resident.isOnline()) {
			    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_CLOSE.getMessage(town.getName()));
			    			}
			    		}
					} else {
						// Open the town
						town.setIsOpen(true);
	            		for(OfflinePlayer resident : town.getPlayerResidents()) {
			    			if(resident.isOnline()) {
			    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_OPEN.getMessage(town.getName()));
			    			}
			    		}
					}
					result = true;
            	} else {
            		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
            	}
				break;
			case TOWN_REDSTONE:
				// Verify town lord
				if(town.isPlayerLord(bukkitPlayer)) {
					if(town.isEnemyRedstoneAllowed()) {
						// Disable enemy redstone
	            		town.setIsEnemyRedstoneAllowed(false);
	            		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_REDSTONE_DISABLE.getMessage(town.getName()));
					} else {
						// Enable enemy redstone
						town.setIsEnemyRedstoneAllowed(true);
						ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_REDSTONE_ENABLE.getMessage(town.getName()));
					}
					result = true;
            	} else {
            		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
            	}
				break;
			default:
				break;
		}
		return result;
	}
	
	private void makeTownNerfs() {
		townNerfs.put(PotionEffectType.SLOW_DIGGING, 0);
	}
	
	public void applyTownNerf(KonPlayer player, KonTown town) {
		if(!player.isAdminBypassActive()) {
			for(PotionEffectType pot : townNerfs.keySet()) {
				int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.FATIGUE);
				int upgradedPotLevel = townNerfs.get(pot) + upgradeLevel;
				player.getBukkitPlayer().addPotionEffect(new PotionEffect(pot, 9999999, upgradedPotLevel));
			}
		}
	}
	
	public void clearTownNerf(KonPlayer player) {
		for(PotionEffectType pot : townNerfs.keySet()) {
			player.getBukkitPlayer().removePotionEffect(pot);
		}
	}
	
	public void clearAllTownNerfs(KonTown town) {
		for(KonPlayer player : konquest.getPlayerManager().getPlayersOnline()) {
			if(town.isLocInside(player.getBukkitPlayer().getLocation())) {
				clearTownNerf(player);
			}
		}
	}
	
	public void refreshTownNerfs(KonTown town) {
		for(KonPlayer player : konquest.getPlayerManager().getPlayersOnline()) {
			if(town.isLocInside(player.getBukkitPlayer().getLocation())) {
				if(player.getKingdom().equals(town.getKingdom())) {
					clearTownNerf(player);
				} else {
					applyTownNerf(player, town);
				}
			}
		}
	}
	
	public Set<PotionEffectType> getTownNerfs() {
		return townNerfs.keySet();
	}
	
	public boolean isTownNerf(PotionEffectType pot) {
		return townNerfs.containsKey(pot);
	}
	
	public void applyTownHearts(KonPlayer player, KonTown town) {
		if(player.getKingdom().equals(town.getKingdom())) {
			int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.HEALTH);
			if(upgradeLevel >= 1) {
				double upgradedBaseHealth = 20 + (upgradeLevel*2);
				player.getBukkitPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(upgradedBaseHealth);
				//ChatUtil.printDebug("Applied max health attribute "+upgradedBaseHealth+" to player "+player.getBukkitPlayer().getName());
			} else {
				clearTownHearts(player);
			}
		}
	}
	
	public void clearTownHearts(KonPlayer player) {
		double defaultValue = player.getBukkitPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue();
		player.getBukkitPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(defaultValue);
		//ChatUtil.printDebug("Cleared max health attribute "+defaultValue+" for player "+player.getBukkitPlayer().getName());
	}
	
	public void clearAllTownHearts(KonTown town) {
		for(KonPlayer player : konquest.getPlayerManager().getPlayersOnline()) {
			if(town.isLocInside(player.getBukkitPlayer().getLocation())) {
				clearTownHearts(player);
			}
		}
	}
	
	public void refreshTownHearts(KonTown town) {
		for(KonPlayer player : konquest.getPlayerManager().getPlayersOnline()) {
			if(town.isLocInside(player.getBukkitPlayer().getLocation())) {
				if(player.getKingdom().equals(town.getKingdom())) {
					applyTownHearts(player, town);
				} else {
					clearTownHearts(player);
				}
			}
		}
	}

	public void updateKingdomOfflineProtection() {
		boolean isOfflineProtectedEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.no_enemy_edit_offline",true);
		int offlineProtectedWarmupSeconds = konquest.getConfigManager().getConfig("core").getInt("core.kingdoms.no_enemy_edit_offline_warmup",0);
		int offlineProtectedMinimumPlayers = konquest.getConfigManager().getConfig("core").getInt("core.kingdoms.no_enemy_edit_offline_minimum",0);
		if(isOfflineProtectedEnabled) {
			// For each kingdom, start protection warmup timer if no players are online, else ensure the timer is stopped
			for(KonKingdom kingdom : getKingdoms()) {
				ArrayList<KonPlayer> onlineKingdomPlayers = konquest.getPlayerManager().getPlayersInKingdom(kingdom.getName());
				int numOnlineKingdomPlayers = onlineKingdomPlayers.size();
				if((offlineProtectedMinimumPlayers <= 0 && onlineKingdomPlayers.isEmpty()) || (numOnlineKingdomPlayers < offlineProtectedMinimumPlayers)) {
					Timer protectedWarmupTimer = kingdom.getProtectedWarmupTimer();
					if(offlineProtectedWarmupSeconds > 0 && protectedWarmupTimer.getTime() == -1 && !kingdom.isOfflineProtected()) {
						// start timer
						ChatUtil.printDebug("Starting kingdom protection warmup timer for "+offlineProtectedWarmupSeconds+" seconds: "+kingdom.getName());
						protectedWarmupTimer.stopTimer();
						protectedWarmupTimer.setTime(offlineProtectedWarmupSeconds);
						protectedWarmupTimer.startTimer();
						int warmupMinutes = offlineProtectedWarmupSeconds / 60;
						int warmupSeconds = offlineProtectedWarmupSeconds % 60;
						String warmupTime = String.format("%d:%02d", warmupMinutes , warmupSeconds);
						//ChatUtil.sendBroadcast(ChatColor.LIGHT_PURPLE+"The Kingdom of "+ChatColor.RED+kingdom.getName()+ChatColor.LIGHT_PURPLE+" will be protected in "+ChatColor.AQUA+warmupTime+ChatColor.LIGHT_PURPLE+" minutes.");
						ChatUtil.sendBroadcast(ChatColor.LIGHT_PURPLE+MessagePath.PROTECTION_NOTICE_KINGDOM_WARMUP.getMessage(kingdom.getName(),warmupTime));
					}
				} else {
					// stop timer, clear protection
					//ChatUtil.printDebug("Clearing kingdom protection: "+kingdom.getName());
					Timer protectedWarmupTimer = kingdom.getProtectedWarmupTimer();
					kingdom.setOfflineProtected(false);
					protectedWarmupTimer.stopTimer();
				}
			}
		}
	}
	
	public int getPlayerLordships(KonPlayer player) {
		int count = 0;
		for(KonTown town : player.getKingdom().getTowns()) {
			if(town.isPlayerLord(player.getOfflineBukkitPlayer())) {
				count = count + 1;
			}
		}
		return count;
	}
	
	public List<KonTown> getPlayerLordshipTowns(KonOfflinePlayer player) {
		List<KonTown> townNames = new ArrayList<KonTown>();
		for(KonTown town : player.getKingdom().getTowns()) {
			if(town.isPlayerLord(player.getOfflineBukkitPlayer())) {
				townNames.add(town);
			}
		}
		return townNames;
	}
	
	public List<KonTown> getPlayerResidenceTowns(KonOfflinePlayer player) {
		List<KonTown> townNames = new ArrayList<KonTown>();
		for(KonTown town : player.getKingdom().getTowns()) {
			if(town.isPlayerResident(player.getOfflineBukkitPlayer())) {
				townNames.add(town);
			}
		}
		return townNames;
	}
	
	public void updatePlayerMembershipStats(KonPlayer player) {
		int countLord = 0;
		int countElite = 0;
		int countResident = 0;
		for(KonTown town : player.getKingdom().getTowns()) {
			if(town.isPlayerLord(player.getOfflineBukkitPlayer())) {
				countLord = countLord + 1;
			} else if(town.isPlayerElite(player.getOfflineBukkitPlayer())) {
				countElite = countElite + 1;
			} else if(town.isPlayerResident(player.getOfflineBukkitPlayer())) {
				countResident = countResident + 1;
			}
		}
		player.getPlayerStats().setStat(KonStatsType.LORDS, countLord);
		player.getPlayerStats().setStat(KonStatsType.KNIGHTS, countElite);
		player.getPlayerStats().setStat(KonStatsType.RESIDENTS, countResident);
	}
	
	public KonLeaderboard getKingdomLeaderboard(KonKingdom kingdom) {
		KonLeaderboard leaderboard = new KonLeaderboard();
		if(kingdom.equals(barbarians)) {
			return leaderboard;
		}
		// Determine scores for all players within towns
		HashMap<OfflinePlayer,KonPlayerScoreAttributes> memberScores = new HashMap<OfflinePlayer,KonPlayerScoreAttributes>();
		int numTownLords = 0;
		int numTownKnights = 0;
		int numTownResidents = 0;
		int numLordLand = 0;
		int numKnightLand = 0;
		int numResidentLand = 0;
		for(KonTown town : kingdom.getTowns()) {
			for(OfflinePlayer offlinePlayer : town.getPlayerResidents()) {
				numTownLords = 0;
				numTownKnights = 0;
				numTownResidents = 0;
				numLordLand = 0;
				numKnightLand = 0;
				numResidentLand = 0;
				if(town.isPlayerLord(offlinePlayer)) {
					numTownLords++;
					numLordLand += town.getChunkList().size();
				} else if(town.isPlayerElite(offlinePlayer)) {
					numTownKnights++;
					numKnightLand += town.getChunkList().size();
				} else {
					numTownResidents++;
					numResidentLand += town.getChunkList().size();
				}
				if(memberScores.containsKey(offlinePlayer)) {
					memberScores.get(offlinePlayer).addAttribute(KonPlayerScoreAttribute.TOWN_LORDS, numTownLords);
					memberScores.get(offlinePlayer).addAttribute(KonPlayerScoreAttribute.TOWN_KNIGHTS, numTownKnights);
					memberScores.get(offlinePlayer).addAttribute(KonPlayerScoreAttribute.TOWN_RESIDENTS, numTownResidents);
					memberScores.get(offlinePlayer).addAttribute(KonPlayerScoreAttribute.LAND_LORDS, numLordLand);
					memberScores.get(offlinePlayer).addAttribute(KonPlayerScoreAttribute.LAND_KNIGHTS, numKnightLand);
					memberScores.get(offlinePlayer).addAttribute(KonPlayerScoreAttribute.LAND_RESIDENTS, numResidentLand);
				} else {
					KonOfflinePlayer validPlayer = konquest.getPlayerManager().getOfflinePlayer(offlinePlayer);
					if(validPlayer != null && validPlayer.getKingdom().equals(kingdom)) {
						KonPlayerScoreAttributes newMemberAttributes = new KonPlayerScoreAttributes();
						newMemberAttributes.setAttribute(KonPlayerScoreAttribute.TOWN_LORDS, numTownLords);
						newMemberAttributes.setAttribute(KonPlayerScoreAttribute.TOWN_KNIGHTS, numTownKnights);
						newMemberAttributes.setAttribute(KonPlayerScoreAttribute.TOWN_RESIDENTS, numTownResidents);
						newMemberAttributes.setAttribute(KonPlayerScoreAttribute.LAND_LORDS, numLordLand);
						newMemberAttributes.setAttribute(KonPlayerScoreAttribute.LAND_KNIGHTS, numKnightLand);
						newMemberAttributes.setAttribute(KonPlayerScoreAttribute.LAND_RESIDENTS, numResidentLand);
						memberScores.put(offlinePlayer, newMemberAttributes);
					}
				}
			}
		}
		// Find top scoring members
		final int NUM_TOP_PLAYERS = 9;
		int topScore = 0;
		OfflinePlayer topPlayer = null;
		for(int i = 0;i<NUM_TOP_PLAYERS;i++) {
			if(!memberScores.isEmpty()) {
				for(OfflinePlayer offlinePlayer : memberScores.keySet()) {
					int playerScore = memberScores.get(offlinePlayer).getScore();
					if(playerScore > topScore) {
						topScore = playerScore;
						topPlayer = offlinePlayer;
					}
				}
				if(topPlayer != null) {
					leaderboard.addEntry(topPlayer,topScore);
					//ChatUtil.printDebug("Found leaderboard member "+topPlayer.getName()+", score: "+topScore);
					memberScores.remove(topPlayer);
					topScore = 0;
					topPlayer = null;
				}
			}
		}
		return leaderboard;
	}
	
	public KonKingdomScoreAttributes getKingdomScoreAttributes(KonKingdom kingdom) {
		KonKingdomScoreAttributes scoreAttributes = new KonKingdomScoreAttributes();
		if(!kingdom.equals(barbarians) && !kingdom.isPeaceful()) {
			// Gather Kingdom metrics
			int numKingdomTowns = kingdom.getTowns().size();
	    	ArrayList<KonOfflinePlayer> allPlayersInKingdom = konquest.getPlayerManager().getAllPlayersInKingdom(kingdom.getName());
	    	int numAllKingdomPlayers = allPlayersInKingdom.size();
	    	int numKingdomLand = 0;
	    	for(KonTown town : kingdom.getTowns()) {
	    		numKingdomLand += town.getChunkList().size();
	    	}
	    	int numKingdomFavor = 0;
	    	for(KonOfflinePlayer kingdomPlayer : allPlayersInKingdom) {
	    		numKingdomFavor += (int) KonquestPlugin.getBalance(kingdomPlayer.getOfflineBukkitPlayer());
	    	}
	    	// Gather favor costs
	    	int cost_settle = (int)konquest.getConfigManager().getConfig("core").getDouble("core.favor.cost_settle");
	    	int cost_claim = (int)konquest.getConfigManager().getConfig("core").getDouble("core.favor.cost_claim");
	    	// Set attributes
	    	scoreAttributes.setAttributeWeight(KonKingdomScoreAttribute.TOWNS, cost_settle+2);
	    	scoreAttributes.setAttributeWeight(KonKingdomScoreAttribute.LAND, cost_claim+1);
	    	scoreAttributes.setAttribute(KonKingdomScoreAttribute.TOWNS, numKingdomTowns);
	    	scoreAttributes.setAttribute(KonKingdomScoreAttribute.LAND, numKingdomLand);
	    	scoreAttributes.setAttribute(KonKingdomScoreAttribute.FAVOR, numKingdomFavor);
	    	scoreAttributes.setAttribute(KonKingdomScoreAttribute.POPULATION, numAllKingdomPlayers);
		}
		return scoreAttributes;
	}
	
	public int getKingdomScore(KonKingdom kingdom) {
		int score = getKingdomScoreAttributes(kingdom).getScore();
		return score;
	}
	
	public KonPlayerScoreAttributes getPlayerScoreAttributes(KonOfflinePlayer offlinePlayer) {
		KonPlayerScoreAttributes scoreAttributes = new KonPlayerScoreAttributes();
		if(!offlinePlayer.isBarbarian() && !offlinePlayer.getKingdom().isPeaceful()) {
			// Gather player metrics
			int numTownLords = 0;
			int numTownKnights = 0;
			int numTownResidents = 0;
			int numLordLand = 0;
			int numKnightLand = 0;
			int numResidentLand = 0;
			for(KonTown town : offlinePlayer.getKingdom().getTowns()) {
				if(town.isPlayerLord(offlinePlayer.getOfflineBukkitPlayer())) {
					numTownLords++;
					numLordLand += town.getChunkList().size();
				} else if(town.isPlayerElite(offlinePlayer.getOfflineBukkitPlayer())) {
					numTownKnights++;
					numKnightLand += town.getChunkList().size();
				} else if(town.isPlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
					numTownResidents++;
					numResidentLand += town.getChunkList().size();
				}
			}
			scoreAttributes.setAttribute(KonPlayerScoreAttribute.TOWN_LORDS, numTownLords);
			scoreAttributes.setAttribute(KonPlayerScoreAttribute.TOWN_KNIGHTS, numTownKnights);
			scoreAttributes.setAttribute(KonPlayerScoreAttribute.TOWN_RESIDENTS, numTownResidents);
			scoreAttributes.setAttribute(KonPlayerScoreAttribute.LAND_LORDS, numLordLand);
			scoreAttributes.setAttribute(KonPlayerScoreAttribute.LAND_KNIGHTS, numKnightLand);
			scoreAttributes.setAttribute(KonPlayerScoreAttribute.LAND_RESIDENTS, numResidentLand);
		}
		return scoreAttributes;
	}
	
	public int getPlayerScore(KonOfflinePlayer offlinePlayer) {
		int score = getPlayerScoreAttributes(offlinePlayer).getScore();
		return score;
	}
	
	/**
	 * Determines all block locations at the top of a chunk which lie on the border of the chunk's territory.
	 * @param nearbyChunks
	 * @param player
	 * @return
	 */
	public HashMap<Location,Color> getBorderLocationMap(ArrayList<Chunk> renderChunks, KonPlayer player) {
		//TODO: Poor coding style, optimize with a general FOR loop
		HashMap<Location,Color> locationMap = new HashMap<Location,Color>();
		// Evaluate every chunk in the provided list. If it's claimed, check each adjacent chunk and determine border locations
		for(Chunk chunk : renderChunks) {
			Point point = konquest.toPoint(chunk);
			World renderWorld = chunk.getWorld();
			if(isChunkClaimed(point,renderWorld)) {
				KonKingdom chunkKingdom = getChunkTerritory(point,renderWorld).getKingdom();
				Location renderLoc;
				Color renderColor;
				if(chunkKingdom.equals(getBarbarians())) {
					renderColor = Color.YELLOW;
				} else if(chunkKingdom.equals(getNeutrals())) {
					renderColor = Color.GRAY;
				} else {
					if(player.getKingdom().equals(chunkKingdom)) {
						renderColor = Color.GREEN;
					} else {
						renderColor = Color.RED;
					}
				}
				Point sidePoint;
				boolean isClaimed = false;
				int z_fixed, x_fixed;
				int y_max;
				double y_mod;
				final int Y_MIN_LIMIT = 1;
				final int Y_MAX_LIMIT = 256;
				final double X_MOD = 0.5;
				final double Y_MOD = 1;
				final double Y_MOD_SNOW = 1.1;
				final double Z_MOD = 0.5;
				// Side chunk in +Z direction
				sidePoint = new Point(point.x + 0, point.y + 1);
				isClaimed = isChunkClaimed(sidePoint,renderWorld);
				if(!isClaimed || (isClaimed && !getChunkTerritory(sidePoint,renderWorld).getKingdom().equals(chunkKingdom))) {
					ChunkSnapshot chunkSnap = chunk.getChunkSnapshot(true,false,false);
					z_fixed = 15;
					y_max = 0;
					for(int x = 0;x<16;x++) {
						y_max = chunkSnap.getHighestBlockYAt(x, z_fixed);
						while((chunk.getBlock(x, y_max, z_fixed).isPassable() || !chunk.getBlock(x, y_max, z_fixed).getType().isOccluding()) && y_max > Y_MIN_LIMIT) {
							y_max--;
						}
						while(chunk.getBlock(x, y_max+1, z_fixed).isLiquid() && y_max < Y_MAX_LIMIT) {
							y_max++;
						}
						Block renderBlock = chunk.getBlock(x, y_max, z_fixed);
						Block aboveBlock = chunk.getBlock(x, y_max+1, z_fixed);
						y_mod = Y_MOD;
						if(aboveBlock.getBlockData() instanceof Snow) {
							Snow snowBlock = (Snow)aboveBlock.getBlockData();
							if(snowBlock.getLayers() >= snowBlock.getMinimumLayers()) {
								y_mod = Y_MOD_SNOW;
							}
						}
						renderLoc = new Location(renderBlock.getWorld(),renderBlock.getLocation().getX()+X_MOD,renderBlock.getLocation().getY()+y_mod,renderBlock.getLocation().getZ()+Z_MOD);
						locationMap.put(renderLoc, renderColor);
					}
				}
				// Side chunk in -Z direction
				sidePoint = new Point(point.x + 0, point.y - 1);
				isClaimed = isChunkClaimed(sidePoint,renderWorld);
				if(!isClaimed || (isClaimed && !getChunkTerritory(sidePoint,renderWorld).getKingdom().equals(chunkKingdom))) {
					ChunkSnapshot chunkSnap = chunk.getChunkSnapshot(true,false,false);
					z_fixed = 0;
					y_max = 0;
					for(int x = 0;x<16;x++) {
						y_max = chunkSnap.getHighestBlockYAt(x, z_fixed);
						while((chunk.getBlock(x, y_max, z_fixed).isPassable() || !chunk.getBlock(x, y_max, z_fixed).getType().isOccluding()) && y_max > Y_MIN_LIMIT) {
							y_max--;
						}
						while(chunk.getBlock(x, y_max+1, z_fixed).isLiquid() && y_max < Y_MAX_LIMIT) {
							y_max++;
						}
						Block renderBlock = chunk.getBlock(x, y_max, z_fixed);
						Block aboveBlock = chunk.getBlock(x, y_max+1, z_fixed);
						y_mod = Y_MOD;
						if(aboveBlock.getBlockData() instanceof Snow) {
							Snow snowBlock = (Snow)aboveBlock.getBlockData();
							if(snowBlock.getLayers() >= snowBlock.getMinimumLayers()) {
								y_mod = Y_MOD_SNOW;
							}
						}
						renderLoc = new Location(renderBlock.getWorld(),renderBlock.getLocation().getX()+X_MOD,renderBlock.getLocation().getY()+y_mod,renderBlock.getLocation().getZ()+Z_MOD);
						locationMap.put(renderLoc, renderColor);
					}
				}
				// Side chunk in +X direction
				sidePoint = new Point(point.x + 1, point.y + 0);
				isClaimed = isChunkClaimed(sidePoint,renderWorld);
				if(!isClaimed || (isClaimed && !getChunkTerritory(sidePoint,renderWorld).getKingdom().equals(chunkKingdom))) {
					ChunkSnapshot chunkSnap = chunk.getChunkSnapshot(true,false,false);
					x_fixed = 15;
					y_max = 0;
					for(int z = 0;z<16;z++) {
						y_max = chunkSnap.getHighestBlockYAt(x_fixed, z);
						while((chunk.getBlock(x_fixed, y_max, z).isPassable() || !chunk.getBlock(x_fixed, y_max, z).getType().isOccluding()) && y_max > Y_MIN_LIMIT) {
							y_max--;
						}
						while(chunk.getBlock(x_fixed, y_max+1, z).isLiquid() && y_max < Y_MAX_LIMIT) {
							y_max++;
						}
						Block renderBlock = chunk.getBlock(x_fixed, y_max, z);
						Block aboveBlock = chunk.getBlock(x_fixed, y_max+1, z);
						y_mod = Y_MOD;
						if(aboveBlock.getBlockData() instanceof Snow) {
							Snow snowBlock = (Snow)aboveBlock.getBlockData();
							if(snowBlock.getLayers() >= snowBlock.getMinimumLayers()) {
								y_mod = Y_MOD_SNOW;
							}
						}
						renderLoc = new Location(renderBlock.getWorld(),renderBlock.getLocation().getX()+X_MOD,renderBlock.getLocation().getY()+y_mod,renderBlock.getLocation().getZ()+Z_MOD);
						locationMap.put(renderLoc, renderColor);
					}
				}
				// Side chunk in -X direction
				sidePoint = new Point(point.x - 1, point.y + 0);
				isClaimed = isChunkClaimed(sidePoint,renderWorld);
				if(!isClaimed || (isClaimed && !getChunkTerritory(sidePoint,renderWorld).getKingdom().equals(chunkKingdom))) {
					ChunkSnapshot chunkSnap = chunk.getChunkSnapshot(true,false,false);
					x_fixed = 0;
					y_max = 0;
					for(int z = 0;z<16;z++) {
						y_max = chunkSnap.getHighestBlockYAt(x_fixed, z);
						while((chunk.getBlock(x_fixed, y_max, z).isPassable() || !chunk.getBlock(x_fixed, y_max, z).getType().isOccluding()) && y_max > Y_MIN_LIMIT) {
							y_max--;
						}
						while(chunk.getBlock(x_fixed, y_max+1, z).isLiquid() && y_max < Y_MAX_LIMIT) {
							y_max++;
						}
						Block renderBlock = chunk.getBlock(x_fixed, y_max, z);
						Block aboveBlock = chunk.getBlock(x_fixed, y_max+1, z);
						y_mod = Y_MOD;
						if(aboveBlock.getBlockData() instanceof Snow) {
							Snow snowBlock = (Snow)aboveBlock.getBlockData();
							if(snowBlock.getLayers() >= snowBlock.getMinimumLayers()) {
								y_mod = Y_MOD_SNOW;
							}
						}
						renderLoc = new Location(renderBlock.getWorld(),renderBlock.getLocation().getX()+X_MOD,renderBlock.getLocation().getY()+y_mod,renderBlock.getLocation().getZ()+Z_MOD);
						locationMap.put(renderLoc, renderColor);
					}
				}
			}
		}
		return locationMap;
	}
	
	public void updatePlayerBorderParticles(KonPlayer player, Location loc) {
    	if(player != null) {
			// Border particle update
			ArrayList<Chunk> nearbyChunks = konquest.getAreaChunks(loc, 2);
			boolean isTerritoryNearby = false;
			for(Chunk chunk : nearbyChunks) {
				if(isChunkClaimed(konquest.toPoint(chunk),chunk.getWorld())) {
					isTerritoryNearby = true;
					break;
				}
			}
			if(isTerritoryNearby) {
				// Player is nearby a territory, render border particles
				Timer borderTimer = player.getBorderUpdateLoopTimer();
				borderTimer.stopTimer();
	    		borderTimer.setTime(1);
				HashMap<Location,Color> borderTerritoryMap = getBorderLocationMap(nearbyChunks, player);
	    		player.removeAllBorders();
	    		player.addTerritoryBorders(borderTerritoryMap);
	        	borderTimer.startLoopTimer(10);
			} else {
				// Player is not nearby a territory, stop rendering
				stopPlayerBorderParticles(player);
			}
    	}
    }
	
	public void stopPlayerBorderParticles(KonPlayer player) {
		Timer borderTimer = player.getBorderUpdateLoopTimer();
		if(borderTimer.isRunning()) {
			//ChatUtil.printDebug("Stopping border loop timer for player "+player.getBukkitPlayer().getName());
			borderTimer.stopTimer();
		}
		player.removeAllBorders();
	}
	
	public void updateAllTownDisabledUpgrades() {
		for(KonKingdom kingdom : getKingdoms()) {
			for(KonTown town : kingdom.getTowns()) {
				konquest.getUpgradeManager().updateTownDisabledUpgrades(town);
			}
		}
	}
	
	public Material getTownCriticalBlock() {
		return townCriticalBlock;
	}
	
	private void loadCriticalBlocks() {
		String townCriticalBlockTypeName = konquest.getConfigManager().getConfig("core").getString("core.monuments.critical_block","");
		try {
			townCriticalBlock = Material.valueOf(townCriticalBlockTypeName);
		} catch(IllegalArgumentException e) {
			String message = "Invalid monument critical block \""+townCriticalBlockTypeName+"\" given in core.monuments.critical_block, using default OBSIDIAN";
    		ChatUtil.printConsoleError(message);
    		konquest.opStatusMessages.add(message);
		}
	}
	
	private void loadKingdoms() {
		//ScheduledThreadPoolExecutor service = new ScheduledThreadPoolExecutor(1);
		FileConfiguration kingdomsConfig = konquest.getConfigManager().getConfig("kingdoms");
        if (kingdomsConfig.get("kingdoms") == null) {
        	ChatUtil.printDebug("There is no kingdoms section in kingdoms.yml");
            return;
        }
        boolean isShieldsEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.towns.enable_shields",false);
        boolean isArmorsEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.towns.enable_armor",false);
        double x,y,z;
        float pitch,yaw;
        List<Double> sectionList;
        String worldName;
        String defaultWorldName = konquest.getConfigManager().getConfig("core").getString("core.world_name","world");
        // Count all towns
        int numTowns = 0;
        for(String kingdomName : kingdomsConfig.getConfigurationSection("kingdoms").getKeys(false)) {
        	numTowns += kingdomsConfig.getConfigurationSection("kingdoms."+kingdomName+".towns").getKeys(false).size();
        }
        LoadingPrinter loadBar = new LoadingPrinter(numTowns,"Loading "+numTowns+" Towns");
        // Load all Kingdoms
        for(String kingdomName : kingdomsConfig.getConfigurationSection("kingdoms").getKeys(false)) {
        	//ChatUtil.printDebug("Loading Kingdom: "+kingdomName);
        	ConfigurationSection kingdomSection = kingdomsConfig.getConfigurationSection("kingdoms."+kingdomName);
        	boolean isPeaceful = kingdomSection.getBoolean("peaceful", false);
        	ConfigurationSection monumentSection = kingdomsConfig.getConfigurationSection("kingdoms."+kingdomName+".monument");
        	// Create Kingdoms and Capitals
        	ConfigurationSection capitalSection = kingdomsConfig.getConfigurationSection("kingdoms."+kingdomName+".capital");
        	World capitalWorld = null;
        	if(capitalSection != null) {
        		worldName = capitalSection.getString("world",defaultWorldName);
        		capitalWorld = Bukkit.getWorld(worldName);
        		if(capitalWorld != null) {
        			// Create capital territory
	        		sectionList = capitalSection.getDoubleList("spawn");
	        		x = sectionList.get(0);
	        		y = sectionList.get(1);
	        		z = sectionList.get(2);
	        		if(sectionList.size() > 3) {
	        			double val = sectionList.get(3);
	        			pitch = (float)val;
	        		} else {
	        			pitch = 0;	
	        		}
	        		if(sectionList.size() > 4) {
	        			double val = sectionList.get(4);
	        			yaw = (float)val;
	        		} else {
	        			yaw = 0;	
	        		}
		        	Location capital_spawn = new Location(capitalWorld,x,y,z,yaw,pitch);
		        	sectionList = capitalSection.getDoubleList("center");
	        		x = sectionList.get(0);
	        		y = sectionList.get(1);
	        		z = sectionList.get(2);
		        	Location capital_center = new Location(capitalWorld,x,y,z);
		        	// Create Kingdom and capital
		        	addKingdom(capital_center, kingdomName);
		        	// Set Peaceful mode
		        	kingdomMap.get(kingdomName).setPeaceful(isPeaceful);
		        	// Set Capital spawn point
		        	kingdomMap.get(kingdomName).getCapital().setSpawn(capital_spawn);
		        	// Add all Capital chunk claims
		        	kingdomMap.get(kingdomName).getCapital().addPoints(konquest.formatStringToPoints(capitalSection.getString("chunks")));
		        	// Update territory cache
		        	addAllTerritory(capitalWorld,kingdomMap.get(kingdomName).getCapital().getChunkList());
		        	// Create Monument Templates
		        	if(monumentSection != null) {
		        		sectionList = monumentSection.getDoubleList("travel");
		        		x = sectionList.get(0);
		        		y = sectionList.get(1);
		        		z = sectionList.get(2);
			        	Location monument_travel = new Location(capitalWorld,x,y,z);
			        	sectionList = monumentSection.getDoubleList("cornerone");
		        		x = sectionList.get(0);
		        		y = sectionList.get(1);
		        		z = sectionList.get(2);
			        	Location monument_cornerone = new Location(capitalWorld,x,y,z);
			        	sectionList = monumentSection.getDoubleList("cornertwo");
		        		x = sectionList.get(0);
		        		y = sectionList.get(1);
		        		z = sectionList.get(2);
			        	Location monument_cornertwo = new Location(capitalWorld,x,y,z);
			        	// Create a Monument Template region for current Kingdom
		        		int status = kingdomMap.get(kingdomName).createMonumentTemplate(monument_cornerone, monument_cornertwo, monument_travel);
		        		if(status != 0) {
		        			String message = "Failed to load Monument Template for Kingdom "+kingdomName+", ";
		        			switch(status) {
			        			case 1:
			        				message = message+"base dimensions are not 16x16 blocks.";
			        				break;
			        			case 2:
			        				message = message+"region does not contain enough critical blocks.";
			        				break;
			        			case 3:
			        				message = message+"region does not contain a travel point.";
			        				break;
			        			case 4:
			        				message = message+"region is not within Capital territory.";
			        				break;
		        				default:
		        					message = message+"unknown reason.";
		        					break;
		        			}
		        			ChatUtil.printConsoleError(message);
		        			konquest.opStatusMessages.add(message);
		        		}
		        	} else {
		        		ChatUtil.printConsoleError("Null monument template for Kingdom "+kingdomName+" in config file!");
		        		konquest.opStatusMessages.add("Missing monument template for Kingdom "+kingdomName+" in kingdoms.yml config file. Use \"/k admin monument\" to define the monument template for this Kingdom.");
		        	}
        		} else {
        			String message = "Failed to load territory "+kingdomName+" Capital in an unloaded world, "+worldName+". Check plugin load order.";
        			ChatUtil.printConsoleError(message);
        			konquest.opStatusMessages.add(message);
        		}
        	} else {
        		String message = "Failed to load capital for Kingdom "+kingdomName+", is one created?";
        		ChatUtil.printConsoleError(message);
        		konquest.opStatusMessages.add(message);
        	}
        	
        	// Load all towns
        	boolean isMissingMonuments = false;
        	for(String townName : kingdomsConfig.getConfigurationSection("kingdoms."+kingdomName+".towns").getKeys(false)) {
        		//ChatUtil.printDebug("Loading Town: "+townName);
            	ConfigurationSection townSection = kingdomsConfig.getConfigurationSection("kingdoms."+kingdomName+".towns."+townName);
            	if(townSection != null) {
            		worldName = townSection.getString("world",defaultWorldName);
            		World townWorld = Bukkit.getServer().getWorld(worldName);
            		if(townWorld != null) {
            			// Create town
	            		int base = townSection.getInt("base");
	            		sectionList = townSection.getDoubleList("spawn");
	            		x = sectionList.get(0);
	            		y = sectionList.get(1);
	            		z = sectionList.get(2);
		            	Location town_spawn = new Location(townWorld,x,y,z);
		            	sectionList = townSection.getDoubleList("center");
	            		x = sectionList.get(0);
	            		y = sectionList.get(1);
	            		z = sectionList.get(2);
		            	Location town_center = new Location(townWorld,x,y,z);
		            	// Create Town
		            	kingdomMap.get(kingdomName).addTown(town_center, townName);
		            	KonTown town = kingdomMap.get(kingdomName).getTown(townName);
		            	// Set town spawn point
		            	town.setSpawn(town_spawn);
		            	// Setup town monument parameters from template
		            	town.loadMonument(base, kingdomMap.get(kingdomName).getMonumentTemplate());
		            	if(!kingdomMap.get(kingdomName).getMonumentTemplate().isValid()) {
		            		isMissingMonuments = true;
		            		ChatUtil.printConsoleError("Failed to load monument for Town "+townName+" in kingdom "+kingdomName+" from invalid template");
		            	}
		            	// Add all Town chunk claims
		            	town.addPoints(konquest.formatStringToPoints(townSection.getString("chunks")));
		            	// Update territory cache
			        	addAllTerritory(townWorld,town.getChunkList());
			        	// Set shield
			        	boolean isShieldActive = townSection.getBoolean("shield",false);
			        	int shieldTime = townSection.getInt("shield_time",0);
			        	Date now = new Date();
			        	if(isShieldsEnabled && isShieldActive && shieldTime > (now.getTime()/1000)) {
			        		town.activateShield(shieldTime);
			        	}
			        	// Set armor
			        	boolean isArmorActive = townSection.getBoolean("armor",false);
			        	int armorBlocks = townSection.getInt("armor_blocks",0);
			        	if(isArmorsEnabled && isArmorActive && armorBlocks > 0) {
			        		town.activateArmor(armorBlocks);
			        	}
		            	// Set open flag
		            	boolean isOpen = townSection.getBoolean("open",false);
		            	town.setIsOpen(isOpen);
		            	// Set redstone flag
		            	boolean isRedstone = townSection.getBoolean("redstone",false);
		            	town.setIsEnemyRedstoneAllowed(isRedstone);
		            	// Assign Lord
		            	String lordUUID = townSection.getString("lord","");
		            	if(!lordUUID.equalsIgnoreCase("")) {
		            		//ChatUtil.printDebug("Town "+townName+" Lord ID is: \""+lordUUID+"\"");
		            		UUID playerID = Konquest.idFromString(lordUUID);
		            		if(playerID != null) {
		            			town.setLord(playerID);
		            		} else {
		            			ChatUtil.printDebug("Town "+townName+" in kingdom "+kingdomName+" has a null UUID! Lord remains invalid");
		            		}
		            	} else {
		            		ChatUtil.printDebug("Town "+townName+" in kingdom "+kingdomName+" does not have a stored Lord ID");
		            	}
		            	// Populate Residents
		            	if(townSection.contains("residents")) {
			            	for(String residentUUID : townSection.getConfigurationSection("residents").getKeys(false)) {
			            		boolean isElite = townSection.getBoolean("residents."+residentUUID);
			            		town.addPlayerResident(Bukkit.getOfflinePlayer(UUID.fromString(residentUUID)),isElite);
			            	}
		            	} else {
		            		//ChatUtil.printDebug("Town "+townName+" does not have any stored residents");
		            	}
		            	// Add invite requests
		            	if(townSection.contains("requests")) {
		            		for(String requestUUID : townSection.getConfigurationSection("requests").getKeys(false)) {
		            			boolean type = townSection.getBoolean("requests."+requestUUID);
		            			town.addJoinRequest(UUID.fromString(requestUUID), type);
		            		}
		            	}
		            	// Add upgrades
		            	if(townSection.contains("upgrades")) {
		            		for(String upgradeName : townSection.getConfigurationSection("upgrades").getKeys(false)) {
		            			int level = townSection.getInt("upgrades."+upgradeName);
		            			KonUpgrade upgrade = KonUpgrade.getUpgrade(upgradeName);
		            			if(upgrade != null) {
		            				town.addUpgrade(upgrade, level);
		            			}
		            		}
		            	}
		            	// Update upgrade status
		            	konquest.getUpgradeManager().updateTownDisabledUpgrades(town);
	        			// Update loading bar
		            	loadBar.addProgress(1);
            		} else {
            			String message = "Failed to load town "+townName+" in an unloaded world, "+townWorld+". Check plugin load order.";
            			ChatUtil.printConsoleError(message);
            			konquest.opStatusMessages.add(message);
            		}
	            }
        	}
        	if(isMissingMonuments) {
        		konquest.opStatusMessages.add("Kingdom "+kingdomName+" has Towns with invalid Monuments. You must create a new Monument Template and restart the server.");
        	}
        }
		ChatUtil.printDebug("Loaded Kingdoms");
	}
	
	//TODO Save and update only items which have changed, do not delete everything and write all from memory
	public void saveKingdoms() {
		FileConfiguration kingdomsConfig = konquest.getConfigManager().getConfig("kingdoms");
		kingdomsConfig.set("kingdoms", null); // reset kingdoms config
		ConfigurationSection root = kingdomsConfig.createSection("kingdoms");
		for(KonKingdom kingdom : kingdomMap.values()) {
			ConfigurationSection kingdomSection = root.createSection(kingdom.getName());
			kingdomSection.set("peaceful",kingdom.isPeaceful());
            ConfigurationSection capitalSection = kingdomSection.createSection("capital");
            capitalSection.set("world", kingdom.getCapital().getWorld().getName());
            capitalSection.set("spawn", new int[] {(int) kingdom.getCapital().getSpawnLoc().getBlockX(),
												   (int) kingdom.getCapital().getSpawnLoc().getBlockY(),
												   (int) kingdom.getCapital().getSpawnLoc().getBlockZ(),
												   (int) kingdom.getCapital().getSpawnLoc().getPitch(),
												   (int) kingdom.getCapital().getSpawnLoc().getYaw()});
            capitalSection.set("center", new int[] {(int) kingdom.getCapital().getCenterLoc().getBlockX(),
					 								(int) kingdom.getCapital().getCenterLoc().getBlockY(),
					 								(int) kingdom.getCapital().getCenterLoc().getBlockZ()});
            capitalSection.set("chunks", konquest.formatPointsToString(kingdom.getCapital().getChunkList().keySet()));
            if(kingdom.getMonumentTemplate().isValid()) {
	            ConfigurationSection monumentSection = kingdomSection.createSection("monument");
	            monumentSection.set("travel", new int[] {(int) kingdom.getMonumentTemplate().getTravelPoint().getBlockX(),
														 (int) kingdom.getMonumentTemplate().getTravelPoint().getBlockY(),
														 (int) kingdom.getMonumentTemplate().getTravelPoint().getBlockZ()});
	            monumentSection.set("cornerone", new int[] {(int) kingdom.getMonumentTemplate().getCornerOne().getBlockX(),
						 								 	(int) kingdom.getMonumentTemplate().getCornerOne().getBlockY(),
						 								 	(int) kingdom.getMonumentTemplate().getCornerOne().getBlockZ()});
	            monumentSection.set("cornertwo", new int[] {(int) kingdom.getMonumentTemplate().getCornerTwo().getBlockX(),
						 								 	(int) kingdom.getMonumentTemplate().getCornerTwo().getBlockY(),
						 								 	(int) kingdom.getMonumentTemplate().getCornerTwo().getBlockZ()});
			} else {
				ChatUtil.printConsoleError("Failed to save invalid monument template for Kingdom "+kingdom.getName());
			}
            ConfigurationSection townsSection = kingdomSection.createSection("towns");
            for(KonTown town : kingdom.getTowns()) {
            	ConfigurationSection townInstanceSection = townsSection.createSection(town.getName());
            	townInstanceSection.set("world", town.getWorld().getName());
            	townInstanceSection.set("base", town.getMonument().getBaseY());
            	townInstanceSection.set("spawn", new int[] {(int) town.getSpawnLoc().getBlockX(),
						 								 	(int) town.getSpawnLoc().getBlockY(),
						 								 	(int) town.getSpawnLoc().getBlockZ()});
            	townInstanceSection.set("center", new int[] {(int) town.getCenterLoc().getBlockX(),
						 									 (int) town.getCenterLoc().getBlockY(),
						 									 (int) town.getCenterLoc().getBlockZ()});
                townInstanceSection.set("chunks", konquest.formatPointsToString(town.getChunkList().keySet()));
                townInstanceSection.set("open", town.isOpen());
                townInstanceSection.set("redstone", town.isEnemyRedstoneAllowed());
                townInstanceSection.set("shield", town.isShielded());
                townInstanceSection.set("shield_time", town.getShieldEndTime());
                townInstanceSection.set("armor", town.isArmored());
                townInstanceSection.set("armor_blocks", town.getArmorBlocks());
                townInstanceSection.set("lord", "");
                ConfigurationSection townInstanceResidentSection = townInstanceSection.createSection("residents");
                for(OfflinePlayer resident : town.getPlayerResidents()) {
                	String uuid = resident.getUniqueId().toString();
                	if(town.isPlayerLord(resident)) {
                		townInstanceSection.set("lord", uuid);
                	} else if(town.isPlayerElite(resident)) {
                		townInstanceResidentSection.set(uuid, true);
                	} else {
                		townInstanceResidentSection.set(uuid, false);
                	}
                }
                ConfigurationSection townInstanceRequestsSection = townInstanceSection.createSection("requests");
                for(OfflinePlayer requestee : town.getJoinRequests()) {
                	String uuid = requestee.getUniqueId().toString();
                	townInstanceRequestsSection.set(uuid, false);
                }
                for(OfflinePlayer invitee : town.getJoinInvites()) {
                	String uuid = invitee.getUniqueId().toString();
                	townInstanceRequestsSection.set(uuid, true);
                }
                ConfigurationSection townInstanceUpgradeSection = townInstanceSection.createSection("upgrades");
                for(KonUpgrade upgrade : KonUpgrade.values()) {
                	int level = town.getRawUpgradeLevel(upgrade);
                	if(level > 0) {
                		townInstanceUpgradeSection.set(upgrade.toString(), level);
                	}
                }
            }
		}
		ChatUtil.printDebug("Saved Kingdoms");
	}
	
	public void printPlayerMap(KonPlayer player, int mapSize) {
		printPlayerMap(player, mapSize, player.getBukkitPlayer().getLocation());
	}
	
	public void printPlayerMap(KonPlayer player, int mapSize, Location center) {
		Player bukkitPlayer = player.getBukkitPlayer();
		// Generate Map
    	Point originPoint = konquest.toPoint(center);
    	String mapWildSymbol = "-"; // "\u25A2";// empty square "-";
    	String mapTownSymbol = "+"; // "\u25A4";// plus in square "+";
    	String mapCampSymbol = "="; // "\u25A7";// minus in square "=";
    	String mapRuinSymbol = "%"; // "\u25A9";// dot in square "%";
    	String mapCapitalSymbol = "#"; // "\u25A5";// cross in square "#";
    	String[][] map = new String[mapSize][mapSize];
    	// Determine player's direction
    	BlockFace playerFace = bukkitPlayer.getFacing();
    	String mapPlayer = "!";
    	ChatColor playerColor = ChatColor.YELLOW;
    	if(playerFace.equals(BlockFace.NORTH)) {
    		mapPlayer = "\u25B2";// "\u25B3";// "^";
    	} else if(playerFace.equals(BlockFace.EAST)) {
    		mapPlayer = "\u25B6";// "\u25B7";// ">";
    	} else if(playerFace.equals(BlockFace.SOUTH)) {
    		mapPlayer = "\u25BC";// "\u25BD";// "v";
    	} else if(playerFace.equals(BlockFace.WEST)) {
    		mapPlayer = "\u25C0";// "\u25C1";// "<";
    	}
    	// Determine settlement status and proximity
    	KonTerritory closestTerritory = null;
    	boolean isLocValidSettle = true;
		int minDistance = Integer.MAX_VALUE;
		int proximity = Integer.MAX_VALUE;
		int searchDist = 0;
		int min_distance_capital = konquest.getConfigManager().getConfig("core").getInt("core.towns.min_distance_capital");
		int min_distance_town = konquest.getConfigManager().getConfig("core").getInt("core.towns.min_distance_town");
		if(!konquest.isWorldValid(center.getWorld())) {
			isLocValidSettle = false;
		}
		for(KonKingdom kingdom : kingdomMap.values()) {
			searchDist = Konquest.chunkDistance(center, kingdom.getCapital().getCenterLoc());
			if(searchDist != -1) {
				if(searchDist < minDistance) {
					minDistance = searchDist;
					proximity = searchDist;
					closestTerritory = kingdom.getCapital();
				}
				if(searchDist < min_distance_capital) {
					isLocValidSettle = false;
				}
			}
			for(KonTown town : kingdom.getTowns()) {
				searchDist = Konquest.chunkDistance(center, town.getCenterLoc());
				if(searchDist != -1) {
					if(searchDist < minDistance) {
						minDistance = searchDist;
						proximity = searchDist;
						closestTerritory = town;
					}
					if(searchDist < min_distance_town) {
						isLocValidSettle = false;
					}
				}
			}
		}
		for(KonRuin ruin : konquest.getRuinManager().getRuins()) {
			searchDist = Konquest.chunkDistance(center, ruin.getCenterLoc());
			if(searchDist != -1) {
				if(searchDist < minDistance) {
					minDistance = searchDist;
					proximity = searchDist;
					closestTerritory = ruin;
				}
				if(searchDist < min_distance_town) {
					isLocValidSettle = false;
				}
			}
		}
		for(KonCamp camp : konquest.getCampManager().getCamps()) {
			searchDist = Konquest.chunkDistance(center, camp.getCenterLoc());
			if(searchDist != -1) {
				if(searchDist < minDistance) {
					proximity = searchDist;
					closestTerritory = camp;
				}
			}
			
		}
		// Verify max distance
		int max_distance_all = konquest.getConfigManager().getConfig("core").getInt("core.towns.max_distance_all");
		if(max_distance_all > 0 && minDistance > max_distance_all) {
			isLocValidSettle = false;
		}
		// Verify no overlapping init chunks
		int radius = konquest.getConfigManager().getConfig("core").getInt("core.towns.init_radius");
		for(Point point : konquest.getAreaPoints(center, radius)) {
			if(isChunkClaimed(point,center.getWorld())) {
				isLocValidSettle = false;
			}
		}
    	String settleTip = MessagePath.MENU_MAP_SETTLE_HINT.getMessage();
    	//if(isLocValidSettlement(center)) {
    	if(isLocValidSettle) {
    		settleTip = ChatColor.GOLD+settleTip;
    	} else {
    		settleTip = ChatColor.STRIKETHROUGH+settleTip;
    		settleTip = ChatColor.GRAY+settleTip;
    	}
    	// Evaluate surrounding chunks
    	for(Point mapPoint: konquest.getAreaPoints(center, ((mapSize-1)/2)+1)) {
    		int diffOriginX = (int)(mapPoint.getX() - originPoint.getX());
    		int diffOriginY = (int)(mapPoint.getY() - originPoint.getY());
    		int mapY = (mapSize-1)/2 - diffOriginX; // Swap X & Y to translate map to be oriented up as north
    		int mapX = (mapSize-1)/2 - diffOriginY;
    		if(mapX < 0 || mapX >= mapSize) {
    			ChatUtil.printDebug("Internal Error: Map X was "+mapX);
    			return;
    		}
    		if(mapY < 0 || mapY >= mapSize) {
    			ChatUtil.printDebug("Internal Error: Map Y was "+mapY);
    			return;
    		}
    		if(isChunkClaimed(mapPoint,center.getWorld())) {
    			KonTerritory territory = getChunkTerritory(mapPoint,center.getWorld());
    			ChatColor mapSymbolColor = ChatColor.WHITE;
    			if(territory.getKingdom().equals(getBarbarians())) {
    				mapSymbolColor = ChatColor.YELLOW;
    				playerColor = ChatColor.GOLD;
    			} else if(territory.getKingdom().equals(player.getKingdom())) {
    				mapSymbolColor = ChatColor.GREEN;
    				playerColor = ChatColor.DARK_GREEN;
    			} else if(territory.getKingdom().isPeaceful() || territory.getKingdom().equals(getNeutrals())) {
    				mapSymbolColor = ChatColor.GRAY;
    				playerColor = ChatColor.DARK_GRAY;
    			} else {
    				mapSymbolColor = ChatColor.RED;
    				playerColor = ChatColor.DARK_RED;
    			}
    			switch(territory.getTerritoryType()) {
        		case WILD:
        			map[mapX][mapY] = ChatColor.WHITE+mapWildSymbol;
        			break;
        		case CAPITAL:
        			map[mapX][mapY] = mapSymbolColor+mapCapitalSymbol;
        			break;
        		case TOWN:
        			map[mapX][mapY] = mapSymbolColor+mapTownSymbol;
        			break;
        		case CAMP:
        			map[mapX][mapY] = mapSymbolColor+mapCampSymbol;
        			break;
        		case RUIN:
        			map[mapX][mapY] = mapSymbolColor+mapRuinSymbol;
        			break;
        		case OTHER:
        			map[mapX][mapY] = ChatColor.WHITE+mapWildSymbol;
        			break;
        		default:
        			map[mapX][mapY] = ChatColor.WHITE+mapWildSymbol;
        			break;
        		}
    		} else {
    			map[mapX][mapY] = ChatColor.WHITE+mapWildSymbol;
    			playerColor = ChatColor.GRAY;
    		}
    		// Override origin symbol with player
    		if(mapX == (mapSize-1)/2 && mapY == (mapSize-1)/2) {
    			map[mapX][mapY] = playerColor+mapPlayer;
    		}
    	}
    	// Determine distance to closest territory
    	//KonTerritory closestTerritory = getClosestTerritory(center.getChunk());
    	ChatColor closestTerritoryColor = ChatColor.GRAY;
    	int distance = 0;
    	if(closestTerritory != null) {
    		distance = proximity;
    		//distance = Konquest.distanceInChunks(center.getChunk(), closestTerritory.getCenterLoc().getChunk());
    		//distance = (int) originPoint.distance(konquest.toPoint(closestTerritory.getCenterLoc().getChunk()));
    		if(closestTerritory.getKingdom().equals(getBarbarians())) {
    			closestTerritoryColor = ChatColor.YELLOW;
    		}  else if(closestTerritory.getKingdom().equals(getNeutrals())) {
    			closestTerritoryColor = ChatColor.DARK_GRAY;
    		} else {
    			if(player.getKingdom().equals(closestTerritory.getKingdom())) {
        			closestTerritoryColor = ChatColor.GREEN;
        		} else {
        			closestTerritoryColor = ChatColor.RED;
        		}
    		}
    	}
    	String distStr = "";
    	int maxDist = 99;
    	if(max_distance_all > 0) {
    		maxDist = max_distance_all;
    	}
    	if(distance > maxDist) {
    		distStr = ""+maxDist+"+";
    		closestTerritoryColor = ChatColor.GRAY;
    	} else {
    		distStr = ""+distance;
    	}
    	// Display map
    	String header = ChatColor.GOLD+MessagePath.MENU_MAP_CENTER.getMessage()+": "+(int)originPoint.getX()+","+(int)originPoint.getY();
    	ChatUtil.sendNotice(bukkitPlayer, header);
    	String[] compassRose = {"   N  ",
    	                        " W * E",
    	                        "   S  "};
    	for(int i = mapSize-1; i >= 0; i--) {
    		String mapLine = ChatColor.GOLD+"|| ";
    		for(int j = mapSize-1; j >= 0; j--) {
    			mapLine = mapLine + map[i][j] + " ";
    		}
    		if(i <= mapSize-1 && i >= mapSize-3) {
    			mapLine = mapLine + ChatColor.GOLD + compassRose[mapSize-1-i];
    		}
    		if(i == mapSize-5) {
    			mapLine = mapLine + " " + settleTip;
    		}
    		if(i == mapSize-7) {
    			mapLine = mapLine + " " + closestTerritoryColor+MessagePath.MENU_MAP_PROXIMITY.getMessage()+": "+distStr;
    		}
    		ChatUtil.sendMessage(bukkitPlayer, mapLine);
    	}
	}
}
