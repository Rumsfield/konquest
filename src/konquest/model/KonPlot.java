package konquest.model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class KonPlot {

	private HashSet<Point> points;
	private HashSet<UUID> users;
	
	public KonPlot(Point origin) {
		this.points = new HashSet<Point>();
		this.users = new HashSet<UUID>();
		this.points.add(origin);
	}
	
	public KonPlot(HashSet<Point> points, HashSet<UUID> users) {
		this.points = points;
		this.users = users;
	}
	
	public void addPoints(List<Point> p) {
		points.addAll(p);
	}
	
	public void addPoint(Point p) {
		points.add(p);
	}
	
	public boolean removePoint(Point p) {
		return points.remove(p);
	}
	
	public boolean hasPoint(Point p) {
		return points.contains(p);
	}
	
	public List<Point> getPoints() {
		ArrayList<Point> result = new ArrayList<Point>();
		for(Point p : points) {
			result.add(p);
		}
		return result;
	}
	
	public void addUsers(List<UUID> u) {
		users.addAll(u);
	}
	
	public void addUser(UUID u) {
		users.add(u);
	}
	
	public void addUser(OfflinePlayer u) {
		addUser(u.getUniqueId());
	}
	
	public boolean removeUser(UUID u) {
		return users.remove(u);
	}
	
	public boolean removeUser(OfflinePlayer u) {
		return removeUser(u.getUniqueId());
	}
	
	public boolean hasUser(OfflinePlayer u) {
		return users.contains(u.getUniqueId());
	}
	
	public List<OfflinePlayer> getUsers() {
		ArrayList<OfflinePlayer> result = new ArrayList<OfflinePlayer>();
		for(UUID id : users) {
			result.add(Bukkit.getOfflinePlayer(id));
		}
		return result;
	}
	
	public KonPlot clone() {
		return new KonPlot(this.points,this.users);
	}
}
