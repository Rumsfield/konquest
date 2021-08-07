package konquest;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import com.google.common.collect.MapMaker;

import konquest.manager.AccomplishmentManager;
import konquest.manager.ConfigManager;
import konquest.manager.DirectiveManager;
import konquest.manager.DisplayManager;
import konquest.manager.IntegrationManager;
import konquest.manager.KingdomManager;
import konquest.manager.LanguageManager;
import konquest.manager.LootManager;
import konquest.manager.PlayerManager;
import konquest.manager.RuinManager;
import konquest.manager.ShieldManager;
import konquest.manager.UpgradeManager;
import konquest.map.MapHandler;
import konquest.model.KonCamp;
import konquest.model.KonCapital;
import konquest.model.KonKingdom;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.model.KonRuin;
import konquest.model.KonTerritory;
import konquest.model.KonTerritoryType;
import konquest.model.KonTown;
import konquest.model.KonUpgrade;
import konquest.nms.TeamPacketSender;
import konquest.nms.TeamPacketSender_p754;
import konquest.nms.TeamPacketSender_p755;
import konquest.utility.ChatUtil;
import konquest.utility.Timeable;
import konquest.utility.Timer;
import konquest.command.CommandHandler;
import konquest.database.DatabaseThread;

public class Konquest implements Timeable {

	private KonquestPlugin plugin;
	private static Konquest instance;
	private static String chatTag;
	
	private DatabaseThread databaseThread;
	private AccomplishmentManager accomplishmentManager;
	private DirectiveManager directiveManager;
	private PlayerManager playerManager;
	private KingdomManager kingdomManager;
	private ConfigManager configManager;
	private IntegrationManager integrationManager;
	private LootManager lootManager;
	private CommandHandler commandHandler;
	private DisplayManager displayManager;
	private UpgradeManager upgradeManager;
	private ShieldManager shieldManager;
	private RuinManager ruinManager;
	private LanguageManager languageManager;
	private MapHandler mapHandler;
	
	private Scoreboard scoreboard;
    private Team friendlyTeam;
    private Team enemyTeam;
    private Team barbarianTeam;
    private TeamPacketSender teamPacketSender;
    private boolean isPacketSendEnabled;
	
	private EventPriority chatPriority;
    private List<World> worlds;
    private boolean isWhitelist;
	public List<String> opStatusMessages;
	private Timer saveTimer;
	private Timer compassTimer;
	private int saveIntervalSeconds;
	private long offlineTimeoutSeconds;
	public ConcurrentMap<Player, Location> lastPlaced = new MapMaker().
            weakKeys().
            weakValues().
            makeMap();
	private ConcurrentMap<UUID, ItemStack> headCache = new MapMaker().makeMap();
	private HashMap<Player,KonTerritory> teleportTerritoryQueue;
	private HashMap<Player,Location> teleportLocationQueue;
	
	public Konquest(KonquestPlugin plugin) {
		this.plugin = plugin;
		instance = this;
		chatTag = "§7[§6Konquest§7]§f ";
		
		databaseThread = new DatabaseThread(this);
		accomplishmentManager = new AccomplishmentManager(this);
		directiveManager = new DirectiveManager(this);
		playerManager = new PlayerManager(this);
		kingdomManager = new KingdomManager(this);
		configManager = new ConfigManager(this);
		integrationManager = new IntegrationManager(this);
		lootManager = new LootManager(this);
		commandHandler = new CommandHandler(this);
		displayManager = new DisplayManager(this);
		upgradeManager = new UpgradeManager(this);
		shieldManager = new ShieldManager(this);
		ruinManager = new RuinManager(this);
		languageManager = new LanguageManager(this);
		mapHandler = new MapHandler(this);
		
		chatPriority = EventPriority.LOW;
		worlds = new ArrayList<World>();
		isWhitelist = false;
		opStatusMessages = new ArrayList<String>();
		this.saveTimer = new Timer(this);
		this.compassTimer = new Timer(this);
		this.saveIntervalSeconds = 0;
		this.offlineTimeoutSeconds = 0;
		this.isPacketSendEnabled = false;
		
		teleportTerritoryQueue = new HashMap<Player,KonTerritory>();
		teleportLocationQueue = new HashMap<Player,Location>();
	}
	
	public void initialize() {
		// Initialize managers
		configManager.initialize();
		boolean debug = configManager.getConfig("core").getBoolean("core.debug");
		ChatUtil.printDebug("Debug is "+debug);
		String worldName = configManager.getConfig("core").getString("core.world_name");
		ChatUtil.printDebug("Primary world is "+worldName);
		String configTag = configManager.getConfig("core").getString("core.chat.tag");
		chatTag = ChatColor.translateAlternateColorCodes('&', configTag);
		languageManager.initialize();
		kingdomManager.initialize();
		ruinManager.initialize();
		initManagers();
		initWorlds();
		databaseThread.setSleepSeconds(saveIntervalSeconds);
		if(!databaseThread.isRunning()) {
			ChatUtil.printDebug("Starting database thread");
			databaseThread.getThread().start();
		} else {
			ChatUtil.printDebug("Database thread is already running");
		}
		
		// Create global scoreboard and teams
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        friendlyTeam = scoreboard.registerNewTeam("friendlies");
        friendlyTeam.setColor(ChatColor.GREEN);
        enemyTeam = scoreboard.registerNewTeam("enemies");
        enemyTeam.setColor(ChatColor.RED);
        barbarianTeam = scoreboard.registerNewTeam("barbarians");
        barbarianTeam.setColor(ChatColor.YELLOW);
        
        if(setupTeamPacketSender()) {
        	if(plugin.isProtocolEnabled()) {
        		ChatUtil.printConsoleAlert("Successfully registered name color packets for this server version");
        		isPacketSendEnabled = true;
        	} else {
        		ChatUtil.printConsoleError("Failed to register name color packets, ProtocolLib is disabled! Check version.");
        	}
        } else {
        	ChatUtil.printConsoleError("Failed to register name color packets, the server version is unsupported");
        }
		
		// Render Maps
		mapHandler.initialize();
		
		ChatUtil.printDebug("Finished Initialization");
	}
	
