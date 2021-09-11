package konquest.model;

import konquest.Konquest;

import java.awt.Point;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public class KonCapital extends KonTerritory{
	
	private BossBar capitalBarAll;
	
	public KonCapital(Location loc, KonKingdom kingdom, Konquest konquest) {
		super(loc, kingdom.getName()+" "+konquest.getConfigManager().getConfig("core").getString("core.kingdoms.capital_suffix"), kingdom, KonTerritoryType.CAPITAL, konquest);
		this.capitalBarAll = Bukkit.getServer().createBossBar(ChatColor.GOLD+getName(), BarColor.WHITE, BarStyle.SOLID);
		this.capitalBarAll.setVisible(true);
	}

	public KonCapital(Location loc, String name, KonKingdom kingdom, Konquest konquest) {
		super(loc, name, kingdom, KonTerritoryType.CAPITAL, konquest);
	}
	
	public void updateName() {
		setName(getKingdom().getName()+" "+getKonquest().getConfigManager().getConfig("core").getString("core.kingdoms.capital_suffix"));
	}
	
	@Override
	public boolean addChunk(Point point) {
		addPoint(point);
		return true;
	}
	
	@Override
	public int initClaim() {
		addChunk(getKonquest().toPoint(getCenterLoc()));
		return 0;
	}
	
	public void addBarPlayer(KonPlayer player) {
		if(capitalBarAll == null) {
			return;
		}
		capitalBarAll.addPlayer(player.getBukkitPlayer());
	}
	
	public void removeBarPlayer(KonPlayer player) {
		if(capitalBarAll == null) {
			return;
		}
		capitalBarAll.removePlayer(player.getBukkitPlayer());
	}
	
	public void removeAllBarPlayers() {
		if(capitalBarAll == null) {
			return;
		}
		capitalBarAll.removeAll();
	}
	
	public void updateBarPlayers() {
		if(capitalBarAll == null) {
			return;
		}
		capitalBarAll.removeAll();
		for(KonPlayer player : getKonquest().getPlayerManager().getPlayersOnline()) {
			Player bukkitPlayer = player.getBukkitPlayer();
			if(isLocInside(bukkitPlayer.getLocation())) {
				capitalBarAll.addPlayer(bukkitPlayer);
			}
		}
	}

}
