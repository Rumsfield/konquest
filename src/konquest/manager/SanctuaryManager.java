package konquest.manager;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import konquest.Konquest;
import konquest.model.KonMonumentTemplate;
import konquest.model.KonPropertyFlag;
import konquest.model.KonSanctuary;
import konquest.utility.ChatUtil;

public class SanctuaryManager {

	private Konquest konquest;
	private KingdomManager kingdomManager;
	private TerritoryManager territoryManager;
	private HashMap<String, KonSanctuary> sanctuaryMap; // lower case name maps to sanctuary object
	
	public SanctuaryManager(Konquest konquest) {
		this.konquest = konquest;
		this.kingdomManager = konquest.getKingdomManager();
		this.territoryManager = konquest.getTerritoryManager();
		this.sanctuaryMap = new HashMap<String, KonSanctuary>();
	}
	
	
	public void initialize() {
		loadSanctuaries();
		ChatUtil.printDebug("Sanctuary Manager is ready");
	}
	
	public boolean isSanctuary(String name) {
		// Check for lower-case name only
		return sanctuaryMap.containsKey(name.toLowerCase());
	}
	
	public boolean addSanctuary(Location loc, String name) {
		boolean result = false;
		if(!name.contains(" ") && konquest.validateNameConstraints(name) == 0) {
			// Verify no overlapping init chunks
			for(Point point : konquest.getAreaPoints(loc, 2)) {
				if(territoryManager.isChunkClaimed(point,loc.getWorld())) {
					ChatUtil.printDebug("Found a chunk conflict during sanctuary init: "+name);
					return false;
				}
			}
			// Add sanctuary to map with lower-case key
			String nameLower = name.toLowerCase();
			sanctuaryMap.put(nameLower, new KonSanctuary(loc, name, kingdomManager.getNeutrals(), konquest));
			sanctuaryMap.get(nameLower).initClaim();
			sanctuaryMap.get(nameLower).updateBarPlayers();
			territoryManager.addAllTerritory(loc.getWorld(),sanctuaryMap.get(nameLower).getChunkList());
			konquest.getMapHandler().drawDynmapUpdateTerritory(sanctuaryMap.get(nameLower));
			result = true;
		}
		return result;
	}
	
	public boolean removeSanctuary(String name) {
		boolean result = false;
		
		//TODO: KR Add conditions to check if this removal is allowed...
		// If this sanctuary contains templates...
		//		Check for at least 1 other template in another sanctuary
		//		Switch all kingdoms with templates in this sanctuary to another valid template
		//
		// Or just don't allow? Think...
		/*
		 * Option 1: Allow sanctuary removal if other templates exist, and auto-assign to kingdoms with missing templates.
		 * Pros:
		 * 	- Easy to manage sanctuary territory
		 * Cons:
		 * 	- Can unintentionally remove a sanctuary with most templates
		 * 	- More complicated code logic for automatically checking conditions, assigning missing templates
		 * 
		 * Option 2: Strict conditions, cannot remove sanctuary with active templates, cannot remove active templates, 
		 * 			 must manually re-assign kingdoms to new templates to make all templates non-active within sanctuary
		 * 			 in order to remove it.
		 * Pros:
		 * 	- Simpler code logic with strict rules
		 * 	- More idiot-proof, with checks to prevent template removal
		 * Cons:
		 * 	- More cumbersome for admins to remove templates, need to modify every kingdom first
		 * 
		 * Conclusion: Hybrid approach. Cannot remove sanctuary with active templates, but allow explicit removal of those templates.
		 * 			When removing templates, prompt to confirm and list number of kingdoms using that template. Those kingdoms
		 * 			automatically get assigned another template. There must always be at least 1 valid template somewhere, cannot remove
		 * 			the last template. Kingdoms with null template cannot be captured, etc.
		 */
		
		KonSanctuary oldSanctuary = sanctuaryMap.remove(name.toLowerCase());
		if(oldSanctuary != null) {
			oldSanctuary.clearAllTemplates();
			oldSanctuary.removeAllBarPlayers();
			territoryManager.removeAllTerritory(oldSanctuary.getCenterLoc().getWorld(), oldSanctuary.getChunkList().keySet());
			konquest.getMapHandler().drawDynmapRemoveTerritory(oldSanctuary);
			ChatUtil.printDebug("Removed Sanctuary "+name);
			oldSanctuary = null;
			result = true;
		}
		return result;
	}
	
	public KonSanctuary getSanctuary(String name) {
		return sanctuaryMap.get(name.toLowerCase());
	}
	
