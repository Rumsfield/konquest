package com.github.rumsfield.konquest.model;

import com.github.rumsfield.konquest.api.model.KonquestPlot;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class KonPlot implements KonquestPlot {

	private final HashSet<Point> points;
	private final ArrayList<UUID> users;
	
	public KonPlot(Point origin) {
		this.points = new HashSet<>();
		this.users = new ArrayList<>();
		this.points.add(origin);
	}
	
	public KonPlot(HashSet<Point> points, ArrayList<UUID> users) {
		this.points = points;
		this.users = users;
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
		return new ArrayList<>(points);
	}
	
	public void addUsers(List<UUID> u) {
		for(UUID id : u) {
			addUser(id);
		}
	}
	
	public void addUser(UUID u) {
		if(!users.contains(u)) {
			users.add(u);
		}
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
	
	public void clearUsers() {
		users.clear();
	}
	
	public boolean hasUser(OfflinePlayer u) {
		return users.contains(u.getUniqueId());
	}
	
	public List<OfflinePlayer> getUserOfflinePlayers() {
		ArrayList<OfflinePlayer> result = new ArrayList<>();
		for(UUID id : users) {
			result.add(Bukkit.getOfflinePlayer(id));
		}
		return result;
	}
	
	public List<UUID> getUsers() {
		return new ArrayList<>(users);
	}
	
	public List<String> getUserStrings() {
		ArrayList<String> result = new ArrayList<>();
		for(UUID id : users) {
			result.add(id.toString());
		}
		return result;
	}
	
	public String getDisplayText() {
		String result = MessagePath.LABEL_PLOT.getMessage();
		if(!users.isEmpty()) {
			String firstName = Bukkit.getOfflinePlayer(users.get(0)).getName();
			result = result + " " + firstName;
			if(users.size() > 1) {
				result = result + " + " + (users.size() - 1);
			}
		}
		return result;
	}
	
	@Override
	public KonPlot clone() {
		HashSet<Point> clonePoints = new HashSet<>(points);
		ArrayList<UUID> cloneUsers = new ArrayList<>(users);
		return new KonPlot(clonePoints,cloneUsers);
	}
}
