package com.github.rumsfield.konquest.manager;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.model.KonConfig;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.Version;
import com.github.rumsfield.konquest.utility.ZipUtility;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public class ConfigManager{
	
	private final Konquest konquest;
	private final HashMap<String, KonConfig> configCache;
	private String language;
	private FileConfiguration langConfig;
	
	public ConfigManager(Konquest konquest) {
		this.konquest = konquest;
        this.configCache = new HashMap<>();
        this.language = "english";
	}

	/**
	 * Loads all config and data files.
	 * Handles migrations and updates from older versions.
	 * When an older version has incompatible data files, zip up the plugins/Konquest folder
	 * as an archive and proceed with plugin startup.
	 */
	public void initialize() {
		// Config Settings - unsaved
		addConfig("core", new KonConfig("core",false));
		createBackupData();
		checkIncompatibleUpdate();
		updateConfigVersion("core");
		addConfig("upgrades", new KonConfig("upgrades",false));
		updateConfigVersion("upgrades");
		addConfig("properties", new KonConfig("properties",false));
		updateConfigVersion("properties");
		addConfig("shields", new KonConfig("shields",false));
		updateConfigVersion("shields");
		addConfig("loot", new KonConfig("loot",false));
		updateConfigVersion("loot");
		addConfig("prefix", new KonConfig("prefix",false));
		updateConfigVersion("prefix");
		addConfig("commands", new KonConfig("commands",false));
		updateConfigVersion("commands");

		// Data Storage
		migrateConfigFile("kingdoms.yml","data/kingdoms.yml");
		migrateConfigFile("camps.yml","data/camps.yml");
		migrateConfigFile("ruins.yml","data/ruins.yml");
		addConfig("kingdoms", new KonConfig("data/kingdoms"));
		addConfig("camps", new KonConfig("data/camps"));
		addConfig("ruins", new KonConfig("data/ruins"));
		addConfig("sanctuaries", new KonConfig("data/sanctuaries"));

		// Backup Readme
		Konquest.getInstance().getPlugin().saveResource("backup-instructions-readme.txt", true);

		// Language files
		addConfig("lang_english", new KonConfig("lang/english",false));
		updateConfigVersion("lang_english");
		addConfig("lang_chinese", new KonConfig("lang/chinese",false));
		updateConfigVersion("lang_chinese");
		addConfig("lang_russian", new KonConfig("lang/russian",false));
		updateConfigVersion("lang_russian");
		addConfig("lang_spanish", new KonConfig("lang/spanish",false));
		updateConfigVersion("lang_spanish");
		
		// Language selection
		language = getConfig("core").getString("language","english");
		if(configCache.containsKey("lang_"+language)) {
			// Use a built-in language file
			langConfig = getConfig("lang_"+language);
			ChatUtil.printConsoleAlert("Using "+language+" language file");
		} else {
			// Attempt to find a custom local language file
			if(addConfig("lang_custom", new KonConfig("lang/"+language,false))) {
				langConfig = getConfig("lang_custom");
				ChatUtil.printConsoleAlert("Using custom "+language+" language file");
			} else {
				// Failed to get any useful config, try to use english
				langConfig = getConfig("lang_english");
				ChatUtil.printConsoleError("Failed to load invalid language file "+language+".yml in Konquest/lang folder. Using default lang/english.yml.");
			}
		}
		
		// Config validation
		validateCorePaths();
	}
	
	public FileConfiguration getLang() {
		return langConfig;
	}

	public String getLangName() {
		return language;
	}

	public FileConfiguration getConfig(String key) {
		FileConfiguration result = new YamlConfiguration();
		if(!configCache.containsKey(key)) {
			ChatUtil.printConsoleError("Bad internal reference to file "+key);
		} else {
			result =  configCache.get(key).getConfig();
		}
		return result;
	}
	
	public boolean addConfig(String key, KonConfig config) {
		boolean status = false;
		if(config.saveDefaultConfig()) {
			// A config file exists, either existing or created from defaults
			if(config.reloadConfig()) {
				configCache.put(key, config);
				status = true;
			}
		}
		return status;
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
				Files.createDirectories(destination);
				Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
				oldFile.delete();
				ChatUtil.printConsoleAlert("Migrated data file "+oldPath+" to "+newpath);
			} catch (IOException e) {
				e.printStackTrace();
				ChatUtil.printConsoleError("Failed to move file "+oldPath+" to "+newpath);
			}
		}
	}
	
	private boolean validateCorePaths() {
		boolean result = true;
		FileConfiguration coreConfig = getConfig("core");
		for(CorePath path : CorePath.values()) {
			if(!coreConfig.contains(path.getPath(),true)) {
				result = false;
				ChatUtil.printConsoleError("Core configuration file is missing path: "+path.getPath());
			}
		}
		if(!result) {
			ChatUtil.printConsoleError("The Konquest core.yml config file may be corrupted. Try renaming or deleting the file, then restart the server.");
		}
		return result;
	}

	private void createBackupData() {
		int numBackups = konquest.getCore().getInt(CorePath.BACKUP_DATA_AMOUNT.getPath());
		numBackups = Math.max(numBackups,0); // minimum 0
		if(numBackups == 0) {
			ChatUtil.printConsoleAlert("Data backups are disabled.");
			return;
		}
		// Create a backup ZIP
		String dateStr = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new java.util.Date());
		KonquestPlugin plugin = Konquest.getInstance().getPlugin();
		File konquestFolder = plugin.getDataFolder();
		File dataFolder = new File(konquestFolder, "data");
		if(!dataFolder.exists()) {
			ChatUtil.printConsoleAlert("Skipping backup, data folder not found.");
			return;
		}
		String destFolder = konquestFolder.getAbsolutePath();
		String destName = "backup_data_"+dateStr+".zip";
		String archiveName = destFolder + File.separator + destName;
		ChatUtil.printConsoleAlert("Creating backup archive: "+destName);
		ArrayList<File> zipSources = new ArrayList<>();
		zipSources.add(dataFolder);
		ZipUtility zipUtil = new ZipUtility();
		try {
			zipUtil.zip(zipSources, archiveName);
		} catch (Exception ex) {
			// some errors occurred
			ChatUtil.printConsoleError("Failed to create backup archive of "+dataFolder.getPath());
			ex.printStackTrace();
		}
		// Remove old backup ZIP(s)
		FileFilter backupFileFilter = (file) -> file.getName().matches("backup_data_.+\\.zip");
		File[] allBackupArchives = konquestFolder.listFiles(backupFileFilter);
		if(allBackupArchives != null && allBackupArchives.length > numBackups) {
			// Found more backup ZIP files than limit setting
			// Sort by date modified
			Arrays.sort(allBackupArchives, Comparator.comparingLong(File::lastModified));
			// Delete the oldest
			int numDelete = allBackupArchives.length - numBackups;
			for(int i = 0; i < numDelete; i++) {
				ChatUtil.printConsoleAlert("Deleting backup archive: "+allBackupArchives[i].getName());
				allBackupArchives[i].delete();
			}
		}
	}

	/**
	 * Known incompatible version boundaries:
	 * 	0.11.0
	 */
	private void checkIncompatibleUpdate() {
		// Get the core config and check for incompatible version changes
		ArrayList<Version> versionBoundaries = new ArrayList<>();
		versionBoundaries.add(new Version("0.11.0"));

		KonquestPlugin plugin = Konquest.getInstance().getPlugin();
		FileConfiguration core = configCache.get("core").getConfig();
		String fileVersionStr = core.getString("version","0.0.0");
		String pluginVersionStr = plugin.getDescription().getVersion();
		Version coreVersion = new Version(fileVersionStr);
		Version pluginVersion = new Version(pluginVersionStr);

		for (Version boundary : versionBoundaries) {
			if(!coreVersion.equals(new Version("0.0.0")) && coreVersion.compareTo(boundary) < 0 && pluginVersion.compareTo(boundary) >= 0) {
				// Core config file version is earlier than a boundary version.
				// Print messages and archive the Konquest folder.
				ChatUtil.printConsoleError("/!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\");
				ChatUtil.printConsoleError("Updating from Konquest version "+fileVersionStr+" to version "+pluginVersionStr+" requires extra setup!");
				ChatUtil.printConsoleError("You must review all kingdoms for correctness, or delete the Konquest folder.");
				ChatUtil.printConsoleError("Not all plugin data is guaranteed to transfer to the new version.");
				ChatUtil.printConsoleError("The original plugin data folder will be archived to Konquest_"+fileVersionStr+".zip.");
				ChatUtil.printConsoleError("--> Recommended to delete the Konquest folder and restart the server. <--");
				ChatUtil.printConsoleError("/!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\");
				// Archive to zip
				String destFolder = plugin.getDataFolder().getParentFile().getAbsolutePath();
				String destName = plugin.getDataFolder().getName()+"_"+fileVersionStr+".zip";
				String archiveName = destFolder + File.separator + destName;
				ChatUtil.printConsole("Archive: "+archiveName);
				ArrayList<File> zipSources = new ArrayList<>();
				zipSources.add(plugin.getDataFolder());
				ZipUtility zipUtil = new ZipUtility();
				try {
					zipUtil.zip(zipSources, archiveName);
				} catch (Exception ex) {
					// some errors occurred
					ChatUtil.printConsoleError("Failed to archive Konquest plugin folder.");
					ex.printStackTrace();
				}
				// Exit the checking loop
				break;
			}
		}
	}

}
