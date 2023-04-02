package com.github.rumsfield.konquest.model;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.utility.ChatUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;

public class KonConfig {
	private File file;
	private final String name;
	private final String fileName;
	private YamlConfiguration config;
	private final boolean doSave;
	
	private final KonquestPlugin plugin;
	
	public KonConfig(String name) {
		this.name = name;
		this.fileName = name+".yml";
		this.doSave = true;
		this.plugin = Konquest.getInstance().getPlugin();
	}
	
	public KonConfig(String name, boolean doSave) {
		this.name = name;
		this.fileName = name+".yml";
		this.doSave = doSave;
		this.plugin = Konquest.getInstance().getPlugin();
	}
	
	// returns false if the configuration could not be loaded from a default resource or file
	public boolean reloadConfig() {
		boolean result = true;
		boolean isFileValid = true;
		config = new YamlConfiguration();
		if (file != null) {
			try {
				//config.load(file);
				config = YamlConfiguration.loadConfiguration(file);
			} catch (Exception e) {
				e.printStackTrace();
				ChatUtil.printConsoleError(fileName+" is not a valid configuration file! Check bad file for syntax errors.");
				File badFile = new File(plugin.getDataFolder(), fileName+".bad");
				Path source = file.toPath();
				Path destination = badFile.toPath();
				try {
					Files.createDirectories(destination);
					Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
					saveDefaultConfig();
				} catch (IOException io) {
					io.printStackTrace();
					ChatUtil.printConsoleError("Failed to save bad config file "+fileName);
				}
				isFileValid = false;
			}
		}
		// look for defaults in jar, if any
        InputStream defaultFile = plugin.getResource(fileName);
        if (defaultFile != null) {
            Reader defaultConfigStream = new InputStreamReader(defaultFile, StandardCharsets.UTF_8);
			YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(defaultConfigStream);
			if(isFileValid) {
				config.setDefaults(defaultConfig);
			} else {
				config = defaultConfig;
			}
		}
        config.options().parseComments(true);
        return result;
	}
	
	public FileConfiguration getConfig() {
		if (config == null) {
			reloadConfig();
		}
		return config;
	}
	
	public void saveConfig() {
		if (config == null || file == null || config.getKeys(false).isEmpty() || !doSave) {
	        return;
	    }
	    try {
	        config.save(file);
	    } catch (IOException exception) {
	    	exception.printStackTrace();
	    }
	}
	
	// Returns false if the file was missing and does not have a default resource
	public boolean saveDefaultConfig() {
	    if (file == null) {
	        file = new File(plugin.getDataFolder(), fileName);
	    }
	    boolean result = true;
	    if (!file.exists()) {
	    	try {
	    		plugin.saveResource(fileName, false);
	    		ChatUtil.printConsoleAlert("Saved default resource "+fileName);
	    	} catch(IllegalArgumentException e) {
	    		result = false;
	    		ChatUtil.printConsoleError("Unknown resource "+fileName+", check spelling.");
	    	}
	    }
	    return result;
	}
	
	// Returns true if the config file had its version upgraded
	public boolean updateVersion() {
		if (config == null || file == null) {
	        return false;
	    }
		boolean result = false;
		String fileVersion = config.getString("version","0.0.0");
		String pluginVersion = plugin.getDescription().getVersion();
		if(!fileVersion.equalsIgnoreCase(pluginVersion)) {
			// Update config version to current plugin version
			if(fileVersion.equals("0.0.0")) {
				// The config is a default resource, update 0.0.0 to plugin version
				config.set("version", pluginVersion);
				try {
					config.save(file);
				} catch (IOException exception) {
					exception.printStackTrace();
				}
				ChatUtil.printConsoleAlert("Created default config file \""+fileName+"\" for version "+pluginVersion);
			} else {
				// The config is from an older plugin version
				// Make a copy of config file
				String oldFileName = name+"-v"+fileVersion+".yml";
				File oldVersionFile = new File(plugin.getDataFolder(), oldFileName);
				try {
			        config.save(oldVersionFile);
			    } catch (Exception exception) {
			    	exception.printStackTrace();
					Path source = file.toPath();
					Path destination = oldVersionFile.toPath();
					try {
						Files.createDirectories(destination);
						Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException io) {
						io.printStackTrace();
						ChatUtil.printConsoleError("Failed to save old config "+oldFileName+".");
					}
			    }
                Reader defaultConfigStream = new InputStreamReader(Objects.requireNonNull(plugin.getResource(fileName)), StandardCharsets.UTF_8);
				YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(defaultConfigStream);
				// Update any valid non-defaults
				Map<String,Object> allPaths = config.getValues(true);
				for(String path : allPaths.keySet()) {
					if(defaultConfig.contains(path,false) && !(allPaths.get(path) instanceof ConfigurationSection)) {
						defaultConfig.set(path, allPaths.get(path));
					}
				}
				// Update version
				defaultConfig.set("version", pluginVersion);
				file = new File(plugin.getDataFolder(), fileName);
				try {
					defaultConfig.save(file);
				} catch (IOException exception) {
					exception.printStackTrace();
				}
				// Reload from new file
				reloadConfig();
				ChatUtil.printConsoleAlert("Updated config file \""+fileName+"\" to version "+pluginVersion);
			}
			result = true;
		}
		return result;
	}
	
	public void saveNewConfig() {
		if (config == null) {
			reloadConfig();
		}
		File badFile = new File(plugin.getDataFolder(), fileName+".bad");
		try {
	        config.save(badFile);
	    } catch (IOException exception) {
	    	exception.printStackTrace();
	    }
		file = new File(plugin.getDataFolder(), fileName);
		plugin.saveResource(fileName, true);
		reloadConfig();
	}
	
}