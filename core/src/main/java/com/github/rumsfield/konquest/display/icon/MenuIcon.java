package com.github.rumsfield.konquest.display.icon;

import org.bukkit.inventory.ItemStack;

public abstract class MenuIcon {

	private int index;

	public MenuIcon(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int val) {
		this.index = val;
	}

	public abstract String getName();

	public abstract ItemStack getItem();

	public abstract boolean isClickable();
	
}