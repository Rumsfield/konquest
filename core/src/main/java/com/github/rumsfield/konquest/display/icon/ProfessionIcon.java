package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

public class ProfessionIcon extends MenuIcon {

	private final Villager.Profession profession;
	private final boolean isClickable;

	public ProfessionIcon(Villager.Profession profession, int index, boolean isClickable) {
		super(index);
		this.profession = profession;
		this.isClickable = isClickable;
	}
	
	public Villager.Profession getProfession() {
		return profession;
	}

	@Override
	public String getName() {
		return DisplayManager.nameFormat + CompatibilityUtil.getProfessionName(profession);
	}

	@Override
	public ItemStack getItem() {
		return CompatibilityUtil.buildItem(CompatibilityUtil.getProfessionMaterial(profession), getName(), getLore());
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}
}
