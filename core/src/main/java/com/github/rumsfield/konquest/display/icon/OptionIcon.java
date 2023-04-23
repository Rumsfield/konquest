package com.github.rumsfield.konquest.display.icon;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class OptionIcon implements MenuIcon {

	public enum optionAction {
		TOWN_OPEN,
		TOWN_PLOT_ONLY,
		TOWN_FRIENDLY_REDSTONE,
		TOWN_REDSTONE,
		TOWN_GOLEM
    }
	
	private final optionAction action;
	private final String name;
	private final List<String> lore;
	private final Material mat;
	private final int index;
	private final ItemStack item;
	
	public OptionIcon(optionAction action, String name, List<String> lore, Material mat, int index) {
		this.action = action;
		this.name = name;
		this.lore = lore;
		this.mat = mat;
		this.index = index;
		this.item = initItem();
	}
	
	private ItemStack initItem() {
		ItemStack item = new ItemStack(mat);
		ItemMeta meta = item.getItemMeta();
		assert meta != null;
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		meta.setDisplayName(getName());
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}
	
	public optionAction getAction() {
		return action;
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
