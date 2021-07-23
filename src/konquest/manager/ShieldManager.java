package konquest.manager;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import konquest.Konquest;
import konquest.model.KonShield;
import konquest.utility.ChatUtil;

public class ShieldManager {

	private Konquest konquest;
	private boolean isEnabled;
	private ArrayList<KonShield> shields;
	
	public ShieldManager(Konquest konquest) {
		this.konquest = konquest;
		this.isEnabled = false;
		this.shields = new ArrayList<KonShield>();
	}
	
	public void initialize() {
		if(loadShields()) {
			isEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.towns.enable_shields",false);
		}
		ChatUtil.printDebug("Shield Manager is ready, enabled "+isEnabled);
	}
	
	private boolean loadShields() {
		shields.clear();
		FileConfiguration shieldsConfig = konquest.getConfigManager().getConfig("shields");
        if (shieldsConfig.get("shields") == null) {
        	ChatUtil.printDebug("There is no shields section in shields.yml");
            return false;
        }
        KonShield newShield;
        for(String shieldName : shieldsConfig.getConfigurationSection("shields").getKeys(false)) {
        	boolean status = true;
        	int shieldTime = 0;
        	int shieldCost = 0;
        	ConfigurationSection shieldSection = shieldsConfig.getConfigurationSection("shields."+shieldName);
        	if(shieldSection.contains("time")) {
        		shieldTime = shieldSection.getInt("time",0);
    		} else {
    			ChatUtil.printDebug("Shields.yml is missing time section for: "+shieldName);
    			status = false;
    		}
        	if(shieldSection.contains("cost")) {
        		shieldCost = shieldSection.getInt("cost",0);
    		} else {
    			ChatUtil.printDebug("Shields.yml is missing cost section for: "+shieldName);
    			status = false;
    		}
        	if(status) {
        		newShield = new KonShield(shieldName,shieldTime,shieldCost);
        		shields.add(newShield);
        	} else {
        		ChatUtil.printConsoleError("Invalid shield option: "+shieldName+". Must include time and cost values.");
        	}
        }
		return true;
	}
	
	public KonShield getShield(String id) {
		KonShield result = null;
		for(KonShield s : shields) {
			if(s.getId().equalsIgnoreCase(id)) {
				result = s;
				break;
			}
		}
		return result;
	}
	
	public List<KonShield> getShields() {
		return shields;
	}
	
	
}
