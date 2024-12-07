package com.github.rumsfield.konquest;

import com.github.rumsfield.konquest.api.KonquestAPI;
import com.github.rumsfield.konquest.api.event.KonquestEvent;
import com.github.rumsfield.konquest.api.model.*;
import com.github.rumsfield.konquest.command.CommandHandler;
import com.github.rumsfield.konquest.database.DatabaseThread;
import com.github.rumsfield.konquest.listener.TNTListener;
import com.github.rumsfield.konquest.manager.*;
import com.github.rumsfield.konquest.manager.TravelManager.TravelDestination;
import com.github.rumsfield.konquest.map.MapHandler;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.nms.*;
import com.github.rumsfield.konquest.shop.ShopHandler;
import com.github.rumsfield.konquest.utility.*;
import com.github.rumsfield.konquest.utility.Timer;
import com.google.common.collect.MapMaker;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

public class Konquest implements KonquestAPI, Timeable {
	
	private final KonquestPlugin plugin;
	private static Konquest instance;
	private static String chatTag;
	private static String chatMessage;
	private static String chatDivider;
	public static String friendColor1 		= "§7";
	public static String friendColor2 		= "§7";
	public static String enemyColor1 		= "§7";
	public static String enemyColor2 		= "§7";
	public static String tradeColor1 		= "§7";
	public static String tradeColor2 		= "§7";
	public static String peacefulColor1 	= "§7";
	public static String peacefulColor2 	= "§7";
	public static String alliedColor1 		= "§7";
	public static String alliedColor2 		= "§7";
	public static String barbarianColor1 	= "§7";
	public static String barbarianColor2 	= "§7";
	public static String neutralColor1 		= "§7";
	public static String neutralColor2 		= "§7";

	public static ChatColor blockedProtectionColor = ChatColor.DARK_RED;
	public static ChatColor blockedShieldColor = ChatColor.DARK_AQUA;
	public static ChatColor blockedFlagColor = ChatColor.DARK_GRAY;

	public static String metaTntOwnerId = "konquest-tnt-owner";
	public static String healthModName = "konquest.health_buff"; // never change this :)
	public NamespacedKey healthModKey;
	
	private final DatabaseThread databaseThread;
	private final AccomplishmentManager accomplishmentManager;
	private final DirectiveManager directiveManager;
	private final PlayerManager playerManager;
	private final KingdomManager kingdomManager;
	private final CampManager campManager;
	private final ConfigManager configManager;
	private final IntegrationManager integrationManager;
	private final LootManager lootManager;
	private final CommandHandler commandHandler;
	private final DisplayManager displayManager;
	private final UpgradeManager upgradeManager;
	private final ShieldManager shieldManager;
	private final RuinManager ruinManager;
	private final LanguageManager languageManager;
	private final MapHandler mapHandler;
	private final ShopHandler shopHandler;
	private final PlaceholderManager placeholderManager;
	private final PlotManager plotManager;
	private final TravelManager travelManager;
	private final SanctuaryManager sanctuaryManager;
	private final TerritoryManager territoryManager;
	
	private Scoreboard scoreboard;
    private Team friendlyTeam;
    private Team enemyTeam;
    private Team tradeTeam;
    private Team peacefulTeam;
    private Team alliedTeam;
    private Team barbarianTeam;
    private VersionHandler versionHandler;
	private boolean isVersionSupported;
    private boolean isVersionHandlerEnabled;
	
	private EventPriority chatPriority;
	private static final EventPriority defaultChatPriority = EventPriority.HIGH;
    private final List<World> worlds;
	private final List<World> ignoredWorlds;
    private boolean isWhitelist;
	public List<String> opStatusMessages;
	private final Timer saveTimer;
	private final Timer compassTimer;
	private final Timer pingTimer;
	private int saveIntervalSeconds;
	private long offlineTimeoutSeconds;
	public ConcurrentMap<Player, Location> lastPlaced = new MapMaker().
            weakKeys().
            weakValues().
            makeMap();
	private final ConcurrentMap<UUID, ItemStack> headCache = new MapMaker().makeMap();
	private final HashMap<Player,Location> teleportLocationQueue;
	
	public Konquest(KonquestPlugin plugin) {
		this.plugin = plugin;
		instance = this;
		chatTag = "§7[§6Konquest§7]§f ";
		chatMessage = "%PREFIX% %KINGDOM% §7| %TITLE% %NAME% %SUFFIX% ";
		chatDivider = "§8»§r ";

		healthModKey = new NamespacedKey(plugin,"health_upgrade");
		
		this.databaseThread = new DatabaseThread(this);
		this.accomplishmentManager = new AccomplishmentManager(this);
		this.directiveManager = new DirectiveManager(this);
		this.playerManager = new PlayerManager(this);
		this.kingdomManager = new KingdomManager(this);
		this.campManager = new CampManager(this);
		this.configManager = new ConfigManager(this);
		this.integrationManager = new IntegrationManager(this);
		this.lootManager = new LootManager(this);
		this.commandHandler = new CommandHandler(this);
		this.displayManager = new DisplayManager(this);
		this.upgradeManager = new UpgradeManager(this);
		this.shieldManager = new ShieldManager(this);
		this.ruinManager = new RuinManager(this);
		this.languageManager = new LanguageManager(this);
		this.mapHandler = new MapHandler(this);
		this.shopHandler = new ShopHandler(this);
		this.plotManager = new PlotManager(this);
		this.travelManager = new TravelManager(this);
		this.sanctuaryManager = new SanctuaryManager(this);
		this.territoryManager = new TerritoryManager(this);
		this.placeholderManager = new PlaceholderManager(this);

		this.versionHandler = null;
		
		this.chatPriority = defaultChatPriority;
		this.worlds = new ArrayList<>();
		this.ignoredWorlds = new ArrayList<>();
		this.isWhitelist = false;
		this.opStatusMessages = new ArrayList<>();
		this.saveTimer = new Timer(this);
		this.compassTimer = new Timer(this);
		this.pingTimer = new Timer(this);
		this.saveIntervalSeconds = 0;
		this.offlineTimeoutSeconds = 0;
		this.isVersionSupported = false;
		this.isVersionHandlerEnabled = false;
		this.teleportLocationQueue = new HashMap<>();
	}
	