	public void reload() {
		configManager.reloadConfigs();
		initManagers();
		initWorlds();
		ChatUtil.printDebug("Finished Reload");
	}
	
	private void initManagers() {
		integrationManager.initialize();
		lootManager.initialize();
		displayManager.initialize();
		playerManager.initialize();
		accomplishmentManager.initialize();
		directiveManager.initialize();
		upgradeManager.initialize();
		shieldManager.initialize();
		offlineTimeoutSeconds = (long)(configManager.getConfig("core").getInt("core.kingdoms.offline_timeout_days",0)*86400);
		if(offlineTimeoutSeconds > 0 && offlineTimeoutSeconds < 86400) {
			offlineTimeoutSeconds = 86400;
			ChatUtil.printConsoleError("offline_timeout_seconds in core.yml is less than 1 day, overriding to 1 day to prevent data loss.");
		}
		saveIntervalSeconds = configManager.getConfig("core").getInt("core.save_interval",60)*60;
		ChatUtil.printConsoleAlert("Save interval is "+saveIntervalSeconds+" seconds");
		if(saveIntervalSeconds > 0) {
			saveTimer.stopTimer();
			saveTimer.setTime(saveIntervalSeconds);
			saveTimer.startLoopTimer();
		}
		compassTimer.stopTimer();
		compassTimer.setTime(30); // 30 second compass update interval
		compassTimer.startLoopTimer();
		// Set chat even priority
		chatPriority = getEventPriority(configManager.getConfig("core").getString("core.chat.priority","low"));
		// Update kingdom stuff
		kingdomManager.updateSmallestKingdom();
		kingdomManager.updateAllTownDisabledUpgrades();
		kingdomManager.updateKingdomOfflineProtection();
		
	}
	
	private void initWorlds() {
		List<String> worldNameList = configManager.getConfig("core").getStringList("core.world_blacklist");
		isWhitelist = configManager.getConfig("core").getBoolean("core.world_blacklist_reverse",false);
		// Verify listed worlds exist
		for(String name : worldNameList) {
			boolean matches = false;
			for(World world : Bukkit.getServer().getWorlds()) {
				if(world.getName().equals(name)) {
					matches = true;
					worlds.add(world);
					break;
				}
			}
			if(!matches) {
				ChatUtil.printConsoleError("core.world_blacklist name \""+name+"\" does not match any server worlds, check spelling and case.");
			}
		}
	}
	
	public void initOnlinePlayers() {
		// Fetch any players that happen to be in the server already (typically from /reload)
        for(Player bukkitPlayer : Bukkit.getServer().getOnlinePlayers()) {
			initPlayer(bukkitPlayer);
			ChatUtil.printConsole("Loaded online player "+bukkitPlayer.getName());
		}
	}
	
	public KonPlayer initPlayer(Player bukkitPlayer) {
		KonPlayer player = null;
		bukkitPlayer.setScoreboard(getScoreboard());
    	// Fetch player from the database
    	// Also instantiates player object in PlayerManager
		databaseThread.getDatabase().fetchPlayerData(bukkitPlayer);
		if(!playerManager.isPlayer(bukkitPlayer)) {
			ChatUtil.printDebug("Failed to init a non-existent player!");
			return null;
		}
    	player = playerManager.getPlayer(bukkitPlayer);
    	// Update all player's nametag color packets
    	updateNamePackets(player);
    	// Update offline protections
    	kingdomManager.updateKingdomOfflineProtection();
    	// Update player membership stats
    	kingdomManager.updatePlayerMembershipStats(player);
    	// Updates based on login position
    	Location loginLoc = bukkitPlayer.getLocation();
    	kingdomManager.clearTownHearts(player);
    	if(kingdomManager.isChunkClaimed(loginLoc)) {
			KonTerritory loginTerritory = kingdomManager.getChunkTerritory(loginLoc);
    		if(loginTerritory.getTerritoryType().equals(KonTerritoryType.TOWN)) { 
	    		// Player joined located within a Town
	    		KonTown town = (KonTown) loginTerritory;
	    		town.addBarPlayer(player);
	    		// For enemy players, apply effects
	    		if(!player.getKingdom().equals(town.getKingdom())) {
	    			kingdomManager.applyTownNerf(player, town);
	    			kingdomManager.clearTownHearts(player);
	    		} else {
	    			kingdomManager.clearTownNerf(player);
	    			kingdomManager.applyTownHearts(player, town);
	    		}
    		} else if(loginTerritory.getTerritoryType().equals(KonTerritoryType.RUIN)) {
    			// Player joined located within a Ruin
    			KonRuin ruin = (KonRuin) loginTerritory;
    			ruin.addBarPlayer(player);
    			ruin.spawnAllGolems();
    		} else if(loginTerritory.getTerritoryType().equals(KonTerritoryType.CAPITAL)) {
    			// Player joined located within a Capital
    			KonCapital capital = (KonCapital) loginTerritory;
    			capital.addBarPlayer(player);
    		} else if(loginTerritory.getTerritoryType().equals(KonTerritoryType.CAMP)) {
    			// Player joined located within a Camp
    			KonCamp camp = (KonCamp) loginTerritory;
    			camp.addBarPlayer(player);
    		}
		} else {
			// Player joined located outside of a Town
			kingdomManager.clearTownNerf(player);
		}
    	kingdomManager.updatePlayerBorderParticles(player,bukkitPlayer.getLocation());
    	ChatUtil.resetTitle(bukkitPlayer);
		return player;
	}
	
