package com.github.rumsfield.konquest.display.icon;

import org.bukkit.inventory.ItemStack;

public interface MenuIcon {
	
	int getIndex();
	
	String getName();
	
	ItemStack getItem();
	
	boolean isClickable();
	
}