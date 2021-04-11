package konquest.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import konquest.Konquest;
import konquest.KonquestPlugin;

public class KonConfig {
	private File file;
	private String name;
	private FileConfiguration config;
	
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
	
	public void saveDefaultConfig() {
	    if (file == null) {
	        file = new File(plugin.getDataFolder(), name);
	    }
	    
	    if (!file.exists()) {
	    	plugin.saveResource(name, false);
	    }
	}
	
	public void saveNewConfig() {
		if (config == null) {
			reloadConfig();
		}
		try {
	        config.save(name+".bad");
	    } catch (IOException exception) {
	    	exception.printStackTrace();
	    }
		file = new File(plugin.getDataFolder(), name);
		plugin.saveResource(name, true);
	}
	
	public void applyHeader(String header) {
		config.options().copyHeader(false);
		config.options().header(header);
	}
}