	public static Konquest getInstance() {
		return instance;
	}
	
	public KonquestPlugin getPlugin() {
		return plugin;
	}
	
	public AccomplishmentManager getAccomplishmentManager() {
		return accomplishmentManager;
	}
	
	public DirectiveManager getDirectiveManager() {
		return directiveManager;
	}
	
	public PlayerManager getPlayerManager() {
		return playerManager;
	}
	
	public KingdomManager getKingdomManager() {
		return kingdomManager;
	}
	
	public ConfigManager getConfigManager() {
		return configManager;
	}
	
	public IntegrationManager getIntegrationManager() {
		return integrationManager;
	}
	
	public LootManager getLootManager() {
		return lootManager;
	}
	
	public CommandHandler getCommandHandler() {
		return commandHandler;
	}
	
	/*public String getWorldName() {
		return worldName;
	}*/
	
	public Scoreboard getScoreboard() {
		return scoreboard;
	}
	
	public DatabaseThread getDatabaseThread() {
		return databaseThread;
	}
	
	public DisplayManager getDisplayManager() {
		return displayManager;
	}
	
	public UpgradeManager getUpgradeManager() {
		return upgradeManager;
	}
	
	public ShieldManager getShieldManager() {
		return shieldManager;
	}
	
	public RuinManager getRuinManager() {
		return ruinManager;
	}
	
	public LanguageManager lang() {
		return languageManager;
	}
	
	public MapHandler getMapHandler() {
		return mapHandler;
	}
	
	public long getOfflineTimeoutSeconds() {
		return offlineTimeoutSeconds;
	}
	
	public EventPriority getChatPriority() {
		return chatPriority;
	}
	
	public boolean isWorldValid(World world) {
		boolean result = false;
		if(isWhitelist) {
			result = worlds.contains(world);
		} else {
			result = !worlds.contains(world);
		}
		return result;
	}
	
	@Override
	public void onEndTimer(int taskID) {
		if(taskID == 0) {
			ChatUtil.printDebug("Save Timer ended with null taskID!");
		} else if(taskID == saveTimer.getTaskID()) {
			// Prune residents for being offline too long
			if(offlineTimeoutSeconds != 0) {
				// Search all stored players and prune
				Date now = new Date();
				for(KonOfflinePlayer player : playerManager.getAllKonOfflinePlayers()) {
					long lastPlayedTime = player.getOfflineBukkitPlayer().getLastPlayed();
					if(lastPlayedTime > 0 && now.after(new Date(lastPlayedTime + (offlineTimeoutSeconds*1000)))) {
						// Offline player has exceeded timeout period, prune from residencies
						for(KonTown town : player.getKingdom().getTowns()) {
							if(town.getPlayerResidents().contains(player.getOfflineBukkitPlayer())) {
								boolean status = town.removePlayerResident(player.getOfflineBukkitPlayer());
								ChatUtil.printDebug("Pruned player "+player.getOfflineBukkitPlayer().getName()+" from town "+town.getName()+" in kingdom "+player.getKingdom().getName()+", got "+status);
							}
						}
					}
				}
			}
			// Save config files
			kingdomManager.saveKingdoms();
			kingdomManager.saveCamps();
			ruinManager.saveRuins();
			//playerManager.saveAllPlayers();
			configManager.saveConfigs();
			saveTimer.setTime(saveIntervalSeconds);
			ChatUtil.sendAdminBroadcast("Saved all config files");
		} else if(taskID == compassTimer.getTaskID()) {
			// Update compass target for all players with permission and compass in inventory
			for(KonPlayer player : playerManager.getPlayersOnline()) {
				if(player.getBukkitPlayer().hasPermission("konquest.compass") && 
						isWorldValid(player.getBukkitPlayer().getWorld()) &&
						player.getBukkitPlayer().getInventory().contains(Material.COMPASS)) {
					// Find nearest enemy town
	    			ArrayList<KonKingdom> enemyKingdoms = kingdomManager.getKingdoms();
	    			if(!enemyKingdoms.isEmpty()) {
	    				enemyKingdoms.remove(player.getKingdom());
	    			}
	    			KonTerritory nearestTerritory = null;
	    			int minDistance = Integer.MAX_VALUE;
	    			for(KonKingdom kingdom : enemyKingdoms) {
	    				for(KonTown town : kingdom.getTowns()) {
	    					// Only find enemy towns which do not have the counter-intelligence upgrade level 2+
	    					int upgradeLevel = upgradeManager.getTownUpgradeLevel(town, KonUpgrade.COUNTER);
	    					if(upgradeLevel < 2) {
	    						int townDist = chunkDistance(player.getBukkitPlayer().getLocation(), town.getCenterLoc());
	    						if(townDist != -1 && townDist < minDistance) {
	    							minDistance = townDist;
	    							nearestTerritory = town;
	    						}
	    					}
	    				}
	    			}
	    			if(nearestTerritory != null) {
	    				Location nearestEnemyTownLoc = nearestTerritory.getCenterLoc();
	    				player.getBukkitPlayer().setCompassTarget(nearestEnemyTownLoc);
	    			}
	        	}
			}
		}
	}
	