	public Collection<KonSanctuary> getSanctuaries() {
		return sanctuaryMap.values();
	}
	
	public Set<String> getSanctuaryNames() {
		return sanctuaryMap.keySet();
	}
	
	public String getSanctuaryNameOfTemplate(String name) {
		String result = "";
		for(KonSanctuary sanctuary : sanctuaryMap.values()) {
			if(sanctuary.isTemplate(name)) {
				result = sanctuary.getName();
			}
		}
		return result;
	}
	
	public boolean isTemplate(String name) {
		boolean result = false;
		for(KonSanctuary sanctuary : sanctuaryMap.values()) {
			if(sanctuary.isTemplate(name)) {
				result = true;
				break;
			}
		}
		return result;
	}
	
	public KonMonumentTemplate getTemplate(String name) {
		KonMonumentTemplate result = null;
		for(KonSanctuary sanctuary : sanctuaryMap.values()) {
			if(sanctuary.isTemplate(name)) {
				result = sanctuary.getTemplate(name);
				break;
			}
		}
		return result;
	}
	
	public KonMonumentTemplate getTemplate(Location loc) {
		KonMonumentTemplate result = null;
		for(KonSanctuary sanctuary : sanctuaryMap.values()) {
			for(KonMonumentTemplate template : sanctuary.getTemplates()) {
				if(template.isLocInside(loc)) {
					result = template;
					break;
				}
			}
		}
		return result;
	}
	
	public Set<String> getAllTemplateNames() {
		Set<String> result = new HashSet<String>();
		for(KonSanctuary sanctuary : sanctuaryMap.values()) {
			for(KonMonumentTemplate template : sanctuary.getTemplates()) {
				result.add(template.getName());
			}
		}
		return result;
	}
	
	public int getNumTemplates() {
		int result = 0;
		for(KonSanctuary sanctuary : sanctuaryMap.values()) {
			result += sanctuary.getTemplates().size();
		}
		return result;
	}
	
	/**
	 * createMonumentTemplate - Creates a new monument template, verifies stuff.
	 * @param sanctuary		The sanctuary territory that contains this new template
	 * @param corner1		The first corner of the template region
	 * @param corner2		The second (opposite) corner of the template region
	 * @param travelPoint	The location for travel to the template region
	 * @return  The status code returned by validateTemplate()
	 */
	public int createMonumentTemplate(KonSanctuary sanctuary, String name, Location corner1, Location corner2, Location travelPoint, boolean save) {
		// Create new monument
		KonMonumentTemplate template = new KonMonumentTemplate(name, corner1, corner2, travelPoint);
		// Validate it
		int status = validateTemplate(template, sanctuary);
		
		if(status == 0) {
			// Passed all checks
			template.setValid(true);
			// Add to sanctuary
			sanctuary.addTemplate(name, template);
			// Before exit, save to file
			if(save) {
				saveSanctuaries();
				konquest.getConfigManager().saveConfigs();
			}
		}
		return status;
	}
	
	public int createMonumentTemplate(KonSanctuary sanctuary, String name, Location corner1, Location corner2, Location travelPoint) {
		return createMonumentTemplate(sanctuary, name, corner1, corner2, travelPoint, true);
	}
	
