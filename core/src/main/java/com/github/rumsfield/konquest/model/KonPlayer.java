package com.github.rumsfield.konquest.model;

import com.github.rumsfield.konquest.api.model.KonquestPlayer;
import com.github.rumsfield.konquest.utility.*;
import com.github.rumsfield.konquest.utility.Timer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.*;

public class KonPlayer extends KonOfflinePlayer implements KonquestPlayer, Timeable {

	public enum RegionType {
		NONE,
		MONUMENT,
		RUIN_CRITICAL,
		RUIN_SPAWN
	}
	
	/*
	 * Follow logic:
	 * 	Player starts with NONE, can enter any type state.
	 * 	While in any type, enter again to return to NONE.
	 *  While in any type and enter into a new type, switch to new state.
	 */
	public enum FollowType {
		NONE,
		CLAIM,
		UNCLAIM,
		ADMIN_CLAIM,
		ADMIN_UNCLAIM
	}
	
	private final Player bukkitPlayer;
	
	private RegionType settingRegion;
	private FollowType autoFollow;
	private String regionTemplateName;
	private String regionSanctuaryName;
	private Location regionCornerOneBuffer;
	private Location regionCornerTwoBuffer;
	private double regionTemplateCost;
	
	private boolean isAdminBypassActive;
	private boolean isGlobalChat;
	private boolean isMapAuto;
	private boolean isPriorityTitleDisplay;
	private boolean isCombatTagged;
	private boolean isFlying;
	private boolean isBorderDisplay;
	private final Timer priorityTitleDisplayTimer;
	private final Timer borderUpdateLoopTimer;
	private final Timer monumentTemplateLoopTimer;
	private final Timer monumentShowLoopTimer;
	private final Timer combatTagTimer;
	private final Timer flyDisableWarmupTimer;
	private long recordPlayCooldownTime;
	private int monumentShowLoopCount;
	private long flyDisableTime;
	private final ArrayList<Mob> targetMobList;
	private final HashMap<KonDirective,Integer> directiveProgress;
	private final KonStats playerStats;
	private final KonPrefix playerPrefix;
	private final HashMap<Location, Color> borderMap;
	private final HashMap<Location, Color> borderPlotMap;
	private final HashMap<Location,Color> monumentTemplateBoundary;
	private final HashSet<Location> monumentShowBoundary;
	private Block lastTargetBlock;
	private final KonClaimRegister adminClaimRegister;
	
	public KonPlayer(Player bukkitPlayer, KonKingdom kingdom, boolean isBarbarian) {
		super(bukkitPlayer, kingdom, isBarbarian);

		this.bukkitPlayer = bukkitPlayer;
		this.settingRegion = RegionType.NONE;
		this.regionTemplateName = "";
		this.regionSanctuaryName = "";
		this.regionTemplateCost = 0;
		this.isAdminBypassActive = false;
		this.autoFollow = FollowType.NONE;
		this.isGlobalChat = true;
		this.isMapAuto = false;
		this.isPriorityTitleDisplay = false;
		this.isCombatTagged = false;
		this.isFlying = false;
		this.isBorderDisplay = true;
		this.priorityTitleDisplayTimer = new Timer(this);
		this.borderUpdateLoopTimer = new Timer(this);
		this.monumentTemplateLoopTimer = new Timer(this);
		this.monumentShowLoopTimer = new Timer(this);
		this.combatTagTimer = new Timer(this);
		this.flyDisableWarmupTimer = new Timer(this);
		this.recordPlayCooldownTime = 0;
		this.monumentShowLoopCount = 0;
		this.flyDisableTime = 0;
		this.targetMobList = new ArrayList<>();
		this.directiveProgress = new HashMap<>();
		this.playerStats = new KonStats();
		this.playerPrefix = new KonPrefix();
		this.adminClaimRegister = new KonClaimRegister();
		this.borderMap = new HashMap<>();
		this.borderPlotMap = new HashMap<>();
		this.monumentTemplateBoundary = new HashMap<>();
		this.monumentShowBoundary = new HashSet<>();
	}
	
