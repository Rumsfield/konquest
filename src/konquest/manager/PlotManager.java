package konquest.manager;

import konquest.Konquest;
import konquest.utility.ChatUtil;

public class PlotManager {

	private Konquest konquest;
	private boolean isPlotsEnabled;
	private boolean isAllowBuild;
	private boolean isAllowContainers;
	private boolean isIgnoreKnights;
	private int maxSize;
	
	public PlotManager(Konquest konquest) {
		this.konquest = konquest;
		this.isPlotsEnabled = false;
		this.isAllowBuild = false;
		this.isAllowContainers = false;
		this.isIgnoreKnights = false;
		this.maxSize = 16;
	}
	
	public void initialize() {
		isPlotsEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.plots.enable",true);
		isAllowBuild = konquest.getConfigManager().getConfig("core").getBoolean("core.plots.allow_build",false);
		isAllowContainers = konquest.getConfigManager().getConfig("core").getBoolean("core.plots.allow_containers",false);
		isIgnoreKnights = konquest.getConfigManager().getConfig("core").getBoolean("core.plots.ignore_knights",true);
		maxSize = konquest.getConfigManager().getConfig("core").getInt("core.plots.max_size",16);
		ChatUtil.printDebug("Plot Manager is ready");
	}
	
	public boolean isEnabled() {
		return isPlotsEnabled;
	}
	
	public boolean isBuildAllowed() {
		return isAllowBuild;
	}
	
	public boolean isContainerAllowed() {
		return isAllowContainers;
	}
	
	public boolean isKnightIgnored() {
		return isIgnoreKnights;
	}
	
	public int getMaxSize() {
		return maxSize;
	}
	
	
	
}
