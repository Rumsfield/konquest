package konquest.manager;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
import konquest.model.KonCamp;
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
	private HashMap<String,KonCamp> barbarianCamps;
	private HashMap<PotionEffectType,Integer> townNerfs;
	private Material townCriticalBlock;
	
	public static int DEFAULT_MAP_SIZE = 9; // 10 lines of chat, minus one for header, odd so player's chunk is centered
	
	public KingdomManager(Konquest konquest) {
		this.konquest = konquest;
		kingdomMap = new HashMap<String, KonKingdom>();
		barbarians = new KonKingdom("Barbarians",konquest);
		neutrals = new KonKingdom("Neutrals",konquest);
		territoryWorldCache = new HashMap<World,KonTerritoryCache>();
		barbarianCamps = new HashMap<String,KonCamp>();
		townNerfs = new HashMap<PotionEffectType,Integer>();
		townCriticalBlock = Material.OBSIDIAN;
	}
	
	public void initialize() {
		loadCriticalBlocks();
		loadKingdoms();
		updateKingdomOfflineProtection();
		makeTownNerfs();
		ChatUtil.printDebug("Kingdom Manager is ready");
	}
	
	public void initCamps() {
		loadCamps();
		//updateTerritoryCache(); // function moved into loadCamps()
		ChatUtil.printDebug("Loaded camps");
	}
	
	public boolean addKingdom(Location loc, String name) {
		if(!name.contains(" ") && !isKingdom(name)) {
			kingdomMap.put(name, new KonKingdom(loc, name, konquest));
			kingdomMap.get(name).initCapital();
			//updateTerritoryCache();
			addAllTerritory(loc.getWorld(),kingdomMap.get(name).getCapital().getChunkList());
			//ChatUtil.printDebug("Added new Kingdom "+name);
			return true;
		}
		return false;
	}
	
	public boolean removeKingdom(String name) {
		KonKingdom oldKingdom = kingdomMap.remove(name);
		if(oldKingdom != null) {
			//updateTerritoryCache();
			removeAllTerritory(oldKingdom.getCapital().getWorld(),oldKingdom.getCapital().getChunkList().keySet());
			ChatUtil.printDebug("Removed Kingdom "+name);
			return true;
		}
		return false;
	}
	
	public boolean renameKingdom(String oldName, String newName) {
		if(kingdomMap.containsKey(oldName)) {
			getKingdom(oldName).setName(newName);
			KonKingdom kingdom = kingdomMap.remove(oldName);
			kingdomMap.put(newName, kingdom);
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
	 */
	public int assignPlayerKingdom(KonPlayer player, String kingdomName) {
		//TODO: Some sort of penalty for changing kingdoms, check if barbarian first
		int status = 0;
		if(isKingdom(kingdomName)) {
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
			if(config_max_player_diff == 0 || 
					(config_max_player_diff != 0 && targetKingdomPlayerCount < (smallestKingdomPlayerCount+config_max_player_diff))) {
				removeCamp(player);
				player.setKingdom(getKingdom(kingdomName));
		    	player.setBarbarian(false);
		    	player.getBukkitPlayer().teleport(player.getKingdom().getCapital().getSpawnLoc());
		    	player.getBukkitPlayer().setBedSpawnLocation(getKingdom(kingdomName).getCapital().getSpawnLoc(), true);
		    	//konquest.getPlayerManager().saveAllPlayers();
		    	//konquest.getPlayerManager().updateAllSavedPlayers();
		    	updateSmallestKingdom();
		    	konquest.updateNamePackets(player);
		    	konquest.getDirectiveManager().displayBook(player);
		    	updateKingdomOfflineProtection();
			} else {
				status = 2;
			}
		} else {
			status = 1;
		}
		return status;
	}
	
	/**
	 * Exiles a player to the Barbarians and teleports to a random Wild location.
	 * Sets their exileKingdom value to their current Kingdom.
	 * Removes all stats and disables prefix.
	 * @param player
	 * @return true if not already barbarian and the teleport was successful, else false.
	 */
	public boolean exilePlayer(KonPlayer player) {
    	if(player.isBarbarian()) {
    		return false;
    	}
    	if(!konquest.isWorldValid(player.getBukkitPlayer().getLocation().getWorld())) {
    		return false;
    	}
		Location randomWildLoc = konquest.getRandomWildLocation(1000,player.getBukkitPlayer().getLocation().getWorld());
    	if(randomWildLoc == null) {
    		return false;
    	}
    	player.getBukkitPlayer().teleport(randomWildLoc);
    	player.getBukkitPlayer().setBedSpawnLocation(randomWildLoc, true);
    	player.setExileKingdom(player.getKingdom());
    	// Remove residency
    	for(KonTown town : player.getKingdom().getTowns()) {
    		if(town.removePlayerResident(player.getOfflineBukkitPlayer())) {
    			//ChatUtil.sendNotice(player.getBukkitPlayer(), "Banished from "+town.getName()+"!");
    			ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_EXILE_NOTICE_TOWN.getMessage(town.getName()));
    		}
    	}
    	// Clear all stats
    	player.getPlayerStats().clearStats();
    	// Disable prefix
    	player.getPlayerPrefix().setEnable(false);
    	// Force into global chat mode
    	player.setIsGlobalChat(true);
    	// Make into barbarian
    	player.setKingdom(getBarbarians());
    	player.setBarbarian(true);
    	//konquest.getPlayerManager().saveAllPlayers();
    	//konquest.getPlayerManager().updateAllSavedPlayers();
    	konquest.updateNamePackets(player);
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
	 * 			5 - error, bad town placement
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
			for(KonKingdom kingdom : kingdomMap.values()) {
				// Verify name is not taken by existing towns or kingdoms
				if(name.equalsIgnoreCase(kingdom.getName()) || kingdom.hasTown(name)) {
					ChatUtil.printDebug("Failed to add town, name equals existing town or kingdom: "+name);
					return 3;
				}
				// Verify proximity to other territories
				searchDistance = Konquest.chunkDistance(loc, kingdom.getCapital().getCenterLoc());
				if(searchDistance != -1 && searchDistance < min_distance_capital) {
					ChatUtil.printDebug("Failed to add town, too close to capital "+kingdom.getCapital().getName());
					return 5;
				}
				for(KonTown town : kingdom.getTowns()) {
					searchDistance = Konquest.chunkDistance(loc, town.getCenterLoc());
					if(searchDistance != -1 && searchDistance < min_distance_town) {
						ChatUtil.printDebug("Failed to add town, too close to town "+town.getName());
						return 5;
					}
				}
			}
			for(KonRuin ruin : konquest.getRuinManager().getRuins()) {
				if(name.equalsIgnoreCase(ruin.getName())) {
					ChatUtil.printDebug("Failed to add town, name equals existing ruin: "+name);
					return 3;
				}
				searchDistance = Konquest.chunkDistance(loc, ruin.getCenterLoc());
				if(searchDistance != -1 && searchDistance < min_distance_town) {
					ChatUtil.printDebug("Failed to add town, too close to ruin "+ruin.getName());
					return 5;
				}
			}
			for(TravelDestination keyword : TravelDestination.values()) {
				if(name.equalsIgnoreCase(keyword.toString())) {
					ChatUtil.printDebug("Failed to add town, name equals territory keyword: "+name);
					return 3;
				}
			}
			// Verify no overlapping init chunks
			int radius = konquest.getConfigManager().getConfig("core").getInt("core.towns.init_radius");
			ChatUtil.printDebug("Checking for chunk conflicts with radius "+radius);
			for(Point point : konquest.getAreaPoints(loc, radius)) {
				if(isChunkClaimed(point,loc.getWorld())) {
					ChatUtil.printDebug("Found a chunk conflict");
					return 1;
				}
			}
			// Verify valid monument template
			if(getKingdom(kingdomName).getMonumentTemplate().isValid()) {
				// Attempt to add the town
				if(getKingdom(kingdomName).addTown(loc, name)) {
					// Attempt to initialize the town
					int initStatus = getKingdom(kingdomName).initTown(name);
					if(initStatus == 0) {
						// When a town is added successfully, update the chunk cache
						//updateTerritoryCache();
						addAllTerritory(loc.getWorld(),getKingdom(kingdomName).getTown(name).getChunkList());
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
				konquest.getIntegrationManager().deleteShopsInPoints(townPoints,town.getWorld());
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
	public boolean conquerTown(String name, String oldKingdomName, KonPlayer conquerPlayer) {
		if(conquerPlayer.isBarbarian()) {
			return false;
		}
		if(!conquerPlayer.getKingdom().getMonumentTemplate().isValid()) {
			return false;
		}
		if(isKingdom(oldKingdomName) && getKingdom(oldKingdomName).hasTown(name)) {
			getKingdom(oldKingdomName).getTown(name).purgeResidents();
			getKingdom(oldKingdomName).getTown(name).clearUpgrades();
			getKingdom(oldKingdomName).getTown(name).setKingdom(conquerPlayer.getKingdom());
			conquerPlayer.getKingdom().addTownConquer(name, getKingdom(oldKingdomName).removeTownConquer(name));
			conquerPlayer.getKingdom().getTown(name).refreshMonument();
			conquerPlayer.getKingdom().getTown(name).setPlayerLord(conquerPlayer.getOfflineBukkitPlayer());
			refreshTownNerfs(conquerPlayer.getKingdom().getTown(name));
			refreshTownHearts(conquerPlayer.getKingdom().getTown(name));
			conquerPlayer.getKingdom().getTown(name).updateBarPlayers();
			konquest.getIntegrationManager().deleteShopsInPoints(conquerPlayer.getKingdom().getTown(name).getChunkList().keySet(),conquerPlayer.getKingdom().getTown(name).getWorld());
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
			if(getChunkTerritory(loc).removeChunk(loc)) {
				//updateTerritoryCache();
				removeTerritory(loc);
				return true;
			}
		}
		return false;
	}
	/*
	public void updateTerritoryCache() {
		territoryWorldCache.clear();
		if(!kingdomMap.isEmpty()) {
			for(String name : kingdomMap.keySet()) {
				KonKingdom kingdom = kingdomMap.get(name);
				territoryCache.putAll(kingdom.getCapital().getChunkList());
				if(!kingdom.isTownMapEmpty()) {
					for(String town : kingdom.getTownNames()) {
						territoryCache.putAll(kingdom.getTown(town).getChunkList());
					}
				}
			}
		}
		if(!barbarianCamps.isEmpty()) {
			for(KonCamp camp : barbarianCamps.values()) {
				territoryCache.putAll(camp.getChunkList());
			}
		}
		//ChatUtil.printDebug("Updated Chunk-Territory hashmap");
	}*/
	
	/*
	 * Territory cache methods
	 */
	
	/*
	public void initTerritoryCache() {
		territoryWorldCache.clear();
		// Populate territory in primary world
		KonTerritoryCache cache = new KonTerritoryCache();
		if(!kingdomMap.isEmpty()) {
			for(String name : kingdomMap.keySet()) {
				KonKingdom kingdom = kingdomMap.get(name);
				cache.putAll(kingdom.getCapital().getChunkList());
				if(!kingdom.isTownMapEmpty()) {
					for(String town : kingdom.getTownNames()) {
						cache.putAll(kingdom.getTown(town).getChunkList());
					}
				}
			}
		}
		if(!barbarianCamps.isEmpty()) {
			for(KonCamp camp : barbarianCamps.values()) {
				cache.putAll(camp.getChunkList());
			}
		}
		World primaryWorld = Bukkit.getWorld(konquest.getWorldName());
		territoryWorldCache.put(primaryWorld, cache);
	}*/
	
	public void addAllTerritory(World world, HashMap<Point,KonTerritory> pointMap) {
		/*String territoryName = "null";
		if(!pointMap.isEmpty()) {
			for(KonTerritory territory : pointMap.values()) {
				if(territory != null) {
					territoryName = territory.getName();
					break;
				}
			}
		}*/
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
	
	/*
	public void addTerritory(Chunk chunk, KonTerritory territory) {
		addTerritory(chunk.getWorld(),konquest.toPoint(chunk),territory);
	}
	*/
	
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
	/*
	public String getSmallestKingdomName() {
		KonKingdom smallestKingdom = null;
		int smallestSize = 9999;
		for(KonKingdom kingdom : kingdomMap.values()) {
			kingdom.setSmallest(false);
			int size = konquest.getPlayerManager().getAllPlayersInKingdom(kingdom.getName()).size();
			if(size < smallestSize) {
				smallestKingdom = kingdom;
				smallestSize = size;
			}
		}
		if(smallestKingdom == null) {
			ChatUtil.printDebug("Failed to get smallest kingdom, got null!");
			return "";
		}
		return smallestKingdom.getName();
	}
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
	
	/*
	public boolean isChunkClaimed(Chunk chunk) {
		boolean result = false;
		if(territoryWorldCache.containsKey(chunk.getWorld())) {
			result = territoryWorldCache.get(chunk.getWorld()).has(konquest.toPoint(chunk));
		}
		return result;
	}
	*/
	
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
	
	/*
	// This can return null!
	public KonTerritory getChunkTerritory(Chunk chunk) {
		if(territoryWorldCache.containsKey(chunk.getWorld())) {
			return territoryWorldCache.get(chunk.getWorld()).get(konquest.toPoint(chunk));
		} else {
			return null;
		}
	}
	*/
	
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
	
	/*
	// This can return null!
	public KonTerritory getClosestTerritory(Chunk chunk) {
		KonTerritory closestTerritory = null;
		int minDistance = Integer.MAX_VALUE;
		int searchDist = 0;
		World searchWorld = chunk.getWorld();
		for(KonKingdom kingdom : kingdomMap.values()) {
			searchDist = Konquest.distanceInChunks(chunk, kingdom.getCapital().getCenterLoc().getChunk());
			if(searchWorld.equals(kingdom.getCapital().getCenterLoc().getWorld()) && searchDist < minDistance) {
				minDistance = searchDist;
				closestTerritory = kingdom.getCapital();
			}
			for(KonTown town : kingdom.getTowns()) {
				searchDist = Konquest.distanceInChunks(chunk, town.getCenterLoc().getChunk());
				if(searchWorld.equals(town.getCenterLoc().getWorld()) && searchDist < minDistance) {
					minDistance = searchDist;
					closestTerritory = town;
				}
			}
		}
		for(KonRuin ruin : konquest.getRuinManager().getRuins()) {
			searchDist = Konquest.distanceInChunks(chunk, ruin.getCenterLoc().getChunk());
			if(searchWorld.equals(ruin.getCenterLoc().getWorld()) && searchDist < minDistance) {
				minDistance = searchDist;
				closestTerritory = ruin;
			}
		}
		return closestTerritory;
	}
	*/
	
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
	
	/*
	public boolean isLocValidSettlement(Location loc) {
		// Check for valid world
		World searchWorld = loc.getWorld();
		if(!konquest.isWorldValid(searchWorld)) {
			return false;
		}
		// Check for minimum distance
		int min_distance_capital = konquest.getConfigManager().getConfig("core").getInt("core.towns.min_distance_capital");
		int min_distance_town = konquest.getConfigManager().getConfig("core").getInt("core.towns.min_distance_town");
		for(KonKingdom kingdom : kingdomMap.values()) {
			if(searchWorld.equals(kingdom.getCapital().getCenterLoc().getWorld()) && 
					Konquest.distanceInChunks(loc, kingdom.getCapital().getCenterLoc()) < min_distance_capital) {
				return false;
			}
			for(KonTown town : kingdom.getTowns()) {
				if(searchWorld.equals(town.getCenterLoc().getWorld()) && 
						Konquest.distanceInChunks(loc, town.getCenterLoc()) < min_distance_town) {
					return false;
				}
			}
		}
		for(KonRuin ruin : konquest.getRuinManager().getRuins()) {
			if(searchWorld.equals(ruin.getCenterLoc().getWorld()) && 
					Konquest.distanceInChunks(loc, ruin.getCenterLoc()) < min_distance_town) {
				return false;
			}
		}
		// Verify no overlapping init chunks
		int radius = konquest.getConfigManager().getConfig("core").getInt("core.towns.init_radius");
		for(Chunk chunk : konquest.getAreaChunks(loc, radius)) {
			if(isChunkClaimed(chunk)) {
				return false;
			}
		}
		return true;
	}
	*/
	
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
	
	public boolean isCampSet(KonOfflinePlayer player) {
		/*if(player.isBarbarian()) {
			String uuid = player.getOfflineBukkitPlayer().getUniqueId().toString();
			if(barbarianCamps.containsKey(uuid)) {
				ChatUtil.printDebug("isCampSet found valid camp for player "+player.getOfflineBukkitPlayer().getName()+" "+uuid);
				return true;
			} else {
				ChatUtil.printDebug("isCampSet found a barbarian with no camp: "+player.getOfflineBukkitPlayer().getName()+" "+uuid);
				for(String keyUUID : barbarianCamps.keySet()) {
					ChatUtil.printDebug("...keys include "+keyUUID);
				}
				return false;
			}
		} else {
			ChatUtil.printDebug("isCampSet failed because player is not a barbarian: "+player.getOfflineBukkitPlayer().getName());
			return false;
		}*/
		String uuid = player.getOfflineBukkitPlayer().getUniqueId().toString();
		return player.isBarbarian() && barbarianCamps.containsKey(uuid);
	}
	
	public KonCamp getCamp(KonOfflinePlayer player) {
		String uuid = player.getOfflineBukkitPlayer().getUniqueId().toString();
		return barbarianCamps.get(uuid);
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
				if(isChunkClaimed(point,addWorld)) {
					ChatUtil.printDebug("Found a chunk conflict in camp placement for player "+player.getOfflineBukkitPlayer().getName()+" "+uuid);
					return 1;
				}
			}
			// Attempt to add the camp
			KonCamp newCamp = new KonCamp(loc,player.getOfflineBukkitPlayer(),barbarians,konquest);
			barbarianCamps.put(uuid,newCamp);
			newCamp.initClaim();
			//update the chunk cache, add points to primary world cache
			addAllTerritory(loc.getWorld(),newCamp.getChunkList());
			//updateTerritoryCache();
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
			//update the chunk cache, remove all points from primary world
			//updateTerritoryCache();
			removeAllTerritory(removedCamp.getWorld(),removedCamp.getChunkList().keySet());
		} else {
			ChatUtil.printDebug("Failed to remove camp for missing UUID "+uuid);
			return false;
		}
		ChatUtil.printDebug("Successfully removed camp for UUID "+uuid);
		return true;
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
				} if(chunkKingdom.equals(getNeutrals())) {
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
		FileConfiguration kingdomsConfig = konquest.getConfigManager().getConfig("kingdoms");
        if (kingdomsConfig.get("kingdoms") == null) {
        	ChatUtil.printDebug("There is no kingdoms section in kingdoms.yml");
            return;
        }
        double x,y,z;
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
        	Date kStart = new Date();
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
        		sectionList = capitalSection.getDoubleList("spawn");
        		x = sectionList.get(0);
        		y = sectionList.get(1);
        		z = sectionList.get(2);
	        	Location capital_spawn = new Location(capitalWorld,x,y,z);
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
        	} else {
        		String message = "Failed to load capital for Kingdom "+kingdomName+", is one created?";
        		ChatUtil.printConsoleError(message);
        		konquest.opStatusMessages.add(message);
        	}
        	Date kStep1 = new Date();
        	// Create Monument Templates
        	if(monumentSection != null && capitalWorld != null) {
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
	        	// Load monument template chunk
	        	//capitalWorld.loadChunk(monument_cornerone.getBlockX()/16,monument_cornerone.getBlockZ()/16);
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
        	Date kStep2 = new Date();
        	// Load all towns
        	boolean isMissingMonuments = false;
        	World townWorld = null;
        	for(String townName : kingdomsConfig.getConfigurationSection("kingdoms."+kingdomName+".towns").getKeys(false)) {
        		//ChatUtil.printDebug("Loading Town: "+townName);
            	ConfigurationSection townSection = kingdomsConfig.getConfigurationSection("kingdoms."+kingdomName+".towns."+townName);
            	if(townSection != null) {
            		Date start = new Date();
            		worldName = townSection.getString("world",defaultWorldName);
            		townWorld = Bukkit.getWorld(worldName);
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
	            	Date step1 = new Date();
	            	// Create Town
	            	kingdomMap.get(kingdomName).addTown(town_center, townName);
	            	KonTown town = kingdomMap.get(kingdomName).getTown(townName);
	            	// Set town spawn point
	            	town.setSpawn(town_spawn);
	            	Date step2 = new Date();
	            	// Load monument chunk
	            	townWorld.loadChunk(town_center.getBlockX()/16,town_center.getBlockZ()/16);
	            	// Setup town monument parameters from template
	            	int status = town.loadMonument(base, kingdomMap.get(kingdomName).getMonumentTemplate());
	            	if(status != 0) {
	            		isMissingMonuments = true;
	            		ChatUtil.printConsoleError("Failed to load monument for Town "+townName+" in kingdom "+kingdomName+" from invalid template");
	            		//konquest.opStatusMessages.add("Failed to load monument for Town "+townName+" in kingdom "+kingdomName+" from invalid template");
	            	}
	            	// Unload monument chunk
	            	townWorld.unloadChunk(town_center.getBlockX()/16,town_center.getBlockZ()/16);
	            	Date step3 = new Date();
	            	// Add all Town chunk claims
	            	town.addPoints(konquest.formatStringToPoints(townSection.getString("chunks")));
	            	// Update territory cache
		        	addAllTerritory(townWorld,town.getChunkList());
		        	Date step4 = new Date();
	            	// Set open flag
	            	boolean isOpen = townSection.getBoolean("open",false);
	            	town.setIsOpen(isOpen);
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
	            	Date step5 = new Date();
	            	// Populate Residents
	            	if(townSection.contains("residents")) {
		            	for(String residentUUID : townSection.getConfigurationSection("residents").getKeys(false)) {
		            		boolean isElite = townSection.getBoolean("residents."+residentUUID);
		            		town.addPlayerResident(Bukkit.getOfflinePlayer(UUID.fromString(residentUUID)),isElite);
		            	}
	            	} else {
	            		//ChatUtil.printDebug("Town "+townName+" does not have any stored residents");
	            	}
	            	Date step6 = new Date();
	            	// Add invite requests
	            	if(townSection.contains("requests")) {
	            		for(String requestUUID : townSection.getConfigurationSection("requests").getKeys(false)) {
	            			boolean type = townSection.getBoolean("requests."+requestUUID);
	            			town.addJoinRequest(UUID.fromString(requestUUID), type);
	            		}
	            	}
	            	Date step7 = new Date();
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
	            	Date step8 = new Date();
	            	// Update upgrade status
	            	konquest.getUpgradeManager().updateTownDisabledUpgrades(town);
	            	// Update loading bar
	            	loadBar.addProgress(1);
	            	Date step9 = new Date();
	            	int s1 = (int)(step1.getTime()-start.getTime());
	            	int s2 = (int)(step2.getTime()-start.getTime());
        			int s3 = (int)(step3.getTime()-start.getTime());
        			int s4 = (int)(step4.getTime()-start.getTime());
        			int s5 = (int)(step5.getTime()-start.getTime());
        			int s6 = (int)(step6.getTime()-start.getTime());
        			int s7 = (int)(step7.getTime()-start.getTime());
        			int s8 = (int)(step8.getTime()-start.getTime());
        			int s9 = (int)(step9.getTime()-start.getTime());
        			ChatUtil.printDebug("Town "+townName+" timings: "+s1+","+s2+","+s3+","+s4+","+s5+","+s6+","+s7+","+s8+","+s9);
	            }
        	}
        	if(isMissingMonuments) {
        		konquest.opStatusMessages.add("Kingdom "+kingdomName+" has Towns with invalid Monuments. You must create a new Monument Template and restart the server.");
        	}
        	Date kStep3 = new Date();
        	int s1 = (int)(kStep1.getTime()-kStart.getTime());
        	int s2 = (int)(kStep2.getTime()-kStart.getTime());
			int s3 = (int)(kStep3.getTime()-kStart.getTime());
			ChatUtil.printDebug("Kingdom "+kingdomName+" timings: "+s1+","+s2+","+s3);
        }
		ChatUtil.printDebug("Loaded Kingdoms");
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
            		addCamp(camp_center, offlinePlayer);
            		getCamp(offlinePlayer).addPoints(konquest.formatStringToPoints(playerCampSection.getString("chunks")));
            		addAllTerritory(world,getCamp(offlinePlayer).getChunkList());
            		totalCamps++;
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
	
	//TODO Save and update only items which have changed, do not delete everything and write all from memory
	public void saveKingdoms() {
		// This is a lame way to try to ensure that the kingdom map was initialized, and to avoid erasing the YML file when memory is gone
		if(!kingdomMap.isEmpty()) {
			FileConfiguration kingdomsConfig = konquest.getConfigManager().getConfig("kingdoms");
			kingdomsConfig.set("kingdoms", null); // reset kingdoms config
			ConfigurationSection root = kingdomsConfig.createSection("kingdoms");
			for(KonKingdom kingdom : kingdomMap.values()) {
				ConfigurationSection kingdomSection = root.createSection(kingdom.getName());
				kingdomSection.set("peaceful",kingdom.isPeaceful());
	            ConfigurationSection capitalSection = kingdomSection.createSection("capital");
	            capitalSection.set("world", kingdom.getCapital().getWorld().getName());
	            capitalSection.set("spawn", new int[] {(int) kingdom.getCapital().getSpawnLoc().getX(),
													   (int) kingdom.getCapital().getSpawnLoc().getY(),
													   (int) kingdom.getCapital().getSpawnLoc().getZ()});
	            capitalSection.set("center", new int[] {(int) kingdom.getCapital().getCenterLoc().getX(),
						 								(int) kingdom.getCapital().getCenterLoc().getY(),
						 								(int) kingdom.getCapital().getCenterLoc().getZ()});
	            capitalSection.set("chunks", konquest.formatPointsToString(kingdom.getCapital().getChunkList().keySet()));
	            if(kingdom.getMonumentTemplate().isValid()) {
		            ConfigurationSection monumentSection = kingdomSection.createSection("monument");
		            monumentSection.set("travel", new int[] {(int) kingdom.getMonumentTemplate().getTravelPoint().getX(),
															 (int) kingdom.getMonumentTemplate().getTravelPoint().getY(),
															 (int) kingdom.getMonumentTemplate().getTravelPoint().getZ()});
		            monumentSection.set("cornerone", new int[] {(int) kingdom.getMonumentTemplate().getCornerOne().getX(),
							 								 	(int) kingdom.getMonumentTemplate().getCornerOne().getY(),
							 								 	(int) kingdom.getMonumentTemplate().getCornerOne().getZ()});
		            monumentSection.set("cornertwo", new int[] {(int) kingdom.getMonumentTemplate().getCornerTwo().getX(),
							 								 	(int) kingdom.getMonumentTemplate().getCornerTwo().getY(),
							 								 	(int) kingdom.getMonumentTemplate().getCornerTwo().getZ()});
				} else {
					ChatUtil.printConsoleError("Failed to save invalid monument template for Kingdom "+kingdom.getName());
				}
	            ConfigurationSection townsSection = kingdomSection.createSection("towns");
	            for(KonTown town : kingdom.getTowns()) {
	            	ConfigurationSection townInstanceSection = townsSection.createSection(town.getName());
	            	townInstanceSection.set("world", town.getWorld().getName());
	            	townInstanceSection.set("base", town.getMonument().getBaseY());
	            	townInstanceSection.set("spawn", new int[] {(int) town.getSpawnLoc().getX(),
							 								 	(int) town.getSpawnLoc().getY(),
							 								 	(int) town.getSpawnLoc().getZ()});
	            	townInstanceSection.set("center", new int[] {(int) town.getCenterLoc().getX(),
							 									 (int) town.getCenterLoc().getY(),
							 									 (int) town.getCenterLoc().getZ()});
	                townInstanceSection.set("chunks", konquest.formatPointsToString(town.getChunkList().keySet()));
	                townInstanceSection.set("open", town.isOpen());
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
		} else {
			ChatUtil.printDebug("The kingdom memory cache is invalid, aborted save");
		}
	}
	
	public void saveCamps() {
		if(!barbarianCamps.isEmpty()) {
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
					closestTerritory = ruin;
				}
				if(searchDist < min_distance_town) {
					isLocValidSettle = false;
				}
			}
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
    	int maxDistance = 99;
    	if(closestTerritory != null) {
    		distance = minDistance;
    		//distance = Konquest.distanceInChunks(center.getChunk(), closestTerritory.getCenterLoc().getChunk());
    		//distance = (int) originPoint.distance(konquest.toPoint(closestTerritory.getCenterLoc().getChunk()));
    		if(closestTerritory.getKingdom().equals(getBarbarians())) {
    			closestTerritoryColor = ChatColor.YELLOW;
    		} else {
    			if(player.getKingdom().equals(closestTerritory.getKingdom())) {
        			closestTerritoryColor = ChatColor.GREEN;
        		} else {
        			closestTerritoryColor = ChatColor.RED;
        		}
    		}
    	}
    	if(distance > maxDistance) {
    		distance = 99;
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
    			mapLine = mapLine + " " + closestTerritoryColor+MessagePath.MENU_MAP_PROXIMITY.getMessage()+": "+distance;
    		}
    		ChatUtil.sendMessage(bukkitPlayer, mapLine);
    	}
	}
}
