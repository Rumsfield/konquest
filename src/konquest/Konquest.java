package konquest;

import java.awt.Point;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang.StringUtils;
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
import konquest.manager.CampManager;
import konquest.manager.ConfigManager;
import konquest.manager.DirectiveManager;
import konquest.manager.DisplayManager;
import konquest.manager.GuildManager;
import konquest.manager.IntegrationManager;
import konquest.manager.KingdomManager;
import konquest.manager.LanguageManager;
import konquest.manager.LootManager;
import konquest.manager.PlaceholderManager;
import konquest.manager.PlayerManager;
import konquest.manager.PlotManager;
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
import konquest.nms.Handler_1_16_R3;
import konquest.nms.Handler_1_17_R1;
import konquest.nms.Handler_1_18_R1;
import konquest.nms.TeamPacketSender;
import konquest.nms.TeamPacketSender_p754;
import konquest.nms.TeamPacketSender_p755;
//import konquest.nms.TeamPacketSender_p756;
import konquest.nms.TeamPacketSender_p757;
import konquest.nms.VersionHandler;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;
import konquest.utility.Timeable;
import konquest.utility.Timer;
import konquest.command.CommandHandler;
import konquest.database.DatabaseThread;

public class Konquest implements Timeable {

	private KonquestPlugin plugin;
	private static Konquest instance;
	private static String chatTag;
	private static String chatMessage;
	public static final String chatDivider = "�7�";
	public static ChatColor friendColor1 = ChatColor.GREEN;
	public static ChatColor friendColor2 = ChatColor.DARK_GREEN;
	public static ChatColor enemyColor1 = ChatColor.RED;
	public static ChatColor enemyColor2 = ChatColor.DARK_RED;
	public static ChatColor armisticeColor1 = ChatColor.LIGHT_PURPLE;
	public static ChatColor armisticeColor2 = ChatColor.DARK_PURPLE;
	public static ChatColor barbarianColor = ChatColor.YELLOW;
	public static ChatColor neutralColor = ChatColor.GRAY;
	
	private DatabaseThread databaseThread;
	private AccomplishmentManager accomplishmentManager;
	private DirectiveManager directiveManager;
	private PlayerManager playerManager;
	private KingdomManager kingdomManager;
	private CampManager campManager;
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
	private PlaceholderManager placeholderManager;
	private PlotManager plotManager;
	private GuildManager guildManager;
	
	private Scoreboard scoreboard;
    private Team friendlyTeam;
    private Team enemyTeam;
    private Team armisticeTeam;
    private Team barbarianTeam;
    private TeamPacketSender teamPacketSender;
    private boolean isPacketSendEnabled;
    private VersionHandler versionHandler;
    private boolean isVersionHandlerEnabled;
	
	private EventPriority chatPriority;
	private static final EventPriority defaultChatPriority = EventPriority.HIGH;
    private List<World> worlds;
    private boolean isWhitelist;
    private boolean isBlacklistIgnored;
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
	private HashMap<Player,Location> teleportLocationQueue;
	
	public Konquest(KonquestPlugin plugin) {
		this.plugin = plugin;
		instance = this;
		chatTag = "�7[�6Konquest�7]�f ";
		chatMessage = "%PREFIX% %KINGDOM% �7| %TITLE% %NAME% %SUFFIX% ";
		
		databaseThread = new DatabaseThread(this);
		accomplishmentManager = new AccomplishmentManager(this);
		directiveManager = new DirectiveManager(this);
		playerManager = new PlayerManager(this);
		kingdomManager = new KingdomManager(this);
		campManager = new CampManager(this);
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
		placeholderManager = new PlaceholderManager(this);
		plotManager = new PlotManager(this);
		guildManager = new GuildManager(this);
		
		teamPacketSender = null;
		versionHandler = null;
		
		chatPriority = defaultChatPriority;
		worlds = new ArrayList<World>();
		isWhitelist = false;
		isBlacklistIgnored = false;
		opStatusMessages = new ArrayList<String>();
		this.saveTimer = new Timer(this);
		this.compassTimer = new Timer(this);
		this.saveIntervalSeconds = 0;
		this.offlineTimeoutSeconds = 0;
		this.isPacketSendEnabled = false;
		this.isVersionHandlerEnabled = false;
		
		//teleportTerritoryQueue = new HashMap<Player,KonTerritory>();
		teleportLocationQueue = new HashMap<Player,Location>();
	}
	
