package konquest.manager;

import java.awt.Point;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import konquest.Konquest;
import konquest.display.PlotMenu;
import konquest.model.KonTown;

public class PlotManager {

	private Konquest konquest;
	private boolean isPlotsEnabled;
	private HashMap<Inventory, PlotMenu> plotMenus;
	
	public PlotManager(Konquest konquest) {
		this.konquest = konquest;
		this.isPlotsEnabled = false;
		this.plotMenus = new HashMap<Inventory, PlotMenu>();
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
	
	public void displayPlotMenu(Player bukkitPlayer, KonTown town) {
		
		DisplayManager.playMenuOpenSound(bukkitPlayer);
		
		Point center = konquest.toPoint(bukkitPlayer.getLocation());
		if(!town.isLocInside(bukkitPlayer.getLocation())) {
			center = konquest.toPoint(town.getCenterLoc());
		}
		
		PlotMenu newMenu = new PlotMenu(town, center);
		
		//TODO: stuff
		
		
		newMenu.refreshNavigationButtons();
		plotMenus.put(newMenu.getCurrentView().getInventory(), newMenu);
		// Schedule delayed task to display inventory to player
		Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
            @Override
            public void run() {
            	bukkitPlayer.openInventory(newMenu.getCurrentView().getInventory());
            }
        },1);
		
	}
	
}
