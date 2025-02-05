package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class InfoIcon extends MenuIcon{
	
	private final String name;
	private final Material mat;
	private String info;
	private final boolean isClickable;
	
	public InfoIcon(String name, Material mat, int index, boolean isClickable) {
		super(index);
		this.name = name;
		this.mat = mat;
		this.info = "";
		this.isClickable = isClickable;
	}
	
	public void setInfo(String info) {
		this.info = info;
	}
	
	public String getInfo() {
		return info;
	}

	@Override
	public String getName() {
		return DisplayManager.nameFormat+name;
	}

	@Override
	public ItemStack getItem() {
		return CompatibilityUtil.buildItem(mat, getName(), getLore());
	}
	
	@Override
	public boolean isClickable() {
		return isClickable;
	}
	
}