	public void addMobAttacker(Mob mob) {
		if(!targetMobList.contains(mob)) {
			targetMobList.add(mob);
		}
	}
	
	public boolean removeMobAttacker(Mob mob) {
		return targetMobList.remove(mob);
	}
	
	public void clearAllMobAttackers() {
		for(Mob m : targetMobList) {
			m.setTarget(null);
		}
		targetMobList.clear();
	}
	
	public void setDirectiveProgress(KonDirective dir, int stage) {
		directiveProgress.put(dir, stage);
	}
	
	public int getDirectiveProgress(KonDirective dir) {
		int result = 0;
		if(directiveProgress.containsKey(dir)) {
			result = directiveProgress.get(dir);
		}
		return result;
	}
	
	// Getters
	public Player getBukkitPlayer() {
		return bukkitPlayer;
	}
	
	public boolean isSettingRegion() {
		return (!settingRegion.equals(RegionType.NONE));
	}
	
	public RegionType getRegionType() {
		return settingRegion;
	}
	
	public Location getRegionCornerOneBuffer() {
		return regionCornerOneBuffer;
	}
	
	public Location getRegionCornerTwoBuffer() {
		return regionCornerTwoBuffer;
	}
	
	public String getRegionTemplateName() {
		return regionTemplateName;
	}
	
	public String getRegionSanctuaryName() {
		return regionSanctuaryName;
	}

	public double getRegionTemplateCost() {
		return regionTemplateCost;
	}
	
	public boolean isAdminBypassActive() {
		return isAdminBypassActive;
	}
	
	public boolean isAutoFollowActive() {
		return (!autoFollow.equals(FollowType.NONE));
	}
	
	public FollowType getAutoFollow() {
		return autoFollow;
	}
	
	public boolean isGlobalChat() {
		return isGlobalChat;
	}
	
	public boolean isMapAuto() {
		return isMapAuto;
	}
	
	public boolean isPriorityTitleDisplay() {
		return isPriorityTitleDisplay;
	}
	
	public boolean isCombatTagged() {
		return isCombatTagged;
	}
	
	public boolean isFlyEnabled() {
		return isFlying;
	}
	
	public boolean isBorderDisplay() {
		return isBorderDisplay;
	}
	
	public Timer getPriorityTitleDisplayTimer() {
		return priorityTitleDisplayTimer;
	}
	
	public KonStats getPlayerStats() {
		return playerStats;
	}
	
	public KonPrefix getPlayerPrefix() {
		return playerPrefix;
	}
	
	public KonClaimRegister getAdminClaimRegister() {
		return adminClaimRegister;
	}
	
	public Timer getBorderUpdateLoopTimer() {
		return borderUpdateLoopTimer;
	}
	
	public Timer getCombatTagTimer() {
		return combatTagTimer;
	}
	
	// Setters
	
	public void settingRegion(RegionType type) {
		settingRegion = type;
		// Manage monument template boundary timer
		if(type.equals(RegionType.MONUMENT)) {
			monumentTemplateLoopTimer.stopTimer();
			monumentTemplateLoopTimer.setTime(1);
			monumentTemplateLoopTimer.startLoopTimer(5);
			ChatUtil.printDebug("Starting monument template Timer for "+bukkitPlayer.getName());
		} else if(monumentTemplateLoopTimer.isRunning()){
			monumentTemplateLoopTimer.stopTimer();
			monumentTemplateBoundary.clear();
			ChatUtil.printDebug("Stopped running monument template Timer for "+bukkitPlayer.getName());
		} else {
			ChatUtil.printDebug("Doing nothing with monument template Timer for "+bukkitPlayer.getName());
		}
	}
	
	public void setRegionCornerOneBuffer(Location loc) {
		regionCornerOneBuffer = loc;
	}
	
	public void setRegionCornerTwoBuffer(Location loc) {
		regionCornerTwoBuffer = loc;
	}
	
