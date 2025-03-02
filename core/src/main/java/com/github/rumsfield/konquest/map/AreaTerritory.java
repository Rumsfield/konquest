package com.github.rumsfield.konquest.map;

import com.github.rumsfield.konquest.model.KonTerritory;
import com.github.rumsfield.konquest.utility.ChatUtil;
import org.bukkit.Location;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AreaTerritory {

	private final Set<Point> areaPoints;
	private final List<double[]> xPoints;
	private final List<double[]> zPoints;
	private final List<double[]> xContours;
	private final List<double[]> zContours;
	private final String worldName;
	private final Location center;
	private final KonTerritory territory;
	
	private enum FaceDirection {
		NE,
		SE,
		SW,
		NW
    }
	
	private static class Coord {
		int x, z;
		boolean isConvex;
		FaceDirection face;
		Coord(int x, int z, boolean isConvex, FaceDirection face) {
			this.x = x;
			this.z = z;
			this.isConvex = isConvex;
			this.face = face;
		}
		public String toString() {
        	return String.format("{%d,%d,%s,%s}",x,z, isConvex,face.toString());
        }
	}
	
	public AreaTerritory(KonTerritory territory) {
		this.territory = territory;
		this.worldName = territory.getWorld().getName();
		this.areaPoints = territory.getChunkList().keySet();
		this.center = territory.getCenterLoc();
		this.xPoints = new ArrayList<>();
		this.zPoints = new ArrayList<>();
		this.xContours = new ArrayList<>();
		this.zContours = new ArrayList<>();
		calculateCorners();
	}
	
	private void calculateCorners() {
		ArrayList<Coord> cornerList = new ArrayList<>();
		
		int[] cornerLUTX = {1,	1,	-1,	-1};
		int[] cornerLUTZ = {1, -1,  -1,  1};
		int[] offsetLUTX = {16,	16,	 0,	 0};
		int[] offsetLUTZ = {16, 0,	 0,	 16};
		FaceDirection[] dirLUT = {FaceDirection.NE,FaceDirection.SE,FaceDirection.SW,FaceDirection.NW};
		boolean adj1, adj2, opp;
		
		for(Point p : areaPoints) {
			/* Evaluate Corners */
			for(int i = 0; i < 4; i++) {
				adj1 = areaPoints.contains(new Point(p.x,            p.y+cornerLUTZ[i]));
				adj2 = areaPoints.contains(new Point(p.x+cornerLUTX[i], p.y));
				opp  = areaPoints.contains(new Point(p.x+cornerLUTX[i],p.y+cornerLUTZ[i]));
				if(adj1 && adj2 && !opp) {
					cornerList.add(new Coord(p.x*16+offsetLUTX[i],p.y*16+offsetLUTZ[i],false,dirLUT[i]));
				} else if(!adj1 && !adj2) {
					cornerList.add(new Coord(p.x*16+offsetLUTX[i],p.y*16+offsetLUTZ[i],true,dirLUT[i]));
				}
			}
		}
		List<List<Coord>> allContours = sortedCoords(cornerList);

		for(List<Coord> contour : allContours) {
			double[] x = new double[contour.size()];
			double[] z = new double[contour.size()];
			for(int i = 0; i < contour.size(); i++) {
				x[i] = contour.get(i).x;
				z[i] = contour.get(i).z;
			}
			xContours.add(x);
			zContours.add(z);
		}
		
		for(Point p : areaPoints) {
			double[] x = {p.x*16, p.x*16+16};
			double[] z = {p.y*16, p.y*16+16};
			xPoints.add(x);
			zPoints.add(z);
		}
	}
	
	public int getNumContours() {
		return xContours.size();
	}
	
	public double[] getXContour(int n) {
		return xContours.get(n);
	}
	
	public double[] getZContour(int n) {
		return zContours.get(n);
	}
	
	public int getNumPoints() {
		return xPoints.size();
	}
	
	public double[] getXPoint(int n) {
		return xPoints.get(n);
	}
	
	public double[] getZPoint(int n) {
		return zPoints.get(n);
	}
	
	public String getWorldName() {
		return worldName;
	}
	
	public double getCenterX() {
		return center.getX();
	}
	
	public double getCenterY() {
		return center.getY();
	}
	
	public double getCenterZ() {
		return center.getZ();
	}

	public KonTerritory getTerritory() {
		return territory;
	}
	
	/**
	 * Search pattern: Start at x-most corner and move clockwise along contour. If there
	 * 	are corners left over, begin a new search for a new contour, etc.
	 * 		
	 * @param coords - List of all corners in this territory
	 * @return List of contours
	 */
	private List<List<Coord>> sortedCoords(List<Coord> coords) {
		List<List<Coord>> sortedList = new ArrayList<>();
		List<Coord> remainingCorners = new ArrayList<>(coords);
		//
		int timeout = 9001;
		while(!remainingCorners.isEmpty() && timeout > 0) {

			List<Coord> contour = new ArrayList<>();
			// Find max x corner
			Coord current = remainingCorners.get(0);
			for (Coord c : remainingCorners) {
				if (c.x > current.x) {
					current = c;
				}
			}
			Coord candidate;
			remainingCorners.remove(current);
			contour.add(current);
			//ChatUtil.printDebug("Beginning contour search for territory with "+remainingCorners.size()+" corners...");
			// Find consecutive corners
			boolean isSearchSuccessful = true;
			while(contour.size() < coords.size() && isSearchSuccessful) {
				//ChatUtil.printDebug("Searching for nearest corner adjacent to "+current.toString());
				candidate = null;
				for (Coord c : remainingCorners) {
					if (current.isConvex) {
						switch (current.face) {
							case NE:
								if (current.x == c.x && current.z > c.z) {
									if (candidate == null || c.z > candidate.z) {
										candidate = c;
									}
								}
								break;
							case SE:
								if (current.z == c.z && current.x > c.x) {
									if (candidate == null || c.x > candidate.x) {
										candidate = c;
									}
								}
								break;
							case SW:
								if (current.x == c.x && current.z < c.z) {
									if (candidate == null || c.z < candidate.z) {
										candidate = c;
									}
								}
								break;
							case NW:
								if (current.z == c.z && current.x < c.x) {
									if (candidate == null || c.x < candidate.x) {
										candidate = c;
									}
								}
								break;
							default:
								break;
						}
					} else {
						switch (current.face) {
							case NE:
								if (current.z == c.z && current.x < c.x) {
									if (candidate == null || c.x < candidate.x) {
										candidate = c;
									}
								}
								break;
							case SE:
								if (current.x == c.x && current.z > c.z) {
									if (candidate == null || c.z > candidate.z) {
										candidate = c;
									}
								}
								break;
							case SW:
								if (current.z == c.z && current.x > c.x) {
									if (candidate == null || c.x > candidate.x) {
										candidate = c;
									}
								}
								break;
							case NW:
								if (current.x == c.x && current.z < c.z) {
									if (candidate == null || c.z < candidate.z) {
										candidate = c;
									}
								}
								break;
							default:
								break;
						}
					}
				}
				if (candidate != null) {
					remainingCorners.remove(candidate);
					contour.add(candidate);
					current = candidate;
				} else {
					isSearchSuccessful = false;
				}
			}
			if(!contour.isEmpty()) {
				sortedList.add(contour);
			} else {
				ChatUtil.printDebug("Failed to add empty contour!");
			}
			timeout--;
		}
		if(timeout == 0) {
			ChatUtil.printDebug("Contour search timed out!");
		}
		return sortedList;
	}
	
}
