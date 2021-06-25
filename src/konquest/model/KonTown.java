package konquest.model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import konquest.Konquest;
import konquest.utility.BlockPaster;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;
import konquest.utility.Timeable;
import konquest.utility.Timer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;


/**
 * KonTown - Represents a Town Territory, has a center chunk monument
 * @author Rumsfield
 * @prerequisites	The Town's Kingdom must have a valid Monument Template
 */
public class KonTown extends KonTerritory implements Timeable{
	
	private KonMonument monument;
	private Timer monumentTimer;
	private Timer captureTimer;
	private Timer raidAlertTimer;
	private HashMap<UUID, Timer> playerTravelTimers;
	private boolean isCaptureDisabled;
	private boolean isRaidAlertDisabled;
	private BossBar monumentBarAllies;
	private BossBar monumentBarEnemies;
	private UUID lord;
	private HashMap<UUID,Boolean> residents;
	private HashMap<UUID,Boolean> joinRequests; // Player UUID, invite direction (true = requesting join to resident, false = requesting add from lord/knight)
	private boolean isOpen;
	private boolean isAttacked;
	private ArrayList<UUID> defenders;
	private HashMap<KonUpgrade,Integer> upgrades;
	private HashMap<KonUpgrade,Integer> disabledUpgrades;
	private KonTownRabbit rabbit;
	
	public KonTown(Location loc, String name, KonKingdom kingdom, Konquest konquest) {
		super(loc, name, kingdom, KonTerritoryType.TOWN, konquest);
		// Assume there is a valid Monument Template
		this.monument = new KonMonument(loc);
		this.monumentTimer = new Timer(this);
		this.captureTimer = new Timer(this);
		this.raidAlertTimer = new Timer(this);
		this.playerTravelTimers = new HashMap<UUID, Timer>();
		this.isCaptureDisabled = false;
		this.isRaidAlertDisabled = false;
		this.monumentBarAllies = Bukkit.getServer().createBossBar(ChatColor.GREEN+name, BarColor.GREEN, BarStyle.SOLID);
		this.monumentBarAllies.setVisible(true);
		this.monumentBarEnemies = Bukkit.getServer().createBossBar(ChatColor.RED+name, BarColor.RED, BarStyle.SOLID);
		this.monumentBarEnemies.setVisible(true);
		this.lord = null; // init with no lord
		this.residents = new HashMap<UUID,Boolean>();
		this.joinRequests = new HashMap<UUID,Boolean>();
		this.isOpen = false; // init as a closed Town, requires Lord to add players as residents for build/container perms
		this.isAttacked = false;
		this.defenders = new ArrayList<UUID>();
		this.upgrades = new HashMap<KonUpgrade,Integer>();
		this.disabledUpgrades = new HashMap<KonUpgrade,Integer>();
		this.rabbit = new KonTownRabbit(getSpawnLoc());
	}

	/**
	 * Adds a chunk to the chunkMap and checks to make sure its within the max distance from town center
	 * @param chunk
	 * @return true if chunk is within max distance from town center, else false
	 */
	@Override
	public boolean addChunk(Chunk chunk) {
		Point addChunk = getKonquest().toPoint(chunk);
		Point centerChunk = getKonquest().toPoint(getCenterLoc());
		int maxChunkRange = getKonquest().getConfigManager().getConfig("core").getInt("core.towns.min_distance_town")-1;
		
		int distX = (int)Math.abs(addChunk.getX() - centerChunk.getX());
		int distY = (int)Math.abs(addChunk.getY() - centerChunk.getY());
		
		if(!chunk.getWorld().equals(getCenterLoc().getWorld()) || distX > maxChunkRange || distY > maxChunkRange) {
			ChatUtil.printDebug("Failed to add chunk in territory "+getName()+", too far");
			return false;
		}
		addPoint(getKonquest().toPoint(chunk));
		return true;
	}

