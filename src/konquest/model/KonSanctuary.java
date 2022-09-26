package konquest.model;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

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
	
	public KonSanctuary(Location loc, String name, KonKingdom kingdom, Konquest konquest) {
		super(loc, name, kingdom, konquest);
		this.sanctuaryBarAll = Bukkit.getServer().createBossBar(Konquest.sanctuaryColor+MessagePath.TERRITORY_SANCTUARY.getMessage().trim()+" "+getName(), BarColor.WHITE, BarStyle.SEGMENTED_20);
		this.sanctuaryBarAll.setVisible(true);
		
		this.properties = new HashMap<KonPropertyFlag,Boolean>();
		initProperties();
	}
	
	private void initProperties() {
		properties.clear();
		//TODO: Replace defaults with config options
		properties.put(KonPropertyFlag.TRAVEL, 	true);
		properties.put(KonPropertyFlag.PVP, 	false);
		properties.put(KonPropertyFlag.BUILD, 	false);
		properties.put(KonPropertyFlag.USE, 	true);
		properties.put(KonPropertyFlag.MOBS, 	false);
		properties.put(KonPropertyFlag.PORTALS, true);
		properties.put(KonPropertyFlag.ENTER, 	true);
		properties.put(KonPropertyFlag.EXIT, 	true);
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

}
