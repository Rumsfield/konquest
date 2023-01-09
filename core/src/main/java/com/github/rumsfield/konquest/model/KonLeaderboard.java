package com.github.rumsfield.konquest.model;

import java.util.ArrayList;

import org.bukkit.OfflinePlayer;

import com.github.rumsfield.konquest.utility.ChatUtil;

public class KonLeaderboard {

	private ArrayList<String> leaderNames;
	private ArrayList<Integer> leaderScores;
	private ArrayList<OfflinePlayer> leaderPlayers;
	private int entries;
	
	public KonLeaderboard() {
		this.leaderNames = new ArrayList<String>();
		this.leaderScores = new ArrayList<Integer>();
		this.leaderPlayers = new ArrayList<OfflinePlayer>();
		this.entries = 0;
	}
	
	public void clearAll() {
		leaderNames.clear();
		leaderPlayers.clear();
		leaderScores.clear();
		entries = 0;
	}
	
	public int getSize() {
		return entries;
	}
	
	public boolean isEmpty() {
		return entries == 0;
	}
	
	public void addEntry(OfflinePlayer bukkitOfflinePlayer, int score) {
		leaderNames.add(bukkitOfflinePlayer.getName());
		leaderPlayers.add(bukkitOfflinePlayer);
		leaderScores.add(score);
		entries++;
	}
	
	public String getName(int index) {
		String result = "";
		try {
			result = leaderNames.get(index);
		} catch(IndexOutOfBoundsException e) {
			ChatUtil.printDebug("Bad leaderboard index: "+e.getMessage());
		}
		return result;
	}
	
	public OfflinePlayer getOfflinePlayer(int index) {
		OfflinePlayer result = null;
		try {
			result = leaderPlayers.get(index);
		} catch(IndexOutOfBoundsException e) {
			ChatUtil.printDebug("Bad leaderboard index: "+e.getMessage());
		}
		return result;
	}
	
	public int getScore(int index) {
		int result = 0;
		try {
			result = leaderScores.get(index);
		} catch(IndexOutOfBoundsException e) {
			ChatUtil.printDebug("Bad leaderboard index: "+e.getMessage());
		}
		return result;
	}
	
}
