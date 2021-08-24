package konquest.map;

import java.awt.Point;
import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;

import konquest.model.KonTerritory;
import konquest.utility.ChatUtil;

//TODO: X and Z corner double[]'s are primary boundary contour. Internal unclaimed points yield secondary contours within primary area that will
// be shaded white. These dead zones will be kept in a collection that can be used to spawn secondary AreaMarkers to mark the unclaimed pockets.

//TODO: Keep set of points for individual area markers, and set of contours for boundaries

public class AreaTerritory {

	private Set<Point> areaPoints;
	private List<double[]> xPoints;
	private List<double[]> zPoints;
	private List<double[]> xContours;
	private List<double[]> zContours;
	private String worldName;
	private Location center;
	
	private static enum FaceDirection {
		NE,
		SE,
		SW,
		NW;
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
        	return String.format("{%d,%d,%s,%s}",x,z,String.valueOf(isConvex),face.toString());
        }
	}
	
	public AreaTerritory(KonTerritory territory) {
		this.worldName = territory.getWorld().getName();
		this.areaPoints = territory.getChunkList().keySet();
		this.center = territory.getCenterLoc();
		this.xPoints = new ArrayList<double[]>();
		this.zPoints = new ArrayList<double[]>();
		this.xContours = new ArrayList<double[]>();
		this.zContours = new ArrayList<double[]>();
		calculateCorners();
	}
	
	private void calculateCorners() {
		ArrayList<Coord> cornerList = new ArrayList<Coord>();
		
		int[] cornerLUTX = {1,	1,	-1,	-1};
		int[] cornerLUTZ = {1, -1,  -1,  1};
		int[] offsetLUTX = {16,	16,	 0,	 0};
		int[] offsetLUTZ = {16, 0,	 0,	 16};
		FaceDirection[] dirLUT = {FaceDirection.NE,FaceDirection.SE,FaceDirection.SW,FaceDirection.NW};
		boolean adj1, adj2, opp = false;
		
		for(Point p : areaPoints) {
			/* Evaluate Corners */
			for(int i = 0; i < 4; i++) {
				adj1 = areaPoints.contains(new Point(p.x+0,            p.y+cornerLUTZ[i]));
				adj2 = areaPoints.contains(new Point(p.x+cornerLUTX[i],p.y+0));
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
		return areaPoints.size();
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
	
	public Set<Point> getPoints() {
		return areaPoints;
	}
	
	/**
	 * Search pattern: Start at x-most corner and move clockwise along contour. If there
	 * 	are corners left over, begin a new search for a new contour, etc.
	 * 		
	 * @param coords - List of all corners in this territory
	 * @return List of contours
	 */
	private List<List<Coord>> sortedCoords(List<Coord> coords) {
		List<List<Coord>> sortedList = new ArrayList<List<Coord>>();
		List<Coord> remainingCorners = new ArrayList<Coord>();
		remainingCorners.addAll(coords);
		//
		int timeout = 100;
		while(!remainingCorners.isEmpty() && timeout > 0) {

			List<Coord> contour = new ArrayList<Coord>();
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
									if ((candidate != null && c.z > candidate.z) || candidate == null) {
										candidate = c;
									}
								}
								break;
							case SE:
								if (current.z == c.z && current.x > c.x) {
									if ((candidate != null && c.x > candidate.x) || candidate == null) {
										candidate = c;
									}
								}
								break;
							case SW:
								if (current.x == c.x && current.z < c.z) {
									if ((candidate != null && c.z < candidate.z) || candidate == null) {
										candidate = c;
									}
								}
								break;
							case NW:
								if (current.z == c.z && current.x < c.x) {
									if ((candidate != null && c.x < candidate.x) || candidate == null) {
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
									if ((candidate != null && c.x < candidate.x) || candidate == null) {
										candidate = c;
									}
								}
								break;
							case SE:
								if (current.x == c.x && current.z > c.z) {
									if ((candidate != null && c.z > candidate.z) || candidate == null) {
										candidate = c;
									}
								}
								break;
							case SW:
								if (current.z == c.z && current.x > c.x) {
									if ((candidate != null && c.x > candidate.x) || candidate == null) {
										candidate = c;
									}
								}
								break;
							case NW:
								if (current.x == c.x && current.z < c.z) {
									if ((candidate != null && c.z < candidate.z) || candidate == null) {
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
					//ChatUtil.printDebug("  Found next corner: "+candidate.toString());
				} else {
					isSearchSuccessful = false;
					//ChatUtil.printDebug("Could not find corner contiguous corner, stopping contour search.");
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