	/**
	 * initClaim - initializes the Monument, pastes the template and claims chunks.
	 * @return  0 - success
	 * 			1 - error, bad monument init
	 * 			2 - error, bad town height
	 * 			3 - error, too much air below town
	 * 			4 - error, bad chunks
	 */
	@Override
	public int initClaim() {
		
		// Verify monument template and chunk gradient and initialize travelPoint and baseY coordinate
		if(!monument.initialize(getKingdom().getMonumentTemplate())) {
			ChatUtil.printDebug("Town init failed: monument did not initialize correctly");
			return 1;
		}
		
		// Verify monument paste Y level is within config min/max range
		int config_min_y = getKonquest().getConfigManager().getConfig("core").getInt("core.towns.min_settle_height");
		int config_max_y = getKonquest().getConfigManager().getConfig("core").getInt("core.towns.max_settle_height");
		if(config_min_y < 0) {
			config_min_y = 0;
		}
		if(config_min_y != 0 && monument.getBaseY() < config_min_y) {
			ChatUtil.printDebug("Town init failed: "+monument.getBaseY()+" less than min limit "+config_min_y);
			return 2;
		}
		if(config_max_y > getWorld().getMaxHeight()) {
			config_max_y = getWorld().getMaxHeight();
		}
		if(config_max_y != 0 && monument.getBaseY() > config_max_y) {
			ChatUtil.printDebug("Town init failed: "+monument.getBaseY()+" greater than max limit "+config_max_y);
			return 2;
		}
		
		// Verify monument template will not pass the height limit
		if(monument.getBaseY() + getKingdom().getMonumentTemplate().getHeight() + 1 >= getWorld().getMaxHeight()) {
			ChatUtil.printDebug("Town init failed: too high");
			return 2;
		}
		
		// Verify there is not too much air below the base Y location, no more than 10 layers of 16x16 blocks.
		if(countAirBelowMonument() > 2560) {
			ChatUtil.printDebug("Town init failed: too much air below monument");
			return 3;
		}
		
		// Verify there is not too much water in this chunk, no more than 3 layers of 16x16 blocks.
		if(countWaterInChunk() > 768) {
			ChatUtil.printDebug("Town init failed: too much water in the chunk");
			return 3;
		}
        
		// Add chunks around the monument chunk
		if(!addChunks(getKonquest().getAreaChunks(getCenterLoc(), getKonquest().getConfigManager().getConfig("core").getInt("core.towns.init_radius")))) {
			ChatUtil.printDebug("Town init failed: problem adding some chunks");
			return 4;
		}
		
		// Copy Monument template blocks into town center chunk
		if(!pasteMonumentFromTemplate(getKingdom().getMonumentTemplate())) {
			ChatUtil.printDebug("Town init failed: monument did not paste correctly");
			return 1;
		}

		// Validate Monument
		monument.setIsValid(true);
		
		// Set spawn point to monument travel point
		setSpawn(monument.getTravelPoint());
		
		return 0;
	}
	
	public boolean pasteMonumentFromTemplate(KonMonumentTemplate template) {
		if(!template.isValid()) {
			return false;
		}
		monument.setIsItemDropsDisabled(true);
		monument.setIsDamageDisabled(true);
		
		int topBlockX = Math.max(template.getCornerOne().getBlockX(), template.getCornerTwo().getBlockX());
        int topBlockY = Math.max(template.getCornerOne().getBlockY(), template.getCornerTwo().getBlockY());
        int topBlockZ = Math.max(template.getCornerOne().getBlockZ(), template.getCornerTwo().getBlockZ());
        int bottomBlockX = Math.min(template.getCornerOne().getBlockX(), template.getCornerTwo().getBlockX());
        int bottomBlockY = Math.min(template.getCornerOne().getBlockY(), template.getCornerTwo().getBlockY());
        int bottomBlockZ = Math.min(template.getCornerOne().getBlockZ(), template.getCornerTwo().getBlockZ());

        ChunkSnapshot chunkSnapshot = getWorld().getChunkAt(getCenterLoc()).getChunkSnapshot(true,false,false);
        Chunk fillChunk = getWorld().getChunkAt(getCenterLoc());
        int base_y = 0;
        int monument_y = monument.getBaseY();
        for (int x = bottomBlockX; x <= topBlockX; x++) {
            for (int z = bottomBlockZ; z <= topBlockZ; z++) {
            	base_y = chunkSnapshot.getHighestBlockYAt(x-bottomBlockX, z-bottomBlockZ);
                // Fill air between world and monument base
                if(base_y < monument_y) {
                	for (int k = base_y; k <= monument_y; k++) {
                		fillChunk.getBlock(x-bottomBlockX, k, z-bottomBlockZ).setType(Material.STONE);
                	}
                }
            }
        }
        
        BlockPaster monumentPaster = new BlockPaster(getCenterLoc(),bottomBlockY,monument.getBaseY(),bottomBlockY,topBlockX,topBlockZ,bottomBlockX,bottomBlockZ);
        for (int y = bottomBlockY; y <= topBlockY; y++) {
        	monumentPaster.setY(y);
        	//BlockPaster monumentPaster = new BlockPaster(getCenterLoc(),y,monument.getBaseY(),bottomBlockY,topBlockX,topBlockZ,bottomBlockX,bottomBlockZ);
        	monumentPaster.startPaste();
        }
        monument.setIsItemDropsDisabled(false);
        monument.setIsDamageDisabled(false);
		return true;
	}

