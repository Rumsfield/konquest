package konquest.model;

import java.util.Collection;
import java.util.HashSet;

/**
 * Represents a group of adjacent camps
 *
 */
public class KonCampGroup {

	private HashSet<KonCamp> camps;
	
	public KonCampGroup() {
		this.camps = new HashSet<KonCamp>();
	}
	
	public boolean containsCamp(KonCamp camp) {
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
	
}
