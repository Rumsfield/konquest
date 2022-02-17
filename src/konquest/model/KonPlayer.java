package konquest.model;

import konquest.api.model.KonquestPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;
import konquest.utility.Timeable;
import konquest.utility.Timer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.ChatColor;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class KonPlayer extends KonOfflinePlayer implements KonquestPlayer, Timeable {

	public enum RegionType {
		NONE,
		MONUMENT,
		RUIN_CRITICAL,
		RUIN_SPAWN;
	}
	
	private Player bukkitPlayer;
	//private KonKingdom exileKingdom;
	
	private RegionType settingRegion;
	private String regionKingdomName;
	private Location regionCornerOneBuffer;
	private Location regionCornerTwoBuffer;
	
	private boolean isAdminBypassActive;
	private boolean isClaimingFollow;
	private boolean isAdminClaimingFollow;
	private boolean isGlobalChat;
	private boolean isExileConfirmed;
	private boolean isMapAuto;
	private boolean isGiveLordConfirmed;
	private boolean isPriorityTitleDisplay;
	private boolean isCombatTagged;
	private boolean isFlying;
	private boolean isBorderDisplay;
	
	private Timer exileConfirmTimer;
	private Timer giveLordConfirmTimer;
	private Timer priorityTitleDisplayTimer;
	private Timer borderUpdateLoopTimer;
	private Timer monumentTemplateLoopTimer;
	private Timer monumentShowLoopTimer;
	private Timer combatTagTimer;
	private Timer flyDisableWarmupTimer;
	private long recordPlayCooldownTime;
	private int monumentShowLoopCount;
	private long flyDisableTime;
	private ArrayList<Mob> targetMobList;
	private HashMap<KonDirective,Integer> directiveProgress;
	private KonStats playerStats;
	private KonPrefix playerPrefix;
	private HashMap<Location, Color> borderMap;
	private HashMap<Location, Color> borderPlotMap;
	private HashMap<Location,Color> monumentTemplateBoundary;
	private HashSet<Location> monumentShowBoundary;
	private Block lastTargetBlock;
	
	public KonPlayer(Player bukkitPlayer, KonKingdom kingdom, boolean isBarbarian) {
		super(bukkitPlayer, kingdom, isBarbarian);
		//super((OfflinePlayer) bukkitPlayer, kingdom, isBarbarian);
		
		this.bukkitPlayer = bukkitPlayer;
		//this.exileKingdom = kingdom;
		this.settingRegion = RegionType.NONE;
		this.regionKingdomName = "";
		this.isAdminBypassActive = false;
		this.isClaimingFollow = false;
		this.isAdminClaimingFollow = false;
		this.isGlobalChat = true;
		this.isExileConfirmed = false;
		this.isMapAuto = false;
		this.isGiveLordConfirmed = false;
		this.isPriorityTitleDisplay = false;
		this.isCombatTagged = false;
		this.isFlying = false;
		this.isBorderDisplay = true;
		this.exileConfirmTimer = new Timer(this);
		this.giveLordConfirmTimer = new Timer(this);
		this.priorityTitleDisplayTimer = new Timer(this);
		this.borderUpdateLoopTimer = new Timer(this);
		this.monumentTemplateLoopTimer = new Timer(this);
		this.monumentShowLoopTimer = new Timer(this);
		this.combatTagTimer = new Timer(this);
		this.flyDisableWarmupTimer = new Timer(this);
		this.recordPlayCooldownTime = 0;
		this.monumentShowLoopCount = 0;
		this.flyDisableTime = 0;
		this.targetMobList = new ArrayList<Mob>();
		this.directiveProgress = new HashMap<KonDirective,Integer>();
		this.playerStats = new KonStats();
		this.playerPrefix = new KonPrefix();
		this.borderMap = new HashMap<Location, Color>();
		this.borderPlotMap = new HashMap<Location, Color>();
		this.monumentTemplateBoundary = new HashMap<Location,Color>();
		this.monumentShowBoundary = new HashSet<Location>();
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
	
	public Collection<KonDirective> getDirectives() {
		if(directiveProgress.isEmpty()) {
			return Collections.emptySet();
		} else {
			return (Collection<KonDirective>) directiveProgress.keySet();
		}
	}
	
	public void clearAllDirectives() {
		directiveProgress.clear();
	}
	
	// Getters
	public Player getBukkitPlayer() {
		return bukkitPlayer;
	}
	
	/*
	public KonKingdom getExileKingdom() {
		return exileKingdom;
	}
	*/
	
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
	
	public String getRegionKingdomName() {
		return regionKingdomName;
	}
	
	public boolean isAdminBypassActive() {
		return isAdminBypassActive;
	}
	
	public boolean isClaimingFollow() {
		return isClaimingFollow;
	}
	
	public boolean isAdminClaimingFollow() {
		return isAdminClaimingFollow;
	}
	
	public boolean isGlobalChat() {
		return isGlobalChat;
	}
	
	public boolean isExileConfirmed() {
		return isExileConfirmed;
	}
	
	public Timer getExileConfirmTimer() {
		return exileConfirmTimer;
	}
	
	public boolean isMapAuto() {
		return isMapAuto;
	}
	
	public boolean isGiveLordConfirmed() {
		return isGiveLordConfirmed;
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
	
	public Timer getGiveLordConfirmTimer() {
		return giveLordConfirmTimer;
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
	
	public Timer getBorderUpdateLoopTimer() {
		return borderUpdateLoopTimer;
	}
	
	public Timer getCombatTagTimer() {
		return combatTagTimer;
	}
	
	// Setters
	
	/*
	public void setExileKingdom(KonKingdom newKingdom) {
		exileKingdom = newKingdom;
	}
	*/
	
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
	
	public void setRegionKingdomName(String name) {
		regionKingdomName = name;
	}
	
	public void setIsAdminBypassActive(boolean val) {
		isAdminBypassActive = val;
	}
	
	public void setIsClaimingFollow(boolean val) {
		isClaimingFollow = val;
	}
	
	public void setIsAdminClaimingFollow(boolean val) {
		isAdminClaimingFollow = val;
	}
	
	public void setIsGlobalChat(boolean val) {
		isGlobalChat = val;
	}
	
	public void setIsExileConfirmed(boolean val) {
		isExileConfirmed = val;
	}
	
	public void setIsGiveLordConfirmed(boolean val) {
		isGiveLordConfirmed = val;
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
		exileConfirmTimer.stopTimer();
		giveLordConfirmTimer.stopTimer();
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
		// Set record cooldown time for 60 seconds from now
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
		} else if(taskID == exileConfirmTimer.getTaskID()) {
			// Clear exile confirmation
			//ChatUtil.printDebug("Player exile confirmation Timer ended with taskID: "+taskID+" for "+bukkitPlayer.getName());
			isExileConfirmed = false;
		} else if(taskID == giveLordConfirmTimer.getTaskID()) {
			// Clear give lord confirmation
			//ChatUtil.printDebug("Player give lord confirmation Timer ended with taskID: "+taskID+" for "+bukkitPlayer.getName());
			isGiveLordConfirmed = false;
		} else if(taskID == priorityTitleDisplayTimer.getTaskID()) {
			// Clear priority title display
			//ChatUtil.printDebug("Player priority title display Timer ended with taskID: "+taskID+" for "+bukkitPlayer.getName());
			isPriorityTitleDisplay = false;
		} else if(taskID == borderUpdateLoopTimer.getTaskID()) {
			Color particleColor;
			for(Location loc : borderMap.keySet()) {
				if(loc.getWorld().equals(getBukkitPlayer().getWorld()) && loc.distance(getBukkitPlayer().getLocation()) < 12) {
					particleColor = borderMap.get(loc);
					getBukkitPlayer().spawnParticle(Particle.REDSTONE, loc, 2, 0.25, 0, 0.25, new Particle.DustOptions(particleColor,1));
				}
			}
			for(Location loc : borderPlotMap.keySet()) {
				if(loc.getWorld().equals(getBukkitPlayer().getWorld()) && loc.distance(getBukkitPlayer().getLocation()) < 12) {
					particleColor = borderPlotMap.get(loc);
					double red = particleColor.getRed() / 255D;
					double green = particleColor.getGreen() / 255D;
					double blue = particleColor.getBlue() / 255D;
					getBukkitPlayer().spawnParticle(Particle.SPELL_MOB_AMBIENT, loc, 0, red, green, blue, 1);
				}
			}
		} else if(taskID == monumentTemplateLoopTimer.getTaskID()) {
			updateMonumentTemplateBoundary();
			for(Location loc : monumentTemplateBoundary.keySet()) {
				getBukkitPlayer().spawnParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, new Particle.DustOptions(monumentTemplateBoundary.get(loc),1));
			}
		} else if(taskID == monumentShowLoopTimer.getTaskID()) {
			if(monumentShowLoopCount <= 0) {
				monumentShowLoopTimer.stopTimer();
				ChatUtil.printDebug("Ended monument show Timer for "+bukkitPlayer.getName());
			} else {
				monumentShowLoopCount--;
				for(Location loc : monumentShowBoundary) {
					getBukkitPlayer().spawnParticle(Particle.REDSTONE, loc, 1, 0, 0, 0, new Particle.DustOptions(Color.LIME,1));
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
				if(timeLeft <= 3) {
					ChatUtil.sendKonPriorityTitle(this, "", ChatColor.GOLD+""+(timeLeft+1), 16, 2, 2);
				}
			}
			
		}
	}
	
	private void updateMonumentTemplateBoundary() {
    	Block target = bukkitPlayer.getTargetBlock(null, 3);
		if(lastTargetBlock == null || !lastTargetBlock.equals(target)) {
			//ChatUtil.printDebug("Rendering new monument template boundary for "+bukkitPlayer.getName());
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
    	HashSet<Location> locationSet = new HashSet<Location>();
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
