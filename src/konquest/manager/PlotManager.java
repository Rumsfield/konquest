package konquest.manager;

import java.awt.Point;

import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import konquest.Konquest;

public class PlotManager {

	private Konquest konquest;
	private boolean isPlotsEnabled;
	
	public PlotManager(Konquest konquest) {
		this.konquest = konquest;
		this.isPlotsEnabled = false;
	}
	
	//TODO: Ensure there is error checking/validation on editor finish, to prevent issues when multiple players are editing the same plot.
	
	public void initialize() {
		
		isPlotsEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.plots.enable",true);
		
	}
	
	public boolean isEnabled() {
		return isPlotsEnabled;
	}
	
	public void createPlot(Point p, World w) {
		
	}
	
	public void deletePlot(Point p, World w) {
		
	}
	
	public void addPlotPoint(Point p, World w) {
		
	}
	
	public void removePlotPoint(Point p, World w) {
		
	}
	
	public void addPlotUser(OfflinePlayer player) {
		
	}
	
	public void removePlotUser(OfflinePlayer player) {
		
	}
	
}
