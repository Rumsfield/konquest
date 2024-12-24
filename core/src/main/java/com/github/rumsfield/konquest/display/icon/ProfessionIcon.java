package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ProfessionIcon extends MenuIcon {

	private final String name;
	private final List<String> lore;
	private final Villager.Profession profession;
	private final boolean isClickable;

	private final String nameColor = DisplayManager.nameFormat;

	public ProfessionIcon(List<String> lore, Villager.Profession profession, int index, boolean isClickable) {
		super(index);
		this.name = CompatibilityUtil.getProfessionName(profession);
		this.lore = lore;
		this.profession = profession;
		this.isClickable = isClickable;
	}
	
	public Villager.Profession getProfession() {
		return profession;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ItemStack getItem() {
		return CompatibilityUtil.buildItem(CompatibilityUtil.getProfessionMaterial(profession), nameColor+getName(), lore);
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}
}
