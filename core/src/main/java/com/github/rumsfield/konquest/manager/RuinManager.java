package com.github.rumsfield.konquest.manager;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.api.manager.KonquestRuinManager;
import com.github.rumsfield.konquest.api.model.KonquestRuin;
import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonRuin;
import com.github.rumsfield.konquest.model.KonSanctuary;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.awt.*;
import java.util.List;
import java.util.*;

public class RuinManager implements KonquestRuinManager {

	private final Konquest konquest;
	private final HashMap<String, KonRuin> ruinMap; // lower case name maps to ruin object
	private Material ruinCriticalBlock;
	private boolean isRuinDataNull;
	
	public RuinManager(Konquest konquest) {
		this.konquest = konquest;
		this.ruinMap = new HashMap<>();
		this.ruinCriticalBlock = Material.OBSIDIAN;
		this.isRuinDataNull = false;
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
	
	public void rewardPlayers(KonRuin ruin, KonKingdom kingdom) {
		int rewardFavor = konquest.getCore().getInt(CorePath.RUINS_CAPTURE_REWARD_FAVOR.getPath(),0);
		int rewardExp = konquest.getCore().getInt(CorePath.RUINS_CAPTURE_REWARD_EXP.getPath(),0);
		for(KonPlayer friendly : getRuinPlayers(ruin,kingdom)) {
			// Give reward to player
			if(rewardFavor > 0) {
				ChatUtil.printDebug("Ruin capture favor rewarded to player "+friendly.getBukkitPlayer().getName());
	            if(KonquestPlugin.depositPlayer(friendly.getBukkitPlayer(), rewardFavor)) {
	            	ChatUtil.sendNotice(friendly.getBukkitPlayer(), ChatColor.LIGHT_PURPLE+MessagePath.PROTECTION_NOTICE_RUIN.getMessage(ruin.getName()));
	            }
			}
			if(rewardExp > 0) {
				friendly.getBukkitPlayer().giveExp(rewardExp);
				ChatUtil.sendNotice(friendly.getBukkitPlayer(), MessagePath.GENERIC_NOTICE_REWARD_EXP.getMessage(rewardExp));
			}
		}
	}
	
	// returns list of players in kingdom inside ruin
	public List<KonPlayer> getRuinPlayers(KonRuin ruin, KonKingdom kingdom) {
		List<KonPlayer> players = new ArrayList<>();
		for(KonPlayer friendly : konquest.getPlayerManager().getPlayersInKingdom(kingdom)) {
			if(isLocInsideRuin(ruin,friendly.getBukkitPlayer().getLocation())) {
				players.add(friendly);
			}
		}
		return players;
	}
	
	public boolean isRuin(String name) {
		// Check for lower-case name only
		return ruinMap.containsKey(name.toLowerCase());
	}
	
	public boolean isLocInsideRuin(KonquestRuin ruinArg, Location loc) {
		boolean result = false;
		if(ruinArg instanceof KonRuin) {
			KonRuin ruin = (KonRuin) ruinArg;
			if(konquest.getTerritoryManager().isChunkClaimed(loc)) {
				if(ruin.equals(konquest.getTerritoryManager().getChunkTerritory(loc))) {
					result = true;
				}
			}
		}
		return result;
	}
	
	public boolean addRuin(Location loc, String name) {
		boolean result = false;
		if(konquest.validateNameConstraints(name) == 0) {
			// Verify no overlapping init chunks
			for(Point point : konquest.getAreaPoints(loc, 2)) {
				if(konquest.getTerritoryManager().isChunkClaimed(point,loc.getWorld())) {
					ChatUtil.printDebug("Found a chunk conflict during ruin init: "+name);
					return false;
				}
			}
			// Add ruin to map with lower-case key
			String nameLower = name.toLowerCase();
			ruinMap.put(nameLower, new KonRuin(loc, name, konquest.getKingdomManager().getNeutrals(), konquest));
			ruinMap.get(nameLower).initClaim();
			ruinMap.get(nameLower).updateBarPlayers();
			// Update territory cache
			konquest.getTerritoryManager().addAllTerritory(loc.getWorld(),ruinMap.get(nameLower).getChunkList());
			// Update border particles
			konquest.getTerritoryManager().updatePlayerBorderParticles(loc);
			// Update maps
			konquest.getMapHandler().drawDynmapUpdateTerritory(ruinMap.get(nameLower));
			result = true;
		}
		return result;
	}
	
	public boolean removeRuin(String name) {
		boolean result = false;
		KonRuin oldRuin = ruinMap.remove(name.toLowerCase());
		if(oldRuin != null) {
			ArrayList<KonPlayer> nearbyPlayers = konquest.getPlayerManager().getPlayersNearTerritory(oldRuin);
			oldRuin.removeAllBarPlayers();
			oldRuin.removeAllGolems();
			// Update territory cache
			konquest.getTerritoryManager().removeAllTerritory(oldRuin.getCenterLoc().getWorld(), oldRuin.getChunkList().keySet());
			// Update border particles
			for(KonPlayer player : nearbyPlayers) {
				konquest.getTerritoryManager().updatePlayerBorderParticles(player);
			}
			konquest.getMapHandler().drawDynmapRemoveTerritory(oldRuin);
			ChatUtil.printDebug("Removed Ruin "+name);
			oldRuin = null;
			result = true;
		}
		return result;
	}
	
	public boolean renameRuin(String name, String newName) {
		boolean result = false;
		if(isRuin(name) && konquest.validateNameConstraints(newName) == 0) {
			ruinMap.get(name.toLowerCase()).setName(newName);
			KonRuin ruin = ruinMap.remove(name.toLowerCase());
			ruinMap.put(newName.toLowerCase(), ruin);
			ruin.updateBarTitle();
			ruin.updateBarPlayers();
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
		Set<String> result = new HashSet<>();
		for(KonRuin ruin : ruinMap.values()) {
			result.add(ruin.getName());
		}
		return result;
	}
	
	public Material getRuinCriticalBlock() {
		return ruinCriticalBlock;
	}
	
	private void loadCriticalBlocks() {
		String ruinCriticalBlockTypeName = konquest.getCore().getString(CorePath.RUINS_CRITICAL_BLOCK.getPath(),"");
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
        	ChatUtil.printConsoleError("Failed to load any ruins from ruins.yml! Check file permissions.");
			isRuinDataNull = true;
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
	                	konquest.getTerritoryManager().addAllTerritory(world,ruin.getChunkList());
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
		if(isRuinDataNull && ruinMap.isEmpty()) {
			// There was probably an issue loading ruins, do not save.
			ChatUtil.printConsoleError("Aborted saving ruin data because a problem was encountered while loading data from ruins.yml");
			return;
		}
		FileConfiguration ruinsConfig = konquest.getConfigManager().getConfig("ruins");
		ruinsConfig.set("ruins", null); // reset ruins config
		ConfigurationSection root = ruinsConfig.createSection("ruins");
		for(String name : ruinMap.keySet()) {
			KonRuin ruin = ruinMap.get(name);
			ConfigurationSection ruinSection = root.createSection(ruin.getName());
			ruinSection.set("world", ruin.getWorld().getName());
			ruinSection.set("center", new int[] {ruin.getCenterLoc().getBlockX(),
					ruin.getCenterLoc().getBlockY(),
					ruin.getCenterLoc().getBlockZ()});
			ruinSection.set("chunks", konquest.formatPointsToString(ruin.getChunkList().keySet()));
			ruinSection.set("criticals", konquest.formatLocationsToString(ruin.getCriticalLocations()));
			ruinSection.set("spawns", konquest.formatLocationsToString(ruin.getSpawnLocations()));
		}
	}
	
}
