package com.github.rumsfield.konquest.map;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.HelperUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.dynmap.markers.MarkerIcon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

//TODO: Make this class into a listener, make events for territory updates, deletes, etc

//TODO: Figure out way to have created and removed areas update on web page without refresh

public class MapHandler {

	private final Konquest konquest;
	private final HashMap<String,Renderable> renderers;

    static final int sanctuaryColor = 0x646464;
	static final int ruinColor = 0x242424;
	static final int campColor = 0xa3a10a;
	static final int lineDefaultColor = 0x000000;
	static final int lineCapitalColor = 0x8010d0;

	static boolean isEnableKingdoms = true;
	static boolean isEnableCamps = true;
	static boolean isEnableSanctuaries = true;
	static boolean isEnableRuins = true;
	static boolean isShowBanners = true;

	private static final HashMap<String,String> imageBase64Cache = new HashMap<>();

	public MapHandler(Konquest konquest) {
		this.konquest = konquest;
		this.renderers = new HashMap<>();
	}
	
	public void initialize() {
		if (konquest.getIntegrationManager().getDynmap().isEnabled()) {
			renderers.put("Dynmap",new DynmapRender(konquest));
		}
		if (konquest.getIntegrationManager().getBlueMap().isEnabled()) {
			renderers.put("BlueMap",new BlueMapRender(konquest));
		}
		if (konquest.getIntegrationManager().getSquaremap().isEnabled()) {
			renderers.put("squaremap",new SquaremapRender(konquest));
		}
		for(Renderable ren : renderers.values()) {
			ren.initialize();
		}
		isEnableKingdoms = konquest.getCore().getBoolean(CorePath.INTEGRATION_MAP_OPTIONS_ENABLE_KINGDOMS.getPath());
		isEnableCamps = konquest.getCore().getBoolean(CorePath.INTEGRATION_MAP_OPTIONS_ENABLE_CAMPS.getPath());
		isEnableSanctuaries = konquest.getCore().getBoolean(CorePath.INTEGRATION_MAP_OPTIONS_ENABLE_SANCTUARIES.getPath());
		isEnableRuins = konquest.getCore().getBoolean(CorePath.INTEGRATION_MAP_OPTIONS_ENABLE_RUINS.getPath());
		isShowBanners = konquest.getCore().getBoolean(CorePath.INTEGRATION_MAP_OPTIONS_SHOW_BANNERS.getPath());

		if (isShowBanners) {
			loadBannerImages();
		}

		printMapFeatures();
	}

	private void printMapFeatures() {
		String lineTemplate = "%-30s -> %s";
		String unavailable = ChatColor.GRAY+"Unavailable";
		String statusKingdoms = ChatUtil.boolean2enable(isEnableKingdoms);
		String statusCamps = ChatUtil.boolean2enable(isEnableCamps);
		String statusSanctuaries = ChatUtil.boolean2enable(isEnableSanctuaries);
		String statusRuins = ChatUtil.boolean2enable(isEnableRuins);
		String statusBanners = ChatUtil.boolean2enable(isShowBanners);
		StringBuilder availableMaps = new StringBuilder();
		if (renderers.isEmpty()) {
			availableMaps.append("None");
			statusKingdoms = unavailable;
			statusCamps = unavailable;
			statusSanctuaries = unavailable;
			statusRuins = unavailable;
			statusBanners = unavailable;
		} else {
			Iterator<Renderable> renderIterator = renderers.values().iterator();
			while (renderIterator.hasNext()) {
				availableMaps.append(renderIterator.next().getMapName());
				if (renderIterator.hasNext()) {
					availableMaps.append(", ");
				}
			}
		}
		String [] status = {
				String.format(lineTemplate,"Available Maps", availableMaps),
				String.format(lineTemplate,"Show Kingdoms",statusKingdoms),
				String.format(lineTemplate,"Show Barbarian Camps",statusCamps),
				String.format(lineTemplate,"Show Sanctuaries",statusSanctuaries),
				String.format(lineTemplate,"Show Ruins",statusRuins),
				String.format(lineTemplate,"Display Banners",statusBanners),
		};
		ChatUtil.printConsoleAlert("Map Summary...");
		for (String row : status) {
			String line = ChatColor.GOLD+"> "+ChatColor.RESET + row;
			Bukkit.getServer().getConsoleSender().sendMessage(line);
		}
	}

