package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonTownOption;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import org.bukkit.inventory.ItemStack;


public class OptionIcon extends MenuIcon {
	
	private final KonTownOption option;
	private final boolean isClickable;
	
	public OptionIcon(KonTownOption option, int index, boolean isClickable) {
		super(index);
		this.option = option;
		this.isClickable = isClickable;
		// Item Lore
		addDescription(option.getDescription());
	}
	
	public KonTownOption getOption() {
		return option;
	}

	@Override
	public String getName() {
		return DisplayManager.nameFormat+option.getName();
	}

	@Override
	public ItemStack getItem() {
		return CompatibilityUtil.buildItem(option.getDisplayMaterial(), getName(), getLore());
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}

}