	/**
	 * Checks a template for validation criteria.
	 * This method modifies the reference of the 'template' argument.
	 * @param sanctuary
	 * @param template
	 * @return Status code
	 * 			0 - Success
	 * 			1 - Region is not 16x16 blocks in base
	 * 			2 - Region does not contain enough critical blocks
	 * 			3 - Region does not contain the travel point
	 * 			4 - Region is not within given sanctuary territory
	 * 			5 - Bad name
	 * 	
	 */
	public int validateTemplate(KonMonumentTemplate template, KonSanctuary sanctuary) {
		if(sanctuary == null || template.getCornerOne() == null || template.getCornerTwo() == null || template.getTravelPoint() == null || template.getName() == null) {
			ChatUtil.printDebug("Failed to validate Monument Template, null arguments");
			return 4;
		}
		String name = template.getName();
		Location corner1 = template.getCornerOne();
		Location corner2 = template.getCornerTwo();
		Location travelPoint = template.getTravelPoint();
		
		if(name.contains(" ") || konquest.validateNameConstraints(name) != 0) {
			ChatUtil.printDebug("Failed to validate Monument Template, bad name: \""+name+"\"");
			return 5;
		}
		// Check that both corners are within territory
		if(!sanctuary.isLocInside(corner1) || !sanctuary.isLocInside(corner2)) {
			ChatUtil.printDebug("Failed to validate Monument Template, corners are not inside sanctuary territory");
			return 4;
		}
		// Check 16x16 base dimensions
		int diffX = (int)Math.abs(corner1.getX()-corner2.getX())+1;
		int diffZ = (int)Math.abs(corner1.getZ()-corner2.getZ())+1;
		if(diffX != 16 || diffZ != 16) {
			ChatUtil.printDebug("Failed to validate Monument Template, not 16x16: "+diffX+"x"+diffZ);
			return 1;
		}
		// Check for at least as many critical blocks as required critical hits
		// Also check for any chests for loot flag
		ArrayList<Location> criticalBlockLocs = new ArrayList<Location>();
		int maxCriticalhits = konquest.getConfigManager().getConfig("core").getInt("core.monuments.destroy_amount");
		int bottomBlockX, bottomBlockY, bottomBlockZ = 0;
		int topBlockX, topBlockY, topBlockZ = 0;
		int c1X = corner1.getBlockX();
		int c1Y = corner1.getBlockY();
		int c1Z = corner1.getBlockZ();
		int c2X = corner2.getBlockX();
		int c2Y = corner2.getBlockY();
		int c2Z = corner2.getBlockZ();
		bottomBlockX = (c1X < c2X) ? c1X : c2X;
		bottomBlockY = (c1Y < c2Y) ? c1Y : c2Y;
		bottomBlockZ = (c1Z < c2Z) ? c1Z : c2Z;
		topBlockX = (c1X < c2X) ? c2X : c1X;
		topBlockY = (c1Y < c2Y) ? c2Y : c1Y;
		topBlockZ = (c1Z < c2Z) ? c2Z : c1Z;
		int criticalBlockCount = 0;
		boolean containsChest = false;
		for (int x = bottomBlockX; x <= topBlockX; x++) {
            for (int y = bottomBlockY; y <= topBlockY; y++) {
                for (int z = bottomBlockZ; z <= topBlockZ; z++) {
                	Block monumentBlock = corner1.getWorld().getBlockAt(x, y, z);
                	if(monumentBlock.getType().equals(konquest.getKingdomManager().getTownCriticalBlock())) {
                		criticalBlockCount++;
                		criticalBlockLocs.add(monumentBlock.getLocation());
                	} else if(monumentBlock.getState() instanceof Chest) {
                		containsChest = true;
                	}
                }
            }
        }
		if(criticalBlockCount < maxCriticalhits) {
			ChatUtil.printDebug("Failed to validate Monument Template, not enough critical blocks. Found "+criticalBlockCount+", required "+maxCriticalhits);
			return 2;
		}
		// Check that travel point is inside of the corner bounds
		if(travelPoint.getBlockX() < bottomBlockX || travelPoint.getBlockX() > topBlockX ||
				travelPoint.getBlockY() < bottomBlockY || travelPoint.getBlockY() > topBlockY ||
				travelPoint.getBlockZ() < bottomBlockZ || travelPoint.getBlockZ() > topBlockZ) {
			ChatUtil.printDebug("Failed to create Monument Template, travel point is outside of corner bounds");
			return 3;
		}
		// Set loot flag
		template.setLoot(containsChest);
		if(containsChest) {
			ChatUtil.printDebug("Validated Monument Template "+name+" with loot chest(s)");
		} else {
			ChatUtil.printDebug("Validated Monument Template "+name+" without loot");
		}
		return 0;
	}
	
	public boolean removeMonumentTemplate(String name) {
		boolean result = false;
		for(KonSanctuary sanctuary : sanctuaryMap.values()) {
			if(sanctuary.isTemplate(name)) {
				sanctuary.stopTemplateBlanking(name);
				result = sanctuary.removeTemplate(name);
				break;
			}
		}
		return result;
	}
	
