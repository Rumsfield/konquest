package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.model.KonTownOption;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class OptionIcon implements MenuIcon {
	
	private final KonTownOption option;
	private final String name;
	private final List<String> lore;
	private final Material mat;
	private final int index;
	private final ItemStack item;
	
	public OptionIcon(KonTownOption option, String name, List<String> lore, Material mat, int index) {
		this.option = option;
		this.name = name;
		this.lore = lore;
		this.mat = mat;
		this.index = index;
		this.item = initItem();
	}
	
	private ItemStack initItem() {
		String name = getName();
		return CompatibilityUtil.buildItem(mat, name, lore);
	}
	
	public KonTownOption getOption() {
		return option;
	}
	
	@Override
	public int getIndex() {
		return index;
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
		return true;
	}

}
