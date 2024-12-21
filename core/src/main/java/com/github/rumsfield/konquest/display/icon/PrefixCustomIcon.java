package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.model.KonCustomPrefix;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PrefixCustomIcon extends MenuIcon {

	private final List<String> lore;
	private final boolean isClickable;
	private final KonCustomPrefix prefix;
	private final ItemStack item;
	
	public PrefixCustomIcon(KonCustomPrefix prefix, List<String> lore, int index, boolean isClickable) {
		super(index);
		this.prefix = prefix;
		this.lore = lore;
		this.isClickable = isClickable;
		this.item = initItem();
	}
	
	private ItemStack initItem() {
		Material material = Material.IRON_BARS;
		boolean isProtected = false;
		if(isClickable) {
			isProtected = true;
			material = Material.GOLD_BLOCK;
		}
		String name = ChatUtil.parseHex(prefix.getName());
		return CompatibilityUtil.buildItem(material, name, lore, isProtected);
	}
	
	public KonCustomPrefix getPrefix() {
		return prefix;
	}

	@Override
	public String getName() {
		return prefix.getName();
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