	public void initialize() {
		// Initialize managers
		configManager.initialize();
		checkCorePaths();
		boolean debug = getCore().getBoolean(CorePath.DEBUG.getPath());
		ChatUtil.printDebug("Beginning core Konquest initialization");
		ChatUtil.printDebug("Debug is "+debug);
		String worldName = getCore().getString(CorePath.WORLD_NAME.getPath());
		ChatUtil.printDebug("Primary world is "+worldName);

		initColors();
		languageManager.initialize();
		kingdomManager.loadCriticalBlocks(); // load critical block material before sanctuaries
		sanctuaryManager.initialize(); // Load sanctuaries and monument templates
		kingdomManager.initialize(); // Load all kingdoms + towns
		sanctuaryManager.refresh(); // Update sanctuary references to neutrals kingdom
		ruinManager.initialize();
		initManagers();
		initWorlds();
		kingdomManager.updateKingdomOfflineProtection(true);
		printConfigFeatures();
		
		databaseThread.setSleepSeconds(saveIntervalSeconds);
		if(!databaseThread.isRunning()) {
			ChatUtil.printDebug("Starting database thread");
			databaseThread.getThread().start();
		} else {
			ChatUtil.printDebug("Database thread is already running");
		}

		initScoreboard();
        
        // Set up version-specific classes
		initVersionHandlers();
		
		// Render Maps
		mapHandler.initialize();

		// Enable special event listeners
		boolean isTNTListenerEnabled = getCore().getBoolean(CorePath.ENABLE_ADVANCED_TNT_PROTECTION.getPath(),true);
		if (isTNTListenerEnabled) {
			plugin.getServer().getPluginManager().registerEvents(new TNTListener(plugin), plugin);
			ChatUtil.printDebug("Enabled TNT Listener");
		}
		
		ChatUtil.printDebug("Finished core Konquest initialization");
	}

	// Initialization that happens after the database is ready on startup
	public void initializePostDB() {
		accomplishmentManager.loadCustomPrefixes();
		databaseThread.getDatabase().spawnTables();
		playerManager.initAllSavedPlayers();
		kingdomManager.loadLegacyKingdomMemberships();
		campManager.initCamps();
		mapHandler.drawAllTerritories();
		initOnlinePlayers();
		integrationManager.getDiscordSrv().setKonquestReady();
		integrationManager.getDiscordSrv().refreshRoles();
	}
	
	public void disable() {
		integrationManager.disable();
		sanctuaryManager.saveSanctuaries();
		kingdomManager.saveKingdoms();
		campManager.saveCamps();
		ruinManager.saveRuins();
		ruinManager.regenAllRuins();
		ruinManager.removeAllGolems();
		playerManager.clearAllMobTargets();
		configManager.saveConfigs();
		databaseThread.flushDatabase();
		databaseThread.getDatabase().getDatabaseConnection().disconnect();
	}
	
	private void initVersionHandlers() {
    	ChatUtil.printConsoleAlert("Your server version is "+Bukkit.getServer().getBukkitVersion());
    	boolean isProtocolLibEnabled = integrationManager.getProtocolLib().isEnabled();
    	// Version-specific cases
    	try {
			if (CompatibilityUtil.apiVersion.compareTo(new Version("1.16")) < 0) {
				isVersionSupported = false;

			} else if (CompatibilityUtil.apiVersion.compareTo(new Version("1.16.5")) <= 0) {
				isVersionSupported = true;
				if(isProtocolLibEnabled) { versionHandler = new Handler_1_16_R3(); }

			} else if (CompatibilityUtil.apiVersion.compareTo(new Version("1.17.1")) <= 0) {
				isVersionSupported = true;
				if(isProtocolLibEnabled) { versionHandler = new Handler_1_17_R1(); }

			} else if (CompatibilityUtil.apiVersion.compareTo(new Version("1.18.1")) <= 0) {
				isVersionSupported = true;
				if(isProtocolLibEnabled) { versionHandler = new Handler_1_18_R1(); }

			} else if (CompatibilityUtil.apiVersion.compareTo(new Version("1.18.2")) <= 0) {
				isVersionSupported = true;
				if(isProtocolLibEnabled) { versionHandler = new Handler_1_18_R2(); }

			} else if (CompatibilityUtil.apiVersion.compareTo(new Version("1.21.4")) <= 0) {
				isVersionSupported = true;
				if(isProtocolLibEnabled) { versionHandler = new Handler_1_19_R1(); }

			} else {
				isVersionSupported = false;

			}
    	} catch (Exception | NoClassDefFoundError e) {
    		ChatUtil.printConsoleError("Failed to setup a version handler, ProtocolLib is probably missing. ");
    		e.printStackTrace();
    	}

		if (!isVersionSupported) {
			ChatUtil.printConsoleError("This version of Minecraft is not supported by Konquest! Some features may not work correctly.");
		}

		if(isProtocolLibEnabled) {
			if(versionHandler != null) {
				isVersionHandlerEnabled = true;
				ChatUtil.printConsoleAlert("Successfully registered name color packets for this server version");
			}
		} else {
			ChatUtil.printConsoleError("Failed to register name color packets, ProtocolLib is missing or disabled! Check version.");
		}

		if(!isVersionHandlerEnabled) {
			ChatUtil.printConsoleError("Some Konquest features are disabled. See previous error messages.");
		}
	}
	
	public void reload() {
		ChatUtil.printConsoleAlert("Reloading config files");
		configManager.reloadConfigs();
		initManagers();
		integrationManager.getDiscordSrv().reloadSettings();
		initWorlds();
		printConfigFeatures();
		ChatUtil.printConsoleAlert("Finished reload");
	}
	
	private void initManagers() {
		// Set up chat formats
		String configTag = getCore().getString(CorePath.CHAT_TAG.getPath(),"");
		chatTag = ChatUtil.parseHex(configTag);
		ChatUtil.printDebug("Chat tag is "+chatTag);
		String configMessage = getCore().getString(CorePath.CHAT_MESSAGE.getPath(),"");
		if(!configMessage.equals("")) {
			// Cannot be an empty string
			chatMessage = ChatUtil.parseHex(configMessage);
		}
		ChatUtil.printDebug("Chat message is "+chatMessage);
		String configDivider = getCore().getString(CorePath.CHAT_DIVIDER.getPath(),"");
		chatDivider = ChatUtil.parseHex(configDivider);
		ChatUtil.printDebug("Chat divider is "+chatDivider);

		kingdomManager.loadOptions();
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
		offlineTimeoutSeconds = getCore().getInt(CorePath.KINGDOMS_OFFLINE_TIMEOUT_DAYS.getPath(),0)* 86400L;
		if(offlineTimeoutSeconds > 0 && offlineTimeoutSeconds < 86400) {
			offlineTimeoutSeconds = 86400;
			ChatUtil.printConsoleError("Offline timeout setting is less than 1 day, overriding to 1 day to prevent data loss.");
		}
		saveIntervalSeconds = getCore().getInt(CorePath.SAVE_INTERVAL.getPath(),60)*60;
		if(saveIntervalSeconds > 0) {
			saveTimer.stopTimer();
			saveTimer.setTime(saveIntervalSeconds);
			saveTimer.startLoopTimer();
		}
		compassTimer.stopTimer();
		compassTimer.setTime(30); // 30 second compass update interval
		compassTimer.startLoopTimer();
		pingTimer.stopTimer();
		pingTimer.setTime(60*60); // 1 hour database ping interval
		pingTimer.startLoopTimer();
		// Set chat even priority
		chatPriority = getEventPriority(getCore().getString(CorePath.CHAT_PRIORITY.getPath(),"HIGH"));
		// Update kingdom stuff
		kingdomManager.loadArmorBlacklist();
		kingdomManager.loadJoinExileCooldowns();
		kingdomManager.updateSmallestKingdom();
		kingdomManager.updateAllTownDisabledUpgrades();
		kingdomManager.updateKingdomOfflineProtection();
	}
	