	public void initialize() {
		// Initialize managers
		configManager.initialize();
		boolean debug = configManager.getConfig("core").getBoolean("core.debug");
		ChatUtil.printDebug("Debug is "+debug);
		String worldName = configManager.getConfig("core").getString("core.world_name");
		ChatUtil.printDebug("Primary world is "+worldName);
		
		initColors();
		languageManager.initialize();
		kingdomManager.initialize();
		ruinManager.initialize();
		guildManager.initialize();
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
        friendlyTeam.setColor(friendColor1);
        enemyTeam = scoreboard.registerNewTeam("enemies");
        enemyTeam.setColor(enemyColor1);
        armisticeTeam = scoreboard.registerNewTeam("armistice");
        armisticeTeam.setColor(armisticeColor1);
        barbarianTeam = scoreboard.registerNewTeam("barbarians");
        barbarianTeam.setColor(barbarianColor);
        
        // Set up version-specific classes
        initVersionHandlers();
		
		// Render Maps
		mapHandler.initialize();
		
		ChatUtil.printDebug("Finished Initialization");
	}
	
	private void initVersionHandlers() {
		String version;
    	try {
    		version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    	} catch (ArrayIndexOutOfBoundsException e) {
    		ChatUtil.printConsoleError("Failed to determine server version.");
    		return;
    	}
    	ChatUtil.printConsoleAlert("Your server version is "+version+", "+Bukkit.getServer().getBukkitVersion());
    	boolean isTeamPacketSenderReady = false;
    	boolean isVersionHandlerReady = false;
    	
    	// Version-specific cases
    	switch(version) {
    		case "v1_16_R3":
    			teamPacketSender = new TeamPacketSender_p754();
    			versionHandler = new Handler_1_16_R3();
    			break;
    		case "v1_17_R1":
    			teamPacketSender = new TeamPacketSender_p755();
    			versionHandler = new Handler_1_17_R1();
    			break;
    		case "v1_18_R1":
    			teamPacketSender = new TeamPacketSender_p757();
    			versionHandler = new Handler_1_18_R1();
    			break;
    		default:
    			break;
    	}
    	isTeamPacketSenderReady = teamPacketSender != null;
    	isVersionHandlerReady = versionHandler != null;
		
    	if(isTeamPacketSenderReady) {
        	if(plugin.isProtocolEnabled()) {
        		ChatUtil.printConsoleAlert("Successfully registered name color packets for this server version.");
        		isPacketSendEnabled = true;
        	} else {
        		ChatUtil.printConsoleError("Failed to register name color packets, ProtocolLib is disabled! Check version.");
        	}
        } else {
        	ChatUtil.printConsoleError("Failed to register name color packets, the server version is unsupported.");
        }
    	
    	if(isVersionHandlerReady) {
    		isVersionHandlerEnabled = true;
    	} else {
    		ChatUtil.printConsoleError("Some Konquest features may not work for this server version.");
    	}
    	
	}
	
	public void reload() {
		configManager.reloadConfigs();
		initManagers();
		initWorlds();
		ChatUtil.printDebug("Finished Reload");
	}
	
	private void initManagers() {
		String configTag = configManager.getConfig("core").getString("core.chat.tag");
		chatTag = ChatUtil.parseHex(configTag);
		ChatUtil.printDebug("Chat tag is "+chatTag);
		String configMessage = configManager.getConfig("core").getString("core.chat.message","");
		if(!configMessage.equals("")) {
			chatMessage = ChatUtil.parseHex(configMessage);
		}
		ChatUtil.printDebug("Chat message is "+chatMessage);
		integrationManager.initialize();
		lootManager.initialize();
		displayManager.initialize();
		playerManager.initialize();
		accomplishmentManager.initialize();
		directiveManager.initialize();
		upgradeManager.initialize();
		shieldManager.initialize();
		placeholderManager.initialize();
		plotManager.initialize();
		guildManager.loadOptions();
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
		chatPriority = getEventPriority(configManager.getConfig("core").getString("core.chat.priority","HIGH"));
		// Update kingdom stuff
		kingdomManager.updateSmallestKingdom();
		kingdomManager.updateAllTownDisabledUpgrades();
		kingdomManager.updateKingdomOfflineProtection();
		
	}
	
