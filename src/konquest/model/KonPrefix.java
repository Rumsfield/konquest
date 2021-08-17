package konquest.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class KonPrefix {

	private ArrayList<KonPrefixType> prefixList;
	private KonPrefixType mainPrefix;
	private boolean enabled;
	private String customLabel;
	private String customPrefix;
	private HashSet<String> availableCustoms;
	
	public KonPrefix() {
		this.prefixList = new ArrayList<KonPrefixType>();
		this.prefixList.add(KonPrefixType.getDefault());
		this.mainPrefix = KonPrefixType.getDefault();
		this.enabled = false;
		this.customLabel = "";
		this.customPrefix = "";
		this.availableCustoms = new HashSet<String>();
	}
	
	public void setEnable(boolean en) {
		enabled = en;
	}
	
	public boolean isEnabled() {
		return (enabled == true);
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
	 * Set the player's main prefix if it is a valid added prefix
	 * @param prefix
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
			customLabel = "";
			result = true;
		}
		return result;
	}
	
	public boolean hasPrefix(KonPrefixType prefix) {
		return prefixList.contains(prefix);
	}
	
	public String getMainPrefixName() {
		if(customLabel == null || customLabel.equals("")) {
			return mainPrefix.getName();
		} else {
			return customPrefix;
		}
	}
	
	public KonPrefixType getMainPrefix() {
		return mainPrefix;
	}
	
	public Collection<String> getPrefixNames() {
		Collection<String> result = new ArrayList<String>();
		for(KonPrefixType pre : prefixList) {
			result.add(pre.getName());
		}
		return result;
	}
	
	public boolean setCustomPrefix(String label, String prefix) {
		if(isCustomAvailable(label)) {
			customLabel = label;
			customPrefix = prefix;
			return true;
		}
		return false;
	}
	
	public String getCustom() {
		return customLabel;
	}
	
	public void addAvailableCustom(String label) {
		availableCustoms.add(label);
	}
	
	public boolean isCustomAvailable(String label) {
		return availableCustoms.contains(label);
	}
}