	// Helper methods
	/**
	 * Gets chunks around loc, (2r-1)^2 chunks squared
	 * @param loc
	 * @param radius
	 * @return (2r-1)^2 chunks squared
	 */
	public ArrayList<Chunk> getAreaChunks(Location loc, int radius) {
		ArrayList<Chunk> areaChunks = new ArrayList<Chunk>();
		areaChunks.add(loc.getChunk());
		int curX = loc.getChunk().getX();
		int curZ = loc.getChunk().getZ();
		if(radius > 0) {
			int min = (radius-1)*-1;
			int max = (radius-1);
			for(int x=min;x<=max;x++) {
				for(int z=min;z<=max;z++) {
					if(x != 0 || z != 0) {
						areaChunks.add(loc.getWorld().getChunkAt(curX+x, curZ+z));
					}
				}
			}
		}
		//ChatUtil.printDebug("Got chunks: "+Arrays.toString(areaChunks.toArray()));
		return areaChunks;
	}

	public ArrayList<Point> getAreaPoints(Location loc, int radius) {
		ArrayList<Point> areaPoints = new ArrayList<Point>();
		Point center = toPoint(loc);
		areaPoints.add(center);
		if(radius > 0) {
			int min = (radius-1)*-1;
			int max = (radius-1);
			for(int x=min;x<=max;x++) {
				for(int z=min;z<=max;z++) {
					if(x != 0 || z != 0) {
						areaPoints.add(new Point(center.x + x, center.y + z));
					}
				}
			}
		}
		return areaPoints;
	}
	
	/**
	 * Gets chunks surrounding loc, (2r-1)^2-1 chunks squared
	 * @param loc
	 * @param radius
	 * @return (2r-1)^2 chunks squared
	 */
	public ArrayList<Chunk> getSurroundingChunks(Location loc, int radius) {
		ArrayList<Chunk> areaChunks = new ArrayList<Chunk>();
		int curX = loc.getChunk().getX();
		int curZ = loc.getChunk().getZ();
		if(radius > 0) {
			int min = (radius-1)*-1;
			int max = (radius-1);
			for(int x=min;x<=max;x++) {
				for(int z=min;z<=max;z++) {
					if(x != 0 || z != 0) {
						areaChunks.add(loc.getWorld().getChunkAt(curX+x, curZ+z));
					}
				}
			}
		}
		return areaChunks;
	}
	
	public ArrayList<Point> getSurroundingPoints(Location loc, int radius) {
		ArrayList<Point> areaPoints = new ArrayList<Point>();
		Point center = toPoint(loc);
		if(radius > 0) {
			int min = (radius-1)*-1;
			int max = (radius-1);
			for(int x=min;x<=max;x++) {
				for(int z=min;z<=max;z++) {
					if(x != 0 || z != 0) {
						areaPoints.add(new Point(center.x + x, center.y + z));
					}
				}
			}
		}
		return areaPoints;
	}
	
	public ArrayList<Chunk> getSideChunks(Chunk chunk) {
		ArrayList<Chunk> sideChunks = new ArrayList<Chunk>();
		int[] coordLUTX = {0,1,0,-1};
		int[] coordLUTZ = {1,0,-1,0};
		int curX = chunk.getX();
		int curZ = chunk.getZ();
		for(int i = 0;i<4;i++) {
			sideChunks.add(chunk.getWorld().getChunkAt(curX+coordLUTX[i], curZ+coordLUTZ[i]));
		}
		return sideChunks;
	}
	
	public ArrayList<Chunk> getSideChunks(Location loc) {
		ArrayList<Chunk> sideChunks = new ArrayList<Chunk>();
		int[] coordLUTX = {0,1,0,-1};
		int[] coordLUTZ = {1,0,-1,0};
		int curX = loc.getChunk().getX();
		int curZ = loc.getChunk().getZ();
		for(int i = 0;i<4;i++) {
			sideChunks.add(loc.getWorld().getChunkAt(curX+coordLUTX[i], curZ+coordLUTZ[i]));
		}
		return sideChunks;
	}
	
	public ArrayList<Point> getSidePoints(Location loc) {
		ArrayList<Point> sidePoints = new ArrayList<Point>();
		Point center = toPoint(loc);
		int[] coordLUTX = {0,1,0,-1};
		int[] coordLUTZ = {1,0,-1,0};
		for(int i = 0;i<4;i++) {
			sidePoints.add(new Point(center.x + coordLUTX[i], center.y + coordLUTZ[i]));
		}
		return sidePoints;
	}
	
	public Point toPoint(Location loc) {
		return new Point((int)Math.floor((double)loc.getBlockX()/16),(int)Math.floor((double)loc.getBlockZ()/16));
	}
	
	public Point toPoint(Chunk chunk) {
		return new Point(chunk.getX(),chunk.getZ());
	}
	
	public Chunk toChunk(Point point, World world) {
		return world.getChunkAt(point.x, point.y);
	}
	
	/*
	public static int distanceInChunks(Location loc1, Location loc2) {
		return distanceInChunks(loc1.getChunk(), loc2.getChunk());
	}
	
	public static int distanceInChunks(Chunk chunk1, Chunk chunk2) {
		if(chunk1.getWorld().getName().equals(chunk2.getWorld().getName())) {
			return Math.max(Math.abs(chunk1.getX() - chunk2.getX()), Math.abs(chunk1.getZ() - chunk2.getZ()));
		} else {
			return -1;
		}
	}
	*/
	
	public static int chunkDistance(Location loc1, Location loc2) {
		if(loc1.getWorld().getName().equals(loc2.getWorld().getName())) {
			int loc1X = (int)Math.floor((double)loc1.getBlockX()/16);
			int loc1Z = (int)Math.floor((double)loc1.getBlockZ()/16);
			int loc2X = (int)Math.floor((double)loc2.getBlockX()/16);
			int loc2Z = (int)Math.floor((double)loc2.getBlockZ()/16);
			return Math.max(Math.abs(loc1X - loc2X), Math.abs(loc1Z - loc2Z));
		} else {
			return -1;
		}
	}
	
