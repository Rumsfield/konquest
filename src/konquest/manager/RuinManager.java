package konquest.manager;

import java.awt.Point;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.api.manager.KonquestRuinManager;
import konquest.model.KonPlayer;
import konquest.model.KonRuin;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class RuinManager implements KonquestRuinManager {

	private Konquest konquest;
	private KingdomManager kingdomManager;
	private HashMap<String, KonRuin> ruinMap; // lower case name maps to ruin object
	private Material ruinCriticalBlock;
	
	public RuinManager(Konquest konquest) {
		this.konquest = konquest;
		this.kingdomManager = konquest.getKingdomManager();
		this.ruinMap = new HashMap<String, KonRuin>();
		this.ruinCriticalBlock = Material.OBSIDIAN;
	}
	
	public void initialize() {
		loadCriticalBlocks();
		loadRuins();
		regenAllRuins();
		ChatUtil.printDebug("Ruin Manager is ready");
	}
	
	public void regenAllRuins() {
		for(KonRuin ruin : ruinMap.values()) {
			ruin.regenCriticalBlocks();
		}
	}
	
	public void removeAllGolems() {
		for(KonRuin ruin : ruinMap.values()) {
			ruin.removeAllGolems();
		}
	}
	
	public void rewardPlayers(KonRuin ruin, KonPlayer capturePlayer) {
		int rewardFavor = konquest.getConfigManager().getConfig("core").getInt("core.ruins.capture_reward_favor",0);
		int rewardExp = konquest.getConfigManager().getConfig("core").getInt("core.ruins.capture_reward_exp",0);
		for(KonPlayer friendly : konquest.getPlayerManager().getPlayersInKingdom(capturePlayer.getKingdom())) {
			if(isLocInsideRuin(ruin,friendly.getBukkitPlayer().getLocation())) {
				// Give reward to player
				if(rewardFavor > 0) {
					ChatUtil.printDebug("Ruin capture favor rewarded to player "+friendly.getBukkitPlayer().getName());
		            if(KonquestPlugin.depositPlayer(friendly.getBukkitPlayer(), rewardFavor)) {
		            	ChatUtil.sendNotice(friendly.getBukkitPlayer(), ChatColor.LIGHT_PURPLE+MessagePath.PROTECTION_NOTICE_RUIN.getMessage(ruin.getName()));
		            }
				}
				if(rewardExp > 0) {
					friendly.getBukkitPlayer().giveExp(rewardExp);
					//ChatUtil.sendNotice(friendly.getBukkitPlayer(), ChatColor.WHITE+"EXP rewarded: "+ChatColor.GREEN+rewardExp);
					ChatUtil.sendNotice(friendly.getBukkitPlayer(), MessagePath.GENERIC_NOTICE_REWARD_EXP.getMessage(rewardExp));
				}
			}
		}
	}
	
	public boolean isRuin(String name) {
		// Check for lower-case name only
		return ruinMap.containsKey(name.toLowerCase());
	}
	
	public boolean isLocInsideRuin(KonRuin ruin, Location loc) {
		boolean result = false;
		if(kingdomManager.isChunkClaimed(loc)) {
			if(ruin.equals(kingdomManager.getChunkTerritory(loc))) {
				result = true;
			}
		}
		return result;
	}
	
	public boolean addRuin(Location loc, String name) {
		boolean result = false;
		if(!name.contains(" ") && !isRuin(name)) {
			// Verify no overlapping init chunks
			for(Point point : konquest.getAreaPoints(loc, 2)) {
				if(kingdomManager.isChunkClaimed(point,loc.getWorld())) {
					ChatUtil.printDebug("Found a chunk conflict during ruin init: "+name);
					return false;
				}
			}
			// Add ruin to map with lower-case key
			String nameLower = name.toLowerCase();
			ruinMap.put(nameLower, new KonRuin(loc, name, kingdomManager.getNeutrals(), konquest));
			ruinMap.get(nameLower).initClaim();
			ruinMap.get(nameLower).updateBarPlayers();
			kingdomManager.addAllTerritory(loc.getWorld(),ruinMap.get(nameLower).getChunkList());
			konquest.getMapHandler().drawDynmapUpdateTerritory(ruinMap.get(nameLower));
			result = true;
		}
		return result;
	}
	
	public boolean removeRuin(String name) {
		boolean result = false;
		KonRuin oldRuin = ruinMap.remove(name.toLowerCase());
		if(oldRuin != null) {
			oldRuin.removeAllBarPlayers();
			oldRuin.removeAllGolems();
			kingdomManager.removeAllTerritory(oldRuin.getCenterLoc().getWorld(), oldRuin.getChunkList().keySet());
			konquest.getMapHandler().drawDynmapRemoveTerritory(oldRuin);
			ChatUtil.printDebug("Removed Ruin "+name);
			oldRuin = null;
			result = true;
		}
		return result;
	}
	
	public KonRuin getRuin(String name) {
		return ruinMap.get(name.toLowerCase());
	}
	
	public Collection<KonRuin> getRuins() {
		return ruinMap.values();
	}
	
	public Set<String> getRuinNames() {
		return ruinMap.keySet();
	}
	
	public Material getRuinCriticalBlock() {
		return ruinCriticalBlock;
	}
	
	private void loadCriticalBlocks() {
		String ruinCriticalBlockTypeName = konquest.getConfigManager().getConfig("core").getString("core.ruins.critical_block","");
		try {
			ruinCriticalBlock = Material.valueOf(ruinCriticalBlockTypeName);
		} catch(IllegalArgumentException e) {
			String message = "Invalid ruin critical block \""+ruinCriticalBlockTypeName+"\" given in core.ruins.critical_block, using default OBSIDIAN";
    		ChatUtil.printConsoleError(message);
    		konquest.opStatusMessages.add(message);
		}
	}
	
	private void loadRuins() {
		FileConfiguration ruinsConfig = konquest.getConfigManager().getConfig("ruins");
        if (ruinsConfig.get("ruins") == null) {
        	ChatUtil.printDebug("There is no ruins section in ruins.yml");
            return;
        }
        double x,y,z;
        List<Double> sectionList;
        String worldName;
        KonRuin ruin;
        // Load all Ruins
        ConfigurationSection ruinsSection = ruinsConfig.getConfigurationSection("ruins");
        for(String ruinName : ruinsConfig.getConfigurationSection("ruins").getKeys(false)) {
        	ConfigurationSection ruinSection = ruinsSection.getConfigurationSection(ruinName);
        	worldName = ruinSection.getString("world","world");
        	sectionList = ruinSection.getDoubleList("center");
    		x = sectionList.get(0);
    		y = sectionList.get(1);
    		z = sectionList.get(2);
    		World world = Bukkit.getWorld(worldName);
    		if(world != null) {
	        	Location ruin_center = new Location(world,x,y,z);
	        	if(addRuin(ruin_center,ruinName)) {
	        		ruin = getRuin(ruinName);
	        		if(ruin != null) {
	        			ruin.addPoints(konquest.formatStringToPoints(ruinSection.getString("chunks","")));
	                	for(Location loc : konquest.formatStringToLocations(ruinSection.getString("criticals",""),world)) {
	                		loc.setWorld(world);
	                		ruin.addCriticalLocation(loc);
	            		}
	                	for(Location loc : konquest.formatStringToLocations(ruinSection.getString("spawns",""),world)) {
	                		loc.setWorld(world);
	                		ruin.addSpawnLocation(loc);
	            		}
	                	kingdomManager.addAllTerritory(world,ruin.getChunkList());
	        		} else {
	        			String message = "Could not load ruin "+ruinName+", ruins.yml may be corrupted and needs to be deleted.";
	            		ChatUtil.printConsoleError(message);
	            		konquest.opStatusMessages.add(message);
	        		}
	        	} else {
	        		String message = "Failed to load ruin "+ruinName+", ruins.yml may be corrupted and needs to be deleted.";
	        		ChatUtil.printConsoleError(message);
	        		konquest.opStatusMessages.add(message);
	        	}
    		} else {
    			String message = "Failed to load ruin "+ruinName+" in an unloaded world, "+worldName+". Check plugin load order.";
    			ChatUtil.printConsoleError(message);
    			konquest.opStatusMessages.add(message);
    		}
        }
	}
	
	public void saveRuins() {
		FileConfiguration ruinsConfig = konquest.getConfigManager().getConfig("ruins");
		ruinsConfig.set("ruins", null); // reset ruins config
		ConfigurationSection root = ruinsConfig.createSection("ruins");
		for(String name : ruinMap.keySet()) {
			KonRuin ruin = ruinMap.get(name);
			ConfigurationSection ruinSection = root.createSection(ruin.getName());
			ruinSection.set("world", ruin.getWorld().getName());
			ruinSection.set("center", new int[] {(int) ruin.getCenterLoc().getBlockX(),
					 							 (int) ruin.getCenterLoc().getBlockY(),
					 							 (int) ruin.getCenterLoc().getBlockZ()});
			ruinSection.set("chunks", konquest.formatPointsToString(ruin.getChunkList().keySet()));
			ruinSection.set("criticals", konquest.formatLocationsToString(ruin.getCriticalLocations()));
			ruinSection.set("spawns", konquest.formatLocationsToString(ruin.getSpawnLocations()));
		}
	}
	
}
