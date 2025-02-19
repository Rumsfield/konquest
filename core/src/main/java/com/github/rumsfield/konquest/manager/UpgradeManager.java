package com.github.rumsfield.konquest.manager;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.api.manager.KonquestUpgradeManager;
import com.github.rumsfield.konquest.api.model.KonquestTown;
import com.github.rumsfield.konquest.api.model.KonquestUpgrade;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class UpgradeManager implements KonquestUpgradeManager {
	
	private final Konquest konquest;
	private final HashMap<KonUpgrade,List<Integer>> upgradeCosts;
	private final HashMap<KonUpgrade,List<Integer>> upgradePopulations;
	private boolean isEnabled;
	
	public UpgradeManager(Konquest konquest) {
		this.konquest = konquest;
		this.upgradeCosts = new HashMap<>();
		this.upgradePopulations = new HashMap<>();
		this.isEnabled = false;
	}
	
	public void initialize() {
		if(!loadUpgrades()) {
        	// Encountered invalid upgrades.yml, overwrite with default
        	konquest.getConfigManager().overwriteBadConfig("upgrades");
        	konquest.getConfigManager().updateConfigVersion("upgrades");
        	// Attempt to load again
        	if(!loadUpgrades()) {
        		ChatUtil.printConsoleError("Failed to load bad upgrades from upgrades.yml. Try deleting all upgrades files and re-starting the server.");
        	}
        }
		isEnabled = konquest.getCore().getBoolean(CorePath.TOWNS_ENABLE_UPGRADES.getPath(),false);
		ChatUtil.printDebug("Upgrade Manager is ready, enabled "+isEnabled);
	}
	
	public boolean isEnabled() {
		return isEnabled;
	}
	
	private boolean loadUpgrades() {
		upgradeCosts.clear();
		upgradePopulations.clear();
		FileConfiguration upgradesConfig = konquest.getConfigManager().getConfig("upgrades");
        if (upgradesConfig.get("upgrades") == null) {
        	ChatUtil.printDebug("There is no upgrades section in upgrades.yml");
            return false;
        }
        List<Integer> costList;
        List<Integer> popList;
        KonUpgrade upgrade;
        boolean status = true;
        for(String upgradeName : upgradesConfig.getConfigurationSection("upgrades").getKeys(false)) {
        	upgrade = KonUpgrade.getUpgrade(upgradeName);
        	if(upgrade != null) {
        		ConfigurationSection upgradeSection = upgradesConfig.getConfigurationSection("upgrades."+upgradeName);
        		costList = new ArrayList<>();
        		popList = new ArrayList<>();
				assert upgradeSection != null;
				if(upgradeSection.contains("costs")) {
        			costList = upgradeSection.getIntegerList("costs");
        		} else {
        			ChatUtil.printDebug("Upgrades.yml is missing costs section for: "+upgradeName);
        			status = false;
        		}
        		if(upgradeSection.contains("populations")) {
        			popList = upgradeSection.getIntegerList("populations");
        		} else {
        			ChatUtil.printDebug("Upgrades.yml is missing populations section for: "+upgradeName);
        			status = false;
        		}
        		// Check array lengths
        		if(costList.size() == upgrade.getMaxLevel() && popList.size() == upgrade.getMaxLevel()) {
        			upgradeCosts.put(upgrade,costList);
        			upgradePopulations.put(upgrade,popList);
        			//ChatUtil.printDebug("Upgrade Manager loaded "+upgradeName+" with "+costList.size()+" levels.");
        		} else {
        			ChatUtil.printDebug("Upgrades.yml has the wrong number of costs/populations for "+upgradeName+", has "+costList.size()+" costs and "+popList.size()+" populations, but expected "+upgrade.getMaxLevel());
        			status = false;
        		}
        	} else {
        		ChatUtil.printDebug("Upgrades.yml contains bad upgrade name: "+upgradeName);
        		status = false;
        	}
        }
        return status;
	}

	/**
	 * Provides a list of all upgrades.
	 *
	 * @return All upgrades
	 */
	public List<KonquestUpgrade> getAllUpgrades() {
        return new ArrayList<>(Arrays.asList(KonUpgrade.values()));
	}
	
	/**
	 * Provides a list of upgrades available for purchase for the given town.
	 * Checks for valid cost and level requirements
	 * Upgrade levels will not be listed if cost is less than 0 (e.g. -1)
	 * @param townArg KonquestTown to check
	 * @return Map of KonquestUpgrade to Integer of next level available
	 */
	public HashMap<KonquestUpgrade,Integer> getAvailableUpgrades(KonquestTown townArg) {
		HashMap<KonquestUpgrade,Integer> result = new HashMap<>();
		if(!(townArg instanceof KonTown)) return result;
		KonTown town = (KonTown) townArg;
		for(KonUpgrade upgrade : KonUpgrade.values()) {
			int currentLevel = town.getRawUpgradeLevel(upgrade);
			int nextLevel = currentLevel+1;
			int nextCost = getUpgradeCost(upgrade,nextLevel);
			if(nextCost >= 0 && nextLevel <= upgrade.getMaxLevel()) {
				result.put(upgrade, nextLevel);
			}
		}
		return result;
	}
	
	public int getUpgradeCost(KonquestUpgrade upgrade, int level) {
		int result = -1;
		if(level > 0 && level <= upgrade.getMaxLevel()) {
			if(upgradeCosts.containsKey(upgrade)) {
				result = upgradeCosts.get(upgrade).get(level-1);
			}
		}
		return result;
	}
	
	public int getUpgradePopulation(KonquestUpgrade upgrade, int level) {
		int result = 0;
		if(level > 0 && level <= upgrade.getMaxLevel()) {
			if(upgradePopulations.containsKey(upgrade)) {
				result = upgradePopulations.get(upgrade).get(level-1);
			}
		}
		return result;
	}
	
	/**
	 * Main method for applying town upgrades.
	 * Used by normal players purchasing an upgrade.
	 * @param town the town to upgrade
	 * @param upgrade upgrade to apply
	 * @param level the level of the upgrade
	 * @param bukkitPlayer player purchasing the upgrade
	 * @return True on successful addition of the upgrade, else false.
	 */
	public boolean addTownUpgrade(KonTown town, KonUpgrade upgrade, int level, Player bukkitPlayer) {
		// Check that upgrades are enabled
		if(!isEnabled) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
			return false;
		}
		if(town.hasPropertyValue(KonPropertyFlag.UPGRADE) && !town.getPropertyValue(KonPropertyFlag.UPGRADE)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DISABLED.getMessage());
			return false;
		}
		// Check that the town is not under attack
		boolean isAttackCheckEnabled = konquest.getCore().getBoolean(CorePath.TOWNS_UPGRADE_WHILE_ATTACKED.getPath(),false);
		if(!isAttackCheckEnabled && town.isAttacked()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.MENU_UPGRADE_FAIL_ATTACK.getMessage());
			return false;
		}
		// Check that the town has the previous level, if applicable
		int currentLevel = town.getUpgradeLevel(upgrade);
		if(level < 1 || level > upgrade.getMaxLevel() || level != currentLevel+1) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.MENU_UPGRADE_FAIL_LEVEL.getMessage(upgrade.getName(),level,currentLevel));
			return false;
		}
		// Check that the town has enough population
		int currentPopulation = town.getNumResidents();
		int requiredPopulation = upgradePopulations.get(upgrade).get(level-1);
		if(currentPopulation < requiredPopulation) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.MENU_UPGRADE_FAIL_POPULATION.getMessage(upgrade.getName(),level,requiredPopulation));
			return false;
		}
		// Check that the player has enough favor
		int requiredCost = upgradeCosts.get(upgrade).get(level-1);
		if(KonquestPlugin.getBalance(bukkitPlayer) < requiredCost) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.MENU_UPGRADE_FAIL_COST.getMessage(upgrade.getName(),level,requiredCost));
            return false;
		}
		// Passed all checks, add the upgrade
		town.addUpgrade(upgrade, level);
		if(upgrade.equals(KonUpgrade.HEALTH)) {
			konquest.getKingdomManager().refreshTownHearts(town);
		}
		ChatUtil.sendNotice(bukkitPlayer, MessagePath.MENU_UPGRADE_ADD.getMessage(upgrade.getName(),level,town.getName()));
		ChatUtil.printDebug("Applied new upgrade "+upgrade.getName()+" level "+level+" to town "+town.getName());
		// Withdraw cost
		KonPlayer player = konquest.getPlayerManager().getPlayer(bukkitPlayer);
        if(KonquestPlugin.withdrawPlayer(bukkitPlayer, requiredCost)) {
        	if(player != null) {
        		konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR, requiredCost);
        	}
        }
		return true;
	}
	
	public boolean applyTownUpgrade(KonquestTown townArg, KonquestUpgrade upgrade, int level) {
		if(!isEnabled) return false;
		if(level < 0 || level > upgrade.getMaxLevel()) return false;
		if(!(townArg instanceof KonTown)) return false;
		KonTown town = (KonTown) townArg;
		town.addUpgrade(upgrade, level);
		if(upgrade.equals(KonUpgrade.HEALTH)) {
			konquest.getKingdomManager().refreshTownHearts(town);
		}
		ChatUtil.printDebug("Applied upgrade "+upgrade.getName()+" level "+level+" to town "+town.getName());
		updateTownDisabledUpgrades(town);
		return true;
	}
	
	public int getTownUpgradeLevel(KonquestTown townArg, KonquestUpgrade upgrade) {
		int result = 0;
		if(isEnabled && townArg instanceof KonTown) {
			KonTown town = (KonTown) townArg;
			result = town.getUpgradeLevel(upgrade);
		}
		return result;
	}
	
	public void updateTownDisabledUpgrades(KonTown town) {
		for(KonUpgrade upgrade : KonUpgrade.values()) {
			// If the town has this upgrade...
			int level = town.getRawUpgradeLevel(upgrade);
			if(level > 0) {
				if(isEnabled) {
					// Check if the upgrade is disabled (cost < 0)
					int cost = getUpgradeCost(upgrade,level);
					if(cost >= 0) {
						// Check every purchased upgrade meets pop requirement
						int townPop = town.getPlayerResidents().size();
						if(townPop < upgradePopulations.get(upgrade).get(level-1)) {
							// Current population is below this upgrade level's requirement
							// Find new level for disabled upgrade, down to 0 minimum
							int newLevel = level;
							while(newLevel > 0 && townPop < upgradePopulations.get(upgrade).get(newLevel-1)) {
								newLevel--;
							}
							town.disableUpgrade(upgrade,newLevel);
							ChatUtil.printDebug("UPGRADE: Disabled upgrade "+upgrade.getName()+" from level "+level+" to "+newLevel+" for town "+town.getName());
						} else {
							// Current population is greater than or equal to this upgrade level's requirement
							boolean status = town.allowUpgrade(upgrade);
							if(status) {
								ChatUtil.printDebug("UPGRADE: Successfully allowed upgrade "+upgrade.getName()+" level "+level+" for town "+town.getName());
							} else {
								ChatUtil.printDebug("UPGRADE: No change to upgrade "+upgrade.getName()+" level "+level+" for town "+town.getName());
							}
						}
					} else {
						// This upgrade level is disabled because either the cost is set to a negative number or is missing from the upgrades.yml
						town.disableUpgrade(upgrade,0);
						ChatUtil.printDebug("UPGRADE: Disabled invalid upgrade "+upgrade.getName()+" to level 0 for town "+town.getName()+", cost is "+cost);
					}
				} else {
					// Upgrades are disabled in config, disable every purchased upgrade
					town.disableUpgrade(upgrade,0);
					ChatUtil.printDebug("UPGRADE: Disabled unused upgrade "+upgrade.getName()+" to level 0 for town "+town.getName());
				}
			}
		}
	}
	
	
}