	public String formatPointsToString(Collection<Point> points) {
		String result = "";
        for(Point point : points) {
        	int x = (int)point.getX();
        	int y = (int)point.getY();
        	result = result+x+","+y+".";
        }
        return result;
	}
	
	public ArrayList<Point> formatStringToPoints(String coords) {
		ArrayList<Point> points = new ArrayList<Point>();
		String[] coord_list = coords.split("\\.");
		//ChatUtil.printDebug("Split coords: "+Arrays.toString(coord_list));
		for(String coord : coord_list) {
			if(!coord.equals("")) {
				//ChatUtil.printDebug("Parsing chunk coord: "+coord);
				String[] coord_pair = coord.split(",");
				int x = Integer.parseInt(coord_pair[0]);
				int z = Integer.parseInt(coord_pair[1]);
				//ChatUtil.printDebug("Got chunk coord: "+x+","+z);
				points.add(new Point(x,z));
			}
		}
		//ChatUtil.printDebug("Chunk coords: "+Arrays.toString(points.toArray()));
		return points;
	}
	
	public String formatLocationsToString(Collection<Location> locs) {
		String result = "";
        for(Location loc : locs) {
        	int x = loc.getBlockX();
        	int y = loc.getBlockY();
        	int z = loc.getBlockZ();
        	result = result+x+","+y+","+z+".";
        }
        return result;
	}
	
	public ArrayList<Location> formatStringToLocations(String coords, World world) {
		ArrayList<Location> locations = new ArrayList<Location>();
		String[] coord_list = coords.split("\\.");
		//ChatUtil.printDebug("Split coords: "+Arrays.toString(coord_list));
		for(String coord : coord_list) {
			if(!coord.equals("")) {
				//ChatUtil.printDebug("Parsing chunk coord: "+coord);
				String[] coord_pair = coord.split(",");
				int x = Integer.parseInt(coord_pair[0]);
				int y = Integer.parseInt(coord_pair[1]);
				int z = Integer.parseInt(coord_pair[2]);
				//ChatUtil.printDebug("Got chunk coord: "+x+","+z);
				// Add location in primary world by default
				locations.add(new Location(world,x,y,z));
			}
		}
		//ChatUtil.printDebug("Chunk coords: "+Arrays.toString(points.toArray()));
		return locations;
	}
	
	// This can return null!
	public Location getRandomWildLocation(int worldSize, World world) {
		Location wildLoc = null;
		ChatUtil.printDebug("Generating random wilderness location for size "+worldSize);
		
		int randomNumX = 0;
		int randomNumZ = 0;
		int randomNumY = 0;
		boolean foundValidLoc = false;
		int timeout = 0;
		while(!foundValidLoc) {
			randomNumX = ThreadLocalRandom.current().nextInt(-1*(worldSize/2), (worldSize/2) + 1);
			randomNumZ = ThreadLocalRandom.current().nextInt(-1*(worldSize/2), (worldSize/2) + 1);
			randomNumY = world.getHighestBlockYAt(randomNumX,randomNumZ) + 3;
			wildLoc = new Location(world, randomNumX, randomNumY, randomNumZ);
			if(!kingdomManager.isChunkClaimed(wildLoc)) {
				foundValidLoc = true;
			} else {
				timeout++;
				ChatUtil.printDebug("Got claimed location, trying again...");
			}
			if(timeout > 100) {
				ChatUtil.printDebug("There was a problem getting a random wilderness location: timeout");
				return null;
			}
		}
		ChatUtil.printDebug("Got wilderness location "+wildLoc.getX()+","+wildLoc.getY()+","+wildLoc.getZ());
		return wildLoc;
	}
	
	/**
	 * Gets a random location in a square chunk area, excluding the center chunk
	 * @param center
	 * @param radius
	 * @return
	 */
	public Location getSafeRandomCenteredLocation(Location center, int radius) {
		Location randLoc = null;
		ChatUtil.printDebug("Generating random centered location for radius "+radius);
		int randomChunkIdx = 0;
		int randomNumX = 0;
		int randomNumZ = 0;
		int randomNumY = 0;
		boolean foundValidLoc = false;
		int timeout = 0;
		while(!foundValidLoc) {
			ArrayList<Chunk> chunkList = getSurroundingChunks(center, radius);
			randomChunkIdx = ThreadLocalRandom.current().nextInt(0, chunkList.size());
			randomNumX = ThreadLocalRandom.current().nextInt(0, 16);
			randomNumZ = ThreadLocalRandom.current().nextInt(0, 16);
			randomNumY = chunkList.get(randomChunkIdx).getChunkSnapshot(true,false,false).getHighestBlockYAt(randomNumX, randomNumZ);
			Block randBlock = chunkList.get(randomChunkIdx).getBlock(randomNumX, randomNumY, randomNumZ);
			Block randBlockDown = chunkList.get(randomChunkIdx).getBlock(randomNumX, randomNumY-1, randomNumZ);
			randLoc = randBlock.getLocation();
			randLoc.add(0.5,2,0.5);
			ChatUtil.printDebug("Checking block material target: "+randBlock.getType().toString());
			ChatUtil.printDebug("Checking block material down: "+randBlockDown.getType().toString());
			if(!randBlockDown.getType().equals(Material.LAVA)) {
				foundValidLoc = true;
			} else {
				timeout++;
				ChatUtil.printDebug("Got dangerous location, trying again...");
			}
			if(timeout > 100) {
				ChatUtil.printDebug("There was a problem getting a safe centered location: timeout");
				return null;
			}
		}
		ChatUtil.printDebug("Got safe centered location "+randLoc.getX()+","+randLoc.getY()+","+randLoc.getZ());
		double x0,x1,z0,z1;
		x0 = randLoc.getX();
		x1 = center.getX();
		z0 = randLoc.getZ();
		z1 = center.getZ();
		float yaw = (float)(180-(Math.atan2((x0-x1),(z0-z1))*180/Math.PI));
		randLoc.setYaw(yaw);
		return randLoc;
	}
	
