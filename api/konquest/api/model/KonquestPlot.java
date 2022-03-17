package konquest.api.model;

import java.awt.Point;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

/**
 * A plot of land within a town, with player members (users) and land (points).
 * 
 * @author Rumsfield
 *
 */
public interface KonquestPlot {

	/**
	 * Checks whether the given point is a part of this plot.
	 * Use the {@link konquest.api.KonquestAPI#toPoint(Location loc) toPoint} method to convert from location to point easily.
	 * Make sure to check that the world matches also.
	 * 
	 * @param point The point to check
	 * @return True when this plot contains the point, else false
	 */
	public boolean hasPoint(Point point);
	
	/**
	 * Checks whether the given player is a member (user) of this plot.
	 * 
	 * @param player The player to check
	 * @return True when the player is a member, else false
	 */
	public boolean hasUser(OfflinePlayer player);
	
	/**
	 * Gets the list of all land (points) for this plot.
	 * 
	 * @return The list of points
	 */
	public List<Point> getPoints();
	
	/**
	 * Gets the list of all members (users) for this plot.
	 * 
	 * @return The list of players
	 */
	public List<OfflinePlayer> getUserOfflinePlayers();
	
}
