package com.github.rumsfield.konquest.display.icon;

import org.bukkit.inventory.ItemStack;

public interface MenuIcon {
	
	public int getIndex();
	
	public String getName();
	
	public ItemStack getItem();
	
	public boolean isClickable();
	
}

/*
public abstract class MenuIcon {

	private ItemStack item;
	private String name;
	private int index;
	
	public MenuIcon(String name, int index) {
		this.name = name;
		this.index = index;
		this.item =  new ItemStack(Material.DIRT, 1);
	}
	
	public String getName() {
		return name;
	}
	
	public int getIndex() {
		return index;
	}
	
	public ItemStack getItem() {
		return item;
	}
	
	public void setItem(ItemStack item) {
		this.item = item;
	}
	
	public abstract ItemStack initItem();
	
}
*/