    private boolean setupTeamPacketSender() {
    	String version;
    	try {
    		version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    	} catch (ArrayIndexOutOfBoundsException e) {
    		ChatUtil.printConsoleError("Failed to determine server version.");
    		return false;
    	}
    	ChatUtil.printConsoleAlert("Your server version is "+version);
    	if(version.equals("v1_16_R3")) {
    		teamPacketSender = new TeamPacketSender_p754();
    	} else if(version.equals("v1_17_R1")) {
    		teamPacketSender = new TeamPacketSender_p755();
    	}
    	return teamPacketSender != null;
    }
    
    /**
     * Sends updated team packets for the given player
     * @param player
     */
    public void updateNamePackets(KonPlayer player) {
    	if(!isPacketSendEnabled) {
    		return;
    	}
    	// Loop over all online players, populate team lists and send each online player a team packet for arg player
    	// Send arg player packets for each team with lists of online players
		List<String> friendlyNames = new ArrayList<String>();
		List<String> enemyNames = new ArrayList<String>();
		List<String> barbarianNames = new ArrayList<String>();
    	for(KonPlayer onlinePlayer : playerManager.getPlayersOnline()) {
    		// Place online player is appropriate list w.r.t. player
    		if(onlinePlayer.isBarbarian()) {
    			barbarianNames.add(onlinePlayer.getBukkitPlayer().getName());
    		} else {
    			if(onlinePlayer.getKingdom().equals(player.getKingdom())) {
    				friendlyNames.add(onlinePlayer.getBukkitPlayer().getName());
    			} else {
    				enemyNames.add(onlinePlayer.getBukkitPlayer().getName());
    			}
    		}
    		// Send appropriate team packet to online player
    		if(player.isBarbarian()) {
    			teamPacketSender.sendPlayerTeamPacket(onlinePlayer.getBukkitPlayer(), Arrays.asList(player.getBukkitPlayer().getName()), barbarianTeam);
    		} else {
    			if(player.getKingdom().equals(onlinePlayer.getKingdom())) {
    				teamPacketSender.sendPlayerTeamPacket(onlinePlayer.getBukkitPlayer(), Arrays.asList(player.getBukkitPlayer().getName()), friendlyTeam);
    			} else {
    				teamPacketSender.sendPlayerTeamPacket(onlinePlayer.getBukkitPlayer(), Arrays.asList(player.getBukkitPlayer().getName()), enemyTeam);
    			}
    		}
    	}
    	// Send packets to player
    	if(!friendlyNames.isEmpty()) {
			teamPacketSender.sendPlayerTeamPacket(player.getBukkitPlayer(), friendlyNames, friendlyTeam);
    	}
    	if(!enemyNames.isEmpty()) {
    		teamPacketSender.sendPlayerTeamPacket(player.getBukkitPlayer(), enemyNames, enemyTeam);
    	}
    	if(!barbarianNames.isEmpty()) {
    		teamPacketSender.sendPlayerTeamPacket(player.getBukkitPlayer(), barbarianNames, barbarianTeam);
    	}
    }
    
    /*
    //TODO This could be optimized to reduce loop Order, and only update as needed
    public void updateNamePackets() {
    	if(teamPacketSender != null) {
	    	// Update all Kingdom player's nametag color packets
			for(KonKingdom kingdom : kingdomManager.getKingdoms()) {
				// For each kingdom, determine friendlies and enemies
				List<Player> friendlyPlayers = new ArrayList<Player>();
				List<String> friendlyNames = new ArrayList<String>();
				List<String> enemyNames = new ArrayList<String>();
				List<String> barbarianNames = new ArrayList<String>();
				// Populate friendly and enemy lists
				for(KonPlayer player : playerManager.getPlayersOnline()) {
		    		if(player.getKingdom().equals(kingdom)) {
		    			friendlyNames.add(player.getBukkitPlayer().getName());
		    			friendlyPlayers.add(player.getBukkitPlayer());
		    		} else if(!player.isBarbarian()) {
		    			enemyNames.add(player.getBukkitPlayer().getName());
		    		} else {
		    			barbarianNames.add(player.getBukkitPlayer().getName());
		    		}
		    	}
				// For each friendly player in this kingdom, send packet update
				for(Player kingdomPlayer : friendlyPlayers) {
					if(!friendlyNames.isEmpty()) {
						teamPacketSender.sendPlayerTeamPacket(kingdomPlayer, friendlyNames, friendlyTeam);
			    	}
			    	if(!enemyNames.isEmpty()) {
			    		teamPacketSender.sendPlayerTeamPacket(kingdomPlayer, enemyNames, enemyTeam);
			    	}
			    	if(!barbarianNames.isEmpty()) {
			    		teamPacketSender.sendPlayerTeamPacket(kingdomPlayer, barbarianNames, barbarianTeam);
			    	}
				}
			}
			// Update all Barbarian player's nametag color packets
			List<Player> barbarianPlayers = new ArrayList<Player>();
			List<String> enemyNames = new ArrayList<String>();
			List<String> barbarianNames = new ArrayList<String>();
			// Populate barbarian and enemy lists
			for(KonPlayer player : playerManager.getPlayersOnline()) {
	    		if(player.isBarbarian()) {
	    			barbarianNames.add(player.getBukkitPlayer().getName());
	    			barbarianPlayers.add(player.getBukkitPlayer());
	    		} else {
	    			enemyNames.add(player.getBukkitPlayer().getName());
	    		}
	    	}
			// For each barbarian player, send packet update
			for(Player barbarianPlayer : barbarianPlayers) {
		    	if(!enemyNames.isEmpty()) {
		    		teamPacketSender.sendPlayerTeamPacket(barbarianPlayer, enemyNames, enemyTeam);
		    	}
		    	if(!barbarianNames.isEmpty()) {
		    		teamPacketSender.sendPlayerTeamPacket(barbarianPlayer, barbarianNames, barbarianTeam);
		    	}
			}
    	}
    }
    */
    
