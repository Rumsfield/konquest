package konquest;

import java.awt.Point;
import java.util.ArrayList;
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
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitRunnable;
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
import konquest.manager.UpgradeManager;
import konquest.model.KonKingdom;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.model.KonRuin;
import konquest.model.KonTerritory;
import konquest.model.KonTerritoryType;
import konquest.model.KonTown;
import konquest.model.KonUpgrade;
import konquest.nms.TeamPacketSender;
import konquest.nms.TeamPacketSender_1_16_R3;
import konquest.utility.ChatUtil;
import konquest.utility.Timeable;
import konquest.utility.Timer;
import konquest.command.CommandHandler;
import konquest.database.DatabaseThread;

public class Konquest implements Timeable {

	private KonquestPlugin plugin;
	private static Konquest instance;
	
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
	private RuinManager ruinManager;
	private LanguageManager languageManager;
	
	private Scoreboard scoreboard;
    private Team friendlyTeam;
    private Team enemyTeam;
    private Team barbarianTeam;
    private TeamPacketSender teamPacketSender;
	
	private String worldName;
	public List<String> opStatusMessages;
	private Timer saveTimer;
	private Timer compassTimer;
	private int saveIntervalSeconds;
	private long offlineTimeoutSeconds;
	public ConcurrentMap<Player, Location> lastPlaced = new MapMaker().
            weakKeys().
            weakValues().
            makeMap();
	
	private HashMap<Location,Player> teleportQueue;
	
	public Konquest(KonquestPlugin plugin) {
		this.plugin = plugin;
		instance = this;
		
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
		ruinManager = new RuinManager(this);
		languageManager = new LanguageManager(this);
		
		worldName = "world";
		opStatusMessages = new ArrayList<String>();
		this.saveTimer = new Timer(this);
		this.compassTimer = new Timer(this);
		this.saveIntervalSeconds = 0;
		this.offlineTimeoutSeconds = 0;
		
		teleportQueue = new HashMap<Location,Player>();
	}
	
	public void initialize() {
		// Initialize managers
		configManager.initialize();
		languageManager.initialize();
		worldName = configManager.getConfig("core").getString("core.world_name","world");
		ChatUtil.printConsoleAlert("Primary world is "+worldName);
		kingdomManager.initialize();
		ruinManager.initialize();
		initManagers();
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
        	ChatUtil.printConsoleAlert("Successfully registered name color packets for this server version");
        } else {
        	ChatUtil.printConsoleError("Failed to register name color packets, the server version is unsupported");
        }
		
		kingdomManager.updateSmallestKingdom();
		kingdomManager.updateAllTownDisabledUpgrades();
		
