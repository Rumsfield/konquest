package konquest.model;

import org.bukkit.Location;

public class KonMonumentTemplate {

	private KonKingdom kingdom;
	private Location corner1;
	private Location corner2;
	private Location travelPoint;
	private boolean isValid;
	
	public KonMonumentTemplate(KonKingdom kingdom) {
		this.kingdom = kingdom;
		this.isValid = false;
	}
	
	public KonMonumentTemplate(KonKingdom kingdom, Location corner1, Location corner2, Location travelPoint) {
		this.kingdom = kingdom;
		this.corner1 = corner1;
		this.corner2 = corner2;
		this.travelPoint = travelPoint;
		this.isValid = true;
	}
	
	public int getHeight() {
		if(isValid && corner1 != null && corner2 != null) {
			return (int)Math.abs(corner1.getY()-corner2.getY());
		} else {
			return 0;
		}
	}
	public KonKingdom getKingdom() {
		return kingdom;
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
}
