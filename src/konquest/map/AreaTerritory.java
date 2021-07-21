package konquest.map;

import java.awt.Point;
import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.Comparator;
import java.util.List;
import java.util.Set;

import konquest.model.KonTerritory;
//import konquest.utility.ChatUtil;

//TODO: X and Z corner double[]'s are primary boundary contour. Internal unclaimed points yield secondary contours within primary area that will
// be shaded white. These dead zones will be kept in a collection that can be used to spawn secondary AreaMarkers to mark the unclaimed pockets.

public class AreaTerritory {

	private Set<Point> areaPoints;
	private double[] xCorners;
	private double[] zCorners;
	private String worldName;
	
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
		List<Coord> corners = sortedCoords(cornerList);
		xCorners = new double[corners.size()];
		zCorners = new double[corners.size()];
		
		for(int i = 0; i < corners.size(); i++) {
			xCorners[i] = corners.get(i).x;
			zCorners[i] = corners.get(i).z;
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
	
	/**
	 * Search pattern: Start at x-most corner and move clockwise along contour
	 * 		
	 * @param coords
	 * @return
	 */
	private List<Coord> sortedCoords(List<Coord> coords) {
		List<Coord> sortedList = new ArrayList<Coord>();
		List<Coord> remainingCorners = new ArrayList<Coord>();
		remainingCorners.addAll(coords);
		// Find max x corner
		Coord current = remainingCorners.get(0);
		for (Coord c : remainingCorners) {
			if (c.x > current.x) {
				current = c;
			}
		}
		Coord candidate;
		remainingCorners.remove(current);
		sortedList.add(current);
		//ChatUtil.printDebug("Beginning corner search for territory with "+coords.size()+" corners...");
		// Find consecutive corners
		boolean isSearchSuccessful = true;
		while(sortedList.size() < coords.size() && isSearchSuccessful) {
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
				sortedList.add(candidate);
				current = candidate;
				//ChatUtil.printDebug("  Found next corner: "+candidate.toString());
			} else {
				isSearchSuccessful = false;
				//ChatUtil.printDebug("Could not find corner! Stopping search.");
			}
		}
		
		return sortedList;
	}
	
	/*
	private List<Coord> sortedCoords(List<Coord> coords) {
  		List<Coord> sortedList = coords;
  		ChatUtil.printDebug("Sorting "+coords.size()+" town corners...");
  		// Calculate geometric bounding box center
  		int xMax = 0;
  		int zMax = 0;
  		int xMin = Integer.MAX_VALUE;
  		int zMin = Integer.MAX_VALUE;
  		for (Coord c : coords) {
  			if(c.x > xMax) {
  				xMax = c.x;
  			}
  			if(c.x < xMin) {
  				xMin = c.x;
  			}
  			if(c.z > zMax) {
  				zMax = c.z;
  			}
  			if(c.z < zMin) {
  				zMin = c.z;
  			}
  		}
  		Coord center = new Coord((xMin+xMax)/2,(zMin+zMax)/2);
  		ChatUtil.printDebug("  Center X = "+center.x+", Z = "+center.z);
  		// Define clockwise sorting criteria
  		// https://stackoverflow.com/questions/6989100/sort-points-in-clockwise-order
  		Comparator<Coord> coordComparator = new Comparator<Coord>() {
  			@Override
  			public int compare(final Coord a, Coord b) {
  				if(a.x == b.x && a.z == b.z) {
  					ChatUtil.printDebug("  Compared ("+a.x+","+a.z+") & ("+b.x+","+b.z+") code 0");
  					return 0;
  				}
  				// a is +x of center and b is -x of center
  				if(a.x - center.x >= 0 && b.x - center.x < 0) {
  					ChatUtil.printDebug("  Compared ("+a.x+","+a.z+") & ("+b.x+","+b.z+") code 1");
  					return -1;
  				}
  				// a is -x of center and b is +x of center
  				if(a.x - center.x < 0 && b.x - center.x >= 0) {
  					ChatUtil.printDebug("  Compared ("+a.x+","+a.z+") & ("+b.x+","+b.z+") code 2");
  					return 1;
  				}
  				// a and b are on center x axis
  				if(a.x - center.x == 0 && b.x - center.x == 0) {
  					// a or b are +z of center
  					if(a.z - center.z >= 0 || b.z - center.z >= 0) {
  						if(a.z > b.z) {
  							ChatUtil.printDebug("  Compared ("+a.x+","+a.z+") & ("+b.x+","+b.z+") code 3");
  							return -1;
  						} else {
  							ChatUtil.printDebug("  Compared ("+a.x+","+a.z+") & ("+b.x+","+b.z+") code 4");
  							return 1;
  						}
  					// a and b are -z of center
  					} else {
  						if(b.z > a.z) {
  							ChatUtil.printDebug("  Compared ("+a.x+","+a.z+") & ("+b.x+","+b.z+") code 5");
  							return -1;
  						} else {
  							ChatUtil.printDebug("  Compared ("+a.x+","+a.z+") & ("+b.x+","+b.z+") code 6");
  							return 1;
  						}
  					}
  				}
  				
  				// compute the cross product of vectors (center -> a) x (center -> b)
  				int det = ((a.x - center.x) * (b.z - center.z)) - ((b.x - center.x) * (a.z - center.z));
  				if(det > 0) {
  					ChatUtil.printDebug("  Compared ("+a.x+","+a.z+") & ("+b.x+","+b.z+") code 7");
  					return -1;
  				}
  				if(det < 0) {
  					ChatUtil.printDebug("  Compared ("+a.x+","+a.z+") & ("+b.x+","+b.z+") code 8");
  					return 1;
  				}
  				
  				// points a and b are on the same line from the center
  			    // check which point is closer to the center
  			    int d1 = (a.x - center.x) * (a.x - center.x) + (a.z - center.z) * (a.z - center.z);
  			    int d2 = (b.x - center.x) * (b.x - center.x) + (b.z - center.z) * (b.z - center.z);
  			    if(d1 < d2) {
  			    	ChatUtil.printDebug("  Compared ("+a.x+","+a.z+") & ("+b.x+","+b.z+") code 9");
					return -1;
				} else {
					ChatUtil.printDebug("  Compared ("+a.x+","+a.z+") & ("+b.x+","+b.z+") code 10");
					return 1;
				}
  			}
  		};
  		Collections.sort(sortedList, coordComparator);
  		ChatUtil.printDebug("  Resulting sort: "+Arrays.toString(sortedList.toArray()));
  		return sortedList;
  	}
	*/
}
