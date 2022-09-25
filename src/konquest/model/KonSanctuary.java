package konquest.model;

import java.awt.Point;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.api.model.KonquestTerritoryType;
import konquest.utility.MessagePath;

public class KonSanctuary extends KonTerritory implements KonBarDisplayer {

	private BossBar sanctuaryBarAll;
	
	public KonSanctuary(Location loc, KonKingdom kingdom, Konquest konquest) {
		super(loc, MessagePath.TERRITORY_SANCTUARY.getMessage(), kingdom, konquest);
		this.sanctuaryBarAll = Bukkit.getServer().createBossBar(Konquest.sanctuaryColor+getName(), BarColor.WHITE, BarStyle.SEGMENTED_20);
		this.sanctuaryBarAll.setVisible(true);
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

}
