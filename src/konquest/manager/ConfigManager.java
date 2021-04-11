package konquest.manager;

import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import konquest.Konquest;
import konquest.model.KonConfig;
import konquest.utility.ChatUtil;

public class ConfigManager{
	
	private Konquest konquest;
	private HashMap<String, KonConfig> configCache;
	private String coreHeader;
	
	public ConfigManager(Konquest konquest) {
		this.konquest = konquest;
        this.configCache = new HashMap<String, KonConfig>();
        this.coreHeader = 	"Core Konquest settings\r\n" + 
			        		"core:\r\n" + 
			        		"  debug:                           Enable debug messages (true/false)\r\n" + 
			        		"  world_name:                      Primary world name (string)\r\n" + 
			        		"  save_interval:                   Save interval in minutes, 0 to disable (integer >= 0)\r\n" + 
			        		"  integration:\r\n" +
			        		"    quickshop:						Enable plugin integration with QuickShop (true/false)\r\n" + 
			        		"    combatlogx:					Enable plugin integration with CombatLogX (CURRENTLY NOT SUPPORTED) (true/false)\r\n" + 
			        		"    essentials:					Enable plugin integration with Essentials (CURRENTLY NOT SUPPORTED) (true/false)\r\n" + 
			        		"    luckperms:						Enable plugin integration with LuckPerms (true/false)\r\n" + 
			        		"  \r\n" + 
			        		"  kingdoms:\r\n" + 
			        		"    capital_suffix:                Suffix to be appended to Kingdom names which will be the name of the Capital city (string)\r\n" + 
			        		"    capital_pvp:                   Allow player damage in capitals (true/false)\r\n" + 
			        		"    protect_containers_use:        Prevent players in other kingdoms using containers in claimed land (true/false)\r\n" + 
			        		"    protect_containers_break:      Prevent players in other kingdoms breaking containers in claimed land (true/false)\r\n" + 
			        		"    no_enemy_enter:                Prevent enemy players from entering Kingdom Capitals (true/false)\r\n" + 
			        		"    no_enemy_travel:               Prevent enemy players from traveling within Kingdom territory (true/false)\r\n" + 
			        		"    no_enemy_edit_offline:         Prevent enemy players from breaking any blocks in a Kingdom with no players online (true/false)\r\n" + 
			        		"    no_enemy_edit_offline_warmup:  Time in seconds from when the last online player in a Kingdom disconnects to when that Kingdom becomes protected from enemy raids/edits (0 to disable, integer >= 0)\r\n" + 
			        		"    smallest_exp_boost_percent:    Percentage boost EXP gain for the smallest kingdom (0 to disable, 0 - 100)\r\n" + 
			        		"    offline_timeout_seconds:       Time in seconds a player must be offline to be removed from their Kingdom (0 to disable, integer >= 0)\r\n" + 
			        		"    golem_attack_enemies:          Force iron golems to attack enemy players (true/false)\r\n" + 
			        		"    max_player_diff:               Kingdoms with this many players more than other Kingdoms will not accept new players (0 to disable, integer >= 0)\r\n" + 
			        		"\r\n" + 
			        		"  towns:\r\n" + 
			        		"    min_settle_height:             Minimum height level for new town settlements (0 to disable, 0 - 256)\r\n" + 
			        		"    max_settle_height:             Maximum height level for new town settlements (0 to disable, 0 - 256), Note, if max_settle_height is less than min_settle_height, things might break!\r\n" + 
			        		"    min_distance_town:             Minimum distance in chunks between Towns and other Towns (integer >= 0)\r\n" + 
			        		"    min_distance_capital:          Minimum distance in chunks between Towns and Capitals (integer >= 0)\r\n" + 
			        		"    init_radius:                   Radius of initial settlements. Initial chunk area will be (2r-1)^2 chunks squared (integer >= 0)\r\n" + 
			        		"    capture_cooldown:              Time in seconds before a town can be captured again (integer >= 0)\r\n" + 
			        		"    raid_alert_cooldown:           Time in seconds between raid alerts for Towns (integer >= 0)\r\n" + 
			        		"    travel_cooldown:               Time in seconds for travel cool-down (integer >= 0)\r\n" + 
			        		"\r\n" + 
			        		"  camps:\r\n" + 
			        		"    init_radius:                   Radius of camps for barbarians. Initial chunk area will be (2r-1)^2 chunks squared (integer >= 0)\r\n" + 
			        		"\r\n" + 
			        		"  monuments:\r\n" + 
			        		"    critical_block:                Blocks which must be destroyed to capture a town monument (Material, https://minecraft-ids.grahamedgecombe.com/, e.g. for obsidian, enum is minecraft:obsidian, use OBSIDIAN)\r\n" + 
			        		"    destroy_amount:                Amount of each destroy_blocks that must be destroyed to capture a town monument (integer >= 0)\r\n" + 
			        		"    damage_regen:                  The time in seconds it takes a damaged monument to regenerate after being attacked (integer >= 0)\r\n" + 
			        		"	 loot_refresh:                  The time in seconds for a loot chest within a monument to refill (integer >= 0)\r\n" + 
			        		"	 loot_count:					The number of randomly generated loot items to refill a monument loot chest with (integer >= 0)\r\n" + 
			        		"  \r\n" + 
			        		"  favor:\r\n" + 
			        		"    cost_spy:                      Cost to spawn a new spy map (0 for free, integer >= 0)\r\n" + 
			        		"    cost_settle:                   Cost to settle a new Town (0 for free, integer >= 0)\r\n" + 
			        		"    cost_settle_increment:         Cost increment for each new town settled (0 for free, integer >= 0)\r\n" + 
			        		"    cost_rename:                   Cost to rename a town (0 for free, integer >= 0)\r\n" + 
			        		"    cost_claim:                    Cost to claim land (0 for free, integer >= 0)\r\n" + 
			        		"    cost_travel:                   Cost to travel (0 for free, integer >= 0)\r\n" + 
			        		"";
	}
	
	public void initialize() {
		addConfig("core", new KonConfig("core.yml"));
		configCache.get("core").applyHeader(coreHeader);
		addConfig("upgrades", new KonConfig("upgrades.yml"));
		addConfig("camps", new KonConfig("camps.yml"));
		addConfig("kingdoms", new KonConfig("kingdoms.yml"));
		addConfig("ruins", new KonConfig("ruins.yml"));
		
        System.out.println("[DEBUG]: Debug is "+getConfig("core").getBoolean("core.debug"));
	}
	
	public FileConfiguration getConfig(String key) {
		return configCache.get(key).getConfig();
	}
	
	public void addConfig(String key, KonConfig config) {
		config.saveDefaultConfig();
		config.reloadConfig();
		configCache.put(key, config);
	}
	
	public HashMap<String, KonConfig> getConfigCache() {
		return configCache;
	}
	
	public Konquest getKonquest() {
		return konquest;
	}
	
	public void reloadConfigs() {
		for (KonConfig config : configCache.values()) {
			config.reloadConfig();
		}
	}
	
	public void saveConfigs() {
		for (KonConfig config : configCache.values()) {
			config.saveConfig();
		}
	}
	
	public void saveConfig(String name) {
		if(configCache.containsKey(name)) {
			configCache.get(name).saveConfig();
		} else {
			ChatUtil.printDebug("ERROR: Tried to save non-existant config "+name);
		}
	}
	
	public void overwriteBadConfig(String key) {
		configCache.get(key).saveNewConfig();
		Konquest.getInstance().getPlugin().getServer().getConsoleSender().sendMessage(ChatColor.GOLD+"[Konquest] Error: Bad config file \""+key+"\", saved default version");
	}

}