	/*
	public boolean pasteMonumentFromTemplate(KonMonumentTemplate template) {
		
		if(!template.isValid()) {
			return false;
		}
		monument.setIsItemDropsDisabled(true);
		
		int topBlockX = Math.max(template.getCornerOne().getBlockX(), template.getCornerTwo().getBlockX());
        int topBlockY = Math.max(template.getCornerOne().getBlockY(), template.getCornerTwo().getBlockY());
        int topBlockZ = Math.max(template.getCornerOne().getBlockZ(), template.getCornerTwo().getBlockZ());
        int bottomBlockX = Math.min(template.getCornerOne().getBlockX(), template.getCornerTwo().getBlockX());
        int bottomBlockY = Math.min(template.getCornerOne().getBlockY(), template.getCornerTwo().getBlockY());
        int bottomBlockZ = Math.min(template.getCornerOne().getBlockZ(), template.getCornerTwo().getBlockZ());
        
        for (int x = bottomBlockX; x <= topBlockX; x++) {
            for (int y = bottomBlockY; y <= topBlockY; y++) {
                for (int z = bottomBlockZ; z <= topBlockZ; z++) {
                    Block templateBlock = Bukkit.getServer().getWorld(getKonquest().getWorldName()).getBlockAt(x, y, z);
                    Block monumentBlock = Bukkit.getServer().getWorld(getKonquest().getWorldName()).getChunkAt(getCenterLoc()).getBlock(x-bottomBlockX, y-bottomBlockY+monument.getBaseY(), z-bottomBlockZ);
                    int base_y = Bukkit.getServer().getWorld(getKonquest().getWorldName()).getChunkAt(getCenterLoc()).getChunkSnapshot(true,false,false).getHighestBlockYAt(x-bottomBlockX, z-bottomBlockZ);
                    int monument_y = monument.getBaseY();
                    // Fill air between world and monument base
                    if(y == bottomBlockY && base_y < monument_y) {
                    	for (int k = base_y; k <= monument_y; k++) {
                    		Block fillBlock = Bukkit.getServer().getWorld(getKonquest().getWorldName()).getChunkAt(getCenterLoc()).getBlock(x-bottomBlockX, k, z-bottomBlockZ);
                    		fillBlock.setType(Material.STONE);
                    	}
                    }
                    // Set local block to monument template block
                    monumentBlock.setType(templateBlock.getType());
                    monumentBlock.setBlockData(templateBlock.getBlockData().clone());
                    //Remove snow
                    if(monumentBlock.getBlockData() instanceof Snow) {
                    	//Snowable snowBlockData = (Snowable)monumentBlock.getBlockData();
                    	//snowBlockData.setSnowy(false);
                    	monumentBlock.setType(Material.AIR);
                    }
                }
            }
        }
        monument.setIsItemDropsDisabled(false);
		return true;
	}
	*/

