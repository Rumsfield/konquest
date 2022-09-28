package konquest.model;

import java.awt.Point;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.api.model.KonquestTerritoryType;
import konquest.utility.MessagePath;

public class KonSanctuary extends KonTerritory implements KonBarDisplayer, KonPropertyFlagHolder {

	private BossBar sanctuaryBarAll;
	private Map<KonPropertyFlag,Boolean> properties;
	private Map<String, KonMonumentTemplate> templates;
	
	public KonSanctuary(Location loc, String name, KonKingdom kingdom, Konquest konquest) {
		super(loc, name, kingdom, konquest);
		this.sanctuaryBarAll = Bukkit.getServer().createBossBar(Konquest.neutralColor+MessagePath.TERRITORY_SANCTUARY.getMessage().trim()+" "+getName(), BarColor.WHITE, BarStyle.SEGMENTED_20);
		this.sanctuaryBarAll.setVisible(true);
		this.properties = new HashMap<KonPropertyFlag,Boolean>();
		initProperties();
		this.templates = new HashMap<String, KonMonumentTemplate>();
	}
	
	private void initProperties() {
		properties.clear();
		properties.put(KonPropertyFlag.TRAVEL, 	getKonquest().getConfigManager().getConfig("properties").getBoolean("properties.sanctuaries.travel"));
		properties.put(KonPropertyFlag.PVP, 	getKonquest().getConfigManager().getConfig("properties").getBoolean("properties.sanctuaries.pvp"));
		properties.put(KonPropertyFlag.BUILD, 	getKonquest().getConfigManager().getConfig("properties").getBoolean("properties.sanctuaries.build"));
		properties.put(KonPropertyFlag.USE, 	getKonquest().getConfigManager().getConfig("properties").getBoolean("properties.sanctuaries.use"));
		properties.put(KonPropertyFlag.MOBS, 	getKonquest().getConfigManager().getConfig("properties").getBoolean("properties.sanctuaries.mobs"));
		properties.put(KonPropertyFlag.PORTALS, getKonquest().getConfigManager().getConfig("properties").getBoolean("properties.sanctuaries.portals"));
		properties.put(KonPropertyFlag.ENTER, 	getKonquest().getConfigManager().getConfig("properties").getBoolean("properties.sanctuaries.enter"));
		properties.put(KonPropertyFlag.EXIT, 	getKonquest().getConfigManager().getConfig("properties").getBoolean("properties.sanctuaries.exit"));
	}

	@Override
	public void addBarPlayer(KonPlayer player) {
		if(sanctuaryBarAll == null) {
			return;
		}
		sanctuaryBarAll.addPlayer(player.getBukkitPlayer());
	}

	@Override
	public void removeBarPlayer(KonPlayer player) {
		if(sanctuaryBarAll == null) {
			return;
		}
		sanctuaryBarAll.removePlayer(player.getBukkitPlayer());
	}

	@Override
	public void removeAllBarPlayers() {
		if(sanctuaryBarAll == null) {
			return;
		}
		sanctuaryBarAll.removeAll();
	}

	@Override
	public void updateBarPlayers() {
		if(sanctuaryBarAll == null) {
			return;
		}
		sanctuaryBarAll.removeAll();
		for(KonPlayer player : getKonquest().getPlayerManager().getPlayersOnline()) {
			Player bukkitPlayer = player.getBukkitPlayer();
			if(isLocInside(bukkitPlayer.getLocation())) {
				sanctuaryBarAll.addPlayer(bukkitPlayer);
			}
		}
	}

	@Override
	public int initClaim() {
		addChunk(Konquest.toPoint(getCenterLoc()));
		return 0;
	}

	@Override
	public boolean addChunk(Point point) {
		addPoint(point);
		return true;
	}

	@Override
	public boolean testChunk(Point point) {
		return true;
	}

	@Override
	public KonquestTerritoryType getTerritoryType() {
		return KonquestTerritoryType.SANCTUARY;
	}

	@Override
	public boolean setPropertyValue(KonPropertyFlag property, boolean value) {
		boolean result = false;
		if(properties.containsKey(property)) {
			properties.put(property, value);
			result = true;
		}
		return result;
	}

	@Override
	public boolean getPropertyValue(KonPropertyFlag property) {
		boolean result = false;
		if(properties.containsKey(property)) {
			result = properties.get(property);
		}
		return result;
	}
	
	@Override
	public boolean hasPropertyValue(KonPropertyFlag property) {
		return properties.containsKey(property);
	}

	@Override
	public Map<KonPropertyFlag, Boolean> getAllProperties() {
		return new HashMap<KonPropertyFlag, Boolean>(properties);
	}
	
	public Collection<KonMonumentTemplate> getTemplates() {
		return templates.values();
	}
	
	public Set<String> getTemplateNames() {
		return templates.keySet();
	}
	
	public boolean isTemplate(String name) {
		return templates.containsKey(name.toLowerCase());
	}
	
	public boolean addTemplate(String name, KonMonumentTemplate template) {
		boolean result = false;
		if(!templates.containsKey(name.toLowerCase())) {
			templates.put(name.toLowerCase(), template);
			result = true;
		}
		return result;
	}
	
	public KonMonumentTemplate getTemplate(String name) {
		KonMonumentTemplate result = null;
		if(templates.containsKey(name.toLowerCase())) {
			result = templates.get(name.toLowerCase());
		}
		return result;
	}
	
	public boolean removeTemplate(String name) {
		boolean result = false;
		if(templates.containsKey(name.toLowerCase())) {
			templates.remove(name.toLowerCase());
			result = true;
		}
		return result;
	}
	
	public void clearAllTemplates() {
		templates.clear();
	}

}
