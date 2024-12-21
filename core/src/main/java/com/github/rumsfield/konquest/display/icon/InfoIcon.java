package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class InfoIcon extends MenuIcon{
	
	private final String name;
	private final List<String> lore;
	private final Material mat;
	private String info;
	private final ItemStack item;
	private final boolean isClickable;
	
	public InfoIcon(String name, List<String> lore, Material mat, int index, boolean isClickable) {
		super(index);
		this.name = name;
		this.lore = lore;
		this.mat = mat;
		this.info = "";
		this.isClickable = isClickable;
		this.item = initItem();
	}

	private ItemStack initItem() {
		String name = getName();
		return CompatibilityUtil.buildItem(mat, name, lore);
	}
	
	public void setInfo(String info) {
		this.info = info;
	}
	
	public String getInfo() {
		return info;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ItemStack getItem() {
		return item;
	}
	
	@Override
	public boolean isClickable() {
		return isClickable;
	}
	
}