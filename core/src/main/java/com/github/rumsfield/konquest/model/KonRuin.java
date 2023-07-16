package com.github.rumsfield.konquest.model;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestRuin;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.utility.*;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class KonRuin extends KonTerritory implements KonquestRuin, KonBarDisplayer, KonPropertyFlagHolder, Timeable {

	private final Timer spawnTimer;
	private final Timer captureTimer;
	private boolean isCaptureDisabled;
	private final BossBar ruinBarAll;
	private final Map<KonPropertyFlag,Boolean> properties;
	private final HashMap<Location,Boolean> criticalLocations; // Block location, enabled flag
	private final HashMap<Location,KonRuinGolem> spawnLocations; // Block location, Ruin Golem

	/*
	 * Ruin Golem Behavior:
	 * + All spawn when player enters ruin & not on respawn cool-down & ruin is not disabled
	 * + All alive target player that interacts with critical block (begins mining)
	 * + When a golem is killed, respawn at location after cool-down if players are inside ruin & ruin is not disabled
	 * + When a target player leaves the ruin, stop targeting
	 * + Kill all alive when a ruin is captured
	 * + Remove all when a ruin ends capture cool-down & no players are inside
	 * + When a golem changes target to a monster due to pathfinding, kill the monster and switch targets back to last player
	 */
	public KonRuin(Location loc, String name, KonKingdom kingdom, Konquest konquest) {
		super(loc, name, kingdom, konquest);
		this.spawnTimer = new Timer(this);
		this.captureTimer = new Timer(this);
		this.isCaptureDisabled = false;
		this.ruinBarAll = Bukkit.getServer().createBossBar(Konquest.neutralColor1+MessagePath.TERRITORY_RUIN.getMessage().trim()+" "+getName(), BarColor.WHITE, BarStyle.SOLID);
		this.ruinBarAll.setVisible(true);
		this.criticalLocations = new HashMap<>();
		this.spawnLocations = new HashMap<>();
		this.properties = new HashMap<>();
		initProperties();
	}

	public static java.util.List<KonPropertyFlag> getProperties() {
		java.util.List<KonPropertyFlag> result = new ArrayList<>();
		result.add(KonPropertyFlag.PVP);
		result.add(KonPropertyFlag.PVE);
		result.add(KonPropertyFlag.USE);
		result.add(KonPropertyFlag.CHEST);
		result.add(KonPropertyFlag.PORTALS);
		result.add(KonPropertyFlag.ENTER);
		result.add(KonPropertyFlag.EXIT);
		return result;
	}

	@Override
	public void initProperties() {
		properties.clear();
		for (KonPropertyFlag flag : getProperties()) {
			properties.put(flag, getKonquest().getConfigManager().getConfig("properties").getBoolean("properties.ruins."+flag.toString().toLowerCase()));
		}
	}

	@Override
	public boolean setPropertyValue(KonPropertyFlag property, boolean value) {
		boolean result = false;
		if(properties.containsKey(property)) {
			properties.put(property, value);
			result = true;
		}
		return result;
	}

	@Override
	public boolean getPropertyValue(KonPropertyFlag property) {
		boolean result = false;
		if(properties.containsKey(property)) {
			result = properties.get(property);
		}
		return result;
	}

	@Override
	public boolean hasPropertyValue(KonPropertyFlag property) {
		return properties.containsKey(property);
	}

	@Override
	public Map<KonPropertyFlag, Boolean> getAllProperties() {
		return new HashMap<>(properties);
	}

	@Override
	public int initClaim() {
		// Upon territory creation...
		// Add center chunk claim
		addChunk(Konquest.toPoint(getCenterLoc()));
		return 0;
	}

	@Override
	public boolean addChunk(Point point) {
		addPoint(point);
		return true;
	}
	
	@Override
	public boolean testChunk(Point point) {
		return true;
	}

	@Override
	public void onEndTimer(int taskID) {
		if(taskID == 0) {
			ChatUtil.printDebug("Ruin Timer ended with null taskID!");
		} else if(taskID == spawnTimer.getTaskID()) {
			// Spawn random mobs at valid spawn locations
			//TODO
			
		} else if(taskID == captureTimer.getTaskID()) {
			ChatUtil.printDebug("Ruin Capture Timer ended with taskID: "+taskID);
			// When a capture cooldown timer ends
			isCaptureDisabled = false;
			setBarProgress(1.0);
			regenCriticalBlocks();
			if(isEmpty()) {
				removeAllGolems();
			} else {
				spawnAllGolems();
			}
		} else {
			// Check for respawn timer in golem spawn map
			for(KonRuinGolem golem : spawnLocations.values()) {
				if(taskID == golem.getRespawnTimer().getTaskID()) {
					// This golem's respawn timer expired, try to spawn it
					golem.setIsRespawnCooldown(false);
					if(!isCaptureDisabled && !isEmpty()) {
						golem.spawn();
					} else {
						ChatUtil.printDebug("Failed to respawn golem in disabled or empty ruin "+getName());
					}
				}
			}
		}
	}
	
	public void regenCriticalBlocks() {
		for(Location loc : criticalLocations.keySet()) {
			criticalLocations.put(loc, true);
			loc.getWorld().getBlockAt(loc).setType(getKonquest().getRuinManager().getRuinCriticalBlock());
			loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_PLACE, 1.0F, 0.6F);
		}
	}
	
	public boolean isCaptureDisabled() {
		return isCaptureDisabled;
	}
	
	public void setIsCaptureDisabled(boolean val) {
		isCaptureDisabled = val;
		if(val) {
			killAllGolems();
		}
	}
	
	public Timer getSpawnTimer() {
		return spawnTimer;
	}
	
	public Timer getCaptureTimer() {
		return captureTimer;
	}
	
	public String getCaptureCooldownString() {
		return String.format("%02d:%02d", captureTimer.getMinutes(), captureTimer.getSeconds());
	}
	
	public void addBarPlayer(KonPlayer player) {
		ruinBarAll.addPlayer(player.getBukkitPlayer());
	}
	
	public void removeBarPlayer(KonPlayer player) {
		ruinBarAll.removePlayer(player.getBukkitPlayer());
	}
	
	public void removeAllBarPlayers() {
		ruinBarAll.removeAll();
	}
	
	public void updateBarPlayers() {
		ruinBarAll.removeAll();
		for(KonPlayer player : getKonquest().getPlayerManager().getPlayersOnline()) {
			Player bukkitPlayer = player.getBukkitPlayer();
			if(isLocInside(bukkitPlayer.getLocation())) {
				ruinBarAll.addPlayer(bukkitPlayer);
			}
		}
	}

	@Override
	public void updateBarTitle() {
		ruinBarAll.setTitle(Konquest.neutralColor1+MessagePath.TERRITORY_RUIN.getMessage().trim()+" "+getName());
	}
	
	public void setBarProgress(double progress) {
		ruinBarAll.setProgress(progress);
	}

	public boolean addCriticalLocation(Location loc) {
		// Check that the location is inside of this ruin
		if(!this.isLocInside(loc)) return false;
		// Check that the block at the location is a critical type
		if(!loc.getBlock().getType().equals(getKonquest().getRuinManager().getRuinCriticalBlock())) return false;
		criticalLocations.put(loc, true);
		getKonquest().getMapHandler().drawDynmapLabel(this);
		return true;
	}
	
	public boolean addCriticalLocation(Set<Location> locs) {
		for(Location loc : locs) {
			if(!addCriticalLocation(loc)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean addSpawnLocation(Location loc) {
		// Check that the location is inside of this ruin
		if(!this.isLocInside(loc)) return false;
		spawnLocations.put(loc, new KonRuinGolem(loc, this));
		getKonquest().getMapHandler().drawDynmapLabel(this);
		return true;
	}
	
	public boolean addSpawnLocation(Set<Location> locs) {
		for(Location loc : locs) {
			if(!addSpawnLocation(loc)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isCriticalLocation(Location loc) {
		return criticalLocations.containsKey(loc);
	}
	
	public boolean isSpawnLocation(Location loc) {
		return spawnLocations.containsKey(loc);
	}
	
	public boolean setCriticalLocationEnabled(Location loc, boolean val) {
		boolean result = false;
		if(criticalLocations.containsKey(loc)) {
			criticalLocations.put(loc, val);
			result = true;
		}
		return result;
	}
	
	public void clearCriticalLocations() {
		criticalLocations.clear();
	}
	
	public void clearSpawnLocations() {
		removeAllGolems();
		spawnLocations.clear();
	}
	
	public Set<Location> getCriticalLocations() {
		return criticalLocations.keySet();
	}
	
	public Set<Location> getSpawnLocations() {
		return spawnLocations.keySet();
	}
	
	// Returns total number of critical blocks
	public int getMaxCriticalHits() {
		int result = 0;
		if(!criticalLocations.isEmpty()) {
			result = criticalLocations.size();
		}
		return result;
	}

	// Returns remaining number of unbroken critical blocks
	public int getRemainingCriticalHits() {
		int result = 0;
		if(!criticalLocations.isEmpty()) {
			for(boolean status : criticalLocations.values()) {
				if(status) {
					result++;
				}
			}
		}
		return result;
	}
	
	// Returns true if no players are located within ruin territory
	public boolean isEmpty() {
		boolean result = true;
		for(KonPlayer player : getKonquest().getPlayerManager().getPlayersOnline()) {
			Location playerLoc = player.getBukkitPlayer().getLocation();
			if(getKonquest().getTerritoryManager().isChunkClaimed(playerLoc)) {
				if(getKonquest().getTerritoryManager().getChunkTerritory(playerLoc).equals(this)) {
					result = false;
					break;
				}
			}
		}
		return result;
	}

	/*
	 * Ruin Golem Methods
	 */
	public void spawnAllGolems() {
		if(!isCaptureDisabled) {
			// Prune any golems not linked to this ruin
			int min_distance_town = getKonquest().getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_TOWN.getPath());
			for(Entity ent : getCenterLoc().getWorld().getNearbyEntities(getCenterLoc(), min_distance_town*16, 64, min_distance_town*16)) {
				if(ent.getType().equals(EntityType.IRON_GOLEM) && ent instanceof IronGolem) {
					IronGolem foundGolem = (IronGolem)ent;
					boolean foundMatches = false;
					for(KonRuinGolem golem : spawnLocations.values()) {
						if(golem.matches(foundGolem)) {
							foundMatches = true;
						}
					}
					if(!foundMatches) {
						foundGolem.remove();
						foundGolem = null;
						ChatUtil.printDebug("Pruned Iron Golem in ruin "+getName());
					}
				}
			}
			// Spawn all golems
			for(KonRuinGolem golem : spawnLocations.values()) {
				golem.spawn();
			}
		}
	}
	
	public void respawnAllGolems() {
		if(!isCaptureDisabled) {
			for(KonRuinGolem golem : spawnLocations.values()) {
				golem.respawn();
			}
		}
	}
	
	public void respawnAllDistantGolems() {
		if(!isCaptureDisabled) {
			for(Location spawnLoc : spawnLocations.keySet()) {
				KonRuinGolem golem = spawnLocations.get(spawnLoc);
				if(golem.getLocation().distance(spawnLoc) > 32) {
					golem.respawn();
				}
			}
		}
	}
	
	public void removeAllGolems() {
		for(KonRuinGolem golem : spawnLocations.values()) {
			golem.remove();
		}
	}
	
	public void killAllGolems() {
		for(KonRuinGolem golem : spawnLocations.values()) {
			golem.kill();
		}
	}
	
	public void targetAllGolemsToPlayer(Player player) {
		for(KonRuinGolem golem : spawnLocations.values()) {
			golem.targetTo(player);
		}
	}
	
	public void targetGolemToPlayer(Player player, IronGolem targetGolem) {
		for(KonRuinGolem golem : spawnLocations.values()) {
			if(golem.matches(targetGolem)) {
				golem.targetTo(player);
			}
		}
	}
	
	public void targetGolemToLast(IronGolem targetGolem) {
		for(KonRuinGolem golem : spawnLocations.values()) {
			if(golem.matches(targetGolem)) {
				golem.targetToLast();
			}
		}
	}
	
	public LivingEntity getGolemLastTarget(IronGolem targetGolem) {
		LivingEntity result = null;
		for(KonRuinGolem golem : spawnLocations.values()) {
			if(golem.matches(targetGolem)) {
				result = golem.getLastTarget();
			}
		}
		return result;
	}
	
	public void stopTargetingPlayer(Player player) {
		for(KonRuinGolem golem : spawnLocations.values()) {
			if(golem.isTarget(player)) {
				golem.dropTargets();
			}
		}
	}
	
	public boolean isGolem(IronGolem testGolem) {
		boolean result = false;
		for(KonRuinGolem golem : spawnLocations.values()) {
			if(golem.matches(testGolem)) {
				result = true;
			}
		}
		return result;
	}
	
	public void onGolemDeath(IronGolem deadGolem) {
		// When a golem dies, find it and attempt to initiate respawn cooldown
		for(KonRuinGolem golem : spawnLocations.values()) {
			if(golem.matches(deadGolem)) {
				golem.setIsRespawnCooldown(true);
				golem.setLastTarget(null);
				int golemRespawnTimeSeconds = getKonquest().getCore().getInt(CorePath.RUINS_RESPAWN_COOLDOWN.getPath());
				golem.getRespawnTimer().stopTimer();
				golem.getRespawnTimer().setTime(golemRespawnTimeSeconds);
				golem.getRespawnTimer().startTimer();
			}
		}
	}
	
	@Override
	public KonquestTerritoryType getTerritoryType() {
		return KonquestTerritoryType.RUIN;
	}
	
}
