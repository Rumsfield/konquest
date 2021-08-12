package konquest.manager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

import org.bukkit.configuration.file.FileConfiguration;

import konquest.Konquest;
import konquest.model.KonConfig;
import konquest.utility.ChatUtil;

public class ConfigManager{
	
	private Konquest konquest;
	private HashMap<String, KonConfig> configCache;
	private String language;
	private FileConfiguration langConfig;
	
	public ConfigManager(Konquest konquest) {
		this.konquest = konquest;
        this.configCache = new HashMap<String, KonConfig>();
        this.language = "english";
	}
        
	public void initialize() {
		// Config Settings
		addConfig("core", new KonConfig("core"));
		updateConfigVersion("core");
		addConfig("upgrades", new KonConfig("upgrades"));
		updateConfigVersion("upgrades");
		addConfig("shields", new KonConfig("shields"));
		addConfig("loot", new KonConfig("loot"));
		// Data Storage
		migrateConfigFile("kingdoms.yml","data/kingdoms.yml");
		migrateConfigFile("camps.yml","data/camps.yml");
		migrateConfigFile("ruins.yml","data/ruins.yml");
		addConfig("kingdoms", new KonConfig("data/kingdoms"));
		addConfig("camps", new KonConfig("data/camps"));
		addConfig("ruins", new KonConfig("data/ruins"));
		// Language files
		addConfig("lang_english", new KonConfig("lang/english"));
		updateConfigVersion("lang_english");
		// Language selection
		language = getConfig("core").getString("language","english");
		if(configCache.containsKey("lang_"+language)) {
			// Use a built-in language file
			langConfig = getConfig("lang_"+language);
			ChatUtil.printConsoleAlert("Using "+language+" language file");
		} else {
			// Attempt to find a custom local language file
			if(addConfig("lang_custom", new KonConfig("lang/"+language))) {
				langConfig = getConfig("lang_custom");
				ChatUtil.printConsoleAlert("Using custom "+language+" language file");
			} else {
				langConfig = getConfig("lang_english");
				ChatUtil.printConsoleError("Failed to load invalid language file "+language+".yml in Konquest/lang folder. Using default lang/english.yml.");
			}
		}
	}
	
	public FileConfiguration getLang() {
		return langConfig;
	}
	
	public FileConfiguration getConfig(String key) {
		if(!configCache.containsKey(key)) {
			ChatUtil.printConsoleError("Failed to find non-existant config "+key);
		}
		return configCache.get(key).getConfig();
	}
	
	public boolean addConfig(String key, KonConfig config) {
		boolean status = false;
		if(config.saveDefaultConfig()) {
			if(config.reloadConfig()) {
				configCache.put(key, config);
				status = true;
			}
		}
		return status;
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
			ChatUtil.printConsoleError("Tried to save non-existant config "+name);
		}
	}
	
	public void updateConfigVersion(String name) {
		if(configCache.containsKey(name)) {
			configCache.get(name).updateVersion();
		} else {
			ChatUtil.printConsoleError("Tried to update non-existant config "+name);
		}
	}
	
	public void overwriteBadConfig(String key) {
		configCache.get(key).saveNewConfig();
		configCache.get(key).reloadConfig();
		ChatUtil.printConsoleError("Bad config file \""+key+"\", saved default version. Review this file for errors.");
	}
	
	private void migrateConfigFile(String oldPath, String newpath) {
		File oldFile = new File(Konquest.getInstance().getPlugin().getDataFolder(), oldPath);
		File newFile = new File(Konquest.getInstance().getPlugin().getDataFolder(), newpath);
		if(oldFile.exists()) {
			Path source = oldFile.toPath();
			Path destination = newFile.toPath();
			try {
				Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
				oldFile.delete();
				ChatUtil.printConsoleAlert("Migrated data file "+oldPath+" to "+newpath);
			} catch (IOException e) {
				e.printStackTrace();
				ChatUtil.printDebug("Failed to move file "+oldPath+" to "+newpath);
			}
			/*
			if(oldFile.renameTo(newFile)) {
				oldFile.delete();
				ChatUtil.printConsoleAlert("Migrated data file "+oldPath+" to "+newpath);
			} else {
				ChatUtil.printDebug("Failed to rename file "+oldPath+" to "+newpath);
			}
			*/
		}
	}

}