	private void loadSanctuaries() {
		FileConfiguration sanctuariesConfig = konquest.getConfigManager().getConfig("sanctuaries");
        if (sanctuariesConfig.get("sanctuaries") == null) {
        	ChatUtil.printDebug("There is no sanctuaries section in sanctuaries.yml");
            return;
        }
        double x,y,z;
        float pitch,yaw;
        List<Double> sectionList;
        String worldName;
        KonSanctuary sanctuary;
        // Load all Sanctuaries
        ConfigurationSection sanctuariesSection = sanctuariesConfig.getConfigurationSection("sanctuaries");
        for(String sanctuaryName : sanctuariesConfig.getConfigurationSection("sanctuaries").getKeys(false)) {
        	ConfigurationSection sanctuarySection = sanctuariesSection.getConfigurationSection(sanctuaryName);
        	worldName = sanctuarySection.getString("world","world");
        	World sanctuaryWorld = Bukkit.getWorld(worldName);
        	if(sanctuaryWorld != null) {
        		sectionList = sanctuarySection.getDoubleList("spawn");
        		x = sectionList.get(0);
        		y = sectionList.get(1);
        		z = sectionList.get(2);
        		if(sectionList.size() > 3) {
        			double val = sectionList.get(3);
        			pitch = (float)val;
        		} else {
        			pitch = 0;	
        		}
        		if(sectionList.size() > 4) {
        			double val = sectionList.get(4);
        			yaw = (float)val;
        		} else {
        			yaw = 0;	
        		}
            	Location sanctuarySpawn = new Location(sanctuaryWorld,x,y,z,yaw,pitch);
            	sectionList = sanctuarySection.getDoubleList("center");
        		x = sectionList.get(0);
        		y = sectionList.get(1);
        		z = sectionList.get(2);
	        	Location sanctuaryCenter = new Location(sanctuaryWorld,x,y,z);
	        	if(addSanctuary(sanctuaryCenter,sanctuaryName)) {
	        		sanctuary = getSanctuary(sanctuaryName);
	        		if(sanctuary != null) {
	        			// Set spawn location
		        		sanctuary.setSpawn(sanctuarySpawn);
		        		// Set territory chunks
	        			sanctuary.addPoints(konquest.formatStringToPoints(sanctuarySection.getString("chunks","")));
	        			territoryManager.addAllTerritory(sanctuaryWorld,sanctuary.getChunkList());
	        			// Set properties
	        			ConfigurationSection sanctuaryPropertiesSection = sanctuarySection.getConfigurationSection("properties");
	        			for(String propertyName : sanctuaryPropertiesSection.getKeys(false)) {
	        				boolean value = sanctuaryPropertiesSection.getBoolean(propertyName);
	        				KonPropertyFlag property = KonPropertyFlag.getFlag(propertyName);
	        				boolean status = sanctuary.setPropertyValue(property, value);
	        				if(!status) {
	        					ChatUtil.printDebug("Failed to set invalid property "+propertyName+" to Sanctuary "+sanctuaryName);
	        				}
	        			}
	        			// Load Monument Templates
	        			ConfigurationSection sanctuaryMonumentsSection = sanctuarySection.getConfigurationSection("monuments");
	        			for(String templateName : sanctuaryMonumentsSection.getKeys(false)) {
	        				ConfigurationSection templateSection = sanctuaryMonumentsSection.getConfigurationSection(templateName);
	        				sectionList = templateSection.getDoubleList("travel");
			        		x = sectionList.get(0);
			        		y = sectionList.get(1);
			        		z = sectionList.get(2);
				        	Location templateTravel = new Location(sanctuaryWorld,x,y,z);
				        	sectionList = templateSection.getDoubleList("cornerone");
			        		x = sectionList.get(0);
			        		y = sectionList.get(1);
			        		z = sectionList.get(2);
				        	Location templateCornerOne = new Location(sanctuaryWorld,x,y,z);
				        	sectionList = templateSection.getDoubleList("cornertwo");
			        		x = sectionList.get(0);
			        		y = sectionList.get(1);
			        		z = sectionList.get(2);
				        	Location templateCornerTwo = new Location(sanctuaryWorld,x,y,z);
	        				// Create template
				        	int status = createMonumentTemplate(sanctuary, templateName, templateCornerOne, templateCornerTwo, templateTravel, false);
				        	if(status != 0) {
			        			String message = "Failed to load Monument Template for Sanctuary "+sanctuaryName+", ";
			        			switch(status) {
				        			case 1:
				        				message = message+"base dimensions are not 16x16 blocks.";
				        				break;
				        			case 2:
				        				message = message+"region does not contain enough critical blocks.";
				        				break;
				        			case 3:
				        				message = message+"region does not contain a travel point.";
				        				break;
				        			case 4:
				        				message = message+"region is not within territory.";
				        				break;
				        			case 5:
				        				message = message+"invalid name.";
				        				break;
			        				default:
			        					message = message+"unknown reason.";
			        					break;
			        			}
			        			ChatUtil.printConsoleError(message);
			        			konquest.opStatusMessages.add(message);
			        		}
	        			}
	        			// Done
	        		} else {
	        			String message = "Could not load Sanctuary "+sanctuaryName+", sanctuaries.yml may be corrupted and needs to be deleted.";
	            		ChatUtil.printConsoleError(message);
	            		konquest.opStatusMessages.add(message);
	        		}
	        	} else {
        			String message = "Failed to load Sanctuary "+sanctuaryName+", sanctuaries.yml may be corrupted and needs to be deleted.";
            		ChatUtil.printConsoleError(message);
            		konquest.opStatusMessages.add(message);
        		}
        	} else {
    			String message = "Failed to load Sanctuary "+sanctuaryName+" in an unloaded world, "+worldName+". Check plugin load order.";
    			ChatUtil.printConsoleError(message);
    			konquest.opStatusMessages.add(message);
    		}
        }
	}
	
