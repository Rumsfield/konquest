package konquest.model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import konquest.Konquest;
import konquest.api.model.KonquestUpgrade;
import konquest.api.model.KonquestTerritoryType;
import konquest.api.model.KonquestTown;
import konquest.utility.BlockPaster;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;
import konquest.utility.RequestKeeper;
import konquest.utility.Timeable;
import konquest.utility.Timer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;


/**
 * KonTown - Represents a Town Territory, has a center chunk monument
 * @author Rumsfield
 * @prerequisites	The Town's Kingdom must have a valid Monument Template
 */
public class KonTown extends KonTerritory implements KonquestTown, KonBarDisplayer, Timeable {
	
	private KonMonument monument;
	private Timer monumentTimer;
	private Timer captureTimer;
	private Timer raidAlertTimer;
	private Timer shieldTimer;
	private HashMap<UUID, Timer> playerTravelTimers;
	private boolean isCaptureDisabled;
	private boolean isRaidAlertDisabled;
	private BossBar monumentBarAllies;
	private BossBar monumentBarEnemies;
	private BossBar monumentBarArmistice;
	//private BossBar shieldArmorBarAll;
	private UUID lord;
	private HashMap<UUID,Boolean> residents;
	private RequestKeeper joinRequestKeeper;
	private boolean isOpen;
	private boolean isEnemyRedstoneAllowed;
	private boolean isResidentPlotOnly;
	private boolean isAttacked;
	private boolean isShielded;
	private boolean isArmored;
	private int shieldEndTimeSeconds;
	private int shieldNowTimeSeconds;
	private int armorTotalBlocks;
	private int armorCurrentBlocks;
	private double armorProgress;
	private ArrayList<UUID> defenders;
	private HashMap<KonquestUpgrade,Integer> upgrades;
	private HashMap<KonquestUpgrade,Integer> disabledUpgrades;
	private KonTownRabbit rabbit;
	private HashMap<Point,KonPlot> plots;
	
	public KonTown(Location loc, String name, KonKingdom kingdom, Konquest konquest) {
		super(loc, name, kingdom, KonquestTerritoryType.TOWN, konquest);
		// Assume there is a valid Monument Template
		this.monument = new KonMonument(loc);
		this.monumentTimer = new Timer(this);
		this.captureTimer = new Timer(this);
		this.raidAlertTimer = new Timer(this);
		this.shieldTimer = new Timer(this);
		this.playerTravelTimers = new HashMap<UUID, Timer>();
		this.isCaptureDisabled = false;
		this.isRaidAlertDisabled = false;
		this.monumentBarAllies = Bukkit.getServer().createBossBar(Konquest.friendColor1+name, ChatUtil.mapBarColor(Konquest.friendColor1), BarStyle.SOLID);
		this.monumentBarAllies.setVisible(true);
		this.monumentBarAllies.setProgress(1.0);
		this.monumentBarEnemies = Bukkit.getServer().createBossBar(Konquest.enemyColor1+name, ChatUtil.mapBarColor(Konquest.enemyColor1), BarStyle.SOLID);
		this.monumentBarEnemies.setVisible(true);
		this.monumentBarEnemies.setProgress(1.0);
		this.monumentBarArmistice = Bukkit.getServer().createBossBar(Konquest.armisticeColor1+name, ChatUtil.mapBarColor(Konquest.armisticeColor1), BarStyle.SOLID);
		this.monumentBarArmistice.setVisible(true);
		this.monumentBarArmistice.setProgress(1.0);
		//this.shieldArmorBarAll = Bukkit.getServer().createBossBar(ChatColor.DARK_AQUA+"Shield", BarColor.BLUE, BarStyle.SOLID);
		//this.shieldArmorBarAll.setVisible(false);
		//this.shieldArmorBarAll.setProgress(0);
		this.lord = null; // init with no lord
		this.residents = new HashMap<UUID,Boolean>();
		this.joinRequestKeeper = new RequestKeeper();
		this.isOpen = false; // init as a closed Town, requires Lord to add players as residents for build/container perms
		this.isEnemyRedstoneAllowed = false;
		this.isResidentPlotOnly = false;
		this.isAttacked = false;
		this.isShielded = false;
		this.shieldEndTimeSeconds = 0;
		this.shieldNowTimeSeconds = 0;
		this.armorTotalBlocks = 0;
		this.armorCurrentBlocks = 0;
		this.armorProgress = 0.0;
		this.defenders = new ArrayList<UUID>();
		this.upgrades = new HashMap<KonquestUpgrade,Integer>();
		this.disabledUpgrades = new HashMap<KonquestUpgrade,Integer>();
		this.rabbit = new KonTownRabbit(getSpawnLoc());
		this.plots = new HashMap<Point,KonPlot>();
	}

	/**
	 * Adds a chunk to the chunkMap and checks to make sure its within the max distance from town center
	 * @param chunk
	 * @return true if chunk is within max distance from town center, else false
	 */
	@Override
	public boolean addChunk(Point point) {
		boolean testResult = testChunk(point);
		if(testResult) {
			addPoint(point);
		}
		return testResult;
	}
	
