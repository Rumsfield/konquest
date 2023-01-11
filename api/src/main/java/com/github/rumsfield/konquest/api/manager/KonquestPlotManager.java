package com.github.rumsfield.konquest.api.manager;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.github.rumsfield.konquest.api.model.KonquestTown;

/**
 * A manager for town plots in Konquest.
 * 
 * @author Rumsfield
 *
 */
public interface KonquestPlotManager {

	/**
	 * Checks whether town plots are enabled from the Konquest configuration.
	 * 
	 * @return True when plots are enabled, else false
	 */
	public boolean isEnabled();
	
	/**
	 * Checks if the plot at the given location for the given town is protected from edits from the given player.
	 * Town lords can always build in all plots.
	 * 
	 * @param town The town to check
	 * @param loc The location to check
	 * @param player The player trying to build
	 * @return True when the player is not allowed to edit the plot at loc in town
	 */
	public boolean isPlayerPlotProtectBuild(KonquestTown town, Location loc, Player player);
	
	/**
	 * Checks if the plot at the given location for the given town is protected from container access from the given player.
	 * Town lords can always access containers in all plots.
	 * 
	 * @param town The town to check
	 * @param loc The location to check
	 * @param player The player trying to access containers
	 * @return True when the player is not allowed to access containers in the plot at loc in town
	 */
	public boolean isPlayerPlotProtectContainer(KonquestTown town, Location loc, Player player);
}
