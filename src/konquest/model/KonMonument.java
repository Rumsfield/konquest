package konquest.model;

import konquest.utility.ChatUtil;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;

public class KonMonument{

	private Location centerLoc;
	private int baseY;
	private int height;
	private Location travelPoint;
	private boolean isValid;
	private int criticalHits;
	private boolean isItemDropsDisabled;
	private boolean isDamageDisabled;
	
	public KonMonument(Location centerLoc) {
		this.centerLoc = centerLoc;
		this.baseY = (int)centerLoc.getY();
		this.height = 0;
		this.travelPoint = centerLoc;
		this.isValid = false;
		this.criticalHits = 0;
		this.isItemDropsDisabled = false;
		this.isDamageDisabled = false;
	}
	
	/**
	 * Initialize a monument from a kingdom's monument template
	 * @param template
	 * @return status code:
	 * 			0 = success
	 * 			1 = invalid template
	 * 			2 = bad chunk gradient
	 * 			3 = bedrock placement
	 */
	public int initialize(KonMonumentTemplate template, Location centerLoc) {
		// Verify valid template
		if(!template.isValid()) {
			ChatUtil.printDebug("Monument init failed: template is not valid");
			return 1;
		}
		// Verify template is 16x16 blocks
		int x_diff = (int)Math.abs(template.getCornerOne().getX()-template.getCornerTwo().getX())+1;
		int z_diff = (int)Math.abs(template.getCornerOne().getZ()-template.getCornerTwo().getZ())+1;
		
		if(Math.abs(x_diff) != 16 || Math.abs(z_diff) != 16) {
			ChatUtil.printDebug("Monument init failed: template is not 16x16 blocks");
			return 1;
		}
		// Verify center chunk gradient, ignoring leaves, grass
		Chunk chunk = centerLoc.getChunk();
		ChunkSnapshot chunkSnap = chunk.getChunkSnapshot(true,false,false);
		String minMaterial = "";
		String maxMaterial = "";
		int maxY = 0;
		int minY = 256;
		for(int x=0;x<16;x++) {
			for(int z=0;z<16;z++) {
				int y = chunkSnap.getHighestBlockYAt(x, z);
				//ChatUtil.printDebug("Checking chunk block "+x+","+z);
				// Search for next highest block if current block is leaves
				while(chunk.getBlock(x, y-1, z).isPassable() || !chunkSnap.getBlockType(x, y-1, z).isOccluding() || chunkSnap.getBlockType(x, y-1, z).equals(Material.AIR)) {
					//ChatUtil.printDebug("	Found leaves or air at local position "+x+","+y+","+z);
					y--;
					if(y <= 0) {
						ChatUtil.printDebug("Could not find non-leaves block in chunk "+chunkSnap.toString()+", local position "+x+","+y+","+z);
						break;
					}
				}
				//ChatUtil.printDebug("		Highest block: "+chunkSnap.getBlockType(x, y-1, z).toString());
				if(y > maxY) {
					maxY = y;
					maxMaterial = chunkSnap.getBlockType(x, y-1, z).toString();
				}
				if(y < minY) {
					minY = y;
					minMaterial = chunkSnap.getBlockType(x, y-1, z).toString();
				}
			}
		}
		ChatUtil.printDebug("Total Highest block: "+maxMaterial);
		ChatUtil.printDebug("Total Lowest block: "+minMaterial);
		if(maxY - minY > 3) {
			int gradient = maxY - minY;
			ChatUtil.printDebug("Monument init failed: town center is not flat enough, gradient is "+gradient+" but must be at most 3.");
			return 2;
		}
		if(maxMaterial.equals(Material.BEDROCK.toString())) {
			ChatUtil.printDebug("Monument init failed: town monument attempted to place on bedrock.");
			return 3;
		}
		
		int standY = centerLoc.getBlockY();
		if(standY < minY || standY > maxY) {
			ChatUtil.printDebug("Monument init failed: center position "+standY+" is outside of gradiant bounds ("+minY+","+maxY+")");
			return 3;
		}
		baseY = standY-1;
		
		// Passed all init checks, update the monument with Template parameters
		boolean updatePass = updateFromTemplate(template);
		if(!updatePass) {
			ChatUtil.printDebug("Monument init failed: could not update invalid template");
		}
		return updatePass ? 0 : 1;
	}
	