	public void saveSanctuaries() {
		/*
		 * Save file format
		 * 
		 * 	sanctuaries:
		 * 		<name>:
		 * 			world: ?
		 * 			spawn:
		 * 			- ?
		 * 			- ?
		 * 			- ?
		 * 			- ?
		 * 			- ?
		 * 			center:
		 * 			- ?
		 * 			- ?
		 * 			- ?
		 * 			chunks: ""
		 * 			properties:
		 * 				travel: ?
		 * 				pvp: ?
		 * 				build: ?
		 * 				use: ?
		 * 				mobs: ?
		 * 				portals: ?
		 * 				enter: ?
		 * 				exit: ?
		 * 			monuments:
		 * 				<name>:
		 * 					travel:
		 * 					- ?
		 * 					- ?
		 * 					- ?
		 * 					cornerone:
		 * 					- ?
		 * 					- ?
		 * 					- ?
		 * 					cornertwo:
		 * 					- ?
		 * 					- ?
		 * 					- ?
		 */
		FileConfiguration sanctuariesConfig = konquest.getConfigManager().getConfig("sanctuaries");
		sanctuariesConfig.set("sanctuaries", null); // reset sanctuaries config
		ConfigurationSection root = sanctuariesConfig.createSection("sanctuaries");
		for(String name : sanctuaryMap.keySet()) {
			KonSanctuary sanctuary = sanctuaryMap.get(name);
			ConfigurationSection sanctuarySection = root.createSection(sanctuary.getName());
			sanctuarySection.set("world", sanctuary.getWorld().getName());
			sanctuarySection.set("spawn", new int[] {(int) sanctuary.getSpawnLoc().getBlockX(),
												   (int) sanctuary.getSpawnLoc().getBlockY(),
												   (int) sanctuary.getSpawnLoc().getBlockZ(),
												   (int) sanctuary.getSpawnLoc().getPitch(),
												   (int) sanctuary.getSpawnLoc().getYaw()});
			sanctuarySection.set("center", new int[] {(int) sanctuary.getCenterLoc().getBlockX(),
					 								(int) sanctuary.getCenterLoc().getBlockY(),
					 								(int) sanctuary.getCenterLoc().getBlockZ()});
			sanctuarySection.set("chunks", konquest.formatPointsToString(sanctuary.getChunkList().keySet()));
			// Properties
			ConfigurationSection sanctuaryPropertiesSection = sanctuarySection.createSection("properties");
			for(KonPropertyFlag flag : sanctuary.getAllProperties().keySet()) {
				sanctuaryPropertiesSection.set(flag.toString(), sanctuary.getAllProperties().get(flag));
			}
			// Monuments
			ConfigurationSection sanctuaryMonumentsSection = sanctuarySection.createSection("monuments");
			for(String monumentName : sanctuary.getTemplateNames()) {
				KonMonumentTemplate template = sanctuary.getTemplate(monumentName);
				if(template.isValid()) {
		            ConfigurationSection monumentSection = sanctuaryMonumentsSection.createSection(monumentName);
		            monumentSection.set("travel", new int[] {(int) template.getTravelPoint().getBlockX(),
															 (int) template.getTravelPoint().getBlockY(),
															 (int) template.getTravelPoint().getBlockZ()});
		            monumentSection.set("cornerone", new int[] {(int) template.getCornerOne().getBlockX(),
							 								 	(int) template.getCornerOne().getBlockY(),
							 								 	(int) template.getCornerOne().getBlockZ()});
		            monumentSection.set("cornertwo", new int[] {(int) template.getCornerTwo().getBlockX(),
							 								 	(int) template.getCornerTwo().getBlockY(),
							 								 	(int) template.getCornerTwo().getBlockZ()});
				} else {
					ChatUtil.printConsoleError("Failed to save invalid monument template named "+monumentName+", in Sanctuary "+name);
				}
			}
		}
	}
	
}
