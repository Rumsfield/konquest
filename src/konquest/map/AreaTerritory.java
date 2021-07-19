package konquest.map;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Set;

import konquest.model.KonTerritory;

public class AreaTerritory {

	private Set<Point> areaPoints;
	private double[] xCorners;
	private double[] zCorners;
	private String worldName;
	
	private static class Coord {
		int x, z;
		Coord(int x, int z) {
			this.x = x;
			this.z = z;
		}
	}
	
	public AreaTerritory(KonTerritory territory) {
		this.worldName = territory.getWorld().getName();
		this.areaPoints = territory.getChunkList().keySet();
		calculateCorners();
	}
	
	private void calculateCorners() {
		ArrayList<Coord> cornerList = new ArrayList<Coord>();
		
		int[] cornerLUTX = {1,	1,	-1,	-1};
		int[] cornerLUTZ = {1, -1,  -1,  1};
		int[] offsetLUTX = {16,	16,	 0,	 0};
		int[] offsetLUTZ = {16, 0,	 0,	 16};
		boolean adj1, adj2, opp = false;
		
		for(Point p : areaPoints) {
			/* Evaluate Corners */
			for(int i = 0; i < 4; i++) {
				adj1 = areaPoints.contains(new Point(p.x+0,            p.y+cornerLUTZ[i]));
				adj2 = areaPoints.contains(new Point(p.x+cornerLUTX[i],p.y+0));
				opp  = areaPoints.contains(new Point(p.x+cornerLUTX[i],p.y+cornerLUTZ[i]));
				if( (!adj1 && !adj2) || (adj1 && adj2 && !opp)) {
					cornerList.add(new Coord(p.x*16+offsetLUTX[i],p.y*16+offsetLUTZ[i]));
				}
			}
		}
		
		xCorners = new double[cornerList.size()];
		zCorners = new double[cornerList.size()];
		
		for(int i = 0; i < cornerList.size(); i++) {
			xCorners[i] = cornerList.get(i).x;
			zCorners[i] = cornerList.get(i).z;
		}
		
	}
	
	public double[] getXCorners() {
		return xCorners;
	}
	
	public double[] getZCorners() {
		return zCorners;
	}
	
	public String getWorldName() {
		return worldName;
	}
	
}
