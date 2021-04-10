package konquest.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.model.KonPlayer;
import konquest.model.KonStatsType;
import konquest.model.KonTown;
import konquest.model.KonUpgrade;
import konquest.utility.ChatUtil;
import net.milkbowl.vault.economy.EconomyResponse;

public class UpgradeManager {
	
	private Konquest konquest;
	private HashMap<KonUpgrade,List<Integer>> upgradeCosts;
	private HashMap<KonUpgrade,List<Integer>> upgradePopulations;
	private boolean isEnabled;
	
	public UpgradeManager(Konquest konquest) {
		this.konquest = konquest;
		this.upgradeCosts = new HashMap<KonUpgrade,List<Integer>>();
		this.upgradePopulations = new HashMap<KonUpgrade,List<Integer>>();
		this.isEnabled = false;
	}
	
	public void initialize() {
		loadUpgrades();
		isEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.towns.enable_upgrades",false);
		ChatUtil.printDebug("Upgrade Manager is ready, enabled "+isEnabled);
	}
	
	private void loadUpgrades() {
		upgradeCosts.clear();
		upgradePopulations.clear();
		FileConfiguration upgradesConfig = konquest.getConfigManager().getConfig("upgrades");
        if (upgradesConfig.get("upgrades") == null) {
        	ChatUtil.printDebug("There is no upgrades section in upgrades.yml");
            return;
        }
        List<Integer> costList;
        List<Integer> popList;
        KonUpgrade upgrade;
        for(String upgradeName : upgradesConfig.getConfigurationSection("upgrades").getKeys(false)) {
        	upgrade = KonUpgrade.getUpgrade(upgradeName);
        	if(upgrade != null) {
        		ConfigurationSection upgradeSection = upgradesConfig.getConfigurationSection("upgrades."+upgradeName);
        		costList = new ArrayList<Integer>();
        		popList = new ArrayList<Integer>();
        		if(upgradeSection.contains("costs")) {
        			costList = upgradeSection.getIntegerList("costs");
        		} else {
        			ChatUtil.printDebug("Upgrades.yml is missing costs section for: "+upgradeName);
        		}
        		if(upgradeSection.contains("populations")) {
        			popList = upgradeSection.getIntegerList("populations");
        		} else {
        			ChatUtil.printDebug("Upgrades.yml is missing populations section for: "+upgradeName);
        		}
        		// Check array lengths
        		if(costList.size() == upgrade.getMaxLevel() && popList.size() == upgrade.getMaxLevel()) {
        			upgradeCosts.put(upgrade,costList);
        			upgradePopulations.put(upgrade,popList);
        			//ChatUtil.printDebug("Upgrade Manager loaded "+upgradeName+" with "+costList.size()+" levels.");
        		} else {
        			ChatUtil.printDebug("Upgrades.yml has the wrong number of costs/populations for "+upgradeName+", has "+costList.size()+" costs and "+popList.size()+" populations, but expected "+upgrade.getMaxLevel());
        		}
        	} else {
        		ChatUtil.printDebug("Upgrades.yml contains bad upgrade name: "+upgradeName);
        	}
        }
	}
	
	public HashMap<KonUpgrade,Integer> getAvailableUpgrades(KonTown town) {
		HashMap<KonUpgrade,Integer> result = new HashMap<KonUpgrade,Integer>();
		//ChatUtil.printDebug("Generating available upgrades for town "+town.getName());
		for(KonUpgrade upgrade : KonUpgrade.values()) {
			int currentLevel = town.getRawUpgradeLevel(upgrade);
			//ChatUtil.printDebug("Upgrade "+upgrade.toString()+" is at level "+currentLevel+" out of "+upgrade.getMaxLevel());
			if(currentLevel < upgrade.getMaxLevel()) {
				result.put(upgrade, currentLevel+1);
			}
		}
		return result;
	}
	
	public int getUpgradeCost(KonUpgrade upgrade, int level) {
		int result = 0;
		if(level > 0 && level <= upgrade.getMaxLevel()) {
			if(upgradeCosts.containsKey(upgrade)) {
				result = upgradeCosts.get(upgrade).get(level-1);
			}
		}
		return result;
	}
	