	private void loadBannerImages() {
		imageBase64Cache.clear();
		ArrayList<String> bannerNames = new ArrayList<>();
		bannerNames.add("default");
		bannerNames.add("sanctuary");
		bannerNames.add("ruin");
		bannerNames.add("camp");
		bannerNames.addAll(konquest.getKingdomManager().getKingdomNames());
		// Add image files from plugins folder
		for (String bannerKey : bannerNames) {
			String imagePath = "banners/"+bannerKey+".png";
			File imageFile = new File(konquest.getPlugin().getDataFolder(), imagePath);
			if (imageFile.exists() && imageFile.isFile()) {
				// A banner image exists, verify size
				long fileSizeBytes = imageFile.length();
                long IMAGE_MAX_BYTES = 100000;
                if (fileSizeBytes > IMAGE_MAX_BYTES) {
					ChatUtil.printConsoleError("Failed to load banner image file \""+imagePath+"\", file size "+fileSizeBytes+" exceeds maximum limit of "+ IMAGE_MAX_BYTES +" bytes.");
					continue;
				}
				// File is under size limit, add to cache as Base64 encoded string
				String imageData = "";
				try {
					imageData = Base64.getEncoder().encodeToString(Files.readAllBytes(imageFile.toPath()));
				} catch (IOException exc) {
					ChatUtil.printConsoleError("Failed to load banner image file \""+imagePath+"\", problem reading file, check read permissions.");
				}
				if (!imageData.isEmpty()) {
					// Image data is valid
					imageBase64Cache.put(bannerKey,imageData);
				}
			}
		}
		// Ensure default image exists
		if (!imageBase64Cache.containsKey("default")) {
			// Add plugin resource
			InputStream defaultResource = konquest.getPlugin().getResource("banners/default.png");
			if (defaultResource != null) {
				String imageData = "";
				try {
					imageData = Base64.getEncoder().encodeToString(defaultResource.readAllBytes());
				} catch (IOException exc) {
					ChatUtil.printConsoleError("Failed to load default banner image resource, problem reading file, check read permissions.");
				}
				if (!imageData.isEmpty()) {
					// Image data is valid
					imageBase64Cache.put("default",imageData);
				}
			} else {
				ChatUtil.printConsoleError("Failed to load default banner image from plugin resources.");
			}
		}
		// Display loaded results
		ChatUtil.printConsoleAlert("Loaded Map Banner Images...");
		ArrayList<String> loadedBannerNames = new ArrayList<>();
		for (String bannerName : bannerNames) {
			if (imageBase64Cache.containsKey(bannerName)) {
				loadedBannerNames.add(bannerName);
			}
		}
		String line = ChatColor.GOLD+"> "+ChatColor.RESET + HelperUtil.formatCommaSeparatedList(loadedBannerNames);
		Bukkit.getServer().getConsoleSender().sendMessage(line);
	}

	/* Rendering Methods */

	public void drawUpdateTerritory(KonKingdom kingdom) {
		drawUpdateTerritory(kingdom,"");
	}

	public void drawUpdateTerritory(KonKingdom kingdom, String rendererName) {
		for (KonTown town : kingdom.getCapitalTowns()) {
			drawUpdateTerritory(town, rendererName);
		}
	}

	public void drawUpdateTerritory(KonTerritory territory) {
		drawUpdateTerritory(territory,"");
	}

	public void drawUpdateTerritory(KonTerritory territory, String rendererName) {
		if (isTerritoryDisabled(territory) || isTerritoryInvalid(territory)) return;
		AreaTerritory area = new AreaTerritory(territory);
		// Draw updates
		for (Renderable render : getRenderers(rendererName)) {
			try {
				render.drawUpdate(area);
			} catch (Exception | Error problem) {
				ChatUtil.printConsoleError(render.getMapName()+" encountered a problem while trying to draw territory "+territory.getName());
				problem.printStackTrace();
			}
		}
	}

	public void drawRemoveTerritory(KonTerritory territory) {
		drawRemoveTerritory(territory,"");
	}

	public void drawRemoveTerritory(KonTerritory territory, String rendererName) {
		if (isTerritoryDisabled(territory) || isTerritoryInvalid(territory)) return;
		AreaTerritory area = new AreaTerritory(territory);
		// Draw removes
		for (Renderable render : getRenderers(rendererName)) {
			try {
				render.drawRemove(area);
			} catch (Exception | Error problem) {
				ChatUtil.printConsoleError(render.getMapName()+" encountered a problem while trying to remove territory "+territory.getName());
				problem.printStackTrace();
			}
		}
	}

	public void drawLabelTerritory(KonTerritory territory) {
		drawLabelTerritory(territory,"");
	}
	