    public static UUID idFromString(String id) {
    	UUID result = null;
    	try {
    		result = UUID.fromString(id);
    	} catch(IllegalArgumentException e) {
    		e.printStackTrace();
    	}
    	return result;
    }
    
    public void telePlayerTerritory(Player player, KonTerritory travelTerritory) {
    	Point locPoint = toPoint(travelTerritory.getCenterLoc());
    	//Location destination = new Location(loc.getWorld(),loc.getX()+0.5,loc.getY()+1.0,loc.getZ()+0.5);
    	if(travelTerritory.getWorld().isChunkLoaded(locPoint.x,locPoint.y)) {
    	//if(loc.getChunk().isLoaded()) {
    		//player.teleport(new Location(loc.getWorld(),loc.getX(),loc.getY()+1.0,loc.getZ()));
    		ChatUtil.printDebug("Teleporting player "+player.getName()+" to loaded territory");
    		Location qLoc = travelTerritory.getSpawnLoc();
    		Location destination = new Location(qLoc.getWorld(),qLoc.getX()+0.5,qLoc.getY()+1.0,qLoc.getZ()+0.5);
    		player.teleport(destination,TeleportCause.PLUGIN);
    		/*new BukkitRunnable() {
				public void run() {
					player.teleport(destination,TeleportCause.PLUGIN);
				}
			}.runTaskLater(getPlugin(), 2L);*/
    	} else {
    		teleportTerritoryQueue.put(player,travelTerritory);
    		ChatUtil.printDebug("Queueing player "+player.getName()+" for unloaded territory destination");
    		travelTerritory.getWorld().loadChunk(locPoint.x,locPoint.y);
    	}
    }
    
    public void telePlayerLocation(Player player, Location travelLocation) {
    	Point locPoint = toPoint(travelLocation);
    	//Location destination = new Location(loc.getWorld(),loc.getX()+0.5,loc.getY()+1.0,loc.getZ()+0.5);
    	if(travelLocation.getWorld().isChunkLoaded(locPoint.x,locPoint.y)) {
    	//if(loc.getChunk().isLoaded()) {
    		//player.teleport(new Location(loc.getWorld(),loc.getX(),loc.getY()+1.0,loc.getZ()));
    		ChatUtil.printDebug("Teleporting player "+player.getName()+" to loaded chunk");
    		Location qLoc = travelLocation;
    		Location destination = new Location(qLoc.getWorld(),qLoc.getX()+0.5,qLoc.getY()+1.0,qLoc.getZ()+0.5);
    		player.teleport(destination,TeleportCause.PLUGIN);
    		/*new BukkitRunnable() {
				public void run() {
					player.teleport(destination,TeleportCause.PLUGIN);
				}
			}.runTaskLater(getPlugin(), 2L);*/
    	} else {
    		teleportLocationQueue.put(player,travelLocation);
    		ChatUtil.printDebug("Queueing player "+player.getName()+" for unloaded chunk destination");
    		travelLocation.getWorld().loadChunk(locPoint.x,locPoint.y);
    	}
    }

    public void applyQueuedTeleports(Chunk chunk) {
    	Point cPoint = toPoint(chunk);
    	Point qPoint;
		Location qLoc;
    	if(!teleportTerritoryQueue.isEmpty()) {
	    	for(Player qPlayer : teleportTerritoryQueue.keySet()) {
	    		qLoc = teleportTerritoryQueue.get(qPlayer).getSpawnLoc();
	    		qPoint = toPoint(qLoc);
	    		if(qPoint.equals(cPoint) && chunk.getWorld().equals(qLoc.getWorld())) {
	    			Location destination = new Location(qLoc.getWorld(),qLoc.getX()+0.5,qLoc.getY()+1.0,qLoc.getZ()+0.5);
	    			qPlayer.teleport(destination,TeleportCause.PLUGIN);
	    			/*new BukkitRunnable() {
	    				public void run() {
	    					qPlayer.teleport(qLoc,TeleportCause.PLUGIN);
	    				}
	    			}.runTaskLater(getPlugin(), 2L);*/
	    			ChatUtil.printDebug("Teleporting territory queued player "+qPlayer.getName());
	    			teleportTerritoryQueue.remove(qPlayer);
	    		}
	    	}
    	}
    	if(!teleportLocationQueue.isEmpty()) {
	    	for(Player qPlayer : teleportLocationQueue.keySet()) {
	    		qLoc = teleportLocationQueue.get(qPlayer);
	    		qPoint = toPoint(qLoc);
	    		if(qPoint.equals(cPoint) && chunk.getWorld().equals(qLoc.getWorld())) {
	    			Location destination = new Location(qLoc.getWorld(),qLoc.getX()+0.5,qLoc.getY()+1.0,qLoc.getZ()+0.5);
	    			qPlayer.teleport(destination,TeleportCause.PLUGIN);
	    			/*new BukkitRunnable() {
	    				public void run() {
	    					qPlayer.teleport(qLoc,TeleportCause.PLUGIN);
	    				}
	    			}.runTaskLater(getPlugin(), 2L);*/
	    			ChatUtil.printDebug("Teleporting chunk queued player "+qPlayer.getName());
	    			teleportLocationQueue.remove(qPlayer);
	    		}
	    	}
    	}
    }
    
