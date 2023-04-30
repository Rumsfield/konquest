package com.github.rumsfield.konquest.model;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestCamp;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.utility.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.awt.*;

public class KonCamp extends KonTerritory implements KonquestCamp, KonBarDisplayer, Timeable {
	
	private final OfflinePlayer owner;
	private final Timer raidAlertTimer;
	private final Timer protectedWarmupTimer;
	private final Timer protectedCountdownTimer;
	private boolean isRaidAlertDisabled;
	private boolean isOfflineProtected;
	private Location bedLocation;
	private final BossBar campBarAll;
	
	public KonCamp(Location loc, OfflinePlayer owner, KonKingdom kingdom, Konquest konquest) {
		super(loc, MessagePath.LABEL_CAMP.getMessage().trim()+"_"+owner.getName(), kingdom, konquest);
		
		this.owner = owner;
		this.raidAlertTimer = new Timer(this);
		this.protectedWarmupTimer = new Timer(this);
		this.protectedCountdownTimer = new Timer(this);
		this.isRaidAlertDisabled = false;
		this.isOfflineProtected = false;
		this.bedLocation = loc;
		this.campBarAll = Bukkit.getServer().createBossBar(Konquest.barbarianColor1+getName(), ChatUtil.mapBarColor(Konquest.barbarianColor1), BarStyle.SOLID);
		this.campBarAll.setVisible(true);
		initProtection();
	}
	
	private void initProtection() {
		boolean isOfflineProtectedEnabled = getKonquest().getCore().getBoolean(CorePath.CAMPS_NO_ENEMY_EDIT_OFFLINE.getPath(),true);
		if(isOfflineProtectedEnabled && !isOwnerOnline()) {
			// Immediately enable protection
			isOfflineProtected = true;
			campBarAll.setTitle(Konquest.barbarianColor1+getName()+" "+MessagePath.LABEL_PROTECTED.getMessage());
		}
	}

	/**
	 * Initializes the camp territory.
	 * @return  0 - success
	 * 			4 - error, bad chunk
	 */
	public int initClaim() {
		if(!addChunks(getKonquest().getAreaPoints(getCenterLoc(), getKonquest().getCore().getInt(CorePath.CAMPS_INIT_RADIUS.getPath())))) {
			ChatUtil.printDebug("Camp init failed: problem adding some chunks");
			return 4;
		}
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
	
	public boolean isOwnerOnline() {
		return owner.isOnline();
	}
	
	public OfflinePlayer getOwner() {
		return owner;
	}
	
	public boolean isPlayerOwner(OfflinePlayer player) {
		return player.getUniqueId().equals(owner.getUniqueId());
	}
	
	public boolean isRaidAlertDisabled() {
		return isRaidAlertDisabled;
	}
	
	public void setIsRaidAlertDisabled(boolean val) {
		isRaidAlertDisabled = val;
	}
	
	public Timer getRaidAlertTimer() {
		return raidAlertTimer;
	}
	
	public Location getBedLocation() {
		return bedLocation;
	}
	
	public void setBedLocation(Location loc) {
		bedLocation = loc;
	}

	@Override
	public void onEndTimer(int taskID) {
		if(taskID == 0) {
			ChatUtil.printDebug("Camp Timer ended with null taskID!");
		} else if(taskID == raidAlertTimer.getTaskID()) {
			ChatUtil.printDebug("Raid Alert Timer ended with taskID: "+taskID);
			// When a raid alert cooldown timer ends
			isRaidAlertDisabled = false;
		} else if(taskID == protectedWarmupTimer.getTaskID()) {
			ChatUtil.printDebug("Offline protection warmup Timer ended with taskID: "+taskID);
			// When a protection warmup timer ends
			isOfflineProtected = true;
			protectedCountdownTimer.stopTimer();
			campBarAll.setTitle(Konquest.barbarianColor1+getName()+" "+MessagePath.LABEL_PROTECTED.getMessage());
		} else if(taskID == protectedCountdownTimer.getTaskID()) {
			// Update protection countdown title
			String remainingTime = Konquest.getTimeFormat(protectedWarmupTimer.getTime(),ChatColor.RED);
			campBarAll.setTitle(Konquest.barbarianColor1+getName()+" "+remainingTime);
			//ChatUtil.printDebug("Camp protection countdown tick with taskID: "+taskID);
		} else {
			ChatUtil.printDebug("Camp Timer ended with unknown taskID: "+taskID);
		}
	}
	
	public void addBarPlayer(KonPlayer player) {
		campBarAll.addPlayer(player.getBukkitPlayer());
	}
	
	public void removeBarPlayer(KonPlayer player) {
		campBarAll.removePlayer(player.getBukkitPlayer());
	}
	
	public void removeAllBarPlayers() {
		campBarAll.removeAll();
	}
	
	public void updateBarPlayers() {
		campBarAll.removeAll();
		for(KonPlayer player : getKonquest().getPlayerManager().getPlayersOnline()) {
			Player bukkitPlayer = player.getBukkitPlayer();
			if(isLocInside(bukkitPlayer.getLocation())) {
				campBarAll.addPlayer(bukkitPlayer);
			}
		}
	}

	@Override
	public void updateBarTitle() {
		campBarAll.setTitle(Konquest.barbarianColor1+getName());
	}
	
	public void setProtected(boolean val) {
		if(val) {
			// Optionally start warmup timer to protect this camp
			boolean isOfflineProtectedEnabled = getKonquest().getCore().getBoolean(CorePath.CAMPS_NO_ENEMY_EDIT_OFFLINE.getPath(),true);
			int offlineProtectedWarmupSeconds = getKonquest().getCore().getInt(CorePath.CAMPS_NO_ENEMY_EDIT_OFFLINE_WARMUP.getPath(),0);
			if(isOfflineProtectedEnabled) {
				if(offlineProtectedWarmupSeconds > 0 && protectedWarmupTimer.getTime() == -1 && !isOfflineProtected) {
					// Start warmup timer
					protectedWarmupTimer.stopTimer();
					protectedWarmupTimer.setTime(offlineProtectedWarmupSeconds);
					protectedWarmupTimer.startTimer();
					// Start countdown timer
					protectedCountdownTimer.stopTimer();
					protectedCountdownTimer.setTime(0);
					protectedCountdownTimer.startLoopTimer();
				} else if(offlineProtectedWarmupSeconds <= 0) {
					// Immediately enable protection
					isOfflineProtected = true;
					campBarAll.setTitle(Konquest.barbarianColor1+getName()+" "+MessagePath.LABEL_PROTECTED.getMessage());
				}
			}
		} else {
			// Remove protection
			isOfflineProtected = false;
			protectedWarmupTimer.stopTimer();
			protectedCountdownTimer.stopTimer();
			campBarAll.setTitle(Konquest.barbarianColor1+getName());
		}
	}
	
	public boolean isProtected() {
		return isOfflineProtected;
	}
	
	public void applyGlow(Player bukkitPlayer) {
		boolean isGlowEnabled = getKonquest().getCore().getBoolean(CorePath.CAMPS_ENEMY_GLOW.getPath(), true);
		if (!isGlowEnabled) return;
		bukkitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 5, 1));
	}
	
	@Override
	public KonquestTerritoryType getTerritoryType() {
		return KonquestTerritoryType.CAMP;
	}

}
