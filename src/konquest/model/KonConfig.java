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

public class KonConfig {
	private File file;
	private String name;
	private YamlConfiguration config;
	
	private KonquestPlugin plugin;
	
	public KonConfig(String name) {
		this.name = name;
		plugin = Konquest.getInstance().getPlugin();
	}
	
	public void reloadConfig() {
		config = YamlConfiguration.loadConfiguration(file);
		
		// look for defaults in jar
		try {
			Reader defaultConfigStream = new InputStreamReader(plugin.getResource(name), "UTF8");
			
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
	
	// Returns true if the file was missing and has been replaced with the default
	public boolean saveDefaultConfig() {
	    if (file == null) {
	        file = new File(plugin.getDataFolder(), name);
	    }
	    boolean result = false;
	    if (!file.exists()) {
	    	plugin.saveResource(name, false);
	    	result = true;
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
			config.set("version", pluginVersion);
			saveConfig();
			result = true;
			Konquest.getInstance().getPlugin().getServer().getConsoleSender().sendMessage(ChatColor.RED+"[Konquest] Invalid or missing config file \""+name+"\" replaced with latest default version. Manually edit new YML file.");
		}
		return result;
	}
	
	public void saveNewConfig() {
		if (config == null) {
			reloadConfig();
		}
		File badFile = new File(plugin.getDataFolder(), name+".bad");
		try {
	        config.save(badFile);
	    } catch (IOException exception) {
	    	exception.printStackTrace();
	    }
		file = new File(plugin.getDataFolder(), name);
		plugin.saveResource(name, true);
	}
	
}