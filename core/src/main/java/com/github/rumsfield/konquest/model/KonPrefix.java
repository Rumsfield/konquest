package com.github.rumsfield.konquest.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class KonPrefix {

	private final ArrayList<KonPrefixType> prefixList;
	private KonPrefixType mainPrefix;
	private boolean enabled;
	private KonCustomPrefix customPrefix;
	private final HashSet<String> availableCustoms;
	
	public KonPrefix() {
		this.prefixList = new ArrayList<>();
		this.prefixList.add(KonPrefixType.getDefault());
		this.mainPrefix = KonPrefixType.getDefault();
		this.enabled = false;
		this.customPrefix = null;
		this.availableCustoms = new HashSet<>();
	}
	
	public void setEnable(boolean en) {
		enabled = en;
	}
	
	public boolean isEnabled() {
		return (enabled);
	}
	
	public void addPrefix(KonPrefixType prefix) {
		if(!prefixList.contains(prefix)) {
			prefixList.add(prefix);
		}
	}
	
	public void addPrefix(Collection<KonPrefixType> prefixes) {
		for(KonPrefixType pre : prefixes) {
			if(!prefixList.contains(pre)) {
				prefixList.add(pre);
			}
		}
	}
	
	public void clear() {
		prefixList.clear();
	}
	
	/**
	 * Set the player's main prefix if it is a valid-added prefix
	 * @param prefix the prefix to set
	 */
	public boolean setPrefix(KonPrefixType prefix) {
		boolean result = false;
		if(hasPrefix(prefix)) {
			mainPrefix = prefix;
			result = true;
		}
		return result;
	}
	
	public boolean selectPrefix(KonPrefixType prefix) {
		boolean result = false;
		if(prefixList.contains(prefix)) {
			mainPrefix = prefix;
			customPrefix = null;
			result = true;
		}
		return result;
	}
	
	public boolean hasPrefix(KonPrefixType prefix) {
		return prefixList.contains(prefix);
	}
	
	public String getMainPrefixName() {
		if(customPrefix == null) {
			return mainPrefix.getName();
		} else {
			return customPrefix.getName();
		}
	}
	
	public KonPrefixType getMainPrefix() {
		return mainPrefix;
	}
	
	public Collection<String> getPrefixNames() {
		Collection<String> result = new ArrayList<>();
		for(KonPrefixType pre : prefixList) {
			result.add(pre.getName());
		}
		return result;
	}
	
	public boolean setCustomPrefix(KonCustomPrefix prefix) {
		if(isCustomAvailable(prefix.getLabel())) {
			customPrefix = prefix;
			return true;
		}
		return false;
	}
	
	public String getCustom() {
		if(customPrefix == null) {
			return "";
		} else {
			return customPrefix.getLabel();
		}
	}
	
	public void addAvailableCustom(String label) {
		availableCustoms.add(label);
	}
	
	public boolean isCustomAvailable(String label) {
		return availableCustoms.contains(label);
	}
}
