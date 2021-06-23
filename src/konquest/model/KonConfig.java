package konquest.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
//import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.utility.ChatUtil;

public class KonConfig {
	private File file;
	private String name;
	private String fileName;
	private YamlConfiguration config;
	
	private KonquestPlugin plugin;
	
	public KonConfig(String name) {
		this.name = name;
		this.fileName = name+".yml";
		plugin = Konquest.getInstance().getPlugin();
	}
	
	public void reloadConfig() {
		config = YamlConfiguration.loadConfiguration(file);
		
		// look for defaults in jar
		try {
			Reader defaultConfigStream = new InputStreamReader(plugin.getResource(fileName), "UTF8");
			
			if (defaultConfigStream != null) {
				YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(defaultConfigStream);
				config.setDefaults(defaultConfig);
			}
		} catch (IOException exception) {
			exception.printStackTrace();
		}
	}
	
	public FileConfiguration getConfig() {
		if (config == null) {
			reloadConfig();
		}
		
		return config;
	}
	
	public void saveConfig() {
		if (config == null || file == null) {
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
	    	} catch(IllegalArgumentException e) {
	    		result = false;
	    		ChatUtil.printConsoleError("Failed to save unknown resource "+fileName);
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
				if(config.contains("version")) {
					config.set("version", pluginVersion);
					try {
				        config.save(file);
				    } catch (IOException exception) {
				    	exception.printStackTrace();
				    }
				}
				ChatUtil.printConsoleAlert("Created default config file \""+fileName+"\" for version "+pluginVersion);
			} else {
				// The config is from an older plugin version
				// Make a copy of config file
				String oldFileName = name+"-v"+fileVersion+".yml";
				File oldVersionFile = new File(plugin.getDataFolder(), oldFileName);
				try {
			        config.save(oldVersionFile);
			    } catch (IOException exception) {
			    	exception.printStackTrace();
			    }
				try {
					Reader defaultConfigStream = new InputStreamReader(plugin.getResource(fileName), "UTF8");
					if (defaultConfigStream != null) {
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
				} catch (IOException exception) {
					exception.printStackTrace();
				}
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