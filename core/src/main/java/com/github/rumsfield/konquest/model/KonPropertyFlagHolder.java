package com.github.rumsfield.konquest.model;

import java.util.Map;

public interface KonPropertyFlagHolder {

	public boolean setPropertyValue(KonPropertyFlag property, boolean value);
	
	public boolean getPropertyValue(KonPropertyFlag property);
	
	public boolean hasPropertyValue(KonPropertyFlag property);
	
	public Map<KonPropertyFlag,Boolean> getAllProperties();
	
	
}
