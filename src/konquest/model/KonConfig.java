package konquest.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.bukkit.ChatColor;
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
	
	public boolean updateVersion() {
		if (config == null || file == null) {
	        return false;
	    }
		boolean result = false;
		String fileVersion = config.getString("version","0.0.0");
		String pluginVersion = plugin.getDescription().getVersion();
		if(!fileVersion.equalsIgnoreCase(pluginVersion)) {
			if(fileVersion.equals("0.0.0")) {
				// Update default config version to current plugin version
				if(config.contains("version")) {
					config.set("version", pluginVersion);
					try {
				        config.save(file);
				    } catch (IOException exception) {
				    	exception.printStackTrace();
				    }
				}
			} else {
				// Make a copy of config file
				String oldFileName = name+"-v"+fileVersion+".yml";
				File oldVersionFile = new File(plugin.getDataFolder(), oldFileName);
				try {
			        config.save(oldVersionFile);
			    } catch (IOException exception) {
			    	exception.printStackTrace();
			    }
				// Save default config & update version
				file = new File(plugin.getDataFolder(), fileName);
				plugin.saveResource(fileName, true);
				reloadConfig();
				config.set("version", pluginVersion);
				try {
			        config.save(file);
			    } catch (IOException exception) {
			    	exception.printStackTrace();
			    }
			}
			result = true;
			Konquest.getInstance().getPlugin().getServer().getConsoleSender().sendMessage(ChatColor.RED+"[Konquest] Invalid or missing config file \""+fileName+"\" replaced with latest default version. Manually edit new YML file if desired.");
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