	@Override
	public boolean testChunk(Point point) {
		Point centerChunk = Konquest.toPoint(getCenterLoc());
		int maxChunkRange = getKonquest().getConfigManager().getConfig("core").getInt("core.towns.max_size");
		if(maxChunkRange < 0) {
			maxChunkRange = 0;
		}
		if(maxChunkRange >= 1) {
			int distX = (int)Math.abs(point.getX() - centerChunk.getX());
			int distY = (int)Math.abs(point.getY() - centerChunk.getY());
			if(distX > (maxChunkRange-1) || distY > (maxChunkRange-1)) {
				ChatUtil.printDebug("Failed to test chunk in territory "+getName()+", too far");
				return false;
			}
		}
		return true;
	}

	/**
	 * initClaim - initializes the Monument, pastes the template and claims chunks.
	 * @return  0 - success
	 * 			11 - error, monument invalid
	 * 			12 - error, monument gradient
	 * 			13 - error, monument bedrock
	 * 			2 - error, bad town height
	 * 			3 - error, bad chunks
	 * 			4 - error, too much air below town
	 * 			5 - error, too much water below town
	 * 			6 - error, containers below monument
	 */
	@Override
	public int initClaim() {
		
		// Verify monument template and chunk gradient and initialize travelPoint and baseY coordinate
		int flatness = getKonquest().getConfigManager().getConfig("core").getInt("core.towns.settle_check_flatness",3);
		int monumentStatus = monument.initialize(getKingdom().getMonumentTemplate(), getCenterLoc(), flatness);
		if(monumentStatus != 0) {
			ChatUtil.printDebug("Town init failed: monument did not initialize correctly");
			return 10 + monumentStatus;
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
		int world_min_y = getWorld().getMinHeight();
		int world_max_y = getWorld().getMaxHeight();
		if(monument.getBaseY() <= world_min_y) {
			ChatUtil.printDebug("Town init failed: "+monument.getBaseY()+" less than world min limit "+world_min_y);
			return 2;
		}
		if(monument.getBaseY() >= world_max_y) {
			ChatUtil.printDebug("Town init failed: "+monument.getBaseY()+" greater than world max limit "+world_max_y);
			return 2;
		}
		
		// Verify monument template will not pass the height limit
		if(monument.getBaseY() + getKingdom().getMonumentTemplate().getHeight() + 1 >= getWorld().getMaxHeight()) {
			ChatUtil.printDebug("Town init failed: too high");
			return 2;
		}
		
		// Iterate over every block in the chunk to check for bad conditions
		int countWaterBlocks = 0;
		int countAirBlocks = 0;
		int countContainers = 0;
		int baseDepth = getKonquest().getConfigManager().getConfig("core").getInt("core.towns.settle_checks_depth",0);
		int yMin = 0;
		int yMax = monument.getBaseY();
		if(baseDepth > 0) {
			yMin = monument.getBaseY() - baseDepth;
		}
		if(yMin < getWorld().getMinHeight()) {
			yMin = getWorld().getMinHeight();
		}
		if(yMax > getWorld().getMaxHeight()-1) {
			yMax = getWorld().getMaxHeight()-1;
		}
		for (int x = 0; x <= 15; x++) {
            for (int y = getWorld().getMinHeight(); y <= getWorld().getMaxHeight()-1; y++) {
                for (int z = 0; z <= 15; z++) {
                    Block currentBlock = getWorld().getChunkAt(getCenterLoc()).getBlock(x, y, z);
                    // Checks for any block in the chunk
                    if(currentBlock.getType().equals(Material.WATER)) {
                    	countWaterBlocks++;
                    }
                    // Checks for blocks within min max monument Y levels
                    if(y >= yMin && y <= yMax) {
                    	if(currentBlock.getType().equals(Material.AIR)) {
                    		countAirBlocks++;
                        } else if(currentBlock.getState() instanceof Container) {
                        	countContainers++;
                        }
                    }
                }
            }
        }
		
		// Verify there is not too much air below the base Y location, no more than 10 layers of 16x16 blocks.
		if(baseDepth > 0 && countAirBlocks > 2560) {
			ChatUtil.printDebug("Town init failed: too much air below monument");
			return 4;
		}
		
		// Verify there is not too much water in this chunk, no more than 5 layers of 16x16 blocks.
		if(countWaterBlocks > 1280) {
			ChatUtil.printDebug("Town init failed: too much water in the chunk");
			return 5;
		}
		
		// Verify there are not too many containers below the monument
		if(baseDepth > 0 && countContainers > 0) {
			ChatUtil.printDebug("Town init failed: too many containers in the chunk");
			return 6;
		}
        
		// Add chunks around the monument chunk
		int radius = getKonquest().getConfigManager().getConfig("core").getInt("core.towns.init_radius");
		if(radius < 1) {
			radius = 1;
		}
		if(!addChunks(getKonquest().getAreaPoints(getCenterLoc(), radius))) {
			ChatUtil.printDebug("Town init failed: problem adding some chunks");
			return 3;
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
		//Date start = new Date();
		monument.setIsItemDropsDisabled(true);
		monument.setIsDamageDisabled(true);
		
		int topBlockX = Math.max(template.getCornerOne().getBlockX(), template.getCornerTwo().getBlockX());
        int topBlockY = Math.max(template.getCornerOne().getBlockY(), template.getCornerTwo().getBlockY());
        int topBlockZ = Math.max(template.getCornerOne().getBlockZ(), template.getCornerTwo().getBlockZ());
        int bottomBlockX = Math.min(template.getCornerOne().getBlockX(), template.getCornerTwo().getBlockX());
        int bottomBlockY = Math.min(template.getCornerOne().getBlockY(), template.getCornerTwo().getBlockY());
        int bottomBlockZ = Math.min(template.getCornerOne().getBlockZ(), template.getCornerTwo().getBlockZ());
        
        // Determine minimum Y level of paste chunk below monument Y base
        int monument_y = monument.getBaseY();
        int fill_y = getCenterLoc().getWorld().getMinHeight();
        int min_fill_y = getCenterLoc().getWorld().getMinHeight();
        //Date step1 = new Date();
        /*
        int pasteX = (int)Math.floor((double)getCenterLoc().getBlockX()/16);
        int pasteZ = (int)Math.floor((double)getCenterLoc().getBlockZ()/16);
        if(getCenterLoc().getWorld().isChunkLoaded(pasteX,pasteZ)) {
        	ChatUtil.printDebug("Paste fill chunk ("+pasteX+","+pasteZ+") is loaded");
        } else {
        	ChatUtil.printDebug("Paste fill chunk ("+pasteX+","+pasteZ+") is NOT loaded!");
        }
        */
        //Chunk fillChunk = getCenterLoc().getWorld().getChunkAt(getCenterLoc());
        Chunk fillChunk = getCenterLoc().getChunk();
        //Date step2 = new Date();
        ChunkSnapshot fillChunkSnap = fillChunk.getChunkSnapshot(true,false,false);
        //Date step3 = new Date();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
            	fill_y = fillChunkSnap.getHighestBlockYAt(x, z);
            	while((fillChunk.getBlock(x, fill_y, z).isPassable() || !fillChunkSnap.getBlockType(x, fill_y, z).isOccluding()) && fill_y > fillChunk.getWorld().getMinHeight()) {
            		fill_y--;
				}
            	if((x == 0 && z == 0) || fill_y < min_fill_y) {
            		min_fill_y = fill_y;
            	}
            }
        }
        //Date step4 = new Date();
        //ChatUtil.printDebug("Pasting monument ("+fillChunk.getX()+","+fillChunk.getZ()+") at base "+monument_y+" found minimum Y level: "+min_fill_y);
        // Fill air between world and monument base
        if(min_fill_y < monument_y) {
	        for (int x = 0; x < 16; x++) {
	            for (int z = 0; z < 16; z++) {
                	for (int y = min_fill_y; y <= monument_y; y++) {
                		fillChunk.getBlock(x, y, z).setType(Material.STONE);
                	}
	            }
	        }
        }
        //Date step5 = new Date();
        //Chunk pasteChunk = getCenterLoc().getWorld().getChunkAt(getCenterLoc());
        World templateWorld = template.getCornerOne().getWorld();
        BlockPaster monumentPaster = new BlockPaster(fillChunk,templateWorld,bottomBlockY,monument.getBaseY(),bottomBlockY,topBlockX,topBlockZ,bottomBlockX,bottomBlockZ);
        for (int y = bottomBlockY; y <= topBlockY; y++) {
        	monumentPaster.setY(y);
        	//BlockPaster monumentPaster = new BlockPaster(getCenterLoc(),y,monument.getBaseY(),bottomBlockY,topBlockX,topBlockZ,bottomBlockX,bottomBlockZ);
        	monumentPaster.startPaste();
        }
        monument.setIsItemDropsDisabled(false);
        monument.setIsDamageDisabled(false);
        //Date step6 = new Date();
        /*
        int s1 = (int)(step1.getTime()-start.getTime());
    	int s2 = (int)(step2.getTime()-start.getTime());
		int s3 = (int)(step3.getTime()-start.getTime());
		int s4 = (int)(step4.getTime()-start.getTime());
		int s5 = (int)(step5.getTime()-start.getTime());
		int s6 = (int)(step6.getTime()-start.getTime());
		ChatUtil.printDebug("Monument paste timings: "+s1+","+s2+","+s3+","+s4+","+s5+","+s6);
		*/
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
	
	/**
	 * Counts all water blocks in the entire chunk
	 * @return
	 */
	public int countWaterInChunk() {
		int count = 0;
		for (int x = 0; x <= 15; x++) {
            for (int y = getWorld().getMinHeight(); y <= getWorld().getMaxHeight()-1; y++) {
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
	
	/**
	 * Count the number of air blocks below the monument base, for height specified by limit
	 * @param limit - The height below the monument base to check for air
	 * @return
	 */
	public int countAirBelowMonument(int limit) {
		int count = 0;
		int yMin = 0;
		int yMax = monument.getBaseY();
		if(limit > 0) {
			yMin = monument.getBaseY() - limit;
		}
		if(yMin < getWorld().getMinHeight()) {
			yMin = getWorld().getMinHeight();
		}
		if(yMax > getWorld().getMaxHeight()-1) {
			yMax = getWorld().getMaxHeight()-1;
		}
		for (int x = 0; x <= 15; x++) {
            for (int y = yMin; y <= yMax; y++) {
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
	
	/*
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
	*/
	
	public boolean isLocInsideCenterChunk(Location loc) {
		Point centerPoint = Konquest.toPoint(getCenterLoc());
		Point testPoint = Konquest.toPoint(loc);
		return centerPoint.x == testPoint.x && centerPoint.y == testPoint.y;
	}
	
	public boolean isChunkCenter(Chunk chunk) {
		Point centerPoint = Konquest.toPoint(getCenterLoc());
		Point testPoint = Konquest.toPoint(chunk);
		return centerPoint.x == testPoint.x && centerPoint.y == testPoint.y;
	}
	
	public boolean isLocInsideMonumentProtectionArea(Location loc) {
		Point centerPoint = Konquest.toPoint(getCenterLoc());
		Point testPoint = Konquest.toPoint(loc);
		int baseDepth = getKonquest().getConfigManager().getConfig("core").getInt("core.towns.settle_checks_depth",0);
		if(baseDepth < 0) {
			baseDepth = 0;
		}
		int bottomLevel = monument.getBaseY() - baseDepth;
		if(bottomLevel < getWorld().getMinHeight()) {
			baseDepth = getWorld().getMinHeight();
		}
		return centerPoint.x == testPoint.x && centerPoint.y == testPoint.y && loc.getBlockY() >= bottomLevel;
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
			//monument.setHeight(template.getHeight());
			monument.updateFromTemplate(template);
			monument.setIsValid(true);
			//pasteMonumentFromTemplate(getKingdom().getMonumentTemplate());
		} else {
			status = 1;
		}
		return status;
	}
	
	/**
	 * Reloads a monument from template, meant to be used when server loads the chunk
	 */
	public boolean reloadMonument() {
		boolean result = false;
		if(monument.isValid()) {
			result = monument.updateFromTemplate(getKingdom().getMonumentTemplate());
			pasteMonumentFromTemplate(getKingdom().getMonumentTemplate());
			setSpawn(monument.getTravelPoint());
		}
		return result;
	}
	
	/**
	 * Updates a monument from template, meant to be used for town capture
	 */
	public void refreshMonument() {
		if(monument.isValid()) {
			removeMonumentBlocks();
			monument.clearCriticalHits();
			monument.updateFromTemplate(getKingdom().getMonumentTemplate());
			setSpawn(monument.getTravelPoint());
			// Delay paste by 1 tick
			Bukkit.getScheduler().scheduleSyncDelayedTask(getKonquest().getPlugin(), new Runnable() {
	            @Override
	            public void run() {
	            	pasteMonumentFromTemplate(getKingdom().getMonumentTemplate());
	            }
	        },1);
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
			//pasteMonumentFromTemplate(getKingdom().getMonumentTemplate());
			reloadMonument();
			setAttacked(false);
			//setBarProgress(1.0);
			updateBar();
			getWorld().playSound(getCenterLoc(), Sound.BLOCK_ANVIL_USE, (float)1, (float)0.8);
			//String kingdomName = getKingdom().getName();
			//int numPlayers = getKonquest().getPlayerManager().getPlayersInKingdom(kingdomName).size();
			//ChatUtil.printDebug("Found "+numPlayers+" in Kingdom "+kingdomName+" for message sender");
			for(KonPlayer player : getKonquest().getPlayerManager().getPlayersInKingdom(getKingdom().getName())) {
				//ChatUtil.printDebug("Sent monument safe message to player "+player.getBukkitPlayer().getName());
				//ChatUtil.sendNotice(player.getBukkitPlayer(), "The Town of "+getName()+" is safe, for now...", ChatColor.DARK_GREEN);
				ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_SAFE.getMessage(getName()), ChatColor.GREEN);
			}
		} else if(taskID == captureTimer.getTaskID()) {
			ChatUtil.printDebug("Capture Timer ended with taskID: "+taskID);
			// When a capture cooldown timer ends
			isCaptureDisabled = false;
		} else if(taskID == raidAlertTimer.getTaskID()) {
			ChatUtil.printDebug("Raid Alert Timer ended with taskID: "+taskID);
			// When a raid alert cooldown timer ends
			isRaidAlertDisabled = false;
		} else if(taskID == shieldTimer.getTaskID()) {
			// When shield loop timer ends (1 second loop)
			//ChatUtil.printDebug("Town "+getName()+" shield tick with taskID: "+taskID);
			Date now = new Date();
			shieldNowTimeSeconds = (int)(now.getTime()/1000);
			if(shieldEndTimeSeconds < shieldNowTimeSeconds) {
				deactivateShield();
			} else {
				//refreshShieldBarTitle();
				updateBar();
			}
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
		monumentBarArmistice.removeAll();
		//shieldArmorBarAll.removeAll();
		for(KonPlayer player : getKonquest().getPlayerManager().getPlayersOnline()) {
			Player bukkitPlayer = player.getBukkitPlayer();
			if(isLocInside(bukkitPlayer.getLocation())) {
				if(player.getKingdom().equals(getKingdom())) {
					monumentBarAllies.addPlayer(bukkitPlayer);
				} else {
					if(getKonquest().getGuildManager().isArmistice(player, this)) {
						monumentBarArmistice.addPlayer(bukkitPlayer);
					} else {
						monumentBarEnemies.addPlayer(bukkitPlayer);
					}
				}
				//shieldArmorBarAll.addPlayer(bukkitPlayer);
			}
		}
	}
	
	public void addBarPlayer(KonPlayer player) {
		if(player.getKingdom().equals(getKingdom())) {
			monumentBarAllies.addPlayer(player.getBukkitPlayer());
		} else {
			if(getKonquest().getGuildManager().isArmistice(player, this)) {
				monumentBarArmistice.addPlayer(player.getBukkitPlayer());
			} else {
				monumentBarEnemies.addPlayer(player.getBukkitPlayer());
			}
		}
		//shieldArmorBarAll.addPlayer(player.getBukkitPlayer());
	}
	
	public void removeBarPlayer(KonPlayer player) {
		monumentBarAllies.removePlayer(player.getBukkitPlayer());
		monumentBarEnemies.removePlayer(player.getBukkitPlayer());
		monumentBarArmistice.removePlayer(player.getBukkitPlayer());
		//shieldArmorBarAll.removePlayer(player.getBukkitPlayer());
	}
	
	public void removeAllBarPlayers() {
		monumentBarAllies.removeAll();
		monumentBarEnemies.removeAll();
		monumentBarArmistice.removeAll();
		//shieldArmorBarAll.removeAll();
	}
	
	public void setBarProgress(double prog) {
		monumentBarAllies.setProgress(prog);
		monumentBarEnemies.setProgress(prog);
		monumentBarArmistice.setProgress(prog);
	}
	
	public void setAttacked(boolean val) {
		if(val == true) {
			this.isAttacked = true;
			// Start Monument regenerate timer for target town when no armor nor shield
			int monumentRegenTimeSeconds = getKonquest().getConfigManager().getConfig("core").getInt("core.monuments.damage_regen");
			monumentTimer.stopTimer();
			monumentTimer.setTime(monumentRegenTimeSeconds);
			monumentTimer.startTimer();
			/*
			if(!(isShielded || isArmored)) {
				
			}
			*/
		} else {
			this.isAttacked = false;
			defenders.clear();
		}
	}
	
	public boolean isAttacked() {
		return isAttacked;
	}
	
	public void updateBar() {
		/*
		 * When town block is broken without shield or armor, attacked=true & show critical hit progress.
		 * If shields or armor are added while attacked, preserve attacked state and critical progress in background,
		 * but display armor/shield progress. Refresh regen timer while armor/shield is active when blocks broken.
		 * When shields/armor are depleted, and regen timer has not expired, restore attacked title & critical bar progress.
		 * Otherwise, when regen timer has expired, restore to normal non-attacked title & 1.0 progress.
		 */
		String separator = " - ";
		int remainingSeconds = 0;
		String remainingTime = "";
		String armor = MessagePath.LABEL_ARMOR.getMessage();
		String shield = MessagePath.LABEL_SHIELD.getMessage();
		String critical = MessagePath.LABEL_CRITICAL_HITS.getMessage();
		
		// Set title conditions
		if(isShielded && isArmored) {
			remainingSeconds = getRemainingShieldTimeSeconds();
			remainingTime = Konquest.getTimeFormat(remainingSeconds,Konquest.friendColor1);
			monumentBarAllies.setTitle(Konquest.friendColor1+getName()+separator+armorCurrentBlocks+" "+armor+" | "+shield+" "+remainingTime);
			remainingTime = Konquest.getTimeFormat(remainingSeconds,Konquest.enemyColor1);
			monumentBarEnemies.setTitle(Konquest.enemyColor1+getName()+separator+armorCurrentBlocks+" "+armor+" | "+shield+" "+remainingTime);
			remainingTime = Konquest.getTimeFormat(remainingSeconds,Konquest.armisticeColor1);
			monumentBarArmistice.setTitle(Konquest.armisticeColor1+getName()+separator+armorCurrentBlocks+" "+armor+" | "+shield+" "+remainingTime);
		} else if(isShielded) {
			remainingSeconds = getRemainingShieldTimeSeconds();
			remainingTime = Konquest.getTimeFormat(remainingSeconds,Konquest.friendColor1);
			monumentBarAllies.setTitle(Konquest.friendColor1+getName()+separator+shield+" "+remainingTime);
			remainingTime = Konquest.getTimeFormat(remainingSeconds,Konquest.enemyColor1);
			monumentBarEnemies.setTitle(Konquest.enemyColor1+getName()+separator+shield+" "+remainingTime);
			remainingTime = Konquest.getTimeFormat(remainingSeconds,Konquest.armisticeColor1);
			monumentBarArmistice.setTitle(Konquest.armisticeColor1+getName()+separator+shield+" "+remainingTime);
		} else if(isArmored) {
			monumentBarAllies.setTitle(Konquest.friendColor1+getName()+separator+armorCurrentBlocks+" "+armor);
			monumentBarEnemies.setTitle(Konquest.enemyColor1+getName()+separator+armorCurrentBlocks+" "+armor);
			monumentBarArmistice.setTitle(Konquest.armisticeColor1+getName()+separator+armorCurrentBlocks+" "+armor);
		} else if(isAttacked) {
			monumentBarAllies.setTitle(Konquest.friendColor1+getName()+separator+critical);
			monumentBarEnemies.setTitle(Konquest.enemyColor1+getName()+separator+critical);
			monumentBarArmistice.setTitle(Konquest.armisticeColor1+getName()+separator+critical);
		} else {
			monumentBarAllies.setTitle(Konquest.friendColor1+getName());
			monumentBarEnemies.setTitle(Konquest.enemyColor1+getName());
			monumentBarArmistice.setTitle(Konquest.armisticeColor1+getName());
		}
		
		// Set progress conditions
		if(isShielded || isArmored) {
			monumentBarAllies.setStyle(BarStyle.SEGMENTED_10);
			monumentBarEnemies.setStyle(BarStyle.SEGMENTED_10);
			monumentBarArmistice.setStyle(BarStyle.SEGMENTED_10);
			setBarProgress(armorProgress);
		} else if(isAttacked) {
			monumentBarAllies.setStyle(BarStyle.SOLID);
			monumentBarEnemies.setStyle(BarStyle.SOLID);
			monumentBarArmistice.setStyle(BarStyle.SOLID);
			// Set bar with critical hits
			int maxCriticalhits = getKonquest().getKingdomManager().getMaxCriticalHits();
			double progress = (double)(maxCriticalhits - getMonument().getCriticalHits()) / (double)maxCriticalhits;
			setBarProgress(progress);
		} else {
			monumentBarAllies.setStyle(BarStyle.SOLID);
			monumentBarEnemies.setStyle(BarStyle.SOLID);
			monumentBarArmistice.setStyle(BarStyle.SOLID);
			setBarProgress(1.0);
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
	
	public void applyGlow(Player bukkitPlayer) {
		boolean isGlowEnabled = getKonquest().getConfigManager().getConfig("core").getBoolean("core.towns.enemy_glow", true);
		if(isGlowEnabled) {
			bukkitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20*5, 1));
			//ChatUtil.printDebug("Applied glowing to "+bukkitPlayer.getName()+": "+bukkitPlayer.isGlowing());
		}
	}
	
	public boolean sendRaidAlert() {
		boolean result = false;
		// Attempt to start a raid alert
		if(!isRaidAlertDisabled()) {
			// Alert all players of this town's kingdom
			for(KonPlayer player : getKonquest().getPlayerManager().getPlayersInKingdom(getKingdom().getName())) {
				if(!player.isAdminBypassActive() && !player.getBukkitPlayer().getGameMode().equals(GameMode.SPECTATOR)) {
					ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_RAID.getMessage(getName(),getName()),ChatColor.DARK_RED);
					ChatUtil.sendKonPriorityTitle(player, ChatColor.DARK_RED+MessagePath.PROTECTION_NOTICE_RAID_ALERT.getMessage(), ChatColor.DARK_RED+""+getName(), 60, 1, 10);
				}
			}
			// Start Raid Alert disable timer for target town
			int raidAlertTimeSeconds = getKonquest().getConfigManager().getConfig("core").getInt("core.towns.raid_alert_cooldown");
			ChatUtil.printDebug("Starting raid alert timer for "+raidAlertTimeSeconds+" seconds in town "+getName());
			Timer raidAlertTimer = getRaidAlertTimer();
			setIsRaidAlertDisabled(true);
			raidAlertTimer.stopTimer();
			raidAlertTimer.setTime(raidAlertTimeSeconds);
			raidAlertTimer.startTimer();
		}
		return result;
	}
	
	public void setIsOpen(boolean val) {
		isOpen = val;
	}
	
	public boolean isOpen() {
		return isOpen ? true : false;
	}
	
	public void setIsEnemyRedstoneAllowed(boolean val) {
		isEnemyRedstoneAllowed = val;
	}
	
	public boolean isEnemyRedstoneAllowed() {
		return isEnemyRedstoneAllowed ? true : false;
	}
	
	public void setIsPlotOnly(boolean val) {
		isResidentPlotOnly = val;
	}
	
	public boolean isPlotOnly() {
		return isResidentPlotOnly ? true : false;
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
		joinRequestKeeper.clearRequests();
	}
	
	public void setPlayerLord(OfflinePlayer player) {
		setLord(player.getUniqueId());
	}
	
	public void setLord(UUID id) {
		lord = id;
		if(!residents.containsKey(id)) {
			residents.put(id,true);
			getKonquest().getUpgradeManager().updateTownDisabledUpgrades(this);
			getKonquest().getMapHandler().drawDynmapLabel(this);
		}
	}
	
	public boolean isLord(UUID id) {
		boolean status = false;
		if(lord != null) {
			status =id.equals(lord);
		}
		return status;
	}
	
	public boolean isPlayerLord(OfflinePlayer player) {
		return isLord(player.getUniqueId());
	}
	
	public boolean setPlayerKnight(OfflinePlayer player, boolean val) {
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
	public boolean isPlayerKnight(OfflinePlayer player) {
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
			getKonquest().getMapHandler().drawDynmapLabel(this);
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
			getKonquest().getMapHandler().drawDynmapLabel(this);
			if(residents.isEmpty()) {
				clearShieldsArmors();
			}
			for(KonPlot plot : getPlots()) {
				plot.removeUser(player);
			}
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
	
	public UUID getLord() {
		return lord;
	}
	
	public ArrayList<OfflinePlayer> getPlayerKnights() {
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
	
	public void addUpgrade(KonquestUpgrade upgrade, int level) {
		upgrades.put(upgrade, level);
	}
	
	public boolean hasUpgrade(KonquestUpgrade upgrade) {
		return upgrades.containsKey(upgrade);
	}
	
	public void clearUpgrades() {
		upgrades.clear();
	}
	
	// Returns current upgrade level, modified when disabled
	public int getUpgradeLevel(KonquestUpgrade upgrade) {
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
	
	public int getRawUpgradeLevel(KonquestUpgrade upgrade) {
		int result = 0;
		if(upgrades.containsKey(upgrade)) {
			result = upgrades.get(upgrade);
		}
		return result;
	}
	
	public void disableUpgrade(KonquestUpgrade upgrade, int newLevel) {
		disabledUpgrades.put(upgrade,newLevel);
	}
	
	public boolean allowUpgrade(KonquestUpgrade upgrade) {
		boolean result = false;
		if(disabledUpgrades.containsKey(upgrade)) {
			disabledUpgrades.remove(upgrade);
			result = true;
		}
		return result;
	}
	
	public boolean isUpgradeDisabled(KonquestUpgrade upgrade) {
		return disabledUpgrades.containsKey(upgrade);
	}
	
	// Players who have tried joining but need to be added
	public List<OfflinePlayer> getJoinRequests() {
		return joinRequestKeeper.getJoinRequests();
	}
	
	// Players who have been added but need to join
	public List<OfflinePlayer> getJoinInvites() {
		return joinRequestKeeper.getJoinInvites();
	}
	
	public boolean addJoinRequest(UUID id, Boolean type) {
		return joinRequestKeeper.addJoinRequest(id, type);
	}
	
	// Does the player have an existing request to be added?
	public boolean isJoinRequestValid(UUID id) {
		return joinRequestKeeper.isJoinRequestValid(id);
	}
	
	// Does the player have an existing invite to join?
	public boolean isJoinInviteValid(UUID id) {
		return joinRequestKeeper.isJoinInviteValid(id);
	}
	
	public void removeJoinRequest(UUID id) {
		joinRequestKeeper.removeJoinRequest(id);
	}
	
	public void notifyJoinRequest(UUID id) {
		String name = Bukkit.getOfflinePlayer(id).getName();
		for(OfflinePlayer offlinePlayer : getPlayerKnights()) {
			if(offlinePlayer.isOnline()) {
				//ChatUtil.sendNotice((Player)offlinePlayer, "Received new request from "+name+" to join "+getName());
				//ChatUtil.sendNotice((Player)offlinePlayer, name+" wants to join "+getName()+", use \"/k town "+getName()+" add "+name+"\" to allow, \"/k town "+getName()+" kick "+name+"\" to deny", ChatColor.LIGHT_PURPLE);
				ChatUtil.sendNotice((Player)offlinePlayer, MessagePath.GENERIC_NOTICE_JOIN_REQUEST.getMessage(name,getName(),getName(),name,getName(),name), ChatColor.LIGHT_PURPLE);
			}
		}
	}
	
	public boolean isTownWatchProtected() {
		boolean result = false;
		int upgradeLevel = getKonquest().getUpgradeManager().getTownUpgradeLevel(this, KonquestUpgrade.WATCH);
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
	
	public boolean isShielded() {
		return isShielded;
	}
	
	public boolean isArmored() {
		return isArmored;
	}
	
	public void activateShield(int val) {
		if(isShielded) {
			// Already shielded
			shieldEndTimeSeconds = val;
		} else {
			// Activate new shield
			isShielded = true;
			shieldEndTimeSeconds = val;
			Date now = new Date();
			shieldNowTimeSeconds = (int)(now.getTime()/1000);
			shieldTimer.stopTimer();
			shieldTimer.setTime(0);
			shieldTimer.startLoopTimer();
			// No longer attacked when shielded
			//setAttacked(false);
		}
		//shieldArmorBarAll.setVisible(true);
		//refreshShieldBarTitle();
		updateBar();
	}
	
	public void deactivateShield() {
		if(isShielded) {
			playDeactivateSound();
		}
		isShielded = false;
		shieldTimer.stopTimer();
		//refreshShieldBarTitle();
		updateBar();
	}
	
	public void activateArmor(int val) {
		if(!isArmored) {
			// Activate new armor
			isArmored = true;
			// No longer attacked when armored
			//setAttacked(false);
		}
		armorCurrentBlocks = val;
		armorTotalBlocks = armorCurrentBlocks;
		armorProgress = 1.0;
		//shieldArmorBarAll.setVisible(true);
		//shieldArmorBarAll.setProgress(1.0);
		//setBarProgress(1.0);
		//refreshShieldBarTitle();
		updateBar();
	}
	
	public void deactivateArmor() {
		if(isArmored) {
			playDeactivateSound();
		}
		isArmored = false;
		//shieldArmorBarAll.setProgress(0.0);
		//setBarProgress(0.0);
		armorCurrentBlocks = 0;
		armorTotalBlocks = 0;
		armorProgress = 0.0;
		//refreshShieldBarTitle();
		updateBar();
	}
	
	public void clearShieldsArmors() {
		deactivateShield();
		deactivateArmor();
	}
	
	public boolean damageArmor(int damage) {
		boolean result = false;
		if(isArmored) {
			armorCurrentBlocks -= damage;
			result = true;
			if(armorCurrentBlocks <= 0) {
				deactivateArmor();
			} else {
				armorProgress = (double)armorCurrentBlocks/armorTotalBlocks;
				//shieldArmorBarAll.setProgress(progress);
				//refreshShieldBarTitle();
				//setBarProgress(progress);
				updateBar();
			}
		}
		return result;
	}
	
	/*
	private void refreshShieldBarTitle() {
		ChatColor titleColor = ChatColor.AQUA;
		int remainingSeconds = getRemainingShieldTimeSeconds();
		String remainingTime = Konquest.getTimeFormat(remainingSeconds,titleColor);
		String armor = MessagePath.LABEL_ARMOR.getMessage();
		String shield = MessagePath.LABEL_SHIELD.getMessage();
		if(isShielded && isArmored) {
			shieldArmorBarAll.setTitle(titleColor+""+armorCurrentBlocks+" "+armor+" | "+shield+" "+remainingTime);
		} else if(isShielded) {
			shieldArmorBarAll.setTitle(titleColor+shield+" "+remainingTime);
		} else if(isArmored) {
			shieldArmorBarAll.setTitle(titleColor+""+armorCurrentBlocks+" "+armor);
		} else {
			shieldArmorBarAll.setProgress(0.0);
			shieldArmorBarAll.setVisible(false);
		}
	}
	*/
	
	public int getShieldEndTime() {
		return shieldEndTimeSeconds;
	}
	
	public int getArmorBlocks() {
		return armorCurrentBlocks;
	}
	
	public int getRemainingShieldTimeSeconds() {
		int result = 0;
		if(isShielded && shieldEndTimeSeconds > shieldNowTimeSeconds) {
			result = shieldEndTimeSeconds - shieldNowTimeSeconds;
		}
		return result;
	}
	
	private void playDeactivateSound() {
		getWorld().playSound(getCenterLoc(), Sound.BLOCK_GLASS_BREAK, (float)3.0, (float)0.3);
	}
	
	/*public boolean addPlot(Location loc) {
		boolean result = false;
		if(this.isLocInside(loc)) {
			Point p = Konquest.toPoint(loc);
			plots.put(p,new KonPlot(p));
			result = true;
		}
		return result;
	}*/
	
	public void putPlot(KonPlot plot) {
		if(plot == null) {
			ChatUtil.printDebug("Failed to set null plot!");
			return;
		}
		//ChatUtil.printDebug("Putting plot with "+plot.getPoints().size()+" points...");
		for(Point p : plot.getPoints()) {
			plots.put(p, plot);
			//ChatUtil.printDebug("  Put point "+p.x+","+p.y);
		}
		//ChatUtil.printDebug("Finished putting plot, plot size: "+plots.size());
	}
	
	public void removePlot(KonPlot plot) {
		if(plot == null) {
			ChatUtil.printDebug("Failed to remove null plot!");
			return;
		}
		//ChatUtil.printDebug("Removing plot with "+plot.getPoints().size()+" points...");
		for(Point p : plot.getPoints()) {
			plots.remove(p);
			/*if(plots.containsKey(p)) {
				plots.remove(p);
				//ChatUtil.printDebug("  Removed point "+p.x+","+p.y);
			}*/
		}
	}
	
	public boolean hasPlot(Location loc) {
		return hasPlot(Konquest.toPoint(loc), loc.getWorld());
	}
	
	public boolean hasPlot(Point p, World w) {
		boolean result = false;
		if(this.getChunkList().containsKey(p) && w.equals(getWorld())) {
			result = plots.containsKey(p);
		}
		return result;
	}
	
	// This can return null!
	public KonPlot getPlot(Location loc) {
		return getPlot(Konquest.toPoint(loc), loc.getWorld());
	}
	
	// This can return null!
	public KonPlot getPlot(Point p, World w) {
		KonPlot result = null;
		if(this.getChunkList().containsKey(p) && w.equals(getWorld())) {
			result = plots.get(p);
		}
		return result;
	}
	
	public List<KonPlot> getPlots() {
		ArrayList<KonPlot> result = new ArrayList<KonPlot>();
		for(KonPlot plot : plots.values()) {
			if(!result.contains(plot)) {
				result.add(plot);
			}
		}
		return result;
	}
	
	public void clearPlots() {
		plots.clear();
	}
	
}
