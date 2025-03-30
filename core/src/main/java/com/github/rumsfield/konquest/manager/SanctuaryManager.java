package com.github.rumsfield.konquest.manager;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.HelperUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * A Sanctuary is a territory which contains monument templates.
 * A monument template can exist in an invalid state, where it does not meet specific criteria (critical blocks, size, etc).
 */
public class SanctuaryManager {

	private final Konquest konquest;
	private final HashMap<String, KonSanctuary> sanctuaryMap; // lower case name maps to sanctuary object
	private boolean isSanctuaryDataNull;
	
	public SanctuaryManager(Konquest konquest) {
		this.konquest = konquest;
		this.sanctuaryMap = new HashMap<>();
		this.isSanctuaryDataNull = false;
	}
	
	
	public void initialize() {
		// Loads sanctuary territories and monument templates
		loadSanctuaries();
		ChatUtil.printDebug("Sanctuary Manager is ready");
	}
	
	public void refresh() {
		// Resets all sanctuary kingdoms, used during plugin enable init sequence
		for(KonSanctuary sanctuary : sanctuaryMap.values()) {
			sanctuary.setKingdom(konquest.getKingdomManager().getNeutrals());
		}
	}
	
	public boolean isSanctuary(String name) {
		// Check for lower-case name only
		return sanctuaryMap.containsKey(name.toLowerCase());
	}
	
	public boolean addSanctuary(Location loc, String name) {
		boolean result = false;
		if(konquest.validateNameConstraints(name) == 0) {
			// Verify no overlapping init chunks
			for(Point point : HelperUtil.getAreaPoints(loc, 2)) {
				if(konquest.getTerritoryManager().isChunkClaimed(point,loc.getWorld())) {
					ChatUtil.printDebug("Found a chunk conflict during sanctuary init: "+name);
					return false;
				}
			}
			// Add sanctuary to map with lower-case key
			String nameLower = name.toLowerCase();
			sanctuaryMap.put(nameLower, new KonSanctuary(loc, name, konquest.getKingdomManager().getNeutrals(), konquest));
			sanctuaryMap.get(nameLower).initClaim();
			sanctuaryMap.get(nameLower).updateBarPlayers();
			// Update territory cache
			konquest.getTerritoryManager().addAllTerritory(loc.getWorld(),sanctuaryMap.get(nameLower).getChunkList());
			// Update border particles
			konquest.getTerritoryManager().updatePlayerBorderParticles(loc);
			// Update maps
			konquest.getMapHandler().drawUpdateTerritory(sanctuaryMap.get(nameLower));
			result = true;
		}
		return result;
	}
	
	// Return false if the removal was not allowed
	public boolean removeSanctuary(String name) {
		/*
		 * 1) Removing a sanctuary also removes all templates within.
		 * 2) When removing a template, active kingdoms get re-assigned to another valid template, if one exists, else the kingdoms have a null template.
		 * 3) When a new template is created, all kingdoms with invalid templates get assigned to it.
		 */
		
		// Check if name exists
		if(!isSanctuary(name)) return false;
		// Remove the sanctuary
		KonSanctuary oldSanctuary = sanctuaryMap.remove(name.toLowerCase());
		if(oldSanctuary != null) {
			ArrayList<KonPlayer> nearbyPlayers = konquest.getPlayerManager().getPlayersNearTerritory(oldSanctuary);
			// Get template names that are being removed
			Set<String> templateNames = oldSanctuary.getTemplateNames();
			// Clear all templates
			oldSanctuary.clearAllTemplates();
			// Remove display bars
			oldSanctuary.removeAllBarPlayers();
			// Remove territory
			ArrayList<Point> sanctuaryPoints = new ArrayList<>(oldSanctuary.getChunkList().keySet());
			konquest.getShopHandler().deleteShopsInPoints(sanctuaryPoints,oldSanctuary.getWorld());
			konquest.getTerritoryManager().removeAllTerritory(oldSanctuary.getCenterLoc().getWorld(), oldSanctuary.getChunkList().keySet());
			// Update border particles
			for(KonPlayer player : nearbyPlayers) {
				konquest.getTerritoryManager().updatePlayerBorderParticles(player);
			}
			// Update maps
			konquest.getMapHandler().drawRemoveTerritory(oldSanctuary);
			ChatUtil.printDebug("Removed Sanctuary "+name);
			// De-reference the object
			oldSanctuary = null;
			// Clear templates from kingdoms
			for(KonKingdom kingdom : konquest.getKingdomManager().getKingdoms()) {
				if(templateNames.contains(kingdom.getMonumentTemplateName())) {
					kingdom.clearMonumentTemplate();
					ChatUtil.printDebug("Cleared monument template from kingdom "+kingdom.getName());
				}
			}
			// Update kingdom template assignments
			refreshKingdomTemplates();
			return true;
		}
		return false;
	}
	
