package konquest.manager;

import java.util.HashMap;

import org.bukkit.configuration.file.FileConfiguration;

import konquest.Konquest;
import konquest.model.KonConfig;
import konquest.utility.ChatUtil;

public class ConfigManager{
	
	private Konquest konquest;
	private HashMap<String, KonConfig> configCache;
	
	public ConfigManager(Konquest konquest) {
		this.konquest = konquest;
        this.configCache = new HashMap<String, KonConfig>();
	}
        
	public void initialize() {
		addConfig("core", new KonConfig("core"));
		updateConfigVersion("core");
		addConfig("upgrades", new KonConfig("upgrades"));
		updateConfigVersion("upgrades");
		addConfig("camps", new KonConfig("camps"));
		addConfig("kingdoms", new KonConfig("kingdoms"));
		addConfig("ruins", new KonConfig("ruins"));
		
        //System.out.println("[Konquest]: Debug is "+getConfig("core").getBoolean("core.debug"));
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
	
	public void updateConfigVersion(String name) {
		if(configCache.containsKey(name)) {
			configCache.get(name).updateVersion();
		} else {
			ChatUtil.printDebug("ERROR: Tried to update non-existant config "+name);
		}
	}
	
	public void overwriteBadConfig(String key) {
		configCache.get(key).saveNewConfig();
		configCache.get(key).reloadConfig();
		ChatUtil.printConsoleError("Bad config file \""+key+"\", saved default version. Review this file for errors.");
	}

}
