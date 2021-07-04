package konquest.model;

import java.awt.Point;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import konquest.Konquest;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;
import konquest.utility.Timeable;
import konquest.utility.Timer;

public class KonCamp extends KonTerritory implements Timeable {
	
	private OfflinePlayer owner;
	private Timer raidAlertTimer;
	private boolean isRaidAlertDisabled;
	private Location bedLocation;
	
	public KonCamp(Location loc, OfflinePlayer owner, KonKingdom kingdom, Konquest konquest) {
		super(loc, MessagePath.LABEL_CAMP.getMessage().trim()+"_"+owner.getName(), konquest.getKingdomManager().getBarbarians(), KonTerritoryType.CAMP, konquest);
		
		this.owner = owner;
		this.raidAlertTimer = new Timer(this);
		this.isRaidAlertDisabled = false;
		this.bedLocation = loc;
	}

	/**
	 * Initializes the camp territory.
	 * @return  0 - success
	 * 			4 - error, bad chunk
	 */
	public int initClaim() {
		if(!addChunks(getKonquest().getAreaPoints(getCenterLoc(), getKonquest().getConfigManager().getConfig("core").getInt("core.camps.init_radius")))) {
			ChatUtil.printDebug("Camp init failed: problem adding some chunks");
			return 4;
		}
		return 0;
	}

	public boolean addChunk(Point point) {
		//addPoint(getKonquest().toPoint(chunk));
		addPoint(point);
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
			ChatUtil.printDebug("Town Timer ended with null taskID!");
		} else if(taskID == raidAlertTimer.getTaskID()) {
			ChatUtil.printDebug("Raid Alert Timer ended with taskID: "+taskID);
			// When a raid alert cooldown timer ends
			isRaidAlertDisabled = false;
		} else {
			ChatUtil.printDebug("Town Timer ended with unknown taskID: "+taskID);
		}
	}

}