	public boolean renameSanctuary(String name, String newName) {
		boolean result = false;
		if(isSanctuary(name) && konquest.validateNameConstraints(newName) == 0) {
			konquest.getMapHandler().drawRemoveTerritory(sanctuaryMap.get(name.toLowerCase()));
			sanctuaryMap.get(name.toLowerCase()).setName(newName);
			KonSanctuary sanctuary = sanctuaryMap.remove(name.toLowerCase());
			sanctuaryMap.put(newName.toLowerCase(), sanctuary);
			konquest.getMapHandler().drawUpdateTerritory(sanctuary);
			sanctuary.updateBarTitle();
			sanctuary.updateBarPlayers();
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
		Set<String> result = new HashSet<>();
		for(KonSanctuary sanctuary : sanctuaryMap.values()) {
			result.add(sanctuary.getName());
		}
		return result;
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

	public KonSanctuary getSanctuaryOfTemplate(KonMonumentTemplate template) {
		for(KonSanctuary sanctuary : sanctuaryMap.values()) {
			if(sanctuary.isTemplate(template.getName())) {
				return sanctuary;
			}
		}
		return null;
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
	
	public boolean isValidTemplate(String name) {
		boolean result = false;
		for(KonSanctuary sanctuary : sanctuaryMap.values()) {
			if(sanctuary.isTemplate(name) && sanctuary.getTemplate(name).isValid()) {
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
		Set<String> result = new HashSet<>();
		for(KonSanctuary sanctuary : sanctuaryMap.values()) {
			for(KonMonumentTemplate template : sanctuary.getTemplates()) {
				result.add(template.getName());
			}
		}
		return result;
	}
	
	public Set<String> getAllValidTemplateNames() {
		Set<String> result = new HashSet<>();
		for(KonSanctuary sanctuary : sanctuaryMap.values()) {
			for(KonMonumentTemplate template : sanctuary.getTemplates()) {
				if(template.isValid()) {
					result.add(template.getName());
				}
			}
		}
		return result;
	}

	public Set<KonMonumentTemplate> getAllTemplates() {
		Set<KonMonumentTemplate> result = new HashSet<>();
		for(KonSanctuary sanctuary : sanctuaryMap.values()) {
			result.addAll(sanctuary.getTemplates());
		}
		return result;
	}

	public Set<KonMonumentTemplate> getAllValidTemplates() {
		Set<KonMonumentTemplate> result = new HashSet<>();
		for(KonSanctuary sanctuary : sanctuaryMap.values()) {
			for(KonMonumentTemplate template : sanctuary.getTemplates()) {
				if(template.isValid()) {
					result.add(template);
				}
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

	public int getTallestTemplateHeight() {
		int maxHeight = -1;
		for(KonSanctuary sanctuary : sanctuaryMap.values()) {
			for(KonMonumentTemplate template : sanctuary.getTemplates()) {
				int templateHeight = template.getHeight();
				if(templateHeight > maxHeight) {
					maxHeight = templateHeight;
				}
			}
		}
		return maxHeight;
	}
	
	/**
	 * For each kingdom with a null template, assign any remaining valid template
	 */
	public void refreshKingdomTemplates() {
		ChatUtil.printDebug("Refreshing all kingdom templates...");
		// Check for valid templates
		Set<KonMonumentTemplate> templates = getAllValidTemplates();
		if(!templates.isEmpty()) {
			// Choose a template
			KonMonumentTemplate chosenTemplate = templates.iterator().next();
			// Assign kingdoms in need
			for(KonKingdom kingdom : konquest.getKingdomManager().getKingdoms()) {
				if(!kingdom.hasMonumentTemplate()) {
					ChatUtil.printDebug("Updating kingdom "+kingdom.getName()+" to template "+chosenTemplate.getName());
					kingdom.updateMonumentTemplate(chosenTemplate);
				}
			}
		} else {
			ChatUtil.printDebug("No valid templates exist!");
		}
	}

	public boolean renameMonumentTemplate(String name, String newName) {
		if (!isTemplate(name) || konquest.validateNameConstraints(newName) != 0) {
			return false;
		}
		// Get the template's sanctuary
		KonSanctuary templateSanctuary = null;
		for(KonSanctuary sanctuary : sanctuaryMap.values()) {
			if(sanctuary.isTemplate(name)) {
				templateSanctuary = sanctuary;
				break;
			}
		}
		if (templateSanctuary == null) {
			return false;
		}
		// Update the name
		return templateSanctuary.renameTemplate(name,newName);
	}
	
	public boolean removeMonumentTemplate(String name) {
		boolean result = false;
		String sanctuaryName = "";
		for(KonSanctuary sanctuary : sanctuaryMap.values()) {
			if(sanctuary.isTemplate(name)) {
				sanctuary.stopTemplateBlanking(name);
				result = sanctuary.removeTemplate(name);
				// Update Sanctuary Label
				konquest.getMapHandler().drawLabelTerritory(sanctuary);
				sanctuaryName = sanctuary.getName();
				break;
			}
		}
		if(result) {
			ChatUtil.printDebug("Removed template "+name+" from sanctuary "+sanctuaryName);
			// Clear templates from kingdoms
			for(KonKingdom kingdom : konquest.getKingdomManager().getKingdoms()) {
				if(kingdom.getMonumentTemplateName().equals(name)) {
					kingdom.clearMonumentTemplate();
					ChatUtil.printDebug("Cleared monument template from kingdom "+kingdom.getName());
				}
			}
			// Try to update templates
			refreshKingdomTemplates();
		} else {
			ChatUtil.printDebug("Failed to remove template "+name);
		}
		return result;
	}
	
	/**
	 * createMonumentTemplate - Creates a new monument template, or updates an existing one.
	 * @param sanctuary		The sanctuary territory that contains this new template
	 * @param corner1		The first corner of the template region
	 * @param corner2		The second (opposite) corner of the template region
	 * @param travelPoint	The location for travel to the template region
	 * @param cost          The unique cost of this template, in addition to the base confi cost
	 * @param save          Whether to save to file upon successful creation
	 * @param forceLoad     Whether to force a template to be added even when invalid
	 * @return  The status code
	 * 			0 - success
	 * 			1 - Region is not 16x16 blocks in base
	 * 			2 - Region does not contain enough critical blocks
	 * 			3 - Region does not contain the travel point
	 * 			4 - Region is not within given sanctuary territory
	 * 			5 - Bad name
	 * 		    10 - Update to existing template failed, restored previous template
	 */
	public int createMonumentTemplate(KonSanctuary sanctuary, String name, Location corner1, Location corner2, Location travelPoint, double cost, boolean save, boolean forceLoad) {
		// Check if the given name is an existing template
		KonMonumentTemplate template;
		int status;
		if(isTemplate(name)) {
			// The template already exists, update it
			template = getTemplate(name);
			template.setCornerOne(corner1);
			template.setCornerTwo(corner2);
			template.setTravelPoint(travelPoint);
			template.setCost(cost);
			template.setValid(false);
			// Validate it
			status = validateTemplate(template, sanctuary);
			// Check for success
			if(status == 0) {
				// Passed all checks, update template
				template.setValid(true);
				ChatUtil.printDebug("Updated existing template "+name);
			} else {
				ChatUtil.printDebug("Failed to update existing template "+name);
				// Updated fields failed validation, revert them
				boolean restoreCheck = template.restorePrevious();
				// Re-validate previous template fields
				if(restoreCheck) {
					status = validateTemplate(template, sanctuary);
					if(status == 0) {
						// Passed restore checks
						template.setValid(true);
						ChatUtil.printDebug("Restored existing template "+name);
						return 10;
					} else {
						ChatUtil.printDebug("Failed to validate restored existing template "+name);
					}
				} else {
					// Restored template fields are null, this shouldn't happen :(
					ChatUtil.printDebug("Failed to restore Monument Template, null fields: \""+name+"\"");
					status = -1;
				}
			}
		} else {
			// There is no template, try to create a new one
			if(konquest.validateNameConstraints(name) != 0) {
				ChatUtil.printDebug("Failed to create Monument Template, bad name: \""+name+"\"");
				return 5;
			}
			template = new KonMonumentTemplate(name, corner1, corner2, travelPoint, cost);
			// Validate it
			status = validateTemplate(template, sanctuary);
			// Check for success
			if(status == 0) {
				// Passed all checks
				template.setValid(true);
				// Add to sanctuary
				sanctuary.addTemplate(name, template);
				ChatUtil.printDebug("Created new valid template "+name);
			} else {
				// Add to sanctuary when invalid if forced to
				if(forceLoad) {
					sanctuary.addTemplate(name, template);
				}
				ChatUtil.printDebug("Created new invalid template "+name);
			}
		}
		// Update Sanctuary Label
		konquest.getMapHandler().drawLabelTerritory(sanctuary);
		// Before exit, save to file
		if(status == 0 && save) {
			saveSanctuaries();
			konquest.getConfigManager().saveConfigs();
		}
		return status;
	}
	
	public int createMonumentTemplate(KonSanctuary sanctuary, String name, Location corner1, Location corner2, Location travelPoint, double cost) {
		return createMonumentTemplate(sanctuary, name, corner1, corner2, travelPoint, cost, true, false);
	}

	public void loadMonumentTemplate(KonSanctuary sanctuary, String name, Location corner1, Location corner2, Location travelPoint, double cost) {
		int status = createMonumentTemplate(sanctuary, name, corner1, corner2, travelPoint, cost, false, true);
		if(status != 0) {
			String message = "Invalid Monument Template "+name+" for Sanctuary "+sanctuary.getName()+", ";
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
	
	/**
	 * Checks a template for validation criteria.
	 * This method modifies the reference of the 'template' argument.
	 * @param template The template to validate
	 * @param sanctuary The sanctuary that contains this template
	 * @return Status code
	 * 			0 - Success
	 * 			1 - Region is not 16x16 blocks in base
	 * 			2 - Region does not contain enough critical blocks
	 * 			3 - Region does not contain the travel point
	 * 			4 - Region is not within given sanctuary territory
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
		int maxCriticalhits = konquest.getCore().getInt(CorePath.MONUMENTS_DESTROY_AMOUNT.getPath());
		int bottomBlockX, bottomBlockY, bottomBlockZ;
		int topBlockX, topBlockY, topBlockZ;
		int c1X = corner1.getBlockX();
		int c1Y = corner1.getBlockY();
		int c1Z = corner1.getBlockZ();
		int c2X = corner2.getBlockX();
		int c2Y = corner2.getBlockY();
		int c2Z = corner2.getBlockZ();
		bottomBlockX = Math.min(c1X, c2X);
		bottomBlockY = Math.min(c1Y, c2Y);
		bottomBlockZ = Math.min(c1Z, c2Z);
		topBlockX = Math.max(c1X, c2X);
		topBlockY = Math.max(c1Y, c2Y);
		topBlockZ = Math.max(c1Z, c2Z);
		int criticalBlockCount = 0;
		int totalBlockCount = 0;
		int lootChestCount = 0;
		boolean containsChest = false;
		for (int x = bottomBlockX; x <= topBlockX; x++) {
            for (int y = bottomBlockY; y <= topBlockY; y++) {
                for (int z = bottomBlockZ; z <= topBlockZ; z++) {
                	Block monumentBlock = corner1.getWorld().getBlockAt(x, y, z);
                	if(monumentBlock.getType().equals(konquest.getKingdomManager().getTownCriticalBlock())) {
                		criticalBlockCount++;
                	} else if(monumentBlock.getState().getBlockData() instanceof Chest) {
						// Found a chest block.
						// Only count single chests and "right" sides of double chests.
						Chest blockChestData = (Chest)monumentBlock.getState().getBlockData();
						if(blockChestData.getType().equals(Chest.Type.SINGLE) || blockChestData.getType().equals(Chest.Type.RIGHT)) {
							lootChestCount++;
						}
                		containsChest = true;
                	}
                	if(monumentBlock.getType().isSolid()) {
                		totalBlockCount++;
                	}
                }
            }
        }
		// Set template info
		template.setNumBlocks(totalBlockCount);
		template.setNumCriticals(criticalBlockCount);
		template.setNumLootChests(lootChestCount);
		template.setLoot(containsChest);
		if(criticalBlockCount < maxCriticalhits) {
			ChatUtil.printDebug("Failed to validate Monument Template, not enough critical blocks. Found "+criticalBlockCount+", required "+maxCriticalhits);
			return 2;
		}
		// Check that travel point is inside the corner bounds
		if(travelPoint.getBlockX() < bottomBlockX || travelPoint.getBlockX() > topBlockX ||
				travelPoint.getBlockY() < bottomBlockY || travelPoint.getBlockY() > topBlockY ||
				travelPoint.getBlockZ() < bottomBlockZ || travelPoint.getBlockZ() > topBlockZ) {
			ChatUtil.printDebug("Failed to create Monument Template, travel point is outside of corner bounds");
			return 3;
		}
		if(containsChest) {
			ChatUtil.printDebug("Validated Monument Template "+name+" with "+lootChestCount+" loot chest(s)");
		} else {
			ChatUtil.printDebug("Validated Monument Template "+name+" without loot");
		}
		return 0;
	}
	
	private void loadSanctuaries() {
		FileConfiguration sanctuariesConfig = konquest.getConfigManager().getConfig("sanctuaries");
        if (sanctuariesConfig.get("sanctuaries") == null) {
        	ChatUtil.printConsoleError("Failed to load any sanctuaries from sanctuaries.yml! Check file permissions.");
			isSanctuaryDataNull = true;
            return;
        }
        double x,y,z;
        float pitch,yaw;
		double cost;
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
	        			sanctuary.addPoints(HelperUtil.formatStringToPoints(sanctuarySection.getString("chunks","")));
	        			konquest.getTerritoryManager().addAllTerritory(sanctuaryWorld,sanctuary.getChunkList());
	        			// Set properties
	        			ConfigurationSection sanctuaryPropertiesSection = sanctuarySection.getConfigurationSection("properties");
						if(sanctuaryPropertiesSection != null) {
							for (String propertyName : sanctuaryPropertiesSection.getKeys(false)) {
								boolean value = sanctuaryPropertiesSection.getBoolean(propertyName);
								KonPropertyFlag property = KonPropertyFlag.getFlag(propertyName);
								boolean status = sanctuary.setPropertyValue(property, value);
								if (!status) {
									ChatUtil.printDebug("Failed to set invalid property " + propertyName + " to Sanctuary " + sanctuaryName);
								}
							}
						}
						// Update title
						sanctuary.updateBarTitle();
	        			// Load Monument Templates
	        			ConfigurationSection sanctuaryMonumentsSection = sanctuarySection.getConfigurationSection("monuments");
						assert sanctuaryMonumentsSection != null;
	        			for(String templateName : sanctuaryMonumentsSection.getKeys(false)) {
	        				ConfigurationSection templateSection = sanctuaryMonumentsSection.getConfigurationSection(templateName);
							cost = templateSection.getDouble("cost",0.0);
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
	        				// Create  & validate template
							// If it fails validation, it is still loaded into memory, but marked as invalid.
				        	loadMonumentTemplate(sanctuary, templateName, templateCornerOne, templateCornerTwo, templateTravel, cost);
							// Update template
							if (isTemplate(templateName)) {
								KonMonumentTemplate template = getTemplate(templateName);
								if (templateSection.contains("loot")) {
									template.setLootTableName(templateSection.getString("loot",LootManager.defaultLootTableName));
								}
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
        if(sanctuaryMap.isEmpty()) {
			ChatUtil.printDebug("No Sanctuaries to load!");
		} else {
			ChatUtil.printDebug("Loaded Sanctuaries");
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
		 *                  cost:
		 * 					loot:
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
		if(isSanctuaryDataNull && sanctuaryMap.isEmpty()) {
			// There was probably an issue loading sanctuaries, do not save.
			ChatUtil.printConsoleError("Aborted saving sanctuary data because a problem was encountered while loading data from sanctuaries.yml");
			return;
		}
		// Create new config entries
		FileConfiguration newSaveConfig = new YamlConfiguration();
		ConfigurationSection root = newSaveConfig.createSection("sanctuaries");
		try {
			for (String name : sanctuaryMap.keySet()) {
				KonSanctuary sanctuary = sanctuaryMap.get(name);
				ConfigurationSection sanctuarySection = root.createSection(sanctuary.getName());
				sanctuarySection.set("world", sanctuary.getWorld().getName());
				sanctuarySection.set("spawn", new int[]{sanctuary.getSpawnLoc().getBlockX(),
						sanctuary.getSpawnLoc().getBlockY(),
						sanctuary.getSpawnLoc().getBlockZ(),
						(int) sanctuary.getSpawnLoc().getPitch(),
						(int) sanctuary.getSpawnLoc().getYaw()});
				sanctuarySection.set("center", new int[]{sanctuary.getCenterLoc().getBlockX(),
						sanctuary.getCenterLoc().getBlockY(),
						sanctuary.getCenterLoc().getBlockZ()});
				sanctuarySection.set("chunks", HelperUtil.formatPointsToString(sanctuary.getChunkList().keySet()));
				// Properties
				ConfigurationSection sanctuaryPropertiesSection = sanctuarySection.createSection("properties");
				for (KonPropertyFlag flag : KonPropertyFlag.values()) {
					if (sanctuary.hasPropertyValue(flag)) {
						sanctuaryPropertiesSection.set(flag.toString(), sanctuary.getPropertyValue(flag));
					}
				}
				// Monuments
				// Any template in memory, valid or invalid, gets saved to data file.
				// When it gets loaded again by plugin onEnable, validation checks will print detailed errors.
				ConfigurationSection sanctuaryMonumentsSection = sanctuarySection.createSection("monuments");
				for (String monumentName : sanctuary.getTemplateNames()) {
					KonMonumentTemplate template = sanctuary.getTemplate(monumentName);
					if (!template.isValid()) {
						ChatUtil.printConsoleError("Saved invalid monument template named " + monumentName + ", in Sanctuary " + name + ".");
					}
					ConfigurationSection monumentSection = sanctuaryMonumentsSection.createSection(monumentName);
					monumentSection.set("cost", template.getCost());
					monumentSection.set("loot", template.getLootTableName());
					monumentSection.set("travel", new int[]{template.getTravelPoint().getBlockX(),
							template.getTravelPoint().getBlockY(),
							template.getTravelPoint().getBlockZ()});
					monumentSection.set("cornerone", new int[]{template.getCornerOne().getBlockX(),
							template.getCornerOne().getBlockY(),
							template.getCornerOne().getBlockZ()});
					monumentSection.set("cornertwo", new int[]{template.getCornerTwo().getBlockX(),
							template.getCornerTwo().getBlockY(),
							template.getCornerTwo().getBlockZ()});
				}
			}
			// Save to file
			FileConfiguration sanctuariesConfig = konquest.getConfigManager().getConfig("sanctuaries");
			sanctuariesConfig.set("sanctuaries", newSaveConfig.get("sanctuaries")); // apply new save data
			if(!sanctuaryMap.isEmpty()) {
				ChatUtil.printConsole("Saved Sanctuaries");
			}
		} catch (Exception | Error internalError) {
			ChatUtil.printConsoleError("Failed to save sanctuaries, report this as a bug to the plugin author!");
			internalError.printStackTrace();
		}
	}
	
}
