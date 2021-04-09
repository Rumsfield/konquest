package konquest.model;

import java.util.ArrayList;
import java.util.Collection;

public class KonPrefix {

	private ArrayList<KonPrefixType> prefixList;
	private KonPrefixType mainPrefix;
	private boolean enabled;
	
	public KonPrefix() {
		this.prefixList = new ArrayList<KonPrefixType>();
		this.prefixList.add(KonPrefixType.getDefault());
		this.mainPrefix = KonPrefixType.getDefault();
		this.enabled = false;
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
			result = true;
		}
		return result;
	}
	
	public boolean hasPrefix(KonPrefixType prefix) {
		return prefixList.contains(prefix);
	}
	
	public String getMainPrefixName() {
		return mainPrefix.getName();
	}
	
	public Collection<String> getPrefixNames() {
		Collection<String> result = new ArrayList<String>();
		for(KonPrefixType pre : prefixList) {
			result.add(pre.getName());
		}
		return result;
	}
	
	
	
}
