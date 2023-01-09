package com.github.rumsfield.konquest.manager;


public class MigrationManager {

	
	
	/*
	 * Konquest 0.10.3
	 */
	/*
	private void loadKingdoms() {
		//ScheduledThreadPoolExecutor service = new ScheduledThreadPoolExecutor(1);
		FileConfiguration kingdomsConfig = konquest.getConfigManager().getConfig("kingdoms");
        if (kingdomsConfig.get("kingdoms") == null) {
        	ChatUtil.printDebug("There is no kingdoms section in kingdoms.yml");
            return;
        }
        boolean isShieldsEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.towns.enable_shields",false);
        boolean isArmorsEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.towns.enable_armor",false);
        double x,y,z;
        float pitch,yaw;
        List<Double> sectionList;
        String worldName;
        String defaultWorldName = konquest.getConfigManager().getConfig("core").getString("core.world_name","world");
        // Count all towns
        int numTowns = 0;
        for(String kingdomName : kingdomsConfig.getConfigurationSection("kingdoms").getKeys(false)) {
        	numTowns += kingdomsConfig.getConfigurationSection("kingdoms."+kingdomName+".towns").getKeys(false).size();
        }
        LoadingPrinter loadBar = new LoadingPrinter(numTowns,"Loading "+numTowns+" Towns");
        // Load all Kingdoms
        for(String kingdomName : kingdomsConfig.getConfigurationSection("kingdoms").getKeys(false)) {
        	//ChatUtil.printDebug("Loading Kingdom: "+kingdomName);
        	ConfigurationSection kingdomSection = kingdomsConfig.getConfigurationSection("kingdoms."+kingdomName);
        	boolean isPeaceful = kingdomSection.getBoolean("peaceful", false);
        	ConfigurationSection monumentSection = kingdomsConfig.getConfigurationSection("kingdoms."+kingdomName+".monument");
        	// Create Kingdoms and Capitals
        	ConfigurationSection capitalSection = kingdomsConfig.getConfigurationSection("kingdoms."+kingdomName+".capital");
        	World capitalWorld = null;
        	if(capitalSection != null) {
        		worldName = capitalSection.getString("world",defaultWorldName);
        		capitalWorld = Bukkit.getWorld(worldName);
        		if(capitalWorld != null) {
        			// Create capital territory
	        		sectionList = capitalSection.getDoubleList("spawn");
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
		        	Location capital_spawn = new Location(capitalWorld,x,y,z,yaw,pitch);
		        	sectionList = capitalSection.getDoubleList("center");
	        		x = sectionList.get(0);
	        		y = sectionList.get(1);
	        		z = sectionList.get(2);
		        	Location capital_center = new Location(capitalWorld,x,y,z);
		        	// Create Kingdom and capital
		        	addKingdom(capital_center, kingdomName);
		        	// Set Peaceful mode
		        	kingdomMap.get(kingdomName).setPeaceful(isPeaceful);
		        	// Set Capital spawn point
		        	kingdomMap.get(kingdomName).getCapital().setSpawn(capital_spawn);
		        	// Add all Capital chunk claims
		        	kingdomMap.get(kingdomName).getCapital().addPoints(konquest.formatStringToPoints(capitalSection.getString("chunks")));
		        	// Update territory cache
		        	territoryManager.addAllTerritory(capitalWorld,kingdomMap.get(kingdomName).getCapital().getChunkList());
		        	// Create Monument Templates
		        	if(monumentSection != null) {
		        		sectionList = monumentSection.getDoubleList("travel");
		        		x = sectionList.get(0);
		        		y = sectionList.get(1);
		        		z = sectionList.get(2);
			        	Location monument_travel = new Location(capitalWorld,x,y,z);
			        	sectionList = monumentSection.getDoubleList("cornerone");
		        		x = sectionList.get(0);
		        		y = sectionList.get(1);
		        		z = sectionList.get(2);
			        	Location monument_cornerone = new Location(capitalWorld,x,y,z);
			        	sectionList = monumentSection.getDoubleList("cornertwo");
		        		x = sectionList.get(0);
		        		y = sectionList.get(1);
		        		z = sectionList.get(2);
			        	Location monument_cornertwo = new Location(capitalWorld,x,y,z);
			        	// Apply missing criticals, if any
			        	for(Location loc : konquest.formatStringToLocations(monumentSection.getString("criticals",""),capitalWorld)) {
			        		capitalWorld.getBlockAt(loc).setType(townCriticalBlock);
	            		}
			        	// Create a Monument Template region for current Kingdom, avoid saving
			        	// Creation of template will update critical locations within kingdom object
			        	
			        	//TODO: KR update this
		        		//int status = kingdomMap.get(kingdomName).createMonumentTemplate(monument_cornerone, monument_cornertwo, monument_travel, false);
			        	int status = 0;
		        		if(status != 0) {
		        			String message = "Failed to load Monument Template for Kingdom "+kingdomName+", ";
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
			        				message = message+"region is not within Capital territory.";
			        				break;
		        				default:
		        					message = message+"unknown reason.";
		        					break;
		        			}
		        			ChatUtil.printConsoleError(message);
		        			konquest.opStatusMessages.add(message);
		        		}
		        	} else {
		        		ChatUtil.printConsoleError("Null monument template for Kingdom "+kingdomName+" in config file!");
		        		konquest.opStatusMessages.add("Missing monument template for Kingdom "+kingdomName+" in kingdoms.yml config file. Use \"/k admin monument\" to define the monument template for this Kingdom.");
		        	}
        		} else {
        			String message = "Failed to load territory "+kingdomName+" Capital in an unloaded world, "+worldName+". Check plugin load order.";
        			ChatUtil.printConsoleError(message);
        			konquest.opStatusMessages.add(message);
        		}
        	} else {
        		String message = "Failed to load capital for Kingdom "+kingdomName+", is one created?";
        		ChatUtil.printConsoleError(message);
        		konquest.opStatusMessages.add(message);
        	}
        	
        	// Load all towns
        	boolean isMissingMonuments = false;
        	for(String townName : kingdomsConfig.getConfigurationSection("kingdoms."+kingdomName+".towns").getKeys(false)) {
        		//ChatUtil.printDebug("Loading Town: "+townName);
            	ConfigurationSection townSection = kingdomsConfig.getConfigurationSection("kingdoms."+kingdomName+".towns."+townName);
            	if(townSection != null) {
            		worldName = townSection.getString("world",defaultWorldName);
            		World townWorld = Bukkit.getServer().getWorld(worldName);
            		if(townWorld != null) {
            			// Create town
	            		int base = townSection.getInt("base");
	            		sectionList = townSection.getDoubleList("spawn");
	            		x = sectionList.get(0);
	            		y = sectionList.get(1);
	            		z = sectionList.get(2);
		            	Location town_spawn = new Location(townWorld,x,y,z);
		            	sectionList = townSection.getDoubleList("center");
	            		x = sectionList.get(0);
	            		y = sectionList.get(1);
	            		z = sectionList.get(2);
		            	Location town_center = new Location(townWorld,x,y,z);
		            	// Create Town
		            	kingdomMap.get(kingdomName).addTown(town_center, townName);
		            	KonTown town = kingdomMap.get(kingdomName).getTown(townName);
		            	// Set town spawn point
		            	town.setSpawn(town_spawn);
		            	// Setup town monument parameters from template
		            	town.loadMonument(base, kingdomMap.get(kingdomName).getMonumentTemplate());
		            	if(!kingdomMap.get(kingdomName).getMonumentTemplate().isValid()) {
		            		isMissingMonuments = true;
		            		ChatUtil.printConsoleError("Failed to load monument for Town "+townName+" in kingdom "+kingdomName+" from invalid template");
		            	}
		            	// Add all Town chunk claims
		            	town.addPoints(konquest.formatStringToPoints(townSection.getString("chunks")));
		            	// Update territory cache
		            	territoryManager.addAllTerritory(townWorld,town.getChunkList());
			        	// Set shield
			        	boolean isShieldActive = townSection.getBoolean("shield",false);
			        	int shieldTime = townSection.getInt("shield_time",0);
			        	Date now = new Date();
			        	if(isShieldsEnabled && isShieldActive && shieldTime > (now.getTime()/1000)) {
			        		town.activateShield(shieldTime);
			        	}
			        	// Set armor
			        	boolean isArmorActive = townSection.getBoolean("armor",false);
			        	int armorBlocks = townSection.getInt("armor_blocks",0);
			        	if(isArmorsEnabled && isArmorActive && armorBlocks > 0) {
			        		town.activateArmor(armorBlocks);
			        	}
		            	// Set open flag
		            	boolean isOpen = townSection.getBoolean("open",false);
		            	town.setIsOpen(isOpen);
		            	// Set plot flag
		            	boolean isPlotOnly = townSection.getBoolean("plot",false);
		            	town.setIsPlotOnly(isPlotOnly);
		            	// Set redstone flag
		            	boolean isRedstone = townSection.getBoolean("redstone",false);
		            	town.setIsEnemyRedstoneAllowed(isRedstone);
		            	// Set golem offensive flag
		            	boolean isGolemOffensive = townSection.getBoolean("golem_offensive",false);
		            	town.setIsGolemOffensive(isGolemOffensive);
		            	// Assign Lord
		            	String lordUUID = townSection.getString("lord","");
		            	if(!lordUUID.equalsIgnoreCase("")) {
		            		//ChatUtil.printDebug("Town "+townName+" Lord ID is: \""+lordUUID+"\"");
		            		UUID playerID = Konquest.idFromString(lordUUID);
		            		if(playerID != null) {
		            			town.setLord(playerID);
		            		} else {
		            			ChatUtil.printDebug("Town "+townName+" in kingdom "+kingdomName+" has a null UUID! Lord remains invalid");
		            		}
		            	} else {
		            		ChatUtil.printDebug("Town "+townName+" in kingdom "+kingdomName+" does not have a stored Lord ID");
		            	}
		            	// Populate Residents
		            	if(townSection.contains("residents")) {
			            	for(String residentUUID : townSection.getConfigurationSection("residents").getKeys(false)) {
			            		boolean isElite = townSection.getBoolean("residents."+residentUUID);
			            		town.addPlayerResident(Bukkit.getOfflinePlayer(UUID.fromString(residentUUID)),isElite);
			            	}
		            	} else {
		            		//ChatUtil.printDebug("Town "+townName+" does not have any stored residents");
		            	}
		            	// Add invite requests
		            	if(townSection.contains("requests")) {
		            		for(String requestUUID : townSection.getConfigurationSection("requests").getKeys(false)) {
		            			boolean type = townSection.getBoolean("requests."+requestUUID);
		            			town.addJoinRequest(UUID.fromString(requestUUID), type);
		            		}
		            	}
		            	// Add upgrades
		            	if(townSection.contains("upgrades")) {
		            		for(String upgradeName : townSection.getConfigurationSection("upgrades").getKeys(false)) {
		            			int level = townSection.getInt("upgrades."+upgradeName);
		            			KonquestUpgrade upgrade = KonquestUpgrade.getUpgrade(upgradeName);
		            			if(upgrade != null) {
		            				town.addUpgrade(upgrade, level);
		            			}
		            		}
		            	}
		            	// Update upgrade status
		            	konquest.getUpgradeManager().updateTownDisabledUpgrades(town);
		            	// Create plots
		            	if(townSection.contains("plots")) {
		            		for(String plotIndex : townSection.getConfigurationSection("plots").getKeys(false)) {
		            			//ChatUtil.printDebug("Creating plot "+plotIndex+" for town "+town.getName());
		            			HashSet<Point> points = new HashSet<Point>();
		            			points.addAll(konquest.formatStringToPoints(townSection.getString("plots."+plotIndex+".chunks")));
		            			ArrayList<UUID> users = new ArrayList<UUID>();
		            			for(String user : townSection.getStringList("plots."+plotIndex+".members")) {
		            				users.add(UUID.fromString(user));
		            			}
		            			KonPlot plot = new KonPlot(points,users);
		            			if(!konquest.getPlotManager().addPlot(town, plot)) {
		            				ChatUtil.printConsoleError("Failed to add incompatible plot to town "+town.getName());
		            			}
		            		}
		            	}
	        			// Update loading bar
		            	loadBar.addProgress(1);
            		} else {
            			String message = "Failed to load town "+townName+" in an unloaded world, "+townWorld+". Check plugin load order.";
            			ChatUtil.printConsoleError(message);
            			konquest.opStatusMessages.add(message);
            		}
	            }
        	}
        	if(isMissingMonuments) {
        		konquest.opStatusMessages.add("Kingdom "+kingdomName+" has Towns with invalid Monuments. You must create a new Monument Template and restart the server.");
        	}
        }
		ChatUtil.printDebug("Loaded Kingdoms");
	}
	*/
}
