package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.Konquest;
import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ProfessionIcon implements MenuIcon {

	private final String name;
	private final List<String> lore;
	private final Villager.Profession profession;
	private final int index;
	private final boolean isClickable;
	
	public ProfessionIcon(String name, List<String> lore, Villager.Profession profession, int index, boolean isClickable) {
		this.name = name;
		this.lore = lore;
		this.profession = profession;
		this.index = index;
		this.isClickable = isClickable;
	}
	
	public Villager.Profession getProfession() {
		return profession;
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
		ItemStack item = new ItemStack(Konquest.getProfessionMaterial(profession),1);
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

	@Override
	public boolean isClickable() {
		return isClickable;
	}
}
