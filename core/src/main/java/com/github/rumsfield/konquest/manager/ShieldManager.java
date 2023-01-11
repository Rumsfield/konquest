package com.github.rumsfield.konquest.manager;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.api.manager.KonquestShieldManager;
import com.github.rumsfield.konquest.api.model.KonquestTown;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ShieldManager implements KonquestShieldManager {

	private final Konquest konquest;
	private boolean isShieldsEnabled;
	private boolean isArmorsEnabled;
	private final ArrayList<KonShield> shields;
	private final ArrayList<KonArmor> armors;
	
	public ShieldManager(Konquest konquest) {
		this.konquest = konquest;
		this.isShieldsEnabled = false;
		this.isArmorsEnabled = false;
		this.shields = new ArrayList<>();
		this.armors = new ArrayList<>();
	}
	
	public void initialize() {
		if(loadShields()) {
			isShieldsEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.towns.enable_shields",false);
		}
		if(loadArmors()) {
			isArmorsEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.towns.enable_armor",false);
		}
		// Check for shields or armor that must be disabled, due to reload
		for(KonKingdom kingdom : konquest.getKingdomManager().getKingdoms()) {
			for(KonTown town : kingdom.getTowns()) {
				if(!isShieldsEnabled && town.isShielded()) {
					town.deactivateShield();
				}
				if(!isArmorsEnabled && town.isArmored()) {
					town.deactivateArmor();
				}
			}
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
		boolean status = true;
		for(String shieldName : shieldsConfig.getConfigurationSection("shields").getKeys(false)) {
        	int shieldTime = 0;
        	int shieldCost = 0;
        	ConfigurationSection shieldSection = shieldsConfig.getConfigurationSection("shields."+shieldName);
			assert shieldSection != null;
			if(shieldSection.contains("charge")) {
        		shieldTime = shieldSection.getInt("charge",0);
    		} else {
    			ChatUtil.printDebug("Shields.yml is missing charge section for shield: "+shieldName);
    			status = false;
    		}
        	if(shieldSection.contains("cost")) {
        		shieldCost = shieldSection.getInt("cost",0);
    		} else {
    			ChatUtil.printDebug("Shields.yml is missing cost section for shield: "+shieldName);
			}
			newShield = new KonShield(shieldName, shieldTime, shieldCost);
			shields.add(newShield);
		}
		return status;
	}
	
	private boolean loadArmors() {
		armors.clear();
		FileConfiguration shieldsConfig = konquest.getConfigManager().getConfig("shields");
        if (shieldsConfig.get("armors") == null) {
        	ChatUtil.printDebug("There is no armors section in shields.yml");
            return false;
        }
        KonArmor newArmor;
		boolean status = true;
		for(String armorName : shieldsConfig.getConfigurationSection("armors").getKeys(false)) {
        	int armorBlocks = 0;
        	int armorCost = 0;
        	ConfigurationSection armorSection = shieldsConfig.getConfigurationSection("armors."+armorName);
			assert armorSection != null;
			if(armorSection.contains("charge")) {
        		armorBlocks = armorSection.getInt("charge",0);
    		} else {
    			ChatUtil.printDebug("Shields.yml is missing charge section for armor: "+armorName);
    			status = false;
    		}
        	if(armorSection.contains("cost")) {
        		armorCost = armorSection.getInt("cost",0);
    		} else {
    			ChatUtil.printDebug("Shields.yml is missing cost section for armor: "+armorName);
    			status = false;
    		}
			newArmor = new KonArmor(armorName, armorBlocks, armorCost);
			armors.add(newArmor);
		}
		return status;
	}
	
	public KonShield getShield(String id) {
		KonShield result = null;
		for(KonShield shield : shields) {
			if(shield.getId().equalsIgnoreCase(id)) {
				result = shield;
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
		int shieldTime = shield.getDurationSeconds();
		int endTime = (int)(now.getTime()/1000) + shieldTime;
		
		// Check that the player has enough favor
		int requiredCost = shield.getCost()*town.getNumResidents();
		if(KonquestPlugin.getBalance(bukkitPlayer) < requiredCost) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.MENU_SHIELD_FAIL_COST.getMessage(requiredCost));
            return false;
		}
		
		// Check that town is not under attack optionally
		boolean isAttackCheckEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.towns.shields_while_attacked",false);
		if(!isAttackCheckEnabled && town.isAttacked()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.MENU_SHIELD_FAIL_ATTACK.getMessage());
            return false;
		}
		
		// Check that town does not have an active shield optionally
		boolean isShieldAddEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.towns.shields_add",false);
		if(!isShieldAddEnabled && town.isShielded()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.MENU_SHIELD_FAIL_ADD.getMessage());
            return false;
		}
		
		// Check that town does not exceed maximum optionally
		int maxShields = konquest.getConfigManager().getConfig("core").getInt("core.towns.max_shields",0);
		if(maxShields > 0 && shield.getDurationSeconds()+town.getRemainingShieldTimeSeconds() > maxShields) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.MENU_SHIELD_FAIL_MAX.getMessage(maxShields));
            return false;
		}
		
		// Passed checks, activate the shield
		String timeFormat = Konquest.getTimeFormat(shield.getDurationSeconds(), ChatColor.AQUA);
		String shieldName = shield.getId()+" "+MessagePath.LABEL_SHIELD.getMessage();
		if(town.isShielded()) {
			endTime = town.getShieldEndTime() + shieldTime;
			ChatUtil.sendNotice(bukkitPlayer, MessagePath.MENU_SHIELD_ACTIVATE_ADD.getMessage(shieldName,timeFormat));
			ChatUtil.printDebug("Activated town shield addition "+shield.getId()+" to town "+town.getName()+" for end time "+endTime);
		} else {
			ChatUtil.sendNotice(bukkitPlayer, MessagePath.MENU_SHIELD_ACTIVATE_NEW.getMessage(shieldName,timeFormat));
			ChatUtil.printDebug("Activated new town shield "+shield.getId()+" to town "+town.getName()+" for end time "+endTime);
		}
		town.activateShield(endTime);
		
		// Withdraw cost
		KonPlayer player = konquest.getPlayerManager().getPlayer(bukkitPlayer);
        if(KonquestPlugin.withdrawPlayer(bukkitPlayer, requiredCost)) {
        	if(player != null) {
        		konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR, requiredCost);
        	}
        }
		return true;
	}
	
	public boolean activateTownArmor(KonArmor armor, KonTown town, Player bukkitPlayer) {
		if(!isArmorsEnabled) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
			return false;
		}
		
		// Check that the player has enough favor
		int requiredCost = armor.getCost()*town.getNumResidents();
		if(KonquestPlugin.getBalance(bukkitPlayer) < requiredCost) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.MENU_SHIELD_FAIL_COST.getMessage(requiredCost));
            return false;
		}
		
		// Check that town is not under attack optionally
		boolean isAttackCheckEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.towns.armor_while_attacked",false);
		if(!isAttackCheckEnabled && town.isAttacked()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.MENU_SHIELD_FAIL_ATTACK.getMessage());
            return false;
		}
		
		// Check that town does not have an active armor optionally
		boolean isArmorAddEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.towns.armor_add",false);
		if(!isArmorAddEnabled && town.isArmored()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.MENU_SHIELD_FAIL_ADD.getMessage());
            return false;
		}
		
		// Check that town does not exceed maximum optionally
		int maxArmor = konquest.getConfigManager().getConfig("core").getInt("core.towns.max_armor",0);
		if(maxArmor > 0 && armor.getBlocks()+town.getArmorBlocks() > maxArmor) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.MENU_SHIELD_FAIL_MAX.getMessage(maxArmor));
            return false;
		}
		
		// Passed checks, activate the armor
		int newArmor = armor.getBlocks();
		String armorName = armor.getId()+" "+MessagePath.LABEL_ARMOR.getMessage();
		if(town.isArmored()) {
			newArmor = town.getArmorBlocks() + armor.getBlocks();
			ChatUtil.sendNotice(bukkitPlayer, MessagePath.MENU_SHIELD_ACTIVATE_ADD.getMessage(armorName,armor.getBlocks()));
			ChatUtil.printDebug("Activated town armor addition "+armor.getId()+" to town "+town.getName()+" for blocks "+armor.getBlocks());
		} else {
			ChatUtil.sendNotice(bukkitPlayer, MessagePath.MENU_SHIELD_ACTIVATE_NEW.getMessage(armorName,armor.getBlocks()));
			ChatUtil.printDebug("Activated new town armor "+armor.getId()+" to town "+town.getName()+" for blocks "+armor.getBlocks());
		}
		town.activateArmor(newArmor);
		
		// Withdraw cost
		KonPlayer player = konquest.getPlayerManager().getPlayer(bukkitPlayer);
        if(KonquestPlugin.withdrawPlayer(bukkitPlayer, requiredCost)) {
        	if(player != null) {
        		konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR, requiredCost);
        	}
        }
		return true;
	}
	
	public int getTownShieldTime(KonquestTown townArg) {
		int result = -1;
		if(townArg instanceof KonTown) {
			KonTown town = (KonTown) townArg;
			result = town.getShieldEndTime();
		}
		return result;
	}
	
	public int getTownArmorBlocks(KonquestTown townArg) {
		int result = -1;
		if(townArg instanceof KonTown) {
			KonTown town = (KonTown) townArg;
			result = town.getArmorBlocks();
		}
		return result;
	}
	
	/*
	 * Override methods, used for admin commands
	 */
	
	/**
	 * Sets a town's shields for (value) seconds from now
	 * @param townArg The town to modify shields
	 * @param value Time in seconds from now to end shields. If 0, deactivate shields.
	 */
	public boolean shieldSet(KonquestTown townArg, int value) {
		boolean result = false;
		if(!(townArg instanceof KonTown)) return false;
		KonTown town = (KonTown) townArg;
		Date now = new Date();
		int newEndTime = (int)(now.getTime()/1000) + value;
		if(town.isShielded()) {
			if(value <= 0) {
				town.deactivateShield();
			} else {
				town.activateShield(newEndTime);
			}
			result = true;
		} else {
			if(value > 0) {
				town.activateShield(newEndTime);
				result = true;
			}
		}

		return result;
	}
	
	/**
	 * Adds to a town's shields in seconds, positive or negative
	 * @param townArg The town to modify shields
	 * @param value Time in seconds to add to current shields. If result is <= 0, deactivate shields.
	 */
	public boolean shieldAdd(KonquestTown townArg, int value) {
		boolean result = false;
		if(townArg instanceof KonTown) {
			KonTown town = (KonTown) townArg;
			Date now = new Date();
			int newEndTime = (int)(now.getTime()/1000) + value;
			if(town.isShielded()) {
				newEndTime = town.getShieldEndTime() + value;
			}
			Date end = new Date((long)newEndTime*1000);
			if(now.after(end)) {
				// End time is less than now, deactivate shields and do nothing
				if(town.isShielded()) {
					town.deactivateShield();
					result = true;
				}
			} else {
				// End time is in the future, add to shields
				town.activateShield(newEndTime);
				result = true;
			}
		}
		return result;
	}

	/**
	 * Sets a town's armor for (value) blocks
	 * @param townArg The town to modify armor
	 * @param value Amount of blocks to set the town's armor to. If 0, deactivate armor.
	 */
	public boolean armorSet(KonquestTown townArg, int value) {
		boolean result = false;
		if(!(townArg instanceof KonTown)) return false;
		KonTown town = (KonTown) townArg;
		if(town.isArmored()) {
			if(value <= 0) {
				town.deactivateArmor();
			} else {
				town.activateArmor(value);
			}
			result = true;
		} else {
			if(value > 0) {
				town.activateArmor(value);
				result = true;
			}
		}
		return result;
	}
	
	/**
	 * Adds to a town's armor in blocks, positive or negative
	 * @param townArg The town to modify armor
	 * @param value Blocks to add to current armor. If result is <= 0, deactivate armor.
	 */
	public boolean armorAdd(KonquestTown townArg, int value) {
		boolean result = false;
		if(!(townArg instanceof KonTown)) return false;
		KonTown town = (KonTown) townArg;
		int newBlocks = value;
		if(town.isArmored()) {
			newBlocks = town.getArmorBlocks() + value;
		}
		if(newBlocks <= 0) {
			// Blocks is 0 or less, deactivate armor and do nothing
			if(town.isArmored()) {
				town.deactivateArmor();
				result = true;
			}
		} else {
			// Blocks are valid, add to armor
			town.activateArmor(newBlocks);
			result = true;
		}
		return result;
	}
	
}