		ChatUtil.printDebug("Finished Initialization");
	}
	
	public void reload() {
		configManager.reloadConfigs();
		initManagers();
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
	}
	
	public void initOnlinePlayers() {
		// Fetch any players that happen to be in the server already (typically from /reload)
        for(Player bukkitPlayer : Bukkit.getServer().getOnlinePlayers()) {
			initPlayer(bukkitPlayer);
			ChatUtil.printStatus("Loaded online player "+bukkitPlayer.getName());
		}
	}
	
	public KonPlayer initPlayer(Player bukkitPlayer) {
		KonPlayer player = null;
		bukkitPlayer.setScoreboard(getScoreboard());
    	// Fetch player from the database
    	// Also instantiates player object in PlayerManager
		databaseThread.getDatabase().fetchPlayerData(bukkitPlayer);
    	player = playerManager.getPlayer(bukkitPlayer);
    	// Update all player's nametag color packets
    	updateNamePackets();
    	// Update offline protections
    	kingdomManager.updateKingdomOfflineProtection();
    	// Update player membership stats
    	kingdomManager.updatePlayerMembershipStats(player);
    	// Updates based on login position
    	Chunk chunkLogin = bukkitPlayer.getLocation().getChunk();
    	kingdomManager.clearTownHearts(player);
    	if(kingdomManager.isChunkClaimed(chunkLogin)) {
			KonTerritory loginTerritory = kingdomManager.getChunkTerritory(chunkLogin);
    		if(loginTerritory.getTerritoryType().equals(KonTerritoryType.TOWN)) { 
	    		// Player joined located within a Town
	    		KonTown town = (KonTown) loginTerritory;
	    		town.addBarPlayer(playerManager.getPlayer(bukkitPlayer));
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
    			ruin.addBarPlayer(playerManager.getPlayer(bukkitPlayer));
    			ruin.spawnAllGolems();
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
	
	public String getWorldName() {
		return worldName;
	}
	
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
	
	public RuinManager getRuinManager() {
		return ruinManager;
	}
	
	public LanguageManager lang() {
		return languageManager;
	}
	
	public long getOfflineTimeoutSeconds() {
		return offlineTimeoutSeconds;
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
						player.getBukkitPlayer().getWorld().equals(Bukkit.getWorld(worldName)) &&
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
	    						int townDist = distanceInChunks(player.getBukkitPlayer().getLocation().getChunk(), town.getCenterLoc().getChunk());
	    						if(townDist < minDistance) {
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
	
	public Point toPoint(Location loc) {
		return new Point(loc.getChunk().getX(),loc.getChunk().getZ());
	}
	
	public Point toPoint(Chunk chunk) {
		return new Point(chunk.getX(),chunk.getZ());
	}
	
	public Chunk toChunk(Point point) {
		return Bukkit.getWorld(worldName).getChunkAt(point.x, point.y);
	}
	
	public int distanceInChunks(Location loc1, Location loc2) {
		int diffX = Math.abs(loc1.getChunk().getX() - loc2.getChunk().getX());
		int diffZ = Math.abs(loc1.getChunk().getZ() - loc2.getChunk().getZ());
		return Math.max(diffX, diffZ);
	}
	
	public int distanceInChunks(Chunk chunk1, Chunk chunk2) {
		int diffX = Math.abs(chunk1.getX() - chunk2.getX());
		int diffZ = Math.abs(chunk1.getZ() - chunk2.getZ());
		return Math.max(diffX, diffZ);
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
	
	public ArrayList<Location> formatStringToLocations(String coords) {
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
				locations.add(new Location(Bukkit.getWorld(worldName),x,y,z));
			}
		}
		//ChatUtil.printDebug("Chunk coords: "+Arrays.toString(points.toArray()));
		return locations;
	}
	
	// This can return null!
	public Location getRandomWildLocation(int worldSize) {
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
			randomNumY = Bukkit.getServer().getWorld(worldName).getHighestBlockYAt(randomNumX,randomNumZ) + 3;
			wildLoc = new Location(Bukkit.getServer().getWorld(worldName), randomNumX, randomNumY, randomNumZ);
			if(!kingdomManager.isChunkClaimed(wildLoc.getChunk())) {
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
		return randLoc;
	}
	/*
	public void setPlayersToFriendlies(Player player, List<String> friendlies) {
        net.minecraft.server.v1_16_R3.Scoreboard nmsScoreboard = new net.minecraft.server.v1_16_R3.Scoreboard();
        ScoreboardTeam nmsTeam = new ScoreboardTeam(nmsScoreboard, friendlyTeam.getName());
        PacketPlayOutScoreboardTeam packet = new PacketPlayOutScoreboardTeam(nmsTeam, friendlies, 3);
        CraftPlayer craftPlayer = (CraftPlayer) player;
        craftPlayer.getHandle().playerConnection.sendPacket(packet);
    }
 
    public void setPlayersToEnemies(Player player, List<String> enemies) {
        net.minecraft.server.v1_16_R3.Scoreboard nmsScoreboard = new net.minecraft.server.v1_16_R3.Scoreboard();
        ScoreboardTeam nmsTeam = new ScoreboardTeam(nmsScoreboard, enemyTeam.getName());
        PacketPlayOutScoreboardTeam packet = new PacketPlayOutScoreboardTeam(nmsTeam, enemies, 3);
        CraftPlayer craftPlayer = (CraftPlayer) player;
        craftPlayer.getHandle().playerConnection.sendPacket(packet);
    }
    
    public void setPlayersToBarbarians(Player player, List<String> barbarians) {
        net.minecraft.server.v1_16_R3.Scoreboard nmsScoreboard = new net.minecraft.server.v1_16_R3.Scoreboard();
        ScoreboardTeam nmsTeam = new ScoreboardTeam(nmsScoreboard, barbarianTeam.getName());
        PacketPlayOutScoreboardTeam packet = new PacketPlayOutScoreboardTeam(nmsTeam, barbarians, 3);
        CraftPlayer craftPlayer = (CraftPlayer) player;
        craftPlayer.getHandle().playerConnection.sendPacket(packet);
    }
    */
    private boolean setupTeamPacketSender() {
    	String version;
    	try {
    		version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    	} catch (ArrayIndexOutOfBoundsException e) {
    		ChatUtil.printDebug("Failed to determine server version.");
    		return false;
    	}
    	plugin.getServer().getConsoleSender().sendMessage(ChatColor.GOLD+"[Konquest] Your server version is "+version);
    	if(version.equals("v1_16_R3")) {
    		teamPacketSender = new TeamPacketSender_1_16_R3();
    	}
    	return teamPacketSender != null;
    }
    
    //TODO This could be optimized to reduce loop Order, and only update as needed
    public void updateNamePackets() {
    	if(teamPacketSender != null) {
	    	// Update all Kingdom player's nametag color packets
			for(String kingdomName : kingdomManager.getKingdomNames()) {
				// For each kingdom, determine friendlies and enemies
				List<Player> friendlyPlayers = new ArrayList<Player>();
				List<String> friendlyNames = new ArrayList<String>();
				List<String> enemyNames = new ArrayList<String>();
				List<String> barbarianNames = new ArrayList<String>();
				// Populate friendly and enemy lists
				for(KonPlayer player : playerManager.getPlayersOnline()) {
		    		if(player.getKingdom().getName().equalsIgnoreCase(kingdomName)) {
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
						teamPacketSender.setPlayersToFriendlies(kingdomPlayer, friendlyNames, friendlyTeam);
			    	}
			    	if(!enemyNames.isEmpty()) {
			    		teamPacketSender.setPlayersToEnemies(kingdomPlayer, enemyNames, enemyTeam);
			    	}
			    	if(!barbarianNames.isEmpty()) {
			    		teamPacketSender.setPlayersToBarbarians(kingdomPlayer, barbarianNames, barbarianTeam);
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
		    		teamPacketSender.setPlayersToEnemies(barbarianPlayer, enemyNames, enemyTeam);
		    	}
		    	if(!barbarianNames.isEmpty()) {
		    		teamPacketSender.setPlayersToBarbarians(barbarianPlayer, barbarianNames, barbarianTeam);
		    	}
			}
    	}
    }
    
    public static UUID idFromString(String id) {
    	UUID result = null;
    	try {
    		result = UUID.fromString(id);
    	} catch(IllegalArgumentException e) {
    		e.printStackTrace();
    	}
    	return result;
    }
    
    public void telePlayer(Player player, Location loc) {
    	if(loc.getChunk().isLoaded()) {
    		//player.teleport(new Location(loc.getWorld(),loc.getX(),loc.getY()+1.0,loc.getZ()));
    		ChatUtil.printDebug("Teleporting player "+player.getName()+" to loaded chunk");
    		new BukkitRunnable() {
				public void run() {
					player.teleport(new Location(loc.getWorld(),loc.getX()+0.5,loc.getY()+1.0,loc.getZ()+0.5),TeleportCause.PLUGIN);
				}
			}.runTaskLater(getPlugin(), 2L);
    	} else {
    		teleportQueue.put(new Location(loc.getWorld(),loc.getX()+0.5,loc.getY()+1.0,loc.getZ()+0.5),player);
    		ChatUtil.printDebug("Queueing player "+player.getName()+" for unloaded chunk destination");
    	}
    }
    
    public void applyQueuedTeleports(Chunk chunk) {
    	if(!teleportQueue.isEmpty()) {
	    	for(Location loc : teleportQueue.keySet()) {
	    		if(loc.getChunk().equals(chunk)) {
	    			//teleportQueue.get(loc).teleport(loc);
	    			Player player = teleportQueue.get(loc);
	    			new BukkitRunnable() {
	    				public void run() {
	    					player.teleport(loc,TeleportCause.PLUGIN);
	    				}
	    			}.runTaskLater(getPlugin(), 2L);
	    			ChatUtil.printDebug("Teleporting queued player "+teleportQueue.get(loc).getName());
	    			teleportQueue.remove(loc);
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
	
}