	public void setRegionTemplateName(String name) {
		regionTemplateName = name;
	}
	
	public void setRegionSanctuaryName(String name) {
		regionSanctuaryName = name;
	}

	public void setRegionTemplateCost(double cost) {
		regionTemplateCost = cost;
	}
	
	public void setIsAdminBypassActive(boolean val) {
		isAdminBypassActive = val;
	}
	
	public void setAutoFollow(FollowType val) {
		autoFollow = val;
	}
	
	public void setIsGlobalChat(boolean val) {
		isGlobalChat = val;
	}
	
	public void setIsPriorityTitleDisplay(boolean val) {
		isPriorityTitleDisplay = val;
	}
	
	public void setIsCombatTagged(boolean val) {
		isCombatTagged = val;
	}
	
	public void setIsBorderDisplay(boolean val) {
		isBorderDisplay = val;
	}
	
	public void setIsFlyEnabled(boolean val) {
		try {
			if(val && !isFlying) {
				bukkitPlayer.setAllowFlight(true);
				isFlying = true;
	    	} else if(!val && isFlying) {
	    		// Attempt to tp the player to the ground beneath their feet
	    		if(bukkitPlayer.isFlying()) {
	    			Location playerLoc = bukkitPlayer.getLocation();
	    			ChunkSnapshot snap = playerLoc.getChunk().getChunkSnapshot();
	    			int x = playerLoc.getBlockX() - snap.getX()*16;
	    			int y = playerLoc.getBlockY();
	    			int z = playerLoc.getBlockZ() - snap.getZ()*16;
	    			while(snap.getBlockType(x, y, z).isAir()) {
	    				y--;
	    				if(y < playerLoc.getWorld().getMinHeight()) {
	    					y = playerLoc.getWorld().getMinHeight();
	    					break;
	    				}
	    			}
	    			bukkitPlayer.teleport(new Location(playerLoc.getWorld(),playerLoc.getBlockX()+0.5,y+1,playerLoc.getBlockZ()+0.5),TeleportCause.PLUGIN);
	    		}
	    		bukkitPlayer.setFlying(false);
	    		bukkitPlayer.setAllowFlight(false);
	    		isFlying = false;
	    		flyDisableWarmupTimer.stopTimer();
	    	}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setFlyDisableWarmup(boolean enable) {
		// Begin or cancel the fly disable warmup (5 seconds)
		if(enable && isFlying) {
			Date now = new Date();
			flyDisableTime = now.getTime() + (5 * 1000);
			flyDisableWarmupTimer.stopTimer();
			flyDisableWarmupTimer.setTime(0);
			flyDisableWarmupTimer.startLoopTimer();
		} else {
			flyDisableWarmupTimer.stopTimer();
		}
	}
	
	public void removeAllBorders() {
		borderMap.clear();
	}
	
	public void addTerritoryBorders(HashMap<Location, Color> locs) {
		borderMap.putAll(locs);
	}
	
	public void removeAllPlotBorders() {
		borderPlotMap.clear();
	}
	
	public void addTerritoryPlotBorders(HashMap<Location, Color> locs) {
		borderPlotMap.putAll(locs);
	}
	
	public void stopTimers() {
		priorityTitleDisplayTimer.stopTimer();
		borderUpdateLoopTimer.stopTimer();
		monumentTemplateLoopTimer.stopTimer();
		monumentShowLoopTimer.stopTimer();
		combatTagTimer.stopTimer();
		flyDisableWarmupTimer.stopTimer();
	}
	
	public void setIsMapAuto(boolean val) {
		isMapAuto = val;
	}
	
	public void markRecordPlayCooldown() {
		Date now = new Date();
		// Set record cool-down time for 60 seconds from now
		recordPlayCooldownTime = now.getTime() + (60*1000);
	}
	
	public boolean isRecordPlayCooldownOver() {
		Date now = new Date();
		return now.after(new Date(recordPlayCooldownTime));
	}
	
	public void startMonumentShow(Location loc0, Location loc1) {
		monumentShowLoopTimer.stopTimer();
		monumentShowLoopTimer.setTime(1);
		monumentShowLoopTimer.startLoopTimer(5);
		monumentShowLoopCount = 40; // 10 seconds
		monumentShowBoundary.clear();
		monumentShowBoundary.addAll(getEdgeLocations(loc0,loc1));
		ChatUtil.printDebug("Starting monument show Timer for "+bukkitPlayer.getName());
	}

	@Override
	public void onEndTimer(int taskID) {
		if(taskID == 0) {
			ChatUtil.printDebug("Player Timer ended with null taskID!");
		} else if(taskID == priorityTitleDisplayTimer.getTaskID()) {
			// Clear priority title display
			isPriorityTitleDisplay = false;
		} else if(taskID == borderUpdateLoopTimer.getTaskID()) {
			Color particleColor;
			for(Location loc : borderMap.keySet()) {
				if(loc.getWorld().equals(getBukkitPlayer().getWorld()) && loc.distance(getBukkitPlayer().getLocation()) < 12) {
					particleColor = borderMap.get(loc);
					getBukkitPlayer().spawnParticle(CompatibilityUtil.getParticle("dust"), loc, 2, 0.25, 0, 0.25, new Particle.DustOptions(particleColor,1));
				}
			}
			for(Location loc : borderPlotMap.keySet()) {
				if(loc.getWorld().equals(getBukkitPlayer().getWorld()) && loc.distance(getBukkitPlayer().getLocation()) < 12) {
					particleColor = borderPlotMap.get(loc);
					double red = particleColor.getRed() / 255D;
					double green = particleColor.getGreen() / 255D;
					double blue = particleColor.getBlue() / 255D;
					getBukkitPlayer().spawnParticle(CompatibilityUtil.getParticle("spell"), loc, 0, red, green, blue, 1);
				}
			}
		} else if(taskID == monumentTemplateLoopTimer.getTaskID()) {
			updateMonumentTemplateBoundary();
			for(Location loc : monumentTemplateBoundary.keySet()) {
				getBukkitPlayer().spawnParticle(CompatibilityUtil.getParticle("dust"), loc, 1, 0, 0, 0, new Particle.DustOptions(monumentTemplateBoundary.get(loc),1));
			}
		} else if(taskID == monumentShowLoopTimer.getTaskID()) {
			if(monumentShowLoopCount <= 0) {
				monumentShowLoopTimer.stopTimer();
				ChatUtil.printDebug("Ended monument show Timer for "+bukkitPlayer.getName());
			} else {
				monumentShowLoopCount--;
				for(Location loc : monumentShowBoundary) {
					getBukkitPlayer().spawnParticle(CompatibilityUtil.getParticle("dust"), loc, 1, 0, 0, 0, new Particle.DustOptions(Color.LIME,1));
				}
			}
		} else if(taskID == combatTagTimer.getTaskID()) {
			isCombatTagged = false;
			ChatUtil.sendKonPriorityTitle(this, "", ChatColor.GOLD+MessagePath.PROTECTION_NOTICE_UNTAGGED.getMessage(), 20, 1, 10);
			ChatUtil.sendNotice(this.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_UNTAG_MESSAGE.getMessage());
			ChatUtil.printDebug("Combat tag timer ended with taskID: "+taskID+" for "+bukkitPlayer.getName());
		} else if(taskID == flyDisableWarmupTimer.getTaskID()) {
			// Display fly disable countdown, then disable flying
			Date now = new Date();
			if(flyDisableTime <= now.getTime()) {
				setIsFlyEnabled(false);
				flyDisableWarmupTimer.stopTimer();
			} else {
				int timeLeft = (int)(flyDisableTime - now.getTime()) / 1000;
				getBukkitPlayer().sendTitle(" ", ChatColor.GOLD+""+(timeLeft+1), 2, 10, 2);
			}
		}
	}
	
	private void updateMonumentTemplateBoundary() {
    	Block target = bukkitPlayer.getTargetBlock(null, 3);
		if(lastTargetBlock == null || !lastTargetBlock.equals(target)) {
			lastTargetBlock = target;
			// Check for player creating monument template
    		if(isSettingRegion() && getRegionType().equals(RegionType.MONUMENT)) {
    			// Player is currently setting a monument template region
    			// Draw boundary box between first position and player position
    			Location loc0 = getRegionCornerOneBuffer();
    			Location loc1 = target.getLocation();
    			if(loc0 != null && getRegionCornerTwoBuffer() == null) {
    				monumentTemplateBoundary.clear();
    				// Add X lines
    				int xMax,xMin;
    				Color xColor;
    				if(loc1.getBlockX() > loc0.getBlockX()) {
    					xMax = loc1.getBlockX();
    					xMin = loc0.getBlockX();
    				} else {
    					xMax = loc0.getBlockX();
    					xMin = loc1.getBlockX();
    				}
    				if(xMax-xMin == 15) {
    					xColor = Color.LIME;
    				} else if(xMax-xMin < 15) {
    					xColor = Color.ORANGE;
    				} else {
    					xColor = Color.MAROON;
    				}
    				for(int i=xMin;i<=xMax;i++) {
    					monumentTemplateBoundary.put(new Location(loc0.getWorld(),i+0.5,loc0.getBlockY()+1,loc0.getBlockZ()+0.5),xColor);
    					monumentTemplateBoundary.put(new Location(loc0.getWorld(),i+0.5,loc0.getBlockY()+1,loc1.getBlockZ()+0.5),xColor);
    					monumentTemplateBoundary.put(new Location(loc0.getWorld(),i+0.5,loc1.getBlockY()+1,loc0.getBlockZ()+0.5),xColor);
    					monumentTemplateBoundary.put(new Location(loc0.getWorld(),i+0.5,loc1.getBlockY()+1,loc1.getBlockZ()+0.5),xColor);
    				}
    				// Add Z lines
    				int zMax,zMin;
    				Color zColor;
    				if(loc1.getBlockZ() > loc0.getBlockZ()) {
    					zMax = loc1.getBlockZ();
    					zMin = loc0.getBlockZ();
    				} else {
    					zMax = loc0.getBlockZ();
    					zMin = loc1.getBlockZ();
    				}
    				if(zMax-zMin == 15) {
    					zColor = Color.LIME;
    				} else if(zMax-zMin < 15) {
    					zColor = Color.ORANGE;
    				} else {
    					zColor = Color.MAROON;
    				}
    				for(int i=zMin;i<=zMax;i++) {
    					monumentTemplateBoundary.put(new Location(loc0.getWorld(),loc0.getBlockX()+0.5,loc0.getBlockY()+1,i+0.5),zColor);
    					monumentTemplateBoundary.put(new Location(loc0.getWorld(),loc0.getBlockX()+0.5,loc1.getBlockY()+1,i+0.5),zColor);
    					monumentTemplateBoundary.put(new Location(loc0.getWorld(),loc1.getBlockX()+0.5,loc0.getBlockY()+1,i+0.5),zColor);
    					monumentTemplateBoundary.put(new Location(loc0.getWorld(),loc1.getBlockX()+0.5,loc1.getBlockY()+1,i+0.5),zColor);
    				}
    				// Add Y lines
    				int yMax,yMin;
    				Color yColor;
    				if(loc1.getBlockY() > loc0.getBlockY()) {
    					yMax = loc1.getBlockY();
    					yMin = loc0.getBlockY();
    				} else {
    					yMax = loc0.getBlockY();
    					yMin = loc1.getBlockY();
    				}
    				if(xMax-xMin == 15 && zMax-zMin == 15) {
    					yColor = Color.LIME;
    				} else if(xMax-xMin > 15 || zMax-zMin > 15) {
    					yColor = Color.MAROON;
    				} else {
    					yColor = Color.ORANGE;
    				}
    				for(int i=yMin;i<=yMax;i++) {
    					monumentTemplateBoundary.put(new Location(loc0.getWorld(),loc0.getBlockX()+0.5,i+1,loc0.getBlockZ()+0.5),yColor);
    					monumentTemplateBoundary.put(new Location(loc0.getWorld(),loc0.getBlockX()+0.5,i+1,loc1.getBlockZ()+0.5),yColor);
    					monumentTemplateBoundary.put(new Location(loc0.getWorld(),loc1.getBlockX()+0.5,i+1,loc0.getBlockZ()+0.5),yColor);
    					monumentTemplateBoundary.put(new Location(loc0.getWorld(),loc1.getBlockX()+0.5,i+1,loc1.getBlockZ()+0.5),yColor);
    				}
    			}
    		}
    	}
    }
	
	private HashSet<Location> getEdgeLocations(Location loc0, Location loc1) {
    	HashSet<Location> locationSet = new HashSet<>();
		// Add X lines
		int xMax,xMin;
		if(loc1.getBlockX() > loc0.getBlockX()) {
			xMax = loc1.getBlockX();
			xMin = loc0.getBlockX();
		} else {
			xMax = loc0.getBlockX();
			xMin = loc1.getBlockX();
		}
		for(int i=xMin;i<=xMax;i++) {
			locationSet.add(new Location(loc0.getWorld(),i+0.5,loc0.getBlockY()+1,loc0.getBlockZ()+0.5));
			locationSet.add(new Location(loc0.getWorld(),i+0.5,loc0.getBlockY()+1,loc1.getBlockZ()+0.5));
			locationSet.add(new Location(loc0.getWorld(),i+0.5,loc1.getBlockY()+1,loc0.getBlockZ()+0.5));
			locationSet.add(new Location(loc0.getWorld(),i+0.5,loc1.getBlockY()+1,loc1.getBlockZ()+0.5));
		}
		// Add Z lines
		int zMax,zMin;
		if(loc1.getBlockZ() > loc0.getBlockZ()) {
			zMax = loc1.getBlockZ();
			zMin = loc0.getBlockZ();
		} else {
			zMax = loc0.getBlockZ();
			zMin = loc1.getBlockZ();
		}
		for(int i=zMin;i<=zMax;i++) {
			locationSet.add(new Location(loc0.getWorld(),loc0.getBlockX()+0.5,loc0.getBlockY()+1,i+0.5));
			locationSet.add(new Location(loc0.getWorld(),loc0.getBlockX()+0.5,loc1.getBlockY()+1,i+0.5));
			locationSet.add(new Location(loc0.getWorld(),loc1.getBlockX()+0.5,loc0.getBlockY()+1,i+0.5));
			locationSet.add(new Location(loc0.getWorld(),loc1.getBlockX()+0.5,loc1.getBlockY()+1,i+0.5));
		}
		// Add Y lines
		int yMax,yMin;
		if(loc1.getBlockY() > loc0.getBlockY()) {
			yMax = loc1.getBlockY();
			yMin = loc0.getBlockY();
		} else {
			yMax = loc0.getBlockY();
			yMin = loc1.getBlockY();
		}
		for(int i=yMin;i<=yMax;i++) {
			locationSet.add(new Location(loc0.getWorld(),loc0.getBlockX()+0.5,i+1,loc0.getBlockZ()+0.5));
			locationSet.add(new Location(loc0.getWorld(),loc0.getBlockX()+0.5,i+1,loc1.getBlockZ()+0.5));
			locationSet.add(new Location(loc0.getWorld(),loc1.getBlockX()+0.5,i+1,loc0.getBlockZ()+0.5));
			locationSet.add(new Location(loc0.getWorld(),loc1.getBlockX()+0.5,i+1,loc1.getBlockZ()+0.5));
		}
		return locationSet;
    }
	
}