	private void initWorlds() {
		List<String> worldNameList = getCore().getStringList(CorePath.WORLD_BLACKLIST.getPath());
		isWhitelist = getCore().getBoolean(CorePath.WORLD_BLACKLIST_REVERSE.getPath(),false);
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
		// Ignored Worlds
		List<String> ignoredWorldNameList = getCore().getStringList(CorePath.WORLD_IGNORELIST.getPath());
		for(String name : ignoredWorldNameList) {
			boolean matches = false;
			for(World world : Bukkit.getServer().getWorlds()) {
				if(world.getName().equals(name)) {
					matches = true;
					ignoredWorlds.add(world);
					break;
				}
			}
			if(!matches) {
				ChatUtil.printConsoleError("core.world_ignorelist name \""+name+"\" does not match any server worlds, check spelling and case.");
			}
		}
	}

	private void initScoreboard() {
		boolean useRelationSuffix = getCore().getBoolean(CorePath.PLAYER_NAMETAG_SUFFIX_RELATION.getPath());
		// Create global scoreboard and teams
		scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		// Friendly team - players in the same kingdom
		friendlyTeam = scoreboard.registerNewTeam("friendlies");
		friendlyTeam.setColor(ChatUtil.lookupChatColor(friendColor1));
		// Enemy team - players in another kingdom at war
		enemyTeam = scoreboard.registerNewTeam("enemies");
		enemyTeam.setColor(ChatUtil.lookupChatColor(enemyColor1));
		// Trade team - players in another kingdom with a trade agreement
		tradeTeam = scoreboard.registerNewTeam("traders");
		tradeTeam.setColor(ChatUtil.lookupChatColor(tradeColor1));
		// Peaceful team - players in another kingdom at peace
		peacefulTeam = scoreboard.registerNewTeam("peaceful");
		peacefulTeam.setColor(ChatUtil.lookupChatColor(peacefulColor1));
		// Allied team - players in another kingdom in an alliance
		alliedTeam = scoreboard.registerNewTeam("allied");
		alliedTeam.setColor(ChatUtil.lookupChatColor(alliedColor1));
		// Barbarian team - players that are barbarians (no kingdom)
		barbarianTeam = scoreboard.registerNewTeam("barbarians");
		barbarianTeam.setColor(ChatUtil.lookupChatColor(barbarianColor1));
		// Apply suffix optionally
		if(useRelationSuffix) {
			String separator = " ";
			friendlyTeam.setSuffix(separator+friendColor2+MessagePath.PLACEHOLDER_FRIENDLY.getMessage());
			enemyTeam.setSuffix(separator+enemyColor2+MessagePath.PLACEHOLDER_ENEMY.getMessage());
			tradeTeam.setSuffix(separator+tradeColor2+MessagePath.PLACEHOLDER_TRADER.getMessage());
			peacefulTeam.setSuffix(separator+peacefulColor2+MessagePath.PLACEHOLDER_PEACEFUL.getMessage());
			alliedTeam.setSuffix(separator+alliedColor2+MessagePath.PLACEHOLDER_ALLY.getMessage());
			barbarianTeam.setSuffix(separator+barbarianColor2+MessagePath.PLACEHOLDER_BARBARIAN.getMessage());
		}
	}
	
	private void initColors() {
		HashMap<CorePath,String> colorMapPrimary = new HashMap<>();
		HashMap<CorePath,String> colorMapSecondary = new HashMap<>();
		// Default colors
		colorMapPrimary.put(CorePath.COLORS_PRIMARY_FRIENDLY,		""+ChatColor.GREEN);
		colorMapPrimary.put(CorePath.COLORS_PRIMARY_ENEMY,			""+ChatColor.RED);
		colorMapPrimary.put(CorePath.COLORS_PRIMARY_TRADE,			""+ChatColor.LIGHT_PURPLE);
		colorMapPrimary.put(CorePath.COLORS_PRIMARY_PEACEFUL,		""+ChatColor.WHITE);
		colorMapPrimary.put(CorePath.COLORS_PRIMARY_ALLY,			""+ChatColor.AQUA);
		colorMapPrimary.put(CorePath.COLORS_PRIMARY_BARBARIAN,		""+ChatColor.YELLOW);
		colorMapPrimary.put(CorePath.COLORS_PRIMARY_NEUTRAL,		""+ChatColor.GRAY);
		colorMapSecondary.put(CorePath.COLORS_SECONDARY_FRIENDLY,	""+ChatColor.DARK_GREEN);
		colorMapSecondary.put(CorePath.COLORS_SECONDARY_ENEMY,		""+ChatColor.DARK_RED);
		colorMapSecondary.put(CorePath.COLORS_SECONDARY_TRADE,		""+ChatColor.DARK_PURPLE);
		colorMapSecondary.put(CorePath.COLORS_SECONDARY_PEACEFUL,	""+ChatColor.GRAY);
		colorMapSecondary.put(CorePath.COLORS_SECONDARY_ALLY,		""+ChatColor.DARK_AQUA);
		colorMapSecondary.put(CorePath.COLORS_SECONDARY_BARBARIAN,	""+ChatColor.GOLD);
		colorMapSecondary.put(CorePath.COLORS_SECONDARY_NEUTRAL,	""+ChatColor.DARK_GRAY);
		// Update default colors from config
		HashMap<CorePath,String> updateMap = new HashMap<>();
		// Parse primary colors from config
		for(CorePath colorPath : colorMapPrimary.keySet()) {
			String configColor = getCore().getString(colorPath.getPath(),"");
			// Check for named ChatColor enum (required)
			ChatColor namedColor = ChatUtil.parseColorCode(configColor);
			if(namedColor == null) {
				// Failed to match config setting to ChatColor enum
				ChatUtil.printConsoleError("Invalid ChatColor name "+colorPath+": "+configColor+", primary colors require ChatColor names.");
			} else {
				// Update color map from config using ChatColor enum
				updateMap.put(colorPath,""+namedColor);
			}
		}
		// Parse secondary colors from config
		for(CorePath colorPath : colorMapSecondary.keySet()) {
			String configColor = getCore().getString(colorPath.getPath(),"");
			// First, check for named ChatColor enum
			ChatColor namedColor = ChatUtil.parseColorCode(configColor);
			if(namedColor == null) {
				// Next, parse format codes and hex
				String formatColor = ChatUtil.parseHex(configColor);
				updateMap.put(colorPath,""+formatColor);
			} else {
				// Update color map from config using ChatColor enum
				updateMap.put(colorPath,""+namedColor);
			}
		}
		// Update color map
		for(CorePath colorPath : updateMap.keySet()) {
			if(colorMapPrimary.containsKey(colorPath)) {
				colorMapPrimary.put(colorPath,updateMap.get(colorPath));
			} else if(colorMapSecondary.containsKey(colorPath)) {
				colorMapSecondary.put(colorPath,updateMap.get(colorPath));
			}
		}
		// Assign color fields
		friendColor1         = colorMapPrimary.get(CorePath.COLORS_PRIMARY_FRIENDLY);
		friendColor2         = colorMapSecondary.get(CorePath.COLORS_SECONDARY_FRIENDLY);
		enemyColor1          = colorMapPrimary.get(CorePath.COLORS_PRIMARY_ENEMY);
		enemyColor2          = colorMapSecondary.get(CorePath.COLORS_SECONDARY_ENEMY);
		tradeColor1 		 = colorMapPrimary.get(CorePath.COLORS_PRIMARY_TRADE);
		tradeColor2 		 = colorMapSecondary.get(CorePath.COLORS_SECONDARY_TRADE);
		peacefulColor1       = colorMapPrimary.get(CorePath.COLORS_PRIMARY_PEACEFUL);
		peacefulColor2       = colorMapSecondary.get(CorePath.COLORS_SECONDARY_PEACEFUL);
		alliedColor1         = colorMapPrimary.get(CorePath.COLORS_PRIMARY_ALLY);
		alliedColor2         = colorMapSecondary.get(CorePath.COLORS_SECONDARY_ALLY);
		barbarianColor1      = colorMapPrimary.get(CorePath.COLORS_PRIMARY_BARBARIAN);
		barbarianColor2      = colorMapSecondary.get(CorePath.COLORS_SECONDARY_BARBARIAN);
		neutralColor1        = colorMapPrimary.get(CorePath.COLORS_PRIMARY_NEUTRAL);
		neutralColor2        = colorMapSecondary.get(CorePath.COLORS_SECONDARY_NEUTRAL);
	}