	public void drawLabelTerritory(KonTerritory territory, String rendererName) {
		if (isTerritoryDisabled(territory) || isTerritoryInvalid(territory)) return;
		AreaTerritory area = new AreaTerritory(territory);
		// Draw labels
		for (Renderable render : getRenderers(rendererName)) {
			try {
				render.drawLabel(area);
			} catch (Exception | Error problem) {
				ChatUtil.printConsoleError(render.getMapName()+" encountered a problem while trying to label territory "+territory.getName());
				problem.printStackTrace();
			}
		}
	}
	
	public void postBroadcast(String message) {
		for(Renderable render : renderers.values()) {
			try {
				render.postBroadcast(message);
			} catch (Exception | Error problem) {
				ChatUtil.printConsoleError(render.getMapName()+" encountered a problem while trying to broadcast a message");
				problem.printStackTrace();
			}
		}
	}

	public void drawAllTerritories() {
		drawAllTerritories("");
	}
	
	public void drawAllTerritories(String rendererName) {
		Date start = new Date();
		// Sanctuaries
		for (KonSanctuary sanctuary : konquest.getSanctuaryManager().getSanctuaries()) {
			drawUpdateTerritory(sanctuary, rendererName);
		}
		// Ruins
		for (KonRuin ruin : konquest.getRuinManager().getRuins()) {
			drawUpdateTerritory(ruin, rendererName);
		}
		// Camps
		for (KonCamp camp : konquest.getCampManager().getCamps()) {
			drawUpdateTerritory(camp, rendererName);
		}
		// Kingdoms
		for (KonKingdom kingdom : konquest.getKingdomManager().getKingdoms()) {
			drawUpdateTerritory(kingdom, rendererName);
		}
		Date end = new Date();
		int time = (int)(end.getTime() - start.getTime());
		if (rendererName.isEmpty()) {
			ChatUtil.printDebug("Rendering all territories in maps took "+time+" ms");
		} else {
			ChatUtil.printDebug("Rendering all territories with "+rendererName+" took "+time+" ms");
		}
	}

	/* Helper Methods */

	private ArrayList<Renderable> getRenderers(String name) {
		ArrayList<Renderable> renderList = new ArrayList<>();
		if(name.isEmpty() || !renderers.containsKey(name)) {
			// Include all renderers
			renderList.addAll(renderers.values());
		} else {
			// Only include the specified renderer
			renderList.add(renderers.get(name));
		}
		return renderList;
	}

	public static int getWebColor(KonTerritory territory) {
		int result = 0xFFFFFF;
		int webColor = territory.getKingdom().getWebColor();
		if(webColor == -1) {
			int hash = territory.getKingdom().getName().hashCode();
			result = hash & 0xFFFFFF;
		} else {
			result = webColor;
		}
		return result;
	}

	private boolean isTerritoryInvalid(KonTerritory territory) {
		boolean isValid = false;
		switch (territory.getTerritoryType()) {
			case SANCTUARY:
			case RUIN:
			case CAMP:
			case TOWN:
				isValid = true;
				break;
			case CAPITAL:
				// Capitals are only valid for created kingdoms (not barbarians or neutrals)
				if (territory.getKingdom().isCreated()) {
					isValid = true;
				}
				break;
			default:
				break;
		}
		return !isValid;
	}

	private boolean isTerritoryDisabled(KonTerritory territory) {
		boolean result = false;
		switch (territory.getTerritoryType()) {
			case SANCTUARY:
				result = isEnableSanctuaries;
				break;
			case RUIN:
				result = isEnableRuins;
				break;
			case CAMP:
				result = isEnableCamps;
				break;
			case CAPITAL:
			case TOWN:
				result = isEnableKingdoms;
				break;
			default:
				break;
		}
		return !result;
	}

	static String getGroupLabel(KonTerritory territory) {
		String result = "Konquest";
		switch (territory.getTerritoryType()) {
			case SANCTUARY:
				result = "Konquest Sanctuaries";
				break;
			case RUIN:
				result = "Konquest Ruins";
				break;
			case CAMP:
				result = "Konquest Barbarian Camps";
				break;
			case CAPITAL:
			case TOWN:
				result = "Konquest Kingdoms";
				break;
			default:
				break;
		}
		return result;
	}