	private void initWorlds() {
		List<String> worldNameList = configManager.getConfig("core").getStringList("core.world_blacklist");
		isWhitelist = configManager.getConfig("core").getBoolean("core.world_blacklist_reverse",false);
		isBlacklistIgnored = configManager.getConfig("core").getBoolean("core.world_blacklist_ignore",false);
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
	
	private void initColors() {
		ChatColor color;
		String configColor = "";
		/* Friendly Primary Color */
		configColor = configManager.getConfig("core").getString("core.colors.friendly_primary","");
		color = ChatUtil.parseColorCode(configColor);
		if(color == null) {
			ChatUtil.printConsoleError("Invalid color code core.colors.friendly_primary: "+configColor);
		} else {
			friendColor1 = color;
		}
		/* Friendly Secondary Color */
		configColor = configManager.getConfig("core").getString("core.colors.friendly_secondary","");
		color = ChatUtil.parseColorCode(configColor);
		if(color == null) {
			ChatUtil.printConsoleError("Invalid color code core.colors.friendly_secondary: "+configColor);
		} else {
			friendColor2 = color;
		}
		/* Enemy Primary Color */
		configColor = configManager.getConfig("core").getString("core.colors.enemy_primary","");
		color = ChatUtil.parseColorCode(configColor);
		if(color == null) {
			ChatUtil.printConsoleError("Invalid color code core.colors.enemy_primary: "+configColor);
		} else {
			enemyColor1 = color;
		}
		/* Enemy Secondary Color */
		configColor = configManager.getConfig("core").getString("core.colors.enemy_secondary","");
		color = ChatUtil.parseColorCode(configColor);
		if(color == null) {
			ChatUtil.printConsoleError("Invalid color code core.colors.enemy_secondary: "+configColor);
		} else {
			enemyColor2 = color;
		}
		/* Armistice Primary Color */
		configColor = configManager.getConfig("core").getString("core.colors.armistice_primary","");
		color = ChatUtil.parseColorCode(configColor);
		if(color == null) {
			ChatUtil.printConsoleError("Invalid color code core.colors.armistice_primary: "+configColor);
		} else {
			armisticeColor1 = color;
		}
		/* Armistice Secondary Color */
		configColor = configManager.getConfig("core").getString("core.colors.armistice_secondary","");
		color = ChatUtil.parseColorCode(configColor);
		if(color == null) {
			ChatUtil.printConsoleError("Invalid color code core.colors.armistice_secondary: "+configColor);
		} else {
			armisticeColor2 = color;
		}
		/* Barbarian Color */
		configColor = configManager.getConfig("core").getString("core.colors.barbarian","");
		color = ChatUtil.parseColorCode(configColor);
		if(color == null) {
			ChatUtil.printConsoleError("Invalid color code core.colors.barbarian: "+configColor);
		} else {
			barbarianColor = color;
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
    	//if(player.isBarbarian()) {
    		KonCamp testCamp = campManager.getCamp(player);
    		if(testCamp != null) {
    			testCamp.setProtected(false);
    			testCamp.setOnlineOwner(bukkitPlayer);
    		}
    	//}
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
	
	public void save() {
		// Save config files
		kingdomManager.saveKingdoms();
		campManager.saveCamps();
		ruinManager.saveRuins();
		guildManager.saveGuilds();
		configManager.saveConfigs();
	}
	
	public static Konquest getInstance() {
		return instance;
	}
	
	public KonquestPlugin getPlugin() {
		return plugin;
	}
	
	public VersionHandler getVersionHandler() {
		// This can be null!
		return versionHandler;
	}
	
	public boolean isVersionHandlerEnabled() {
		return isVersionHandlerEnabled;
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
	
	public CampManager getCampManager() {
		return campManager;
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
	
	public PlaceholderManager getPlaceholderManager() {
		return placeholderManager;
	}
	
	public PlotManager getPlotManager() {
		return plotManager;
	}
	
	public GuildManager getGuildManager() {
		return guildManager;
	}
	
	public long getOfflineTimeoutSeconds() {
		return offlineTimeoutSeconds;
	}
	
	public EventPriority getChatPriority() {
		return chatPriority;
	}
	
	public boolean isWorldValid(Location loc) {
		if(loc != null && loc.getWorld() != null) {
			return isWorldValid(loc.getWorld());
		} else {
			return false;
		}
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
	
	public boolean isWorldIgnored(Location loc) {
		if(loc != null) {
			return isBlacklistIgnored && !isWorldValid(loc);
		}
		return true;
	}
	
	public boolean isWorldIgnored(World world) {
		if(world != null) {
			return isBlacklistIgnored && !isWorldValid(world);
		}
		return true;
	}
	
	/**
	 * Checks for name conflicts and constraints for all namable objects
	 * @param name - The name of an object (town, ruin, etc)
	 * @param player - The player requesting the name, to be sent status messages
	 * @return Status code
	 * 			0 - Success, no issue found
	 * 			1 - Error, name is not strictly alpha-numeric
	 * 			2 - Error, name has more than 20 characters
	 * 			3 - Error, name is an existing player
	 * 			4 - Error, name is a kingdom
	 * 			5 - Error, name is a town
	 * 			6 - Error, name is a ruin
	 * 			7 - Error, name is a guild
	 */
	public int validateName(String name, Player player) {
		if(name == null || name.equals("") || !StringUtils.isAlphanumeric(name)) {
    		if(player != null) {
    			ChatUtil.sendError(player, MessagePath.GENERIC_ERROR_FORMAT_NAME.getMessage());
    		}
			return 1;
    	}
    	if(name.length() > 20) {
    		if(player != null) {
    			ChatUtil.sendError(player, MessagePath.GENERIC_ERROR_LENGTH_NAME.getMessage());
    		}
    		return 2;
    	}
    	if(playerManager.isPlayerNameExist(name)) {
    		if(player != null) {
    			ChatUtil.sendError(player, MessagePath.GENERIC_ERROR_TAKEN_NAME.getMessage());
    		}
    		return 3;
    	}
		if(kingdomManager.isKingdom(name)) {
			if(player != null) {
    			ChatUtil.sendError(player, MessagePath.GENERIC_ERROR_TAKEN_NAME.getMessage());
    		}
			return 4;
		}
		for(KonKingdom kingdom : kingdomManager.getKingdoms()) {
			if(kingdom.hasTown(name)) {
				if(player != null) {
	    			ChatUtil.sendError(player, MessagePath.GENERIC_ERROR_TAKEN_NAME.getMessage());
	    		}
				return 5;
			}
		}
		if(ruinManager.isRuin(name)) {
			if(player != null) {
    			ChatUtil.sendError(player, MessagePath.GENERIC_ERROR_TAKEN_NAME.getMessage());
    		}
			return 6;
		}
		if(guildManager.isGuild(name)) {
			if(player != null) {
    			ChatUtil.sendError(player, MessagePath.GENERIC_ERROR_TAKEN_NAME.getMessage());
    		}
			return 7;
		}
		return 0;
	}
	
	@Override
	public void onEndTimer(int taskID) {
		if(taskID == 0) {
			ChatUtil.printDebug("Save Timer ended with null taskID!");
		} else if(taskID == saveTimer.getTaskID()) {
			// Prune residents and camp owners for being offline too long
			if(offlineTimeoutSeconds != 0) {
				// Search all stored players and prune
				Date now = new Date();
				for(KonOfflinePlayer player : playerManager.getAllKonOfflinePlayers()) {
					long lastPlayedTime = player.getOfflineBukkitPlayer().getLastPlayed();
					if(lastPlayedTime > 0 && now.after(new Date(lastPlayedTime + (offlineTimeoutSeconds*1000)))) {
						// Offline player has exceeded timeout period, prune from residencies and camp
						boolean doExile = configManager.getConfig("core").getBoolean("core.kingdoms.offline_timeout_exile",false);
						if(!player.isBarbarian()) {
							if(doExile) {
								getKingdomManager().exileOfflinePlayer(player);
							} else {
								for(KonTown town : player.getKingdom().getTowns()) {
									if(town.getPlayerResidents().contains(player.getOfflineBukkitPlayer())) {
										boolean status = town.removePlayerResident(player.getOfflineBukkitPlayer());
										ChatUtil.printDebug("Pruned player "+player.getOfflineBukkitPlayer().getName()+" from town "+town.getName()+" in kingdom "+player.getKingdom().getName()+", got "+status);
									}
								}
							}
						} else {
							if(campManager.isCampSet(player)) {
								campManager.removeCamp(player);
								ChatUtil.printDebug("Pruned player "+player.getOfflineBukkitPlayer().getName()+" from camp");
							}
						}
					}
				}
			}
			// Save config files
			save();
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
	
	public ArrayList<Point> getBorderPoints(Location loc, int radius) {
		ArrayList<Point> areaPoints = new ArrayList<Point>();
		Point center = toPoint(loc);
		if(radius > 0) {
			int min = (radius-1)*-1;
			int max = (radius-1);
			for(int x=min;x<=max;x++) {
				for(int z=min;z<=max;z++) {
					if(x == min || z == min || x == max || z == max) {
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
	
	public static Point toPoint(Location loc) {
		return new Point((int)Math.floor((double)loc.getBlockX()/16),(int)Math.floor((double)loc.getBlockZ()/16));
	}
	
	public static Point toPoint(Chunk chunk) {
		return new Point(chunk.getX(),chunk.getZ());
	}
	
	public static Chunk toChunk(Point point, World world) {
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
	public Location getRandomWildLocation(World world) {
		Location wildLoc = null;
		int radius = configManager.getConfig("core").getInt("core.travel.wild_radius",500);
		int offsetX = configManager.getConfig("core").getInt("core.travel.wild_center_x",0);
		int offsetZ = configManager.getConfig("core").getInt("core.travel.wild_center_z",0);
		radius = radius > 0 ? radius : 2;
		ChatUtil.printDebug("Generating random wilderness location at center "+offsetX+","+offsetZ+" in radius "+radius);
		int randomNumX = 0;
		int randomNumZ = 0;
		int randomNumY = 0;
		boolean foundValidLoc = false;
		int timeout = 0;
		while(!foundValidLoc) {
			randomNumX = ThreadLocalRandom.current().nextInt(-1*(radius), (radius) + 1) + offsetX;
			randomNumZ = ThreadLocalRandom.current().nextInt(-1*(radius), (radius) + 1) + offsetZ;
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
		List<String> armisticeNames = new ArrayList<String>();
		List<String> barbarianNames = new ArrayList<String>();
    	for(KonPlayer onlinePlayer : playerManager.getPlayersOnline()) {
    		boolean isArmistice = guildManager.isArmistice(onlinePlayer, player);
    		// Place online player in appropriate list w.r.t. player
    		if(onlinePlayer.isBarbarian()) {
    			barbarianNames.add(onlinePlayer.getBukkitPlayer().getName());
    		} else {
    			if(onlinePlayer.getKingdom().equals(player.getKingdom())) {
    				friendlyNames.add(onlinePlayer.getBukkitPlayer().getName());
    			} else {
    				if(isArmistice) {
    					armisticeNames.add(onlinePlayer.getBukkitPlayer().getName());
    				} else {
    					enemyNames.add(onlinePlayer.getBukkitPlayer().getName());
    				}
    			}
    		}
    		// Send appropriate team packet to online player
    		if(player.isBarbarian()) {
    			teamPacketSender.sendPlayerTeamPacket(onlinePlayer.getBukkitPlayer(), Arrays.asList(player.getBukkitPlayer().getName()), barbarianTeam);
    		} else {
    			if(player.getKingdom().equals(onlinePlayer.getKingdom())) {
    				teamPacketSender.sendPlayerTeamPacket(onlinePlayer.getBukkitPlayer(), Arrays.asList(player.getBukkitPlayer().getName()), friendlyTeam);
    			} else {
    				if(isArmistice) {
    					teamPacketSender.sendPlayerTeamPacket(onlinePlayer.getBukkitPlayer(), Arrays.asList(player.getBukkitPlayer().getName()), armisticeTeam);
    				} else {
    					teamPacketSender.sendPlayerTeamPacket(onlinePlayer.getBukkitPlayer(), Arrays.asList(player.getBukkitPlayer().getName()), enemyTeam);
    				}
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
    	if(!armisticeNames.isEmpty()) {
    		teamPacketSender.sendPlayerTeamPacket(player.getBukkitPlayer(), armisticeNames, armisticeTeam);
    	}
    	if(!barbarianNames.isEmpty()) {
    		teamPacketSender.sendPlayerTeamPacket(player.getBukkitPlayer(), barbarianNames, barbarianTeam);
    	}
    }
    
    public void updateNamePackets(OfflinePlayer offlineBukkitPlayer) {
    	if(offlineBukkitPlayer != null && offlineBukkitPlayer.getName() != null && offlineBukkitPlayer.isOnline()) {
    		KonPlayer player = playerManager.getPlayer((Player)offlineBukkitPlayer);
    		if(player != null) {
    			updateNamePackets(player);
    		}
    	}
    }
    
    public void updateNamePackets(UUID id) {
    	OfflinePlayer offlineBukkitPlayer = Bukkit.getOfflinePlayer(id);
    	updateNamePackets(offlineBukkitPlayer);
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
    
    public void telePlayerLocation(Player player, Location travelLocation) {
    	Point locPoint = toPoint(travelLocation);
    	Location qLoc = travelLocation;
		Location destination = new Location(qLoc.getWorld(),qLoc.getBlockX()+0.5,qLoc.getBlockY()+1.0,qLoc.getBlockZ()+0.5,qLoc.getYaw(),qLoc.getPitch());
    	if(travelLocation.getWorld().isChunkLoaded(locPoint.x,locPoint.y)) {
    		ChatUtil.printDebug("Teleporting player "+player.getName()+" to loaded chunk");
    		player.teleport(destination,TeleportCause.PLUGIN);
    	} else {
    		teleportLocationQueue.put(player,destination);
    		ChatUtil.printDebug("Queueing player "+player.getName()+" for unloaded chunk destination");
    		travelLocation.getWorld().loadChunk(locPoint.x,locPoint.y);
    	}
    }

    public void applyQueuedTeleports(Chunk chunk) {
    	Point cPoint = toPoint(chunk);
    	Point qPoint;
		Location qLoc;
		/*
    	if(!teleportTerritoryQueue.isEmpty()) {
	    	for(Player qPlayer : teleportTerritoryQueue.keySet()) {
	    		qLoc = teleportTerritoryQueue.get(qPlayer).getSpawnLoc();
	    		qPoint = toPoint(qLoc);
	    		if(qPoint.equals(cPoint) && chunk.getWorld().equals(qLoc.getWorld())) {
	    			Location destination = new Location(qLoc.getWorld(),qLoc.getBlockX()+0.5,qLoc.getBlockY()+1.0,qLoc.getBlockZ()+0.5,qLoc.getYaw(),qLoc.getPitch());
	    			qPlayer.teleport(destination,TeleportCause.PLUGIN);
	    			ChatUtil.printDebug("Teleporting territory queued player "+qPlayer.getName());
	    			teleportTerritoryQueue.remove(qPlayer);
	    		}
	    	}
    	}
    	*/
    	if(!teleportLocationQueue.isEmpty()) {
	    	for(Player qPlayer : teleportLocationQueue.keySet()) {
	    		qLoc = teleportLocationQueue.get(qPlayer);
	    		qPoint = toPoint(qLoc);
	    		if(qPoint.equals(cPoint) && chunk.getWorld().equals(qLoc.getWorld())) {
	    			//Location destination = new Location(qLoc.getWorld(),qLoc.getBlockX()+0.5,qLoc.getBlockY()+1.0,qLoc.getBlockZ()+0.5,qLoc.getYaw(),qLoc.getPitch());
	    			qPlayer.teleport(qLoc,TeleportCause.PLUGIN);
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
    
    /**
     * Determines primary color based on player relationships
     * @param displayPlayer - The target player to show the color to
     * @param contextPlayer - The player to base the color from
     * @param isArmistice - Are the two players in an armistice?
     * @return Color
     */
    public static ChatColor getDisplayPrimaryColor(KonOfflinePlayer displayPlayer, KonOfflinePlayer contextPlayer, boolean isArmistice) {
    	ChatColor result = ChatColor.RED;
    	if(contextPlayer.isBarbarian()) {
    		result = barbarianColor;
		} else {
			if(contextPlayer.getKingdom().equals(displayPlayer.getKingdom())) {
				result = friendColor1;
    		} else {
    			if(isArmistice) {
    				result = armisticeColor1;
    			} else {
    				result = enemyColor1;
    			}
    		}
		}
    	return result;
    }
    
    public static ChatColor getDisplayPrimaryColor(KonKingdom displayKingdom, KonKingdom contextKingdom, boolean isArmistice) {
    	ChatColor result = ChatColor.RED;
    	if(contextKingdom.equals(displayKingdom)) {
			result = friendColor1;
		} else {
			if(isArmistice) {
				result = armisticeColor1;
			} else {
				result = enemyColor1;
			}
		}
    	return result;
    }
    
    public static ChatColor getDisplayPrimaryColor(KonOfflinePlayer displayPlayer, KonTown contextTown, boolean isArmistice) {
    	return getDisplayPrimaryColor(displayPlayer.getKingdom(), contextTown.getKingdom(), isArmistice);
    }
    
    /**
     * Determines secondary color based on player relationships
     * @param displayPlayer - The target player to show the color to
     * @param contextPlayer - The player to base the color from
     * @param isArmistice - Are the two players in an armistice?
     * @return Color
     */
    public static ChatColor getDisplaySecondaryColor(KonOfflinePlayer displayPlayer, KonOfflinePlayer contextPlayer, boolean isArmistice) {
    	ChatColor result = ChatColor.RED;
    	if(contextPlayer.isBarbarian()) {
    		result = barbarianColor;
		} else {
			if(contextPlayer.getKingdom().equals(displayPlayer.getKingdom())) {
				result = friendColor2;
    		} else {
    			if(isArmistice) {
    				result = armisticeColor2;
    			} else {
    				result = enemyColor2;
    			}
    		}
		}
    	return result;
    }
    
    public ChatColor getDisplayKingdomColor(KonKingdom displayKingdom, KonKingdom contextKingdom, boolean isArmistice) {
    	ChatColor result = ChatColor.RED;
    	if(contextKingdom.equals(kingdomManager.getBarbarians())) {
    		result = barbarianColor;
    	} else if(contextKingdom.equals(kingdomManager.getNeutrals())) {
    		result = neutralColor;
    	} else if(contextKingdom.equals(displayKingdom)) {
			result = friendColor1;
		} else {
			if(isArmistice) {
				result = armisticeColor1;
			} else {
				result = enemyColor1;
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
    
    public static void playFailSound(Player bukkitPlayer) {
    	Bukkit.getScheduler().scheduleSyncDelayedTask(instance.getPlugin(), new Runnable() {
            @Override
            public void run() {
            	bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_GLASS_BREAK, (float)0.5, (float)1.4);
            }
        },1);
    }
    
    public static void playDiscountSound(Player bukkitPlayer) {
    	Bukkit.getScheduler().scheduleSyncDelayedTask(instance.getPlugin(), new Runnable() {
            @Override
            public void run() {
            	bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, (float)1.0, (float)1.0);
            }
        },1);
    }
    
    public static void playTownArmorSound(Player bukkitPlayer) {
    	playTownArmorSound(bukkitPlayer.getLocation());
    }
    
    public static void playTownArmorSound(Location loc) {
    	loc.getWorld().playSound(loc, Sound.ENTITY_SHULKER_SHOOT, (float)1.0, (float)2);
    }
    
    public static void playCampGroupSound(Location loc) {
    	loc.getWorld().playSound(loc, Sound.BLOCK_FENCE_GATE_OPEN, (float)1.0, (float)0.7);
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
    
    public static String getLastSeenFormat(OfflinePlayer offlineBukkitPlayer) {
    	Date date = new Date(); // Now
    	if(!offlineBukkitPlayer.isOnline()) {
    		date = new Date(offlineBukkitPlayer.getLastPlayed()); // Last joined
    	}
    	//SimpleDateFormat formater = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
    	SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        return formater.format(date);
    }
    
    public static EventPriority getEventPriority(String priority) {
    	EventPriority result = defaultChatPriority;
    	if(priority != null) {
    		try {
    			result = EventPriority.valueOf(priority.toUpperCase());
    		} catch(IllegalArgumentException e) {
    			// do nothing
    		}
    	}
    	return result;
    }
    
    public static String getChatTag() {
    	return chatTag;
    }
    
    public static String getChatMessage() {
    	return chatMessage;
    }
	
}
