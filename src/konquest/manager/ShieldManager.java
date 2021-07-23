package konquest.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.model.KonArmor;
import konquest.model.KonPlayer;
import konquest.model.KonShield;
import konquest.model.KonStatsType;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;
import net.milkbowl.vault.economy.EconomyResponse;

public class ShieldManager {

	private Konquest konquest;
	private boolean isShieldsEnabled;
	private boolean isArmorsEnabled;
	private ArrayList<KonShield> shields;
	private ArrayList<KonArmor> armors;
	
	public ShieldManager(Konquest konquest) {
		this.konquest = konquest;
		this.isShieldsEnabled = false;
		this.isArmorsEnabled = false;
		this.shields = new ArrayList<KonShield>();
		this.armors = new ArrayList<KonArmor>();
	}
	
	public void initialize() {
		if(loadShields()) {
			isShieldsEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.towns.enable_shields",false);
		}
		if(loadArmors()) {
			isArmorsEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.towns.enable_armor",false);
		}
		ChatUtil.printDebug("Shield Manager is ready, shields: "+isShieldsEnabled+", armors: "+isArmorsEnabled);
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
    			ChatUtil.printDebug("Shields.yml is missing time section for shield: "+shieldName);
    			status = false;
    		}
        	if(shieldSection.contains("cost")) {
        		shieldCost = shieldSection.getInt("cost",0);
    		} else {
    			ChatUtil.printDebug("Shields.yml is missing cost section for shield: "+shieldName);
    			status = false;
    		}
        	if(status || shieldTime == 0 || shieldCost == 0) {
        		newShield = new KonShield(shieldName,shieldTime,shieldCost);
        		shields.add(newShield);
        	} else {
        		ChatUtil.printConsoleError("Invalid shield option: "+shieldName+". Must include valid time and cost values.");
        	}
        }
		return true;
	}
	
	private boolean loadArmors() {
		armors.clear();
		FileConfiguration shieldsConfig = konquest.getConfigManager().getConfig("shields");
        if (shieldsConfig.get("armors") == null) {
        	ChatUtil.printDebug("There is no armors section in shields.yml");
            return false;
        }
        KonArmor newArmor;
        for(String armorName : shieldsConfig.getConfigurationSection("armors").getKeys(false)) {
        	boolean status = true;
        	int armorBlocks = 0;
        	int armorCost = 0;
        	ConfigurationSection armorSection = shieldsConfig.getConfigurationSection("armor."+armorName);
        	if(armorSection.contains("blocks")) {
        		armorBlocks = armorSection.getInt("blocks",0);
    		} else {
    			ChatUtil.printDebug("Shields.yml is missing blocks section for armor: "+armorName);
    			status = false;
    		}
        	if(armorSection.contains("cost")) {
        		armorCost = armorSection.getInt("cost",0);
    		} else {
    			ChatUtil.printDebug("Shields.yml is missing cost section for armor: "+armorName);
    			status = false;
    		}
        	if(status || armorBlocks == 0 || armorCost == 0) {
        		newArmor = new KonArmor(armorName,armorBlocks,armorCost);
        		armors.add(newArmor);
        	} else {
        		ChatUtil.printConsoleError("Invalid armor option: "+armorName+". Must include valid blocks and cost values.");
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
	
	public boolean isShieldsEnabled() {
		return isShieldsEnabled;
	}
	
	public boolean isArmorsEnabled() {
		return isArmorsEnabled;
	}
	
	public List<KonShield> getShields() {
		return shields;
	}
	
	public List<KonArmor> getArmors() {
		return armors;
	}
	
	public boolean activateTownShield(KonShield shield, KonTown town, Player bukkitPlayer) {
		if(!isShieldsEnabled) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
			return false;
		}
		
		Date now = new Date();
		long shieldTime = (shield.getDurationSeconds()*1000);
		long endTime = now.getTime() + shieldTime;
		
		// Check that the player has enough favor
		int requiredCost = shield.getCost()*town.getNumResidents();
		if(KonquestPlugin.getBalance(bukkitPlayer) < requiredCost) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.MENU_SHIELD_FAIL_COST.getMessage(shield.getId(),requiredCost));
            return false;
		}
		
		// Check that town is not under attack optionally
		boolean isAttackCheckEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.towns.shields_while_attacked",false);
		if(isAttackCheckEnabled && town.isAttacked()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.MENU_SHIELD_FAIL_ATTACK.getMessage());
            return false;
		}
		
		// Passed checks, activate the shield
		if(town.isShielded()) {
			endTime = town.getShieldEndTime() + shieldTime;
			ChatUtil.sendNotice(bukkitPlayer, MessagePath.MENU_SHIELD_ACTIVATE_ADD.getMessage(shield.getId(),shield.getDurationFormat()));
			ChatUtil.printDebug("Activated town shield addition "+shield.getId()+" to town "+town.getName()+" for end time "+endTime);
		} else {
			town.activateShield();
			ChatUtil.sendNotice(bukkitPlayer, MessagePath.MENU_SHIELD_ACTIVATE_NEW.getMessage(shield.getId(),shield.getDurationFormat()));
			ChatUtil.printDebug("Activated new town shield "+shield.getId()+" to town "+town.getName()+" for end time "+endTime);
		}
		town.setShieldEndTime(endTime);
		
		// Withdraw cost
		KonPlayer player = konquest.getPlayerManager().getPlayer(bukkitPlayer);
		EconomyResponse r = KonquestPlugin.withdrawPlayer(bukkitPlayer, requiredCost);
        if(r.transactionSuccess()) {
        	String balanceF = String.format("%.2f",r.balance);
        	String amountF = String.format("%.2f",r.amount);
        	ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_REDUCE_FAVOR.getMessage(amountF,balanceF));
        	if(player != null) {
        		konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)requiredCost);
        	}
        } else {
        	ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(r.errorMessage));
        }
		return true;
	}
	
	
}