	private void checkCorePaths() {
		// Check all CorePath enums to ensure they have a valid core.yml path.
		for(CorePath testPath : CorePath.values()) {
			if(!getCore().contains(testPath.getPath())) {
				ChatUtil.printConsoleError("Internal error, core path "+testPath.getPath()+" does not exist within core.yml file.");
			}
		}
	}

	private void printConfigFeatures() {
		String lineTemplate = "%-30s -> %s";
		String [] status = {
				String.format(lineTemplate,"Accomplishment Prefixes",ChatUtil.boolean2enable(getCore().getBoolean(CorePath.ACCOMPLISHMENT_PREFIX.getPath()))),
				String.format(lineTemplate,"Tutorial Quests",ChatUtil.boolean2enable(getCore().getBoolean(CorePath.DIRECTIVE_QUESTS.getPath()))),
				String.format(lineTemplate,"Chat Formatting",ChatUtil.boolean2enable(getCore().getBoolean(CorePath.CHAT_ENABLE_FORMAT.getPath()))),
				String.format(lineTemplate,"Combat Tag",ChatUtil.boolean2enable(getCore().getBoolean(CorePath.COMBAT_PREVENT_COMMAND_ON_DAMAGE.getPath()))),
				String.format(lineTemplate,"Admin Kingdoms Only",ChatUtil.boolean2enable(getCore().getBoolean(CorePath.KINGDOMS_CREATE_ADMIN_ONLY.getPath()))),
				String.format(lineTemplate,"Kingdom Capital Swap",ChatUtil.boolean2enable(getCore().getBoolean(CorePath.KINGDOMS_CAPITAL_SWAP_ENABLE.getPath()))),
				String.format(lineTemplate,"Kingdom Allied Building",ChatUtil.boolean2enable(getCore().getBoolean(CorePath.KINGDOMS_ALLY_BUILD.getPath()))),
				String.format(lineTemplate,"Town Upgrades",ChatUtil.boolean2enable(getCore().getBoolean(CorePath.TOWNS_ENABLE_UPGRADES.getPath()))),
				String.format(lineTemplate,"Town Shields",ChatUtil.boolean2enable(getCore().getBoolean(CorePath.TOWNS_ENABLE_SHIELDS.getPath()))),
				String.format(lineTemplate,"Town Armor",ChatUtil.boolean2enable(getCore().getBoolean(CorePath.TOWNS_ENABLE_ARMOR.getPath()))),
				String.format(lineTemplate,"Town Specializations",ChatUtil.boolean2enable(getCore().getBoolean(CorePath.TOWNS_DISCOUNT_ENABLE.getPath()))),
				String.format(lineTemplate,"Town Plots",ChatUtil.boolean2enable(getCore().getBoolean(CorePath.PLOTS_ENABLE.getPath()))),
				String.format(lineTemplate,"Barbarian Camps",ChatUtil.boolean2enable(getCore().getBoolean(CorePath.CAMPS_ENABLE.getPath()))),
				String.format(lineTemplate,"Barbarian Clans",ChatUtil.boolean2enable(getCore().getBoolean(CorePath.CAMPS_CLAN_ENABLE.getPath())))
		};
		ChatUtil.printConsoleAlert("Feature Summary...");
		for (String row : status) {
			String line = ChatColor.GOLD+"> "+ChatColor.RESET + row;
			Bukkit.getServer().getConsoleSender().sendMessage(line);
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
		KonPlayer player;
    	// Fetch player from the database
    	// Also instantiates player object in PlayerManager
		databaseThread.getDatabase().fetchPlayerData(bukkitPlayer);
		if(!playerManager.isOnlinePlayer(bukkitPlayer)) {
			ChatUtil.printDebug("Failed to init a non-existent player!");
			return null;
		}
    	player = playerManager.getPlayer(bukkitPlayer);
    	if(player == null) {
			ChatUtil.printDebug("Failed to init a null player!");
			return null;
		}
    	ChatUtil.printDebug("Initializing Konquest player "+bukkitPlayer.getName());
    	// Update all player's nametag color packets
    	boolean isPlayerNametagFormatEnabled = getCore().getBoolean(CorePath.PLAYER_NAMETAG_FORMAT.getPath(),false);
    	if(isPlayerNametagFormatEnabled) {
    		bukkitPlayer.setScoreboard(getScoreboard());
    		updateNamePackets(player);
    	}
		// Sanity check player's memberships with all kingdoms/towns
		kingdomManager.checkPlayerMemberships(player);
    	// Update offline protections
    	kingdomManager.updateKingdomOfflineProtection();
    	campManager.deactivateCampProtection(player);
    	// Update player membership stats
    	kingdomManager.updatePlayerMembershipStats(player);
    	// Try to reset base health from legacy health upgrades
    	kingdomManager.clearTownHearts(player);
    	boolean doReset = getCore().getBoolean(CorePath.RESET_LEGACY_HEALTH.getPath(),false);
    	if(doReset) {
			Attribute maxHealth = CompatibilityUtil.getAttribute("health");
			if (maxHealth != null) {
				AttributeInstance playerHealth = bukkitPlayer.getAttribute(maxHealth);
				if (playerHealth != null) {
					double baseHealth = playerHealth.getBaseValue();
					if(baseHealth > 20) {
						playerHealth.setBaseValue(20);
					}
				}
			}
    	}
		// Force prefix title if needed
		boolean isTitleAlwaysShown = getCore().getBoolean(CorePath.CHAT_ALWAYS_SHOW_TITLE.getPath(),false);
		if(isTitleAlwaysShown) {
			player.getPlayerPrefix().setEnable(true);
		}
    	// Updates based on login position
    	Location loginLoc = bukkitPlayer.getLocation();
    	if(territoryManager.isChunkClaimed(loginLoc)) {
			KonTerritory loginTerritory = territoryManager.getChunkTerritory(loginLoc);
			if(loginTerritory instanceof KonBarDisplayer) {
				((KonBarDisplayer)loginTerritory).addBarPlayer(player);
			}
			if(loginTerritory instanceof KonRuin) {
    			((KonRuin)loginTerritory).spawnAllGolems();
    		}
    		if(loginTerritory instanceof KonTown) { 
	    		// Player joined located within a Town/Capital
	    		KonTown town = (KonTown) loginTerritory;
	    		// For enemy players, apply effects
	    		KonquestRelationshipType playerRole = kingdomManager.getRelationRole(player.getKingdom(), loginTerritory.getKingdom());
	    		if(playerRole.equals(KonquestRelationshipType.FRIENDLY)) {
	    			kingdomManager.applyTownHearts(player, town);
	    		} else {
	    			kingdomManager.clearTownHearts(player);
	    		}
	    		if(playerRole.equals(KonquestRelationshipType.ENEMY)) {
	    			kingdomManager.applyTownNerf(player, town);
	    		} else {
	    			kingdomManager.clearTownNerf(player);
	    		}
				// Send a raid alert for enemies and barbarians
				if(playerRole.equals(KonquestRelationshipType.ENEMY) || playerRole.equals(KonquestRelationshipType.BARBARIAN)) {
					town.sendRaidAlert(player);
				}
    		}
		} else {
			// Player joined located outside of a Town
			kingdomManager.clearTownNerf(player);
		}
    	territoryManager.updatePlayerBorderParticles(player);
    	ChatUtil.resetTitle(bukkitPlayer);
		return player;
	}
	
	public void save() {
		// Save config files
		sanctuaryManager.saveSanctuaries();
		kingdomManager.saveKingdoms();
		campManager.saveCamps();
		ruinManager.saveRuins();
		configManager.saveConfigs();
	}
	
	/* API Methods */
	public String getFriendlyPrimaryColor() {
		return friendColor1;
	}
	
	public String getFriendlySecondaryColor() {
		return friendColor2;
	}
	
	public String getEnemyPrimaryColor() {
		return enemyColor1;
	}
	
	public String getEnemySecondaryColor() {
		return enemyColor2;
	}
	
	public String getTradePrimaryColor() {
		return tradeColor1;
	}

	public String getTradeSecondaryColor() {
		return tradeColor2;
	}

	public String getPeacefulPrimaryColor() {
		return peacefulColor1;
	}

	public String getPeacefulSecondaryColor() {
		return peacefulColor2;
	}

	public String getAlliedPrimaryColor() {
		return alliedColor1;
	}

	public String getAlliedSecondaryColor() {
		return alliedColor2;
	}

	public String getBarbarianPrimaryColor() {
		return barbarianColor1;
	}

	public String getBarbarianSecondaryColor() {
		return barbarianColor2;
	}

	public String getNeutralPrimaryColor() {
		return neutralColor1;
	}

	public String getNeutralSecondaryColor() {
		return neutralColor2;
	}

	/* Regular Methods */
	
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

	public boolean isVersionSupported() { return isVersionSupported; }

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
	
	public SanctuaryManager getSanctuaryManager() {
		return sanctuaryManager;
	}
	
	public KingdomManager getKingdomManager() {
		return kingdomManager;
	}
	
	public TerritoryManager getTerritoryManager() {
		return territoryManager;
	}
	
	public CampManager getCampManager() {
		return campManager;
	}
	
	public ConfigManager getConfigManager() {
		return configManager;
	}
	
	public FileConfiguration getCore() {
		return configManager.getConfig("core");
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

	public ShopHandler getShopHandler() {
		return shopHandler;
	}
	
	public PlaceholderManager getPlaceholderManager() {
		return placeholderManager;
	}
	
	public PlotManager getPlotManager() {
		return plotManager;
	}
	
	public TravelManager getTravelManager() {
		return travelManager;
	}
	
	public long getOfflineTimeoutSeconds() {
		return offlineTimeoutSeconds;
	}
	
	public EventPriority getChatPriority() {
		return chatPriority;
	}

	public static String getChatTag() {
		return chatTag;
	}

	public static String getChatMessage() {
		return chatMessage;
	}

	public static String getChatDivider() {
		return chatDivider;
	}
	
	public boolean isWorldValid(Location loc) {
		if(loc != null && loc.getWorld() != null) {
			return isWorldValid(loc.getWorld());
		}
		return false;
	}

	public boolean isWorldValid(World world) {
		// A world is valid when:
		// - it's NOT in the ignoreList, and it's in the whitelist (when reverse = true)
		// - it's NOT in the ignoreList, and it's NOT in the blacklist (when reverse = false)
		if(world != null) {
			boolean isWorldInBlacklist = worlds.contains(world);
			boolean isWorldInIgnorelist = ignoredWorlds.contains(world);
			if (isWhitelist) {
				return !isWorldInIgnorelist && isWorldInBlacklist;
			} else {
				return !isWorldInIgnorelist && !isWorldInBlacklist;
			}
		}
		return false;
	}
	
	public boolean isWorldIgnored(Location loc) {
		if(loc != null && loc.getWorld() != null) {
			return isWorldIgnored(loc.getWorld());
		}
		return true;
	}
	
	public boolean isWorldIgnored(World world) {
		if(world != null) {
			return ignoredWorlds.contains(world);
		}
		return true;
	}
	
	/**
	 * Checks for name conflicts and constraints for all namable objects
	 * @param name - The name of an object (town, ruin, etc)
	 * @return Status code
	 * 			0 - Success, no issue found
	 * 			1 - Error, name is not strictly alpha-numeric
	 * 			2 - Error, name has more than 20 characters
	 * 			3 - Error, name is an existing player
	 * 			4 - Error, name is a kingdom
	 * 			5 - Error, name is a town
	 * 			6 - Error, name is a ruin
	 * 			7 - Error, name is a guild [deprecated]
	 * 			8 - Error, name is a sanctuary
	 * 			9 - Error, name is a template
	 * 			10 - Error, name is reserved word
	 */
	public int validateNameConstraints(String name) {
		if(name == null || name.equals("") || name.contains(" ") || !StringUtils.isAlphanumeric(name.replace("_",""))) {
			return 1;
    	}
    	if(name.length() > 20) {
    		return 2;
    	}
    	if(playerManager.isPlayerNameExist(name)) {
    		return 3;
    	}
		if(kingdomManager.isKingdom(name)) {
			return 4;
		}
		for(KonKingdom kingdom : kingdomManager.getKingdoms()) {
			if(kingdom.hasTown(name)) {
				return 5;
			}
		}
		if(ruinManager.isRuin(name)) {
			return 6;
		}
		if(sanctuaryManager.isSanctuary(name)) {
			return 8;
		}
		if(sanctuaryManager.isTemplate(name)) {
			return 9;
		}
		for(TravelDestination keyword : TravelDestination.values()) {
			if(name.equalsIgnoreCase(keyword.toString())) {
				return 10;
			}
		}
		List<String> reservedWords = new ArrayList<>();
		reservedWords.add("konquest");
		reservedWords.add("kingdom");
		reservedWords.add("town");
		reservedWords.add("camp");
		reservedWords.add("ruin");
		reservedWords.add("sanctuary");
		reservedWords.add("templates");
		reservedWords.add("template");
		reservedWords.add("monument");
		reservedWords.add("all");
		for(String word : reservedWords) {
			if(name.equalsIgnoreCase(word)) {
				return 10;
			}
		}
		return 0;
	}
	
	public int validateName(String name, CommandSender sender) {
		int result = validateNameConstraints(name);
		if(sender != null) {
			if(result == 1) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_FORMAT_NAME.getMessage());
			} else if(result == 2) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_LENGTH_NAME.getMessage());
			} else if(result >= 3) {
				ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_TAKEN_NAME.getMessage());
			}
		}
		return result;
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
				for(KonOfflinePlayer player : playerManager.getAllKonquestOfflinePlayers()) {
					long lastPlayedTime = player.getOfflineBukkitPlayer().getLastPlayed();
					if(lastPlayedTime > 0 && now.after(new Date(lastPlayedTime + (offlineTimeoutSeconds*1000)))) {
						// Offline player has exceeded timeout period, prune from residencies and camp
						boolean doExile = getCore().getBoolean(CorePath.KINGDOMS_OFFLINE_TIMEOUT_EXILE.getPath(),false);
						if(!player.isBarbarian()) {
							if(doExile) {
								UUID id = player.getOfflineBukkitPlayer().getUniqueId();
								// Forced full exile
								String kingdomName = player.getKingdom().getName();
								int status = getKingdomManager().exilePlayerBarbarian(id,false,false,true,true);
								ChatUtil.printDebug("Pruned player "+player.getOfflineBukkitPlayer().getName()+" by exile from kingdom "+kingdomName+", status "+status);
							} else {
								for(KonTown town : player.getKingdom().getTowns()) {
									if(town.getPlayerResidents().contains(player.getOfflineBukkitPlayer())) {
										boolean status = town.removePlayerResident(player.getOfflineBukkitPlayer());
										ChatUtil.printDebug("Pruned player "+player.getOfflineBukkitPlayer().getName()+" from town "+town.getName()+" in kingdom "+player.getKingdom().getName()+", status "+status);
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
	    			List<KonKingdom> enemyKingdoms = player.getKingdom().getActiveRelationKingdoms(KonquestDiplomacyType.WAR);
	    			KonTerritory nearestTerritory = null;
	    			int minDistance = Integer.MAX_VALUE;
	    			for(KonKingdom kingdom : enemyKingdoms) {
	    				for(KonTown town : kingdom.getCapitalTowns()) {
	    					// Only find enemy towns which do not have the counter-intelligence upgrade level 2+
	    					int upgradeLevel = upgradeManager.getTownUpgradeLevel(town, KonUpgrade.COUNTER);
	    					if(upgradeLevel < 2) {
	    						int townDist = HelperUtil.chunkDistance(player.getBukkitPlayer().getLocation(), town.getCenterLoc());
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
		} else if(taskID == pingTimer.getTaskID()) {
			boolean status = databaseThread.getDatabase().getDatabaseConnection().pingDatabase();
			if(status) {
				ChatUtil.printDebug("Database ping success!");
			}
		}
	}
	
	// This can return null!
	public Location getRandomWildLocation(World world) {
		Location wildLoc = null;
		final int MAX_ATTEMPTS = 100;
		int radius = getCore().getInt(CorePath.TRAVEL_WILD_RADIUS.getPath(),500);
		int offsetX = getCore().getInt(CorePath.TRAVEL_WILD_CENTER_X.getPath(),0);
		int offsetZ = getCore().getInt(CorePath.TRAVEL_WILD_CENTER_Z.getPath(),0);
		radius = radius > 0 ? radius : 2;
		ChatUtil.printDebug("Generating random wilderness location at center "+offsetX+","+offsetZ+" in radius "+radius);
		int randomNumX;
		int randomNumZ;
		int randomNumY;
		boolean foundValidLoc = false;
		int timeout = 0;
		// Metrics for criteria checking
		int numClaimed = 0;
		int numWater = 0;
		while(!foundValidLoc) {
			if(timeout > MAX_ATTEMPTS) {
				ChatUtil.printDebug("Failed to get a random wilderness location. Claimed attempts: "+numClaimed+"; Water attempts: "+numWater);
				return null;
			}
			// Generate new location
			randomNumX = ThreadLocalRandom.current().nextInt(-1*(radius), (radius) + 1) + offsetX;
			randomNumZ = ThreadLocalRandom.current().nextInt(-1*(radius), (radius) + 1) + offsetZ;
			Block randomBlock = world.getHighestBlockAt(randomNumX,randomNumZ);
			randomNumY = randomBlock.getY() + 2;
			wildLoc = new Location(world, randomNumX, randomNumY, randomNumZ);
			// Check for valid location criteria
			if(territoryManager.isChunkClaimed(wildLoc)) {
				// This location is claimed
				numClaimed++;
				timeout++;
				continue;
			}
			if(!randomBlock.getType().isSolid()) {
				// This location is liquid
				numWater++;
				timeout++;
				continue;
			}
			// Passed all checks
			foundValidLoc = true;
		}
		ChatUtil.printDebug("Got wilderness location "+wildLoc.getX()+","+wildLoc.getY()+","+wildLoc.getZ()+". Claimed attempts: "+numClaimed+"; Water attempts: "+numWater);
		return wildLoc;
	}
	
	/**
	 * Gets a random location in a square chunk area, excluding the center chunk
	 * @param center A block centered location
	 * @param radius a radius
	 * @return random location within radius of the center
	 */
	public Location getSafeRandomCenteredLocation(Location center, int radius) {
		Location randLoc = null;
		ChatUtil.printDebug("Generating random centered location for radius "+radius);
		int randomChunkIdx;
		int randomNumX;
		int randomNumZ;
		int randomNumY;
		boolean foundValidLoc = false;
		int timeout = 0;
		ArrayList<Chunk> chunkList = HelperUtil.getSurroundingChunks(center, radius);
		while(!foundValidLoc) {
			randomChunkIdx = ThreadLocalRandom.current().nextInt(0, chunkList.size());
			randomNumX = ThreadLocalRandom.current().nextInt(0, 16);
			randomNumZ = ThreadLocalRandom.current().nextInt(0, 16);
			randomNumY = chunkList.get(randomChunkIdx).getChunkSnapshot(true,false,false).getHighestBlockYAt(randomNumX, randomNumZ);
			Block randBlock = chunkList.get(randomChunkIdx).getBlock(randomNumX, randomNumY, randomNumZ);
			Block randBlockDown = chunkList.get(randomChunkIdx).getBlock(randomNumX, randomNumY-1, randomNumZ);
			randLoc = randBlock.getLocation();
			randLoc.add(0.5,2,0.5);
			ChatUtil.printDebug("Checking block material target: "+ randBlock.getType());
			ChatUtil.printDebug("Checking block material down: "+ randBlockDown.getType());
			if(!randBlockDown.getType().equals(Material.LAVA) && randBlockDown.getType().isSolid()) {
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
     * @param player The player to send the packets to
     */
    public void updateNamePackets(KonPlayer player) {
    	if(!isVersionHandlerEnabled) {
    		return;
    	}
    	// Loop over all online players, populate team lists and send each online player a team packet for arg player
    	// Send arg player packets for each team with lists of online players
		List<String> friendlyNames = new ArrayList<>();
		List<String> enemyNames = new ArrayList<>();
		List<String> tradeNames = new ArrayList<>();
		List<String> peacefulNames = new ArrayList<>();
		List<String> alliedNames = new ArrayList<>();
		List<String> barbarianNames = new ArrayList<>();
		Team onlinePacketTeam;
    	for(KonPlayer onlinePlayer : playerManager.getPlayersOnline()) {
    		// Place online player in appropriate list w.r.t. player
			KonquestRelationshipType otherPlayerRole = kingdomManager.getRelationRole(player.getKingdom(),onlinePlayer.getKingdom());
			switch(otherPlayerRole) {
				case BARBARIAN:
					barbarianNames.add(onlinePlayer.getBukkitPlayer().getName());
					break;
				case FRIENDLY:
					friendlyNames.add(onlinePlayer.getBukkitPlayer().getName());
					break;
				case ENEMY:
					enemyNames.add(onlinePlayer.getBukkitPlayer().getName());
					break;
				case TRADE:
					tradeNames.add(onlinePlayer.getBukkitPlayer().getName());
					break;
				case ALLY:
					alliedNames.add(onlinePlayer.getBukkitPlayer().getName());
					break;
				default:
					// Assumed peaceful (default)
					peacefulNames.add(onlinePlayer.getBukkitPlayer().getName());
					break;
			}
    		// Determine appropriate team for player w.r.t. online player
			KonquestRelationshipType thisPlayerRole = kingdomManager.getRelationRole(onlinePlayer.getKingdom(),player.getKingdom());
			switch(thisPlayerRole) {
				case BARBARIAN:
					onlinePacketTeam = barbarianTeam;
					break;
				case FRIENDLY:
					onlinePacketTeam = friendlyTeam;
					break;
				case ENEMY:
					onlinePacketTeam = enemyTeam;
					break;
				case TRADE:
					onlinePacketTeam = tradeTeam;
					break;
				case ALLY:
					onlinePacketTeam = alliedTeam;
					break;
				default:
					// Assumed peaceful (default)
					onlinePacketTeam = peacefulTeam;
					break;
			}
    		// Send appropriate team packet to online player
			boolean status = versionHandler.sendPlayerTeamPacket(onlinePlayer.getBukkitPlayer(), Collections.singletonList(player.getBukkitPlayer().getName()), onlinePacketTeam);
			if(!status) ChatUtil.printConsoleError("Failed to send Team Update packet, make sure ProtocolLib is updated and works for this Minecraft version.");
    	}
    	// Send packets to player
    	if(!barbarianNames.isEmpty()) {
    		versionHandler.sendPlayerTeamPacket(player.getBukkitPlayer(), barbarianNames, barbarianTeam);
    	}
    	if(!friendlyNames.isEmpty()) {
    		versionHandler.sendPlayerTeamPacket(player.getBukkitPlayer(), friendlyNames, friendlyTeam);
    	}
    	if(!enemyNames.isEmpty()) {
    		versionHandler.sendPlayerTeamPacket(player.getBukkitPlayer(), enemyNames, enemyTeam);
    	}
    	if(!tradeNames.isEmpty()) {
    		versionHandler.sendPlayerTeamPacket(player.getBukkitPlayer(), tradeNames, tradeTeam);
    	}
    	if(!alliedNames.isEmpty()) {
    		versionHandler.sendPlayerTeamPacket(player.getBukkitPlayer(), alliedNames, alliedTeam);
    	}
    	if(!peacefulNames.isEmpty()) {
    		versionHandler.sendPlayerTeamPacket(player.getBukkitPlayer(), peacefulNames, peacefulTeam);
    	}
    }

	// Updates the name packets for all online players in a kingdom
    public void updateNamePackets(KonKingdom kingdom) {
    	for(KonPlayer player : playerManager.getPlayersInKingdom(kingdom)) {
			updateNamePackets(player);
		}
    }

    public void telePlayerLocation(Player player, Location travelLocation) {
    	Point locPoint = HelperUtil.toPoint(travelLocation);
		Location destination = new Location(travelLocation.getWorld(), travelLocation.getBlockX()+0.5, travelLocation.getBlockY()+0.5, travelLocation.getBlockZ()+0.5, travelLocation.getYaw(), travelLocation.getPitch());
    	if(travelLocation.getWorld().isChunkLoaded(locPoint.x,locPoint.y)) {
    		ChatUtil.printDebug("Teleporting player "+player.getName()+" to loaded chunk");
    		player.teleport(destination,TeleportCause.PLUGIN);
    		playTravelSound(player);
    	} else {
    		teleportLocationQueue.put(player,destination);
    		ChatUtil.printDebug("Queueing player "+player.getName()+" for unloaded chunk destination");
    		travelLocation.getWorld().loadChunk(locPoint.x,locPoint.y);
    	}
    }

    public void applyQueuedTeleports(Chunk chunk) {
    	Point cPoint = HelperUtil.toPoint(chunk);
    	Point qPoint;
		Location qLoc;
    	if(!teleportLocationQueue.isEmpty()) {
	    	for(Player qPlayer : teleportLocationQueue.keySet()) {
	    		qLoc = teleportLocationQueue.get(qPlayer);
	    		qPoint = HelperUtil.toPoint(qLoc);
	    		if(qPoint.equals(cPoint) && chunk.getWorld().equals(qLoc.getWorld())) {
	    			//Location destination = new Location(qLoc.getWorld(),qLoc.getBlockX()+0.5,qLoc.getBlockY()+1.0,qLoc.getBlockZ()+0.5,qLoc.getYaw(),qLoc.getPitch());
	    			qPlayer.teleport(qLoc,TeleportCause.PLUGIN);
	    			playTravelSound(qPlayer);
	    			ChatUtil.printDebug("Teleporting chunk queued player "+qPlayer.getName());
	    			teleportLocationQueue.remove(qPlayer);
	    		}
	    	}
    	}
    }
    
    public ItemStack getPlayerHead(OfflinePlayer bukkitOfflinePlayer) {
		if(!headCache.containsKey(bukkitOfflinePlayer.getUniqueId())) {
    		ChatUtil.printDebug("Missing "+bukkitOfflinePlayer.getName()+" player head in the cache, creating...");
    		ItemStack item = new ItemStack(Material.PLAYER_HEAD);
    		SkullMeta meta = (SkullMeta)item.getItemMeta();
        	meta.setOwningPlayer(bukkitOfflinePlayer);
    		item.setItemMeta(meta);
    		headCache.put(bukkitOfflinePlayer.getUniqueId(),item);
    		return item;
    	} else {
    		return headCache.get(bukkitOfflinePlayer.getUniqueId());
    	}
    }

	/*
	 * Color Methods
	 */

    public String getDisplayPrimaryColor(KonquestKingdom displayKingdom, KonquestKingdom contextKingdom) {
    	String result = neutralColor1;
		KonquestRelationshipType role = kingdomManager.getRelationRole(displayKingdom,contextKingdom);
    	switch(role) {
	    	case BARBARIAN:
	    		result = barbarianColor1;
	    		break;
	    	case NEUTRAL:
	    		result = neutralColor1;
	    		break;
	    	case ENEMY:
	    		result = enemyColor1;
	    		break;
	    	case FRIENDLY:
	    		result = friendColor1;
	    		break;
	    	case ALLY:
	    		result = alliedColor1;
	    		break;
	    	case TRADE:
	    		result = tradeColor1;
	    		break;
	    	case PEACEFUL:
	    		result = peacefulColor1;
	    		break;
    		default:
    			break;
    	}
    	return result;
    }
    
    public String getDisplayPrimaryColor(KonquestOfflinePlayer displayPlayer, KonquestOfflinePlayer contextPlayer) {
    	return getDisplayPrimaryColor(displayPlayer.getKingdom(),contextPlayer.getKingdom());
    }
    
    public String getDisplayPrimaryColor(KonquestOfflinePlayer displayPlayer, KonquestTerritory contextTerritory) {
    	return getDisplayPrimaryColor(displayPlayer.getKingdom(),contextTerritory.getKingdom());
    }
    
    public String getDisplaySecondaryColor(KonquestKingdom displayKingdom, KonquestKingdom contextKingdom) {
    	String result = neutralColor2;
		KonquestRelationshipType role = kingdomManager.getRelationRole(displayKingdom,contextKingdom);
    	switch(role) {
	    	case BARBARIAN:
	    		result = barbarianColor2;
	    		break;
	    	case NEUTRAL:
	    		result = neutralColor2;
	    		break;
	    	case ENEMY:
	    		result = enemyColor2;
	    		break;
	    	case FRIENDLY:
	    		result = friendColor2;
	    		break;
	    	case ALLY:
	    		result = alliedColor2;
	    		break;
	    	case TRADE:
	    		result = tradeColor2;
	    		break;
	    	case PEACEFUL:
	    		result = peacefulColor2;
				break;
    		default:
    			break;
    	}
    	return result;
    }
    
    public String getDisplaySecondaryColor(KonquestOfflinePlayer displayPlayer, KonquestOfflinePlayer contextPlayer) {
    	return getDisplaySecondaryColor(displayPlayer.getKingdom(),contextPlayer.getKingdom());
    }
    
    public String getDisplaySecondaryColor(KonquestOfflinePlayer displayPlayer, KonquestTerritory contextTerritory) {
    	return getDisplaySecondaryColor(displayPlayer.getKingdom(),contextTerritory.getKingdom());
    }

	/*
	 * Sound Methods
	 */
    
    public static void playSuccessSound(Player bukkitPlayer) {
    	Bukkit.getScheduler().scheduleSyncDelayedTask(instance.getPlugin(),
				() -> bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, (float)1.0, (float)1.3),1);
    	Bukkit.getScheduler().scheduleSyncDelayedTask(instance.getPlugin(),
				() -> bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, (float)1.0, (float)1.7),4);
    }
    
    public static void playFailSound(Player bukkitPlayer) {
    	Bukkit.getScheduler().scheduleSyncDelayedTask(instance.getPlugin(),
				() -> bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_GLASS_BREAK, (float)0.5, (float)1.4),1);
    }
    
    public static void playDiscountSound(Player bukkitPlayer) {
    	Bukkit.getScheduler().scheduleSyncDelayedTask(instance.getPlugin(),
				() -> bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, (float)1.0, (float)1.0),1);
    }
    
    public static void playTownArmorSound(Player bukkitPlayer) {
    	playTownArmorSound(bukkitPlayer.getLocation());
    }
    
    public static void playTownArmorSound(Location loc) {
    	loc.getWorld().playSound(loc, Sound.ENTITY_SHULKER_SHOOT, (float)1.0, (float)2);
    }

	public static void playTownSettleSound(Location loc) {
		loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_USE, (float)0.5, (float)1);
	}
    
    public static void playCampGroupSound(Location loc) {
    	loc.getWorld().playSound(loc, Sound.BLOCK_FENCE_GATE_OPEN, (float)1.0, (float)0.7);
    }
    
    public static void playTravelSound(Player bukkitPlayer) {
    	Bukkit.getScheduler().scheduleSyncDelayedTask(instance.getPlugin(),
				() -> bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.ENTITY_EGG_THROW, (float)1.0, (float)0.1),1);
    }

	/*
	 * Events and Commands
	 */
    
    public static EventPriority getEventPriority(String priority) {
    	EventPriority result = defaultChatPriority;
    	if(priority != null) {
    		try {
    			result = EventPriority.valueOf(priority.toUpperCase());
    		} catch(IllegalArgumentException ignored) {}
    	}
    	return result;
    }

    public static void callKonquestEvent(KonquestEvent event) {
    	if(event != null) {
	    	try {
	            Bukkit.getServer().getPluginManager().callEvent(event);
			} catch(IllegalStateException e) {
				ChatUtil.printConsoleError("Failed to call Konquest event!");
				e.printStackTrace();
			}
    	} else {
    		ChatUtil.printDebug("Could not call null Konquest event");
    	}
    }

	public void executeCustomCommand(CustomCommandPath command, Player bukkitPlayer) {
		// Get custom command
		String commandPath = command.getPath();
		FileConfiguration customCommandConfig = configManager.getConfig("commands");
		String customCommand = customCommandConfig.getString(commandPath,"");
		// Check for empty string
		ChatUtil.printDebug("Running command \""+customCommand+"\" for "+commandPath);
		if (!customCommand.isEmpty()) {
			// Replace tags in command string
			customCommand = customCommand.replace("%PLAYER%", bukkitPlayer.getName());
			// Execute the command as console
			try {
				boolean result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), customCommand);
				if (!result) {
					ChatUtil.printConsoleWarning("Could not run command \""+customCommand+"\" from commands.yml entry "+commandPath);
					// Check for leading forward slash
					if (customCommand.matches("^/.+")) {
						ChatUtil.printConsoleWarning("Custom command starts with a forward slash \"/\". Remove the slash from the command in commands.yml.");
					}
				}
			} catch (CommandException me) {
				ChatUtil.printConsoleError("Failed to execute custom command \""+customCommand+"\" from commands.yml entry "+commandPath);
				ChatUtil.printConsole(me.getMessage());
				me.printStackTrace();
			}
		}
	}
	
}