	public boolean removeMonumentBlocks() {
		if(!monument.isValid()) {
			ChatUtil.printDebug("Town remove monument failed: Monument is not valid");
			return false;
		}
		int minY = monument.getBaseY();
		int maxY = monument.getBaseY()+monument.getHeight();
		if(minY == 0 || maxY == 0) {
			ChatUtil.printDebug("Town remove monument failed: Base or Height value is 0");
			return false;
		}
		monument.setIsItemDropsDisabled(true);
		for (int x = 0; x <= 15; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = 0; z <= 15; z++) {
                    Block currentBlock = getWorld().getChunkAt(getCenterLoc()).getBlock(x, y, z);
                    if(y == minY) {
                    	currentBlock.setType(Material.DIRT);
                    } else {
                    	currentBlock.setType(Material.AIR);
                    }
                }
            }
        }
		monument.setIsItemDropsDisabled(false);
		return true;
	}
	
	public int countWaterInChunk() {
		int count = 0;
		for (int x = 0; x <= 15; x++) {
            for (int y = 0; y <= getWorld().getMaxHeight()-1; y++) {
                for (int z = 0; z <= 15; z++) {
                    Block currentBlock = getWorld().getChunkAt(getCenterLoc()).getBlock(x, y, z);
                    if(currentBlock.getType().equals(Material.WATER)) {
                    	count++;
                    }
                }
            }
        }
		return count;
	}
	
	public int countAirBelowMonument() {
		int count = 0;
		for (int x = 0; x <= 15; x++) {
            for (int y = 0; y <= monument.getBaseY(); y++) {
                for (int z = 0; z <= 15; z++) {
                    Block currentBlock = getWorld().getChunkAt(getCenterLoc()).getBlock(x, y, z);
                    if(currentBlock.getType().equals(Material.AIR)) {
                    	count++;
                    }
                }
            }
        }
		return count;
	}
	
	public void fillAirBelowMonument() {
		for (int x = 0; x <= 15; x++) {
            for (int y = 0; y <= monument.getBaseY(); y++) {
                for (int z = 0; z <= 15; z++) {
                    Block currentBlock = getWorld().getChunkAt(getCenterLoc()).getBlock(x, y, z);
                    if(currentBlock.getType().equals(Material.AIR)) {
                    	currentBlock.setType(Material.STONE);
                    }
                }
            }
        }
	}
	
	public void fillAllBelowMonument() {
		for (int x = 0; x <= 15; x++) {
            for (int y = 0; y <= monument.getBaseY(); y++) {
                for (int z = 0; z <= 15; z++) {
                    Block currentBlock = getWorld().getChunkAt(getCenterLoc()).getBlock(x, y, z);
                    if(!currentBlock.getType().equals(Material.BEDROCK)) {
                    	currentBlock.setType(Material.STONE);
                    }
                }
            }
        }
	}

	public boolean isLocInsideCenterChunk(Location loc) {
		Chunk centerChunk = getCenterLoc().getChunk();
		Chunk testChunk = loc.getChunk();
		return centerChunk.getX() == testChunk.getX() && centerChunk.getZ() == testChunk.getZ();
	}
	
	/**
	 * Method used for loading a monument on server start
	 * @param base
	 * @param height
	 * @param isValid
	 */
	public int loadMonument(int base, KonMonumentTemplate template) {
		int status = 0;
		monument.setBaseY(base);
		if(template.isValid()) {
			monument.setHeight(template.getHeight());
			monument.setIsValid(true);
			pasteMonumentFromTemplate(getKingdom().getMonumentTemplate());
		} else {
			status = 1;
		}
		return status;
	}
	
	public void refreshMonument() {
		if(monument.isValid()) {
			removeMonumentBlocks();
			monument.clearCriticalHits();
			monument.updateFromTemplate(getKingdom().getMonumentTemplate());
			pasteMonumentFromTemplate(getKingdom().getMonumentTemplate());
			setSpawn(monument.getTravelPoint());
		}
	}
	
	public KonMonument getMonument() {
		return monument;
	}
	
	public boolean isCaptureDisabled() {
		return isCaptureDisabled;
	}
	
	public void setIsCaptureDisabled(boolean val) {
		isCaptureDisabled = val;
	}
	
	public boolean isRaidAlertDisabled() {
		return isRaidAlertDisabled;
	}
	
	public void setIsRaidAlertDisabled(boolean val) {
		isRaidAlertDisabled = val;
	}
	
	public boolean addPlayerTravelCooldown(UUID uuid) {
		if(playerTravelTimers.containsKey(uuid)) {
			return false;
		}
		int travelCooldownSeconds = getKonquest().getConfigManager().getConfig("core").getInt("core.towns.travel_cooldown");
		Timer travelCooldownTimer = new Timer(this);
		travelCooldownTimer.stopTimer();
		travelCooldownTimer.setTime(travelCooldownSeconds);
		travelCooldownTimer.startTimer();
		String name = Bukkit.getOfflinePlayer(uuid).getName();
		playerTravelTimers.put(uuid, travelCooldownTimer);
		ChatUtil.printDebug("Starting player travel timer for "+travelCooldownSeconds+" seconds with taskID "+travelCooldownTimer.getTaskID()+" for player "+name);
		return true;
	}
	
	public boolean isPlayerTravelDisabled(UUID uuid) {
		return playerTravelTimers.containsKey(uuid);
	}
	
	public int getPlayerTravelCooldown(UUID uuid) {
		int cooldownTime = 0;
		if(playerTravelTimers.containsKey(uuid)) {
			cooldownTime = playerTravelTimers.get(uuid).getSeconds();
		}
		return cooldownTime;
	}
	
	public String getPlayerTravelCooldownString(UUID uuid) {
		String cooldownTime = "";
		if(playerTravelTimers.containsKey(uuid)) {
			//cooldownTime = ""+playerTravelTimers.get(uuid).getMinutes()+":"+playerTravelTimers.get(uuid).getSeconds();
			cooldownTime = String.format("%02d:%02d", playerTravelTimers.get(uuid).getMinutes(), playerTravelTimers.get(uuid).getSeconds());
		}
		return cooldownTime;
	}
	
	public String getCaptureCooldownString() {
		return String.format("%02d:%02d", captureTimer.getMinutes(), captureTimer.getSeconds());
	}

	/**
	 * Timer is used to regenerate monument on timer end.
	 * Town Timer starts when a monument is damaged in KonquestListener.
	 * Town Timer restarts whenever damage is taken.
	 */
	@Override
	public void onEndTimer(int taskID) {
		if(taskID == 0) {
			ChatUtil.printDebug("Town Timer ended with null taskID!");
		} else if(taskID == monumentTimer.getTaskID()) {
			ChatUtil.printDebug("Monument Timer ended with taskID: "+taskID);
			// When a monument timer ends
			monument.clearCriticalHits();
			pasteMonumentFromTemplate(getKingdom().getMonumentTemplate());
			setAttacked(false);
			setBarProgress(1.0);
			updateBar();
			
			//String kingdomName = getKingdom().getName();
			//int numPlayers = getKonquest().getPlayerManager().getPlayersInKingdom(kingdomName).size();
			//ChatUtil.printDebug("Found "+numPlayers+" in Kingdom "+kingdomName+" for message sender");
			for(KonPlayer player : getKonquest().getPlayerManager().getPlayersInKingdom(getKingdom().getName())) {
				//ChatUtil.printDebug("Sent monument safe message to player "+player.getBukkitPlayer().getName());
				//ChatUtil.sendNotice(player.getBukkitPlayer(), "The Town of "+getName()+" is safe, for now...", ChatColor.DARK_GREEN);
				ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_SAFE.getMessage(getName()), ChatColor.DARK_GREEN);
			}
		} else if(taskID == captureTimer.getTaskID()) {
			ChatUtil.printDebug("Capture Timer ended with taskID: "+taskID);
			// When a capture cooldown timer ends
			isCaptureDisabled = false;
		} else if(taskID == raidAlertTimer.getTaskID()) {
			ChatUtil.printDebug("Raid Alert Timer ended with taskID: "+taskID);
			// When a raid alert cooldown timer ends
			isRaidAlertDisabled = false;
		} else {
			// Check for timer in player travel cooldown map
			for(UUID uuid : playerTravelTimers.keySet()) {
				if(taskID == playerTravelTimers.get(uuid).getTaskID()) {
					String name = Bukkit.getOfflinePlayer(uuid).getName();
					ChatUtil.printDebug("Travel Cooldown Timer ended for player: "+name);
					playerTravelTimers.remove(uuid);
					return;
				}
			}
			ChatUtil.printDebug("Town Timer ended with unknown taskID: "+taskID);
		}
	}
	
	public Timer getMonumentTimer() {
		return monumentTimer;
	}
	
	public Timer getCaptureTimer() {
		return captureTimer;
	}
	
	public Timer getRaidAlertTimer() {
		return raidAlertTimer;
	}
	
	public void updateBarPlayers() {
		monumentBarAllies.removeAll();
		monumentBarEnemies.removeAll();
		for(KonPlayer player : getKonquest().getPlayerManager().getPlayersOnline()) {
			Player bukkitPlayer = player.getBukkitPlayer();
			if(isLocInside(bukkitPlayer.getLocation())) {
				if(player.getKingdom().equals(getKingdom())) {
					monumentBarAllies.addPlayer(bukkitPlayer);
				} else {
					monumentBarEnemies.addPlayer(bukkitPlayer);
				}
			}
		}
	}
	
	public void addBarPlayer(KonPlayer player) {
		if(player.getKingdom().equals(getKingdom())) {
			monumentBarAllies.addPlayer(player.getBukkitPlayer());
		} else {
			monumentBarEnemies.addPlayer(player.getBukkitPlayer());
		}
	}
	
	public void removeBarPlayer(KonPlayer player) {
		monumentBarAllies.removePlayer(player.getBukkitPlayer());
		monumentBarEnemies.removePlayer(player.getBukkitPlayer());
	}
	
	public void removeAllBarPlayers() {
		monumentBarAllies.removeAll();
		monumentBarEnemies.removeAll();
	}
	
	public void setBarProgress(double prog) {
		monumentBarAllies.setProgress(prog);
		monumentBarEnemies.setProgress(prog);
	}
	
	public void setAttacked(boolean val) {
		this.isAttacked = val;
		if(val == false) {
			defenders.clear();
		}
	}
	
	public boolean isAttacked() {
		return isAttacked;
	}
	
	public void updateBar() {
		if(isAttacked) {
			monumentBarAllies.setTitle(ChatColor.GREEN+getName()+" Critical Hits Remaining");
			monumentBarAllies.setColor(BarColor.PURPLE);
			monumentBarEnemies.setTitle(ChatColor.RED+getName()+" Critical Hits Remaining");
			monumentBarEnemies.setColor(BarColor.PURPLE);
		} else {
			monumentBarAllies.setTitle(ChatColor.GREEN+getName());
			monumentBarAllies.setColor(BarColor.GREEN);
			monumentBarEnemies.setTitle(ChatColor.RED+getName());
			monumentBarEnemies.setColor(BarColor.RED);
		}
	}
	
	public boolean addDefender(Player bukkitPlayer) {
		boolean result = false;
		if(!defenders.contains(bukkitPlayer.getUniqueId())) {
			defenders.add(bukkitPlayer.getUniqueId());
			result = true;
		}
		return result;
	}
	
	public void setIsOpen(boolean val) {
		isOpen = val;
	}
	
	public boolean isOpen() {
		return isOpen ? true : false;
	}
	
	public boolean canClaimLordship(KonPlayer player) {
		boolean result = false;
		if(!isLordValid()) {
			if(isOpen()) {
				result = true;
			} else {
				if(getPlayerResidents().isEmpty()) {
					result = true;
				} else if(isPlayerResident(player.getOfflineBukkitPlayer())) {
					result = true;
				}
			}
		}
		return result;
	}
	
	public void purgeResidents() {
		lord = null;
		residents.clear();
		joinRequests.clear();
	}
	
	public void setPlayerLord(OfflinePlayer player) {
		lord = player.getUniqueId();
		if(!residents.containsKey(player.getUniqueId())) {
			residents.put(player.getUniqueId(),true);
			getKonquest().getUpgradeManager().updateTownDisabledUpgrades(this);
		}
	}
	
	public void setLord(UUID id) {
		lord = id;
		if(!residents.containsKey(id)) {
			residents.put(id,true);
			getKonquest().getUpgradeManager().updateTownDisabledUpgrades(this);
		}
	}
	
	public boolean isPlayerLord(OfflinePlayer player) {
		boolean status = false;
		if(lord != null) {
			status = player.getUniqueId().equals(lord);
		}
		return status;
	}
	
	public boolean setPlayerElite(OfflinePlayer player, boolean val) {
		boolean status = true;
		UUID playerUUID = player.getUniqueId();
		if(residents.containsKey(playerUUID)) {
			residents.put(playerUUID,val);
		} else {
			status = false;
		}
		return status;
	}
	
	// Returns true when player is Lord or Knight
	public boolean isPlayerElite(OfflinePlayer player) {
		boolean status = true;
		UUID playerUUID = player.getUniqueId();
		if(residents.containsKey(playerUUID)) {
			status = residents.get(playerUUID);
		} else {
			status = false;
		}
		return status;
	}
	
	public boolean addPlayerResident(OfflinePlayer player, boolean isElite) {
		boolean status = false;
		UUID playerUUID = player.getUniqueId();
		if(!residents.containsKey(playerUUID)) {
			residents.put(playerUUID,isElite);
			getKonquest().getUpgradeManager().updateTownDisabledUpgrades(this);
			status = true;
		}
		return status;
	}
	
	public boolean removePlayerResident(OfflinePlayer player) {
		boolean status = false;
		UUID playerUUID = player.getUniqueId();
		if(residents.containsKey(playerUUID)) {
			residents.remove(playerUUID);
			if(isPlayerLord(player)) {
				lord = null;
			}
			getKonquest().getUpgradeManager().updateTownDisabledUpgrades(this);
			status = true;
		}
		return status;
	}
	
	public boolean isPlayerResident(OfflinePlayer player) {
		return residents.containsKey(player.getUniqueId());
	}
	
	public boolean isLordValid() {
		return lord != null ? true : false;
	}
	
	public OfflinePlayer getPlayerLord() {
		if(lord != null) {
			return Bukkit.getOfflinePlayer(lord);
		}
		return null;
	}
	
	public ArrayList<OfflinePlayer> getPlayerElites() {
		ArrayList<OfflinePlayer> eliteList = new ArrayList<OfflinePlayer>();
		for(UUID id : residents.keySet()) {
			if(residents.get(id)) {
				eliteList.add(Bukkit.getOfflinePlayer(id));
			}
		}
		return eliteList;
	}
	
	public ArrayList<OfflinePlayer> getPlayerResidents() {
		ArrayList<OfflinePlayer> residentList = new ArrayList<OfflinePlayer>();
		for(UUID id : residents.keySet()) {
			residentList.add(Bukkit.getOfflinePlayer(id));
		}
		return residentList;
	}
	
	public int getNumResidents() {
		return residents.size();
	}
	
	public int getNumResidentsOnline() {
		int result = 0;
		for(UUID id : residents.keySet()) {
			if(Bukkit.getOfflinePlayer(id).isOnline()) {
				result++;
			}
		}
		return result;
	}
	
	public void addUpgrade(KonUpgrade upgrade, int level) {
		upgrades.put(upgrade, level);
	}
	
	public boolean hasUpgrade(KonUpgrade upgrade) {
		return upgrades.containsKey(upgrade);
	}
	
	public void clearUpgrades() {
		upgrades.clear();
	}
	
	// Returns current upgrade level, modified when disabled
	public int getUpgradeLevel(KonUpgrade upgrade) {
		int result = 0;
		if(upgrades.containsKey(upgrade)) {
			if(disabledUpgrades.containsKey(upgrade)) {
				result = disabledUpgrades.get(upgrade);
			} else {
				result = upgrades.get(upgrade);
			}
		}
		return result;
	}
	
	public int getRawUpgradeLevel(KonUpgrade upgrade) {
		int result = 0;
		if(upgrades.containsKey(upgrade)) {
			result = upgrades.get(upgrade);
		}
		return result;
	}
	
	public void disableUpgrade(KonUpgrade upgrade, int newLevel) {
		disabledUpgrades.put(upgrade,newLevel);
	}
	
	public boolean allowUpgrade(KonUpgrade upgrade) {
		boolean result = false;
		if(disabledUpgrades.containsKey(upgrade)) {
			disabledUpgrades.remove(upgrade);
			result = true;
		}
		return result;
	}
	
	public boolean isUpgradeDisabled(KonUpgrade upgrade) {
		return disabledUpgrades.containsKey(upgrade);
	}
	
	// Players who have tried joining but need to be added
	public List<OfflinePlayer> getJoinRequests() {
		ArrayList<OfflinePlayer> result = new ArrayList<OfflinePlayer>();
		for(UUID id : joinRequests.keySet()) {
			if(joinRequests.get(id) == false) {
				result.add(Bukkit.getOfflinePlayer(id));
			}
		}
		return result;
	}
	
	// Players who have been added but need to join
	public List<OfflinePlayer> getJoinInvites() {
		ArrayList<OfflinePlayer> result = new ArrayList<OfflinePlayer>();
		for(UUID id : joinRequests.keySet()) {
			if(joinRequests.get(id) == true) {
				result.add(Bukkit.getOfflinePlayer(id));
			}
		}
		return result;
	}
	
	public boolean addJoinRequest(UUID id, Boolean type) {
		boolean result = false;
		if(!joinRequests.containsKey(id)) {
			joinRequests.put(id, type);
			result = true;
		}
		return result;
	}
	
	// Does the player have an existing request to be added?
	public boolean isJoinRequestValid(UUID id) {
		boolean result = false;
		if(joinRequests.containsKey(id)) {
			result = (joinRequests.get(id) == false);
		}
		return result;
	}
	
	// Does the player have an existing invite to join?
	public boolean isJoinInviteValid(UUID id) {
		boolean result = false;
		if(joinRequests.containsKey(id)) {
			result = (joinRequests.get(id) == true);
		}
		return result;
	}
	
	public void removeJoinRequest(UUID id) {
		joinRequests.remove(id);
	}
	
	public void notifyJoinRequest(UUID id) {
		String name = Bukkit.getOfflinePlayer(id).getName();
		for(OfflinePlayer offlinePlayer : getPlayerElites()) {
			if(offlinePlayer.isOnline()) {
				//ChatUtil.sendNotice((Player)offlinePlayer, "Received new request from "+name+" to join "+getName());
				//ChatUtil.sendNotice((Player)offlinePlayer, name+" wants to join "+getName()+", use \"/k town "+getName()+" add "+name+"\" to allow, \"/k town "+getName()+" kick "+name+"\" to deny", ChatColor.LIGHT_PURPLE);
				ChatUtil.sendNotice((Player)offlinePlayer, MessagePath.GENERIC_NOTICE_JOIN_REQUEST.getMessage(name,getName(),getName(),name,getName(),name), ChatColor.LIGHT_PURPLE);
			}
		}
	}
	
	public boolean isTownWatchProtected() {
		boolean result = false;
		int upgradeLevel = getKonquest().getUpgradeManager().getTownUpgradeLevel(this, KonUpgrade.WATCH);
		if(upgradeLevel > 0) {
			int minimumOnlineResidents = upgradeLevel; // 1, 2, 3
			if(getNumResidentsOnline() < minimumOnlineResidents) {
				result = true;
			}
		}
		return result;
	}
	
	public void spawnRabbit() {
		rabbit.spawn();
	}
	
}
