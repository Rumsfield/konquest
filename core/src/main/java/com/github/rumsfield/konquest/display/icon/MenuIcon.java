package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.display.StateMenu;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

public abstract class MenuIcon {

	private int index; // The slot index in the inventory of this icon
	private StateMenu.State state; // The state that this icon will update the menu to

	public MenuIcon(int index) {
		this.index = index;
		this.state = null;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int val) {
		this.index = val;
	}

	public @Nullable StateMenu.State getState() {
		return state;
	}

	public void setState(StateMenu.State state) {
		this.state = state;
	}

	public abstract String getName();

	public abstract ItemStack getItem();

	public abstract boolean isClickable();
	
}