    public static List<String> stringPaginate(String sentence) {
    	ArrayList<String> result = new ArrayList<String>();
    	String[] words = sentence.split(" ");
    	String line = "";
    	// create lines no more than 30 characters (including spaces) long
    	for(int i=0;i<words.length;i++) {
    		String test = line + words[i];
    		if(i == words.length-1) {
    			if(test.length() > 30) {
        			result.add(line.trim());
        			result.add(words[i].trim());
        		} else {
        			result.add(test.trim());
        		}
    		} else {
    			if(test.length() > 30) {
        			result.add(line.trim());
        			line = words[i] + " ";
        		} else {
        			line = line + words[i] + " ";
        		}
    		}
    	}
    	return result;
    }
    
    public ItemStack getPlayerHead(OfflinePlayer bukkitOfflinePlayer) {
    	if(bukkitOfflinePlayer.getUniqueId() != null && !headCache.containsKey(bukkitOfflinePlayer.getUniqueId())) {
    		ChatUtil.printDebug("Missing "+bukkitOfflinePlayer.getName()+" player head in the cache, creating...");
    		ItemStack item = new ItemStack(Material.PLAYER_HEAD);
    		SkullMeta meta = (SkullMeta)item.getItemMeta();
        	meta.setOwningPlayer(bukkitOfflinePlayer);
    		item.setItemMeta(meta);
    		headCache.put(bukkitOfflinePlayer.getUniqueId(),item);
    		/*
    		Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), new Runnable() {
                @Override
                public void run() {
                	
                	SkullMeta meta = (SkullMeta)item.getItemMeta();
                	meta.setOwningPlayer(bukkitOfflinePlayer);
            		item.setItemMeta(meta);
            		
            		Bukkit.getScheduler().runTask(getPlugin(), new Runnable() {
                        @Override
                        public void run() {
                        	headCache.put(bukkitOfflinePlayer.getUniqueId(),item);
                        }
            		});
            		
                }
            });
			*/
    		return item;
    	} else {
    		return headCache.get(bukkitOfflinePlayer.getUniqueId());
    	}
    }
    
    public static ChatColor getContextColor(KonOfflinePlayer observer, KonOfflinePlayer target) {
    	ChatColor result = ChatColor.RED;
    	if(target.isBarbarian()) {
    		result = ChatColor.YELLOW;
    	} else {
    		if(target.getKingdom().equals(observer.getKingdom())) {
    			result = ChatColor.GREEN;
    		}
    	}
    	return result;
    }
    
    public static void playSuccessSound(Player bukkitPlayer) {
    	Bukkit.getScheduler().scheduleSyncDelayedTask(instance.getPlugin(), new Runnable() {
            @Override
            public void run() {
            	bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, (float)1.0, (float)1.3);
            }
        },1);
    	Bukkit.getScheduler().scheduleSyncDelayedTask(instance.getPlugin(), new Runnable() {
            @Override
            public void run() {
            	bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, (float)1.0, (float)1.7);
            }
        },4);
    }
    
    public static void playTownArmorSound(Player bukkitPlayer) {
    	playTownArmorSound(bukkitPlayer.getLocation());
    }
    
    public static void playTownArmorSound(Location loc) {
    	loc.getWorld().playSound(loc, Sound.ENTITY_SHULKER_SHOOT, (float)1.0, (float)2);
    }
    
    public static String getTimeFormat(int valSeconds, ChatColor color) {
		int days = valSeconds / 86400;
		int hours = valSeconds % 86400 / 3600;
		int minutes = valSeconds % 3600 / 60;
		int seconds = valSeconds % 60;
		
		ChatColor nColor = ChatColor.GRAY;
		ChatColor numColor = color;
		if(valSeconds <= 30) {
			numColor = ChatColor.DARK_RED;
		}
		String result = "";
		String format = "";
		
		if(days != 0) {
			format = numColor+"%03d"+nColor+"D:"+numColor+"%02d"+nColor+"H:"+numColor+"%02d"+nColor+"M:"+numColor+"%02d"+nColor+"S";
			result = String.format(format, days, hours, minutes, seconds);
		} else if(hours != 0) {
			format = numColor+"%02d"+nColor+"H:"+numColor+"%02d"+nColor+"M:"+numColor+"%02d"+nColor+"S";
			result = String.format(format, hours, minutes, seconds);
		} else if(minutes != 0) {
			format = numColor+"%02d"+nColor+"M:"+numColor+"%02d"+nColor+"S";
			result = String.format(format, minutes, seconds);
		} else {
			format = numColor+"%02d"+nColor+"S";
			result = String.format(format, seconds);
		}
		
		return result;		
	}
    
    public static EventPriority getEventPriority(String priority) {
    	EventPriority result = EventPriority.LOW;
    	if(priority != null) {
    		try {
    			result = EventPriority.valueOf(priority);
    		} catch(IllegalArgumentException e) {
    			// do nothing
    		}
    	}
    	return result;
    }
    
    public static String getChatTag() {
    	return chatTag;
    }
	
}