	public boolean updateFromTemplate(KonMonumentTemplate template) {
		// Verify valid template
		if(!template.isValid()) {
			return false;
		}
		// Translate travel point location
        int minBlockX = Math.min(template.getCornerOne().getBlockX(), template.getCornerTwo().getBlockX());
        int minBlockY = Math.min(template.getCornerOne().getBlockY(), template.getCornerTwo().getBlockY());
        int minBlockZ = Math.min(template.getCornerOne().getBlockZ(), template.getCornerTwo().getBlockZ());
        
		//ChatUtil.printDebug("Updating monument travel point");
		//ChatUtil.printDebug("Using template base point "+minBlockX+","+minBlockY+","+minBlockZ);
		int offsetX = (int)(template.getTravelPoint().getX() - minBlockX);
		int offsetY = (int)(template.getTravelPoint().getY() - minBlockY);
		int offsetZ = (int)(template.getTravelPoint().getZ() - minBlockZ);
		//ChatUtil.printDebug("Using template offset "+offsetX+","+offsetY+","+offsetZ);
		travelPoint = centerLoc.getChunk().getBlock(0, baseY+1, 0).getLocation().clone().add(offsetX, offsetY+1, offsetZ);
		//ChatUtil.printDebug("Updated new travel location "+travelPoint.getX()+","+travelPoint.getY()+","+travelPoint.getZ());
		height = template.getHeight();
		//isValid = template.isValid();
		return true;
	}

	
	public boolean isValid() {
		return isValid;
	}
	
	public Location getTravelPoint() {
		return travelPoint;
	}
	
	public int getBaseY() {
		return baseY;
	}
	
	public int getHeight() {
		return height;
	}
	
	public int getCriticalHits() {
		return criticalHits;
	}
	
	public int getTopY() {
		return baseY+height;
	}
	
	// Used by entityListener.onItemSpawn() to prevent item drops during monument pastes/removes
	public boolean isItemDropsDisabled() {
		return isItemDropsDisabled;
	}
	
	// Used by entityListener.onEntityDamage() to prevent damage during monument pastes/removes
	public boolean isDamageDisabled() {
		return isDamageDisabled;
	}
	
	public boolean isLocInside(Location loc) {
		//ChatUtil.printDebug("Evaluating whether location is inside Monument region: "+loc.toString());
		if(height == 0) {
			return false;
		}
		Chunk centerChunk = centerLoc.getChunk();
		Chunk testChunk = loc.getChunk();
		boolean isChunkMatch = centerLoc.getWorld().equals(loc.getWorld()) && centerChunk.getX() == testChunk.getX() && centerChunk.getZ() == testChunk.getZ();
		//ChatUtil.printDebug("Chunk match: "+isChunkMatch);	
		//ChatUtil.printDebug("Comparing Y "+loc.getBlockY()+" to baseY "+baseY+" and height "+height);
		return isChunkMatch && loc.getBlockY() >= baseY && loc.getBlockY() < baseY+height;
	}
	
	public void setIsValid(boolean val) {
		isValid = val;
	}
	
	public void setBaseY(int newY) {
		baseY = newY;
	}
	
	public void setHeight(int newHeight) {
		height = newHeight;
	}
	
	public void addCriticalHit() {
		criticalHits++;
	}
	
	public void clearCriticalHits() {
		criticalHits = 0;
	}
	
	public void setIsItemDropsDisabled(boolean val) {
		isItemDropsDisabled = val;
	}
	
	public void setIsDamageDisabled(boolean val) {
		isDamageDisabled = val;
	}
	
}
