package com.github.rumsfield.konquest.model;

import com.github.rumsfield.konquest.api.model.KonquestCamp;
import com.github.rumsfield.konquest.api.model.KonquestCampGroup;
import org.bukkit.OfflinePlayer;

import java.util.Collection;
import java.util.HashSet;

/**
 * Represents a group of adjacent camps
 *
 */
public class KonCampGroup implements KonquestCampGroup {

	private final HashSet<KonCamp> camps;
	
	public KonCampGroup() {
		this.camps = new HashSet<>();
	}
	
	public boolean containsCamp(KonquestCamp camp) {
		return camps.contains(camp);
	}
	
	public boolean addCamp(KonCamp camp) {
		return camps.add(camp);
	}
	
	public boolean removeCamp(KonCamp camp) {
		return camps.remove(camp);
	}
	
	public void clearCamps() {
		camps.clear();
	}
	
	public Collection<KonCamp> getCamps() {
		return camps;
	}
	
	public void mergeGroup(KonCampGroup otherGroup) {
		camps.addAll(otherGroup.getCamps());
	}
	
	public boolean isPlayerMember(OfflinePlayer bukkitOfflinePlayer) {
		boolean result = false;
		for(KonCamp camp : camps) {
			if(camp.isPlayerOwner(bukkitOfflinePlayer)) {
				result = true;
				break;
			}
		}
		return result;
	}
	
}
