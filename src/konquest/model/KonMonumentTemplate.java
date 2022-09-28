package konquest.model;

import org.bukkit.Location;

public class KonMonumentTemplate {

	private Location corner1;
	private Location corner2;
	private Location travelPoint;
	private boolean isValid;
	private boolean hasLoot;
	private String name;
	
	public KonMonumentTemplate(String name, Location corner1, Location corner2, Location travelPoint) {
		this.name = name;
		this.corner1 = corner1;
		this.corner2 = corner2;
		this.travelPoint = travelPoint;
		this.isValid = false;
		this.hasLoot = false;
	}
	
	public String getName() {
		return name;
	}
	
	public int getHeight() {
		if(isValid && corner1 != null && corner2 != null) {
			return (int)Math.abs(corner1.getY()-corner2.getY());
		} else {
			return 0;
		}
	}
	
	public boolean isValid() {
		return isValid;
	}
	
	public Location getCornerOne() {
		return corner1;
	}
	
	public Location getCornerTwo() {
		return corner2;
	}
	
	public Location getTravelPoint() {
		return travelPoint;
	}
	
	public void setCornerOne(Location loc) {
		corner1 = loc;
	}
	
	public void setCornerTwo(Location loc) {
		corner2 = loc;
	}
	
	public void setTravelPoint(Location loc) {
		travelPoint = loc;
	}
	
	public void setValid(boolean isNewValid) {
		isValid = isNewValid;
	}
	
	public void setLoot(boolean val) {
		hasLoot = val;
	}
	
	public boolean hasLoot() {
		return hasLoot;
	}
	
	public boolean isLocInside(Location loc) {
		if(isValid) {
			int topBlockX = Math.max(corner1.getBlockX(), corner2.getBlockX());
	        int topBlockY = Math.max(corner1.getBlockY(), corner2.getBlockY());
	        int topBlockZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());
	        int bottomBlockX = Math.min(corner1.getBlockX(), corner2.getBlockX());
	        int bottomBlockY = Math.min(corner1.getBlockY(), corner2.getBlockY());
	        int bottomBlockZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
	        if(loc.getBlockX() <= topBlockX && loc.getBlockY() <= topBlockY && loc.getBlockZ() <= topBlockZ &&
	        		loc.getBlockX() >= bottomBlockX && loc.getBlockY() >= bottomBlockY && loc.getBlockZ() >= bottomBlockZ) {
	        	return true;
	        }
		}
		return false;
	}
}
