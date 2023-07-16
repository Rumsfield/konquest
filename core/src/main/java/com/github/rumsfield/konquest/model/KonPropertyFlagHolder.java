package com.github.rumsfield.konquest.model;

import java.util.Map;

public interface KonPropertyFlagHolder {

	void initProperties();

	boolean setPropertyValue(KonPropertyFlag property, boolean value);
	
	boolean getPropertyValue(KonPropertyFlag property);
	
	boolean hasPropertyValue(KonPropertyFlag property);
	
	Map<KonPropertyFlag,Boolean> getAllProperties();
	
	
}
