package konquest.manager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import konquest.Konquest;
import konquest.model.KonGuild;
import konquest.model.KonKingdom;
import konquest.model.KonPlayer;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;

public class GuildManager {

	private Konquest konquest;
	private boolean isEnabled;
	private int payIntervalSeconds;
	private double payPerChunk;
	private double payPerResident;
	private double payLimit;
	private double specialChangeCost;
	
	private HashSet<KonGuild> guilds;
	
	public GuildManager(Konquest konquest) {
		this.konquest = konquest;
		this.isEnabled = false;
		this.payIntervalSeconds = 1800;
		this.payPerChunk = 1.0;
		this.payPerResident = 0.5;
		this.payLimit = 100;
		this.specialChangeCost = 200;
		this.guilds = new HashSet<KonGuild>();
	}
	
	public void initialize() {
		isEnabled 			= konquest.getConfigManager().getConfig("core").getBoolean("core.guilds.enable",false);
		payIntervalSeconds 	= konquest.getConfigManager().getConfig("core").getInt("core.guilds.pay_interval_seconds",0);
		payPerChunk 		= konquest.getConfigManager().getConfig("core").getDouble("core.guilds.pay_per_chunk",0);
		payPerResident 		= konquest.getConfigManager().getConfig("core").getDouble("core.guilds.pay_per_resident",0);
		payLimit 			= konquest.getConfigManager().getConfig("core").getDouble("core.guilds.pay_limit",0);
		specialChangeCost 	= konquest.getConfigManager().getConfig("core").getDouble("core.guilds.special_change_cost",0);
		ChatUtil.printDebug("Guild Manager is ready, enabled: "+isEnabled);
	}
	
	public boolean isEnabled() {
		return isEnabled;
	}
	
	public int createGuild(String name, KonPlayer master) {
		
		
		return 0;
	}
	
	public void editGuildOpen(boolean val) {
		
	}
	
	public void joinGuild(KonPlayer player, KonGuild guild) {
		
	}
	
	public void leaveGuild(KonPlayer player, KonGuild guild) {
		
	}
	
	
	public List<KonGuild> getAllGuilds() {
		List<KonGuild> result = new ArrayList<KonGuild>();
		for(KonGuild guild : guilds) {
			result.add(guild);
		}
		return result;
	}
	
	public List<KonGuild> getKingdomGuilds(KonKingdom kingdom) {
		List<KonGuild> result = new ArrayList<KonGuild>();
		for(KonGuild guild : guilds) {
			if(kingdom.equals(guild.getKingdom())) {
				result.add(guild);
			}
		}
		return result;
	}
	
	public List<KonGuild> getInviteGuilds(KonPlayer player) {
		List<KonGuild> result = new ArrayList<KonGuild>();
		for(KonGuild guild : guilds) {
			if(player.getKingdom().equals(guild.getKingdom()) && guild.isJoinInviteValid(player.getBukkitPlayer().getUniqueId())) {
				result.add(guild);
			}
		}
		return result;
	}
	
	public KonGuild getTownGuild(KonTown town) {
		KonGuild result = null;
		for(KonGuild guild : guilds) {
			if(guild.isTownMember(town)) {
				result = guild;
				break;
			}
		}
		return result;
	}
	
	
	
}
