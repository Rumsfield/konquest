package konquest.model;

import konquest.Konquest;
import konquest.api.model.KonquestCapital;
import konquest.api.model.KonquestTerritoryType;
import konquest.utility.CorePath;

import org.bukkit.Location;


public class KonCapital extends KonTown implements KonquestCapital {

	public KonCapital(Location loc, KonKingdom kingdom, Konquest konquest) {
		super(loc, kingdom.getName()+" "+konquest.getConfigManager().getConfig("core").getString(CorePath.KINGDOMS_CAPITAL_SUFFIX.getPath()), kingdom, konquest);
		
	}
	
	public KonCapital(Location loc, String name, KonKingdom kingdom, Konquest konquest) {
		super(loc, name, kingdom, konquest);
	}
	
	public void updateName() {
		setName(getKingdom().getName()+" "+getKonquest().getConfigManager().getConfig("core").getString(CorePath.KINGDOMS_CAPITAL_SUFFIX.getPath()));
		updateBar();
	}
	
	@Override
	public KonquestTerritoryType getTerritoryType() {
		return KonquestTerritoryType.CAPITAL;
	}
	
	/*
	 * 
	 * Original class implementation
	 * 
	 */
	
	/*
public class KonCapital extends KonTerritory implements KonquestCapital, KonBarDisplayer {

	private BossBar capitalBarAll;
	
	public KonCapital(Location loc, KonKingdom kingdom, Konquest konquest) {
		super(loc, kingdom.getName()+" "+konquest.getConfigManager().getConfig("core").getString("core.kingdoms.capital_suffix"), kingdom, KonquestTerritoryType.CAPITAL, konquest);
		this.capitalBarAll = Bukkit.getServer().createBossBar(ChatColor.GOLD+getName(), BarColor.WHITE, BarStyle.SOLID);
		this.capitalBarAll.setVisible(true);
	}

	public KonCapital(Location loc, String name, KonKingdom kingdom, Konquest konquest) {
		super(loc, name, kingdom, KonquestTerritoryType.CAPITAL, konquest);
	}
	
	public void updateName() {
		setName(getKingdom().getName()+" "+getKonquest().getConfigManager().getConfig("core").getString("core.kingdoms.capital_suffix"));
		capitalBarAll.setTitle(ChatColor.GOLD+getName());
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
	public int initClaim() {
		addChunk(Konquest.toPoint(getCenterLoc()));
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
	*/
}