	public int getUpgradePopulation(KonUpgrade upgrade, int level) {
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
	 * @param town
	 * @param upgrade
	 * @param level
	 * @return True on successful addition of the upgrade, else false.
	 */
	public boolean addTownUpgrade(KonTown town, KonUpgrade upgrade, int level, Player bukkitPlayer) {
		// Check that upgrades are enabled
		if(!isEnabled) {
			ChatUtil.sendError(bukkitPlayer, "Error adding "+upgrade.getDescription()+" level "+level+": upgrades are currently disabled. Talk to an admin!");
			return false;
		}
		// Check that the town has the previous level, if applicable
		int currentLevel = town.getUpgradeLevel(upgrade);
		if(level < 1 || level > upgrade.getMaxLevel() || level != currentLevel+1) {
			ChatUtil.sendError(bukkitPlayer, "Error adding "+upgrade.getDescription()+" level "+level+": level is greater than maximum or too high from current level ("+currentLevel+")");
			return false;
		}
		// Check that the town has enough population
		int currentPopulation = town.getNumResidents();
		int requiredPopulation = upgradePopulations.get(upgrade).get(level-1);
		if(currentPopulation < requiredPopulation) {
			ChatUtil.sendError(bukkitPlayer, "Error adding "+upgrade.getDescription()+" level "+level+": town population is too low, requires "+requiredPopulation);
			return false;
		}
		// Check that the player has enough favor
		int requiredCost = upgradeCosts.get(upgrade).get(level-1);
		if(KonquestPlugin.getEconomy().getBalance(bukkitPlayer) < requiredCost) {
			ChatUtil.sendError(bukkitPlayer, "Error adding "+upgrade.getDescription()+" level "+level+": requires "+requiredCost+" Favor");
            return false;
		}
		// Passed all checks, add the upgrade
		town.addUpgrade(upgrade, level);
		if(upgrade.equals(KonUpgrade.HEALTH)) {
			konquest.getKingdomManager().refreshTownHearts(town);
		}
		ChatUtil.sendNotice(bukkitPlayer, "Added "+upgrade.getDescription()+" level "+level+"!");
		ChatUtil.printDebug("Applied new upgrade "+upgrade.getDescription()+" level "+level+" to town "+town.getName());
		// Withdraw cost
		KonPlayer player = konquest.getPlayerManager().getPlayer(bukkitPlayer);
		EconomyResponse r = KonquestPlugin.getEconomy().withdrawPlayer(bukkitPlayer, requiredCost);
        if(r.transactionSuccess()) {
        	String balanceF = String.format("%.2f",r.balance);
        	String amountF = String.format("%.2f",r.amount);
        	ChatUtil.sendNotice(bukkitPlayer, "Favor reduced by "+amountF+", total: "+balanceF);
        	konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)requiredCost);
        } else {
        	ChatUtil.sendError(bukkitPlayer, String.format("An error occured: %s", r.errorMessage));
        }
		return true;
	}
	
	public boolean forceTownUpgrade(KonTown town, KonUpgrade upgrade, int level, Player bukkitPlayer) {
		// Check that upgrades are enabled
		if(!isEnabled) {
			ChatUtil.sendError(bukkitPlayer, "Error forcing "+upgrade.getDescription()+" level "+level+": upgrades are currently disabled.");
			return false;
		}
		town.addUpgrade(upgrade, level);
		if(upgrade.equals(KonUpgrade.HEALTH)) {
			konquest.getKingdomManager().refreshTownHearts(town);
		}
		ChatUtil.sendNotice(bukkitPlayer, "Forced "+upgrade.getDescription()+" level "+level+" to town "+town.getName());
		ChatUtil.printDebug("Applied forced upgrade "+upgrade.getDescription()+" level "+level+" to town "+town.getName());
		return true;
	}
	
	public int getTownUpgradeLevel(KonTown town, KonUpgrade upgrade) {
		int result = 0;
		if(isEnabled) {
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
						ChatUtil.printDebug("Disabled upgrade "+upgrade.getDescription()+" from level "+level+" to "+newLevel+" for town "+town.getName());
					} else {
						// Current population is greater than or equal to this upgrade level's requirement
						boolean status = town.allowUpgrade(upgrade);
						if(status) {
							ChatUtil.printDebug("Successfully allowed upgrade "+upgrade.getDescription()+" level "+level+" for town "+town.getName());
						}
					}
				} else {
					// Upgrades are disabled in config, disable every purchased upgrade
					town.disableUpgrade(upgrade,0);
				}
			}
		}
	}
	
	
}
