package konquest.model;

import java.awt.Point;

import org.bukkit.Location;
import org.bukkit.World;

import konquest.Konquest;

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
		return false;
	}
	
	// Returns true if this template's region is inside of the given chunk (point,world)
	public boolean isInsideChunk(Point point, World world) {
		boolean result = false;
		if(corner1.getWorld().equals(world) && corner2.getWorld().equals(world)) {
			// Determine 4 X/Y plane corners
			World allWorld = corner1.getWorld();
			double topX = Math.max(corner1.getX(), corner2.getX());
			double topZ = Math.max(corner1.getZ(), corner2.getZ());
			double botX = Math.min(corner1.getX(), corner2.getX());
			double botZ = Math.min(corner1.getZ(), corner2.getZ());
	        
	        Location c1 = new Location(allWorld, topX, 0.0, topZ);
	        Location c2 = new Location(allWorld, topX, 0.0, botZ);
	        Location c3 = new Location(allWorld, botX, 0.0, topZ);
	        Location c4 = new Location(allWorld, botX, 0.0, botZ);
			
			Point p1 = Konquest.toPoint(c1);
			Point p2 = Konquest.toPoint(c2);
			Point p3 = Konquest.toPoint(c3);
			Point p4 = Konquest.toPoint(c4);
			
			if(point.equals(p1) || point.equals(p2) || point.equals(p3) || point.equals(p4)) {
				result = true;
			}
		}
		return result;
	}
}
