package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.model.KonCustomPrefix;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class PrefixCustomIcon extends MenuIcon {

	private final boolean isClickable;
	private final KonCustomPrefix prefix;
	
	public PrefixCustomIcon(KonCustomPrefix prefix, int index, boolean isClickable) {
		super(index);
		this.prefix = prefix;
		this.isClickable = isClickable;
	}
	
	public KonCustomPrefix getPrefix() {
		return prefix;
	}

	@Override
	public String getName() {
		return ChatUtil.parseHex(prefix.getName());
	}

	@Override
	public ItemStack getItem() {
		Material material = Material.IRON_BARS;
		boolean isProtected = false;
		if(isClickable) {
			isProtected = true;
			material = Material.GOLD_BLOCK;
		}
		return CompatibilityUtil.buildItem(material, getName(), getLore(), isProtected);
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}
}
