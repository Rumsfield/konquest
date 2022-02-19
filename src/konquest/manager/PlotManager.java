package konquest.manager;

import java.awt.Point;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.api.manager.KonquestPlotManager;
import konquest.api.model.KonquestTown;
import konquest.model.KonPlot;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;

public class PlotManager implements KonquestPlotManager {

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
		isPlotsEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.plots.enable",false);
		isAllowBuild = konquest.getConfigManager().getConfig("core").getBoolean("core.plots.allow_build",false);
		isAllowContainers = konquest.getConfigManager().getConfig("core").getBoolean("core.plots.allow_containers",false);
		isIgnoreKnights = konquest.getConfigManager().getConfig("core").getBoolean("core.plots.ignore_knights",false);
		maxSize = konquest.getConfigManager().getConfig("core").getInt("core.plots.max_size",16);
		ChatUtil.printDebug("Plot Manager is ready, enabled: "+isPlotsEnabled);
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
	
	public void removePlotPoint(KonTown town, Location loc) {
		if(town.hasPlot(loc)) {
			//ChatUtil.printDebug("Removing plot point in town "+town.getName());
			KonPlot plot = town.getPlot(loc).clone();
			if(plot != null) {
				town.removePlot(plot);
				plot.removePoint(Konquest.toPoint(loc));
				if(!plot.getPoints().isEmpty()) {
					town.putPlot(plot);
				}
			}
		}
	}
	
	public boolean addPlot(KonTown town, KonPlot plot) {
		boolean enabled = konquest.getConfigManager().getConfig("core").getBoolean("core.plots.enable",false);
		if(enabled) {
			// Verify plot points exist within town
			for(Point p : plot.getPoints()) {
				if(!town.getChunkList().containsKey(p)) {
					return false;
				}
			}
			// Verify plot users are town residents
			for(OfflinePlayer p : plot.getUserOfflinePlayers()) {
				if(!town.isPlayerResident(p)) {
					return false;
				}
			}
			//ChatUtil.printDebug("Adding plot to town "+town.getName());
			// Add the plot to the town
			town.putPlot(plot);
		}
		return true;
	}
	
	/**
	 * Check if the plot at the given location for the given town is protected from the given player.
	 * Town lords can always build in all plots.
	 * @param town
	 * @param loc
	 * @param player
	 * @return True when the player is not allowed to edit the plot at loc in town.
	 */
	public boolean isPlayerPlotProtectBuild(KonquestTown townArg, Location loc, Player player) {
		boolean result = false;
		if(townArg instanceof KonTown) {
			KonTown town = (KonTown) townArg;
			if(town.hasPlot(loc)) {
				// Town has a plot at the given location
				if(!isBuildAllowed()) {
					// Only plot members may build
					if(!town.getPlot(loc).hasUser(player)) {
						// The player is not a plot member
						if(isKnightIgnored()) {
							// Knights ignore plot protection
							if(!town.isPlayerElite(player)) {
								// The player is not a knight or lord, and cannot edit this plot
								result = true;
							}
						} else {
							// Knights are included in protections
							if(!town.isPlayerLord(player)) {
								// The player is not the lord, and cannot edit this plot
								result = true;
							}
						}
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Check if the plot at the given location for the given town is protected from the given player.
	 * Town lords can always access containers in all plots.
	 * @param town
	 * @param loc
	 * @param player
	 * @return True when the player is not allowed to access containers in the plot at loc in town.
	 */
	public boolean isPlayerPlotProtectContainer(KonquestTown townArg, Location loc, Player player) {
		boolean result = false;
		if(townArg instanceof KonTown) {
			KonTown town = (KonTown) townArg;
			if(town.hasPlot(loc)) {
				// Town has a plot at the given location
				if(!isContainerAllowed()) {
					// Only plot members may build
					if(!town.getPlot(loc).hasUser(player)) {
						// The player is not a plot member
						if(isKnightIgnored()) {
							// Knights ignore plot protection
							if(!town.isPlayerElite(player)) {
								// The player is not a knight or lord, and cannot edit this plot
								result = true;
							}
						} else {
							// Knights are included in protections
							if(!town.isPlayerLord(player)) {
								// The player is not the lord, and cannot edit this plot
								result = true;
							}
						}
					}
				}
			}
		}
		return result;
	}
	
}
