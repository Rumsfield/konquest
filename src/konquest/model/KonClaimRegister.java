package konquest.model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/*
 * Holds the most recent sets of claims that a player makes, to support claim undo operations.
 * This register works as a Last In First Out (LIFO) Queue.
 * 
 * Intended usage of this class:
 * 	Push data
 *  Get current data, then pop
 */

public class KonClaimRegister {

	private ArrayList<Set<Point>> claimRegister;
	private ArrayList<KonTerritory> territoryRegister;
	private final int MAX = 10;
	
	private Set<Point> currentClaim;
	private KonTerritory currentTerritory;
	
	
	public KonClaimRegister() {
		this.claimRegister = new ArrayList<Set<Point>>();
		this.territoryRegister = new ArrayList<KonTerritory>();
		this.currentClaim = new HashSet<Point>();
		this.currentTerritory = null;
	}
	
	public Set<Point> getClaim() {
		return currentClaim;
	}
	
	public KonTerritory getTerritory() {
		return currentTerritory;
	}
	
	// Adds data to register and updates current data
	public void push(Set<Point> claim, KonTerritory territory) {
		if(claim == null || territory == null) {
			return;
		}
		
		// Insert new data to front of list
		claimRegister.add(0,claim);
		territoryRegister.add(0,territory);
		
		// Trim to max size
		if(claimRegister.size() > MAX) {
			claimRegister.remove(MAX);
		}
		if(territoryRegister.size() > MAX) {
			territoryRegister.remove(MAX);
		}
		
		// Update current data
		update();
		
	}
	
	// Removes the first data and shifts all other up, updates current
	public void pop() {
		
		// Remove last data
		if(!claimRegister.isEmpty()) {
			claimRegister.remove(0);
		}
		if(!territoryRegister.isEmpty()) {
			territoryRegister.remove(0);
		}
		
		// Update current data
		update();
		
	}
	
	private void update() {
		if(claimRegister.isEmpty()) {
			currentClaim = new HashSet<Point>();
		} else {
			currentClaim = claimRegister.get(0);
		}
		
		if(territoryRegister.isEmpty()) {
			currentTerritory = null;
		} else {
			currentTerritory = territoryRegister.get(0);
		}
		
	}
	
}
