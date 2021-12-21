package konquest.manager;

import konquest.Konquest;
import konquest.utility.ChatUtil;

public class GuildManager {

	private Konquest konquest;
	private boolean isEnabled;
	private int payIntervalSeconds;
	private double payPerChunk;
	private double payPerResident;
	private double payLimit;
	private double specialChangeCost;
	
	public GuildManager(Konquest konquest) {
		this.konquest = konquest;
		this.isEnabled = false;
		this.payIntervalSeconds = 600;
		this.payPerChunk = 1.0;
		this.payPerResident = 0.5;
		this.payLimit = 100;
		this.specialChangeCost = 200;
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
}
