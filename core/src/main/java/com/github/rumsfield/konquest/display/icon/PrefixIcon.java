package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.model.KonPrefixType;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class PrefixIcon extends MenuIcon {

	private final boolean isClickable;
	private final KonPrefixType prefix;
	
	public PrefixIcon(KonPrefixType prefix, int index, boolean isClickable) {
		super(index);
		this.prefix = prefix;
		this.isClickable = isClickable;
	}
	
	public KonPrefixType getPrefix() {
		return prefix;
	}

	@Override
	public String getName() {
		ChatColor nameColor = isClickable ? ChatColor.DARK_GREEN : ChatColor.GRAY;
		return nameColor+prefix.getName();
	}

	@Override
	public ItemStack getItem() {
		Material material = isClickable ? prefix.category().getMaterial() : Material.IRON_BARS;
		return CompatibilityUtil.buildItem(material, getName(), getLore());
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}

}