	static String getIconLabel(KonTerritory territory) {
		String result = "Konquest";
		switch (territory.getTerritoryType()) {
			case SANCTUARY:
				result = MessagePath.MAP_SANCTUARY.getMessage()+" "+territory.getName();
				break;
			case RUIN:
				result = MessagePath.MAP_RUIN.getMessage()+" "+territory.getName();
				break;
			case CAMP:
				result = MessagePath.MAP_BARBARIAN.getMessage()+" "+territory.getName();
				break;
			case CAPITAL:
				result = territory.getKingdom().getCapital().getName();
				break;
			case TOWN:
				result = territory.getKingdom().getName()+" "+territory.getName();
				break;
			default:
				break;
		}
		return result;
	}

	private static String getImageData(String bannerKey) {
		String result = "";
		if (imageBase64Cache.containsKey(bannerKey)) {
			result = imageBase64Cache.get(bannerKey);
		} else if (imageBase64Cache.containsKey("default")) {
			result = imageBase64Cache.get("default");
		}
		return result;
	}

	static String getAreaLabel(KonTerritory territory) {
		String result = "Konquest";
		String bodyBegin = "<body style=\"background-color:#fff0cc;font-family:Helvetica;\">";
		String nameHeaderFormat = "<h2 style=\"text-align:center;color:#de791b;\">%s</h2>";
		String typeHeaderFormat = "<h3 style=\"color:#8048b8;\">%s</h3>";
		String propertyLineFormat = "<b>%s:</b> %s <br>";
		String bannerImageFormat = "<div align=\"center\"><img src=\"data:image/png;base64,%s\" alt=\"Banner\" height=\"96\"></div>";
		String imageData;
		StringBuilder labelMaker = new StringBuilder();
		switch (territory.getTerritoryType()) {
			case SANCTUARY:
				KonSanctuary sanctuary = (KonSanctuary)territory;
				labelMaker.append(bodyBegin);
				if (isShowBanners) {
					imageData = getImageData("sanctuary");
					if (!imageData.isEmpty()) {
						labelMaker.append(String.format(bannerImageFormat, imageData));
					}
				}
				labelMaker.append(String.format(nameHeaderFormat, sanctuary.getName()))
						.append("<hr>")
						.append(String.format(typeHeaderFormat, MessagePath.MAP_SANCTUARY.getMessage()))
						.append("<p>")
						.append(String.format(propertyLineFormat, MessagePath.MAP_TEMPLATES.getMessage(), sanctuary.getTemplates().size()))
						.append(String.format(propertyLineFormat, MessagePath.MAP_LAND.getMessage(), sanctuary.getChunkList().size()))
						.append("</p>")
						.append("</body>");
				result = labelMaker.toString();
				break;
			case RUIN:
				KonRuin ruin = (KonRuin)territory;
				labelMaker.append(bodyBegin);
				if (isShowBanners) {
					imageData = getImageData("ruin");
					if (!imageData.isEmpty()) {
						labelMaker.append(String.format(bannerImageFormat, imageData));
					}
				}
				labelMaker.append(String.format(nameHeaderFormat, ruin.getName()))
						.append("<hr>")
						.append(String.format(typeHeaderFormat, MessagePath.MAP_RUIN.getMessage()))
						.append("<p>")
						.append(String.format(propertyLineFormat, MessagePath.MAP_CRITICAL_HITS.getMessage(), ruin.getMaxCriticalHits()))
						.append(String.format(propertyLineFormat, MessagePath.MAP_GOLEM_SPAWNS.getMessage(), ruin.getSpawnLocations().size()))
						.append(String.format(propertyLineFormat, MessagePath.MAP_LAND.getMessage(), ruin.getChunkList().size()))
						.append(String.format(propertyLineFormat, MessagePath.MAP_COOLDOWN.getMessage(), ruin.getCaptureCooldownString()))
						.append("</p>")
						.append("</body>");
				result = labelMaker.toString();
				break;
			case CAMP:
				KonCamp camp = (KonCamp)territory;
				labelMaker.append(bodyBegin);
				if (isShowBanners) {
					imageData = getImageData("camp");
					if (!imageData.isEmpty()) {
						labelMaker.append(String.format(bannerImageFormat, imageData));
					}
				}
				labelMaker.append(String.format(bannerImageFormat, "camp"))
						.append(String.format(nameHeaderFormat, camp.getName()))
						.append("<hr>")
						.append(String.format(typeHeaderFormat, MessagePath.MAP_BARBARIANS.getMessage()))
						.append("<p>")
						.append(String.format(propertyLineFormat, MessagePath.MAP_OWNER.getMessage(), camp.getOwner().getName()))
						.append(String.format(propertyLineFormat, MessagePath.MAP_LAND.getMessage(), camp.getChunkList().size()))
						.append("</p>")
						.append("</body>");
				result = labelMaker.toString();
				break;
			case CAPITAL:
				KonCapital capital = (KonCapital)territory;
				String capitalLordName = "-";
				if(capital.getPlayerLord() != null) {
					capitalLordName = capital.getPlayerLord().getName();
				}
				String kingdomMasterName = "-";
				if(capital.getKingdom().isMasterValid()) {
					kingdomMasterName = capital.getKingdom().getPlayerMaster().getName();
				}
				int numKingdomOfficers = capital.getKingdom().getPlayerOfficersOnly().size();
				int numAllKingdomPlayers = territory.getKingdom().getNumMembers();
				int numKingdomTowns = territory.getKingdom().getTowns().size();
				int numKingdomLand = 0;
				for(KonTown town : territory.getKingdom().getCapitalTowns()) {
					numKingdomLand += town.getNumLand();
				}
				labelMaker.append(bodyBegin);
				if (isShowBanners) {
					imageData = getImageData(capital.getKingdom().getName());
					if (!imageData.isEmpty()) {
						labelMaker.append(String.format(bannerImageFormat, imageData));
					}
				}
				labelMaker.append(String.format(nameHeaderFormat, capital.getName()))
						.append("<hr>")
						.append(String.format(typeHeaderFormat, MessagePath.MAP_CAPITAL.getMessage()))
						.append("<p>")
						.append(String.format(propertyLineFormat, MessagePath.MAP_KINGDOM.getMessage(), capital.getKingdom().getName()))
						.append(String.format(propertyLineFormat, MessagePath.MAP_LORD.getMessage(), capitalLordName))
						.append(String.format(propertyLineFormat, MessagePath.MAP_KNIGHTS.getMessage(), capital.getPlayerKnightsOnly().size()))
						.append(String.format(propertyLineFormat, MessagePath.MAP_RESIDENTS.getMessage(), capital.getNumResidents()))
						.append(String.format(propertyLineFormat, MessagePath.MAP_LAND.getMessage(), capital.getNumLand()))
						.append(String.format(propertyLineFormat, MessagePath.MAP_COOLDOWN.getMessage(), capital.getCaptureCooldownString()))
						.append("</p>")
						.append(String.format(typeHeaderFormat, MessagePath.MAP_KINGDOM.getMessage()))
						.append("<p>")
						.append(String.format(propertyLineFormat, MessagePath.MAP_MASTER.getMessage(), kingdomMasterName))
						.append(String.format(propertyLineFormat, MessagePath.MAP_OFFICERS.getMessage(), numKingdomOfficers))
						.append(String.format(propertyLineFormat, MessagePath.MAP_MEMBERS.getMessage(), numAllKingdomPlayers))
						.append(String.format(propertyLineFormat, MessagePath.MAP_TOWNS.getMessage(), numKingdomTowns))
						.append(String.format(propertyLineFormat, MessagePath.MAP_LAND.getMessage(), numKingdomLand))
						.append("</p>")
						.append("</body>");
				result = labelMaker.toString();
				break;
			case TOWN:
				KonTown town = (KonTown)territory;
				String townLordName = "-";
				if(town.getPlayerLord() != null) {
					townLordName = town.getPlayerLord().getName();
				}
				labelMaker.append(bodyBegin);
				if (isShowBanners) {
					imageData = getImageData(town.getKingdom().getName());
					if (!imageData.isEmpty()) {
						labelMaker.append(String.format(bannerImageFormat, imageData));
					}
				}
				labelMaker.append(String.format(nameHeaderFormat, town.getName()))
						.append("<hr>")
						.append(String.format(typeHeaderFormat, MessagePath.MAP_TOWN.getMessage()))
						.append("<p>")
						.append(String.format(propertyLineFormat, MessagePath.MAP_KINGDOM.getMessage(), town.getKingdom().getName()))
						.append(String.format(propertyLineFormat, MessagePath.MAP_LORD.getMessage(), townLordName))
						.append(String.format(propertyLineFormat, MessagePath.MAP_KNIGHTS.getMessage(), town.getPlayerKnightsOnly().size()))
						.append(String.format(propertyLineFormat, MessagePath.MAP_RESIDENTS.getMessage(), town.getNumResidents()))
						.append(String.format(propertyLineFormat, MessagePath.MAP_LAND.getMessage(), town.getNumLand()))
						.append(String.format(propertyLineFormat, MessagePath.MAP_COOLDOWN.getMessage(), town.getCaptureCooldownString()))
						.append("</p>")
						.append("</body>");
				result = labelMaker.toString();
				break;
			default:
				break;
		}
		return result;
	}

}
